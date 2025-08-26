package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.dto.AccountDTO;
import com.BMS.Bank_Management_System.dto.CreateCustomerAndAccountRequest;
import com.BMS.Bank_Management_System.entity.Account;
import com.BMS.Bank_Management_System.entity.*;
import com.BMS.Bank_Management_System.entity.User;
import com.BMS.Bank_Management_System.exception.InsufficientBalanceException;
import com.BMS.Bank_Management_System.exception.ResourceNotFoundException;
import com.BMS.Bank_Management_System.exception.UnauthorizedActionException;
import com.BMS.Bank_Management_System.repository.AccountRepository;
import com.BMS.Bank_Management_System.repository.TransactionRepository;
import com.BMS.Bank_Management_System.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.BMS.Bank_Management_System.mapper.AccountMapper;
import com.BMS.Bank_Management_System.dto.CustomerSearchDTO;
import com.BMS.Bank_Management_System.dto.CreateAccountForCustomerRequest;
import com.BMS.Bank_Management_System.dto.UpdateCustomerRequest;
import com.BMS.Bank_Management_System.dto.NameChangeRequest;
import com.BMS.Bank_Management_System.dto.ComplianceDocumentRequest;
import com.BMS.Bank_Management_System.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final AccountMapper accountMapper;
    private final com.BMS.Bank_Management_System.service.AdminService adminService;

    public void createAccount(AccountDTO accountDTO) {
        User user = userRepository.findById(accountDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + accountDTO.getUserId()));

        Account account = Account.builder()
                .accountNumber(accountDTO.getAccountNumber() != null ? accountDTO.getAccountNumber() : generateAccountNumber())
                .balance(accountDTO.getBalance() != null ? accountDTO.getBalance() : BigDecimal.ZERO)
                .accountType(accountDTO.getAccountType() != null ? AccountType.valueOf(accountDTO.getAccountType()) : AccountType.SAVINGS)
                .status(accountDTO.getStatus() != null ? AccountStatus.valueOf(accountDTO.getStatus()) : AccountStatus.PENDING_APPROVAL)
                .user(user)
                .build();

        accountRepository.save(account);
    }

    public void deposit(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        ensureAccountActive(account);
        ensureUserCanOperate(account);

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        transactionRepository.save(Transaction.builder()
                .account(account)
                .fromAccount(null)
                .toAccount(account)
                .amount(amount)
                .type("DEPOSIT")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .referenceId(generateReferenceId())
                .description("Deposit made")
                .performedBy(getCurrentUser())
                .build());
        notificationService.notifyUser(account.getUser().getId(), "TXN_SUCCESS", "Deposit of " + amount + " to account " + account.getAccountNumber());
    }

    public void withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        ensureAccountActive(account);
        ensureUserCanOperate(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Not enough funds");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        transactionRepository.save(Transaction.builder()
                .account(account)
                .fromAccount(account)
                .toAccount(null)
                .amount(amount)
                .type("WITHDRAW")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .referenceId(generateReferenceId())
                .description("Withdrawal made")
                .performedBy(getCurrentUser())
                .build());
        notificationService.notifyUser(account.getUser().getId(), "TXN_SUCCESS", "Withdrawal of " + amount + " from account " + account.getAccountNumber());
    }

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Account from = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + fromAccountId));
        Account to = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + toAccountId));

        ensureAccountActive(from);
        ensureAccountActive(to);
        ensureUserCanOperate(from); // only owner or staff/admin can initiate transfer from 'from' account

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Not enough funds");
        }

        String ref = generateReferenceId();
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);

        transactionRepository.save(Transaction.builder()
                .account(from)
                .fromAccount(from)
                .toAccount(to)
                .amount(amount)
                .type("TRANSFER_DEBIT")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .referenceId(ref)
                .description("Transfer to account " + to.getAccountNumber())
                .performedBy(getCurrentUser())
                .build());

        transactionRepository.save(Transaction.builder()
                .account(to)
                .fromAccount(from)
                .toAccount(to)
                .amount(amount)
                .type("TRANSFER_CREDIT")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .referenceId(ref)
                .description("Transfer from account " + from.getAccountNumber())
                .performedBy(getCurrentUser())
                .build());
        notificationService.notifyUser(from.getUser().getId(), "TXN_SUCCESS", "Transferred " + amount + " to account " + to.getAccountNumber());
        notificationService.notifyUser(to.getUser().getId(), "TXN_SUCCESS", "Received " + amount + " from account " + from.getAccountNumber());
    }

    public List<AccountDTO> getMyAccounts(String username) {
        List<Account> accounts = accountRepository.findByUser_Username(username);
        return accounts.stream().map(accountMapper::toDto).collect(Collectors.toList());
    }


    public void approveAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
        notificationService.notifyUser(account.getUser().getId(), "ACCOUNT_APPROVED", "Your account " + account.getAccountNumber() + " has been approved.");
    }

    public void freezeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);
        notificationService.notifyUser(account.getUser().getId(), "ACCOUNT_FROZEN", "Your account " + account.getAccountNumber() + " has been frozen.");
    }

    public void unfreezeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
        notificationService.notifyUser(account.getUser().getId(), "ACCOUNT_UNFROZEN", "Your account " + account.getAccountNumber() + " has been unfrozen.");
    }

    public void rejectAccount(Long accountId, String staffUsername) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if (account.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Only pending accounts can be rejected");
        }

        // Get user info before deletion for audit
        Long userId = account.getUser().getId();
        String accountNumber = account.getAccountNumber();
        String username = account.getUser().getUsername();

        // Delete the rejected account
        accountRepository.delete(account);

        // Log the rejection in audit log
        if (staffUsername != null) {
            adminService.saveAudit(staffUsername, "REJECT_ACCOUNT",
                    "Rejected pending account " + accountNumber + " for user " + username + " (ID: " + userId + ")");
        }

        // Notify user about account rejection
        notificationService.notifyUser(userId, "ACCOUNT_REJECTED",
                "Your account application for " + accountNumber + " has been rejected. Please contact staff for more information.");
    }

    private void ensureAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active: status=" + account.getStatus());
        }
    }

    private void ensureUserCanOperate(Account account) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedActionException("No authenticated user");
        }
        boolean privileged = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_STAFF".equals(a));
        if (privileged) {
            return;
        }
        String username = auth.getName();
        if (!account.getUser().getUsername().equals(username)) {
            throw new UnauthorizedActionException("You are not allowed to operate on this account");
        }
    }

    private String generateAccountNumber() {
        // simple: 12-digit random number; in production enforce uniqueness with DB check
        String base = String.valueOf(System.currentTimeMillis());
        return base.substring(base.length() - 12);
    }

    private String generateReferenceId() {
        return "TXN-" + System.nanoTime();
    }

    private com.BMS.Bank_Management_System.entity.User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    public void createCustomerAndAccount(CreateCustomerAndAccountRequest req) {
        // upsert by username/email
        User user = userRepository.findByUsername(req.getUsername())
                .or(() -> req.getEmail() != null ? userRepository.findByEmail(req.getEmail()) : Optional.empty())
                .or(() -> req.getPhone() != null ? userRepository.findByPhone(req.getPhone()) : Optional.empty())
                .orElse(null);


        if (user == null) {
            user = new User();
            user.setUsername(req.getUsername());
            user.setEmail(req.getEmail());
            user.setPhone(req.getPhone());
            user.setMotherName(req.getMotherName());
            String plain = (req.getPassword() != null && !req.getPassword().isBlank()) ? req.getPassword() : "Temp@12345";
            user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(plain));
            user.setRole(Role.CUSTOMER);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user = userRepository.save(user);
        }

        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setUserId(user.getId());
        accountDTO.setAccountType(req.getAccountType());
        if (req.getOpeningBalance() != null && req.getOpeningBalance().compareTo(new java.math.BigDecimal("200")) < 0) {
            throw new IllegalArgumentException("Opening balance must be at least 200");
        }
        accountDTO.setBalance(req.getOpeningBalance() != null ? req.getOpeningBalance() : new java.math.BigDecimal("200"));
        accountDTO.setStatus("PENDING_APPROVAL");
        createAccount(accountDTO);
    }

    public void createCustomerAndAccountWithId(CreateCustomerAndAccountRequest req, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        // Create or find user
        User user = userRepository.findByUsername(req.getUsername())
                .or(() -> req.getEmail() != null ? userRepository.findByEmail(req.getEmail()) : Optional.empty())
                .or(() -> req.getPhone() != null ? userRepository.findByPhone(req.getPhone()) : Optional.empty())
                .orElse(null);

        if (user == null) {
            user = new User();
            user.setUsername(req.getUsername());
            user.setEmail(req.getEmail());
            String plain = (req.getPassword() != null && !req.getPassword().isBlank()) ? req.getPassword() : "Temp@12345";
            user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(plain));
            user.setRole(Role.CUSTOMER);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            // set additional KYC info
            user.setPhone(req.getPhone());
            user.setMotherName(req.getMotherName());
            user = userRepository.save(user);
        }
        // If user exists and optional fields provided, update them
        else {
            boolean changed = false;
            if (req.getPhone() != null && (user.getPhone() == null || user.getPhone().isBlank())) {
                user.setPhone(req.getPhone());
                changed = true;
            }
            if (req.getMotherName() != null && (user.getMotherName() == null || user.getMotherName().isBlank())) {
                user.setMotherName(req.getMotherName());
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
        }
        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.storeNationalId(user.getId(), file);
            user.setNationalIdImageUrl(path);
            userRepository.save(user);
        }

        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setUserId(user.getId());
        accountDTO.setAccountType(req.getAccountType());
        accountDTO.setBalance(req.getOpeningBalance());
        accountDTO.setStatus("PENDING_APPROVAL");
        createAccount(accountDTO);
    }

    public List<CustomerSearchDTO> searchCustomers(String query, int limit) {
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(
                        query, query, query, org.springframework.data.domain.PageRequest.of(0, limit)
                ).stream()
                .filter(user -> user.getRole() == Role.CUSTOMER)  // Only customers
                .map(this::toCustomerSearchDTO)
                .collect(Collectors.toList());
    }

    private CustomerSearchDTO toCustomerSearchDTO(User user) {
        CustomerSearchDTO dto = new CustomerSearchDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());

        // Count accounts for this customer
        int accountCount = accountRepository.findByUser_Username(user.getUsername()).size();
        dto.setAccountCount(accountCount);

        // Determine status based on account statuses
        String status = "ACTIVE";
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.Instant.now())) {
            status = "LOCKED";
        }
        dto.setStatus(status);

        return dto;
    }

    public CustomerSearchDTO getCustomerById(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("User with id " + customerId + " is not a customer");
        }

        return toCustomerSearchDTO(customer);
    }

    public void createAccountForExistingCustomer(Long customerId, CreateAccountForCustomerRequest request) {
        // Verify customer exists and is a CUSTOMER
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("User with id " + customerId + " is not a customer");
        }

        // Check if customer has at least one ACTIVE account
        List<Account> existingAccounts = accountRepository.findByUser_Username(customer.getUsername());
        boolean hasActiveAccount = existingAccounts.stream()
                .anyMatch(acc -> acc.getStatus() == AccountStatus.ACTIVE);

        if (!hasActiveAccount) {
            throw new IllegalArgumentException("Customer must have at least one active account before creating additional accounts");
        }

        // Validate opening balance
        if (request.getOpeningBalance() != null && request.getOpeningBalance().compareTo(new java.math.BigDecimal("200")) < 0) {
            throw new IllegalArgumentException("Opening balance must be at least 200");
        }

        // Create account for existing customer
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setUserId(customerId);
        accountDTO.setAccountType(request.getAccountType());
        accountDTO.setBalance(request.getOpeningBalance() != null ? request.getOpeningBalance() : new java.math.BigDecimal("200"));
        accountDTO.setStatus("PENDING_APPROVAL");

        createAccount(accountDTO);
    }

    public void updateCustomerInformation(Long customerId, UpdateCustomerRequest request, String staffUsername) {
        // Verify customer exists and is a CUSTOMER
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("User with id " + customerId + " is not a customer");
        }

        boolean changesMade = false;
        String changeDetails = "";

        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().isBlank() && !request.getEmail().equals(customer.getEmail())) {
            // Check if email is already taken by another user
            if (userRepository.existsByEmail(request.getEmail()) && !request.getEmail().equals(customer.getEmail())) {
                throw new IllegalArgumentException("Email " + request.getEmail() + " is already in use");
            }
            customer.setEmail(request.getEmail());
            changesMade = true;
            changeDetails += "Email updated to: " + request.getEmail() + "; ";
        }

        // Update phone if provided
        if (request.getPhone() != null && !request.getPhone().isBlank() && !request.getPhone().equals(customer.getPhone())) {
            // Check if phone is already taken by another user
            if (userRepository.existsByPhone(request.getPhone()) && !request.getPhone().equals(customer.getPhone())) {
                throw new IllegalArgumentException("Phone " + request.getPhone() + " is already in use");
            }
            customer.setPhone(request.getPhone());
            changesMade = true;
            changeDetails += "Phone updated to: " + request.getPhone() + "; ";
        }

        // Update mother's name if provided
        if (request.getMotherName() != null && !request.getMotherName().isBlank() && !request.getMotherName().equals(customer.getMotherName())) {
            customer.setMotherName(request.getMotherName());
            changesMade = true;
            changeDetails += "Mother's name updated to: " + request.getMotherName() + "; ";
        }

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            customer.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(request.getPassword()));
            changesMade = true;
            changeDetails += "Password reset; ";
        }

        if (!changesMade) {
            throw new IllegalArgumentException("No changes were made to customer information");
        }

        // Save the updated customer
        userRepository.save(customer);

        // Log the changes in audit log
        if (staffUsername != null) {
            adminService.saveAudit(staffUsername, "UPDATE_CUSTOMER_INFO",
                    "Updated customer " + customer.getUsername() + " (ID: " + customerId + "). " + changeDetails);
        }
    }

    public void changeCustomerName(Long customerId, NameChangeRequest request, org.springframework.web.multipart.MultipartFile legalDocument, String staffUsername) throws java.io.IOException {
        // Verify customer exists and is a CUSTOMER
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("User with id " + customerId + " is not a customer");
        }

        // Comprehensive compliance validation
        validateNameChangeRequest(request);

        // Check if new username is already taken
        if (userRepository.existsByUsername(request.getNewUsername()) && !request.getNewUsername().equals(customer.getUsername())) {
            throw new IllegalArgumentException("Username " + request.getNewUsername() + " is already in use");
        }

        // Validate legal document
        if (legalDocument == null || legalDocument.isEmpty()) {
            throw new IllegalArgumentException("Legal document is required for name change");
        }

        // Store the legal document with enhanced naming
        String documentPath = fileStorageService.storeLegalDocument(customerId, legalDocument,
                "name-change-" + request.getDocumentType().toLowerCase().replace("_", "-"));

        // Update customer username
        String oldUsername = customer.getUsername();
        customer.setUsername(request.getNewUsername());
        userRepository.save(customer);

        // Create comprehensive audit log with compliance details
        String changeDetails = String.format(
                "Name changed from '%s' to '%s'. Reason: %s. Document: %s (ID: %s, Authority: %s, Issue Date: %s). " +
                        "Verified by: %s using method: %s on %s. Document stored at: %s. Additional Notes: %s",
                oldUsername, request.getNewUsername(), request.getReasonForChange(),
                request.getDocumentType(), request.getDocumentNumber(), request.getIssuingAuthority(),
                request.getIssueDate(), request.getVerifiedBy(), request.getVerificationMethod(),
                request.getVerificationDate(), documentPath,
                request.getAdditionalNotes() != null ? request.getAdditionalNotes() : "None"
        );

        if (staffUsername != null) {
            adminService.saveAudit(staffUsername, "CHANGE_CUSTOMER_NAME_COMPLIANCE", changeDetails);
        }

        // Send detailed notification to customer about name change
        notificationService.notifyUser(customerId, "NAME_CHANGED_COMPLIANCE",
                "Your legal name has been changed from '" + oldUsername + "' to '" + request.getNewUsername() + "' " +
                        "based on " + request.getDocumentType() + " (ID: " + request.getDocumentNumber() + "). " +
                        "Please use your new legal name for future logins. " +
                        "This change was verified and approved by our compliance team.");
    }

    private void validateNameChangeRequest(NameChangeRequest request) {
        // Required field validation
        if (request.getNewUsername() == null || request.getNewUsername().isBlank()) {
            throw new IllegalArgumentException("New username is required");
        }

        if (request.getReasonForChange() == null || request.getReasonForChange().isBlank()) {
            throw new IllegalArgumentException("Reason for name change is required");
        }

        if (request.getDocumentType() == null || request.getDocumentType().isBlank()) {
            throw new IllegalArgumentException("Document type is required");
        }

        if (request.getDocumentNumber() == null || request.getDocumentNumber().isBlank()) {
            throw new IllegalArgumentException("Document number is required");
        }

        if (request.getIssuingAuthority() == null || request.getIssuingAuthority().isBlank()) {
            throw new IllegalArgumentException("Issuing authority is required");
        }

        if (request.getIssueDate() == null || request.getIssueDate().isBlank()) {
            throw new IllegalArgumentException("Issue date is required");
        }

        if (request.getVerificationMethod() == null || request.getVerificationMethod().isBlank()) {
            throw new IllegalArgumentException("Verification method is required");
        }

        if (request.getVerifiedBy() == null || request.getVerifiedBy().isBlank()) {
            throw new IllegalArgumentException("Verifier username is required");
        }

        if (request.getVerificationDate() == null || request.getVerificationDate().isBlank()) {
            throw new IllegalArgumentException("Verification date is required");
        }

        // Validate reason for change
        java.util.Set<String> validReasons = java.util.Set.of("DIVORCE", "MARRIAGE", "LEGAL_CHANGE", "CORRECTION");
        if (!validReasons.contains(request.getReasonForChange().toUpperCase())) {
            throw new IllegalArgumentException("Invalid reason for change. Must be one of: " + validReasons);
        }

        // Validate document type
        java.util.Set<String> validDocumentTypes = java.util.Set.of("COURT_ORDER", "MARRIAGE_CERT", "DIVORCE_DECREE", "GOVERNMENT_ID");
        if (!validDocumentTypes.contains(request.getDocumentType().toUpperCase())) {
            throw new IllegalArgumentException("Invalid document type. Must be one of: " + validDocumentTypes);
        }

        // Validate verification method
        java.util.Set<String> validVerificationMethods = java.util.Set.of("STAFF_VERIFIED", "ADMIN_VERIFIED", "THIRD_PARTY");
        if (!validVerificationMethods.contains(request.getVerificationMethod().toUpperCase())) {
            throw new IllegalArgumentException("Invalid verification method. Must be one of: " + validVerificationMethods);
        }

        // Validate date formats (basic validation)
        try {
            java.time.LocalDate.parse(request.getIssueDate());
            java.time.LocalDate.parse(request.getVerificationDate());
            if (request.getExpiryDate() != null && !request.getExpiryDate().isBlank()) {
                java.time.LocalDate.parse(request.getExpiryDate());
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD format");
        }

        // Validate that verification date is not in the future
        java.time.LocalDate verificationDate = java.time.LocalDate.parse(request.getVerificationDate());
        if (verificationDate.isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Verification date cannot be in the future");
        }
    }


    private void validateComplianceDocument(ComplianceDocumentRequest compliance) {
        if (compliance.getDocumentType() == null || compliance.getDocumentType().isBlank()) {
            throw new IllegalArgumentException("Document type is required for compliance");
        }
        if (compliance.getDocumentNumber() == null || compliance.getDocumentNumber().isBlank()) {
            throw new IllegalArgumentException("Document number is required for compliance");
        }
        if (compliance.getIssuingAuthority() == null || compliance.getIssuingAuthority().isBlank()) {
            throw new IllegalArgumentException("Issuing authority is required for compliance");
        }
        if (compliance.getIssueDate() == null || compliance.getIssueDate().isBlank()) {
            throw new IllegalArgumentException("Issue date is required for compliance");
        }
        if (compliance.getVerifiedBy() == null || compliance.getVerifiedBy().isBlank()) {
            throw new IllegalArgumentException("Verifier username is required for compliance");
        }
        if (compliance.getVerificationDate() == null || compliance.getVerificationDate().isBlank()) {
            throw new IllegalArgumentException("Verification date is required for compliance");
        }

        // Validate date format
        try {
            java.time.LocalDate.parse(compliance.getIssueDate());
            java.time.LocalDate.parse(compliance.getVerificationDate());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD format");
        }
    }
}