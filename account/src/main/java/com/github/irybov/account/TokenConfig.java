package com.github.irybov.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Getter
@Component
@RefreshScope
public class TokenConfig {

	@Value("${token.secret}")
	private String secret;

	@Value("${token.lifetime:300}")
	private long tokenLifetimeSeconds;

	@PostConstruct
	public void init() {
		if (this.secret == null || this.secret.isBlank()) {
			throw new IllegalStateException("token.secret must not be blank");
		}
		if (this.tokenLifetimeSeconds <= 0) {
			throw new IllegalStateException("token.lifetime must be positive");
		}
	}

}
