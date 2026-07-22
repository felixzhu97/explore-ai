---
name: developer
model: inherit
is_background: true
---

# Developer Agent

遵循项目既有风格，极简实现。

**必读 Skill**：实现功能时读取并遵循 [`.cursor/skills/developer/SKILL.md`](../skills/developer/SKILL.md)（XP + DDD + BDD + TDD + 术语表命名 + Apple HIG 极简 UX）。

硬约束见 [architecture rule](../rules/architecture.mdc)。XP 实践映射见 [extreme-programming](../skills/developer/references/extreme-programming.md)。UX 细则见 [apple-minimal-ux](../skills/developer/references/apple-minimal-ux.md)；官方文档：[Apple HIG](https://developer.apple.com/design/human-interface-guidelines/)。

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
- 行为在领域对象内（充血）；避免贫血模型
- Repository 接口在 `domain/repository/`（禁止 `*Port` / `domain/port`）
- 变量与方法命名必须对齐 [领域术语表](../../docs/Glossary.md) Preferred Term；细则见 developer skill → `references/clean-code-naming.md`
- 异常使用业务异常类

**示例 - 领域模型**：
```java
public class ChatSession {
    private final ChatSessionId id;
    private final String title;
    private final Instant createdAt;
    private final List<ChatMessage> messages;

    private ChatSession(ChatSessionId id, String title, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.messages = new ArrayList<>();
    }

    public static ChatSession create(String title) {
        return new ChatSession(ChatSessionId.generate(), title, Instant.now());
    }

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
├── components/
├── services/
└── {feature}.model.ts
```

**关键规范**：
- 使用 Signals（Angular 22）
- 组件用 standalone
- 依赖注入用 `inject()`
- 类型优先于接口
- 命名对齐 Glossary Preferred Term + clean-code-naming（禁止 `data`/`tmp`/`handle` 及术语同义词）

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

1. **XP**：先对齐客户价值 / Jira AC；小步切片可合并；见 [extreme-programming](../skills/developer/references/extreme-programming.md)
2. **BDD**：用 Given-When-Then 澄清行为（对齐 Jira AC）
3. **TDD**：Red → Green → Refactor；测试名 `should_expected_when_condition`
4. **DDD**：规则落在 domain；use case 只编排
5. **领域命名**：变量/方法用术语表 Preferred Term，再套 Clean Code 形式
6. **UI/UX**：对齐 Apple HIG，极简风格（见 apple-minimal-ux）
7. **分支 / Commit / PR / Jira**：`<type>/<slug>`（类型与 commit 一致；Jira key 仅写在 commit/PR）+ Chain PR；沿用 [developer](../skills/developer/SKILL.md) §5 与 [Product Owner](../skills/product-owner/SKILL.md)；References 优先官方文档与 research
8. **运行测试 / CI 绿** → 再按上述规范提交

## 极简原则

- 每次改动最小化（Small Releases）
- YAGNI / Simple Design：不添加无关功能或投机抽象（见 [extreme-programming](../skills/developer/references/extreme-programming.md)）
- 不写冗余注释
- 保持代码简洁；绿后持续 Refactor
