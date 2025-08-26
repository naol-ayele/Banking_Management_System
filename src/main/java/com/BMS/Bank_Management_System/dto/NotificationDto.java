package com.BMS.Bank_Management_System.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDto {
    private Long id;
    private String type;
    private String message;
    private String status;
    private LocalDateTime createdAt;
}
