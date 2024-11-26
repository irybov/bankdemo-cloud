package com.github.irybov.bill;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
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
		String update = 
				String.format("UPDATE bankdemo.bills SET is_active = %b WHERE id = %d", isActive, id);
		template.update(update);
		return isActive;
	}
	
	public void updateBalance(Map<Integer, Double> data) {
		
		List<Bill> bills = jdbc.findByIdIn(data.keySet());
		bills.forEach(bill -> bill.update(data.get(bill.getId())));
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
	}
	
	public void delete(int id) {jdbc.deleteById(id);}
	
	Bill getBill(int id) {return jdbc.findById(id).get();}
	
}
