# AI-Explore Project

> ⚠️ 本文件由 `.claude/generate-rules.sh` 自动生成
> 修改规范请编辑 `.cursor/rules/*.mdc`，然后运行此脚本重新生成


<!-- source: .cursor/rules/angular-standards.mdc -->

# Angular Coding Standards

## Project Structure

```
src/
├── app/
│   ├── {feature}/
│   │     ├── components/
│   │     ├── services/
│   │     └── {feature}.routes.ts
│   ├── app.component.ts
│   ├── app.config.ts
│   └── app.routes.ts
└── styles/
    ├── _variables.scss
    ├── _mixins.scss
    └── _typography.scss
```

## Component Standards

### Naming

| Type            | Rule                   | Example                    |
| --------------- | ---------------------- | -------------------------- |
| Component file  | kebab-case             | `user-card.component.ts`   |
| Component class | PascalCase + Component | `UserCardComponent`        |
| Selector        | kebab-case + prefix    | `app-user-card`            |
| Template file   | Same as component      | `user-card.component.html` |
| Style file      | Same as component      | `user-card.component.scss` |

### Component Template

```typescript
@Component({
  selector: 'app-user-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './user-card.component.html',
  styleUrl: './user-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserCardComponent {
  // Input properties use input()
  user = input.required<User>();
  editable = input(false);

  // Output events use output()
  edit = output<User>();
  delete = output<string>();

  // Internal state uses signal
  isExpanded = signal(false);

  // Computed properties use computed
  fullName = computed(() => `${this.user().firstName} ${this.user().lastName}`);

  // Dependency injection uses inject()
  private userService = inject(UserService);

  // Methods use arrow functions to preserve this
  handleEdit = () => this.edit.emit(this.user());

  handleDelete = () => this.delete.emit(this.user().id);
}
```

### Template Syntax

```html
<!-- Use @ control flow (Angular 17+) -->
@if (user(); as user) {
<div class="user-card">
  <h3>{{ fullName() }}</h3>
  @if (editable()) {
  <button (click)="handleEdit()">Edit</button>
  }
</div>
} @else {
<app-skeleton />
}

<!-- Loop -->
@for (item of items(); track item.id) {
<app-list-item [item]="item" />
}

<!-- Safe navigation -->
<p>{{ user()?.email ?? 'No email' }}</p>
```

## Architecture Rules

### 1. Top-Level Structure

```
src/app/
├── app/         Application entry and composition (bootstrap + layout)
├── core/        Global singleton capabilities (API / interceptors / auth / config)
├── shared/      Reusable non-business capabilities (UI / utils / models)
├── features/    Business modules (domain-scoped)
├── layout/      Page layout components
└── environments/ Environment config
```

### 2. Feature Module Rules (Critical)

Each feature must be an independent, self-contained module:

```
features/<feature-name>/
├── components/   UI components
├── services/     Business state and logic
├── models/       Data structures (DTOs / VOs)
└── pages/        Page-level components (optional)
```

**Forbidden:**
- Feature-to-feature direct imports
- Feature accessing core internals
- Feature depending on shared business models

### 3. Core Rules

Core contains only:
- API Client
- HTTP interceptors
- Auth / session
- i18n / config
- Global singleton services

**Forbidden:** Business logic, feature-related code

### 4. Shared Rules

Shared contains only:
- UI components (button, card, modal)
- Pipes / directives
- Utils
- Generic models (no business semantics)

**Forbidden:** API calls, feature logic, domain-specific models

### 5. DTO / Model Organization

DTOs must be co-located with their feature:

```
features/ai/models/
features/rag/models/
```

**Forbidden:** Separate dto/ or web/dto directories

### 6. Dependency Direction (Strict)

**Allowed:**
```
features → shared
features → core
layout → shared
layout → core
app → all
```

**Forbidden:**
```
core → features
shared → features
features → features (cross-module)
```

### 7. Layout Rules

Layout components handle only:
- Page structure (header/sidebar/main)
- Router-outlet organization

**Forbidden:** API calls, state management, feature logic

### 8. Refactoring Principles

Every structural improvement must satisfy:
- Directory depth ≤ 4
- Feature internal depth ≤ 3
- DTOs not standalone
- Features fully autonomous
- Shared without business logic

### 9. Ultimate Principle

> Keep it simple. Keep features isolated. Avoid over-layering.

## Service Standards

```typescript
@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  // Public readonly signals
  users = signal<User[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  // Return Observable for components to subscribe
  fetchUsers(): Observable<User[]> {
    return this.http.get<User[]>('/api/users').pipe(
      tap((users) => this.users.set(users)),
      catchError(this.handleError)
    );
  }

  private handleError = (error: HttpErrorResponse) => {
    this.error.set(error.message);
    return throwError(() => error);
  };
}
```

## Routing Standards

```typescript
// Use lazy loading
export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'users',
    loadChildren: () => import('./features/users/routes'),
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/routes'),
    canMatch: [authGuard, adminGuard],
  },
];
```

## Style Standards

### Tailwind CSS v4 (Primary)

Use Tailwind CSS v4 utility classes with CSS variable references for theme colors.

**CSS Variable Reference Syntax:**
```html
<!-- Reference theme colors -->
<button class="bg-primary text-white hover:bg-primary-hover">Primary</button>
<button class="bg-[--color-primary] text-[--color-text]">Custom</button>

<!-- Spacing -->
<div class="p-4 gap-2">Content</div>

<!-- Border radius -->
<div class="rounded-lg">Rounded</div>

<!-- Shadows -->
<div class="shadow-card">Card</div>
```

**Class Binding for Dynamic Variants:**
```typescript
getClasses(): string {
  const classes: string[] = [];
  if (this.variant() === 'primary') {
    classes.push('bg-primary text-white');
  }
  return classes.join(' ');
}
```

**Key Patterns:**
- Use `bg-primary` for theme colors mapped in `@theme`
- Use `bg-[--color-*]` for direct CSS variable references
- Use `shadow-[--shadow-*]` for theme shadows
- Use `rounded-*` for border radius

### BEM Naming (Legacy - Phase Out)

Migrate existing BEM-style SCSS to Tailwind classes:

```scss
// Before (legacy)
.card {
  &__header { }
  &--elevated { }
}

// After (Tailwind)
class="bg-surface shadow-elevated rounded-xl"
```

### Inline Animations

For complex animations, use inline `@keyframes`:

```typescript
styles: [`
  @keyframes slideIn {
    from { opacity: 0; transform: translateY(-8px); }
    to { opacity: 1; transform: translateY(0); }
  }
  .animate-slide-in {
    animation: slideIn 0.2s ease forwards;
  }
`]
```

## Type Standards

```typescript
// Prefer interface for data structures
interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: Email;
  role: UserRole;
  createdAt: Date;
}

// Use enum for enumerations
enum UserRole {
  Admin = 'admin',
  User = 'user',
  Guest = 'guest',
}

// API response
interface ApiResponse<T> {
  data: T;
  meta: PaginationMeta;
}

// Form DTO
interface CreateUserDto {
  firstName: string;
  lastName: string;
  email: Email;
}
```

## Error Handling

```typescript
// Component error handling
@Component({...})
export class UserListComponent {
  private userService = inject(UserService);

  error = this.userService.error;

  @if (error()) {
    <app-error-state [message]="error()" (retry)="loadUsers()" />
  }
}

// Service error handling
private handleError(error: HttpErrorResponse): Observable<never> {
  const message = error.error?.message || 'An error occurred';
  this.notificationService.showError(message);
  return throwError(() => error);
}
```

## Testing Standards

```typescript
describe('UserCardComponent', () => {
  let component: UserCardComponent;
  let fixture: ComponentFixture<UserCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UserCardComponent);
    component = fixture.componentInstance;
    component.user = TestFixtures.user;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display user name', () => {
    const name = fixture.nativeElement.querySelector('.user-card__name');
    expect(name.textContent).toContain('John Doe');
  });

  it('should emit edit event', () => {
    const editSpy = spyOn(component.edit, 'emit');
    component.handleEdit();
    expect(editSpy).toHaveBeenCalledWith(TestFixtures.user);
  });
});
```

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

<!-- source: .cursor/rules/clean-architecture.mdc -->

# Clean Architecture Standards

> Based on Robert C. Martin's "Clean Architecture"

## Core Principle: The Dependency Rule

> Source code dependencies must point only inward. Outer circles can depend on inner circles. Inner circles know nothing about outer circles.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Frameworks & Drivers                         │
│                  (Database, Web Framework)                      │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────────────────────────────────────────┐
│                      Interface Adapters                        │
│              (Controllers, Gateways, Presenters)                 │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────────────────────────────────────────┐
│                         Use Cases                               │
│                   (Application Business Rules)                  │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────────────────────────────────────────┐
│                          Entities                              │
│                   (Enterprise Business Rules)                   │
└─────────────────────────────────────────────────────────────────┘
```


## Project Structure

### Java

```
src/main/java/com/example/
├── entity/                    # Innermost circle
│ ├── model/                   # Aggregates, Entities
│ ├── vo/                      # Value Objects
│ └── service/                 # Domain Services
│
├── usecase/                   # Application layer
│ ├── {feature}/
│ │   ├── {FeatureName}Input.java    # Input data structure
│ │   ├── {FeatureName}Output.java   # Output data structure
│ │   └── {FeatureName}UseCase.java # Interface + Implementation
│ └── port/                          # External interfaces
│
├── interfaceadapter/          # Interface Adapters circle
│ ├── controller/             # REST controllers
│ ├── presenter/              # Output presenters
│ └── gateway/               # Database/external gateways
│
└── framework/                # Outermost circle
    ├── persistence/         # Database implementation
    └── external/           # External services
```

### TypeScript

```
src/
├── entity/                    # Innermost circle
│ ├── models/
│ ├── value-objects/
│ └── services/
│
├── usecase/                   # Application layer
│ ├── {feature}/
│ │   ├── create-order.input.ts
│ │   ├── create-order.output.ts
│ │   └── create-order.use-case.ts
│ └── port/                          # External interfaces
│
├── interfaceadapter/          # Interface Adapters
│ ├── controller/
│ ├── presenter/
│ └── gateway/
│
└── framework/                # Outermost circle
    ├── database/
    └── external/
```


## Use Cases (Application Circle)

### Input & Output Data Structures

```java
// Input - what the use case needs
public record PlaceOrderInput(
  List<OrderLineInput> lines,
  ShippingAddressInput shipping
) {
  public record OrderLineInput(UUID productId, int quantity) {}
  public record ShippingAddressInput(String street, String city, String zip) {}
}

// Output - what the use case returns
public record PlaceOrderOutput(
  UUID orderId,
  OrderStatus status,
  Money total
) {}
```

```typescript
// Input
export interface PlaceOrderInput {
  lines: ReadonlyArray<{ productId: ProductId; quantity: number }>;
  shipping: { street: string; city: string; zip: string };
}

// Output
export interface PlaceOrderOutput {
  orderId: OrderId;
  status: OrderStatus;
  total: Money;
}
```

### Use Case Implementation

```java
// Interface (in usecase/{feature}/)
public interface PlaceOrderUseCase {
  PlaceOrderOutput execute(PlaceOrderInput input);
}

// Implementation
public class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {
  private final OrderGateway orderGateway;
  private final ProductGateway productGateway;
  
  public PlaceOrderUseCaseImpl(OrderGateway orderGateway, ProductGateway productGateway) {
    this.orderGateway = orderGateway;
    this.productGateway = productGateway;
  }
  
  @Override
  public PlaceOrderOutput execute(PlaceOrderInput input) {
    Order order = Order.create();
    
    for (var line : input.lines()) {
      Product product = productGateway.findById(line.productId())
        .orElseThrow(() -> ProductNotFoundException.forId(line.productId()));
      order.addLine(product, line.quantity());
    }
    
    order.place();
    orderGateway.save(order);
    
    return new PlaceOrderOutput(order.getId(), order.getStatus(), order.getTotal());
  }
}
```


## Frameworks (Outermost Circle)

Frameworks contain database implementations, web servers, external services.

```java
// This circle knows about database, ORM, etc.
// All SQL stays here

@Repository
public class JpaOrderRepository implements OrderGateway {
  // JPA/Hibernate code lives here
  // SQL queries stay in this circle
}
```


## Architecture Checklist

- [ ] Entities circle has no framework annotations
- [ ] Use case interfaces define application behavior
- [ ] Source code dependencies point inward only
- [ ] Entities use factory methods (private constructors)
- [ ] Value Objects are immutable
- [ ] Business logic in entities/use cases
- [ ] Gateways implement interfaces defined in use case circle
- [ ] Simple data structures cross boundaries
- [ ] All database/SQL code in outermost circle
- [ ] Composition root wires all dependencies

## References

- [The Clean Architecture - Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [The Dependency Rule - InformIT](https://www.informit.com/articles/article.aspx?p=2832399)

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

## 架构合规

- [ ] 遵循整洁架构 (entity → usecase → interfaceadapter → framework)
- [ ] Entity/USecase 层无框架依赖 (@Entity, @Service, @Component 不在 entity/usecase)
- [ ] Repository 接口定义在 use case 层，Gateway 实现在 interfaceadapter 层
- [ ] 无循环依赖，依赖方向指向内层
- [ ] 使用 Input/Output 数据结构跨边界

## 代码质量

- [ ] 无 God Class (> 300 行需拆分)
- [ ] 遵循单一职责原则 (SRP)
- [ ] 方法长度不超过 20 行
- [ ] 无注释掉的代码
- [ ] 错误处理完善 (异常转换)

## 命名规范

| 类型 | Java | TypeScript |
|------|------|------------|
| 类名 | PascalCase | PascalCase |
| 方法名 | camelCase | camelCase |
| 常量 | UPPER_SNAKE_CASE | UPPER_SNAKE_CASE |
| 包名 | lowercase | - |
| 文件名 | PascalCase.java | PascalCase.ts |

## 测试覆盖

- [ ] 领域逻辑有单元测试
- [ ] 测试遵循命名约定 (`should_expected_when_condition`)
- [ ] 无硬编码测试数据 (使用 Factory/Fixture)
- [ ] 边界条件已覆盖 (null, empty, max)

## 安全

- [ ] 代码中无敏感信息 (密码、Token 硬编码)
- [ ] 输入验证存在 (@Valid, Zod schema)
- [ ] SQL 注入防护 (参数化查询)
- [ ] XSS 防护 (转义输出)

## 性能

- [ ] 无 N+1 查询问题
- [ ] 集合操作使用 Stream API (Java) / 数组方法 (TS)
- [ ] 大数据量场景考虑分页/流式处理

## 文档

- [ ] Public API 有 JSDoc / Javadoc
- [ ] 复杂业务逻辑有注释说明意图
- [ ] 无 TODO/FIXME 遗留

<!-- source: .cursor/rules/commit-pr-standards.mdc -->

# Commit & Pull Request Standards

## Core Principles

1. **One commit = One complete change**: Each commit delivers one finished piece of work
2. **Chain PRs**: Each PR is based on the previous branch (not main)
3. **References**: Always search the web in real-time for authoritative sources


## Pull Request Format

PR body mirrors the commit message. References must be identical.

```
References
- [Title](URL)
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

<!-- source: .cursor/rules/ddd-standards.mdc -->

# Domain-Driven Design Standards

> Based on Vaughn Vernon's "Implementing Domain-Driven Design" and Eric Evans' "Domain-Driven Design"

## Rich Domain Model (充血模型) vs Anemic Domain Model (贫血模型)

| Aspect | Anemic (贫血) | Rich (充血) |
|--------|---------------|-------------|
| Behavior | In service classes | In entities |
| State | Exposed via getters/setters | Private, controlled via methods |
| Logic | Procedural | Encapsulated |
| Coupling | High (services know entity internals) | Low (entities are self-contained) |

> **Rule: Use Rich Domain Model. Entities encapsulate behavior and invariants.**


## Building Blocks

### Aggregates

Aggregate = Cluster of associated objects treated as one unit for data changes.

```
Aggregate Root (聚合根)
├── Entity 1
├── Entity 2
└── Value Objects
```

**Rules:**
1. Each aggregate has one root (Aggregate Root)
2. External objects reference only the root
3. Transaction boundary = aggregate boundary

```java
// ✅ GOOD - Rich Aggregate Root
public class Order extends AggregateRoot {
    private final OrderId id;
    private OrderStatus status;
    private final List<OrderLine> lines;  // Internal entities
    
    // Factory method
    private Order(OrderId id) {
        this.id = Objects.requireNonNull(id);
        this.status = OrderStatus.DRAFT;
        this.lines = new ArrayList<>();
    }
    
    public static Order create() {
        return new Order(OrderId.generate());
    }
    
    // Reconstitution
    public static Order reconstitute(OrderId id, OrderStatus status, List<OrderLine> lines) {
        Order order = new Order(id);
        order.status = status;
        order.lines.addAll(lines);
        return order;
    }
    
    // Business logic - behavior in domain
    public void addLine(Product product, int quantity) {
        validateNotPlaced();
        if (quantity <= 0) throw new OrderException.InvalidQuantity(quantity);
        lines.add(OrderLine.create(product, quantity));
    }
    
    public void place() {
        validateCanPlace();
        this.status = OrderStatus.PLACED;
        registerEvent(new OrderPlacedEvent(this.id));
    }
    
    private void validateCanPlace() {
        if (lines.isEmpty()) throw new OrderException.EmptyOrder();
    }
    
    private void validateNotPlaced() {
        if (status == OrderStatus.PLACED) throw new OrderException.AlreadyPlaced();
    }
    
    // Getters only - no setters
    public OrderId getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getLines() { return List.copyOf(lines); }
}

// ❌ BAD - Anemic Entity
public class Order {
    private UUID id;
    private OrderStatus status;
    private List<OrderLine> lines;
    
    // Getters and setters only - no behavior
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    // ... all getters and setters
}
```

### Entities

Entity has identity that persists beyond its attributes.

```java
public class OrderLine extends Entity<OrderLineId> {
    private final ProductId productId;
    private final Money unitPrice;
    private final int quantity;
    
    private OrderLine(OrderLineId id, ProductId productId, Money unitPrice, int quantity) {
        super(id);
        this.productId = Objects.requireNonNull(productId);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        this.quantity = quantity;
    }
    
    public static OrderLine create(Product product, int quantity) {
        return new OrderLine(OrderLineId.generate(), product.getId(), product.getPrice(), quantity);
    }
    
    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
    
    // Identity equality
    @Override
    public boolean sameAs(OrderLine other) {
        return this.id.equals(other.id);
    }
}
```

### Value Objects

Immutable, no identity, compared by value.

```java
// ✅ Record-based Value Object (Java 16+)
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount cannot be negative");
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency))
            throw new CurrencyMismatchException();
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }
    
    public static Money ZERO(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
}

// Value object for identity
public record OrderId(UUID value) {
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
}
```

### Domain Services

Operations that don't belong to any entity — stateless services.

```java
// When an operation conceptually belongs to a service
// (doesn't fit in any single entity)
public class PricingService {
    
    public Money calculateDiscount(Order order, Coupon coupon) {
        if (coupon.isExpired()) return Money.ZERO(order.getCurrency());
        if (!coupon.appliesTo(order)) return Money.ZERO(order.getCurrency());
        
        return coupon.calculateDiscount(order.getTotal());
    }
}
```

### Domain Events

Capture significant occurrences in the domain.

```java
// Event class
public record OrderPlacedEvent(
    OrderId orderId,
    Instant occurredAt
) implements DomainEvent {
    public OrderPlacedEvent(OrderId orderId) {
        this(orderId, Instant.now());
    }
}

// Publishing from aggregate root
public class Order extends AggregateRoot {
    public void place() {
        // ...
        registerEvent(new OrderPlacedEvent(this.id));
    }
}

// Event subscriber
@EventListener
public class OrderEventHandler {
    public void on(OrderPlacedEvent event) {
        // React to event
    }
}
```

### Repositories

Abstract persistence for aggregates.

```java
// Interface in domain layer
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}

// Implementation in infrastructure layer
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final SpringDataOrderRepository jpaRepository;
    
    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }
    
    @Override
    public void save(Order order) {
        jpaRepository.save(toEntity(order));
    }
}
```

### Factories

Encapsulate complex object creation.

```java
// Factory method on aggregate
public class OrderFactory {
    
    public Order createOrder(Customer customer, List<CartItem> items) {
        Order order = Order.create();
        
        for (CartItem item : items) {
            order.addLine(item.getProduct(), item.getQuantity());
        }
        
        order.setCustomer(customer);
        order.setShippingAddress(customer.getDefaultAddress());
        
        return order;
    }
}
```


## Key Principles

### Encapsulation

**Rule: Hide internals. Expose behavior. Protect invariants.**

```java
// ❌ BAD - Exposes internals
public class Order {
    public List<OrderLine> lines;  // Public field!
}

// ❌ BAD - Exposes internals for modification
public List<OrderLine> getLines() {
    return lines;  // Returns mutable list
}

// ✅ GOOD - Controlled access
public List<OrderLine> getLines() {
    return List.copyOf(lines);  // Immutable copy
}
```

### Invariants

**Rule: Never allow invalid state. Enforce invariants in constructors and methods.**

```java
public class Account {
    private Money balance;
    private boolean closed;
    
    // Invariant enforced: balance >= 0 always
    public void withdraw(Money amount) {
        if (closed) throw new AccountException.AccountClosed();
        if (balance.compareTo(amount) < 0) 
            throw new AccountException.InsufficientFunds();
        balance = balance.subtract(amount);
    }
}
```

### Ubiquitous Language

**Rule: Use domain terms everywhere. No technical jargon in domain.**

```java
// ❌ BAD - Technical terms
public void updateOrderStatus(Long orderId, String newStatus);

// ✅ GOOD - Domain language
public void place(OrderId orderId);
public void cancel(OrderId orderId);
public void ship(OrderId orderId);
```

### One Object Per Transaction

**Rule: One aggregate per transaction. Don't modify multiple aggregates in one transaction.**

```java
// ❌ BAD - Modifying multiple aggregates
public void transfer(Order order, Customer customer) {
    order.cancel();      // Aggregate 1
    customer.credit(order.getAmount());  // Aggregate 2
    // What if second operation fails?
}

// ✅ GOOD - One aggregate per transaction
// Use Domain Events for cross-aggregate communication
public void cancel(Order order) {
    order.cancel();
    registerEvent(new OrderCancelledEvent(order.getId(), order.getAmount()));
}

@EventListener
public void OnOrderCancelled(OrderCancelledEvent event) {
    customer.credit(event.getAmount());  // Separate transaction
}
```


## Architecture Checklist

### Domain Layer

- [ ] Entities have private state, no public setters
- [ ] Aggregates enforce invariants
- [ ] Value Objects are immutable
- [ ] Factory methods for complex construction
- [ ] Domain Events for state changes
- [ ] No framework annotations in domain

### Use Cases

- [ ] Single responsibility (one operation)
- [ ] No business logic (delegates to entities)
- [ ] Input/Output as data structures
- [ ] Transactions scoped to one aggregate

### Integration

- [ ] Repository interfaces in domain
- [ ] Repository implementations in infrastructure
- [ ] Domain Events for cross-aggregate communication
- [ ] Aggregates are transaction boundaries


<!-- source: .cursor/rules/java-standards.mdc -->

# Java/Spring Boot Hexagonal Architecture Standards

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│ adapter/in (Driving)                                            │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ REST Controllers / DTOs (Request/Response)                   │ │
│ └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│ domain (Core - No External Dependencies)                        │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Entities / Value Objects / Aggregates / Ports / Domain Svcs │ │
│ └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│ adapter/out (Driven)                                            │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Persistence / External Services / Third-party Integrations   │ │
│ └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│ config                                                          │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Spring Configuration / Infrastructure Wiring                 │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Dependency Rule

```
adapter/in → domain ← adapter/out
config → domain, adapter/in, adapter/out
```

**Domain NEVER depends on any external framework or infrastructure.**

## Project Structure (Hexagonal)

```
src/main/java/com/example/
├── domain/                                    # Core (Framework-free)
│   ├── model/                                 # Aggregates, Entities
│   │   ├── Order.java                        # Aggregate Root
│   │   ├── OrderId.java                      # Identity Value Object
│   │   └── OrderLine.java                    # Entity
│   ├── vo/                                    # Value Objects
│   │   ├── Money.java
│   │   ├── Email.java
│   │   └── Address.java
│   ├── service/                               # Domain Services
│   │   └── PricingService.java
│   ├── repository/                            # Port Interfaces
│   │   └── OrderRepository.java
│   ├── port/                                  # Input/Output Ports
│   │   ├── in/
│   │   │   └── PlaceOrderUseCase.java
│   │   └── out/
│   │       └── PaymentGateway.java
│   └── exception/                             # Domain Exceptions
│       └── OrderException.java
│
├── adapter/
│   ├── in/
│   │   └── web/                              # Driving Adapter
│   │       ├── controller/
│   │       │   └── OrderController.java
│   │       └── dto/
│   │           ├── request/
│   │           │   └── PlaceOrderRequest.java
│   │           └── response/
│   │               └── OrderResponse.java
│   │
│   └── out/
│       ├── persistence/                       # Driven Adapter
│       │   ├── jpa/
│       │   │   └── OrderJpaRepository.java
│       │   └── repository/
│       │       └── JpaOrderRepository.java
│       └── payment/                           # External Service Adapter
│           └── StripePaymentAdapter.java
│
└── config/                                   # Infrastructure
    ├── OrderConfig.java
    └── SecurityConfig.java
```

## Domain Layer Examples

### Entity (Rich Model)

```java
package com.example.domain.model;

import com.example.domain.exception.OrderException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Order extends AggregateRoot {

    private final UUID id;
    private OrderStatus status;
    private final List<OrderLine> lines;
    private final Instant createdAt;

    // Factory method - ensures valid construction
    private Order(UUID id, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = OrderStatus.DRAFT;
        this.lines = new ArrayList<>();
    }

    public static Order create() {
        return new Order(UUID.randomUUID(), Instant.now());
    }

    // Business logic encapsulated in domain
    public void place() {
        validateCanPlace();
        this.status = OrderStatus.PLACED;
        registerEvent(new OrderPlacedEvent(this.id));
    }

    public void addLine(Product product, int quantity) {
        validateCanModify();
        if (quantity <= 0) {
            throw OrderException.invalidQuantity(quantity);
        }
        lines.add(new OrderLine(product.getId(), product.getPrice(), quantity));
    }

    public Money calculateTotal() {
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }

    private void validateCanPlace() {
        if (lines.isEmpty()) {
            throw OrderException.emptyOrder();
        }
    }

    private void validateCanModify() {
        if (status != OrderStatus.DRAFT) {
            throw OrderException.cannotModifyAfterPlacement();
        }
    }

    // Getters only - no setters
    public UUID getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getLines() { return List.copyOf(lines); }
}
```

### Value Object (Immutable)

```java
package com.example.domain.vo;

public record Money(BigDecimal amount, Currency currency) {

    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.USD);

    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }
}
```

### Port Interface (Domain defines contract)

```java
package com.example.domain.repository;

public interface OrderRepository {

    Optional<Order> findById(OrderId id);

    void save(Order order);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
```

## Adapter Layer Examples

### Input Port Implementation (Use Case)

```java
package com.example.adapter.in.web.controller;

@Component
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody PlaceOrderRequest request) {
        var command = PlaceOrderCommand.from(request);
        var result = placeOrderUseCase.execute(command);
        return OrderResponse.from(result);
    }
}

@ApplicationService
@RequiredArgsConstructor
class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    @Override
    @Transactional
    public OrderResult execute(PlaceOrderCommand command) {
        // Transform input to domain objects
        Order order = Order.create();

        for (var line : command.lines()) {
            Product product = productService.findById(line.productId())
                .orElseThrow(() -> ProductNotFoundException.of(line.productId()));
            order.addLine(product, line.quantity());
        }

        // Execute business logic
        order.place();

        // Persist
        orderRepository.save(order);

        // Return result
        return OrderResult.from(order);
    }
}
```

### Output Port Implementation (Driven Adapter)

```java
package com.example.adapter.out.persistence.repository;

@Repository
@RequiredArgsConstructor
class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository springRepository;

    @Override
    public Optional<Order> findById(OrderId id) {
        return springRepository.findById(id.value())
            .map(this::toDomain);
    }

    @Override
    public void save(Order order) {
        springRepository.save(toEntity(order));
    }

    @Override
    public Page<Order> findByStatus(OrderStatus status, Pageable pageable) {
        return springRepository.findByStatus(status, pageable)
            .map(this::toDomain);
    }

    private Order toDomain(OrderEntity entity) {
        // Map entity to domain
        return Order.reconstitute(
            OrderId.of(entity.getId()),
            entity.getStatus(),
            entity.getLines().stream()
                .map(this::toDomainLine)
                .toList()
        );
    }

    private OrderEntity toEntity(Order order) {
        return OrderEntity.builder()
            .id(order.getId().value())
            .status(order.getStatus())
            .lines(order.getLines().stream()
                .map(this::toEntityLine)
                .toList())
            .build();
    }
}
```

## Naming Conventions

| Type | Rule | Example |
|------|------|---------|
| Domain Model | PascalCase | `Order`, `Money`, `Email` |
| Value Objects | PascalCase | `OrderId`, `UserId` |
| Port Interfaces | PascalCase + UseCase/Repository | `PlaceOrderUseCase`, `OrderRepository` |
| Adapter Implementation | PascalCase + Suffix | `PlaceOrderUseCaseImpl`, `JpaOrderRepository` |
| Methods | camelCase | `findById`, `calculateTotal` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Package names | lowercase | `com.example.domain.model` |

## DTO Design (Record)

```java
// Command (Input)
public record PlaceOrderCommand(
    @NotEmpty List<OrderLineDto> lines,
    @NotNull ShippingAddressDto shipping
) {
    public record OrderLineDto(UUID productId, int quantity) {}
    public record ShippingAddressDto(String street, String city, String zip) {}
}

// Result (Output)
public record OrderResult(
    UUID orderId,
    String status,
    Money total,
    Instant createdAt
) {
    public static OrderResult from(Order order) {
        return new OrderResult(
            order.getId().value(),
            order.getStatus().name(),
            order.calculateTotal(),
            order.getCreatedAt()
        );
    }
}
```

## Checklist

### Architecture Compliance

- [ ] Domain layer has no framework annotations (@Entity, @Service, @Repository)
- [ ] Repository interfaces defined in domain, implementations in adapter/out
- [ ] No circular dependencies
- [ ] Dependency arrows point inward toward domain

### Code Quality

- [ ] Entities use factory methods (private constructor)
- [ ] Value Objects are immutable (record)
- [ ] Business logic in domain, not in adapters
- [ ] Use cases orchestrate, don't contain business logic

### Testing

- [ ] Domain logic has unit tests (no mocks needed)
- [ ] Adapter tests use integration tests or mocks
- [ ] Test naming follows convention

## References

- [Ports and Adapters Pattern - Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Getting Started with Hexagonal Architecture](https://reflectoring.io/spring-boot-hexagonal/)

<!-- source: .cursor/rules/jira-ticket-creation.mdc -->

# Jira Ticket Creation Standards

## User Story Format

Every ticket must include a user story in the following format:

```
**As a** [role]
**I want** [action/feature]
**So that** [benefit/value]
```

## Acceptance Criteria Format

Use **GIVEN-WHEN-THEN** (BDD) format for all acceptance criteria:

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
## User Story

**As a** [role]
**I want** [action/feature]
**So that** [benefit/value]

## Acceptance Criteria

**GIVEN** [precondition]
**WHEN** [action]
**THEN** [outcome]

[Additional GIVEN-WHEN-THEN blocks...]
```

## Story Points

All **Story** and **Task** type tickets **must** have a Story Point estimate filled in before being added to a Sprint.

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

### Quick Checklist

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

<!-- Generated at Thu Jul  2 07:30:57 CST 2026 -->
