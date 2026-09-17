package com.example.loanapproval.service;

import com.example.loanapproval.domain.LoanApplication;
import com.example.loanapproval.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Used by {@code RecordDecisionWorker} to resolve {@code finalStatus} per the precedence rules
 * in backend-plan.md section 2.2, and to persist the outcome to the (stub, in-memory)
 * {@code loan_applications} read model.
 *
 * Precedence, evaluated in this exact order (matches the approved backend plan):
 *  1. routingDecision == "AUTO_APPROVE"      -> finalStatus = "APPROVED"
 *  2. routingDecision == "AUTO_REJECT"       -> finalStatus = "REJECTED"
 *  3. managerDecision present                -> map APPROVE/REJECT -> finalStatus
 *     (managerDecision takes precedence over officerDecision - a senior review, when it
 *     happens, is the final word)
 *  4. officerDecision present                -> map APPROVE/REJECT -> finalStatus
 *  5. none of the above                      -> throw (retryable) - a modeling inconsistency,
 *     not a business outcome; must not silently produce a finalStatus.
 */
@Service
public class LoanDecisionService {

    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final LoanApplicationRepository loanApplicationRepository;

    public LoanDecisionService(LoanApplicationRepository loanApplicationRepository) {
        this.loanApplicationRepository = loanApplicationRepository;
    }

    /**
     * @throws IllegalStateException if no branch of the precedence logic yields a decision -
     *         the caller (worker) should translate this into a retryable job failure.
     */
    public String resolveFinalStatus(String routingDecision, String managerDecision, String officerDecision) {
        if ("AUTO_APPROVE".equals(routingDecision)) {
            return STATUS_APPROVED;
        }
        if ("AUTO_REJECT".equals(routingDecision)) {
            return STATUS_REJECTED;
        }
        if (managerDecision != null && !managerDecision.isBlank()) {
            return mapDecisionToStatus(managerDecision, "managerDecision");
        }
        if (officerDecision != null && !officerDecision.isBlank()) {
            return mapDecisionToStatus(officerDecision, "officerDecision");
        }
        throw new IllegalStateException(
                "Cannot resolve finalStatus: routingDecision=" + routingDecision
                        + ", managerDecision=" + managerDecision
                        + ", officerDecision=" + officerDecision
                        + " - modeling inconsistency, not a business outcome");
    }

    private String mapDecisionToStatus(String decision, String sourceFieldName) {
        return switch (decision.toUpperCase()) {
            case "APPROVE" -> STATUS_APPROVED;
            case "REJECT" -> STATUS_REJECTED;
            default -> throw new IllegalStateException(
                    "Unrecognized " + sourceFieldName + " value: " + decision + " (expected APPROVE/REJECT)");
        };
    }

    /**
     * Persists the final outcome to the loan_applications read model, updating the row created
     * at process start (looked up by processInstanceKey, per backend-plan.md's chosen linking
     * key). If no row exists yet (e.g. the app was started outside the REST API), creates a
     * minimal one so the decision is not lost.
     */
    public LoanApplication recordDecision(String processInstanceKey,
                                           String routingDecision,
                                           String managerDecision,
                                           String managerComments,
                                           String officerDecision,
                                           String officerComments,
                                           String finalStatus) {
        LoanApplication application = loanApplicationRepository.findById(processInstanceKey)
                .orElseGet(() -> {
                    LoanApplication fresh = new LoanApplication();
                    fresh.setApplicationId(processInstanceKey);
                    fresh.setProcessInstanceKey(processInstanceKey);
                    return fresh;
                });

        application.setRoutingDecision(routingDecision);
        application.setManagerDecision(managerDecision);
        application.setManagerComments(managerComments);
        application.setOfficerDecision(officerDecision);
        application.setOfficerComments(officerComments);
        application.setFinalStatus(finalStatus);
        application.setStatus("COMPLETED");
        application.setCompletedAt(OffsetDateTime.now());
        application.setUpdatedAt(OffsetDateTime.now());

        return loanApplicationRepository.save(application);
    }
}
