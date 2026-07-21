# 0007. Global exception handler for API errors

## Status
Accepted

## Context
Certain error conditions, such as violating the `username` unique constraint on player creation, were surfacing as raw `500 Internal Server Error` responses with a full stack trace in the JSON body, since nothing translated the underlying `DataIntegrityViolationException` into a meaningful HTTP status. Handling this with a try/catch inside each service method would not scale, since the same category of error will recur across future entities.

## Decision
Add a `@RestControllerAdvice` class (`GlobalExceptionHandler`) with an `@ExceptionHandler(DataIntegrityViolationException.class)` method that returns a `409 Conflict` with a small `ErrorResponse` DTO (`{"message": "..."}`) instead of the default error page. Being a controller advice, it applies automatically to every `@RestController` in the app without requiring changes to individual controllers. Exceptions that already carry HTTP-status meaning, such as `ResponseStatusException` (used for the not-found case), don't need a handler here, since Spring already understands them natively.

## Consequences
Constraint violations now return a clean, predictable error shape instead of a raw stack trace, and the same handler automatically covers future entities with unique constraints without additional code. The current message is intentionally generic ("a record with these values already exists") rather than field-specific, since `username` is currently the only unique constraint in the system — more precise, per-field messaging was deferred until it's actually needed.
