package com.thepathways.rate_limiter_tokenbucket_API.controller;

import com.thepathways.rate_limiter_tokenbucket_API.service.TokenBucketRateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RateLimiterController {

    private final TokenBucketRateLimiterService rateLimiterService;

    @GetMapping("/request")
    public ResponseEntity<String> makeRequest(@RequestHeader("X-User-Id") String userId) {
        boolean allowed = rateLimiterService.allowRequest(userId);

        if (!allowed) {
            return ResponseEntity.status(429).body("Too Many Requests");
        }

        return ResponseEntity.ok("Request Successful");
    }
}
