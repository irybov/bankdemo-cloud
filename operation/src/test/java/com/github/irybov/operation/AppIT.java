package com.github.irybov.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@Transactional
//@Sql("/test-operations-h2.sql")
@TestInstance(Lifecycle.PER_CLASS)
class AppIT {
	
	@Autowired
	private MockMvc mockMVC;
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private TestRestTemplate restTemplate;
	
	@Autowired
	private DataSource dataSource;	
	private ResourceDatabasePopulator populator;
	
	@BeforeAll
	void prepare() {		
		populator = new ResourceDatabasePopulator();
		populator.addScripts(new ClassPathResource("test-operations-h2.sql"));
		populator.execute(dataSource);
	}
	
	@Test
	void context_loading(ApplicationContext context) {
		assertThat(context).isNotNull();
	}
	
	@Test
	void can_save() throws Exception {
		
		Operation.OperationBuilder builder = Operation.builder();
		Operation operation = builder
			.amount(0.00)
			.action("external")
			.currency("SEA")
			.createdAt(Timestamp.valueOf(OffsetDateTime.now()
					.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
			.bank("Demo")
			.build();
/*		
		mockMVC.perform(post("/operations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(operation)))
		.andExpect(status().isCreated());		
*/
		ResponseEntity<Void> response = 
				restTemplate.postForEntity("/operations", operation, Void.class);
	    assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
	}
	
	@Test
	void can_get_one() throws Exception {
/*		
		mockMVC.perform(get("/operations/{id}", "1"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.id").value(1))
		.andExpect(jsonPath("$.createdAt").exists())
		.andExpect(jsonPath("$.amount").value(500.00))
		.andExpect(jsonPath("$.action").value("deposit"))
		.andExpect(jsonPath("$.bank").value("Demo"))
		.andExpect(jsonPath("$.sender").value(0))
		.andExpect(jsonPath("$.recipient").value(1))
		.andExpect(jsonPath("$.currency").value("RUB"));
*/		
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
		
/*		
		mockMVC.perform(get("/operations/{id}", "6"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.id").value(6))
		.andExpect(jsonPath("$.createdAt").exists())
		.andExpect(jsonPath("$.amount").value(800.00))
		.andExpect(jsonPath("$.action").value("transfer"))
		.andExpect(jsonPath("$.bank").value("Demo"))
		.andExpect(jsonPath("$.sender").value(2))
		.andExpect(jsonPath("$.recipient").value(3))
		.andExpect(jsonPath("$.currency").value("RUB"));
*/		
		response = restTemplate.getForEntity("/operations/6", Operation.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
	    assertThat(response.getBody().getId(), is(6L));
	    assertThat(response.getBody().getAction(), equalTo("transfer"));
	    assertThat(response.getBody().getAmount(), is(800.00));
	    assertThat(response.getBody().getBank(), equalTo("Demo"));
	    assertThat(response.getBody().getCreatedAt(), notNullValue());
	    assertThat(response.getBody().getCurrency(), equalTo("RUB"));
	    assertThat(response.getBody().getSender(), is(1));
	    assertThat(response.getBody().getRecipient(), is(3));
	}
	
	@Test
	void can_get_list() throws Exception {
/*		
		mockMVC.perform(get("/operations/{id}/list", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.length()").value(4));
*/		
		ResponseEntity<List<Operation>> response = 
				restTemplate.exchange("/operations/0/list", HttpMethod.GET, null, 
						new ParameterizedTypeReference<List<Operation>>(){});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().size(), is(4));
	}

	@Test
	void can_get_page() throws Exception {
		
		mockMVC.perform(get("/operations/{id}/pageable", "1")
						.param("sort", "amount,asc")
						.param("sort", "id,desc")
						.param("page", "0")
						.param("size", "2")
				)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.pageable").exists())
			.andExpect(jsonPath("$.sort").exists())
			.andExpect(jsonPath("$['sort']['sorted']").value("true"))
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content.length()", is(2)))
			.andDo(print());

//        String url = "http://localhost:8888/operations/1/pageable";
//        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url)
//          .queryParam("sort", "amount,asc")
//          .queryParam("sort", "id,desc")
//          .queryParam("page", 0)
//          .queryParam("size", 2);
//		
//        ResponseEntity<PageImpl<Operation>> response = restTemplate.exchange(uriBuilder.toUriString(),
//                HttpMethod.GET, null, new ParameterizedTypeReference<PageImpl<Operation>>(){});
//        assertThat(response.getStatusCode(), is(HttpStatus.OK));
//        assertThat(response.getBody().getContent().size(), is(2));
	}

	@AfterAll void clear() {populator = null;}
	
}
