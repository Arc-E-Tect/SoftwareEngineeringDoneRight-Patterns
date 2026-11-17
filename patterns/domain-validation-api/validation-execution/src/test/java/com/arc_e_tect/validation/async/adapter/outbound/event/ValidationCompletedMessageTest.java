package com.arc_e_tect.validation.async.adapter.outbound.event;

import com.arc_e_tect.validation.async.application.domain.model.Outcome;
import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;
import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The message of a validation's completion")
class ValidationCompletedMessageTest {

    static final String ID = "8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f";
    static final String LOCATION = "/payments/v1/validations/name-iban-match/" + ID;
    static final Instant AT = Instant.parse("2026-11-17T10:35:02Z");
    static final ValidationCompleted COMPLETED = new ValidationCompleted(ID, "name-iban-match", ValidationJob.Status.COMPLETED,
            Optional.of(Outcome.VALID), AT, LOCATION);
    static final String COMPLETED_JSON = "{\"validationId\":\"" + ID + "\",\"validation\":\"name-iban-match\",\"status\":\"completed\","
            + "\"outcome\":\"valid\",\"completedAt\":\"2026-11-17T10:35:02Z\",\"location\":\"" + LOCATION + "\"}";

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("of a completed job has the contract's shape, with the outcome")
    void ofACompletedJob() {
        assertEquals(COMPLETED_JSON, ValidationCompletedMessage.json(json, COMPLETED));
    }

    @Test
    @DisplayName("of a failed job has no outcome")
    void ofAFailedJob() {
        ValidationCompleted failed = new ValidationCompleted(ID, "name-iban-match", ValidationJob.Status.FAILED, Optional.empty(), AT, LOCATION);

        assertEquals("{\"validationId\":\"" + ID + "\",\"validation\":\"name-iban-match\",\"status\":\"failed\","
                + "\"completedAt\":\"2026-11-17T10:35:02Z\",\"location\":\"" + LOCATION + "\"}", ValidationCompletedMessage.json(json, failed));
    }

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource({"VALID, valid", "INVALID, invalid", "INDETERMINATE, indeterminate"})
    @DisplayName("names the outcome as the contract does")
    void namesTheOutcome(Outcome outcome, String named) {
        ValidationCompleted completed = new ValidationCompleted(ID, "name-iban-match", ValidationJob.Status.COMPLETED,
                Optional.of(outcome), AT, LOCATION);

        assertEquals(named, ValidationCompletedMessage.of(completed).outcome());
    }

    @Test
    @DisplayName("has the moment the job completed as an RFC 3339 timestamp, to the fraction of a second")
    void hasTheMomentAsATimestamp() {
        ValidationCompleted completed = new ValidationCompleted(ID, "name-iban-match", ValidationJob.Status.FAILED, Optional.empty(),
                Instant.parse("2026-11-17T10:35:02.125Z"), LOCATION);

        assertEquals("2026-11-17T10:35:02.125Z", ValidationCompletedMessage.of(completed).completedAt());
    }
}
