package com.arc_e_tect.payments.system;

import com.arc_e_tect.payments.PaymentsApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** The service, started once on a random port with the accounts of the system profile. */
@CucumberContextConfiguration
@SpringBootTest(classes = PaymentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("system")
public class CucumberSpringConfiguration {
}
