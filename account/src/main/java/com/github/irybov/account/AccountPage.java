package com.github.irybov.account;

import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountPage {

    private int pageNumber = 0;
    private int pageSize = 10;
    private Sort.Direction sortDirection = Sort.Direction.ASC;
    private String sortBy = "id";
	
}
