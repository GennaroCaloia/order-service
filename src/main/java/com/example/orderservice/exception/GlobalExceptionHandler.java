package com.example.orderservice.exception;

import com.example.orderservice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 400 Bad Request ───────────────────────────────────────

    /**
     * Errori di validazione Bean Validation su @RequestBody.
     * Produce un campo fieldErrors con il dettaglio per campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        return build(HttpStatus.BAD_REQUEST,
                "Validation failed",
                "La request contiene campi non validi",
                request,
                fieldErrors);
    }

    /**
     * Violazioni di @Validated su @RequestParam e @PathVariable.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> ErrorResponse.FieldError.builder()
                        .field(extractFieldName(cv.getPropertyPath().toString()))
                        .rejectedValue(cv.getInvalidValue())
                        .message(cv.getMessage())
                        .build())
                .toList();

        return build(HttpStatus.BAD_REQUEST,
                "Constraint violation",
                "Parametri non validi",
                request,
                fieldErrors);
    }

    /**
     * Body JSON malformato o tipo incompatibile.
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            Exception ex,
            HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST,
                "Bad request",
                ex.getMessage(),
                request,
                null);
    }

    // ── 404 Not Found ─────────────────────────────────────────

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            OrderNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Risorsa non trovata: {}", ex.getMessage());

        return build(HttpStatus.NOT_FOUND,
                "Not found",
                ex.getMessage(),
                request,
                null);
    }

    // ── 409 Conflict ──────────────────────────────────────────

    /**
     * Transizione di stato non consentita dalla state machine.
     */
    @ExceptionHandler(IllegalOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalOrderStateException ex,
            HttpServletRequest request) {

        log.warn("Transizione di stato illegale: {}", ex.getMessage());

        return build(HttpStatus.CONFLICT,
                "State conflict",
                ex.getMessage(),
                request,
                null);
    }

    /**
     * Concurrent update rilevato da JPA optimistic locking.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {

        log.warn("Optimistic lock conflict: {}", ex.getMessage());

        return build(HttpStatus.CONFLICT,
                "Concurrent update",
                "L'ordine è stato modificato da un'altra richiesta. Riprova.",
                request,
                null);
    }

    // ── 422 Unprocessable Entity ──────────────────────────────

    /**
     * Ordine in stato non cancellabile.
     */
    @ExceptionHandler(OrderNotCancellableException.class)
    public ResponseEntity<ErrorResponse> handleNotCancellable(
            OrderNotCancellableException ex,
            HttpServletRequest request) {

        log.warn("Cancellazione non consentita: {}", ex.getMessage());

        return build(HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable",
                ex.getMessage(),
                request,
                null);
    }

    // ── 500 Internal Server Error ─────────────────────────────

    /**
     * Fallback per qualsiasi eccezione non gestita esplicitamente.
     * Logga lo stack trace completo — le eccezioni attese sopra
     * usano solo warn/info.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Eccezione non gestita su {}: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "Si è verificato un errore imprevisto.",
                request,
                null);
    }

    // ── Builder helper ────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request,
            List<ErrorResponse.FieldError> fieldErrors) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .traceId(MDC.get("traceId"))   // null se tracing non ancora configurato
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Il propertyPath di una ConstraintViolation ha la forma
     * "methodName.paramName.fieldName" — estraiamo solo l'ultimo segmento.
     */
    private String extractFieldName(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0
                ? propertyPath.substring(lastDot + 1)
                : propertyPath;
    }
}