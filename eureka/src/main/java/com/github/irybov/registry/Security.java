package com.github.irybov.registry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration
public class Security {
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		return http.csrf(csrf -> csrf.disable())
				   .authorizeHttpRequests(requests -> requests
					       .requestMatchers("/eureka/css/**", "/eureka/js/**", "/eureka/fonts/**", "/eureka/images/**")
					       .permitAll()
					       .anyRequest().authenticated())
				   .httpBasic(withDefaults())
				   .build();
	}

}
