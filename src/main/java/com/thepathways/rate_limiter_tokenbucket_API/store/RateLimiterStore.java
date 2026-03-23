package com.thepathways.rate_limiter_tokenbucket_API.store;

public interface RateLimiterStore {

    boolean tryConsume(String userId);
}
