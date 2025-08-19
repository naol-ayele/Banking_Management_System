package com.BMS.Banking_Management_System.repository;

import com.BMS.Banking_Management_System.entity.ScheduledTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {
    List<ScheduledTransfer> findByActiveTrueAndNextRunAtBefore(LocalDateTime time);
}

