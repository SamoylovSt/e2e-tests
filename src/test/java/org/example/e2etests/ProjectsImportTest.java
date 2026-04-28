package org.example.e2etests;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.crypto.SecretKey;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
@Slf4j
public class ProjectsImportTest extends E2eTestBase {

    @BeforeEach
    void setUp() {
        // Создаём профиль для тестов
        jdbcTemplate.update("DELETE FROM profile_service.profiles WHERE telegram_user_id = 123456789");
        jdbcTemplate.update(
                "INSERT INTO profile_service.profiles (id, telegram_user_id) VALUES (1, 123456789)"
        );
        log.info("Profile created for user 123456789");
    }
    @Test
    void shouldPersistProjectInDb() {

        assertTableIsEmpty("project_service.projects");
        assertTableIsEmpty("profile_service.project");
        startImport("/api/project/project");
        assertTableHasRecords("project_service.projects");
        assertTableHasRecords("profile_service.project");
    }


    private void assertTableIsEmpty(String tableName) {
        int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
        assertThat(count).isZero();
    }

    private String createAdminToken() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        Date now = new Date();
        return Jwts.builder()
                .subject("1")
                .claim("roles", List.of("ADMIN"))
                .claim("telegram_username", "e2e_test_admin")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000))
                .signWith(key)
                .compact();
    }

    private void startImport(String path) {
        int port = gateway.getMappedPort(8080);
        String host = gateway.getHost();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("github_repository_url", "https://github.com/zhukovsd/currency-exchange-test");
        requestBody.put("programming_language", "Java");
        requestBody.put("roadmap_project", "CURRENCY-EXCHANGE");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, createHeaders());

        restTemplate.postForEntity(
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
}
