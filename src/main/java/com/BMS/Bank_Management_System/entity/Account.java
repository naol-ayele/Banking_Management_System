package com.BMS.Bank_Management_System.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @OneToMany(mappedBy = "fromAccount")
    @JsonManagedReference("sentTransactions")
    private List<Transaction> sentTransactions;

    @OneToMany(mappedBy = "toAccount")
    @JsonManagedReference("receivedTransactions")
    private List<Transaction> receivedTransactions;

    @OneToMany(mappedBy = "account")
    @JsonManagedReference("accountTransactions")
    private List<Transaction> transactions;

    public String getUsername() {
        return null;
    }
}
