# AI-assisted development log

## Principles
- AI proposes; I decide. I can explain every line I merge.
- Domain logic and tests are written by me first; AI reviews and challenges.
- Every AI-generated change is run, tested, and read before merge.

## Log
| Date | Task | Tool | What AI did | What I verified | Issue found / lesson |
|------|------|------|-------------|-----------------|----------------------|
| 2026-11-25 | Week 0 scaffold | Claude | Generated pom.xml, app class, config, CI, Compose | Build could not be run in the AI sandbox (no Maven Central access); it was verified locally | no issues
| 2026-11-26 | Week 1 scaffold | Claude | Generated initial code | build passed first time with 40 tests, and that the controller and error-handler code was only verified on my mac pro | no isses found/ studied the java code and found interesting difference between C# .NET and Java Spring Boot Maven architecture |
## Prompts that worked
- (add as you go)

## Where AI was wrong
- (add as you go)
