package com.example.loanapproval.workers;

import com.example.loanapproval.service.LoanRiskScoringService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the risk-bucketing thresholds against the loan-risk-decision DMN's expected
 * riskCategory values (LOW / MEDIUM / HIGH), per backend-plan.md section 2.1.
 */
class LoanRiskScoringServiceTest {

    private final LoanRiskScoringService service = new LoanRiskScoringService();

    @Test
    void lowRatioIsBucketedLow() {
        // ratio = 5000 / 100000 = 0.05 <= 0.15 -> LOW
        var result = service.assess(5000.0, 100000.0);
        assertEquals(LoanRiskScoringService.RISK_LOW, result.riskCategory());
    }

    @Test
    void ratioAtLowBoundaryIsBucketedLow() {
        // ratio = 15000 / 100000 = 0.15 == boundary -> LOW (inclusive)
        var result = service.assess(15000.0, 100000.0);
        assertEquals(LoanRiskScoringService.RISK_LOW, result.riskCategory());
    }

    @Test
    void midRatioIsBucketedMedium() {
        // ratio = 30000 / 100000 = 0.30 -> MEDIUM
        var result = service.assess(30000.0, 100000.0);
        assertEquals(LoanRiskScoringService.RISK_MEDIUM, result.riskCategory());
    }

    @Test
    void ratioAtMediumBoundaryIsBucketedMedium() {
        // ratio = 40000 / 100000 = 0.40 == boundary -> MEDIUM (inclusive)
        var result = service.assess(40000.0, 100000.0);
        assertEquals(LoanRiskScoringService.RISK_MEDIUM, result.riskCategory());
    }

    @Test
    void highRatioIsBucketedHigh() {
        // ratio = 60000 / 100000 = 0.60 -> HIGH
        var result = service.assess(60000.0, 100000.0);
        assertEquals(LoanRiskScoringService.RISK_HIGH, result.riskCategory());
    }

    @Test
    void missingRequestedAmountThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.assess(null, 100000.0));
    }

    @Test
    void missingDeclaredIncomeThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.assess(5000.0, null));
    }

    @Test
    void nonPositiveDeclaredIncomeThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.assess(5000.0, 0.0));
    }

    @Test
    void simulatedCreditScoreStaysWithinConventionalRange() {
        var result = service.assess(90000.0, 100000.0); // ratio 0.9 -> 850 - (0.9 * 500) = 400
        assertEquals(400.0, result.simulatedCreditScore(), 0.0001);
    }

    @Test
    void simulatedCreditScoreClampsAtFloorForExtremeRatios() {
        var result = service.assess(200000.0, 100000.0); // ratio 2.0 -> would be -150, clamped to 300
        assertEquals(300.0, result.simulatedCreditScore(), 0.0001);
    }
}
