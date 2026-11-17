package com.arc_e_tect.validation.async.application.port.inbound;

import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;

/** Runs a validation as a job in the background, once per idempotency key. */
public interface SubmitValidationJobUseCase {

    SubmitValidationJobResult submit(SubmitValidationJobCommand command);
}
