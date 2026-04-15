package org.example.e2etests;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.example.e2etests.containers.InfrastructureContainers;
import org.example.e2etests.containers.ServiceContainers;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.lifecycle.Startables;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class E2eTestBase {

    private static final List<String> REQUIRED_TOPICS = List.of(
        "projects.project.created"
    );

    @BeforeAll
    static void startContainers() throws Exception {
        Startables.deepStart(Stream.of(
            InfrastructureContainers.POSTGRES,
            InfrastructureContainers.KAFKA
        )).join();

        createKafkaTopics();

        Startables.deepStart(Stream.of(
            ServiceContainers.GATEWAY,
            ServiceContainers.AUTH_SERVICE,
            ServiceContainers.DATA_IMPORTER,
            ServiceContainers.PROFILE_SERVICE,
            ServiceContainers.PROJECT_SERVICE,
            ServiceContainers.MENTOR_SERVICE,
            ServiceContainers.JOB_MARKET_ANALYTICS
        )).join();
    }

    private static void createKafkaTopics() throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");

        try (AdminClient adminClient = AdminClient.create(props)) {
            List<NewTopic> topics = REQUIRED_TOPICS.stream()
                .map(name -> new NewTopic(name, 1, (short) 1))
                .toList();
            adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", InfrastructureContainers.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", InfrastructureContainers.POSTGRES::getUsername);
        registry.add("spring.datasource.password", InfrastructureContainers.POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:29092");
    }
}
