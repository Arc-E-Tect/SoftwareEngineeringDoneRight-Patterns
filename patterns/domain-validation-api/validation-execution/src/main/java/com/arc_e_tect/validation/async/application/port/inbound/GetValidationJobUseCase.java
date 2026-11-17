package com.arc_e_tect.validation.async.application.port.inbound;

import com.arc_e_tect.validation.async.application.port.inbound.command.GetValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;

import java.util.Optional;

/** Finds a validation job, for the job resource. */
public interface GetValidationJobUseCase {

    Optional<ValidationJobView> find(GetValidationJobCommand command);
}
