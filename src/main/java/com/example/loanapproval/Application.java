package com.example.loanapproval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Entry point for the Loan Approval backend.
 *
 * {@code @EnableJpaAuditing} backs the {@code @CreatedDate}/{@code @LastModifiedDate} columns on
 * {@code LoanApplication} (created_at/updated_at), per db-design.md v1 section 3.1. Spring Data's
 * default auditing DateTimeProvider produces {@code LocalDateTime}, which cannot be converted to
 * the entity's {@code OffsetDateTime} fields ("Cannot convert unsupported date type
 * java.time.LocalDateTime to java.time.OffsetDateTime") - an explicit {@code OffsetDateTime}
 * DateTimeProvider bean is required, per the auditingDateTimeProvider bean name Spring Data looks
 * for by convention.
 */
@SpringBootApplication
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
