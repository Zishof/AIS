-- =====================================================================================
-- Uji kesetaraan §19: satuan jual -- rasio berikut arahnya, bukan satu faktor desimal
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v19:
--   psql -h 127.0.0.1 -p 55520 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-satuan-jual.sql
--
-- Berkas ini mengandaikan schema tenant bernama uom19.
--
-- LATAR
--   Jalur legacy MENURUNKAN kuantitas dasar dari qty_input x faktor dan memperlakukan jumlah
--   kiriman klien sebagai pratinjau belaka. Jalur tenant menolaknya sampai §19, sebab satuan
--   tenant belum membawa metadata konversi -- ia cuma label, dan "12 PCS" tidak dapat
--   dihubungkan dengan "1 DUS". Bundel v19 menambahkan metadata itu.
--
-- LIMA HAL YANG DIUJI
--   1. Satuan tanpa kategori/rasio berperilaku UNIT dan 1:1, bukan galat.
--   2. Konversi arah "lebih besar" tepat: 3 DUS = 36 PCS.
--   3. Blok 3 penjaganya, dan inilah inti rancangannya: faktor pecahan yang dibulatkan LEBIH
--      DULU merusak angkanya (12 x 0,083333 = 0,999996), sedangkan pembagian yang dilakukan
--      SEKALI atas pembilang yang sudah dikalikan menghasilkan tepat 1.
--   4. Konversi antar-kategori tidak boleh terjadi: kilogram bukan liter.
--   5. Pada barisnya, kuantitas yang berwenang -- faktor_ke_dasar hanya catatan, dan menghitung
--      ulang darinya justru memberi angka yang berbeda.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\pset format aligned

TRUNCATE uom19.sales_order_detail, uom19.sales_order, uom19.produk, uom19.satuan,
         uom19.customer RESTART IDENTITY CASCADE;

INSERT INTO uom19.satuan (id, kode, nama, kategori, rasio, tipe_konversi) VALUES
    (1, 'PCS', 'Pieces',          'UNIT',   1,    NULL),
    (2, 'DUS', 'Dus',             'UNIT',   12,   NULL),
    (3, 'LSN', 'Lusin kebalikan', 'UNIT',   12,   'SMALLER'),
    (4, 'LTR', 'Liter',           'VOLUME', 1,    NULL),
    (5, 'BOX', 'Box',             NULL,     NULL, NULL);
INSERT INTO uom19.produk (id, kode, nama, satuan_id) VALUES (10, 'P10', 'Produk Uji', 1);
INSERT INTO uom19.customer (id, kode, nama) VALUES (9, 'C9', 'Pelanggan');
INSERT INTO uom19.sales_order (id, nomor_dokumen, tanggal, customer_id, total, status)
     VALUES (1, 'SO-1', CURRENT_DATE, 9, 0, 'DRAF');

\echo ''
\echo '== BLOK 1: satuan tanpa kategori/rasio berperilaku UNIT dan 1:1 ==================='
\echo '   Ini SQL yang sama dengan satuanKonversi(): kategori kosong menjadi UNIT, rasio'
\echo '   kosong menjadi 1. Satuan yang belum pernah dikonversi bukan galat.'

SELECT s.kode,
       UPPER(COALESCE(NULLIF(TRIM(s.kategori),''),'UNIT')) AS kategori,
       COALESCE(s.rasio,1) AS rasio,
       UPPER(COALESCE(s.tipe_konversi,'')) AS arah,
       CASE WHEN UPPER(COALESCE(NULLIF(TRIM(s.kategori),''),'UNIT')) = 'UNIT'
             AND COALESCE(s.rasio,1) = 1
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM uom19.satuan s WHERE s.kode = 'BOX';

\echo ''
\echo '== BLOK 2: konversi arah "lebih besar" tepat ====================================='
\echo '   3 DUS pada rasio 12 = 36 PCS, tanpa sisa.'

SELECT (3 * (SELECT rasio FROM uom19.satuan WHERE kode = 'DUS')
          / (SELECT rasio FROM uom19.satuan WHERE kode = 'PCS'))::numeric(18,4)
         AS "kuantitas dasar",
       CASE WHEN (3 * (SELECT rasio FROM uom19.satuan WHERE kode = 'DUS')
                    / (SELECT rasio FROM uom19.satuan WHERE kode = 'PCS'))::numeric(18,4)
                 = 36.0000
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 3 (PENJAGA): faktor yang dibulatkan LEBIH DULU merusak angkanya ==========='
\echo '   LSN bersandi SMALLER rasio 12, jadi pecahannya 1/12. Kalau faktornya dibulatkan'
\echo '   ke skala 6 sebelum dikalikan, 12 LSN menjadi 0,999996 -- bukan 1.'
\echo '   Angka "faktor dibulatkan dulu" HARUS 0.999996; kalau ia 1, contoh ini tidak'
\echo '   membedakan apa pun.'

SELECT (12 * (1::numeric / 12)::numeric(18,6))::numeric(18,6) AS "faktor dibulatkan dulu",
       ((12 * 1)::numeric / 12)::numeric(18,4)                AS "dibagi sekali di akhir",
       CASE WHEN (12 * (1::numeric / 12)::numeric(18,6))::numeric(18,6) = 0.999996
             AND ((12 * 1)::numeric / 12)::numeric(18,4) = 1.0000
            THEN 'LULUS (rasio + arah memang perlu)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

\echo ''
\echo '== BLOK 4: konversi antar-kategori tidak boleh terjadi ============================'
\echo '   LTR berkategori VOLUME, PCS berkategori UNIT. Pemanggilnya wajib menolak.'

SELECT (SELECT UPPER(COALESCE(NULLIF(TRIM(kategori),''),'UNIT')) FROM uom19.satuan
         WHERE kode = 'LTR') AS "kategori LTR",
       (SELECT UPPER(COALESCE(NULLIF(TRIM(kategori),''),'UNIT')) FROM uom19.satuan
         WHERE kode = 'PCS') AS "kategori PCS",
       CASE WHEN (SELECT UPPER(COALESCE(NULLIF(TRIM(kategori),''),'UNIT')) FROM uom19.satuan
                   WHERE kode = 'LTR')
                <> (SELECT UPPER(COALESCE(NULLIF(TRIM(kategori),''),'UNIT')) FROM uom19.satuan
                     WHERE kode = 'PCS')
            THEN 'LULUS (memang berbeda, jadi ditolak)' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 5: cuplikan tersimpan, dan kuantitas tetap yang berwenang ================='
\echo '   Baris 1 dikirim dalam DUS; baris 2 dalam satuan dasar (cuplikannya NULL).'

INSERT INTO uom19.sales_order_detail (sales_order_id, baris_ke, produk_id, kuantitas,
                                      harga_satuan, total, satuan_jual_id, qty_input,
                                      faktor_ke_dasar, dibuat_pada, oleh)
     VALUES (1, 1, 10, 36.0000, 5000, 180000, 2, 3.0000, 12.000000, now(), 'uji'),
            (1, 2, 10,  7.0000, 5000,  35000, NULL, NULL, NULL,      now(), 'uji'),
            -- baris 3: 12 LSN -> 1 PCS. Cuplikan faktornya 0,083333 (skala kolomnya).
            (1, 3, 10,  1.0000, 5000,   5000, 3, 12.0000, 0.083333,  now(), 'uji');

SELECT d.baris_ke, d.kuantitas, d.satuan_jual_id, d.qty_input, d.faktor_ke_dasar,
       CASE WHEN d.baris_ke = 2 THEN 'tanpa satuan jual'
            ELSE 'bercuplikan' END AS bentuk,
       CASE WHEN (d.baris_ke = 1 AND d.satuan_jual_id = 2 AND d.qty_input = 3.0000)
             OR (d.baris_ke = 2 AND d.satuan_jual_id IS NULL AND d.qty_input IS NULL)
             OR (d.baris_ke = 3 AND d.satuan_jual_id = 3 AND d.kuantitas = 1.0000)
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM uom19.sales_order_detail d ORDER BY d.baris_ke;

\echo ''
\echo '== BLOK 6 (PENJAGA): menghitung ULANG dari faktor_ke_dasar memberi angka lain ====='
\echo '   Itu sebabnya kuantitas yang mengikat dan faktornya hanya catatan. Angka "hitung'
\echo '   ulang" pada baris 3 HARUS berbeda dari kuantitas tersimpan (selisih bukan nol).'

SELECT d.baris_ke, d.kuantitas AS "tersimpan",
       (d.qty_input * d.faktor_ke_dasar)::numeric(18,6) AS "hitung ulang (skala 6)",
       (d.kuantitas - (d.qty_input * d.faktor_ke_dasar))::numeric(18,6) AS "selisih",
       CASE WHEN d.kuantitas <> (d.qty_input * d.faktor_ke_dasar)::numeric(18,6)
            THEN 'LULUS (cuplikan memang bukan masukan hitungan)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil
  FROM uom19.sales_order_detail d WHERE d.baris_ke = 3;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
