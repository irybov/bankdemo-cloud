package com.github.irybov.bill;

import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillPage {

    private int pageNumber = 0;
    private int pageSize = 5;
    private Sort.Direction sortDirection = Sort.Direction.ASC;
    private String sortBy = "id";
	
}
