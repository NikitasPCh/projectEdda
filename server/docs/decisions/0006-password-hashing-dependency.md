# 0006. Password hashing via spring-security-crypto only

## Status
Accepted

## Context
Player passwords were initially stored as plaintext as a placeholder, with the understanding that real hashing would be added before the feature was considered complete. The natural dependency for this in the Spring ecosystem is `spring-boot-starter-security`, but that artifact auto-configures Spring Security's full filter chain, which would lock every existing endpoint behind a login form/basic auth by default — behavior not wanted yet, since authentication hasn't been designed or built.

## Decision
Depend only on `spring-security-crypto`, a smaller standalone artifact that provides password hashing utilities (`BCryptPasswordEncoder`) without pulling in Spring Security's autoconfiguration or endpoint security. A `PasswordEncoder` bean is exposed via a `SecurityConfig` configuration class, coded against the `PasswordEncoder` interface rather than the concrete `BCryptPasswordEncoder` class, and injected into `PlayerService` to hash passwords on player creation.

## Consequences
Passwords are hashed with an industry-standard, adaptive algorithm without introducing unwanted authentication behavior on existing endpoints. Swapping the hashing algorithm later only requires changing the single bean definition, since all callers depend on the `PasswordEncoder` interface. The trade-off is that this only covers hashing — actual authentication (verifying credentials, sessions or tokens) remains a separate, not-yet-built feature, and will likely need `spring-boot-starter-security` (or an equivalent) at that point.
