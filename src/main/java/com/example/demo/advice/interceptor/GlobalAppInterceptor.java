package com.example.demo.advice.interceptor;


import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j(topic = "MIDDLEWARE-INTERCEPTOR")
@Configuration
public class GlobalAppInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            @Nonnull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @Nonnull Object handler
    ) throws Exception
    {
        HandlerInterceptor.super.preHandle(request, response, handler);

        log.info("-> preHandle in Interceptor");

        return true;
    }

    @Override
    public void postHandle(
            @NonNull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Object handler,
            ModelAndView modelAndView
    ) throws Exception
    {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);

        log.info("-> postHandle in Interceptor");
    }

    @Override
    public void afterCompletion(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Object handler,
            Exception ex
    ) throws Exception
    {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);

        log.info("-> afterCompletion in Interceptor");
    }
}

