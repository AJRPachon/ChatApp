-- Nullable phone column on profiles, used to resolve a shared contact card's
-- phone number to a registered app user (invitation relationship lookup).
-- Not yet populated: no signup/profile flow captures phone today.
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS phone text;
