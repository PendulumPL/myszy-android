-- V4: wspolna edycja i bezpieczne usuwanie paragonow

drop policy if exists razem_expenses_update_members on public.razem_expenses;
create policy razem_expenses_update_members on public.razem_expenses for update to authenticated
using (public.is_household_member(household_id))
with check (public.is_household_member(household_id));

drop policy if exists receipts_delete on storage.objects;
create policy receipts_delete on storage.objects for delete to authenticated
using (bucket_id='receipts' and public.is_household_member(((storage.foldername(name))[1])::uuid));