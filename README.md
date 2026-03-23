# Rate Limiter Token Bucket API

Spring Boot API that demonstrates a per-user token bucket rate limiter.

The application exposes one endpoint:

- `GET /api/request`

Each request must include:

- header: `X-User-Id`

Current behavior:

- each user gets a separate bucket
- bucket capacity: `5`
- refill tokens: `5`
- refill interval: `60000 ms`
- first 5 requests for a user succeed
- 6th request for the same user returns `429 Too Many Requests`

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- H2 in-memory database
- Maven Wrapper
- JUnit 5
- MockMvc

## High-Level Flow

Request flow:

```text
Client
  -> RateLimiterController
  -> TokenBucketRateLimiterService
  -> RateLimiterStore
  -> JpaRateLimiterStore
  -> RateLimitBucketRepository
  -> H2 table: rate_limit_bucket
```

Logic flow:

1. Client calls `GET /api/request` with `X-User-Id`.
2. Controller forwards the user ID to the service.
3. Service delegates to the `RateLimiterStore` abstraction.
4. Current store implementation is `JpaRateLimiterStore`.
5. Store reads that user's bucket state from the database.
6. `TokenBucket` applies refill logic based on time.
7. If a token is available, one token is consumed and request is allowed.
8. Updated bucket state is saved back to the database.
9. Response is returned:
   - `200 Request Successful`
   - or `429 Too Many Requests`

## Why This Design

This project was intentionally refactored so the service does not know where bucket state is stored.

Current implementation:

- `RateLimiterStore` is the abstraction
- `JpaRateLimiterStore` is the H2/JPA-backed implementation

This makes it easier to switch later to:

- MySQL with JPA
- Redis for distributed rate limiting

The controller and service do not need major changes for that future move.

## Project Structure

```text
rate-limiter-tokenbucket-API/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── README.md
└── src
    ├── main
    │   ├── java/com/thepathways/rate_limiter_tokenbucket_API
    │   │   ├── RateLimiterTokenbucketApiApplication.java
    │   │   ├── config
    │   │   │   └── RateLimiterProperties.java
    │   │   ├── controller
    │   │   │   └── RateLimiterController.java
    │   │   ├── entity
    │   │   │   └── RateLimitBucketEntity.java
    │   │   ├── model
    │   │   │   └── TokenBucket.java
    │   │   ├── repository
    │   │   │   └── RateLimitBucketRepository.java
    │   │   ├── service
    │   │   │   └── TokenBucketRateLimiterService.java
    │   │   ├── store
    │   │   │   ├── JpaRateLimiterStore.java
    │   │   │   └── RateLimiterStore.java
    │   │   └── time
    │   │       ├── SystemTimeProvider.java
    │   │       └── TimeProvider.java
    │   └── resources
    │       └── application.properties
    └── test
        └── java/com/thepathways/rate_limiter_tokenbucket_API
            └── RateLimiterTokenbucketApiApplicationTests.java
```

Folder purpose:

- `controller`: exposes REST endpoints
- `service`: application/business layer
- `store`: storage abstraction and current JPA-backed implementation
- `repository`: Spring Data JPA repository
- `entity`: database-mapped bucket state
- `model`: token bucket domain logic
- `config`: typed application configuration
- `time`: time abstraction used to make refill logic testable
- `resources`: runtime properties
- `test`: integration and behavior tests

## Configuration

Current configuration is in `src/main/resources/application.properties`:

```properties
server.port=8080

rate-limiter.capacity=5
rate-limiter.refill-tokens=5
rate-limiter.refill-interval-ms=60000

spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## How Token Refill Works

This implementation uses lazy refill.

That means:

- there is no background scheduler
- there is no cron job
- refill is checked only when a new request comes in

Example:

- user has exhausted all 5 tokens
- next request comes before 60 seconds: blocked
- next request comes after 60 seconds: bucket refills and request can succeed

## Build And Run

Clean build:

```bash
./mvnw clean verify
```

Run the app:

```bash
java -jar target/rate-limiter-tokenbucket-API-0.0.1-SNAPSHOT.jar
```

Or use:

```bash
./mvnw spring-boot:run
```

## API Usage

### 1. Single request

```bash
curl -i -H "X-User-Id: demo-user" http://127.0.0.1:8080/api/request
```

Expected response:

```text
HTTP/1.1 200
Request Successful
```

### 2. Hit the limit with one user

```bash
for i in 1 2 3 4 5 6
do
  curl -i -H "X-User-Id: user1" http://127.0.0.1:8080/api/request
done
```

Expected:

- requests `1..5` -> `200`
- request `6` -> `429`

### 3. Mixed users do not share the same bucket

```bash
curl -i -H "X-User-Id: user1" http://127.0.0.1:8080/api/request
curl -i -H "X-User-Id: user1" http://127.0.0.1:8080/api/request
curl -i -H "X-User-Id: user1" http://127.0.0.1:8080/api/request
curl -i -H "X-User-Id: user1" http://127.0.0.1:8080/api/request
curl -i -H "X-User-Id: user2" http://127.0.0.1:8080/api/request
curl -i -H "X-User-Id: user2" http://127.0.0.1:8080/api/request
```

Expected:

- all 6 requests succeed
- because the limit is per user, not global

### 4. H2 console

Open:

```text
http://127.0.0.1:8080/h2-console
```

Use:

- JDBC URL: `jdbc:h2:mem:testdb`
- username: `sa`
- password: `password`

You can inspect the table:

```sql
select * from rate_limit_bucket;
```

## Automated Test Cases

Current automated tests cover:

1. Per-user rate limiting
   - first 5 requests succeed
   - 6th request fails with `429`
   - another user still succeeds

2. Mixed-user traffic isolation
   - total requests across users can exceed 5
   - no blocking happens unless an individual user crosses their own limit

3. Refill behavior
   - user exhausts the bucket
   - simulated time moves forward by 60 seconds
   - bucket refills and requests succeed again

4. Concurrency
   - 10 concurrent requests for the same user
   - exactly 5 succeed
   - exactly 5 fail

Run tests:

```bash
./mvnw test
```

## Current Storage Model

Current bucket state is stored in the `rate_limit_bucket` table.

Columns:

- `user_id`
- `available_tokens`
- `last_refill_timestamp`
- `version`

The `version` column is used for optimistic locking support during concurrent updates.

## Production Notes

This project is good for demonstrating the design and core algorithm, but it is still a learning/demo implementation.

Current limitations:

- H2 is in-memory, so state is lost on restart
- this is not yet a distributed limiter
- for real multi-instance deployments, Redis is usually a better backend
- `spring.jpa.hibernate.ddl-auto=update` is okay for demo, but migrations are better for production

## Future Improvement Path

### Move to MySQL

Minimal conceptual changes:

- change datasource configuration
- keep `JpaRateLimiterStore`
- keep repository/entity model

### Move to Redis

Main changes:

- add `RedisRateLimiterStore implements RateLimiterStore`
- switch active store implementation
- use atomic Redis operations or Lua scripting
- keep controller and service flow the same

Future Redis flow:

```text
Client
  -> Controller
  -> Service
  -> RateLimiterStore
  -> RedisRateLimiterStore
  -> Redis
```

## Sample Outcomes

Single user:

```text
request=1 status=200 body=Request Successful
request=2 status=200 body=Request Successful
request=3 status=200 body=Request Successful
request=4 status=200 body=Request Successful
request=5 status=200 body=Request Successful
request=6 status=429 body=Too Many Requests
```

Mixed users:

```text
request=1 user=user1 status=200 body=Request Successful
request=2 user=user1 status=200 body=Request Successful
request=3 user=user1 status=200 body=Request Successful
request=4 user=user1 status=200 body=Request Successful
request=5 user=user2 status=200 body=Request Successful
request=6 user=user2 status=200 body=Request Successful
```

## Summary

This project demonstrates:

- token bucket rate limiting
- per-user bucket isolation
- configurable rate-limiter policy
- clean storage abstraction
- H2/JPA-backed persistence today
- easier migration path to MySQL or Redis tomorrow
