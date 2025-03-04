package com.github.irybov.bill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

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
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.cloud.stream.test.binder.TestSupportBinderConfiguration;
import org.springframework.cloud.stream.test.matcher.MessageQueueMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.irybov.shared.BillDTO;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
//@Import(TestSupportBinderConfiguration.class)
public class AppIT {
	
	@Autowired
	private TestRestTemplate restTemplate;
	
    @Autowired
    private MessageCollector collector;
    @Autowired
    private Source source;
    @Autowired
    private ObjectMapper mapper;
	
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
		populator.addScripts(new ClassPathResource("test-bill-h2.sql"));
		populator.execute(dataSource);
	}
	
	@Test
	void context_loading(ApplicationContext context) {
		assertThat(context).isNotNull();
		
		String path = "http://"+uri+":"+port;
		
		ResponseEntity<Void> response = 
				restTemplate.getForEntity(path + "/swagger-ui/", Void.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
        
        response = 
				restTemplate.getForEntity(path + "/v3/api-docs", Void.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}
	
	@Test
	void multi_test() throws JsonProcessingException {
		
		// create
        String url = "http://"+uri+":"+port+"/bills";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url)
	        .queryParam("currency", "SEA")
	        .queryParam("owner", 1);        
//        ResponseEntity<Void> response = restTemplate.exchange(uriBuilder.toUriString(), 
//        		HttpMethod.POST, null, Void.class);
//        assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
        ResponseEntity<BillDTO> bill = 
        		restTemplate.postForEntity(uriBuilder.toUriString(), null, BillDTO.class);
        assertThat(bill.getStatusCode(), is(HttpStatus.CREATED));
	    assertThat(bill.getBody().getId(), is(2));
	    assertThat(bill.getBody().getBalance().doubleValue(), is(0.00));
	    assertThat(bill.getBody().getCurrency(), is("SEA"));
//	    assertThat(bill.getBody().getOwner(), is(1));
	    assertThat(bill.getBody().isActive(), is(true));
	    
	    // failed creation
        uriBuilder = UriComponentsBuilder.fromUriString(url)
    	        .queryParam("currency", "coin")
    	        .queryParam("owner", -1); 
        ResponseEntity<List<String>> violations = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.POST, null, new ParameterizedTypeReference<List<String>>(){});
        assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(violations.getBody().size(), is(2));
        assertThat(violations.getBody().contains("Currency should be 3 capital letters"), is(true));
        assertThat(violations.getBody().contains("Owner's id should be positive"), is(true));
        
        // get one
		bill = restTemplate.getForEntity("/bills/1", BillDTO.class);
		assertThat(bill.getStatusCode(), is(HttpStatus.OK));
	    assertThat(bill.getBody().getId(), is(1));
	    assertThat(bill.getBody().getBalance().doubleValue(), is(10.00));
	    assertThat(bill.getBody().getCurrency(), is("USD"));
//	    assertThat(bill.getBody().getOwner(), is(1));
	    assertThat(bill.getBody().isActive(), is(true));
	    
	    // absent one
	    bill = restTemplate.getForEntity("/bills/5", BillDTO.class);
	    assertThat(bill.getStatusCode(), is(HttpStatus.NOT_FOUND));
	    assertThat(bill.hasBody(), is(false));
	    
	    // get list
		ResponseEntity<List<BillDTO>> list = restTemplate.exchange("/bills/1/list", HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<BillDTO>>(){});
		assertThat(list.getStatusCode(), is(HttpStatus.OK));
		assertThat(list.getBody().size(), is(2));
		
		// empty list
		list = restTemplate.exchange("/bills/5/list", HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<BillDTO>>(){});
		assertThat(list.getStatusCode(), is(HttpStatus.OK));
		assertThat(list.getBody().isEmpty(), is(true));
		
		// change status
        url = "http://"+uri+":"+port+"/bills/2/status";
        uriBuilder = UriComponentsBuilder.fromUriString(url);		
		ResponseEntity<Boolean> status = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.PATCH, null, Boolean.class);
        assertThat(status.getStatusCode(), is(HttpStatus.OK));
        assertThat(status.getBody(), is(false));
        
        // failed change
        url = "http://"+uri+":"+port+"/bills/5/status";
        uriBuilder = UriComponentsBuilder.fromUriString(url);		
		status = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.PATCH, null, Boolean.class);
        assertThat(status.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(status.hasBody(), is(false));
        
        // update balance
        url = "http://"+uri+":"+port+"/bills";
        Map<Integer, Double> data = new LinkedHashMap<>();
        data.put(1, -3.00);
        data.put(2, 44.00);
        HttpEntity<Map<Integer, Double>> request = new HttpEntity<>(data);
		ResponseEntity<Void> response = 
				restTemplate.exchange(url, HttpMethod.PATCH, request, Void.class);
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        
        // inspect message
        Queue<Message<?>> queue = collector.forChannel(source.output());
        Message<String> message = (Message<String>) queue.poll();
        assertThat(message.getPayload()).isEqualTo(mapper.writeValueAsString(data));
        
        // delete
        url = "http://"+uri+":"+port+"/bills/2";
        uriBuilder = UriComponentsBuilder.fromUriString(url);        
        ResponseEntity<Void> delete = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.DELETE, null, Void.class);
        assertThat(delete.getStatusCode(), is(HttpStatus.NO_CONTENT));
        
        // check
		list = restTemplate.exchange("/bills/1/list", HttpMethod.GET, 
				null, new ParameterizedTypeReference<List<BillDTO>>(){});
		assertThat(list.getStatusCode(), is(HttpStatus.OK));
		assertThat(list.getBody().size(), is(1));
		assertThat(list.getBody().get(0).getBalance().doubleValue(), is(7.00));
	}
	
	@AfterAll void clear() {populator = null;}

}
