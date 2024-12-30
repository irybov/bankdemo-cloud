package com.github.irybov.account;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

import com.github.irybov.shared.AccountDTO;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AccountMapperImpl.class)
public class AccountMapperTest {
	
	@Autowired
	private AccountMapper mapStruct;
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
	}
	
	@Test
	void to_DTO_List() {
		
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
	
	@Test
	void to_DataBase() {
		
		Registration registration = new Registration();
		registration.setName("Admin");
		registration.setSurname("Adminov");
		registration.setPhone("0000000000");
		registration.setEmail("adminov@greenmail.io");
		registration.setBirthday(LocalDate.of(2001, 01, 01));
		registration.setPassword("superadmin");
		
		Account account = mapStruct.toDB(registration);
		assertAll(
			() -> assertEquals(true, account.isActive()), 
			() -> assertNull(account.getBills()), 
			() -> assertEquals(registration.getBirthday(), account.getBirthday()), 
			() -> assertEquals(registration.getEmail(), account.getEmail()), 
			() -> assertEquals(registration.getName(), account.getName()), 
			() -> assertEquals(registration.getSurname(), account.getSurname()), 
			() -> assertEquals(registration.getPhone(), account.getPhone()), 
			() -> assertEquals(registration.getPassword(), account.getPassword()), 
			() -> assertNull(account.getUpdatedAt()), 
			() -> assertNotNull(account.getCreatedAt())
		);
	}
	
	@AfterAll
	static void clear() {account = null;}

}
