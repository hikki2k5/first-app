package com.example.demo.advice.response;



import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
public class ResultApiRes {

    private Boolean success;
    private String message;
    private String path;
    private HttpStatus status;
    private Integer code;
    private Object responseData;
    private Object errors;
    @Setter(AccessLevel.NONE)
    private final Long timestamp = System.currentTimeMillis();

    public ResultApiRes(Exception ex, HttpStatus status, HttpServletRequest request, HttpServletResponse response){
        response.setStatus(status.value());
        this.setSuccess(false);
        this.setPath(request.getRequestURI());
        this.setStatus(status);
        this.setCode(status.value());
        this.setMessage(ex.getMessage());
        Violation violation = new Violation();
        violation.setField(ex.getCause() == null ? "" : ex.getCause().getMessage());
        violation.setMessage(ex.getCause() == null ? "" : ex.getMessage());
        this.setErrors(violation);
    }

    public ResultApiRes(Exception ex, Violation violation, HttpStatus status, HttpServletRequest request, HttpServletResponse response){
        response.setStatus(status.value());
        this.setSuccess(false);
        this.setPath(request.getRequestURI());
        this.setStatus(status);
        this.setCode(status.value());
        this.setMessage(ex.getMessage());
        this.setErrors(violation);
    }

    @Data
    @NoArgsConstructor
    public static class Violation {
        private String field;
        private String message;
    }

}

