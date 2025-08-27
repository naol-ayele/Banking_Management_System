package com.BMS.Bank_Management_System.repository;

import com.BMS.Bank_Management_System.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.BMS.Bank_Management_System.entity.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccount_Id(Long accountId);
    List<Transaction> findByPerformedBy_Username(String username);
    List<Transaction> findByPerformedBy_Role(Role role);
    Optional<Transaction> findByToken(String token);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.timestamp >= :startDate")
    BigDecimal sumTransactionsSince(@Param("accountId") Long accountId, @Param("startDate") LocalDateTime startDate);

}

