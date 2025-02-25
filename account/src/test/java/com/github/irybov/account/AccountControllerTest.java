package com.github.irybov.account;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;

@WebMvcTest(AccountController.class)
@Import(AccountMapperImpl.class)
public class AccountControllerTest {
	
	@MockBean
	private AccountService service;
	@Autowired
	private MockMvc mockMVC;
	@Autowired
	private ObjectMapper mapper;
	
	private static AccountMapper mapStruct;
	private static Account account;
	@BeforeAll
	static void prepare() {
		
		account = new Account();
		account.setCreatedAt(Timestamp.valueOf(OffsetDateTime.now()
				.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()));
		account.setName("Admin");
		account.setSurname("Adminov");
		account.setPhone("0000000000");
		account.setEmail("adminov@greenmail.io");
		account.setBirthday(LocalDate.of(2001, 01, 01));
		account.setPassword("superadmin");
		account.setRoles(Collections.singleton(Role.ADMIN.getName()));
	
		mapStruct = Mappers.getMapper(AccountMapper.class);
	}
	
	@Test
	void can_create() throws Exception {
		
		Registration registration = new Registration();
		registration.setName("Admin");
		registration.setSurname("Adminov");
		registration.setPhone("0000000000");
		registration.setEmail("adminov@greenmail.io");
		registration.setBirthday(LocalDate.of(2001, 01, 01));
		registration.setPassword("superadmin");
		
		doNothing().when(service).create(refEq(registration));
		
		mockMVC.perform(post("/accounts")
		.contentType(MediaType.APPLICATION_JSON)
		.content(mapper.writeValueAsString(registration)))
		.andExpect(status().isCreated());
		
		verify(service).create(refEq(registration));
	}
	
	@Test
	void can_get_token() throws Exception {
		
		when(service.generateToken(anyString())).thenReturn("token");
		
		mockMVC.perform(head("/accounts/login")
		.header("Login", "0000000000:superadmin"))
		.andExpect(header().string("Token", "token"))
		.andExpect(status().isOk());
		
		verify(service).generateToken(anyString());
	}
	
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
//		when(service.checkFraud(anyString(), anyString())).thenReturn(true);
		
//		mockMVC.perform(get("/accounts/{id}", "0"))
		mockMVC.perform(get("/accounts/{phone}", "0000000000"))
//				.header(HttpHeaders.AUTHORIZATION, "jwt"))
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
//		verify(service).checkFraud(anyString(), anyString());
	}

	@Test
	void can_get_all() throws Exception {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Account> accounts = Collections.nCopies(size, account).stream()
				.collect(Collectors.toList());
		when(service.getAll()).thenReturn(mapStruct.toList(accounts));
		
		mockMVC.perform(get("/accounts"))
//				.header(HttpHeaders.AUTHORIZATION, "jwt"))
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
//		when(service.checkFraud(anyString(), anyString())).thenReturn(true);
		
		mockMVC.perform(post("/accounts/{phone}/bills", "0000000000")
//				.header(HttpHeaders.AUTHORIZATION, "jwt")
				.param("currency", "SEA"))
		.andExpect(status().isCreated())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON));
		
		verify(service).addBill(anyString(), anyString());
//		verify(service).checkFraud(anyString(), anyString());
	}
	
	@Test
	void can_delete_bill() throws Exception {
		
		doNothing().when(service).deleteBill(anyString(), anyInt());
		
		mockMVC.perform(delete("/accounts/{phone}/bills/{id}", "0000000000", "0"))
		.andExpect(status().isNoContent());
		
		verify(service).deleteBill(anyString(), anyInt());
	}
	
	@AfterAll
	static void clear() {account = null;}
	
}
