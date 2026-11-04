package com.github.irybov.admin;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import jakarta.servlet.DispatcherType;

@EnableWebSecurity
@Configuration
public class Security {
	
    private final AdminServerProperties adminServer;
    public Security(AdminServerProperties adminServer) {this.adminServer = adminServer;}
    
//    @Value("${spring.boot.admin.ui.public-url}")
//    private String dashBoardUrl;
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
	    final String loginPageUrl = "/login";
		
	    SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
	    successHandler.setTargetUrlParameter("redirectTo");
	    successHandler.setDefaultTargetUrl(this.adminServer.path("/"));
//	    successHandler.setAlwaysUseDefaultTargetUrl(true);
	    
	    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
//	    repository.setCookieName("XSRF-TOKEN");
//	    repository.setHeaderName("X-XSRF-TOKEN");
//	    repository.setParameterName("_csrf");
		
		return http
				.addFilterAfter(new CsrfFilter(), BasicAuthenticationFilter.class)
				.csrf(csrf -> csrf
//					.disable())
					.csrfTokenRepository(repository)
		            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
		            .ignoringRequestMatchers(
		                PathPatternRequestMatcher.withDefaults()
		                    .matcher(HttpMethod.POST, this.adminServer.path("/instances")),
		                PathPatternRequestMatcher.withDefaults()
		                    .matcher(HttpMethod.DELETE, this.adminServer.path("/instances/*")),
		                PathPatternRequestMatcher.withDefaults()
		                    .matcher(HttpMethod.POST, this.adminServer.path("/notifications/**")),
		                PathPatternRequestMatcher.withDefaults()
		                    .matcher(HttpMethod.DELETE, this.adminServer.path("/notifications/**")),
		                PathPatternRequestMatcher.withDefaults()
		                    .matcher(this.adminServer.path("/actuator/**"))
		            ))
				    .authorizeHttpRequests(requests -> requests
//						   .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
						   .requestMatchers(this.adminServer.path("/assets/**")).permitAll()
						   .requestMatchers(this.adminServer.path(loginPageUrl)).permitAll()
//						   .requestMatchers(RequestMatchers.anyOf(
////								   path.matcher("/login"),
////								   path.matcher(HttpMethod.POST, "/instances"),
//								   PathPatternRequestMatcher.withDefaults()
//			                        .matcher(HttpMethod.POST, this.adminServer.path("/instances")),
////								   path.matcher(HttpMethod.DELETE, "/instances"),
//								   PathPatternRequestMatcher.withDefaults()
//			                        .matcher(HttpMethod.DELETE, this.adminServer.path("/instances/*")),
////								   path.matcher("/actuator/**")
////								   PathPatternRequestMatcher.pathPattern(this.adminServer.path("/actuator")),
//								   PathPatternRequestMatcher.withDefaults()
//			                        .matcher(this.adminServer.path("/actuator/**"))
//					            ))
//					       .authenticated()
//						   .permitAll()
//					       .requestMatchers(
//					    		   PathPatternRequestMatcher.withDefaults()
//					    		   .matcher(this.adminServer.path("/actuator/**"))).authenticated()
					       .anyRequest().authenticated())
			        .formLogin(login -> login
			        		.loginPage(this.adminServer.path(loginPageUrl))
			        		.successHandler(successHandler))
			        .logout(logout -> logout
				        	.logoutRequestMatcher(
				        			PathPatternRequestMatcher.withDefaults()
			                        .matcher(HttpMethod.POST, this.adminServer.path("/logout")))
					        .invalidateHttpSession(true)
					        .clearAuthentication(true)
					        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
					        .logoutSuccessUrl(this.adminServer.path(loginPageUrl) + "?logout"))
				    .httpBasic(withDefaults())
		            .rememberMe(rememberMe -> rememberMe.key(UUID.randomUUID().toString()).tokenValiditySeconds(1209600))
				    .build();
	}

}
