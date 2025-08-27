package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.config.LoanProperties;
import com.BMS.Bank_Management_System.entity.Loan;
import com.BMS.Bank_Management_System.entity.LoanRepayment;
import com.BMS.Bank_Management_System.repository.LoanRepaymentRepository;
import com.BMS.Bank_Management_System.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanPenaltyService {

    private final LoanRepaymentRepository repaymentRepo;
    private final LoanRepository loanRepo;
    private final LoanProperties loanProps;
    private final NotificationService notificationService;

    // Runs daily; configurable via properties
    @Scheduled(cron = "#{@loanProperties.getPenaltyCron()}")
    public void applyDailyPenalties() {
        LocalDate today = LocalDate.now();
        log.info("Running daily loan penalty job for date {}", today);

        List<LoanRepayment> overdue = repaymentRepo.findOverdueForPenalty(today);

        BigDecimal perDay = loanProps.getPenaltyPercentPerDay(); // e.g. 0.05 means 5% per day
        if (perDay.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Penalty percent per day is <= 0; skipping penalties.");
            return;
        }

        for (LoanRepayment r : overdue) {
            try {
                // Guard: ensure still overdue (amountPaid < amountDue)
                BigDecimal due = r.getAmountDue();
                BigDecimal paid = r.getAmountPaid() == null ? BigDecimal.ZERO : r.getAmountPaid();
                if (paid.compareTo(due) >= 0) continue;

                // Penalty for 1 day (job runs daily). If you want cumulative, store & add daily.
                BigDecimal base = due.subtract(paid); // outstanding on this installment
                BigDecimal penalty = base
                        .multiply(perDay)
                        .setScale(2, RoundingMode.HALF_UP);

                // Accumulate penalty on the repayment
                BigDecimal existing = r.getPenaltyAmount() == null ? BigDecimal.ZERO : r.getPenaltyAmount();
                r.setPenaltyAmount(existing.add(penalty));
                repaymentRepo.save(r);

                // Optionally push up to the loan outstanding balance (if you track it)
                Loan loan = r.getLoan();
                if (loan.getOutstandingBalance() != null) {
                    loan.setOutstandingBalance(loan.getOutstandingBalance().add(penalty));
                    loanRepo.save(loan);
                }

                // Notify customer
                Long userId = loan.getUser().getId(); // assumes Loan has User user;
                notificationService.notifyUser(
                        userId,
                        "PENALTY_APPLIED",
                        "Daily penalty " + penalty + " applied to loan #" + loan.getId()
                                + " for overdue installment due " + r.getDueDate()
                );
            } catch (Exception ex) {
                log.error("Failed applying penalty for repayment id={} (loan id={})",
                        r.getId(), r.getLoan() != null ? r.getLoan().getId() : null, ex);
            }
        }

        log.info("Daily loan penalty job completed. Overdue processed: {}", overdue.size());
    }
}
