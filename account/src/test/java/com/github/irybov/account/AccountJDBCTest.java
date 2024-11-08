package com.github.irybov.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
	}
	
	@Test
	void can_get_all() {
		List<Account> accounts = (List<Account>) jdbc.findAll();
		assertThat(accounts.size() == 4);
	}
	
	@AfterAll void clear() {
		populator.setScripts(new ClassPathResource("test-reset-table-h2.sql"));
		populator.execute(dataSource);
		populator = null;
	}
	
}
