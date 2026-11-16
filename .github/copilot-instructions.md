# Project Overview

This public repository holds the design patterns published by Arc-E-Tect: one document per pattern, and a reference implementation per pattern.
Each pattern lives in its own folder under `patterns/`; the API-Only specification library that the reference implementations subscribe to is shared at `api/`, with one bundle target per pattern.
`docs/decision-log.adoc` records every decision taken for this repository; read it before changing structure, tooling or conventions.

## Security: zero credentials policy

No passwords, secrets, private keys, tokens, credentials, or sensitive production data may be committed to version control, including test credentials.
`ZERO_CREDENTIALS_POLICY.md` is the policy, and `SECURITY.md` lists the controls that enforce it.

- Use environment variables or a deployment secret manager for all secrets.
- Use placeholders such as `${API_TOKEN}` or `<API_TOKEN_DESCRIPTION>` in documentation and configuration templates.
- Never assign secret-like configuration values literal defaults, including in tests.
- Keep `.env` files local and ignored. Only sanitized `.env.example`, `.env.sample`, or `.env.template` files may be committed.
- Never disclose a credential in source, documentation, logs, issues, pull requests, or generated artifacts.
- Store CI/CD secrets in GitHub Actions secrets. The credentials for the Gradle Plugin Portal and Maven Central, and the signing keys, belong in the `release` environment.

When a new secret is required:

1. Document its name and purpose in a sanitized example file.
2. Read it through an environment variable or a secret-manager integration.
3. Add the relevant local file pattern to `.gitignore`.
4. Verify the staged-content checks in `.githooks/pre-commit` reject a hardcoded value.
5. Update `SECURITY.md`, `ZERO_CREDENTIALS_POLICY.md` or other operational documentation when the handling policy changes.

## Git hooks

Install the repository hooks before contributing:

```bash
./scripts/setup-hooks.sh
```

The hooks provide local defense in depth by rejecting keystores and likely hardcoded secrets, linting modified GitHub Actions workflows, rejecting npm manifests with a UTF-8 byte order mark, and preventing pushes to branches with closed or merged pull requests. They do not replace server-side controls and must not be bypassed except for a documented, reviewed reason. `.githooks/README.md` describes each check.

## GitHub Actions

- Keep the default `GITHUB_TOKEN` read-only. Every workflow declares `permissions: contents: read` at the top, and a job raises only what its own use of `GITHUB_TOKEN` needs. A job that authenticates with a personal access token gets no write grant.
- All external actions in `uses:` must be pinned to a full 40-character commit SHA, which the repository enforces. Include a version comment beside the SHA.
- Use only GitHub-owned or verified-creator actions unless the repository's allowed-actions policy is deliberately extended and documented.
- Every job that runs on a runner declares `timeout-minutes`. Every workflow that is not only reusable declares a `concurrency` group: builds cancel superseded runs, releases queue.
- Every `actions/checkout` sets `persist-credentials: false`, unless a later step in the same job pushes with git; such a checkout carries a comment that says so.
- A job that uses the Gradle Plugin Portal, Maven Central or signing secrets declares `environment: release`.
- Run `actionlint` after changing a workflow, and fix all reported errors before committing.
- Do not use `pull_request_target` with untrusted pull-request code. Do not expose secrets to pull-request workflows from forks.
- Use immutable action, container, and dependency versions. Do not use mutable `latest` tags.

## Commits and pull requests

- Commits follow Conventional Commits and are signed; `.github/git-commit-instructions.md` gives the types and scopes.
- When a commit contains code that Claude changed, its message ends with the trailer `Co-authored-by: Claude <noreply@anthropic.com>`.
- Work on a branch and merge to `main` through a pull request, which is squash-merged. Do not bypass the repository ruleset.
- Create Git worktrees in `../worktrees-<repo name>/`, never inside the checkout.
- Before pushing an existing branch, check that its pull request is still open. Do not push additional commits to a merged or closed pull-request branch.

## Documentation

- AsciiDoc and Markdown use one sentence per line; never wrap a sentence over several lines.
- Every project has a `README.adoc` that explains what the project is, and how to build, test and run it.
- A reference implementation omits security on purpose; its README and the root README say so, and that it must not be used as-is in production.

## Build conventions

- Gradle only, with the Groovy DSL and a committed wrapper; every project is a standalone build.
- Java 25.
- Versions are managed with the `de.fayard.refreshVersions` plugin and kept in `gradle/libs.versions.toml`; `versions.properties` stays empty, and build scripts contain no versions or inline coordinates.
- Every catalog alias carries the suffix `-sedr`, in all alias categories (`versions`, `libraries`, `bundles` and `plugins`). Dependencies managed by a BOM are catalog entries without a version.
- Use `gradlePluginPortal()` for Gradle plugins and `mavenCentral()` for everything else; `mavenLocal()` only where a decision in the decision log allows it.
- Test suites are declared only in the `testing { suites { ... } }` DSL: `test` (unit), `testComponent`, `testContract`, `testSystem` and `testArchitecture`. Never use the term "integration test".
- Unit tests carry two gates: 100% JaCoCo coverage and a 100% PIT mutation score. A test exists to verify one behaviour of the unit, with one assertion and a `@DisplayName` that says what it proves; coverage follows from that, never the other way round.
