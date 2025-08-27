package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.AccountDTO;
import com.BMS.Bank_Management_System.service.AccountService;
import com.BMS.Bank_Management_System.service.ScheduledTransferService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final ScheduledTransferService scheduledTransferService;

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<String> deposit(@PathVariable Long accountId, @RequestParam BigDecimal amount) {
        accountService.deposit(accountId, amount);
        return ResponseEntity.ok("Deposit successful");
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<String> withdraw(@PathVariable Long accountId, @RequestParam BigDecimal amount) {
        accountService.withdraw(accountId, amount);
        return ResponseEntity.ok("Withdrawal successful");
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestParam Long fromAccountId,
                                           @RequestParam Long toAccountId,
                                           @RequestParam BigDecimal amount) {
        accountService.transfer(fromAccountId, toAccountId, amount);
        return ResponseEntity.ok("Transfer successful");
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<java.util.List<AccountDTO>> getMyAccounts(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(accountService.getMyAccounts(username));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @PostMapping("/schedule-transfer")
    public ResponseEntity<?> scheduleTransfer(@RequestParam Long fromAccountId,
                                              @RequestParam Long toAccountId,
                                              @RequestParam BigDecimal amount,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime firstRunAt) {
        if (firstRunAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Invalid firstRunAt. Must be in the future.");
        }

        scheduledTransferService.createScheduledTransfer(fromAccountId, toAccountId, amount, firstRunAt);
        return ResponseEntity.ok("Transfer scheduled");
    }

}

