package com.exchangerateapi.account.api;

import com.exchangerateapi.account.service.AccountService;
import com.exchangerateapi.account.dto.AccountDto;
import com.exchangerateapi.account.dto.AccountRegisterRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account Controller", description = "계좌 관련 기능 제공")
@RestController
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 생성")
    @PostMapping("/accounts")
    public ResponseEntity<?> registerAccount(@RequestBody @Valid AccountRegisterRequestDto accountRegisterRequestDto) {
        AccountDto response = accountService.register(accountRegisterRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "계좌 조회")
    @GetMapping("/accounts/{id}")
    public ResponseEntity<?> findAccountById(@PathVariable Long id) {
        AccountDto response = accountService.findAccountById(id);
        return ResponseEntity.ok(response);
    }

}
