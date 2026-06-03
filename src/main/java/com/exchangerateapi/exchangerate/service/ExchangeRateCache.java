package com.exchangerateapi.exchangerate.service;

import com.exchangerateapi.global.enums.Currency;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateCache {

    private static final long CACHE_EXPIRY_TIME = 15000;
    private final Map<String, ExchangeRateData> exchangeRateCache = new ConcurrentHashMap<>();

    private final ExchangeRateProvider exchangeRateProvider;

    public BigDecimal get(Currency base, Currency quote) {
        String currencyCode = getCurrencyCode(base, quote);
        ExchangeRateData exchangeRateData = exchangeRateCache.get(currencyCode);

        if (exchangeRateData != null
                && System.currentTimeMillis() - exchangeRateData.lastCachedTime < CACHE_EXPIRY_TIME) {
            log.info("[캐시 조회 성공] currencyCode={}, exchangeRate={}", currencyCode, exchangeRateData.exchangeRate);
            return exchangeRateData.exchangeRate;
        }

        return forceUpdate(base, quote);
    }

    private BigDecimal forceUpdate(Currency base, Currency quote) {
        BigDecimal exchangeRate = put(base, quote, exchangeRateProvider.fetchExchangeRate(base, quote));
        log.info("[캐시 강제 업데이트] currencyCode={}/{}, exchangeRate={}", base, quote, exchangeRate);

        return exchangeRate;
    }

    public BigDecimal put(Currency base, Currency quote, BigDecimal exchangeRate) {
        String currencyCode = getCurrencyCode(base, quote);

        ExchangeRateData exchangeRateData = ExchangeRateData.builder()
                .exchangeRate(exchangeRate)
                .lastCachedTime(System.currentTimeMillis())
                .build();

        exchangeRateCache.put(currencyCode, exchangeRateData);
        return exchangeRate;
    }

    @Builder
    static class ExchangeRateData {
        BigDecimal exchangeRate;
        Long lastCachedTime;
    }

    private static String getCurrencyCode(Currency base, Currency quote) {
        return base + "/" + quote;
    }
}
