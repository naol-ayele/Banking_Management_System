package com.BMS.Bank_Management_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private Long userId;
    private String customerUsername;  // ✅ new
    private String customerEmail;     // ✅ new
    private String accountType; // SAVINGS, CURRENT
    private String status;      // PENDING_APPROVAL, ACTIVE, FROZEN, CLOSED
}
