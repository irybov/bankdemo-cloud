package com.github.irybov.account;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AccountMapperImpl.class)
public class AccountMapperTest {
	
	@Autowired
	private AccountMapper mapStruct;
	private static Account account;
	
	@BeforeAll
	static void prepare() {account = new Account(Timestamp.valueOf(OffsetDateTime.now()
			.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()), 
			"Admin", "Adminov", "0000000000", "adminov@greenmail.io", 
			LocalDate.of(2001, 01, 01), "superadmin"); account.setId(0);}
	
	@Test
	void test() {
		
		AccountDTO dto = mapStruct.toDTO(account);
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Account> accounts = Collections.nCopies(size, account).stream()
				.collect(Collectors.toList());
		List<AccountDTO> dtos = mapStruct.toList(accounts);
		
		assertAll(
				() -> assertEquals(dto.isActive(), account.isActive()), 
				() -> assertEquals(dto.getBills(), null), 
				() -> assertEquals(dto.getBirthday(), account.getBirthday()), 
				() -> assertEquals(dto.getEmail(), account.getEmail()), 
				() -> assertEquals(dto.getName(), account.getName()), 
				() -> assertEquals(dto.getSurname(), account.getSurname()), 
				() -> assertEquals(dto.getPhone(), account.getPhone()), 
				() -> assertEquals(dto.getCreatedAt(), account.getCreatedAt())
			);
			
			assertAll(
				() -> assertEquals(dtos.size(), accounts.size()), 
				() -> assertThat(dtos).hasSameClassAs(new ArrayList<AccountDTO>())
			);
	}
	
	@AfterAll
	static void clear() {account = null;}

}
