-- V1: core table for the greenfield scenario (docs/03 task A4).
-- Analytics table arrives in V2 during the brownfield scenario (ADR-004).
-- Written to run unchanged on PostgreSQL and H2 (MODE=PostgreSQL).

CREATE SEQUENCE shorturl_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE short_url (
    id           BIGINT        NOT NULL PRIMARY KEY,
    code         VARCHAR(16)   NOT NULL,
    long_url     VARCHAR(2048) NOT NULL,
    custom_alias BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE NULL,
    deleted_at   TIMESTAMP WITH TIME ZONE NULL
);

-- Uniqueness is enforced here, not only in application code (ADR-002, task C5/C6).
CREATE UNIQUE INDEX ux_short_url_code ON short_url (code);
