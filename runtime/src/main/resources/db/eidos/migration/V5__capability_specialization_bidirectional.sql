DROP TABLE IF EXISTS capability_specialization;

CREATE TABLE capability_specialization (
    agent_id        VARCHAR(255) NOT NULL,
    tenancy_id      VARCHAR(255) NOT NULL,
    capability_name VARCHAR(100) NOT NULL,
    domain          VARCHAR(200) NOT NULL,
    signal_type     VARCHAR(20)  NOT NULL,
    signal_count    INT          NOT NULL DEFAULT 0,
    last_recorded   TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (agent_id, tenancy_id, capability_name, domain, signal_type)
);
