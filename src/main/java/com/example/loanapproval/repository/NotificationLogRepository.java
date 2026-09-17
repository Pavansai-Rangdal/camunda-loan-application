package com.example.loanapproval.repository;

import com.example.loanapproval.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link NotificationLog}, per db-design.md v1 section 7.
 *
 * {@code findByProcessInstanceKeyOrderBySentAtDesc} replaces the Stage 4 in-memory stub's
 * {@code findByProcessInstanceKey} linear scan with an indexed FK lookup
 * (idx_notification_logs_loan_application_id / process_instance_key is unique per application),
 * ordered so the most recent notification comes first.
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {

    List<NotificationLog> findByProcessInstanceKeyOrderBySentAtDesc(String processInstanceKey);
}
