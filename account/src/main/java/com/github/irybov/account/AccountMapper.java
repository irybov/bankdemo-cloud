package com.github.irybov.account;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", 
	unmappedTargetPolicy = ReportingPolicy.IGNORE, 
	nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL, 
	injectionStrategy = InjectionStrategy.FIELD)
public interface AccountMapper {

	@Mapping(target = "bills", ignore = true)
	AccountDTO toDTO(Account entity);
	List<AccountDTO> toList(List<Account> entities);
	
}
