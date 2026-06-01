package com.example.shortlink.service.impl;

import com.example.shortlink.service.RocketMQProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RocketMQProducerServiceImpl implements RocketMQProducerService {

    private static final String TOPIC = "short-link-access-topic";

    @Override
    public void sendAccessLog(String shortCode, String ipAddress, String userAgent) {
        String message = String.format("%s|%s|%s", shortCode, ipAddress, userAgent);
        // rocketMQTemplate.sendOneWay(TOPIC, message);
        log.info("Mock sent access log message: {} (RocketMQ not available)", message);
    }
}