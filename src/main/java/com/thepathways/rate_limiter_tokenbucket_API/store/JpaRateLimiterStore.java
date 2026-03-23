package com.thepathways.rate_limiter_tokenbucket_API.store;

import com.thepathways.rate_limiter_tokenbucket_API.config.RateLimiterProperties;
import com.thepathways.rate_limiter_tokenbucket_API.entity.RateLimitBucketEntity;
import com.thepathways.rate_limiter_tokenbucket_API.model.TokenBucket;
import com.thepathways.rate_limiter_tokenbucket_API.repository.RateLimitBucketRepository;
import com.thepathways.rate_limiter_tokenbucket_API.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaRateLimiterStore implements RateLimiterStore {

    private final RateLimitBucketRepository rateLimitBucketRepository;
    private final RateLimiterProperties rateLimiterProperties;
    private final TimeProvider timeProvider;

    @Override
    public boolean tryConsume(String userId) {
        int MAX_RETRIES = rateLimiterProperties.getMaxRetries();
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            long now = timeProvider.currentTimeMillis();
            RateLimitBucketEntity bucketEntity = rateLimitBucketRepository.findById(userId)
                    .orElseGet(() -> new RateLimitBucketEntity(userId, rateLimiterProperties.getCapacity(), now));

            TokenBucket bucket = new TokenBucket(
                    rateLimiterProperties.getCapacity(),
                    rateLimiterProperties.getRefillTokens(),
                    rateLimiterProperties.getRefillIntervalMs(),
                    bucketEntity.getAvailableTokens(),
                    bucketEntity.getLastRefillTimestamp());

            boolean allowed = bucket.tryConsume(now);
            bucketEntity.setAvailableTokens(bucket.getTokens());
            bucketEntity.setLastRefillTimestamp(bucket.getLastRefillTimestamp());

            try {
                rateLimitBucketRepository.saveAndFlush(bucketEntity);
                return allowed;
            }
            catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException ex) {
                if (attempt == MAX_RETRIES - 1) {
                    throw ex;
                }
            }
        }

        return false;
    }
}
