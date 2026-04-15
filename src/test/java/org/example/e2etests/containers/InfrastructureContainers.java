package org.example.e2etests.containers;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

@SuppressWarnings("resource")
public final class InfrastructureContainers {

    public static final Network NETWORK = Network.newNetwork();

    public static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:18.0-alpine")
            .withNetwork(NETWORK)
            .withNetworkAliases("database")
            .withDatabaseName("it_mentor_community_platform")
            .withUsername("root")
            .withPassword("password")
            .withInitScript("init.sql");

    @SuppressWarnings("deprecation")
    public static final FixedHostPortGenericContainer<?> KAFKA =
        new FixedHostPortGenericContainer<>("apache/kafka:3.7.0")
            .withNetwork(NETWORK)
            .withNetworkAliases("kafka")
            .withFixedExposedPort(29092, 29092)
            .withEnv("KAFKA_NODE_ID", "1")
            .withEnv("KAFKA_PROCESS_ROLES", "broker,controller")
            .withEnv("KAFKA_LISTENERS",
                "PLAINTEXT_INTERNAL://0.0.0.0:9092," +
                "PLAINTEXT_EXTERNAL://0.0.0.0:29092," +
                "CONTROLLER://0.0.0.0:9093")
            .withEnv("KAFKA_ADVERTISED_LISTENERS",
                "PLAINTEXT_INTERNAL://kafka:9092," +
                "PLAINTEXT_EXTERNAL://localhost:29092")
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
                "PLAINTEXT_INTERNAL:PLAINTEXT,PLAINTEXT_EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT")
            .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT_INTERNAL")
            .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
            .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@kafka:9093")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_CLUSTER_ID", "YWJhY2RlZmdoaWprbG1u")
            .waitingFor(Wait.forLogMessage(".*Kafka Server started.*", 1)
                .withStartupTimeout(Duration.ofMinutes(2)));

    private InfrastructureContainers() {}
}
