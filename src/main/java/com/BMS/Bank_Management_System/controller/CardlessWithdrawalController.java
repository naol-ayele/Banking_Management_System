package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.CardlessWithdrawalRequest;
import com.BMS.Bank_Management_System.dto.CardlessWithdrawalResponse;
import com.BMS.Bank_Management_System.dto.ProcessWithdrawalRequest;
import com.BMS.Bank_Management_System.service.CardlessWithdrawalService;
import com.BMS.Bank_Management_System.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cardless")
@RequiredArgsConstructor
public class CardlessWithdrawalController {

    private final CardlessWithdrawalService cardlessWithdrawalService;
    private final QrCodeService qrCodeService;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/withdrawal/request")
    public ResponseEntity<CardlessWithdrawalResponse> requestCardlessWithdrawal(
            @RequestBody CardlessWithdrawalRequest request) {

        return ResponseEntity.ok(cardlessWithdrawalService.requestCardlessWithdrawal(
                request.getAccountId(), request.getAmount()));
    }


    // For ATM to get withdrawal details
    @GetMapping("/withdrawal/details/{token}")
    public ResponseEntity<CardlessWithdrawalRequest> getWithdrawalDetails(
            @PathVariable String token) {

        CardlessWithdrawalRequest details =
                cardlessWithdrawalService.getWithdrawalDetails(token);

        if (details == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }

    @GetMapping("/withdrawal/qr/{token}")
    public ResponseEntity<byte[]> getWithdrawalQrCode(@PathVariable String token) throws Exception {
        String qrData = "BMS_WITHDRAWAL:" + token; // Custom schema
        byte[] qrCode = qrCodeService.generateQrCode(qrData, 200, 200);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }
}