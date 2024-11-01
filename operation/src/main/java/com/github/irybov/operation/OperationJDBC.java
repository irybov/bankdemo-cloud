package com.github.irybov.operation;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationJDBC extends PagingAndSortingRepository<Operation, Long> {
	
//	@Query("SELECT * FROM bankdemo.operations WHERE sender=:id OR recipient=:id ORDER BY id DESC")
	List<Operation> findBySenderOrRecipientOrderByIdDesc(int sender, int recipient);

}
