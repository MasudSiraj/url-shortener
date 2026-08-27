-- V3: dedicated sequence for the optional sequence-based code generator (task C7, ADR-002).
-- Kept separate from shorturl_seq (entity ids) so switching generators never consumes ids.
-- Starts at 62^4 so every encoded code is at least 5 characters.
CREATE SEQUENCE short_code_seq START WITH 14776336 INCREMENT BY 1;
