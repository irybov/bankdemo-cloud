package com.github.irybov.bill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
import org.springframework.web.util.UriComponentsBuilder;

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
		populator.addScripts(new ClassPathResource("test-data-h2.sql"));
		populator.execute(dataSource);
	}
	
	@Test
	void context_loading(ApplicationContext context) {assertThat(context).isNotNull();}
	
	@Test
	void multi_test() {
		
		// create
        String url = "http://"+uri+":"+port+"/bills";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url)
	        .queryParam("currency", "SEA")
	        .queryParam("owner", 1);        
        ResponseEntity<Void> response = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.POST, null, Void.class);
        assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
        
        // get one
		ResponseEntity<Bill> bill = restTemplate.getForEntity("/bills/2", Bill.class);
		assertThat(bill.getStatusCode(), is(HttpStatus.OK));
	    assertThat(bill.getBody().getId(), is(2));
	    assertThat(bill.getBody().getBalance().doubleValue(), is(0.00));
	    assertThat(bill.getBody().getCurrency(), is("SEA"));
	    assertThat(bill.getBody().getOwner(), is(1));
	    assertThat(bill.getBody().isActive(), is(true));
	    
	    // get list
		ResponseEntity<List<Bill>> list = restTemplate.exchange("/bills/1/list", HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<Bill>>(){});
		assertThat(list.getStatusCode(), is(HttpStatus.OK));
		assertThat(list.getBody().size(), is(2));
		
		// change status
        url = "http://"+uri+":"+port+"/bills/2/status";
        uriBuilder = UriComponentsBuilder.fromUriString(url);		
		ResponseEntity<Boolean> status = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.PATCH, null, Boolean.class);
        assertThat(status.getStatusCode(), is(HttpStatus.OK));
        assertThat(status.getBody(), is(false));
        
        // update balance
        url = "http://"+uri+":"+port+"/bills/1/balance";
        uriBuilder = UriComponentsBuilder.fromUriString(url)
        	.queryParam("amount", -15.00);		
		ResponseEntity<Double> balance = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.PATCH, null, Double.class);
        assertThat(balance.getStatusCode(), is(HttpStatus.OK));
        assertThat(balance.getBody(), is(-5.00));
        
        // delete
        url = "http://"+uri+":"+port+"/bills/2";
        uriBuilder = UriComponentsBuilder.fromUriString(url);        
        ResponseEntity<Void> delete = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.DELETE, null, Void.class);
        assertThat(delete.getStatusCode(), is(HttpStatus.OK));
        
        // check
		list = restTemplate.exchange("/bills/1/list", HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<Bill>>(){});
		assertThat(list.getStatusCode(), is(HttpStatus.OK));
		assertThat(list.getBody().size(), is(1));
	}
	
	@AfterAll void clear() {populator = null;}

}
