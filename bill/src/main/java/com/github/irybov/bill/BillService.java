package com.github.irybov.bill;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BillService {

	private final BillJDBC jdbc;
	private final JdbcTemplate template;
	
	public void create(String currency, int owner) {
		Bill bill = new Bill(currency, owner);
		bill.create();
		jdbc.save(bill);
	}
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	public Bill getOne(int id) {return jdbc.findById(id).get();}
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	public List<Bill> getList(int owner) {return jdbc.findByOwner(owner);}
	
	public boolean changeStatus(int id) {
		String select = String.format("SELECT is_active FROM bankdemo.bills WHERE id = %d", id);
		boolean isActive = template.queryForObject(select, Boolean.class);
		if(isActive) isActive = false;
		else isActive = true;
		String update = String.format("UPDATE bankdemo.bills SET is_active = %b WHERE id = %d", isActive, id);
		template.update(update);
		return isActive;
	}
	
	public double updateBalance(int id, double amount) {
		Bill bill = getOne(id);
		bill.update(amount);
		jdbc.save(bill);
//		String select = String.format("SELECT balance FROM bankdemo.operations WHERE id = %d", id);
//		BigDecimal balance = template.queryForObject(select, BigDecimal.class);
//		return balance;
		return bill.getBalance().doubleValue();
	}
	
	public void delete(int id) {jdbc.deleteById(id);}
	
}
