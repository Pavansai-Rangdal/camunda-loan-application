package com.example.loanapproval.service;

import org.springframework.stereotype.Service;

/**
 * Pure, independently-testable logic used by {@code CreditCheckWorker} to turn
 * requestedAmount/declaredIncome into the {@code riskCategory} bucket
 * ("LOW" / "MEDIUM" / "HIGH") that the {@code loan-risk-decision} DMN table expects on its
 * Input_RiskCategory column, per backend-plan.md section 2.1.
 *
 * No real credit bureau call - this is a simulated, deterministic scoring rule over the
 * debt-to-request ratio (requestedAmount / declaredIncome). Thresholds are chosen to produce a
 * sensible mix of outcomes against the DMN's existing rules (HIGH -> AUTO_REJECT, LOW with
 * amount &lt;= 10000 -> AUTO_APPROVE, etc.) without changing the DMN itself.
 */
@Service
public class LoanRiskScoringService {

    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";

    // ratio = requestedAmount / declaredIncome
    private static final double LOW_RISK_MAX_RATIO = 0.15;
    private static final double MEDIUM_RISK_MAX_RATIO = 0.40;

    public record RiskAssessment(String riskCategory, double simulatedCreditScore, double ratio) {
    }

    /**
     * @throws IllegalArgumentException if requestedAmount or declaredIncome are missing/invalid -
     *         the caller (worker) is expected to translate this into a retryable job failure
     *         rather than silently defaulting a risk rating.
     */
    public RiskAssessment assess(Double requestedAmount, Double declaredIncome) {
        if (requestedAmount == null || declaredIncome == null) {
            throw new IllegalArgumentException(
                    "requestedAmount and declaredIncome are both required to compute riskCategory");
        }
        if (requestedAmount <= 0) {
            throw new IllegalArgumentException("requestedAmount must be a positive number");
        }
        if (declaredIncome <= 0) {
            throw new IllegalArgumentException("declaredIncome must be a positive number");
        }

        double ratio = requestedAmount / declaredIncome;
        String riskCategory = bucketRatio(ratio);

        // Diagnostic-only pseudo credit score (not required by any gateway or DMN rule),
        // clamped to a conventional 300-850 range, decreasing as the ratio increases.
        double simulatedCreditScore = clamp(850.0 - (ratio * 500.0), 300.0, 850.0);

        return new RiskAssessment(riskCategory, simulatedCreditScore, ratio);
    }

    private String bucketRatio(double ratio) {
        if (ratio <= LOW_RISK_MAX_RATIO) {
            return RISK_LOW;
        }
        if (ratio <= MEDIUM_RISK_MAX_RATIO) {
            return RISK_MEDIUM;
        }
        return RISK_HIGH;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
