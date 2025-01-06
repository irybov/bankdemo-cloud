package com.github.irybov.operation;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "bill", path = "/bills")
public interface BillClient {

	@PatchMapping
	public void updateBalance(@RequestBody Map<Integer, Double> data);
}
