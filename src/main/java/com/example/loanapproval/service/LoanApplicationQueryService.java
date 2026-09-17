package com.example.loanapproval.service;

import com.example.loanapproval.domain.LoanApplication;
import com.example.loanapproval.repository.LoanApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Backs GET /api/loan-applications and GET /api/loan-applications/{id} - reads from the
 * PostgreSQL-backed read model only, per backend-plan.md section 3 and db-design.md v1 section 7:
 * the DB is the source for listing/history so React doesn't hammer Camunda directly for
 * reporting views.
 *
 * {@code list(...)} now delegates real pagination/indexed filtering to the repository
 * (findByStatus / findByApplicantEmail / findByStatusAndApplicantEmail, all Pageable-backed by
 * the idx_loan_applications_status / idx_loan_applications_applicant_email indexes) instead of
 * loading every row into memory and paginating with subList.
 */
@Service
public class LoanApplicationQueryService {

    private final LoanApplicationRepository loanApplicationRepository;

    public LoanApplicationQueryService(LoanApplicationRepository loanApplicationRepository) {
        this.loanApplicationRepository = loanApplicationRepository;
    }

    public record Page(java.util.List<LoanApplication> items, int page, int size, long totalElements) {
    }

    public Page list(String status, String applicantEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));

        boolean hasStatus = status != null && !status.isBlank();
        boolean hasEmail = applicantEmail != null && !applicantEmail.isBlank();

        org.springframework.data.domain.Page<LoanApplication> result;
        if (hasStatus && hasEmail) {
            result = loanApplicationRepository.findByStatusAndApplicantEmail(status, applicantEmail, pageable);
        } else if (hasStatus) {
            result = loanApplicationRepository.findByStatus(status, pageable);
        } else if (hasEmail) {
            result = loanApplicationRepository.findByApplicantEmail(applicantEmail, pageable);
        } else {
            result = loanApplicationRepository.findAll(pageable);
        }

        return new Page(result.getContent(), page, size, result.getTotalElements());
    }

    public Optional<LoanApplication> getById(String applicationId) {
        return loanApplicationRepository.findById(applicationId);
    }
}
