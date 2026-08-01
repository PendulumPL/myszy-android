-- Mysze v20: dokładne udziały historyczne i techniczna korekta Settle Up.
alter table public.razem_expenses add column if not exists pawel_share numeric(12,2);
alter table public.razem_expenses add column if not exists ania_share numeric(12,2);
alter table public.razem_expenses add column if not exists import_key text;
-- Starsze instalacje aplikacji mogły powstać przed polem źródła wpisu.
-- Dodajemy je bezpiecznie, aby import mógł rozróżniać dane Settle Up.
alter table public.razem_expenses add column if not exists source text not null default 'manual';
alter table public.razem_expenses add column if not exists category text not null default 'Inne';
alter table public.razem_expenses add column if not exists comment text not null default '';
alter table public.razem_households add column if not exists balance_correction numeric(12,2) not null default 0;

alter table public.razem_expenses drop constraint if exists razem_expenses_category_check;
alter table public.razem_expenses add constraint razem_expenses_category_check
  check (category in ('Jedzenie', 'Spożywcze', 'Zakupy', 'Dom', 'Paliwo', 'Zdrowie', 'Transport', 'Rozrywka', 'Rachunki', 'Inne'));
alter table public.razem_expenses drop constraint if exists razem_expenses_source_check;
alter table public.razem_expenses add constraint razem_expenses_source_check
  check (source in ('manual', 'receipt', 'alior_notification', 'settleup_import'));

alter table public.razem_expenses drop constraint if exists razem_expenses_household_import_key_unique;
alter table public.razem_expenses add constraint razem_expenses_household_import_key_unique
  unique (household_id, import_key);
