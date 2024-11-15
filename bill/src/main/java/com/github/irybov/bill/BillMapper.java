package com.github.irybov.bill;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.github.irybov.shared.BillDTO;

@Mapper(componentModel = "spring", 
	nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL, 
	injectionStrategy = InjectionStrategy.FIELD)
public interface BillMapper {
	
	BillDTO toDTO(Bill entity);
	List<BillDTO> toList(List<Bill> entities);

}
