package com.example.demo.advice.response;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;


@Slf4j(topic = "GLOBAL-OBJECT-RESPONSE-WRAPPER")
@RestControllerAdvice
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> MapperType
    ) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedMapperType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response
    ) {

        String requestPath = request.getURI().getPath();

        if(requestPath.contains("api-docs") || requestPath.contains("swagger"))
            return body;

        if(body instanceof ResultApiRes || body instanceof byte[])
            return body;

        HttpStatus statusRes = HttpStatus.OK;
        response.setStatusCode(statusRes);
        ResultApiRes res = new ResultApiRes();
        res.setSuccess(true);
        res.setMessage("Successful");
        res.setStatus(statusRes);
        res.setCode(statusRes.value());
        res.setPath(requestPath);
        res.setResponseData(body);
        return res;
    }
}

