package com.BMS.Banking_Management_System.controller;

import com.BMS.Banking_Management_System.dto.TransactionDTO;
import com.BMS.Banking_Management_System.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @GetMapping("/{accountId}/history")
    public ResponseEntity<List<TransactionDTO>> getTransactionHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountId));
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping("/{accountId}/statement")
    public ResponseEntity<InputStreamResource> generateStatement(@PathVariable Long accountId) {
        ByteArrayInputStream bis = transactionService.generateStatement(accountId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=statement.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @GetMapping("/{accountId}/history/filter")
    public ResponseEntity<List<TransactionDTO>> getFilteredHistory(
            @PathVariable Long accountId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountId, from, to, type));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @GetMapping("/{accountId}/summary")
    public ResponseEntity<com.BMS.Banking_Management_System.dto.TransactionSummary> getMonthlySummary(
            @PathVariable Long accountId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(transactionService.getMonthlySummary(accountId, year, month));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @GetMapping("/me/performed")
    public ResponseEntity<List<TransactionDTO>> getMyPerformed() {
        return ResponseEntity.ok(transactionService.getMyPerformedTransactions());
    }

    // Admin transactions endpoint moved to AdminController at /api/admin/transactions

    // New endpoint for single transaction receipt PDF
    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    @GetMapping("/receipt/{transactionId}")
    public ResponseEntity<InputStreamResource> getTransactionReceipt(@PathVariable Long transactionId) {
        ByteArrayInputStream bis = transactionService.generateSingleTransactionReceipt(transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=transaction_receipt.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
}
