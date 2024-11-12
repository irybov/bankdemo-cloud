package com.github.irybov.operation;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.SQLQueryFactory;

@Service
@Transactional(readOnly = true, noRollbackFor = Exception.class)
public class OperationService {

	@Autowired
	private OperationJDBC operationJDBC;
//	@Autowired
//	private JdbcTemplate jdbcTemplate;
	@Autowired
	private SQLQueryFactory queryFactory;
	
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void save(Operation operation) {operationJDBC.save(operation);}
	
	Operation construct(double amount, Action action, String currency, int sender, 
			int recipient, String bank) {
		
		Operation operation = null;
		switch(action) {
			case DEPOSIT:
				operation = Operation.builder()
				.amount(amount)
				.action(action.name().toLowerCase())
				.currency(currency)
				.recipient(recipient)
				.createdAt(Timestamp.valueOf(OffsetDateTime.now()
						.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
				.bank(bank)
				.build();
				
			case WITHDRAW:
				operation = Operation.builder()
				.amount(amount)
				.action(action.name().toLowerCase())
				.currency(currency)
				.sender(sender)
				.createdAt(Timestamp.valueOf(OffsetDateTime.now()
						.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
				.bank(bank)
				.build();
			
			case TRANSFER:
				operation = Operation.builder()
				.amount(amount)
				.action(action.name().toLowerCase())
				.currency(currency)
				.sender(sender)
				.recipient(recipient)
				.createdAt(Timestamp.valueOf(OffsetDateTime.now()
						.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
				.bank(bank)
				.build();
				
			case EXTERNAL:
				operation = Operation.builder()
				.amount(amount)
				.action(action.name().toLowerCase())
				.currency(currency)
				.sender(sender)
				.recipient(recipient)
				.createdAt(Timestamp.valueOf(OffsetDateTime.now()
						.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
				.bank(bank)
				.build();
		}		
		return operation;
	}
	
	public Operation getOne(long id) {
		return operationJDBC.findById(id).get();
	}
	
	public List<Operation> getList(int id) {
		return operationJDBC.findBySenderOrRecipientOrderByIdDesc(id, id);
	}
	
	public Page<Operation> getPage(int id, String action, Double minval, Double maxval,
			OffsetDateTime mindate, OffsetDateTime maxdate, Pageable pageable){
		
		Timestamp dawn = null;
		if(mindate != null) dawn = Timestamp.valueOf(mindate.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
		Timestamp dusk = null;
		if(maxdate != null) dusk = Timestamp.valueOf(maxdate.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
		
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
			
		PathBuilder<Operation> entityPath = 
				new PathBuilder<>(Operation.class, QOperations.operations.getMetadata());
		List<OrderSpecifier<?>> list = new LinkedList<>();
        for(Sort.Order order : pageable.getSort()) {
            PathBuilder<Object> path = entityPath.get(order.getProperty());
            list.add(new OrderSpecifier(Order.valueOf(order.getDirection().name()), path));
        }
        OrderSpecifier<?>[] array = list.toArray(new OrderSpecifier[list.size()]);
		
		List<Operation> operations = queryFactory
				.select(Projections.constructor(Operation.class, QOperations.operations.all()))
				.from(QOperations.operations)
				.where(where)
				.orderBy(array)
				.limit(pageable.getPageSize())
				.offset(pageable.getOffset())
				.fetch();
		Long total = queryFactory
				.select(Projections.constructor(Operation.class, QOperations.operations.all()))
				.from(QOperations.operations)
				.where(where)
				.fetchCount();
/*		
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
//		List<Operation> operations = jdbcTemplate.query(query, BeanPropertyRowMapper.newInstance(Operation.class));
		
		parts.clear();
		String count = 
				String.format("SELECT COUNT(*) FROM bankdemo.operations WHERE (sender = %d OR recipient = %d)", id, id);
		parts.add(count);
		filtering(parts, action, minval, maxval, dawn, dusk);
		query = new String(String.join(" ", parts));
*/		
//		long total = operationJDBC.count();
//		Long total = jdbcTemplate.queryForObject(query, Long.class);
		return new PageImpl<>(operations, pageable, total);
	}	
//	private void filtering(List<String> parts, String action, Double minval, Double maxval,
//			Timestamp dawn, Timestamp dusk) {
//		
//		if(action != null) parts.add(String.format("action LIKE %s", "'"+action+"'"));
//		if(minval != null && maxval != null) parts.add(String.format("amount BETWEEN %.2f AND %.2f", minval, maxval).replace(',', '.'));
//		else if(minval != null) parts.add(String.format("amount >= %.2f", minval).replace(',', '.'));
//		else if(maxval != null) parts.add(String.format("amount <= %.2f", maxval).replace(',', '.'));
//		if(dawn != null && dusk != null)  parts.add(String.format("created_at BETWEEN %s AND %s", "'"+dawn+"'", "'"+dusk+"'"));
//		else if(dawn != null) parts.add(String.format("created_at >= %s", "'"+dawn+"'"));
//		else if(dusk != null) parts.add(String.format("created_at <= %s", "'"+dusk+"'"));
//		
//		for(int i = 1; i < parts.size(); i++) {
//			StringBuilder sb = new StringBuilder();
//			sb.append("AND ");
//			sb.append(parts.get(i));
//			parts.set(i, sb.toString());
//		}
//	}
	
}
