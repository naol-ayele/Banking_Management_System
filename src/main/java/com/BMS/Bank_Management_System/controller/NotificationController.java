package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.NotificationDto;
import com.BMS.Bank_Management_System.security.CustomUserDetails;
import com.BMS.Bank_Management_System.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN','LOAN_OFFICER')")
    @GetMapping
    public ResponseEntity<List<NotificationDto>> myNotifications(Authentication auth) {
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN','LOAN_OFFICER')")
    @PostMapping("/{id}/read")
    public ResponseEntity<String> markRead(@PathVariable Long id, Authentication auth) {
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok("OK");
    }
}
