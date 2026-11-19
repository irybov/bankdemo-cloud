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
import org.springframework.http.MediaType;
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
	@ApiResponse(content = @Content(schema = @Schema(implementation = BillDTO.class)))
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BillDTO create(
			@Parameter(description = "Currency international ID code", example = "SEA")
			@Pattern(regexp = "^[A-Z]{3}$", message = "Currency should be 3 capital letters")
			@RequestParam String currency, 
			@Positive(message = "Owner's id should be positive")
			@Parameter(description = "ID of the account to add new bill", example = "42")
			@RequestParam int owner) {
		return service.create(currency, owner);
	}
	
	@Operation(description = "Fetches one bill")
	@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BillDTO.class)))
	@GetMapping("/{id}")
	public BillDTO getOne(
			@Parameter(description = "ID of the bill to fetch", example = "42")
			@PathVariable int id) {
		return service.getOne(id);
	}
	
	@Operation(description = "Gets list of bills")
	@ApiResponse(responseCode = "200", 
		content = @Content(array = @ArraySchema(schema = @Schema(implementation = BillDTO.class))))
	@GetMapping("/{owner}/list")
//	public List<BillDTO> getList(@PathVariable int owner) {return service.getList(owner);}
	public Set<BillDTO> getList(
			@Parameter(description = "ID of the account to get bills", example = "42")
			@PathVariable int owner) {
		return service.getList(owner);
	}
	
	@Operation(description = "Changes bill's status")
	@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Boolean.class))) 
	@PatchMapping("/{id}/status")
	public boolean changeStatus(
	        @Parameter(description = "ID of the bill to delete", example = "42")
	        @PathVariable int id) {
		return service.changeStatus(id);
	}
	
	@Operation(description = "Updates bill's balance")
	@PatchMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateBalance(
	        @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                description = "Map of bill IDs to balance delta/amount",
	                content = @Content(
	                    mediaType = MediaType.APPLICATION_JSON_VALUE,
	                    schema = @Schema(
	                        example = "{\"101\": 250.50, \"102\": -40.00}"
	                    )
	                )
	            )
			@RequestBody Map<Integer, Double> data) {
		service.updateBalance(data);
	}
	
	@Operation(description = "Deletes existing bill")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@Parameter(description = "ID of the bill to delete", example = "42")
			@PathVariable int id) {
		service.delete(id);
	}
	
	@ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NoSuchElementException.class, EmptyResultDataAccessException.class})
    protected void handleSearchingException(RuntimeException e) {}
    @ExceptionHandler(HandlerMethodValidationException.class)
	protected ResponseEntity<List<String>> handleMethodValidationException(HandlerMethodValidationException exc) {
		List<String> errors = exc.getParameterValidationResults()
			.stream()
			.map(ParameterValidationResult::getResolvableErrors)
			.flatMap(List::stream)
			.map(MessageSourceResolvable::getDefaultMessage)
			.collect(Collectors.toList());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
	}
	@ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<List<String>> handleValidationException(ConstraintViolationException e) {
	    List<String> violations = new ArrayList<String>();
	    for(ConstraintViolation<?> violation : e.getConstraintViolations()) {
	    	violations.add(violation.getMessage());
	    }
        return ResponseEntity.badRequest().body(violations);
    }
    
}
