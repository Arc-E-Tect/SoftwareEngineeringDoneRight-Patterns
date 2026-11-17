package com.arc_e_tect.validation.async.adapter.outbound.memory;

import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationJobStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps validation jobs in memory, for as long as the service runs; it keeps every job, so it suits
 * an example or a test, not a service that runs for long.
 */
public class InMemoryValidationJobStore implements ValidationJobStore {

    private record Key(String validation, String idempotencyKey) {
    }

    private final Map<String, ValidationJob> jobs = new ConcurrentHashMap<>();
    private final Map<Key, String> byKey = new ConcurrentHashMap<>();

    @Override
    public ValidationJob claim(ValidationJob candidate) {
        String validationId = byKey.computeIfAbsent(new Key(candidate.validation(), candidate.idempotencyKey()), key -> {
            jobs.put(candidate.validationId(), candidate);
            return candidate.validationId();
        });
        return jobs.get(validationId);
    }

    @Override
    public void update(ValidationJob job) {
        jobs.put(job.validationId(), job);
    }

    @Override
    public Optional<ValidationJob> find(String validation, String validationId) {
        return Optional.ofNullable(jobs.get(validationId)).filter(job -> job.validation().equals(validation));
    }
}
