package com.arc_e_tect.payments.system;

import com.arc_e_tect.payments.PaymentsApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * The service, started once on a random port with the accounts of the system profile, and with
 * Kafka and the webhook's subscribers to tell that a job has run.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = PaymentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("system")
@Import(EventInfrastructure.class)
public class CucumberSpringConfiguration {
}
