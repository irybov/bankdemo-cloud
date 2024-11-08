package com.github.irybov.account;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
	
	private final AccountService service;
	
	@GetMapping("/{id}")
	public Account getOne(@PathVariable int id) {return service.getOne(id);}

	@GetMapping
	public List<Account> getAll() {return service.getAll();}
	
}
