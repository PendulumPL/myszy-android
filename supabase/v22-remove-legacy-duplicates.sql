-- Mysze v22: usuwa wyłącznie stare, dokładne kopie wpisów Settle Up.
-- Bezpieczeństwo: skrypt wymaga znalezienia dokładnie 60 duplikatów.
-- Nie dotyka pojedynczego wpisu testowego z powiadomienia Alior.
begin;

do $$
declare
  pawel uuid;
  household uuid;
  removed_count integer;
begin
  select user_id into pawel
  from public.razem_members
  where lower(nickname) like '%myszo%'
  limit 1;

  if pawel is null then
    raise exception 'Nie znaleziono konta Myszo';
  end if;
  select household_id into household from public.razem_members where user_id = pawel limit 1;

  with duplicate_rows as (
    select distinct legacy.id
    from public.razem_expenses legacy
    join public.razem_expenses imported
      on imported.household_id = legacy.household_id
     and imported.import_key like 'settleup:%'
     and imported.amount = legacy.amount
     and imported.occurred_at = legacy.occurred_at
    where legacy.household_id = household
      and legacy.import_key is null
      and coalesce(legacy.source, 'manual') = 'manual'
  )
  delete from public.razem_expenses legacy
  using duplicate_rows d
  where legacy.id = d.id;

  get diagnostics removed_count = row_count;
  if removed_count <> 60 then
    raise exception 'Zabezpieczenie: znaleziono % duplikatów zamiast oczekiwanych 60. Nic nie zostało zapisane.', removed_count;
  end if;

  -- Arkusz zachowuje grosze po stronie osoby płacącej.
  -- Dla wydatków Myszo udział Myszy musi być resztą od pełnej kwoty.
  update public.razem_expenses e
  set ania_share = round(e.amount - e.pawel_share, 2)
  where e.household_id = household
    and e.import_key like 'settleup:%'
    and e.payer_id = pawel
    and lower(e.merchant) not like 'uregulowanie długu%';

  -- Część zaimportowana z Settle Up będzie po tym zgodna do grosza.
  update public.razem_households set balance_correction = 0.23 where id = household;
end $$;

with pawel as (
  select user_id from public.razem_members where lower(nickname) like '%myszo%' limit 1
), calculation as (
  select sum(
    case
      when lower(e.merchant) like '%spłata%'
        or lower(e.merchant) like '%splata%'
        or lower(e.merchant) like '%rozliczen%'
        or lower(e.merchant) like 'uregulowanie długu%'
        or lower(e.merchant) like 'uregulowanie dlugu%'
      then case when e.payer_id = (select user_id from pawel) then e.amount else -e.amount end
      when e.payer_id = (select user_id from pawel)
      then coalesce(e.ania_share, e.amount - e.amount * e.pawel_percent / 100.0)
      else -coalesce(e.pawel_share, e.amount * e.pawel_percent / 100.0)
    end
  ) as raw_balance
  from public.razem_expenses e
  where e.household_id = (select household_id from public.razem_members where user_id = (select user_id from pawel) limit 1)
)
select
  (select count(*) from public.razem_expenses) as wszystkie_wpisy,
  (select count(*) from public.razem_expenses where import_key like 'settleup:%') as wpisy_settle_up,
  round(raw_balance, 2) as saldo_przed_korekta,
  (select balance_correction from public.razem_households limit 1) as korekta,
  round(raw_balance + (select balance_correction from public.razem_households limit 1), 2) as saldo_koncowe_pawla
from calculation;

commit;