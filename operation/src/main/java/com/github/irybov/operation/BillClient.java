package com.github.irybov.operation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;

@HttpExchange("/bills")
public interface BillClient {

    @Retryable(
            includes = {
                    HttpServerErrorException.class,               // 500, 502, 503, 504, etc.
                    HttpClientErrorException.TooManyRequests.class, // 429
                    ResourceAccessException.class                 // Network / read timeouts
                },
            maxRetries = 2,
            delay = 500,
            multiplier = 2.0,
            jitter = 100,
            timeout = 5000,
            timeUnit = TimeUnit.MILLISECONDS
        )
    @PatchExchange
	public void updateBalance(@RequestHeader("Idempotency-Key") UUID eventId, @RequestBody Map<Integer, Double> data);
}
