package com.BMS.Bank_Management_System.dto.loan;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanResponse {
    private Long id;
    private Long userId;
    private Long accountId;
    private BigDecimal principal;
    private BigDecimal interestRate;
    private int termDays;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String status;
    private BigDecimal totalDue;
    private BigDecimal outstanding;
    private BigDecimal penaltyPercentPerDay;
    private String remark;
}

