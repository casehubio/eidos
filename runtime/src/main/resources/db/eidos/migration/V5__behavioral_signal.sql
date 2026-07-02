DROP TABLE IF EXISTS behavioral_signal;

CREATE TABLE behavioral_signal (
    agent_id        VARCHAR(255) NOT NULL,
    tenancy_id      VARCHAR(255) NOT NULL,
    capability_name VARCHAR(100) NOT NULL,
    qualifier       VARCHAR(200) NOT NULL,
    signal_type     VARCHAR(20)  NOT NULL,
    signal_count    INT          NOT NULL DEFAULT 0,
    last_recorded   TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (agent_id, tenancy_id, capability_name, qualifier, signal_type)
);
