package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.*;
import com.BMS.Bank_Management_System.entity.User;
import com.BMS.Bank_Management_System.entity.Message;
import com.BMS.Bank_Management_System.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDTO> createChatRoom(
            @RequestBody CreateChatRoomRequest request,
            Authentication authentication) {
        User creator = (User) authentication.getPrincipal();
        ChatRoomDTO chatRoom = chatService.createChatRoom(request, creator);
        return ResponseEntity.ok(chatRoom);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> getUserChatRooms(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<ChatRoomDTO> chatRooms = chatService.getUserChatRooms(user);
        return ResponseEntity.ok(chatRooms);
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestBody SendMessageRequest request,
            Authentication authentication) {
        User sender = (User) authentication.getPrincipal();
        MessageDTO message = chatService.sendMessage(request, sender);
        return ResponseEntity.ok(message);
    }

    @PostMapping(value = "/messages/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageDTO> sendFileMessage(
            @RequestParam("chatRoomId") Long chatRoomId,
            @RequestParam("content") String content,
            @RequestParam("type") String type,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        User sender = (User) authentication.getPrincipal();

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("File size cannot exceed 50MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        SendMessageRequest request = SendMessageRequest.builder()
                .chatRoomId(chatRoomId)
                .content(content)
                .type(Message.MessageType.valueOf(type.toUpperCase()))
                .fileName(originalFilename)
                .fileSize(file.getSize())
                .build();

        MessageDTO message = chatService.sendFileMessage(request, file, sender);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<Page<MessageDTO>> getChatRoomMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageDTO> messages = chatService.getChatRoomMessages(chatRoomId, user, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/rooms/{chatRoomId}/messages/search")
    public ResponseEntity<Page<MessageDTO>> searchMessages(
            @PathVariable Long chatRoomId,
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageDTO> messages = chatService.searchMessagesInChatRoom(chatRoomId, searchTerm, user, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/rooms/{chatRoomId}/participants")
    public ResponseEntity<List<ChatRoomDTO.ChatParticipantDTO>> getChatRoomParticipants(
            @PathVariable Long chatRoomId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<ChatRoomDTO.ChatParticipantDTO> participants = chatService.getChatRoomParticipants(chatRoomId, user);
        return ResponseEntity.ok(participants);
    }
}

