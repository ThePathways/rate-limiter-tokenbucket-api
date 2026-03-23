package com.thepathways.rate_limiter_tokenbucket_API.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thepathways.rate_limiter_tokenbucket_API.entity.RateLimitBucketEntity;

public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucketEntity, String> {
}
