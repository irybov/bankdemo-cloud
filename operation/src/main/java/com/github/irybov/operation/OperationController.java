package com.github.irybov.operation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {
	
	private final OperationService service;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestBody Operation operation) {service.save(operation);}
	
	@GetMapping("/{id}")
	public Operation getOne(@PathVariable long id) {return service.getOne(id);}
	
	@GetMapping("/{id}/list")
	public List<Operation> getList(@PathVariable int id) {return service.getList(id);}
	
	@GetMapping("/{id}/page")
	public Page<Operation> getPage(@PathVariable int id, 
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
			Optional<LocalDate> mindate, 
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
			Optional<LocalDate> maxdate,
			@RequestParam(required = false) Double minval, 
			@RequestParam(required = false) Double maxval,
			@RequestParam(required = false) String action, 
			Pageable pageable){
		
		OffsetDateTime dateFrom = null;
		OffsetDateTime dateTo = null;
		if(mindate.isPresent()) dateFrom = OffsetDateTime.of(mindate.get(), 
				LocalTime.MIN, ZoneOffset.UTC);
		if(maxdate.isPresent()) dateTo = OffsetDateTime.of(maxdate.get(), 
				LocalTime.MAX, ZoneOffset.UTC);
		
		return service.getPage(id, action, minval, maxval, dateFrom, dateTo, pageable);
	}
	
}
