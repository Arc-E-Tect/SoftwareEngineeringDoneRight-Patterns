package com.arc_e_tect.payments.system;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
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
    /** Where the job of the last validation answered with 202 is. */
    private String jobLocation;
    /** Where the job of the validation a scenario's Given submitted is. */
    private String submittedJobLocation;

    /** Where the job of the last validation answered with 202 is, for the steps about what the service tells of it. */
    String jobLocation() {
        return jobLocation;
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private void send(HttpRequest request) throws IOException, InterruptedException {
        response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 202) {
            jobLocation = response.headers().firstValue("Location").orElseThrow();
        }
    }

    private static String body(String name, String iban) {
        return "{\"iban\": \"%s\", \"accountHolderName\": \"%s\"}".formatted(iban, name);
    }

    private HttpRequest.Builder validation(String name, String iban) {
        return HttpRequest.newBuilder(uri(VALIDATIONS))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, application/problem+json")
                .POST(HttpRequest.BodyPublishers.ofString(body(name, iban)));
    }

    @When("a payer validates the name {string} against the IBAN {string}")
    public void aPayerValidates(String name, String iban) throws IOException, InterruptedException {
        aClientSendsTheRequest(body(name, iban));
    }

    @When("a payer validates the name {string} against the IBAN {string}, preferring an asynchronous answer with Idempotency-Key {string}")
    public void aPayerValidatesAsynchronously(String name, String iban, String key) throws IOException, InterruptedException {
        send(validation(name, iban).header("Prefer", "respond-async").header("Idempotency-Key", key).build());
    }

    @Given("a payer validated the name {string} against the IBAN {string}, preferring an asynchronous answer with Idempotency-Key {string}")
    public void aPayerValidatedAsynchronously(String name, String iban, String key) throws IOException, InterruptedException {
        aPayerValidatesAsynchronously(name, iban, key);
        submittedJobLocation = jobLocation;
    }

    @When("a payer validates the name {string} against the IBAN {string}, preferring an asynchronous answer without an Idempotency-Key")
    public void aPayerValidatesAsynchronouslyWithoutAKey(String name, String iban) throws IOException, InterruptedException {
        send(validation(name, iban).header("Prefer", "respond-async").build());
    }

    @When("the client reads the job")
    public void theClientReadsTheJob() throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(uri(jobLocation)).header("Accept", "application/json, application/problem+json").GET().build());
    }

    @When("the client reads the job until it has run")
    public void theClientReadsTheJobUntilItHasRun() throws IOException, InterruptedException {
        Instant giveUp = Instant.now().plus(Duration.ofSeconds(10));
        theClientReadsTheJob();
        while ("pending".equals(JsonPath.read(response.body(), "$.status")) && Instant.now().isBefore(giveUp)) {
            Thread.sleep(50);
            theClientReadsTheJob();
        }
    }

    @When("a client sends the request {}")
    public void aClientSendsTheRequest(String body) throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(uri(VALIDATIONS))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, application/problem+json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    @When("a client sends a request without a body")
    public void aClientSendsARequestWithoutABody() throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(uri(VALIDATIONS))
                .header("Accept", "application/json, application/problem+json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());
    }

    @When("a client sends a valid request as {string}")
    public void aClientSendsAValidRequestAs(String contentType) throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(uri(VALIDATIONS))
                .header("Content-Type", contentType)
                .header("Accept", "application/json, application/problem+json")
                .POST(HttpRequest.BodyPublishers.ofString(body("Jan de Vries", "NL91ABNA0417164300")))
                .build());
    }

    @When("a client sends a valid request, accepting only {string}")
    public void aClientSendsAValidRequestAcceptingOnly(String accept) throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(uri(VALIDATIONS))
                .header("Content-Type", "application/json")
                .header("Accept", accept)
                .POST(HttpRequest.BodyPublishers.ofString(body("Jan de Vries", "NL91ABNA0417164300")))
                .build());
    }

    @When("a client asks for the validation job {string}")
    public void aClientAsksForTheJob(String validationId) throws IOException, InterruptedException {
        aClientAsksForTheJobAcceptingOnly(validationId, "application/json, application/problem+json");
    }

    @When("a client asks for the validation job {string}, accepting only {string}")
    public void aClientAsksForTheJobAcceptingOnly(String validationId, String accept) throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(uri(VALIDATIONS + "/" + validationId))
                .header("Accept", accept)
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

    @Then("the problem says what is wrong at {string}")
    public void theProblemSaysWhatIsWrongAt(String context) {
        List<String> contexts = JsonPath.read(response.body(), "$.errors[*].context");
        assertTrue(contexts.contains(context), response.body());
    }

    @Then("the problem names the country {string}")
    public void theProblemNamesTheCountry(String countryCode) {
        assertEquals(countryCode, JsonPath.read(response.body(), "$.countryCode"));
    }

    @Then("the answer is a pending {string} job")
    public void theAnswerIsAPendingJob(String validation) {
        assertEquals(List.of(validation, "pending"), List.of(JsonPath.read(response.body(), "$.validation"), JsonPath.read(response.body(), "$.status")));
    }

    @Then("the answer says where the job is")
    public void theAnswerSaysWhereTheJobIs() {
        assertEquals(VALIDATIONS + "/" + JsonPath.read(response.body(), "$.validationId"), jobLocation);
    }

    @Then("the answer says when to read the job")
    public void theAnswerSaysWhenToReadTheJob() {
        assertTrue(Integer.parseInt(response.headers().firstValue("Retry-After").orElse("0")) >= 1, response.headers().toString());
    }

    @Then("the answer does not say when to read the job")
    public void theAnswerDoesNotSayWhenToReadTheJob() {
        assertFalse(response.headers().firstValue("Retry-After").isPresent(), response.headers().toString());
    }

    @Then("it is the same job")
    public void itIsTheSameJob() {
        assertEquals(submittedJobLocation, jobLocation);
    }

    @Then("the job is {string}")
    public void theJobIs(String status) {
        assertEquals(status, JsonPath.read(response.body(), "$.status"));
    }

    @Then("the job's result has outcome {string}")
    public void theJobsResultHasOutcome(String outcome) {
        assertEquals(outcome, JsonPath.read(response.body(), "$.result.outcome"));
    }

    @Then("the job's problem is of type {string}")
    public void theJobsProblemIsOfType(String type) {
        assertEquals(type, JsonPath.read(response.body(), "$.problem.type"));
    }

    @Then("the answer is not stored")
    public void theAnswerIsNotStored() {
        assertTrue(response.headers().allValues("Cache-Control").contains("no-store"), response.headers().toString());
    }
}
