package com.exchangerateapi.exchangerate.api;

import com.exchangerateapi.exchangerate.service.ExchangeRateService;
import com.exchangerateapi.global.enums.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange-rate")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/v1")
    public ResponseEntity<?> getExchangeRate(
            @RequestParam("base") Currency baseCurrency,
            @RequestParam("quote") Currency quoteCurrency) {

        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(baseCurrency, quoteCurrency);
        return ResponseEntity.ok(Map.of(
                baseCurrency + "/" + quoteCurrency, exchangeRate));
    }
}
