package com.BMS.Bank_Management_System.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false)
    private String acctype;

    @Column (nullable = false)
    private BigDecimal amount;

    @Column (nullable = false)
    private LocalDateTime timeStamp;

    @Column (nullable = false)
    private String referenceID;

    private String description;

    @Column (nullable = false)
    private String status; //status of the transaction: Success/Failer

    @Column (nullable = false)

    @ManyToOne
    @JoinColumn (name = "from_account_id")
    @JsonBackReference ("sentTransactions")
    private Account fromAccount;

    @ManyToOne
    @JoinColumn (name = "to_account_id")
    @JsonBackReference ("receivedTransactions")
    private Account toAccount;

    @ManyToOne
    @JoinColumn ("performed_by_user_id")
    private User performedBy;

    @ManyToOne
    @JoinColumn (name = "account_id", nullable = false)
    @JsonBackReference ("accountTransictions")
    private Account account;






}
