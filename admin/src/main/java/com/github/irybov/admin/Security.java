package com.github.irybov.admin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@EnableWebSecurity
@Configuration
public class Security {
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
	    SavedRequestAwareAuthenticationSuccessHandler successHandler 
	        = new SavedRequestAwareAuthenticationSuccessHandler();
	    successHandler.setTargetUrlParameter("redirectTo");
	    successHandler.setDefaultTargetUrl("/");
	
	    http
	    	.authorizeHttpRequests(urlConfig -> urlConfig
		            .requestMatchers("/assets/**").permitAll()
		            .requestMatchers("/login").permitAll().anyRequest().authenticated()
//		            .antMatchers("/actuator/**").hasRole("ADMIN"))
		            )
	        .formLogin(login -> login
	        		.loginPage("/login")
	        		.successHandler(successHandler))
	        .logout(logout -> logout
	        		.logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/logout"))
		            .invalidateHttpSession(true)
		            .clearAuthentication(true)
		            .deleteCookies("JSESSIONID")
		            .logoutSuccessUrl("/login"))
	        .httpBasic(Customizer.withDefaults())
	        .csrf(csrf -> csrf
			.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
			.ignoringRequestMatchers("/instances", "/instances/*", "/actuator/**"));
		
		return http.build();
	}

}
