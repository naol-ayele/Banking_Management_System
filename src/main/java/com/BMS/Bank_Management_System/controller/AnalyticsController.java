//controls the financial analysis and loan eligibility checks for users
package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/financial-summary")
    public ResponseEntity<Map<String, Object>> getFinancialSummary(@RequestParam Long user_id) {
        return ResponseEntity.ok(analyticsService.getFinancialSummary(user_id));
    }

    @GetMapping("/loan-eligibility")
    public ResponseEntity<Map<String, Object>> checkLoanEligibility(
            @RequestParam Long user_id,
            @RequestParam(required = false) Double desired_amount
    ) {
        return ResponseEntity.ok(analyticsService.checkLoanEligibility(user_id, desired_amount));
    }
}
