# 0004. Single view held in one component, routing deferred

## Status
Accepted

## Context
The initial frontend scope covers login, registration, and a character dashboard. Two structural approaches were considered: introduce a routing library (React Router) now, giving each screen its own route/URL, or hold all screens as conditionally-rendered sections within a single component, switched by a local piece of state.

At this stage there are only a handful of screens, all reachable through a single linear flow (log in or register, then land on the dashboard) with no independent deep-linking need (e.g. no requirement to bookmark or share a URL to a specific screen yet). Introducing a router before there's a real navigation structure to justify it would add a dependency and a new concept without solving a problem that exists yet.

## Decision
`App` holds a single `view` state variable (`'login' | 'register' | 'dashboard'`), and each screen is a conditionally-rendered block within that one component. No routing library is used yet.

## Consequences
Adding or switching screens today means adding another branch to this same conditional, not defining a new route. This is deliberately provisional: additional views (inventory, chat, market, etc.) are an explicitly intended future direction, and introducing React Router at that point — once there's a real set of distinct, navigable screens — is expected to be an additive change (wrapping the existing views in routes) rather than a rearchitecture of what exists today. This decision covers view/navigation structure only, not the choice of React and Vite themselves (`0001`, `0002`).
