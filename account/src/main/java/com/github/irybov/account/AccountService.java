package com.github.irybov.account;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.irybov.shared.BillDTO;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, noRollbackFor = Exception.class)
@RequiredArgsConstructor
public class AccountService {

	private final RestTemplate restTemplate;
	private final AccountMapper mapStruct;
	private final AccountJDBC jdbc;
	
	public AccountDTO getOne(int id) {return mapStruct.toDTO(jdbc.findById(id).get());}
	
	public AccountDTO getOne(String phone) {
		
		Account account = getAccount(phone);
		ResponseEntity<List<BillDTO>> response = 
				restTemplate.exchange("http://BILL/bills/" + account.getId() + "/list", 
				HttpMethod.GET, null, new ParameterizedTypeReference<List<BillDTO>>(){});
		
		AccountDTO dto = mapStruct.toDTO(account);
		dto.setBills(new HashSet<>(response.getBody()));
		return dto;
	}
	
	public List<AccountDTO> getAll() {return mapStruct.toList((List<Account>) jdbc.findAll());}
	
	public BillDTO addBill(String phone, String currency) {
		
		Account account = getAccount(phone);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://BILL/bills")
    	        .queryParam("currency", currency)
    	        .queryParam("owner", account.getId());
        BillDTO bill = restTemplate.postForObject(uriBuilder.toUriString(), null, BillDTO.class);
        
        if(account.getBills() != null) {account.getBills().add(bill.getId());}
        else {account.setBills(Stream.of(bill.getId()).collect(Collectors.toSet()));}       
        jdbc.save(account);
        return bill;
	}
	
	Account getAccount(String phone) {return jdbc.findByPhone(phone);}
	
}
