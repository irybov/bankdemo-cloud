package com.github.irybov.account;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "phone", cacheStrategy = CacheStrategy.NEVER)
@Table(schema="bankdemo", name="accounts")
public class Account implements Serializable {
	
	@Id
	@Column("id")
	private Integer id;
	@CreatedDate
	@Column("created_at")
	@NonNull
	private Timestamp createdAt;
	@LastModifiedDate
	@Column("updated_at")
	private Timestamp updatedAt;
	@Column("is_active")
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
	@Column("roles")
	private Set<String> roles;

}
