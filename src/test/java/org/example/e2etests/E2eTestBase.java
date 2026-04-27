package org.example.e2etests;

import io.jsonwebtoken.security.Keys;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

import javax.crypto.SecretKey;
import java.util.Base64;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
public abstract class E2eTestBase {

    @Autowired
    KafkaConsumer<String, String> kafkaConsumer;
    @Value("${jwt.secret}")
    String secret;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    TestRestTemplate restTemplate;
    @Autowired
    PostgreSQLContainer<?> postgres;
    @Autowired
    KafkaContainer kafka;
    @Autowired
    KafkaConsumer<String, String> kafkaConsumer;
    @Autowired
    GenericContainer<?> gateway;
    @Autowired
    GenericContainer<?> authService;
    @Autowired
    GenericContainer<?> dataImporter;
    @Autowired
    GenericContainer<?> profileService;
    @Autowired
    GenericContainer<?> projectService;
    @Autowired
    GenericContainer<?> mentorService;
    @Autowired
    GenericContainer<?> jobMarketAnalytics;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    TestRestTemplate testRestTemplate;

    @Value("${jwt.secret}")
    String jwtSecret;

    @Value("${telegram.init-data}")
    String telegramInitData;

    @Value("${telegram.updated-init-data}")
    String updatedTelegramInitData;

    SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
    }
}
