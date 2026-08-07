/*
 * SQL DIAGNOSTIK RBAC NEW UI
 * Jalankan hanya pada database development/staging terlebih dahulu.
 * Semua query di bagian A bersifat read-only.
 * Verifikasi nama fisik kolom dari schema PostgreSQL aktual sebelum membuat migration.
 */

/* A1. Cari menu pengelolaan role dan pengguna existing. */
SELECT id, root, child, label, url, aktif, nomor_urut,
       tampil_di_pt, tampil_di_sekolah, buka_halaman_baru
FROM menu
WHERE lower(coalesce(label, '')) LIKE ANY (ARRAY[
          '%role%', '%grup pengguna%', '%hak akses%', '%pengguna%', '%user%'
      ])
   OR lower(coalesce(url, '')) LIKE ANY (ARRAY[
          '%tbmrole%', '%tbmuser%', '%role%', '%user%'
      ])
ORDER BY nomor_urut, root, child, id;

/* A2. Semua menu sebuah role beserta privilege. Ganti :ROLE_ID di client SQL. */
SELECT
    r.roleid,
    r.rolename,
    m.id AS menu_id,
    m.root,
    m.child,
    m.label,
    m.url,
    m.aktif AS menu_aktif,
    rp._read,
    rp._create,
    rp._update,
    rp._delete,
    rp._approve,
    rp._reject
FROM tbmrole r
JOIN job_has_menu jhm ON jhm.job = r.roleid
JOIN menu m ON m.id = jhm.menu
LEFT JOIN role_privilage rp
       ON rp.role = r.roleid
      AND rp.menu = m.id
WHERE r.roleid = :ROLE_ID
ORDER BY m.nomor_urut, m.root, m.child, m.id;

/* A3. Assignment menu tanpa privilege row; New UI harus fail closed. */
SELECT jhm.job AS role_id, jhm.menu AS menu_id, m.label, m.url
FROM job_has_menu jhm
JOIN menu m ON m.id = jhm.menu
LEFT JOIN role_privilage rp
       ON rp.role = jhm.job
      AND rp.menu = jhm.menu
WHERE rp.id IS NULL
ORDER BY jhm.job, m.nomor_urut, m.root, m.child;

/* A4. Privilege orphan/tidak mempunyai assignment job_has_menu. */
SELECT rp.id, rp.role, rp.menu, m.label, m.url,
       rp._read, rp._create, rp._update, rp._delete,
       rp._approve, rp._reject
FROM role_privilage rp
LEFT JOIN job_has_menu jhm
       ON jhm.job = rp.role
      AND jhm.menu = rp.menu
LEFT JOIN menu m ON m.id = rp.menu
WHERE jhm.job IS NULL
ORDER BY rp.role, rp.menu;

/* A5. Role-menu assigned tetapi READ tidak aktif. */
SELECT jhm.job AS role_id, m.id AS menu_id, m.label, m.url,
       coalesce(rp._read, 0) AS can_read
FROM job_has_menu jhm
JOIN menu m ON m.id = jhm.menu
LEFT JOIN role_privilage rp
       ON rp.role = jhm.job
      AND rp.menu = jhm.menu
WHERE coalesce(rp._read, 0) <> 1
ORDER BY jhm.job, m.nomor_urut, m.root, m.child;

/* A6. Menu inactive yang masih di-assign. */
SELECT jhm.job AS role_id, m.id AS menu_id, m.label, m.url
FROM job_has_menu jhm
JOIN menu m ON m.id = jhm.menu
WHERE coalesce(m.aktif, false) = false
ORDER BY jhm.job, m.nomor_urut, m.root, m.child;

/* A7. Ringkasan jumlah menu per role. */
SELECT r.roleid, r.rolename, r.aktif,
       count(DISTINCT jhm.menu) AS assigned_menu,
       count(DISTINCT CASE WHEN rp._read = 1 THEN rp.menu END) AS readable_menu
FROM tbmrole r
LEFT JOIN job_has_menu jhm ON jhm.job = r.roleid
LEFT JOIN role_privilage rp
       ON rp.role = jhm.job
      AND rp.menu = jhm.menu
GROUP BY r.roleid, r.rolename, r.aktif
ORDER BY r.rolename, r.roleid;

/* A8. Pengguna dengan role duplikat pada slot 1-5.
 * Verifikasi nama fisik FK column Tbmuser terlebih dahulu melalui \d tbmuser.
 * Sesuaikan user_role/user_role2/... dengan nama schema aktual.
 */
SELECT *
FROM tbmuser u
WHERE (u.user_role IS NOT NULL AND u.user_role IN (u.user_role2, u.user_role3, u.user_role4, u.user_role5))
   OR (u.user_role2 IS NOT NULL AND u.user_role2 IN (u.user_role3, u.user_role4, u.user_role5))
   OR (u.user_role3 IS NOT NULL AND u.user_role3 IN (u.user_role4, u.user_role5))
   OR (u.user_role4 IS NOT NULL AND u.user_role4 = u.user_role5);

/* A9. Role nonaktif yang masih dipakai pengguna.
 * Sesuaikan nama kolom fisik role slot sesuai schema aktual.
 */
SELECT r.roleid, r.rolename, count(*) AS jumlah_referensi
FROM tbmrole r
JOIN tbmuser u
  ON r.roleid IN (u.user_role, u.user_role2, u.user_role3, u.user_role4, u.user_role5)
WHERE coalesce(r.aktif, false) = false
GROUP BY r.roleid, r.rolename
ORDER BY jumlah_referensi DESC;

/* A10. Duplikasi privilege untuk pasangan role-menu. */
SELECT role, menu, count(*) AS jumlah
FROM role_privilage
GROUP BY role, menu
HAVING count(*) > 1
ORDER BY jumlah DESC, role, menu;

/* B. Template transaction untuk seed menu, JANGAN dijalankan sebelum Codex
 * menemukan record/struktur parent existing dan memverifikasi nama sequence/kolom.
 * Tidak ada ID hard-coded di template ini.
 */

/*
BEGIN;

-- 1) Cari parent sistem/maintenance existing dan lock seperlunya.
-- 2) INSERT menu hanya jika URL/key stabil belum ada.
-- 3) Berikan job_has_menu + role_privilage hanya kepada role administrator
--    existing yang benar-benar teridentifikasi dari source/config.
-- 4) Preview hasil.

-- COMMIT;
-- ROLLBACK;
*/
