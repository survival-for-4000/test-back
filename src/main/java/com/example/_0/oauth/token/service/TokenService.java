package com.example._0.oauth.token.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token.expire-time}")
    private long refreshTokenExpireTime;

    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveRefreshToken(String providerId, String refreshToken) {
        redisTemplate.opsForValue().set(
                providerId + ":refresh-token",
                refreshToken,
                refreshTokenExpireTime,
                TimeUnit.MILLISECONDS
        );
    }

    public String getRefreshToken(String providerId) {
        return redisTemplate.opsForValue().get(providerId + ":refresh-token");
    }

}