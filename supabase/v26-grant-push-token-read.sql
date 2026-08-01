-- Allow the server-only push function to read the recipient token.
-- The mobile app still sees only its own rows through RLS.
grant select, delete on public.razem_device_tokens to service_role;
