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
- Jira domain is `https://felixzhu.atlassian.net/`, and the corresponding `cloudId` is `https://felixzhu.atlassian.net`

## Execution Flow

### 1. Identify or Create a Jira Issue

**Existing issue:**
Use the user-provided issue key directly, such as `AI-11`.

**New issue:**
Create it according to project standards. The issue title must be in English. The description must include:

- **Background**: why this work is needed
- **User Story**: `As a ... I want ... So that ...`
- **Acceptance Criteria**: at least 3 GIVEN-WHEN-THEN items
- **Notes**: additional constraints, follow-ups, or implementation hints

Also estimate story points and write them to `customfield_10016` inside the `additional_fields` parameter.

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

The PR description template is in `.cursor/skills/jira-driven-development/templates/pr-body.md`.

Command:

```bash
PR_BODY=$(sed "s/<ISSUE_KEY>/<issue-key>/g" .cursor/skills/jira-driven-development/templates/pr-body.md)
gh pr create --title "<type>: <short description>" --base <base-branch> --body "$PR_BODY"
```

### 7. Sync Jira

| Timing | Action |
|---|---|
| Start development | Comment on issue: Started development on branch `<branch>` |
| Create PR | Comment on issue: PR opened: `<pr-url>` |
| PR merged | Transition issue to `Done` |

Comment templates are in `.cursor/skills/jira-driven-development/templates/jira-comment.md`.

## Branch Chain Rules

```
main
 └── feat/AI-10              # PR #1 -> base: main
      └── feat/AI-11         # PR #2 -> base: feat/AI-10
           └── feat/AI-12    # PR #3 -> base: feat/AI-11
```

Except for the initial feature branch, every PR in the chain must be based on the previous feature branch, not directly on `main`.

## Quick Command Reference

```bash
# Create branch
git checkout -b feat/AI-11

# Commit
git commit -m "feat: ...

References:
- [Title](URL)"

# Push
git push -u origin HEAD

# Create PR
PR_BODY=$(sed "s/<ISSUE_KEY>/AI-11/g" .cursor/skills/jira-driven-development/templates/pr-body.md)
gh pr create --title "feat: ..." --base feat/<prev> --body "$PR_BODY"

# View PR
gh pr view
```

## Notes

- Commit messages must comply with project standards
- One commit = one complete change
- Do not add redundant signatures such as `Co-authored-by`, `Made with xxx`
- Search for authoritative references before committing
