begin;

-- Usuwa wyłącznie testowe dane aplikacji Razem.
-- Konta Supabase Auth pozostają bez zmian.
delete from public.razem_expenses;
delete from public.razem_members;
delete from public.razem_households;

-- W tej aplikacji każde konto należy dokładnie do jednego wspólnego domu.
alter table public.razem_members
    drop constraint if exists razem_members_one_home;

alter table public.razem_members
    add constraint razem_members_one_home unique (user_id);

commit;