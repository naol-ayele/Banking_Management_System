package com.BMS.Bank_Management_System.repository;

import com.BMS.Bank_Management_System.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}