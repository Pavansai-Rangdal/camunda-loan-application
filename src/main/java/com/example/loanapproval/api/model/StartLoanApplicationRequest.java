package com.example.loanapproval.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * POST /api/loan-applications request body - applicant intake fields per implementation-plan.md
 * ("Frontend scope" / "Process-start form") and backend-plan.md section 3.
 */
public record StartLoanApplicationRequest(
        @NotBlank String applicantName,
        @NotBlank @Email String applicantEmail,
        @NotNull @Positive Double requestedAmount,
        @NotNull @Positive Double declaredIncome,
        Integer loanTermMonths,
        String loanPurpose
) {
}
