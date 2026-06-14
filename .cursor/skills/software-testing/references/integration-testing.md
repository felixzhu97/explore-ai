# Integration Testing

Tests that verify the correct collaboration between components, including database access, API calls, and message queue interactions.

## When to Use

Use integration tests when unit tests are insufficient to verify cross-component behavior. Integration tests are appropriate for verifying repository implementations, REST controller endpoints, message producer/consumer contracts, and any code that depends on external resources. They should run against real infrastructure (or test containers) and cover the paths that unit tests cannot reach.

## Core Idea

### Database Integration Testing

```java
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Rollback(false)  // Set to false when viewing test data
    void shouldPersistAndRetrieveOrder() {
        // Given
        Order order = OrderTestBuilder.builder()
            .withLine(Money.of(100), 2)
            .build();
        order.place();

        // When
        orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Order> found = orderRepository.findById(order.getId());
        assertThat(found).isPresent();
        assertThat(found.get().totalAmount()).isEqualTo(Money.of(200));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void shouldFindOrdersByCustomer() {
        // Given
        Customer customer = createCustomer("test@example.com");
        Order order1 = createOrder(customer);
        Order order2 = createOrder(customer);
        orderRepository.saveAll(List.of(order1, order2));

        // When
        Page<Order> orders = orderRepository.findByCustomer(customer.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(orders.getTotalElements()).isEqualTo(2);
    }
}
```

### API Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrderAndReturn201() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
            List.of(new OrderLineRequest("prod-1", 2))
        );

        // When/Then
        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("PLACED"))
            .andExpect(jsonPath("$.totalAmount").value(200.00));
    }

    @Test
    void shouldReturn400WhenOrderIsEmpty() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(List.of());

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("empty")));
    }
}
```

### Message Queue Integration Testing

```java
@Testcontainers
class OrderEventIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaMessageListenerContainer<String, OrderPlacedEvent> listener;

    @Test
    void shouldReceiveOrderPlacedEvent() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        OrderPlacedEvent receivedEvent = null;

        listener.setupMessageListener(event -> {
            receivedEvent = event;
            latch.countDown();
        });

        // When
        Order order = createPlacedOrder();
        kafkaTemplate.send("order-events", order.getId().toString(),
            new OrderPlacedEvent(order.getId(), order.getTotalAmount()));

        // Then
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedEvent.orderId()).isEqualTo(order.getId());
    }
}
```

## Bad/Good Examples

### Good: Testcontainers for Real Database

```java
@Testcontainers
class PostgresOrderRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldPersistOrderWithAllLines() {
        Order order = OrderTestBuilder.builder().withLine(Money.of(50), 3).build();
        repository.save(order);

        Order found = repository.findById(order.getId()).orElseThrow();
        assertThat(found.getLines()).hasSize(1);
    }
}
```

### Bad: Integration Tests Without Transactions

```java
// ❌ No isolation — tests bleed state
@SpringBootTest
class BadRepositoryTest {
    @Autowired OrderRepository repository;

    @Test
    void test1() { repository.save(order1); }

    @Test
    void test2() {
        // test1's data may still be here
        assertThat(repository.findAll()).isNotEmpty();
    }
}
```

- Tests must be independent; use `@Transactional` or fresh containers per test
- False positives make tests unreliable

## Real Implementation Reference

- Repository tests: `apps/server/src/test/java/com/ai/infrastructure/persistence/`
- Controller tests: `apps/server/src/test/java/com/ai/interface/controller/`
- Message tests: `apps/server/src/test/java/com/ai/integration/`

## Related References

- [Unit Testing](./unit-testing.md)
- [E2E Testing](./e2e-testing.md)
- [Test Data Management](./test-data-management.md)
- [Test Coverage](./test-coverage.md)
- [Software Testing](../SKILL.md)
