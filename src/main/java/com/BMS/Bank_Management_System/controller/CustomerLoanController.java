package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.TransactionDTO;
import com.BMS.Bank_Management_System.dto.loan.LoanApplicationRequest;
import com.BMS.Bank_Management_System.dto.loan.LoanResponse;
import com.BMS.Bank_Management_System.dto.loan.RepaymentRequest;
import com.BMS.Bank_Management_System.entity.LoanRepayment;
import com.BMS.Bank_Management_System.repository.LoanRepaymentRepository;
import com.BMS.Bank_Management_System.service.LoanPdfService;
import com.BMS.Bank_Management_System.service.LoanService;
import com.BMS.Bank_Management_System.util.LoanPdfGenerator;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class CustomerLoanController {

    private final LoanService loanService;
    private final LoanPdfService loanPdfService;
    private final LoanPdfGenerator loanPdfGenerator;
    private final LoanRepaymentRepository repaymentRepo;

    @PostMapping("/apply")
    public ResponseEntity<LoanResponse> apply(@RequestBody LoanApplicationRequest req, Authentication auth) {
        return ResponseEntity.ok(loanService.apply(auth.getName(), req));
    }

    @PostMapping("/{loanId}/repay")
    public ResponseEntity<LoanResponse> repay(@PathVariable Long loanId,
                                              @RequestBody RepaymentRequest req,
                                              Authentication auth) {
        return ResponseEntity.ok(loanService.repay(loanId, req, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<LoanResponse>> myLoans(Authentication auth) {
        return ResponseEntity.ok(loanService.myLoans(auth.getName()));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<LoanResponse> getOne(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.getOne(loanId));
    }

    @GetMapping("/{loanId}/statement.pdf")
    public void downloadLoanStatement(@PathVariable Long loanId, HttpServletResponse response) throws IOException {
        List<TransactionDTO> rows = loanPdfService.buildLoanStatementRows(loanId);
        String loanNumber = "LOAN-" + loanId;

        ByteArrayInputStream pdf = loanPdfGenerator.generateLoanStatement(loanNumber, rows);

        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loan-statement-" + loanId + ".pdf");
        pdf.transferTo(response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/repayments/{repaymentId}/receipt.pdf")
    public void downloadRepaymentReceipt(@PathVariable Long repaymentId, HttpServletResponse response) throws IOException {
        LoanRepayment r = repaymentRepo.findById(repaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found"));

        TransactionDTO row = loanPdfService.buildSingleRepaymentRow(r);
        String loanNumber = "LOAN-" + r.getLoan().getId();

        ByteArrayInputStream pdf = loanPdfGenerator.generateRepaymentReceipt(loanNumber, List.of(row));

        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repayment-receipt-" + repaymentId + ".pdf");
        pdf.transferTo(response.getOutputStream());
        response.flushBuffer();
    }
}
