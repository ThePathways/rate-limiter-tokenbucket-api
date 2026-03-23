package com.thepathways.rate_limiter_tokenbucket_API.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "rate_limit_bucket")
public class RateLimitBucketEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Column(name = "available_tokens", nullable = false)
    private long availableTokens;

    @Column(name = "last_refill_timestamp", nullable = false)
    private long lastRefillTimestamp;

    @Version
    @Column(name = "version")
    private Long version;

    protected RateLimitBucketEntity() {
    }

    public RateLimitBucketEntity(String userId, long availableTokens, long lastRefillTimestamp) {
        this.userId = userId;
        this.availableTokens = availableTokens;
        this.lastRefillTimestamp = lastRefillTimestamp;
    }

    public String getUserId() {
        return userId;
    }

    public long getAvailableTokens() {
        return availableTokens;
    }

    public void setAvailableTokens(long availableTokens) {
        this.availableTokens = availableTokens;
    }

    public long getLastRefillTimestamp() {
        return lastRefillTimestamp;
    }

    public void setLastRefillTimestamp(long lastRefillTimestamp) {
        this.lastRefillTimestamp = lastRefillTimestamp;
    }
}
