package com.example.loanapproval.workers;

import com.example.loanapproval.service.LoanDecisionService;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * job type "record-decision" - bound to Task_RecordDecision ("Record loan decision"), reached
 * after Gateway_MergeDecision, per backend-plan.md section 2.2.
 *
 * Owns the decision-precedence logic that is not modeled as BPMN gateways - the approved BPMN's
 * Gateway_FinalOutcome condition reads finalStatus, which only this worker sets. See
 * {@link LoanDecisionService#resolveFinalStatus} for the exact 4-branch precedence order.
 *
 * No {@code @Variable(required = ...)} - not supported in Camunda 8.8. All decision/comment
 * variables are optional here by design (only one branch of officer/manager applies per
 * instance) - manual null checks only.
 */
@Component
public class RecordDecisionWorker {

    private static final Logger log = LoggerFactory.getLogger(RecordDecisionWorker.class);

    private final LoanDecisionService loanDecisionService;

    public RecordDecisionWorker(LoanDecisionService loanDecisionService) {
        this.loanDecisionService = loanDecisionService;
    }

    @JobWorker(type = "record-decision")
    public Map<String, Object> handle(ActivatedJob job,
                                       @Variable String routingDecision,
                                       @Variable String managerDecision,
                                       @Variable String managerComments,
                                       @Variable String officerDecision,
                                       @Variable String officerComments) {
        String processInstanceKey = String.valueOf(job.getProcessInstanceKey());

        final String finalStatus;
        try {
            finalStatus = loanDecisionService.resolveFinalStatus(routingDecision, managerDecision, officerDecision);
        } catch (IllegalStateException e) {
            // Guarded defensively per plan: should not happen given the BPMN's gateway
            // coverage, but a modeling inconsistency must not silently produce a finalStatus -
            // surface as a retryable incident instead.
            throw new RuntimeException("record-decision: " + e.getMessage(), e);
        }

        loanDecisionService.recordDecision(processInstanceKey, routingDecision, managerDecision,
                managerComments, officerDecision, officerComments, finalStatus);

        log.info("record-decision: processInstanceKey={} routingDecision={} managerDecision={} officerDecision={} -> finalStatus={}",
                processInstanceKey, routingDecision, managerDecision, officerDecision, finalStatus);

        return Map.of("finalStatus", finalStatus);
    }
}
