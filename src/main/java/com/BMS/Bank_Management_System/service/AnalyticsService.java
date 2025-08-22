// AnalyticsService.java
package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.entity.Transaction;
import com.BMS.Bank_Management_System.entity.User;
import com.BMS.Bank_Management_System.repository.TransactionRepository;
import com.BMS.Bank_Management_System.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public Map<String, Object> getFinancialSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Transaction> transactions = transactionRepository.findByPerformedBy_Username(user.getUsername());

        if (transactions.isEmpty()) {
            return Map.of(
                    "user_id", userId,
                    "income_monthly", 0.0,
                    "spend_monthly", 0.0,
                    "savings_rate", 0.0,
                    "top_categories", Collections.emptyMap(),
                    "credit_score", 650
            );
        }

        double income = transactions.stream()
                .filter(t -> "DEPOSIT".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        double spend = transactions.stream()
                .filter(t -> "WITHDRAW".equalsIgnoreCase(t.getType()) || "TRANSFER".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        double savingsRate = income > 0 ? (income - spend) / income : 0.0;

        // Group spend by description (simple categorization)
        Map<String, Double> categories = transactions.stream()
                .filter(t -> "WITHDRAW".equalsIgnoreCase(t.getType()) || "TRANSFER".equalsIgnoreCase(t.getType()))
                .collect(Collectors.groupingBy(
                        t -> Optional.ofNullable(t.getDescription()).orElse("Other"),
                        Collectors.summingDouble(t -> t.getAmount().doubleValue())
                ));

        // Top 5 categories
        Map<String, Double> topCategories = categories.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return Map.of(
                "user_id", userId,
                "income_monthly", income,
                "spend_monthly", spend,
                "savings_rate", savingsRate,
                "top_categories", topCategories,
                "credit_score", calculateCreditScore(income, spend)
        );
    }

    public Map<String, Object> checkLoanEligibility(Long userId, Double desiredAmount) {
        Map<String, Object> summary = getFinancialSummary(userId);

        double income = (double) summary.get("income_monthly");
        double spend = (double) summary.get("spend_monthly");
        int creditScore = (int) summary.get("credit_score");

        double baseLoanLimit = income * 5;
        boolean eligible = creditScore >= 650 && income > spend;

        String reason = eligible ? "Eligible for loan" : "Not eligible due to low credit or high spend.";
        if (desiredAmount != null && desiredAmount > baseLoanLimit) {
            eligible = false;
            reason = "Requested amount exceeds loan limit.";
        }

        return Map.of(
                "user_id", userId,
                "eligible", eligible,
                "max_amount", baseLoanLimit,
                "credit_score", creditScore,
                "reason", reason
        );
    }

    // Simple credit score calculator
    private int calculateCreditScore(double income, double spend) {
        if (income <= 0) return 500;
        double ratio = spend / income;
        if (ratio < 0.5) return 750;
        if (ratio < 0.8) return 700;
        if (ratio < 1.0) return 650;
        return 600;
    }
}
