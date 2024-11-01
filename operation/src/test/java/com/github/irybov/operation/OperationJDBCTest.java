package com.github.irybov.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/test-operations-h2.sql")
@DataJdbcTest
class OperationJDBCTest {
	
	@Autowired
	private OperationJDBC operationJDBC;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@ParameterizedTest
	@CsvSource({"1, 1, 3", "2, 2, 2", "3, 3, 3"})
	void test_findBySenderOrRecipientOrderByIdDesc(int sender, int recipient, int quantity) {
		
	    Comparator<Operation> compareById = Comparator.comparing(Operation::getId).reversed();	
		List<Operation> operations =
				operationJDBC.findBySenderOrRecipientOrderByIdDesc(sender, recipient);
		assertThat(operations.size()).isEqualTo(quantity);
		assertThat(operations).isSortedAccordingTo((compareById));
	}
	
	//@Execution(ExecutionMode.CONCURRENT)
	@ParameterizedTest
	@MethodSource("params")
	void test_findAllBySpecs(int id, String action, Double minval, Double maxval,
			OffsetDateTime mindate, OffsetDateTime maxdate, int quantity) {
		
		OperationPage page = new OperationPage();
		Pageable pageable = PageRequest.of(page.getPageNumber(), page.getPageSize(),
				page.getSortDirection(), page.getSortBy());
		
		Timestamp dawn = Timestamp.valueOf(mindate.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
		Timestamp dusk = Timestamp.valueOf(maxdate.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
/*		
		Predicate or = QPredicate.builder()
				.add(id, QOperations.operations.sender::eq)
				.add(id, QOperations.operations.recipient::eq)
				.buildOr();
		Predicate and = QPredicate.builder()
				.add(action, QOperations.operations.action::like)
				.add(minval, maxval, QOperations.operations.amount::between)
				.add(dawn, dusk, QOperations.operations.createdAt::between)
				.buildAnd();
		Predicate where = ExpressionUtils.allOf(or, and);		
//		Page<Operation> resultPage = operationJDBC.findAll(where, pageable);
*/		
		List<String> parts = new ArrayList<>();
		String select = 
				String.format("SELECT * FROM bankdemo.operations WHERE (sender = %d OR recipient = %d)", id, id);
		parts.add(select);
		if(action != null) parts.add(String.format("AND action LIKE %s", "'"+action+"'"));
		if(minval != null && maxval != null) parts.add(String.format("AND amount BETWEEN %.2f AND %.2f", minval, maxval).replace(',', '.'));
		if(dawn != null && dusk != null)  parts.add(String.format("AND created_at BETWEEN %s AND %s", "'"+dawn+"'", "'"+dusk+"'"));
		String paging = 
				String.format("ORDER BY %s %s LIMIT %d OFFSET %d", "id", "DESC", pageable.getPageSize(), pageable.getOffset());
		parts.add(paging);
		
		String query = String.join(" ", parts);
		List<Operation> operations = jdbcTemplate.query(query, BeanPropertyRowMapper.newInstance(Operation.class));
		long count = operationJDBC.count();
		Page<Operation> resultPage = new PageImpl<>(operations, pageable, count);
		
		assertThat(operations.size()).isEqualTo(quantity);		
		assertThat(resultPage.getContent().size()).isEqualTo(quantity);
		assertThat(count).isEqualTo(6);		
	}
	private static Stream<Arguments> params() {
		return Stream.of(Arguments.of(1, "deposit",  100.00, 700.00,
				OffsetDateTime.now().minusDays(1L), OffsetDateTime.now().plusDays(1L), 1),
				Arguments.of(2, "transfer",  200.00, 900.00,
						OffsetDateTime.now().minusDays(1L), OffsetDateTime.now().plusDays(1L), 2));
	}

	@Test
	void test_save() {
		
		Operation.OperationBuilder builder = Operation.builder();
		Operation operation = builder
			.amount(0.00)
			.action("external")
			.currency("SEA")
			.createdAt(Timestamp.valueOf(OffsetDateTime.now()
					.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
			.bank("Demo")
			.build();
		
		Operation entity = operationJDBC.save(operation);
		assertThat(entity.getId() == 1);
	}

}
