package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.AccountDTO;
import com.BMS.Bank_Management_System.dto.CreateCustomerAndAccountRequest;
import com.BMS.Bank_Management_System.dto.CustomerSearchDTO;
import com.BMS.Bank_Management_System.dto.CreateAccountForCustomerRequest;
import com.BMS.Bank_Management_System.dto.UpdateCustomerRequest;
import com.BMS.Bank_Management_System.dto.NameChangeRequest;
import com.BMS.Bank_Management_System.dto.ComplianceDocumentRequest;
import com.BMS.Bank_Management_System.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final AccountService accountService;

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping(value = "/create-account", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createAccount(
            @RequestPart(value = "payload", required = false) com.BMS.Bank_Management_System.dto.CreateCustomerAndAccountRequest payload,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "payload", required = false) String payloadRaw
    ) throws Exception {
        if (payload == null && payloadRaw != null) {
            payload = new com.fasterxml.jackson.databind.ObjectMapper().readValue(payloadRaw, com.BMS.Bank_Management_System.dto.CreateCustomerAndAccountRequest.class);
        }
        if (payload == null) {
            return ResponseEntity.badRequest().body("Missing payload");
        }
        if (file != null && !file.isEmpty()) {
            accountService.createCustomerAndAccountWithId(payload, file);
        } else {
            accountService.createCustomerAndAccount(payload);
        }
        return ResponseEntity.ok("Customer and account created successfully");
    }

    // JSON fallback when no file is being uploaded
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping(value = "/create-account", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAccountJson(@RequestBody CreateCustomerAndAccountRequest payload) {
        accountService.createCustomerAndAccount(payload);
        return ResponseEntity.ok("Customer and account created successfully");
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PutMapping("/update-account/{accountId}")
    public ResponseEntity<String> updateAccount(@PathVariable Long accountId, @RequestBody AccountDTO accountDTO) {
        accountService.updateAccount(accountId, accountDTO);
        return ResponseEntity.ok("Account updated successfully");
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PutMapping("/accounts/{accountId}/approve")
    public ResponseEntity<String> approveAccount(@PathVariable Long accountId) {
        accountService.approveAccount(accountId);
        return ResponseEntity.ok("Account approved");
    }

    // Deprecated endpoints merged into /create-account

    @GetMapping("/customers/search")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<CustomerSearchDTO>> searchCustomers(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(accountService.searchCustomers(q, limit));
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

    @PutMapping("/customers/{customerId}/compliance-update")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<String> updateCustomerWithCompliance(
            @PathVariable Long customerId,
            @RequestBody UpdateCustomerRequest request,
            @RequestPart(value = "compliance", required = false) ComplianceDocumentRequest compliance,
            org.springframework.security.core.Authentication authentication
    ) {
        String staffUsername = authentication.getName();
        accountService.updateCustomerInformationWithCompliance(customerId, request, compliance, staffUsername);
        return ResponseEntity.ok("Customer information updated with compliance tracking");
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
}
