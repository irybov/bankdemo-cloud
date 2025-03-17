package com.github.irybov.bill;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.irybov.shared.BillDTO;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@ExtendWith(MockitoExtension.class)
public class BillServiceTest {

	@Spy
	private BillMapperImpl mapStruct;
	@Mock
	private BillJDBC jdbc;
	@Mock
	private JdbcTemplate template;
	@Mock
	private Source source;
	@InjectMocks
	private BillService service;
	@Mock
	private BillService self;
	
	private AutoCloseable autoClosable;
	private Bill bill;
	
	private BillDTO dto;
//	private static MessageBuilder<?> builder;
	
	@BeforeAll
	void prepare() {
		bill = new Bill("SEA", 0);
		bill.create();
//		builder = mock(MessageBuilder.class, Mockito.CALLS_REAL_METHODS);
	}	
	@BeforeEach
	void set_up() {
		autoClosable = MockitoAnnotations.openMocks(this);
		service = new BillService(mapStruct, jdbc, template, source);
		ReflectionTestUtils.setField(service, "self", self);
		dto = mapStruct.toDTO(bill);
	}
	
	@Test
	void can_create() {
//		Optional<Bill> optional = Optional.of(bill);
//		when(jdbc.findById(anyInt())).thenReturn(optional);
//		when(self.getBill(anyInt())).thenReturn(bill);
//		when(self.getOne(anyInt())).thenReturn(dto);
		when(self.getList(anyInt())).thenReturn(new LinkedHashSet<>());
		bill.setId(0);
		when(jdbc.save(any(Bill.class))).thenReturn(bill);
		assertThat(service.create(bill.getCurrency(), bill.getOwner()))
		.isExactlyInstanceOf(BillDTO.class);
		verify(jdbc).save(any(Bill.class));
//		verify(self).getOne(anyInt());
//		verify(self).getBill(anyInt());
//		verify(jdbc).findById(anyInt());
		verify(self).getList(anyInt());
		bill.setId(null);
	}
/*	
	@Test
	void can_get_one() {
		Optional<Bill> optional = Optional.of(bill);
		when(jdbc.findById(anyInt())).thenReturn(optional);
		assertThat(service.getBill(anyInt())).isExactlyInstanceOf(Bill.class);
		verify(jdbc).findById(anyInt());
	}
*/	
	@Test
	void can_get_dto() {
		when(self.getBill(anyInt())).thenReturn(bill);
//		Optional<Bill> optional = Optional.of(bill);
//		when(jdbc.findById(anyInt())).thenReturn(optional);
		assertThat(service.getOne(anyInt())).isExactlyInstanceOf(BillDTO.class);
//		verify(jdbc).findById(anyInt());
		verify(self).getBill(anyInt());
	}
	
	@Test
	void try_get_absent_one() {
		when(self.getBill(anyInt())).thenThrow(new NoSuchElementException());
//		Optional<Bill> optional = Optional.empty();
//		when(jdbc.findById(anyInt())).thenReturn(optional);
		assertThrows(NoSuchElementException.class, () -> service.getOne(anyInt()));
		assertThatThrownBy(() -> service.getOne(anyInt())).isInstanceOf(NoSuchElementException.class);
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> service.getOne(anyInt()));
//		verify(jdbc, times(3)).findById(anyInt());
		verify(self, times(3)).getBill(anyInt());
	}
	
	@Test
	void can_get_list() {
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Bill> list = 
//				Collections.nCopies(size, bill).stream()
				Stream.generate(() -> new Bill("SEA", 0)).limit(size)
				.collect(Collectors.toList());
		int i = 0;
		for(Bill bill : list) bill.setId(new Integer(++i));
		Set<Bill> bills = new HashSet<>(list);
		
		when(jdbc.findByOwner(anyInt())).thenReturn(bills);
//		List<BillDTO> results = service.getList(anyInt());
		Set<BillDTO> results = service.getList(anyInt());
		assertAll(
//				() -> assertThat(results).hasSameClassAs(new ArrayList<BillDTO>()), 
				() -> assertThat(results).hasSameClassAs(new HashSet<BillDTO>()), 
				() -> assertThat(results.size()).isEqualTo(size));
		verify(jdbc).findByOwner(anyInt());
	}
	
	@Test
	void try_get_empty_list() {
//		final int owner = new Random().nextInt();
//		when(jdbc.findByOwner(anyInt())).thenReturn(new LinkedList<Bill>());
		when(jdbc.findByOwner(anyInt())).thenReturn(new HashSet<Bill>());
		assertTrue(service.getList(anyInt()).isEmpty());
		verify(jdbc).findByOwner(anyInt());
	}
	
	@Test
	void can_change_status() {
		
		when(self.getBill(anyInt())).thenReturn(bill);
		when(self.getOne(anyInt())).thenReturn(dto);
//		Optional<Bill> optional = Optional.of(bill);
//		when(jdbc.findById(anyInt())).thenReturn(optional);
		
		boolean isActive = true;
		when(template.queryForObject(anyString(), eq(Boolean.class))).thenReturn(isActive);
		when(template.update(anyString())).thenReturn(anyInt());
//		doNothing().when(template.update(anyString()));
		assertThat(service.changeStatus(anyInt())).isFalse();
		verify(template).queryForObject(anyString(), eq(Boolean.class));
		verify(template).update(anyString());
		
//		verify(jdbc).findById(anyInt());
		verify(self).getBill(anyInt());
		verify(self).getOne(anyInt());
	}
	
	@Test
	void try_change_status() {
		final int id = new Random().nextInt();
		when(template.queryForObject(anyString(), eq(Boolean.class)))
			.thenThrow(new EmptyResultDataAccessException(1));
		assertThrows(EmptyResultDataAccessException.class, () -> service.changeStatus(id));
		assertThatThrownBy(() -> service.changeStatus(id)).isInstanceOf(EmptyResultDataAccessException.class);
		assertThatExceptionOfType(EmptyResultDataAccessException.class).isThrownBy(() -> service.changeStatus(id));
		verify(template, times(3)).queryForObject(anyString(), eq(Boolean.class));
	}
	
	@Test
	void can_update_balance() {
		
		MessageChannel channel = Mockito.mock(MessageChannel.class);
		
//		List<Bill> bills = new LinkedList<>();
		Set<Bill> bills = new HashSet<>();
		Bill stub = new Bill("SEA", 0);
		stub.create();
		
		when(self.getOne(anyInt())).thenReturn(dto);
		Set<BillDTO> dtos = new HashSet<>(mapStruct.toSet(bills));
		when(self.getList(anyInt())).thenReturn(dtos);
		
		int billID = 0;
		int stubID = 9;
		double positive = 5.00;
		double negative = -15.00;
		Map<Integer, Double> data = new LinkedHashMap<>();
		
//		Optional<Bill> optional = Optional.of(bill);
//		when(jdbc.findById(anyInt())).thenReturn(optional);
		
		when(jdbc.findByIdIn(any(Set.class))).thenReturn(bills);
//		when(jdbc.findById(0)).thenReturn(Optional.of(bill));
//		when(jdbc.findById(9)).thenReturn(Optional.of(stub));
		when(jdbc.saveAll(any(Set.class))).thenReturn(bills);
//		doNothing().when(jdbc.save(any(Bill.class)));
		when(source.output()).thenReturn(channel);
		when(channel.send(any(Message.class))).thenReturn(true);

		bill.setId(billID);
		bills.add(bill);
		data.put(billID, positive);
		service.updateBalance(data);
		assertEquals(5.00, bill.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
		
		stub.setId(stubID);
		bills.add(stub);
		data.put(billID, negative);
		data.put(stubID, negative);
		service.updateBalance(data);
		assertEquals(-10.00, bill.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
		assertEquals(-15.00, stub.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
		
		verify(jdbc, times(2)).findByIdIn(any(Set.class));
//		verify(jdbc, times(3)).findById(anyInt());
		verify(jdbc, times(2)).saveAll(any(Set.class));
		verify(source, times(2)).output();
		verify(channel, times(2)).send(any(Message.class));
		
//		verify(jdbc, times(3)).findById(anyInt());
		verify(self, times(3)).getList(anyInt());
		verify(self, times(3)).getOne(anyInt());
	}
	
	@Test
	void can_delete() {
		
		Set<Bill> bills = new HashSet<>();
		Bill stub = new Bill("SEA", 0);
		stub.create();
		
		Set<BillDTO> dtos = new HashSet<>(mapStruct.toSet(bills));
		when(self.getList(anyInt())).thenReturn(dtos);
		doNothing().when(self).deleteCachedList(anyInt());
		
		when(self.getBill(anyInt())).thenReturn(bill);
		when(self.getOne(anyInt())).thenReturn(dto);
		
		service.delete(anyInt());
		assertEquals(dtos.isEmpty(), true);
		
		verify(self).getBill(anyInt());
		verify(self).getOne(anyInt());
		verify(self).getList(anyInt());
		verify(self).deleteCachedList(anyInt());
//		verify(self).updateCachedList(anyInt(), any(Set.class));
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
