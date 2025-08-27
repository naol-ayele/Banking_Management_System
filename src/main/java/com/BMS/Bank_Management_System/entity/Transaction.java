package com.BMS.Bank_Management_System.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; // DEPOSIT, WITHDRAW, TRANSFER

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String description;

    @Column(nullable = false)
    private String referenceId;

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED

    @ManyToOne
    @JoinColumn(name = "from_account_id")
    @JsonBackReference("sentTransactions")
    private Account fromAccount;

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    @JsonBackReference("receivedTransactions")
    private Account toAccount;

    @ManyToOne
    @JoinColumn(name = "performed_by_user_id")
    private User performedBy;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    @JsonBackReference("accountTransactions")
    private Account account;

    private String token;
    private LocalDateTime expiryTime;
}

