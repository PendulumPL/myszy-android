-- Mysze v23: usuwa wyłącznie 59 potwierdzonych, starych kopii z okresu sprzed importu Settle Up.
-- Nie usuwa zakupu Stokrotka 7,98 zł z 15.07.2026 ani wpisów z powiadomień.
-- Całość działa transakcyjnie: przy innym wyniku nie zapisze żadnej zmiany.
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

  select household_id into household
  from public.razem_members
  where user_id = pawel
  limit 1;

  with duplicate_rows as (
    select distinct legacy.id
    from public.razem_expenses legacy
    join public.razem_expenses imported
      on imported.household_id = legacy.household_id
     and imported.import_key like 'settleup:%'
     and imported.amount = legacy.amount
     and imported.occurred_at = legacy.occurred_at
    where legacy.household_id = household
      -- Dawne wpisy mają pusty tekst, a nie SQL-owe NULL w import_key.
      and coalesce(trim(legacy.import_key), '') = ''
      and coalesce(legacy.source, 'manual') = 'manual'
  )
  delete from public.razem_expenses legacy
  using duplicate_rows d
  where legacy.id = d.id;

  get diagnostics removed_count = row_count;
  if removed_count <> 59 then
    raise exception 'Zabezpieczenie: znaleziono % kopii zamiast oczekiwanych 59. Nic nie zostało zapisane.', removed_count;
  end if;

  -- Odtwarza pełne grosze udziału Myszy w wierszach Settle Up opłaconych przez Myszo.
  update public.razem_expenses e
  set ania_share = round(e.amount - e.pawel_share, 2)
  where e.household_id = household
    and e.import_key like 'settleup:%'
    and e.payer_id = pawel
    and lower(e.merchant) not like '%spłata%'
    and lower(e.merchant) not like '%splata%'
    and lower(e.merchant) not like '%rozliczen%'
    and lower(e.merchant) not like 'uregulowanie długu%'
    and lower(e.merchant) not like 'uregulowanie dlugu%';

  -- Korekta za zaokrąglenia z oryginalnego eksportu Settle Up.
  update public.razem_households
  set balance_correction = 0.23
  where id = household;
end $$;

with pawel as (
  select user_id
  from public.razem_members
  where lower(nickname) like '%myszo%'
  limit 1
), household as (
  select household_id
  from public.razem_members
  where user_id = (select user_id from pawel)
  limit 1
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
  where e.household_id = (select household_id from household)
)
select
  (select count(*) from public.razem_expenses where household_id = (select household_id from household)) as wpisy_po_czyszczeniu,
  (select count(*) from public.razem_expenses where household_id = (select household_id from household) and import_key like 'settleup:%') as wpisy_settle_up,
  round(raw_balance, 2) as saldo_przed_korekta,
  (select balance_correction from public.razem_households where id = (select household_id from household)) as korekta,
  round(raw_balance + (select balance_correction from public.razem_households where id = (select household_id from household)), 2) as saldo_koncowe_pawla
from calculation;

commit;