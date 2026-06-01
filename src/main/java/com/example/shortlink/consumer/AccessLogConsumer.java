package com.example.shortlink.consumer;

import com.example.shortlink.entity.AccessLog;
import com.example.shortlink.mapper.AccessLogMapper;
import com.example.shortlink.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogConsumer {

    private final AccessLogMapper accessLogMapper;
    private final RedisService redisService;

    public void processAccessLog(String message) {
        log.info("Processing access log message: {}", message);
        
        try {
            String[] parts = message.split("\\|");
            if (parts.length >= 3) {
                String shortCode = parts[0];
                String ipAddress = parts[1];
                String userAgent = parts[2];
                
                AccessLog accessLog = new AccessLog();
                accessLog.setShortCode(shortCode);
                accessLog.setIpAddress(ipAddress);
                accessLog.setUserAgent(userAgent);
                accessLog.setDeviceType(parseDeviceType(userAgent));
                accessLog.setAccessTime(LocalDateTime.now());
                
                accessLogMapper.insert(accessLog);
                
                redisService.incrementAccessCount(shortCode);
                
                log.info("Access log recorded for shortCode: {}", shortCode);
            }
        } catch (Exception e) {
            log.error("Error processing access log message: {}", e.getMessage(), e);
        }
    }

    private String parseDeviceType(String userAgent) {
        if (userAgent == null) {
            return "UNKNOWN";
        }
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") && userAgent.contains("mobile")) {
            return "MOBILE";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "TABLET";
        } else if (userAgent.contains("windows") || userAgent.contains("macintosh") || userAgent.contains("linux")) {
            return "DESKTOP";
        }
        return "UNKNOWN";
    }
}
