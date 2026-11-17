package com.arc_e_tect.validation.async.application.domain.service;

import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import com.arc_e_tect.validation.async.application.port.inbound.SubmitValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationJobStore;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Submits a validation as a job, idempotently: the first submission with a key stores a pending job
 * and runs it in the background; a later one with that key and an equal request gets the same job,
 * and one with a different request is refused. Whichever adapter submits, that holds.
 */
public class SubmitValidationJobService implements SubmitValidationJobUseCase {

    private final ValidationJobStore store;
    private final Executor executor;
    private final JobProblem unexpectedFailure;

    public SubmitValidationJobService(ValidationJobStore store, Executor executor, JobProblem unexpectedFailure) {
        this.store = Objects.requireNonNull(store, "store");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.unexpectedFailure = Objects.requireNonNull(unexpectedFailure, "unexpectedFailure");
    }

    @Override
    public SubmitValidationJobResult submit(SubmitValidationJobCommand command) {
        ValidationJob candidate = ValidationJob.pending(command.validation(), command.idempotencyKey(), command.request());
        ValidationJob job = store.claim(candidate);
        if (!job.validationId().equals(candidate.validationId())) {
            return job.request().equals(command.request())
                    ? new SubmitValidationJobResult.Accepted(ValidationJobViews.of(job))
                    : new SubmitValidationJobResult.IdempotencyKeyReused(command.idempotencyKey());
        }
        executor.execute(() -> store.update(run(job, command.work())));
        return new SubmitValidationJobResult.Accepted(ValidationJobViews.of(job));
    }

    private ValidationJob run(ValidationJob job, Supplier<JobOutcome> work) {
        try {
            return switch (work.get()) {
                case JobOutcome.Completed completed -> job.completed(completed.result());
                case JobOutcome.Failed failed -> job.failed(new JobProblem(failed.type(), failed.title(), failed.status(), failed.detail()));
            };
        } catch (RuntimeException unexpected) {
            return job.failed(unexpectedFailure);
        }
    }
}
