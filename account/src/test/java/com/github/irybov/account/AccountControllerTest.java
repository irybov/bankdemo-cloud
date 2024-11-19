package com.github.irybov.account;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.irybov.shared.BillDTO;

@WebMvcTest(AccountController.class)
@Import(AccountMapperImpl.class)
public class AccountControllerTest {
	
	@MockBean
	private AccountService service;
	@Autowired
	private MockMvc mockMVC;
	
	private static AccountMapper mapStruct;
	private static Account account;
	@BeforeAll
	static void prepare() {account = new Account(Timestamp.valueOf(OffsetDateTime.now()
			.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()), 
			"Admin", "Adminov", "0000000000", "adminov@greenmail.io", 
			LocalDate.of(2001, 01, 01), "superadmin"); 
			mapStruct = Mappers.getMapper(AccountMapper.class);}
	
	@Test
	void can_get_one() throws Exception {
		
		AccountDTO dto = mapStruct.toDTO(account);		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> bills = Stream.generate(() -> new BillDTO()).limit(size)
				.collect(Collectors.toList());
		int i = 1;
		for(BillDTO bill : bills) {bill.setId(new Integer(i++));}
		dto.setBills(new HashSet<>(bills));
		
//		when(service.getOne(anyInt())).thenReturn(mapStruct.toDTO(account));
		when(service.getOne(anyString())).thenReturn(dto);
		
//		mockMVC.perform(get("/accounts/{id}", "0"))
		mockMVC.perform(get("/accounts/{phone}", "0000000000"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.createdAt").exists())
		.andExpect(jsonPath("$.birthday").exists())
		.andExpect(jsonPath("$.name").value("Admin"))
		.andExpect(jsonPath("$.surname").value("Adminov"))
		.andExpect(jsonPath("$.phone").value("0000000000"))
		.andExpect(jsonPath("$.email").value("adminov@greenmail.io"))
		.andExpect(jsonPath("$.bills").isArray())
		.andExpect(jsonPath("$.bills.length()").value(size))
		.andExpect(jsonPath("$.isActive").value(false));
		
//		verify(service).getOne(anyInt());
		verify(service).getOne(anyString());
	}

	@Test
	void can_get_all() throws Exception {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Account> accounts = Collections.nCopies(size, account).stream()
				.collect(Collectors.toList());
		when(service.getAll()).thenReturn(mapStruct.toList(accounts));
		
		mockMVC.perform(get("/accounts"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.length()").value(size));
		
		verify(service).getAll();
	}
	
	@Test
	void can_add_bill() throws Exception {
		
		BillDTO bill = new BillDTO();		
		when(service.addBill(anyString(), anyString())).thenReturn(bill);
		
		mockMVC.perform(patch("/accounts/{phone}", "0000000000")
				.param("currency", "SEA"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON));
		
		verify(service).addBill(anyString(), anyString());
	}
	
	@AfterAll
	static void clear() {account = null;}
	
}
