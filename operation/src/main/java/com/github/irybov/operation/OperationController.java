package com.github.irybov.operation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

@Tag(name = "Operation's microservice controller")
@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
@Validated
public class OperationController {
	
	private final OperationService service;

	@Operation(description = "Save money operation")
	@ApiResponses(@ApiResponse(responseCode  = "201"))
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@Valid @RequestBody OperationDTO dto) {service.save(dto);}
	
	@Operation(description = "Gets one operation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = com.github.irybov.operation.Operation.class))), 
			@ApiResponse(responseCode = "404")
		})
	@GetMapping("/{id}")
	public com.github.irybov.operation.Operation getOne(@PathVariable long id) {return service.getOne(id);}
	
	@Operation(description = "Gets list of operations")
	@ApiResponses(@ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = com.github.irybov.operation.Operation.class)))))
	@GetMapping("/{id}/list")
	public List<com.github.irybov.operation.Operation> getList(@PathVariable int id) {return service.getList(id);}
	
	@Operation(description = "Gets page of operations")
	@PagebleAPI
	@GetMapping("/{id}/page")
	public Page<com.github.irybov.operation.Operation> getPage(@PathVariable int id, 
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
	
	@ResponseStatus(code = HttpStatus.NOT_FOUND)
	@ExceptionHandler
	protected void handleException(NoSuchElementException e){}
	
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<List<String>> handleMethodArgumentsException(MethodArgumentNotValidException e) {
	    List<String> errors = new ArrayList<String>();
	    for(FieldError error : e.getBindingResult().getFieldErrors()) {
	        errors.add(error.getDefaultMessage());
	    }
        return ResponseEntity.badRequest().body(errors);
    }
	
}
