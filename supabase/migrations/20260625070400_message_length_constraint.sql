-- Enforce maximum message content length at DB level
ALTER TABLE public.messages
    ADD CONSTRAINT messages_content_length_check
    CHECK (char_length(content) <= 4000);
