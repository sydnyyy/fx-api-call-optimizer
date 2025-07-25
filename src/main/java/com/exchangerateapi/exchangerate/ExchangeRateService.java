package com.exchangerateapi.exchangerate;

import com.exchangerateapi.global.enums.Currency;
import com.exchangerateapi.exchangerate.google.ExchangeRateGoogleFinanceScraper;
import com.exchangerateapi.exchangerate.manana.ExchangeRateMananaService;
import com.exchangerateapi.exchangerate.naver.ExchangeRateNaverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

	private final ExchangeRateNaverService naverService;
	private final ExchangeRateMananaService mananaService;
	private final ExchangeRateGoogleFinanceScraper googleFinanceScraper;

	private final ConcurrentHashMap<Currency, ReentrantLock> currencyLocks = new ConcurrentHashMap<>();
	private final Map<Currency, ExchangeRateStatus> exchangeRateResults = new ConcurrentHashMap<>();
	private final long CACHE_EXPIRY_TIME = 2000;

	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		.withZone(ZoneId.systemDefault());

	public void updateExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
		if(isAvailableExchangeRate(baseCurrency, quoteCurrency)) {
			return;
		}

		ReentrantLock lock = currencyLocks.computeIfAbsent(baseCurrency, k -> new ReentrantLock());
//		try {
			if (lock.tryLock()) {  // TODO: tryLock(500, TimeUnit.MILLISECONDS) 설정
				try {
					// double checking
					if (isAvailableExchangeRate(baseCurrency, quoteCurrency)) {
						return;
					}

					CompletableFuture
						.supplyAsync(() -> naverService.getExchangeRate(baseCurrency, quoteCurrency))
						.orTimeout(CACHE_EXPIRY_TIME, TimeUnit.MILLISECONDS)
						.thenApply(result -> new BigDecimal(result.toString())
							.setScale(2, RoundingMode.CEILING))
						.thenApply(exchangeRate -> {
							updateExchangeRateStatus(baseCurrency, exchangeRate);

							log.info("[Main(Naver)] {} 환율 {} 업데이트, {}",
									baseCurrency,
								exchangeRate,
								formatter.format(Instant.ofEpochMilli(exchangeRateResults.get(baseCurrency).lastCachedTime)));
							return exchangeRate;
						})
						.exceptionally(ex -> {
							log.error("Naver API failed or timed out, calling Manana and Google... {}", ex.getMessage());
							fallbackUpdate(baseCurrency, quoteCurrency);
							return null;
						}).join();
				} finally {
					lock.unlock();
				}
			} else {
				log.info("다른 스레드에 의해 {} 단위가 업데이트 중입니다.", baseCurrency);
			}
//		} catch (InterruptedException e) {
//			log.error("{} ReentrantLock 획득 중 스레드 인터럽트 발생: {}", baseCurrency, e.getMessage());
//		}
	}

	private void fallbackUpdate(Currency baseCurrency, Currency quoteCurrency) {
		CompletableFuture
			.anyOf(
				CompletableFuture
					.supplyAsync(() -> mananaService.getExchangeRate(baseCurrency, quoteCurrency))
					.orTimeout(CACHE_EXPIRY_TIME, TimeUnit.MILLISECONDS),

				CompletableFuture
					.supplyAsync(() -> googleFinanceScraper.getExchangeRate(baseCurrency, quoteCurrency))
					.orTimeout(CACHE_EXPIRY_TIME, TimeUnit.MILLISECONDS))

			.thenApply(result -> new BigDecimal(result.toString())
				.setScale(2, RoundingMode.CEILING))
			.thenApply(exchangeRate -> {
				updateExchangeRateStatus(baseCurrency, exchangeRate);

				log.info("[Fallback] {} 환율 {} 업데이트, {}",
						baseCurrency,
					exchangeRateResults.get(baseCurrency).exchangeRate,
					formatter.format(Instant.ofEpochMilli(exchangeRateResults.get(baseCurrency).lastCachedTime)));

				return exchangeRate;
			})
			.exceptionally(ex -> {
				log.error(ex.getMessage());
				throw new RuntimeException("Unable to fetch exchange rate", ex);
			})
			.join();
	}

	private void updateExchangeRateStatus(Currency baseCurrency, BigDecimal exchangeRate) {
		ExchangeRateStatus status = exchangeRateResults.computeIfAbsent(baseCurrency, k -> new ExchangeRateStatus());
		status.exchangeRate = exchangeRate;
		status.lastCachedTime = System.currentTimeMillis();
	}

	public boolean isAvailableExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
		return exchangeRateResults.containsKey(baseCurrency)
			&& System.currentTimeMillis() - exchangeRateResults.get(baseCurrency).lastCachedTime < CACHE_EXPIRY_TIME;
	}

	public BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
		if (!isAvailableExchangeRate(baseCurrency, quoteCurrency)) {
			updateExchangeRate(baseCurrency, quoteCurrency);
		}

		return exchangeRateResults.get(baseCurrency).exchangeRate;
	}

	static class ExchangeRateStatus {
		BigDecimal exchangeRate;
		Long lastCachedTime;

		public ExchangeRateStatus() {}
	}
}
