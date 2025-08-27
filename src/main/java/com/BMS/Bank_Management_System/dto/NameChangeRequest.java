package com.BMS.Bank_Management_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NameChangeRequest {
    private String newUsername;                    // REQUIRED: New legal name
    private String reasonForChange;                // REQUIRED: "DIVORCE", "MARRIAGE", "LEGAL_CHANGE", "CORRECTION"
    private String documentType;                   // REQUIRED: "COURT_ORDER", "MARRIAGE_CERT", "DIVORCE_DECREE", "GOVERNMENT_ID"
    private String documentNumber;                 // REQUIRED: Official document number/ID
    private String issuingAuthority;               // REQUIRED: Government body/court that issued it
    private String issueDate;                      // REQUIRED: Date document was issued (YYYY-MM-DD)
    private String expiryDate;                     // OPTIONAL: Expiry date if applicable
    private String additionalNotes;                // OPTIONAL: Additional context or notes
    private String verificationMethod;             // REQUIRED: How it was verified (STAFF_VERIFIED, ADMIN_VERIFIED, THIRD_PARTY)
    private String verifiedBy;                     // REQUIRED: Username of staff/admin who verified
    private String verificationDate;               // REQUIRED: Date of verification (YYYY-MM-DD)
}
