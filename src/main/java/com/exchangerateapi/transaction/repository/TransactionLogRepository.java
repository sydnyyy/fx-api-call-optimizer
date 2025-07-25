package com.exchangerateapi.transaction.repository;

import com.exchangerateapi.transaction.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
}
