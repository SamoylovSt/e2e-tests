package org.example.e2etests.containers;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

@SuppressWarnings("resource")
public final class ServiceContainers {

    private static final String TAG =
        System.getenv().getOrDefault("SERVICE_IMAGE_TAG", "dev");

    private static final String GHCR = "ghcr.io/it-mentor-community-platform";

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Required environment variable '" + name + "' is not set."
            );
        }
        return value;
    }

    private static GenericContainer<?> springService(String imagePath) {
        String serviceName = imagePath.substring(imagePath.lastIndexOf('/') + 1);
        return new GenericContainer<>(GHCR + "/" + imagePath + ":" + TAG)
            .withNetwork(InfrastructureContainers.NETWORK)
            .withEnv("SPRING_PROFILES_ACTIVE", "local-stack")
            .withExposedPorts(8080)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(serviceName)))
            .waitingFor(
                Wait.forHttp("/actuator/health")
                    .forPort(8080)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5))
            );
    }

    public static final GenericContainer<?> GATEWAY =
        springService("gateway/gateway");

    public static final GenericContainer<?> AUTH_SERVICE =
        springService("auth-service/auth-service")
            .withEnv("TELEGRAM_BOT_TOKEN", requireEnv("TELEGRAM_BOT_TOKEN"))
            .dependsOn(InfrastructureContainers.POSTGRES);

    public static final GenericContainer<?> DATA_IMPORTER =
        springService("data-importer/data-importer")
            .withEnv("GOOGLE_APPLICATION_CREDENTIALS_JSON",
                requireEnv("GOOGLE_APPLICATION_CREDENTIALS_JSON"))
            .dependsOn(InfrastructureContainers.POSTGRES, InfrastructureContainers.KAFKA);

    public static final GenericContainer<?> PROFILE_SERVICE =
        springService("profile-service/profile-service")
            .dependsOn(InfrastructureContainers.POSTGRES, InfrastructureContainers.KAFKA);

    public static final GenericContainer<?> PROJECT_SERVICE =
        springService("project-service/project-service")
            .dependsOn(InfrastructureContainers.POSTGRES, InfrastructureContainers.KAFKA);

    public static final GenericContainer<?> MENTOR_SERVICE =
        springService("mentor-service/mentor-service")
            .dependsOn(InfrastructureContainers.POSTGRES, InfrastructureContainers.KAFKA);

    public static final GenericContainer<?> JOB_MARKET_ANALYTICS =
        springService("job-market-analytics-service/job-market-analytics-service")
            .dependsOn(InfrastructureContainers.POSTGRES, InfrastructureContainers.KAFKA);

    private ServiceContainers() {}
}
