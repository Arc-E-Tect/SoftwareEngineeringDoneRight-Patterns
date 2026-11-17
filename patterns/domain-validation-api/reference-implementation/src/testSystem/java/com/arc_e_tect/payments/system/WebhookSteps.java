package com.arc_e_tect.payments.system;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Plays the subscribers of the validationCompleted webhook, and checks what each of them is told. */
public class WebhookSteps {

    @Autowired
    private WireMockServer subscribers;

    @Autowired
    private Environment environment;

    @Autowired
    private HttpSteps http;

    /** What the subscriber of the last check was told. */
    private String told;

    @Given("the validationCompleted webhook has these subscribers:")
    public void theWebhookHasTheseSubscribers(DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        rows.forEach(row -> subscribers.stubFor(post(urlEqualTo(row.get("path")))
                .willReturn(aResponse().withStatus(Integer.parseInt(row.get("answers"))))));
        assertEquals(rows.stream().map(row -> URI.create(subscribers.baseUrl() + row.get("path"))).toList(),
                Binder.get(environment).bind("validation.execution.events.webhook.subscribers", Bindable.listOf(URI.class)).get());
    }

    @Then("{string} is told that the job is {string} with outcome {string}")
    public void isToldWithOutcome(String path, String status, String outcome) {
        told = toldAbout(path);
        assertEquals(List.of(status, outcome), List.of(JsonPath.read(told, "$.status"), JsonPath.read(told, "$.outcome")), told);
    }

    @Then("{string} is told that the job is {string} without an outcome")
    public void isToldWithoutOutcome(String path, String status) {
        told = toldAbout(path);
        Map<String, Object> event = JsonPath.read(told, "$");
        assertEquals(status, event.get("status"), told);
        assertFalse(event.containsKey("outcome"), told);
    }

    @Then("the subscriber is told where the job is")
    public void theSubscriberIsToldWhereTheJobIs() {
        assertEquals(http.jobLocation(), JsonPath.read(told, "$.location"));
    }

    /** What the subscriber at the path is told about the job, once it has run; delivery is asynchronous. */
    private String toldAbout(String path) {
        String validationId = http.jobLocation().substring(http.jobLocation().lastIndexOf('/') + 1);
        Instant giveUp = Instant.now().plus(Duration.ofSeconds(10));
        Optional<String> body = delivered(path, validationId);
        while (body.isEmpty() && Instant.now().isBefore(giveUp)) {
            sleep();
            body = delivered(path, validationId);
        }
        return body.orElseThrow(() -> new AssertionError(path + " was not told about job " + validationId));
    }

    private Optional<String> delivered(String path, String validationId) {
        return subscribers.findAll(postRequestedFor(urlEqualTo(path))).stream()
                .map(LoggedRequest::getBodyAsString)
                .filter(body -> validationId.equals(JsonPath.read(body, "$.validationId")))
                .findFirst();
    }

    private static void sleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }
}
