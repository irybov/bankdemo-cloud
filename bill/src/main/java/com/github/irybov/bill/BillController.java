package com.github.irybov.bill;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillController {
	
	private final BillService service;
	
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void create(@RequestParam String currency, @RequestParam int owner) {
		service.create(currency, owner);
	}
	
	@GetMapping("/{id}")
	public Bill getOne(@PathVariable int id) {return service.getOne(id);}
	
	@GetMapping("/{owner}/list")
	public List<Bill> getList(@PathVariable int owner) {return service.getList(owner);}
	
	@PatchMapping("/{id}/status")
	public boolean changeStatus(@PathVariable int id) {return service.changeStatus(id);}
	
	@PatchMapping("/{id}/balance")
	public double updateBalance(@PathVariable int id, @RequestParam double amount) {
		return service.updateBalance(id, amount);
	}
	
	@DeleteMapping("/{id}")
	public void delete(@PathVariable int id) {service.delete(id);}
	
}
