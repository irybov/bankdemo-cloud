package com.github.irybov.admin;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
public class Security {
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
	    SavedRequestAwareAuthenticationSuccessHandler successHandler 
	        = new SavedRequestAwareAuthenticationSuccessHandler();
	    successHandler.setTargetUrlParameter("redirectTo");
	    successHandler.setDefaultTargetUrl("/");
	
	    http
	    	.authorizeHttpRequests(urlConfig -> urlConfig
		            .antMatchers("/assets/**").permitAll()
		            .mvcMatchers("/login").permitAll().anyRequest().authenticated()
//		            .antMatchers("/actuator/**").hasRole("ADMIN"))
		            )
	        .formLogin(login -> login
	        		.loginPage("/login")
	        		.successHandler(successHandler))
	        .logout(logout -> logout
	        		.logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
		            .invalidateHttpSession(true)
		            .clearAuthentication(true)
		            .deleteCookies("JSESSIONID")
		            .logoutSuccessUrl("/login"))
	        .httpBasic(Customizer.withDefaults())
	        .csrf()
	        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
	        .ignoringAntMatchers("/instances", "/instances/*", "/actuator/**");
		
		return http.build();
	}

}
