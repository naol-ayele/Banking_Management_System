package com.BMS.Bank_Management_System.service;


import com.BMS.Bank_Management_System.dto.NotificationDto;
import com.BMS.Bank_Management_System.entity.Notification;
import com.BMS.Bank_Management_System.repository.NotificationRepository;
import com.BMS.Bank_Management_System.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void notifyUser(Long userId, String type, String message) {
        userRepository.findById(userId).ifPresent(user -> {
            Notification n = Notification.builder()
                    .user(user)
                    .type(type)
                    .message(message)
                    .channel("IN_APP")
                    .status("SENT")
                    .createdAt(LocalDateTime.now())
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(n);
        });
    }

    // Use userId instead of username
    public List<NotificationDto> getMyNotifications(Long userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setType(n.getType());
        dto.setMessage(n.getMessage());
        dto.setStatus(n.getStatus());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }

    public void markAsRead(Long id, Long userId) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setStatus("READ");
                n.setReadAt(LocalDateTime.now());
                notificationRepository.save(n);
            }
        });
    }
}

