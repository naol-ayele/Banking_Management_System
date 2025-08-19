package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.entity.AuditLog;
import com.BMS.Bank_Management_System.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public void log(String username, String action, String details) {
        auditLogRepository.save(AuditLog.builder()
                .username(username)
                .action(action)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build()
        );
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }
}
