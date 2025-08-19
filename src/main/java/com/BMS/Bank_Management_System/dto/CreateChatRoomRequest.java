package com.BMS.Bank_Management_System.dto;

import com.BMS.Bank_Management_System.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatRoomRequest {

    private String name;
    private ChatRoom.ChatRoomType type;
    private String description;
    private List<Long> participantUserIds;
}

