package com.example.loanapproval.workers;

import com.example.loanapproval.service.NotificationService;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * job type "send-notification" - one worker, reused by four different BPMN nodes
 * (Task_SendOfficerReminder, Task_SendManagerReminder, Task_SendNotification), disambiguated by
 * the notificationType input variable each BPMN task sets via zeebe:ioMapping, per
 * backend-plan.md section 2.3.
 *
 * Simulated only - no real email/SMS provider call; logs + persists a notification record.
 *
 * Reads the full job variables map (rather than individual {@code @Variable} parameters) because
 * this worker is invoked from boundary events with cancelActivity="false" and must not assume any
 * particular variable is present - it only needs whatever process-scope variables happen to be
 * available at that point (applicantName, requestedAmount, finalStatus, etc.).
 */
@Component
public class SendNotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationWorker.class);

    private final NotificationService notificationService;

    public SendNotificationWorker(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @JobWorker(type = "send-notification")
    public Map<String, Object> handle(ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        Object notificationTypeValue = variables.get("notificationType");
        String notificationType = notificationTypeValue != null ? notificationTypeValue.toString() : null;

        if (notificationType == null || notificationType.isBlank()) {
            throw new RuntimeException("send-notification: notificationType variable is missing - "
                    + "check the zeebe:ioMapping on the calling BPMN task");
        }

        String processInstanceKey = String.valueOf(job.getProcessInstanceKey());
        notificationService.sendNotification(processInstanceKey, notificationType, variables);

        log.info("send-notification: processInstanceKey={} notificationType={}", processInstanceKey, notificationType);

        return Map.of();
    }
}
