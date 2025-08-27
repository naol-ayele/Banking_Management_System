package com.BMS.Bank_Management_System.dto.loan;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepaymentRequest {
    private Long fromAccountId;    // if null, use loan.accountId
    private BigDecimal amount;
    private String note;
}
