package com.example.demo.advice.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
@Slf4j(topic = "GLOBAL-OBJECT-RESPONSE-WRAPPER")
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        String path = request.getURI().getPath();

        // bỏ swagger
        if (path.contains("swagger") || path.contains("api-docs")) {
            return body;
        }

        // đã wrap rồi
        if (body instanceof ResultApiRes) {
            return body;
        }

        ResultApiRes res = new ResultApiRes();
        res.setSuccess(true);
        res.setMessage("Successful");
        res.setPath(path);
        res.setResponseData(body);
        res.setCode(200);
        res.setStatus(HttpStatus.OK);

        // ⭐ xử lý riêng String
        if (body instanceof String) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return mapper.writeValueAsString(res);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return res;
    }
}



