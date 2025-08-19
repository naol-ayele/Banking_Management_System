package com.BMS.Bank_Management_System.entity;


import jakarta.persistence.*;
        import lombok.*;

        import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String type; // e.g., ACCOUNT_APPROVED, ACCOUNT_FROZEN, TXN_SUCCESS

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String channel; // IN_APP (MVP)

    @Column(nullable = false)
    private String status; // PENDING, SENT, READ

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}


