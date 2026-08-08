-- Read-only Hybrid Menu V2 diagnostic. URL-safe di sini adalah pendekatan SQL;
-- mapping explicit Menu.id -> New UI tetap diverifikasi oleh Java registry/self-test.
WITH RECURSIVE roles(role) AS (
    VALUES ('am'), ('Akademik'), ('mhs'), ('kpsk')
), assigned AS (
    SELECT r.role, m.id, COALESCE(m.root, 0) root, COALESCE(m.child, 0) child,
           COALESCE(m.nomorurut, 0) nomor_urut, m.label, COALESCE(m.url, '') url,
           COALESCE(rp._read, 0) = 1 readable,
           COALESCE(m.tampildipt, TRUE) visible_pt,
           COALESCE(m.tampildisekolah, TRUE) visible_school
    FROM roles r
    JOIN job_has_menu j ON j.job = r.role
    JOIN menu m ON m.id = j.menu AND m.aktif IS NOT FALSE
    LEFT JOIN role_privilage rp ON rp.role = r.role AND rp.menu = m.id
), route_ready AS (
    SELECT * FROM assigned
    WHERE readable AND url <> '' AND url !~ '(\.\.|://|\\)'
), visible(role, id, root, child) AS (
    SELECT role, id, root, child FROM route_ready
    UNION
    SELECT p.role, p.id, p.root, p.child
    FROM assigned p
    JOIN visible c ON c.role = p.role AND c.root = p.child
), classified AS (
    SELECT a.*,
           EXISTS (SELECT 1 FROM visible c WHERE c.role = a.role AND c.root = a.child) has_visible_child,
           EXISTS (SELECT 1 FROM assigned p WHERE p.role = a.role AND p.child = a.root) has_assigned_parent,
           (v.id IS NOT NULL) visible
    FROM assigned a
    LEFT JOIN visible v ON v.role = a.role AND v.id = a.id
)
SELECT role,
       COUNT(*) AS assigned_active,
       COUNT(*) FILTER (WHERE readable) AS assigned_read,
       COUNT(*) FILTER (WHERE NOT readable) AS assigned_read_forbidden,
       COUNT(*) FILTER (WHERE visible AND has_visible_child) AS sidebar_branches,
       COUNT(*) FILTER (WHERE visible AND NOT has_visible_child AND readable) AS catalog_leaves,
       COUNT(*) FILTER (WHERE visible AND has_visible_child AND (NOT readable OR url = '')) AS structural_only,
       COUNT(*) FILTER (WHERE visible AND root <> 0 AND NOT has_assigned_parent) AS orphan_visible,
       COUNT(*) FILTER (WHERE visible AND visible_pt) AS visible_pt_nodes,
       COUNT(*) FILTER (WHERE visible AND visible_school) AS visible_school_nodes
FROM classified
GROUP BY role
ORDER BY role;

-- Integritas hierarchy assignment: duplicate parent child-group dan self-cycle langsung.
WITH assigned AS (
    SELECT j.job role, m.id, COALESCE(m.root, 0) root, COALESCE(m.child, 0) child
    FROM job_has_menu j JOIN menu m ON m.id = j.menu AND m.aktif IS NOT FALSE
)
SELECT role,
       COUNT(*) FILTER (WHERE root = child AND root <> 0) AS direct_self_cycles,
       COUNT(*) FILTER (WHERE parent_count > 1) AS duplicate_child_groups
FROM (
    SELECT a.*, COUNT(*) OVER (PARTITION BY role, child) parent_count
    FROM assigned a
) x
GROUP BY role
ORDER BY role;
