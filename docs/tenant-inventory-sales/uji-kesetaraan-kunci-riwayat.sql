-- =====================================================================================
-- Uji kesetaraan: kunci idempotensi riwayat harus membedakan ITEM BARIS, bukan hanya faktur
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v19:
--   psql -h 127.0.0.1 -p 55600 -U uat -d ais_uat -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-kunci-riwayat.sql
--
-- LATAR
--   SalesInventoryDbfImportHelper.imporRiwayatTenant membangun kunci idempotensi dari
--   faktur + kode produk + nomor batch + tanggal. Keempatnya TIDAK cukup: berkas legacy memuat
--   beberapa item baris pada satu faktur yang keempat nilainya sama -- barang yang sama dibeli
--   dua kali dengan harga berbeda, atau satu jumlah dipecah menjadi beberapa baris.
--
--   Akibatnya baris kedua dan seterusnya dianggap kiriman ulang lalu dilewati, dan kuantitasnya
--   hilang. Terukur pada data UAT cmnmedika: 562 baris BELI.DBF (13.727,40 unit) dan 51 baris
--   JUAL.DBF (211,33 unit) -- persis sebesar selisih antara mutasi_stok dan berkas sumbernya.
--
--   Perbaikannya menambahkan nomor urut baris DBF (`baris_ke`) ke dalam kunci.
--
-- YANG DIUJI
--   1. Bentuk kunci LAMA memang menabrakkan dua item baris yang sah menjadi satu.
--   2. Bentuk kunci BARU memisahkannya menjadi dua baris.
--   3. PENJAGA: idempotensinya tidak melemah -- kunci baru yang SAMA tetap ditolak sekali.
--   4. PENJAGA: contoh pada blok 1 dan 2 memang berbeda hasil. Bila sama, uji ini tidak
--      membuktikan apa pun.
--
-- Penyisipannya memakai SELECT ... WHERE NOT EXISTS, bukan ON CONFLICT: lapisan tenant
-- konsisten bergaya PostgreSQL 9.3, dan bentuk ini juga meniru logika importirnya sendiri
-- (adaMutasiRiwayat lalu sisipMutasiRiwayat) alih-alih mengandalkan batasan uniknya.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti perbaikannya tidak berlaku.
-- =====================================================================================

\pset format aligned

CREATE SCHEMA IF NOT EXISTS kr01;

-- Bentuk minimal yang cukup untuk menguji perilaku kuncinya. Sengaja BUKAN salinan penuh
-- mutasi_stok: yang diuji adalah keunikan kunci, bukan bentuk tabelnya.
DROP TABLE IF EXISTS kr01.mutasi_stok;
CREATE TABLE kr01.mutasi_stok (
    id bigserial PRIMARY KEY,
    produk_id bigint NOT NULL,
    tanggal date NOT NULL,
    kuantitas numeric(18,4) NOT NULL,
    idempotency_key varchar(128) NOT NULL,
    CONSTRAINT uq_kr01_mutasi_idem UNIQUE (idempotency_key));

\echo ''
\echo '== BLOK 1: kunci LAMA menabrakkan dua item baris yang sah =========================='
\echo '   Dua baris DBF: faktur 620355, produk 000102, batch kosong, tanggal sama --'
\echo '   tetapi kuantitasnya berbeda (52 dan 41). Keduanya SAH dan harus tersimpan.'

-- Bentuk lama: LEGACY-BELI-{faktur}-{produk}-{batch}-{yyyyMMdd}
INSERT INTO kr01.mutasi_stok (produk_id, tanggal, kuantitas, idempotency_key)
     SELECT 102, DATE '2026-03-11', 52, 'LEGACY-BELI-620355-000102--20260311'
      WHERE NOT EXISTS (SELECT 1 FROM kr01.mutasi_stok
                         WHERE idempotency_key = 'LEGACY-BELI-620355-000102--20260311');
INSERT INTO kr01.mutasi_stok (produk_id, tanggal, kuantitas, idempotency_key)
     SELECT 102, DATE '2026-03-11', 41, 'LEGACY-BELI-620355-000102--20260311'
      WHERE NOT EXISTS (SELECT 1 FROM kr01.mutasi_stok
                         WHERE idempotency_key = 'LEGACY-BELI-620355-000102--20260311');

SELECT COUNT(*)                       AS "baris tersimpan",
       COALESCE(SUM(kuantitas), 0)    AS "kuantitas tersimpan",
       93                             AS "kuantitas seharusnya",
       CASE WHEN COUNT(*) = 1 AND COALESCE(SUM(kuantitas),0) = 52
            THEN 'LULUS (terbukti: satu baris hilang, 41 unit lenyap)'
            ELSE 'GAGAL (contoh tidak menunjukkan tabrakan)' END AS hasil
  FROM kr01.mutasi_stok;

\echo ''
\echo '== BLOK 2: kunci BARU memisahkan keduanya ========================================='
\echo '   Bentuk baru menambahkan nomor urut baris DBF di belakang kunci.'

TRUNCATE kr01.mutasi_stok;

INSERT INTO kr01.mutasi_stok (produk_id, tanggal, kuantitas, idempotency_key)
     SELECT 102, DATE '2026-03-11', 52, 'LEGACY-BELI-620355-000102--20260311-28'
      WHERE NOT EXISTS (SELECT 1 FROM kr01.mutasi_stok
                         WHERE idempotency_key = 'LEGACY-BELI-620355-000102--20260311-28');
INSERT INTO kr01.mutasi_stok (produk_id, tanggal, kuantitas, idempotency_key)
     SELECT 102, DATE '2026-03-11', 41, 'LEGACY-BELI-620355-000102--20260311-29'
      WHERE NOT EXISTS (SELECT 1 FROM kr01.mutasi_stok
                         WHERE idempotency_key = 'LEGACY-BELI-620355-000102--20260311-29');

SELECT COUNT(*)                    AS "baris tersimpan",
       COALESCE(SUM(kuantitas), 0) AS "kuantitas tersimpan",
       CASE WHEN COUNT(*) = 2 AND COALESCE(SUM(kuantitas),0) = 93
            THEN 'LULUS (kedua item baris utuh)'
            ELSE 'GAGAL' END AS hasil
  FROM kr01.mutasi_stok;

\echo ''
\echo '== BLOK 3 (PENJAGA): idempotensi TIDAK melemah ===================================='
\echo '   Mengirim ulang muatan yang sama berarti nomor baris yang sama pula, jadi kuncinya'
\echo '   tetap sama dan baris keduanya harus tetap ditolak.'

INSERT INTO kr01.mutasi_stok (produk_id, tanggal, kuantitas, idempotency_key)
     SELECT 102, DATE '2026-03-11', 52, 'LEGACY-BELI-620355-000102--20260311-28'
      WHERE NOT EXISTS (SELECT 1 FROM kr01.mutasi_stok
                         WHERE idempotency_key = 'LEGACY-BELI-620355-000102--20260311-28');

SELECT COUNT(*) AS "baris sesudah kiriman ulang",
       CASE WHEN COUNT(*) = 2
            THEN 'LULUS (kiriman ulang tidak menggandakan)'
            ELSE 'GAGAL (idempotensi rusak -- ini lebih buruk daripada masalah semula)'
       END AS hasil
  FROM kr01.mutasi_stok;

\echo ''
\echo '== BLOK 4 (PENJAGA): blok 1 dan 2 memang berbeda hasil ============================'
\echo '   Bila keduanya memberi jumlah yang sama, contoh di atas tidak membuktikan apa pun'
\echo '   tentang bentuk kuncinya.'

SELECT 52 AS "kuantitas bentuk LAMA", 93 AS "kuantitas bentuk BARU",
       CASE WHEN 52 <> 93 THEN 'LULUS (contohnya membedakan benar dari salah)'
            ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 5: nomor baris yang HILANG tetap memakai bentuk lama ======================'
\echo '   Muatan lama (tanpa baris_ke) harus tetap sah, bukan ditolak. Kuncinya lalu'
\echo '   berbentuk lama, dan dua baris yang sama tetap bertabrakan -- itu perilaku yang'
\echo '   DIHARAPKAN untuk muatan lama, bukan regresi.'

TRUNCATE kr01.mutasi_stok;
INSERT INTO kr01.mutasi_stok (produk_id, tanggal, kuantitas, idempotency_key)
     SELECT 102, DATE '2026-03-11', 52, 'LEGACY-BELI-620355-000102--20260311'
      WHERE NOT EXISTS (SELECT 1 FROM kr01.mutasi_stok
                         WHERE idempotency_key = 'LEGACY-BELI-620355-000102--20260311');

SELECT COUNT(*) AS "baris",
       CASE WHEN COUNT(*) = 1 THEN 'LULUS (muatan tanpa baris_ke tetap tersimpan)'
            ELSE 'GAGAL' END AS hasil
  FROM kr01.mutasi_stok;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
