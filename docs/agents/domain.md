# Domain Docs

Engineering skills should read the repository's domain documentation before exploring relevant code.

## Before exploring

- Read `GLOSSARY.md` at the repository root if it exists.
- Read ADRs under `docs/adr/` that touch the area being changed.

If these files do not exist, proceed silently. The domain-modeling skill creates them lazily when terms or decisions are resolved.

## File structure

This is a single-context repository:

```text
/
├── GLOSSARY.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

## Use the glossary vocabulary

When output names a domain concept, use the term defined in `GLOSSARY.md`. If a needed concept is not defined, note it for the domain-modeling skill.

## Flag ADR conflicts

If output contradicts an existing ADR, surface the conflict explicitly rather than silently overriding it.
