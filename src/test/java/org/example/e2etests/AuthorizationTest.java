package org.example.e2etests;

import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationTest extends E2eTestBase {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(200);
    private static final Duration KAFKA_POLL_TIMEOUT = Duration.ofSeconds(5);
    private static final String AUTH_ENDPOINT = "/api/auth/by-telegram";
    private static final String PROFILE_ENDPOINT = "/api/profile/profile";
    private static final String KAFKA_TOPIC = "auth.user.created";
    private static final String EXPECTED_ROLE = "STUDENT";


    @Test
    void shouldRegisterUserWithTelegramUsername() {
        String accessToken = authenticateViaTelegram(telegramInitData);
        Claims claims = parseJwt(accessToken);
        Long telegramUserId = extractTelegramUserIdFrom(claims);

        assertUserRole(claims);
        Awaitility.await()
                .atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL_INTERVAL)
                .ignoreExceptions()
                .untilAsserted(() ->
                        assertThat(fetchUserProfile(accessToken, telegramUserId))
                                .isNotNull()
                                .extracting(ResponseEntity::getStatusCode)
                                .isEqualTo(HttpStatus.OK)
                );

        assertUserPersistedInDatabase(telegramUserId);
        assertProfilePersistedInDatabase(telegramUserId);
        assertKafkaEventPublished();
    }

    @Test
    void shouldAuthorizeExistingUser() {

        String firstToken = authenticateViaTelegram(telegramInitData);
        Claims firstClaims = parseJwt(firstToken);
        Long telegramUserId = extractTelegramUserIdFrom(firstClaims);

        String secondToken = authenticateViaTelegram(telegramInitData);
        Claims secondClaims = parseJwt(secondToken);

        assertUserRole(secondClaims);
        assertThat(extractTelegramUserIdFrom(secondClaims)).isEqualTo(telegramUserId);

        Awaitility.await().atMost(AWAIT_TIMEOUT).pollInterval(AWAIT_POLL_INTERVAL)
                .ignoreExceptions()
                .untilAsserted(() ->
                        assertThat(fetchUserProfile(secondToken, telegramUserId))
                                .extracting(ResponseEntity::getStatusCode)
                                .isEqualTo(HttpStatus.OK)
                );

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT * FROM auth_service.users WHERE telegram_user_id = ?", telegramUserId);
        assertThat(users).hasSize(1);
    }

    @Test
    void shouldAuthorizeUserWithUpdatedData() {
        String firstToken = authenticateViaTelegram(telegramInitData);
        Claims firstClaims = parseJwt(firstToken);
        Long telegramUserId = extractTelegramUserIdFrom(firstClaims);

        String updatedToken = authenticateViaTelegram(updatedTelegramInitData);
        Claims updatedClaims = parseJwt(updatedToken);

        assertUserRole(updatedClaims);
        assertThat(extractTelegramUserIdFrom(updatedClaims)).isEqualTo(telegramUserId);

        Awaitility.await()
                .atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL_INTERVAL)
                .ignoreExceptions()
                .untilAsserted(() ->
                        assertThat(fetchUserProfile(updatedToken, telegramUserId))
                                .extracting(ResponseEntity::getStatusCode)
                                .isEqualTo(HttpStatus.OK)
                );

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT * FROM auth_service.users WHERE telegram_user_id = ?", telegramUserId);
        assertThat(users).hasSize(1);
    }

    private String authenticateViaTelegram(String telegramInitData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> request = new HttpEntity<>(telegramInitData, headers);

        ResponseEntity<String> response = testRestTemplate.postForEntity(
                AUTH_ENDPOINT,
                request,
                String.class
        );

        assertThat(response.getHeaders().getFirst("X-Access-Token")).isNotNull();
        return response.getHeaders().getFirst("X-Access-Token");
    }

    private Claims parseJwt(String token){
         return Jwts.parser()
                 .verifyWith(secretKey())
                 .build()
                 .parseSignedClaims(token)
                 .getPayload();
    }

    private Long extractTelegramUserIdFrom(Claims claims) {
        assertThat(claims.getSubject()).isNotBlank();
        return Long.parseLong(claims.getSubject());
    }

    private ResponseEntity<JsonNode> fetchUserProfile(String token, Long telegramUserId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Access-Token", token);
        headers.set("X-Telegram-User-Id", String.valueOf(telegramUserId));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        return testRestTemplate.exchange(
                PROFILE_ENDPOINT,
                HttpMethod.GET,
                request,
                JsonNode.class
        );
    }

    private void assertUserRole(Claims claims) {
        List<String> roles = claims.get("roles", List.class);
        assertThat(roles).contains(EXPECTED_ROLE);
    }

    private void assertUserPersistedInDatabase(Long telegramUserId) {
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT * FROM auth_service.users WHERE telegram_user_id = ?",
                telegramUserId
        );
        assertThat(user).containsEntry("telegram_user_id", telegramUserId);
    }

    private void assertProfilePersistedInDatabase(Long telegramUserId) {
        Map<String, Object> profile = jdbcTemplate.queryForMap(
                "SELECT * FROM profile_service.profiles WHERE telegram_user_id = ?",
                telegramUserId
        );
        assertThat(profile).containsEntry("telegram_user_id", telegramUserId);
    }

    private void assertKafkaEventPublished() {
        kafkaConsumer.subscribe(Collections.singletonList(KAFKA_TOPIC));
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(kafkaConsumer, KAFKA_POLL_TIMEOUT);
        assertThat(records.count()).isGreaterThan(0);
    }

}