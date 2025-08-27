package com.BMS.Bank_Management_System.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ChatParticipant> participants = new HashSet<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Message> messages = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ChatRoomType {
        GENERAL_STAFF,    // All staff can join
        ADMIN_ONLY,       // Only admin users
        DEPARTMENT,        // Department specific
        PRIVATE           // One-on-one chat
    }
}
