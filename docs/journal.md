# Weekly journal

One short retrospective per week: what I learned, what was hard, what's next.

## Week 0
- Learned:
1. Structural & Physical Differences

In Java, you use a single Maven pom.xml (or multi-module Maven) and separate layers primarily using packages (com.bank.accounts.domain). In C#, you use a Solution (.sln) that groups multiple independent Projects (.csproj), which compile into separate .dll files.

Your Java Setup (Package-based separation)

Equivalent C# .NET Setup (Project/Assembly-based separation)

In .NET, each layer or module is typically its own .csproj project inside a src/ folder so the compiler strictly enforces dependency rules (e.g., Domain cannot reference Infrastructure).

2. Layer-by-Layer Translation


Java Spring Boot Layer	C# .NET Equivalent	Key Differences in .NET
pom.xml (Maven)	.sln & .csproj (NuGet)	Dependencies are managed per .csproj via NuGet packages, or centrally using Directory.Packages.props.
domain	*.Domain	Uses C# records for immutable Value Objects and sealed classes for Entities. Often uses MediatR to dispatch Domain Events.
application	*.Application	Uses MediatR for CQRS (IRequest, IRequestHandler) and FluentValidation instead of JSR-380 (@Valid).
infra	*.Infrastructure	Uses Entity Framework Core (EF Core) instead of Spring Data JPA/Hibernate, or Dapper for high-performance banking queries.
web	*.WebApi / Presentation	Instead of separate web folders inside each module, .NET usually has one host Web API project that routes to all modules via ASP.NET Core Controllers or Minimal APIs.
api (in Customers)	*.Contracts / *.PublicApi	Because C# uses separate .csproj assemblies, Accounts.Application cannot see Customers.Domain unless explicitly referenced. A Customers.Contracts project holds public DTOs and integration events shared across modules.


3. Key Architectural Differences in .NET Banking Apps

• Strict Compiler Enforcement (internal keyword): In Java, unless you use Java 9+ modules (module-info.java) or ArchUnit tests, Accounts can accidentally import classes from Customers.infra. In C#, classes in Customers.Infrastructure can be marked internal, making it impossible at compile time for Accounts to access them.
• Single Entry Point (Composition Root): In Spring Boot, @SpringBootApplication scans all packages automatically. In .NET, the single BankingApp.WebApi project acts as the Composition Root, where each module registers its own services via extension methods (e.g., builder.Services.AddAccountsModule();).
• Vertical Slice Architecture Trend: Modern .NET banking applications increasingly group code by feature rather than technical folders (Domain, Application, Infra). For example: Features/Accounts/OpenAccount/ contains the endpoint, validation, command, and database query in one file or folder.
If you'd like to explore this further, let me know if you want to see:
• Code examples comparing a Spring Boot @Service / @RestController to a .NET CQRS handler and Minimal API
• How EF Core handles banking transactions and concurrency compared to Spring Data JPA
• Whether you are migrating this Java app to .NET or designing a new system from scratch

 
- Next: Week 1, customers and accounts modules.              