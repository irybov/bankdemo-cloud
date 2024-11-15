package com.github.irybov.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
class AppIT {
	
	@Autowired
	private TestRestTemplate restTemplate;
	
	@Autowired
	private DataSource dataSource;
	private ResourceDatabasePopulator populator;
	
	@Value("${server.address}")
	private String uri;
	@Value("${local.server.port}")
	private int port;
	
	@BeforeAll
	void prepare() {		
		populator = new ResourceDatabasePopulator();
		populator.addScripts(new ClassPathResource("test-operations-h2.sql"));
		populator.execute(dataSource);
	}
	
	@Test
	void context_loading(ApplicationContext context) {assertThat(context).isNotNull();}
	
	@Test
	void can_save() throws Exception {
		
		Operation.OperationBuilder builder = Operation.builder();
		Operation operation = builder
			.amount(0.00)
			.action("unknown")
			.currency("SEA")
			.createdAt(Timestamp.valueOf(OffsetDateTime.now()
					.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
			.bank("Demo")
			.build();
		
		ResponseEntity<Void> response = 
				restTemplate.postForEntity("/operations", operation, Void.class);
	    assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
	}
	
	@Test
	void can_get_one() throws Exception {
		
		ResponseEntity<Operation> response = 
				restTemplate.getForEntity("/operations/1", Operation.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
	    assertThat(response.getBody().getId(), is(1L));
	    assertThat(response.getBody().getAction(), equalTo("deposit"));
	    assertThat(response.getBody().getAmount(), is(500.00));
	    assertThat(response.getBody().getBank(), equalTo("Demo"));
	    assertThat(response.getBody().getCreatedAt(), notNullValue());
	    assertThat(response.getBody().getCurrency(), equalTo("RUB"));
	    assertThat(response.getBody().getSender(), is(0));
	    assertThat(response.getBody().getRecipient(), is(1));
		
		response = restTemplate.getForEntity("/operations/6", Operation.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
	    assertThat(response.getBody().getId(), is(6L));
	    assertThat(response.getBody().getAction(), equalTo("transfer"));
	    assertThat(response.getBody().getAmount(), is(800.00));
	    assertThat(response.getBody().getBank(), equalTo("Demo"));
	    assertThat(response.getBody().getCreatedAt(), notNullValue());
	    assertThat(response.getBody().getCurrency(), equalTo("RUB"));
	    assertThat(response.getBody().getSender(), is(2));
	    assertThat(response.getBody().getRecipient(), is(3));
	}
	
	@Test
	void can_get_list() throws Exception {
		
		ResponseEntity<List<Operation>> response = 
				restTemplate.exchange("/operations/0/list", HttpMethod.GET, null, 
						new ParameterizedTypeReference<List<Operation>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().size(), is(4));
	}

	@Test
	void can_get_page() throws Exception {
		
        String url = "http://"+uri+":"+port+"/operations/1/pageable";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url)
	        .queryParam("minval", 99.99)
	        .queryParam("maxval", 500.01)
	        .queryParam("sort", "amount,asc")
	        .queryParam("sort", "id,desc")
	        .queryParam("page", 1)
	        .queryParam("size", 2);
		
        ResponseEntity<Page<Operation>> response = restTemplate.exchange(uriBuilder.toUriString(), 
                HttpMethod.GET, null, new ParameterizedTypeReference<Page<Operation>>(){});
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getContent().size(), is(1));
	}

	@AfterAll void clear() {
		populator.setScripts(new ClassPathResource("test-clean-data-h2.sql"));
		populator.execute(dataSource);
		populator = null;
	}
	
}
