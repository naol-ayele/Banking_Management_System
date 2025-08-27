package com.BMS.Bank_Management_System.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column
    private Integer creditScore = 650; //used in the financial analysis for the ai part

    private String motherName;

    private String nationalIdImageUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<Account> accounts = new java.util.ArrayList<>();

    // Security controls
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Builder.Default
    private Instant lockedUntil = null;
}



