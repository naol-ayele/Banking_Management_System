package com.bms.Banking.Management.System.repository;

import com.bms.Banking.Management.System.entity.ScheduledTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {
    List<ScheduledTransfer> findByActiveTrueAndNextRunAtBefore(LocalDateTime time);
}

