package com.github.irybov.bill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.github.irybov.shared.BillDTO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;

@Api(description = "Bill's microservice controller")
@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
@Validated
public class BillController {
	
	private final BillService service;
	
	@ApiOperation("Creates new bill")
	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "", response = BillDTO.class), 
			@ApiResponse(code = 400, message = "", responseContainer = "List", response = String.class)})
	@ApiParam(value = "Currency should be 3 capital letters", required = true, format = "^[A-Z]{3}$")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BillDTO create(
			@Pattern(regexp = "^[A-Z]{3}$", message = "Currency should be 3 capital letters") 
			@RequestParam String currency, 
			@Positive(message = "Owner's id should be positive")
			@RequestParam int owner) {
		return service.create(currency, owner);
	}
	
	@ApiOperation("Gets one bill")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "", response = BillDTO.class), 
			@ApiResponse(code = 404, message = "")})
	@GetMapping("/{id}")
	public BillDTO getOne(@PathVariable int id) {return service.getOne(id);}
	
	@ApiOperation("Gets list of bills")
	@ApiResponses(@ApiResponse(code = 200, message = "", responseContainer = "List", response = BillDTO.class))
	@GetMapping("/{owner}/list")
	public List<BillDTO> getList(@PathVariable int owner) {return service.getList(owner);}
	
	@ApiOperation("Changes bill's status")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "", response = Boolean.class), 
			@ApiResponse(code = 404, message = "")})
	@PatchMapping("/{id}/status")
	public boolean changeStatus(@PathVariable int id) {return service.changeStatus(id);}
	
	@ApiOperation("Updates bill's balance")
	@ApiResponses(@ApiResponse(code = 201, message = ""))
	@PatchMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateBalance(@RequestBody Map<Integer, Double> data) {service.updateBalance(data);}
	
	@ApiOperation("Deletes existing bill")
	@ApiResponses(@ApiResponse(code = 204, message = ""))
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable int id) {service.delete(id);}
	
	@ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NoSuchElementException.class, EmptyResultDataAccessException.class})
    protected void handleSearchingException(RuntimeException e) {}
    
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<List<String>> handleValidationException(ConstraintViolationException e) {
	    List<String> violations = new ArrayList<String>();
	    for(ConstraintViolation<?> violation : e.getConstraintViolations()) {
	    	violations.add(violation.getMessage());
	    }
        return ResponseEntity.badRequest().body(violations);
    }
    
}
