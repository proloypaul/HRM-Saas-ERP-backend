-- V1__baseline_schema.md (PostgreSQL Flyway Migration)
-- Baseline schema setup for HRM SaaS ERP

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Baseline table to verify migration execution and audit structure
CREATE TABLE IF NOT EXISTS system_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM'
);

INSERT INTO system_audit_logs (event_type, description)
VALUES ('SYSTEM_INIT', 'HRM SaaS ERP Database initialized successfully with baseline Flyway migration.');
