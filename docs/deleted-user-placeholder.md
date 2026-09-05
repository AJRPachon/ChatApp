# "Deleted user" placeholder account

`delete-account` (see `supabase/functions/delete-account/index.ts`) reassigns several
`NOT NULL` / hard-FK (`NO ACTION`) columns — `messages.sender_id`, `conversations.created_by`,
`calls.caller_id`, `calls.callee_id`, `call_signals.sender_id` — to a permanent placeholder
account instead of the real user being deleted, because none of those columns can be set to
`NULL` (they're `NOT NULL`, confirmed via the Supabase SQL editor against `pg_constraint`) and
none of them cascade-delete from `auth.users`/`profiles`. Without this, `auth.admin.deleteUser()`
fails with a Postgres FK-violation for any account that ever sent a message, created a
conversation, or made a call — i.e. almost every real user.

## Why this isn't a migration

Creating the row requires the Supabase Auth **Admin API** (`POST /auth/v1/admin/users`), not a
plain SQL `INSERT INTO auth.users`, so that GoTrue's own invariants (identities, encrypted
password, etc.) are set up correctly. Auth-schema rows are deliberately not managed via
`supabase/migrations/` in this project. This is a **one-time, per-project manual step** — run it
again (with the same fixed id) if this project is ever cloned into a new Supabase project (e.g.
a from-scratch staging environment).

## What was created (production project `oouhhjszqlbnhcnugvvs`, 2026-09-05)

- `auth.users` row via the Admin API:
  - `id`: `00000000-0000-0000-0000-000000000001` (fixed — referenced by that literal string in
    `delete-account/index.ts` as `DELETED_USER_PLACEHOLDER_ID`)
  - `email`: `deleted-user@chatapp.internal.invalid` (`.invalid` TLD — RFC 2606, guaranteed to
    never resolve/receive mail)
  - `email_confirm: true`, random password (never recorded/needed — see next point)
  - `ban_duration`: set far enough in the future to be permanent (this account must never be
    able to sign in — it's a foreign-key anchor, not a real login)
  - `user_metadata.full_name`: `"Usuario eliminado"` (picked up by the `handle_new_user()`
    trigger, which auto-creates the matching `profiles` row from it)
- The `on_auth_user_created` trigger auto-created the `profiles` row from the metadata above;
  `show_online_status` was then set to `false` on it manually (everything else — `username`,
  `avatar_url`, `public_key`, `phone` — stays `NULL`, matching a normal empty profile).

## Reproducing this on a different Supabase project

```bash
curl -X POST "https://<project-ref>.supabase.co/auth/v1/admin/users" \
  -H "apikey: <service_role key>" \
  -H "Authorization: Bearer <service_role key>" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "00000000-0000-0000-0000-000000000001",
    "email": "deleted-user@chatapp.internal.invalid",
    "email_confirm": true,
    "password": "<any random string — never used again>",
    "ban_duration": "876000h",
    "user_metadata": { "full_name": "Usuario eliminado" }
  }'
```

Then, once the trigger has created the `profiles` row:

```sql
update profiles set show_online_status = false
where id = '00000000-0000-0000-0000-000000000001';
```

## If this ID or account is ever lost/renamed

`delete-account` hardcodes `DELETED_USER_PLACEHOLDER_ID`. If the placeholder account is ever
deleted or the id changes, every past reassignment (historical messages/calls/conversations
already pointing at it) stays valid — only *future* `delete-account` calls would break, the same
FK-violation way this whole placeholder was built to avoid. Treat this account as permanent
infrastructure, not a normal user row: never delete it, never lift its ban.
