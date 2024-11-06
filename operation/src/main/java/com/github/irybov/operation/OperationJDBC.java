package com.github.irybov.operation;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationJDBC extends PagingAndSortingRepository<Operation, Long> {
	
	List<Operation> findBySenderOrRecipientOrderByIdDesc(int sender, int recipient);
}
