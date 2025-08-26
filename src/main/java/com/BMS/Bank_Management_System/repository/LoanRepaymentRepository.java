package com.BMS.Bank_Management_System.repository;

import com.BMS.Bank_Management_System.entity.LoanRepayment;
import com.BMS.Bank_Management_System.entity.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {

    // Only the ones where amount paid < loan total due
    @Query("SELECT lr FROM LoanRepayment lr " +
            "WHERE lr.loan.dueDate < :date AND lr.status = :status")
    List<LoanRepayment> findOverdueForPenalty(@Param("date") LocalDate date);


    // For statements/receipts
    List<LoanRepayment> findByLoan_IdOrderByLoan_DueDateAsc(Long loanId);
}
