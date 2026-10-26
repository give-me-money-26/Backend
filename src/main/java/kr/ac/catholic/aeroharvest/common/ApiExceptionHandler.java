package kr.ac.catholic.aeroharvest.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> badRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "입력값을 확인해주세요.", fields, request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    ResponseEntity<ApiError> unprocessable(UnprocessableEntityException exception, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), Map.of(), request);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, Map<String, String> fields,
                                               HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(
                OffsetDateTime.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fields));
    }

    public record ApiError(OffsetDateTime timestamp, int status, String error, String message,
                           String path, Map<String, String> fieldErrors) {}
}
