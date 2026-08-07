CREATE TABLE IF NOT EXISTS api_key (
    api_key VARCHAR(64) PRIMARY KEY,
    label VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (api_key) REFERENCES api_key(api_key) ON DELETE CASCADE,
    INDEX idx_api_key (api_key)
);

CREATE TABLE IF NOT EXISTS period_config (
    period_number TINYINT PRIMARY KEY,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    category VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    location VARCHAR(200),
    teacher VARCHAR(100),
    day_of_week TINYINT NOT NULL,
    start_period TINYINT NOT NULL,
    end_period TINYINT NOT NULL,
    weeks JSON NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (schedule_id) REFERENCES schedule(id) ON DELETE CASCADE,
    INDEX idx_schedule_day (schedule_id, day_of_week)
);

CREATE TABLE IF NOT EXISTS todo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL,
    ddl DATETIME NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    recurring_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (api_key) REFERENCES api_key(api_key) ON DELETE CASCADE,
    INDEX idx_api_key (api_key),
    INDEX idx_ddl (ddl),
    INDEX idx_completed (completed),
    INDEX idx_todo_recurring (recurring_id)
);

CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key VARCHAR(64) NOT NULL,
    schedule_id BIGINT,
    title VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (api_key) REFERENCES api_key(api_key) ON DELETE CASCADE,
    INDEX idx_ai_conv_api_key (api_key)
);

CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT,
    action_type VARCHAR(32),
    action_status VARCHAR(16),
    action_data JSON,
    data_fingerprint VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE,
    INDEX idx_ai_msg_conv (conversation_id)
);

CREATE TABLE IF NOT EXISTS ai_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    api_key VARCHAR(64) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    action_status VARCHAR(16) NOT NULL,
    action_data JSON,
    data_fingerprint VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES ai_message(id) ON DELETE CASCADE,
    INDEX idx_ai_action_msg (message_id),
    INDEX idx_ai_action_pending (api_key, scope, action_status)
);

CREATE TABLE IF NOT EXISTS recurring_todo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL,
    frequency VARCHAR(16) NOT NULL,
    day_of_week TINYINT,
    day_of_month TINYINT,
    trigger_time TIME,
    script TEXT,
    ddl_offset_minutes INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    chain_after_complete BOOLEAN NOT NULL DEFAULT FALSE,
    last_triggered_at DATETIME,
    next_trigger_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (api_key) REFERENCES api_key(api_key) ON DELETE CASCADE,
    INDEX idx_recurring_api_key (api_key),
    INDEX idx_recurring_next (enabled, next_trigger_at)
);
