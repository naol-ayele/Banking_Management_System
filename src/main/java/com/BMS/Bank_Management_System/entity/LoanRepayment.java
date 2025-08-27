package com.BMS.Bank_Management_System.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "loan_repayments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(nullable = false)
    private BigDecimal amount; // amount actually paid

    @Column(nullable = false)
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepaymentStatus status;

    private String referenceId;
    private String note; // e.g., "auto penalty applied"

    @Column(nullable = true)
    private BigDecimal penaltyAmount; // accumulate daily penalties

    // -------------------------
    // Helper Methods
    // -------------------------
    public BigDecimal getAmountPaid() {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    public BigDecimal getAmountDue() {
        if (loan == null || loan.getTotalDue() == null) return BigDecimal.ZERO;
        return loan.getTotalDue();
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount != null ? penaltyAmount : BigDecimal.ZERO;
    }

    public void setPenaltyAmount(BigDecimal value) {
        this.penaltyAmount = value != null ? value : BigDecimal.ZERO;
    }
    public void setAmountPaid(BigDecimal value) {
        this.amount = value != null ? value : BigDecimal.ZERO;
    }


    public LocalDateTime getDueDate() {
        if (loan == null || loan.getDueDate() == null) return null;
        return loan.getDueDate().atStartOfDay();
    }

    public Account getPaidBy() {
        return account;
    }

    public boolean isLate() {
        LocalDateTime due = getDueDate();
        return due != null && LocalDateTime.now().isAfter(due);
    }

    /**
     * Calculate current penalty based on loan daily penalty rate
     */
    public BigDecimal calculateCurrentPenalty() {
        if (loan == null || loan.getPenaltyPercentPerDay() == null) return BigDecimal.ZERO;

        LocalDateTime due = getDueDate();
        if (due == null) return BigDecimal.ZERO;

        long daysLate = ChronoUnit.DAYS.between(due, LocalDateTime.now());
        if (daysLate <= 0) return BigDecimal.ZERO;

        BigDecimal principalDue = getAmountDue().subtract(getAmountPaid());
        if (principalDue.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        return principalDue
                .multiply(loan.getPenaltyPercentPerDay())
                .multiply(BigDecimal.valueOf(daysLate));
    }
}

