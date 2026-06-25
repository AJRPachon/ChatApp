-- E2EE support: add is_encrypted flag to messages and public_key to profiles
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS is_encrypted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS public_key TEXT;
