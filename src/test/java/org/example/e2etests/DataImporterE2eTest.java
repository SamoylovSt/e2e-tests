package org.example.e2etests;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class DataImporterE2eTest extends E2eTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${JWT_SECRET}")
    String secret;

    private KafkaConsumer<String, String> kafkaConsumer;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("group.id", "test-" + System.currentTimeMillis());
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");

        kafkaConsumer = new KafkaConsumer<>(props);
    }

    @AfterEach
    void tearDown() {
        kafkaConsumer.close();
    }

    @Test
    void shouldImportUsersAndSendMessagesToKafka() throws InterruptedException {
        assertTableIsEmpty("auth_service.users");
        kafkaConsumer.subscribe(List.of("auth.user.created"));

        startImport("/api/data-importer/start-users-import");

        Thread.sleep(10_000);

        assertTableHasRecords("auth_service.users");

        ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);
        assertThat(records.iterator().next().value()).contains("telegram_user_id");
    }

    @Test
    void shouldImportProfiles() throws InterruptedException {
        assertTableIsEmpty("profile_service.profiles");

        startImport("/api/data-importer/start-profiles-import");

        Thread.sleep(10_000);

        assertTableHasRecords("profile_service.profiles");
    }

    @Test
    void shouldImportProjectsAndSendMessagesToKafka() throws InterruptedException {
        assertTableIsEmpty("project_service.projects");
        kafkaConsumer.subscribe(List.of("projects.project.created"));

        startImport("/api/data-importer/start-projects-import");

        Thread.sleep(10_000);

        assertTableHasRecords("project_service.projects");

        ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);
        assertThat(records.iterator().next().value()).contains("author_telegram_user_id");
    }

    private String createAdminToken() {
        return Jwts.builder()
                .subject("1")
                .claim("telegram_username", "e2e_test_admin")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    private void assertTableIsEmpty(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Integer.class
        );
        assertThat(count).isZero();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Access-Token", createAdminToken());
        headers.set("X-User-Roles", "ADMIN");
        return headers;
    }

    private void assertTableHasRecords(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Integer.class
        );
        assertThat(count).isGreaterThan(0);
    }

    private void startImport(String path) {
        int port = dataImporter.getMappedPort(8080);

        restTemplate.postForEntity(
                "http://localhost:" + port + path,
                new HttpEntity<>(createHeaders()),
                String.class
        );
    }
}
