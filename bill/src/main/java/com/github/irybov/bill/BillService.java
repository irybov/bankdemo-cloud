package com.github.irybov.bill;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.irybov.shared.BillDTO;

import lombok.RequiredArgsConstructor;

@EnableBinding(Source.class)
@Service
@Transactional
@RequiredArgsConstructor
//@CacheConfig(cacheNames = "bills")
public class BillService {
	
	private final BillMapper mapStruct;
	private final BillJDBC jdbc;
	private final JdbcTemplate template;
	private final Source source;
	
	@Lazy
	@Autowired
	private BillService self;
	
	private static final String BILLS = "bills";
	private static final String DTOS = "dtos";
	private static final String SETS = "sets";
	
	@CachePut(cacheNames = DTOS, key = "#result.id")
	public BillDTO create(String currency, int owner) {
		
		Bill bill = new Bill(currency, owner);
		bill.create();
		bill = jdbc.save(bill);

		BillDTO dto = mapStruct.toDTO(bill);
//		BillDTO dto = self.getOne(bill.getId());
		Set<BillDTO> list = self.getList(bill.getOwner());
		list.add(dto);
		self.updateCachedList(owner, list);
		
		return dto;
	}
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	@Cacheable(cacheNames = DTOS, key = "#id", sync = true)
	public BillDTO getOne(int id) {return mapStruct.toDTO(self.getBill(id));}
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	@Cacheable(cacheNames = SETS, key = "#owner", sync = true)
//	public List<BillDTO> getList(int owner) {return mapStruct.toList(jdbc.findByOwner(owner));}
	public Set<BillDTO> getList(int owner) {return mapStruct.toSet(jdbc.findByOwner(owner));}
	
	public boolean changeStatus(int id) {
		
		String select = String.format("SELECT is_active FROM bankdemo.bills WHERE id = %d", id);
		boolean isActive = template.queryForObject(select, Boolean.class);
		isActive = !isActive;
//		isActive ^= true;
		String update = 
				String.format("UPDATE bankdemo.bills SET is_active = %b WHERE id = %d", isActive, id);
		template.update(update);
		
		Bill bill = self.getBill(id);
		bill.setActive(isActive);
		self.updateCachedBill(bill);
		BillDTO dto = self.getOne(id);
		Set<BillDTO> dtos = self.getList(bill.getOwner());
		dtos.remove(dto);
		dto.setActive(isActive);
		dto = self.updateCachedDTO(dto);
		dtos.add(dto);
		int owner = bill.getOwner();
		self.updateCachedList(owner, dtos);
		
		return isActive;
	}
	
	public void updateBalance(Map<Integer, Double> data) {
		
//		List<Bill> bills = jdbc.findByIdIn(data.keySet());
		Set<Bill> bills = jdbc.findByIdIn(data.keySet());
//		bills.forEach(bill -> bill.update(data.get(bill.getId())));
		
		for(Bill bill : bills) {
			int id = bill.getId();
			bill.update(data.get(id));
			self.updateCachedBill(bill);
			BillDTO dto = self.getOne(id);
			int owner = bill.getOwner();
			Set<BillDTO> dtos = self.getList(owner);
			dtos.remove(dto);
			dto.setBalance(bill.getBalance());
			dto = self.updateCachedDTO(dto);
			dtos.add(dto);
			self.updateCachedList(owner, dtos);
		}
		jdbc.saveAll(bills);
/*		
		template.batchUpdate("UPDATE bankdemo.bills SET balance = ? WHERE id = ?",
	             new BatchPreparedStatementSetter() {
		    @Override
		    public void setValues(PreparedStatement ps, int i) throws SQLException {
		        ps.setBigDecimal(1, bills.get(i).getBalance());
		        ps.setLong(2, bills.get(i).getId());
		    }
		    @Override
		    public int getBatchSize() {return bills.size();}
		});
*/
		Message<Map<Integer, Double>> message = MessageBuilder.withPayload(data).build();
		source.output().send(message);
	}
	
	@Caching(evict = {
			@CacheEvict(cacheNames = DTOS, key = "#id"), 
			@CacheEvict(cacheNames = BILLS, key = "#id")
			})
	public void delete(int id) {
		
		Bill bill = self.getBill(id);
		BillDTO dto = self.getOne(id);
		int owner = bill.getOwner();
		Set<BillDTO> list = self.getList(owner);
		list.remove(dto);
		if(list.isEmpty()) self.deleteCachedList(owner);
		else self.updateCachedList(owner, list);
		jdbc.deleteById(id);
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	@CachePut(cacheNames = DTOS, key = "#result.id", unless = "#result == null")
	public BillDTO updateCachedDTO(BillDTO dto) {return dto;}
/*
	@Transactional(propagation = Propagation.SUPPORTS)
	@Cacheable(cacheNames = LISTS, key = "#owner", unless = "#result.isEmpty()")
	public Set<BillDTO> getCachedList(int owner) {return new LinkedHashSet<>();}
*/
	@Transactional(propagation = Propagation.SUPPORTS)
	@CachePut(cacheNames = SETS, key = "#owner")
	public Set<BillDTO> updateCachedList(int owner, Set<BillDTO> list) {return list;}

	@Transactional(propagation = Propagation.SUPPORTS)
	@CacheEvict(cacheNames = SETS, key = "#owner", beforeInvocation = true)
	public void deleteCachedList(int owner) {}
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	@Cacheable(cacheNames = BILLS, key = "#id", sync = true)
	public Bill getBill(int id) {return jdbc.findById(id).get();}
	
	@Transactional(propagation = Propagation.SUPPORTS)
	@CachePut(cacheNames = BILLS, key = "#result.id")
	public Bill updateCachedBill(Bill bill) {return bill;}
	
}
