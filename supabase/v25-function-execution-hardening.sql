-- Security Advisor hardening for security-definer helpers.
-- Apply to DEV first, then verify: sign in, load a Home, create/join a Home,
-- add an expense, and open a receipt.

-- Internal trigger/helper functions never need to be called through the API.
revoke execute on function public.log_razem_expense_activity() from public;
revoke execute on function public.log_razem_expense_activity() from anon, authenticated;

-- Membership checks are required by RLS policies, but anonymous callers do not
-- need access. Authenticated users retain the minimum required permission.
revoke execute on function public.is_household_member(uuid) from public;
revoke execute on function public.is_household_member(uuid) from anon;
grant execute on function public.is_household_member(uuid) to authenticated;
