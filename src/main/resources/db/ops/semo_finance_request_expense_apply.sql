-- Apply once to an existing SEMO database before deploying request-expense linkage.
ALTER TABLE finance_expense
    ADD COLUMN source_finance_request_id BIGINT NULL AFTER entered_by_club_profile_id,
    ADD CONSTRAINT fk_finance_expense_source_request
        FOREIGN KEY (source_finance_request_id) REFERENCES finance_request(finance_request_id),
    ADD CONSTRAINT uk_finance_expense_source_request UNIQUE (source_finance_request_id);
