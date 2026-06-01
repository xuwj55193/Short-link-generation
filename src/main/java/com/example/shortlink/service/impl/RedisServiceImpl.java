package com.example.shortlink.service.impl;

import com.example.shortlink.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_SET = "blacklist:ips";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String ACCESS_STATS_PREFIX = "access_stats:";
    private static final int MAX_REQUESTS = 100;
    private static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    @Override
    public boolean isBlacklistedIp(String ip) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(BLACKLIST_SET, ip));
    }

    @Override
    public void addBlacklistedIp(String ip) {
        redisTemplate.opsForSet().add(BLACKLIST_SET, ip);
    }

    @Override
    public void removeBlacklistedIp(String ip) {
        redisTemplate.opsForSet().remove(BLACKLIST_SET, ip);
    }

    @Override
    public boolean isRateLimited(String ip) {
        String key = RATE_LIMIT_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count != null && count == 1) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        
        return count != null && count > MAX_REQUESTS;
    }

    @Override
    public void incrementAccessCount(String shortCode) {
        String key = ACCESS_STATS_PREFIX + shortCode;
        redisTemplate.opsForHash().increment(key, "total", 1);
        String today = java.time.LocalDate.now().toString();
        redisTemplate.opsForHash().increment(key, today, 1);
    }

    @Override
    public Long getAccessCount(String shortCode) {
        String key = ACCESS_STATS_PREFIX + shortCode;
        Object count = redisTemplate.opsForHash().get(key, "total");
        return count != null ? Long.parseLong(count.toString()) : 0;
    }
}