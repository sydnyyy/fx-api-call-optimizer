package com.exchangerateapi.exchangerate.scheduler;

import com.exchangerateapi.exchangerate.service.ExchangeRateCache;
import com.exchangerateapi.exchangerate.service.ExchangeRateProvider;
import com.exchangerateapi.global.enums.Currency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateUpdateScheduler {

    private final ExchangeRateProvider exchangeRateProvider;
    private final ExchangeRateCache exchangeRateCache;

    @Scheduled(cron = "0 * * * * *")
    public void updateExchangeRate() {
        BigDecimal exchangeRate = exchangeRateProvider.fetchExchangeRate(
                Currency.USD, Currency.KRW
        );

        log.info("[캐시 스케줄러 업데이트] currencyCode={}/{}, exchangeRate={}", Currency.USD, Currency.KRW, exchangeRate);
        exchangeRateCache.put(Currency.USD, Currency.KRW, exchangeRate);
    }
}
