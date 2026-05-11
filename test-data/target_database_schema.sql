-- 目标数据库：测试清洗数据库
-- 数据库名：raccoon_test_db

-- 员工信息表
CREATE TABLE employee_info (
    id SERIAL PRIMARY KEY,
    employee_no VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(20),
    position VARCHAR(100),
    department VARCHAR(100),
    education VARCHAR(50),
    status VARCHAR(50),
    phone VARCHAR(50),
    email VARCHAR(100),
    hire_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加字段注释
COMMENT ON COLUMN employee_info.id IS 'ID';
COMMENT ON COLUMN employee_info.employee_no IS '工号';
COMMENT ON COLUMN employee_info.name IS '姓名';
COMMENT ON COLUMN employee_info.gender IS '性别';
COMMENT ON COLUMN employee_info.position IS '职位';
COMMENT ON COLUMN employee_info.department IS '部门';
COMMENT ON COLUMN employee_info.education IS '学历';
COMMENT ON COLUMN employee_info.status IS '在职状态';
COMMENT ON COLUMN employee_info.phone IS '联系电话';
COMMENT ON COLUMN employee_info.email IS '邮箱';
COMMENT ON COLUMN employee_info.hire_date IS '入职日期';

-- 创建索引
CREATE INDEX idx_employee_no ON employee_info(employee_no);
CREATE INDEX idx_department ON employee_info(department);
CREATE INDEX idx_status ON employee_info(status);

-- 插入测试数据（包含各种脏数据）
INSERT INTO employee_info (employee_no, name, gender, position, department, education, status, phone, email, hire_date) VALUES
-- 正常数据
('E001', '张三', '男', '高级工程师', '技术部', '本科', '在职', '13800138001', 'zhangsan@company.com', '2020-01-15'),
('E002', '李四', '女', '产品经理', '产品部', '硕士', '在职', '13800138002', 'lisi@company.com', '2019-06-20'),
('E003', '王五', '男', '架构师', '技术部', '硕士', '在职', '13800138003', 'wangwu@company.com', '2018-03-10'),

-- 职位脏数据（缩写、别名、带备注）
('E004', '赵六', '男', '高工', '技术部', '本科', '在职', '13800138004', 'zhaoliu@company.com', '2021-02-01'),
('E005', '孙七', '女', '高级工程师（外聘）', '技术部', '本科', '在职', '13800138005', 'sunqi@company.com', '2021-03-15'),
('E006', '周八', '男', '高级开发', '技术部', '硕士', '在职', '13800138006', 'zhouba@company.com', '2020-07-20'),
('E007', '吴九', '女', 'Senior Engineer', '技术部', '本科', '在职', '13800138007', 'wujiu@company.com', '2019-11-05'),
('E008', '郑十', '男', '资深工程师', '技术部', '硕士', '在职', '13800138008', 'zhengshi@company.com', '2020-09-12'),

-- 部门脏数据
('E009', '刘一', '女', '产品经理', '产品', '本科', '在职', '13800138009', 'liuyi@company.com', '2021-01-08'),
('E010', '陈二', '男', '产品经理', '产品部门', '硕士', '在职', '13800138010', 'chener@company.com', '2020-05-18'),
('E011', '杨三', '女', '产品经理', 'Product', '本科', '在职', '13800138011', 'yangsan@company.com', '2019-08-22'),

-- 学历脏数据
('E012', '黄四', '男', '工程师', '技术部', '大学本科', '在职', '13800138012', 'huangsi@company.com', '2021-04-10'),
('E013', '林五', '女', '工程师', '技术部', '本科学历', '在职', '13800138013', 'linwu@company.com', '2020-10-25'),
('E014', '徐六', '男', '工程师', '技术部', 'Bachelor', '在职', '13800138014', 'xuliu@company.com', '2019-12-30'),
('E015', '朱七', '女', '高级工程师', '技术部', '研究生', '在职', '13800138015', 'zhuqi@company.com', '2018-07-15'),
('E016', '何八', '男', '架构师', '技术部', '硕士研究生', '在职', '13800138016', 'heba@company.com', '2017-09-20'),

-- 性别脏数据
('E017', '马九', 'M', '工程师', '技术部', '本科', '在职', '13800138017', 'majiu@company.com', '2021-05-12'),
('E018', '梁十', 'F', '产品经理', '产品部', '硕士', '在职', '13800138018', 'liangshi@company.com', '2020-11-08'),
('E019', '宋一', '先生', '工程师', '技术部', '本科', '在职', '13800138019', 'songyi@company.com', '2019-06-15'),
('E020', '唐二', '女士', '产品经理', '产品部', '本科', '在职', '13800138020', 'tanger@company.com', '2021-07-20'),

-- 在职状态脏数据
('E021', '韩三', '男', '工程师', '技术部', '本科', '在岗', '13800138021', 'hansan@company.com', '2020-02-14'),
('E022', '冯四', '女', '产品经理', '产品部', '硕士', '正常', '13800138022', 'fengsi@company.com', '2019-04-18'),
('E023', '曹五', '男', '工程师', '技术部', '本科', '离职', '13800138023', 'caowu@company.com', '2018-05-22'),
('E024', '袁六', '女', '产品经理', '产品部', '本科', '已离职', '13800138024', 'yuanliu@company.com', '2017-08-30'),
('E025', '邓七', '男', '高级工程师', '技术部', '硕士', 'Active', '13800138025', 'dengqi@company.com', '2020-12-05'),

-- 混合脏数据
('E026', '彭八', 'M', '高工', '技术', '大学本科', '在岗', '13800138026', 'pengba@company.com', '2021-08-15'),
('E027', '谢九', 'F', '产品经理', 'Product Dept', '研究生', '正常', '13800138027', 'xiejiu@company.com', '2020-03-22'),
('E028', '傅十', '先生', 'Senior Engineer', '技术部门', 'Bachelor', 'Active', '13800138028', 'fushi@company.com', '2019-09-10'),

-- 更多正常数据作为对照
('E029', '姚一', '男', '测试工程师', '测试部', '本科', '在职', '13800138029', 'yaoyi@company.com', '2021-06-01'),
('E030', '顾二', '女', '测试工程师', '测试部', '本科', '在职', '13800138030', 'guer@company.com', '2020-08-15');

-- 查看数据统计
-- SELECT position, COUNT(*) as count FROM employee_info GROUP BY position ORDER BY count DESC;
-- SELECT department, COUNT(*) as count FROM employee_info GROUP BY department ORDER BY count DESC;
-- SELECT education, COUNT(*) as count FROM employee_info GROUP BY education ORDER BY count DESC;
-- SELECT gender, COUNT(*) as count FROM employee_info GROUP BY gender ORDER BY count DESC;
-- SELECT status, COUNT(*) as count FROM employee_info GROUP BY status ORDER BY count DESC;
