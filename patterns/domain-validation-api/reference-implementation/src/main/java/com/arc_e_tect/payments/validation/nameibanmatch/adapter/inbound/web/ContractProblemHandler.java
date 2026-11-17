package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Answers the problems the contract declares for what the service does not answer itself: a
 * request that breaks the contract with {@code 400} and the invalid-request problem, and what fails
 * unexpectedly with {@code 500} and the internal-server-error problem, which says nothing of what
 * failed. Every problem, these and those the service answers itself, is {@code no-store}.
 *
 * <p>The invalid-request problem has an error for every place the request breaks the contract that
 * can be named: a member of the body by its JSON Pointer, a header or a path parameter by the name
 * the contract gives it. It applies to the whole service, because some requests are refused before
 * a controller is chosen: one without a body, which then has no {@code Content-Type} either.
 */
@RestControllerAdvice
public class ContractProblemHandler extends ResponseEntityExceptionHandler {

    private static final URI INVALID_REQUEST = URI.create("https://api.example.com/problems/invalid-request");
    private static final URI INTERNAL_SERVER_ERROR = URI.create("https://api.example.com/problems/internal-server-error");
    private static final String DOES_NOT_MATCH = "The request does not match the contract.";
    private static final String UNEXPECTED = "An unexpected error occurred while processing the request.";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error(error.getDefaultMessage(), pointer(error.getField())))
                .toList();
        return invalidRequest(ex, DOES_NOT_MATCH, errors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getCause() instanceof DatabindException unreadable && !unreadable.getPath().isEmpty()
                ? List.of(error(message(unreadable), pointer(unreadable.getPath())))
                : List.of();
        return invalidRequest(ex, DOES_NOT_MATCH, errors, headers, request);
    }

    /** Without a Content-Type there is no body, which the contract requires: that breaks the contract, rather than being a media type it does not accept. */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers,
                                                                     HttpStatusCode status, WebRequest request) {
        if (request.getHeader(HttpHeaders.CONTENT_TYPE) == null) {
            return invalidRequest(ex, "The request has no body, though the contract requires one.", List.of(), headers, request);
        }
        return super.handleHttpMediaTypeNotSupported(ex, headers, status, request);
    }

    /** A header or a path parameter that breaks the contract; the generated interface validates them. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, HandlerMethod handler, WebRequest request) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(violation -> error(violation.getMessage(), context(violation, handler)))
                .toList();
        return invalidRequest(ex, DOES_NOT_MATCH, errors, new HttpHeaders(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        logger.error("Unexpected failure while processing " + request.getDescription(false), ex);
        return handleExceptionInternal(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Every problem is not stored; a {@code 400} is the contract's invalid-request problem, and a
     * {@code 5xx} its internal-server-error problem, whatever Spring made of it.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode,
                                                             WebRequest request) {
        // Some exceptions hand over headers that cannot be changed.
        HttpHeaders notStored = HttpHeaders.copyOf(headers);
        notStored.setCacheControl(CacheControl.noStore());
        Object answered = statusCode.is5xxServerError()
                ? problem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, UNEXPECTED)
                : body;
        // Spring makes the problem of its own exceptions here; it answers nothing once the response is committed.
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, answered, notStored, statusCode, request);
        if (response != null && statusCode.value() == HttpStatus.BAD_REQUEST.value() && response.getBody() instanceof ProblemDetail problem) {
            problem.setType(INVALID_REQUEST);
            problem.setTitle("Invalid Request");
        }
        return response;
    }

    private ResponseEntity<Object> invalidRequest(Exception ex, String detail, List<Map<String, String>> errors, HttpHeaders headers,
                                                  WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, INVALID_REQUEST, detail);
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * A problem titled by its status's reason phrase, as the internal-server-error problem is; a
     * {@code 400} is titled Invalid Request in {@link #handleExceptionInternal}, as every one is.
     */
    private static ProblemDetail problem(HttpStatus status, URI type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        return problem;
    }

    private static Map<String, String> error(String message, String context) {
        return Map.of("message", message, "context", context);
    }

    /** The JSON Pointer of a field as Spring names it: {@code details.findings[2].rule} is {@code /details/findings/2/rule}. */
    private static String pointer(String field) {
        return "/" + field.replace("[", ".").replace("]", "").replace('.', '/');
    }

    private static String pointer(List<JacksonException.Reference> path) {
        return path.stream()
                .map(reference -> "/" + (reference.getPropertyName() == null ? reference.getIndex() : reference.getPropertyName()))
                .collect(Collectors.joining());
    }

    private static String message(DatabindException unreadable) {
        return unreadable instanceof UnrecognizedPropertyException
                ? "is not a member the contract declares"
                : "has the wrong type for the contract";
    }

    /** A header or a path parameter by the name the contract gives it; a member of the body by its JSON Pointer. */
    private static String context(ConstraintViolation<?> violation, HandlerMethod handler) {
        StringBuilder pointer = new StringBuilder();
        for (Path.Node node : violation.getPropertyPath()) {
            if (node.getKind() == ElementKind.PARAMETER) {
                MethodParameter parameter = handler.getMethodParameters()[node.as(Path.ParameterNode.class).getParameterIndex()];
                Optional<String> named = httpName(parameter);
                if (named.isPresent()) {
                    return named.get();
                }
            } else if (node.getKind() == ElementKind.PROPERTY) {
                pointer.append('/').append(node.getName());
            }
        }
        return pointer.toString();
    }

    private static Optional<String> httpName(MethodParameter parameter) {
        RequestHeader header = parameter.getParameterAnnotation(RequestHeader.class);
        if (header != null) {
            return Optional.of(header.value());
        }
        return Optional.ofNullable(parameter.getParameterAnnotation(PathVariable.class)).map(PathVariable::value);
    }
}
