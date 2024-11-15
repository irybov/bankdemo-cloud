package com.github.irybov.account;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.EqualsAndHashCode.CacheStrategy;

@Data
@EqualsAndHashCode(of = "phone", cacheStrategy = CacheStrategy.NEVER)
@Table(schema="bankdemo", name="accounts")
public class Account {
	
	@Id
	@Column("id")
	private Integer id;
	@Column("created_at")
	@NonNull
	private Timestamp createdAt;
	@Column("updated_at")
	private Timestamp updatedAt;
	@Column("is_active")
//	@JsonProperty(value="isActive")
	private boolean isActive;
	@Column("name")
	@NonNull
	private String name;
	@Column("surname")
	@NonNull
	private String surname;
	@Column("phone")
	@NonNull
	private String phone;
	@Column("email")
	@NonNull
	private String email;
	@Column("birthday")
	@NonNull
	private LocalDate birthday;
	@Column("password")
	@NonNull
	private String password;
	@Column("bills")
	private Set<Integer> bills;

}
