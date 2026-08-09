package com.github.irybov.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
// import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
//@AutoConfigureWireMock(port = 8888)
class AppIT {
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
	// private TestRestTemplate testRestTemplate;
	private WebTestClient webTestClient;
	// @Autowired
    // private ObjectMapper mapper;
	
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
		populator.addScripts(new ClassPathResource("test-operations-h2.sql"));
		populator.execute(dataSource);
//		mockServer = MockRestServiceServer.createServer(restTemplate);
		wireMockServer = new WireMockServer(new WireMockConfiguration().port(8761));
		wireMockServer.start();
		WireMock.configureFor("localhost", 8761);
	}
	
	@Test
	void context_loading(ApplicationContext context) {
		assertThat(context).isNotNull();
		
		webTestClient.get()
		.uri("/swagger-ui/index.html")
		.exchange()
		.expectStatus().isOk()
		.expectHeader().exists(HttpHeaders.CONTENT_TYPE).equals(MediaType.TEXT_HTML_VALUE);
        
		webTestClient.get()
		.uri("/v3/api-docs")
		.exchange()
		.expectStatus().isOk()
		.expectHeader().exists(HttpHeaders.CONTENT_TYPE).equals(MediaType.APPLICATION_JSON_VALUE);
		
	}
	
	@Test
	void can_save() {
/*		
	    mockServer.expect(ExpectedCount.once(), requestTo(new URI("http://BILL/bills")))
	    .andExpect(method(HttpMethod.PATCH))
	    .andRespond(withStatus(HttpStatus.OK));
*/		
		String requestURI = "/bills";
		wireMockServer.stubFor(WireMock.patch(WireMock.urlEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.OK.value())));
/*		
		Operation.OperationBuilder builder = Operation.builder();
		Operation operation = builder
			.amount(0.00)
			.action("unknown")
			.currency("SEA")
			.createdAt(Timestamp.valueOf(OffsetDateTime.now()
					.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
			.bank("Demo")
			.build();
*/		
		OperationDTO operation = new OperationDTO(0.01, Action.EXTERNAL, "SEA", 4, 4, "Demo");
		
		webTestClient.post()
		.uri("/operations")
		.contentType(MediaType.APPLICATION_JSON)
		.bodyValue(operation)
		.exchange()
		.expectStatus().isCreated()
		.expectBody().isEmpty();
		// ResponseEntity<Void> response = 
		// 		testRestTemplate.postForEntity("/operations", operation, Void.class);
	    // assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
	    
//	    mockServer.verify();
//	    mockServer.reset();
	    wireMockServer.verify(WireMock.patchRequestedFor(WireMock.urlEqualTo(requestURI)));
	}
	
	@Test
	void fail_to_save() {
		
		OperationDTO operation = new OperationDTO(-0.01, null, "coin", -4, 1_000_000_000, " ");
		// HttpEntity<OperationDTO> entity = new HttpEntity<>(operation);

		webTestClient.post()
		.uri("/operations")
		.contentType(MediaType.APPLICATION_JSON)
		.bodyValue(operation)
		.exchange()
		.expectStatus().isBadRequest()
		.expectBody(new ParameterizedTypeReference<List<String>>(){})
		.consumeWith(response -> {
			List<String> list = response.getResponseBody();
			assertThat(list.size(), is(6));
			assertThat(list.contains("Amount of money should be higher than zero"), is(true));
			assertThat(list.contains("Action must not be null"), is(true));
			assertThat(list.contains("Currency code should be 3 capital characters length"), is(true));
			assertThat(list.contains("Sender's bill number should be positive"), is(true));
			assertThat(list.contains("Recepient's bill number should be less than 10 digits length"), is(true));
			assertThat(list.contains("Bank's name must not be blank"), is(true));
		});
	/* 		
		ResponseEntity<List<String>> violations = 
				testRestTemplate.exchange("/operations", HttpMethod.POST, entity, 
						new ParameterizedTypeReference<List<String>>(){});
	    assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(violations.getBody().size(), is(6));
        assertThat(violations.getBody().contains("Amount of money should be higher than zero"), is(true));
        assertThat(violations.getBody().contains("Action must not be null"), is(true));
        assertThat(violations.getBody().contains("Currency code should be 3 capital characters length"), is(true));
        assertThat(violations.getBody().contains("Sender's bill number should be positive"), is(true));
        assertThat(violations.getBody().contains("Recepient's bill number should be less than 10 digits length"), is(true));
        assertThat(violations.getBody().contains("Bank's name must not be blank"), is(true));
	 */	
	}
	
	@Test
	void can_get_one() {

		Operation response = webTestClient.get()
		.uri("/operations/1")
		.exchange()
		.expectStatus().isOk()
		.returnResult(Operation.class)
		.getResponseBody()
		.blockFirst();
		// ResponseEntity<Operation> response = 
		// 		testRestTemplate.getForEntity("/operations/1", Operation.class);
	    assertThat(response.getId(), is(1L));
	    assertThat(response.getAction(), equalTo("deposit"));
	    assertThat(response.getAmount(), is(500.00));
	    assertThat(response.getBank(), equalTo("Demo"));
	    assertThat(response.getCreatedAt(), notNullValue());
	    assertThat(response.getCurrency(), equalTo("RUB"));
	    assertThat(response.getSender(), is(0));
	    assertThat(response.getRecipient(), is(1));
		

		response = webTestClient.get()
		.uri("/operations/6")
		.exchange()
		.expectStatus().isOk()
		.returnResult(Operation.class)
		.getResponseBody()
		.blockFirst();
		// response = testRestTemplate.getForEntity("/operations/6", Operation.class);
	    assertThat(response.getId(), is(6L));
	    assertThat(response.getAction(), equalTo("transfer"));
	    assertThat(response.getAmount(), is(800.00));
	    assertThat(response.getBank(), equalTo("Demo"));
	    assertThat(response.getCreatedAt(), notNullValue());
	    assertThat(response.getCurrency(), equalTo("RUB"));
	    assertThat(response.getSender(), is(2));
	    assertThat(response.getRecipient(), is(3));
	}
	
	@Test
	void request_absent() {

		webTestClient.get()
		.uri("/operations/10")
		.exchange()
		.expectStatus().isNotFound()
		.expectBody().isEmpty();
	/* 	
		ResponseEntity<Operation> response = 
				testRestTemplate.getForEntity("/operations/10", Operation.class);
		assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
		assertThat(response.hasBody(), is(false));
 	*/
	}
	
	@Test
	void can_get_list() {

		webTestClient.get()
		.uri("/operations/0/list")
		.exchange()
		.expectStatus().isOk()
		.expectBody(new ParameterizedTypeReference<List<Operation>>(){})
		.value(itemList -> {
			assertThat(itemList).hasSize(4);
		});
/* 		
		ResponseEntity<List<Operation>> response = 
				testRestTemplate.exchange("/operations/0/list", HttpMethod.GET, null, 
						new ParameterizedTypeReference<List<Operation>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().size(), is(4));
 */
	}
	
	@Test
	void try_get_empty_list() {

		webTestClient.get()
		.uri("/operations/5/list")
		.exchange()
		.expectStatus().isOk()
		.expectBodyList(Operation.class)
		.hasSize(0);
/* 		
		ResponseEntity<List<Operation>> response = 
				testRestTemplate.exchange("/operations/5/list", HttpMethod.GET, null, 
						new ParameterizedTypeReference<List<Operation>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().isEmpty(), is(true));
 */
	}

	@Test
	void can_get_page() {
		
        String url = "http://"+uri+":"+port+"/operations/1/page";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url)
	        .queryParam("minval", 99.99)
	        .queryParam("maxval", 500.01)
	        .queryParam("sort", "amount,asc")
	        .queryParam("sort", "id,desc")
	        .queryParam("page", 1)
	        .queryParam("size", 2);

		webTestClient.get()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isOk()
		// .expectBody(new ParameterizedTypeReference<Page<Operation>>(){})
		.expectBody()
		// .value(page -> {
		// 	assert page != null;
		// 	assert page.getTotalElements() == 3;
		// 	assert page.getPageable().isPaged() == true;
		// 	assert page.getSort().isSorted() == true;
		// 	List<Operation> content = page.getContent();
		// 	assert content.size() == 1;
		// });
		.jsonPath("$.content").isArray()
		.jsonPath("$.content.length()").isEqualTo(1)
		.jsonPath("$.totalElements").isEqualTo(3)
		.jsonPath("$.pageable.paged").exists()
		.jsonPath("$.pageable.sort").exists();
/* 		
        ResponseEntity<Page<Operation>> response = testRestTemplate.exchange(uriBuilder.toUriString(), 
                HttpMethod.GET, null, new ParameterizedTypeReference<Page<Operation>>(){});
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getContent().size(), is(1));
        assertThat(response.getBody().getPageable().isPaged(), is(true));
        assertThat(response.getBody().getSort().isSorted(), is(true));
 */		
	}

	@AfterAll void clear() {
		populator.setScripts(new ClassPathResource("test-clean-data-h2.sql"));
		populator.execute(dataSource);
		populator = null;
		wireMockServer.stop();
	}
	
}
