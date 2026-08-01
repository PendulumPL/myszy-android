-- Run once in Supabase SQL Editor.
alter table public.razem_expenses add column if not exists comment text not null default '';
create table if not exists public.razem_closed_months (
  household_id uuid not null references public.razem_households(id) on delete cascade,
  month_key text not null check (month_key ~ '^\d{4}-\d{2}$'),
  closed_by uuid not null references auth.users(id),
  closed_at timestamptz not null default now(),
  primary key (household_id, month_key)
);
alter table public.razem_closed_months enable row level security;
drop policy if exists razem_closed_months_members on public.razem_closed_months;
create policy razem_closed_months_members on public.razem_closed_months for all to authenticated
using (public.is_household_member(household_id))
with check (public.is_household_member(household_id) and closed_by = auth.uid());
