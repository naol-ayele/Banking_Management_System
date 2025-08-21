package com.BMS.Bank_Management_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardlessWithdrawalResponse {
    private String token;
    private String referenceId;
    private BigDecimal amount;
    private LocalDateTime expiryTime;

}