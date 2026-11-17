package com.arc_e_tect.payments.system;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Reads the topic the service publishes to, and checks the event about a scenario's job. */
public class EventSteps {

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private HttpSteps http;

    /** The event of the last check. */
    private ConsumerRecord<String, String> event;

    @Then("an event on {string} says that the job is {string} with outcome {string}")
    public void anEventSaysWithOutcome(String topic, String status, String outcome) {
        event = eventAbout(topic);
        assertEquals(List.of(status, outcome), List.of(JsonPath.read(event.value(), "$.status"), JsonPath.read(event.value(), "$.outcome")),
                event.value());
    }

    @Then("an event on {string} says that the job is {string} without an outcome")
    public void anEventSaysWithoutOutcome(String topic, String status) {
        event = eventAbout(topic);
        Map<String, Object> published = JsonPath.read(event.value(), "$");
        assertEquals(status, published.get("status"), event.value());
        assertFalse(published.containsKey("outcome"), event.value());
    }

    @Then("the event is keyed by the job's identifier")
    public void theEventIsKeyedByTheJobsIdentifier() {
        assertEquals(validationId(), event.key());
    }

    @Then("the event says where the job is")
    public void theEventSaysWhereTheJobIs() {
        assertEquals(http.jobLocation(), JsonPath.read(event.value(), "$.location"));
    }

    private String validationId() {
        return http.jobLocation().substring(http.jobLocation().lastIndexOf('/') + 1);
    }

    /** The event on the topic about the scenario's job, read from the start; publication is asynchronous. */
    private ConsumerRecord<String, String> eventAbout(String topic) {
        Map<String, Object> settings = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(settings)) {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(partition -> new TopicPartition(topic, partition.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            Instant giveUp = Instant.now().plus(Duration.ofSeconds(10));
            while (Instant.now().isBefore(giveUp)) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                    if (validationId().equals(JsonPath.read(record.value(), "$.validationId"))) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("No event on " + topic + " about job " + validationId());
    }
}
