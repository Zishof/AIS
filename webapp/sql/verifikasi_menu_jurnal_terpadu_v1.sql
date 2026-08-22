\set ON_ERROR_STOP on
BEGIN READ ONLY;
SELECT count(*) AS menu_count, min(child) AS min_child,max(child) AS max_child
FROM public.menu WHERE id BETWEEN 2000460500 AND 2000460528;
SELECT count(*) AS child_count FROM public.menu WHERE root=4605 AND child BETWEEN 460501 AND 460528;
SELECT count(*) AS admin_assignment_count FROM public.job_has_menu WHERE job='am' AND menu BETWEEN 2000460500 AND 2000460528;
ROLLBACK;
