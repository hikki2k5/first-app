package com.example.demo.advice.exception;


import com.example.demo.advice.response.ResultApiRes;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.util.*;

@Slf4j(topic = "GLOBAL-EXCEPTION-HANDLER")
@RestControllerAdvice
public class GlobalExceptionHandler implements AccessDeniedHandler, AuthenticationEntryPoint {

    private void logError(HttpStatus status, Exception ex){
        log.error("{} -> code {}", ex.getMessage(), status.value());
    }

    public void printError(Exception ex, HttpStatus status, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(new ObjectMapper().writeValueAsString(new ResultApiRes(ex, status, request, response)));
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        logError(status, authException);
        this.printError(authException, status, request, response);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        HttpStatus status = HttpStatus.FORBIDDEN;
        logError(status, accessDeniedException);
        this.printError(accessDeniedException, status, request, response);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResultApiRes handleResponseStatusException(ApplicationException ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        logError(status, ex);
        return new ResultApiRes(ex, status, request, response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultApiRes methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        logError(status, ex);
        return new ResultApiRes(ex, status, request, response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResultApiRes handlingResourceNotFoundException(NoResourceFoundException ex, HttpServletRequest request, HttpServletResponse response){
        HttpStatus status = HttpStatus.NOT_FOUND;
        logError(status, ex);
        return new ResultApiRes(ex, status, request, response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResultApiRes handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request, HttpServletResponse response){
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logError(status, ex);
        ResultApiRes.Violation violation = new ResultApiRes.Violation();
        violation.setField(ex.getName());
        violation.setMessage("Expected type [" + ex.getRequiredType().getSimpleName() + "]");
        return new ResultApiRes(ex, violation, status, request, response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultApiRes handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logError(status, ex);
        Throwable cause = ex.getCause();
        ResultApiRes.Violation violation = new ResultApiRes.Violation();
        if (cause instanceof InvalidFormatException invalidFormatException) {

            // Lấy path cuối (thường là field gây lỗi)
            String fieldName = "unknown";
            if (invalidFormatException.getPath() != null && !invalidFormatException.getPath().isEmpty()) {
                // Reference.toString() thường ra dạng: Dto["age"] hoặc Dto[0]
                String refStr = String.valueOf(
                        invalidFormatException.getPath().get(invalidFormatException.getPath().size() - 1)
                );

                // Try parse ["field"]
                int start = refStr.indexOf("[\"");
                int end = refStr.indexOf("\"]");
                if (start >= 0 && end > start) {
                    fieldName = refStr.substring(start + 2, end);
                } else {
                    // fallback: để nguyên string cho dễ debug
                    fieldName = refStr;
                }
            }

            String expectedType = invalidFormatException.getTargetType() != null
                    ? invalidFormatException.getTargetType().getSimpleName()
                    : "unknown";

            violation.setField(fieldName);
            violation.setMessage("Expected type: " + expectedType);
        }

        else if (cause instanceof JsonParseException jsonParseException) {

            String messageResponse = "Invalid JSON format.";

            JsonLocation location = jsonParseException.getLocation();
            if (location != null) {
                int line = location.getLineNr();
                int column = location.getColumnNr();
                messageResponse = String.format(
                        "Invalid JSON at line %d, column %d.",
                        line,
                        column
                );
            }

            violation.setMessage(messageResponse);
        }

        return new ResultApiRes(ex, violation, status, request, response);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultApiRes handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request, HttpServletResponse response) throws NoSuchFieldException {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logError(status, ex);
        BindingResult bindingResult = ex.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        ResultApiRes.Violation violation = new ResultApiRes.Violation();
        if(!fieldErrors.isEmpty()) {
            FieldError fieldError = fieldErrors.get(0);
            violation.setField(fieldError.getField());
            violation.setMessage(fieldError.getDefaultMessage());
        }
        return new ResultApiRes(ex, violation, status, request, response);
    }

    @ExceptionHandler({
            InternalAuthenticationServiceException.class,
            AuthenticationException.class
    })
    public ResultApiRes handleBadAuthException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        logError(status, ex);
        return new ResultApiRes(ex, status, request, response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResultApiRes handleDisabledException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        logError(status, ex);
        Exception exr = new Exception("Account is locked");
        return new ResultApiRes(exr, status, request, response);
    }

    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class
    })
    public ResultApiRes handleBadCredentialsException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        logError(status, ex);
        Exception exr = new Exception("Wrong username or password");
        return new ResultApiRes(exr, status, request, response);
    }

    @ExceptionHandler({
            HttpClientErrorException.class,
            IllegalArgumentException.class,
            FileAlreadyExistsException.class,
            UnsupportedOperationException.class,
            MissingServletRequestParameterException.class
    })
    public ResultApiRes handleHttpClientErrorException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logError(status, ex);
        return new ResultApiRes(ex, status, request, response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResultApiRes handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        logError(status, ex);
        return new ResultApiRes(ex, status, request, response);
    }

    @ExceptionHandler
    public ResultApiRes handleException(Exception ex, HttpServletRequest request, HttpServletResponse response){
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        logError(status, ex);
        return new ResultApiRes(ex, status, request, response);
    }

}
