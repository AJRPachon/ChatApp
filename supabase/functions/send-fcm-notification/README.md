# send-fcm-notification

Sends a push notification (FCM v1) to the other participants of a conversation when a message is
inserted. It is called by a **Database Webhook** (Dashboard, Database, Webhooks, `on_new_message`,
INSERT on `messages`), not by the app.

## Authentication

The function refuses every request that does not carry the shared secret in the
`x-webhook-secret` header. The platform's own JWT check is not enough: the anon key that ships
inside the app passes it.

To set it up (once, and again if the secret is rotated):

1. Generate a secret, for example `openssl rand -hex 32`.
2. `supabase secrets set FCM_WEBHOOK_SECRET=<secret>`
3. In the `on_new_message` webhook, add the HTTP header `x-webhook-secret: <secret>`.
4. `supabase functions deploy send-fcm-notification`

With `FCM_WEBHOOK_SECRET` unset the function answers 500 to everything (it fails closed), so no
notification is sent until steps 2 to 4 are done.

## Other secrets

`SUPABASE_URL`, `SERVICE_ROLE_JWT`, `FIREBASE_PROJECT_ID`, `SA_CLIENT_EMAIL`, `SA_PRIVATE_KEY`.

## Logging

The function does not log the payload: it carries the message content.
