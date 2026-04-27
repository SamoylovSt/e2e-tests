package org.example.e2etests;

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
    GenericContainer<?> gateway;
    @Autowired GenericContainer<?> authService;
    @Autowired GenericContainer<?> dataImporter;
    @Autowired GenericContainer<?> profileService;
    @Autowired GenericContainer<?> projectService;
    @Autowired GenericContainer<?> mentorService;
    @Autowired GenericContainer<?> jobMarketAnalytics;
}
