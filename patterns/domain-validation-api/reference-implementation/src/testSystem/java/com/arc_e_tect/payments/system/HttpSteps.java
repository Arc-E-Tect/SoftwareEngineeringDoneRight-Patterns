package com.arc_e_tect.payments.system;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sends requests to the running service over HTTP, and checks what it answers. */
public class HttpSteps {

    private static final String VALIDATIONS = "/payments/v1/validations/name-iban-match";

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    private HttpResponse<String> response;

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private void send(HttpRequest request) throws IOException, InterruptedException {
        response = http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @When("a payer validates the name {string} against the IBAN {string}")
    public void aPayerValidates(String name, String iban) throws IOException, InterruptedException {
        aClientSendsTheRequest("{\"iban\": \"%s\", \"accountHolderName\": \"%s\"}".formatted(iban, name));
    }

    @When("a client sends the request {}")
    public void aClientSendsTheRequest(String body) throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(uri(VALIDATIONS))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, application/problem+json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    @When("a client asks for the validation job {string}")
    public void aClientAsksForTheJob(String validationId) throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(uri(VALIDATIONS + "/" + validationId))
                .header("Accept", "application/json, application/problem+json")
                .GET()
                .build());
    }

    @Then("the answer is {int}")
    public void theAnswerIs(int status) {
        assertEquals(status, response.statusCode(), response.body());
    }

    @Then("the outcome is {string}")
    public void theOutcomeIs(String outcome) {
        assertEquals(outcome, JsonPath.read(response.body(), "$.outcome"));
    }

    @Then("the match is {string}")
    public void theMatchIs(String match) {
        assertEquals(match, JsonPath.read(response.body(), "$.details.match"));
    }

    @Then("the registered name is {string}")
    public void theRegisteredNameIs(String name) {
        assertEquals(name, JsonPath.read(response.body(), "$.details.registeredName"));
    }

    @Then("there is no registered name")
    public void thereIsNoRegisteredName() {
        Map<String, Object> details = JsonPath.read(response.body(), "$.details");
        assertFalse(details.containsKey("registeredName"));
    }

    @Then("there are no details")
    public void thereAreNoDetails() {
        Map<String, Object> result = JsonPath.read(response.body(), "$");
        assertFalse(result.containsKey("details"));
    }

    @Then("there is a finding {string}")
    public void thereIsAFinding(String rule) {
        assertEquals(List.of(rule), JsonPath.read(response.body(), "$.findings[*].rule"));
    }

    @Then("there is a finding {string} about {string}")
    public void thereIsAFindingAbout(String rule, String pointer) {
        assertEquals(List.of(Map.of("rule", rule, "pointer", pointer)),
                JsonPath.read(response.body(), "$.findings[*]['rule','pointer']"));
    }

    @Then("the answer is a problem of type {string}")
    public void theAnswerIsAProblemOfType(String type) {
        assertEquals(type, JsonPath.read(response.body(), "$.type"));
    }

    @Then("the problem names the country {string}")
    public void theProblemNamesTheCountry(String countryCode) {
        assertEquals(countryCode, JsonPath.read(response.body(), "$.countryCode"));
    }

    @Then("the answer is not stored")
    public void theAnswerIsNotStored() {
        assertTrue(response.headers().allValues("Cache-Control").contains("no-store"), response.headers().toString());
    }
}
