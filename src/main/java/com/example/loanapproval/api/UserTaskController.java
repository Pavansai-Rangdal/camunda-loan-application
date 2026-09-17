package com.example.loanapproval.api;

import com.example.loanapproval.domain.LoanApplication;
import com.example.loanapproval.repository.LoanApplicationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/tasks, GET /api/tasks/{taskId}, POST /api/tasks/{taskId}/complete
 * per backend-plan.md section 3.
 */
@RestController
@RequestMapping("/api/tasks")
public class UserTaskController {

    /**
     * Fixed 1:1 mapping from BPMN user task element name to candidate group, per
     * backend-plan.md section 3 - used as the candidate-group filtering fallback since the v2
     * /user-tasks/search filter is not confirmed to support filtering by candidate group
     * directly.
     */
    private static final Map<String, String> TASK_NAME_TO_CANDIDATE_GROUP = Map.of(
            "Review loan application", "loan-officers",
            "Provide second approval", "senior-managers"
    );

    private final WebClient camunda;
    private final ObjectMapper mapper;
    private final LoanApplicationRepository loanApplicationRepository;

    public UserTaskController(WebClient camundaRestClient, ObjectMapper objectMapper,
                               LoanApplicationRepository loanApplicationRepository) {
        this.camunda = camundaRestClient;
        this.mapper = objectMapper;
        this.loanApplicationRepository = loanApplicationRepository;
    }

    /**
     * GET /api/tasks?candidateGroup=loan-officers|senior-managers&processInstanceKey=...
     *
     * Filters to CREATED state via Camunda, then applies the candidate-group fallback
     * client-side by matching the BPMN element name (each name maps 1:1 to a fixed candidate
     * group in this process).
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> listTasks(
            @RequestParam(required = false) String candidateGroup,
            @RequestParam(required = false) String processInstanceKey) {

        Map<String, Object> filter = new HashMap<>();
        filter.put("state", "CREATED");
        if (processInstanceKey != null && !processInstanceKey.isBlank()) {
            // processInstanceKey is a string in v2 search filters.
            filter.put("processInstanceKey", processInstanceKey);
        }

        Map<String, Object> body = Map.of("filter", filter);

        return camunda.post()
                .uri("/user-tasks/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), ApiErrorHandler::extractError)
                .bodyToMono(JsonNode.class)
                .map(root -> {
                    List<Map<String, Object>> items = new ArrayList<>();
                    JsonNode itemsNode = root.path("items");
                    if (itemsNode.isArray()) {
                        for (JsonNode item : itemsNode) {
                            String name = item.path("name").asText(null);
                            String mappedGroup = TASK_NAME_TO_CANDIDATE_GROUP.get(name);

                            if (candidateGroup != null && !candidateGroup.isBlank()
                                    && !candidateGroup.equalsIgnoreCase(mappedGroup)) {
                                continue;
                            }

                            Map<String, Object> taskSummary = new LinkedHashMap<>();
                            taskSummary.put("userTaskKey", item.path("userTaskKey").asText());
                            taskSummary.put("name", name);
                            taskSummary.put("processDefinitionId", item.path("processDefinitionId").asText(null));
                            taskSummary.put("processInstanceKey", item.path("processInstanceKey").asText(null));
                            taskSummary.put("creationDate", item.path("creationDate").asText(null));
                            taskSummary.put("candidateGroup", mappedGroup);
                            items.add(taskSummary);
                        }
                    }
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("items", items);
                    return result;
                });
    }

    /**
     * GET /api/tasks/{taskId}
     *
     * 3-step pattern (all three mandatory, per new-api skill / backend-plan.md section 3):
     *  1. POST /user-tasks/search by userTaskKey (string) -> processInstanceKey
     *  2. POST /variables/search with scopeKey = processInstanceKey (string) -> Camunda-set vars
     *  3. Enrich with the local loan_applications row (applicant contact fields etc. that job
     *     workers never explicitly returned as Camunda variables), via putIfAbsent so live
     *     Camunda variables win on conflicts. For the Senior Manager task this also surfaces the
     *     Loan Officer's own officerDecision/officerComments.
     */
    @GetMapping(value = "/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getTaskDetail(@PathVariable String taskId) {
        return camunda.post()
                .uri("/user-tasks/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("filter", Map.of("userTaskKey", taskId)))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), ApiErrorHandler::extractError)
                .bodyToMono(JsonNode.class)
                .flatMap(taskResponse -> {
                    JsonNode items = taskResponse.path("items");
                    JsonNode taskNode = (items.isArray() && items.size() > 0) ? items.get(0) : null;

                    Map<String, Object> taskMeta = new LinkedHashMap<>();
                    String processInstanceKey = taskId;
                    if (taskNode != null) {
                        processInstanceKey = taskNode.path("processInstanceKey").asText(taskId);
                        taskMeta.put("userTaskKey", taskNode.path("userTaskKey").asText());
                        taskMeta.put("name", taskNode.path("name").asText(null));
                        taskMeta.put("processDefinitionId", taskNode.path("processDefinitionId").asText(null));
                        taskMeta.put("processInstanceKey", processInstanceKey);
                        taskMeta.put("state", taskNode.path("state").asText(null));
                        taskMeta.put("candidateGroup",
                                TASK_NAME_TO_CANDIDATE_GROUP.get(taskNode.path("name").asText(null)));
                    }

                    String scopeKey = processInstanceKey;
                    return camunda.post()
                            .uri("/variables/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("filter", Map.of("scopeKey", scopeKey)))
                            .retrieve()
                            .onStatus(status -> !status.is2xxSuccessful(), ApiErrorHandler::extractError)
                            .bodyToMono(JsonNode.class)
                            .map(root -> {
                                Map<String, Object> vars = new LinkedHashMap<>();
                                JsonNode varItems = root.path("items");
                                if (varItems.isArray()) {
                                    for (JsonNode item : varItems) {
                                        vars.put(item.path("name").asText(), deserializeValue(item.path("value").asText()));
                                    }
                                }
                                return vars;
                            })
                            .map(vars -> {
                                // Step 3: enrich with the local read model row, Camunda vars win on conflict.
                                loanApplicationRepository.findById(scopeKey).ifPresent(app -> {
                                    vars.putIfAbsent("applicantName", app.getApplicantName());
                                    vars.putIfAbsent("applicantEmail", app.getApplicantEmail());
                                    vars.putIfAbsent("requestedAmount", app.getRequestedAmount());
                                    vars.putIfAbsent("declaredIncome", app.getDeclaredIncome());
                                    vars.putIfAbsent("loanTermMonths", app.getLoanTermMonths());
                                    vars.putIfAbsent("loanPurpose", app.getLoanPurpose());
                                    vars.putIfAbsent("riskCategory", app.getRiskCategory());
                                    vars.putIfAbsent("routingDecision", app.getRoutingDecision());
                                    // Surfaces prior officer input for the Senior Manager's screen.
                                    vars.putIfAbsent("officerDecision", app.getOfficerDecision());
                                    vars.putIfAbsent("officerComments", app.getOfficerComments());
                                });

                                Map<String, Object> result = new LinkedHashMap<>();
                                result.put("task", taskMeta);
                                result.put("variables", vars);
                                return result;
                            });
                });
    }

    /**
     * POST /api/tasks/{taskId}/complete
     *
     * Two fixes always applied (per new-api skill / backend-plan.md section 3):
     *  1. Double-wrapping fix: unwrap the inner "variables" map before forwarding to Camunda.
     *  2. Claim-before-complete: claim with assignee "system" first (errors suppressed - the
     *     task may already be assigned), then complete.
     *
     * Variable names sent here (officerDecision, officerComments, escalateToSeniorManager,
     * managerDecision, managerComments) are exactly the ones the approved BPMN references.
     */
    @SuppressWarnings("unchecked")
    @PostMapping(value = "/{taskId}/complete", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> completeTask(@PathVariable String taskId,
                                                   @RequestBody(required = false) Map<String, Object> requestBody) {
        Map<String, Object> actualVars;
        if (requestBody != null) {
            Object inner = requestBody.get("variables");
            actualVars = (inner instanceof Map) ? (Map<String, Object>) inner : requestBody;
        } else {
            actualVars = Map.of();
        }
        Map<String, Object> completionBody = Map.of("variables", actualVars);

        return camunda.post()
                .uri("/user-tasks/{key}/assignment", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("assignee", "system"))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> Mono.empty())
                .then(
                        camunda.post()
                                .uri("/user-tasks/{key}/completion", taskId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(completionBody)
                                .retrieve()
                                .onStatus(status -> !status.is2xxSuccessful(), ApiErrorHandler::extractError)
                                .bodyToMono(Void.class)
                                .thenReturn(Map.<String, Object>of("status", "completed"))
                );
    }

    /** Unwrap Camunda v2 double-serialized variable values. */
    private Object deserializeValue(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return mapper.readValue(raw, Object.class);
        } catch (Exception e) {
            return raw;
        }
    }
}
