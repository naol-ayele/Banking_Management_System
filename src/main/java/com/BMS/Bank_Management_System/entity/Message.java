package com.BMS.Bank_Management_System.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private String fileUrl;

    @Column
    private String fileName;

    @Column
    private Long fileSize;

    @Column(nullable = false)
    @Builder.Default
    private boolean edited = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MessageType {
        TEXT,
        FILE,
        IMAGE,
        SYSTEM_MESSAGE
    }
}
