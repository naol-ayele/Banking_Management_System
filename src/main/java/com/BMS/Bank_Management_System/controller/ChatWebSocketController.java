package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.MessageDTO;
import com.BMS.Bank_Management_System.entity.User;
import com.BMS.Bank_Management_System.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public MessageDTO sendMessage(@Payload MessageDTO chatMessage) {
        log.info("Received message: {}", chatMessage.getContent());
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public MessageDTO addUser(@Payload MessageDTO chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSenderUsername());
        log.info("User {} joined the chat", chatMessage.getSenderUsername());
        return chatMessage;
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload String username, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        messagingTemplate.convertAndSend("/topic/typing", username + " is typing...");
    }

    @MessageMapping("/chat.read")
    public void handleRead(@Payload Long chatRoomId, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/read", "Message read");
    }
}


