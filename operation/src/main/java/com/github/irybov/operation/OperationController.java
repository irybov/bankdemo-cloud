package com.github.irybov.operation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;

@Api(description = "Operation's microservice controller")
@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {
	
	private final OperationService service;

	@ApiOperation("Saves money operation")
	@ApiResponses(value = {@ApiResponse(code = 201, message = "")})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@Valid @RequestBody OperationDTO dto) {service.save(dto);}
	
	@ApiOperation("Gets one operation")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "", response = Operation.class)})
	@GetMapping("/{id}")
	public Operation getOne(@PathVariable long id) {return service.getOne(id);}
	
	@ApiOperation("Gets list of operations")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "", 
			responseContainer = "List", response = Operation.class)})
	@GetMapping("/{id}/list")
	public List<Operation> getList(@PathVariable int id) {return service.getList(id);}
	
	@ApiOperation("Gets page of operations")
	@PagebleAPI
	@GetMapping("/{id}/page")
	public Page<Operation> getPage(@PathVariable int id, 
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
			Optional<LocalDate> mindate, 
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
			Optional<LocalDate> maxdate,
			@RequestParam(required = false) Double minval, 
			@RequestParam(required = false) Double maxval,
			@RequestParam(required = false) String action, 
			@SortDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		
		OffsetDateTime dateFrom = null;
		OffsetDateTime dateTo = null;
		if(mindate.isPresent()) dateFrom = OffsetDateTime.of(mindate.get(), 
				LocalTime.MIN, ZoneOffset.UTC);
		if(maxdate.isPresent()) dateTo = OffsetDateTime.of(maxdate.get(), 
				LocalTime.MAX, ZoneOffset.UTC);
		
		return service.getPage(id, action, minval, maxval, dateFrom, dateTo, pageable);
	}
	
}
