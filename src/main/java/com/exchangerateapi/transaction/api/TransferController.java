package com.exchangerateapi.transaction.api;

import com.exchangerateapi.transaction.dto.TransactionLogDto;
import com.exchangerateapi.transaction.dto.TransactionRequestDto;
import com.exchangerateapi.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transfer Controller", description = "이체 기능 제공")
@RestController
@RequiredArgsConstructor
@Validated
public class TransferController {

	private final TransactionService transactionService;

	@Operation(summary = "이체")
	@PostMapping("/transfer")
	public ResponseEntity<?> transfer(@RequestBody @Valid TransactionRequestDto transactionRequestDto) {
		TransactionLogDto response = transactionService.transfer(transactionRequestDto);
		return ResponseEntity.ok(response);
	}
}
