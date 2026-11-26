# Week 2 study guide: PostgreSQL, Flyway and JPA for a C# developer

Week 2 replaced the in-memory storage with PostgreSQL. The services and controllers did not change except for transactions, which is the payoff of the repository interfaces from Week 1.

## Suggested reading order

1. `src/main/resources/db/migration/V1__customers_and_accounts.sql`: the schema, with its constraints and the sequence.
2. `customers/domain/Customer.java`, then `accounts/domain/Account.java` and `CurrencyConverter.java`: the same classes as Week 1, now carrying JPA annotations.
3. `customers/infra/*` and `accounts/infra/*`: a Spring Data interface plus a small adapter implementing the Week 1 port.
4. `accounts/infra/SequenceAccountNumberGenerator.java` and the `AccountNumberGenerator` interface it implements.
5. `CustomerService` and `AccountService`: `@Transactional`.
6. `shared/error/ApiExceptionHandler.java`: the new database-conflict handler.
7. Tests: `support/PostgresTestSupport`, `support/RepositoryTest`, the two `Jpa*RepositoryTest` classes, then `ApiEndToEndTest`.
8. `docs/adr/ADR-011-jpa-annotations-on-domain-classes.md`.

## Concept map

| Java / Spring (this repo) | C# / .NET counterpart | Where to look |
|---|---|---|
| Flyway `V1__name.sql` migrations, applied once in order | EF Core migrations, or DbUp / Flyway for .NET | `db/migration/` |
| JPA entity (`@Entity`, `@Column`) | EF Core entity with data annotations | `Customer`, `Account` |
| Hibernate (the JPA implementation) | EF Core | everywhere in `infra` |
| Spring Data `JpaRepository` interface, implementation generated at startup | A generic repository over `DbSet<T>` (but you write no class at all) | `CustomerJpaRepository` |
| Derived query `findByEmail`, `findAllByOrderByCreatedAtAscIdAsc` | LINQ `Where(...)`, `OrderBy(...)` | `*JpaRepository` |
| `@Transactional` on a service method | A unit of work: `SaveChanges` in one transaction, or `TransactionScope` | services |
| Persistence context and dirty checking: change a loaded entity, no explicit update call needed | EF Core `ChangeTracker` | `AccountService.changeStatus` |
| `@Version` optimistic locking | `[ConcurrencyCheck]` / `rowversion` | `Account.version` |
| `@Enumerated(EnumType.STRING)` | EF Core `HasConversion<string>()` | entities |
| `AttributeConverter` | EF Core value converter | `CurrencyConverter` |
| `JdbcTemplate` | `SqlCommand` / Dapper | `SequenceAccountNumberGenerator` |
| `@DataJpaTest` | An integration test with a real `DbContext` | `Jpa*RepositoryTest` |
| `TestEntityManager` `flush()` and `clear()` | `SaveChanges()` and `ChangeTracker.Clear()` | repository tests |
| Testcontainers | Testcontainers for .NET | `PostgresTestSupport` |

## Five ideas to understand

**1. The database is the real guarantee.** Java checks give friendly messages (`email already exists`), but two requests can pass the check at the same instant. The `UNIQUE` constraint is what actually prevents duplicates, and `ApiExceptionHandler` turns a violation into a 409. Look at `JpaAccountRepositoryTest`: it bypasses Java with raw SQL to prove the CHECK constraints reject bad values anyway.

**2. The persistence context.** Inside a transaction, Hibernate keeps every entity it loaded in a first-level cache and watches it for changes. That is why `changeStatus` needs no update call: Hibernate notices the status changed and writes it when the transaction commits. It is also why the repository tests call `flush()` (run the SQL now) and `clear()` (empty the cache) before reading: without them, `findById` would hand back the very object we just saved and prove nothing about the database.

**3. Why `Long version`, not `long`.** For an entity whose id we assign ourselves (a `UUID` created in Java), Spring Data needs a way to know "new, INSERT" versus "existing, UPDATE". A `null` version means never saved. A primitive `long` can never be null, so Spring Data would fall back to checking the id, treat every save as an update, and run an extra SELECT first.

**4. A failed statement poisons a PostgreSQL transaction.** After any error, every later statement in that transaction fails with "current transaction is aborted" until it rolls back. That is why `JpaAccountRepositoryTest` has two separate tests for two CHECK constraints instead of one test with two failures.

**5. Tests share one database, so they must not assume it is empty.** `PostgresTestSupport` starts one container for the whole test run. `@DataJpaTest` tests roll back after each test, but `ApiEndToEndTest` commits real rows. So the repository tests assert on their own rows (`containsSubsequence`, `contains`) rather than on the whole table.

## Run it yourself

```
docker compose up -d
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Use the curl commands from the Week 1 guide to create a customer and an account. Then:

```
# Look inside the database
docker exec -it harbour-postgres psql -U harbour -d harbour -c '\d accounts'
docker exec -it harbour-postgres psql -U harbour -d harbour -c 'select * from customers'
docker exec -it harbour-postgres psql -U harbour -d harbour -c 'select version, description, success from flyway_schema_history'
```

Stop the app (Ctrl+C), start it again, and list the accounts: **your data survived the restart**, which was impossible in Week 1. The local profile also prints every SQL statement Hibernate runs, so watch what a single request does.

## Experiments (10 minutes each)

1. **Break the mapping.** Change `@Column(name = "full_name", ...)` in `Customer` to `full_nme` and start the app. Hibernate's schema validation refuses to start and names the missing column. Revert it.
2. **Edit an applied migration.** After running the app once with the local profile, add a space or a comment to `V1__customers_and_accounts.sql` and restart. Flyway reports a checksum mismatch. That is why applied migrations are never edited, only followed by `V2__...`. Revert it.
3. **Prove why `clear()` matters.** Remove `em.clear()` from `savesAndReadsBackEveryField` in `JpaCustomerRepositoryTest`. The test still passes, but now for the wrong reason: it reads from the cache and never touches the database.
4. **Watch the lifecycle in SQL.** With the app running, freeze an account and read the console. You should see a SELECT followed by an UPDATE that includes `where id=? and version=?`.
5. **Send duplicate requests quickly.** Send the same new-customer request twice in a row from the terminal. The second returns 409 from the Java check. Then ask yourself which of the two protections caught it and which one would catch two truly simultaneous requests.

## What is deliberately missing

- **Pagination.** The plan lists it for Week 2, but the customer and account lists are small and fixed in order. It moves to Week 3, where the transaction history endpoint is the first list that can grow large.
- **Payees table.** There is no payees module yet, so the table waits for the feature that needs it.
- **Relationships between entities.** `Account` stores a `customerId` rather than a `@ManyToOne Customer`, keeping the modules decoupled. Lazy loading and the N+1 query problem become relevant when we add mapped relationships.
- **Balances and the zero-balance rule for closing accounts** (Week 3), **concurrent-update tests for `@Version`** (Week 4), and **authentication** (Week 5).
