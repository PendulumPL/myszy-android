-- V24: Dom Myszy jest prywatną przestrzenią dokładnie dla jednej pary.
-- Uruchom najpierw w prywatnym projekcie Supabase po sprawdzeniu, że
-- każdy użytkownik ma najwyżej jedno członkostwo. Ten skrypt niczego nie usuwa.

drop policy if exists razem_expenses_all on public.razem_expenses;
drop policy if exists razem_expenses_update_members on public.razem_expenses;
drop policy if exists razem_expenses_delete_members on public.razem_expenses;
drop policy if exists razem_expenses_select_members on public.razem_expenses;
drop policy if exists razem_expenses_insert_members on public.razem_expenses;

create policy razem_expenses_select_members
on public.razem_expenses
for select
to authenticated
using (public.is_household_member(household_id));

create policy razem_expenses_insert_members
on public.razem_expenses
for insert
to authenticated
with check (
  public.is_household_member(household_id)
  and created_by = (select auth.uid())
  and exists (
    select 1
    from public.razem_members member
    where member.household_id = razem_expenses.household_id
      and member.user_id = razem_expenses.payer_id
  )
);

create policy razem_expenses_update_members
on public.razem_expenses
for update
to authenticated
using (public.is_household_member(household_id))
with check (
  public.is_household_member(household_id)
  and exists (
    select 1
    from public.razem_members member
    where member.household_id = razem_expenses.household_id
      and member.user_id = razem_expenses.payer_id
  )
);

create policy razem_expenses_delete_members
on public.razem_expenses
for delete
to authenticated
using (public.is_household_member(household_id));

create or replace function public.create_razem_household(member_nickname text)
returns table(household_id uuid, invite_code text)
language plpgsql security definer set search_path = public
as $$
declare h public.razem_households;
begin
  if auth.uid() is null then
    raise exception 'Logowanie wymagane';
  end if;
  if exists (select 1 from public.razem_members where user_id = auth.uid()) then
    raise exception 'To konto należy już do Domu Myszy';
  end if;

  insert into public.razem_households(name, created_by)
  values ('Dom Myszy', auth.uid())
  returning * into h;
  insert into public.razem_members(household_id, user_id, nickname)
  values (h.id, auth.uid(), member_nickname);
  return query select h.id, h.invite_code;
end;
$$;

create or replace function public.join_razem_household(code text, member_nickname text)
returns uuid language plpgsql security definer set search_path = public
as $$
declare
  hid uuid;
  member_count integer;
begin
  if auth.uid() is null then
    raise exception 'Logowanie wymagane';
  end if;

  select id into hid
  from public.razem_households
  where invite_code = upper(trim(code));
  if hid is null then
    raise exception 'Nieprawidłowy kod zaproszenia';
  end if;

  if exists (
    select 1 from public.razem_members
    where household_id = hid and user_id = auth.uid()
  ) then
    return hid;
  end if;
  if exists (select 1 from public.razem_members where user_id = auth.uid()) then
    raise exception 'To konto należy już do innego Domu Myszy';
  end if;

  -- Blokada wiersza gospodarstwa domowego zapobiega równoczesnemu dołączeniu
  -- dwóch osób do ostatniego wolnego miejsca.
  perform 1 from public.razem_households where id = hid for update;
  select count(*) into member_count
  from public.razem_members
  where household_id = hid;
  if member_count >= 2 then
    raise exception 'Ten Dom Myszy ma już dwie osoby';
  end if;

  insert into public.razem_members(household_id, user_id, nickname)
  values (hid, auth.uid(), member_nickname);
  return hid;
end;
$$;

revoke all on function public.create_razem_household(text) from public;
revoke all on function public.join_razem_household(text, text) from public;
grant execute on function public.create_razem_household(text) to authenticated;
grant execute on function public.join_razem_household(text, text) to authenticated;
