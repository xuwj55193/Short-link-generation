package com.example.shortlink.service;

public interface RedisService {

    boolean isBlacklistedIp(String ip);

    void addBlacklistedIp(String ip);

    void removeBlacklistedIp(String ip);

    boolean isRateLimited(String ip);

    void incrementAccessCount(String shortCode);

    Long getAccessCount(String shortCode);
}