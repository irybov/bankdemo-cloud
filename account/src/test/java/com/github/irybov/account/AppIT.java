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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.core.io.ClassPathResource;
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

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
public class AppIT {
	
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
	
	@Autowired
	private TestRestTemplate testRestTemplate;
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private DataSource dataSource;
	private ResourceDatabasePopulator populator;
	private MockRestServiceServer mockServer;
	
	@Value("${server.address}")
	private String uri;
	@Value("${local.server.port}")
	private int port;
	
	@BeforeAll
	void prepare() {		
		populator = new ResourceDatabasePopulator();
		populator.addScripts(new ClassPathResource("test-accounts-h2.sql"));
		populator.execute(dataSource);
		mockServer = MockRestServiceServer.createServer(restTemplate);
	}
	
	@Test
	void context_loading(ApplicationContext context) {assertThat(context).isNotNull();}
	
	@Test
	void can_get_one() throws JsonProcessingException, URISyntaxException {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> bills = Stream.generate(() -> new BillDTO()).limit(size)
				.collect(Collectors.toList());
		int i = 1;
		for(BillDTO bill : bills) {bill.setId(new Integer(i++));}
		
	    mockServer.expect(ExpectedCount.once(), requestTo(new URI("http://BILL/bills/2/list")))
	    .andExpect(method(HttpMethod.GET))
	    .andRespond(withStatus(HttpStatus.OK)
	    .contentType(MediaType.APPLICATION_JSON)
	    .body(mapper.writeValueAsString(bills)));
		
		ResponseEntity<AccountDTO> response = 
//				restTemplate.getForEntity("/accounts/2", AccountDTO.class);
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
	    
	    mockServer.verify();
	    mockServer.reset();
	}
	
	@Test
	void can_get_all() {
		
		ResponseEntity<List<AccountDTO>> response = 
				testRestTemplate.exchange("/accounts", HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<AccountDTO>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().size(), is(4));
	}
	
	@Test
	void can_add_bill() throws JsonProcessingException, URISyntaxException {
		
		String currency = "SEA";
		BillDTO bill = new BillDTO();
		bill.setId(new Integer(0));
		bill.setCurrency(currency);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://BILL/bills")
    	        .queryParam("currency", currency)
    	        .queryParam("owner", 1);
	    mockServer.expect(ExpectedCount.once(), requestTo(uriBuilder.toUriString()))
	    .andExpect(method(HttpMethod.POST))
	    .andRespond(withStatus(HttpStatus.OK)
	    .contentType(MediaType.APPLICATION_JSON)
	    .body(mapper.writeValueAsString(bill)));

        uriBuilder = UriComponentsBuilder.fromUriString("/accounts/0000000000")
    	        .queryParam("currency", currency);
        BillDTO response = 
        		testRestTemplate.patchForObject(uriBuilder.toUriString(), null, BillDTO.class);
        assertThat(response.getId() == 0);
        assertThat(response.getCurrency().equals(currency));
	    
	    mockServer.verify();
	    mockServer.reset();
	}

	@AfterAll void clear() {populator = null; mockServer = null;}
	
}
