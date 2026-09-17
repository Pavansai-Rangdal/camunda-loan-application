package com.example.loanapproval.api;

import com.example.loanapproval.api.model.LoanApplicationResponse;
import com.example.loanapproval.api.model.StartLoanApplicationRequest;
import com.example.loanapproval.domain.LoanApplication;
import com.example.loanapproval.repository.LoanApplicationRepository;
import com.example.loanapproval.service.LoanApplicationQueryService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * POST /api/loan-applications, GET /api/loan-applications, GET /api/loan-applications/{id}
 * per backend-plan.md section 3.
 */
@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationController {

    private static final String PROCESS_DEFINITION_ID = "loan-approval-process";

    private final WebClient camunda;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationQueryService queryService;

    public LoanApplicationController(WebClient camundaRestClient,
                                      LoanApplicationRepository loanApplicationRepository,
                                      LoanApplicationQueryService queryService) {
        this.camunda = camundaRestClient;
        this.loanApplicationRepository = loanApplicationRepository;
        this.queryService = queryService;
    }

    /**
     * Starts a new loan-approval-process instance via POST /v2/process-instances and creates a
     * row in the local read model (status SUBMITTED) so the application is immediately listable
     * without waiting on any worker to run.
     *
     * processInstanceKey comes back from Camunda as a string - never cast to Number/Long
     * directly.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LoanApplicationResponse>> startApplication(@Valid @RequestBody StartLoanApplicationRequest req) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("applicantName", req.applicantName());
        variables.put("applicantEmail", req.applicantEmail());
        variables.put("requestedAmount", req.requestedAmount());
        variables.put("declaredIncome", req.declaredIncome());
        if (req.loanTermMonths() != null) {
            variables.put("loanTermMonths", req.loanTermMonths());
        }
        if (req.loanPurpose() != null) {
            variables.put("loanPurpose", req.loanPurpose());
        }

        Map<String, Object> body = Map.of(
                "processDefinitionId", PROCESS_DEFINITION_ID,
                "variables", variables
        );

        return camunda.post()
                .uri("/process-instances")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), ApiErrorHandler::extractError)
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    // processInstanceKey is returned as a JSON string per Camunda v2 - never cast to Number.
                    String processInstanceKey = response.path("processInstanceKey").asText();

                    LoanApplication application = new LoanApplication();
                    application.setApplicationId(processInstanceKey);
                    application.setProcessInstanceKey(processInstanceKey);
                    application.setProcessDefinitionId(PROCESS_DEFINITION_ID);
                    application.setApplicantName(req.applicantName());
                    application.setApplicantEmail(req.applicantEmail());
                    application.setRequestedAmount(req.requestedAmount());
                    application.setDeclaredIncome(req.declaredIncome());
                    application.setLoanTermMonths(req.loanTermMonths());
                    application.setLoanPurpose(req.loanPurpose());
                    application.setStatus("SUBMITTED");
                    application.setSubmittedAt(OffsetDateTime.now());
                    application.setUpdatedAt(OffsetDateTime.now());

                    loanApplicationRepository.save(application);

                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(LoanApplicationResponse.from(application, "ACTIVE"));
                });
    }

    /**
     * Paginated list/search of applications - reads from the local read model only, no Camunda
     * call, per backend-plan.md section 3.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listApplications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String applicantEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LoanApplicationQueryService.Page result = queryService.list(status, applicantEmail, page, size);

        List<LoanApplicationResponse> items = result.items().stream()
                .map(a -> LoanApplicationResponse.from(a, a.getFinalStatus() != null ? "COMPLETED" : "ACTIVE"))
                .toList();

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("items", items);
        responseBody.put("page", result.page());
        responseBody.put("size", result.size());
        responseBody.put("totalElements", result.totalElements());

        return ResponseEntity.ok(responseBody);
    }

    /**
     * Application detail + current stage. Primarily a DB read; optionally cross-checks
     * GET /v2/process-instances/{key} for "state" (ACTIVE/COMPLETED) if the DB record doesn't
     * already have a finalStatus - best-effort only, DB remains the source for business fields.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LoanApplicationResponse>> getApplication(@PathVariable String id) {
        LoanApplication application = queryService.getById(id)
                .orElseThrow(() -> new NoSuchElementException("No loan application found with id " + id));

        if (application.getFinalStatus() != null) {
            return Mono.just(ResponseEntity.ok(LoanApplicationResponse.from(application, "COMPLETED")));
        }

        return camunda.get()
                .uri("/process-instances/{key}", id)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), ApiErrorHandler::extractError)
                .bodyToMono(JsonNode.class)
                .map(json -> json.path("state").asText("ACTIVE"))
                .map(state -> ResponseEntity.ok(LoanApplicationResponse.from(application, state)))
                .onErrorResume(e -> Mono.just(ResponseEntity.ok(LoanApplicationResponse.from(application, "ACTIVE"))));
    }
}
