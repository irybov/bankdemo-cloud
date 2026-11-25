package com.github.irybov.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RefreshScope
public class TokenConfig {

	@Value("${token.secret}")
	private String secret;

	@PostConstruct
	public void init() {
		if (this.secret == null || this.secret.isBlank()) {
			throw new IllegalStateException("token.secret must not be blank");
		}
	}
	public String getSecret() {return secret;}

}
