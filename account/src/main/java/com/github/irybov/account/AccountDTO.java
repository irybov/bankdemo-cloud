package com.github.irybov.account;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.irybov.shared.BillDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountDTO {
	
	private Timestamp createdAt;
	private Timestamp updatedAt;
	@JsonProperty(value="isActive")
	private boolean isActive;
	private String name;
	private String surname;
	private String phone;
	private String email;
	private LocalDate birthday;
	private Set<BillDTO> bills;

}
