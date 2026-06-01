package com.example.shortlink.controller;

import com.example.shortlink.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @GetMapping(value = "/", produces = "text/html")
    public org.springframework.core.io.Resource index() {
        return new org.springframework.core.io.ClassPathResource("static/index.html");
    }

    @PostMapping("/api/shortlink/generate")
    public ResponseEntity<Map<String, Object>> generateShortLink(
            @RequestBody Map<String, String> request,
            @RequestParam(defaultValue = "1") Long userId) {
        
        String longUrl = request.get("longUrl");
        if (longUrl == null || longUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL不能为空"));
        }

        String shortCode = shortLinkService.generateShortLink(longUrl, userId);
        String shortUrl = "http://localhost:8080/" + shortCode;

        Map<String, Object> response = new HashMap<>();
        response.put("shortUrl", shortUrl);
        response.put("shortCode", shortCode);
        response.put("longUrl", longUrl);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        // 如果短代码是 "api"，则不处理，避免与 API 路径冲突
        if ("api".equals(shortCode) || "index.html".equals(shortCode)) {
            return ResponseEntity.notFound().build();
        }
        
        String longUrl = shortLinkService.getLongUrl(shortCode);
        
        if (longUrl == null) {
            return ResponseEntity.notFound().build();
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        shortLinkService.recordAccess(shortCode, ipAddress, userAgent);
        
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }

    @GetMapping("/stats/{shortCode}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String shortCode) {
        Long count = shortLinkService.getAccessCount(shortCode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("shortCode", shortCode);
        response.put("accessCount", count);
        
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}