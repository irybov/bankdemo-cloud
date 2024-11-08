package com.github.irybov.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.sql.Timestamp;
import java.time.LocalDate;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
public class AppIT {
	
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
		populator.addScripts(new ClassPathResource("test-accounts-h2.sql"));
		populator.execute(dataSource);
	}
	
	@Test
	void context_loading(ApplicationContext context) {assertThat(context).isNotNull();}
	
	@Test
	void can_get_one() {
		
		ResponseEntity<Account> response = restTemplate.getForEntity("/accounts/2", Account.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
	    assertThat(response.getBody().getId(), is(2));
//	    assertThat(response.getBody().getCreatedAt(), notNullValue(Timestamp.class));
	    assertThat(response.getBody().getUpdatedAt(), nullValue());
	    assertThat(response.getBody().getBirthday(), notNullValue(LocalDate.class));
	    assertThat(response.getBody().getName(), is("Kae"));
	    assertThat(response.getBody().getSurname(), is("Yukawa"));
	    assertThat(response.getBody().getPhone(), is("1111111111"));
	    assertThat(response.getBody().getEmail(), is("yukawa@greenmail.io"));
	    assertThat(response.getBody().getBills().size(), is(2));
	    assertThat(response.getBody().isActive(), is(true));
	}
	
	@Test
	void can_get_all() {
		
		ResponseEntity<List<Account>> list = restTemplate.exchange("/accounts", HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<Account>>(){});
		assertThat(list.getStatusCode(), is(HttpStatus.OK));
		assertThat(list.getBody().size(), is(4));
	}

	@AfterAll void clear() {populator = null;}
	
}
