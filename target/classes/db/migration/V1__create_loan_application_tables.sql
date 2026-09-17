-- Loan Approval Application - initial schema (db-design.md v1, section 3 & 5)
-- Greenfield migration: no prior tables, no baseline-on-migrate required.

CREATE TABLE loan_applications (
    id                      VARCHAR(64)   PRIMARY KEY,
    application_id          VARCHAR(64)   NOT NULL,
    process_definition_id   VARCHAR(255),
    process_instance_key    VARCHAR(64)   NOT NULL,

    applicant_name          VARCHAR(255)  NOT NULL,
    applicant_email         VARCHAR(255)  NOT NULL,
    requested_amount        NUMERIC(14,2) NOT NULL,
    declared_income         NUMERIC(14,2) NOT NULL,
    loan_term_months        INTEGER,
    loan_purpose            VARCHAR(255),

    risk_category           VARCHAR(16),
    simulated_credit_score  NUMERIC(6,2),
    routing_decision        VARCHAR(16),

    officer_decision        VARCHAR(16),
    officer_comments        TEXT,
    manager_decision        VARCHAR(16),
    manager_comments        TEXT,

    final_status            VARCHAR(16),
    status                  VARCHAR(16)   NOT NULL DEFAULT 'SUBMITTED',

    submitted_at            TIMESTAMPTZ   NOT NULL,
    updated_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_loan_applications_application_id UNIQUE (application_id),
    CONSTRAINT uq_loan_applications_process_instance_key UNIQUE (process_instance_key)
);

CREATE INDEX idx_loan_applications_status ON loan_applications (status);
CREATE INDEX idx_loan_applications_process_instance_key ON loan_applications (process_instance_key);
CREATE INDEX idx_loan_applications_applicant_email ON loan_applications (applicant_email);

CREATE TABLE notification_logs (
    id                      VARCHAR(64)  PRIMARY KEY,
    loan_application_id     VARCHAR(64)  NOT NULL,
    process_instance_key    VARCHAR(64)  NOT NULL,
    notification_type       VARCHAR(32)  NOT NULL,
    recipient_group         VARCHAR(64)  NOT NULL,
    message                 TEXT         NOT NULL,
    sent_at                 TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_notification_logs_loan_application
        FOREIGN KEY (loan_application_id) REFERENCES loan_applications (id)
);

CREATE INDEX idx_notification_logs_loan_application_id ON notification_logs (loan_application_id);
