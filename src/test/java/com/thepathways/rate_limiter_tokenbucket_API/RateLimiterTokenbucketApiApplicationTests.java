package com.thepathways.rate_limiter_tokenbucket_API;

import com.thepathways.rate_limiter_tokenbucket_API.repository.RateLimitBucketRepository;
import com.thepathways.rate_limiter_tokenbucket_API.service.TokenBucketRateLimiterService;
import com.thepathways.rate_limiter_tokenbucket_API.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimiterTokenbucketApiApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenBucketRateLimiterService rateLimiterService;

	@Autowired
	private RateLimitBucketRepository rateLimitBucketRepository;

	@Autowired
	private MutableTimeProvider timeProvider;

	@BeforeEach
	void setUp() {
		rateLimitBucketRepository.deleteAll();
		timeProvider.setCurrentTimeMillis(1_000_000L);
	}

	@Test
	void rateLimitingIsAppliedPerUser() throws Exception {
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(get("/api/request").header("X-User-Id", "test-user"))
					.andExpect(status().isOk())
					.andExpect(content().string("Request Successful"));
		}

		mockMvc.perform(get("/api/request").header("X-User-Id", "test-user"))
				.andExpect(status().isTooManyRequests())
				.andExpect(content().string("Too Many Requests"));

		mockMvc.perform(get("/api/request").header("X-User-Id", "another-user"))
				.andExpect(status().isOk())
				.andExpect(content().string("Request Successful"));
	}

	@Test
	void mixedUserTrafficDoesNotShareBuckets() {
		for (int i = 0; i < 4; i++) {
			assertTrue(rateLimiterService.allowRequest("user1"));
		}

		for (int i = 0; i < 2; i++) {
			assertTrue(rateLimiterService.allowRequest("user2"));
		}

		assertTrue(rateLimiterService.allowRequest("user1"));
		assertEquals(false, rateLimiterService.allowRequest("user1"));
		assertTrue(rateLimiterService.allowRequest("user2"));
		assertTrue(rateLimiterService.allowRequest("user2"));
		assertTrue(rateLimiterService.allowRequest("user2"));
		assertEquals(false, rateLimiterService.allowRequest("user2"));
	}

	@Test
	void tokensAreRefilledAfterInterval() {
		String userId = "refill-user";

		for (int i = 0; i < 5; i++) {
			assertTrue(rateLimiterService.allowRequest(userId));
		}
		assertEquals(false, rateLimiterService.allowRequest(userId));

		timeProvider.setCurrentTimeMillis(1_060_000L);

		for (int i = 0; i < 5; i++) {
			assertTrue(rateLimiterService.allowRequest(userId));
		}
		assertEquals(false, rateLimiterService.allowRequest(userId));
	}

	@Test
	void concurrentRequestsStillRespectCapacity() throws Exception {
		String userId = "concurrent-user";
		int requestCount = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
		CountDownLatch ready = new CountDownLatch(requestCount);
		CountDownLatch start = new CountDownLatch(1);

		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int i = 0; i < requestCount; i++) {
				futures.add(executorService.submit(concurrentRequest(userId, ready, start)));
			}

			assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();

			int successCount = 0;
			for (Future<Boolean> future : futures) {
				if (future.get(5, TimeUnit.SECONDS)) {
					successCount++;
				}
			}

			assertEquals(5, successCount);
			assertEquals(5, requestCount - successCount);
		}
		finally {
			executorService.shutdownNow();
		}
	}

	private Callable<Boolean> concurrentRequest(String userId, CountDownLatch ready, CountDownLatch start) {
		return () -> {
			ready.countDown();
			if (!start.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Concurrent test did not start in time");
			}
			return rateLimiterService.allowRequest(userId);
		};
	}

	@TestConfiguration
	static class TestTimeConfiguration {

		@Bean
		@Primary
		MutableTimeProvider timeProvider() {
			return new MutableTimeProvider();
		}
	}

	static class MutableTimeProvider implements TimeProvider {

		private final AtomicLong currentTimeMillis = new AtomicLong();

		@Override
		public long currentTimeMillis() {
			return currentTimeMillis.get();
		}

		void setCurrentTimeMillis(long currentTimeMillis) {
			this.currentTimeMillis.set(currentTimeMillis);
		}
	}

}
