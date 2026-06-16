# C4 PlantUML Templates

PlantUML templates for generating C4 Model diagrams. These templates use the C4-PlantUML stdlib to produce standardized architecture diagrams.

## When to Use

Use these templates when you need to generate machine-readable, version-controllable architecture diagrams. PlantUML diagrams can be included in documentation, generated as part of CI/CD, and reviewed alongside code changes.

## System Context Template

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml

LAYOUT_WITH_LEGEND()

title System Context diagram for Internet Banking System

Person(customer, "Customer", "A customer of the bank")
Person(admin, "Admin", "A bank administrator")
System(banking_system, "Internet Banking System", "Allows customers to view their accounts and make payments")

System_Ext(email, "Email System", "The internal email system")
System_Ext(payment, "Payment Gateway", "Processes online payments")
System_Ext(fax, "Fax System", "The internal fax system")

Rel(customer, banking_system, "Uses")
Rel(admin, banking_system, "Uses")
Rel(banking_system, email, "Sends email via")
Rel(banking_system, payment, "Processes payments via")
Rel(admin, fax, "Sends fax via")
@enduml
```

## Container Template

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

LAYOUT_WITH_LEGEND()

title Container diagram for Internet Banking System

Person(customer, "Customer", "A customer of the bank")

Container(web_app, "Web Application", "Java, Spring MVC", "Serves web pages")
Container(mobile_app, "Mobile App", "Xamarin", "Cross-platform mobile app")
Container(api, "API Application", "Java, Spring Boot", "Provides API")
ContainerDb(db, "Database", "PostgreSQL", "Stores data")

System_Ext(email, "Email System", "Sends emails")

Rel(customer, web_app, "Uses")
Rel(customer, mobile_app, "Uses")
Rel(web_app, api, "Calls")
Rel(mobile_app, api, "Calls")
Rel(api, db, "Reads/Writes")
Rel(api, email, "Sends emails")
@enduml
```

## Component Template

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

LAYOUT_WITH_LEGEND()

title Component diagram for API Application

Container(api, "API Application", "Java, Spring Boot", "Provides REST API for banking operations")

ContainerDb(db, "Database", "PostgreSQL", "Stores customer and account data")
System_Ext(audit, "Audit System", "External audit logging service")

Component(account_controller, "AccountController", "Spring MVC", "Handles account requests")
Component(account_service, "AccountService", "Spring @Service", "Business logic for accounts")
Component(transaction_service, "TransactionService", "Spring @Service", "Processes transactions")

Component(account_repository, "AccountRepository", "Spring Data JPA", "Persists Account entities")
Component(transaction_repository, "TransactionRepository", "Spring Data JPA", "Persists Transaction entities")

Rel(account_controller, account_service, "Uses")
Rel(transaction_service, account_service, "Uses")
Rel(account_service, account_repository, "Uses")
Rel(account_service, transaction_repository, "Uses")
Rel(account_service, audit, "Publishes events to")
Rel(account_repository, db, "Reads/Writes")
@enduml
```

## Deployment Template

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Deployment.puml

LAYOUT_WITH_LEGEND()

title Deployment diagram for Internet Banking System

Deployment_Node(docker, "Docker Host", "Docker") {
    Container(web_app, "Web Application", "Java, Spring Boot", "Serves web UI")
    Container(api, "API Application", "Java, Spring Boot", "REST API")
    ContainerDb(postgres, "PostgreSQL", "Database", "Stores data")
}

Deployment_Node(cloud, "Cloud Provider") {
    Deployment_Node(k8s, "Kubernetes Cluster") {
        Container(email_service, "Email Service", "Spring Boot", "Sends emails")
    }
}

Rel(web_app, api, "HTTPS")
Rel(api, postgres, "JDBC")
Rel(api, email_service, "HTTP")
```

## PlantUML Tools

| Tool | Description |
|------|-------------|
| **VS Code Extension** | PlantUML extension with live preview |
| **Standalone** | PlantUML JAR with PlantUML server |
| **Online** | PlantText PlantUML editor for quick prototyping |
| **CI/CD Integration** | Generate PNG/SVG during build pipelines |

## Real Implementation Reference

The C4-PlantUML templates can be integrated into a Java/Spring Boot project's documentation:

```bash
# Generate diagrams as part of the build
mvn plantuml:generate
```

Add PlantUML diagram files alongside code to document architecture decisions:

```
docs/
├── context.puml
├── container.puml
└── components/
    ├── order-component.puml
    └── payment-component.puml
```

## Related References

- [C4 Four Levels](./c4-four-levels.md) — The four abstraction levels these templates generate
- [C4 Documentation Best Practices](./c4-documentation-best-practices.md) — How to keep diagrams accurate over time
- [Architecture Decision Records](./architecture-decision-records.md) — Document why technology choices were made
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — How diagrams map to Clean Architecture layers
