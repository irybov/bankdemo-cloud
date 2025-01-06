package com.github.irybov.shared;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.irybov.shared.BillDTO;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode.CacheStrategy;

@Getter
@Setter
@EqualsAndHashCode(of = "phone", cacheStrategy = CacheStrategy.NEVER)
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
//	private String password;
	private Set<BillDTO> bills;

}
