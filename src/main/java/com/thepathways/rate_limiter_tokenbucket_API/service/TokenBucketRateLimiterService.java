package com.thepathways.rate_limiter_tokenbucket_API.service;

import com.thepathways.rate_limiter_tokenbucket_API.store.RateLimiterStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenBucketRateLimiterService {

    private final RateLimiterStore rateLimiterStore;

    public boolean allowRequest(String userId) {
        return rateLimiterStore.tryConsume(userId);
    }
}
