package com.exchangerateapi.exchangerate.service.manana;

import com.exchangerateapi.global.enums.Currency;
import com.exchangerateapi.exchangerate.service.manana.dto.ExchangeRateMananaResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeRateMananaService {

	private final ExchangeRateMananaClient exchangeRateMananaClient;

	public BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
		List<ExchangeRateMananaResponseDto> result = exchangeRateMananaClient.getExchangeRate(quoteCurrency, baseCurrency);
		BigDecimal rate = result.getFirst().rate();

		if (rate.compareTo(BigDecimal.ONE) >= 0) {
			return rate.setScale(3, RoundingMode.CEILING);
		}
		return rate.setScale(7, RoundingMode.CEILING);
	}
}
