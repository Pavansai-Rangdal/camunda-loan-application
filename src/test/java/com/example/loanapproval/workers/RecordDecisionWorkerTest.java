package com.example.loanapproval.workers;

import com.example.loanapproval.repository.LoanApplicationRepository;
import com.example.loanapproval.service.LoanDecisionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Unit tests covering the 4-branch decision-precedence logic that backs RecordDecisionWorker,
 * per backend-plan.md section 2.2:
 *  1. routingDecision == AUTO_APPROVE -> APPROVED
 *  2. routingDecision == AUTO_REJECT  -> REJECTED
 *  3. managerDecision present (takes precedence over officerDecision) -> mapped
 *  4. officerDecision present (no escalation) -> mapped
 *  5. none of the above -> throws (modeling inconsistency)
 */
class RecordDecisionWorkerTest {

    // LoanApplicationRepository is a Spring Data JPA interface (Stage 6) - these tests only
    // exercise the pure precedence logic in resolveFinalStatus(...), which never touches the
    // repository, so a Mockito mock (no live DB/Spring context) is all that's needed to
    // construct the service.
    private final LoanDecisionService service = new LoanDecisionService(mock(LoanApplicationRepository.class));

    @Test
    void autoApproveRoutingWinsRegardlessOfOtherFields() {
        String status = service.resolveFinalStatus("AUTO_APPROVE", "REJECT", "REJECT");
        assertEquals(LoanDecisionService.STATUS_APPROVED, status);
    }

    @Test
    void autoRejectRoutingWinsRegardlessOfOtherFields() {
        String status = service.resolveFinalStatus("AUTO_REJECT", "APPROVE", "APPROVE");
        assertEquals(LoanDecisionService.STATUS_REJECTED, status);
    }

    @Test
    void managerDecisionTakesPrecedenceOverOfficerDecision() {
        // routingDecision is MANUAL_REVIEW / SENIOR_REVIEW (neither AUTO_*), both officer and
        // manager decisions present - manager's decision must win.
        String status = service.resolveFinalStatus("SENIOR_REVIEW", "REJECT", "APPROVE");
        assertEquals(LoanDecisionService.STATUS_REJECTED, status);
    }

    @Test
    void managerApproveMapsToApproved() {
        String status = service.resolveFinalStatus("SENIOR_REVIEW", "APPROVE", null);
        assertEquals(LoanDecisionService.STATUS_APPROVED, status);
    }

    @Test
    void officerDecisionUsedWhenNoManagerDecisionPresent() {
        String status = service.resolveFinalStatus("MANUAL_REVIEW", null, "APPROVE");
        assertEquals(LoanDecisionService.STATUS_APPROVED, status);
    }

    @Test
    void officerRejectMapsToRejected() {
        String status = service.resolveFinalStatus("MANUAL_REVIEW", null, "REJECT");
        assertEquals(LoanDecisionService.STATUS_REJECTED, status);
    }

    @Test
    void noDecisionAvailableThrowsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> service.resolveFinalStatus("MANUAL_REVIEW", null, null));
    }

    @Test
    void unrecognizedManagerDecisionValueThrows() {
        assertThrows(IllegalStateException.class,
                () -> service.resolveFinalStatus("SENIOR_REVIEW", "MAYBE", "APPROVE"));
    }
}
