---
name: angular-react-ui-test
description: Angular 与 React 迁移后 UI/交互一致性测试规范与 Playwright E2E 测试指南。对比验证 Angular 应用与 React 原版在视觉、交互、i18n 等方面的完全一致性。
globs: **/*.spec.ts **/*.test.ts **/*.cy.ts
alwaysApply: false
---

# Angular ↔ React UI/交互一致性测试规范

## 概述

本规范用于验证 Angular 迁移后的 UI 与 React 原版在视觉、交互、国际化等方面完全一致。适用于所有已迁移组件的回归测试。

---

## 核心对比维度

| 维度 | React 实现 | Angular 实现 | 验证要点 |
|------|-----------|-------------|---------|
| **布局结构** | `styled-components` / CSS-in-JS | SCSS + BEM | 容器结构、Grid/Flex 布局 |
| **颜色变量** | `theme.ts` 导出 tokens | CSS 变量 + SCSS variables | 主色、背景、边框、文字色 |
| **间距系统** | `spacing.ts` (xs/sm/md/lg/xl) | `--spacing-xs` 等 CSS 变量 | 内外边距一致 |
| **圆角** | `radius.ts` (sm/md/lg/full) | `--radius-*` CSS 变量 | 按钮、卡片、输入框圆角 |
| **字体** | `typography.ts` (fontSize/weight/family) | CSS 变量 | 字号、行高、字重 |
| **动画** | `@emotion/react` keyframes | SCSS `@keyframes` / Angular `@keyframes` | 持续时间、缓动函数一致 |
| **交互状态** | `:hover` / `:active` / `:focus-visible` | 同上 | 悬停变色、点击缩放、焦点环 |

---

## 已迁移组件对照表

### 1. AIHub (`apps/web/src/components/AIHub.tsx` → `apps/web-angular/src/app/components/ai-hub/`)

| 子区域 | React 样式 | Angular 样式 | 状态 |
|--------|-----------|-------------|------|
| SegmentedControl | `SegmentedControl.tsx` | `segmented-control/` | ✅ 已迁移 |
| Chat Area | `ChatContainer`, `MessageBubble`, `InputArea` | 需完整实现 | ⚠️ 简化版 |
| Image Generation | `ImageSection`, `PromptArea` | 缺失 | ❌ 未迁移 |
| TTS | `TTSSection`, `AudioPlayer` | 缺失 | ❌ 未迁移 |
| Model Selector | `ModelSelector` + Provider/Model select | 缺失 | ❌ 未迁移 |

**测试检查项：**
- [ ] SegmentedControl tab 切换动画一致
- [ ] Chat 输入框 placeholder 国际化
- [ ] 消息气泡对齐方向（user 右/assistant 左）
- [ ] 消息气泡圆角 (radius-lg + radius-sm)
- [ ] 加载 spinner 颜色与旋转速度
- [ ] 空状态图标与文案国际化
- [ ] Quick action buttons 样式与悬停效果

### 2. SegmentedControl (`apps/web/src/components/SegmentedControl.tsx` → `apps/web-angular/src/app/components/segmented-control/`)

| 属性 | React | Angular | 状态 |
|------|-------|---------|------|
| 容器 | `Container` (inline-flex, padding 3px) | `.container` (inline-flex, padding 3px) | ✅ 一致 |
| 选项按钮 | `Option` (padding 8px 20px) | `.option` (padding 8px 20px) | ✅ 一致 |
| 激活态 | `box-shadow: ${shadows.sm}` | `box-shadow: 0 2px 4px rgba(0,0,0,0.06)` | ⚠️ 阴影值需核对 |
| 字体 | 14px medium | 14px 500 | ✅ 一致 |
| ARIA | `role="tablist"`, `aria-selected` | 同上 | ✅ 一致 |

**测试检查项：**
- [ ] 激活选项白色背景 + 阴影
- [ ] 非激活选项透明背景 + hover 变色
- [ ] focus-visible 焦点环样式
- [ ] disabled 状态 opacity
- [ ] `box-shadow` 值完全一致

### 3. StatusBadge (`apps/web/src/components/agents/StatusBadge.tsx` → `apps/web-angular/src/app/components/agents/status-badge/`)

| 状态 | React 颜色 | Angular CSS 变量 | 状态 |
|------|-----------|-----------------|------|
| online | successLight / success | var(--color-success-light) | ✅ |
| offline | rgba(0,0,0,0.06) / textTertiary | 同上 | ✅ |
| busy | warningLight / warning | var(--color-warning-light) | ✅ |
| error | errorLight / error | var(--color-error-light) | ✅ |
| pending | primaryLight / primary | var(--color-primary-light) | ✅ |

**测试检查项：**
- [ ] Dot 圆点 (6px × 6px) + `background: currentColor`
- [ ] padding 4px 10px
- [ ] font-size xs + font-weight medium
- [ ] border-radius full (圆角胶囊)
- [ ] showDot 可控制显示/隐藏

### 4. VisionPanel (`apps/web/src/components/VisionPanel.tsx` → `apps/web-angular/src/app/components/ai/vision-panel/`)

| 功能 | React | Angular | 状态 |
|------|-------|---------|------|
| Tab 切换 | `SegmentedControl` | 内联 `.segmented-control` | ✅ |
| 图片拖放 | `onDrop` / `onDragOver` | 同上 | ✅ |
| 预览图 | `PreviewImage` (max 100%) | `.preview-image` | ✅ |
| Zoom Modal | `ImageZoomModal` | 内联 `.zoom-modal` | ⚠️ 需合并 |
| Caption 结果 | `"{{ caption }}"` | 同上 | ✅ |
| Detection 列表 | `DetectionItem` | `.detection-item` | ✅ |
| OCR 文本 | `<pre>` 格式化 | 同上 | ✅ |
| 错误消息 | `ErrorMessage` 红色背景 | `.error-message` | ⚠️ 颜色值需核对 |

**测试检查项：**
- [ ] 拖放区域虚线边框样式
- [ ] 图片悬停放大 1.02 + 阴影
- [ ] Zoom Hint 显示/隐藏动画
- [ ] ClearButton 圆形半透明黑色
- [ ] LoadingOverlay 毛玻璃效果
- [ ] Spinner 颜色与旋转速度
- [ ] 错误消息背景色 (#ffebee) / 文字色 (#c62828)

### 5. RAGChat (`apps/web/src/components/RAGChat.tsx` → `apps/web-angular/src/app/components/ai/rag-chat/`)

| 功能 | React | Angular | 状态 |
|------|-------|---------|------|
| Toast | `ToastItem` 滑入动画 | 同上 | ✅ |
| Document Card | `DocumentCard` Apple 风格 | 同上 | ✅ |
| Skeleton Loading | shimmer 动画 | shimmer 动画 | ✅ |
| 上传状态 | FileTag success/error/uploading | 同上 | ✅ |
| Chat Message | `MessageBubble` + fadeIn | 同上 | ✅ |
| Source Badge | `SourceBadge` + expand | 同上 | ✅ |
| Markdown 渲染 | react-markdown | 简化 `renderMarkdown()` | ⚠️ 功能差距 |
| 快捷问题 | QuickActions | QuickActions | ✅ |
| 输入区 | `TextArea` + `SendButton` | 同上 | ✅ |

**测试检查项：**
- [ ] Toast slideIn 动画 (translateX 100% → 0)
- [ ] Document Card 选中态背景 primary + 阴影
- [ ] Document Card 删除动画 fadeOut
- [ ] Skeleton shimmer 渐变方向 (200% → -200%)
- [ ] Message Bubble 对齐方向
- [ ] Source Panel 左侧 3px primary 色边框
- [ ] 上传按钮禁用态 opacity

### 6. ChatMessage (`apps/web/src/components/agents/ChatMessage.tsx` → `apps/web-angular/src/app/components/agents/chat-message/`)

| 功能 | React | Angular | 状态 |
|------|-------|---------|------|
| fadeIn 动画 | `@emotion/react` keyframes | Angular `@keyframes` | ✅ |
| 用户消息 | 蓝色背景 + 白色文字 | primary 背景 | ⚠️ 需核对 |
| Assistant 消息 | surface 背景 + 边框 | 同上 | ✅ |
| 代码高亮 | rehype-highlight | 简化版本 | ⚠️ 功能差距 |
| Tool Calls | `ToolResult` 组件 | 同上 | ✅ |
| 时间戳 | `MessageTime` xs 字号 | 同上 | ✅ |
| JSON 格式化 | syntax highlight | `highlightJson()` | ✅ |

**测试检查项：**
- [ ] fadeIn 持续时间 (0.2s) + translateY (8px)
- [ ] 用户消息 border-radius (lg + sm 右下)
- [ ] Assistant 消息 border-radius (lg + sm 左下)
- [ ] 代码块 background (surface-secondary)
- [ ] JSON key 紫色 / string 绿色 / number 红色

---

## Playwright E2E 测试指南

### 安装与配置

```bash
# 安装 Playwright
pnpm add -D @playwright/test
npx playwright install chromium

# 配置文件 playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  use: {
    baseURL: 'http://localhost:4200', // Angular
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'angular', use: { baseURL: 'http://localhost:4200' } },
    { name: 'react', use: { baseURL: 'http://localhost:5173' } },
  ],
});
```

### 视觉一致性测试示例

```typescript
// e2e/visual-consistency.spec.ts
import { test, expect } from '@playwright/test';

// 通用视觉辅助函数
async function captureAndCompare(
  page: Page,
  component: string,
  reactUrl: string,
  angularUrl: string
) {
  // React 截图
  await page.goto(reactUrl);
  await page.waitForLoadState('networkidle');
  const reactScreenshot = await page.screenshot({ fullPage: false });

  // Angular 截图
  await page.goto(angularUrl);
  await page.waitForLoadState('networkidle');
  const angularScreenshot = await page.screenshot({ fullPage: false });

  // 使用像素差异检测 (阈值 0.1%)
  const diff = await page.evaluate(
    (react: Buffer, angular: Buffer) => {
      // ... 像素差异计算逻辑
    },
    reactScreenshot,
    angularScreenshot
  );

  expect(diff).toBeLessThan(0.001); // < 0.1% 差异
}

// ==================== SegmentedControl ====================

test.describe('SegmentedControl 视觉一致性', () => {
  test('tab 选项样式一致', async ({ page }) => {
    await page.goto('http://localhost:4200/ai-hub');
    const angularTabs = page.locator('.segment-button');
    const count = await angularTabs.count();
    expect(count).toBe(3); // chat / image / tts

    // 验证激活态
    await expect(angularTabs.nth(0)).toHaveClass(/active/);
    await expect(angularTabs.nth(0)).toHaveCSS('background-color', 'rgb(255, 255, 255)');

    // 验证非激活态
    await expect(angularTabs.nth(1)).not.toHaveClass(/active/);
    const bgColor = await angularTabs.nth(1).evaluate(
      (el) => getComputedStyle(el).backgroundColor
    );
    expect(bgColor).toBe('transparent');

    // 验证 box-shadow 激活态
    const boxShadow = await angularTabs.nth(0).evaluate(
      (el) => getComputedStyle(el).boxShadow
    );
    expect(boxShadow).toContain('0 2px 4px');
  });

  test('tab 切换交互一致', async ({ page }) => {
    await page.goto('http://localhost:4200/ai-hub');
    const tabs = page.locator('.segment-button');

    await tabs.nth(1).click();
    await expect(tabs.nth(1)).toHaveClass(/active/);
    await expect(tabs.nth(0)).not.toHaveClass(/active/);

    // 验证 focus-visible
    await tabs.nth(2).focus();
    const outline = await tabs.nth(2).evaluate(
      (el) => getComputedStyle(el).boxShadow
    );
    expect(outline).toContain('rgba(0, 122, 255, 0.3)');
  });
});

// ==================== StatusBadge ====================

test.describe('StatusBadge 视觉一致性', () => {
  const statusCases = [
    { status: 'online', expectedBg: 'rgb(52, 199, 89)', label: 'Online' },
    { status: 'offline', expectedBg: 'rgba(0, 0, 0, 0.06)', label: 'Offline' },
    { status: 'busy', expectedBg: 'rgb(255, 204, 0)', label: 'Busy' },
    { status: 'error', expectedBg: 'rgb(255, 59, 48)', label: 'Error' },
    { status: 'pending', expectedBg: 'rgb(0, 122, 255)', label: 'Pending' },
  ] as const;

  statusCases.forEach(({ status, expectedBg, label }) => {
    test(`状态 ${status} 颜色正确`, async ({ page }) => {
      await page.goto('http://localhost:4200/agent-chat');
      const badge = page.locator(`app-status-badge .badge--${status}`);

      await expect(badge).toBeVisible();
      await expect(badge).toContainText(label);
      const bg = await badge.evaluate((el) => getComputedStyle(el).backgroundColor);
      expect(bg).toBe(expectedBg);
    });
  });

  test('Dot 显示/隐藏一致', async ({ page }) => {
    await page.goto('http://localhost:4200/agent-chat');

    const badgeWithDot = page.locator('app-status-badge').first();
    const dot = badgeWithDot.locator('.badge__dot');
    await expect(dot).toBeVisible();

    // 验证 dot 尺寸
    const size = await dot.evaluate((el) => ({
      width: el.offsetWidth,
      height: el.offsetHeight,
    }));
    expect(size.width).toBe(6);
    expect(size.height).toBe(6);

    // 验证 dot 圆形
    const borderRadius = await dot.evaluate((el) => getComputedStyle(el).borderRadius);
    expect(borderRadius).toBe('50%');
  });
});

// ==================== VisionPanel ====================

test.describe('VisionPanel 交互一致性', () => {
  test('图片拖放区域样式', async ({ page }) => {
    await page.goto('http://localhost:4200/vision');

    const imageArea = page.locator('.image-area');
    const bg = await imageArea.evaluate((el) => getComputedStyle(el).backgroundColor);
    expect(bg).toBe('rgb(245, 245, 247)'); // #f5f5f7

    // 虚线边框
    const border = await imageArea.evaluate((el) => getComputedStyle(el).border);
    expect(border).toContain('dashed');
  });

  test('图片预览缩放交互', async ({ page }) => {
    await page.goto('http://localhost:4200/vision');

    // 上传测试图片
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('./test-image.png');

    const preview = page.locator('.preview-image');
    await expect(preview).toBeVisible();

    // 悬停验证
    await preview.hover();
    const transform = await preview.evaluate((el) => getComputedStyle(el).transform);
    expect(transform).toBe('matrix(1.02, 0, 0, 1.02, 0, 0)'); // scale(1.02)

    // Zoom Hint 显示
    const hint = page.locator('.zoom-hint');
    const hintOpacity = await hint.evaluate((el) => getComputedStyle(el).opacity);
    expect(hintOpacity).toBe('1');
  });

  test('Zoom Modal 样式', async ({ page }) => {
    await page.goto('http://localhost:4200/vision');

    // 先上传图片
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('./test-image.png');

    // 点击预览图打开 modal
    await page.locator('.preview-image').click();

    const modal = page.locator('.zoom-modal');
    await expect(modal).toBeVisible();

    // 背景色
    const bg = await modal.evaluate((el) => getComputedStyle(el).backgroundColor);
    expect(bg).toBe('rgba(0, 0, 0, 0.9)');

    // 关闭按钮
    const closeBtn = modal.locator('.zoom-close');
    await expect(closeBtn).toBeVisible();

    // 点击背景关闭
    await modal.click({ position: { x: 10, y: 10 } });
    await expect(modal).not.toBeVisible();
  });

  test('Loading Spinner 样式', async ({ page }) => {
    await page.goto('http://localhost:4200/vision');

    // 上传图片后点击分析
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('./test-image.png');
    await page.locator('.action-button').click();

    const spinner = page.locator('.spinner');
    await expect(spinner).toBeVisible();

    const borderColor = await spinner.evaluate((el) => getComputedStyle(el).borderTopColor);
    expect(borderColor).toBe('rgb(0, 113, 227)'); // #0071e3

    // 验证 animation
    const animation = await spinner.evaluate((el) => getComputedStyle(el).animation);
    expect(animation).toContain('spin');
  });
});

// ==================== RAGChat ====================

test.describe('RAGChat 交互一致性', () => {
  test('Toast 滑入动画', async ({ page }) => {
    await page.goto('http://localhost:4200/rag-chat');

    // 触发 toast (例如上传文件)
    const fileInput = page.locator('input#file-upload');
    await fileInput.setInputFiles('./test.pdf');

    const toast = page.locator('.toast-item').first();
    await expect(toast).toBeVisible();

    // 验证初始状态 (从右侧滑入)
    const transform = await toast.evaluate((el) => getComputedStyle(el).transform);
    expect(transform).toBe('matrix(1, 0, 0, 1, 0, 0)'); // 已滑入
  });

  test('Document Card 选中态', async ({ page }) => {
    await page.goto('http://localhost:4200/rag-chat');
    await page.waitForSelector('.document-card', { timeout: 5000 });

    const card = page.locator('.document-card').first();
    await card.click();

    // 验证选中态样式
    await expect(card).toHaveClass(/selected/);
    const bg = await card.evaluate((el) => getComputedStyle(el).backgroundColor);
    expect(bg).toBe('rgb(0, 122, 255)'); // primary blue
  });

  test('Document Card 删除动画', async ({ page }) => {
    await page.goto('http://localhost:4200/rag-chat');
    await page.waitForSelector('.document-card', { timeout: 5000 });

    const card = page.locator('.document-card').first();
    const deleteBtn = card.locator('.delete-button');

    // 等待删除完成
    await deleteBtn.click();
    await expect(card).toHaveClass(/deleting/);

    // 等待动画完成
    await page.waitForTimeout(500);
  });

  test('Chat 输入框样式', async ({ page }) => {
    await page.goto('http://localhost:4200/rag-chat');

    const input = page.locator('.chat-input');
    await expect(input).toBeVisible();

    // 验证边框
    const border = await input.evaluate((el) => getComputedStyle(el).borderRadius);
    expect(border).toBe('12px');

    // 验证 focus 样式
    await input.focus();
    const boxShadow = await input.evaluate((el) => getComputedStyle(el).boxShadow);
    expect(boxShadow).toContain('rgba(0, 113, 227, 0.1)');
  });

  test('Send Button 禁用态', async ({ page }) => {
    await page.goto('http://localhost:4200/rag-chat');

    const sendBtn = page.locator('.send-button');
    // 空输入时禁用
    await expect(sendBtn).toBeDisabled();

    const opacity = await sendBtn.evaluate((el) => getComputedStyle(el).opacity);
    expect(opacity).toBe('0.5');

    // 输入内容后启用
    await page.locator('.chat-input').fill('test message');
    await expect(sendBtn).toBeEnabled();
  });
});

// ==================== i18n 一致性 ====================

test.describe('国际化一致性', () => {
  test('所有 UI 文本来自 i18n', async ({ page }) => {
    await page.goto('http://localhost:4200/ai-hub');

    // 检查是否有硬编码英文
    const hardcodedStrings = [
      'Chat',
      'Image',
      'TTS',
      'Generate',
      'Send',
    ];

    for (const str of hardcodedStrings) {
      const elements = page.locator(`text="${str}"`);
      const count = await elements.count();
      if (count > 0) {
        // 检查是否是国际化文本
        const first = elements.first();
        const parent = await first.locator('..').evaluate((el) => {
          const angular = (el as HTMLElement).getAttribute('ng-reflect-i18n') ||
                         (el as HTMLElement).getAttribute('i18n');
          return angular;
        });
        expect(parent ?? 'NOT I18N').not.toBe('NOT I18N');
      }
    }
  });

  test('5 种语言切换一致性', async ({ page }) => {
    await page.goto('http://localhost:4200/ai-hub');

    // 切换语言
    const langSwitcher = page.locator('[data-testid="lang-switcher"]');
    const languages = ['en', 'zh', 'ja', 'fr', 'es'];

    for (const lang of languages) {
      await langSwitcher.selectOption(lang);
      await page.waitForTimeout(500);

      // 验证 SegmentedControl labels
      const activeTab = page.locator('.segment-button.active');
      const label = await activeTab.textContent();
      expect(label).toBeTruthy();
    }
  });
});

// ==================== 组件通信一致性 ====================

test.describe('组件通信一致性', () => {
  test('SegmentedControl → 子组件数据流', async ({ page }) => {
    await page.goto('http://localhost:4200/ai-hub');

    // 点击 Vision tab
    const tabs = page.locator('.segment-button');
    await tabs.nth(1).click();

    // 验证 VisionPanel 渲染
    await expect(page.locator('.vision-panel')).toBeVisible();

    // 点击 OCR tab
    await tabs.nth(2).click();
    await expect(page.locator('.ocr-text')).toBeVisible();
  });

  test('RAGChat 消息流', async ({ page }) => {
    await page.goto('http://localhost:4200/rag-chat');

    // 发送消息
    await page.locator('.chat-input').fill('What is this about?');
    await page.locator('.send-button').click();

    // 验证消息显示
    await expect(page.locator('.message-bubble.user')).toBeVisible();

    // 验证 assistant placeholder
    await expect(page.locator('.message-bubble:not(.user)')).toBeVisible();
  });
});
```

### 快速运行命令

```bash
# 启动 Angular dev server
pnpm --filter @ai-test/web-angular run start &

# 启动 React dev server
pnpm --filter @ai-test/web run start &

# 运行全部测试
pnpm exec playwright test

# 运行特定组件测试
pnpm exec playwright test --grep "SegmentedControl"

# 生成测试报告
pnpm exec playwright test --reporter=html
```

---

## 已知差距与修复优先级

| 优先级 | 组件 | 差距描述 | 修复方式 |
|--------|------|---------|---------|
| 🔴 高 | AIHub | Chat/Image/TTS 子区域未完整实现 | 补充 VisionPanel、RAGChat、TTS 功能 |
| 🔴 高 | VisionPanel | Zoom Modal 未提取为独立组件 | 复用 `image-zoom-modal/` |
| 🟡 中 | RAGChat | Markdown 渲染功能简化 | 集成 `marked` + `highlight.js` |
| 🟡 中 | ChatMessage | 代码高亮功能简化 | 集成 `highlight.js` |
| 🟢 低 | i18n | 部分组件仍有硬编码文本 | 统一使用 `I18nService` |

---

## 视觉差异快速排查清单

当发现 Angular 与 React 渲染不一致时，按以下顺序排查：

1. **颜色值**：检查 `theme.ts` vs CSS 变量是否完全对应
2. **间距**：检查 `spacing.ts` 的 rem/px 值
3. **圆角**：检查 `radius.ts` 的数值
4. **动画时长**：检查 `transitions` 对象的时长
5. **字体**：检查 `typography.ts` 的 fontFamily 顺序
6. **阴影**：检查 `shadows.ts` 的 rgba 值
7. **z-index**：检查模态框、Toast 的层级
8. **BEM 嵌套**：Angular SCSS 可能存在选择器优先级差异
