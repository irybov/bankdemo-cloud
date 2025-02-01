package com.github.irybov.account;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.irybov.shared.BillDTO;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(name = "bill", path = "/bills")
public interface BillClient {

	@GetMapping("/{owner}/list")
	@CircuitBreaker(name = "bill", fallbackMethod = "emptyList")
	public List<BillDTO> getList(@PathVariable int owner);	
	default List<BillDTO> emptyList(Throwable exception) {return new ArrayList<>();}
	
	@PostMapping
	public BillDTO create(@RequestParam String currency, @RequestParam int owner);
}
