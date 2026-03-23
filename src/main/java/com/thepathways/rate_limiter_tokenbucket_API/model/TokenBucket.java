package com.thepathways.rate_limiter_tokenbucket_API.model;

public class TokenBucket {

    private final long capacity;
    private final long refillIntervalMs;
    private final long refillTokens;
    private long tokens;
    private long lastRefillTimestamp;

    public TokenBucket(long capacity, long refillTokens, long refillIntervalMs, long tokens, long lastRefillTimestamp) {
        if (capacity <= 0 || refillTokens <= 0 || refillIntervalMs <= 0) {
            throw new IllegalArgumentException("Rate limiter values must be greater than zero");
        }
        if (tokens < 0 || tokens > capacity) {
            throw new IllegalArgumentException("Token count must be between zero and capacity");
        }
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMs = refillIntervalMs;
        this.tokens = tokens;
        this.lastRefillTimestamp = lastRefillTimestamp;
    }

    public static TokenBucket newBucket(long capacity, long refillTokens, long refillIntervalMs, long now) {
        return new TokenBucket(capacity, refillTokens, refillIntervalMs, capacity, now);
    }

    public boolean tryConsume(long now) {
        refill(now);
        if (tokens > 0) {
            tokens--;
            return true;
        }
        return false;
    }

    private void refill(long now) {
        long elapsed = now - lastRefillTimestamp;

        if (elapsed >= refillIntervalMs) {
            long intervals = elapsed / refillIntervalMs;
            long newTokens = Math.min(capacity, tokens + intervals * refillTokens);
            tokens = newTokens;
            lastRefillTimestamp += intervals * refillIntervalMs;
        }
    }

    public long getTokens() {
        return tokens;
    }

    public long getLastRefillTimestamp() {
        return lastRefillTimestamp;
    }
}
