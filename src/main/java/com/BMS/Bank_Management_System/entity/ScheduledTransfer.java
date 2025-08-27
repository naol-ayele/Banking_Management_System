package com.BMS.Bank_Management_System.entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_transfers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduledTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime nextRunAt;

    @Column(nullable = false)
    private boolean active;
}

