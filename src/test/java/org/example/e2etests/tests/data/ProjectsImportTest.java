package org.example.e2etests.tests.data;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.example.e2etests.tests.base.E2eTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
public class ProjectsImportTest extends E2eTestBase {

    private static boolean profileCreated = false;

    @BeforeEach
    void setUp() throws InterruptedException {
        if (!profileCreated) {
            profileCreate();
            profileCreated = true;
        }
        jdbcTemplate.execute("DELETE FROM profile_service.project");
        jdbcTemplate.execute("DELETE FROM project_service.projects");
        assertTableIsEmpty("project_service.projects");
        assertTableIsEmpty("profile_service.project");
        assertKafkaTopicEmpty("projects.project.created");
    }

    @Test
    void shouldCreateProjectAndSaveToDatabaseAndGoogleSheets() throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("github_repository_url", "https://github.com/zhukovsd/currency-exchange-test");
        requestBody.put("programming_language", "Java");
        requestBody.put("roadmap_project", "CURRENCY-EXCHANGE");

        startImport("/api/project/project", requestBody);
        Thread.sleep(5000);
        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertTableHasRecords("project_service.projects");
                    assertTableHasRecords("profile_service.project");
                });

        List<List<Object>> values = googleSheetsHelper.readSheet(testSpreadsheetId, "Projects!A:ZZ");
        boolean projectFound = values.stream()
                .anyMatch(row -> row.toString().contains("currency-exchange-test"));
        assertThat(projectFound).isTrue();
    }

    @Test
    void shouldCreateProjectAndSaveToDatabaseAndGoogleSheetsFromTgBot() throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("author_telegram_user_id", 123456789);
        requestBody.put("github_repository_url", "https://github.com/zhukovsd/hangman-test");
        requestBody.put("programming_language", "Java");
        requestBody.put("roadmap_project", "HANGMAN");
        requestBody.put("author_telegram_username", "zhukovsd");
        requestBody.put("project_source_type", "TELEGRAM_BOT");

        startImport("/api/project/internal/project", requestBody);
        Thread.sleep(5000);
        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertTableHasRecords("project_service.projects");
                    assertTableHasRecords("profile_service.project");
                });

        List<List<Object>> values = googleSheetsHelper.readSheet(testSpreadsheetId, "Projects!A:ZZ");
        boolean projectFound = values.stream()
                .anyMatch(row -> row.toString().contains("hangman-test"));
        assertThat(projectFound).isTrue();
    }

    @Test
    void shouldCreateProjectAndSaveToDatabaseFromDataImporter() throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("author_telegram_user_id", 123456789);
        requestBody.put("github_repository_url", "https://github.com/zhukovsd/hangman-test2");
        requestBody.put("programming_language", "Java");
        requestBody.put("roadmap_project", "HANGMAN");
        requestBody.put("author_telegram_username", "zhukovsd");
        requestBody.put("added_timestamp", "1765628000");
        requestBody.put("project_source_type", "DATA_IMPORTER");

        startImport("/api/project/internal/project", requestBody);
        Thread.sleep(5000);
        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertTableHasRecords("project_service.projects");
                    assertTableHasRecords("profile_service.project");
                });
    }

    private void assertKafkaTopicEmpty(String topic) {
        kafkaConsumer.subscribe(List.of(topic));
        kafkaConsumer.poll(Duration.ofMillis(500));
        kafkaConsumer.seekToEnd(kafkaConsumer.assignment());
        ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(2));
        assertThat(records.count()).isZero();
    }

    private void assertTableIsEmpty(String tableName) {
        int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
        assertThat(count).isZero();
    }

    private String createAdminToken() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
        Date now = new Date();
        return Jwts.builder()
                .subject("123456789")
                .claim("roles", List.of("ADMIN"))
                .claim("telegram_username", "e2e_test_admin")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000))
                .signWith(key)
                .compact();
    }

    private void startImport(String path, Map<String, Object> requestBody) {
        int port = gateway.getMappedPort(8080);
        String host = gateway.getHost();
        if (path.contains("internal")) {
            port = projectService.getMappedPort(8080);
            host = projectService.getHost();
        }
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, createHeaders());

        testRestTemplate.postForEntity(
                "http://" + host + ":" + port + path,
                requestEntity,
                String.class
        );

    }


    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Access-Token", createAdminToken());
        headers.set("X-Telegram-User-Id", "123456789");
        headers.set("X-Telegram-Username", "llllqqqqqqqqq");
        headers.set("X-User-Roles", "STUDENT");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private void assertTableHasRecords(String tableName) {
        int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
        assertThat(count).isGreaterThan(0);
    }

    private void profileCreate() {
        Map<String, Object> details = new HashMap<>();
        details.put("github_profile_url", "https://github.com/created_by_internal_request");
        details.put("telegram_url", "https://t.me/created_by_internal_request");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("telegram_user_id", 123456789);
        requestBody.put("details", details);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        int port = profileService.getMappedPort(8080);
        String host = profileService.getHost();

        testRestTemplate.postForEntity("http://" + host + ":" + port + "/api/profile/internal/profile", entity, String.class);
    }
}
