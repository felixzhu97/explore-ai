# Testing Core

Minimal testing standards for this repo. Applied via the [developer skill](../SKILL.md).

## Pyramid

| Layer | Share | Focus |
|-------|-------|-------|
| Unit | ~70% | Domain / pure logic; &lt; 1ms; no I/O |
| Integration | ~20% | Collaborators (DB, Spring, queues); Testcontainers when needed |
| E2E | ~10% | Critical user journeys only (Playwright); keep few |

Prefer many fast unit tests over a wide E2E suite.

## BDD

- Business language; one scenario = one behavior
- Structure: **Given** precondition → **When** action → **Then** outcome
- Outcomes, not “system calls X”
- Same vocabulary as [Glossary](../../../../docs/Glossary.md)

## TDD

```
Red (failing test) → Green (minimal code) → Refactor (keep green)
```

- Write the test first when implementing domain/application behavior
- AAA: Arrange / Act / Assert
- Do not test private methods; do not hit network/DB in unit tests

## Naming

Natural language with spaces — do **not** use `should_result_when_condition` snake_case:

```
should expected result when condition
```

| Surface | Example |
|---------|---------|
| Vitest `it(...)` / JUnit `@DisplayName` | `should open popover below chip when space is available` |
| Java method identifier | `shouldOpenPopoverBelowChipWhenSpaceIsAvailable` |

## Test doubles

| Type | Use |
|------|-----|
| Dummy | Unused parameter filler |
| Fake | In-memory repo / simplified collaborator |
| Stub | Fixed return values |
| Mock | Verify interactions only when the interaction is the contract |
| Spy | Partial real object + call recording |

Prefer **Fake** for repositories over heavy mocking.

## Anti-patterns (avoid)

| Smell | Fix |
|-------|-----|
| Asserting internals / counts of private state | Assert business outcomes |
| `assertTrue(result)` with no meaning | Precise assertions |
| Mock everything | Fake or real simple collaborators |
| Unit tests that open DB/network | Move to integration or stub |
| Commented-out / ignored tests | Delete or fix |
| Ice-cream cone (many E2E, few unit) | Rebalance toward unit |

## Layers in this codebase

| Code under test | Prefer |
|-----------------|--------|
| `domain/` | Unit + TDD |
| `service/` | Unit with Fake repos; light integration |
| `infra/` / `controller/` | Slice integration (`@DataJpaTest`, `@WebMvcTest`) |
| Critical UI flows | Few E2E |

## Spring Boot 4 slice tests

Use modular test starters already on the classpath (`spring-boot-webmvc-test`,
`spring-boot-data-jpa-test`). Default `./gradlew test` excludes `@Tag("integration")`;
run `./gradlew integrationTest` for full-context ITs.

### Controller — `@WebMvcTest`

Do **not** instantiate controllers with `new` for HTTP contract tests. Prefer
`MockMvcTester` + `@MockitoBean` collaborators:

```java
@WebMvcTest(controllers = ChatController.class)
@ActiveProfiles("test")
class ChatControllerTest {

  @Autowired MockMvcTester mvc;
  @MockitoBean ChatUseCase chatUseCase;
  @MockitoBean OwnerContext ownerContext;

  @Test
  @DisplayName("should return 400 when chat message is null")
  void shouldReturn400WhenChatMessageIsNull() {
    when(ownerContext.requireValue(any())).thenReturn("c:client-1");
    assertThat(mvc.post().uri("/api/chat")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"message\":null}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }
}
```

Attach client identity when needed: `.with(ClientIdentityRequestPostProcessor.withClientId(...))`.

Prefer `@SliceWebMvcTest` (in `com.ai.testsupport`) over raw `@WebMvcTest` — it excludes
application servlet filters (quota, CSRF, client identity) so controller slices start
without the full filter dependency graph.

References:
- [Spring Boot Testing](https://docs.spring.io/spring-boot/reference/testing/index.html)
- [Test modules](https://docs.spring.io/spring-boot/reference/testing/test-modules.html)

### JPA — `@DataJpaTest`

Extend `AbstractDataJpaTest`, add `@EntityScan` for the aggregate under test, and assert
round-trip persistence (mapping, converters, custom queries):

```java
@DataJpaTest
@EntityScan(basePackageClasses = ChatSession.class)
@Import(SpringDataChatSessionRepository.class)
class ChatSessionJpaTest extends AbstractDataJpaTest {

  @Autowired TestEntityManager em;
  @Autowired SpringDataChatSessionRepository repository;

  @Test
  @DisplayName("should round-trip session when persisted")
  void shouldRoundTripSessionWhenPersisted() { /* ... */ }
}
```
