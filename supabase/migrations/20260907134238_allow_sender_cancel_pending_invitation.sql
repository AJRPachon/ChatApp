-- Allow the sender of a pending invitation to cancel it (pending -> rejected).
--
-- Root cause of a real bug found via manual QA: InvitationRemoteSource.updateStatus
-- (called by InvitationRepositoryImpl.cancelSentInvitation, the "Sent" tab's Cancel
-- action) issued `update invitations set status = 'rejected' where id = ...` as the
-- sender. No existing UPDATE policy covers that transition:
--   - "Receptor responde invitaciones" / "invitations_update": receiver-only.
--   - "Remitente puede reenviar invitación rechazada": sender-only, but only the
--     opposite direction (rejected -> pending, for resending after a decline).
-- Postgrest doesn't error when RLS filters an UPDATE down to zero matched rows — it
-- returns success with an empty result — so the app showed "Invitación cancelada"
-- and reloaded the list while the database silently kept the row at 'pending'.

create policy "Remitente puede cancelar invitación pendiente"
on public.invitations
for update
to authenticated
using (auth.uid() = sender_id and status = 'pending')
with check (auth.uid() = sender_id and status = 'rejected');
