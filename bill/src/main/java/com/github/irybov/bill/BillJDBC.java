package com.github.irybov.bill;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillJDBC extends CrudRepository<Bill, Integer> {
	
	List<Bill> findByOwner(int owner);
}
