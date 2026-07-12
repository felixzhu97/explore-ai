# AI Chat Service Quick Start Guide

## AI 聊天服务快速入门指南

---

## 1. Prerequisites | 前置条件

### Required Software | 必需软件


| Software   | Version | Download                             |
| ---------- | ------- | ------------------------------------ |
| Node.js    | >= 20.x | [nodejs.org](https://nodejs.org)     |
| Java (JDK) | >= 25   | [adoptium.net](https://adoptium.net) |
| pnpm       | >= 8.x  | `npm install -g pnpm`                |


### Verify Installation | 验证安装

```bash
# Check versions / 检查版本
node --version    # Should show v20.x.x or higher
java --version    # Should show 25.x.x or higher
pnpm --version    # Should show 8.x.x or higher
```

> **Note | 注意**: This project uses `pnpm` as the package manager. Please do not use `npm` or `yarn`.

---

## 2. Quick Installation | 快速安装

### Step 1: Install Dependencies | 步骤 1: 安装依赖

```bash
# Install all dependencies (root + backend + frontend)
# 安装所有依赖（根目录 + 后端 + 前端）
pnpm install
```

This command will install:

- Root workspace dependencies
- Frontend (Angular) dependencies at project root (`src/main/web`)
- Backend (Spring Boot) dependencies (downloaded via Gradle)

### Step 2: Verify Installation | 步骤 2: 验证安装

```bash
# Check installed packages
pnpm list --depth 0
```

---

## 3. Environment Configuration | 环境配置

### Step 1: Create Backend Environment File | 步骤 1: 创建后端环境文件

```bash

# Copy example env file
cp .env.example .env
```

### Step 2: Configure DeepSeek API Key | 步骤 2: 配置 DeepSeek API Key

Edit the `.env` file in ``:

```bash
# .env

# DeepSeek API Configuration
DEEPSEEK_API_KEY=your-deepseek-api-key-here
DEEPSEEK_BASE_URL=https://api.deepseek.com
```

#### How to Get DeepSeek API Key | 如何获取 DeepSeek API Key

1. Visit [platform.deepseek.com](https://platform.deepseek.com)
2. Register or log in to your account
3. Go to **API Keys** section
4. Click **Create API Key**
5. Copy and paste the key into `.env`

> **Important | 重要**: Never commit your `.env` file to version control. It is already in `.gitignore`.

---

## 4. Starting Services | 启动服务

### Option A: Start Services Separately (Recommended) | 选项 A: 分别启动前后端（推荐）

#### Start Backend | 启动后端

```bash
# Terminal 1: Start Spring Boot backend
./gradlew bootRun
```

The backend runs on **[http://localhost:9000](http://localhost:9000)**

#### Start Frontend | 启动前端

```bash
# Terminal 2: Start Angular frontend (from project root)
pnpm start
```

The frontend runs on **[http://localhost:4200](http://localhost:4200)**

### Option B: Backend Only or Frontend Only | 选项 B: 仅启动一端

Use `./gradlew bootRun` or `pnpm start` when you only need the backend or frontend.

### Verify Services are Running | 验证服务运行状态

```bash
# Check backend health
curl http://localhost:9000/actuator/health

# Check frontend
open http://localhost:4200
```

---

## 5. First Use | 首次使用

### Step 1: Open the Application | 步骤 1: 打开应用

Open your browser and navigate to:

- Frontend: **[http://localhost:4200](http://localhost:4200)**

### Step 2: Send Your First Message | 步骤 2: 发送第一条消息

1. Find the chat input box on the page
2. Type a message (e.g., "Hello, how are you?")
3. Press **Enter** or click the **Send** button
4. Wait for the AI response

### Step 3: View Chat History | 步骤 3: 查看聊天历史

- Your messages and AI responses are displayed in the chat area
- Previous conversations are saved automatically

---

## 6. Common Commands | 常用命令

### Development Commands | 开发命令


| Command             | Description         | 说明    |
| ------------------- | ------------------- | ----- |
| `./gradlew bootRun` | Start backend only  | 仅启动后端 |
| `pnpm start`        | Start frontend only | 仅启动前端 |


### Build Commands | 构建命令


| Command           | Description        | 说明    |
| ----------------- | ------------------ | ----- |
| `pnpm build`      | Build all packages | 构建所有包 |
| `pnpm build`      | Build frontend     | 构建前端  |
| `./gradlew build` | Build backend      | 构建后端  |


### Test Commands | 测试命令


| Command           | Description             | 说明       |
| ----------------- | ----------------------- | -------- |
| `pnpm test`       | Run all tests           | 运行所有测试   |
| `pnpm test:watch` | Run tests in watch mode | 监听模式运行测试 |
| `pnpm test`       | Run frontend tests      | 运行前端测试   |
| `./gradlew test`  | Run backend tests       | 运行后端测试   |


### Lint Commands | 代码检查命令


| Command     | Description       | 说明     |
| ----------- | ----------------- | ------ |
| `pnpm lint` | Lint all packages | 检查所有包  |
| `pnpm lint` | Lint frontend     | 检查前端代码 |


### Clean Commands | 清理命令


| Command           | Description               | 说明       |
| ----------------- | ------------------------- | -------- |
| `pnpm clean`      | Clean all build artifacts | 清理所有构建产物 |
| `./gradlew clean` | Clean backend             | 清理后端构建   |


---

## 7. Troubleshooting | 故障排除

### Common Issues | 常见问题

#### Issue 1: "Connection refused" or "Backend not responding"

**Symptom | 症状**: Frontend cannot connect to backend

**Solutions | 解决方案**:

```bash
# 1. Check if backend is running
ps aux | grep java | grep -v grep

# 2. Check backend port
curl http://localhost:9000/actuator/health

# 3. Restart backend
./gradlew bootRun
```

#### Issue 2: "API Key not configured" or "Invalid API Key"

**Symptom | 症状**: AI responses fail or return errors

**Solutions | 解决方案**:

```bash
# 1. Check .env file exists
cat .env

# 2. Verify API key format (should start with sk-)
grep DEEPSEEK_API_KEY .env

# 3. Restart backend after configuring
./gradlew bootRun
```

#### Issue 3: "CORS error" in browser console

**Symptom | 症状**: Browser blocks API requests due to CORS policy

**Solutions | 解决方案**:

The backend is already configured to allow CORS from `http://localhost:4200`.

```bash
# 1. Verify backend CORS settings
curl -I -H "Origin: http://localhost:4200" http://localhost:9000/api/chat

# 2. Check browser console for specific error details
```

#### Issue 4: Frontend build fails

**Symptom | 症状**: Angular build errors

**Solutions | 解决方案**:

```bash
# 1. Clear node_modules and reinstall
rm -rf node_modules
pnpm install

# 2. Clear Angular cache (from project root)
rm -rf .angular node_modules dist
pnpm install && pnpm build
```

#### Issue 5: Backend build fails

**Symptom | 症状**: Gradle build errors

**Solutions | 解决方案**:

```bash
# 1. Clean and rebuild
./gradlew clean build

# 2. Check Java version
java --version  # Must be 25+

# 3. Check Gradle wrapper
./gradlew --version
```

### Port Reference | 端口参考


| Service               | Port | URL                                            |
| --------------------- | ---- | ---------------------------------------------- |
| Backend (Spring Boot) | 9000 | [http://localhost:9000](http://localhost:9000) |
| Frontend (Angular)    | 4200 | [http://localhost:4200](http://localhost:4200) |


### Environment Variables Reference | 环境变量参考


| Variable            | Default                                              | Description                           |
| ------------------- | ---------------------------------------------------- | ------------------------------------- |
| `DEEPSEEK_API_KEY`  | -                                                    | Your DeepSeek API key (required)      |
| `DEEPSEEK_BASE_URL` | [https://api.deepseek.com](https://api.deepseek.com) | DeepSeek API endpoint                 |
| `IMAGE_PROVIDER`    | `ollama`                                             | Image provider: `ollama` or `openai`  |
| `IMAGE_MODEL`       | `x/flux2-klein`                                      | Ollama/OpenAI image model name        |
| `IMAGE_BASE_URL`    | `http://localhost:11434/v1`                          | Image API base URL                    |
| `OPENAI_API_KEY`    | -                                                    | Required when `IMAGE_PROVIDER=openai` |


### Image Generation (Free Ollama) | 图像生成（免费 Ollama）

Chat uses DeepSeek; image generation uses a **separate provider** (default: local Ollama, zero API cost).

```bash
# 1. Pull an image model (requires GPU on Apple Silicon or NVIDIA)
ollama pull x/flux2-klein

# 2. Ensure Ollama is running
ollama serve

# 3. Start backend and frontend (two terminals), then open /generate/image
./gradlew bootRun   # terminal 1
pnpm start          # terminal 2
```

Optional paid path: set `IMAGE_PROVIDER=openai`, `IMAGE_API_KEY`, and `IMAGE_BASE_URL=https://api.openai.com/v1` in `.env`.

### Need More Help? | 需要更多帮助？

- **Backend Issues**: Check logs in `build.gradle.kts`
- **Frontend Issues**: Check root `package.json`
- **Architecture Docs**: See [ARCHITECTURE.md](./ARCHITECTURE.md)

---

## Quick Reference Card | 快速参考卡

```
┌─────────────────────────────────────────────────────────────┐
│                    QUICK START SUMMARY                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Install:     pnpm install                               │
│                                                              │
│  2. Configure:   .env                           │
│                   └─ DEEPSEEK_API_KEY=sk-xxx               │
│                                                              │
│  3. Start:       ./gradlew bootRun  +  pnpm start           │
│                   └─ Backend:  http://localhost:9000        │
│                   └─ Frontend: http://localhost:4200       │
│                                                              │
│  4. Use:         Open browser → Type message → Get reply    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

**Enjoy your AI Chat experience! | 享受您的 AI 聊天体验！**