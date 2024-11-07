package com.github.irybov.operation;

import java.sql.Timestamp;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.querydsl.core.annotations.QueryProjection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(schema="bankdemo", name="operations")
@ToString
public class Operation {
	
	@QueryProjection
	public Operation(String action, Double amount, String bank, Timestamp createdAt, String currency, 
			Long id, Integer sender, Integer recipient) {
		this.id = id;
		this.createdAt = createdAt;
		this.amount = amount;
		this.action = action;
		this.currency = currency;
		this.sender = sender;
		this.recipient = recipient;
		this.bank = bank;
	}
	
	@EqualsAndHashCode.Exclude
	@Id
	@Column("id")
	private Long id;
	@Column("created_at")
	private Timestamp createdAt;
	@Column("amount")
	private Double amount;
	@Column("action")
	private String action;
	@Column("currency")
	private String currency;
	@Column("sender")
	private Integer sender;
	@Column("recipient")
	private Integer recipient;
	@Column("bank")
	private String bank;

}
