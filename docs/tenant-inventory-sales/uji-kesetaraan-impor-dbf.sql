-- =====================================================================================
-- Uji kesetaraan §23: impor DBF ke schema tenant -- ISI BILA KOSONG, dan idempotensinya
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v19:
--   psql -h 127.0.0.1 -p 55526 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-impor-dbf.sql
--
-- Berkas ini mengandaikan schema tenant bernama a22.
--
-- LATAR
--   Impor legacy menyimpan lewat entitas Hibernate yang menyematkan schema-nya pada anotasi.
--   Dijalankan untuk pemakai tenant, master miliknya mendarat di schema BERSAMA sementara
--   schema tenantnya tetap kosong -- impor melapor sukses, layarnya tidak menampilkan apa pun.
--   Karena itu jalur tenant menulis lewat SQL asli.
--
-- EMPAT HAL YANG DIUJI
--   1. ISI BILA KOSONG: impor tidak pernah menimpa nilai yang sudah terisi. Berkas DBF adalah
--      cerminan data LAMA; menimpanya akan mengembalikan ejaan dan alamat lama setiap kali
--      impor diulang.
--   2. Blok 2 penjaganya: nilai yang KOSONG memang terisi. Kalau tidak, blok 1 lulus hanya
--      karena tidak ada yang pernah ditulis.
--   3. Saldo awal masuk sebagai MUTASI, dan mengulang impor tidak melipatgandakannya.
--   4. Harga umum (customer_id NULL) tidak berlipat pada impor ulang -- penjaganya memakai
--      IS NOT DISTINCT FROM, sebab perbandingan = terhadap NULL selalu tidak-diketahui.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\pset format aligned

TRUNCATE a22.harga_jual_customer, a22.harga_beli_supplier, a22.mutasi_stok,
         a22.customer_profile, a22.supplier_profile, a22.sales_assignment,
         a22.produk, a22.satuan, a22.salesperson, a22.supplier, a22.customer
      RESTART IDENTITY CASCADE;
INSERT INTO a22.toko (id, nama) VALUES (1, 'Toko') ON CONFLICT DO NOTHING;
INSERT INTO a22.gudang (id, kode, nama, toko_id) VALUES (1, 'G1', 'Gudang', 1)
     ON CONFLICT DO NOTHING;

-- Supplier yang SUDAH dirapikan di layar: namanya ejaan baru, teleponnya sudah diisi.
INSERT INTO a22.supplier (id, kode, nama, aktif, dibuat_pada, oleh)
     VALUES (1, 'SUP1', 'PT Sumber Rejeki (rapi)', true, now(), 'uji');
INSERT INTO a22.supplier_profile (supplier_id, alamat1, telp, syarat_bayar_hari, dibuat_pada, oleh)
     VALUES (1, 'Jl. Baru No. 1', '021-999', 30, now(), 'uji');
-- Supplier yang baru saja lahir tanpa nama/telepon.
INSERT INTO a22.supplier (id, kode, nama, aktif, dibuat_pada, oleh)
     VALUES (2, 'SUP2', '', true, now(), 'uji');
INSERT INTO a22.supplier_profile (supplier_id, dibuat_pada, oleh) VALUES (2, now(), 'uji');

\echo ''
\echo '== BLOK 1: ISI BILA KOSONG tidak menimpa yang sudah terisi ========================'
\echo '   Ini pernyataan yang sama dengan isiNamaMitra() dan isiProfilSupplier().'

UPDATE a22.supplier SET nama = 'SUMBER REJEKI (DBF LAMA)', tanggal_dirubah = now()
 WHERE id = 1 AND (nama IS NULL OR TRIM(nama) = '');
UPDATE a22.supplier_profile SET
       alamat1 = COALESCE(NULLIF(TRIM(alamat1),''), 'Jl. Lama No. 9'),
       telp = COALESCE(NULLIF(TRIM(telp),''), '021-111'),
       syarat_bayar_hari = CASE WHEN COALESCE(syarat_bayar_hari,0) = 0 THEN 7
                                ELSE syarat_bayar_hari END,
       tanggal_dirubah = now() WHERE supplier_id = 1;

SELECT s.nama, p.alamat1, p.telp, p.syarat_bayar_hari,
       CASE WHEN s.nama = 'PT Sumber Rejeki (rapi)' AND p.alamat1 = 'Jl. Baru No. 1'
             AND p.telp = '021-999' AND p.syarat_bayar_hari = 30
            THEN 'LULUS (yang rapi tidak ditimpa)' ELSE 'GAGAL' END AS hasil
  FROM a22.supplier s JOIN a22.supplier_profile p ON p.supplier_id = s.id WHERE s.id = 1;

\echo ''
\echo '== BLOK 2 (PENJAGA): yang KOSONG memang terisi ===================================='
\echo '   Tanpa blok ini, blok 1 bisa lulus hanya karena tidak ada yang pernah ditulis.'

UPDATE a22.supplier SET nama = 'SUMBER MAKMUR (DBF)', tanggal_dirubah = now()
 WHERE id = 2 AND (nama IS NULL OR TRIM(nama) = '');
UPDATE a22.supplier_profile SET
       alamat1 = COALESCE(NULLIF(TRIM(alamat1),''), 'Jl. Lama No. 9'),
       telp = COALESCE(NULLIF(TRIM(telp),''), '021-111'),
       syarat_bayar_hari = CASE WHEN COALESCE(syarat_bayar_hari,0) = 0 THEN 7
                                ELSE syarat_bayar_hari END,
       tanggal_dirubah = now() WHERE supplier_id = 2;

SELECT s.nama, p.alamat1, p.telp, p.syarat_bayar_hari,
       CASE WHEN s.nama = 'SUMBER MAKMUR (DBF)' AND p.alamat1 = 'Jl. Lama No. 9'
             AND p.telp = '021-111' AND p.syarat_bayar_hari = 7
            THEN 'LULUS (yang kosong memang terisi)' ELSE 'GAGAL' END AS hasil
  FROM a22.supplier s JOIN a22.supplier_profile p ON p.supplier_id = s.id WHERE s.id = 2;

\echo ''
\echo '== BLOK 3: saldo awal = MUTASI, dan impor ulang tidak melipatgandakannya =========='
\echo '   Model tenant tidak punya kolom stok; saldo mana pun adalah penjumlahan mutasi.'
\echo '   Pernyataan di bawah dijalankan DUA KALI, persis seperti berkas DBF yang sama'
\echo '   diimpor ulang.'

INSERT INTO a22.produk (id, kode, nama, status, aktif, dibuat_pada, oleh)
     VALUES (10, 'P10', 'Produk Uji', 'AKTIF', true, now(), 'uji');

INSERT INTO a22.mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah, kuantitas,
                             nomor_dokumen, dibuat_pada, oleh)
     SELECT 10, 1, CURRENT_DATE, 'OPNAME', 1, 250, 'MIGRASI-DBF', now(), 'uji'
      WHERE NOT EXISTS (SELECT 1 FROM a22.mutasi_stok m
                         WHERE m.produk_id = 10 AND m.nomor_dokumen = 'MIGRASI-DBF');
INSERT INTO a22.mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah, kuantitas,
                             nomor_dokumen, dibuat_pada, oleh)
     SELECT 10, 1, CURRENT_DATE, 'OPNAME', 1, 250, 'MIGRASI-DBF', now(), 'uji'
      WHERE NOT EXISTS (SELECT 1 FROM a22.mutasi_stok m
                         WHERE m.produk_id = 10 AND m.nomor_dokumen = 'MIGRASI-DBF');

SELECT COUNT(*) AS "baris mutasi", COALESCE(SUM(m.arah * m.kuantitas),0) AS "saldo",
       CASE WHEN COUNT(*) = 1 AND COALESCE(SUM(m.arah * m.kuantitas),0) = 250
            THEN 'LULUS (dijalankan dua kali, tetap satu baris)' ELSE 'GAGAL' END AS hasil
  FROM a22.mutasi_stok m WHERE m.produk_id = 10 AND m.nomor_dokumen = 'MIGRASI-DBF';

\echo ''
\echo '== BLOK 4 (PENJAGA): harga umum ber-customer NULL tidak berlipat =================='
\echo '   Penjaganya HARUS memakai IS NOT DISTINCT FROM. Dengan = biasa, perbandingan'
\echo '   terhadap NULL selalu tidak-diketahui dan penjaganya lolos setiap kali.'

INSERT INTO a22.harga_jual_customer (customer_id, produk_id, berlaku_dari, harga, aktif,
                                     dibuat_pada, oleh)
     SELECT NULL, 10, DATE '2026-01-01', 12500, true, now(), 'uji'
      WHERE NOT EXISTS (SELECT 1 FROM a22.harga_jual_customer h
                         WHERE h.customer_id IS NOT DISTINCT FROM NULL AND h.produk_id = 10
                           AND h.berlaku_dari = DATE '2026-01-01');
INSERT INTO a22.harga_jual_customer (customer_id, produk_id, berlaku_dari, harga, aktif,
                                     dibuat_pada, oleh)
     SELECT NULL, 10, DATE '2026-01-01', 12500, true, now(), 'uji'
      WHERE NOT EXISTS (SELECT 1 FROM a22.harga_jual_customer h
                         WHERE h.customer_id IS NOT DISTINCT FROM NULL AND h.produk_id = 10
                           AND h.berlaku_dari = DATE '2026-01-01');

SELECT (SELECT COUNT(*) FROM a22.harga_jual_customer
         WHERE produk_id = 10 AND customer_id IS NULL) AS "dgn IS NOT DISTINCT FROM",
       (SELECT COUNT(*) FROM a22.harga_jual_customer h
         WHERE h.customer_id = NULL AND h.produk_id = 10) AS "yang cocok dgn = NULL",
       CASE WHEN (SELECT COUNT(*) FROM a22.harga_jual_customer
                   WHERE produk_id = 10 AND customer_id IS NULL) = 1
             AND (SELECT COUNT(*) FROM a22.harga_jual_customer h
                   WHERE h.customer_id = NULL AND h.produk_id = 10) = 0
            THEN 'LULUS (= NULL memang tidak pernah cocok)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
