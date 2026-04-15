package org.example.e2etests;

import org.example.e2etests.containers.InfrastructureContainers;
import org.example.e2etests.containers.ServiceContainers;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmokeTest extends E2eTestBase {

    @Test
    void infrastructureContainersAreRunning() {
        assertThat(InfrastructureContainers.POSTGRES.isRunning()).as("PostgreSQL").isTrue();
        assertThat(InfrastructureContainers.KAFKA.isRunning()).as("Kafka").isTrue();
    }

    @Test
    void serviceContainersAreRunning() {
        assertThat(ServiceContainers.GATEWAY.isRunning()).as("gateway").isTrue();
        assertThat(ServiceContainers.AUTH_SERVICE.isRunning()).as("auth-service").isTrue();
        assertThat(ServiceContainers.DATA_IMPORTER.isRunning()).as("data-importer").isTrue();
        assertThat(ServiceContainers.PROFILE_SERVICE.isRunning()).as("profile-service").isTrue();
        assertThat(ServiceContainers.PROJECT_SERVICE.isRunning()).as("project-service").isTrue();
        assertThat(ServiceContainers.MENTOR_SERVICE.isRunning()).as("mentor-service").isTrue();
        assertThat(ServiceContainers.JOB_MARKET_ANALYTICS.isRunning()).as("job-market-analytics-service").isTrue();
    }
}
