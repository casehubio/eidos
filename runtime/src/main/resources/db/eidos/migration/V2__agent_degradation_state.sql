CREATE TABLE agent_degradation_state (
    agent_id            VARCHAR(255)             NOT NULL PRIMARY KEY,
    degradation_reason  VARCHAR(50)              NOT NULL,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
