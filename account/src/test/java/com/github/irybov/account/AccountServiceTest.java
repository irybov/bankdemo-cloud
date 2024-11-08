package com.github.irybov.account;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyInt;
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AccountServiceTest {
	
	@Mock
	private AccountJDBC jdbc;
	@InjectMocks
	private AccountService service;
	
	private Account account;
	
	@BeforeAll
	void prepare() {
		MockitoAnnotations.openMocks(this);
		service = new AccountService(jdbc);
		account = new Account(Timestamp.valueOf(OffsetDateTime.now()
				.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()), 
				"Admin", "Adminov", "0000000000", "adminov@greenmail.io", 
				LocalDate.of(2001, 01, 01), "superadmin");
	}

	@Test
	void can_get_one() {
		Optional<Account> optional = Optional.of(account);
		when(jdbc.findById(anyInt())).thenReturn(optional);
		assertThat(service.getOne(anyInt())).isExactlyInstanceOf(Account.class);
		verify(jdbc).findById(anyInt());
	}
	
	@Test
	void can_get_all() {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Account> accounts = Collections.nCopies(size, account).stream()
				.collect(Collectors.toList());
		when(jdbc.findAll()).thenReturn(accounts);
		List<Account> results = service.getAll();
		assertAll(
				() -> assertThat(results).hasSameClassAs(new ArrayList<Account>()),
				() -> assertThat(results.size()).isEqualTo(accounts.size()));
		verify(jdbc).findAll();
	}
	
	@AfterAll
	void clear() {service = null; account = null;}
	
}
