package com.arc_e_tect.validation.async.application.port.inbound.command;

import java.net.URI;
import java.util.Objects;

/** How the validation a job runs ended: with its result and the verdict it reached, or with the problem it answered. */
public sealed interface JobOutcome permits JobOutcome.Completed, JobOutcome.Failed {

    record Completed(Object result, ValidationOutcome outcome) implements JobOutcome {

        public Completed {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    record Failed(URI type, String title, int status, String detail) implements JobOutcome {
    }
}
