-- Myszy DEV/demo: clean database bootstrap.
-- Run this ONCE in the SQL Editor of the separate myszy-dev project.
-- It creates no real household, user, expense, receipt or device token.

create extension if not exists pgcrypto;

create table public.razem_households (
  id uuid primary key default gen_random_uuid(),
  name text not null default 'Dom Myszy',
  invite_code text not null unique default upper(substr(encode(gen_random_bytes(8), 'hex'), 1, 8)),
  default_pawel_percent smallint not null default 60 check (default_pawel_percent between 0 and 100),
  balance_correction numeric(12,2) not null default 0,
  created_by uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create table public.razem_members (
  household_id uuid not null references public.razem_households(id) on delete cascade,
  user_id uuid not null unique references auth.users(id) on delete cascade,
  nickname text not null check (char_length(trim(nickname)) between 1 and 40),
  created_at timestamptz not null default now(),
  primary key (household_id, user_id)
);

create table public.razem_expenses (
  id uuid primary key default gen_random_uuid(),
  household_id uuid not null references public.razem_households(id) on delete cascade,
  created_by uuid not null references auth.users(id) on delete cascade,
  payer_id uuid not null references auth.users(id) on delete restrict,
  merchant text not null check (char_length(trim(merchant)) between 1 and 160),
  amount numeric(12,2) not null check (amount > 0),
  pawel_percent smallint not null check (pawel_percent between 0 and 100),
  pawel_share numeric(12,2),
  ania_share numeric(12,2),
  receipt_path text,
  import_key text,
  source text not null default 'manual' check (source in ('manual', 'receipt', 'alior_notification', 'settleup_import')),
  category text not null default 'Inne' check (category in ('Jedzenie', 'Spożywcze', 'Zakupy', 'Dom', 'Paliwo', 'Zdrowie', 'Transport', 'Rozrywka', 'Rachunki', 'Inne')),
  comment text not null default '',
  occurred_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  unique (household_id, import_key)
);

create table public.razem_closed_months (
  household_id uuid not null references public.razem_households(id) on delete cascade,
  month_key text not null check (month_key ~ '^[0-9]{4}-[0-9]{2}$'),
  closed_by uuid not null references auth.users(id) on delete restrict,
  closed_at timestamptz not null default now(),
  primary key (household_id, month_key)
);

create table public.razem_device_tokens (
  token text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  household_id uuid not null references public.razem_households(id) on delete cascade,
  updated_at timestamptz not null default now()
);

create table public.razem_activity (
  id uuid primary key default gen_random_uuid(),
  household_id uuid not null references public.razem_households(id) on delete cascade,
  expense_id uuid,
  user_id uuid not null references auth.users(id) on delete cascade,
  action text not null check (action in ('added', 'edited', 'deleted', 'settled', 'receipt_removed')),
  merchant text not null default '',
  amount numeric not null default 0,
  occurred_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index razem_members_household_idx on public.razem_members(household_id);
create index razem_expenses_household_occurred_idx on public.razem_expenses(household_id, occurred_at desc);
create index razem_closed_months_household_idx on public.razem_closed_months(household_id);
create index razem_activity_household_created_idx on public.razem_activity(household_id, created_at desc);

alter table public.razem_households enable row level security;
alter table public.razem_members enable row level security;
alter table public.razem_expenses enable row level security;
alter table public.razem_closed_months enable row level security;
alter table public.razem_device_tokens enable row level security;
alter table public.razem_activity enable row level security;

create function public.is_household_member(hid uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.razem_members
    where household_id = hid
      and user_id = auth.uid()
  );
$$;

create function public.create_razem_household(member_nickname text)
returns table(household_id uuid, invite_code text)
language plpgsql
security definer
set search_path = public
as $$
declare
  h public.razem_households;
begin
  if auth.uid() is null then
    raise exception 'Zaloguj się, aby utworzyć dom';
  end if;

  if exists (select 1 from public.razem_members where user_id = auth.uid()) then
    raise exception 'To konto należy już do domu';
  end if;

  insert into public.razem_households (created_by)
  values (auth.uid())
  returning * into h;

  insert into public.razem_members (household_id, user_id, nickname)
  values (h.id, auth.uid(), trim(member_nickname));

  return query select h.id, h.invite_code;
end;
$$;

create function public.join_razem_household(code text, member_nickname text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  hid uuid;
  member_count integer;
begin
  if auth.uid() is null then
    raise exception 'Zaloguj się, aby dołączyć do domu';
  end if;

  select id into hid
  from public.razem_households
  where invite_code = upper(trim(code));

  if hid is null then
    raise exception 'Nieprawidłowy kod zaproszenia';
  end if;

  if exists (
    select 1 from public.razem_members
    where household_id = hid
      and user_id = auth.uid()
  ) then
    return hid;
  end if;

  if exists (select 1 from public.razem_members where user_id = auth.uid()) then
    raise exception 'To konto należy już do innego domu';
  end if;

  perform 1 from public.razem_households where id = hid for update;

  select count(*) into member_count
  from public.razem_members
  where household_id = hid;

  if member_count >= 2 then
    raise exception 'Ten Dom Myszy ma już dwie osoby';
  end if;

  insert into public.razem_members (household_id, user_id, nickname)
  values (hid, auth.uid(), trim(member_nickname));

  return hid;
end;
$$;

create policy "razem_households_select_members"
on public.razem_households for select
to authenticated
using (public.is_household_member(id));

create policy "razem_members_select_members"
on public.razem_members for select
to authenticated
using (public.is_household_member(household_id));

create policy "razem_expenses_select_members"
on public.razem_expenses for select
to authenticated
using (public.is_household_member(household_id));

create policy "razem_expenses_insert_members"
on public.razem_expenses for insert
to authenticated
with check (
  public.is_household_member(household_id)
  and created_by = auth.uid()
  and exists (
    select 1 from public.razem_members
    where household_id = razem_expenses.household_id
      and user_id = razem_expenses.payer_id
  )
);

create policy "razem_expenses_update_members"
on public.razem_expenses for update
to authenticated
using (public.is_household_member(household_id))
with check (
  public.is_household_member(household_id)
  and exists (
    select 1 from public.razem_members
    where household_id = razem_expenses.household_id
      and user_id = razem_expenses.payer_id
  )
);

create policy "razem_expenses_delete_members"
on public.razem_expenses for delete
to authenticated
using (public.is_household_member(household_id));

create policy "razem_closed_months_members"
on public.razem_closed_months for all
to authenticated
using (public.is_household_member(household_id))
with check (
  public.is_household_member(household_id)
  and closed_by = auth.uid()
);

create policy "razem_device_tokens_own"
on public.razem_device_tokens for all
to authenticated
using (user_id = auth.uid())
with check (
  user_id = auth.uid()
  and public.is_household_member(household_id)
);

create policy "razem_activity_select_members"
on public.razem_activity for select
to authenticated
using (public.is_household_member(household_id));

create policy "razem_activity_insert_members"
on public.razem_activity for insert
to authenticated
with check (
  public.is_household_member(household_id)
  and user_id = auth.uid()
);

create function public.log_razem_expense_activity()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if tg_op = 'INSERT' then
    insert into public.razem_activity (household_id, expense_id, user_id, action, merchant, amount)
    values (new.household_id, new.id, auth.uid(), 'added', new.merchant, new.amount);
    return new;
  end if;

  if tg_op = 'UPDATE' then
    insert into public.razem_activity (household_id, expense_id, user_id, action, merchant, amount)
    values (new.household_id, new.id, auth.uid(), 'edited', new.merchant, new.amount);
    return new;
  end if;

  insert into public.razem_activity (household_id, expense_id, user_id, action, merchant, amount)
  values (old.household_id, old.id, auth.uid(), 'deleted', old.merchant, old.amount);
  return old;
end;
$$;

create trigger razem_expenses_activity_trigger
after insert or update or delete on public.razem_expenses
for each row execute function public.log_razem_expense_activity();

insert into storage.buckets (id, name, public)
values ('receipts', 'receipts', false)
on conflict (id) do update set public = false;

create policy "receipts_select_household_members"
on storage.objects for select
to authenticated
using (
  bucket_id = 'receipts'
  and public.is_household_member(split_part(name, '/', 1)::uuid)
);

create policy "receipts_insert_household_members"
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'receipts'
  and public.is_household_member(split_part(name, '/', 1)::uuid)
);

create policy "receipts_delete_household_members"
on storage.objects for delete
to authenticated
using (
  bucket_id = 'receipts'
  and public.is_household_member(split_part(name, '/', 1)::uuid)
);

revoke all on function public.create_razem_household(text) from public;
revoke all on function public.join_razem_household(text, text) from public;

-- RLS decides which rows are visible or editable; these grants only let the
-- authenticated API role reach the tables so that the RLS policies can apply.
grant select on public.razem_households, public.razem_members, public.razem_expenses,
  public.razem_closed_months, public.razem_device_tokens, public.razem_activity
  to authenticated;
grant insert, update, delete on public.razem_expenses, public.razem_closed_months,
  public.razem_device_tokens, public.razem_activity
  to authenticated;

-- The push Edge Function uses its server-only admin client to find the other
-- household member's token. New Supabase projects require this table privilege
-- explicitly; service_role still bypasses RLS, while app users remain limited
-- by razem_device_tokens_own.
grant select, delete on public.razem_device_tokens to service_role;

grant execute on function public.create_razem_household(text) to authenticated;
grant execute on function public.join_razem_household(text, text) to authenticated;
