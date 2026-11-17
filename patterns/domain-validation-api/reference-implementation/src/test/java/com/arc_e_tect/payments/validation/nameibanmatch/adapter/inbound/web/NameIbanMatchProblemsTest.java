package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The problems of the name-iban-match validation")
class NameIbanMatchProblemsTest {

    private static ProblemDetail problem(int status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        return problem;
    }

    @Nested
    @DisplayName("for an unsupported country")
    class UnsupportedCountry {

        private final ErrorResponseException exception = NameIbanMatchProblems.unsupportedCountry("BR");

        @Test
        @DisplayName("is the contract's unsupported-country problem, with the country code")
        void isTheUnsupportedCountryProblem() {
            ProblemDetail expected = problem(422, "https://api.example.com/payments/problems/unsupported-country",
                    "Unsupported Country", "Accounts in BR cannot be verified.");
            expected.setProperty("countryCode", "BR");

            assertEquals(expected, exception.getBody());
        }

        @Test
        @DisplayName("has status 422")
        void hasStatus422() {
            assertEquals(HttpStatusCode.valueOf(422), exception.getStatusCode());
        }

        @Test
        @DisplayName("is not stored")
        void isNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), exception.getHeaders().getCacheControl());
        }
    }

    @Nested
    @DisplayName("for an IBAN with wrong check digits")
    class InvalidChecksum {

        private final ErrorResponseException exception = NameIbanMatchProblems.invalidChecksum();

        @Test
        @DisplayName("is the contract's invalid-iban-checksum problem")
        void isTheInvalidChecksumProblem() {
            assertEquals(problem(422, "https://api.example.com/payments/problems/invalid-iban-checksum",
                    "Invalid IBAN Checksum", "The check digits of the IBAN are wrong."), exception.getBody());
        }

        @Test
        @DisplayName("has status 422")
        void hasStatus422() {
            assertEquals(HttpStatusCode.valueOf(422), exception.getStatusCode());
        }

        @Test
        @DisplayName("is not stored")
        void isNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), exception.getHeaders().getCacheControl());
        }
    }

    @Nested
    @DisplayName("for a request the use case refuses")
    class InvalidRequest {

        private final ErrorResponseException exception = NameIbanMatchProblems.invalidRequest("A payee claim needs the name of the account holder");

        @Test
        @DisplayName("is the contract's invalid-request problem, with the reason")
        void isTheInvalidRequestProblem() {
            assertEquals(problem(400, "https://api.example.com/problems/invalid-request",
                    "Invalid Request", "A payee claim needs the name of the account holder"), exception.getBody());
        }

        @Test
        @DisplayName("has status 400")
        void hasStatus400() {
            assertEquals(HttpStatusCode.valueOf(400), exception.getStatusCode());
        }

        @Test
        @DisplayName("is not stored")
        void isNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), exception.getHeaders().getCacheControl());
        }
    }

    @Nested
    @DisplayName("for a validation job that does not exist")
    class JobNotFound {

        private final ErrorResponseException exception =
                NameIbanMatchProblems.jobNotFound("8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f");

        @Test
        @DisplayName("is the contract's validation-job-not-found problem, naming the identifier")
        void isTheJobNotFoundProblem() {
            assertEquals(problem(404, "https://api.example.com/problems/validation-job-not-found", "Validation Job Not Found",
                            "No name-iban-match validation job has the identifier 8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f."),
                    exception.getBody());
        }

        @Test
        @DisplayName("has status 404")
        void hasStatus404() {
            assertEquals(HttpStatusCode.valueOf(404), exception.getStatusCode());
        }

        @Test
        @DisplayName("is not stored")
        void isNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), exception.getHeaders().getCacheControl());
        }
    }
}
