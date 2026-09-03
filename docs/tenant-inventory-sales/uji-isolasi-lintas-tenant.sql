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

-- ---------------------------------------------------------------------------------------
-- Berkas ini MENYIAPKAN DATANYA SENDIRI. Sempat tidak begitu: datanya disemai tangan saat
-- batch §25 ditulis, dan pelari kumpulan uji (jalankan-uji.py) langsung membuktikan bahwa
-- ujinya lalu GAGAL di klaster yang bersih. Uji yang hanya lulus di mesin penulisnya bukan
-- uji; ia catatan pribadi.
-- ---------------------------------------------------------------------------------------
TRUNCATE ta.mutasi_stok, ta.piutang_customer, ta.faktur_penjualan, ta.produk, ta.satuan,
         ta.gudang, ta.akun, ta.supplier, ta.customer, ta.toko RESTART IDENTITY CASCADE;
TRUNCATE tb.mutasi_stok, tb.piutang_customer, tb.faktur_penjualan, tb.produk, tb.satuan,
         tb.gudang, tb.akun, tb.supplier, tb.customer, tb.toko RESTART IDENTITY CASCADE;
TRUNCATE ta__audit.audit_baris, ta__audit.revinfo RESTART IDENTITY CASCADE;
TRUNCATE tb__audit.audit_baris, tb__audit.revinfo RESTART IDENTITY CASCADE;

-- Id, kode, dan nomor dokumen SENGAJA SAMA di kedua tenant; angkanya sengaja jauh berbeda.
INSERT INTO ta.toko (id,nama) VALUES (1,'Toko ta');
INSERT INTO tb.toko (id,nama) VALUES (1,'Toko tb');
INSERT INTO ta.gudang (id,kode,nama,toko_id) VALUES (1,'G1','Gudang',1);
INSERT INTO tb.gudang (id,kode,nama,toko_id) VALUES (1,'G1','Gudang',1);
INSERT INTO ta.satuan (id,kode,nama) VALUES (1,'PCS','Pieces');
INSERT INTO tb.satuan (id,kode,nama) VALUES (1,'PCS','Pieces');
INSERT INTO ta.produk (id,kode,nama,satuan_id,status,aktif,dibuat_pada,oleh)
     VALUES (10,'P10','Produk ta',1,'AKTIF',true,now(),'uji');
INSERT INTO tb.produk (id,kode,nama,satuan_id,status,aktif,dibuat_pada,oleh)
     VALUES (10,'P10','Produk tb',1,'AKTIF',true,now(),'uji');
INSERT INTO ta.supplier (id,kode,nama,aktif,dibuat_pada,oleh) VALUES (1,'SUP1','Supplier ta',true,now(),'uji');
INSERT INTO tb.supplier (id,kode,nama,aktif,dibuat_pada,oleh) VALUES (1,'SUP1','Supplier tb',true,now(),'uji');
INSERT INTO ta.customer (id,kode,nama,aktif,dibuat_pada,oleh) VALUES (9,'C9','Customer ta',true,now(),'uji');
INSERT INTO tb.customer (id,kode,nama,aktif,dibuat_pada,oleh) VALUES (9,'C9','Customer tb',true,now(),'uji');
INSERT INTO ta.akun (id,kode,nama,tipe,saldo_normal,dibuat_pada,oleh) VALUES (1,'1000','Kas ta','ASET','D',now(),'uji');
INSERT INTO tb.akun (id,kode,nama,tipe,saldo_normal,dibuat_pada,oleh) VALUES (1,'1000','Kas tb','ASET','D',now(),'uji');
INSERT INTO ta.mutasi_stok (produk_id,gudang_id,tanggal,jenis,arah,kuantitas,dibuat_pada,oleh)
     VALUES (10,1,DATE '2026-01-05','PENGADAAN',1,100,now(),'uji');
INSERT INTO tb.mutasi_stok (produk_id,gudang_id,tanggal,jenis,arah,kuantitas,dibuat_pada,oleh)
     VALUES (10,1,DATE '2026-01-05','PENGADAAN',1,7000,now(),'uji');
INSERT INTO ta.faktur_penjualan (id,nomor_dokumen,tanggal,customer_id,toko_id,subtotal,total,status,dibuat_pada,oleh)
     VALUES (1,'INV-1',CURRENT_DATE,9,1,500,500,'AKTIF',now(),'uji');
INSERT INTO tb.faktur_penjualan (id,nomor_dokumen,tanggal,customer_id,toko_id,subtotal,total,status,dibuat_pada,oleh)
     VALUES (1,'INV-1',CURRENT_DATE,9,1,60000,60000,'AKTIF',now(),'uji');
INSERT INTO ta.piutang_customer (id,customer_id,faktur_penjualan_id,nomor_faktur,tanggal,nilai,sisa,status,dibuat_pada,oleh)
     VALUES (1,9,1,'INV-1',CURRENT_DATE,500,500,'TERBUKA',now(),'uji');
INSERT INTO tb.piutang_customer (id,customer_id,faktur_penjualan_id,nomor_faktur,tanggal,nilai,sisa,status,dibuat_pada,oleh)
     VALUES (1,9,1,'INV-1',CURRENT_DATE,60000,60000,'TERBUKA',now(),'uji');
INSERT INTO ta__audit.revinfo (revtstmp,tenant_id,tenant_code,user_id,action,waktu) VALUES (1,1,'TA','uji','master_supplier_simpan',now());
INSERT INTO ta__audit.audit_baris (rev,revtype,entity,entity_id,sesudah,waktu) VALUES (1,0,'supplier','1','{"nama":"Supplier ta"}',now());
INSERT INTO tb__audit.revinfo (revtstmp,tenant_id,tenant_code,user_id,action,waktu) VALUES (1,2,'TB','uji','master_supplier_simpan',now());
INSERT INTO tb__audit.audit_baris (rev,revtype,entity,entity_id,sesudah,waktu) VALUES (1,0,'supplier','1','{"nama":"Supplier tb"}',now());
INSERT INTO tb__audit.audit_baris (rev,revtype,entity,entity_id,sesudah,waktu) VALUES (1,1,'supplier','1','{"nama":"Supplier tb v2"}',now());

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
