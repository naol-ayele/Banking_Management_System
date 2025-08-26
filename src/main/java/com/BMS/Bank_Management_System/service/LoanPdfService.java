package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.dto.TransactionDTO;
import com.BMS.Bank_Management_System.entity.LoanRepayment;
import com.BMS.Bank_Management_System.repository.LoanRepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanPdfService {

    private final LoanRepaymentRepository repaymentRepo;

    public List<TransactionDTO> buildLoanStatementRows(Long loanId) {
        List<LoanRepayment> rows = repaymentRepo.findByLoan_IdOrderByLoan_DueDateAsc(loanId);

        return rows.stream().map(r -> {
            BigDecimal amount = (r.getAmountPaid() != null ? r.getAmountPaid() : BigDecimal.ZERO)
                    .add(r.getPenaltyAmount() != null ? r.getPenaltyAmount() : BigDecimal.ZERO);

            TransactionDTO dto = new TransactionDTO();
            // Use the due date as the "date" on statement; set time to NOON if needed
            LocalDateTime dueDateTime = r.getDueDate() != null
                    ? r.getDueDate().with(LocalTime.NOON)
                    : null;
            dto.setTimestamp(dueDateTime);
            dto.setDescription("Loan repayment (due " + r.getDueDate() + ") - Status: " + r.getStatus());
            dto.setAmount(amount);
            dto.setPerformedByUsername(
                    r.getPaidBy() != null ? r.getPaidBy().getUsername() : "SYSTEM");
            return dto;
        }).toList();
    }

    public TransactionDTO buildSingleRepaymentRow(LoanRepayment r) {
        TransactionDTO dto = new TransactionDTO();
        dto.setTimestamp(
                r.getPaidAt() != null
                        ? r.getPaidAt()
                        : r.getDueDate() != null ? r.getDueDate().with(LocalTime.NOON)
                        : LocalDateTime.now());
        dto.setDescription("Repayment #" + r.getId() + " for loan #" + r.getLoan().getId());
        BigDecimal total = (r.getAmountPaid() != null ? r.getAmountPaid() : BigDecimal.ZERO)
                .add(r.getPenaltyAmount() != null ? r.getPenaltyAmount() : BigDecimal.ZERO);
        dto.setAmount(total);
        dto.setPerformedByUsername(
                r.getPaidBy() != null ? r.getPaidBy().getUsername() : "SYSTEM");
        return dto;
    }
}

