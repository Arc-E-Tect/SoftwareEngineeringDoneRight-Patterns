package com.arc_e_tect.payments.contract;

import com.arc_e_tect.payments.contract.restdocs.ValidateNameIbanMatchContractTests;
import org.junit.jupiter.api.DisplayName;

/**
 * The generated contract tests of POST /v1/validations/name-iban-match, against the running service.
 *
 * <p>The validation changes no state, so a success case needs none arranged. Whether the service
 * answers {@code 200} or {@code 202} is the request's to say, not the state's: the contract names
 * {@code Prefer: respond-async} as what selects {@code 202}, and the {@code Idempotency-Key} it
 * requires, so the {@code 202} case sends both, the {@code 200} cases leave {@code Prefer} out, and
 * an invalid request sends {@code Prefer} without the key.
 */
@DisplayName("The contract of POST /v1/validations/name-iban-match")
class ValidateNameIbanMatchContractTest extends RunningService implements ValidateNameIbanMatchContractTests {

    @Override
    public void arrangeState(ContractCase contractCase) {
        switch (contractCase.id()) {
            case "success-200-required", "success-200-full", "success-202-required" -> {
                // The validation changes no state: the request alone selects the status.
            }
            default -> throw new IllegalArgumentException("No state to arrange for case " + contractCase.id());
        }
    }
}
