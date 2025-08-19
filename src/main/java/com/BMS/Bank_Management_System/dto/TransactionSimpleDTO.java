
package com.BMS.Banking_Management_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSimpleDTO {
    private Long id;
    private String type;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String description;
    private String referenceId;
    private String status;
    private String performedBy;
}


