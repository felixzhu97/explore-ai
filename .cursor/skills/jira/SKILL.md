---
name: jira
description: Creates and updates Jira tickets for ExploreAI. Always use the project template (Background, User Story, numbered GIVEN-WHEN-THEN scenarios, Definition of Done, Story Points) and cite official docs or research links when referencing standards. Use when creating or editing Jira issues, acceptance criteria, story points, or calling Jira MCP tools.
---

# Jira

**Every** new or edited ticket in this project **must** use the template below. Do not invent alternate structures.

When Background or DoD cites external standards, APIs, or papers: use **official documentation** and **research** URLs (same priority as [developer](../developer/SKILL.md) §5 — [dependency-docs](../developer/references/dependency-docs.md), [sources.md](../business-tech-analysis/references/sources.md), arXiv abs pages).

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
- **Definition of Done**

## User Story Format

Every ticket must include a user story:

```
**As a** [role]
**I want** [action/feature]
**So that** [benefit/value]
```

## Acceptance Criteria Format

Use **numbered scenarios** with standard **GIVEN / WHEN / THEN** (and optional **AND**). Each scenario is independently testable.

```
1. Scenario: [short business outcome name]
   **GIVEN** [precondition / system state]
   **WHEN** [user or system action]
   **THEN** [observable result]
   **AND** [additional observable result]   ← optional
```

### Rules

- One scenario = one behavior; name the scenario in business language
- At least **3–5** scenarios per ticket (happy path + edge/error cases)
- **GIVEN** = state only; **WHEN** = single trigger; **THEN/AND** = observable outcomes only
- No implementation details (APIs, DB, class names)
- Present tense ("the user clicks", not "the user will click")
- UI tickets: reference OpenAI-style interaction patterns where relevant

### Good Examples

```
1. Scenario: Open file picker on upload
   **GIVEN** a user is logged in
   **WHEN** they click the "Upload" button
   **THEN** a file picker dialog opens

2. Scenario: Retry after network loss during upload
   **GIVEN** an upload is in progress
   **WHEN** the network connection is lost
   **THEN** an error message is displayed
   **AND** the user can retry the upload
```

### Bad Examples (Avoid)

```
1. Scenario: Call API
   **WHEN** the button is clicked
   **THEN** the system calls the upload API and saves to the database

2. Scenario: Upload works
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

## Definition of Done

Replace free-form Notes. Every ticket lists concrete Done checks (adapt to the work; keep all that apply):

```
- [ ] Acceptance Criteria scenarios pass (manual and/or automated)
- [ ] Unit / relevant tests added or updated (`should_…_when_…`)
- [ ] Code follows architecture + Domain Glossary Preferred Terms
- [ ] No new lint/build failures
- [ ] Docs / Domain Glossary updated if concepts changed
- [ ] Story Point estimate set (`customfield_10016`) for Story/Task
- [ ] Linked commit/PR includes References (official docs / research)
- [ ] External citations use official or research URLs (not random blogs)
```

Add ticket-specific Done items when needed (e.g. a11y check, migration run, feature flag off by default).

## Ticket Structure Template

```
## Background

[Explain why this work is needed]

## User Story

**As a** [role]
**I want** [action/feature]
**So that** [benefit/value]

## Acceptance Criteria

1. Scenario: [name]
   **GIVEN** [precondition]
   **WHEN** [action]
   **THEN** [outcome]

2. Scenario: [name]
   **GIVEN** [precondition]
   **WHEN** [action]
   **THEN** [outcome]
   **AND** [outcome]

3. Scenario: [name]
   **GIVEN** [precondition]
   **WHEN** [action]
   **THEN** [outcome]

## Definition of Done

- [ ] Acceptance Criteria scenarios pass (manual and/or automated)
- [ ] Unit / relevant tests added or updated (`should_…_when_…`)
- [ ] Code follows architecture + Domain Glossary Preferred Terms
- [ ] No new lint/build failures
- [ ] Docs / Domain Glossary updated if concepts changed
- [ ] Story Point estimate set (`customfield_10016`) for Story/Task
- [ ] Linked commit/PR includes References (official docs / research)
- [ ] External citations use official or research URLs (not random blogs)
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

- [ ] Contains Background, User Story, Acceptance Criteria, and Definition of Done
- [ ] User story follows As a / I want / So that format
- [ ] At least 3 numbered scenarios with GIVEN / WHEN / THEN
- [ ] Acceptance criteria focus on behavior, not implementation
- [ ] Edge cases and error states are covered
- [ ] OpenAI-style interaction patterns referenced where applicable
- [ ] Definition of Done checkboxes are concrete and testable
- [ ] **Story Point is filled in** (`customfield_10016`)

## Atlassian MCP Integration

For MCP tool list, JSON examples, localized issueType mapping, and workflow, see [references/mcp.md](references/mcp.md).
