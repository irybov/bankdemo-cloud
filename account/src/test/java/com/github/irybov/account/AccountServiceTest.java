package com.github.irybov.account;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AccountServiceTest {
	
	@Mock
	private Environment env;
//	@Mock
//	private Validator validator;
	@Mock
	private BillClient billClient;
//	private RestTemplate restTemplate;
	@Spy
	private AccountMapperImpl mapStruct;
	@Mock
	private AccountJDBC jdbc;
	@InjectMocks
	private AccountService service;
	@Mock
	private AccountService self;
	
	private Account account;
	
	@BeforeEach
	void prepare() {
		
		MockitoAnnotations.openMocks(this);
		service = new AccountService(env, billClient, mapStruct, jdbc);
		ReflectionTestUtils.setField(service, "self", self);
		
		account = new Account();
		account.setId(0);
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
		
		when(self.update(any(Account.class))).thenReturn(account);
//		when(jdbc.save(account)).thenReturn(account);
		
		service.create(registration);
		assertThat(mapStruct.toDB(registration)).isExactlyInstanceOf(Account.class);
		
//		verify(jdbc).save(account);
		verify(self).update(any(Account.class));
	}
	
	@Test
	void can_update() {
		
		when(jdbc.save(any(Account.class))).thenReturn(account);
		assertThat(service.update(account)).isExactlyInstanceOf(Account.class);;
		verify(jdbc).save(any(Account.class));
	}
	
	@Test
	void can_generate_token() {
		
		when(env.getProperty("token.secret")).thenReturn("rAUOQK5LF3s0unfY8jbOkJc8Ep9H9v3Y");
		when(env.getProperty("token.lifetime")).thenReturn("300");
//		when(jdbc.findByPhone(anyString())).thenReturn(account);
		when(self.getAccount(anyString())).thenReturn(account);
		
		assertThat(service.generateToken("0000000000:superadmin"))
			.isExactlyInstanceOf(String.class);
		
//		verify(jdbc).findByPhone(anyString());
		verify(self).getAccount(anyString());
		verify(env).getProperty("token.secret");
		verify(env).getProperty("token.lifetime");
	}
	
	@Test
	void try_wrong_password() {
		
		String header = "0000000000:superclown";
		
		when(self.getAccount(anyString())).thenReturn(account);
//		when(jdbc.findByPhone(anyString())).thenReturn(account);
		
		assertThrows(ResponseStatusException.class, () -> service.generateToken(header));
		assertThatThrownBy(() -> service.generateToken(header)).isInstanceOf(ResponseStatusException.class);
		assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> service.generateToken(header));
		
//		verify(jdbc, times(3)).findByPhone(anyString());
		verify(self, times(3)).getAccount(anyString());
	}

	@Test
	void can_get_one() {
		
//		Optional<Account> optional = Optional.of(account);
/*		
		when(jdbc.findById(anyInt())).thenReturn(optional);
		assertThat(service.getOne(anyInt())).isExactlyInstanceOf(AccountDTO.class);
		verify(jdbc).findById(anyInt());
*/		
//		BillDTO bill = new BillDTO();
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<BillDTO> list = 
//				Collections.nCopies(size, bill).stream()
				Stream.generate(() -> new BillDTO()).limit(size)
				.collect(Collectors.toList());
		int i = 0;
		for(BillDTO bill : list) bill.setId(new Integer(++i));
		Set<BillDTO> bills = new HashSet<>(list);

		when(self.getAccount(anyString())).thenReturn(account);
//		when(jdbc.findByPhone(anyString())).thenReturn(account);
//		when(restTemplate.exchange("http://BILL/bills/" + account.getId() + "/list", 
//				HttpMethod.GET, null, new ParameterizedTypeReference<List<BillDTO>>(){}))
//			.thenReturn(new ResponseEntity<List<BillDTO>>(bills, HttpStatus.OK));
		when(billClient.getList(anyInt())).thenReturn(bills);
		
		AccountDTO account = service.getOne(anyString());
		assertThat(account).isExactlyInstanceOf(AccountDTO.class);
		assertThat(account.getBills().size()).isEqualTo(size);

		verify(self).getAccount(anyString());
//		verify(jdbc).findByPhone(anyString());
//		verify(restTemplate).exchange("http://BILL/bills/" + account.getId() + "/list", 
//				HttpMethod.GET, null, new ParameterizedTypeReference<List<BillDTO>>(){});
		verify(billClient).getList(anyInt());
	}
	
	@Test
	void request_absent() {
		
		when(jdbc.findByPhone(anyString())).thenReturn(null);
		
		assertThrows(ResponseStatusException.class, () -> service.getAccount(anyString()));
		assertThatThrownBy(() -> service.getAccount(anyString())).isInstanceOf(ResponseStatusException.class);
		assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> service.getAccount(anyString()));
		
		verify(jdbc, times(3)).findByPhone(anyString());
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
	void can_change_password() {

		when(self.getAccount(anyString())).thenReturn(account);
//		when(jdbc.findByPhone(anyString())).thenReturn(account);
//		when(jdbc.save(account)).thenReturn(account);
		when(self.update(any(Account.class))).thenReturn(account);
		
		service.changePassword(anyString(), "terminator");
		assertThat(account.getPassword()).isEqualTo("terminator");
		
		verify(self).getAccount(anyString());
//		verify(jdbc).findByPhone(anyString());
//		verify(jdbc).save(account);
		verify(self).update(any(Account.class));
	}
	
	@Test
	void can_add_bill() {
		
		BillDTO bill = new BillDTO();
//        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://BILL/bills")
//    	        .queryParam("currency", "SEA")
//    	        .queryParam("owner", account.getId());

		when(self.getAccount(anyString())).thenReturn(account);
//		when(jdbc.findByPhone(anyString())).thenReturn(account);
		when(self.update(any(Account.class))).thenReturn(account);
//		when(jdbc.save(any(Account.class))).thenReturn(account);
//		when(restTemplate.postForObject(uriBuilder.toUriString(), null, BillDTO.class))
//			.thenReturn(bill);
		when(billClient.create(anyString(), anyInt())).thenReturn(bill);
		
		assertThat(service.addBill(anyString(), "SEA")).isExactlyInstanceOf(BillDTO.class);
//		verify(jdbc).findByPhone(anyString());
		verify(self).update(any(Account.class));
//		verify(jdbc).save(any(Account.class));
//		verify(restTemplate).postForObject(uriBuilder.toUriString(), null, BillDTO.class);
		verify(billClient).create(anyString(), anyInt());
		verify(self).getAccount(anyString());
	}
	
	@Test
	void can_delete_bill() {
		
		Set<Integer> bills = IntStream.range(1,5).collect(HashSet::new, Set::add, Set::addAll);
		account.setBills(bills);

		when(self.getAccount(anyString())).thenReturn(account);
//		when(jdbc.findByPhone(anyString())).thenReturn(account);
		when(self.update(any(Account.class))).thenReturn(account);
		doNothing().when(billClient).delete(anyInt());
		
		service.deleteBill(anyString(), 1);
		assertThat(account.getBills().size()).isEqualTo(3);

		verify(self).getAccount(anyString());
//		verify(jdbc).findByPhone(anyString());
		verify(self).update(any(Account.class));
		verify(billClient).delete(anyInt());
	}
	
	@AfterEach
	void clear() {service = null; account = null;}
	
}
