package com.BMS.Bank_Management_System.dto;

import com.BMS.Bank_Management_System.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    private Long chatRoomId;
    private String content;
    private Message.MessageType type;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
