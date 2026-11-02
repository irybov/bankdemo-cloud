package com.github.irybov.admin;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import jakarta.servlet.DispatcherType;

@EnableWebSecurity
@Configuration
public class Security {
	
    private final AdminServerProperties adminServer;
    public Security(AdminServerProperties adminServer) {this.adminServer = adminServer;}
    
    @Value("${spring.boot.admin.ui.public-url}")
    private String uiUrl;
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		final String contextPath = this.adminServer.getContextPath();
		final var path = PathPatternRequestMatcher.withDefaults().basePath(contextPath);
	    final String relativeLoginPageUrl = "/login";
//	    final String absoluteLoginPageUrl = contextPath + relativeLoginPageUrl;
	    final String absoluteLoginPageUrl = this.adminServer.path("/login");
		
	    SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
	    successHandler.setTargetUrlParameter("redirectTo");
	    successHandler.setDefaultTargetUrl(this.adminServer.path("/"));
	    successHandler.setAlwaysUseDefaultTargetUrl(true);
		
		return http.csrf(csrf -> csrf.disable())
				   .authorizeHttpRequests(requests -> requests
						   .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
//						   .requestMatchers(path.matcher("/assets/**")).permitAll()
						   .requestMatchers(this.adminServer.path("/assets/**")).permitAll()
//						   .requestMatchers(path.matcher(relativeLoginPageUrl)).permitAll()
//						   .requestMatchers(relativeLoginPageUrl).permitAll()
						   .requestMatchers(absoluteLoginPageUrl).permitAll()
						   .requestMatchers(RequestMatchers.anyOf(
//								   path.matcher("/login"),
//								   path.matcher(HttpMethod.POST, "/instances"),
								   PathPatternRequestMatcher.pathPattern(HttpMethod.POST, this.adminServer.path("/instances")),
//								   path.matcher(HttpMethod.DELETE, "/instances"),
								   PathPatternRequestMatcher.pathPattern(HttpMethod.DELETE, this.adminServer.path("/instances")),
//								   path.matcher("/actuator/**")
								   PathPatternRequestMatcher.pathPattern(this.adminServer.path("/actuator")),
								   PathPatternRequestMatcher.pathPattern(this.adminServer.path("/actuator/**"))
					            ))
					       .permitAll()
					       .anyRequest().authenticated())
			        .formLogin(login -> login
			        		.loginPage(absoluteLoginPageUrl)
			        		.loginProcessingUrl(absoluteLoginPageUrl)
			        		.successHandler(successHandler)
			        		.permitAll())
			        .logout(logout -> logout
				        	.logoutRequestMatcher(
//				        			path.matcher(HttpMethod.POST, "/logout"))
				        			PathPatternRequestMatcher.pathPattern(HttpMethod.POST, this.adminServer.path("/logout")))
					        .invalidateHttpSession(true)
					        .clearAuthentication(true)
					        .deleteCookies("JSESSIONID")
					        .logoutSuccessUrl(absoluteLoginPageUrl + "?logout"))
	                .exceptionHandling(exception -> exception
	                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(absoluteLoginPageUrl)))
				   .httpBasic(withDefaults())
				   .build();
	}

}
