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
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

	private static final long TRY_LOCK_TIMEOUT_MS = 1;
	private static final long FUTURE_TIMEOUT_MS = 10000;

	private final Map<String, ReentrantLock> currencyLocks = new ConcurrentHashMap<>();
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

	private BigDecimal handleCacheMiss(Currency baseCurrency, Currency quoteCurrency) {
		String currencyCode = getCurrencyCode(baseCurrency, quoteCurrency);
		ReentrantLock lock = currencyLocks.computeIfAbsent(currencyCode, k -> new ReentrantLock());

		CompletableFuture<BigDecimal> sharedFuture = currencyFutures.computeIfAbsent(currencyCode, k -> new CompletableFuture<>());

		try {
			if (lock.tryLock(TRY_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
				return executeFetchAndCache(baseCurrency, quoteCurrency, sharedFuture);
			} else {
				return awaitFetchCompletion(currencyCode, sharedFuture);
			}
		} catch (InterruptedException e) {
			log.error("🔴 {}/{} ReentrantLock 획득 중 스레드 인터럽트 발생", baseCurrency, quoteCurrency, e);
			Thread.currentThread().interrupt();
			sharedFuture.completeExceptionally(new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL));
			throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	private BigDecimal executeFetchAndCache(Currency baseCurrency, Currency quoteCurrency, CompletableFuture<BigDecimal> sharedFuture) {
		String currencyCode = getCurrencyCode(baseCurrency, quoteCurrency);
		try {
			return exchangeRateProvider.fetchExchangeRate(baseCurrency, quoteCurrency)
					.whenComplete((result, throwable) -> {
						if (throwable != null) {
							log.error("[Leader 환율 조회 실패] currencyCode={}", currencyCode, throwable.getCause());
							sharedFuture.completeExceptionally(throwable.getCause());
						} else {
							exchangeRateCache.put(currencyCode, result);
							sharedFuture.complete(result);
							log.info("[Leader 환율 조회 성공] currencyCode={}, exchangeRate={} 환율 캐시 업데이트 완료", currencyCode, result);
						}
					}).join();
		} catch (CompletionException e) {
			throw e.getCause() instanceof CustomException
					? (CustomException) e.getCause() : new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
		} finally {
			currencyLocks.remove(currencyCode);
			currencyFutures.remove(currencyCode);
		}
	}

	private BigDecimal awaitFetchCompletion(String currencyCode, CompletableFuture<BigDecimal> sharedFuture) {
		try {
			return sharedFuture.orTimeout(FUTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS).join();
		} catch (Exception e) {
			log.error("[Follower 환율 조회 실패] currencyCode={} 환율 조회 대기 중 오류 발생", currencyCode, e);
			throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
		}
	}

	public static String getCurrencyCode(Currency baseCurrency, Currency quoteCurrency) {
		return baseCurrency + "/" + quoteCurrency;
	}
}
