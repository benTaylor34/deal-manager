package com.dealmanager.web;

import com.dealmanager.domain.DealRuleViolationException;
import com.dealmanager.domain.IllegalTransitionException;
import com.dealmanager.service.InvalidRequestException;
import com.dealmanager.service.NotFoundException;
import com.dealmanager.service.StaleVersionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps domain exceptions to RFC 9457 problem details with the right status code. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail invalid(InvalidRequestException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    @ExceptionHandler(IllegalTransitionException.class)
    ProblemDetail illegalTransition(IllegalTransitionException e) {
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "Illegal status transition", e.getMessage());
        pd.setProperty("currentStatus", e.from());
        pd.setProperty("action", e.action());
        return pd;
    }

    @ExceptionHandler(DealRuleViolationException.class)
    ProblemDetail ruleViolation(DealRuleViolationException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violated", e.getMessage());
    }

    @ExceptionHandler(StaleVersionException.class)
    ProblemDetail stale(StaleVersionException e) {
        ProblemDetail pd = problem(HttpStatus.PRECONDITION_FAILED, "Stale version", e.getMessage());
        pd.setProperty("currentVersion", e.actual());
        return pd;
    }

    @ExceptionHandler(ETags.PreconditionRequiredException.class)
    ProblemDetail preconditionRequired(ETags.PreconditionRequiredException e) {
        return problem(HttpStatus.PRECONDITION_REQUIRED, "If-Match required", e.getMessage());
    }

    /** Two requests passed the If-Match check at the same moment; Hibernate caught the second. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(ObjectOptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "Concurrent update",
                "The deal was changed by someone else at the same time. Reload and try again.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "Your role is not allowed to do this");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
