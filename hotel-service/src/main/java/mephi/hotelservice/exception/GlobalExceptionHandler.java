package mephi.hotelservice.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import mephi.hotelservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
        ResourceNotFoundException ex,
        HttpServletRequest request
    ) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RoomNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotAvailableException(
        RoomNotAvailableException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Room not available: roomId={}, requestId={}, message={}",
            ex.getRoomId(),
            ex.getRequestId(),
            ex.getMessage()
        );

        ErrorResponse error = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
        DuplicateResourceException ex,
        HttpServletRequest request
    ) {
        log.warn("Duplicate resource: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        BindingResult result = ex.getBindingResult();

        List<ErrorResponse.FieldError> fieldErrors = result.getFieldErrors().stream()
            .map(fe -> ErrorResponse.FieldError.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .rejectedValue(fe.getRejectedValue())
                .build()
            )
            .collect(Collectors.toList());

        log.warn("Validation failed: {} errors", fieldErrors.size());

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("One or more fields have validation errors")
            .path(request.getRequestURI())
            .fieldErrors(fieldErrors)
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest request
    ) {
        log.warn("Type mismatch: parameter={}, value={}", ex.getName(), ex.getValue());

        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());

        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "Access denied. You don't have permission to access this resource.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
        AuthenticationException ex,
        HttpServletRequest request
    ) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            "Authentication required. Please provide valid credentials.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unexpected error occurred", ex);

        ErrorResponse error = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
