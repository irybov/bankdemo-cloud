package com.github.irybov.account;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountJDBC extends CrudRepository<Account, Integer> {

	Account findByPhone(String phone);
}
