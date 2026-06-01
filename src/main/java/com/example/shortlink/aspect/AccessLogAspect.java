package com.example.shortlink.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class AccessLogAspect {

    @Pointcut("execution(* com.example.shortlink.controller..*.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        HttpServletRequest request = null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof HttpServletRequest) {
                request = (HttpServletRequest) arg;
                break;
            }
        }

        String method = request != null ? request.getMethod() : "UNKNOWN";
        String url = request != null ? request.getRequestURI() : "UNKNOWN";
        String ip = request != null ? request.getRemoteAddr() : "UNKNOWN";

        log.info("Request: {} {} from IP: {}", method, url, ip);

        Object result;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            log.error("Error processing request: {} {} - {}", method, url, e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Response: {} {} completed in {}ms", method, url, duration);
        }
    }
}