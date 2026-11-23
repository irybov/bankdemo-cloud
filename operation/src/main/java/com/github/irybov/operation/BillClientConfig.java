package com.github.irybov.operation;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.NoSuchElementException;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

import tools.jackson.databind.ObjectMapper;

//@Configuration(proxyBeanMethods = false)
public class BillClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer billingGroupConfigurer(
    		ObjectMapper objectMapper, 
    		LoadBalancerInterceptor loadBalancerInterceptor) {
    	
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofMillis(500))
                .withReadTimeout(Duration.ofSeconds(1));
    	
        return groups -> groups
            .filterByName("bill")
            .forEachClient((group, builder) -> builder
                .baseUrl("http://bill")
                .requestInterceptor(loadBalancerInterceptor)
                .requestFactory(ClientHttpRequestFactoryBuilder.httpComponents().build(settings))
            	.defaultStatusHandler(
                HttpStatusCode::isError,
                (request, response) -> {
                    int status = response.getStatusCode().value();
                    byte[] body = response.getBody().readAllBytes();

                    // Optional: deserialize structured ProblemDetail / error payload
                    // ApiError error = objectMapper.readValue(body, ApiError.class);

                    switch (status) {
	                    case 404 -> throw new NoSuchElementException("Bill not found");
	                    case 400, 422 -> throw HttpClientErrorException.create(
	                        response.getStatusCode(),
	                        response.getStatusText(),
	                        response.getHeaders(),
	                        response.getBody().readAllBytes(),
	                        StandardCharsets.UTF_8
	                    );
	                    case 429, 502, 503, 504 -> throw HttpServerErrorException.create(
	                        response.getStatusCode(),
	                        response.getStatusText(),
	                        response.getHeaders(),
	                        response.getBody().readAllBytes(),
	                        StandardCharsets.UTF_8
	                    );
	                    default -> throw new UnknownHttpStatusCodeException(
	                        status,
	                        response.getStatusText(),
	                        response.getHeaders(),
	                        response.getBody().readAllBytes(),
	                        StandardCharsets.UTF_8
	                    );
                    }
                }
            ));
    }
	
}
