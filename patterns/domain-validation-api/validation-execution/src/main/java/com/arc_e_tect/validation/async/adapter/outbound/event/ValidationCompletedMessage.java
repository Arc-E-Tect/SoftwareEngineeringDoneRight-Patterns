package com.arc_e_tect.validation.async.adapter.outbound.event;

import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;

/**
 * The completion of a validation job as the contract's {@code ValidationCompletedEventV1}, which the
 * webhook and the topic share: a reference to the job, with the outcome only when it completed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ValidationCompletedMessage(String validationId, String validation, String status, String outcome, String completedAt,
                                  String location) {

    static ValidationCompletedMessage of(ValidationCompleted completed) {
        return new ValidationCompletedMessage(completed.validationId(), completed.validation(), named(completed.status()),
                completed.outcome().map(ValidationCompletedMessage::named).orElse(null), completed.completedAt().toString(),
                completed.location());
    }

    static String json(JsonMapper json, ValidationCompleted completed) {
        return json.writeValueAsString(of(completed));
    }

    private static String named(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
