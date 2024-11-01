package com.github.irybov.operation;

import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationPage {

    private int pageNumber = 0;
    private int pageSize = 20;
    private Sort.Direction sortDirection = Sort.Direction.DESC;
    private String sortBy = "id";
	
}
