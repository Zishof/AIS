-- =====================================================================================
-- Uji isolasi lintas-tenant (§25) -- inilah yang menjadi alasan seluruh pemindahan ini
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi DUA schema tenant v1-v19 (ta, tb) berikut schema
-- auditnya (ta__audit, tb__audit):
--   psql -h 127.0.0.1 -p 55530 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-isolasi-lintas-tenant.sql
--
-- LATAR
--   Setiap batch sebelumnya menguji SATU tenant: apakah kuerinya setara dengan jalur legacy.
--   Yang tidak pernah diuji ujung-ke-ujung adalah sifat yang menjadi ALASAN seluruh pemindahan
--   ini: dua tenant tidak boleh saling melihat.
--
--   Bahayanya khas dan diam. Kalau isolasinya bocor, kuerinya tetap berjalan dan tetap
--   mengembalikan baris yang tampak wajar -- hanya saja barisnya milik orang lain.
--
-- MENGAPA DATANYA SENGAJA BERTABRAKAN
--   Kedua tenant diisi id, kode, dan nomor dokumen YANG SAMA (produk id 10 kode P10, faktur
--   INV-1, akun 1000, supplier id 1). Tanpa tabrakan itu, uji isolasi bisa lulus hanya karena
--   datanya kebetulan berbeda -- bukan karena schemanya memang memisahkan. Blok 5 penjaganya.
--
-- ANGKANYA DIBUAT JAUH BERBEDA (100 lawan 7.000; 500 lawan 60.000) supaya kebocoran terlihat
-- sebagai angka yang salah, bukan sekadar nama yang tertukar.
--
-- MENGAPA A -> B -> A
--   Kumpulan koneksi (c3p0) mengembalikan koneksi ke kolam beserta keadaannya. Kalau jalur
--   tenant bersandar pada SET search_path, koneksi yang dipakai ulang akan membawa schema
--   tenant sebelumnya. Jalur ini TIDAK memakai search_path -- nama schemanya disambung sebagai
--   literal tervalidasi pada tiap pernyataan -- dan blok 1 sampai 4 membuktikannya dengan
--   membaca A, lalu B, lalu A lagi pada koneksi yang sama.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti isolasinya bocor.
-- =====================================================================================

\pset format aligned

\echo ''
\echo '== BLOK 1: stok -- A, lalu B, lalu A lagi ========================================='
\echo '   Produk ber-id 10 dan kode P10 ada di KEDUA tenant.'

SELECT (SELECT COALESCE(SUM(arah * kuantitas),0) FROM ta.mutasi_stok WHERE produk_id = 10) AS "A",
       (SELECT COALESCE(SUM(arah * kuantitas),0) FROM tb.mutasi_stok WHERE produk_id = 10) AS "B",
       (SELECT COALESCE(SUM(arah * kuantitas),0) FROM ta.mutasi_stok WHERE produk_id = 10) AS "A lagi",
       CASE WHEN (SELECT COALESCE(SUM(arah * kuantitas),0) FROM ta.mutasi_stok WHERE produk_id = 10) = 100
             AND (SELECT COALESCE(SUM(arah * kuantitas),0) FROM tb.mutasi_stok WHERE produk_id = 10) = 7000
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 2: piutang -- nomor faktur INV-1 ada di keduanya =========================='

SELECT (SELECT nilai FROM ta.piutang_customer WHERE nomor_faktur = 'INV-1') AS "A",
       (SELECT nilai FROM tb.piutang_customer WHERE nomor_faktur = 'INV-1') AS "B",
       CASE WHEN (SELECT nilai FROM ta.piutang_customer WHERE nomor_faktur = 'INV-1') = 500
             AND (SELECT nilai FROM tb.piutang_customer WHERE nomor_faktur = 'INV-1') = 60000
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 3: bagan akun -- akun 1000 ada di keduanya ================================'

SELECT (SELECT nama FROM ta.akun WHERE kode = '1000') AS "A",
       (SELECT nama FROM tb.akun WHERE kode = '1000') AS "B",
       CASE WHEN (SELECT nama FROM ta.akun WHERE kode = '1000') = 'Kas ta'
             AND (SELECT nama FROM tb.akun WHERE kode = '1000') = 'Kas tb'
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 4: jejak audit -- schema audit terpisah per tenant ========================'
\echo '   Inilah yang tidak dapat dilakukan Envers: default_schema-nya statis per'
\echo '   SessionFactory, sehingga seluruh tenant akan berbagi satu schema audit.'

SELECT (SELECT COUNT(*) FROM ta__audit.audit_baris WHERE entity = 'supplier' AND entity_id = '1') AS "A",
       (SELECT COUNT(*) FROM tb__audit.audit_baris WHERE entity = 'supplier' AND entity_id = '1') AS "B",
       CASE WHEN (SELECT COUNT(*) FROM ta__audit.audit_baris
                   WHERE entity = 'supplier' AND entity_id = '1') = 1
             AND (SELECT COUNT(*) FROM tb__audit.audit_baris
                   WHERE entity = 'supplier' AND entity_id = '1') = 2
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 5 (PENJAGA): datanya MEMANG bertabrakan ==================================='
\echo '   Kalau kolom "bertabrakan" di bawah bukan "ya", seluruh blok di atas tidak'
\echo '   membuktikan apa pun: isolasinya bisa lulus hanya karena datanya kebetulan beda.'

SELECT (SELECT kode FROM ta.produk WHERE id = 10) AS "kode produk A",
       (SELECT kode FROM tb.produk WHERE id = 10) AS "kode produk B",
       CASE WHEN (SELECT kode FROM ta.produk WHERE id = 10)
                 = (SELECT kode FROM tb.produk WHERE id = 10)
             AND (SELECT nomor_dokumen FROM ta.faktur_penjualan WHERE id = 1)
                 = (SELECT nomor_dokumen FROM tb.faktur_penjualan WHERE id = 1)
            THEN 'ya' ELSE 'tidak' END AS "bertabrakan",
       CASE WHEN (SELECT kode FROM ta.produk WHERE id = 10)
                 = (SELECT kode FROM tb.produk WHERE id = 10)
             AND (SELECT nomor_dokumen FROM ta.faktur_penjualan WHERE id = 1)
                 = (SELECT nomor_dokumen FROM tb.faktur_penjualan WHERE id = 1)
            THEN 'LULUS (tabrakan memang ada, jadi ujinya bermakna)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

\echo ''
\echo '== BLOK 6 (PENJAGA): kueri TANPA nama schema memang mencampur ====================='
\echo '   Inilah yang terjadi bila jalur tenant bersandar pada search_path lalu koneksinya'
\echo '   dipakai ulang. Angka gabungan di bawah HARUS berbeda dari angka kedua tenant --'
\echo '   kalau sama, contoh ini tidak menunjukkan bahaya apa pun.'

SELECT (SELECT COALESCE(SUM(arah * kuantitas),0) FROM ta.mutasi_stok)
     + (SELECT COALESCE(SUM(arah * kuantitas),0) FROM tb.mutasi_stok) AS "bila tercampur",
       CASE WHEN ((SELECT COALESCE(SUM(arah * kuantitas),0) FROM ta.mutasi_stok)
                + (SELECT COALESCE(SUM(arah * kuantitas),0) FROM tb.mutasi_stok)) = 7100
            THEN 'LULUS (campuran memang memberi angka lain: 7100)'
            ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
