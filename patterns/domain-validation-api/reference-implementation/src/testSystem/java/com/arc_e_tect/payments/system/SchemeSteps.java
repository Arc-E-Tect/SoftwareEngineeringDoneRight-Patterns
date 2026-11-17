package com.arc_e_tect.payments.system;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme.PayeeVerificationSchemeProperties;
import com.arc_e_tect.payments.validation.nameibanmatch.configuration.NameIbanMatchProperties;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that the service runs with the accounts and countries a feature states, so that what a
 * scenario relies on is written in the feature rather than hidden in a configuration file.
 */
public class SchemeSteps {

    @Autowired
    private PayeeVerificationSchemeProperties scheme;

    @Autowired
    private NameIbanMatchProperties validation;

    private String answers(PayeeVerificationSchemeProperties.Account account) {
        if (!account.available()) {
            return "not at all";
        }
        return account.responseTime().compareTo(scheme.timeout()) < 0 ? "in time" : "too late";
    }

    @Given("the payee verification scheme knows these accounts:")
    public void theSchemeKnowsTheseAccounts(DataTable accounts) {
        List<Map<String, String>> configured = scheme.accounts().stream()
                .map(account -> Map.of("iban", account.iban(), "registered name", account.registeredName(), "answers", answers(account)))
                .toList();
        assertEquals(accounts.asMaps(), configured);
    }

    @Given("the scheme covers accounts in {string}")
    public void theSchemeCoversAccountsIn(String countries) {
        assertEquals(String.join(", ", new TreeSet<>(Arrays.asList(countries.split(",\\s*")))),
                String.join(", ", new TreeSet<>(validation.supportedCountries())));
    }
}
