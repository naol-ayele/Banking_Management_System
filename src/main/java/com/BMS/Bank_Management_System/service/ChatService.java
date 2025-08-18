package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.dto.*;
import com.BMS.Bank_Management_System.entity.*;
import com.BMS.Bank_Management_System.exception.ResourceNotFoundException;
import com.BMS.Bank_Management_System.exception.UnauthorizedActionException;
import com.BMS.Bank_Management_System.repository.ChatParticipantRepository;
import com.BMS.Bank_Management_System.repository.ChatRoomRepository;
import com.BMS.Bank_Management_System.repository.MessageRepository;
import com.BMS.Bank_Management_System.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public ChatRoomDTO createChatRoom(CreateChatRoomRequest request, User creator) {
        if (creator.getRole() != Role.STAFF && creator.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Only STAFF and ADMIN users can create chat rooms");
        }

        if (request.getType() == ChatRoom.ChatRoomType.ADMIN_ONLY && creator.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Only ADMIN users can create ADMIN_ONLY chat rooms");
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .createdBy(creator)
                .active(true)
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);

        // Add creator as first participant
        addCreatorAsParticipant(chatRoom, creator);

        // Filter out creator from participant list to avoid duplicates
        List<Long> otherParticipantIds = request.getParticipantUserIds().stream()
                .filter(userId -> !userId.equals(creator.getId()))
                .collect(Collectors.toList());

        // Add other participants
        addParticipantsToChatRoom(chatRoom, otherParticipantIds, creator);

        log.info("Chat room '{}' created by user {}", chatRoom.getName(), creator.getUsername());
        return convertToChatRoomDTO(chatRoom, creator.getId());
    }

    public MessageDTO sendMessage(SendMessageRequest request, User sender) {
        if (sender.getRole() != Role.STAFF && sender.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Only STAFF and ADMIN users can send messages");
        }

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));

        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(
                        request.getChatRoomId(), sender.getId())
                .orElseThrow(() -> new UnauthorizedActionException("You are not a participant in this chat room"));

        Message message = Message.builder()
                .content(request.getContent())
                .type(request.getType())
                .sender(sender)
                .chatRoom(chatRoom)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .sentAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .edited(false)
                .deleted(false)
                .build();

        message = messageRepository.save(message);
        chatRoom.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        MessageDTO messageDTO = convertToMessageDTO(message);
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoom.getId(), messageDTO);

        log.info("Message sent in chat room '{}' by user {}", chatRoom.getName(), sender.getUsername());
        return messageDTO;
    }

    public MessageDTO sendFileMessage(SendMessageRequest request, MultipartFile file, User sender) {
        if (sender.getRole() != Role.STAFF && sender.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Only STAFF and ADMIN users can send messages");
        }

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Validate file size (50MB limit)
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("File size cannot exceed 50MB");
        }

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));

        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(
                        request.getChatRoomId(), sender.getId())
                .orElseThrow(() -> new UnauthorizedActionException("You are not a participant in this chat room"));

        // Store the file and get the URL
        String fileUrl = storeFile(file, sender.getId());

        Message message = Message.builder()
                .content(request.getContent())
                .type(request.getType())
                .sender(sender)
                .chatRoom(chatRoom)
                .fileUrl(fileUrl)
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .sentAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .edited(false)
                .deleted(false)
                .build();

        message = messageRepository.save(message);
        chatRoom.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        MessageDTO messageDTO = convertToMessageDTO(message);
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoom.getId(), messageDTO);

        log.info("File message sent in chat room '{}' by user {}", chatRoom.getName(), sender.getUsername());
        return messageDTO;
    }

    private String storeFile(MultipartFile file, Long userId) {
        // Simple file storage - in production, you'd want to use a proper file storage service
        try {
            String fileName = userId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            // For now, just return a placeholder URL
            // In production, save to cloud storage (AWS S3, Google Cloud Storage, etc.)
            return "/uploads/chat-files/" + fileName;
        } catch (Exception e) {
            log.error("Error storing file", e);
            throw new RuntimeException("Failed to store file");
        }
    }

    public List<ChatRoomDTO> getUserChatRooms(User user) {
        if (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Only STAFF and ADMIN users can access chat rooms");
        }

        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsByUserId(user.getId());
        return chatRooms.stream()
                .map(chatRoom -> convertToChatRoomDTO(chatRoom, user.getId()))
                .collect(Collectors.toList());
    }

    public Page<MessageDTO> getChatRoomMessages(Long chatRoomId, User user, Pageable pageable) {
        // Debug logging
        log.debug("Checking if user {} (ID: {}) is participant in chat room {}",
                user.getUsername(), user.getId(), chatRoomId);

        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(chatRoomId, user.getId())
                .orElseThrow(() -> {
                    log.error("User {} (ID: {}) is not a participant in chat room {}",
                            user.getUsername(), user.getId(), chatRoomId);
                    return new UnauthorizedActionException("You are not a participant in this chat room");
                });

        participant.setLastReadAt(LocalDateTime.now());
        chatParticipantRepository.save(participant);

        Page<Message> messages = messageRepository.findByChatRoomIdOrderBySentAtDesc(chatRoomId, pageable);
        return messages.map(this::convertToMessageDTO);
    }

    public Page<MessageDTO> searchMessagesInChatRoom(Long chatRoomId, String searchTerm, User user, Pageable pageable) {
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(chatRoomId, user.getId())
                .orElseThrow(() -> new UnauthorizedActionException("You are not a participant in this chat room"));

        Page<Message> messages = messageRepository.searchMessagesInChatRoom(chatRoomId, searchTerm, pageable);
        return messages.map(this::convertToMessageDTO);
    }

    public List<ChatRoomDTO.ChatParticipantDTO> getChatRoomParticipants(Long chatRoomId, User user) {
        // Check if user is a participant
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(chatRoomId, user.getId())
                .orElseThrow(() -> new UnauthorizedActionException("You are not a participant in this chat room"));

        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomId(chatRoomId);
        return participants.stream()
                .map(this::convertToParticipantDTO)
                .collect(Collectors.toList());
    }

    private void addCreatorAsParticipant(ChatRoom chatRoom, User creator) {
        ChatParticipant creatorParticipant = ChatParticipant.builder()
                .user(creator)
                .chatRoom(chatRoom)
                .joinedAt(LocalDateTime.now())
                .lastReadAt(LocalDateTime.now())
                .active(true)
                .isAdmin(creator.getRole() == Role.ADMIN)
                .build();

        chatParticipantRepository.save(creatorParticipant);
    }

    private void addParticipantsToChatRoom(ChatRoom chatRoom, List<Long> userIds, User creator) {
        for (Long userId : userIds) {
            // Skip if this is the creator (already added)
            if (userId.equals(creator.getId())) {
                continue;
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            if (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN) {
                throw new UnauthorizedActionException("Only STAFF and ADMIN users can be added to chat rooms");
            }

            // Check if participant already exists
            boolean alreadyExists = chatParticipantRepository.findByChatRoomIdAndUserId(chatRoom.getId(), userId).isPresent();
            if (alreadyExists) {
                log.warn("User {} is already a participant in chat room {}", user.getUsername(), chatRoom.getName());
                continue;
            }

            ChatParticipant participant = ChatParticipant.builder()
                    .user(user)
                    .chatRoom(chatRoom)
                    .joinedAt(LocalDateTime.now())
                    .lastReadAt(LocalDateTime.now())
                    .active(true)
                    .isAdmin(user.getRole() == Role.ADMIN)
                    .build();

            chatParticipantRepository.save(participant);
        }
    }

    private ChatRoomDTO convertToChatRoomDTO(ChatRoom chatRoom, Long currentUserId) {
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomId(chatRoom.getId());

        return ChatRoomDTO.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .type(chatRoom.getType())
                .description(chatRoom.getDescription())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .createdByUsername(chatRoom.getCreatedBy().getUsername())
                .active(chatRoom.isActive())
                .participantCount(participants.size())
                .unreadMessageCount(0)
                .participants(participants.stream().map(this::convertToParticipantDTO).collect(Collectors.toList()))
                .lastMessage(null)
                .build();
    }

    private ChatRoomDTO.ChatParticipantDTO convertToParticipantDTO(ChatParticipant participant) {
        return ChatRoomDTO.ChatParticipantDTO.builder()
                .userId(participant.getUser().getId())
                .username(participant.getUser().getUsername())
                .email(participant.getUser().getEmail())
                .role(participant.getUser().getRole().name())
                .joinedAt(participant.getJoinedAt())
                .lastReadAt(participant.getLastReadAt())
                .isAdmin(participant.isAdmin())
                .build();
    }

    private MessageDTO convertToMessageDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .type(message.getType())
                .senderUsername(message.getSender() != null ? message.getSender().getUsername() : "SYSTEM")
                .senderRole(message.getSender() != null ? message.getSender().getRole().name() : "SYSTEM")
                .chatRoomId(message.getChatRoom().getId())
                .chatRoomName(message.getChatRoom().getName())
                .sentAt(message.getSentAt())
                .updatedAt(message.getUpdatedAt())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .edited(message.isEdited())
                .deleted(message.isDeleted())
                .build();
    }
}

