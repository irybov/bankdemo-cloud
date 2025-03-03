package com.github.irybov.account;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import lombok.RequiredArgsConstructor;

@Api(description = "Account's microservice controller")
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {
	
	private final AccountService service;
	
	@ApiOperation("Registers new account")
	@ApiResponses(value = {@ApiResponse(code = 201, message = "")})
	@PostMapping()
	@ResponseStatus(HttpStatus.CREATED)
	public void create(@Valid @RequestBody Registration registration) {service.create(registration);}
	
	@ApiOperation("Returns JWT")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "", 
		responseHeaders = @ResponseHeader(name = "Token", description = "", response = String.class))})
	@RequestMapping(method = RequestMethod.HEAD)
	public void getToken(@RequestHeader(name = "Login") String header, HttpServletResponse res) {
		String token = service.generateToken(header);
		res.addHeader("Token", token);
	}
	
//	@GetMapping("/{id}")
//	public AccountDTO getOne(@PathVariable int id) {return service.getOne(id);}
	
	@ApiOperation("Gets one account")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "", response = AccountDTO.class)})
	@GetMapping("/{phone}")
	public AccountDTO getOne(@PathVariable String phone) {return service.getOne(phone);} 
//			@RequestHeader(name = HttpHeaders.AUTHORIZATION) String header) {
//		if(service.checkFraud(phone, header)) {return service.getOne(phone);}
//		else {throw new SecurityException();}
//	}

	@ApiOperation("Gets list of accounts")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "", 
		responseContainer = "List", response = AccountDTO.class)})
	@GetMapping
	public List<AccountDTO> getAll() {return service.getAll();}
	
	@ApiOperation("Changes account's password")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "")})
	@ApiParam(value = "Password should be 10-60 symbols length", required = true)
	@PatchMapping("/{phone}")
	public void changePassword(@PathVariable String phone, 
			@NotBlank(message = "Password must not be blank") 
			@Size(min=10, max=60, message = "Password should be 10-60 symbols length") 
			@RequestParam String password) {
		service.changePassword(phone, password);
	}
	
	@ApiOperation("Adds new bill to account")
	@ApiResponses(value = {@ApiResponse(code = 201, message = "", response = BillDTO.class)})
	@PostMapping("/{phone}/bills")
	@ResponseStatus(HttpStatus.CREATED)
	public BillDTO addBill(@PathVariable String phone, @RequestParam String currency) {
		return service.addBill(phone, currency);
//			@RequestHeader(name = HttpHeaders.AUTHORIZATION) String header) {
//		if(service.checkFraud(phone, header)) {return service.addBill(phone, currency);}
//		else {throw new SecurityException();}
	}
	
	@ApiOperation("Deletes existing bill from account")
	@ApiResponses(value = {@ApiResponse(code = 204, message = "")})
	@DeleteMapping("/{phone}/bills/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteBill(@PathVariable String phone, @PathVariable Integer id) {
		service.deleteBill(phone, id);
	}
	
}
