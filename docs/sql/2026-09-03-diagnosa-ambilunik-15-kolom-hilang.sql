-- =====================================================================================
-- Diagnosa dampak data lama: 15 kolom referensi TIDAK dikenali GrupTransaksi.ambilUnik()
--
-- Latar: GrupTransaksi punya 41 kolom referensi dokumen sumber, tetapi ambilUnik()
-- (ais/database/model/akunting/GrupTransaksi.java) hanya menyusun kodeUnik dari 26 di
-- antaranya. Lima belas kolom berikut TIDAK menyumbang apa pun ke kunci:
--   log_pembayaran, penerimaan_pengadaan_master_asset, saldo_awal_master_asset,
--   pertangungjawaban_kas_besar, transaksi_koperasi, pajak, pembayaran_gaji,
--   pembatalan_transaksi (PembatalanTransaksiKantin), penghapusan_master_asset,
--   pembayaran_anggota_koperasi, pencairan_diskon, penyesuaian_saldo_anggota,
--   modal_penyertaan_koperasi, pembagian_shu, nota_sales_biaya
--
-- Akibatnya kodeUnik = ref TELANJANG (tanpa kelas+id dokumen). Dua bentuk kerusakan:
--   (a) ref terisi tapi dipakai berulang oleh banyak dokumen (mis. "pajak" dari
--       REF_PAJAK, "DP_PEKERJAAN" dari PostingDpPemesananPekerjaanAction) -> kodeUnik
--       GLOBAL lintas dokumen/tenant -> hanya jurnal PERTAMA yang tertulis, sisanya
--       dianggap duplikat: jurnalnya TIDAK ditulis, cap posting dokumen lain tertimpa.
--   (b) ref kosong -> kodeUnik NULL -> "kode_unik = NULL" tidak pernah cocok di SQL ->
--       posting ulang dokumen yang sama menerbitkan jurnal DUPLIKAT.
-- Rincian lengkap: Javadoc kelas GrupTransaksi, bagian "PERINGATAN INTEGRITAS TAMBAHAN".
--
-- Skrip ini HANYA-BACA (seluruhnya SELECT; tidak ada INSERT/UPDATE/DELETE/DDL) dan aman
-- dijalankan pada basis data produksi, sebaiknya memakai pengguna read-only.
--
-- CATATAN: mesin kerja penulis skrip ini HANYA tersambung ke instans Postgres lokal
-- efemeral (kosong dari jurnal) -- angka di bawah HARUS diambil dengan menjalankan
-- skrip ini langsung terhadap basis data UAT/produksi yang sesungguhnya.
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- Q0. DDL: apakah kolom kode_unik BENAR-BENAR punya unique index/constraint di database
--     (bukan cuma anotasi @Column(unique = true) di kelas Java)? Bila kosong, artinya
--     Hibernate tidak pernah berhasil menegakkannya di skema ini -- kemungkinan karena
--     baris ber-kode_unik NULL/duplikat sudah ada saat ALTER TABLE ... ADD CONSTRAINT
--     dicoba dan gagal diam-diam pada suatu waktu, atau memang belum pernah dijalankan.
-- -------------------------------------------------------------------------------------
SELECT n.nspname AS skema, t.relname AS tabel, c.conname AS nama_constraint,
       pg_get_constraintdef(c.oid) AS definisi
FROM pg_constraint c
JOIN pg_class t ON t.oid = c.conrelid
JOIN pg_namespace n ON n.oid = t.relnamespace
WHERE t.relname = 'grup_transaksi' AND n.nspname = 'akunting' AND c.contype = 'u'
UNION ALL
SELECT n.nspname, t.relname, i.relname, pg_get_indexdef(i.oid)
FROM pg_index ix
JOIN pg_class i ON i.oid = ix.indexrelid
JOIN pg_class t ON t.oid = ix.indrelid
JOIN pg_namespace n ON n.oid = t.relnamespace
WHERE t.relname = 'grup_transaksi' AND n.nspname = 'akunting' AND ix.indisunique;
-- Baca hasil: baris kosong -> TIDAK ADA unique index/constraint pada kode_unik di DB
-- ini meski entity memetakannya @Column(unique = true); dedup CommonAkunting.saveTransaksi
-- hanya bergantung pada Criteria.eq di level aplikasi, bukan penjagaan database.

-- -------------------------------------------------------------------------------------
-- Q1. Untuk MASING-MASING dari 15 kolom: berapa dokumen bercap posting (kolom
--     posting_history* terisi) TETAPI ref-nya NULL -> kodeUnik NULL -> setiap posting
--     ulang berpotensi menulis jurnal DUPLIKAT. Dibandingkan dengan jumlah grup_transaksi
--     yang benar-benar menunjuk dokumen itu (indikasi jurnal ganda bila lebih banyak dari
--     dokumen bercap).
-- -------------------------------------------------------------------------------------
WITH kolom(nama, tabel_referensi, kolom_id_dok, kolom_cap) AS (
    VALUES
    ('log_pembayaran',                'public.log_pembayaran',                         'id', NULL),
    ('penerimaan_pengadaan_master_asset','asset.penerimaan_pengadaan_master_asset',     'id', 'posting_history'),
    ('saldo_awal_master_asset',        'asset.saldo_awal_master_asset',                 'id', 'posting_history'),
    ('pertangungjawaban_kas_besar',    'akunting.pertangungjawaban_kas_besar',          'id', 'posting_history'),
    ('transaksi_koperasi',             'koperasi.transaksi_koperasi',                   'id', 'posting_history'),
    ('pajak',                          'akunting.pajak',                                'id', 'posting_history'),
    ('pembayaran_gaji',                'payroll.pembayaran_gaji',                       'id', 'posting_history'),
    ('pembatalan_transaksi',           'koperasi.pembatalan_transaksi',                 'id', 'posting_history'),
    ('penghapusan_master_asset',       'asset.penghapusan_master_asset',                'id', 'posting_history'),
    ('pembayaran_anggota_koperasi',    'koperasi.pembayaran_anggota_koperasi',          'id', 'posting_history'),
    ('pencairan_diskon',               'koperasi.pencairan_diskon',                     'id', 'posting_history'),
    ('penyesuaian_saldo_anggota',      'koperasi.penyesuaian_saldo_anggota',            'id', 'posting_history'),
    ('modal_penyertaan_koperasi',      'koperasi.modal_penyertaan',                     'id', 'posting_history'),
    ('pembagian_shu',                  'koperasi.pembagian_shu',                        'id', 'posting_history'),
    ('nota_sales_biaya',               'koperasi.nota_sales_biaya',                     'id', 'posting_history')
)
SELECT nama AS kolom_referensi,
       (SELECT count(*) FROM akunting.grup_transaksi g WHERE g.ref IS NULL
          AND (CASE nama
               WHEN 'log_pembayaran' THEN g.log_pembayaran
               WHEN 'penerimaan_pengadaan_master_asset' THEN g.penerimaan_pengadaan_master_asset
               WHEN 'saldo_awal_master_asset' THEN g.saldo_awal_master_asset
               WHEN 'pertangungjawaban_kas_besar' THEN g.pertangungjawaban_kas_besar
               WHEN 'transaksi_koperasi' THEN g.transaksi_koperasi
               WHEN 'pajak' THEN g.pajak
               WHEN 'pembayaran_gaji' THEN g.pembayaran_gaji
               WHEN 'pembatalan_transaksi' THEN g.pembatalan_transaksi
               WHEN 'penghapusan_master_asset' THEN g.penghapusan_master_asset
               WHEN 'pembayaran_anggota_koperasi' THEN g.pembayaran_anggota_koperasi
               WHEN 'pencairan_diskon' THEN g.pencairan_diskon
               WHEN 'penyesuaian_saldo_anggota' THEN g.penyesuaian_saldo_anggota
               WHEN 'modal_penyertaan_koperasi' THEN g.modal_penyertaan_koperasi
               WHEN 'pembagian_shu' THEN g.pembagian_shu
               WHEN 'nota_sales_biaya' THEN g.nota_sales_biaya
               END) IS NOT NULL) AS grup_jurnal_ber_ref_null
FROM kolom
ORDER BY 1;
-- CATATAN: kueri ini memakai CASE per baris VALUES agar satu bentuk kueri menutupi
-- seluruh 15 kolom -- silakan jalankan versi per-kolom di Q2 untuk rincian per dokumen
-- bila salah satu baris di atas menunjukkan angka mencurigakan (>1 grup per dokumen).

-- -------------------------------------------------------------------------------------
-- Q2. RINCIAN per dokumen untuk kolom yang menunjukkan gejala: lebih dari satu
--     grup_transaksi menunjuk dokumen yang sama (indikasi jurnal DUPLIKAT dari kasus
--     ref NULL). Sesuaikan nama kolom pada tiga tempat bertanda <<KOLOM>> lalu jalankan
--     satu-per-satu untuk kolom yang ingin diperiksa.
-- -------------------------------------------------------------------------------------
-- SELECT g.<<KOLOM>> AS dokumen_id, count(*) AS jumlah_grup_jurnal,
--        array_agg(g.id ORDER BY g.id) AS id_grup_transaksi
-- FROM akunting.grup_transaksi g
-- WHERE g.<<KOLOM>> IS NOT NULL
-- GROUP BY g.<<KOLOM>>
-- HAVING count(*) > 1
-- ORDER BY jumlah_grup_jurnal DESC
-- LIMIT 200;

-- -------------------------------------------------------------------------------------
-- Q3. Kunci GLOBAL yang terbukti bertabrakan lintas dokumen: ref terisi tapi kolom
--     referensi PEMILIKNYA tidak dikenali ambilUnik(), sehingga kodeUnik = ref telanjang.
--     Baris berikut menghitung berapa kali tiap nilai ref semacam itu benar-benar dipakai
--     ulang oleh grup_transaksi yang BERBEDA dokumen sumbernya -- itulah jurnal yang
--     "hilang" karena dianggap duplikat dari jurnal ber-ref sama pertama.
-- -------------------------------------------------------------------------------------
SELECT g.ref,
       count(*)                                            AS jumlah_grup_dengan_ref_ini,
       count(DISTINCT g.pajak)                              AS distinct_pajak,
       count(DISTINCT g.saldo_awal_master_asset)            AS distinct_saldo_awal_master_asset,
       count(*) FILTER (WHERE g.pajak IS NOT NULL)          AS baris_pajak,
       count(*) FILTER (WHERE g.saldo_awal_master_asset IS NOT NULL) AS baris_saldo_awal_master_asset
FROM akunting.grup_transaksi g
WHERE g.ref IN ('pajak', 'DP_PEKERJAAN', 'DP_BALIK_PEKERJAAN')
GROUP BY g.ref
ORDER BY g.ref;
-- Baca hasil: jumlah_grup_dengan_ref_ini = 1 untuk suatu ref -> aman, baru satu dokumen
-- pernah terposting dengan ref itu. Bila > 1 dan distinct_<kolom> juga > 1 -> BUKTI
-- tabrakan nyata: lebih dari satu dokumen SUMBER BERBEDA berbagi satu kodeUnik "pajak"
-- atau "DP_PEKERJAAN"; hanya salah satu yang benar-benar dapat jurnal, sisanya senyap
-- gagal ditulis saat diposting (lihat CommonAkunting.saveTransaksi baris ~700).
-- Ganti daftar ref pada WHERE dan kolom pada SELECT bila REF_* lain (lihat konstanta
-- PostingJurnalHelper) dicurigai dipakai berulang oleh kolom yang tidak dikenali
-- ambilUnik().

-- -------------------------------------------------------------------------------------
-- Q4. Konteks pemakaian tiap kolom -- berapa dokumen di tabel sumbernya SUDAH bercap
--     posting (posting_history terisi). Bila nol untuk suatu kolom, modul itu tidak
--     pernah dipakai di instalasi ini dan TIDAK ADA dampak nyata dari cacat ambilUnik()
--     untuk kolom tersebut walau secara kode tetap rawan.
-- -------------------------------------------------------------------------------------
SELECT 'saldo_awal_master_asset (DP_PEKERJAAN)' AS modul, count(*) AS dokumen_bercap
FROM asset.saldo_awal_master_asset WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'pajak', count(*) FROM akunting.pajak WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'log_pembayaran (admin)', count(*) FROM public.log_pembayaran WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'log_pembayaran (payment gateway)', count(*) FROM public.log_pembayaran
    WHERE posting_history_payment_gateway IS NOT NULL
UNION ALL
SELECT 'transaksi_koperasi', count(*) FROM koperasi.transaksi_koperasi WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'pembayaran_anggota_koperasi', count(*) FROM koperasi.pembayaran_anggota_koperasi
    WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'pencairan_diskon', count(*) FROM koperasi.pencairan_diskon WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'penyesuaian_saldo_anggota', count(*) FROM koperasi.penyesuaian_saldo_anggota
    WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'modal_penyertaan_koperasi', count(*) FROM koperasi.modal_penyertaan
    WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'pembagian_shu', count(*) FROM koperasi.pembagian_shu WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'nota_sales_biaya', count(*) FROM koperasi.nota_sales_biaya WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'pembayaran_gaji', count(*) FROM payroll.pembayaran_gaji WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'pertangungjawaban_kas_besar', count(*) FROM akunting.pertangungjawaban_kas_besar
    WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'penerimaan_pengadaan_master_asset', count(*) FROM asset.penerimaan_pengadaan_master_asset
    WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'penghapusan_master_asset', count(*) FROM asset.penghapusan_master_asset
    WHERE posting_history IS NOT NULL
UNION ALL
SELECT 'pembatalan_transaksi (kantin)', count(*) FROM koperasi.pembatalan_transaksi
    WHERE posting_history IS NOT NULL
ORDER BY 2 DESC;
-- NAMA SKEMA/TABEL/KOLOM DI ATAS BELUM DIVALIDASI terhadap DDL nyata (mesin kerja
-- penulis tidak tersambung ke basis data berisi data) -- SESUAIKAN nama skema/tabel bila
-- berbeda dari asumsi di sini sebelum menjalankan Q1/Q4. Sumber kebenaran nama kolom
-- entity: anotasi @JoinColumn pada GrupTransaksi.java (lihat daftar di kepala berkas ini);
-- nama tabel sumber masing-masing dokumen ada pada anotasi @Table kelas entity-nya
-- masing-masing (mis. ais.database.model.akunting.Pajak, ais.database.model.asset.*,
-- ais.database.model.koperasi.*, ais.database.model.payroll.PembayaranGaji).

-- -------------------------------------------------------------------------------------
-- Q5. Cacat BONUS (bukan bagian dari 15 kolom di atas): cabang transaksiPegawai pada
--     ambilUnik() salah menulis PembayaranGajiPunyaPegawai.class.getName() alih-alih
--     TransaksiPegawai.class.getName(). Baris ini menghitung tabrakan id NYATA antara
--     kedua tabel yang akan menyebabkan salah satu INSERT gagal karena unique constraint
--     kode_unik (bila constraint itu memang ada -- lihat Q0), atau salah satunya
--     tercap ulang tanpa jurnal baru (bila constraint TIDAK ada, lihat Q0).
-- -------------------------------------------------------------------------------------
SELECT count(*) AS id_bertabrakan_transaksi_pegawai_vs_pembayaran_gaji
FROM payroll.transaksi_pegawai tp
JOIN payroll.pembayaran_gaji_punya_pegawai pg ON pg.id = tp.id;
-- NAMA TABEL DI ATAS ASUMSI DARI KONVENSI PENAMAAN -- sesuaikan bila berbeda. Baris > 0
-- berarti ada risiko NYATA saat kedua dokumen dengan id sama pernah/akan diposting.

-- =====================================================================================
-- CARA MEMBACA HASIL
-- * Q0 kosong -> TIDAK ADA unique index pada kode_unik di database ini meski entity
--   memetakannya unique -- berarti dedup HANYA ditegakkan di level aplikasi (rentan
--   race condition antar sesi/thread), dan migrasi 15 kolom TIDAK akan gagal karena
--   constraint database saat backfill (tetap bisa gagal secara LOGIS bila backfill
--   naif membuat dua baris ber-kodeUnik baru yang sama).
-- * Q0 berisi -> unique index ADA -- migrasi backfill kode_unik untuk 15 kolom WAJIB
--   dilakukan hati-hati (lihat strategi di dokumen pendamping) karena UPDATE massal
--   yang mengubah kode_unik banyak baris sekaligus BISA melanggar constraint di
--   tengah transaksi bila urutannya tidak diperhitungkan.
-- * Q1/Q4 semua nol untuk suatu kolom -> modul itu tidak pernah dipakai di instalasi
--   ini, aman ditunda.
-- * Q1 ada isinya (grup_jurnal_ber_ref_null > 0) -> berpotensi jurnal duplikat dari
--   kasus (b) pada dokumen kolom itu; lanjutkan ke Q2 untuk konfirmasi per dokumen.
-- * Q3 ada baris dengan jumlah_grup_dengan_ref_ini > 1 DAN distinct_<kolom> > 1 ->
--   BUKTI EMPIRIS tabrakan kunci global dari kasus (a); hitung selisih antara jumlah
--   dokumen bercap (Q4) dan jumlah grup_transaksi unik untuk tahu berapa jurnal yang
--   hilang.
-- * Q5 > 0 -> risiko nyata cacat transaksiPegawai; lihat GrupTransaksi.java sekitar
--   ambilUnik() cabang "else if (transaksiPegawai != null)".
--
-- LANGKAH LANJUTAN: jangan menambal kode ambilUnik() untuk 15 kolom ini tanpa migrasi
-- backfill kode_unik baris lama terlebih dahulu -- menambah cabang baru mengubah nilai
-- kodeUnik yang SUDAH tersimpan (kolom unique) untuk setiap baris lama yang memakai
-- kolom itu, karena getKodeUnik() menghitung ulang nilainya setiap kali entity di-flush.
-- Lihat strategi migrasi pada dokumen pos pendamping sebelum mengubah ambilUnik().
-- =====================================================================================
