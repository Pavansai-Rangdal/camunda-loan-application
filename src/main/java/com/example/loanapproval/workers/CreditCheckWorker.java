package com.example.loanapproval.workers;

import com.example.loanapproval.service.LoanRiskScoringService;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * job type "credit-check" - bound to Task_CheckCredit ("Check credit-worthiness"),
 * lane System/Automated, per backend-plan.md section 2.1.
 *
 * Simulated risk scoring only - no real external credit bureau call. Sets riskCategory
 * (LOW/MEDIUM/HIGH), the exact input the loan-risk-decision DMN table expects on its
 * Input_RiskCategory column.
 *
 * No {@code @Variable(required = ...)} - not supported in Camunda 8.8. Manual null checks only.
 */
@Component
public class CreditCheckWorker {

    private static final Logger log = LoggerFactory.getLogger(CreditCheckWorker.class);

    private final LoanRiskScoringService riskScoringService;

    public CreditCheckWorker(LoanRiskScoringService riskScoringService) {
        this.riskScoringService = riskScoringService;
    }

    @JobWorker(type = "credit-check")
    public Map<String, Object> handle(ActivatedJob job,
                                       @Variable Double requestedAmount,
                                       @Variable Double declaredIncome) {
        if (requestedAmount == null || declaredIncome == null) {
            // Missing required financial input must surface as a retryable incident (BPMN
            // retries="3"), not a false risk rating - throw rather than default.
            throw new RuntimeException(
                    "credit-check: requestedAmount and declaredIncome are both required, got requestedAmount="
                            + requestedAmount + ", declaredIncome=" + declaredIncome);
        }

        LoanRiskScoringService.RiskAssessment assessment;
        try {
            assessment = riskScoringService.assess(requestedAmount, declaredIncome);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("credit-check: invalid input - " + e.getMessage(), e);
        }

        log.info("credit-check: processInstanceKey={} requestedAmount={} declaredIncome={} -> riskCategory={} simulatedCreditScore={}",
                job.getProcessInstanceKey(), requestedAmount, declaredIncome,
                assessment.riskCategory(), assessment.simulatedCreditScore());

        return Map.of(
                "riskCategory", assessment.riskCategory(),
                "simulatedCreditScore", assessment.simulatedCreditScore()
        );
    }
}
