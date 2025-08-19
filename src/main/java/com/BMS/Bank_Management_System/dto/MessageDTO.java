package com.BMS.Bank_Management_System.dto;

import com.BMS.Bank_Management_System.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {

    private Long id;
    private String content;
    private Message.MessageType type;
    private String senderUsername;
    private String senderRole;
    private Long chatRoomId;
    private String chatRoomName;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private boolean edited;
    private boolean deleted;
}

