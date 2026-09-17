package com.example.loanapproval.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * JPA entity mirroring one loan-approval-process instance, per db-design.md v1 section 3.1.
 *
 * Primary key strategy (db-design.md v1 section 5): a natural VARCHAR(64) string key, NOT a
 * surrogate BIGSERIAL/UUID. The process instance key is already globally unique per application
 * and every caller already has it in hand (worker payload, REST path parameter, DB lookup), so
 * introducing a second, Camunda-unrelated key would add no benefit.
 *
 * The design calls for three columns holding the same linking value ({@code id} - the PK,
 * {@code application_id}, and {@code process_instance_key}) for readability/explicitness even
 * though they are identical today. To avoid touching every existing call site that only calls
 * {@link #setApplicationId(String)} (the controller, {@code LoanDecisionService}'s fallback
 * branch, etc.), {@link #setApplicationId(String)} keeps its original signature but now also
 * synchronizes the {@code id} field (the actual {@code @Id}) - no caller needed to change.
 */
@Entity
@Table(name = "loan_applications")
@EntityListeners(AuditingEntityListener.class)
public class LoanApplication {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "application_id", length = 64, unique = true, nullable = false)
    private String applicationId; // == processInstanceKey, as a string

    @Column(name = "process_definition_id", length = 255)
    private String processDefinitionId;

    @Column(name = "process_instance_key", length = 64, unique = true, nullable = false)
    private String processInstanceKey;

    @Column(name = "applicant_name", length = 255, nullable = false)
    private String applicantName;

    @Column(name = "applicant_email", length = 255, nullable = false)
    private String applicantEmail;

    @Column(name = "requested_amount", columnDefinition = "NUMERIC(14,2)", nullable = false)
    private Double requestedAmount;

    @Column(name = "declared_income", columnDefinition = "NUMERIC(14,2)", nullable = false)
    private Double declaredIncome;

    @Column(name = "loan_term_months")
    private Integer loanTermMonths;

    @Column(name = "loan_purpose", length = 255)
    private String loanPurpose;

    @Column(name = "risk_category", length = 16)
    private String riskCategory;

    @Column(name = "simulated_credit_score", columnDefinition = "NUMERIC(6,2)")
    private Double simulatedCreditScore;

    @Column(name = "routing_decision", length = 16)
    private String routingDecision;

    @Column(name = "officer_decision", length = 16)
    private String officerDecision;

    @Column(name = "officer_comments", columnDefinition = "TEXT")
    private String officerComments;

    @Column(name = "manager_decision", length = 16)
    private String managerDecision;

    @Column(name = "manager_comments", columnDefinition = "TEXT")
    private String managerComments;

    @Column(name = "final_status", length = 16)
    private String finalStatus; // null until Task_RecordDecision runs

    @Column(name = "status", length = 16, nullable = false)
    private String status = "SUBMITTED"; // SUBMITTED -> IN_PROGRESS -> COMPLETED, matches column DEFAULT

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public LoanApplication() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        this.id = applicationId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessInstanceKey() {
        return processInstanceKey;
    }

    public void setProcessInstanceKey(String processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public Double getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(Double requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public Double getDeclaredIncome() {
        return declaredIncome;
    }

    public void setDeclaredIncome(Double declaredIncome) {
        this.declaredIncome = declaredIncome;
    }

    public Integer getLoanTermMonths() {
        return loanTermMonths;
    }

    public void setLoanTermMonths(Integer loanTermMonths) {
        this.loanTermMonths = loanTermMonths;
    }

    public String getLoanPurpose() {
        return loanPurpose;
    }

    public void setLoanPurpose(String loanPurpose) {
        this.loanPurpose = loanPurpose;
    }

    public String getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory;
    }

    public Double getSimulatedCreditScore() {
        return simulatedCreditScore;
    }

    public void setSimulatedCreditScore(Double simulatedCreditScore) {
        this.simulatedCreditScore = simulatedCreditScore;
    }

    public String getRoutingDecision() {
        return routingDecision;
    }

    public void setRoutingDecision(String routingDecision) {
        this.routingDecision = routingDecision;
    }

    public String getOfficerDecision() {
        return officerDecision;
    }

    public void setOfficerDecision(String officerDecision) {
        this.officerDecision = officerDecision;
    }

    public String getOfficerComments() {
        return officerComments;
    }

    public void setOfficerComments(String officerComments) {
        this.officerComments = officerComments;
    }

    public String getManagerDecision() {
        return managerDecision;
    }

    public void setManagerDecision(String managerDecision) {
        this.managerDecision = managerDecision;
    }

    public String getManagerComments() {
        return managerComments;
    }

    public void setManagerComments(String managerComments) {
        this.managerComments = managerComments;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
