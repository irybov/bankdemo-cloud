package com.github.irybov.account;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.github.irybov.shared.AccountDTO;

@Mapper(componentModel = "spring", 
	unmappedTargetPolicy = ReportingPolicy.IGNORE, 
	nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL, 
	injectionStrategy = InjectionStrategy.FIELD)
public interface AccountMapper {

	@Mapping(target = "bills", ignore = true)
	AccountDTO toDTO(Account entity);
	List<AccountDTO> toList(List<Account> entities);
	
	Account toDB(Registration registration);
	@AfterMapping
	default void initialize(@MappingTarget Account account) {
		account.setCreatedAt(Timestamp.valueOf(OffsetDateTime.now()
			.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()));
		account.setActive(true);
	}
}
