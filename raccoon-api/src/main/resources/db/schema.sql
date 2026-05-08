-- =====================================================
-- Raccoon 数据清洗工具 - PostgreSQL 数据库脚本
-- =====================================================

-- 1. 清洗规则表
CREATE TABLE IF NOT EXISTS cleaning_rules (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    column_description TEXT,
    standard_value VARCHAR(255) NOT NULL,
    dirty_values TEXT[] NOT NULL,
    confidence DECIMAL(3,2) DEFAULT 1.0,
    source VARCHAR(50) DEFAULT 'manual',
    auto_apply BOOLEAN DEFAULT false,
    usage_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- 创建索引
CREATE INDEX idx_cleaning_rules_table_column ON cleaning_rules(table_name, column_name);
CREATE INDEX idx_cleaning_rules_source ON cleaning_rules(source);
CREATE INDEX idx_cleaning_rules_confidence ON cleaning_rules(confidence);

-- 添加注释
COMMENT ON TABLE cleaning_rules IS '数据清洗规则表';
COMMENT ON COLUMN cleaning_rules.table_name IS '表名';
COMMENT ON COLUMN cleaning_rules.column_name IS '字段名';
COMMENT ON COLUMN cleaning_rules.column_description IS '字段描述';
COMMENT ON COLUMN cleaning_rules.standard_value IS '标准值';
COMMENT ON COLUMN cleaning_rules.dirty_values IS '错误值数组';
COMMENT ON COLUMN cleaning_rules.confidence IS '规则置信度(0-1)';
COMMENT ON COLUMN cleaning_rules.source IS '来源: manual/ai_auto/ai_confirmed/learned';
COMMENT ON COLUMN cleaning_rules.auto_apply IS '是否自动应用';
COMMENT ON COLUMN cleaning_rules.usage_count IS '使用次数';
COMMENT ON COLUMN cleaning_rules.success_count IS '成功次数';

-- 2. 候选规则表
CREATE TABLE IF NOT EXISTS candidate_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT REFERENCES cleaning_rules(id) ON DELETE CASCADE,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    standard_value VARCHAR(255) NOT NULL,
    candidate_value VARCHAR(255) NOT NULL,
    confidence DECIMAL(3,2) NOT NULL,
    affected_count INT DEFAULT 0,
    reasoning TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(100)
);

-- 创建索引
CREATE INDEX idx_candidate_rules_status ON candidate_rules(status);
CREATE INDEX idx_candidate_rules_table_column ON candidate_rules(table_name, column_name);
CREATE INDEX idx_candidate_rules_confidence ON candidate_rules(confidence DESC);

-- 添加注释
COMMENT ON TABLE candidate_rules IS 'AI发现的候选规则表';
COMMENT ON COLUMN candidate_rules.status IS '状态: pending/approved/rejected';

-- 3. 清洗日志表
CREATE TABLE IF NOT EXISTS cleaning_logs (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    record_id BIGINT,
    rule_id BIGINT REFERENCES cleaning_rules(id) ON DELETE SET NULL,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    executed_by VARCHAR(100)
);

-- 创建索引
CREATE INDEX idx_cleaning_logs_table_column ON cleaning_logs(table_name, column_name);
CREATE INDEX idx_cleaning_logs_executed_at ON cleaning_logs(executed_at DESC);
CREATE INDEX idx_cleaning_logs_rule_id ON cleaning_logs(rule_id);

-- 添加注释
COMMENT ON TABLE cleaning_logs IS '数据清洗执行日志表';

-- 4. 学习日志表
CREATE TABLE IF NOT EXISTS learning_logs (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    discovered_standard VARCHAR(255),
    discovered_variants TEXT[],
    confidence DECIMAL(3,2),
    evidence_count INT,
    learning_method VARCHAR(50),
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_learning_logs_table_column ON learning_logs(table_name, column_name);
CREATE INDEX idx_learning_logs_status ON learning_logs(status);
CREATE INDEX idx_learning_logs_created_at ON learning_logs(created_at DESC);

-- 添加注释
COMMENT ON TABLE learning_logs IS '系统学习日志表';
COMMENT ON COLUMN learning_logs.learning_method IS '学习方法: clustering/pattern/feedback';

-- 5. 用户反馈表
CREATE TABLE IF NOT EXISTS user_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT REFERENCES cleaning_rules(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL,
    original_suggestion TEXT,
    user_modification TEXT,
    feedback_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(100)
);

-- 创建索引
CREATE INDEX idx_user_feedbacks_rule_id ON user_feedbacks(rule_id);
CREATE INDEX idx_user_feedbacks_action ON user_feedbacks(action);
CREATE INDEX idx_user_feedbacks_time ON user_feedbacks(feedback_time DESC);

-- 添加注释
COMMENT ON TABLE user_feedbacks IS '用户反馈表';
COMMENT ON COLUMN user_feedbacks.action IS '操作: approved/rejected/modified';

-- 6. 字段元数据表
CREATE TABLE IF NOT EXISTS column_metadata (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    data_type VARCHAR(50),
    description TEXT,
    value_pattern VARCHAR(100),
    cardinality INT,
    sample_values TEXT[],
    monitor_enabled BOOLEAN DEFAULT true,
    last_analyzed TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(table_name, column_name)
);

-- 创建索引
CREATE INDEX idx_column_metadata_monitor ON column_metadata(monitor_enabled);
CREATE INDEX idx_column_metadata_table ON column_metadata(table_name);

-- 添加注释
COMMENT ON TABLE column_metadata IS '字段元数据表';
COMMENT ON COLUMN column_metadata.value_pattern IS '值模式: enum/free_text/code';
COMMENT ON COLUMN column_metadata.cardinality IS '唯一值数量';
COMMENT ON COLUMN column_metadata.monitor_enabled IS '是否启用监控';

-- 7. 清洗任务表
CREATE TABLE IF NOT EXISTS cleaning_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(200) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    total_records INT DEFAULT 0,
    cleaned_records INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending',
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- 创建索引
CREATE INDEX idx_cleaning_tasks_status ON cleaning_tasks(status);
CREATE INDEX idx_cleaning_tasks_created_at ON cleaning_tasks(created_at DESC);

-- 添加注释
COMMENT ON TABLE cleaning_tasks IS '清洗任务记录表';
COMMENT ON COLUMN cleaning_tasks.status IS '状态: pending/running/completed/failed';

-- 8. 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

-- 添加注释
COMMENT ON TABLE system_config IS '系统配置表';

-- 插入默认配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('self_learning.enabled', 'true', '是否启用自学习'),
('self_learning.auto_add_threshold', '0.95', '自动添加规则的置信度阈值'),
('self_learning.auto_apply_threshold', '0.98', '自动应用清洗的置信度阈值'),
('safety.max_auto_clean_records', '1000', '单次自动清洗的最大记录数'),
('llm.batch_size', '50', '每批发送给大模型的数量')
ON CONFLICT (config_key) DO NOTHING;

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为需要的表添加更新时间触发器
CREATE TRIGGER update_cleaning_rules_updated_at BEFORE UPDATE ON cleaning_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_column_metadata_updated_at BEFORE UPDATE ON column_metadata
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_system_config_updated_at BEFORE UPDATE ON system_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
