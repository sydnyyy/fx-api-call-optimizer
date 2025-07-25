package com.exchangerateapi.account.dto;

import com.exchangerateapi.global.enums.Currency;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountDto (
	Long id,
	BigDecimal money,
	Currency currency
) {
}
