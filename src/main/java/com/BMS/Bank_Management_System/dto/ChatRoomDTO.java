package com.BMS.Bank_Management_System.dto;

import com.BMS.Bank_Management_System.entity.ChatRoom;
import com.BMS.Bank_Management_System.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDTO {

    private Long id;
    private String name;
    private ChatRoom.ChatRoomType type;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByUsername;
    private boolean active;
    private int participantCount;
    private int unreadMessageCount;
    private List<ChatParticipantDTO> participants;
    private MessageDTO lastMessage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatParticipantDTO {
        private Long userId;
        private String username;
        private String email;
        private String role;
        private LocalDateTime joinedAt;
        private LocalDateTime lastReadAt;
        private boolean isAdmin;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageDTO {
        private Long id;
        private String content;
        private Message.MessageType type;
        private String senderUsername;
        private LocalDateTime sentAt;
        private String fileUrl;
        private String fileName;
        private Long fileSize;
    }
}
