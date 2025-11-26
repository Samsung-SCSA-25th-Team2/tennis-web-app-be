package com.example.scsa.exception;

import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.exception.court.CourtNotFoundException;
import com.example.scsa.exception.match.MatchNotFoundException;
import com.example.scsa.exception.profile.InvalidProfileUpdateException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 전역 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
@Hidden // Swagger UI에서 숨김
public class GlobalExceptionHandler {

    /**
     * javax.validation.Valid 또는 @Validated 으로 binding error 발생시 발생
     * HttpMessageConverter 에서 등록한 HttpMessageConverter binding 못할 경우 발생
     * 주로 @RequestBody, @RequestPart 어노테이션에서 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                        error.getDefaultMessage()
                ))
                .toList();

        final ErrorResponse response = ErrorResponse.builder()
                .error(ErrorCode.INVALID_INPUT_VALUE.getMessage())
                .errorCode(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .fieldErrors(fieldErrors)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * @ModelAttribute 으로 binding error 발생시 BindException 발생
     * ref https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-modelattrib-method-args
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.warn("Bind error: {}", e.getMessage());

        String errorMessage = !e.getBindingResult().getAllErrors().isEmpty()
                ? e.getBindingResult().getAllErrors().get(0).getDefaultMessage()
                : ErrorCode.INVALID_INPUT_VALUE.getMessage();

        final ErrorResponse response = ErrorResponse.of(
                errorMessage,
                ErrorCode.INVALID_INPUT_VALUE.getCode()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * enum type 일치하지 않아 binding 못할 경우 발생
     * 주로 @RequestParam enum으로 binding 못했을 경우 발생
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {} for parameter {}", e.getValue(), e.getName());
        final ErrorResponse response = ErrorResponse.of(
                ErrorCode.INVALID_TYPE_VALUE.getMessage(),
                ErrorCode.INVALID_TYPE_VALUE.getCode()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 필수 파라미터가 누락된 경우
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getParameterName());
        final ErrorResponse response = ErrorResponse.of(
                ErrorCode.MISSING_REQUEST_PARAMETER.getMessage() + " : " + e.getParameterName(),
                ErrorCode.MISSING_REQUEST_PARAMETER.getCode()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 지원하지 않은 HTTP method 호출 할 경우 발생
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMethod());
        final ErrorResponse response = ErrorResponse.of(
                ErrorCode.METHOD_NOT_ALLOWED.getMessage(),
                ErrorCode.METHOD_NOT_ALLOWED.getCode()
        );
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * 사용자를 찾을 수 없는 경우
     */
    @ExceptionHandler(UserNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.of(
                e.getMessage(),
                ErrorCode.USER_NOT_FOUND.getCode()
        );
        return new ResponseEntity<>(response, ErrorCode.USER_NOT_FOUND.getStatus());
    }

    /**
     * 테니스장을 찾을 수 없는 경우
     */
    @ExceptionHandler(CourtNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleCourtNotFoundException(CourtNotFoundException e) {
        log.warn("Court not found: {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.of(
                e.getMessage(),
                ErrorCode.COURT_NOT_FOUND.getCode()
        );
        return new ResponseEntity<>(response, ErrorCode.COURT_NOT_FOUND.getStatus());
    }

    /**
     * 경기를 찾을 수 없는 경우
     */
    @ExceptionHandler(MatchNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleMatchNotFoundException(MatchNotFoundException e) {
        log.warn("Match not found: {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.of(
                e.getMessage(),
                ErrorCode.MATCH_NOT_FOUND.getCode()
        );
        return new ResponseEntity<>(response, ErrorCode.MATCH_NOT_FOUND.getStatus());
    }

    /**
     * 잘못된 프로필 수정 요청인 경우
     */
    @ExceptionHandler(InvalidProfileUpdateException.class)
    protected ResponseEntity<ErrorResponse> handleInvalidProfileUpdateException(InvalidProfileUpdateException e) {
        log.warn("Invalid profile update: {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.of(
                e.getMessage(),
                ErrorCode.INVALID_PROFILE_UPDATE.getCode()
        );
        return new ResponseEntity<>(response, ErrorCode.INVALID_PROFILE_UPDATE.getStatus());
    }

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("Business exception [{}]: {}", e.getErrorCode().getCode(), e.getMessage());
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse response = ErrorResponse.of(
                e.getMessage(),
                errorCode.getCode()
        );
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    /**
     * 정적 리소스를 찾을 수 없는 경우 (favicon, login 등)
     * 로그 레벨을 낮춰서 에러 로그가 쌓이지 않도록 함
     */
    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        String resourcePath = e.getResourcePath();

        // favicon, login 등 일반적인 브라우저 요청은 debug 레벨로만 로깅
        if (resourcePath.equals("favicon.ico") || resourcePath.equals("login") ||
            resourcePath.startsWith("assets/") || resourcePath.startsWith("static/")) {
            log.debug("Static resource not found (expected): {}", resourcePath);
        } else {
            // 그 외 리소스는 warn 레벨로 로깅
            log.warn("Resource not found: {}", resourcePath);
        }

        final ErrorResponse response = ErrorResponse.of(
                "Resource not found: " + resourcePath,
                "RESOURCE_NOT_FOUND"
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * 그 외 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception", e);
        final ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                ErrorCode.INTERNAL_SERVER_ERROR.getCode()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}