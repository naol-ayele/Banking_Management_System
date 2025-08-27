package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.dto.TransactionDTO;
import com.BMS.Bank_Management_System.dto.UserDTO;
import com.BMS.Bank_Management_System.dto.AccountWithTransactionsDTO;
import com.BMS.Bank_Management_System.dto.TransactionSimpleDTO;
import com.BMS.Bank_Management_System.entity.Role;
import com.BMS.Bank_Management_System.entity.User;
import com.BMS.Bank_Management_System.exception.ResourceNotFoundException;
import com.BMS.Bank_Management_System.repository.TransactionRepository;
import com.BMS.Bank_Management_System.repository.UserRepository;
import com.BMS.Bank_Management_System.repository.AuditLogRepository;
import com.BMS.Bank_Management_System.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.BMS.Bank_Management_System.mapper.UserMapper;
import com.BMS.Bank_Management_System.mapper.AccountWithTransactionsMapper;
import lombok.RequiredArgsConstructor;
import com.BMS.Bank_Management_System.entity.AuditLog;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AccountWithTransactionsMapper accountWithTransactionsMapper;

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public UserDTO updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    // MapStruct handles mapping

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);
        saveAudit(null, "DELETE_USER", "Deleted user id=" + userId);
    }

    public void saveAudit(String username, String action, String details) {
        com.BMS.Bank_Management_System.entity.AuditLog log = com.BMS.Bank_Management_System.entity.AuditLog.builder()
                .username(username)
                .action(action)
                .details(details)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    public java.util.List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public String resetUserPassword(Long userId, String requestedNewPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String temp = (requestedNewPassword != null && !requestedNewPassword.isBlank()) ? requestedNewPassword : generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temp));
        userRepository.save(user);
        return temp;
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$%";
        java.security.SecureRandom r = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    public List<AccountWithTransactionsDTO> getAllAccountsWithTransactions() {
        return accountRepository.findAll().stream()
                .map(accountWithTransactionsMapper::toDto)
                .collect(Collectors.toList());
    }
}
