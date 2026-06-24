CREATE TABLE capability_specialization (
    agent_id        VARCHAR(255) NOT NULL,
    tenancy_id      VARCHAR(255) NOT NULL,
    capability_name VARCHAR(100) NOT NULL,
    domain          VARCHAR(200) NOT NULL,
    decline_count   INT          NOT NULL DEFAULT 0,
    last_declined   TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (agent_id, tenancy_id, capability_name, domain)
);
