-- Reactions only reached the other participant when they reopened the chat: message_reactions was
-- not in the Realtime publication. The app now subscribes to INSERT / DELETE on it while a chat is
-- open (ReactionRepositoryImpl.observeReactions).
-- INSERT events go through the SELECT RLS policy (participants only). DELETE events are not
-- RLS-filtered, but with the default replica identity they only carry the primary key
-- (message_id, user_id, emoji), which is all the client needs to remove the row.
ALTER PUBLICATION supabase_realtime ADD TABLE public.message_reactions;
