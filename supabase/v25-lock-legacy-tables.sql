-- V25: stare tabele nie są używane przez obecną aplikację Myszy.
-- Zachowujemy ich dane, ale blokujemy publiczny dostęp przez PostgREST.
alter table if exists public.households enable row level security;
alter table if exists public.profiles enable row level security;
alter table if exists public.transactions enable row level security;
