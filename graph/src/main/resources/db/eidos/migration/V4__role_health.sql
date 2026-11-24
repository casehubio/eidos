ALTER TABLE agent_task ADD COLUMN role VARCHAR(100);
CREATE INDEX idx_agent_task_role ON agent_task (agent_id, tenancy_id, role);

CREATE TABLE role_hold_cycle (
    agent_id VARCHAR(255) NOT NULL,
    tenancy_id VARCHAR(255) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    consecutive_cycles INT NOT NULL DEFAULT 0,
    last_evaluated_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (agent_id, tenancy_id, role_name)
);
