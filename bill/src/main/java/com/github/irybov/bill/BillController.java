package com.github.irybov.bill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.method.ParameterValidationResult;
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
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.github.irybov.shared.BillDTO;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

@Tag(name = "Bill's microservice controller")
@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
// @Validated
public class BillController {
	
	private final BillService service;
	
	@Operation(description = "Creates new bill")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = BillDTO.class))), 
			@ApiResponse(responseCode = "400", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
		})
	@Parameters({	
		@Parameter(name = "currency", description = "Currency should be 3 capital letters", required = true, schema = @Schema(type = "string", format = "^[A-Z]{3}$")), 
		@Parameter(name = "owner", description = "Owner's id should be positive", required = true, schema = @Schema(type = "integer"))
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BillDTO create(
			@RequestParam @Pattern(regexp = "^[A-Z]{3}$", message = "Currency should be 3 capital letters") 
			String currency, 
			@RequestParam @Positive(message = "Owner's id should be positive")
			int owner) {
		return service.create(currency, owner);
	}
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<List<String>> handleMethodValidationException(HandlerMethodValidationException exc) {
		List<String> errors = exc.getParameterValidationResults()
			.stream()
			.map(ParameterValidationResult::getResolvableErrors)
			.flatMap(List::stream)
			.map(MessageSourceResolvable::getDefaultMessage)
			.collect(Collectors.toList());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
	}
	
	@Operation(description = "Gets one bill")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BillDTO.class))), 
			@ApiResponse(responseCode = "404")})
	@GetMapping("/{id}")
	public BillDTO getOne(@PathVariable int id) {return service.getOne(id);}
	
	@Operation(description = "Gets list of bills")
//	@ApiResponse(code = 200, message = "", responseContainer = "List", response = BillDTO.class)
	@ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BillDTO.class))))
	@GetMapping("/{owner}/list")
//	public List<BillDTO> getList(@PathVariable int owner) {return service.getList(owner);}
	public Set<BillDTO> getList(@PathVariable int owner) {return service.getList(owner);}
	
	@Operation(description = "Changes bill's status")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Boolean.class))), 
			@ApiResponse(responseCode = "404")})
	@PatchMapping("/{id}/status")
	public boolean changeStatus(@PathVariable int id) {return service.changeStatus(id);}
	
	@Operation(description = "Updates bill's balance")
	@ApiResponse(responseCode = "201")
	@PatchMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateBalance(@RequestBody Map<Integer, Double> data) {service.updateBalance(data);}
	
	@Operation(description = "Deletes existing bill")
	@ApiResponse(responseCode = "204")
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
