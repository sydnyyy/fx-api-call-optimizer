package com.exchangerateapi.transaction.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TransactionLogDto (
	Long sendId,
	Long receiverId,
	BigDecimal exchangeRate,
	BigDecimal amount,
	BigDecimal balanceAfterTransaction
) {
}
