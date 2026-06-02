package com.exchangerateapi.exchangerate.service;

import com.exchangerateapi.global.enums.Currency;
import com.exchangerateapi.global.exception.CustomException;
import com.exchangerateapi.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

	private static final long FUTURE_TIMEOUT_MS = 10000;

	private final Map<String, CompletableFuture<BigDecimal>> currencyFutures = new ConcurrentHashMap<>();

	private final ExchangeRateProvider exchangeRateProvider;
	private final ExchangeRateCache exchangeRateCache;

	public BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
		if (baseCurrency.equals(quoteCurrency)) {
			return BigDecimal.ONE;
		}

		return exchangeRateCache.getIfValid(getCurrencyCode(baseCurrency, quoteCurrency))
				.orElseGet(() -> handleCacheMiss(baseCurrency, quoteCurrency));
	}

	public BigDecimal handleCacheMiss(Currency baseCurrency, Currency quoteCurrency) {
		String currencyCode = getCurrencyCode(baseCurrency, quoteCurrency);

		CompletableFuture<BigDecimal> future = currencyFutures.computeIfAbsent(
				currencyCode, k -> createExchangeRateFuture(baseCurrency, quoteCurrency)
		);

		return joinWithTimeout(future);
	}

	// 테스트를 위해 public 설정함
	 public CompletableFuture<BigDecimal> createExchangeRateFuture(Currency baseCurrency, Currency quoteCurrency) {
		String currencyCode = getCurrencyCode(baseCurrency, quoteCurrency);

		return exchangeRateProvider.fetchExchangeRate(baseCurrency, quoteCurrency)
				.toCompletableFuture()
				.whenComplete((r, ex) -> {
					try {
						if (ex != null) {
							log.error("[Leader 환율 조회 실패] currencyCode={}", currencyCode, ex.getCause());
						} else {
							exchangeRateCache.put(currencyCode, r);
							log.info("[Leader 환율 조회 성공] currencyCode={}, exchangeRate={} 환율 캐시 업데이트 완료", currencyCode, r);
						}
					} finally {
						currencyFutures.remove(currencyCode);
					}
				});
	}

	// 테스트를 위해 public 설정함
	public BigDecimal joinWithTimeout(CompletableFuture<BigDecimal> future) {
		try {
			return future.orTimeout(FUTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS).join();
		} catch (CompletionException e) {
			throw e.getCause() instanceof CustomException
					? (CustomException) e.getCause()
					: new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
		}
	}

	public static String getCurrencyCode(Currency baseCurrency, Currency quoteCurrency) {
		return baseCurrency + "/" + quoteCurrency;
	}
}
