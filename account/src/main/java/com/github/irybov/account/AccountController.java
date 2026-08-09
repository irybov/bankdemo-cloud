package com.github.irybov.account;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

@Tag(name = "Account's microservice controller")
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {
	
	private final AccountService service;
	
	@Operation(description = "Registers new account")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201"), 
		@ApiResponse(responseCode = "404", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
	})
	@PostMapping()
	@ResponseStatus(HttpStatus.CREATED)
	public void create(@Valid @RequestBody Registration registration) {service.create(registration);}
	
	@Operation(description = "Returns JWT")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", headers = @Header(
			name = "Token", 
			description = "Security token", 
			schema = @Schema(type = "string")
		)), 
		@ApiResponse(responseCode = "400"), 
		@ApiResponse(responseCode = "404")
	})
	@RequestMapping(method = RequestMethod.HEAD)
	public void getToken(@Pattern(regexp = "\\d{10}[:]{1}.{10,60}", message = "Header should match pattern") 
			@RequestHeader(name = "Login") String header, HttpServletResponse res) {
		String token = service.generateToken(header);
		res.addHeader("Token", token);
	}
	
//	@GetMapping("/{id}")
//	public AccountDTO getOne(@PathVariable int id) {return service.getOne(id);}
	
	@Operation(description = "Gets one account")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AccountDTO.class))), 
		@ApiResponse(responseCode = "404")
	})
	@GetMapping("/{phone}")
	public AccountDTO getOne(@PathVariable String phone) {return service.getOne(phone);} 
//			@RequestHeader(name = HttpHeaders.AUTHORIZATION) String header) {
//		if(service.checkFraud(phone, header)) {return service.getOne(phone);}
//		else {throw new SecurityException();}
//	}

	@Operation(description = "Gets list of accounts")
	@ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountDTO.class))))
	@GetMapping
	public List<AccountDTO> getAll() {return service.getAll();}
	
	@Operation(description = "Changes account's password")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200"), 
			@ApiResponse(responseCode = "400", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
	})
	@Parameters({	
		@Parameter(name = "phone", description = "Phone number must not be blank", required = true, schema = @Schema(type = "string", format = "^\\d{10}$")), 
		@Parameter(name = "password", description = "Password should be 10-60 symbols length", required = true, schema = @Schema(type = "string"))
	})
	@PatchMapping("/{phone}")
	public void changePassword(@PathVariable String phone, 
			@NotBlank(message = "Password must not be blank") 
			@Size(min=10, max=60, message = "Password should be 10-60 symbols length") 
			@RequestParam String password) {
		service.changePassword(phone, password);
	}
	
	@Operation(description = "Adds new bill to account")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = BillDTO.class))), 
		@ApiResponse(responseCode = "400", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))})
	@Parameters({	
		@Parameter(name = "currency", description = "Currency should be 3 capital letters", required = true, schema = @Schema(type = "string", format = "^[A-Z]{3}$")), 
		@Parameter(name = "phone", description = "Phone number must not be blank", required = true, schema = @Schema(type = "string", format = "^\\d{10}$"))
	})
	@PostMapping("/{phone}/bills")
	@ResponseStatus(HttpStatus.CREATED)
	public BillDTO addBill(@PathVariable String phone, 
			@Pattern(regexp = "^[A-Z]{3}$", message = "Currency should be 3 capital letters") 
			@RequestParam String currency) {
		return service.addBill(phone, currency);
//			@RequestHeader(name = HttpHeaders.AUTHORIZATION) String header) {
//		if(service.checkFraud(phone, header)) {return service.addBill(phone, currency);}
//		else {throw new SecurityException();}
	}
	
	@Operation(description = "Deletes existing bill from account")
	@ApiResponse(responseCode = "204")
	@DeleteMapping("/{phone}/bills/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteBill(@PathVariable String phone, @PathVariable Integer id) {
		service.deleteBill(phone, id);
	}
	
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<List<String>> handleArgumentsException(MethodArgumentNotValidException e) {
	    List<String> errors = new ArrayList<String>();
	    for(FieldError error : e.getBindingResult().getFieldErrors()) {
	        errors.add(error.getDefaultMessage());
	    }
        return ResponseEntity.badRequest().body(errors);
    }
	
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<List<String>> handleValidationException(ConstraintViolationException e) {
	    List<String> violations = new ArrayList<String>();
	    for(ConstraintViolation<?> violation : e.getConstraintViolations()) {
	    	violations.add(violation.getMessage());
	    }
        return ResponseEntity.badRequest().body(violations);
    }
    
    @ExceptionHandler(ResponseStatusException.class)
    protected ResponseEntity<String> handleSearchingException(ResponseStatusException e) {
    	return new ResponseEntity<String>(e.getReason(), e.getStatusCode());
    }
    
}
