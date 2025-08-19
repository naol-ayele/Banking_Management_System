package com.BMS.Banking_Management_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class TransactionSummary {
    private int count;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;

    public static TransactionSummary from(List<TransactionDTO> txns) {
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal debits = BigDecimal.ZERO;
        for (TransactionDTO t : txns) {
            if (t.getType() != null && t.getAmount() != null) {
                String type = t.getType().toUpperCase();
                if (type.contains("DEPOSIT") || type.contains("CREDIT")) {
                    credits = credits.add(t.getAmount());
                } else if (type.contains("WITHDRAW") || type.contains("DEBIT")) {
                    debits = debits.add(t.getAmount());
                }
            }
        }
        return new TransactionSummary(txns.size(), credits, debits);

    }
}

