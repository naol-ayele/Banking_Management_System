package com.BMS.Bank_Management_System.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.BMS.Bank_Management_System.dto.CardlessWithdrawalRequest;
import com.BMS.Bank_Management_System.dto.CardlessWithdrawalResponse;
import com.BMS.Bank_Management_System.entity.Account;
import com.BMS.Bank_Management_System.entity.AccountStatus;
import com.BMS.Bank_Management_System.entity.Transaction;
import com.BMS.Bank_Management_System.exception.InsufficientBalanceException;
import com.BMS.Bank_Management_System.exception.ResourceNotFoundException;
import com.BMS.Bank_Management_System.repository.AccountRepository;
import com.BMS.Bank_Management_System.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardlessWithdrawalService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Token validity period (20 minutes)
    private static final long TOKEN_EXPIRY_MINUTES = 20;
    private static final String REDIS_KEY_PREFIX = "cardless:";

    @Transactional
    public CardlessWithdrawalResponse requestCardlessWithdrawal(Long accountId, BigDecimal amount) {
        log.info("Requesting cardless withdrawal for account: {}, amount: {}", accountId, amount);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        ensureAccountActive(account);
        ensureSufficientBalance(account, amount);

        // Generate unique token
        String token = generateSecureToken();
        String referenceId = "CWL-" + System.currentTimeMillis();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

        // Create withdrawal request DTO
        CardlessWithdrawalRequest withdrawalRequest = new CardlessWithdrawalRequest(
                accountId, amount, token, referenceId, expiryTime
        );

        // Store in Redis with TTL
        storeInRedis(token, withdrawalRequest);

        // Create pending transaction
        createTransaction(account, amount, token, referenceId, expiryTime);

        // Notify user
        notifyUser(account, token, amount);

        log.info("Cardless withdrawal requested successfully. Token: {}, Reference: {}", token, referenceId);

        return new CardlessWithdrawalResponse(token, referenceId, amount, expiryTime);
    }

    @Transactional
    public void processCardlessWithdrawal(String token, String atmId) {
        log.info("Processing cardless withdrawal for token: {}, ATM: {}", token, atmId);

        // Retrieve and validate token from Redis
        CardlessWithdrawalRequest request = retrieveAndValidateToken(token);

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + request.getAccountId()));

        // Double-check balance and account status
        ensureAccountActive(account);
        ensureSufficientBalance(account, request.getAmount());

        // Process withdrawal
        processWithdrawal(account, request.getAmount());

        // Update transaction status
        updateTransactionStatus(token, atmId);

        // Clean up Redis
        redisTemplate.delete(REDIS_KEY_PREFIX + token);

        // Notify user
        notificationService.notifyUser(account.getUser().getId(), "CARDLESS_WITHDRAWAL_COMPLETED",
                "Cardless withdrawal of " + request.getAmount() +
                        " processed successfully at ATM: " + atmId);

        log.info("Cardless withdrawal processed successfully. Token: {}, Amount: {}", token, request.getAmount());
    }

    public boolean validateToken(String token) {
        log.debug("Validating token: {}", token);
        boolean isValid = redisTemplate.hasKey(REDIS_KEY_PREFIX + token);
        log.debug("Token {} validation result: {}", token, isValid);
        return isValid;
    }

    public CardlessWithdrawalRequest getWithdrawalDetails(String token) {
        log.debug("Getting withdrawal details for token: {}", token);

        String requestJson = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + token);
        if (requestJson == null) {
            log.warn("Token {} not found in Redis", token);
            return null;
        }

        try {
            CardlessWithdrawalRequest request = objectMapper.readValue(requestJson, CardlessWithdrawalRequest.class);

            // Additional safety check
            if (request.getExpiryTime().isBefore(LocalDateTime.now())) {
                log.warn("Token {} found but expired. Cleaning up...", token);
                redisTemplate.delete(REDIS_KEY_PREFIX + token);
                return null;
            }

            return request;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize withdrawal details for token: {}", token, e);
            throw new RuntimeException("Failed to deserialize withdrawal details", e);
        }
    }

    // ============ PRIVATE HELPER METHODS ============

    private void storeInRedis(String token, CardlessWithdrawalRequest request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            redisTemplate.opsForValue().set(
                    REDIS_KEY_PREFIX + token,
                    requestJson,
                    TOKEN_EXPIRY_MINUTES, TimeUnit.MINUTES
            );
            log.debug("Stored token {} in Redis with {} minutes TTL", token, TOKEN_EXPIRY_MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize withdrawal request for token: {}", token, e);
            throw new RuntimeException("Failed to serialize withdrawal request", e);
        }
    }

    private void createTransaction(Account account, BigDecimal amount, String token, String referenceId, LocalDateTime expiryTime) {
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .account(account)
                .amount(amount)
                .type("CARDLESS_WITHDRAWAL_REQUEST")
                .status("PENDING")
                .referenceId(referenceId)
                .token(token)
                .expiryTime(expiryTime)
                .description("Cardless withdrawal request - use token: " + token)
                .timestamp(LocalDateTime.now())
                .build());
        log.debug("Created transaction for token: {}, Transaction ID: {}", token, transaction.getId());
    }

    private void notifyUser(Account account, String token, BigDecimal amount) {
        notificationService.notifyUser(account.getUser().getId(), "CARDLESS_WITHDRAWAL_REQUESTED",
                "Your cardless withdrawal token: " + token +
                        "\nAmount: " + amount +
                        "\nValid for: " + TOKEN_EXPIRY_MINUTES + " minutes" +
                        "\nUse at any partner ATM");
    }

    private CardlessWithdrawalRequest retrieveAndValidateToken(String token) {
        String requestJson = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + token);
        if (requestJson == null) {
            log.warn("Invalid or expired token: {}", token);
            throw new IllegalArgumentException("Invalid or expired token");
        }

        try {
            CardlessWithdrawalRequest request = objectMapper.readValue(requestJson, CardlessWithdrawalRequest.class);

            // Additional expiry check for safety
            if (request.getExpiryTime().isBefore(LocalDateTime.now())) {
                log.warn("Token {} expired. Cleaning up...", token);
                redisTemplate.delete(REDIS_KEY_PREFIX + token);
                throw new IllegalArgumentException("Token has expired");
            }

            return request;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize withdrawal request for token: {}", token, e);
            throw new RuntimeException("Failed to deserialize withdrawal request", e);
        }
    }

    private void processWithdrawal(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        log.debug("Processed withdrawal for account: {}, amount: {}", account.getId(), amount);
    }

    private void updateTransactionStatus(String token, String atmId) {
        Transaction transaction = transactionRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.error("Transaction not found for token: {}", token);
                    return new ResourceNotFoundException("Transaction not found for token: " + token);
                });

        transaction.setStatus("COMPLETED");
        transaction.setDescription("Processed at ATM: " + atmId);
        transaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(transaction);
        log.debug("Updated transaction status for token: {}", token);
    }

    private String generateSecureToken() {
        // Generate 6-digit OTP + UUID for security
        int otp = 100000 + new Random().nextInt(900000);
        String uuidPart = UUID.randomUUID().toString().substring(0, 8);
        return otp + "-" + uuidPart;
    }

    private void ensureAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Account {} is not active. Status: {}", account.getId(), account.getStatus());
            throw new IllegalStateException("Account is not active: status=" + account.getStatus());
        }
    }

    private void ensureSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for account: {}. Current: {}, Required: {}",
                    account.getId(), account.getBalance(), amount);
            throw new InsufficientBalanceException("Not enough funds");
        }
    }
}