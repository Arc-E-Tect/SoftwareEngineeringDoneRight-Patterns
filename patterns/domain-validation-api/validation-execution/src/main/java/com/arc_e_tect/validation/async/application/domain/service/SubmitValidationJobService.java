package com.arc_e_tect.validation.async.application.domain.service;

import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.domain.model.Outcome;
import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;
import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import com.arc_e_tect.validation.async.application.port.inbound.SubmitValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationCompletedPublisher;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Submits a validation as a job, idempotently: the first submission with a key stores a pending job
 * and runs it in the background; a later one with that key and an equal request gets the same job,
 * and one with a different request is refused. Whichever adapter submits, that holds.
 *
 * <p>Once a job has run and is stored as it ended, its completion is published to every publisher,
 * so that whoever is told finds the job as it ended.
 *
 * <p>What fails unexpectedly is logged, once, with the job's identifier and never the request or the
 * result: a validation that throws, which fails its job, and a publisher that throws, which the
 * other publishers and the job are kept from.
 */
public class SubmitValidationJobService implements SubmitValidationJobUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SubmitValidationJobService.class);

    private final ValidationJobStore store;
    private final Executor executor;
    private final Clock clock;
    private final List<ValidationCompletedPublisher> publishers;
    private final JobProblem unexpectedFailure;

    public SubmitValidationJobService(ValidationJobStore store, Executor executor, Clock clock,
                                      List<ValidationCompletedPublisher> publishers, JobProblem unexpectedFailure) {
        this.store = Objects.requireNonNull(store, "store");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.publishers = List.copyOf(Objects.requireNonNull(publishers, "publishers"));
        this.unexpectedFailure = Objects.requireNonNull(unexpectedFailure, "unexpectedFailure");
    }

    @Override
    public SubmitValidationJobResult submit(SubmitValidationJobCommand command) {
        ValidationJob candidate = ValidationJob.pending(command.validation(), command.idempotencyKey(), command.request(),
                command.operationPath());
        ValidationJob job = store.claim(candidate);
        if (!job.validationId().equals(candidate.validationId())) {
            return job.request().equals(command.request())
                    ? new SubmitValidationJobResult.Accepted(ValidationJobViews.of(job))
                    : new SubmitValidationJobResult.IdempotencyKeyReused(command.idempotencyKey());
        }
        executor.execute(() -> run(job, command.work()));
        return new SubmitValidationJobResult.Accepted(ValidationJobViews.of(job));
    }

    private void run(ValidationJob job, Supplier<JobOutcome> work) {
        JobOutcome ended = outcomeOf(job, work);
        ValidationJob ran = switch (ended) {
            case JobOutcome.Completed completed -> job.completed(completed.result());
            case JobOutcome.Failed failed -> job.failed(new JobProblem(failed.type(), failed.title(), failed.status(), failed.detail()));
        };
        store.update(ran);
        Optional<Outcome> outcome = ended instanceof JobOutcome.Completed completed
                ? Optional.of(Outcome.valueOf(completed.outcome().name()))
                : Optional.empty();
        publish(ValidationCompleted.of(ran, outcome, clock.instant()));
    }

    private JobOutcome outcomeOf(ValidationJob job, Supplier<JobOutcome> work) {
        try {
            return work.get();
        } catch (RuntimeException unexpected) {
            LOG.error("The {} validation of job {} failed unexpectedly; the job has failed", job.validation(), job.validationId(), unexpected);
            return new JobOutcome.Failed(unexpectedFailure.type(), unexpectedFailure.title(), unexpectedFailure.status(),
                    unexpectedFailure.detail());
        }
    }

    /** Publishing is best effort: a publisher that fails anyway keeps neither the job nor the other publishers from theirs. */
    private void publish(ValidationCompleted completed) {
        for (ValidationCompletedPublisher publisher : publishers) {
            try {
                publisher.publish(completed);
            } catch (RuntimeException unexpected) {
                // A publisher reports the failures it expects; this is one it did not, so it is logged here.
                LOG.warn("Publisher {} failed to publish the completion of job {}", publisher.getClass().getName(),
                        completed.validationId(), unexpected);
            }
        }
    }
}
