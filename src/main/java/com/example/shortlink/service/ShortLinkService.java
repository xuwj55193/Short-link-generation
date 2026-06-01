package com.example.shortlink.service;

import com.example.shortlink.entity.ShortLink;

public interface ShortLinkService {

    String generateShortLink(String longUrl, Long userId);

    String getLongUrl(String shortCode);

    void recordAccess(String shortCode, String ipAddress, String userAgent);

    Long getAccessCount(String shortCode);
}