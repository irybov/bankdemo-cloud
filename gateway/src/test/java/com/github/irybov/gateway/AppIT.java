package com.github.irybov.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class AppIT {
	
	@Value("${server.address}")
	private String uri;
    @LocalServerPort
    private int port;
    
	@Autowired
	private TestRestTemplate testRestTemplate;
	@Autowired
	private ObjectMapper mapper;
	
	@Value("${app.internal-url}")
	private static String internalURL;
	private static WireMockServer wireMockServer;
	
	@BeforeAll
	static void prepare() {
		wireMockServer = new WireMockServer(new WireMockConfiguration().port(8888));
		wireMockServer.start();
		WireMock.configureFor(internalURL, 8888);
	}
	
	@Test
	void context_loading(ApplicationContext context) {assertThat(context).isNotNull();}
	
	@Test
	void can_get_one_account() throws JsonProcessingException {
		
		String requestURI = "/accounts/0000000000";
		AccountDTO account = new AccountDTO();
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> bills = Stream.generate(() -> new BillDTO()).limit(size)
				.collect(Collectors.toList());
		int i = 1;
		for(BillDTO bill : bills) {bill.setId(new Integer(i++));}
		account.setBills(new HashSet<>(bills));
		
		wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.OK.value())
				.withBody(mapper.writeValueAsString(account))
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
		
		ResponseEntity<AccountDTO> response = 
				testRestTemplate.getForEntity(requestURI, AccountDTO.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().getBills().size(), is(size));
		assertThat(response.getBody().isActive(), is(false));
		
		wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlPathEqualTo(requestURI)));
	}
	
	@Test
	void can_add_bill_to_account() throws JsonProcessingException {
		
		String currency = "SEA";
		BillDTO bill = new BillDTO();
		bill.setId(new Integer(0));
		bill.setCurrency(currency);
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("/accounts/0000000000")
    	        .queryParam("currency", currency);
		String requestURI = uriBuilder.toUriString();
		
		wireMockServer.stubFor(WireMock.patch(WireMock.urlEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.OK.value())
				.withBody(mapper.writeValueAsString(bill))
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
		
        BillDTO dto = testRestTemplate.patchForObject(requestURI, null, BillDTO.class);
        assertThat(dto.getId() == 0);
        assertThat(dto.getCurrency().equals(currency));
		wireMockServer.verify(WireMock.patchRequestedFor(WireMock.urlEqualTo(requestURI)));		
	}
	
	@Test
	void can_get_accounts_list() throws JsonProcessingException {
		
		String requestURI = "/accounts";
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<AccountDTO> accounts = Collections.nCopies(size, new AccountDTO()).stream()
				.collect(Collectors.toList());
		
		wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.OK.value())
				.withBody(mapper.writeValueAsString(accounts))
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
		
		ResponseEntity<List<AccountDTO>> response = 
				testRestTemplate.exchange(requestURI, HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<AccountDTO>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().size(), is(size));
		
		wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlPathEqualTo(requestURI)));
	}
	
	@Test
	void can_create_new_bill() throws JsonProcessingException {
		
		String currency = "SEA";
		BillDTO bill = new BillDTO();
		bill.setId(new Integer(0));
		bill.setCurrency(currency);
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("/bills")
    	        .queryParam("currency", currency);
		String requestURI = uriBuilder.toUriString();
		
		wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.CREATED.value())
				.withBody(mapper.writeValueAsString(bill))
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
		
        BillDTO dto = testRestTemplate.postForObject(requestURI, null, BillDTO.class);
        assertThat(dto.getId() == 0);
        assertThat(dto.getCurrency().equals(currency));
		wireMockServer.verify(WireMock.postRequestedFor(WireMock.urlEqualTo(requestURI)));	
	}
	
	@Test
	void can_update_balance() throws JsonProcessingException {
		
        Map<Integer, Double> data = new LinkedHashMap<>();
        data.put(1, -3.00);
        data.put(2, 44.00);
		String requestURI = "/bills";
		
		wireMockServer.stubFor(WireMock.patch(WireMock.urlPathEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.OK.value())));
		
        testRestTemplate.patchForObject(requestURI, mapper.writeValueAsString(data), Void.class);
		
		wireMockServer.verify(WireMock.patchRequestedFor(WireMock.urlPathEqualTo(requestURI)));	
	}
	
	@Test
	void can_get_one_bill() throws JsonProcessingException {
		
		String currency = "SEA";
		BillDTO bill = new BillDTO();
		bill.setId(new Integer(0));
		bill.setCurrency(currency);
		String requestURI = "/bills/0";
		
		wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.OK.value())
				.withBody(mapper.writeValueAsString(bill))
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
		
		ResponseEntity<BillDTO> response = testRestTemplate.getForEntity(requestURI, BillDTO.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getId() == 0);
        assertThat(response.getBody().getCurrency().equals(currency));
		
		wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlPathEqualTo(requestURI)));
	}
	
	@Test
	void can_delete_bill() {
		
		String requestURI = "/bills/0";
		
		wireMockServer.stubFor(WireMock.delete(WireMock.urlPathEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.OK.value())));
		
		testRestTemplate.delete(requestURI);
		
		wireMockServer.verify(WireMock.deleteRequestedFor(WireMock.urlPathEqualTo(requestURI)));
	}

	@Test
	void can_get_bills_list() throws JsonProcessingException {
		
		String requestURI = "/bills/0/list";
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> bills = Collections.nCopies(size, new BillDTO()).stream()
				.collect(Collectors.toList());
		
		wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.OK.value())
				.withBody(mapper.writeValueAsString(bills))
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
		
		ResponseEntity<List<BillDTO>> response = 
				testRestTemplate.exchange(requestURI, HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<BillDTO>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().size(), is(size));
		
		wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlPathEqualTo(requestURI)));
	}
	
	@Test
	void can_change_status() {
		
		String requestURI = "/bills/0/status";
		
		wireMockServer.stubFor(WireMock.patch(WireMock.urlPathEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withBody("false")
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
				.withStatus(HttpStatus.OK.value())));
		
		ResponseEntity<Boolean> response = 
				testRestTemplate.exchange(requestURI, HttpMethod.PATCH, null, Boolean.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody(), is(false));
		
		wireMockServer.verify(WireMock.patchRequestedFor(WireMock.urlPathEqualTo(requestURI)));	
		
	}
	
	@Test
	void can_save_operation() {
		
		String requestURI = "/operations";
		String json = "{\"action\": \"unknown\", \"amount\": 0.00, \"sender\": 0, "
				+ "\"recipient\": 0, \"currency\": \"SEA\", \"bank\": \"Demo\"}";
		
		wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withStatus(HttpStatus.CREATED.value())));
		
		ResponseEntity<Void> response = 
				testRestTemplate.postForEntity(requestURI, json, Void.class);
		assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
		
		wireMockServer.verify(WireMock.postRequestedFor(WireMock.urlEqualTo(requestURI)));	
	}
	
	@Test
	void can_get_one_operation() throws JsonMappingException, JsonProcessingException {
		
		String requestURI = "/operations/0";
		String json = "{\"id\": 0, \"createdAt\": " + 
				mapper.writeValueAsString(Timestamp.from(Instant.now())) + ", " + 
				"\"amount\": 0.00, \"action\": \"unknown\", \"currency\": \"SEA\", \"sender\": 0, "
				+ "\"recipient\": 0, \"bank\": \"Demo\"}";
		
		wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withBody(json)
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
				.withStatus(HttpStatus.OK.value())));
		
		ResponseEntity<String> response = testRestTemplate.getForEntity(requestURI, String.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		JsonNode node = mapper.readTree(response.getBody());
		assertThat(node.get("id").asInt(), is(0));
		assertThat(node.get("amount").asDouble(), is(0.00));
		assertThat(node.get("action").asText(), is("unknown"));
		assertThat(node.get("currency").asText(), is("SEA"));
		assertThat(node.get("sender").asInt(), is(0));
		assertThat(node.get("recipient").asInt(), is(0));
		assertThat(node.get("bank").asText(), is("Demo"));
		
		wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(requestURI)));	
	}
	
	@Test
	void can_get_operations_list() throws JsonProcessingException {
		
		String requestURI = "/operations/0/list";
		String data = "{\"id\": 0, \"createdAt\": " + 
				mapper.writeValueAsString(Timestamp.from(Instant.now())) + ", " + 
				"\"amount\": 0.00, \"action\": \"unknown\", \"currency\": \"SEA\", \"sender\": 0, "
				+ "\"recipient\": 0, \"bank\": \"Demo\"}";
		String json = String.format("[%s,%s]", data, data);
		
		wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(requestURI))
				.willReturn(WireMock.aResponse()
				.withBody(json)
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
				.withStatus(HttpStatus.OK.value())));
		
		ResponseEntity<String> response = testRestTemplate.getForEntity(requestURI, String.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		JsonNode node = mapper.readTree(response.getBody());
		assertThat(node.isArray(), is(true));
		assertThat(node.size(), is(2));
		
		wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(requestURI)));
	}
	
	@Test
	void can_get_operations_page() {
		
	}
	
	@AfterAll
	static void clear() {wireMockServer.stop();}
	
}
