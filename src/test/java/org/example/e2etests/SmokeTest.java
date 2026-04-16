package org.example.e2etests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

import static org.assertj.core.api.Assertions.assertThat;

class SmokeTest extends E2eTestBase {

    @Autowired PostgreSQLContainer<?> postgres;
    @Autowired KafkaContainer kafka;
    @Autowired GenericContainer<?> gateway;
    @Autowired GenericContainer<?> authService;
    @Autowired GenericContainer<?> dataImporter;
    @Autowired GenericContainer<?> profileService;
    @Autowired GenericContainer<?> projectService;
    @Autowired GenericContainer<?> mentorService;
    @Autowired GenericContainer<?> jobMarketAnalytics;

    @Test
    void infrastructureContainersAreRunning() {
        assertThat(postgres.isRunning()).as("PostgreSQL").isTrue();
        assertThat(kafka.isRunning()).as("Kafka").isTrue();
    }

    @Test
    void serviceContainersAreRunning() {
        assertThat(gateway.isRunning()).as("gateway").isTrue();
        assertThat(authService.isRunning()).as("auth-service").isTrue();
        assertThat(dataImporter.isRunning()).as("data-importer").isTrue();
        assertThat(profileService.isRunning()).as("profile-service").isTrue();
        assertThat(projectService.isRunning()).as("project-service").isTrue();
        assertThat(mentorService.isRunning()).as("mentor-service").isTrue();
        assertThat(jobMarketAnalytics.isRunning()).as("job-market-analytics-service").isTrue();
    }
}
