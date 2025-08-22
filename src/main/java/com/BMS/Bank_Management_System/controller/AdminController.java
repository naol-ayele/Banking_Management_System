package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.UserDTO;
import com.BMS.Bank_Management_System.service.AdminService;
import com.BMS.Bank_Management_System.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import com.BMS.Bank_Management_System.entity.AuditLog;
import com.BMS.Bank_Management_System.dto.TransactionDTO;
import com.BMS.Bank_Management_System.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.BMS.Bank_Management_System.dto.AccountWithTransactionsDTO;
import com.BMS.Bank_Management_System.dto.ResetPasswordRequest;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId, Authentication auth) {
        adminService.deleteUser(userId);
        String actorUsername = auth.getName();
        adminService.saveAudit(actorUsername, "DELETE_USER", "Deleted user id=" + userId);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PutMapping("/accounts/{accountId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> freezeAccount(@PathVariable Long accountId, Authentication auth) {
        accountService.freezeAccount(accountId);
        String actorUsername = auth.getName();
        adminService.saveAudit(actorUsername, "FREEZE_ACCOUNT", "Account id=" + accountId);
        return ResponseEntity.ok("Account frozen");
    }

    @PutMapping("/accounts/{accountId}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unfreezeAccount(@PathVariable Long accountId, Authentication auth) {
        accountService.unfreezeAccount(accountId);
        String actorUsername = auth.getName();
        adminService.saveAudit(actorUsername, "UNFREEZE_ACCOUNT", "Account id=" + accountId);
        return ResponseEntity.ok("Account unfrozen");
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<List<AccountWithTransactionsDTO>> getAllAccounts() {
        return ResponseEntity.ok(adminService.getAllAccountsWithTransactions());
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAllAuditLogs());
    }

    @PostMapping("/users/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetUserPassword(@PathVariable Long userId, @RequestBody(required = false) ResetPasswordRequest body, Authentication auth) {
        String newPass = adminService.resetUserPassword(userId, body != null ? body.getNewPassword() : null);
        adminService.saveAudit(auth.getName(), "RESET_PASSWORD", "Reset password for user id=" + userId);
        return ResponseEntity.ok(newPass);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<TransactionDTO>> getAllTransactionsAsAdmin() {
        return ResponseEntity.ok(transactionService.getAdminViewTransactions());
    }
}

