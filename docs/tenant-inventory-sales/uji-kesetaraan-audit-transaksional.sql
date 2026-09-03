-- =====================================================================================
-- Uji kesetaraan §22: jejak audit untuk empat entitas transaksional
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v19 dan schema auditnya:
--   psql -h 127.0.0.1 -p 55526 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-audit-transaksional.sql
--
-- Berkas ini mengandaikan schema tenant a22 dan schema audit a22__audit.
--
-- LATAR
--   §20 memasang penulis audit untuk tiga entitas master dan MENOLAK empat sisanya dengan
--   menyebut namanya. §22 memasang keempatnya: order, piutang, penerimaan, spj.
--
-- EMPAT HAL YANG DIUJI
--   1. Pembalikan penagihan menulis DUA baris pada SATU peristiwa: dokumen pembaliknya lahir
--      (ADD) dan dokumen yang dibalik ditutup (DEL). Mencatat salah satunya saja membuat
--      riwayat dokumen yang lain bungkam tentang peristiwa terpenting baginya.
--   2. Pemfakturan juga menulis dua baris untuk DUA entitas berbeda: piutangnya lahir dan
--      ordernya berpindah status.
--   3. Blok 3 penjaganya: muatan piutang TIDAK memuat kolom ringkasan (terbayar/sisa).
--      Kalau ia memuatnya, tiap alokasi yang menyentuh dokumen akan tampak sebagai perubahan
--      pada barisnya padahal barisnya tidak disunting.
--   4. Muatan tidak memuat kolom jejak (oleh/dibuat_pada/tanggal_dirubah).
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\pset format aligned

TRUNCATE a22__audit.audit_baris, a22__audit.revinfo RESTART IDENTITY CASCADE;

-- Peristiwa 1: pemfakturan -- piutang lahir, order berpindah status.
INSERT INTO a22__audit.revinfo (revtstmp, tenant_id, tenant_code, user_id, role, action, waktu)
     VALUES (1, 1, 'T1', 'uji', 'OWNER', 'sales_order_faktur', now());
INSERT INTO a22__audit.audit_baris (rev, revtype, entity, entity_id, sebelum, sesudah, waktu)
     VALUES (1, 0, 'piutang_customer', '1', NULL,
             '{"nomor_faktur":"INV-1","nilai":"1000000.00","status":"TERBUKA","customer_id":"9"}',
             now());
INSERT INTO a22__audit.revinfo (revtstmp, tenant_id, tenant_code, user_id, role, action, waktu)
     VALUES (2, 1, 'T1', 'uji', 'OWNER', 'sales_order_faktur', now());
INSERT INTO a22__audit.audit_baris (rev, revtype, entity, entity_id, sebelum, sesudah, waktu)
     VALUES (2, 1, 'sales_order', '77',
             '{"nomor_dokumen":"SO-77","total":"1000000.00","status":"TERKIRIM"}',
             '{"nomor_dokumen":"SO-77","total":"1000000.00","status":"SIAP_TAGIH"}', now());

-- Peristiwa 2: pembalikan penagihan -- pembalik lahir, yang dibalik ditutup.
INSERT INTO a22__audit.revinfo (revtstmp, tenant_id, tenant_code, user_id, role, action, waktu)
     VALUES (3, 1, 'T1', 'lain', 'ADMIN', 'penagihan_balik', now());
INSERT INTO a22__audit.audit_baris (rev, revtype, entity, entity_id, sebelum, sesudah, waktu)
     VALUES (3, 0, 'penerimaan_piutang', '2', NULL,
             '{"nomor_dokumen":"REV-KWT-1","nilai":"-300000.00","pembalik_dari_id":"1"}', now());
INSERT INTO a22__audit.revinfo (revtstmp, tenant_id, tenant_code, user_id, role, action, waktu)
     VALUES (4, 1, 'T1', 'lain', 'ADMIN', 'penagihan_balik', now());
INSERT INTO a22__audit.audit_baris (rev, revtype, entity, entity_id, sebelum, sesudah, waktu)
     VALUES (4, 2, 'penerimaan_piutang', '1',
             '{"nomor_dokumen":"KWT-1","nilai":"300000.00","status":"AKTIF"}',
             '{"nomor_dokumen":"KWT-1","nilai":"300000.00","status":"DIBALIK"}', now());

-- Peristiwa 3: SPJ dibuat.
INSERT INTO a22__audit.revinfo (revtstmp, tenant_id, tenant_code, user_id, role, action, waktu)
     VALUES (5, 1, 'T1', 'uji', 'OWNER', 'spj_simpan', now());
INSERT INTO a22__audit.audit_baris (rev, revtype, entity, entity_id, sebelum, sesudah, waktu)
     VALUES (5, 0, 'surat_perintah_sales', '1', NULL,
             '{"nomor_dokumen":"SPJ-1","salesperson_id":"5","status":"DRAF"}', now());

\echo ''
\echo '== BLOK 1: keempat entitas transaksional punya jejak =============================='

SELECT b.entity, COUNT(*) AS revisi,
       CASE WHEN COUNT(*) > 0 THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM a22__audit.audit_baris b
 WHERE b.entity IN ('sales_order','piutang_customer','penerimaan_piutang',
                    'surat_perintah_sales')
 GROUP BY b.entity ORDER BY b.entity;

\echo ''
\echo '== BLOK 2: satu peristiwa, dua dokumen -- pembalikan penagihan ===================='
\echo '   Pembaliknya lahir (ADD) dan yang dibalik ditutup (DEL). Keduanya harus ada.'

SELECT (SELECT COUNT(*) FROM a22__audit.audit_baris
         WHERE entity = 'penerimaan_piutang' AND revtype = 0) AS "pembalik lahir (ADD)",
       (SELECT COUNT(*) FROM a22__audit.audit_baris
         WHERE entity = 'penerimaan_piutang' AND revtype = 2) AS "yang dibalik ditutup (DEL)",
       CASE WHEN (SELECT COUNT(*) FROM a22__audit.audit_baris
                   WHERE entity = 'penerimaan_piutang' AND revtype = 0) = 1
             AND (SELECT COUNT(*) FROM a22__audit.audit_baris
                   WHERE entity = 'penerimaan_piutang' AND revtype = 2) = 1
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo '   Pemfakturan pun menyentuh dua entitas berbeda dalam satu aksi.'

SELECT COUNT(DISTINCT b.entity) AS "entitas tersentuh",
       CASE WHEN COUNT(DISTINCT b.entity) = 2 THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM a22__audit.audit_baris b JOIN a22__audit.revinfo r ON b.rev = r.rev
 WHERE r.action = 'sales_order_faktur';

\echo ''
\echo '== BLOK 3 (PENJAGA): muatan piutang TIDAK memuat kolom ringkasan =================='
\echo '   terbayar/sisa diturunkan dari alokasi. Kalau ikut ke muatan, tiap alokasi akan'
\echo '   tampak sebagai perubahan pada barisnya padahal barisnya tidak disunting.'
\echo '   Angka "sisa tersimpan" dan "sisa turunan" di bawah HARUS berbeda; kalau sama,'
\echo '   contoh ini tidak membedakan apa pun.'

INSERT INTO a22.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id, nilai,
                                            dibuat_pada, oleh)
     VALUES (1, 1, 300000, now(), 'uji');

SELECT (SELECT sisa FROM a22.piutang_customer WHERE id = 1) AS "sisa tersimpan",
       (SELECT nilai - COALESCE((SELECT SUM(a.nilai)
                FROM a22.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = 1),0)
          FROM a22.piutang_customer WHERE id = 1) AS "sisa turunan",
       (b.sesudah::json->>'sisa') IS NULL      AS "muatan tanpa sisa",
       (b.sesudah::json->>'terbayar') IS NULL  AS "muatan tanpa terbayar",
       CASE WHEN (b.sesudah::json->>'sisa') IS NULL
             AND (b.sesudah::json->>'terbayar') IS NULL
             AND (SELECT sisa FROM a22.piutang_customer WHERE id = 1)
                 <> (SELECT nilai - COALESCE((SELECT SUM(a.nilai)
                        FROM a22.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = 1),0)
                       FROM a22.piutang_customer WHERE id = 1)
            THEN 'LULUS (ringkasan memang basi, dan memang tidak ikut)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil
  FROM a22__audit.audit_baris b WHERE b.entity = 'piutang_customer' AND b.rev = 1;

\echo ''
\echo '== BLOK 4: muatan tidak memuat kolom jejak ========================================'

SELECT COUNT(*) AS "baris bermuatan jejak",
       CASE WHEN COUNT(*) = 0 THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM a22__audit.audit_baris b
 WHERE b.sesudah IS NOT NULL
   AND ((b.sesudah::json->>'oleh') IS NOT NULL
     OR (b.sesudah::json->>'dibuat_pada') IS NOT NULL
     OR (b.sesudah::json->>'tanggal_dirubah') IS NOT NULL);

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
