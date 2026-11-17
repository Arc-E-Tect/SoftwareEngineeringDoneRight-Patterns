package com.arc_e_tect.validation.async.adapter.outbound.memory;

import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The in-memory validation job store")
class InMemoryValidationJobStoreTest {

    private final InMemoryValidationJobStore store = new InMemoryValidationJobStore();
    private final ValidationJob job = ValidationJob.pending("name-iban-match", "key", "request");

    @Test
    @DisplayName("claims a job for a key it does not hold")
    void claimsAJobForANewKey() {
        assertEquals(job, store.claim(job));
    }

    @Test
    @DisplayName("finds a job it claimed")
    void findsAClaimedJob() {
        store.claim(job);

        assertEquals(Optional.of(job), store.find("name-iban-match", job.validationId()));
    }

    @Test
    @DisplayName("answers the job it holds for a key it holds")
    void answersTheJobItHoldsForAKey() {
        store.claim(job);

        assertEquals(job, store.claim(ValidationJob.pending("name-iban-match", "key", "another request")));
    }

    @Test
    @DisplayName("does not store a second job for a key it holds")
    void doesNotStoreASecondJob() {
        store.claim(job);
        ValidationJob second = ValidationJob.pending("name-iban-match", "key", "another request");
        store.claim(second);

        assertEquals(Optional.empty(), store.find("name-iban-match", second.validationId()));
    }

    @Test
    @DisplayName("keeps the keys of different validations apart")
    void keepsTheKeysOfValidationsApart() {
        store.claim(job);
        ValidationJob other = ValidationJob.pending("entry-eligibility", "key", "request");

        assertEquals(other, store.claim(other));
    }

    @Test
    @DisplayName("holds the job as it was updated")
    void holdsTheUpdatedJob() {
        store.claim(job);
        store.update(job.completed("result"));

        assertEquals(Optional.of(job.completed("result")), store.find("name-iban-match", job.validationId()));
    }

    @Test
    @DisplayName("does not find a job of another validation")
    void doesNotFindAJobOfAnotherValidation() {
        store.claim(job);

        assertEquals(Optional.empty(), store.find("entry-eligibility", job.validationId()));
    }

    @Test
    @DisplayName("does not find a job it does not hold")
    void doesNotFindAnUnknownJob() {
        assertEquals(Optional.empty(), store.find("name-iban-match", "8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f"));
    }
}
