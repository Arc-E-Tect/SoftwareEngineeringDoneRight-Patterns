package com.arc_e_tect.payments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

@DisplayName("The payments service")
class PaymentsApplicationTest {

    @Test
    @DisplayName("starts as a Spring Boot application with its arguments")
    void startsAsASpringBootApplication() {
        String[] arguments = {"--server.port=0"};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            PaymentsApplication.main(arguments);

            spring.verify(() -> SpringApplication.run(PaymentsApplication.class, arguments));
        }
    }

    @Test
    @DisplayName("can be created, as Spring does with its configuration")
    void canBeCreated() {
        assertNotNull(new PaymentsApplication());
    }
}
