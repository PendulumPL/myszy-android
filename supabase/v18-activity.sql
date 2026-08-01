-- Trwała historia aktywności Mysze (uruchom jednorazowo w Supabase SQL Editor)
create table if not exists public.razem_activity (
  id uuid primary key default gen_random_uuid(),
  household_id uuid not null references public.razem_households(id) on delete cascade,
  expense_id uuid null,
  user_id uuid not null references auth.users(id) on delete cascade,
  action text not null check (action in ('added','edited','deleted','settled','receipt_removed')),
  merchant text not null default '',
  amount numeric not null default 0,
  occurred_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index if not exists razem_activity_household_created_idx
  on public.razem_activity(household_id, created_at desc);

alter table public.razem_activity enable row level security;
drop policy if exists razem_activity_read on public.razem_activity;
create policy razem_activity_read on public.razem_activity for select to authenticated
  using (public.is_household_member(household_id));
drop policy if exists razem_activity_insert on public.razem_activity;
create policy razem_activity_insert on public.razem_activity for insert to authenticated
  with check (user_id = auth.uid() and public.is_household_member(household_id));

create or replace function public.log_razem_expense_activity()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  actor uuid := coalesce(auth.uid(), new.created_by, old.created_by);
  hid uuid := coalesce(new.household_id, old.household_id);
  eid uuid := coalesce(new.id, old.id);
  act text;
  shop text := coalesce(new.merchant, old.merchant, '');
  val numeric := coalesce(new.amount, old.amount, 0);
begin
  if tg_op = 'INSERT' then act := 'added';
  elsif tg_op = 'UPDATE' then act := 'edited';
  else act := 'deleted'; end if;
  if actor is not null then
    insert into public.razem_activity(household_id, expense_id, user_id, action, merchant, amount)
    values (hid, eid, actor, act, shop, val);
  end if;
  if tg_op = 'DELETE' then return old; else return new; end if;
end; $$;

drop trigger if exists razem_expense_activity_trigger on public.razem_expenses;
create trigger razem_expense_activity_trigger
after insert or update or delete on public.razem_expenses
for each row execute function public.log_razem_expense_activity();
