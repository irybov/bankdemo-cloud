package com.github.irybov.bill;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.irybov.shared.BillDTO;

@WebMvcTest(BillController.class)
@Import(BillMapperImpl.class)
public class BillControllerTest {
	
	@MockBean
	private BillService service;
	@Autowired
	private MockMvc mockMVC;
	
	private static BillMapper mapStruct;	
	private static String currency;
	@BeforeAll
	static void prepare() {currency = "SEA"; mapStruct = Mappers.getMapper(BillMapper.class);}
	
	private BillDTO bill;
	@BeforeEach
	void set_up() {
		Bill entity = new Bill(currency, 0);
		entity.create();
		bill = mapStruct.toDTO(entity);
	}
	
	@Test
	void can_create() throws Exception {
		
//		doNothing().when(service).create(anyString(), anyInt());
		when(service.create(anyString(), anyInt())).thenReturn(bill);
		
		mockMVC.perform(post("/bills")
				.param("currency", currency)
				.param("owner", "1"))
		.andExpect(status().isCreated());
		
		verify(service).create(anyString(), anyInt());
	}
	
	@Test
	void fail_creation() throws Exception {
		
		mockMVC.perform(post("/bills")
				.param("currency", "coin")
				.param("owner", "-1"))
		.andExpect(result -> assertThat
				(result.getResolvedException() instanceof ConstraintViolationException).isTrue())
		.andExpect(status().isBadRequest())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.length()").value(2))
		.andExpect(jsonPath("$", hasItem("Currency should be 3 capital letters")))
		.andExpect(jsonPath("$", hasItem("Owner's id should be positive")));
	}
	
	@Test
	void can_get_one() throws Exception {
		
		when(service.getOne(anyInt())).thenReturn(bill);
		
		mockMVC.perform(get("/bills/{id}", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.createdAt").exists())
		.andExpect(jsonPath("$.balance").value(0.00))
		.andExpect(jsonPath("$.currency").value("SEA"))
		.andExpect(jsonPath("$.isActive").value(true));
		
		verify(service).getOne(anyInt());
	}
	
	@Test
	void try_get_absent_one() throws Exception {
		
		when(service.getOne(anyInt())).thenThrow(new NoSuchElementException());
		
		mockMVC.perform(get("/bills/{id}", "0"))
		.andExpect(result -> assertThat
				(result.getResolvedException() instanceof NoSuchElementException).isTrue())
		.andExpect(status().isNotFound());
		
		verify(service).getOne(anyInt());
	}
	
	@Test
	void can_get_list() throws Exception {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> list = 
//				Collections.nCopies(size, bill).stream()
				Stream.generate(() -> new BillDTO()).limit(size)
				.collect(Collectors.toList());
		int i = 0;
		for(BillDTO bill : list) bill.setId(new Integer(++i));
		Set<BillDTO> bills = new HashSet<>(list);
		
		when(service.getList(anyInt())).thenReturn(bills);
		
		mockMVC.perform(get("/bills/{owner}/list", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.length()").value(size));
		
		verify(service).getList(anyInt());
	}
	
	@Test
	void try_get_empty_list() throws Exception {
		
//		when(service.getList(anyInt())).thenReturn(new LinkedList<BillDTO>());
		when(service.getList(anyInt())).thenReturn(new HashSet<BillDTO>());
		
		mockMVC.perform(get("/bills/{owner}/list", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$").isEmpty());
		
		verify(service).getList(anyInt());
	}
	
	@Test
	void can_change_status() throws Exception {
		
		when(service.changeStatus(anyInt())).thenReturn(false);
		
		mockMVC.perform(patch("/bills/{id}/status", "0"))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$").isBoolean());
		
		verify(service).changeStatus(anyInt());
	}
	
	@Test
	void try_change_status() throws Exception {
		
		when(service.changeStatus(anyInt())).thenThrow(new EmptyResultDataAccessException(1));
		
		mockMVC.perform(patch("/bills/{id}/status", "0"))
		.andExpect(result -> assertThat
				(result.getResolvedException() instanceof EmptyResultDataAccessException).isTrue())
		.andExpect(status().isNotFound());
		
		verify(service).changeStatus(anyInt());
	}
	
	@Test
	void can_update_balance() throws Exception {
		
        Map<Integer, Double> data = new LinkedHashMap<>();
        data.put(1, -3.00);
        data.put(2, 44.00);
		
		doNothing().when(service).updateBalance(any(Map.class));
		
		mockMVC.perform(patch("/bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(data)))
		.andExpect(status().isNoContent());
		
		verify(service).updateBalance(any(Map.class));
	}
	
	@Test
	void can_delete() throws Exception {
		
		doNothing().when(service).delete(anyInt());
		
		mockMVC.perform(delete("/bills/{id}", "0"))
		.andExpect(status().isNoContent());
		
		verify(service).delete(anyInt());
	}

    @AfterAll
    static void clear() {currency = null;}
    @AfterEach
    void tear_down() {bill = null;}
    
}
