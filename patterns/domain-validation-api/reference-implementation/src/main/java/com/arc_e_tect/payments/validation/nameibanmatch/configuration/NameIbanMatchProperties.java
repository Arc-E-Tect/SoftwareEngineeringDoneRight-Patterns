package com.arc_e_tect.payments.validation.nameibanmatch.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;
import java.util.Set;

/** The settings of the name-iban-match validation: the countries whose accounts it can verify. */
@ConfigurationProperties("payments.validation.name-iban-match")
public record NameIbanMatchProperties(Set<String> supportedCountries) {

    public NameIbanMatchProperties {
        supportedCountries = Set.copyOf(Objects.requireNonNull(supportedCountries, "supportedCountries"));
    }
}
