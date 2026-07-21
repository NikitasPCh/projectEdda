# 0005. DTO naming convention

## Status
Accepted

## Context
As more endpoints get added, request and response payload shapes need a consistent, predictable naming convention so the pattern scales without being re-decided for every new entity.

## Decision
Request DTOs (data sent to the API) are named after the specific action they represent, e.g. `CreatePlayerRequest`, since input shapes tend to differ per action — creating a player needs a password, updating one might not. Response DTOs (data returned by the API) are named after the entity/resource they represent, e.g. `PlayerResponse`, since the same response shape is often reused across multiple endpoints for that resource (create, get-by-id, list). Where an entity needs multiple response shapes for different contexts (e.g., a public view versus a detailed self-view), a qualifier is inserted before `Response` rather than introducing a separate naming category, e.g. `PlayerSummaryResponse`. DTOs are implemented as Java records rather than Lombok classes, since they're immutable data carriers with no JPA constraints, and Jackson supports records natively.

## Consequences
The naming convention scales predictably as new entities and endpoints are added, without needing a fresh naming decision each time. It also creates a light readability rule: `*Request` classes are always scoped to one action, `*Response` classes are always scoped to one resource/view — a class's name alone hints at how broadly it's reused.
