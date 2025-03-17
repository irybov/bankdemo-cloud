package com.github.irybov.bill;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		assertAll(
				() -> assertEquals(dto.isActive(), bill.isActive()), 
				() -> assertEquals(dto.getCurrency(), bill.getCurrency()), 
				() -> assertEquals(dto.getBalance(), bill.getBalance()), 
				() -> assertEquals(dto.getCreatedAt(), bill.getCreatedAt())
			);
		
		final int size = new Random().nextInt(Byte.MAX_VALUE + 1);
		List<Bill> list = 
//				Collections.nCopies(size, new Bill("SEA", 0)).stream()
				Stream.generate(() -> new Bill("SEA", 0)).limit(size)
				.collect(Collectors.toList());
		int i = 0;
		for(Bill bill : list) bill.setId(new Integer(++i));
		
		List<BillDTO> dtoList = mapStruct.toList(list);
		assertAll(
			() -> assertEquals(dtoList.size(), size), 
			() -> assertThat(dtoList).hasSameClassAs(new ArrayList<BillDTO>())
		);		
		Set<Bill> bills = new HashSet<>(list);
		Set<BillDTO> dtoSet = mapStruct.toSet(bills);
		assertAll(
				() -> assertEquals(dtoSet.size(), size), 
				() -> assertThat(dtoSet).hasSameClassAs(new HashSet<BillDTO>())
		);
		
	}

	@AfterAll
	static void clear() {bill = null;}
	
}
