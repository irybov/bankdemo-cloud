package com.github.irybov.operation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import feign.Client;
import feign.okhttp.OkHttpClient;

//@Configuration
public class FeignConfig {
	
    @Bean
    @Primary
    public Client client() {
        return new OkHttpClient();
    }
}
