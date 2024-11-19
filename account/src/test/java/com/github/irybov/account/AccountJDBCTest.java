package com.github.irybov.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@Sql("/test-accounts-h2.sql")
@DataJdbcTest
@TestInstance(Lifecycle.PER_CLASS)
public class AccountJDBCTest {

	@Autowired
	private AccountJDBC jdbc;
	
	@Autowired
	private DataSource dataSource;
	private ResourceDatabasePopulator populator;
	
	@BeforeAll
	void prepare() {		
		populator = new ResourceDatabasePopulator();
		populator.addScripts(new ClassPathResource("test-accounts-h2.sql"));
		populator.execute(dataSource);
	}
	
	@Test
	void can_get_one() {
		Account account = jdbc.findById(2).get();
		assertThat(account.getBills().size() == 2);
		
		account = jdbc.findByPhone("3333333333");
		assertThat(account.getBills().size() == 1);
	}
	
	@Test
	void can_get_all() {
		List<Account> accounts = (List<Account>) jdbc.findAll();
		assertThat(accounts.size() == 4);
	}
	
	@Test
	void can_update() {
		
		Account account = jdbc.findByPhone("2222222222");
		assertThat(account.getBills() == null);		
		Set<Integer> ids = IntStream.rangeClosed(4, 6).boxed().collect(Collectors.toSet());
		account.setBills(ids);
		account = jdbc.save(account);
		assertThat(account.getBills().size() == 3);
		
		account = jdbc.findById(4).get();
		assertThat(account.getBills().size() == 1);		
		account.getBills().add(7);
		account = jdbc.save(account);
		assertThat(account.getBills().size() == 2);
	}
	
	@AfterAll void clear() {
		populator.setScripts(new ClassPathResource("test-reset-table-h2.sql"));
		populator.execute(dataSource);
		populator = null;
	}
	
}
