---
name: software-testing
description: Software Testing Methodology Guide. Covers TDD Test-Driven Development, BDD Behavior-Driven Development, Testing Pyramid, Unit Testing, Integration Testing, E2E Testing Strategies and Practices. Detailed references for each topic are available in the references/ directory.
---

# Software Testing

A comprehensive guide to software testing methodology, covering testing strategy, development practices, testing types, and quality guidelines.

## When to Use

Use this skill when designing a test strategy, writing unit or integration tests, applying TDD or BDD practices, or reviewing test quality. The skill provides both conceptual foundations and practical code examples across Java/JUnit, Spring Boot, and TypeScript/Playwright.

## Quick Reference

| Topic | File |
|-------|------|
| Testing Strategy Overview | ./references/testing-strategy-overview.md |
| TDD Test-Driven Development | ./references/tdd-test-driven-development.md |
| BDD Behavior-Driven Development | ./references/bdd-behavior-driven-development.md |
| Unit Testing | ./references/unit-testing.md |
| Integration Testing | ./references/integration-testing.md |
| E2E Testing | ./references/e2e-testing.md |
| Test Coverage | ./references/test-coverage.md |
| Test Data Management | ./references/test-data-management.md |
| Test Review Checklist | ./references/test-review-checklist.md |
| Testing Anti-Patterns | ./references/testing-anti-patterns.md |

## How to Use This Skill

Start with **Testing Strategy Overview** to understand the testing pyramid and layer responsibilities. Then use the specific reference for your current task: write unit tests following **Unit Testing** with AAA Pattern and Test Doubles; apply **TDD** or **BDD** when starting a new feature; validate integration points with **Integration Testing** using `@SpringBootTest` and Testcontainers; write E2E flows with **E2E Testing** using Playwright. Use **Test Review Checklist** during code review, **Testing Anti-Patterns** to diagnose problems, and **Test Coverage** to identify gaps.
