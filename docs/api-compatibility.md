# API Compatibility

The project is currently `0.0.1`.

Until a `1.0.0` API freeze, public APIs may evolve when needed for architecture, adapter support, or correctness. Compatibility decisions should follow these rules:

- Prefer additive changes in `api`.
- Keep convenience APIs when adding richer variants.
- Do not leak platform dependencies into `api` or `core`.
- Keep adapter native types at module boundaries.
- Document breaking changes in release notes before publishing a non-SNAPSHOT version.

Recommended future gate:

- add binary compatibility validation before the first stable release
- publish an API baseline after `1.0.0`
- treat `api`, `core`, `dsl`, `annotations`, `adapters`, and `testkit` as compatibility-sensitive modules
