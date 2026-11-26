# ADR-011: Annotate domain classes with JPA mappings (no separate entity classes)

- Status: Accepted
- Date: 2026-11-26

## Context
Week 2 stores customers and accounts in PostgreSQL through JPA/Hibernate. There are two common ways to do that:

1. Keep the domain classes free of persistence annotations and add separate "entity" classes with mapper code between the two.
2. Put the JPA annotations (`@Entity`, `@Column`, ...) directly on the domain classes.

## Decision
Option 2. `Customer` and `Account` carry `jakarta.persistence` annotations. These come from the standard JPA specification, not from Spring, so the domain still has no framework dependency beyond that API.

The cost of this choice is visible in the code and is accepted:
- fields are not `final` and each class has a `protected` no-argument constructor that only JPA uses;
- classes cannot be `final` (Hibernate may need to subclass them);
- `createdAt` is truncated to microseconds when created, so the in-memory value equals what PostgreSQL stores.

Business rules stay inside the classes and are still tested with plain `new` and a fixed `Clock`, without Spring or a database.

## Consequences
- Much less code than two parallel class hierarchies plus mappers, which suits a project of this size.
- The domain classes are not completely persistence-ignorant. If the model grows much richer than its tables, or a second storage technology appears, we can introduce separate entity classes behind the existing `CustomerRepository` and `AccountRepository` ports without changing services or controllers. That escape hatch is why the ports and JPA adapters stay.
- Hibernate schema validation (`ddl-auto: validate`) checks every mapping against the Flyway-created schema at startup, so mapping and schema cannot silently drift apart.
