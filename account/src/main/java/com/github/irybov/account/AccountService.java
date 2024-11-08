package com.github.irybov.account;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, noRollbackFor = Exception.class)
@RequiredArgsConstructor
public class AccountService {

	private final AccountJDBC jdbc;
	
	public Account getOne(int id) {return jdbc.findById(id).get();}
	
	public List<Account> getAll() {return (List<Account>) jdbc.findAll();}
	
}
