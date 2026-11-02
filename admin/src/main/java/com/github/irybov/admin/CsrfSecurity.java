package com.github.irybov.admin;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.ForwardedHeaderFilter;

import de.codecentric.boot.admin.server.config.AdminServerProperties;

//@EnableWebSecurity
//@Configuration
public class CsrfSecurity {
	
    private final AdminServerProperties adminServer;
    public CsrfSecurity(AdminServerProperties adminServer) {this.adminServer = adminServer;}
    
//    @Value("${server.servlet.context-path:#{''}}")
//    private String contextPath;
    @Value("${spring.boot.admin.ui.public-url}")
    private String uiUrl;
    
//	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		String loginPageUrl = uiUrl+"login";
		var path = PathPatternRequestMatcher.withDefaults();
		
	    SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
	    successHandler.setTargetUrlParameter("redirectTo");
	    successHandler.setDefaultTargetUrl("/");
//	    successHandler.setDefaultTargetUrl(uiUrl);
	    successHandler.setAlwaysUseDefaultTargetUrl(true);
	    
//        XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
//        requestHandler.setCsrfRequestAttributeName(null);
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
	
	    http
	    	.requestCache(cache -> cache.requestCache(new NullRequestCache()))
	    	.authorizeHttpRequests(urlConfig -> urlConfig
//		        .requestMatchers(this.adminServer.path(contextPath+"/assets/**")).permitAll()
		        .requestMatchers(this.adminServer.path("/assets/**")).permitAll()
//		        .requestMatchers(this.adminServer.path(contextPath+"/login")).permitAll().anyRequest().authenticated())
	    		.requestMatchers(this.adminServer.path("/login")).permitAll().anyRequest().authenticated())
//		        .antMatchers("/actuator/**").hasRole("ADMIN"))
	    	.exceptionHandling(exception -> exception
	    		.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(loginPageUrl)))
	        .formLogin(login -> login.loginPage(loginPageUrl).successHandler(successHandler))
	        .logout(logout -> logout
	        	.logoutRequestMatcher(
//	        		PathPatternRequestMatcher.pathPattern(HttpMethod.POST, this.adminServer.path(contextPath+"/logout")))
	        		PathPatternRequestMatcher.pathPattern(HttpMethod.POST, this.adminServer.path("/logout")))
		        .invalidateHttpSession(true)
		        .clearAuthentication(true)
		        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
		        .logoutSuccessUrl(loginPageUrl+"?logout"))
	        .httpBasic(Customizer.withDefaults())
//	        .csrf(csrf -> csrf.csrfTokenRepository(repository).csrfTokenRequestHandler(requestHandler)
	        .csrf(csrf -> csrf.csrfTokenRepository(repository)
	        		.ignoringRequestMatchers(
//            		PathPatternRequestMatcher.withDefaults().matcher(this.adminServer.path("/login")),
//            		PathPatternRequestMatcher.withDefaults().matcher(
//            				this.adminServer.path(contextPath+"/assets/**")),
	        				path.matcher(
//            			HttpMethod.POST, this.adminServer.path(contextPath+"/instances")),
            			HttpMethod.POST, this.adminServer.path("/instances")),
	        				path.matcher(
//            			HttpMethod.DELETE, this.adminServer.path(contextPath+"/instances/*")),
            			HttpMethod.DELETE, this.adminServer.path("/instances/*")),
	        				path.matcher(
//            			this.adminServer.path(contextPath+"/actuator/**"))));
	    				this.adminServer.path("/actuator/**"))));
//            .csrf(csrf -> csrf.disable());
//            .rememberMe(rememberMe -> rememberMe.key(UUID.randomUUID().toString()).tokenValiditySeconds(1209600));
		return http.build();
	}

	/*
	 * @Bean public WebSecurityCustomizer webSecurityCustomizer() { return (web) ->
	 * web.ignoring() .requestMatchers(HttpMethod.POST,
	 * this.adminServer.path(contextPath+"/instances"))
	 * .requestMatchers(HttpMethod.DELETE,
	 * this.adminServer.path(contextPath+"/instances/*"))
	 * .requestMatchers(this.adminServer.path(contextPath+"/actuator/**")); }
	 */
//	@Bean
	public ForwardedHeaderFilter forwardedHeaderFilter() {
	    return new ForwardedHeaderFilter();
	}
	/*
	 * @Bean public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>
	 * cookieProcessorCustomizer() { return factory ->
	 * factory.addInitializers(servletContext -> {
	 * servletContext.getSessionCookieConfig().setPath("/admin"); }); }
	 */
}
