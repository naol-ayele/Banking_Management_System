package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.AtmWithdrawalRequest;
import com.BMS.Bank_Management_System.dto.CardlessWithdrawalRequest;
import com.BMS.Bank_Management_System.dto.ProcessWithdrawalRequest;
import com.BMS.Bank_Management_System.service.CardlessWithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/atm")
public class AtmSimulatorController {

    private final CardlessWithdrawalService cardlessWithdrawalService;

    public AtmSimulatorController(CardlessWithdrawalService cardlessWithdrawalService) {
        this.cardlessWithdrawalService = cardlessWithdrawalService;
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> simulateAtmWithdrawal(@RequestBody AtmWithdrawalRequest request) {
        cardlessWithdrawalService.processCardlessWithdrawal(request.getToken(), "ATM-001");
        return ResponseEntity.ok("Cash dispensed successfully");
    }

    @GetMapping("/validate/{token}")
    public ResponseEntity<Map<String, Object>> validateTokenAtAtm(@PathVariable String token) {
        boolean isValid = cardlessWithdrawalService.validateToken(token);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);

        if (isValid) {
            CardlessWithdrawalRequest details =
                    cardlessWithdrawalService.getWithdrawalDetails(token);
            response.put("amount", details.getAmount());
            response.put("expiry", details.getExpiryTime());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdrawal/process")
    public ResponseEntity<String> processCardlessWithdrawal(
            @RequestBody ProcessWithdrawalRequest request) {

        cardlessWithdrawalService.processCardlessWithdrawal(request.getToken(), request.getAtmId());
        return ResponseEntity.ok("Withdrawal processed successfully");
    }
}