-- DEV/demo only: give the two prepared demo accounts clear names for
-- screenshots and portfolio material. Run this only in the separate
-- myszy-dev Supabase project.

update public.razem_members as m
set nickname = case lower(u.email)
  when 'myszy.dev.pawel@example.com' then 'Paweł'
  when 'myszy.dev.ania@example.com' then 'Ania'
  else m.nickname
end
from auth.users as u
where u.id = m.user_id
  and lower(u.email) in (
    'myszy.dev.pawel@example.com',
    'myszy.dev.ania@example.com'
  );
