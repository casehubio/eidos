CREATE TABLE agent_state (
    agent_id     VARCHAR(255)             NOT NULL,
    tenancy_id   VARCHAR(255)             NOT NULL,
    degradation  VARCHAR(50)              NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (agent_id, tenancy_id)
);
