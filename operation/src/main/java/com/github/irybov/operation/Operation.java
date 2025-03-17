package com.github.irybov.operation;

import java.sql.Timestamp;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.querydsl.core.annotations.QueryProjection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.CacheStrategy;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(schema="bankdemo", name="operations")
@ToString
@EqualsAndHashCode(of = "id", cacheStrategy = CacheStrategy.NEVER)
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
	
//	@EqualsAndHashCode.Exclude
	@Id
	@Column("id")
	private Long id;
	@CreatedDate
	@Column("created_at")
	private Timestamp createdAt;
	@Column("amount")
	@NonNull
	private Double amount;
	@Column("action")
	@NonNull
	private String action;
	@Column("currency")
	@NonNull
	private String currency;
	@Column("sender")
	private Integer sender;
	@Column("recipient")
	private Integer recipient;
	@Column("bank")
	@NonNull
	private String bank;

}
