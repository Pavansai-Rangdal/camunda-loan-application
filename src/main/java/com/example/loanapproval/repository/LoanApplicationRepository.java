package com.example.loanapproval.repository;

import com.example.loanapproval.domain.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link LoanApplication}, per db-design.md v1 section 7.
 *
 * Query methods match exactly what the service layer already called against the Stage 4
 * in-memory stub (save, findById, findAll, findByStatus, findByApplicantEmail) plus real
 * pagination (Page/Pageable) on the list-oriented methods, replacing the manual subList
 * pagination that used to live in {@code LoanApplicationQueryService}.
 */
@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {

    Page<LoanApplication> findByStatus(String status, Pageable pageable);

    Page<LoanApplication> findByApplicantEmail(String applicantEmail, Pageable pageable);

    Page<LoanApplication> findByStatusAndApplicantEmail(String status, String applicantEmail, Pageable pageable);

    // Retained (non-paginated) for any internal caller that needs the full email match set.
    List<LoanApplication> findByApplicantEmail(String applicantEmail);
}
