package com.github.irybov.bill;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

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
				.param("owner", "0"))
		.andExpect(status().isCreated());
		
		verify(service).create(anyString(), anyInt());
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
	void can_get_list() throws Exception {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> bills = Collections.nCopies(size, bill).stream()
				.collect(Collectors.toList());
		
		when(service.getList(anyInt())).thenReturn(bills);
		
		mockMVC.perform(get("/bills/{owner}/list", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.length()").value(size));
		
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
	void can_update_balance() throws Exception {
		
        Map<Integer, Double> data = new LinkedHashMap<>();
        data.put(1, -3.00);
        data.put(2, 44.00);
		
		doNothing().when(service).updateBalance(any(Map.class));
		
		mockMVC.perform(patch("/bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(data)))
		.andExpect(status().isOk());
		
		verify(service).updateBalance(any(Map.class));
	}
	
	@Test
	void can_delete() throws Exception {
		
		doNothing().when(service).delete(anyInt());
		
		mockMVC.perform(delete("/bills/{id}", "0"))
		.andExpect(status().isOk());
		
		verify(service).delete(anyInt());
	}

    @AfterAll
    static void clear() {currency = null;}
    @AfterEach
    void tear_down() {bill = null;}
    
}
