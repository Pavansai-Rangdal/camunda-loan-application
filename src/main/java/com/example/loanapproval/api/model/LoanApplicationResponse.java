package com.example.loanapproval.api.model;

import com.example.loanapproval.domain.LoanApplication;

import java.time.OffsetDateTime;

/**
 * Response DTO for both the list row shape (GET /api/loan-applications) and the detail shape
 * (GET /api/loan-applications/{id}) - per backend-plan.md section 3. Keeps the API contract
 * separate from the internal domain POJO.
 */
public record LoanApplicationResponse(
        String applicationId,
        String processDefinitionId,
        String processInstanceKey,
        String applicantName,
        String applicantEmail,
        Double requestedAmount,
        Double declaredIncome,
        Integer loanTermMonths,
        String loanPurpose,
        String riskCategory,
        String routingDecision,
        String officerDecision,
        String officerComments,
        String managerDecision,
        String managerComments,
        String finalStatus,
        String status,
        String currentStage,
        OffsetDateTime submittedAt,
        OffsetDateTime updatedAt,
        OffsetDateTime completedAt
) {
    public static LoanApplicationResponse from(LoanApplication a, String currentStage) {
        return new LoanApplicationResponse(
                a.getApplicationId(),
                a.getProcessDefinitionId(),
                a.getProcessInstanceKey(),
                a.getApplicantName(),
                a.getApplicantEmail(),
                a.getRequestedAmount(),
                a.getDeclaredIncome(),
                a.getLoanTermMonths(),
                a.getLoanPurpose(),
                a.getRiskCategory(),
                a.getRoutingDecision(),
                a.getOfficerDecision(),
                a.getOfficerComments(),
                a.getManagerDecision(),
                a.getManagerComments(),
                a.getFinalStatus(),
                a.getStatus(),
                currentStage,
                a.getSubmittedAt(),
                a.getUpdatedAt(),
                a.getCompletedAt()
        );
    }
}
