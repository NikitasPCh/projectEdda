# 0002. Vite chosen over Next.js or Remix

## Status
Accepted

## Context
Having settled on React (`0001`), the specific tooling to build and serve it still needed a choice. Next.js and Remix were both considered as alternatives to a plain Vite scaffold. Both are full-stack React frameworks: they expect to own server-side rendering, their own routing and data-loading conventions, and typically an API-routes layer of their own.

This app already has a separate, independent backend — a Spring Boot REST API (plus a WebSocket endpoint for live updates) that owns all business logic and persistence. A framework that wants to own the backend as well would be redundant at best, and a source of architectural conflict at worst (two competing ideas of where routing, data-loading, and API concerns should live).

## Decision
The frontend uses Vite (`npm create vite@latest`, React + JavaScript + ESLint template) as a pure client-side dev server and bundler, with no built-in backend, server-rendering, or API-route opinions of its own.

## Consequences
The frontend's only job is to be a client of the existing Spring Boot API — it fetches, renders, and posts, with no server-side rendering or backend logic living in the frontend project at all. This keeps a single, unambiguous owner for backend concerns (the Spring app) rather than splitting that responsibility across two frameworks with overlapping opinions. The tradeoff is that anything Next.js/Remix would have provided out of the box — e.g. server-side rendering for SEO or first-paint performance — is simply not available and was not a goal for this app (an authenticated, session-based game dashboard, not a public content site).
