package com.example.shortlink.service;

public interface RocketMQProducerService {

    void sendAccessLog(String shortCode, String ipAddress, String userAgent);
}