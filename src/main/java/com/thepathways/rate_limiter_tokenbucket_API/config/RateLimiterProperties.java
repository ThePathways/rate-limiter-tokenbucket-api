package com.thepathways.rate_limiter_tokenbucket_API.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    @Min(1)
    private long capacity;
    @Min(1)
    private long refillTokens;
    @Min(1)
    private long refillIntervalMs;

    private int maxRetries = 10;

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(long refillTokens) {
        this.refillTokens = refillTokens;
    }

    public long getRefillIntervalMs() {
        return refillIntervalMs;
    }

    public void setRefillIntervalMs(long refillIntervalMs) {
        this.refillIntervalMs = refillIntervalMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

}
