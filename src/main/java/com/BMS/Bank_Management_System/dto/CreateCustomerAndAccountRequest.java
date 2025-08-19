package com.BMS.Bank_Management_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCustomerAndAccountRequest {
    // Using username as full name per requirement
    private String username;
    private String email;

    @JsonProperty("phone number")
    @JsonAlias({"phone", "phoneNumber"})
    private String phone;

    private String password; // optional; if null we can generate a temp

    @JsonProperty("mother-name")
    @JsonAlias({"motherName"})
    private String motherName;

    private String accountType; // SAVINGS or CURRENT
    private BigDecimal openingBalance; // optional
}