package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchApi;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchRequestV1;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@DisplayName("The handler of the problems the contract declares")
class ContractProblemHandlerTest {

    private static final URI INVALID_REQUEST = URI.create("https://api.example.com/problems/invalid-request");
    private static final URI INTERNAL_SERVER_ERROR = URI.create("https://api.example.com/problems/internal-server-error");
    private static final URI NOT_ACCEPTABLE = URI.create("https://api.example.com/problems/not-acceptable");
    private static final URI UNSUPPORTED_MEDIA_TYPE = URI.create("https://api.example.com/problems/unsupported-media-type");
    private static final String VALIDATIONS = "/payments/v1/validations/name-iban-match";

    private final ContractProblemHandler handler = new ContractProblemHandler();
    private final MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", VALIDATIONS);
    private final ServletWebRequest request = new ServletWebRequest(servletRequest);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final JsonMapper json = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    private static ProblemDetail problem(ResponseEntity<Object> response) {
        return (ProblemDetail) response.getBody();
    }

    private static Object errors(ResponseEntity<Object> response) {
        return problem(response).getProperties() == null ? null : problem(response).getProperties().get("errors");
    }

    private static Map<String, String> error(String message, String context) {
        return Map.of("message", message, "context", context);
    }

    private static Method operation(String name) {
        for (Method method : NameIbanMatchApi.class.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException(name);
    }

    @Nested
    @DisplayName("answers a request that breaks the contract with the invalid-request problem")
    class InvalidRequest {

        private ResponseEntity<Object> bodyViolation(String objectName, FieldError... errors) throws NoSuchMethodException {
            BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), objectName);
            for (FieldError error : errors) {
                result.addError(error);
            }
            MethodParameter body = new MethodParameter(operation("validateNameIbanMatch"), 0);
            return handler.handleMethodArgumentNotValid(new MethodArgumentNotValidException(body, result),
                    new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        }

        private ResponseEntity<Object> unreadable(String body) {
            try {
                json.readValue(body, NameIbanMatchRequestV1.class);
            } catch (JacksonException cause) {
                return handler.handleHttpMessageNotReadable(new HttpMessageNotReadableException("unreadable", cause,
                        new MockHttpInputMessage(body.getBytes())), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
            }
            throw new IllegalArgumentException("The body is readable: " + body);
        }

        private ResponseEntity<Object> parameterViolation(String operation, Object... arguments) {
            Method method = operation(operation);
            Set<ConstraintViolation<NameIbanMatchController>> violations = validator.forExecutables()
                    .validateParameters(mock(NameIbanMatchController.class), method, arguments);
            HandlerMethod handled = new HandlerMethod(mock(NameIbanMatchController.class), method);
            return handler.handleConstraintViolation(new ConstraintViolationException(violations), handled, request);
        }

        @Test
        @DisplayName("of the contract's type, title and status, saying that the request does not match the contract")
        void ofTheContractsType() throws NoSuchMethodException {
            ProblemDetail problem = problem(bodyViolation("request", new FieldError("request", "iban", "must not be null")));

            assertEquals(List.of(INVALID_REQUEST, "Invalid Request", 400, "The request does not match the contract."),
                    List.of(problem.getType(), problem.getTitle(), problem.getStatus(), problem.getDetail()));
        }

        @Test
        @DisplayName("that is not stored")
        void thatIsNotStored() throws NoSuchMethodException {
            ResponseEntity<Object> response = bodyViolation("request", new FieldError("request", "iban", "must not be null"));

            assertEquals(CacheControl.noStore().getHeaderValue(), response.getHeaders().getCacheControl());
        }

        @Test
        @DisplayName("with an error for every member of the body that breaks it, at its JSON Pointer")
        void withAnErrorForEveryMember() throws NoSuchMethodException {
            ResponseEntity<Object> response = bodyViolation("request",
                    new FieldError("request", "iban", "must match \"^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$\""),
                    new FieldError("request", "accountHolderName", "size must be between 1 and 70"));

            assertEquals(List.of(error("must match \"^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$\"", "/iban"),
                    error("size must be between 1 and 70", "/accountHolderName")), errors(response));
        }

        @Test
        @DisplayName("with a JSON Pointer through nested members and array items")
        void withAPointerThroughNestedMembers() throws NoSuchMethodException {
            ResponseEntity<Object> response = bodyViolation("request", new FieldError("request", "details.findings[2].rule", "must not be null"));

            assertEquals(List.of(error("must not be null", "/details/findings/2/rule")), errors(response));
        }

        @Test
        @DisplayName("with an error for a member of the wrong type")
        void withAnErrorForTheWrongType() {
            assertEquals(List.of(error("has the wrong type for the contract", "/iban")),
                    errors(unreadable("{\"iban\": [\"NL91ABNA0417164300\"], \"accountHolderName\": \"Jan de Vries\"}")));
        }

        @Test
        @DisplayName("with an error for a member the contract does not declare")
        void withAnErrorForAnUnknownMember() {
            assertEquals(List.of(error("is not a member the contract declares", "/nickname")),
                    errors(unreadable("{\"iban\": \"NL91ABNA0417164300\", \"accountHolderName\": \"Jan de Vries\", \"nickname\": \"Jan\"}")));
        }

        @Test
        @DisplayName("with an error at the item of an array that breaks it")
        void withAnErrorAtAnArrayItem() {
            assertEquals(List.of(error("has the wrong type for the contract", "/0")), errors(unreadableList("[{}]")));
        }

        private ResponseEntity<Object> unreadableList(String body) {
            try {
                json.readValue(body, String[].class);
            } catch (JacksonException cause) {
                return handler.handleHttpMessageNotReadable(new HttpMessageNotReadableException("unreadable", cause,
                        new MockHttpInputMessage(body.getBytes())), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
            }
            throw new IllegalArgumentException("The body is readable: " + body);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"[\"NL91ABNA0417164300\", \"Jan de Vries\"]", "{\"iban\": \"NL91ABNA0417164300\","})
        @DisplayName("without errors for a body that breaks it as a whole")
        void withoutErrorsForTheWholeBody(String body) {
            ResponseEntity<Object> response = unreadable(body);

            assertEquals(List.of(INVALID_REQUEST, "No errors"),
                    List.of(problem(response).getType(), errors(response) == null ? "No errors" : errors(response)));
        }

        @Test
        @DisplayName("without errors for a body that is unreadable for another reason")
        void withoutErrorsForAnotherReason() {
            ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(
                    new HttpMessageNotReadableException("unreadable", new IllegalStateException("stream closed"), new MockHttpInputMessage(new byte[0])),
                    new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

            assertNull(errors(response));
        }

        @Test
        @DisplayName("with an error for a header that breaks it, named as the contract names it")
        void withAnErrorForAHeader() {
            ResponseEntity<Object> response = parameterViolation("validateNameIbanMatch",
                    new NameIbanMatchRequestV1("NL91ABNA0417164300", "Jan de Vries"), null, "Not-A-UUID");

            assertEquals(List.of(error("must match \"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$\"", "Idempotency-Key")),
                    errors(response));
        }

        @Test
        @DisplayName("with an error for a path parameter that breaks it, named as the contract names it")
        void withAnErrorForAPathParameter() {
            ResponseEntity<Object> response = parameterViolation("getNameIbanMatchJob", "8D4E2A10-5B7C-4E9F-A3D6-1C2B3A4D5E6F");

            assertEquals(List.of(error("must match \"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$\"", "validationId")),
                    errors(response));
        }

        @Test
        @DisplayName("with an error at the JSON Pointer of a member that breaks it outside a parameter")
        void withAnErrorAtTheViolationsPath() {
            Set<ConstraintViolation<NameIbanMatchRequestV1>> violations = validator.validate(new NameIbanMatchRequestV1("NL91", "Jan de Vries"));
            HandlerMethod handled = new HandlerMethod(mock(NameIbanMatchController.class), operation("validateNameIbanMatch"));

            ResponseEntity<Object> response = handler.handleConstraintViolation(new ConstraintViolationException(violations), handled, request);

            assertEquals(List.of(error("must match \"^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$\"", "/iban")), errors(response));
        }

        @Test
        @DisplayName("with an error at the JSON Pointer of a member of the body that breaks it")
        void withAnErrorAtAMemberOfTheBody() {
            ResponseEntity<Object> response = parameterViolation("validateNameIbanMatch",
                    new NameIbanMatchRequestV1("NL91", "Jan de Vries"), null, null);

            assertEquals(List.of(error("must match \"^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$\"", "/iban")), errors(response));
        }

        @Test
        @DisplayName("for a request without a body, which the contract requires, and so without a Content-Type")
        void forARequestWithoutABody() {
            ResponseEntity<Object> response = handler.handleHttpMediaTypeNotSupported(
                    new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_OCTET_STREAM, List.of(MediaType.APPLICATION_JSON),
                            org.springframework.http.HttpMethod.POST),
                    new HttpHeaders(), HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);

            assertEquals(List.of(HttpStatus.BAD_REQUEST, INVALID_REQUEST, "The request has no body, though the contract requires one."),
                    List.of(response.getStatusCode(), problem(response).getType(), problem(response).getDetail()));
        }

        @Test
        @DisplayName("for any other request Spring refuses with 400")
        void forAnyOther400() throws Exception {
            ResponseEntity<Object> response = handler.handleException(
                    new org.springframework.web.bind.MissingServletRequestParameterException("page", "int"), request);

            assertEquals(List.of(INVALID_REQUEST, "Invalid Request"), List.of(problem(response).getType(), problem(response).getTitle()));
        }

        @Test
        @DisplayName("for a missing header Spring refuses, keeping Spring's detail")
        void forAMissingHeader() throws Exception {
            MethodParameter header = new MethodParameter(operation("validateNameIbanMatch"), 2);
            ResponseEntity<Object> response = handler.handleException(new MissingRequestHeaderException("Idempotency-Key", header), request);

            assertEquals(List.of(INVALID_REQUEST, "Required header 'Idempotency-Key' is not present."),
                    List.of(problem(response).getType(), problem(response).getDetail()));
        }
    }

    @Nested
    @DisplayName("keeps other problems as they are")
    class OtherProblems {

        @Test
        @DisplayName("but not stored: a problem the service answers itself")
        void aProblemTheServiceAnswers() throws Exception {
            ResponseEntity<Object> response = handler.handleException(NameIbanMatchProblems.invalidChecksum(), request);

            assertEquals(List.of(URI.create("https://api.example.com/payments/problems/invalid-iban-checksum"),
                            CacheControl.noStore().getHeaderValue()),
                    List.of(problem(response).getType(), response.getHeaders().getCacheControl()));
        }
    }

    @Nested
    @DisplayName("answers a media type the contract does not declare with the contract's media-type problem")
    class MediaTypes {

        @Test
        @DisplayName("415 unsupported-media-type, not stored: a Content-Type the contract does not accept, with a body")
        void anUnsupportedContentType() {
            servletRequest.setContentType("text/plain");
            ResponseEntity<Object> response = handler.handleHttpMediaTypeNotSupported(
                    new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON), org.springframework.http.HttpMethod.POST),
                    new HttpHeaders(), HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);

            assertEquals(List.of(HttpStatusCode.valueOf(415), UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", CacheControl.noStore().getHeaderValue()),
                    List.of(response.getStatusCode(), problem(response).getType(), problem(response).getTitle(),
                            response.getHeaders().getCacheControl()));
        }

        @Test
        @DisplayName("406 not-acceptable, not stored: an Accept that admits nothing the contract answers with")
        void anUnacceptableAccept() throws Exception {
            ResponseEntity<Object> response = handler.handleException(
                    new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON)), request);

            assertEquals(List.of(HttpStatusCode.valueOf(406), NOT_ACCEPTABLE, "Not Acceptable", CacheControl.noStore().getHeaderValue()),
                    List.of(response.getStatusCode(), problem(response).getType(), problem(response).getTitle(),
                            response.getHeaders().getCacheControl()));
        }
    }

    @Test
    @DisplayName("leaves a 400 that is no problem as it is")
    void leavesA400ThatIsNoProblem() {
        ResponseEntity<Object> response = handler.handleExceptionInternal(new IllegalArgumentException("x"), "not a problem",
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        assertEquals("not a problem", response.getBody());
    }

    @Test
    @DisplayName("answers nothing once the response is committed")
    void answersNothingOnceCommitted() throws Exception {
        org.springframework.mock.web.MockHttpServletResponse committed = new org.springframework.mock.web.MockHttpServletResponse();
        committed.setCommitted(true);

        assertNull(handler.handleException(new org.springframework.web.bind.MissingServletRequestParameterException("page", "int"),
                new ServletWebRequest(servletRequest, committed)));
    }

    @Nested
    @DisplayName("answers what fails unexpectedly with the internal-server-error problem")
    class Unexpected {

        @Test
        @DisplayName("of the contract's type, title and status, without saying what failed")
        void ofTheContractsType() {
            ProblemDetail problem = problem(handler.handleUnexpected(new IllegalStateException("the database password is wrong"), request));

            assertEquals(List.of(INTERNAL_SERVER_ERROR, "Internal Server Error", 500, "An unexpected error occurred while processing the request."),
                    List.of(problem.getType(), problem.getTitle(), problem.getStatus(), problem.getDetail()));
        }

        @Test
        @DisplayName("that is not stored")
        void thatIsNotStored() {
            ResponseEntity<Object> response = handler.handleUnexpected(new IllegalStateException("broken"), request);

            assertEquals(CacheControl.noStore().getHeaderValue(), response.getHeaders().getCacheControl());
        }

        @Test
        @DisplayName("also when Spring fails with 500 itself")
        void whenSpringFails() throws Exception {
            ResponseEntity<Object> response = handler.handleException(
                    new org.springframework.beans.ConversionNotSupportedException("x", String.class, null), request);

            assertEquals(List.of(INTERNAL_SERVER_ERROR, "An unexpected error occurred while processing the request."),
                    List.of(problem(response).getType(), problem(response).getDetail()));
        }
    }
}
