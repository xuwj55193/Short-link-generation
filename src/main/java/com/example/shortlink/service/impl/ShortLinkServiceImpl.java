package com.example.shortlink.service.impl;

import com.example.shortlink.entity.ShortLink;
import com.example.shortlink.mapper.AccessLogMapper;
import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.service.RocketMQProducerService;
import com.example.shortlink.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl implements ShortLinkService {

    private final ShortLinkMapper shortLinkMapper;
    private final AccessLogMapper accessLogMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final RocketMQProducerService rocketMQProducerService;

    private static final String SHORT_LINK_PREFIX = "short_link:";
    private static final String LOCK_PREFIX = "lock:short_link:";
    private static final String ACCESS_STATS_PREFIX = "access_stats:";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final long LOCK_EXPIRE_TIME = 30;

    @Override
    @Transactional
    public String generateShortLink(String longUrl, Long userId) {
        String lockKey = LOCK_PREFIX + longUrl;
        String lockValue = String.valueOf(System.currentTimeMillis());
        
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(lockAcquired)) {
            ShortLink existingLink = shortLinkMapper.selectByLongUrl(longUrl);
            if (existingLink != null) {
                return existingLink.getShortCode();
            }
            throw new RuntimeException("正在生成短链，请稍后重试");
        }

        try {
            ShortLink existingLink = shortLinkMapper.selectByLongUrl(longUrl);
            if (existingLink != null) {
                return existingLink.getShortCode();
            }

            String shortCode = generateUniqueShortCode();
            
            ShortLink shortLink = new ShortLink();
            shortLink.setLongUrl(longUrl);
            shortLink.setShortCode(shortCode);
            shortLink.setUserId(userId);
            shortLink.setCreatedAt(LocalDateTime.now());
            shortLink.setUpdatedAt(LocalDateTime.now());
            
            shortLinkMapper.insert(shortLink);
            
            redisTemplate.opsForValue().set(SHORT_LINK_PREFIX + shortCode, longUrl);
            
            return shortCode;
        } finally {
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    @Override
    public String getLongUrl(String shortCode) {
        String cacheKey = SHORT_LINK_PREFIX + shortCode;
        String longUrl = redisTemplate.opsForValue().get(cacheKey);
        
        if (longUrl != null) {
            return longUrl;
        }
        
        ShortLink shortLink = shortLinkMapper.selectByShortCode(shortCode);
        if (shortLink != null) {
            redisTemplate.opsForValue().set(cacheKey, shortLink.getLongUrl(), 1, TimeUnit.HOURS);
            return shortLink.getLongUrl();
        }
        
        return null;
    }

    @Override
    public void recordAccess(String shortCode, String ipAddress, String userAgent) {
        // 通过 RocketMQ 异步发送访问日志
        rocketMQProducerService.sendAccessLog(shortCode, ipAddress, userAgent);
    }

    @Override
    public Long getAccessCount(String shortCode) {
        String statsKey = ACCESS_STATS_PREFIX + shortCode;
        Object countObj = redisTemplate.opsForHash().get(statsKey, "total");
        if (countObj != null) {
            return Long.parseLong(countObj.toString());
        }
        return accessLogMapper.countAccessByShortCode(shortCode);
    }

    private String generateUniqueShortCode() {
        String shortCode;
        do {
            shortCode = generateShortCode();
        } while (shortLinkMapper.selectByShortCode(shortCode) != null || 
                 Boolean.TRUE.equals(redisTemplate.hasKey(SHORT_LINK_PREFIX + shortCode)));
        return shortCode;
    }

    private String generateShortCode() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf(Math.random());
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest((timestamp + random).getBytes(StandardCharsets.UTF_8));
            String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return base64.substring(0, SHORT_CODE_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}