-- Razem: bezpieczny schemat dla dwÄ‚Ĺ‚ch uÄąÄ˝ytkownikÄ‚Ĺ‚w
create extension if not exists pgcrypto;

create table if not exists public.razem_households (
  id uuid primary key default gen_random_uuid(),
  name text not null default 'Dom Myszy',
  invite_code text not null unique default upper(substr(encode(gen_random_bytes(6), 'hex'), 1, 8)),
  default_pawel_percent smallint not null default 60 check (default_pawel_percent between 0 and 100),
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now()
);

create table if not exists public.razem_members (
  household_id uuid not null references public.razem_households(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  nickname text not null check (char_length(nickname) between 1 and 40),
  created_at timestamptz not null default now(),
  primary key (household_id, user_id)
);

create table if not exists public.razem_expenses (
  id uuid primary key default gen_random_uuid(),
  household_id uuid not null references public.razem_households(id) on delete cascade,
  created_by uuid not null references auth.users(id),
  payer_id uuid not null references auth.users(id),
  merchant text not null,
  amount numeric(12,2) not null check (amount > 0),
  pawel_percent smallint not null default 60 check (pawel_percent between 0 and 100),
  receipt_path text,
  source text not null default 'manual' check (source in ('manual','receipt','alior_notification')),
  category text not null default 'Inne' check (category in ('Jedzenie','Spożywcze','Dom','Paliwo','Zdrowie','Transport','Rozrywka','Rachunki','Inne')),
  occurred_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index if not exists razem_members_user_idx on public.razem_members(user_id);
create index if not exists razem_expenses_household_idx on public.razem_expenses(household_id, occurred_at desc);

alter table public.razem_households enable row level security;
alter table public.razem_members enable row level security;
alter table public.razem_expenses enable row level security;

create or replace function public.is_household_member(target uuid)
returns boolean language sql stable security definer set search_path = public
as $$ select exists(select 1 from razem_members where household_id = target and user_id = auth.uid()) $$;

drop policy if exists razem_households_select on public.razem_households;
create policy razem_households_select on public.razem_households for select to authenticated
using (public.is_household_member(id) or created_by = (select auth.uid()));

drop policy if exists razem_members_select on public.razem_members;
create policy razem_members_select on public.razem_members for select to authenticated
using (public.is_household_member(household_id));

drop policy if exists razem_expenses_all on public.razem_expenses;
create policy razem_expenses_all on public.razem_expenses for all to authenticated
using (public.is_household_member(household_id))
with check (public.is_household_member(household_id) and created_by = (select auth.uid()));

create or replace function public.create_razem_household(member_nickname text)
returns table(household_id uuid, invite_code text)
language plpgsql security definer set search_path = public
as $$
declare h public.razem_households;
begin
  if auth.uid() is null then raise exception 'Logowanie wymagane'; end if;
  insert into razem_households(name, created_by) values ('Dom Myszy', auth.uid()) returning * into h;
  insert into razem_members(household_id,user_id,nickname) values(h.id,auth.uid(),member_nickname);
  return query select h.id,h.invite_code;
end $$;

create or replace function public.join_razem_household(code text, member_nickname text)
returns uuid language plpgsql security definer set search_path = public
as $$
declare hid uuid;
begin
  if auth.uid() is null then raise exception 'Logowanie wymagane'; end if;
  select id into hid from razem_households where invite_code = upper(trim(code));
  if hid is null then raise exception 'NieprawidÄąâ€šowy kod zaproszenia'; end if;
  insert into razem_members(household_id,user_id,nickname)
  values(hid,auth.uid(),member_nickname) on conflict do nothing;
  return hid;
end $$;

revoke all on function public.create_razem_household(text) from public;
revoke all on function public.join_razem_household(text,text) from public;
grant execute on function public.create_razem_household(text) to authenticated;
grant execute on function public.join_razem_household(text,text) to authenticated;

insert into storage.buckets(id,name,public)
values('receipts','receipts',false)
on conflict(id) do update set public=false;

drop policy if exists receipts_select on storage.objects;
create policy receipts_select on storage.objects for select to authenticated
using (bucket_id='receipts' and public.is_household_member(((storage.foldername(name))[1])::uuid));

drop policy if exists receipts_insert on storage.objects;
create policy receipts_insert on storage.objects for insert to authenticated
with check (bucket_id='receipts' and public.is_household_member(((storage.foldername(name))[1])::uuid));