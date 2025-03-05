package com.github.irybov.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.SecretKey;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
public class AppIT {
/*	
	@Autowired
	private RestTemplate restTemplate;
	@TestConfiguration
	static class RestTemplateConfig {
		@Bean
	    @Primary
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}		
	}
*/	
	@Autowired
	private TestRestTemplate testRestTemplate;
	@Autowired
	private ObjectMapper mapper;
	@Autowired
	private Environment env;
	
	@Autowired
	private DataSource dataSource;
	private ResourceDatabasePopulator populator;
//	private MockRestServiceServer mockServer;
	
	@Value("${server.address}")
	private String uri;
	@Value("${local.server.port}")
	private int port;
	
	@Value("${app.internal-url}")
	private static String internalURL;
	private static WireMockServer wireMockServer;
	
	@BeforeAll
	void prepare() {		
		populator = new ResourceDatabasePopulator();
		populator.addScripts(new ClassPathResource("test-accounts-h2.sql"));
		populator.execute(dataSource);
//		mockServer = MockRestServiceServer.createServer(restTemplate);
		wireMockServer = new WireMockServer(new WireMockConfiguration().port(8761));
		wireMockServer.start();
		WireMock.configureFor(internalURL, 8761);
	}
	
	@Test
	void context_loading(ApplicationContext context) {
		assertThat(context).isNotNull();
		
		String path = "http://"+uri+":"+port;
		
		ResponseEntity<Void> response = 
				testRestTemplate.getForEntity(path + "/swagger-ui/", Void.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
        
        response = 
				testRestTemplate.getForEntity(path + "/v3/api-docs", Void.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}
	
	@Test
	void can_create() {
		
		Registration registration = new Registration();
		registration.setName("Kylie");
		registration.setSurname("Bunbury");
		registration.setPhone("4444444444");
		registration.setEmail("bunbury@greenmail.io");
		registration.setBirthday(LocalDate.of(1989, 01, 30));
		registration.setPassword("blackmamba");
		
		HttpEntity<Registration> data = new HttpEntity<>(registration);		
		ResponseEntity<Void> response = 
				testRestTemplate.exchange("/accounts", HttpMethod.POST, data, Void.class);
		assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
	}
	
	@Test
	void fail_to_create() {
		
		Registration registration = new Registration();
		registration.setName("a");
		registration.setSurname("");
		registration.setPhone("aaaaaaaaaa");
		registration.setEmail("a#mail.io");
		registration.setBirthday(LocalDate.of(2111, 01, 01));
		registration.setPassword("superb");
		
		HttpEntity<Registration> data = new HttpEntity<>(registration);		
		ResponseEntity<List<String>> violations = 
				testRestTemplate.exchange("/accounts", HttpMethod.POST, data, 
						new ParameterizedTypeReference<List<String>>(){});
		
        assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(violations.getBody().size(), is(10));
        assertThat(violations.getBody().contains("Name should be 2-20 chars length"), is(true));
        assertThat(violations.getBody().contains("Please input name like Xx"), is(true));
        assertThat(violations.getBody().contains("Surname must not be blank"), is(true));
        assertThat(violations.getBody().contains("Surname should be 2-40 chars length"), is(true));
        assertThat(violations.getBody().contains("Please input surname like Xx or Xx-Xx"), is(true));
        assertThat(violations.getBody().contains("Please input phone number like a row of 10 digits"), is(true));
        assertThat(violations.getBody().contains("Email address is not valid"), is(true));
        assertThat(violations.getBody().contains("Email address should be 10-60 symbols length"), is(true));
        assertThat(violations.getBody().contains("Birthday can't be future time"), is(true));
        assertThat(violations.getBody().contains("Password should be 10-60 symbols length"), is(true));
	}
	
	@Test
	void can_get_token() {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Login", "3333333333:gingerchick");
		HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);
		
		ResponseEntity<Void> response = 
				testRestTemplate.exchange("/accounts", HttpMethod.HEAD, entity, Void.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getHeaders().containsKey("Token"), is(true));
		

		String jwt = response.getHeaders().get("Token").get(0);
		String tokenSecret = env.getProperty("token.secret");
		byte[] secretKeyBytes = tokenSecret.getBytes();
		SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyBytes);
		
		JwtParser parser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
		Jws<Claims> parsedToken = 
				parser.parseSignedClaims(jwt);
		Collection<String> scopes = 
				((Claims) parsedToken.getPayload()).get("scope", Collection.class);
		
		assertThat(scopes.size(), is(2));
	}
	
	@Test
	void try_bad_login() {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Login", "333333:ginger");
		HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);
		
		ResponseEntity<List<String>> violations = 
				testRestTemplate.exchange("/accounts", HttpMethod.HEAD, entity, 
						new ParameterizedTypeReference<List<String>>(){});
		assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
		assertThat(violations.getHeaders().containsKey("Token"), is(false));
//		assertThat(violations.getBody().size(), is(1));
//		assertThat(violations.getBody().contains("Header should match pattern"), is(true));
	}
	
	@Test
	void try_wrong_password() {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Login", "3333333333:gingerfreak");
		HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);
		
		ResponseEntity<String> response = 
				testRestTemplate.exchange("/accounts", HttpMethod.HEAD, entity, String.class);
		assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
		assertThat(response.getHeaders().containsKey("Token"), is(false));
//		assertThat(response.getBody().contains("Wrong password provided"), is(true));
	}
	
	@Test
	void can_get_one() throws JsonProcessingException, URISyntaxException {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> bills = Stream.generate(() -> new BillDTO()).limit(size)
				.collect(Collectors.toList());
		int i = 1;
		for(BillDTO bill : bills) {bill.setId(new Integer(i++));}
/*		
	    mockServer.expect(ExpectedCount.once(), requestTo(new URI("http://BILL/bills/2/list")))
	    .andExpect(method(HttpMethod.GET))
	    .andRespond(withStatus(HttpStatus.OK)
	    .contentType(MediaType.APPLICATION_JSON)
	    .body(mapper.writeValueAsString(bills)));
*/	    
	    String requestURI = "/bills/2/list";
	    wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.OK.value())
				.withBody(mapper.writeValueAsString(bills))
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
/*	    
		HttpHeaders headers = new HttpHeaders();
		headers.set("Login", "1111111111:supervixen");
		HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);		
		ResponseEntity<Void> result = 
				testRestTemplate.exchange("/accounts/login", HttpMethod.HEAD, entity, Void.class);
		String jwt = result.getHeaders().get("Token").get(0);
		
		headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
		entity = new HttpEntity<>(headers);
*/		
		ResponseEntity<AccountDTO> response = 
//				testRestTemplate.exchange(
//						"/accounts/1111111111", HttpMethod.GET, entity, AccountDTO.class);
				testRestTemplate.getForEntity("/accounts/1111111111", AccountDTO.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
//	    assertThat(response.getBody().getId(), is(2));
//	    assertThat(response.getBody().getCreatedAt(), notNullValue(Timestamp.class));
	    assertThat(response.getBody().getUpdatedAt(), nullValue());
	    assertThat(response.getBody().getBirthday(), notNullValue(LocalDate.class));
	    assertThat(response.getBody().getName(), is("Kae"));
	    assertThat(response.getBody().getSurname(), is("Yukawa"));
	    assertThat(response.getBody().getPhone(), is("1111111111"));
	    assertThat(response.getBody().getEmail(), is("yukawa@greenmail.io"));
	    assertThat(response.getBody().getBills(), notNullValue());
	    assertThat(response.getBody().getBills().size(), is(size));
	    assertThat(response.getBody().isActive(), is(true));
	    
//	    mockServer.verify();
//	    mockServer.reset();
	    wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(requestURI)));
	}
	
	@Test
	void request_absent() {
		
		ResponseEntity<String> response = 
				testRestTemplate.getForEntity("/accounts/5555555555", String.class);
		assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
		assertThat(response.getBody().contains("Account with phone 5555555555 not found"), is(true));
//		assertThat(response.hasBody(), is(false));
/*		
		assertThat(response.hasBody(), is(true));
		assertThat(response.getBody().getCreatedAt(), nullValue());
	    assertThat(response.getBody().getUpdatedAt(), nullValue());
	    assertThat(response.getBody().getBirthday(), nullValue());
	    assertThat(response.getBody().getName(), nullValue());
	    assertThat(response.getBody().getSurname(), nullValue());
	    assertThat(response.getBody().getPhone(), nullValue());
	    assertThat(response.getBody().getEmail(), nullValue());
	    assertThat(response.getBody().getBills(), nullValue());
	    assertThat(response.getBody().isActive(), is(false));
*/	    
	}
	
	@Test
	void can_work_on_fault() {
/*		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Login", "1111111111:supervixen");
		HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);		
		ResponseEntity<Void> result = 
				testRestTemplate.exchange("/accounts/login", HttpMethod.HEAD, entity, Void.class);
		String jwt = result.getHeaders().get("Token").get(0);
		
		headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
		entity = new HttpEntity<>(headers);
*/		
		ResponseEntity<AccountDTO> response = 
//				testRestTemplate.exchange(
//						"/accounts/1111111111", HttpMethod.GET, entity, AccountDTO.class);
				testRestTemplate.getForEntity("/accounts/1111111111", AccountDTO.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
	    assertThat(response.getBody().getUpdatedAt(), nullValue());
	    assertThat(response.getBody().getBirthday(), notNullValue(LocalDate.class));
	    assertThat(response.getBody().getName(), is("Kae"));
	    assertThat(response.getBody().getSurname(), is("Yukawa"));
	    assertThat(response.getBody().getPhone(), is("1111111111"));
	    assertThat(response.getBody().getEmail(), is("yukawa@greenmail.io"));
	    assertThat(response.getBody().getBills(), notNullValue());
	    assertThat(response.getBody().getBills().isEmpty(), is(true));
	    assertThat(response.getBody().isActive(), is(true));
	}
	
	@Test
	void can_get_all() {
/*		
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "jwt");
		HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);
*/		
		ResponseEntity<List<AccountDTO>> response = 
				testRestTemplate.exchange("/accounts", HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<AccountDTO>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().size(), is(5));
	}
	
	@Test
	void can_change_password() {
		
		UriComponentsBuilder uriBuilder = 
				UriComponentsBuilder.fromUriString("/accounts/0000000000")
    	        .queryParam("password", "terminator");
		
		ResponseEntity<Void> response = 
				testRestTemplate.exchange(uriBuilder.toUriString(), HttpMethod.PATCH, 
				null, Void.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
	}
	
	@Test
	void try_invalid_password() {
		
		UriComponentsBuilder uriBuilder = 
				UriComponentsBuilder.fromUriString("/accounts/0000000000")
    	        .queryParam("password", " ");
		
		ResponseEntity<List<String>> violations = 
				testRestTemplate.exchange(uriBuilder.toUriString(), HttpMethod.PATCH, 
				null, new ParameterizedTypeReference<List<String>>(){});
		
        assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(violations.getBody().size(), is(1));
//        assertThat(violations.getBody().size(), is(2));
//        assertThat(violations.getBody().contains("Password must not be blank"), is(true));
        assertThat(violations.getBody().contains("Password should be 10-60 symbols length"), is(true));
	}
	
	@Test
	void can_add_bill() throws JsonProcessingException, URISyntaxException {
		
		String currency = "SEA";
		BillDTO bill = new BillDTO();
		bill.setId(new Integer(0));
		bill.setCurrency(currency);
		
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("/bills")
    	        .queryParam("currency", currency)
    	        .queryParam("owner", 3);
/*        
	    mockServer.expect(ExpectedCount.once(), requestTo(uriBuilder.toUriString()))
	    .andExpect(method(HttpMethod.POST))
	    .andRespond(withStatus(HttpStatus.OK)
	    .contentType(MediaType.APPLICATION_JSON)
	    .body(mapper.writeValueAsString(bill)));
*/	    
	    String requestURI = uriBuilder.toUriString();
		wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.CREATED.value())
				.withBody(mapper.writeValueAsString(bill))
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
/*	    
		HttpHeaders headers = new HttpHeaders();
		headers.set("Login", "2222222222:bustyblonde");
		HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);		
		ResponseEntity<Void> response = 
				testRestTemplate.exchange("/accounts/login", HttpMethod.HEAD, entity, Void.class);
		String jwt = response.getHeaders().get("Token").get(0);
*/		
        uriBuilder = UriComponentsBuilder.fromUriString("/accounts/2222222222/bills")
    	        .queryParam("currency", currency);
/*        
        HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
		entity = new HttpEntity<>(headers);
*/		
        BillDTO dto = 
        		testRestTemplate.postForObject(uriBuilder.toUriString(), null, BillDTO.class);
        assertThat(dto.getId() == 0);
        assertThat(dto.getCurrency().equals(currency));
	    
//	    mockServer.verify();
//	    mockServer.reset();
		wireMockServer.verify(WireMock.postRequestedFor(WireMock.urlEqualTo(requestURI)));
	}
	
	@Test
	void fail_to_add_bill() {
		
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("/accounts/2222222222/bills")
    	        .queryParam("currency", "coin");
		
        ResponseEntity<List<String>> violations = 
        		testRestTemplate.exchange(uriBuilder.toUriString(), HttpMethod.POST, null, 
        				new ParameterizedTypeReference<List<String>>(){});
        
        assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(violations.getBody().size(), is(1));
        assertThat(violations.getBody().contains("Currency should be 3 capital letters"), is(true));
	}
	
	@Test
	void can_delete_bill() {
		
		wireMockServer.stubFor(WireMock.delete(WireMock.urlEqualTo("/bills/1"))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.NO_CONTENT.value())));
		
//		testRestTemplate.delete("/accounts/1111111111/bills/1");
		ResponseEntity<Void> response = 
				testRestTemplate.exchange("/accounts/1111111111/bills/1", HttpMethod.DELETE, 
				null, Void.class);
		assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
		
		wireMockServer.verify(WireMock.deleteRequestedFor(WireMock.urlEqualTo("/bills/1")));
	}

	@AfterAll void clear() {populator = null; wireMockServer.shutdownServer();}
	
}
