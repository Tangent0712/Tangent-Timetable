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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (api_key) REFERENCES api_key(api_key) ON DELETE CASCADE,
    INDEX idx_api_key (api_key),
    INDEX idx_ddl (ddl),
    INDEX idx_completed (completed)
);
