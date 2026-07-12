# AI-Explore Project

> ⚠️ 本文件由 `.claude/generate-rules.sh` 自动生成
> 修改规范请编辑 `.cursor/rules/*.mdc`，然后运行此脚本重新生成


<!-- source: .cursor/rules/angular-20.mdc -->

# Angular Best Practices

This project adheres to modern Angular best practices, emphasizing maintainability, performance, accessibility, and scalability.

## TypeScript Best Practices

* **Strict Type Checking:** Always enable and adhere to strict type checking. This helps catch errors early and improves code quality.
* **Prefer Type Inference:** Allow TypeScript to infer types when they are obvious from the context. This reduces verbosity while maintaining type safety.
    * **Bad:**
        ```typescript
        let name: string = 'Angular';
        ```
    * **Good:**
        ```typescript
        let name = 'Angular';
        ```
* **Avoid `any`:** Do not use the `any` type unless absolutely necessary as it bypasses type checking. Prefer `unknown` when a type is uncertain and you need to handle it safely.

## Core Guidelines

* **Standalone Components:** Always use standalone components, directives, and pipes. Avoid using `NgModules` for new features or refactoring existing ones.
* **Implicit Standalone:** When creating standalone components, you do not need to explicitly set `standalone: true` inside the `@Component`, `@Directive` and `@Pipe` decorators, as it is implied by default.
    * **Bad:**
        ```typescript
        @Component({
          standalone: true,
          // ...
        })
        export class MyComponent {}
        ```
    * **Good:**
        ```typescript
        @Component({
          // `standalone: true` is implied
          // ...
        })
        export class MyComponent {}
        ```
* **Signals for State Management:** Utilize Angular Signals for reactive state management within components and services.
* **Lazy Loading:** Implement lazy loading for feature routes to improve initial load times of your application.
* **NgOptimizedImage:** Use `NgOptimizedImage` for all static images to automatically optimize image loading and performance.
* **Host bindings:** Do NOT use the `@HostBinding` and `@HostListener` decorators. Put host bindings inside the `host` object of the `@Component` or `@Directive` decorator instead.

## Components

* **Single Responsibility:** Keep components small, focused, and responsible for a single piece of functionality.
* **`input()` and `output()` Functions:** Prefer `input()` and `output()` functions over the `@Input()` and `@Output()` decorators for defining component inputs and outputs.
    * **Old Decorator Syntax:**
        ```typescript
        @Input() userId!: string;
        @Output() userSelected = new EventEmitter<string>();
        ```
    * **New Function Syntax:**
        ```typescript
        import { input, output } from '@angular/core';

        // ...
        userId = input<string>('');
        userSelected = output<string>();
        ```
* **`computed()` for Derived State:** Use the `computed()` function from `@angular/core` for derived state based on signals.
* **Inline Templates:** Prefer inline templates (template: `...`) for small components to keep related code together. For larger templates, use external HTML files.
* **Reactive Forms:** Prefer Reactive forms over Template-driven forms for complex forms, validation, and dynamic controls due to their explicit, immutable, and synchronous nature.
* **No `ngClass` / `NgClass`:** Do not use the `ngClass` directive. Instead, use native `class` bindings for conditional styling.
    * **Bad:**
        ```html
        <section [ngClass]="{'active': isActive}"></section>
        ```
    * **Good:**
        ```html
        <section [class.active]="isActive"></section>
        <section [class]="{'active': isActive}"></section>
        <section [class]="myClasses"></section>
        ```
* **No `ngStyle` / `NgStyle`:** Do not use the `ngStyle` directive. Instead, use native `style` bindings for conditional inline styles.
    * **Bad:**
        ```html
        <section [ngStyle]="{'font-size': fontSize + 'px'}"></section>
        ```
    * **Good:**
        ```html
        <section [style.font-size.px]="fontSize"></section>
        <section [style]="myStyles"></section>
        ```

## State Management

* **Signals for Local State:** Use signals for managing local component state.
* **`computed()` for Derived State:** Leverage `computed()` for any state that can be derived from other signals.
* **Pure and Predictable Transformations:** Ensure state transformations are pure functions (no side effects) and predictable.
* **Signal value updates:** Do NOT use `mutate` on signals, use `update` or `set` instead.

## Templates

* **Simple Templates:** Keep templates as simple as possible, avoiding complex logic directly in the template. Delegate complex logic to the component's TypeScript code.
* **Native Control Flow:** Use the new built-in control flow syntax (`@if`, `@for`, `@switch`) instead of the older structural directives (`*ngIf`, `*ngFor`, `*ngSwitch`).
    * **Old Syntax:**
        ```html
        <section *ngIf="isVisible">Content</section>
        <section *ngFor="let item of items">{{ item }}</section>
        ```
    * **New Syntax:**
        ```html
        @if (isVisible) {
          <section>Content</section>
        }
        @for (item of items; track item.id) {
          <section>{{ item.name }}</section>
        }
        ```
* **Async Pipe:** Use the `async` pipe to handle observables in templates. This automatically subscribes and unsubscribes, preventing memory leaks.

## Services

* **Single Responsibility:** Design services around a single, well-defined responsibility.
* **`providedIn: 'root'`:** Use the `providedIn: 'root'` option when declaring injectable services to ensure they are singletons and tree-shakable.
* **`inject()` Function:** Prefer the `inject()` function over constructor injection when injecting dependencies, especially within `provide` functions, `computed` properties, or outside of constructor context.
    * **Old Constructor Injection:**
        ```typescript
        constructor(private myService: MyService) {}
        ```
    * **New `inject()` Function:**
        ```typescript
        import { inject } from '@angular/core';

        export class MyComponent {
          private myService = inject(MyService);
          // ...
        }
        ```

<!-- source: .cursor/rules/angular-standards.mdc -->

# Angular Standards

## Project Structure

```
src/main/web/
├── app/
│   ├── {feature}/
│   │   ├── components/
│   │   │   └── {feature}.component.ts
│   │   ├── {feature}.service.ts
│   │   └── {feature}.model.ts
│   └── app.routes.ts
├── styles.css
└── environments/
```

## Component

```typescript
@Component({
  selector: 'app-user-card',
  standalone: true,
  imports: [CommonModule, ListItemComponent],
  templateUrl: './user-card.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserCardComponent {
  user = input.required<User>();
  edit = output<User>();

  fullName = computed(() => `${this.user().firstName} ${this.user().lastName}`);

  handleEdit = () => this.edit.emit(this.user());
}
```

## Template

```html
@if (user(); as user) {
  <div>{{ fullName() }}</div>
} @else {
  <app-skeleton />
}

@for (item of items(); track item.id) {
  <app-list-item [item]="item" />
}
```

## Service

```typescript
@Injectable({ providedIn: 'root' })
export class ChatService {
  messages = signal<ChatMessage[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  private http = inject(HttpClient);

  sendMessage(content: string): Observable<ChatResponse> {
    this.loading.set(true);
    return this.http.post<ChatResponse>('/api/chat', { content }).pipe(
      tap(response => this.messages.update(msgs => [...msgs, response])),
      catchError(this.handleError),
      finalize(() => this.loading.set(false))
    );
  }

  private handleError = (error: HttpErrorResponse) => {
    this.error.set(error.message);
    return throwError(() => error);
  };
}
```

## Routing

```typescript
export const routes: Routes = [
  { path: '', redirectTo: 'chat', pathMatch: 'full' },
  { path: 'chat', loadComponent: () => import('./chat/chat.component').then(m => m.ChatComponent) },
  { path: 'rag', loadComponent: () => import('./rag/rag.component').then(m => m.RagComponent) },
];
```

## Style

```html
<button class="bg-primary text-white rounded-lg shadow-card">Submit</button>
```

## Type

```typescript
interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}
```

## Checklist

- [ ] Standalone components with OnPush
- [ ] Signals for state, Observables for async
- [ ] Lazy loading routes
- [ ] Tailwind for styling

## References

- [Angular](https://angular.dev)
- [Angular In-depth Guides Overview](https://angular.dev/essentials)
- [Signals](https://angular.dev/guide/signals)
- [Components](https://angular.dev/guide/components)
- [Templates](https://angular.dev/guide/templates)
- [Directives](https://angular.dev/guide/directives)
- [Dependency Injection](https://angular.dev/guide/di)
- [Routing](https://angular.dev/guide/routing)
- [Forms](https://angular.dev/guide/forms)
- [HTTP Client](https://angular.dev/guide/http)
- [Server-side & hybrid-rendering](https://angular.dev/guide/ssr)
- [Testing](https://angular.dev/guide/testing)
- [Angular Aria](https://angular.dev/guide/a11y)
- [Internationalization](https://angular.dev/guide/i18n)
- [Animations](https://angular.dev/guide/animations)
- [Drag and drop](https://angular.dev/guide/drag-drop)

## Developer Tools

- [Angular CLI](https://angular.dev/tools/cli)
- [Libraries](https://angular.dev/tools/libraries)
- [DevTools](https://angular.dev/tools/devtools)
- [Language Service](https://angular.dev/tools/language-service)

## Build with AI

- [Get Started](https://angular.dev/ai)
- [LLM prompts and AI IDE setup](https://angular.dev/ai/develop-with-ai)
- [Agent Skills](https://angular.dev/ai/agent-skills)
- [Angular CLI MCP Server setup](https://angular.dev/ai/mcp)
- [Angular AI Tutor](https://angular.dev/ai/ai-tutor)
- [Design Patterns](https://angular.dev/ai/design-patterns)
- [WebMCP](https://angular.dev/ai/webmcp)

## Best Practices

- [Style Guide](https://angular.dev/style-guide)
- [Security](https://angular.dev/best-practices/security)
- [Accessibility](https://angular.dev/best-practices/a11y)
- [Unhandled errors in Angular](https://angular.dev/best-practices/error-handling)
- [Performance](https://angular.dev/best-practices/performance)
- [Keeping up-to-date](https://angular.dev/update)

## Developer Events

- [Angular v22](https://angular.dev/events/v22)
- [Angular v21](https://angular.dev/events/v21)

## Extended Ecosystem

- [NgModules](https://angular.dev/guide/ngmodules/overview)
- [Legacy Animations](https://angular.dev/guide/legacy-animations)
- [Animations](https://angular.dev/guide/animations)
- [Using RxJS with Angular](https://angular.dev/ecosystem/rxjs-interop)
- [Service Workers & PWAs](https://angular.dev/ecosystem/service-workers)
- [Web Workers](https://angular.dev/ecosystem/web-workers)
- [Custom Build Pipeline](https://angular.dev/ecosystem/custom-build-pipeline)
- [Tailwind](https://angular.dev/guide/tailwind)
- [AngularFire](https://github.com/angular/angularfire)
- [Google Maps](https://angular-components.mintlify.app/introduction)
- [Google Pay](https://developers.google.com/pay/api/web)
- [YouTube Player](https://angular-components.mintlify.app/youtube-player/installation)
- [Angular CDK](https://material.angular.dev/cdk/categories)
- [Angular Material](https://material.angular.dev/)
- [Angular Community](https://angular.love)
- [Angular Blog](https://blog.angular.dev)

<!-- source: .cursor/rules/architecture.mdc -->

# Architecture Standards

## Dependency Rule

```
web → application → domain ← infrastructure
```

**Domain has NO dependencies on other layers.**

> **Canonical source:** Layer structure, dependency rule, and Hexagonal terminology mapping live in this file (`architecture.mdc`). `java-standards.mdc` and `CLAUDE.md` link here — do not duplicate architecture diagrams elsewhere.

## Terminology Index (Hexagonal ↔ This Project)

External references (Spring docs, Alistair Cockburn, legacy modules) often use **Ports & Adapters (Hexagonal)** naming. **This repo standardizes on Clean Architecture layers and package names below.**

| Hexagonal concept | Hexagonal layout | This project layer | This project package |
|-------------------|------------------|--------------------|----------------------|
| Driving adapter | `adapter/in` | Web + Application | `{module}/web/`, `{module}/application/` |
| Driven adapter | `adapter/out` | Infrastructure | `{module}/infrastructure/` |
| Outbound port | `domain/port` | Repository interface | `{module}/domain/repository/` |
| Domain core | `domain` | Domain | `{module}/domain/model`, `domain/vo`, `domain/service` |
| Composition / wiring | `config` | Spring configuration | `{module}/infrastructure/config/`, `common/config/` |

### Forbidden in New Code

- `domain/port/` packages — use `domain/repository/` (or `domain/service/` for domain services)
- `*Port` interface suffix — prefer `*Repository`, `*Gateway`, or a domain-specific name (e.g. `ConversationMemoryRepository`)
- `adapter/in` / `adapter/out` packages — use `web/`, `application/`, `infrastructure/`

### Legacy (migrate when touched)

Some modules still contain Hexagonal-style paths (e.g. `rag/domain/port/DocumentReader`). Treat them as **legacy drift**; new abstractions follow `domain/repository/` in this document.

### Related Reading

- [Ports and Adapters - Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- Java coding conventions (naming, REST, tests): `java-standards.mdc`

## Layers

| Layer | Contains |
|-------|----------|
| `domain/` | Entities, Value Objects, Repository interfaces |
| `application/` | Use Cases, Facades |
| `infrastructure/` | Repository implementations, External adapters |
| `web/` | Controllers, DTOs |

## Project Structure

```
src/main/java/com/ai/
├── {module}/                # Feature module
│   ├── domain/
│   │   ├── model/           # Entities, Aggregates
│   │   ├── vo/              # Value Objects
│   │   └── repository/      # Repository interfaces
│   ├── application/
│   │   └── usecase/         # Use cases
│   ├── infrastructure/
│   │   └── persistence/    # Repository implementations
│   └── web/
│       ├── controller/      # REST controllers
│       └── dto/             # Request/Response DTOs
└── common/                  # Shared code
    └── exception/           # Shared exceptions
```

## Domain Rules

- No framework annotations in domain
- Entities: private constructor + factory method
- Value Objects: immutable `record`
- Repository: interface in domain, implementation in infrastructure

## Examples

### Domain

```java
public class ChatSession {
    private final ChatSessionId id;
    private SessionStatus status;

    private ChatSession(ChatSessionId id) {
        this.id = id;
        this.status = SessionStatus.ACTIVE;
    }

    public static ChatSession create() {
        return new ChatSession(new ChatSessionId(UUID.randomUUID()));
    }

    public ChatSessionId id() {
        return id;
    }
}

public record ChatSessionId(UUID value) {}

public interface ChatSessionRepository {
    Optional<ChatSession> findById(ChatSessionId id);
    void save(ChatSession session);
}
```

### Application

```java
public record ChatRequest(String message) {}
public record ChatResponse(String reply) {}

public interface ChatUseCase {
    ChatResponse chat(ChatRequest request);
}

@Service
@RequiredArgsConstructor
class ChatUseCaseImpl implements ChatUseCase {
    private final ChatSessionRepository repository;

    @Override
    public ChatResponse chat(ChatRequest request) {
        return new ChatResponse("reply");
    }
}
```

### Infrastructure

```java
@Repository
@RequiredArgsConstructor
class InMemoryChatSessionRepository implements ChatSessionRepository {
    private final Map<ChatSessionId, ChatSession> store = new ConcurrentHashMap<>();

    @Override
    public Optional<ChatSession> findById(ChatSessionId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(ChatSession session) {
        store.put(session.id(), session);
    }
}
```

### Web

```java
@RestController
@RequiredArgsConstructor
class ChatController {
    private final ChatUseCase chatUseCase;

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatUseCase.chat(request);
    }
}
```

## Checklist

- [ ] Domain has no outward dependencies
- [ ] No circular dependencies
- [ ] Entities encapsulate behavior

<!-- source: .cursor/rules/bdd-standards.mdc -->

# BDD Behavior-Driven Development Standards

## Core Principles

- **Given-When-Then** structure
- **Business language**, not implementation details
- **Single responsibility** for each Scenario
- **Executable documentation** is the test

## Gherkin Keywords

| Keyword | Purpose | Repeatable |
|---------|---------|------------|
| Feature | Feature name | Once per file |
| Background | Shared preconditions | Once per file |
| Scenario | Specific scenario | Multiple |
| Given | Precondition | Multiple |
| When | Trigger action | Once per Scenario |
| Then | Expected result | Multiple |
| And/But | Additional condition | Yes |
| Scenario Outline | Parameterized scenario | One |
| Examples | Parameter table | Once per Outline |

## Scenario Writing Standards

### Good Naming

```gherkin
# Business value focused
Scenario: VIP member enjoys 10% discount
Scenario: Order remains pending after payment failure

# Implementation focused - AVOID
Scenario: Call calculateDiscount method
Scenario: Save method called after clicking button
```

### Structure

```
Feature: [What the feature does]
  As a [role]
  I want [action]
  So that [benefit]

  Scenario: [What happens]
    Given [precondition]
    When [action]
    Then [expected result]
```

### Avoid Over-Detailing

```gherkin
# Focus on outcomes, not implementation
Scenario: Automatically apply available discount at checkout
  Given shopping cart has items and has available coupon
  When checkout is completed
  Then discount amount should be deducted from total

# Implementation details - AVOID
Scenario: Apply discount
  Given user clicks checkout
  Then system calls discount service
  And discount service queries user coupons
```

## Quick Checklist

### Business Value

- [ ] Scenario has clear business context
- [ ] Describes user/role and business value

### Completeness

- [ ] Covers positive and exception flows
- [ ] Covers boundary conditions
- [ ] Parameterized where appropriate (Scenario Outline)

### Readability

- [ ] Uses business terminology
- [ ] No technical implementation details
- [ ] Given/When/Then structure is clear
- [ ] Steps are concise (< 20 words)

### Executability

- [ ] Each Step can be automated
- [ ] No manual steps
- [ ] Data is explicit in Examples or Data Tables

## BDD and TDD Integration

```
BDD Acceptance Tests (What)
    ↓ Source of Requirements
TDD Unit Tests (How)
    ↓ Implementation
Code
```

| BDD Layer | TDD Layer | Purpose |
|-----------|-----------|---------|
| Feature | Test Suite | Organize by feature |
| Scenario | Integration Test | End-to-end flow |
| Step | Unit Test | Specific behavior |

## Detailed Guidelines

For detailed examples and Step Definitions, refer to:

## References

- [Behaviour-Driven Development - Cucumber Official](https://cucumber.io/docs/bdd/)
- [Gherkin Reference - Cucumber Official](https://cucumber.io/docs/gherkin/)

<!-- source: .cursor/rules/clean-code.mdc -->

# Clean Code Standards

> Based on Robert C. Martin's "Clean Code: A Handbook of Agile Software Craftsmanship"

## Core Principles

1. **Small** — Keep functions, classes, and modules small
2. **Well Named** — Names should state purpose clearly
3. **Organized** — Related code grouped together vertically
4. **Ordered** — Most important concepts first


## Functions

### Principles

1. **Small** — Functions should be small; most just a handful of lines
2. **Do One Thing** — Functions should do one thing, do it well, do it only
3. **Few Arguments** — Zero is best, one is okay, two is acceptable, three fight it
4. **No Side Effects** — Calling the function should not produce unexpected changes
5. **No Flag Arguments** — Don't pass boolean to switch behavior

### Examples

```java
// ❌ BAD - Flag argument, multiple responsibilities
public void processOrder(Order order, boolean validate, boolean sendEmail) {
  if (validate) validate(order);
  save(order);
  if (sendEmail) sendConfirmation(order);
}

// ✅ GOOD - No flags, single responsibility
public void validateOrder(Order order) { /* ... */ }
public void saveOrder(Order order) { /* ... */ }
public void sendOrderConfirmation(Order order) { /* ... */ }
```

```typescript
// ❌ BAD - Flag argument
function createReport(includeCharts: boolean, includeSummary: boolean) { /* ... */ }

// ✅ GOOD - Explicit functions
function createDetailedReport() { /* ... */ }
function createSummaryReport() { /* ... */ }
```

```java
// ❌ BAD - Too many arguments
public User createUser(String firstName, String lastName, String email,
  int age, boolean isActive, String role) {
  // ...
}

// ✅ GOOD - Use data structure for many arguments
public record CreateUserRequest(
  String firstName,
  String lastName,
  String email,
  int age,
  boolean isActive,
  String role
) {}

public User createUser(CreateUserRequest request) { /* ... */ }
```

### Switch/If-Else Strategy

```java
// ❌ BAD - Switch scattered across codebase
public Money calculateDiscount(Customer customer) {
  switch (customer.getType()) {
    case "VIP": return customer.getTotal().multiply(0.2);
    case "REGULAR": return customer.getTotal().multiply(0.1);
  }
}

// ✅ GOOD - Polymorphism moves behavior closer to data
public interface DiscountPolicy {
  Money calculate(Customer customer);
}

public class VipDiscountPolicy implements DiscountPolicy {
  public Money calculate(Customer customer) {
    return customer.getTotal().multiply(0.2);
  }
}
```


## Error Handling

### Principles

1. **Fail Fast** — Validate at the beginning, fail immediately
2. **Use Exceptions** — Exceptions are for exceptional cases
3. **Provide Context** — Error messages should explain what went wrong
4. **No Silent Catches** — Don't swallow exceptions without action

### Examples

```java
// ❌ BAD - Silent catch, no action
try {
  processOrder(order);
} catch (Exception e) {
  // Do nothing
}

// ❌ BAD - Generic exception, no context
} catch (Exception e) {
  log.error("Error");
}

// ✅ GOOD - Specific exceptions with context
public void withdraw(Account account, Money amount) {
  if (amount.isNegative()) {
    throw new IllegalArgumentException("Withdrawal amount cannot be negative");
  }
  if (account.balance().isLessThan(amount)) {
    throw new InsufficientFundsException(
      account.id(), amount, account.balance()
    );
  }
  account.debit(amount);
}
```

```typescript
// ❌ BAD - Silent catch
try {
  await fetchData();
} catch (e) {
  // handling...
}

// ✅ GOOD - Fail fast with meaningful message
function divide(a: number, b: number): number {
  if (b === 0) {
    throw new Error('Division by zero is not allowed');
  }
  return a / b;
}
```


## General Rules

1. **Standard Conventions** — Follow team's coding standard
2. **Simpler is Better** — Reduce complexity wherever possible
3. **Boy Scout Rule** — Leave the code cleaner than you found it
4. **Find Root Cause** — Don't treat symptoms, fix the cause
5. **Law of Demeter** — Talk only to immediate friends
6. **Value Objects** — Prefer dedicated types over primitives
7. **Avoid Negative Conditionals** — `isValid` is clearer than `!isInvalid`


<!-- source: .cursor/rules/code-review-checklist.mdc -->

# Code Review Checklist

> Architecture compliance is verified by `architecture.mdc`. This checklist focuses on code quality, naming, testing, security, and performance.

## Code Quality

- [ ] No God Class (拆分 > 300 行的类)
- [ ] 遵循单一职责原则 (SRP)
- [ ] 方法长度不超过 20 行
- [ ] 无注释掉的代码 (使用版本控制)
- [ ] 无 TODO/FIXME 遗留
- [ ] 错误处理完善 (异常转换)
- [ ] 无硬编码值 (使用常量/配置)

## Naming Conventions

| Type | Java | TypeScript |
|------|------|------------|
| 类名 | PascalCase | PascalCase |
| 方法名 | camelCase | camelCase |
| 常量 | UPPER_SNAKE_CASE | UPPER_SNAKE_CASE |
| 包名 | lowercase | - |
| 文件名 | PascalCase.java | kebab-case.ts |

## Testing

- [ ] 领域逻辑有单元测试
- [ ] 测试遵循命名约定 (`should_expected_when_condition`)
- [ ] 无硬编码测试数据 (使用 Factory/Fixture)
- [ ] 边界条件已覆盖 (null, empty, max)
- [ ] 测试独立运行 (无依赖顺序)

## Security

- [ ] 代码中无敏感信息 (密码、Token 硬编码)
- [ ] 输入验证存在 (@Valid, Zod schema)
- [ ] SQL 注入防护 (参数化查询)
- [ ] XSS 防护 (转义输出)
- [ ] 敏感数据不记录日志

## Performance

- [ ] 无 N+1 查询问题
- [ ] 集合操作使用 Stream API (Java) / 数组方法 (TS)
- [ ] 大数据量场景考虑分页/流式处理
- [ ] 避免不必要的内存分配

## Documentation

- [ ] Public API 有 JSDoc / Javadoc
- [ ] 复杂业务逻辑有注释说明意图
- [ ] README 或 ARCHITECTURE.md 存在

## References

- [Clean Code - Robert C. Martin](https://www.goodreads.com/book/show/3735293-clean-code)
- [Code Review Best Practices](https://github.com/google/eng-practices/blob/master/review/index.md)

<!-- source: .cursor/rules/commit-pr-standards.mdc -->

# Commit & Pull Request Standards

## Core Principles

1. **One commit = One complete change**: Each commit delivers one finished piece of work
2. **Chain PRs**: Each PR is based on the previous branch (not main)
3. **References**: Always search the web in real-time for authoritative sources


## Pull Request Format

PR body mirrors the commit message. References must be identical.

```
## Summary
[Brief description of the change]

## References
- [Title](URL)

## Jira Links (if applicable)
- [Jira Issue Key](https://felixzhu.atlassian.net/browse/AI-XXX)
```


## Common Mistakes

| Mistake | Correction |
|---------|------------|
| No references | Always search web in real-time for authoritative sources |
| Multiple unrelated changes | Split into separate commits/branches/PRs |
| Partial work in commit | Complete the change before committing |
| Subject with period | Remove trailing punctuation |
| Past tense | Use imperative: `add` not `added` |
| PR base on main | Always base on previous branch |
| Co-authored-by or Made with | Do not add boilerplate signatures or tool references |


## Project Dependency Reference

When referencing dependencies in commits or PRs, use the official documentation URLs below. Keep this list updated when adding new dependencies. Prefer specific documentation pages over homepages.

### Frontend

| Library | Version | Official Docs |
|---------|---------|---------------|
| Angular | ^22.0.0 | [Angular](https://angular.dev) |
| AnalogJS | ^2.6.0 | [AnalogJS](https://analogjs.org/) |
| PrimeNG | — | [PrimeNG](https://primeng.org/installation) |
| ngx-echarts | — | [ngx-echarts](https://xieziyu.github.io/ngx-echarts/api-doc/) |
| ngx-markdown | — | [ngx-markdown](https://github.com/jfcere/ngx-markdown) |
| Tailwind CSS | ^4.3.1 | [Tailwind CSS](https://tailwindcss.com/docs) |
| PostCSS | ^8.5.15 | [PostCSS](https://postcss.org/docs/) |
| RxJS | ~7.8.0 | [RxJS](https://rxjs.dev/api) |
| DOMPurify | ^3.4.9 | [DOMPurify](https://github.com/cure53/DOMPurify/blob/master/docs/README.md) |
| Marked | ^18.0.5 | [Marked](https://marked.js.org/using_pro) |
| Zone.js | ~0.15.0 | [Zone.js](https://github.com/angular/angular.js/tree/main/packages/zone.js) |
| Sass | ^1.100.0 | [Dart Sass](https://sass-lang.com/documentation/) |
| TypeScript | ~6.0.0 | [TypeScript](https://www.typescriptlang.org/docs/handbook/intro.html) |
| ESLint | — | [ESLint](https://eslint.org/docs/latest/) |
| Vitest | ^4.0.8 | [Vitest](https://vitest.dev/guide/) |
| pnpm | (managed) | [pnpm](https://pnpm.io/cli/install) |

### Backend

| Library | Version | Official Docs |
|---------|---------|---------------|
| Java | 25 | [Java](https://docs.oracle.com/en/java/) |
| Spring Boot | (managed) | [Spring Boot](https://spring.io/projects/spring-boot) |
| Spring AI | (managed) | [Spring AI](https://spring.io/projects/spring-ai) |
| Spring Data JPA | (managed) | [Spring Data JPA](https://spring.io/projects/spring-data-jpa) |
| Spring Retry | 2.0.10 | [Spring Retry](https://docs.spring.io/spring-retry/docs/current/reference/html/) |
| Hibernate ORM | (managed) | [Hibernate ORM](https://hibernate.org/orm) |
| Liquibase | — | [Liquibase](https://www.liquibase.org) |
| Micrometer | (managed) | [Micrometer](https://micrometer.io) |
| Jackson | (managed) | [Jackson](https://github.com/FasterXML/jackson) |
| MapStruct | — | [MapStruct](https://mapstruct.org) |
| Lombok | (managed) | [Lombok](https://projectlombok.org) |
| Logbook | — | [Logbook](https://github.com/zalando/logbook) |
| H2 Database | (managed) | [H2 Database](https://www.h2database.com) |
| MySQL Connector/J | — | [MySQL Connector/J](https://dev.mysql.com/doc/connector-j) |
| PostgreSQL JDBC | (managed) | [PostgreSQL JDBC Driver](https://jdbc.postgresql.org) |
| Apache PDFBox | 3.0.3 | [Apache PDFBox](https://pdfbox.apache.org) |
| Hypersistence Utils | 3.10.0 | [Hypersistence Utils](https://docs.hypersistence.io/hypersistence-utils/) |
| JUnit | 1.20.4 | [JUnit](https://junit.org) |
| Testcontainers | 1.20.4 | [Testcontainers](https://www.testcontainers.org) |
| LangChain4j | — | [LangChain4j](https://langchain4j.dev) |
| DJL (Deep Java Library) | — | [DJL](https://djl.ai) |
| Apache OpenNLP | — | [Apache OpenNLP](https://opennlp.apache.org) |
| Apache Tika | — | [Apache Tika](https://tika.apache.org) |

### Build & Tooling

| Tool | Version | Official Docs |
|------|---------|---------------|
| Gradle | (wrapper) | [Gradle](https://gradle.org) |
| JaCoCo | 0.8.13 | [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/) |
| Error Prone | — | [Error Prone](https://errorprone.info) |
| Checkstyle | — | [Checkstyle](https://checkstyle.org) |
| Husky | — | [Husky](https://typicode.github.io/husky) |
| lint-staged | — | [lint-staged](https://github.com/okonet/lint-staged) |

### Learning References

| Resource | Official Docs |
|----------|---------------|
| Martin Fowler | [martinfowler.com](https://martinfowler.com/) |
| Clean Code / Robert C. Martin | [cleancoder.com](http://cleancoder.com/products) |
| Medium | [medium.com](https://medium.com) |
| dev.to | [dev.to](https://dev.to) |
| arXiv | [arxiv.org](https://arxiv.org) |
| Hugging Face | [huggingface.co](https://huggingface.co) |
| Agile Manifesto | [agilemanifesto.org](https://agilemanifesto.org/) |

### Design References

| Resource | Official Docs |
|----------|---------------|
| Apple Human Interface Guidelines | [developer.apple.com/design](https://developer.apple.com/design/) |
| Material Design | [m3.material.io](https://m3.material.io) |
| Angular Material Design | [material.angular.io](https://material.angular.io) |
| Tailwind CSS | [tailwindcss.com/docs](https://tailwindcss.com/docs) |
| Figma Design Systems | [figma.com/community](https://www.figma.com/community/design-systems) |

### UX References

| Resource | Official Docs |
|----------|---------------|
| Apple HIG - Visual Design | [developer.apple.com/design/human-interface-guidelines/foundations/layout](https://developer.apple.com/design/human-interface-guidelines/foundations/layout) |
| Apple HIG - Typography | [developer.apple.com/design/human-interface-guidelines/foundations/typography](https://developer.apple.com/design/human-interface-guidelines/foundations/typography) |
| Apple HIG - Color | [developer.apple.com/design/human-interface-guidelines/foundations/color](https://developer.apple.com/design/human-interface-guidelines/foundations/color) |
| Apple HIG - Motion | [developer.apple.com/design/human-interface-guidelines/foundations/motion](https://developer.apple.com/design/human-interface-guidelines/foundations/motion) |
| Apple HIG - Accessibility | [developer.apple.com/design/human-interface-guidelines/foundations/accessibility](https://developer.apple.com/design/human-interface-guidelines/foundations/accessibility) |
| shadcn/ui - Apple Design | [shadcn/ui - Apple](https://www.shadcn.io/design/apple) |
| shadcn/ui - OpenAI | [shadcn/ui - OpenAI](https://www.shadcn.io/design/openai) |
| Material Design - Motion | [m3.material.io/styles/motion](https://m3.material.io/styles/motion) |
| Tailwind - Animating with Tailwind | [tailwindcss.com/docs/animation](https://tailwindcss.com/docs/animation) |

### Jira

| Resource | URL |
|----------|-----|
| Jira Site | https://felixzhu.atlassian.net |
| Project AI (ExploreAI) | https://felixzhu.atlassian.net/projects/AI

<!-- source: .cursor/rules/java-standards.mdc -->

# Java/Spring Boot Conventions

> **Architecture (layers, dependency rule, Hexagonal ↔ Clean mapping):** see [`architecture.mdc`](./architecture.mdc).  
> Do **not** add Hexagonal package layouts (`domain/port/`, `adapter/in`, `adapter/out`) in new code.  
> AI/ML development uses the [Spring AI Skill](.cursor/skills/spring-ai/SKILL.md).

## Naming Conventions

| Type | Rule | Example |
|------|------|---------|
| Domain Model | PascalCase | `ChatSession`, `Message` |
| Value Objects | PascalCase | `SessionId`, `Status` |
| Repository | PascalCase + Repository | `ChatRepository` |
| Use Case | PascalCase + UseCase | `ChatUseCase` |
| Controller | PascalCase + Controller | `ChatController` |
| DTO | PascalCase + Request/Response | `ChatRequest` |
| Methods | camelCase | `findById` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY` |
| Package | lowercase | `com.ai.domain.model` |

## Dependency Injection

```java
@Service
@RequiredArgsConstructor
class ChatUseCaseImpl implements ChatUseCase {
    private final ChatRepository repository;
    private final ChatModel chatModel;
}
```

## REST

| Operation | Status |
|-----------|--------|
| Create | 201 |
| Success | 200 |
| No Content | 204 (void) |
| Error | 4xx/5xx |

## Validation

```java
public record ChatRequest(
    @NotBlank String message,
    String sessionId
) {}
```

## Test Naming

```
should_expectedResult_when_condition
```

## References

- [Architecture Standards](./architecture.mdc) — layers, dependency rule, Hexagonal terminology index
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Framework](https://spring.io/projects/spring-framework)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring AI](https://spring.io/projects/spring-ai)
- [Spring AI Skill](.cursor/skills/spring-ai/SKILL.md)
- [Java](https://docs.oracle.com/en/java/)

<!-- source: .cursor/rules/jira-ticket-creation.mdc -->

# Jira Ticket Creation Standards

## Configuration

This project uses Atlassian Cloud. Store these values for all Jira operations:

| Property | Value |
|----------|-------|
| Site URL | https://felixzhu.atlassian.net |
| Cloud ID | `75684fb5-daf5-4962-9581-c4948b9c12cf` |
| User Account ID | `62ee247ff15eecaf500efa39` |
| Primary Project | `AI` (ExploreAI) |

### Available Projects

| Project Key | Name | Issue Types |
|-------------|------|-------------|
| `AI` | ExploreAI | Epic, Story, Task, Subtask, Bug, Feature |
| `FVXI` | 支持 | Service Request, Incident, Task, Subtask |

> **Note**: Most tools require `cloudId` as a parameter. Always include `75684fb5-daf5-4962-9581-c4948b9c12cf` when calling Jira MCP tools.

## Ticket Structure

Every ticket must include:

- **Background**
- **User Story**
- **Acceptance Criteria**
- **Notes**

## User Story Format

Every ticket must include a user story:

```
**As a** [role]
**I want** [action/feature]
**So that** [benefit/value]
```

## Acceptance Criteria Format

Use **GIVEN-WHEN-THEN** (BDD) format:

```
**GIVEN** [precondition/state]
**WHEN** [trigger/action]
**THEN** [expected outcome]
```

### Rules

- Each GIVEN-WHEN-THEN block should cover one specific scenario
- Cover both happy path and edge cases
- Include at least 3-5 acceptance criteria per ticket
- Focus on user interaction and observable behavior, not implementation details
- Use present tense ("the user clicks" not "the user will click")

### Good Examples

```
**GIVEN** a user is logged in
**WHEN** they click the "Upload" button
**THEN** a file picker dialog opens

**GIVEN** an upload is in progress
**WHEN** the network connection is lost
**THEN** an error message is displayed and the upload can be retried
```

### Bad Examples (Avoid)

```
**WHEN** the button is clicked, call the upload API
**THEN** save the result to database

**GIVEN** the user wants to upload a file
**WHEN** they click the button
**THEN** the system works correctly
```

## OpenAI-Style Interaction Patterns

When describing UI/UX acceptance criteria, reference these interaction patterns:

| Pattern | Description |
|---------|-------------|
| Chat bubbles | User messages right-aligned, AI responses left-aligned with avatars |
| Markdown rendering | Support for code blocks, lists, bold, italic |
| Typing indicator | Animated dots during AI processing |
| Copy button | One-click copy for code and text blocks |
| Regenerate | Re-run the last query and replace the response |
| Toast notifications | Non-blocking success/error feedback |
| Skeleton loader | Pulsing placeholder during loading states |
| Smooth transitions | Page/panel open/close animations |

## Ticket Structure Template

```
## Background

[Explain why this work is needed]

## User Story

**As a** [role]
**I want** [action/feature]
**So that** [benefit/value]

## Acceptance Criteria

**GIVEN** [precondition]
**WHEN** [action]
**THEN** [outcome]

[Additional GIVEN-WHEN-THEN blocks...]

## Notes

- [Optional constraints, follow-ups, or implementation hints]
```

## Story Points

All **Story** and **Task** type tickets **must** have a Story Point estimate before entering Sprint planning.

### Story Point Field

| Field | ID | Description |
|-------|-----|-------------|
| Story point estimate | `customfield_10016` | Numeric story point value (e.g., 1, 2, 3, 5, 8, 13) |

### Story Point Guidelines

| Points | Complexity | Description |
|--------|------------|-------------|
| 1 | Very Low | Trivial change, no research needed |
| 2 | Low | Simple task, well understood |
| 3 | Medium | Standard task, minor complexity |
| 5 | Medium-High | Moderate complexity, some unknowns |
| 8 | High | Complex task, multiple components |
| 13 | Very High | High risk, needs decomposition |

### Rules

- **Every ticket must have SP** before entering Sprint planning
- Use Fibonacci sequence (1, 2, 3, 5, 8, 13)
- If a task exceeds 13 SP, consider splitting it
- Completed tickets should be used to calibrate future estimates

## Checklist

- [ ] Contains Background, User Story, Acceptance Criteria, and Notes
- [ ] User story follows As a / I want / So that format
- [ ] At least 3 acceptance criteria with GIVEN-WHEN-THEN structure
- [ ] Acceptance criteria focus on behavior, not implementation
- [ ] Edge cases and error states are covered
- [ ] OpenAI-style interaction patterns referenced where applicable
- [ ] **Story Point is filled in** (`customfield_10016`)


<!-- source: .cursor/rules/tdd-standards.mdc -->

# TDD Test-Driven Development Standards

## Core Principles

### Red-Green-Refactor Cycle

1. **RED** - Write a failing test, specify expected behavior
2. **GREEN** - Write minimal code to make the test pass
3. **REFACTOR** - Clean up code, keeping tests passing
4. Repeat

### Test Naming Convention

```
should_expected_result_when_trigger_condition
```

### Test Structure (AAA Pattern)

- **Arrange** - Prepare test data and dependencies
- **Act** - Execute the operation under test
- **Assert** - Verify the result

## Quick Checklist

### Test Quality

- [ ] Each test is independent (no shared state)
- [ ] No external dependencies (use Mock/Fake)
- [ ] Follows naming convention
- [ ] Boundary conditions covered (null, empty, max)
- [ ] Deterministic results (no randomness)
- [ ] Fast execution (< 100ms)

### Test Scope

- [ ] Test behavior, not implementation
- [ ] No testing private methods
- [ ] No hardcoded expected values
- [ ] No accessing external resources (DB/network)

### Assertion Quality

- [ ] Precise assertions (avoid `assertTrue(true)`)
- [ ] Meaningful failure messages
- [ ] Cover happy path and edge cases

## Test Doubles

| Type | Use When |
|------|----------|
| Dummy | Fill parameters not used |
| Fake | Simplified implementation (e.g., InMemoryRepository) |
| Stub | Pre-configured responses |
| Mock | Verify interactions |
| Spy | Partial verification with real object |

## References

- [Test Driven Development - Martin Fowler Bliki](https://www.martinfowler.com/bliki/TestDrivenDevelopment.html)
- [Test-Driven Development - Wikipedia](https://en.wikipedia.org/wiki/Test-driven_development)
- [Test-Driven Development: By Example - Kent Beck (Book)](https://www.goodreads.com/book/show/97502.Test_Driven_Development)

<!-- source: .cursor/rules/ux-standards.mdc -->

# UX Design Standards

## Core Philosophy

Design for clarity, elegance, and human-centeredness. Prefer simplicity over complexity.

## Principles

| Principle | Description |
|-----------|-------------|
| **Clarity** | Remove unnecessary elements, emphasize essential content |
| **Consistency** | Uniform patterns across the interface |
| **Feedback** | Immediate response to user actions |
| **Accessibility** | Usable by everyone, regardless of ability |

## Design References

- [Apple HIG](https://developer.apple.com/design/human-interface-guidelines/)
- [shadcn/ui - Apple](https://www.shadcn.io/design/apple)
- [shadcn/ui - OpenAI](https://www.shadcn.io/design/openai)

<!-- Generated at Sun Jul 12 10:16:12 CST 2026 -->
