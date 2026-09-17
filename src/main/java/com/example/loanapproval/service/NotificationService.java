package com.example.loanapproval.service;

import com.example.loanapproval.domain.NotificationLog;
import com.example.loanapproval.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Used by {@code SendNotificationWorker} to log + persist a simulated notification per
 * notificationType, per backend-plan.md section 2.3. No real email/SMS provider is called.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public static final String TYPE_OFFICER_REMINDER = "OFFICER_REVIEW_REMINDER";
    public static final String TYPE_MANAGER_REMINDER = "SENIOR_MANAGER_REVIEW_REMINDER";
    public static final String TYPE_OUTCOME = "APPLICATION_OUTCOME";

    private final NotificationLogRepository notificationLogRepository;

    public NotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    /**
     * Branches purely on notificationType. Each branch decides which variables to include in
     * the log/record message; unrecognized types are logged as a generic notification rather
     * than failing the job (a new notification type added to the BPMN later shouldn't cause a
     * hard worker failure).
     */
    public NotificationLog sendNotification(String processInstanceKey, String notificationType,
                                             Map<String, Object> variables) {
        String recipientGroup;
        String message;

        if (TYPE_OFFICER_REMINDER.equals(notificationType)) {
            recipientGroup = "loan-officers";
            message = "Reminder: loan application (process " + processInstanceKey
                    + ") for applicant '" + variables.get("applicantName")
                    + "' has been awaiting Loan Officer review for over 2 days.";
        } else if (TYPE_MANAGER_REMINDER.equals(notificationType)) {
            recipientGroup = "senior-managers";
            message = "Reminder: loan application (process " + processInstanceKey
                    + ") for applicant '" + variables.get("applicantName")
                    + "' has been awaiting Senior Manager second approval for over 2 days.";
        } else if (TYPE_OUTCOME.equals(notificationType)) {
            recipientGroup = "applicant";
            message = "Loan application outcome for '" + variables.get("applicantName")
                    + "' (process " + processInstanceKey + "): " + variables.get("finalStatus")
                    + ". Contact: " + variables.get("applicantEmail");
        } else {
            recipientGroup = "unknown";
            message = "Notification (unrecognized type '" + notificationType + "') for process "
                    + processInstanceKey + ": " + variables;
        }

        log.info("[notification] type={} recipientGroup={} processInstanceKey={} message={}",
                notificationType, recipientGroup, processInstanceKey, message);

        NotificationLog entry = new NotificationLog();
        entry.setId(UUID.randomUUID().toString());
        entry.setProcessInstanceKey(processInstanceKey);
        entry.setNotificationType(notificationType);
        entry.setRecipientGroup(recipientGroup);
        entry.setMessage(message);
        entry.setSentAt(OffsetDateTime.now());

        return notificationLogRepository.save(entry);
    }
}
