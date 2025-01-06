package com.github.irybov.account;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.irybov.shared.BillDTO;

@FeignClient(name = "bill", path = "/bills")
public interface BillClient {

	@GetMapping("/{owner}/list")
	public List<BillDTO> getList(@PathVariable int owner);
	
	@PostMapping
	public BillDTO create(@RequestParam String currency, @RequestParam int owner);
}
