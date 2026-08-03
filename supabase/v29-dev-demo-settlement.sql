-- DEV/demo only: add one fictional settlement for the portfolio screen.
-- Run only in myszy-dev after v27 and v28.

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
    a.user_id as ania_id
  from public.razem_households h
  join public.razem_members p on p.household_id = h.id
  join auth.users pu on pu.id = p.user_id
  join public.razem_members a on a.household_id = h.id and a.user_id <> p.user_id
  join auth.users au on au.id = a.user_id
  where lower(pu.email) = 'myszy.dev.pawel@example.com'
    and lower(au.email) = 'myszy.dev.ania@example.com'
  limit 1
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
  d.ania_id,
  d.ania_id,
  'Spłata rozliczenia',
  56.10::numeric,
  0,
  0,
  56.10::numeric,
  'manual',
  'Inne',
  'Fikcyjne wyrównanie do portfolio',
  'portfolio-demo-settlement-01',
  now() - interval '1 day'
from demo d
where not exists (
  select 1
  from public.razem_expenses e
  where e.household_id = d.household_id
    and e.import_key = 'portfolio-demo-settlement-01'
);

reset request.jwt.claim.sub;
