package com.github.irybov.operation;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OperationDTO {
	
	@NotNull(message = "Amount must not be null")
	@Positive(message = "Amount of money should be higher than zero")
	private Double amount;
	
	@NotNull(message = "Action must not be null")
	private Action action;
	
	@NotBlank(message = "Currency must not be blank")
	@Pattern(regexp = "^[A-Z]{3}$", message = "Currency code should be 3 capital characters length")
	private String currency;
	
	@Positive(message = "Sender's bill number should be positive")
	@Digits(fraction = 0, integer = 9, message = "Sender's bill number should be less than 10 digits length")
	private Integer sender;
	
	@Positive(message = "Recepient's bill number should be positive")
	@Digits(fraction = 0, integer = 9, message = "Recepient's bill number should be less than 10 digits length")
	private Integer recipient;
	
	@NotBlank(message = "Bank's name must not be blank")
	@Size(max = 30)
	private String bank;

}
