package com.github.irybov.account;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
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
	@Cacheable(cacheNames = "sets", key = "#owner", sync = true)
//	public List<BillDTO> getList(@PathVariable int owner);
	public Set<BillDTO> getList(@PathVariable int owner);
//	default List<BillDTO> emptyList(Throwable exception) {return new ArrayList<>();}
	default Set<BillDTO> emptyList(Throwable exception) {return new LinkedHashSet<>();}
	
	@PostMapping
	public BillDTO create(@RequestParam String currency, @RequestParam int owner);
	
	@DeleteMapping("/{id}")
	public void delete(@PathVariable int id);
}
