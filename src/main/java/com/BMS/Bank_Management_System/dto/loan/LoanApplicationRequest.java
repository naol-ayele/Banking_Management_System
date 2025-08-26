package com.BMS.Bank_Management_System.dto.loan;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    private Long accountId;          // where to disburse & default repay
    private BigDecimal principal;    // e.g., 500
    private Integer termDays;        // e.g., 7
    private BigDecimal interestRate; // optional override; null => use default from config
    private String reason;           // optional
}
