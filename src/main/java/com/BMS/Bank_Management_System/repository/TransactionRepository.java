package com.BMS.Bank_Management_System.repository;

import com.BMS.Banking_Management_System.entity.Role;
import com.BMS.Banking_Management_System.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List <Transaction> findByAccount_Id(Long accountId);
    List <Transaction> findByPerformedBy_Username(String username);
    List <Transaction> findByPerformedBy_Role(Role role);
    List <Transaction> findByAccount_AccountNumber(String accountNumber);

}
