# Jira-Driven Development Workflow

> Checkout a branch from a Jira issue, develop following Clean Architecture + DDD + TDD, submit with References, and keep Jira status in sync.

## Trigger Scenarios

Enable this skill when the user intends any of the following:

- "start development"
- "create a Jira task"
- "develop from issue AI-123"
- "create a feature branch to develop"
- "submit a PR"

## Pre-flight Checks

Before starting, confirm:

- The repository root is the project workspace
- `gh` CLI is installed and available
- Atlassian MCP is configured and can use `createJiraIssue`, `searchJiraIssuesUsingJql`, `getJiraIssue`, `addCommentToJiraIssue`, `transitionJiraIssue`
- Jira domain is `http://felixzhu.atlassian.net/`, and the corresponding `cloudId` is `felixzhu.atlassian.net`

## Execution Flow

### 1. Identify or Create a Jira Issue

**Existing issue:**
Use the user-provided issue key directly, such as `AI-11`.

**New issue:**
Create it according to project standards, ensuring it includes:

- **User Story**: `As a ... I want ... So that ...`
- **AC**: at least 3 GIVEN-WHEN-THEN items
- **Story Point**: estimate and write to `customfield_10016`

After creation, save and return the issue key.

### 2. Confirm the Upstream Branch

By default, continue chained development from the latest feature branch; if none exists, start from `main`.

- Previous feature branch: `feat/<previous-feature>`
- Initial branch: `main`

### 3. Create a Feature Branch

Naming rule: `<type>/<ISSUE_KEY>`

| Type | Example |
|---|---|
| feat | `feat/AI-11` |
| fix | `fix/AI-11` |
| refactor | `refactor/AI-11` |

Command:

```bash
git checkout <base-branch>
git pull origin <base-branch>
git checkout -b <branch-name>
```

### 4. Development

Follow project standards:

- Clean Architecture / Hexagonal Architecture
- Rich Domain Model; anemic model is forbidden
- Prefer TDD: RED -> GREEN -> REFACTOR
- Perform a self-review before committing

### 5. Submit Changes

#### 5.1 Determine References

Before committing, search for authoritative references; include at least 1 documentation or official link.

Acceptable sources:

- Official documentation
- Authoritative technical articles
- Official docs for existing project dependencies

#### 5.2 Commit Command

```bash
git add .
git commit -m "$(cat <<'EOF'
<type>: <short description>

References:
- [Title](URL)
EOF
)"
```

#### 5.3 Push to Remote

```bash
git push -u origin HEAD
```

### 6. Create a PR

The PR title should match the commit title.

The PR description template is in `templates/pr-body.md`.

Command:

```bash
gh pr create --title "<type>: <short description>" --base <base-branch> --body "$(cat <<'EOF'
## Summary
- ...

## Jira Ticket

- http://felixzhu.atlassian.net/browse/<ISSUE_KEY>

## References
- [Title](URL)
EOF
)"
```

### 7. Sync Jira

| Timing | Action |
|---|---|
| Start development | Comment on issue: Started development on branch `<branch>` |
| Create PR | Comment on issue: PR opened: `<pr-url>` |
| PR merged | Transition issue to `Done` |

Comment templates are in `templates/jira-comment.md`.

## Branch Chain Rules

```
main
 └── feat/base-feature       # PR #1 -> base: main
      └── feat/extend-1      # PR #2 -> base: feat/base-feature
           └── feat/extend-2 # PR #3 -> base: feat/extend-1
```

Every PR must be based on the **previous feature branch**, not directly on `main`.

## Quick Command Reference

```bash
# Create branch
git checkout -b feat/AI-11-<desc>

# Commit
git commit -m "feat: ...\n\nReferences:\n- [Title](URL)\n"

# Push
git push -u origin HEAD

# Create PR
gh pr create --title "feat: ..." --base feat/<prev> --body "$(cat <<'EOF'
## Summary

## Test plan

## References
EOF
)"

# View PR
gh pr view
```

## Notes

- Commit messages must comply with project standards
- One commit = one complete change
- Do not add redundant signatures such as `Co-authored-by`, `Made with xxx`
- Search for authoritative references before committing
