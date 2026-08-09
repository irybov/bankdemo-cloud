package com.github.irybov.bill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
// import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.function.StreamBridge;
// import org.springframework.cloud.stream.messaging.Source;
// import org.springframework.cloud.stream.test.binder.MessageCollector;
// import org.springframework.cloud.stream.test.binder.TestSupportBinderConfiguration;
// import org.springframework.cloud.stream.test.matcher.MessageQueueMatcher;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.messaging.Message;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.irybov.shared.BillDTO;
import com.hazelcast.core.HazelcastInstance;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EnableTestBinder
@TestInstance(Lifecycle.PER_CLASS)
//@Import(TestSupportBinderConfiguration.class)
public class AppIT {
	
	@Autowired
	// private TestRestTemplate restTemplate;
	private WebTestClient webTestClient;
	
    @Autowired
    // private MessageCollector collector;
	private OutputDestination outputDestination;
    // @Autowired
    // private Source source;
	// private StreamBridge streamBridge;
	
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
		
		// String path = "http://"+uri+":"+port;
		
		webTestClient.get()
		.uri("/swagger-ui/index.html")
		.exchange()
		.expectStatus().isOk()
		.expectHeader().exists(HttpHeaders.CONTENT_TYPE).equals(MediaType.TEXT_HTML_VALUE);
/* 
		ResponseEntity<Void> response = 
				restTemplate.getForEntity(path + "/swagger-ui/", Void.class);
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
				restTemplate.getForEntity(path + "/v3/api-docs", Void.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
 */		
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
		BillDTO bill = webTestClient.post()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isCreated()
		.returnResult(BillDTO.class)
		.getResponseBody()
		.blockFirst();
        // ResponseEntity<BillDTO> bill = 
        // 		restTemplate.postForEntity(uriBuilder.toUriString(), null, BillDTO.class);
        // assertThat(bill.getStatusCode(), is(HttpStatus.CREATED));
	    assertThat(bill.getId(), is(2));
	    assertThat(bill.getBalance().doubleValue(), is(0.00));
	    assertThat(bill.getCurrency(), is("SEA"));
//	    assertThat(bill.getBody().getOwner(), is(1));
	    assertThat(bill.isActive(), is(true));
	    
	    // failed creation
        uriBuilder = UriComponentsBuilder.fromUriString(url)
    	        .queryParam("currency", "coin")
    	        .queryParam("owner", -1); 
				
		webTestClient
		// .mutate()
		// .responseTimeout(Duration.ofMinutes(5))
		// .build()
		.post()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isBadRequest()
		// .expectBodyList(String.class)
		.expectBody(new ParameterizedTypeReference<List<String>>(){})
		.consumeWith(response -> {
			List<String> list = response.getResponseBody();
			assertThat(list.size(), is(2));
			assertThat(list.contains("Currency should be 3 capital letters"), is(true));
			assertThat(list.contains("Owner's id should be positive"), is(true));
		});
		// .hasSize(2)
		// .contains("Currency should be 3 capital letters", "Owner's id should be positive");
/* 					
        ResponseEntity<List<String>> violations = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.POST, null, new ParameterizedTypeReference<List<String>>(){});
        assertThat(violations.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(violations.getBody().size(), is(2));
        assertThat(violations.getBody().contains("Currency should be 3 capital letters"), is(true));
        assertThat(violations.getBody().contains("Owner's id should be positive"), is(true));
 */        
        // get one
		bill = webTestClient.get()
		.uri("/bills/1")
		.exchange()
		.expectStatus().isOk()
		.returnResult(BillDTO.class)
		.getResponseBody()
		.blockFirst();
		// bill = restTemplate.getForEntity("/bills/1", BillDTO.class);
		// assertThat(bill.getStatusCode(), is(HttpStatus.OK));
	    assertThat(bill.getId(), is(1));
	    assertThat(bill.getBalance().doubleValue(), is(10.00));
	    assertThat(bill.getCurrency(), is("USD"));
//	    assertThat(bill.getBody().getOwner(), is(1));
	    assertThat(bill.isActive(), is(true));
	    
	    // absent one
		webTestClient.get()
		.uri("/bills/5")
		.exchange()
		.expectStatus().isNotFound()
		.expectBody().isEmpty();
	    // bill = restTemplate.getForEntity("/bills/5", BillDTO.class);
	    // assertThat(bill.getStatusCode(), is(HttpStatus.NOT_FOUND));
	    // assertThat(bill.hasBody(), is(false));
	    
	    // get list
		webTestClient.get()
		.uri("/bills/1/list")
		.exchange()
		.expectStatus().isOk()
		.expectBody(new ParameterizedTypeReference<Set<BillDTO>>(){})
		.value(itemSet -> {assertThat(itemSet).hasSize(2);});
/* 		
		ResponseEntity<Set<BillDTO>> response = restTemplate.exchange("/bills/1/list", HttpMethod.GET, 
				null, new ParameterizedTypeReference<Set<BillDTO>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().size(), is(2));
 */		
		// empty list
		webTestClient.get()
		.uri("/bills/5/list")
		.exchange()
		.expectStatus().isOk()
		.expectBody(new ParameterizedTypeReference<Set<BillDTO>>(){})
		.value(itemSet -> {assertThat(itemSet).isEmpty();});
/* 		
		response = restTemplate.exchange("/bills/5/list", HttpMethod.GET, 
				null, new ParameterizedTypeReference<Set<BillDTO>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().isEmpty(), is(true));
 */		
		// change status
        url = "http://"+uri+":"+port+"/bills/2/status";
        uriBuilder = UriComponentsBuilder.fromUriString(url);
		webTestClient.patch()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isOk()
		.expectBody(Boolean.class)
		.isEqualTo(false);
/* 		
		ResponseEntity<Boolean> status = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.PATCH, null, Boolean.class);
        assertThat(status.getStatusCode(), is(HttpStatus.OK));
        assertThat(status.getBody(), is(false));
 */ 
		bill = webTestClient.get()
		.uri("/bills/2")
		.exchange()
		.expectStatus().isOk()
		.returnResult(BillDTO.class)
		.getResponseBody()
		.blockFirst();       
		// bill = restTemplate.getForEntity("/bills/2", BillDTO.class);
		// assertThat(bill.getStatusCode(), is(HttpStatus.OK));
	    assertThat(bill.getId(), is(2));
	    assertThat(bill.getBalance().doubleValue(), is(0.00));
	    assertThat(bill.getCurrency(), is("SEA"));
//	    assertThat(bill.getBody().getOwner(), is(1));
	    assertThat(bill.isActive(), is(false));
        
        // failed change
        url = "http://"+uri+":"+port+"/bills/5/status";
        uriBuilder = UriComponentsBuilder.fromUriString(url);
		webTestClient.patch()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isNotFound()
		.expectBody().isEmpty();
/* 		
		status = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.PATCH, null, Boolean.class);
        assertThat(status.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(status.hasBody(), is(false));
 */        
        // update balance
        url = "http://"+uri+":"+port+"/bills";
        Map<Integer, Double> data = new LinkedHashMap<>();
        data.put(1, -3.00);
        data.put(2, 44.00);
        // HttpEntity<Map<Integer, Double>> request = new HttpEntity<>(data);
		webTestClient.patch()
		.uri(url)
		.bodyValue(data)
		.exchange()
		.expectStatus().isNoContent()
		.expectBody().isEmpty();
/* 		
		ResponseEntity<Void> update = 
				restTemplate.exchange(url, HttpMethod.PATCH, request, Void.class);
        assertThat(update.getStatusCode(), is(HttpStatus.NO_CONTENT));
 */ 
		webTestClient.get()
		.uri("/bills/1/list")
		.exchange()
		.expectStatus().isOk()
		.expectBody(new ParameterizedTypeReference<Set<BillDTO>>(){})
		.value(body -> {assertThat(body).hasSize(2);
						List<BillDTO> list = List.copyOf(body);
						assertThat(list.get(0).getBalance().doubleValue(), is(7.00));
						assertThat(list.get(1).getBalance().doubleValue(), is(44.00));
						assertThat(list.get(1).isActive(), is(false));
		});
/* 
		response = restTemplate.exchange("/bills/1/list", HttpMethod.GET, 
				null, new ParameterizedTypeReference<Set<BillDTO>>(){});
		Set<BillDTO> set = response.getBody();
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(set.size(), is(2));
		List<BillDTO> list = new ArrayList<>(set);
		assertThat(list.get(0).getBalance().doubleValue(), is(7.00));
		assertThat(list.get(1).getBalance().doubleValue(), is(44.00));
		assertThat(list.get(1).isActive(), is(false));
 */        
        // inspect message
        // Queue<Message<?>> queue = collector.forChannel(source.output());
        // Message<?> message = queue.poll();
		Message<byte[]> message = outputDestination.receive();
        assertThat(new String(message.getPayload())).isEqualTo(mapper.writeValueAsString(data));
        
        // delete
        url = "http://"+uri+":"+port+"/bills/2";
        uriBuilder = UriComponentsBuilder.fromUriString(url);
		webTestClient.delete()
		.uri(uriBuilder.toUriString())
		.exchange()
		.expectStatus().isNoContent();
/* 		
        ResponseEntity<Void> delete = restTemplate.exchange(uriBuilder.toUriString(), 
        		HttpMethod.DELETE, null, Void.class);
        assertThat(delete.getStatusCode(), is(HttpStatus.NO_CONTENT));
 */        
        // check
		webTestClient.get()
		.uri("/bills/1/list")
		.exchange()
		.expectStatus().isOk()
		.expectBody(new ParameterizedTypeReference<Set<BillDTO>>(){})
		.value(body -> {assertThat(body).hasSize(1);
						assertThat(body.iterator().next().getBalance().doubleValue(), is(7.00));
		});
/* 
		response = restTemplate.exchange("/bills/1/list", HttpMethod.GET, 
				null, new ParameterizedTypeReference<Set<BillDTO>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().size(), is(1));
		assertThat(response.getBody().iterator().next().getBalance().doubleValue(), is(7.00));
 */
	}
	@AfterAll void clear(CacheManager cacheManager){
		populator = null;
		cacheManager.getCacheNames()
		.forEach(cacheName -> {
			var cache = cacheManager.getCache(cacheName);
			if (cache != null) cache.clear();
		});
	}
/* 	
	@AfterAll void clear(HazelcastInstance hazelcastInstance) {
		populator = null;
		hazelcastInstance.getMap("bills").clear();
		hazelcastInstance.getMap("dtos").clear();
		hazelcastInstance.getMap("sets").clear();
	}
 */
}
