package com.arc_e_tect.payments.contract;

import com.arc_e_tect.payments.contract.restdocs.ValidateNameIbanMatchContractTests;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The generated contract tests of POST /v1/validations/name-iban-match, against the running service.
 *
 * <p>The validation changes no state, so a success case needs none arranged. Two success cases
 * cannot pass against a service that keeps the contract: whether it answers {@code 200} or
 * {@code 202} depends on {@code Prefer: respond-async}, which the TranscriberJ cannot tie a
 * response to, so it derives each status for both requests. Those two are disabled, with the
 * reason; their counterparts, with the request that selects their status, run.
 */
@DisplayName("The contract of POST /v1/validations/name-iban-match")
class ValidateNameIbanMatchContractTest extends RunningService implements ValidateNameIbanMatchContractTests {

    private static final String SELECTED_BY_PREFER = "The status is selected by Prefer: respond-async, which the TranscriberJ cannot tie "
            + "a response to; this case sends the request that selects the other status. See the decision log.";

    @Override
    public void arrangeState(ContractCase contractCase) {
        switch (contractCase.id()) {
            case "success-200-required", "success-202-full" -> {
                // The validation changes no state: any service answers this request with this status.
            }
            default -> throw new IllegalArgumentException("No state to arrange for case " + contractCase.id());
        }
    }

    @Test
    @Override
    @Disabled(SELECTED_BY_PREFER)
    public void success200Full_returns200() {
        ValidateNameIbanMatchContractTests.super.success200Full_returns200();
    }

    @Test
    @Override
    @Disabled(SELECTED_BY_PREFER)
    public void success202Required_returns202() {
        ValidateNameIbanMatchContractTests.super.success202Required_returns202();
    }
}
