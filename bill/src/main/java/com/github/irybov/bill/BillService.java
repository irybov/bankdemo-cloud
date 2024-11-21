package com.github.irybov.bill;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.irybov.shared.BillDTO;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BillService {
	
	private final BillMapper mapStruct;
	private final BillJDBC jdbc;
	private final JdbcTemplate template;
	
	public BillDTO create(String currency, int owner) {
		Bill bill = new Bill(currency, owner);
		bill.create();
		bill = jdbc.save(bill);
		return mapStruct.toDTO(bill);
	}
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	public BillDTO getOne(int id) {return mapStruct.toDTO(getBill(id));}
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	public List<BillDTO> getList(int owner) {return mapStruct.toList(jdbc.findByOwner(owner));}
	
	public boolean changeStatus(int id) {
		String select = String.format("SELECT is_active FROM bankdemo.bills WHERE id = %d", id);
		boolean isActive = template.queryForObject(select, Boolean.class);
		if(isActive) isActive = false;
		else isActive = true;
		String update = String.format("UPDATE bankdemo.bills SET is_active = %b WHERE id = %d", isActive, id);
		template.update(update);
		return isActive;
	}
	
	public void updateBalance(Map<Integer, Double> data) {
		
		List<Bill> bills = new LinkedList<>();
		for(Integer id : data.keySet()) {
			Bill bill = getBill(id);
			bill.update(data.get(id));
			bills.add(bill);
		}
		jdbc.saveAll(bills);
//		String select = String.format("SELECT balance FROM bankdemo.operations WHERE id = %d", id);
//		BigDecimal balance = template.queryForObject(select, BigDecimal.class);
//		return balance;
//		return bill.getBalance().doubleValue();
	}
	
	public void delete(int id) {jdbc.deleteById(id);}
	
	Bill getBill(int id) {return jdbc.findById(id).get();}
	
}
