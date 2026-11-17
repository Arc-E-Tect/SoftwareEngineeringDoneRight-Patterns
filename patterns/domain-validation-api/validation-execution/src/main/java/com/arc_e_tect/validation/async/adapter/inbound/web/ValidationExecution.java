package com.arc_e_tect.validation.async.adapter.inbound.web;

import com.arc_e_tect.validation.async.application.port.inbound.GetValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.SubmitValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.command.GetValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The HTTP part of the asynchronous execution, which a validation's controller hands its work to.
 *
 * <p>Without {@code Prefer: respond-async} it runs the validation and answers {@code 200} with its
 * result. With it, it submits the validation as a job, under the request's {@code Idempotency-Key},
 * and answers {@code 202} with the job, its {@code Location} and a {@code Retry-After}. For the job
 * resource it finds the job and answers it as the validation maps it. Every answer is
 * {@code Cache-Control: no-store}.
 */
public class ValidationExecution {

    private static final String RESPOND_ASYNC = "respond-async";

    private final SubmitValidationJobUseCase submit;
    private final GetValidationJobUseCase get;
    private final ValidationExecutionProblems problems;
    private final String retryAfter;

    public ValidationExecution(SubmitValidationJobUseCase submit, GetValidationJobUseCase get,
                               ValidationExecutionProblems problems, Duration retryAfter) {
        this.submit = submit;
        this.get = get;
        this.problems = problems;
        this.retryAfter = Long.toString(retryAfter.toSeconds());
    }

    /**
     * Runs a validation, at once or as a job.
     *
     * @param validation     the validation's name
     * @param prefer         the request's {@code Prefer} header, if any
     * @param idempotencyKey the request's {@code Idempotency-Key} header, if any
     * @param request        what the validation runs on, with value equality: the use case's command
     * @param validate       the validation: its result, or an {@link ErrorResponseException} with the problem
     */
    public <R> ResponseEntity<R> run(String validation, String prefer, String idempotencyKey, Object request, Supplier<R> validate) {
        if (!prefersAnAsynchronousAnswer(prefer)) {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(validate.get());
        }
        if (idempotencyKey == null) {
            throw problems.missingIdempotencyKey();
        }
        return switch (submit.submit(new SubmitValidationJobCommand(validation, idempotencyKey, request, () -> outcome(validate)))) {
            case SubmitValidationJobResult.Accepted accepted -> accepted(accepted.job());
            case SubmitValidationJobResult.IdempotencyKeyReused reused -> throw problems.idempotencyKeyReused(reused.idempotencyKey());
        };
    }

    /** Answers the job of the validation with this identifier, as the validation maps it, or the problem that there is none. */
    public <J> ResponseEntity<J> job(String validation, String validationId, Function<ValidationJobView, J> mapping) {
        ValidationJobView job = get.find(new GetValidationJobCommand(validation, validationId))
                .orElseThrow(() -> problems.jobNotFound(validation, validationId));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (job.status() == ValidationJobView.Status.PENDING) {
            response.header(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return response.body(mapping.apply(job));
    }

    /** Whether one of the preferences, separated by commas and each perhaps with parameters, is respond-async (RFC 7240). */
    private static boolean prefersAnAsynchronousAnswer(String prefer) {
        return prefer != null && Arrays.stream(prefer.split(","))
                .map(preference -> preference.split("[;=]", 2)[0].strip().toLowerCase(Locale.ROOT))
                .anyMatch(RESPOND_ASYNC::equals);
    }

    private static JobOutcome outcome(Supplier<?> validate) {
        try {
            return new JobOutcome.Completed(validate.get());
        } catch (ErrorResponseException refused) {
            ProblemDetail problem = refused.getBody();
            return new JobOutcome.Failed(problem.getType(), problem.getTitle(), problem.getStatus(), problem.getDetail());
        }
    }

    /**
     * The 202 has the job as its body, while the operation's generated signature is typed by its 200;
     * Spring writes whatever body the response holds, so the response is cast here, in this one place.
     */
    @SuppressWarnings("unchecked")
    private <R> ResponseEntity<R> accepted(ValidationJobView job) {
        ResponseEntity<?> response = ResponseEntity.accepted()
                .location(location(job))
                .header(HttpHeaders.RETRY_AFTER, retryAfter)
                .cacheControl(CacheControl.noStore())
                .body(new AcceptedValidationJob(job.validationId(), job.validation(), job.status().name().toLowerCase(Locale.ROOT)));
        return (ResponseEntity<R>) response;
    }

    /** The job resource is the validation's operation path with the job's identifier: the request's path, then the identifier. */
    private static URI location(ValidationJobView job) {
        String path = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getRequestURI();
        return URI.create(path + "/" + job.validationId());
    }
}
