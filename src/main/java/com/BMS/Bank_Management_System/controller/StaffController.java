package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.*;
import com.BMS.Bank_Management_System.entity.AccountStatus;
import com.BMS.Bank_Management_System.entity.User;
import com.BMS.Bank_Management_System.exception.ResourceNotFoundException;
import com.BMS.Bank_Management_System.mapper.AccountMapper;
import com.BMS.Bank_Management_System.repository.AccountRepository;
import com.BMS.Bank_Management_System.repository.UserRepository;
import com.BMS.Bank_Management_System.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final AccountService accountService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping(value = "/create-account", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createAccount(
            @RequestPart(value = "payload") CreateCustomerAndAccountRequest payload,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws Exception {
        if (file != null && !file.isEmpty()) {
            accountService.createCustomerAndAccountWithId(payload, file);
        } else {
            accountService.createCustomerAndAccount(payload);
        }
        return ResponseEntity.ok("Customer and account created successfully");
    }


    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping(value = "/create-account", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAccountJson(@RequestBody CreateCustomerAndAccountRequest payload) {
        accountService.createCustomerAndAccount(payload);
        return ResponseEntity.ok("Customer and account created successfully");
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PutMapping("/accounts/{accountId}/approve")
    public ResponseEntity<String> approveAccount(@PathVariable Long accountId) {
        accountService.approveAccount(accountId);
        return ResponseEntity.ok("Account approved");
    }

    @GetMapping("/customers/search")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<CustomerSearchDTO>> searchCustomers(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(accountService.searchCustomers(q, limit));
    }


    @GetMapping("/users/users/details/{username}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getUserDetailsByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDTO userDto = new UserDTO();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setRole(user.getRole() != null ? user.getRole().name() : null);

        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<CustomerSearchDTO>> getAllCustomers(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(accountService.searchCustomers("", limit));
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CustomerSearchDTO> getCustomerById(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.getCustomerById(customerId));
    }

    @PostMapping("/customers/{customerId}/accounts")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<String> createAccountForExistingCustomer(
            @PathVariable Long customerId,
            @RequestBody CreateAccountForCustomerRequest request
    ) {
        accountService.createAccountForExistingCustomer(customerId, request);
        return ResponseEntity.ok("Account created successfully for existing customer");
    }

    @PutMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<String> updateCustomerInformation(
            @PathVariable Long customerId,
            @RequestBody UpdateCustomerRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        String staffUsername = authentication.getName();
        accountService.updateCustomerInformation(customerId, request, staffUsername);
        return ResponseEntity.ok("Customer information updated successfully");
    }


    @PostMapping("/customers/{customerId}/name-change")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<String> changeCustomerName(
            @PathVariable Long customerId,
            @RequestPart("request") NameChangeRequest request,
            @RequestPart("document") org.springframework.web.multipart.MultipartFile legalDocument,
            org.springframework.security.core.Authentication authentication
    ) throws Exception {
        String staffUsername = authentication.getName();
        accountService.changeCustomerName(customerId, request, legalDocument, staffUsername);
        return ResponseEntity.ok("Customer name changed successfully");
    }

    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @DeleteMapping("/accounts/{accountId}/reject")
    public ResponseEntity<String> rejectAccount(
            @PathVariable Long accountId,
            org.springframework.security.core.Authentication authentication
    ) {
        String staffUsername = authentication.getName();
        accountService.rejectAccount(accountId, staffUsername);
        return ResponseEntity.ok("Account rejected and removed");
    }

    @GetMapping("/accounts/pending")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<List<AccountDTO>> getPendingAccounts() {
        List<AccountDTO> accounts = accountRepository.findByStatus(AccountStatus.PENDING_APPROVAL)
                .stream().map(accountMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(accounts);
    }

}