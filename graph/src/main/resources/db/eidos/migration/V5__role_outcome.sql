CREATE TABLE role_outcome (
    task_id VARCHAR(36) NOT NULL,
    role_quality DOUBLE PRECISION NOT NULL CHECK (role_quality BETWEEN 0 AND 1),
    dimension VARCHAR(100),
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (task_id),
    FOREIGN KEY (task_id) REFERENCES agent_task(task_id)
);
