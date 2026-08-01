-- Uruchom raz w Supabase SQL Editor, aby obie Myszy mogly edytowac wspolne wydatki.
drop policy if exists razem_expenses_update_members on public.razem_expenses;
create policy razem_expenses_update_members
on public.razem_expenses
for update
to authenticated
using (public.is_household_member(household_id))
with check (public.is_household_member(household_id));