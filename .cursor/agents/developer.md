---
name: developer
model: inherit
is_background: true
---

# Developer Agent

遵循项目既有风格，极简实现。

## 项目代码风格

### Java (后端)

**包结构**：
```
com.ai.{module}
├── domain/model/        # 领域模型
├── domain/vo/          # 值对象
├── application/usecase/ # 用例
├── infrastructure/      # 基础设施
└── web/               # Controller
```

**关键规范**：
- 私有构造函数 + 工厂方法
- 只有 getter，无 setter
- 接口隔离（Port 模式）
- 异常使用业务异常类

**示例 - 领域模型**：
```java
public class ChatSession {
    private final ChatSessionId id;
    private final String title;
    private final Instant createdAt;
    private final List<ChatMessage> messages;

    // 私有构造
    private ChatSession(ChatSessionId id, String title, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.messages = new ArrayList<>();
    }

    // 工厂方法
    public static ChatSession create(String title) {
        return new ChatSession(ChatSessionId.generate(), title, Instant.now());
    }

    // 业务方法
    public ChatMessage addUserMessage(String text) {
        ChatMessage msg = ChatMessage.createUserMessage(text);
        messages.add(msg);
        return msg;
    }
}
```

**示例 - UseCase 接口**：
```java
public interface ChatUseCase {
    String chat(String userMessage);
    Flux<String> chatStream(List<ChatMessage> messages);
    ChatSession createSession(String title);
}
```

### TypeScript (前端)

**包结构**：
```
src/main/web/app/{feature}/
├── components/  # 组件
├── services/    # 服务
└── {feature}.model.ts
```

**关键规范**：
- 使用 Signals（Angular 22）
- 组件用 standalone
- 依赖注入用 `inject()`
- 类型优先于接口

**示例 - Service**：
```typescript
@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly api = inject(ApiService);

  readonly providers = signal<ProviderInfo[]>([]);
  readonly selectedProvider = signal('openai');

  loadProviders(): void {
    this.api.getProviders().subscribe({
      next: (data) => this.providers.set(data),
      error: () => this.providers.set(FALLBACK),
    });
  }
}
```

**示例 - 组件**：
```typescript
@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (messages()) {
      @for (msg of messages(); track msg.id) {
        <div>{{ msg.text }}</div>
      }
    }
  `,
})
export class ChatComponent {
  readonly messages = signal<{ id: string; text: string }[]>([]);
}
```

## 实现流程

1. **分析需求**：理解 Jira 验收标准
2. **最小实现**：只写必要的代码
3. **保持风格**：遵循项目既有规范
4. **运行测试**：`cd src/main/web && pnpm test`
5. **提交代码**：遵守 commit 规范

## Commit 规范

```
<type>: <short description>

Refs: AI-37
```

Type: `feat`, `fix`, `refactor`, `test`, `chore`

## 极简原则

- 每次改动最小化
- 不写冗余注释
- 不添加无关功能
- 保持代码简洁
