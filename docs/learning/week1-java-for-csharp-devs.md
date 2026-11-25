# Week 1 study guide: Java and Spring for a C# developer

Read the code in this order. Each step names the Java idea to notice and the C# idea it maps to.

## Suggested reading order

1. `accounts/domain/AccountStatus.java`: enum with a method and a switch expression.
2. `accounts/domain/Account.java` and `customers/domain/Customer.java`: plain domain classes, static factory methods, no framework annotations.
3. `shared/error/*`: a sealed exception hierarchy and the handler that switches over it.
4. `customers/api/*` and `accounts/application/AccountService.java`: how one module talks to another.
5. `*/web/*`: records as request and response bodies, controllers, validation.
6. The tests, in the same order as the code they cover.

## Concept map

| Java / Spring (this repo) | C# / .NET counterpart | Where to look |
|---|---|---|
| `record RegisterCustomerRequest(...)` | `record` types; Java records generate accessors named `fullName()`, not `FullName` | `customers/web/` |
| `sealed class ... permits ...` | No direct built-in equivalent; closest is an abstract base class with a private constructor and nested subclasses | `shared/error/ApiException.java` |
| Pattern-matching `switch` over a sealed type, no `default` | `switch` expression with type patterns, but Java's compiler checks exhaustiveness over the sealed hierarchy | `ApiExceptionHandler.handleApiException` |
| `switch` expression over an enum | `switch` expression; Java requires every constant to be covered | `AccountStatus.canTransitionTo` |
| `Optional<T>` and `.orElseThrow(...)` | Nullable return values / `?? throw` | repositories and services |
| `stream().filter(...).map(...).toList()` | LINQ `Where`, `Select`, `ToList` | `AccountService.open`, repositories |
| `"HB-%010d".formatted(n)` | `string.Format` / interpolation | `AccountNumberGenerator` |
| Text blocks `"""..."""` | Raw string literals | controller tests |
| `final` class | `sealed` class | domain classes |
| `@Service`, `@Repository`, `@Component` | Services registered in `IServiceCollection` (Spring finds them by scanning) | application and infra classes |
| Constructor injection (one constructor, no annotation) | Constructor injection in ASP.NET Core | `CustomerService` |
| `@Configuration` + `@Bean` | `services.AddSingleton(...)` in `Program.cs` | `shared/config/ClockConfig.java` |
| Spring beans are singletons by default | Closest .NET default is the singleton lifetime, but a different default from `AddScoped` | whole app |
| `@RestController`, `@RequestMapping`, `@GetMapping` | `[ApiController]`, `[Route]`, `[HttpGet]` | `*Controller.java` |
| `@Valid` plus `@NotBlank`, `@Email`, `@Size` | DataAnnotations (`[Required]`, `[EmailAddress]`, `[StringLength]`) | `*Request.java` |
| `ProblemDetail` and `@RestControllerAdvice` | `ProblemDetails` and exception-handling middleware | `ApiExceptionHandler` |
| Injecting `java.time.Clock` | `TimeProvider` | services, tests |
| JUnit 5 `@Test`, `@ParameterizedTest` | xUnit `[Fact]`, `[Theory]` | all tests |
| AssertJ `assertThat(...)` | FluentAssertions `.Should()` | all tests |
| `@WebMvcTest` + `MockMvc` + `@MockitoBean` | A lighter cousin of `WebApplicationFactory` with a mocked service | `*ControllerTest.java` |

## Two design ideas worth understanding

**Modules talk through `api` packages.** `AccountService` depends on `CustomerDirectory` (an interface in `customers.api`), never on `Customer` or the customers repository. That is why `AccountServiceTest` can fake the directory with a three-line lambda. Week 9 adds an ArchUnit test that fails the build if someone breaks this rule.

**Domain classes know nothing about Spring.** `Customer` and `Account` have no annotations, so their rules are tested with plain `new` and a fixed `Clock`. Week 2 will add JPA mapping, and the tests should barely change.

## Run it yourself

Start PostgreSQL (the app still needs a datasource at startup, even though Week 1 stores data in memory), then run the app:

```
docker compose up -d
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

In another terminal:

```
curl -s -X POST localhost:8080/api/v1/customers \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Ada Lovelace","email":"ada@example.com"}'

# copy the "id" from the response, then:
curl -s -X POST localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"PASTE-ID-HERE","type":"CHEQUING","currency":"CAD"}'

curl -s localhost:8080/api/v1/accounts
```

Then try to break it: repeat the same email (expect 409), send an empty name (expect 400 with a field list), open a fourth account (expect 422), freeze and then close an account and try to reopen it (expect 422).

## Small experiments (10 minutes each)

1. In `ApiException`, add a fourth subclass to `permits` without handling it in the `switch`. Read the compiler error.
2. In `AccountStatus.canTransitionTo`, delete the `CLOSED` case. Read the compiler error.
3. In `AccountStatusTest`, change one row of the table to the wrong answer and read how the failure is reported per row.
4. In `AccountService`, change `MAX_OPEN_ACCOUNTS_PER_CUSTOMER` to 2 and see exactly which tests fail, and why.

## What is deliberately missing

Balances and the "close only at zero balance" rule (Week 3), persistence and the database-level unique email constraint (Week 2), and authentication and ownership checks (Week 5).
