package com.github.irybov.bill;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.irybov.shared.BillDTO;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BillMapperImpl.class)
public class BillMapperTest {
	
	@Autowired
	private  BillMapper mapStruct;
	private static Bill bill;
	
	@BeforeAll
	static void prepare() {bill = new Bill("SEA", 0); bill.create();}
	
	@Test
	void test() {
		
		BillDTO dto = mapStruct.toDTO(bill);
		List<Bill> bills = new ArrayList<>();
		bills.add(bill);
		bills.add(new Bill("SEA", 0));
		List<BillDTO> dtos = mapStruct.toList(bills);
		
		assertAll(
			() -> assertEquals(dto.getCurrency(), bill.getCurrency()), 
			() -> assertEquals(dto.isActive(), bill.isActive()), 
			() -> assertEquals(dto.getCurrency(), bill.getCurrency()), 
			() -> assertEquals(dto.getBalance(), bill.getBalance()), 
			() -> assertEquals(dto.getCreatedAt(), bill.getCreatedAt())
		);
		
		assertAll(
			() -> assertEquals(dtos.size(), bills.size()), 
			() -> assertThat(dtos).hasSameClassAs(new ArrayList<BillDTO>())
		);
	}

	@AfterAll
	static void clear() {bill = null;}
	
}
