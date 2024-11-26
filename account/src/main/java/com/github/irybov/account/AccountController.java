package com.github.irybov.account;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
	
	private final AccountService service;
	
//	@GetMapping("/{id}")
//	public AccountDTO getOne(@PathVariable int id) {return service.getOne(id);}
	
	@GetMapping("/{phone}")
	public AccountDTO getOne(@PathVariable String phone) {return service.getOne(phone);}

	@GetMapping
	public List<AccountDTO> getAll() {return service.getAll();}
	
	@PatchMapping("/{phone}")
	public BillDTO addBill(@PathVariable String phone, @RequestParam String currency) {
		return service.addBill(phone, currency);
	}
	
}
