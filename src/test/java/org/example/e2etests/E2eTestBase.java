package org.example.e2etests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
public abstract class E2eTestBase {

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
