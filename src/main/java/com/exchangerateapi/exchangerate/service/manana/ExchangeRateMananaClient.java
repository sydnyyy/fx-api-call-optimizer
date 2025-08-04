package com.exchangerateapi.exchangerate.service.manana;

import com.exchangerateapi.global.enums.Currency;
import com.exchangerateapi.exchangerate.service.manana.dto.ExchangeRateMananaResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
	name = "manana-exchange-rate-client",
	url = "https://api.manana.kr/exchange"
)
public interface ExchangeRateMananaClient {

	@GetMapping("/rate.json")
	List<ExchangeRateMananaResponseDto> getExchangeRate(
		@RequestParam("base") Currency base,
		@RequestParam("code") Currency code
	);
}
