package com.github.irybov.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.sql.DataSource;

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
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.spring.SpringConnectionProvider;
import com.querydsl.sql.spring.SpringExceptionTranslator;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/test-operations-h2.sql")
@DataJdbcTest
class OperationJDBCTest {
	
	@Autowired
	private OperationJDBC operationJDBC;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DataSource dataSource;
	
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
	void test_findAllByFilters(int id, String action, Double minval, Double maxval,
			OffsetDateTime mindate, OffsetDateTime maxdate, int quantity) {
		
		OperationPage page = new OperationPage();
		Pageable pageable = PageRequest.of(page.getPageNumber(), page.getPageSize(),
				page.getSortDirection(), page.getSortBy());
		
		Timestamp dawn = null;
		if(mindate != null) dawn = Timestamp.valueOf(mindate.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
		Timestamp dusk = null;
		if(maxdate != null) dusk = Timestamp.valueOf(maxdate.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());

		SQLTemplates templates = new PostgreSQLTemplates();
		Configuration configuration = new Configuration(templates);
		configuration.setExceptionTranslator(new SpringExceptionTranslator());
		SpringConnectionProvider provider = new SpringConnectionProvider(dataSource);
		SQLQueryFactory queryFactory = new SQLQueryFactory(configuration, provider);
/*		
		Predicate or = QPredicate.builder()
				.add(id, QOperations.operations.sender::eq)
				.add(id, QOperations.operations.recipient::eq)
				.buildOr();
		Predicate and = QPredicate.builder()
				.add(action, QOperations.operations.action::like)
//				.add(minval, maxval, QOperation.operation.amount::between)
				.add(minval, QOperations.operations.amount::goe)
				.add(maxval, QOperations.operations.amount::loe)
//				.add(mindate, maxdate, QOperation.operation.createdAt::between)
				.add(dawn, QOperations.operations.createdAt::goe)
				.add(dusk, QOperations.operations.createdAt::loe)
				.buildAnd();
		Predicate where = ExpressionUtils.allOf(or, and);
*/	
//		Page<Operation> resultPage = operationJDBC.findAll(where, pageable);

		List<String> orders = new ArrayList<>();
		Sort sort = pageable.getSort();
		sort.iterator().forEachRemaining(order -> orders.add(order.getProperty() + " " + order.getDirection().name()));
		String orderBy = String.join(", ", orders);
		
		List<String> parts = new ArrayList<>();
		String select = 
				String.format("SELECT * FROM bankdemo.operations WHERE (sender = %d OR recipient = %d)", id, id);
		parts.add(select);
		filtering(parts, action, minval, maxval, dawn, dusk);
		String paging = 
				String.format("ORDER BY %s LIMIT %d OFFSET %d", orderBy, pageable.getPageSize(), pageable.getOffset());
		parts.add(paging);		
		String query = String.join(" ", parts);
		List<Operation> operations = jdbcTemplate.query(query, BeanPropertyRowMapper.newInstance(Operation.class));
		
		parts.clear();
		String count = 
				String.format("SELECT COUNT(*) FROM bankdemo.operations WHERE (sender = %d OR recipient = %d)", id, id);
		parts.add(count);
		filtering(parts, action, minval, maxval, dawn, dusk);
		query = new String(String.join(" ", parts));		
//		long total = operationJDBC.count();
		Long total = jdbcTemplate.queryForObject(query, Long.class);
		Page<Operation> resultPage = new PageImpl<>(operations, pageable, total);
		
		assertThat(operations.size()).isEqualTo(quantity);		
		assertThat(resultPage.getContent().size()).isEqualTo(quantity);
		assertThat(total).isEqualTo(operations.size());		
	}
	private void filtering(List<String> parts, String action, Double minval, Double maxval,
			Timestamp dawn, Timestamp dusk) {
		
		if(action != null) parts.add(String.format("action LIKE %s", "'"+action+"'"));
		if(minval != null && maxval != null) parts.add(String.format("amount BETWEEN %.2f AND %.2f", minval, maxval).replace(',', '.'));
		else if(minval != null) parts.add(String.format("amount >= %.2f", minval).replace(',', '.'));
		else if(maxval != null) parts.add(String.format("amount <= %.2f", maxval).replace(',', '.'));
		if(dawn != null && dusk != null)  parts.add(String.format("created_at BETWEEN %s AND %s", "'"+dawn+"'", "'"+dusk+"'"));
		else if(dawn != null) parts.add(String.format("created_at >= %s", "'"+dawn+"'"));
		else if(dusk != null) parts.add(String.format("created_at <= %s", "'"+dusk+"'"));
		
		for(int i = 1; i < parts.size(); i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("AND ");
			sb.append(parts.get(i));
			parts.set(i, sb.toString());
		}
	}
	private static Stream<Arguments> params() {
		return Stream.of(
				Arguments.of(1, "deposit", 100.00, 700.00,
						OffsetDateTime.now().minusDays(1L), OffsetDateTime.now().plusDays(1L), 1),
				Arguments.of(2, "transfer", 200.00, 900.00,
						OffsetDateTime.now().minusDays(1L), OffsetDateTime.now().plusDays(1L), 2),
				Arguments.of(3, null, null, null, 
						OffsetDateTime.now().minusDays(1L), OffsetDateTime.now().plusDays(1L), 3),
				Arguments.of(3, null, 500.00, null, null, null, 2),
				Arguments.of(3, null, null, 700.00, null, null, 2),
				Arguments.of(0, null, null, null, OffsetDateTime.now().plusDays(1L), null, 0),
				Arguments.of(0, null, null, null, null, OffsetDateTime.now().minusDays(1L), 0),
				Arguments.of(0, null, null, null, null, null, 4)
			);
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
