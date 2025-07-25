package com.exchangerateapi.account.service;

import com.exchangerateapi.account.dto.AccountDto;
import com.exchangerateapi.account.dto.AccountRegisterRequestDto;
import com.exchangerateapi.account.entity.Account;
import com.exchangerateapi.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final AccountRepository accountRepository;

	public AccountDto register(AccountRegisterRequestDto accountRegisterRequestDto) {
		Account account = Account.builder()
			.money(accountRegisterRequestDto.money())
			.currency(accountRegisterRequestDto.currency())
			.build();

		accountRepository.save(account);

		return AccountDto.builder()
			.id(account.getId())
			.money(account.getMoney())
			.currency(account.getCurrency())
			.build();
	}

	public AccountDto findAccountById(Long id) {
		Account account = accountRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Account not found"));

		return AccountDto.builder()
			.id(account.getId())
			.money(account.getMoney())
			.currency(account.getCurrency())
			.build();
	}
}
