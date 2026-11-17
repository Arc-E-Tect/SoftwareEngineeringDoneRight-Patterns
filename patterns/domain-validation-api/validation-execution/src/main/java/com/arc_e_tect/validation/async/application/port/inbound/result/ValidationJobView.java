package com.arc_e_tect.validation.async.application.port.inbound.result;

import java.net.URI;
import java.util.Optional;

/** A validation job as the port shows it: where it is, and its result or its problem once it has run. */
public record ValidationJobView(String validationId, String validation, Status status, Optional<Object> result, Optional<Problem> problem) {

    public enum Status { PENDING, COMPLETED, FAILED }

    public record Problem(URI type, String title, int status, String detail) {
    }
}
