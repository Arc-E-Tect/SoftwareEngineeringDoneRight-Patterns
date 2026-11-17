package com.arc_e_tect.validation.async.adapter.inbound.web;

/** The body of a 202 Accepted: the job the validation runs as, by the contract's shared ValidationJob. */
public record AcceptedValidationJob(String validationId, String validation, String status) {
}
