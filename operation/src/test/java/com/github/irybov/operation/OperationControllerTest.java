package com.github.irybov.operation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

@WebMvcTest(OperationController.class)
class OperationControllerTest {
	
    @MockBean
    private DataSource dataSource;
	@MockBean
	private OperationService service;
	@Autowired
	private MockMvc mockMVC;
	@Autowired
	private ObjectMapper mapper;
	
	private static Operation operation;
	private static Operation.OperationBuilder builder;
	
	@BeforeAll
	static void prepare() {
//		operation = new Operation();
		builder = mock(Operation.OperationBuilder.class, Mockito.CALLS_REAL_METHODS);
		operation = builder
				.amount(0.00)
				.action("unknown")
				.currency("SEA")
				.createdAt(Timestamp.valueOf(OffsetDateTime.now()
						.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
				.bank("Demo")
				.build();
	}

	@Test
	void can_save() throws Exception {
		
		doNothing().when(service).save(any(OperationDTO.class));
		
		mockMVC.perform(post("/operations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(
						new OperationDTO(0.01, Action.EXTERNAL, "SEA", 4, 4, "Demo"))))
		.andExpect(status().isCreated());
		
		verify(service).save(any(OperationDTO.class));
	}
	
	@Test
	void can_get_one() throws Exception {
		
//		Operation.OperationBuilder builder = Operation.builder();
//		Operation operation = builder
//			.amount(0.00)
//			.action("unknown")
//			.currency("SEA")
//			.createdAt(Timestamp.valueOf(OffsetDateTime.now()
//					.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
//			.bank("Demo")
//			.build();
		
		when(service.getOne(anyLong())).thenReturn(operation);
		
		mockMVC.perform(get("/operations/{id}", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
//		.andExpect(jsonPath("$.id").exists())
		.andExpect(jsonPath("$.createdAt").exists())
		.andExpect(jsonPath("$.amount").value(0.00))
		.andExpect(jsonPath("$.action").value("unknown"))
		.andExpect(jsonPath("$.bank").value("Demo"))
		.andExpect(jsonPath("$.currency").value("SEA"));
		
		verify(service).getOne(anyLong());
	}
	
	@Test
	void try_get_absent_one() throws Exception {
		
		when(service.getOne(anyLong())).thenThrow(new NoSuchElementException());
		
		mockMVC.perform(get("/operations/{id}", "0"))
		.andExpect(result -> assertThat
				(result.getResolvedException() instanceof NoSuchElementException).isTrue())
		.andExpect(status().isNotFound());
		
		verify(service).getOne(anyLong());
	}
	
	@Test
	void can_get_list() throws Exception {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Operation> operations = Collections.nCopies(size, operation).stream()
				.collect(Collectors.toList());
		when(service.getList(anyInt())).thenReturn(operations);
		
		MvcResult result = mockMVC.perform(get("/operations/{id}/list", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.length()").value(size))
		.andReturn();
		
		String json = result.getResponse().getContentAsString();
		CollectionType collectionType = mapper.getTypeFactory()
				.constructCollectionType(List.class, Operation.class);
		List<Operation> list = mapper.readValue(json, collectionType);
		
		assertNotNull(list);
		assertEquals(size, list.size());
		
		verify(service).getList(anyInt());
	}
	
	@Test
	void try_get_empty_list() throws Exception {
		
		when(service.getList(anyInt())).thenReturn(new ArrayList<Operation>());
		
		mockMVC.perform(get("/operations/{id}/list", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$").isEmpty());
		
		verify(service).getList(anyInt());
	}
	
	@Test
	void can_get_page() throws Exception {
		
		Operation entity = new Operation();
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Operation> operations = Collections.nCopies(size, entity).stream()
				.collect(Collectors.toList());
		Page<Operation> operationPage = new PageImpl<>(operations);
		
		when(service.getPage(anyInt(), anyString(), anyDouble(), anyDouble(),
				any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
				.thenReturn(operationPage);
		
		mockMVC.perform(get("/operations/{id}/page", "0")
						.param("action", "unknown")
						.param("minval", "0.01")
						.param("maxval", "0.02")
						.param("mindate", "1900-01-01")
						.param("maxdate", "2020-01-01")
						.param("sort", "amount,asc")
						.param("sort", "id,desc")
						.param("page", "0")
						.param("size", "0")
				)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pageable").exists())
			.andExpect(jsonPath("$.sort").exists())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content.length()").value(operations.size()));
		
		verify(service).getPage(anyInt(), anyString(), anyDouble(), anyDouble(),
				any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class));
	}

    @AfterAll
    static void clear() {
    	operation = null;
    	builder = null;
    }
	
}
