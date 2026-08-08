-- Read-only diagnostic. Replace role values when preparing acceptance data.
WITH roles(role) AS (VALUES ('am'), ('amp')),
role_menu AS (
    SELECT r.role, m.id, m.root, m.child,
           (j.menu IS NOT NULL) AS assigned,
           (COALESCE(rp._read, 0) = 1) AS readable,
           COALESCE(m.tampildipt, TRUE) AS visible_pt,
           COALESCE(m.tampildisekolah, TRUE) AS visible_school
    FROM roles r
    CROSS JOIN menu m
    LEFT JOIN job_has_menu j ON j.job = r.role AND j.menu = m.id
    LEFT JOIN role_privilage rp ON rp.role = r.role AND rp.menu = m.id
    WHERE m.aktif IS NOT FALSE
)
SELECT role,
       COUNT(*) FILTER (WHERE assigned AND readable) AS authorized,
       COUNT(*) FILTER (WHERE assigned AND NOT readable) AS assigned_but_forbidden,
       COUNT(*) FILTER (WHERE NOT assigned AND readable) AS privilege_without_assignment,
       COUNT(*) FILTER (WHERE assigned AND readable AND visible_pt) AS authorized_pt,
       COUNT(*) FILTER (WHERE assigned AND readable AND visible_school) AS authorized_school
FROM role_menu
GROUP BY role
ORDER BY role;

-- Ancestor display-only count for one role; recursion follows child(parent)=root(child).
WITH RECURSIVE direct AS (
    SELECT m.id, m.root, m.child
    FROM menu m
    JOIN job_has_menu j ON j.menu = m.id AND j.job = 'am'
    JOIN role_privilage rp ON rp.menu = m.id AND rp.role = j.job
    WHERE m.aktif IS NOT FALSE AND COALESCE(rp._read, 0) = 1
), visible AS (
    SELECT * FROM direct
    UNION
    SELECT parent.id, parent.root, parent.child
    FROM menu parent
    JOIN visible child ON parent.child = child.root
    WHERE parent.aktif IS NOT FALSE
)
SELECT COUNT(*) FILTER (WHERE direct.id IS NULL) AS display_only_ancestors
FROM visible
LEFT JOIN direct ON direct.id = visible.id;
