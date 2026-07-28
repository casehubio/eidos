CREATE TABLE disposition_signal (
    agent_id        VARCHAR(255) NOT NULL,
    tenancy_id      VARCHAR(255) NOT NULL,
    function_term   VARCHAR(30)  NOT NULL,
    count           INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (agent_id, tenancy_id, function_term)
);
