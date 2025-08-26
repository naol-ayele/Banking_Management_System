package com.BMS.Bank_Management_System.repository;

import com.BMS.Bank_Management_System.entity.Loan;
import com.BMS.Bank_Management_System.entity.LoanStatus;
import com.BMS.Bank_Management_System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUser_Username(String username);
    List<Loan> findByStatus(LoanStatus status);
    boolean existsByUserAndStatusIn(User user, List<LoanStatus> statuses);
}
