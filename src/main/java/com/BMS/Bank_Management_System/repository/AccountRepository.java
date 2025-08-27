package com.BMS.Bank_Management_System.repository;

import com.BMS.Bank_Management_System.entity.Account;
import com.BMS.Bank_Management_System.entity.AccountStatus; // import enum
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser_Username(String username);

    List<Account> findByStatus(AccountStatus status);
}
