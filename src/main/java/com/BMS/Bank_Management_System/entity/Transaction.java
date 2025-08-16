package com.BMS.Bank_Management_System.entity;


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






}
