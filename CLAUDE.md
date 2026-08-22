# AI-Explore Project

> ⚠️ 本文件由 `.claude/generate-rules.sh` 自动生成
> 修改规范请编辑 `~/.cursor/rules/*.mdc`，然后运行此脚本重新生成


<!-- source: ~/.cursor/rules/angular-standards.mdc -->

# Angular 编码规范

## 项目结构

```
src/
├── app/
│   ├── core/                 # 单例服务、全局配置
│   │   ├── services/
│   │   ├── guards/
│   │   └── interceptors/
│   ├── shared/                # 可复用组件、指令、管道
│   │   ├── components/
│   │   ├── directives/
│   │   └── pipes/
│   ├── features/              # 功能模块
│   │   └── {feature}/
│   │       ├── components/
│   │       ├── services/
│   │       └── {feature}.routes.ts
│   ├── app.component.ts
│   ├── app.config.ts
│   └── app.routes.ts
└── styles/
    ├── _variables.scss
    ├── _mixins.scss
    └── _typography.scss
```

## 组件规范

### 命名

| 类型 | 规则 | 示例 |
|------|------|------|
| 组件文件 | kebab-case | `user-card.component.ts` |
| 组件类 | PascalCase + Component | `UserCardComponent` |
| 选择器 | kebab-case + 前缀 | `app-user-card` |
| 模板文件 | 与组件同名 | `user-card.component.html` |
| 样式文件 | 与组件同名 | `user-card.component.scss` |

### 组件模板

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
  // 输入属性使用 input()
  user = input.required<User>();
  editable = input(false);

  // 输出事件使用 output()
  edit = output<User>();
  delete = output<string>();

  // 内部状态使用 signal
  isExpanded = signal(false);

  // 计算属性使用 computed
  fullName = computed(() => `${this.user().firstName} ${this.user().lastName}`);

  // 依赖注入使用 inject()
  private userService = inject(UserService);

  // 方法使用箭头函数保持 this
  handleEdit = () => this.edit.emit(this.user());

  handleDelete = () => this.delete.emit(this.user().id);
}
```

### 模板语法

```html
<!-- 使用 @ 控制流 (Angular 17+) -->
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

<!-- 循环 -->
@for (item of items(); track item.id) {
  <app-list-item [item]="item" />
}

<!-- 安全导航 -->
<p>{{ user()?.email ?? 'No email' }}</p>
```

## 服务规范

```typescript
@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  // 公开只读信号
  users = signal<User[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  // 返回 Observable 供组件订阅
  fetchUsers(): Observable<User[]> {
    return this.http.get<User[]>('/api/users').pipe(
      tap(users => this.users.set(users)),
      catchError(this.handleError)
    );
  }

  private handleError = (error: HttpErrorResponse) => {
    this.error.set(error.message);
    return throwError(() => error);
  };
}
```

## 路由规范

```typescript
// 使用延迟加载
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

## 样式规范

```scss
// 使用 BEM 命名
.user-card {
  padding: var(--spacing-md);
  background: var(--bg-card);

  &__header {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
  }

  &__name {
    font-size: var(--font-size-headline);
    font-weight: 600;
  }

  &--compact {
    padding: var(--spacing-xs);
  }

  &--highlighted {
    border: 2px solid var(--system-blue);
  }
}

// 变量定义
:host {
  --spacing-xs: 8px;
  --spacing-md: 16px;
  --radius-md: 12px;
  --bg-card: rgb(255, 255, 255);
}
```

## 类型规范

```typescript
// 优先使用 interface 定义数据结构
interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: Email;
  role: UserRole;
  createdAt: Date;
}

// 使用 enum 定义枚举
enum UserRole {
  Admin = 'admin',
  User = 'user',
  Guest = 'guest',
}

// API 响应
interface ApiResponse<T> {
  data: T;
  meta: PaginationMeta;
}

// 表单 DTO
interface CreateUserDto {
  firstName: string;
  lastName: string;
  email: Email;
}
```

## 错误处理

```typescript
// 组件内错误处理
@Component({...})
export class UserListComponent {
  private userService = inject(UserService);

  error = this.userService.error;

  @if (error()) {
    <app-error-state [message]="error()" (retry)="loadUsers()" />
  }
}

// 服务内错误处理
private handleError(error: HttpErrorResponse): Observable<never> {
  const message = error.error?.message || 'An error occurred';
  this.notificationService.showError(message);
  return throwError(() => error);
}
```

## 测试规范

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

## Angular 必须避免的错误（踩坑经验）

### 1. 模板中禁止使用 `*ngFor`

Angular 17+ 统一使用 `@for` 控制流语法。`*ngFor` 在 standalone 组件中需要 `NgFor` 指令，且与 `@if` 等新语法混用容易出错。

```typescript
// ❌ 错误：使用 *ngFor
*ngFor="let tab of tabs"

// ✅ 正确：使用 @for 控制流
@for (tab of tabs(); track tab.value) {
  <button (click)="select(tab)">{{ tab.label }}</button>
}
```

### 2. 禁止在模板中使用 Angular 动画触发器语法 `[@name]`

Angular 动画需要注册 `BrowserAnimationsModule` 或 `provideAnimations()`。未注册时直接使用 `[@fadeIn]` 会导致运行时错误。

```typescript
// ❌ 错误：模板中写了动画语法但未注册模块
<div class="tab-content" [@fadeIn]>

// ✅ 正确方式一：用纯 CSS 动画替代（推荐）
.tab-content { animation: fadeIn 0.3s ease; }

// ✅ 正确方式二：若必须使用 Angular 动画，需同时在 app.config.ts 注册
import { provideAnimations } from '@angular/platform-browser/animations';
providers: [provideAnimations()]
```

### 3. 必须为所有 HTTP 请求注册 `provideHttpClient()`

在 Angular 15+ 的 standalone 应用中，使用 `HttpClient` 必须显式注册 provider，否则运行时 `No provider for _HttpClient` 报错。

```typescript
// ❌ app.config.ts 缺少 provider，导致所有 HttpClient 注入失败
providers: [provideRouter(routes)]

// ✅ 正确：注册 HttpClient
providers: [
  provideRouter(routes),
  provideHttpClient()
]
```

### 4. 禁止使用 `[key]` 属性

`[key]` 不是 Angular 已知属性，会产生 `Can't bind to 'key'` 编译错误。如需重新触发组件生命周期（如动画），通过改变组件 `@if` 条件或使用 `*ngIf` 实现。

```html
<!-- ❌ 错误 -->
<div class="tab-section" [key]="activeTab()">

<!-- ✅ 正确：改变条件触发组件重建 -->
@if (activeTab() === 'home') {
  <app-home-tab />
}
```

### 5. 所有 UI 文案必须使用 i18n，不硬编码

组件模板中的所有可见文字（按钮、标签、提示语、空状态）必须从 i18n 服务获取，不允许写死英文。

```typescript
// ❌ 错误：模板中硬编码英文
<h3 class="panel-title">AI Chat</h3>
<p class="empty-title">No image yet</p>
<button>Generate Image</button>

// ✅ 正确：使用 I18nService 获取翻译
protected readonly i18n = inject(I18nService);
// 模板中：
// {{ i18n.t().aiHub.chat.title }}
// {{ i18n.t().aiHub.image.emptyState }}
// {{ i18n.t().aiHub.image.generateButton }}
```

### 6. 添加新的 i18n key 时必须同步所有语言

当在 `Translations` 接口中添加新 key 时（如 `nav.aiHub`），必须同时在 `translations` 对象的每种语言（en/zh/ja/fr/es）中添加对应翻译。

```typescript
// ✅ 正确：接口声明 + 5 种语言全部添加
// interfaces
export interface Translations {
  nav: {
    aiinfra: string;
    aiHub: string;  // ← 新 key
  };
}
// en
translations.en.nav.aiHub = 'AI Hub';
// zh
translations.zh.nav.aiHub = 'AI Hub';
// ja
translations.ja.nav.aiHub = 'AI Hub';
// fr
translations.fr.nav.aiHub = 'AI Hub';
// es
translations.es.nav.aiHub = 'AI Hub';
```

### 7. 路由指向完整功能组件，不指向占位组件

路由 path 应直接指向包含完整 UI 和逻辑的功能组件，避免中间层占位组件。

```typescript
// ❌ 错误：路由指向空占位组件
{ path: 'aihubs', component: PlaceholderComponent }

// ✅ 正确：路由直接指向完整组件
{ path: 'aihubs', loadComponent: () => import('./components/ai/ai-hub/ai-hub.component').then(m => m.AiHubComponent) }
```

<!-- source: ~/.cursor/rules/architecture.mdc -->

# Architecture & Java Core

## Dependency Rule

Per feature module:

```
controller → service → domain ← infra
                ↘     ↗
                 mapper
```

**Domain has NO dependencies on other layers.**  
`infra` implements `domain.repository` and other outbound adapters.  
`mapper` maps DTOs ↔ domain objects (MapStruct or plain mappers); no business rules.

## Layers (per feature)

| Package | Role | Contains |
|---------|------|----------|
| `controller/` | Presentation | Controllers, request/response DTOs |
| `service/` | Application | Orchestration only (`@Service`) |
| `domain/` | DDD core | Entities, VOs, repository interfaces |
| `infra/` | Infrastructure | Repository impls, external clients, persistence config |
| `mapper/` | Mapping | DTO ↔ domain mappers (no business logic) |

## Forbidden in New Code

- `domain/port/` — use `domain/repository/` (or `domain/service/`)
- `*Port` interface suffix — prefer `*Repository`, `*Gateway`, or a domain-specific name
- `adapter/in` / `adapter/out` packages
- Top-level-only `web/` / `application/` / `infrastructure/` for **new** code — use per-feature `controller` / `service` / `domain` / `infra` / `mapper`
- Business rules inside `mapper/`

## Project Structure

```
src/main/java/com.ai/
├── {feature}/
│   ├── controller/
│   ├── service/
│   ├── domain/
│   │   ├── model/
│   │   ├── vo/
│   │   └── repository/
│   ├── infra/
│   └── mapper/
└── common/
    └── exception/
```

## Domain Rules

- Domain uses JPA mapping + Lombok + Bean Validation on kernel and types
- `@Id` / `@Version` only on `@MappedSuperclass` bases (`AbstractImmutable` / `AbstractEntity`); subclasses do not repeat them
- No `@Service` / `@Component` on domain types (those stay in `service/`)
- Aggregates / entities: `@Entity` + `@Getter` + `@NoArgsConstructor(PROTECTED, force = true)`; rich behavior
- Value objects: `@Embeddable` (not record-only)
- Repository: interface in `domain/repository/`, implementation in `infra/`

## Java Naming

| Type | Rule | Example |
|------|------|---------|
| Domain Model | PascalCase | `ChatSession` |
| Value Objects | PascalCase | `SessionId` |
| Repository | PascalCase + Repository | `ChatRepository` |
| Service | PascalCase + Service | `ChatService` |
| Controller | PascalCase + Controller | `ChatController` |
| Mapper | PascalCase + Mapper | `ChatMapper` |
| DTO | PascalCase + Request/Response | `ChatRequest` |
| Methods | camelCase | `findById` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY` |
| Package | lowercase | `com.ai.chat.domain.model` |

Prefer `*Service` in `service/` over `*UseCase`.

## DI / REST / Validation

```java
@Service
@RequiredArgsConstructor
class ChatService {
    private final ChatRepository repository;
}
```

| Operation | Status |
|-----------|--------|
| Create | 201 |
| Success | 200 |
| No Content | 204 |
| Error | 4xx/5xx |

```java
public record ChatRequest(@NotBlank String message, String sessionId) {}
```

## Test Naming

Natural language with spaces (no snake_case underscores):

```
should expected result when condition
```

- Vitest / JUnit `@DisplayName`: `should open popover below chip when space is available`
- Java method names (identifiers cannot contain spaces): `shouldOpenPopoverBelowChipWhenSpaceIsAvailable`

## Checklist

- [ ] Domain has no outward dependencies
- [ ] No circular dependencies
- [ ] Entities encapsulate behavior
- [ ] Repository impls live under `infra/`, not `domain/` or `service/`
- [ ] Mappers live under `mapper/` and contain no business rules

## Hard constraints (delivery)

When **creating a Jira ticket**, **branching**, **committing**, or **opening a PR**: always follow [developer](../skills/scrum-team/developers/developer/SKILL.md) §6 (`<type>/<slug>`, Chain PRs, commit/PR templates, prose ≤72 cols) and [Product Owner](../skills/scrum-team/developers/product-owner/SKILL.md). **References** must prefer official documentation and research links ([dependency-docs](../skills/scrum-team/developers/developer/references/dependency-docs.md), [sources](../skills/scrum-team/developers/market-tech-analysis/references/sources.md), arXiv).

## Skills (on demand)

| Task | Skill |
|------|-------|
| Feature / tests / commit / Apple UX | [developer](../skills/scrum-team/developers/developer/SKILL.md) |
| Business Analysis（领域 / 统一语言 / 搭桥） | [business-analysis](../skills/scrum-team/developers/business-analysis/SKILL.md)（由 `business-analyst` 必读） |
| Market + tech strategy（动向 / 竞品 / GTM） | [market-tech-analysis](../skills/scrum-team/developers/market-tech-analysis/SKILL.md)（由 `market-analyst` / `tech-analyst` 按需读取） |
| Spring AI / RAG / tools | [spring-ai](../skills/scrum-team/developers/spring-ai/SKILL.md) |
| Angular depth | [angular-developer](../skills/scrum-team/developers/angular-developer/SKILL.md) |
| Product Owner / Jira | [Product Owner](../skills/scrum-team/developers/product-owner/SKILL.md) |

<!-- source: ~/.cursor/rules/java-standards.mdc -->

# Java/Spring Boot 编码规范

## 项目结构

```
src/main/java/com/ai/api/
├── api/                      # REST Controllers
│   ├── UserController.java
│   └──advice/
│       └── GlobalExceptionHandler.java
├── service/                  # 业务逻辑
│   ├── UserService.java
│   └── impl/
│       └── UserServiceImpl.java
├── repository/               # 数据访问
│   └── UserRepository.java
├── domain/                   # 领域模型
│   ├── entity/
│   │   └── User.java
│   └── vo/
│       └── Email.java
├── dto/                      # 数据传输对象
│   ├── request/
│   │   └── CreateUserRequest.java
│   └── response/
│       └── UserResponse.java
├── config/                   # 配置类
│   ├── SecurityConfig.java
│   └── OpenApiConfig.java
├── exception/                # 自定义异常
│   └── ResourceNotFoundException.java
└── Application.java
```

## 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 类名 | PascalCase | `UserService`, `UserController` |
| 方法名 | camelCase | `findById`, `createUser` |
| 常量 | SCREAMING_SNAKE | `MAX_RETRY_COUNT` |
| 包名 | lowercase | `com.ai.api.service` |
| 测试类 | ClassName + Test | `UserServiceTest` |
| DTO Record | PascalCase + 后缀 | `UserDTO`, `CreateUserRequest` |

## DTO 设计

### 使用 Record (JDK 16+)

```java
// 查询响应
public record UserDTO(
    UUID id,
    String name,
    String email,
    Instant createdAt
) {
    public static UserDTO from(User user) {
        return new UserDTO(
            user.getId(),
            user.getName(),
            user.getEmail().value(),
            user.getCreatedAt()
        );
    }
}

// 创建请求
public record CreateUserRequest(
    @NotBlank(message = "Name is required")
    String name,

    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {}

// 分页响应
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
```

## Controller 规范

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users", description = "Get paginated list of users")
    public ResponseEntity<PageResponse<UserDTO>> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(userService.findAll(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findById(@PathVariable UUID id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserDTO> create(@Valid @RequestBody CreateUserRequest request) {
        UserDTO created = userService.create(request);
        return ResponseEntity
                .created(URI.create("/api/users/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.update(id, request)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userService.delete(id);
    }
}
```

## Service 规范

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    public PageResponse<UserDTO> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);

        return toPageResponse(users);
    }

    public Optional<UserDTO> findById(UUID id) {
        return userRepository.findById(id).map(UserDTO::from);
    }

    @Transactional
    public UserDTO create(CreateUserRequest request) {
        // 业务校验
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // 构建实体
        User user = new User(
                request.name(),
                new Email(request.email()),
                passwordEncoder.encode(request.password())
        );

        // 保存
        User saved = userRepository.save(user);

        // 发布事件
        eventPublisher.publishEvent(new UserCreatedEvent(saved));

        return UserDTO.from(saved);
    }

    @Transactional
    public Optional<UserDTO> update(UUID id, UpdateUserRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.updateName(request.name());
                    return UserDTO.from(userRepository.save(user));
                });
    }

    private PageResponse<UserDTO> toPageResponse(Page<User> page) {
        return new PageResponse<>(
                page.getContent().stream().map(UserDTO::from).toList(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
```

## Repository 规范

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // 方法名查询
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // 自定义查询
    @Query("SELECT u FROM User u WHERE u.active = true AND u.role = :role")
    List<User> findActiveUsersByRole(@Param("role") UserRole role);

    // 分页查询
    Page<User> findByActiveTrue(Pageable pageable);

    // 原生查询
    @Query(value = "SELECT * FROM users WHERE name ILIKE %:name%", nativeQuery = true)
    List<User> searchByName(@Param("name") String name);
}
```

## 异常处理

```java
// 自定义异常
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("User not found with id: " + id);
    }
}

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email);
    }
}

// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), 404));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(message, 400));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred", 500));
    }
}

public record ErrorResponse(String message, int status, Instant timestamp) {
    public ErrorResponse(String message, int status) {
        this(message, status, Instant.now());
    }
}
```

## 安全配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

## 测试规范

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest("John", "john@example.com", "password123");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new User(u.getId(), u.getName(), u.getEmail(), u.getPassword(), Instant.now());
        });

        // When
        UserDTO result = userService.create(request);

        // Then
        assertThat(result.name()).isEqualTo("John");
        assertThat(result.email()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenEmailExists() {
        // Given
        CreateUserRequest request = new CreateUserRequest("John", "john@example.com", "password");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }
}

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should create user and return 201")
    void shouldCreateUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest("John", "john@example.com", "password123");

        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }
}
```

<!-- Generated at Sat Aug 22 16:21:15 CST 2026 -->
