package com.example.loanapproval.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * JPA entity for a simulated notification record (audit trail only - no real email/SMS
 * integration, per backend-plan.md section 2.3 and 6). Per db-design.md v1 section 3.2.
 *
 * {@code id} is already generated as {@code UUID.randomUUID().toString()} by
 * {@code NotificationService.sendNotification(...)} - kept as the natural VARCHAR(64) PK, no
 * {@code @GeneratedValue}.
 *
 * The design adds a {@code loan_application_id} FK column (-> loan_applications.id) that is
 * always the same value as {@code processInstanceKey} in this app's current model. Per the
 * design's own consistency note, no new public field/setter is introduced for it: it is kept in
 * sync automatically whenever {@link #setProcessInstanceKey(String)} is called (the only place
 * {@code NotificationService} already sets this value), so no call site needs to change.
 */
@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "loan_application_id", length = 64, nullable = false)
    private String loanApplicationId;

    @Column(name = "process_instance_key", length = 64, nullable = false)
    private String processInstanceKey;

    @Column(name = "notification_type", length = 32, nullable = false)
    private String notificationType; // OFFICER_REVIEW_REMINDER | SENIOR_MANAGER_REVIEW_REMINDER | APPLICATION_OUTCOME

    @Column(name = "recipient_group", length = 64, nullable = false)
    private String recipientGroup;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    public NotificationLog() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLoanApplicationId() {
        return loanApplicationId;
    }

    public void setLoanApplicationId(String loanApplicationId) {
        this.loanApplicationId = loanApplicationId;
    }

    public String getProcessInstanceKey() {
        return processInstanceKey;
    }

    public void setProcessInstanceKey(String processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
        this.loanApplicationId = processInstanceKey;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getRecipientGroup() {
        return recipientGroup;
    }

    public void setRecipientGroup(String recipientGroup) {
        this.recipientGroup = recipientGroup;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
