-- 清洗规则数据插入脚本
-- 注意：这些规则应该插入到系统数据库（raccoon 项目的数据库），不是目标清洗数据库

-- 1. 职位相关规则
INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'position', '职位名称', '高级工程师', ARRAY['高工', '高级工程师（外聘）', '高级开发', 'Senior Engineer', '资深工程师', '高级工程师-外包', '高级工程师（外聘）', '高级工程师-外派'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'position', '职位名称', '产品经理', ARRAY['产品', 'PM', 'Product Manager', '产品负责人'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'position', '职位名称', '架构师', ARRAY['系统架构师', 'Architect', '技术架构师', '架构'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. 部门相关规则
INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'department', '部门名称', '技术部', ARRAY['技术', '技术部门', 'tech dept', 'Technology', '研发部', 'R&D'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'department', '部门名称', '产品部', ARRAY['产品', '产品部门', 'Product', 'Product Dept', '产品组'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'department', '部门名称', '测试部', ARRAY['测试', 'QA', 'Quality Assurance', '质量部'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. 学历相关规则
INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'education', '学历', '本科', ARRAY['大学本科', '本科学历', 'Bachelor', '学士', '本科毕业'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'education', '学历', '硕士', ARRAY['研究生', '硕士研究生', 'Master', '硕士学位'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'education', '学历', '博士', ARRAY['博士研究生', 'PhD', 'Doctor', '博士学位'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4. 性别相关规则
INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'gender', '性别', '男', ARRAY['M', 'Male', '先生', '男性', 'm'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'gender', '性别', '女', ARRAY['F', 'Female', '女士', '女性', 'f'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 5. 在职状态相关规则
INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'status', '在职状态', '在职', ARRAY['在岗', '正常', 'Active', '工作中', '在职中'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cleaning_rules (table_name, column_name, column_description, standard_value, dirty_values, source, confidence, auto_apply, usage_count, success_count, created_by, created_at, updated_at)
VALUES 
('employee_info', 'status', '在职状态', '离职', ARRAY['已离职', 'Inactive', '离岗', '已离岗', '不在职'], 'manual', 1.00, true, 0, 0, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 查看插入的规则
SELECT id, table_name, column_name, standard_value, array_length(dirty_values, 1) as dirty_count 
FROM cleaning_rules 
ORDER BY table_name, column_name, standard_value;
