package com.github.irybov.bill;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.EqualsAndHashCode.CacheStrategy;

@Data
//@NoArgsConstructor
@Table(schema="bankdemo", name="bills")
@EqualsAndHashCode(of = "id", cacheStrategy = CacheStrategy.NEVER)
public class Bill implements Serializable {

	private static final long serialVersionUID = 1L;
	
//	@EqualsAndHashCode.Exclude
	@Id
	@Column("id")
	private Integer id;
	@CreatedDate
	@Column("created_at")
	private Timestamp createdAt;
	@LastModifiedDate
	@Column("updated_at")
	private Timestamp updatedAt;
	@Column("is_active")
//	@JsonProperty(value="isActive")
	private boolean isActive;
	@Column("balance")
	private BigDecimal balance;
	@Column("currency")
	@NonNull
	private String currency;
	@Column("owner")
	@NonNull
	private Integer owner;
	
	void create() {
		this.createdAt = Timestamp.valueOf(OffsetDateTime.now()
		   .atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
		this.balance = BigDecimal.valueOf(0.00).setScale(2);
		this.isActive = true;
	}
	void update(double amount) {
		this.updatedAt = Timestamp.valueOf(OffsetDateTime.now()
		   .atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
		if(amount > 0.00) this.balance = balance.add(BigDecimal.valueOf(amount).setScale(2));
		else this.balance = balance.subtract(BigDecimal.valueOf(amount).negate().setScale(2));
	}
	
}
