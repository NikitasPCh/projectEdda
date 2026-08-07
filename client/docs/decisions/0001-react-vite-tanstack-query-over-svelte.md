# 0001. React, Vite, and TanStack Query, chosen over Svelte

## Status
Accepted

## Context
The game needed a UI for the first time, sitting on top of an existing Spring Boot backend that is almost entirely request/response-driven: fetch a character summary, POST a selected action, log in, and (soon) receive live progress pushes over a WebSocket. Two frontend frameworks were seriously considered: React and Svelte.

TanStack Query — a library built around caching, refetching, and synchronizing "server state" (data that actually lives on the backend, as opposed to purely local UI state) — was evaluated alongside the framework choice, since the backend's shape (fetch-cache-refetch, occasional one-shot mutations) fits its model closely. It is a React-ecosystem library; choosing it effectively meant choosing React, since an equivalent-maturity option in the Svelte ecosystem was not the same kind of established, widely-documented fit.

## Decision
The frontend is built with React, scaffolded via Vite, using TanStack Query as the server-state layer (`useQuery` for cached/refetched reads, `useMutation` for one-shot writes).

## Consequences
The backend's request/response and eventual live-tick-push shape maps cleanly onto TanStack Query's caching and refetch model, rather than requiring hand-rolled loading/error/staleness state for every piece of server data. React's much larger ecosystem and documentation base was also a real factor, since the person building this frontend is new to JS/React/HTML entirely and benefits from broader tutorial/community coverage than a smaller framework would offer. This decision does not itself address view/routing structure (see `0004`) or the type system (see `0003`), both of which were separate questions layered on top of this base choice.
