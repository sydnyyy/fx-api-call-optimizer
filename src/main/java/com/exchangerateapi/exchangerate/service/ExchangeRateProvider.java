package com.exchangerateapi.exchangerate.service;

import com.exchangerateapi.exchangerate.service.google.ExchangeRateGoogleFinanceScraper;
import com.exchangerateapi.exchangerate.service.manana.ExchangeRateMananaService;
import com.exchangerateapi.exchangerate.service.naver.ExchangeRateNaverService;
import com.exchangerateapi.global.enums.Currency;
import com.exchangerateapi.global.exception.CustomException;
import com.exchangerateapi.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateProvider {

    private final ExchangeRateNaverService naverService;
    private final ExchangeRateMananaService mananaService;
    private final ExchangeRateGoogleFinanceScraper googleFinanceScraper;

    public CompletableFuture<BigDecimal> fetchExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigDecimal exchangeRate = naverService.getExchangeRate(baseCurrency, quoteCurrency);
                log.info("[환율 1차 조회 성공] currencyCode={}/{}, exchangeRate={}", baseCurrency, quoteCurrency, exchangeRate);
                return exchangeRate;
            } catch (Exception e) {
                log.warn("[환율 1차 조회 실패] Fallback 로직 시작. 원인={}", e.getMessage());
                return fetchFromFallbacks(baseCurrency, quoteCurrency).join();
            }
        });
    }

    private CompletableFuture<BigDecimal> fetchFromFallbacks(Currency baseCurrency, Currency quoteCurrency) {
        CompletableFuture<Object> mananaFuture = CompletableFuture.supplyAsync(() -> mananaService.getExchangeRate(baseCurrency, quoteCurrency));
        CompletableFuture<Object> googleFuture = CompletableFuture.supplyAsync(() -> googleFinanceScraper.getExchangeRate(baseCurrency, quoteCurrency));

        return CompletableFuture.anyOf(mananaFuture, googleFuture)
                .thenApply(result -> {
                            BigDecimal exchangeRate = (BigDecimal) result;
                            log.info("[환율 2차 조회 성공] currencyCode={}/{}, exchangeRate={}", baseCurrency, quoteCurrency, exchangeRate);
                            return exchangeRate;
                })
                .exceptionally(ex -> {
                    log.error("[환율 1차 조회 실패] {}/{} 모든 Fallback API 호출 실패", baseCurrency, quoteCurrency, ex);
                    throw new CompletionException(new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL));
                });
    }
}
