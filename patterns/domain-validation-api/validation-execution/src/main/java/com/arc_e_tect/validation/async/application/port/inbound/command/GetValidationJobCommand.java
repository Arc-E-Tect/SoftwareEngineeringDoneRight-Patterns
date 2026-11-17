package com.arc_e_tect.validation.async.application.port.inbound.command;

/** Find the job of a validation with this identifier. */
public record GetValidationJobCommand(String validation, String validationId) {
}
