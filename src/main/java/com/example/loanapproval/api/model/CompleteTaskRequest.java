package com.example.loanapproval.api.model;

import java.util.Map;

/**
 * POST /api/tasks/{taskId}/complete request body wrapper - per backend-plan.md section 3:
 * { "variables": { "officerDecision": "APPROVE"/"REJECT", "officerComments": "...",
 *   "escalateToSeniorManager": true/false } } for the officer task, or
 * { "variables": { "managerDecision": "APPROVE"/"REJECT", "managerComments": "..." } } for the
 * manager task.
 */
public record CompleteTaskRequest(Map<String, Object> variables) {
}
