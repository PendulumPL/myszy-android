-- V26: synchronizowane motywy myszy i kolor profilu członka Domu.
-- Uruchom jednorazowo w Supabase SQL Editor na bazie DEV i prywatnej.

alter table public.razem_members
  add column if not exists avatar_id smallint not null default 0
    check (avatar_id between 0 and 19),
  add column if not exists profile_color integer not null default 0;

drop policy if exists razem_members_update_own_profile on public.razem_members;
create policy razem_members_update_own_profile
  on public.razem_members
  for update to authenticated
  using (user_id = (select auth.uid()) and public.is_household_member(household_id))
  with check (user_id = (select auth.uid()) and public.is_household_member(household_id));
