package com.github.irybov.account;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
	
	private final AccountService service;
	
	@PostMapping()
	@ResponseStatus(HttpStatus.CREATED)
	public void create(@RequestBody Registration registration) {service.create(registration);}
	
	@RequestMapping(path = "/login", method = RequestMethod.HEAD)
	public void getToken(@RequestHeader(name = "Login") String header, HttpServletResponse res) {
		String token = service.generateToken(header);
		res.addHeader("Token", token);
	}
	
//	@GetMapping("/{id}")
//	public AccountDTO getOne(@PathVariable int id) {return service.getOne(id);}
	
	@GetMapping("/{phone}")
	public AccountDTO getOne(@PathVariable String phone) {return service.getOne(phone);} 
//			@RequestHeader(name = HttpHeaders.AUTHORIZATION) String header) {
//		if(service.checkFraud(phone, header)) {return service.getOne(phone);}
//		else {throw new SecurityException();}
//	}

	@GetMapping
	public List<AccountDTO> getAll() {return service.getAll();}
	
	@PostMapping("/{phone}/bills")
	@ResponseStatus(HttpStatus.CREATED)
	public BillDTO addBill(@PathVariable String phone, @RequestParam String currency) {
		return service.addBill(phone, currency);
//			@RequestHeader(name = HttpHeaders.AUTHORIZATION) String header) {
//		if(service.checkFraud(phone, header)) {return service.addBill(phone, currency);}
//		else {throw new SecurityException();}
	}
	
	@DeleteMapping("/{phone}/bills/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteBill(@PathVariable String phone, @PathVariable Integer id) {
		service.deleteBill(phone, id);
	}
	
}
