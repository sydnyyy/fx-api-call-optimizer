package com.exchangerateapi.exchangerate.service;

import com.exchangerateapi.global.enums.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

	private final ExchangeRateCache exchangeRateCache;

	public BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
		if (baseCurrency.equals(quoteCurrency)) {
			return BigDecimal.ONE;
		}

		return exchangeRateCache.get(baseCurrency, quoteCurrency);
	}
}
