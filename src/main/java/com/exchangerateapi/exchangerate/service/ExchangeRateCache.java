package com.exchangerateapi.exchangerate.service;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ExchangeRateCache {

    private static final long CACHE_EXPIRY_TIME = 15000;

    private final Map<String, ExchangeRateData> exchangeRateCache = new ConcurrentHashMap<>();

    public Optional<BigDecimal> getIfValid(String currencyCode) {
        ExchangeRateData exchangeRateData = exchangeRateCache.get(currencyCode);
        if (exchangeRateData != null
                && System.currentTimeMillis() - exchangeRateData.lastCachedTime < CACHE_EXPIRY_TIME) {
            log.info("[캐시 조회 성공] currencyCode={}, exchangeRate={}", currencyCode, exchangeRateData.exchangeRate);
            return Optional.of(exchangeRateData.exchangeRate);
        }
        return Optional.empty();
    }

    public void put(String currencyCode, BigDecimal exchangeRate) {
        ExchangeRateData exchangeRateData = ExchangeRateData.builder()
                .exchangeRate(exchangeRate)
                .lastCachedTime(System.currentTimeMillis())
                .build();

        exchangeRateCache.put(currencyCode, exchangeRateData);
    }

    @Builder
    static class ExchangeRateData {
        BigDecimal exchangeRate;
        Long lastCachedTime;
    }
}
