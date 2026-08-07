# 0003. Plain JavaScript chosen over TypeScript

## Status
Accepted

## Context
Vite's React template offers both a JavaScript and a TypeScript variant at scaffold time. TypeScript would add compile-time type-checking — catching, for example, a typo'd field name from a backend DTO before runtime rather than after. However, the person building this frontend has a strong Java/Spring background but is entirely new to JavaScript, React, and HTML — none of it, not just React on top of already-known JS.

Adopting TypeScript at the same time as JS, React, and HTML would mean learning a type system layered on top of a language, a UI framework, and markup all at once. TypeScript is also not a one-way door: Vite's TypeScript template can be adopted later, and TS supports incremental, file-by-file migration from JS rather than requiring an all-or-nothing conversion.

## Decision
The frontend is written in plain JavaScript. TypeScript may be introduced later, once JS/React/HTML fundamentals are solid, migrated incrementally rather than as an upfront rewrite.

## Consequences
There is no compile-time type-checking on component props, state, or backend API response shapes for now — a mismatch (e.g. a renamed field in a DTO response) would only surface at runtime, not at build time. ESLint (see the project's lint configuration) catches a different, narrower class of problems and does not substitute for this. This is an accepted, deliberate short-term tradeoff in exchange for not stacking a second new concept on top of an already-new language and framework; it is expected to be revisited once the fundamentals are comfortable, not treated as a permanent architectural choice.
