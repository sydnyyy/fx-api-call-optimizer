package com.exchangerateapi.exchangerate.service;

import com.exchangerateapi.global.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceMockTest {

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    @Mock
    private ExchangeRateCache exchangeRateCache;

    @Spy
    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("동시 환율 조회 요청 시 Leader 스레드만 Future를 생성하고 여러 Follower는 join 대기")
    void concurrentFetch_ShouldExecuteLeaderFollowerPattern() throws InterruptedException {
        Currency base = Currency.USD;
        Currency quote = Currency.KRW;
        int count = 5;

        given(exchangeRateCache.getIfValid(any()))
                .willReturn(Optional.empty());

        given(exchangeRateProvider.fetchExchangeRate(base, quote))
                .willReturn(CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return BigDecimal.valueOf(1500);
                }));

        ExecutorService executorService = Executors.newFixedThreadPool(count);
        CountDownLatch readyLatch = new CountDownLatch(count);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            executorService.submit(() -> {
               readyLatch.countDown();
               try {
                   startLatch.await();
                   exchangeRateService.getExchangeRate(base, quote);
               } catch (Exception e) {
                   e.printStackTrace();
               } finally {
                   endLatch.countDown();
               }
            });
        }

        readyLatch.await();
        startLatch.countDown();

        boolean completed = endLatch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        verify(exchangeRateService, times(count)).handleCacheMiss(base, quote);
        verify(exchangeRateService, times(1)).createExchangeRateFuture(base, quote);
        verify(exchangeRateProvider, times(1)).fetchExchangeRate(base, quote);
        verify(exchangeRateService, times(count)).joinWithTimeout(any());

        executorService.shutdown();
    }
}