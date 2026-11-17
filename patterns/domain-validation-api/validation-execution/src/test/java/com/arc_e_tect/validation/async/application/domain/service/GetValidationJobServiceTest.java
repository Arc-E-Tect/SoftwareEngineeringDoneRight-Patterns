package com.arc_e_tect.validation.async.application.domain.service;

import com.arc_e_tect.validation.async.adapter.outbound.memory.InMemoryValidationJobStore;
import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import com.arc_e_tect.validation.async.application.port.inbound.command.GetValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The service that finds validation jobs")
class GetValidationJobServiceTest {

    private static final JobProblem PROBLEM = new JobProblem(URI.create("https://api.example.com/problems/internal-server-error"),
            "Internal Server Error", 500, "An unexpected error occurred while running the validation.");

    private final InMemoryValidationJobStore store = new InMemoryValidationJobStore();
    private final GetValidationJobService service = new GetValidationJobService(store);
    private final ValidationJob pending = store.claim(ValidationJob.pending("name-iban-match", "key", "request"));

    private Optional<ValidationJobView> find(String validation, String validationId) {
        return service.find(new GetValidationJobCommand(validation, validationId));
    }

    @Test
    @DisplayName("finds a pending job, without a result or a problem")
    void findsAPendingJob() {
        assertEquals(Optional.of(new ValidationJobView(pending.validationId(), "name-iban-match",
                ValidationJobView.Status.PENDING, Optional.empty(), Optional.empty())), find("name-iban-match", pending.validationId()));
    }

    @Test
    @DisplayName("finds a completed job with its result")
    void findsACompletedJob() {
        store.update(pending.completed("result"));

        assertEquals(Optional.of("result"), find("name-iban-match", pending.validationId()).orElseThrow().result());
    }

    @Test
    @DisplayName("finds a failed job with its problem")
    void findsAFailedJob() {
        store.update(pending.failed(PROBLEM));

        assertEquals(Optional.of(new ValidationJobView.Problem(PROBLEM.type(), PROBLEM.title(), PROBLEM.status(), PROBLEM.detail())),
                find("name-iban-match", pending.validationId()).orElseThrow().problem());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ValidationJob.Status.class)
    @DisplayName("finds a job with the same status")
    void findsAJobWithTheSameStatus(ValidationJob.Status status) {
        store.update(switch (status) {
            case PENDING -> pending;
            case COMPLETED -> pending.completed("result");
            case FAILED -> pending.failed(PROBLEM);
        });

        assertEquals(status.name(), find("name-iban-match", pending.validationId()).orElseThrow().status().name());
    }

    @Test
    @DisplayName("does not find a job of another validation")
    void doesNotFindAJobOfAnotherValidation() {
        assertEquals(Optional.empty(), find("entry-eligibility", pending.validationId()));
    }

    @Test
    @DisplayName("is refused without a store")
    void isRefusedWithoutAStore() {
        assertThrows(NullPointerException.class, () -> new GetValidationJobService(null));
    }
}
