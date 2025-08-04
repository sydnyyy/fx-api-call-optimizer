package com.exchangerateapi.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    EXCHANGE_RATE_FETCH_FAIL(HttpStatus.SERVICE_UNAVAILABLE, "환율 데이터를 가져올 수 없습니다."),
    INVALID_EXCHANGE_RATE(HttpStatus.INTERNAL_SERVER_ERROR, "환율 값은 0보다 커야 합니다.")
    ;

    private final HttpStatus status;
    private final String message;
}
