package com.bank.positionbook.api.error;

import com.bank.positionbook.model.PositionBookException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static com.bank.positionbook.constants.ApiConstants.PROBLEM_ERRORS_PROPERTY;
import static com.bank.positionbook.constants.ApiConstants.PROBLEM_INVALID_PARAMETER_FORMAT;
import static com.bank.positionbook.constants.ApiConstants.PROBLEM_MALFORMED_REQUEST_DETAIL;
import static com.bank.positionbook.constants.ApiConstants.PROBLEM_UNEXPECTED_DETAIL;
import static com.bank.positionbook.constants.ApiConstants.PROBLEM_VALIDATION_DETAIL;

/** Maps every failure to an RFC 7807 ProblemDetail body. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PositionBookException.class)
    public ResponseEntity<ProblemDetail> handlePositionBookException(PositionBookException ex) {
        HttpStatus status = switch (ex.errorType()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return buildProblemResponse(status, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(this::describeFieldError)
                .toList();
        return buildProblemResponse(HttpStatus.BAD_REQUEST, PROBLEM_VALIDATION_DETAIL, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .toList();
        return buildProblemResponse(HttpStatus.BAD_REQUEST, PROBLEM_VALIDATION_DETAIL, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> errors = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        return buildProblemResponse(HttpStatus.BAD_REQUEST, PROBLEM_VALIDATION_DETAIL, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
        return buildProblemResponse(HttpStatus.BAD_REQUEST, PROBLEM_MALFORMED_REQUEST_DETAIL, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildProblemResponse(HttpStatus.BAD_REQUEST,
                String.format(PROBLEM_INVALID_PARAMETER_FORMAT, ex.getName()), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return buildProblemResponse(HttpStatus.INTERNAL_SERVER_ERROR, PROBLEM_UNEXPECTED_DETAIL, null);
    }

    private String describeFieldError(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            return fieldError.getField() + ": " + fieldError.getDefaultMessage();
        }
        return error.getDefaultMessage();
    }

    private ResponseEntity<ProblemDetail> buildProblemResponse(HttpStatus status, String detail, List<String> errors) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.getReasonPhrase());
        if (errors != null && !errors.isEmpty()) {
            body.setProperty(PROBLEM_ERRORS_PROPERTY, errors);
        }
        return ResponseEntity.status(status).body(body);
    }
}
