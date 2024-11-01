package com.github.irybov.operation;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(schema="bankdemo", name="operations")
@ToString
public class Operation {
	
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
