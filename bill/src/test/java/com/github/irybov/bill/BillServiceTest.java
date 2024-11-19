package com.github.irybov.bill;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

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
import org.springframework.jdbc.core.JdbcTemplate;

import com.github.irybov.shared.BillDTO;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BillServiceTest {

	@Spy
	private BillMapperImpl mapStruct;
	@Mock
	private BillJDBC jdbc;
	@Mock
	private JdbcTemplate template;
	@InjectMocks
	private BillService service;
	
	private AutoCloseable autoClosable;
	private Bill bill;
	
	@BeforeAll
	void prepare() {
		bill = new Bill("SEA", 0);
		bill.create();
	}	
	@BeforeEach
	void set_up() {
		autoClosable = MockitoAnnotations.openMocks(this);
		service = new BillService(mapStruct, jdbc, template);
	}
	
	@Test
	void can_create() {
		when(jdbc.save(any(Bill.class))).thenReturn(bill);
		assertThat(service.create(bill.getCurrency(), bill.getOwner()))
		.isExactlyInstanceOf(BillDTO.class);
		verify(jdbc).save(any(Bill.class));
	}
	
	@Test
	void can_get_bill() {
		Optional<Bill> optional = Optional.of(bill);
		when(jdbc.findById(anyInt())).thenReturn(optional);
		assertThat(service.getBill(anyInt())).isExactlyInstanceOf(Bill.class);
		verify(jdbc).findById(anyInt());
	}
	
	@Test
	void can_get_dto() {
		Optional<Bill> optional = Optional.of(bill);
		when(jdbc.findById(anyInt())).thenReturn(optional);
		assertThat(service.getOne(anyInt())).isExactlyInstanceOf(BillDTO.class);
		verify(jdbc).findById(anyInt());
	}
	
	@Test
	void can_get_list() {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Bill> bills = Collections.nCopies(size, bill).stream()
				.collect(Collectors.toList());
		final int owner = new Random().nextInt();
		
		when(jdbc.findByOwner(owner)).thenReturn(bills);
		List<BillDTO> results = service.getList(owner);
		assertAll(
				() -> assertThat(results).hasSameClassAs(new ArrayList<BillDTO>()),
				() -> assertThat(results.size()).isEqualTo(bills.size()));
		verify(jdbc).findByOwner(owner);
	}
	
	@Test
	void can_change_status() {
		
		boolean isActive = true;
		when(template.queryForObject(anyString(), eq(Boolean.class))).thenReturn(isActive);
		when(template.update(anyString())).thenReturn(anyInt());
//		doNothing().when(template.update(anyString()));
		assertThat(service.changeStatus(anyInt())).isFalse();
		verify(template).queryForObject(anyString(), eq(Boolean.class));
		verify(template).update(anyString());
	}
	
	@Test
	void can_update_balance() {
		
		double amount = 5.00;
		when(jdbc.findById(anyInt())).thenReturn(Optional.of(bill));
		when(jdbc.save(any(Bill.class))).thenReturn(bill);
//		doNothing().when(jdbc.save(any(Bill.class)));
		assertThat(service.updateBalance(anyInt(), amount)).isEqualTo(5.00);
		assertEquals(amount, bill.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
		
		amount = -15.00;
		assertThat(service.updateBalance(anyInt(), amount)).isEqualTo(-10.00);
		assertEquals(-10.00, bill.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
		verify(jdbc, times(2)).findById(anyInt());
		verify(jdbc, times(2)).save(any(Bill.class));
	}
	
    @AfterEach
    void tear_down() throws Exception {
    	autoClosable.close();
    	service = null;
    	bill.setBalance(new BigDecimal(0.00));
    }
    @AfterAll
    void clear() {bill = null;}

}
