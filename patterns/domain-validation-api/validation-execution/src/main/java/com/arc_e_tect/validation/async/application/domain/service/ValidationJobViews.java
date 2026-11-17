package com.arc_e_tect.validation.async.application.domain.service;

import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;

/** Shows a job through the port, without its idempotency key or request. */
final class ValidationJobViews {

    private ValidationJobViews() {
    }

    static ValidationJobView of(ValidationJob job) {
        return new ValidationJobView(job.validationId(), job.validation(), job.location(),
                ValidationJobView.Status.valueOf(job.status().name()),
                job.result(),
                job.problem().map(problem -> new ValidationJobView.Problem(problem.type(), problem.title(), problem.status(), problem.detail())));
    }
}
