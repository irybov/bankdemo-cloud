package com.github.irybov.bill;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@Sql("/test-bill-h2.sql")
@DataJdbcTest
@TestInstance(Lifecycle.PER_CLASS)
public class BillJDBCTest {

	@Autowired
	private BillJDBC jdbc;
	@Autowired
	private JdbcTemplate template;
	
	@Autowired
	private DataSource dataSource;
	private ResourceDatabasePopulator populator;
	
	@BeforeAll
	void prepare() {		
		populator = new ResourceDatabasePopulator();
		populator.addScripts(new ClassPathResource("test-bill-h2.sql"));
		populator.execute(dataSource);
	}
	
	@Test
	void can_create() {
		
		Bill bill = new Bill("SEA", 0);
		bill.create();
		bill = jdbc.save(bill);
		assertThat(bill.getId() == 2);
		assertThat(bill.getCurrency().equals("SEA"));
		assertThat(bill.getBalance() == BigDecimal.valueOf(0.00));
	}
	
	@Test
	void can_get_one() {
		Bill bill = jdbc.findById(1).get();
		assertThat(bill.getCurrency().equals("USD"));
		assertThat(bill.getBalance() == BigDecimal.valueOf(10.00));
	}
	
	@Test
	void can_get_list() {
		List<Bill> bills = jdbc.findByOwner(1);
		assertThat(bills.size() == 2);
	}
	
	@Test
	void can_change_status() {
		
		String select = String.format("SELECT is_active FROM bankdemo.bills WHERE id = %d", 1);
		boolean isActive = template.queryForObject(select, Boolean.class);
		assertThat(isActive == true);
		
		isActive = false;
		String update = String.format("UPDATE bankdemo.bills SET is_active = %b WHERE id = %d", isActive, 1);
		template.update(update);
		
		select = String.format("SELECT is_active FROM bankdemo.bills WHERE id = %d", 1);
		isActive = template.queryForObject(select, Boolean.class);
		assertThat(isActive == false);
	}
	
	@Test
	void can_update_balance() {
		
		Bill bill = jdbc.findById(1).get();
		bill.update(5.00);
		bill = jdbc.save(bill);
		assertThat(bill.getBalance().doubleValue() == 15.00);
		
		bill.update(-20.00);
		bill = jdbc.save(bill);
		assertThat(bill.getBalance().doubleValue() == -5.00);
	}
	
	@Test
	void can_delete() {
		jdbc.deleteById(1);
		jdbc.deleteById(2);
		List<Bill>bills = jdbc.findByOwner(1);
		assertThat(bills.isEmpty());
	}
	
	@AfterAll void clear() {populator = null;}
	
}
