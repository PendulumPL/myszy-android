-- Uruchom raz w Supabase SQL Editor przed użyciem kategorii „Zakupy”.
alter table public.razem_expenses drop constraint if exists razem_expenses_category_check;
alter table public.razem_expenses add constraint razem_expenses_category_check
  check (category in (
    'Jedzenie', 'Spożywcze', 'Zakupy', 'Dom', 'Paliwo',
    'Zdrowie', 'Transport', 'Rozrywka', 'Rachunki', 'Inne'
  ));