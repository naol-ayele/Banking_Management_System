package com.BMS.Bank_Management_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCustomerRequest {
    private String email;
    private String phone;
    private String motherName;
    private String password; // Optional: for password reset
}
