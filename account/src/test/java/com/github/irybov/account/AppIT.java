package com.github.irybov.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.TestConfiguration;
// import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.serialization.Data;
import com.hazelcast.spring.cache.HazelcastCacheManager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "wiremock.reset-mappings-after-each-test=true")
@AutoConfigureWebTestClient
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
public class AppIT {
	
    @Autowired
    private CacheManager cacheManager;
    static GenericContainer<?> hazelcastContainer = new GenericContainer<>("hazelcast/hazelcast:5.7.0")
        	.withEnv("HZ_CLUSTERNAME", "home")
            .withExposedPorts(5701);
    static {hazelcastContainer.start();}
    private static String hazelcastAddress;
    @DynamicPropertySource
    static void hazelcastProperties(DynamicPropertyRegistry registry) {
        hazelcastAddress = hazelcastContainer.getHost() + ":" + hazelcastContainer.getMappedPort(5701);
    }
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ClientConfig clientConfig() {
            ClientConfig config = new ClientConfig();
            config.setClusterName("home");
            config.getNetworkConfig().addAddress(hazelcastAddress);
            return config;
        }
        @Bean
        @Primary
        public HazelcastInstance hazelcastInstance() {
            return HazelcastClient.newHazelcastClient(clientConfig());
        }
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new HazelcastCacheManager(hazelcastInstance());
        }
    }
        
	@Autowired
	// private TestRestTemplate testRestTemplate;
	private WebTestClient webTestClient;
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
	
//	@Value("${app.internal-url}")
//	private static String internalURL;
	private static WireMockServer wireMockServer;
	
	@BeforeAll
	void prepare() {		
		populator = new ResourceDatabasePopulator();
		populator.addScripts(new ClassPathResource("test-accounts-h2.sql"));
		populator.execute(dataSource);
//		mockServer = MockRestServiceServer.createServer(restTemplate);
		wireMockServer = new WireMockServer(new WireMockConfiguration().port(8761));
		wireMockServer.start();
		WireMock.configureFor("localhost", 8761);
	}
	
	@Test
	void context_loading(ApplicationContext context) {
		assertThat(context).isNotNull();
		assertThat(hazelcastContainer.isRunning()).isTrue();
		
		// String path = "http://"+uri+":"+port;

		webTestClient.get()
		.uri("/swagger-ui/index.html")
		.exchange()
		.expectStatus().isOk()
		.expectHeader().exists(HttpHeaders.CONTENT_TYPE).equals(MediaType.TEXT_HTML_VALUE);
/* 		
		ResponseEntity<Void> response = 
				testRestTemplate.getForEntity(path + "/swagger-ui/", Void.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
  */
		webTestClient.get()
		.uri("/v3/api-docs")
		.exchange()
		.expectStatus().isOk()
		.expectHeader().exists(HttpHeaders.CONTENT_TYPE).equals(MediaType.APPLICATION_JSON_VALUE);
/*   
        response = 
				testRestTemplate.getForEntity(path + "/v3/api-docs", Void.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
 */		
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
		// HttpEntity<Registration> data = new HttpEntity<>(registration);

		webTestClient.post()
		.uri("/accounts")
		.contentType(MediaType.APPLICATION_JSON)
		.bodyValue(registration)
		.exchange()
		.expectStatus().isCreated()
		.expectBody().isEmpty();
		// ResponseEntity<Void> response = 
		// 		testRestTemplate.exchange("/accounts", HttpMethod.POST, data, Void.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
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
		// HttpEntity<Registration> data = new HttpEntity<>(registration);

		webTestClient.post()
		.uri("/accounts")
		.contentType(MediaType.APPLICATION_JSON)
		.bodyValue(registration)
		.exchange()
		.expectStatus().isBadRequest()
		.expectBody(new ParameterizedTypeReference<List<String>>(){})
		.consumeWith(response -> {
			List<String> list = response.getResponseBody();
			assertThat(list.size(), is(10));
			assertThat(list.contains("Name should be 2-20 chars length"), is(true));
			assertThat(list.contains("Please input name like Xx"), is(true));
			assertThat(list.contains("Surname must not be blank"), is(true));
			assertThat(list.contains("Surname should be 2-40 chars length"), is(true));
			assertThat(list.contains("Please input surname like Xx or Xx-Xx"), is(true));
			assertThat(list.contains("Please input phone number like a row of 10 digits"), is(true));
			assertThat(list.contains("Email address is not valid"), is(true));
			assertThat(list.contains("Email address should be 10-60 symbols length"), is(true));
			assertThat(list.contains("Birthday can't be future time"), is(true));
			assertThat(list.contains("Password should be 10-60 symbols length"), is(true));
		});
		// ResponseEntity<List<String>> violations = 
		// 		testRestTemplate.exchange("/accounts", HttpMethod.POST, data, 
		// 				new ParameterizedTypeReference<List<String>>(){});
	}
	
	@Test
	void can_get_token() {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Login", "3333333333:gingerchick");
		// HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);

		HttpHeaders response = webTestClient.head()
        .uri("/accounts")
		.headers(h -> h.addAll(headers))
        .exchange()
		.expectStatus().isOk()
        .returnResult(Void.class)
        .getResponseHeaders();
		// ResponseEntity<Void> response = 
		// 		testRestTemplate.exchange("/accounts", HttpMethod.HEAD, entity, Void.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.OK));
		// assertThat(response.getHeaders().containsKey("Token"), is(true));
		
		String jwt = response.getFirst("Token");
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
		// HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);

		webTestClient.head()
        .uri("/accounts")
		.headers(h -> h.addAll(headers))
        .exchange()
		.expectStatus().isBadRequest()
		.expectHeader().doesNotExist("Token")
		.expectBody(new ParameterizedTypeReference<List<String>>(){});
		// .consumeWith(response -> {
		// 	List<String> list = response.getResponseBody();
		// 	assertThat(list.size(), is(1));
		// 	assertThat(list.contains("Header should match pattern"), is(true));
		// });
		// ResponseEntity<List<String>> violations = 
		// 		testRestTemplate.exchange("/accounts", HttpMethod.HEAD, entity, 
		// 				new ParameterizedTypeReference<List<String>>(){});
		// assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
		// assertThat(violations.getHeaders().containsKey("Token"), is(false));
//		assertThat(list.size(), is(1));
//		assertThat(list.contains("Header should match pattern"), is(true));
	}
	
	@Test
	void try_wrong_password() {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Login", "3333333333:gingerfreak");
		// HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);

		webTestClient.head()
        .uri("/accounts")
		.headers(h -> h.addAll(headers))
        .exchange()
		.expectStatus().isUnauthorized()
		.expectHeader().doesNotExist("Token");
		// .expectBody(String.class)
		// .isEqualTo("Wrong password provided");
		// ResponseEntity<String> response = 
		// 		testRestTemplate.exchange("/accounts", HttpMethod.HEAD, entity, String.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
		// assertThat(response.getHeaders().containsKey("Token"), is(false));
//		assertThat(response.contains("Wrong password provided"), is(true));
	}
	
	@Test
	void can_get_one() throws JsonProcessingException, URISyntaxException {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> bills = Stream.generate(() -> new BillDTO())
				.peek(e -> e.setCurrency("SEA"))
				.limit(size)
				.collect(Collectors.toList());
		int i = 1;
		for(BillDTO bill : bills) {bill.setId(i++);}
		Set<BillDTO> list = new HashSet<>(bills);
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
				.withBody(mapper.writeValueAsString(list))
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
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
		AccountDTO response = webTestClient.get()
		.uri("/accounts/1111111111")
		.exchange()
		.expectStatus().isOk()
		.returnResult(AccountDTO.class)
		.getResponseBody()
		.blockFirst();
		// ResponseEntity<AccountDTO> response = 
		// 		testRestTemplate.getForEntity("/accounts/1111111111", AccountDTO.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.OK));
//	    assertThat(response.getId(), is(2));
//	    assertThat(response.getCreatedAt(), notNullValue(Timestamp.class));
	    assertThat(response.getUpdatedAt(), nullValue());
	    assertThat(response.getBirthday(), notNullValue(LocalDate.class));
	    assertThat(response.getName(), is("Kae"));
	    assertThat(response.getSurname(), is("Yukawa"));
	    assertThat(response.getPhone(), is("1111111111"));
	    assertThat(response.getEmail(), is("yukawa@greenmail.io"));
	    assertThat(response.getBills(), notNullValue());
	    assertThat(response.getBills().size(), is(size));
	    assertThat(response.isActive(), is(true));
	    
//	    mockServer.verify();
//	    mockServer.reset();
	    wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(requestURI)));
	}
	
	@Test
	void request_absent() {

		webTestClient.get()
		.uri("/accounts/5555555555")
		.exchange()
		.expectStatus().isNotFound()
		.expectBody(String.class)
		.value(body -> {
			assertTrue(body.contains("Account with phone 5555555555 not found"));
		});
		
		// ResponseEntity<String> response = 
		// 		testRestTemplate.getForEntity("/accounts/5555555555", String.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
		// assertThat(response.contains("Account with phone 5555555555 not found"), is(true));
//		assertThat(response.hasBody(), is(false));
/*		
		assertThat(response.hasBody(), is(true));
		assertThat(response.getCreatedAt(), nullValue());
	    assertThat(response.getUpdatedAt(), nullValue());
	    assertThat(response.getBirthday(), nullValue());
	    assertThat(response.getName(), nullValue());
	    assertThat(response.getSurname(), nullValue());
	    assertThat(response.getPhone(), nullValue());
	    assertThat(response.getEmail(), nullValue());
	    assertThat(response.getBills(), nullValue());
	    assertThat(response.isActive(), is(false));
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
		AccountDTO response = webTestClient.get()
		.uri("/accounts/1111111111")
		.exchange()
		.expectStatus().isOk()
		.returnResult(AccountDTO.class)
		.getResponseBody()
		.blockFirst();
		// ResponseEntity<AccountDTO> response = 
		// 		testRestTemplate.getForEntity("/accounts/1111111111", AccountDTO.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.OK));
	    assertThat(response.getUpdatedAt(), nullValue());
	    assertThat(response.getBirthday(), notNullValue(LocalDate.class));
	    assertThat(response.getName(), is("Kae"));
	    assertThat(response.getSurname(), is("Yukawa"));
	    assertThat(response.getPhone(), is("1111111111"));
	    assertThat(response.getEmail(), is("yukawa@greenmail.io"));
	    assertThat(response.getBills(), notNullValue());
	    assertThat(response.getBills().isEmpty(), is(true));
	    assertThat(response.isActive(), is(true));
	}
	
	@Test
	void can_get_cached_bills() {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> bills = Stream.generate(() -> new BillDTO())
				.peek(e -> e.setCurrency("SEA"))
				.limit(size)
				.collect(Collectors.toList());
		int i = 1;
		for(BillDTO bill : bills) {bill.setId(i++);}
		Set<BillDTO> list = new HashSet<>(bills);
		
		Cache cache = cacheManager.getCache("sets");
		cache.put(2, list);
		
		AccountDTO response = webTestClient.get()
		.uri("/accounts/1111111111")
		.exchange()
		.expectStatus().isOk()
		.returnResult(AccountDTO.class)
		.getResponseBody()
		.blockFirst();
		// ResponseEntity<AccountDTO> response = 
		// 		testRestTemplate.getForEntity("/accounts/1111111111", AccountDTO.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.OK));
	    assertThat(response.getUpdatedAt(), nullValue());
	    assertThat(response.getBirthday(), notNullValue(LocalDate.class));
	    assertThat(response.getName(), is("Kae"));
	    assertThat(response.getSurname(), is("Yukawa"));
	    assertThat(response.getPhone(), is("1111111111"));
	    assertThat(response.getEmail(), is("yukawa@greenmail.io"));
	    assertThat(response.getBills(), notNullValue());
	    assertThat(response.getBills().size(), is(size));
	    assertThat(response.isActive(), is(true));
	    
	    cache.evictIfPresent(2);
	}
	
	@Test
	void can_get_all() {
/*		
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "jwt");
		HttpEntity<HttpHeaders> entity = new HttpEntity<>(headers);
*/		
		webTestClient.get()
		.uri("/accounts")
		.exchange()
		.expectStatus().isOk()
		.expectBody(new ParameterizedTypeReference<Set<AccountDTO>>(){})
		.value(itemSet -> {assertThat(itemSet).hasSize(5);});
		// ResponseEntity<List<AccountDTO>> response = 
		// 		testRestTemplate.exchange("/accounts", HttpMethod.GET, 
		// 		null, new ParameterizedTypeReference<List<AccountDTO>>(){});
		// assertThat(response.getStatusCode(), is(HttpStatus.OK));
		// assertThat(response.size(), is(5));
	}
	
	@Test
	void can_change_password() {
		
		UriComponentsBuilder uriBuilder = 
				UriComponentsBuilder.fromUriString("/accounts/0000000000")
    	        .queryParam("password", "terminator");

		webTestClient.patch()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isOk();
		// ResponseEntity<Void> response = 
		// 		testRestTemplate.exchange(uriBuilder.toUriString(), HttpMethod.PATCH, 
		// 		null, Void.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.OK));
	}
	
	@Test
	void try_invalid_password() {
		
		UriComponentsBuilder uriBuilder = 
				UriComponentsBuilder.fromUriString("/accounts/0000000000")
    	        .queryParam("password", " ");

		webTestClient.patch()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isBadRequest()
		.expectBody(new ParameterizedTypeReference<List<String>>(){})
		.consumeWith(response -> {
			List<String> list = response.getResponseBody();
			assertThat(list.size(), is(1));
			// assertThat(list.contains("Password must not be blank"), is(true));
			assertThat(list.contains("Password should be 10-60 symbols length"), is(true));
		});
// 		ResponseEntity<List<String>> violations = 
// 				testRestTemplate.exchange(uriBuilder.toUriString(), HttpMethod.PATCH, 
// 				null, new ParameterizedTypeReference<List<String>>(){});
//         assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
//         assertThat(list.size(), is(1));
// //        assertThat(list.size(), is(2));
// //        assertThat(list.contains("Password must not be blank"), is(true));
//         assertThat(list.contains("Password should be 10-60 symbols length"), is(true));
	}
	
	@Test
	void can_add_bill() throws JsonProcessingException, URISyntaxException {
		
		String currency = "SEA";
		BillDTO bill = new BillDTO();
		bill.setId(0);
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
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
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
		BillDTO dto = webTestClient.post()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isCreated()
		.returnResult(BillDTO.class)
		.getResponseBody()
		.blockFirst();
        // BillDTO dto = 
        // 		testRestTemplate.postForObject(uriBuilder.toUriString(), null, BillDTO.class);
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

		webTestClient
		.post()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isBadRequest()
		.expectBody(new ParameterizedTypeReference<List<String>>(){})
		.consumeWith(response -> {
			List<String> list = response.getResponseBody();
			assertThat(list.size(), is(1));
			assertThat(list.contains("Currency should be 3 capital letters"), is(true));
		});
        // ResponseEntity<List<String>> violations = 
        // 		testRestTemplate.exchange(uriBuilder.toUriString(), HttpMethod.POST, null, 
        // 				new ParameterizedTypeReference<List<String>>(){});
        
        // assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        // assertThat(list.size(), is(1));
        // assertThat(list.contains("Currency should be 3 capital letters"), is(true));
	}
	
	@Test
	void can_delete_bill() {
		
		wireMockServer.stubFor(WireMock.delete(WireMock.urlEqualTo("/bills/1"))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.NO_CONTENT.value())));
		
		webTestClient.delete()
		.uri("/accounts/1111111111/bills/1")
		.exchange()
		.expectStatus().isNoContent();
		// ResponseEntity<Void> response = 
		// 		testRestTemplate.exchange("/accounts/1111111111/bills/1", HttpMethod.DELETE, 
		// 		null, Void.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
		
		wireMockServer.verify(WireMock.deleteRequestedFor(WireMock.urlEqualTo("/bills/1")));
	}

	@AfterAll void clear() {
		populator = null;
		wireMockServer.shutdownServer();
		cacheManager.getCacheNames()
		.forEach(cacheName -> {
			var cache = cacheManager.getCache(cacheName);
			if (cache != null) cache.clear();
		});
	}
	
}
