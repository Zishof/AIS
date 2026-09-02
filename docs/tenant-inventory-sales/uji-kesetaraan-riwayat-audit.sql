-- =====================================================================================
-- Uji kesetaraan §20: riwayat audit tenant -- jejaknya sendiri, bukan Envers bersama
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v19:
--   psql -h 127.0.0.1 -p 55522 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-riwayat-audit.sql
--
-- Berkas ini mengandaikan schema tenant aud19 dan schema auditnya aud19__audit.
--
-- LATAR
--   org.hibernate.envers.default_schema bersifat STATIS per SessionFactory, sehingga baris
--   audit seluruh tenant berkumpul di satu schema. Membacanya untuk satu tenant berarti
--   menyajikan riwayat perubahan seluruh instalasi kepadanya -- kebocoran, bukan sekadar hasil
--   yang salah. Jalur tenant karena itu membaca audit_baris miliknya sendiri, yang ditulis
--   TenantAuditWriter ke <schema>__audit.
--
-- LIMA HAL YANG DIUJI
--   1. Tiga peristiwa satu baris terbaca berurutan terbaru-dulu, dengan revtype yang benar.
--   2. Penonaktifan dicatat DEL (2), bukan MOD -- bagi pemakai data itulah penghapusannya.
--   3. Blok 3 penjaganya: saringannya entity DAN entity_id. Dua entitas berbeda dengan id
--      yang sama tidak boleh saling terbawa.
--   4. totalRevisi dihitung dari SELURUH revisi, bukan dari 25 yang ditampilkan. Blok 4
--      penjaganya.
--   5. Satu revisi membawa konteksnya SEKALI, dan banyak baris menunjuk kepadanya -- menyimpan
--      satu dokumen berisi banyak item tetap satu revisi, bukan banyak salinan konteks.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\pset format aligned

TRUNCATE aud19__audit.audit_baris, aud19__audit.revinfo RESTART IDENTITY CASCADE;

-- Tiga peristiwa pada supplier 7, seperti ditulis TenantAuditWriter.
INSERT INTO aud19__audit.revinfo (revtstmp, tenant_id, tenant_code, user_id, role, action, waktu)
     VALUES (1, 1, 'T1', 'uji', 'OWNER', 'master_supplier_simpan', now()),
            (2, 1, 'T1', 'uji', 'OWNER', 'master_supplier_simpan', now()),
            (3, 1, 'T1', 'lain', 'ADMIN', 'master_nonaktifkan',    now());

INSERT INTO aud19__audit.audit_baris (rev, revtype, entity, entity_id, sebelum, sesudah, waktu)
     VALUES (1, 0, 'supplier', '7', NULL,
             '{"kode":"SUP7","nama":"Pemasok Awal","aktif":"true","status":"AKTIF"}', now()),
            (2, 1, 'supplier', '7',
             '{"kode":"SUP7","nama":"Pemasok Awal","aktif":"true","status":"AKTIF"}',
             '{"kode":"SUP7","nama":"Pemasok Diubah","aktif":"true","status":"AKTIF"}', now()),
            (3, 2, 'supplier', '7',
             '{"kode":"SUP7","nama":"Pemasok Diubah","aktif":"true","status":"AKTIF"}',
             '{"kode":"SUP7","nama":"Pemasok Diubah","aktif":"false","status":"AKTIF"}', now());

-- Customer ber-id SAMA. Kalau saringannya cuma id, ia akan ikut terbawa.
INSERT INTO aud19__audit.revinfo (revtstmp, tenant_id, tenant_code, user_id, role, action, waktu)
     VALUES (4, 1, 'T1', 'uji', 'OWNER', 'master_customer_simpan', now());
INSERT INTO aud19__audit.audit_baris (rev, revtype, entity, entity_id, sebelum, sesudah, waktu)
     VALUES (4, 0, 'customer', '7', NULL,
             '{"kode":"CUS7","nama":"Pelanggan","aktif":"true"}', now());

\echo ''
\echo '== BLOK 1: tiga peristiwa terbaca terbaru-dulu, revtype benar ====================='
\echo '   Ini kueri yang sama dengan selectRiwayatAudit().'

SELECT b.rev, b.revtype, r.action AS aksi, COALESCE(r.user_id,'') AS oleh,
       CASE WHEN b.rev = 3 AND b.revtype = 2 THEN 'LULUS (nonaktif = DEL)'
            WHEN b.rev = 2 AND b.revtype = 1 THEN 'LULUS (ubah = MOD)'
            WHEN b.rev = 1 AND b.revtype = 0 THEN 'LULUS (baru = ADD)'
            ELSE 'GAGAL' END AS hasil
  FROM aud19__audit.audit_baris b JOIN aud19__audit.revinfo r ON b.rev = r.rev
 WHERE b.entity = 'supplier' AND b.entity_id = '7'
 ORDER BY b.rev DESC;

\echo ''
\echo '== BLOK 2: yang berubah terbaca dari pasangan sebelum/sesudah ====================='
\echo '   Revisi 2 mengubah nama; revisi 3 mengubah aktif. Keduanya terbaca tanpa perlu'
\echo '   menyimpan "kolom apa yang berubah" -- itu turunan dari pasangannya.'

SELECT b.rev,
       (b.sebelum::json->>'nama')  AS "nama sebelum",
       (b.sesudah::json->>'nama')  AS "nama sesudah",
       (b.sebelum::json->>'aktif') AS "aktif sebelum",
       (b.sesudah::json->>'aktif') AS "aktif sesudah",
       CASE WHEN b.rev = 2
                 AND (b.sebelum::json->>'nama') <> (b.sesudah::json->>'nama')
                 AND (b.sebelum::json->>'aktif') = (b.sesudah::json->>'aktif')
            THEN 'LULUS (hanya nama)'
            WHEN b.rev = 3
                 AND (b.sebelum::json->>'nama') = (b.sesudah::json->>'nama')
                 AND (b.sebelum::json->>'aktif') <> (b.sesudah::json->>'aktif')
            THEN 'LULUS (hanya aktif)'
            ELSE 'GAGAL' END AS hasil
  FROM aud19__audit.audit_baris b
 WHERE b.entity = 'supplier' AND b.entity_id = '7' AND b.sebelum IS NOT NULL
 ORDER BY b.rev;

\echo ''
\echo '== BLOK 3 (PENJAGA): saringannya entity DAN entity_id ============================='
\echo '   Supplier 7 dan Customer 7 sama-sama ber-id 7. Angka di bawah HARUS 3 dan 1;'
\echo '   kalau keduanya 4, saringannya cuma id dan riwayat entitas lain ikut terbawa.'

SELECT (SELECT COUNT(*) FROM aud19__audit.audit_baris
         WHERE entity = 'supplier' AND entity_id = '7') AS "supplier/7",
       (SELECT COUNT(*) FROM aud19__audit.audit_baris
         WHERE entity = 'customer' AND entity_id = '7') AS "customer/7",
       (SELECT COUNT(*) FROM aud19__audit.audit_baris WHERE entity_id = '7')
         AS "bila saringannya id saja",
       CASE WHEN (SELECT COUNT(*) FROM aud19__audit.audit_baris
                   WHERE entity = 'supplier' AND entity_id = '7') = 3
             AND (SELECT COUNT(*) FROM aud19__audit.audit_baris
                   WHERE entity = 'customer' AND entity_id = '7') = 1
             AND (SELECT COUNT(*) FROM aud19__audit.audit_baris WHERE entity_id = '7') = 4
            THEN 'LULUS (saringan entity memang membedakan)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

\echo ''
\echo '== BLOK 4 (PENJAGA): totalRevisi dihitung dari SELURUH revisi ====================='
\echo '   Tiga puluh revisi ditambahkan pada customer 9; yang ditampilkan dibatasi 25.'
\echo '   Kalau totalnya diambil dari jumlah baris yang tampil, riwayat panjang akan tampak'
\echo '   pendek. Angka "ditampilkan" HARUS 25 dan "total" HARUS 30.'

INSERT INTO aud19__audit.revinfo (revtstmp, tenant_id, tenant_code, user_id, role, action, waktu)
     SELECT 100 + g, 1, 'T1', 'uji', 'OWNER', 'master_customer_simpan', now()
       FROM generate_series(1, 30) g;
INSERT INTO aud19__audit.audit_baris (rev, revtype, entity, entity_id, sebelum, sesudah, waktu)
     SELECT r.rev, 1, 'customer', '9', NULL, '{"kode":"CUS9","nama":"Berulang","aktif":"true"}',
            now()
       FROM aud19__audit.revinfo r WHERE r.revtstmp > 100;

SELECT (SELECT COUNT(*) FROM (
          SELECT b.rev FROM aud19__audit.audit_baris b
           WHERE b.entity = 'customer' AND b.entity_id = '9'
           ORDER BY b.rev DESC LIMIT 25) q) AS "ditampilkan",
       (SELECT COUNT(*) FROM aud19__audit.audit_baris
         WHERE entity = 'customer' AND entity_id = '9') AS "total",
       CASE WHEN (SELECT COUNT(*) FROM (
                    SELECT b.rev FROM aud19__audit.audit_baris b
                     WHERE b.entity = 'customer' AND b.entity_id = '9'
                     ORDER BY b.rev DESC LIMIT 25) q) = 25
             AND (SELECT COUNT(*) FROM aud19__audit.audit_baris
                   WHERE entity = 'customer' AND entity_id = '9') = 30
            THEN 'LULUS (keduanya memang berbeda)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

\echo ''
\echo '== BLOK 5: satu revisi, banyak baris ============================================='
\echo '   Satu penyimpanan yang menyentuh tiga baris tetap SATU revisi; konteksnya tidak'
\echo '   disalin tiga kali.'

INSERT INTO aud19__audit.revinfo (revtstmp, tenant_id, tenant_code, user_id, role, action, waktu)
     VALUES (900, 1, 'T1', 'uji', 'OWNER', 'master_supplier_simpan', now());
INSERT INTO aud19__audit.audit_baris (rev, revtype, entity, entity_id, sebelum, sesudah, waktu)
     SELECT currval('aud19__audit.revinfo_rev_seq'), 0, 'supplier', g::text, NULL,
            '{"kode":"X","nama":"Massal","aktif":"true"}', now()
       FROM generate_series(101, 103) g;

SELECT (SELECT COUNT(*) FROM aud19__audit.revinfo
         WHERE revtstmp = 900) AS "baris revinfo",
       (SELECT COUNT(*) FROM aud19__audit.audit_baris
         WHERE rev = (SELECT rev FROM aud19__audit.revinfo WHERE revtstmp = 900))
         AS "baris audit_baris",
       CASE WHEN (SELECT COUNT(*) FROM aud19__audit.revinfo WHERE revtstmp = 900) = 1
             AND (SELECT COUNT(*) FROM aud19__audit.audit_baris
                   WHERE rev = (SELECT rev FROM aud19__audit.revinfo WHERE revtstmp = 900)) = 3
            THEN 'LULUS (satu konteks, tiga baris)' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 6: muatan tidak memuat kolom jejak ======================================='
\echo '   "siapa dan kapan" ada pada revinfo. Menyalinnya ke muatan akan membuat setiap'
\echo '   perubahan tampak berbeda pada kolom yang bukan isi datanya.'

SELECT b.sesudah AS muatan,
       (b.sesudah::json->>'oleh') IS NULL             AS "tanpa oleh",
       (b.sesudah::json->>'tanggal_dirubah') IS NULL  AS "tanpa tanggal_dirubah",
       CASE WHEN (b.sesudah::json->>'oleh') IS NULL
             AND (b.sesudah::json->>'tanggal_dirubah') IS NULL
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM aud19__audit.audit_baris b
 WHERE b.entity = 'supplier' AND b.entity_id = '7' AND b.rev = 1;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
