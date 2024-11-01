package com.github.irybov.operation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.querydsl.sql.SQLQueryFactory;

class OperationServiceTest {
	
	@Mock
	private OperationJDBC operationJDBC;
	@Mock
	private JdbcTemplate jdbcTemplate;
//	@Mock
//	private SQLQueryFactory queryFactory;
	@InjectMocks
	private OperationService operationService;

	private AutoCloseable autoClosable;
	
	private static Operation operation;	
	private static Operation.OperationBuilder builder;
	
	@BeforeAll
	static void prepare() {
		operation = new Operation();
		builder = mock(Operation.OperationBuilder.class, Mockito.RETURNS_SELF);
	}
	
	@BeforeEach
	void set_up() {
		autoClosable = MockitoAnnotations.openMocks(this);
		operationService = new OperationService();
		ReflectionTestUtils.setField(operationService, "operationJDBC", operationJDBC);
		ReflectionTestUtils.setField(operationService, "jdbcTemplate", jdbcTemplate);
//		ReflectionTestUtils.setField(operationService, "queryFactory", queryFactory);
	}
	
	@Test
	void can_build() {
		
		String currency = "SEA";
		when(builder.build()).thenReturn(operation);
		
		assertThat(operationService.deposit(new Random().nextDouble(), currency, "^[A-Z]{3}",
				new Random().nextInt(), "Demo")).hasSameClassAs(operation);
		assertThat(operationService.withdraw(new Random().nextDouble(), currency, "^[A-Z]{3}",
				new Random().nextInt(), "Demo")).hasSameClassAs(operation);
		assertThat(operationService.transfer(new Random().nextDouble(), currency, "^[A-Z]{3}",
				new Random().nextInt(), new Random().nextInt(), "Demo")).hasSameClassAs(operation);
	}
	
	@Test
	void can_get_one() {
		Optional<Operation> result = Optional.of(operation);
		when(operationJDBC.findById(anyLong())).thenReturn(result);
		assertThat(operationService.getOne(anyLong())).isExactlyInstanceOf(Operation.class);
		verify(operationJDBC).findById(anyLong());
	}
	
	@Test
	void can_get_list() {
		
		final byte size = (byte) new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Operation> operations = Stream.generate(Operation::new)
				.limit(size)
				.collect(Collectors.toList());
		final int id = new Random().nextInt();
		
		when(operationJDBC.findBySenderOrRecipientOrderByIdDesc(id, id))
			.thenReturn(operations);
		
		List<Operation> dtos = operationService.getList(id);
		assertAll(
				() -> assertThat(dtos).hasSameClassAs(new ArrayList<Operation>()),
				() -> assertThat(dtos.size()).isEqualTo(operations.size()));
		verify(operationJDBC).findBySenderOrRecipientOrderByIdDesc(id, id);
	}
	
	@Test
	void can_get_page() {
		
		final byte size = (byte) new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Operation> operations = Stream.generate(Operation::new)
				.limit(size)
				.collect(Collectors.toList());			
		Page<Operation> result = new PageImpl<Operation>(operations);
		when(jdbcTemplate.query(anyString(), any(BeanPropertyRowMapper.class))).thenReturn(operations);
		when(operationJDBC.count()).thenReturn((long)size);

		final int id = new Random().nextInt();
		final double value = new Random().nextDouble();
		OperationPage page = new OperationPage();
		Pageable pageable = PageRequest.of(page.getPageNumber(), page.getPageSize(),
				   page.getSortDirection(), page.getSortBy());
		
		Page<Operation> dtos = operationService.getPage(id, "^[a-z]{7,8}", value, value,
				OffsetDateTime.of(LocalDate.parse("1900-01-01"), LocalTime.MIN, ZoneOffset.UTC), 
				OffsetDateTime.now(), pageable);
		assertThat(dtos)
			.hasSameClassAs(new PageImpl<Operation>(new ArrayList<Operation>()));
		assertThat(dtos.getContent().size()).isEqualTo(size);
		verify(jdbcTemplate).query(anyString(), any(BeanPropertyRowMapper.class));
		verify(operationJDBC).count();
	}
	
	@Test
	void can_save() {		
		when(operationJDBC.save(any(Operation.class))).thenReturn(new Operation());
		operationService.save(new Operation());
		verify(operationJDBC).save(any(Operation.class));
	}
	
    @AfterEach
    void tear_down() throws Exception {
    	autoClosable.close();
    	operationService = null;
    }

    @AfterAll
    static void clear() {
    	operation = null;
    	builder = null;
    }

}
