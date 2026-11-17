package com.arc_e_tect.payments.validation.nameibanmatch.configuration;

import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.NameMatchRule;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.SupportedCountries;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.service.ValidateNameIbanMatchService;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.ValidateNameIbanMatchUseCase;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.outbound.AccountHolderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the core of the name-iban-match validation: the domain service behind its use case, with
 * the rule and the supported countries, over whichever adapter implements the outbound port.
 * The adapters are Spring components of their own, which the core does not know.
 */
@Configuration(proxyBeanMethods = false)
public class NameIbanMatchConfiguration {

    @Bean
    public ValidateNameIbanMatchUseCase validateNameIbanMatchUseCase(AccountHolderRegistry registry, NameIbanMatchProperties properties) {
        return new ValidateNameIbanMatchService(registry, new NameMatchRule(), new SupportedCountries(properties.supportedCountries()));
    }
}
