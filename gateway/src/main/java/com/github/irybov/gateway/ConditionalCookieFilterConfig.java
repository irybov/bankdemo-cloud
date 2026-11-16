package com.github.irybov.gateway;

import java.util.Set;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Configuration
public class ConditionalCookieFilterConfig {

    private static final Set<String> EXCLUDED_ROUTE_IDS = Set.of("admin-static");

    @Bean
    public GlobalFilter removeCookieFilter() {
        return (exchange, chain) -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (route != null && EXCLUDED_ROUTE_IDS.contains(route.getId())) {
                return chain.filter(exchange);
            }
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .headers(headers -> headers.remove(HttpHeaders.COOKIE))
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }
}
