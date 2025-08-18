package com.BMS.Bank_Management_System.controller;





import com.BMS.Bank_Management_System.entity.Notification;
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

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @GetMapping
    public ResponseEntity<List<Notification>> myNotifications(Authentication auth) {
        return ResponseEntity.ok(notificationService.getMyNotifications(auth.getName()));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @PostMapping("/{id}/read")
    public ResponseEntity<String> markRead(@PathVariable Long id, Authentication auth) {
        notificationService.markAsRead(id, auth.getName());
        return ResponseEntity.ok("OK");
    }
}


