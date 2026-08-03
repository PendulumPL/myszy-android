-- DEV/demo only: add a varied, repeatable set of fictional expenses for
-- screenshots and portfolio demonstrations. Run only in myszy-dev.
-- The import_key guard makes this migration safe to run more than once.

-- SQL Editor runs without an authenticated app session.  The activity trigger
-- uses auth.uid(), so provide the DEV Pawel user only for this seed session.
-- This does not change RLS or any production/app authentication setting.
do $$
declare
  demo_uid text;
begin
  select id::text
    into demo_uid
  from auth.users
  where lower(email) = 'myszy.dev.pawel@example.com'
  limit 1;

  if demo_uid is null then
    raise exception 'DEV seed user myszy.dev.pawel@example.com was not found';
  end if;

  perform set_config('request.jwt.claim.sub', demo_uid, false);
end
$$;

with demo as (
  select
    h.id as household_id,
    p.user_id as pawel_id,
    a.user_id as ania_id
  from public.razem_households h
  join public.razem_members p on p.household_id = h.id
  join auth.users pu on pu.id = p.user_id
  join public.razem_members a on a.household_id = h.id and a.user_id <> p.user_id
  join auth.users au on au.id = a.user_id
  where lower(pu.email) = 'myszy.dev.pawel@example.com'
    and lower(au.email) = 'myszy.dev.ania@example.com'
  limit 1
),
items(merchant, amount, payer, pawel_percent, category, comment, import_key, occurred_at) as (
  values
    ('Zakupy tygodniowe', 156.80::numeric, 'Paweł', 60, 'Spożywcze', 'Fikcyjny koszyk do testów', 'portfolio-demo-01', now() - interval '1 day'),
    ('Kolacja we dwoje', 92.00::numeric, 'Ania', 60, 'Jedzenie', 'Test wspólnego posiłku', 'portfolio-demo-02', now() - interval '2 days'),
    ('Rachunek za internet', 79.99::numeric, 'Paweł', 60, 'Rachunki', 'Stała opłata miesięczna', 'portfolio-demo-03', now() - interval '3 days'),
    ('Chemia do domu', 48.50::numeric, 'Ania', 60, 'Dom', 'Okruszki do wspólnej norki', 'portfolio-demo-04', now() - interval '4 days'),
    ('Bilety do kina', 86.00::numeric, 'Paweł', 60, 'Rozrywka', 'Test kategorii rozrywka', 'portfolio-demo-05', now() - interval '5 days'),
    ('Apteka', 31.40::numeric, 'Ania', 60, 'Zdrowie', 'Przykładowy wydatek zdrowotny', 'portfolio-demo-06', now() - interval '6 days'),
    ('Paliwo', 210.00::numeric, 'Paweł', 60, 'Paliwo', 'Większy wydatek do bilansu', 'portfolio-demo-07', now() - interval '7 days'),
    ('Sklep osiedlowy', 23.70::numeric, 'Ania', 60, 'Spożywcze', 'Mały codzienny wydatek', 'portfolio-demo-08', now() - interval '8 days'),
    ('Kawa i ciasto', 34.00::numeric, 'Paweł', 60, 'Jedzenie', 'Test szybkiego wpisu', 'portfolio-demo-09', now() - interval '9 days'),
    ('Taksówka', 44.90::numeric, 'Ania', 60, 'Transport', 'Przejazd testowy', 'portfolio-demo-10', now() - interval '10 days'),
    ('Zakupy domowe', 118.30::numeric, 'Paweł', 60, 'Dom', 'Większe zakupy do norki', 'portfolio-demo-11', now() - interval '11 days'),
    ('Rachunek za telefon', 59.00::numeric, 'Ania', 60, 'Rachunki', 'Drugi przykład rachunku', 'portfolio-demo-12', now() - interval '12 days'),
    ('Weekendowy obiad', 126.00::numeric, 'Ania', 60, 'Jedzenie', 'Test większego posiłku', 'portfolio-demo-13', now() - interval '13 days'),
    ('Bilety autobusowe', 24.00::numeric, 'Paweł', 60, 'Transport', 'Mały wydatek transportowy', 'portfolio-demo-14', now() - interval '14 days'),
    ('Prezent testowy', 72.00::numeric, 'Ania', 60, 'Inne', 'Neutralny wpis do portfolio', 'portfolio-demo-15', now() - interval '15 days'),
    ('Śniadanie', 38.40::numeric, 'Paweł', 60, 'Jedzenie', 'Ostatni fikcyjny okruszek', 'portfolio-demo-16', now() - interval '16 days')
)
insert into public.razem_expenses (
  household_id,
  created_by,
  payer_id,
  merchant,
  amount,
  pawel_percent,
  pawel_share,
  ania_share,
  source,
  category,
  comment,
  import_key,
  occurred_at
)
select
  d.household_id,
  d.pawel_id,
  case when i.payer = 'Paweł' then d.pawel_id else d.ania_id end,
  i.merchant,
  i.amount,
  i.pawel_percent,
  round(i.amount * i.pawel_percent / 100, 2),
  round(i.amount * (100 - i.pawel_percent) / 100, 2),
  'manual',
  i.category,
  i.comment,
  i.import_key,
  i.occurred_at
from demo d
cross join items i
where not exists (
  select 1
  from public.razem_expenses e
  where e.household_id = d.household_id
    and e.import_key = i.import_key
);

-- Do not leave the temporary SQL Editor claim in the session.
reset request.jwt.claim.sub;
