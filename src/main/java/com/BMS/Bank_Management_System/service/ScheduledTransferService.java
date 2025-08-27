package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.entity.Account;
import com.BMS.Bank_Management_System.entity.ScheduledTransfer;
import com.BMS.Bank_Management_System.exception.ResourceNotFoundException;
import com.BMS.Bank_Management_System.repository.AccountRepository;
import com.BMS.Bank_Management_System.repository.ScheduledTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTransferService {

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public ScheduledTransfer createScheduledTransfer(Long fromAccountId,
                                                     Long toAccountId,
                                                     BigDecimal amount,
                                                     LocalDateTime firstRunAt) {
        if (firstRunAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("firstRunAt must be in the future.");
        }

        Account from = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + fromAccountId));
        Account to = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + toAccountId));

        ScheduledTransfer st = ScheduledTransfer.builder()
                .fromAccount(from)
                .toAccount(to)
                .amount(amount)
                .nextRunAt(firstRunAt)
                .active(true)
                .build();

        return scheduledTransferRepository.save(st);
    }


    @Scheduled(fixedDelay = 60000)
    public void processDueTransfers() {
        List<ScheduledTransfer> due = scheduledTransferRepository.findByActiveTrueAndNextRunAtBefore(LocalDateTime.now());
        for (ScheduledTransfer st : due) {
            try {
                accountService.transfer(st.getFromAccount().getId(), st.getToAccount().getId(), st.getAmount());
                // schedule next run one day later (simple example)
                st.setNextRunAt(st.getNextRunAt().plusDays(1));
            } catch (Exception e) {
                // deactivate on repeated failures (simplified)
                st.setActive(false);
            }
            scheduledTransferRepository.save(st);
        }
    }
}

