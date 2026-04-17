-- ============================================================
-- ChatApp — Supabase Schema
-- Tablas marcadas con [REALTIME] tienen replicación activa.
-- Ejecutar en orden. Requiere extensión uuid-ossp y pgcrypto.
-- ============================================================

-- Habilitar extensiones necesarias
create extension if not exists "uuid-ossp";
create extension if not exists "pgcrypto";

-- ============================================================
-- 1. PROFILES [REALTIME]
--    Un perfil por usuario auth.users.
--    El username es ÚNICO en toda la plataforma.
-- ============================================================
create table public.profiles (
    id          uuid        references auth.users on delete cascade primary key,
    username    text        unique not null
                            constraint username_format
                            check (username ~ '^[a-z0-9_]{3,20}$'),
    display_name text       not null,
    avatar_url  text,
    created_at  timestamptz default now() not null,
    updated_at  timestamptz default now() not null
);

comment on table public.profiles is 'Perfil público de cada usuario registrado.';

-- Trigger: mantener updated_at al día
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create trigger profiles_updated_at
    before update on public.profiles
    for each row execute procedure public.set_updated_at();

-- Trigger: crear perfil automáticamente tras signup
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer as $$
begin
    insert into public.profiles (id, display_name)
    values (
        new.id,
        coalesce(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1))
    );
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.handle_new_user();

-- RLS
alter table public.profiles enable row level security;

create policy "Perfiles visibles para todos los autenticados"
    on public.profiles for select
    using (auth.role() = 'authenticated');

create policy "Usuario puede actualizar su propio perfil"
    on public.profiles for update
    using (auth.uid() = id)
    with check (auth.uid() = id);

-- Activar Realtime para profiles
alter publication supabase_realtime add table public.profiles;

-- ============================================================
-- 2. CONVERSATIONS [REALTIME]
-- ============================================================
create table public.conversations (
    id          uuid        default uuid_generate_v4() primary key,
    name        text,                   -- null en conversaciones directas
    is_group    boolean     default false not null,
    created_by  uuid        references public.profiles(id) not null,
    created_at  timestamptz default now() not null,
    updated_at  timestamptz default now() not null
);

create trigger conversations_updated_at
    before update on public.conversations
    for each row execute procedure public.set_updated_at();

alter table public.conversations enable row level security;

create policy "Usuario ve sus conversaciones"
    on public.conversations for select
    using (
        exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = id
            and cp.user_id = auth.uid()
        )
    );

create policy "Usuario crea conversaciones"
    on public.conversations for insert
    with check (auth.uid() = created_by);

alter publication supabase_realtime add table public.conversations;

-- ============================================================
-- 3. CONVERSATION_PARTICIPANTS
-- ============================================================
create table public.conversation_participants (
    conversation_id uuid references public.conversations(id) on delete cascade,
    user_id         uuid references public.profiles(id) on delete cascade,
    joined_at       timestamptz default now() not null,
    primary key (conversation_id, user_id)
);

alter table public.conversation_participants enable row level security;

create policy "Participante ve su propia membresía"
    on public.conversation_participants for select
    using (auth.uid() = user_id);

create policy "Participante puede unirse a conversación"
    on public.conversation_participants for insert
    with check (auth.uid() = user_id);

-- ============================================================
-- 4. MESSAGES [REALTIME]
-- ============================================================
create table public.messages (
    id              uuid        default uuid_generate_v4() primary key,
    conversation_id uuid        references public.conversations(id) on delete cascade not null,
    sender_id       uuid        references public.profiles(id) not null,
    content         text        not null check (char_length(content) > 0),
    is_read         boolean     default false not null,
    created_at      timestamptz default now() not null
);

create index messages_conversation_idx on public.messages(conversation_id, created_at desc);

alter table public.messages enable row level security;

create policy "Participante ve mensajes de su conversación"
    on public.messages for select
    using (
        exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = messages.conversation_id
            and cp.user_id = auth.uid()
        )
    );

create policy "Participante envía mensajes"
    on public.messages for insert
    with check (
        auth.uid() = sender_id
        and exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = messages.conversation_id
            and cp.user_id = auth.uid()
        )
    );

create policy "Participante marca mensajes como leídos"
    on public.messages for update
    using (
        exists (
            select 1 from public.conversation_participants cp
            where cp.conversation_id = messages.conversation_id
            and cp.user_id = auth.uid()
        )
    )
    with check (sender_id != auth.uid()); -- solo marca leídos los ajenos

alter publication supabase_realtime add table public.messages;

-- ============================================================
-- 5. INVITATIONS [REALTIME]
--    Sistema de notificaciones internas (solicitudes de amistad/chat)
-- ============================================================
create table public.invitations (
    id          uuid        default uuid_generate_v4() primary key,
    sender_id   uuid        references public.profiles(id) not null,
    receiver_id uuid        references public.profiles(id) not null,
    status      text        default 'pending'
                            check (status in ('pending', 'accepted', 'rejected'))
                            not null,
    created_at  timestamptz default now() not null,
    updated_at  timestamptz default now() not null,
    constraint no_self_invitation check (sender_id != receiver_id),
    unique (sender_id, receiver_id)
);

create trigger invitations_updated_at
    before update on public.invitations
    for each row execute procedure public.set_updated_at();

create index invitations_receiver_idx on public.invitations(receiver_id, status);

alter table public.invitations enable row level security;

create policy "Remitente ve sus invitaciones enviadas"
    on public.invitations for select
    using (auth.uid() = sender_id);

create policy "Receptor ve sus invitaciones recibidas"
    on public.invitations for select
    using (auth.uid() = receiver_id);

create policy "Usuario envía invitación como remitente"
    on public.invitations for insert
    with check (auth.uid() = sender_id);

create policy "Receptor responde invitaciones"
    on public.invitations for update
    using (auth.uid() = receiver_id)
    with check (status in ('accepted', 'rejected'));

alter publication supabase_realtime add table public.invitations;

-- ============================================================
-- 6. FUNCIÓN RPC: get_or_create_direct_conversation
--    Crea o reutiliza una conversación directa entre dos usuarios.
-- ============================================================
create or replace function public.get_or_create_direct_conversation(
    user_a uuid,
    user_b uuid
)
returns uuid language plpgsql security definer as $$
declare
    conv_id uuid;
begin
    -- Buscar conversación directa existente entre ambos
    select cp1.conversation_id into conv_id
    from public.conversation_participants cp1
    join public.conversation_participants cp2
        on cp1.conversation_id = cp2.conversation_id
    join public.conversations c
        on c.id = cp1.conversation_id
    where cp1.user_id = user_a
      and cp2.user_id = user_b
      and c.is_group = false
    limit 1;

    if conv_id is null then
        -- Crear nueva conversación directa
        insert into public.conversations (is_group, created_by)
        values (false, user_a)
        returning id into conv_id;

        -- Añadir ambos participantes
        insert into public.conversation_participants (conversation_id, user_id)
        values (conv_id, user_a), (conv_id, user_b);
    end if;

    return conv_id;
end;
$$;

-- ============================================================
-- RESUMEN REALTIME
-- Tablas con supabase_realtime activado:
--   ✅ public.profiles
--   ✅ public.conversations
--   ✅ public.messages
--   ✅ public.invitations
-- ============================================================
