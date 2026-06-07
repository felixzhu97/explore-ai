---
name: angular-migration
description: 迁移前端从 React 迁移到 Angular 最新稳定版本 (Angular 20+)。包含组件迁移、服务重构、路由转换、状态管理、测试适配等完整流程指南。
---

# React to Angular 迁移指南

## 核心概念对照

| React 概念 | Angular 概念 |
|------------|-------------|
| `useState` | `signal()` / `input()` |
| `useEffect` | `effect()` / `ngOnInit` |
| `useMemo` | `computed()` |
| `useCallback` | `bind` / Arrow function |
| `Context` | `inject()` + DI |
| `useRef` | `viewChild()` / `elementRef` |
| `Redux/Zustand` | `@ngrx/signals` / `signalStore` |
| `React Router` | Angular Router |
| `CSS Modules` | SCSS + BEM / CSS Layers |
| `JSX` | Template + HTML |
| `Props` | `@Input()` / `@Output()` |

## Angular 项目初始化

```bash
# 安装 pnpm
npm install -g pnpm

# 安装 Angular CLI
pnpm add -g @angular/cli@latest

# 创建新项目
ng new frontend --routing --style=scss --ssr=false --skip-git

# 启动开发服务器
cd frontend && pnpm start
```

## 迁移检查清单

### Phase 1: 项目结构映射

```
React                    → Angular
─────────────────────────────────────
src/
├── components/    →   src/app/components/
├── hooks/         →   services / signals
├── contexts/      →   inject() providers
├── pages/         →   route components
├── utils/         →   shared utilities
├── api/           →   http services
└── store/         →   signalStore / ngrx
```

### Phase 2: 核心迁移步骤

#### 1. 入口文件

```typescript
// React: main.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

```typescript
// Angular: main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
```

#### 2. 组件转换

```typescript
// React Component
interface Props { title: string; count: number; onIncrement: () => void }

export const Counter = ({ title, count, onIncrement }: Props) => {
  return (
    <div>
      <h1>{title}</h1>
      <p>Count: {count}</p>
      <button onClick={onIncrement}>Increment</button>
    </div>
  );
};
```

```typescript
// Angular Component
import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-counter',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div>
      <h1>{{ title() }}</h1>
      <p>Count: {{ count() }}</p>
      <button (click)="increment.emit()">Increment</button>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CounterComponent {
  title = input.required<string>();
  count = input<number>(0);
  increment = output<void>();
}
```

#### 3. 状态管理 (Signals)

```typescript
// React + useState
const [user, setUser] = useState<User | null>(null);
const [loading, setLoading] = useState(false);

// Angular Signals
import { signal, computed } from '@angular/core';

user = signal<User | null>(null);
loading = signal(false);
fullName = computed(() => `${user()?.firstName} ${user()?.lastName}`);
```

#### 4. 服务注入

```typescript
// React Hook
const useAuth = () => {
  const [user, setUser] = useState(null);
  // ... auth logic
  return { user, login, logout };
};
```

```typescript
// Angular Service
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private _user = signal<User | null>(null);
  user = this._user.asReadonly();

  login(credentials: LoginDto) {
    return this.http.post<User>('/api/auth/login', credentials);
  }
}
```

#### 5. 路由迁移

```typescript
// React Router v6
const routes: Routes = [
  <Route path="/users/:id" element={<UserProfile />} />,
  <Route path="/dashboard" element={<Dashboard />} />,
];

// Angular Router
const routes: Routes = [
  { path: 'users/:id', loadComponent: () => import('./users/profile.component').then(m => m.ProfileComponent) },
  { path: 'dashboard', loadComponent: () => import('./dashboard.component').then(m => m.DashboardComponent) },
];
```

#### 6. HTTP 请求

```typescript
// React fetch
const response = await fetch('/api/users');
const users = await response.json();

// Angular HttpClient
import { inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  getUsers() {
    return this.http.get<User[]>('/api/users');
  }
}
```

## 样式迁移

```scss
// React CSS-in-JS / CSS Modules
// .button { padding: 8px 16px; background: blue; }

// Angular SCSS with BEM
.button {
  padding: $spacing-xs $spacing-md;
  background: $system-blue;
  border-radius: $radius-sm;

  &--primary { background: $system-blue; }
  &--secondary { background: $system-gray; }

  &:active { transform: scale(0.97); }
  &:disabled { opacity: 0.5; }
}
```

## 测试迁移

```typescript
// React + Testing Library
const { getByText } = render(<Counter title="Test" count={5} />);
expect(getByText('Count: 5')).toBeInTheDocument();

// Angular + TestBed
import { ComponentFixture, TestBed } from '@angular/core/testing';

let fixture: ComponentFixture<CounterComponent>;
let component: CounterComponent;

beforeEach(async () => {
  await TestBed.configureTestingModule({
    imports: [CounterComponent],
  }).compileComponents();

  fixture = TestBed.createComponent(CounterComponent);
  component = fixture.componentInstance;
  fixture.detectChanges();
});

it('should display count', () => {
  const h1 = fixture.nativeElement.querySelector('p');
  expect(h1.textContent).toContain('Count: 0');
});
```

## 常见陷阱

1. **变更检测**: 使用 `ChangeDetectionStrategy.OnPush` 提升性能
2. **信号使用**: 组件间通信优先使用 `input()`/`output()` 而非共享状态
3. **依赖注入**: 避免 service 中直接实例化，使用 `inject()` 或 constructor
4. **模板语法**: 使用 `*ngIf`/`@if` (Angular 17+)，避免 `*ngIf` 和 `*ngFor` 混用
5. **类型安全**: 充分利用 TypeScript strict mode 和 Angular 模板类型检查

## 多服务集成调试

### 后端 404 错误诊断流程

> **重要**：遇到 404 错误时，不要假设"后端未运行"。按以下顺序排查：

```
1. 验证后端运行：ps aux | grep uvicorn && curl -I http://localhost:PORT/
2. 检查实际 URL：浏览器 DevTools → Network → 失败的请求
3. 对比三方路径：Frontend → proxy.conf.json → Backend Route
4. 检查 CORS：curl -I -H "Origin: http://localhost:4200" http://localhost:PORT/
```

### URL 路径对齐原则

| 服务 | 前端调用 | Proxy 目标 | Backend 路由 |
|------|---------|------------|-------------|
| TTS | /api/speech | /tts | @app.post("/tts") |
| Vision | /api/vision | /vision | @app.post("/vision") |
| RAG | /api/rag/* | /* | @app.post("/rag/*") |

### CORS 配置要求

```python
# backend main.py
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:4200",  # Angular
        "http://localhost:5173",  # React
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

### proxy.conf.json 示例

```json
{
  "/api/speech": {
    "target": "http://localhost:8001",
    "pathRewrite": { "^/api/speech": "/tts" }
  },
  "/api/vision": {
    "target": "http://localhost:8003",
    "pathRewrite": { "^/api/vision": "/vision" }
  }
}
```

### 调试命令清单

```bash
# 1. 检查后端进程
ps aux | grep -E "(uvicorn|fastapi)" | grep -v grep

# 2. 直接测试后端路由
curl -I http://localhost:8001/tts

# 3. 检查 CORS 响应头
curl -I -H "Origin: http://localhost:4200" http://localhost:8001/tts

# 4. 查看实际请求
# 浏览器 DevTools → Network → 右键请求 → Copy → Copy as cURL
```

## 验证清单

- [ ] 所有 React 组件已转换为 Angular 组件
- [ ] 使用 Angular Signals 进行状态管理
- [ ] 路由懒加载正确配置
- [ ] HTTP 服务使用 HttpClient
- [ ] 样式使用 SCSS + BEM 规范
- [ ] 单元测试覆盖核心组件
- [ ] E2E 测试验证关键流程
- [ ] 性能: 使用 OnPush 变更检测
