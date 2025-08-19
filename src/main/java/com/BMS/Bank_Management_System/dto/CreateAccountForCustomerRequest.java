package com.BMS.Bank_Management_System.dto;

 import lombok.AllArgsConstructor;
 import lombok.Data;
 import lombok.NoArgsConstructor;

 import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountForCustomerRequest {
    private String accountType;
    private BigDecimal openingBalance;
}


