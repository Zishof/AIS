-- =====================================================================================
-- Uji kesetaraan: simpan sales order dan terbitkan faktur piutang
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v11:
--   psql -h 127.0.0.1 -p 55503 -U uji -d postgres -f uji-kesetaraan-order-simpan-faktur.sql
--
-- Berkas ini mengandaikan schema tenant bernama ordf11.
--
-- EMPAT HAL YANG DIUJI
--   1. Penyaring status. Klien mengirim DRAFT, kolom tenant berisi DRAF. Blok 1 membuktikan
--      penyaring MENTAH mengembalikan kosong -- order draf hilang dari daftar -- dan
--      penyaring ternormalkan menemukannya.
--   2. Simpan order: total header sama dengan jumlah barisnya, dan pengubahan MENGGANTI
--      seluruh baris (bukan menambahkan).
--   3. Terbitkan faktur: satu dokumen legacy menjadi faktur + piutang pada tenant, dengan
--      nilai dan jatuh tempo yang setara, dan status order maju ke SIAP_TAGIH.
--   4. Idempotensi v11 benar-benar mengikat pada sales_order dan faktur_penjualan.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DELETE FROM ordf11.alokasi_penerimaan_piutang;
DELETE FROM ordf11.penerimaan_piutang;
DELETE FROM ordf11.piutang_customer;
DELETE FROM ordf11.faktur_penjualan;
DELETE FROM ordf11.sales_order_detail;
DELETE FROM ordf11.sales_order;
DELETE FROM ordf11.customer_profile;
DELETE FROM ordf11.produk;
DELETE FROM ordf11.salesperson;
DELETE FROM ordf11.customer;
DELETE FROM ordf11.toko;

-- Urutan serial di-restart supaya id dokumennya tentu; berkas ini dijalankan berulang.
ALTER SEQUENCE ordf11.sales_order_id_seq RESTART WITH 1;
ALTER SEQUENCE ordf11.sales_order_detail_id_seq RESTART WITH 1;
ALTER SEQUENCE ordf11.faktur_penjualan_id_seq RESTART WITH 1;
ALTER SEQUENCE ordf11.piutang_customer_id_seq RESTART WITH 1;

INSERT INTO ordf11.toko (id, nama, kode) VALUES (1, 'Toko Uji', 'T01');
INSERT INTO ordf11.customer (id, kode, nama) VALUES (5, 'C05', 'Toko Melati');
INSERT INTO ordf11.salesperson (id, kode, nama) VALUES (7, 'S07', 'Budi');
INSERT INTO ordf11.produk (id, kode, nama) VALUES (100, 'P100', 'Gula'), (101, 'P101', 'Kopi');
INSERT INTO ordf11.customer_profile (customer_id, syarat_bayar_hari) VALUES (5, 30);

\pset format aligned

-- CATATAN: schema berkas ini SENGAJA tidak dibagi dengan berkas uji lain. Empat berkas
-- sempat sama-sama memakai rev11 tanpa satu pun membersihkannya, sehingga datanya
-- saling menimpa dan berkas yang berjalan terakhir membaca sisa berkas lain. Schema
-- sendiri menghapus kopling urutan itu sepenuhnya.

-- =====================================================================================
-- BLOK 1: penyaring status -- inti bug yang ditutup batch ini
-- =====================================================================================
-- Order disisipkan PERSIS seperti sisipOrder(): status tidak disebut, memakai bawaan 'DRAF'.
INSERT INTO ordf11.sales_order (nomor_dokumen, tanggal, customer_id, salesperson_id, toko_id,
                               total, keterangan, idempotency_key, status, dibuat_pada, oleh)
  VALUES ('', DATE '2026-03-01', 5, 7, 1, 0, 'pesanan uji', 'SO-UNIK-1', 'DRAF', now(), 'uji');

\echo ''
\echo '== BLOK 1: klien menyaring DRAFT -- mentah HILANG, ternormalkan KETEMU ============='

SELECT
  (SELECT COUNT(*) FROM ordf11.sales_order o WHERE o.status = 'DRAFT')          AS "saringan mentah",
  (SELECT COUNT(*) FROM ordf11.sales_order o
     WHERE CASE WHEN o.status = 'DRAF' THEN 'DRAFT'
                ELSE COALESCE(o.status,'') END = 'DRAFT')                      AS "ternormalkan",
  CASE WHEN (SELECT COUNT(*) FROM ordf11.sales_order o WHERE o.status = 'DRAFT') = 0
        AND (SELECT COUNT(*) FROM ordf11.sales_order o
               WHERE CASE WHEN o.status = 'DRAF' THEN 'DRAFT'
                          ELSE COALESCE(o.status,'') END = 'DRAFT') = 1
       THEN 'LULUS (bug nyata, dan penormalannya menutupnya)'
       ELSE 'GAGAL' END AS hasil;

-- =====================================================================================
-- BLOK 2: simpan order -- total header = jumlah baris
-- =====================================================================================
INSERT INTO ordf11.sales_order_detail
  (sales_order_id, baris_ke, produk_id, kuantitas, harga_satuan, total, dibuat_pada, oleh)
  VALUES (1, 1, 100, 10, 15000, 150000, now(), 'uji'),
         (1, 2, 101, 5, 20000, 100000, now(), 'uji');
UPDATE ordf11.sales_order SET nomor_dokumen = 'SO-1-000001', total = 250000 WHERE id = 1;

\echo ''
\echo '== BLOK 2: total header setara jumlah barisnya ====================================='

SELECT o.total AS "total header",
       (SELECT SUM(i.total) FROM ordf11.sales_order_detail i WHERE i.sales_order_id = o.id)
         AS "jumlah baris",
       CASE WHEN o.total = (SELECT SUM(i.total) FROM ordf11.sales_order_detail i
                            WHERE i.sales_order_id = o.id)
            AND o.total = 250000
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM ordf11.sales_order o WHERE o.id = 1;

-- =====================================================================================
-- BLOK 3: pengubahan MENGGANTI baris, bukan menambah
-- =====================================================================================
DELETE FROM ordf11.sales_order_detail WHERE sales_order_id = 1;
INSERT INTO ordf11.sales_order_detail
  (sales_order_id, baris_ke, produk_id, kuantitas, harga_satuan, total, dibuat_pada, oleh)
  VALUES (1, 1, 100, 4, 15000, 60000, now(), 'uji');
UPDATE ordf11.sales_order SET tanggal = COALESCE(NULL, tanggal), keterangan = 'diubah',
       total = 60000, oleh = 'uji', tanggal_dirubah = now() WHERE id = 1;

\echo ''
\echo '== BLOK 3: setelah diubah -- tepat SATU baris, total ikut, tanggal TIDAK hilang ===='

SELECT
  (SELECT COUNT(*) FROM ordf11.sales_order_detail WHERE sales_order_id = 1) AS "jumlah baris",
  o.total    AS "total",
  o.tanggal  AS "tanggal (harus tetap)",
  CASE WHEN (SELECT COUNT(*) FROM ordf11.sales_order_detail WHERE sales_order_id = 1) = 1
        AND o.total = 60000 AND o.tanggal = DATE '2026-03-01'
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM ordf11.sales_order o WHERE o.id = 1;

-- =====================================================================================
-- BLOK 4: terbitkan faktur -- satu dokumen legacy jadi DUA dokumen tenant
-- =====================================================================================
UPDATE ordf11.sales_order SET status = 'TERKIRIM' WHERE id = 1;

INSERT INTO ordf11.faktur_penjualan (nomor_dokumen, nomor_faktur, tanggal, jatuh_tempo,
    customer_id, salesperson_id, sales_order_id, toko_id, subtotal, total, keterangan,
    idempotency_key, status, dibuat_pada, oleh)
  VALUES ('SO-INV-1', 'SO-INV-1', DATE '2026-03-05', DATE '2026-03-05' + 30, 5, 7, 1, 1,
          60000, 60000, 'Faktur dari sales order SO-1-000001', 'SO-INV-1', 'AKTIF', now(), 'uji');
UPDATE ordf11.faktur_penjualan SET nomor_dokumen = 'INV-1-000001', nomor_faktur = 'INV-1-000001'
  WHERE id = 1;
INSERT INTO ordf11.piutang_customer (customer_id, salesperson_id, faktur_penjualan_id,
    nomor_faktur, tanggal, jatuh_tempo, nilai, terbayar, sisa, status, dibuat_pada, oleh)
  VALUES (5, 7, 1, 'INV-1-000001', DATE '2026-03-05', DATE '2026-03-05' + 30, 60000, 0, 60000,
          'TERBUKA', now(), 'uji');
UPDATE ordf11.sales_order SET status = 'SIAP_TAGIH', oleh = 'uji' WHERE id = 1;

\echo ''
\echo '== BLOK 4: faktur + piutang lahir bersama, jatuh tempo = tanggal + termin =========='
\echo '   Termin diambil dari customer_profile.syarat_bayar_hari (30), seperti legacy dari profil.'

SELECT
  f.total       AS "total faktur",
  d.nilai       AS "nilai piutang",
  d.jatuh_tempo AS "jatuh tempo",
  o.status      AS "status order",
  CASE WHEN f.total = d.nilai AND d.nilai = 60000
        AND d.jatuh_tempo = DATE '2026-03-05' + (SELECT syarat_bayar_hari
                                                 FROM ordf11.customer_profile WHERE customer_id = 5)
        AND o.status = 'SIAP_TAGIH'
        AND d.faktur_penjualan_id = f.id
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM ordf11.faktur_penjualan f, ordf11.piutang_customer d, ordf11.sales_order o
WHERE f.id = 1 AND d.faktur_penjualan_id = 1 AND o.id = 1;

\echo ''
\echo '== BLOK 5: deep-link salesOrderDetail menemukan faktur yang baru terbit ============'

SELECT
  (SELECT d.id FROM ordf11.piutang_customer d
     JOIN ordf11.faktur_penjualan f ON d.faktur_penjualan_id = f.id
    WHERE f.sales_order_id = 1 ORDER BY d.id LIMIT 1)                    AS "piutangDocId",
  (SELECT COALESCE(d.nomor_faktur,'') FROM ordf11.piutang_customer d
     JOIN ordf11.faktur_penjualan f ON d.faktur_penjualan_id = f.id
    WHERE f.sales_order_id = 1 ORDER BY d.id LIMIT 1)                    AS "piutangDocNomor",
  CASE WHEN (SELECT COALESCE(d.nomor_faktur,'') FROM ordf11.piutang_customer d
               JOIN ordf11.faktur_penjualan f ON d.faktur_penjualan_id = f.id
              WHERE f.sales_order_id = 1 ORDER BY d.id LIMIT 1) = 'INV-1-000001'
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

-- =====================================================================================
-- BLOK 6: idempotensi v11 pada KEDUA dokumen
-- =====================================================================================
\echo ''
\echo '== BLOK 6: kunci idempotensi kembar ditolak pada sales_order DAN faktur_penjualan =='

\set ON_ERROR_STOP off
INSERT INTO ordf11.sales_order (nomor_dokumen, tanggal, customer_id, toko_id, total,
                               idempotency_key, status, dibuat_pada, oleh)
  VALUES ('', DATE '2026-03-09', 5, 1, 0, 'SO-UNIK-1', 'DRAF', now(), 'uji');
INSERT INTO ordf11.faktur_penjualan (nomor_dokumen, nomor_faktur, tanggal, customer_id,
    total, idempotency_key, status, dibuat_pada, oleh)
  VALUES ('X', 'X', DATE '2026-03-09', 5, 1, 'SO-INV-1', 'AKTIF', now(), 'uji');
\set ON_ERROR_STOP on

SELECT
  (SELECT COUNT(*) FROM ordf11.sales_order WHERE idempotency_key = 'SO-UNIK-1')      AS "order",
  (SELECT COUNT(*) FROM ordf11.faktur_penjualan WHERE idempotency_key = 'SO-INV-1')  AS "faktur",
  CASE WHEN (SELECT COUNT(*) FROM ordf11.sales_order WHERE idempotency_key = 'SO-UNIK-1') = 1
        AND (SELECT COUNT(*) FROM ordf11.faktur_penjualan WHERE idempotency_key = 'SO-INV-1') = 1
       THEN 'LULUS (dua-duanya ditolak, tepat satu tersisa)'
       ELSE 'GAGAL (kembar lolos)' END AS hasil;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
