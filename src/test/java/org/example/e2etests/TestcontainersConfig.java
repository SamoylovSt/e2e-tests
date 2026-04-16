package org.example.e2etests;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@TestConfiguration(proxyBeanMethods = false)
@SuppressWarnings("resource")
class TestcontainersConfig {

    private static final String TAG =
        System.getenv().getOrDefault("SERVICE_IMAGE_TAG", "dev");

    private static final String GHCR = "ghcr.io/it-mentor-community-platform";

    private static final List<String> REQUIRED_TOPICS = List.of(
        "auth.user.created",
        "auth.user.authenticated",
        "projects.project.created"
    );

    @Value("${TELEGRAM_BOT_TOKEN}")
    private String telegramBotToken;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS_JSON}")
    private String googleCredentialsJson;

    @Bean
    Network network() {
        return Network.newNetwork();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres(Network network) {
        return new PostgreSQLContainer<>("postgres:18.0-alpine")
            .withNetwork(network)
            .withNetworkAliases("database")
            .withDatabaseName("it_mentor_community_platform")
            .withUsername("root")
            .withPassword("password")
            .withInitScript("init.sql");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka(Network network) {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withListener("kafka:19092");
    }

    @Bean
    Boolean kafkaTopicsReady(KafkaContainer kafka) throws Exception {
        if (!kafka.isRunning()) {
            kafka.start();
        }
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(props)) {
            adminClient.createTopics(
                REQUIRED_TOPICS.stream()
                    .map(name -> new NewTopic(name, 1, (short) 1))
                    .toList()
            ).all().get(30, TimeUnit.SECONDS);
        }
        return Boolean.TRUE;
    }

    @Bean
    GenericContainer<?> gateway(Network network) {
        return springService("gateway/gateway", network);
    }

    @Bean
    GenericContainer<?> authService(Network network, PostgreSQLContainer<?> postgres) {
        return springService("auth-service/auth-service", network)
            .withEnv("TELEGRAM_BOT_TOKEN", telegramBotToken)
            .dependsOn(postgres);
    }

    @Bean
    GenericContainer<?> dataImporter(Network network, PostgreSQLContainer<?> postgres,
                                     KafkaContainer kafka, Boolean kafkaTopicsReady) {
        return springService("data-importer/data-importer", network)
            .withEnv("GOOGLE_APPLICATION_CREDENTIALS_JSON", googleCredentialsJson)
            .dependsOn(postgres, kafka);
    }

    @Bean
    GenericContainer<?> profileService(Network network, PostgreSQLContainer<?> postgres,
                                       KafkaContainer kafka, Boolean kafkaTopicsReady) {
        return springService("profile-service/profile-service", network)
            .dependsOn(postgres, kafka);
    }

    @Bean
    GenericContainer<?> projectService(Network network, PostgreSQLContainer<?> postgres,
                                       KafkaContainer kafka, Boolean kafkaTopicsReady) {
        return springService("project-service/project-service", network)
            .dependsOn(postgres, kafka);
    }

    @Bean
    GenericContainer<?> mentorService(Network network, PostgreSQLContainer<?> postgres,
                                      KafkaContainer kafka, Boolean kafkaTopicsReady) {
        return springService("mentor-service/mentor-service", network)
            .dependsOn(postgres, kafka);
    }

    @Bean
    GenericContainer<?> jobMarketAnalytics(Network network, PostgreSQLContainer<?> postgres,
                                           KafkaContainer kafka, Boolean kafkaTopicsReady) {
        return springService("job-market-analytics-service/job-market-analytics-service", network)
            .dependsOn(postgres, kafka);
    }

    private GenericContainer<?> springService(String imagePath, Network network) {
        String serviceName = imagePath.substring(imagePath.lastIndexOf('/') + 1);
        return new GenericContainer<>(GHCR + "/" + imagePath + ":" + TAG)
            .withNetwork(network)
            .withEnv("SPRING_PROFILES_ACTIVE", "local-stack")
            .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:19092")
            .withExposedPorts(8080)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(serviceName)))
            .waitingFor(
                Wait.forHttp("/actuator/health")
                    .forPort(8080)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5))
            );
    }
}
