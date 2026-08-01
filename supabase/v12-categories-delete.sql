-- V12: kategorie wydatkow
alter table public.razem_expenses
  add column if not exists category text not null default 'Inne';

alter table public.razem_expenses drop constraint if exists razem_expenses_category_check;
alter table public.razem_expenses add constraint razem_expenses_category_check
  check (category in ('Jedzenie','Dom','Paliwo','Zdrowie','Transport','Rozrywka','Rachunki','Inne'));

-- Kazdy czlonek wspolnego domu moze usunac wspolny wydatek.
drop policy if exists razem_expenses_delete_members on public.razem_expenses;
create policy razem_expenses_delete_members on public.razem_expenses for delete to authenticated
using (public.is_household_member(household_id));