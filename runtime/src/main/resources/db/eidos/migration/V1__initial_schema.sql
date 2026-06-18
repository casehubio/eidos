-- Consumer configuration required:
--   quarkus.flyway.locations=classpath:db/eidos/migration,<your-own-locations>

CREATE TABLE agent_descriptor (
    internal_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    agent_id               VARCHAR(255)    NOT NULL,
    tenancy_id             VARCHAR(255)    NOT NULL,
    name                   VARCHAR(255),
    version                VARCHAR(255),
    provider               VARCHAR(255),
    model_family           VARCHAR(255),
    model_version          VARCHAR(255),
    weights_fingerprint    VARCHAR(255),
    domain_vocabulary      TEXT,
    slot_vocabulary        TEXT,
    disposition_vocabulary TEXT,
    axis_vocabularies      TEXT,
    slot                   VARCHAR(255),
    jurisdiction           TEXT,
    data_handling_policy   TEXT,
    briefing               TEXT          NULL,
    disposition            TEXT,
    CONSTRAINT uq_agent UNIQUE (agent_id, tenancy_id)
);
CREATE INDEX idx_agent_descriptor_tenancy_slot ON agent_descriptor(tenancy_id, slot);

CREATE TABLE agent_capability (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descriptor_id       BIGINT        NOT NULL
                            REFERENCES agent_descriptor(internal_id) ON DELETE CASCADE,
    agent_id            VARCHAR(255)  NOT NULL,
    tenancy_id          VARCHAR(255)  NOT NULL,
    name                VARCHAR(255)  NOT NULL,
    quality_hint        DOUBLE PRECISION,
    latency_hint_p50_ms BIGINT,
    cost_hint           VARCHAR(255),
    input_types         TEXT,
    output_types        TEXT,
    tags                TEXT,
    epistemic_domains   TEXT,
    excluded_domains    TEXT
);
CREATE INDEX idx_agent_capability_name ON agent_capability(name);
