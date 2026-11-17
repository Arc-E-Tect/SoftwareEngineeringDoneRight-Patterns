package com.arc_e_tect.validation.async.application.domain.service;

import com.arc_e_tect.validation.async.application.port.inbound.GetValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.command.GetValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationJobStore;

import java.util.Objects;
import java.util.Optional;

/** Finds a validation job of a validation, for the job resource. */
public class GetValidationJobService implements GetValidationJobUseCase {

    private final ValidationJobStore store;

    public GetValidationJobService(ValidationJobStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    public Optional<ValidationJobView> find(GetValidationJobCommand command) {
        return store.find(command.validation(), command.validationId()).map(ValidationJobViews::of);
    }
}
