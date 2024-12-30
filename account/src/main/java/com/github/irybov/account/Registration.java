package com.github.irybov.account;

import java.time.LocalDate;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Registration {

	@NotBlank(message = "Name must not be blank")
	@Size(min=2, max=20, message = "Name should be 2-20 chars length")
	@Pattern(regexp = "^[A-Z][a-z]{1,19}", message = "Please input name like Xx")
	private String name;
	
	@NotBlank(message = "Surname must not be blank")
	@Size(min=2, max=40, message = "Surname should be 2-40 chars length")
	@Pattern(regexp = "^[A-Z][a-z]{1,19}([-][A-Z][a-z]{1,19})?",
			message = "Please input surname like Xx or Xx-Xx")
	private String surname;
	
	@NotBlank(message = "Phone number must not be blank")
	@Pattern(regexp = "^\\d{10}$", message = "Please input phone number like a row of 10 digits")
	private String phone;
	
	@NotBlank(message = "Email address must not be blank")
	@Email(message = "Email address is not valid")
	@Size(min=10, max=60, message = "Email address should be 10-60 symbols length")
	private String email;
	
	@NotNull(message = "Please select your date of birth")
	@Past(message = "Birthday can't be future time")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate birthday;
	
	@NotBlank(message = "Password must not be blank")
	@Size(min=10, max=60, message = "Password should be 10-60 symbols length")
	private String password;
	
}
