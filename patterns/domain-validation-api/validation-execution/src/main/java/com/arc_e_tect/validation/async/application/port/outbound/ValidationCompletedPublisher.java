package com.arc_e_tect.validation.async.application.port.outbound;

import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;

/**
 * Tells others that a validation job has run: subscribers to a webhook, a topic. Publishing is best
 * effort, and a job's outcome never depends on it: a publisher reports the failures it expects, such
 * as a subscriber that cannot be reached, and does not throw for them; anything it throws anyway is
 * logged by the service that publishes, and keeps no other publisher from publishing.
 */
public interface ValidationCompletedPublisher {

    void publish(ValidationCompleted completed);
}
