-- =====================================================================================
-- Diagnosa dampak data lama: DELETE tanpa syarat di
-- PengajuanTransaksiPegawaiAction.populateTransaksi() (ais/action/master/payroll/)
--
-- Latar: sebelum perbaikan r84075, populateTransaksi() menghapus SELURUH baris
-- payroll.transaksi_pegawai milik sebuah pengajuan tanpa memeriksa apakah baris itu
-- sudah diposting ke jurnal (posting_history terisi) atau sudah dipotong dari slip gaji
-- yang sudah dibayar (pembayaran_gaji_punya_pegawai terisi), lalu membangkitkan ulang
-- baris baru (postingHistory/pembayaranGajiPunyaPegawai keduanya NULL) hanya bila
-- setujui=true. Dipicu penyimpanan biasa MAUPUN satu klik centang/cabut-centang
-- "Setujui" pada layar pengajuan.
--
-- Tiga bentuk kerusakan yang mungkin sudah terjadi pada data SEBELUM r84075:
--   (a) Jurnal yatim -- akunting.grup_transaksi.transaksi_pegawai menunjuk id baris
--       payroll.transaksi_pegawai yang sudah tidak ada (dihapus lalu diganti baris baru
--       ber-id lain, atau dihapus tanpa pernah diganti karena setujui dicabut).
--   (b) Posting ganda -- baris pengganti lahir dengan posting_history NULL padahal
--       "angsuran ke-N" yang sama secara logis sudah pernah diposting sebelumnya; bila
--       lalu diposting lagi, beban/potongan yang sama terbukukan dua kali ke buku besar.
--       Skrip ini TIDAK BISA mendeteksi kejadian (b) secara langsung (baris lama sudah
--       tergantikan/terhapus, tidak ada jejak "pernah ada N angsuran ter-posting utk
--       pengajuan ini" selain lewat akunting.transaksi/grup_transaksi yang sudah yatim
--       di poin (a), atau lewat audit Envers -- lihat Q3).
--   (c) Pelunasan hilang -- stempel pembayaran_gaji_punya_pegawai yang menandai baris
--       sudah dipotong dari slip gaji yang sudah dibayar ikut lenyap saat baris dihapus;
--       tidak terlihat langsung dari tabel saat ini (barisnya sudah tiada), hanya
--       terindikasi lewat riwayat Envers (Q3) atau lewat baris grup_transaksi yatim yang
--       ref-nya menunjuk slip gaji sudah lunas.
--
-- Rincian lengkap dan kode yang diperbaiki: Javadoc kelas
-- ais.database.model.payroll.TransaksiPegawai (bagian 1 & 3), commit r84075.
--
-- Skrip ini HANYA-BACA (seluruhnya SELECT; tidak ada INSERT/UPDATE/DELETE/DDL) dan aman
-- dijalankan pada basis data produksi, sebaiknya memakai pengguna read-only.
--
-- CATATAN: mesin kerja penulis skrip ini TIDAK tersambung ke basis data UAT/produksi
-- yang berisi data nyata -- skrip ini BELUM PERNAH DIJALANKAN. Angka dampak historis
-- HARUS diambil dengan menjalankan skrip ini langsung terhadap basis data yang
-- sesungguhnya; nama skema/tabel/kolom di bawah diambil dari anotasi @JoinColumn/@Table
-- pada GrupTransaksi.java dan TransaksiPegawai.java (belum divalidasi terhadap DDL nyata).
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- Q0. Apakah kolom akunting.grup_transaksi.transaksi_pegawai punya FOREIGN KEY
--     constraint fisik di database ini? Bila ADA, DELETE lama seharusnya sudah GAGAL
--     (bukan silent orphan) setiap kali ada jurnal yang mereferensikannya -- artinya
--     Q1 di bawah akan selalu nol dan kerusakan (a) tidak mungkin terjadi lewat DELETE
--     ini (meski tetap bisa lewat jalur lain). Bila TIDAK ADA (kemungkinan besar --
--     repo ini tidak menyimpan DDL apa pun di version control), Q1 adalah pengukuran
--     langsung dari kerusakan (a).
-- -------------------------------------------------------------------------------------
SELECT c.conname AS nama_constraint, pg_get_constraintdef(c.oid) AS definisi
FROM pg_constraint c
JOIN pg_class t ON t.oid = c.conrelid
JOIN pg_namespace n ON n.oid = t.relnamespace
WHERE t.relname = 'grup_transaksi' AND n.nspname = 'akunting' AND c.contype = 'f'
  AND pg_get_constraintdef(c.oid) ILIKE '%transaksi_pegawai%';
-- Baca hasil: baris kosong -> TIDAK ADA FK constraint pada kolom ini di DB ini; DELETE
-- lama bisa menghasilkan referensi menggantung tanpa penolakan database sama sekali.

-- -------------------------------------------------------------------------------------
-- Q1. Jurnal YATIM -- baris akunting.grup_transaksi yang transaksi_pegawai terisi
--     tetapi id itu SUDAH TIDAK ADA di payroll.transaksi_pegawai. Ini pengukuran utama
--     dampak (a): tiap baris di sini adalah satu jurnal (kaki debet+kredit) yang
--     dokumen sumbernya sudah lenyap dari layar Transaksi Pegawai, tetapi tetap
--     tercatat di buku besar.
-- -------------------------------------------------------------------------------------
SELECT count(*) AS jumlah_jurnal_yatim
FROM akunting.grup_transaksi g
WHERE g.transaksi_pegawai IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM payroll.transaksi_pegawai tp WHERE tp.id = g.transaksi_pegawai);

-- -------------------------------------------------------------------------------------
-- Q2. Rincian jurnal yatim di atas -- id transaksi_pegawai yang hilang, tanggal jurnal,
--     apakah sudah ditutup (closing), dan nilai jurnalnya (dari akunting.transaksi anak
--     bila masih ada) untuk memperkirakan besaran rupiah yang terdampak.
-- -------------------------------------------------------------------------------------
SELECT g.id AS id_grup_transaksi, g.transaksi_pegawai AS id_transaksi_pegawai_hilang,
       g.tanggal, g.closing IS NOT NULL AS sudah_tutup_buku, g.posting_history,
       (SELECT coalesce(sum(t.nilai), 0) FROM akunting.transaksi t WHERE t.grup_transaksi = g.id)
           AS total_nilai_jurnal
FROM akunting.grup_transaksi g
WHERE g.transaksi_pegawai IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM payroll.transaksi_pegawai tp WHERE tp.id = g.transaksi_pegawai)
ORDER BY g.tanggal DESC
LIMIT 500;

-- -------------------------------------------------------------------------------------
-- Q3. Jejak Envers -- pengajuan yang riwayat revisinya menunjukkan baris
--     payroll.transaksi_pegawai pernah dihapus lalu dibuat ulang berkali-kali untuk
--     id pengajuan yang sama (indikasi pengajuan yang sering disunting/dicentang-
--     cabut setelah baris angsurannya sempat terposting/terlunasi -- kandidat kuat
--     dampak (b)/(c)). Tabel audit standar Hibernate Envers: payroll.transaksi_pegawai_aud
--     dengan kolom revtype (0=INSERT, 1=UPDATE, 2=DELETE) dan rev (id revisi, join ke
--     tabel revisi standar -- sesuaikan nama tabel revisi bila berbeda dari default
--     "revinfo").
-- -------------------------------------------------------------------------------------
SELECT pengajuan_transaksi_pegawai AS id_pengajuan,
       count(*) FILTER (WHERE revtype = 2) AS jumlah_delete,
       count(*) FILTER (WHERE revtype = 0) AS jumlah_insert,
       count(*) FILTER (WHERE revtype = 2 AND (posting_history IS NOT NULL
                                                 OR pembayaran_gaji_punya_pegawai IS NOT NULL))
           AS jumlah_delete_baris_yang_sudah_terkunci
FROM payroll.transaksi_pegawai_aud
WHERE pengajuan_transaksi_pegawai IS NOT NULL
GROUP BY pengajuan_transaksi_pegawai
HAVING count(*) FILTER (WHERE revtype = 2 AND (posting_history IS NOT NULL
                                                 OR pembayaran_gaji_punya_pegawai IS NOT NULL)) > 0
ORDER BY jumlah_delete_baris_yang_sudah_terkunci DESC
LIMIT 200;
-- Baca hasil: baris di sini adalah pengajuan yang riwayatnya membuktikan sebuah baris
-- BERSTATUS SUDAH DIPOSTING ATAU SUDAH DILUNASI pernah terhapus oleh populateTransaksi()
-- -- yaitu bukti langsung dampak (b) (posting ganda berpotensi terjadi bila
-- pengajuannya diposting ulang setelah baris pengganti dibuat) dan (c) (pelunasan yang
-- pernah ada pada baris yang terhapus).

-- -------------------------------------------------------------------------------------
-- Q4. Konteks skala pemakaian -- berapa total pengajuan, berapa yang sudah setujui, dan
--     berapa baris transaksi_pegawai turunan pengajuan (pengajuan_transaksi_pegawai
--     terisi) yang beredar saat ini, sebagai pembanding besaran Q1/Q3.
-- -------------------------------------------------------------------------------------
SELECT (SELECT count(*) FROM payroll.pengajuan_transaksi_pegawai) AS total_pengajuan,
       (SELECT count(*) FROM payroll.pengajuan_transaksi_pegawai WHERE setujui = true)
           AS pengajuan_disetujui,
       (SELECT count(*) FROM payroll.transaksi_pegawai WHERE pengajuan_transaksi_pegawai IS NOT NULL)
           AS baris_turunan_pengajuan_saat_ini,
       (SELECT count(*) FROM payroll.transaksi_pegawai
            WHERE pengajuan_transaksi_pegawai IS NOT NULL AND posting_history IS NOT NULL)
           AS baris_turunan_sudah_diposting,
       (SELECT count(*) FROM payroll.transaksi_pegawai
            WHERE pengajuan_transaksi_pegawai IS NOT NULL AND pembayaran_gaji_punya_pegawai IS NOT NULL)
           AS baris_turunan_sudah_dilunasi;

-- =====================================================================================
-- CARA MEMBACA HASIL
-- * Q0 kosong -> tidak ada FK fisik menjaga referensi ini; Q1 adalah ukuran langsung
--   kerusakan (a). Q0 berisi -> DELETE lama seharusnya gagal saat ada jurnal yang
--   mereferensikannya; bila demikian Q1 seharusnya nol dan kerusakan (a) tidak pernah
--   benar-benar terjadi lewat DELETE ini (meski error itu sendiri berarti operator
--   pernah gagal simpan/centang tanpa pesan yang jelas -- cek log aplikasi terpisah).
-- * Q1/Q2 > 0 -> jurnal yatim NYATA ada di buku besar; Q2 memberi rincian dokumen dan
--   nilai rupiah untuk menilai besaran dan menentukan tindak lanjut (mis. rekonsiliasi
--   manual per baris, atau menandai jurnal itu untuk koreksi/pembalik).
-- * Q3 > 0 untuk suatu pengajuan -> pengajuan itu terbukti pernah kehilangan baris
--   angsuran yang sudah terposting/terlunasi akibat DELETE lama; periksa manual jurnal
--   dan slip gaji pegawai terkait pada rentang tanggal revisi Envers yang bersangkutan
--   untuk memastikan tidak ada posting ganda atau potongan ulang.
-- * Bila Q1, Q2, dan Q3 seluruhnya nol -> kemungkinan besar tidak ada pengajuan yang
--   pernah disunting/dicentang-cabut SETELAH baris angsurannya sempat terposting atau
--   terlunasi di instalasi ini; cacatnya tetap ada di kode lama tetapi belum terpicu
--   pada data yang tersimpan.
-- =====================================================================================
