package com.arc_e_tect.payments.system;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * What the service tells that a job has run: Kafka in Docker, connected through Spring Boot's
 * service connection, with the topic in place as it would be provisioned; and WireMock, which plays
 * the webhook's subscribers at the paths a feature states.
 */
@TestConfiguration(proxyBeanMethods = false)
public class EventInfrastructure {

    static final String SUBSCRIBED = "/hooks/validation-completed";
    static final String UNAVAILABLE = "/hooks/unavailable";

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.2.2"));
    }

    @Bean
    NewTopic validationCompletedTopic(@Value("${validation.execution.events.kafka.topic}") String topic) {
        return new NewTopic(topic, 1, (short) 1);
    }

    @Bean(destroyMethod = "stop")
    WireMockServer webhookSubscribers() {
        WireMockServer subscribers = new WireMockServer(options().dynamicPort());
        subscribers.start();
        return subscribers;
    }

    @Bean
    DynamicPropertyRegistrar webhookSubscriberUrls(WireMockServer subscribers) {
        return properties -> properties.add("validation.execution.events.webhook.subscribers",
                () -> subscribers.baseUrl() + SUBSCRIBED + "," + subscribers.baseUrl() + UNAVAILABLE);
    }
}
