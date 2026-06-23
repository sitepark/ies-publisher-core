# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IES Publisher Core is the core library of the IES Publisher component — a multi-module Maven project that manages publishing pipelines, link checking, and channel management. It follows strict **Hexagonal Architecture** (Ports & Adapters) and uses the Java 25 module system.

## Build & Common Commands

```bash
# Compile
mvn clean compile

# Run all tests
mvn test

# Full build with checks (formatting, PMD, Error Prone + NullAway)
mvn clean verify

# Run a single test class
mvn test -pl linkchecker -Dtest=BackgroundLinkCheckTest

# Auto-format code (Google Java Style) — run before committing
mvn spotless:apply

# Check formatting without applying
mvn spotless:check

# Static analysis (PMD). Error Prone + NullAway run during normal compilation.
mvn pmd:check

# Code coverage report (target/site/jacoco/index.html)
mvn jacoco:report
```

**Requires JDK 25.** Error Prone runs as a compiler plugin; the required `--add-exports`/`--add-opens` for `jdk.compiler` live in `.mvn/jvm.config` and are picked up automatically (locally and in CI).

## Module Structure

```
ies-4-publisher-core/
├── channel/      - Channel management (GetAllChannelInfos use case)
├── linkchecker/  - External link validation with scheduling
├── publishing/   - URL mapping and publish pipeline
└── pom.xml       - Parent POM (dependency management, plugin config)
```

Each module follows the same internal layer structure:
- `domain/entity/` — immutable domain entities (Java records + Builder)
- `domain/value/` — immutable value objects
- `domain/exception/` — business rule violations
- `usecase/` — one class per use case, injected via `@jakarta.inject.Inject`
- `port/` — pure interfaces (Repository, Service, AccessControl)
- `service/` — internal domain services (no external dependencies)

Only `domain.entity`, `domain.value`, `port`, and `usecase` packages are exported via `module-info.java`.

## Architecture Rules

**Dependency direction is strictly enforced:**
- Use Cases → Ports + Domain (allowed)
- Domain → nothing except JDK (allowed)
- Ports → nothing (allowed — pure interfaces only)
- Domain → Use Cases or Ports (forbidden)

Use cases never depend on each other. Ports are never implemented inside this library — implementations live in adapter modules outside this repo.

## Code Patterns

### Domain Entities (Records + Builder)

Each `module-info.java` is `@NullMarked` (JSpecify), so every type is non-null by
default — only genuinely-optional values get `@Nullable`. NullAway (in JSpecify mode)
enforces this at compile time.

```java
@JsonDeserialize(builder = MyEntity.Builder.class)
public record MyEntity(String id, String name, @Nullable String description) {
  public MyEntity {  // compact constructor validates required fields
    Objects.requireNonNull(id, "id is required");
  }
  public static Builder builder() { return new Builder(); }

  @JsonPOJOBuilder(withPrefix = "")
  @SuppressWarnings("NullAway.Init") // required fields are set after construction
  public static class Builder {
    private String id;
    private String name;
    private @Nullable String description; // optional → nullable
    public Builder id(String id) { this.id = id; return this; }
    public Builder name(String name) { this.name = name; return this; }
    public Builder description(String description) { this.description = description; return this; }
    public MyEntity build() { return new MyEntity(id, name, description); }
  }
}
```

### Use Cases

```java
public class StoreMyEntity {
  private final MyEntityRepository repository;
  private final AccessControl accessControl;

  @Inject
  public StoreMyEntity(MyEntityRepository repository, AccessControl accessControl) {
    this.repository = repository;
    this.accessControl = accessControl;
  }

  public void execute(MyEntity entity) { ... }
}
```

### Ports

```java
public interface MyEntityRepository {
  void store(MyEntity entity);
  Optional<MyEntity> findById(String id);
}
```

## Testing

- **JUnit 5** + **Mockito** + **AssertJ** — standard stack across all modules
- **EqualsVerifier** and **ToStringVerifier** for contract tests on domain objects
- Mock all Port dependencies; test business behavior, not implementation details

```bash
# Run tests for a specific module
mvn test -pl linkchecker

# Run a single test method
mvn test -pl linkchecker -Dtest=BackgroundLinkCheckTest#methodName
```

## Code Quality Requirements

All checks run as part of `mvn verify` and **must pass before merging**:

| Tool | Config | Threshold |
|---|---|---|
| Spotless | Google Java Format | Zero violations |
| Error Prone + NullAway | JSpecify mode, NullAway as `ERROR` (main code only) | Any finding fails the build |
| PMD 7 | Custom ruleset | High priority failures block |
| JaCoCo | Report only | No enforced minimum |

NullAway is disabled for test code (tests deliberately pass `null` to verify rejection).

Always run `mvn spotless:apply` before committing to avoid formatting failures in CI.

## Key Dependencies

- `com.sitepark.ies:ies-shared-kernel` — shared domain types across IES modules
- `jakarta.inject:jakarta.inject-api` — DI annotations (`@Inject`); no Spring
- `com.fasterxml.jackson.*` — JSON serialization (databind, jsr310, jdk8)
- `org.jspecify:jspecify` — null-safety annotations (`@NullMarked`, `@Nullable`) enforced by NullAway
