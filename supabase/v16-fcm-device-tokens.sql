-- Run once in Supabase SQL Editor before deploying the send function.
create table if not exists public.razem_device_tokens (
  token text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  household_id uuid not null references public.razem_households(id) on delete cascade,
  updated_at timestamptz not null default now()
);
alter table public.razem_device_tokens enable row level security;
drop policy if exists razem_device_tokens_own on public.razem_device_tokens;
create policy razem_device_tokens_own on public.razem_device_tokens for all to authenticated
using (user_id = auth.uid()) with check (user_id = auth.uid() and public.is_household_member(household_id));

grant select, insert, update, delete on public.razem_device_tokens to authenticated;
grant select, delete on public.razem_device_tokens to service_role;
