package com.github.irybov.account;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
public class AccountControllerTest {
	
	@MockBean
	private AccountService service;
	@Autowired
	private MockMvc mockMVC;
	
	private static Account account;
	@BeforeAll
	static void prepare() {account = new Account(Timestamp.valueOf(OffsetDateTime.now()
			.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()), 
			"Admin", "Adminov", "0000000000", "adminov@greenmail.io", 
			LocalDate.of(2001, 01, 01), "superadmin");}
	
	@Test
	void can_get_one() throws Exception {
		
		when(service.getOne(anyInt())).thenReturn(account);
		
		mockMVC.perform(get("/accounts/{id}", "0"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.createdAt").exists())
		.andExpect(jsonPath("$.birthday").exists())
		.andExpect(jsonPath("$.name").value("Admin"))
		.andExpect(jsonPath("$.surname").value("Adminov"))
		.andExpect(jsonPath("$.phone").value("0000000000"))
		.andExpect(jsonPath("$.email").value("adminov@greenmail.io"))
		.andExpect(jsonPath("$.password").value("superadmin"))
		.andExpect(jsonPath("$.isActive").value(false));
		
		verify(service).getOne(anyInt());
	}

	@Test
	void can_get_all() throws Exception {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Account> accounts = Collections.nCopies(size, account).stream()
				.collect(Collectors.toList());
		when(service.getAll()).thenReturn(accounts);
		
		mockMVC.perform(get("/accounts"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$.length()").value(size));
		
		verify(service).getAll();
	}
	
	@AfterAll
	static void clear() {account = null;}
	
}
