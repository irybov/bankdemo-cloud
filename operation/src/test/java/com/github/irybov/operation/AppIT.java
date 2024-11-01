package com.github.irybov.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
	private DataSource dataSource;
	
	private ResourceDatabasePopulator populator;
	
	@BeforeAll
	void prepare() {		
		populator = new ResourceDatabasePopulator();
		populator.addScripts(new ClassPathResource("test-operations-h2.sql"));
		populator.execute(dataSource);
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
		
		mockMVC.perform(post("/operations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(operation)))
		.andExpect(status().isCreated());
	}
	
	@Test
	void can_get_one() throws Exception {
		
		mockMVC.perform(get("/operations/{id}", "1"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.id").value(1))
		.andExpect(jsonPath("$.createdAt").exists())
		.andExpect(jsonPath("$.amount").value("500.0"))
		.andExpect(jsonPath("$.action").value("deposit"))
		.andExpect(jsonPath("$.bank").value("Demo"))
		.andExpect(jsonPath("$.sender").value(0))
		.andExpect(jsonPath("$.recipient").value(1))
		.andExpect(jsonPath("$.currency").value("RUB"));
		
		mockMVC.perform(get("/operations/{id}", "6"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.id").value(6))
		.andExpect(jsonPath("$.createdAt").exists())
		.andExpect(jsonPath("$.amount").value("800.0"))
		.andExpect(jsonPath("$.action").value("transfer"))
		.andExpect(jsonPath("$.bank").value("Demo"))
		.andExpect(jsonPath("$.sender").value(2))
		.andExpect(jsonPath("$.recipient").value(3))
		.andExpect(jsonPath("$.currency").value("RUB"));
	}
	
	@Test
	void can_get_list() throws Exception {
		
		mockMVC.perform(get("/operations/{id}/list", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.length()").value(4));
	}

	@Test
	void can_get_page() throws Exception {
		
		mockMVC.perform(get("/operations/{id}/page", "1")
						.param("sort", "amount,asc")
						.param("sort", "id,desc")
				)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pageable").exists())
			.andExpect(jsonPath("$.sort").exists())
			.andExpect(jsonPath("$['sort']['sorted']").value("true"))
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content.length()", is(3)));
	}

	@AfterAll void clear() {populator = null;}
	
}
