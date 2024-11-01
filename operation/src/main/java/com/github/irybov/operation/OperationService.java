package com.github.irybov.operation;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQueryFactory;

@Service
@Transactional(readOnly = true, noRollbackFor = Exception.class)
public class OperationService {

	@Autowired
	private OperationJDBC operationJDBC;
	@Autowired
	private JdbcTemplate jdbcTemplate;
//	@Autowired
//	private SQLQueryFactory queryFactory;
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public void save(Operation operation) {operationJDBC.save(operation);}
	
	Operation transfer(double amount, String action, String currency, int sender, 
			int recipient, String bank) {
		return Operation.builder()
				.amount(amount)
				.action(action)
				.currency(currency)
				.sender(sender)
				.recipient(recipient)
				.createdAt(Timestamp.valueOf(OffsetDateTime.now()
						.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
				.bank(bank)
				.build();
	};
	Operation deposit(double amount, String action, String currency, int recipient, 
			String bank) {
		return Operation.builder()
				.amount(amount)
				.action(action)
				.currency(currency)
				.recipient(recipient)
				.createdAt(Timestamp.valueOf(OffsetDateTime.now()
						.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
				.bank(bank)
				.build();
	};
	Operation withdraw(double amount, String action, String currency, int sender, 
			String bank) {
		return Operation.builder()
				.amount(amount)
				.action(action)
				.currency(currency)
				.sender(sender)
				.createdAt(Timestamp.valueOf(OffsetDateTime.now()
						.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
				.bank(bank)
				.build();
	};
	
	public Operation getOne(long id) {
		return operationJDBC.findById(id).get();
	}
	
	public List<Operation> getList(int id) {
		return operationJDBC.findBySenderOrRecipientOrderByIdDesc(id, id);
	}
	
	public Page<Operation> getPage(int id, String action, Double minval, Double maxval,
			OffsetDateTime mindate, OffsetDateTime maxdate, Pageable pageable){
		
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
//		return operationJDBC.findAll(where, pageable);
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
		return new PageImpl<>(operations, pageable, count);
	}
}
