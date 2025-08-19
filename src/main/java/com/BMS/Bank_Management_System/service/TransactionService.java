package com.BMS.Banking_Management_System.service;

import com.BMS.Banking_Management_System.dto.TransactionDTO;
import com.BMS.Banking_Management_System.dto.TransactionSummary;
import com.BMS.Banking_Management_System.entity.Account;
import com.BMS.Banking_Management_System.entity.Transaction;
import com.BMS.Banking_Management_System.exception.ResourceNotFoundException;
import com.BMS.Banking_Management_System.repository.AccountRepository;
import com.BMS.Banking_Management_System.repository.TransactionRepository;
import com.BMS.Banking_Management_System.util.PdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PdfGenerator pdfGenerator;
    private final com.BMS.Banking_Management_System.mapper.TransactionMapper transactionMapper;

    private boolean isAdminOrStaff(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_STAFF"));
    }

    private void verifyAccess(Long accountId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!isAdminOrStaff(auth)) {
            String username = auth.getName();
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

            if (!account.getUser().getUsername().equals(username)) {
                throw new SecurityException("Not authorized to access this account.");
            }
        }
    }







    public List<TransactionDTO> getTransactionHistory(Long accountId) {
        verifyAccess(accountId);
        List<Transaction> transactions = transactionRepository.findByAccount_Id(accountId);
        return transactions.stream().map(transactionMapper::toDto).collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionHistory(Long accountId, LocalDateTime from, LocalDateTime to, String type) {
        verifyAccess(accountId);
        List<Transaction> transactions = transactionRepository.findByAccount_Id(accountId);

        List<Transaction> filtered = transactions.stream()
                .filter(t -> from == null || !t.getTimestamp().isBefore(from))
                .filter(t -> to == null || !t.getTimestamp().isAfter(to))
                .filter(t -> type == null || t.getType().equalsIgnoreCase(type))
                .toList();

        return filtered.stream().map(transactionMapper::toDto).collect(Collectors.toList());
    }

    public TransactionSummary getMonthlySummary(Long accountId, int year, int month) {
        verifyAccess(accountId);

        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusNanos(1);

        List<TransactionDTO> filtered = getTransactionHistory(accountId, start, end, null);
        return TransactionSummary.from(filtered);
    }

    public List<TransactionDTO> getMyPerformedTransactions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        List<Transaction> list = transactionRepository.findByPerformedBy_Username(username);
        return list.stream().map(transactionMapper::toDto).collect(Collectors.toList());
    }

    public List<TransactionDTO> getAdminViewTransactions() {
        List<Transaction> all = transactionRepository.findAll();
        return all.stream().map(transactionMapper::toDto).collect(Collectors.toList());
    }

    public ByteArrayInputStream generateStatement(Long accountId) {
        verifyAccess(accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        List<TransactionDTO> history = getTransactionHistory(accountId);
        return pdfGenerator.generateStatement(account.getAccountNumber(), history);
    }

    public ByteArrayInputStream generateSingleTransactionReceipt(Long transactionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdminStaff = isAdminOrStaff(auth);

        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!isAdminStaff) {
            // Verify that this transaction belongs to an account owned by current user
            Account account = txn.getAccount();
            if (!account.getUser().getUsername().equals(username)) {
                throw new SecurityException("Not authorized to access this transaction.");
            }
        }

        TransactionDTO dto = transactionMapper.toDto(txn);

        return pdfGenerator.generateReceipt(
                txn.getAccount().getAccountNumber(),
                List.of(dto)
        );
    }

}

src/main/java/com/BMS/Bank_Management_System/dto/TransactionDTO.java








src/main/java/com/BMS/Bank_Management_System/service/TransactionService.java
