# ADR-001: Modular monolith with enforced module boundaries

- Status: Accepted
- Date: 2026-11-25

## Context
The platform has several distinct domains (accounts, ledger, transfers, notifications, fraud). It is a solo learning project that must still show production-minded structure.

## Decision
Build one deployable Spring Boot application, organized into modules by domain. Modules interact only through their public `api` package and domain events. ArchUnit tests (added in Week 9) enforce the boundaries.

## Consequences
- Simple to build, run and test; one database transaction can span the ledger and transfers.
- The module seams are the same seams used to extract services later.
- Requires discipline: boundaries are enforced by tests, not by deployment.
