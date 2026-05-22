CREATE TABLE agent_descriptor (
    agent_id               VARCHAR(255)  NOT NULL PRIMARY KEY,
    tenancy_id             VARCHAR(255)  NOT NULL,
    name                   VARCHAR(255),
    version                VARCHAR(255),
    provider               VARCHAR(255),
    model_family           VARCHAR(255),
    model_version          VARCHAR(255),
    weights_fingerprint    VARCHAR(255),
    domain_vocabulary      VARCHAR(255),
    slot_vocabulary        VARCHAR(255),
    disposition_vocabulary VARCHAR(255),
    slot                   VARCHAR(255),
    jurisdiction           VARCHAR(255),
    data_handling_policy   TEXT,
    disposition            TEXT
);

CREATE TABLE agent_capability (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    agent_id            VARCHAR(255)  NOT NULL
                            REFERENCES agent_descriptor(agent_id) ON DELETE CASCADE,
    name                VARCHAR(255)  NOT NULL,
    quality_hint        DOUBLE PRECISION,
    latency_hint_p50_ms BIGINT,
    cost_hint           VARCHAR(255),
    input_types         TEXT,
    output_types        TEXT,
    tags                TEXT,
    epistemic_domains   TEXT
);

CREATE INDEX idx_agent_descriptor_tenancy_slot ON agent_descriptor(tenancy_id, slot);
CREATE INDEX idx_agent_capability_name         ON agent_capability(name);
