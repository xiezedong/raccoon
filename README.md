# 数据清洗工具

## 项目背景

在实际业务中，数据库经常存在大量脏数据，例如职位名称字段中可能存在"高级工程师"、"高工"、"高级工程师（外聘）"等多种表达方式，这些数据不统一会影响数据分析和业务处理。

本项目旨在开发一个智能数据清洗工具，通过人工提供的正反例结合大模型的发散能力，自动发现并修复数据库中的脏数据。

## 核心需求

### 功能需求

1. **规则管理**
   - 支持通过 Excel 导入标准值和错误值对照表
   - 支持多表多字段的清洗规则配置
   - 规则包含：表名、字段名、字段描述、标准值、错误值（多个以顿号分隔）

2. **智能发现**
   - 基于已有规则，利用大模型发现数据库中可能的脏数据
   - 支持编辑距离聚类和向量相似度匹配（可选）
   - 自动识别简称、别名、错别字、多余修饰词等变体

3. **人工审核**
   - 展示 AI 发现的候选脏数据
   - 显示每条规则影响的记录数
   - 支持批量确认、拒绝或修改

4. **数据清洗**
   - 预览清洗影响范围
   - 事务化执行数据更新
   - 记录详细的清洗日志
   - 支持一键回滚

5. **自我学习**
   - 自动扫描数据库发现潜在的标准值和变体
   - 根据置信度自动添加规则
   - 从用户反馈中学习偏好
   - 定期监控新增脏数据

### 非功能需求

- 支持大数据量处理（百万级记录）
- 控制大模型调用成本
- 保证数据安全（审计日志、回滚机制）
- 友好的用户界面

## 技术架构

### 技术栈

- **后端**: Java + Spring Boot
- **前端**: Vue3 + TypeScript + Element Plus
- **数据库**: PostgreSQL (支持 pgvector 扩展)
- **大模型**: OpenAI API / 国内大模型 API（通义千问、文心一言等）

### 核心模块

```
raccoon/
├── raccoon-api/                           # 后端服务
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/raccoon/datacleaning/
│   │   │   │   ├── controller/           # API 控制器
│   │   │   │   ├── service/              # 业务逻辑
│   │   │   │   │   ├── CleaningRuleService.java
│   │   │   │   │   ├── DirtyDataDetector.java
│   │   │   │   │   ├── SelfLearningService.java
│   │   │   │   │   ├── LLMService.java
│   │   │   │   │   └── VectorSimilarityService.java
│   │   │   │   ├── repository/           # 数据访问层
│   │   │   │   ├── model/                # 实体类
│   │   │   │   ├── config/               # 配置类
│   │   │   │   ├── util/                 # 工具类
│   │   │   │   └── exception/            # 异常处理
│   │   │   └── resources/                # 配置文件
│   │   │       ├── application.yml
│   │   │       └── db/migration/         # 数据库迁移脚本
│   │   └── test/                         # 测试代码
│   └── pom.xml                           # Maven 配置
├── raccoon-web/                           # 前端应用
│   ├── src/
│   │   ├── views/                        # 页面组件
│   │   │   ├── RuleManagement.vue
│   │   │   ├── AIDiscovery.vue
│   │   │   ├── CleaningExecution.vue
│   │   │   └── SelfLearningDashboard.vue
│   │   ├── api/                          # API 调用
│   │   └── components/                   # 公共组件
│   └── package.json
└── README.md
```

## 数据库设计

### 核心表结构

#### 1. cleaning_rules - 清洗规则表
```sql
CREATE TABLE cleaning_rules (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(100),              -- 表名
    column_name VARCHAR(100),             -- 字段名
    column_description TEXT,              -- 字段描述
    standard_value VARCHAR(255),          -- 标准值
    dirty_values TEXT[],                  -- 错误值数组
    confidence DECIMAL(3,2) DEFAULT 1.0,  -- 规则置信度
    source VARCHAR(50) DEFAULT 'manual',  -- 来源：manual/ai_auto/ai_confirmed
    auto_apply BOOLEAN DEFAULT false,     -- 是否自动应用
    usage_count INT DEFAULT 0,            -- 使用次数
    success_count INT DEFAULT 0,          -- 成功次数
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### 2. candidate_rules - 候选规则表
```sql
CREATE TABLE candidate_rules (
    id SERIAL PRIMARY KEY,
    rule_id INT REFERENCES cleaning_rules(id),
    candidate_value VARCHAR(255),         -- AI 发现的候选错误值
    confidence DECIMAL(3,2),              -- 置信度
    affected_count INT,                   -- 影响的记录数
    reasoning TEXT,                       -- AI 推理说明
    status VARCHAR(20) DEFAULT 'pending', -- pending/approved/rejected
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### 3. cleaning_logs - 清洗日志表
```sql
CREATE TABLE cleaning_logs (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(100),
    column_name VARCHAR(100),
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    record_id BIGINT,                     -- 原始记录ID
    rule_id INT REFERENCES cleaning_rules(id),
    executed_at TIMESTAMP DEFAULT NOW(),
    executed_by VARCHAR(100)
);
```

#### 4. learning_logs - 学习日志表
```sql
CREATE TABLE learning_logs (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(100),
    column_name VARCHAR(100),
    discovered_standard VARCHAR(255),     -- 发现的标准值
    discovered_variants TEXT[],           -- 发现的变体
    confidence DECIMAL(3,2),              -- 置信度
    evidence_count INT,                   -- 证据数量
    learning_method VARCHAR(50),          -- 学习方法
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### 5. user_feedbacks - 用户反馈表
```sql
CREATE TABLE user_feedbacks (
    id SERIAL PRIMARY KEY,
    rule_id INT REFERENCES cleaning_rules(id),
    action VARCHAR(20),                   -- approved/rejected/modified
    original_suggestion TEXT,             -- AI 原始建议
    user_modification TEXT,               -- 用户修改内容
    feedback_time TIMESTAMP DEFAULT NOW()
);
```

#### 6. column_metadata - 字段元数据表
```sql
CREATE TABLE column_metadata (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(100),
    column_name VARCHAR(100),
    data_type VARCHAR(50),
    description TEXT,
    value_pattern VARCHAR(100),           -- 值的模式：enum/free_text/code
    cardinality INT,                      -- 唯一值数量
    sample_values TEXT[],                 -- 样本值
    monitor_enabled BOOLEAN DEFAULT true, -- 是否启用监控
    last_analyzed TIMESTAMP,
    UNIQUE(table_name, column_name)
);
```

## 核心工作流程

### 阶段一：规则初始化
1. 准备 Excel 文件（包含：表名、字段名、字段描述、正确值、错误值）
2. 通过界面上传 Excel
3. 系统解析并导入到 `cleaning_rules` 表
4. 支持多表多字段批量导入

### 阶段二：已知脏数据检测
1. 根据导入的规则直接查询数据库
2. 使用 SQL: `SELECT * FROM table WHERE column IN (dirty_values)`
3. 展示查询结果和影响记录数
4. 用户确认后执行清洗

### 阶段三：AI 辅助发现
1. **聚合去重**: 获取字段的所有唯一值
2. **预筛选**: 过滤明显正常的值（长度、特殊字符等）
3. **分批处理**: 每批 50 个值发送给大模型
4. **智能分析**: 大模型识别可能的变体
5. **结果保存**: 存入 `candidate_rules` 表待确认

### 阶段四：人工审核
1. 展示 AI 发现的候选规则
2. 按置信度排序
3. 显示每条规则影响的记录数
4. 用户批量确认/拒绝/修改
5. 确认后加入 `cleaning_rules` 表

### 阶段五：执行清洗
1. 预览清洗影响范围
2. 生成 UPDATE SQL 语句
3. 事务化执行更新
4. 记录详细日志到 `cleaning_logs`
5. 支持一键回滚

### 阶段六：自我学习（高级功能）
1. **定期扫描**: 每天自动扫描配置的字段
2. **聚类分析**: 使用编辑距离聚类相似值
3. **AI 判断**: 判断聚类是否属于同一概念
4. **自动添加**: 置信度 > 0.95 自动添加规则
5. **反馈学习**: 从用户行为学习偏好

## 上下文控制策略

### 问题
数据表可能包含百万级记录，如何避免大模型上下文爆炸？

### 解决方案

#### 1. 聚合去重（第一道防线）
```sql
-- 不发送原始数据，只发送唯一值
SELECT dirty_value, COUNT(*) as occurrence_count
FROM table_name
GROUP BY dirty_value
ORDER BY occurrence_count DESC;
```
效果：100万条数据 → 5000个唯一值

#### 2. 分批处理（第二道防线）
- 每批最多 50 个值发送给大模型
- 只携带相关字段的规则作为上下文
- 避免一次性加载所有数据

#### 3. 智能预筛选（第三道防线）
只把"可能有问题"的值发给大模型：
- 长度异常（太短或太长）
- 包含特殊字符（括号、斜杠等）
- 包含数字（可能是编号）
- 与已知标准值编辑距离小

#### 4. 上下文压缩（提示词优化）
- 只提供最相关的 5 个标准值示例
- 使用结构化 JSON 格式
- 要求大模型只返回高置信度结果

## 向量相似度方案（可选）

### 何时引入向量相似度？

**不需要向量（第一阶段 - MVP）**
- 数据量 < 1万条唯一值
- 批量处理为主
- 大模型成本可接受
- 快速验证业务价值

**需要向量（第二阶段 - 优化）**
- 数据量 > 10万条唯一值
- 需要实时查询
- 大模型调用费用超预算
- 需要更快的响应速度

### 向量方案架构

```sql
-- 安装 pgvector 扩展
CREATE EXTENSION vector;

-- 添加向量字段
ALTER TABLE cleaning_rules 
ADD COLUMN standard_value_embedding vector(1536);

-- 创建向量索引
CREATE INDEX ON cleaning_rules 
USING ivfflat (standard_value_embedding vector_cosine_ops);
```

### 工作流程
1. 对所有标准值生成 embedding（调用 OpenAI Embedding API）
2. 对待检测值生成 embedding
3. 用向量相似度找出 Top-K 最相似的标准值（余弦相似度 > 0.7）
4. 只把这些候选项发给大模型做最终判断

### 优势
- 大幅减少大模型调用次数（成本降低 80%+）
- 响应速度更快（向量检索毫秒级）
- 可以离线批量处理

## 自我学习机制

### 三个层次的自动化

#### Level 1: 半自动（需人工确认）
- AI 发现候选规则
- 人工审核后加入 cleaning_rules
- 当前已实现

#### Level 2: 高置信度自动
- 置信度 > 0.95 的规则自动加入
- 自动执行清洗（可配置）
- 定期人工抽查

#### Level 3: 完全自主学习
- 从用户行为学习
- 从清洗结果反馈学习
- 自动优化规则

### 学习策略

#### 策略 A: 聚类发现标准值
1. 获取字段的所有值及频率
2. 使用编辑距离聚类相似的值
3. 对每个聚类，让 AI 判断是否是"标准值+变体"关系
4. 根据置信度决定处理方式

#### 策略 B: AI 智能分析
- 综合考虑字段描述、样本值
- 判断聚类是否表达同一概念
- 给出置信度和推理说明

#### 策略 C: 置信度分级处理
- **置信度 ≥ 0.95**: 自动添加规则（可配置是否自动应用）
- **置信度 ≥ 0.75**: 加入候选列表，等待人工确认
- **置信度 < 0.75**: 仅记录日志，不做处理

#### 策略 D: 反馈强化学习
- 记录用户的确认/拒绝/修改行为
- 分析用户偏好模式
- 自动调整置信度阈值

### 定期自动扫描
- 每天凌晨自动扫描配置的字段
- 分析字段值的变化
- 发现新增的脏数据
- 自动生成清洗建议

## 安全机制

### 自动清洗前的安全检查
1. **影响记录数限制**: 单次自动清洗最大记录数（默认 1000）
2. **置信度要求**: AI 自动生成的规则需要 ≥ 0.98
3. **敏感字段保护**: 敏感字段不允许自动清洗
4. **数据备份**: 清洗前可选备份原始数据

### 审计与回滚
- 详细记录每次清洗操作
- 记录原值、新值、操作人、时间
- 支持一键回滚
- 定期生成清洗报告

## 配置说明

### application.yml 配置示例

```yaml
data-cleaning:
  self-learning:
    enabled: true
    
    # 自动添加规则的置信度阈值
    auto-add-threshold: 0.95
    
    # 自动应用清洗的置信度阈值
    auto-apply-threshold: 0.98
    
    # 定期扫描
    scheduled-scan:
      enabled: true
      cron: "0 0 2 * * ?"  # 每天凌晨2点
      
    # 每次扫描的最大字段数
    max-columns-per-scan: 50
    
    # 通知设置
    notification:
      enabled: true
      email: admin@example.com
      
    # 安全设置
    safety:
      # 单次自动清洗的最大记录数
      max-auto-clean-records: 1000
      # 需要人工确认的影响记录数阈值
      manual-confirm-threshold: 100
      
  # 大模型配置
  llm:
    provider: openai  # openai / qianwen / wenxin
    api-key: ${LLM_API_KEY}
    model: gpt-4
    
  # 向量相似度配置（可选）
  vector:
    enabled: false
    embedding-model: text-embedding-ada-002
```

## Excel 导入格式

| 表名 | 字段名 | 字段描述 | 正确值 | 错误值 | 备注 |
|------|--------|---------|--------|--------|------|
| employees | job_title | 职位名称 | 高级工程师 | 高工、高级工程师（外聘）、高级工程师-外包 | |
| employees | department | 部门名称 | 技术部 | 技术部门、tech dept | |
| projects | status | 项目状态 | 进行中 | 正在进行、进行中... | |

**说明**:
- 错误值支持多个，使用顿号（、）或逗号（,）分隔
- 字段描述很重要，帮助 AI 理解字段语义
- 备注字段可选

## 实施路径

### 第一阶段（MVP - 2-3周）
- ✅ 基础规则管理（CRUD）
- ✅ Excel 导入功能
- ✅ 已知脏数据检测
- ✅ 人工审核界面
- ✅ 数据清洗执行
- ✅ 纯大模型方案（不用向量）

### 第二阶段（AI 辅助 - 2-3周）
- ✅ AI 发现候选规则
- ✅ 编辑距离聚类
- ✅ 分批处理和上下文控制
- ✅ 置信度分级处理

### 第三阶段（自动化 - 2-3周）
- ✅ 高置信度规则自动添加
- ✅ 定期自动扫描
- ✅ 监控新增值
- ✅ 自学习监控界面

### 第四阶段（智能化 - 按需）
- ✅ 向量相似度优化
- ✅ 用户反馈学习
- ✅ 自动优化阈值
- ✅ 跨字段关联学习

## 预期效果

- **效率提升**: 人工清洗 1 天 → AI 辅助 1 小时
- **成本降低**: 通过聚合和预筛选，大模型调用成本降低 80%+
- **准确率**: 人工审核 + AI 辅助，准确率 > 95%
- **可扩展性**: 支持百万级数据处理
- **智能化**: 系统越用越智能，自动发现新规则

## AI助手协作规则

### 语言偏好
- 使用中文进行对话和回复

### 文档管理
- 未经允许不创建总结性md文档

### 沟通风格
- 回复问题需要简明扼要，避免冗长

### 代码修改流程
1. 先阅读相关代码
2. 分析并寻找最合适的解决方案
3. 向用户说明修改方案并征求同意
4. 确认后再执行修改
