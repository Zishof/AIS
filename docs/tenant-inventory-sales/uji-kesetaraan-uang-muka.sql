-- =====================================================================================
-- Uji kesetaraan §18: uang muka faktur adalah DOKUMEN, bukan kolom pengurang
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v18:
--   psql -h 127.0.0.1 -p 55518 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-uang-muka.sql
--
-- Berkas ini mengandaikan schema tenant bernama dp18.
--
-- LATAR
--   Jalur legacy menyimpan dibayar_awal sebagai KOLOM pada dokumen piutang, dan menghitung
--   sisanya: total_faktur - dibayar_awal - SUM(alokasi). Ada DUA pengurang di sana, dan hanya
--   satu yang berasal dari dokumen. Uang mukanya mengurangi tagihan tanpa kwitansi yang bisa
--   ditunjuk, tanpa cara bayar, dan tanpa pasangan di sisi kas.
--
--   Model tenant menghitung sisa: nilai - SUM(alokasi). Satu sumber. Karena itu uang muka
--   diterbitkan sebagai penerimaan_piutang sungguhan berikut alokasinya, dalam transaksi yang
--   sama dengan fakturnya. Fakturnya tetap bernilai PENUH; yang berkurang adalah sisanya.
--
-- EMPAT HAL YANG DIUJI
--   1. Nilai piutang tetap penuh; alokasinya yang memuat uang muka.
--   2. Sisa tagihannya SAMA PERSIS dengan aritmetika legacy.
--   3. Blok 3 penjaganya: meniru kolom pengurang legacy SEKALIGUS menerbitkan alokasinya
--      menghitung uangnya DUA KALI. Inilah yang dihindari.
--   4. Pada daftar piutang, dibayarAwal tetap nol dan teralokasi memuat uang mukanya --
--      outstanding-nya tetap sama, sebab legacy menjumlahkan keduanya.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\pset format aligned

TRUNCATE dp18.alokasi_penerimaan_piutang, dp18.penerimaan_piutang, dp18.piutang_customer,
         dp18.faktur_penjualan, dp18.sales_order, dp18.salesperson, dp18.customer, dp18.toko
      RESTART IDENTITY CASCADE;

INSERT INTO dp18.toko (id, nama) VALUES (1, 'Toko Uji');
INSERT INTO dp18.customer (id, kode, nama) VALUES (9, 'C9', 'Pelanggan Uji');
INSERT INTO dp18.salesperson (id, kode, nama) VALUES (5, 'S5', 'Sales Uji');
INSERT INTO dp18.sales_order (id, nomor_dokumen, tanggal, customer_id, salesperson_id, toko_id,
                              total, status)
     VALUES (77, 'SO-77', CURRENT_DATE, 9, 5, 1, 1000000.00, 'TERKIRIM');

-- Faktur bernilai PENUH. Uang mukanya tidak mengurangi apa pun di sini.
INSERT INTO dp18.faktur_penjualan (id, nomor_dokumen, tanggal, jatuh_tempo, customer_id,
                                   salesperson_id, sales_order_id, toko_id, subtotal, total,
                                   keterangan, idempotency_key, status, dibuat_pada, oleh)
     VALUES (1, 'INV-1-1', CURRENT_DATE, CURRENT_DATE + 30, 9, 5, 77, 1, 1000000.00, 1000000.00,
             'Faktur dari sales order SO-77', 'SO-INV-77', 'AKTIF', now(), 'uji');
INSERT INTO dp18.piutang_customer (id, customer_id, salesperson_id, faktur_penjualan_id,
                                   nomor_faktur, tanggal, jatuh_tempo, nilai, sisa, status,
                                   dibuat_pada, oleh)
     VALUES (1, 9, 5, 1, 'INV-1-1', CURRENT_DATE, CURRENT_DATE + 30, 1000000.00, 1000000.00,
             'TERBUKA', now(), 'uji');

-- Uang muka: DOKUMEN penerimaan berikut alokasinya.
INSERT INTO dp18.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id, salesperson_id,
                                     cara_bayar, nilai, keterangan, idempotency_key, status,
                                     dibuat_pada, oleh)
     VALUES (1, 'KWT-1-1', CURRENT_DATE, 9, 5, 'TUNAI', 300000.00, 'Uang muka faktur INV-1-1',
             'SO-DP-77', 'AKTIF', now(), 'uji');
INSERT INTO dp18.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id, nilai,
                                             dibuat_pada, oleh)
     VALUES (1, 1, 300000.00, now(), 'uji');

\echo ''
\echo '== BLOK 1: nilai piutang tetap PENUH ============================================='
\echo '   Uang muka tidak memotong nilainya; ia muncul sebagai alokasi.'

SELECT d.nilai AS "nilai piutang",
       (SELECT COALESCE(SUM(a.nilai),0) FROM dp18.alokasi_penerimaan_piutang a
         WHERE a.piutang_customer_id = d.id) AS "Sigma alokasi",
       CASE WHEN d.nilai = 1000000.00
             AND (SELECT COALESCE(SUM(a.nilai),0) FROM dp18.alokasi_penerimaan_piutang a
                   WHERE a.piutang_customer_id = d.id) = 300000.00
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM dp18.piutang_customer d WHERE d.id = 1;

\echo ''
\echo '== BLOK 2: sisa tagihan setara aritmetika legacy ================================='
\echo '   legacy: total_faktur - dibayar_awal - SUM(alokasi) = 1000000 - 300000 - 0'
\echo '   tenant: nilai - SUM(alokasi)                       = 1000000 - 300000'

SELECT (COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
          FROM dp18.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0))
         AS "sisa tenant",
       (1000000.00 - 300000.00 - 0) AS "sisa legacy",
       CASE WHEN (COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
              FROM dp18.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0))
            = (1000000.00 - 300000.00 - 0)
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM dp18.piutang_customer d WHERE d.id = 1;

\echo ''
\echo '== BLOK 3 (PENJAGA): meniru kolom pengurang SEKALIGUS menerbitkan alokasinya ====='
\echo '   Uangnya terhitung DUA KALI. Angka di bawah HARUS 400000 -- kalau ia 700000,'
\echo '   contoh ini tidak membedakan apa pun.'

SELECT ((1000000.00 - 300000.00)
        - COALESCE((SELECT SUM(a.nilai) FROM dp18.alokasi_penerimaan_piutang a
                     WHERE a.piutang_customer_id = 1),0)) AS "bila dihitung dua kali",
       CASE WHEN ((1000000.00 - 300000.00)
                  - COALESCE((SELECT SUM(a.nilai) FROM dp18.alokasi_penerimaan_piutang a
                               WHERE a.piutang_customer_id = 1),0)) = 400000.00
            THEN 'LULUS (dua pengurang memang salah)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

\echo ''
\echo '== BLOK 4: daftar piutang -- dibayarAwal nol, teralokasi memuatnya ==============='
\echo '   Klien yang menampilkan kolom "uang muka" melihat nol; yang menampilkan sisa'
\echo '   tagihan melihat angka yang sama dengan legacy.'

SELECT d.nilai AS "totalFaktur", 0 AS "dibayarAwal",
       COALESCE((SELECT SUM(a.nilai) FROM dp18.alokasi_penerimaan_piutang a
                  WHERE a.piutang_customer_id = d.id),0) AS "teralokasi",
       (COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
          FROM dp18.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0))
         AS "outstanding",
       CASE WHEN (COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
              FROM dp18.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0))
            = 700000.00
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM dp18.piutang_customer d WHERE d.id = 1;

\echo ''
\echo '== BLOK 5: kwitansi uang muka dapat ditunjuk ====================================='
\echo '   Inilah yang tidak dimiliki jalur legacy: dokumen dengan nomor, cara bayar, dan'
\echo '   tanggal, yang bisa dicetak dan direkonsiliasi terhadap kas.'

SELECT p.nomor_dokumen, p.cara_bayar, p.nilai, p.idempotency_key,
       CASE WHEN p.sales_trip_id IS NULL THEN 'tanpa trip (pemfakturan kantor)'
            ELSE 'terkait trip' END AS lingkup,
       CASE WHEN p.nomor_dokumen <> '' AND p.cara_bayar = 'TUNAI' AND p.nilai = 300000.00
             AND p.idempotency_key = 'SO-DP-77'
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM dp18.penerimaan_piutang p WHERE p.id = 1;

\echo ''
\echo '== BLOK 6: idempotensi -- kunci uang muka sejajar kunci fakturnya ================'
\echo '   Keduanya lahir dalam satu transaksi, jadi pengulangan tertolak pada indeks unik'
\echo '   faktur lebih dulu dan tidak pernah sampai menerbitkan kwitansi kedua.'

SELECT (SELECT COUNT(*) FROM dp18.faktur_penjualan WHERE idempotency_key = 'SO-INV-77')
         AS "faktur",
       (SELECT COUNT(*) FROM dp18.penerimaan_piutang WHERE idempotency_key = 'SO-DP-77')
         AS "kwitansi uang muka",
       CASE WHEN (SELECT COUNT(*) FROM dp18.faktur_penjualan
                   WHERE idempotency_key = 'SO-INV-77') = 1
             AND (SELECT COUNT(*) FROM dp18.penerimaan_piutang
                   WHERE idempotency_key = 'SO-DP-77') = 1
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
