package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

/**
 * The problems the name-iban-match validation answers with, as RFC 9457 Problem Details with the
 * type, title and status the contract fixes for each. Spring renders them as
 * {@code application/problem+json}.
 */
public final class NameIbanMatchProblems {

    private static final URI UNSUPPORTED_COUNTRY = URI.create("https://api.example.com/payments/problems/unsupported-country");
    private static final URI INVALID_IBAN_CHECKSUM = URI.create("https://api.example.com/payments/problems/invalid-iban-checksum");
    private static final URI INVALID_REQUEST = URI.create("https://api.example.com/problems/invalid-request");

    private NameIbanMatchProblems() {
    }

    /** The IBAN is well-formed, but the payee verification scheme does not cover its country. */
    public static ErrorResponseException unsupportedCountry(String countryCode) {
        ProblemDetail problem = problem(422, UNSUPPORTED_COUNTRY, "Unsupported Country",
                "Accounts in " + countryCode + " cannot be verified.");
        problem.setProperty("countryCode", countryCode);
        return answer(problem);
    }

    /** The IBAN is well-formed, but its check digits are wrong. */
    public static ErrorResponseException invalidChecksum() {
        return answer(problem(422, INVALID_IBAN_CHECKSUM, "Invalid IBAN Checksum", "The check digits of the IBAN are wrong."));
    }

    /** The request breaks the contract in a way the generated validation did not catch. */
    public static ErrorResponseException invalidRequest(String reason) {
        return answer(problem(400, INVALID_REQUEST, "Invalid Request", reason));
    }

    private static ProblemDetail problem(int status, URI type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setType(type);
        problem.setTitle(title);
        return problem;
    }

    private static ErrorResponseException answer(ProblemDetail problem) {
        ErrorResponseException exception = new ErrorResponseException(HttpStatusCode.valueOf(problem.getStatus()), problem, null);
        exception.getHeaders().setCacheControl(CacheControl.noStore());
        return exception;
    }
}
