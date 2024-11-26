package com.github.irybov.bill;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillJDBC extends CrudRepository<Bill, Integer> {
	
	List<Bill> findByOwner(Integer owner);
	
	List<Bill> findByIdIn(Collection<Integer> ids);
}
