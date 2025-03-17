package com.github.irybov.shared;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode.CacheStrategy;

@Getter
@Setter
@EqualsAndHashCode(of = "id", cacheStrategy = CacheStrategy.NEVER)
public class BillDTO implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Integer id;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	@JsonProperty(value="isActive")
	private boolean isActive;
	private BigDecimal balance;
	private String currency;

}
