package com.arc_e_tect.validation.async.application.port.outbound;

import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;

/**
 * Tells others that a validation job has run: subscribers to a webhook, a topic. Publishing is best
 * effort; a publisher reports its own failures, and a job's outcome never depends on them.
 */
public interface ValidationCompletedPublisher {

    void publish(ValidationCompleted completed);
}
