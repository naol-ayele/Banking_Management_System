package com.BMS.Bank_Management_System.dto;
import lombok.Data;

@Data
public class ProcessWithdrawalRequest {
    private String token;
    private String atmId;
}