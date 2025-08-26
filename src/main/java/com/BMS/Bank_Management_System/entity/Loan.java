package com.BMS.Bank_Management_System.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(nullable = false)
    private BigDecimal principal;

    @Column(nullable = false)
    private BigDecimal interestRate; // e.g., 0.30 = 30%

    @Column(nullable = false)
    private int termDays;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false)
    private BigDecimal totalDue; // principal + interest

    @Column(nullable = false)
    private BigDecimal outstanding; // remaining, includes penalties

    @Column(nullable = false)
    private BigDecimal penaltyPercentPerDay; // snapshot from config at creation

    private String remark;

    // -------------------------
    // Helper methods for service
    // -------------------------
    public BigDecimal getOutstandingBalance() {
        return outstanding != null ? outstanding : BigDecimal.ZERO;
    }

    public void setOutstandingBalance(BigDecimal value) {
        this.outstanding = value != null ? value : BigDecimal.ZERO;
    }
}

