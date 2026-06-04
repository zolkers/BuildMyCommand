<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Contributing

Thanks for helping improve BuildMyCommand. The project aims to stay small at the surface, modular internally, and predictable across adapters.

## Development Setup

Requirements:

- JDK 21
- Git
- Gradle through the included wrapper

Run the full verification suite before opening a pull request:

```bash
./gradlew clean check
```

On Windows:

```powershell
.\gradlew.bat clean check
```

## Branches

- `ddev` is the development branch.
- `master` is the release branch.
- Release automation runs only after changes are merged into `master`.

Open pull requests from `ddev` or feature branches into `master` for releases.

## Code Style

- Prefer the canonical Route/SubRoute DSL for examples and public docs.
- Keep adapters behind the adapter contract.
- Keep platform-specific behavior inside platform modules.
- Add tests for parser, registry, adapter, and IntelliJ plugin behavior when changing command semantics.
- Keep public APIs documented with Javadoc and a compact usage example.
- Preserve the MIT/SPDX license header on source, docs, scripts, and configuration files that support comments.

## Pull Requests

Before submitting:

- Explain the behavior change.
- Mention affected modules.
- Add or update tests.
- Update docs when user-facing behavior changes.
- Run `./gradlew clean check`.

Small focused pull requests are easier to review and release.
