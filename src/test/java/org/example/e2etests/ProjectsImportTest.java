package org.example.e2etests;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
public class ProjectsImportTest extends E2eTestBase {
    @Autowired
    private GoogleSheetsTestHelper googleSheetsHelper;
//
    @Value("${GOOGLE_TEST_SPREADSHEET_ID}")
    private String testSpreadsheetId;


    @Test
    void shouldPersistProjectInDb() throws IOException, InterruptedException {
        assertKafkaTopicEmpty("projects.project.created");
        assertTableIsEmpty("project_service.projects");
        assertTableIsEmpty("profile_service.project");
        startImport("/api/project/project");
        assertTableHasRecords("project_service.projects");
        //   assertTableHasRecords("profile_service.project");
        Thread.sleep(10000);



        //     6. Проверяем Google Sheets
//        List<List<Object>> values = googleSheetsHelper.readSheet(testSpreadsheetId, "A1:Z1");
//        assertThat(values).isNotNull();
//        assertThat(values.size()).isGreaterThan(0);
//
//        boolean projectFound = values.stream()
//                .anyMatch(row -> row.toString().contains("currency-exchange-test"));
//        assertThat(projectFound).isTrue();

    }

    private void assertKafkaTopicEmpty(String topic) {
        kafkaConsumer.subscribe(List.of(topic));
        kafkaConsumer.poll(Duration.ofMillis(100));
        kafkaConsumer.seekToBeginning(kafkaConsumer.assignment());
        ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isZero();
    }

    private void assertTableIsEmpty(String tableName) {
        int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
        assertThat(count).isZero();
    }

    private String createAdminToken() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
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

    private void startImport(String path) {
        int port = projectService.getMappedPort(8080);
        String host = projectService.getHost();

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
