# Messaging Model Migration

This note tracks the cutover from the current support-only tables to the shared messaging model.

## Current compatibility

`messaging.support_threads` and `messaging.support_messages` remain the source of truth for the existing support chat API. The new shared tables are added alongside them so the current support flow keeps working while direct user messaging is built.

## Shared model

- `messaging.conversations` stores one row per support or direct conversation.
- `messaging.conversation_participants` stores explicit user participants.
- `messaging.messages` stores message text plus author display-name and role snapshots.

## Planned cutover

1. Add service methods that write support conversations and messages to both the legacy support tables and the shared tables.
2. Backfill existing support threads into `messaging.conversations`, `messaging.conversation_participants`, and `messaging.messages`.
3. Switch support read APIs to the shared tables.
4. Keep legacy tables read-only for one release.
5. Remove legacy support tables after export and audit flows read from the shared model.

Direct conversations should use the shared tables from the start.
