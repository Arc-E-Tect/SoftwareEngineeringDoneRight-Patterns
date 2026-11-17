package com.arc_e_tect.validation.async.application.port.inbound.command;

import java.net.URI;

/** How the validation a job runs ended: with its result, or with the problem it answered. */
public sealed interface JobOutcome permits JobOutcome.Completed, JobOutcome.Failed {

    record Completed(Object result) implements JobOutcome {
    }

    record Failed(URI type, String title, int status, String detail) implements JobOutcome {
    }
}
