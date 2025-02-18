package com.github.irybov.operation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQueryFactory;

class OperationServiceTest {

	@Mock
	private BillClient billClient;
//	private RestTemplate restTemplate;
	@Mock
	private OperationJDBC operationJDBC;
//	@Mock
//	private JdbcTemplate jdbcTemplate;
	@Mock
	private SQLQueryFactory queryFactory;
	@InjectMocks
	private OperationService operationService;

	private AutoCloseable autoClosable;
	
	private static Operation operation;	
	private static Operation.OperationBuilder builder;
	
	@BeforeAll
	static void prepare() {
		operation = new Operation();
		builder = mock(Operation.OperationBuilder.class, Mockito.CALLS_REAL_METHODS);
	}
	
	@BeforeEach
	void set_up() {
		autoClosable = MockitoAnnotations.openMocks(this);
		operationService = new OperationService(billClient, operationJDBC, queryFactory);
	}
	
	@Test
	void can_build() {
		
		String currency = "SEA";
//		when(builder.build()).thenReturn(operation);
		
		assertThat(operationService.construct(
				new OperationDTO(new Random().nextDouble(), Action.DEPOSIT, currency,
				new Random().nextInt(), new Random().nextInt(), "Demo"))).hasSameClassAs(operation);
		assertThat(operationService.construct(
				new OperationDTO(new Random().nextDouble(), Action.WITHDRAW, currency,
				new Random().nextInt(), new Random().nextInt(), "Demo"))).hasSameClassAs(operation);
		assertThat(operationService.construct(
				new OperationDTO(new Random().nextDouble(), Action.TRANSFER, currency,
				new Random().nextInt(), new Random().nextInt(), "Demo"))).hasSameClassAs(operation);
		assertThat(operationService.construct(
				new OperationDTO(new Random().nextDouble(), Action.EXTERNAL, currency,
				new Random().nextInt(), new Random().nextInt(), "Demo"))).hasSameClassAs(operation);
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
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
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
	
	@Disabled
	@Test
	void can_get_page() {
		
		final long size = new Random().nextInt(Byte.MAX_VALUE + 1);
//		List<Operation> operations = Stream.generate(Operation::new)
//				.limit(size)
//				.collect(Collectors.toList());			
//		Page<Operation> result = new PageImpl<Operation>(operations);
		
//		when(jdbcTemplate.query(anyString(), any(BeanPropertyRowMapper.class))).thenReturn(operations);
//		when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(size);
/*		
		when(queryFactory
				.select(any(Expression.class))
				.from(any(QOperations.class))
				.where(any(Predicate.class))
				.orderBy(any(OrderSpecifier[].class))
				.limit(anyInt())
				.offset(anyLong())
				.fetch())
		.thenReturn(operations);
		when(queryFactory
				.select(any(Expression.class))
				.from(any(QOperations.class))
				.where(any(Predicate.class))
				.fetchCount())
		.thenReturn(size);
*/
		final int id = new Random().nextInt();
		final double value = new Random().nextDouble();
		OperationPage page = new OperationPage();
		Pageable pageable = PageRequest.of(page.getPageNumber(), page.getPageSize(),
				   page.getSortDirection(), page.getSortBy());
		
		Page<Operation> dtos = operationService.getPage(id, "unknown", value, value,
				OffsetDateTime.of(LocalDate.parse("1900-01-01"), LocalTime.MIN, ZoneOffset.UTC), 
				OffsetDateTime.now(), pageable);
		
		assertThat(dtos)
			.hasSameClassAs(new PageImpl<Operation>(new ArrayList<Operation>()));
		assertThat(dtos.getContent().size()).isEqualTo(size);
//		verify(jdbcTemplate).query(anyString(), any(BeanPropertyRowMapper.class));
//		verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class));
	}
	
	@Test
	void can_save() {
		
//		doNothing().when(restTemplate).patchForObject(anyString(), any(Map.class), eq(Void.class));
    	doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) {return null;}})
    	.when(billClient).updateBalance(any(Map.class));
//    	.when(restTemplate).patchForObject(anyString(), any(Map.class), eq(Void.class));		
		when(operationJDBC.save(any(Operation.class))).thenReturn(new Operation());
		
		operationService.save(new OperationDTO(new Random().nextDouble(), Action.EXTERNAL, "SEA",
				new Random().nextInt(), new Random().nextInt(), "Demo"));
		
		verify(operationJDBC).save(any(Operation.class));
		verify(billClient).updateBalance(any(Map.class));
//		verify(restTemplate).patchForObject(anyString(), any(Map.class), eq(Void.class));
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
