package com.BMS.Bank_Management_System.service;



import com.BMS.Bank_Management_System.entity.Notification;
import com.BMS.Bank_Management_System.entity.User;
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

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void notifyUser(Long userId, String type, String message) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
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
    }

    public List<Notification> getMyNotifications(String username) {
        return notificationRepository.findByUser_UsernameOrderByCreatedAtDesc(username);
    }

    public void markAsRead(Long id, String username) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser().getUsername().equals(username)) {
                n.setStatus("READ");
                n.setReadAt(LocalDateTime.now());
                notificationRepository.save(n);
            }
        });
    }
}


