-- V2: click analytics (brownfield task C2, ADR-004). Additive; V1 untouched.
CREATE SEQUENCE click_event_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE click_event (
    id           BIGINT        NOT NULL PRIMARY KEY,
    short_url_id BIGINT        NOT NULL REFERENCES short_url(id),
    clicked_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    referrer     VARCHAR(2048) NULL,
    user_agent   VARCHAR(512)  NULL,
    ip_hash      VARCHAR(64)   NOT NULL
);

CREATE INDEX ix_click_event_url_time ON click_event (short_url_id, clicked_at);
