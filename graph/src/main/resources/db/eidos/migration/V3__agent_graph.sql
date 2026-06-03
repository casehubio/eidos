CREATE TABLE agent_task (
    task_id        VARCHAR(36)       PRIMARY KEY,
    agent_id       VARCHAR(255)      NOT NULL,
    tenancy_id     VARCHAR(255)      NOT NULL,
    capability_tag VARCHAR(255)      NOT NULL,
    task_domain    VARCHAR(255),
    external_ref   TEXT,
    started_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at       TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_task_agent          ON agent_task(agent_id, tenancy_id);
CREATE INDEX idx_task_cap_domain     ON agent_task(agent_id, capability_tag, task_domain, tenancy_id);
CREATE INDEX idx_task_cap_tenant     ON agent_task(capability_tag, task_domain, tenancy_id);

CREATE TABLE agent_outcome (
    task_id            VARCHAR(36)       PRIMARY KEY
                                          REFERENCES agent_task(task_id),
    result             VARCHAR(20)       NOT NULL,
    confidence         DOUBLE PRECISION  NOT NULL
                                          CHECK (confidence BETWEEN 0 AND 1),
    degradation_reason VARCHAR(50),
    observed_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE attestation_ref (
    ref_id             VARCHAR(36)   PRIMARY KEY,
    task_id            VARCHAR(36)   REFERENCES agent_task(task_id),
    agent_id           VARCHAR(255)  NOT NULL,
    tenancy_id         VARCHAR(255)  NOT NULL,
    ledger_entry_hash  VARCHAR(255)  NOT NULL,
    entry_type         VARCHAR(255)  NOT NULL,
    attested_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_attestation UNIQUE (ledger_entry_hash, tenancy_id)
);
CREATE INDEX idx_attest_agent ON attestation_ref(agent_id, tenancy_id);
