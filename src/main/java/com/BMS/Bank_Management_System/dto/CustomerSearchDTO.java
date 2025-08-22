package com.BMS.Bank_Management_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSearchDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private int accountCount;
    private String status; // ACTIVE, LOCKED, etc.
}
