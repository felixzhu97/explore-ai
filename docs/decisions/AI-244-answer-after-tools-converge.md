# AI-244 — Converge AnswerAfterToolsAdvisor

Status: Implemented  
Type: Implementation

## Goal

Behavior-parity tests, then replace custom `AnswerAfterToolsAdvisor` with official ToolCalling chain when safe.

## Outcome

Replaced `AnswerAfterToolsAdvisor` with:

- **`ToolCallLoopGuardAdvisor`** — minimal official `ToolCallingAdvisor` subclass; only overrides `doBeforeCall` / `doBeforeStream` to call `ToolCallLoopGuard.maybeDisableToolsRequest` (per-iteration tool disable for DeepSeek DSML).
- **`LoopGuardToolCallingManager`** — decorates the default `ToolCallingManager` to append stage reminders via `ToolCallLoopGuard.withStageReminder` after each tool execution.

Parity covered by `ToolCallLoopGuardAdvisorTest` and extended `ToolCallLoopGuardTest`.

`ToolSearchAdvisor` was not added (AI-251).

## References

- https://docs.spring.io/spring-ai/reference/api/tools.html
- https://docs.spring.io/spring-ai/reference/api/advisors.html
