package com.BMS.Bank_Management_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountWithTransactionsDTO {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private Long userId;
    private String accountType;
    private String status;
    private List<TransactionSimpleDTO> sentTransactions;
}


