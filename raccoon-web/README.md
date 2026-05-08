# Raccoon Web - 数据清洗工具前端

基于 Vue3 + TypeScript + Element Plus 的数据清洗工具前端应用。

## 技术栈

- Vue 3.4
- TypeScript
- Vite 5
- Element Plus 2.7
- Vue Router 4
- Pinia 2
- Axios

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## 项目结构

```
src/
├── api/              # API 接口
├── assets/           # 静态资源
├── components/       # 公共组件
├── router/           # 路由配置
├── stores/           # 状态管理
├── types/            # TypeScript 类型定义
├── utils/            # 工具函数
├── views/            # 页面组件
├── App.vue           # 根组件
└── main.ts           # 入口文件
```

## 功能模块

- **规则管理**: 创建、编辑、删除清洗规则，支持 Excel 导入
- **AI 发现**: AI 辅助发现潜在的脏数据（开发中）
- **清洗执行**: 预览和执行数据清洗操作（开发中）
- **清洗日志**: 查看清洗历史记录（开发中）
