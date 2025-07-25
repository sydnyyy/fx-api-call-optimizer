package com.exchangerateapi.account.repository;

import com.exchangerateapi.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> { }
