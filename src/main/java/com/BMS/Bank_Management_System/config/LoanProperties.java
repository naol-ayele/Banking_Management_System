package com.BMS.Bank_Management_System.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "lms")
@Getter @Setter
public class LoanProperties {

    private BigDecimal baseInterestRate = new BigDecimal("0.30");
    private BigDecimal penaltyPercentPerDay = new BigDecimal("0.05");
    private int minTransactions = 5;
    private int lookbackMonths = 3;
    private String penaltyCron = "0 0 0 * * *";

    // New fields for eligibility brackets
    private BigDecimal lowBracketMax = new BigDecimal("1000");
    private BigDecimal mediumBracketMax = new BigDecimal("10000");
    private BigDecimal lowMaxLoan = new BigDecimal("500");
    private BigDecimal mediumMaxLoan = new BigDecimal("5000");
    private BigDecimal highMaxLoan = new BigDecimal("50000");
}
