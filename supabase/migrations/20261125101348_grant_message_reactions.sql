-- Reactions never reached the server: message_reactions has RLS policies for INSERT / DELETE /
-- SELECT, but the `authenticated` role was never granted the table privileges themselves (the
-- table predates 20260625070401_minimal_grants.sql and was left out of it). Every insert failed
-- with "permission denied" and ReactionRepositoryImpl swallowed it (runCatching), so a reaction
-- only existed in the reacting device's Room. Found by the Maestro realtime_messaging scenario.
-- The existing policies still restrict rows to the caller's own reactions / their conversations.
GRANT SELECT, INSERT, DELETE ON public.message_reactions TO authenticated;
