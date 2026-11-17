package com.arc_e_tect.validation.async.application.domain.model;

import java.net.URI;
import java.util.Objects;

/** Why a validation job failed, as the core of an RFC 9457 problem: its type, title, status and detail. */
public record JobProblem(URI type, String title, int status, String detail) {

    public JobProblem {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(detail, "detail");
    }
}
