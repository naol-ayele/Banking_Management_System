package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.loan.LoanResponse;
import com.BMS.Bank_Management_System.service.LoanService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/loans")
@RequiredArgsConstructor
public class LoanOfficerController {

    private final LoanService loanService;

    @GetMapping("/pending")
    public ResponseEntity<List<LoanResponse>> pending() {
        return ResponseEntity.ok(loanService.listPending());
    }

    @PostMapping("/{loanId}/approve")
    public ResponseEntity<LoanResponse> approve(@PathVariable Long loanId,
                                                @RequestBody RemarkReq req,
                                                Authentication auth) {
        return ResponseEntity.ok(loanService.approve(loanId, auth.getName(), req.getRemark()));
    }

    @PostMapping("/{loanId}/reject")
    public ResponseEntity<LoanResponse> reject(@PathVariable Long loanId,
                                               @RequestBody RemarkReq req,
                                               Authentication auth) {
        return ResponseEntity.ok(loanService.reject(loanId, auth.getName(), req.getRemark()));
    }

    @GetMapping
    public ResponseEntity<List<LoanResponse>> all() {
        return ResponseEntity.ok(loanService.listAll());
    }

    @Data
    public static class RemarkReq { private String remark; }
}
