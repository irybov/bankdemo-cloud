package com.github.irybov.account;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AccountServiceTest {
	
	@Mock
	private Environment env;
	@Mock
	private Validator validator;
	@Mock
	private RestTemplate restTemplate;
	@Spy
	private AccountMapperImpl mapStruct;
	@Mock
	private AccountJDBC jdbc;
	@InjectMocks
	private AccountService service;
	
	private Account account;
	
	@BeforeEach
	void prepare() {
		
		MockitoAnnotations.openMocks(this);
		service = new AccountService(env, validator, restTemplate, mapStruct, jdbc);
		
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
	}
	
	@Test
	void can_create() {
		
		Registration registration = new Registration();
		registration.setName("Admin");
		registration.setSurname("Adminov");
		registration.setPhone("0000000000");
		registration.setEmail("adminov@greenmail.io");
		registration.setBirthday(LocalDate.of(2001, 01, 01));
		registration.setPassword("superadmin");
		
		when(jdbc.save(account)).thenReturn(account);
		service.create(registration);
		assertThat(mapStruct.toDB(registration)).isExactlyInstanceOf(Account.class);
		verify(jdbc).save(account);
	}
	
	@Test
	void can_generate_token() {
		
		when(env.getProperty("token.secret")).thenReturn("rAUOQK5LF3s0unfY8jbOkJc8Ep9H9v3Y");
		when(env.getProperty("token.lifetime")).thenReturn("300");
		when(jdbc.findByPhone(anyString())).thenReturn(account);
		assertThat(service.generateToken("0000000000:superadmin"))
			.isExactlyInstanceOf(String.class);
		verify(jdbc).findByPhone(anyString());
		verify(env).getProperty("token.secret");
		verify(env).getProperty("token.lifetime");
	}

	@Test
	void can_get_one() {
		
		Optional<Account> optional = Optional.of(account);
/*		
		when(jdbc.findById(anyInt())).thenReturn(optional);
		assertThat(service.getOne(anyInt())).isExactlyInstanceOf(AccountDTO.class);
		verify(jdbc).findById(anyInt());
*/		
		BillDTO bill = new BillDTO();
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> bills = Collections.nCopies(size, bill).stream()
				.collect(Collectors.toList());
		
		when(jdbc.findByPhone(anyString())).thenReturn(account);
		when(restTemplate.exchange("http://BILL/bills/" + account.getId() + "/list", 
				HttpMethod.GET, null, new ParameterizedTypeReference<List<BillDTO>>(){}))
			.thenReturn(new ResponseEntity<List<BillDTO>>(bills, HttpStatus.OK));
		assertThat(service.getOne(anyString())).isExactlyInstanceOf(AccountDTO.class);
		verify(jdbc).findByPhone(anyString());
		verify(restTemplate).exchange("http://BILL/bills/" + account.getId() + "/list", 
				HttpMethod.GET, null, new ParameterizedTypeReference<List<BillDTO>>(){});
	}
	
	@Test
	void can_get_all() {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Account> accounts = Collections.nCopies(size, account).stream()
				.collect(Collectors.toList());
		
		when(jdbc.findAll()).thenReturn(accounts);
		List<AccountDTO> results = service.getAll();
		assertAll(
				() -> assertThat(results).hasSameClassAs(new ArrayList<AccountDTO>()),
				() -> assertThat(results.size()).isEqualTo(accounts.size()));
		verify(jdbc).findAll();
	}
	
	@Test
	void can_add_bill() {
		
		BillDTO bill = new BillDTO();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://BILL/bills")
    	        .queryParam("currency", "SEA")
    	        .queryParam("owner", account.getId());
		
		when(jdbc.findByPhone(anyString())).thenReturn(account);
		when(jdbc.save(any(Account.class))).thenReturn(account);
		when(restTemplate.postForObject(uriBuilder.toUriString(), null, BillDTO.class))
			.thenReturn(bill);
		
		assertThat(service.addBill(anyString(), "SEA")).isExactlyInstanceOf(BillDTO.class);
		verify(jdbc).findByPhone(anyString());
		verify(jdbc).save(any(Account.class));
	}
	
	@AfterEach
	void clear() {service = null; account = null;}
	
}
