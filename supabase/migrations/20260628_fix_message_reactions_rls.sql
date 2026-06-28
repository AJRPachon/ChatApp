-- Fix SELECT policy on message_reactions.
-- The original policy references conversation_members which does not exist;
-- the correct table is conversation_participants.
-- Use the SECURITY DEFINER helper is_conversation_participant() already created
-- in 20260628_fix_participants_rls_recursion.sql.

DROP POLICY IF EXISTS "users can view reactions in their conversations" ON message_reactions;

CREATE POLICY "users can view reactions in their conversations"
    ON message_reactions FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM messages m
            WHERE m.id = message_reactions.message_id
              AND public.is_conversation_participant(m.conversation_id, (SELECT auth.uid()))
        )
    );
