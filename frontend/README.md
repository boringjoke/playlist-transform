# 歌单整理器前端

阶段 1 已建立 Nuxt 页面壳、首页视觉基线、基础说明页和后端健康检查联调。
解析、编辑、草稿、导出和平台兼容能力按项目阶段计划逐步接入。

## Setup

安装依赖：

```bash
npm install
```

本地前后端分开启动时，复制 `.env.example` 为本地 `.env`，并确认
`NUXT_PUBLIC_API_BASE_URL` 指向后端地址。`.env` 不提交到仓库。

## Development Server

启动开发服务器：

```bash
npm run dev
```

默认访问 `http://localhost:3000`。如果该地址被其他本地项目占用，可使用项目实际监听地址验证。

## Verification

类型检查：

```bash
npm run typecheck
```

生产构建：

```bash
npm run build
```
