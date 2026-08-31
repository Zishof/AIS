-- =====================================================================================
-- Diagnosa dampak data lama: tabrakan kodeUnik antar-kaki jurnal tagihan siswa
--
-- Latar: sampai r78693, keempat kaki jurnal satu Tagihan (piutang, denda, diskon,
-- dibayar dimuka) menulis dengan ref NULL. GrupTransaksi.ambilUnik() menyusun kodeUnik
-- dari kelas+id dokumen+ref, sehingga keempatnya berbagi satu kunci dan
-- CommonAkunting.saveTransaksi memperlakukan kaki kedua dst. sebagai DUPLIKAT: jurnalnya
-- TIDAK ditulis, dan grup kaki pertama hanya DICAP ULANG dengan riwayat kaki berikutnya.
-- Rincian dan bukti: docs/pos/71-tabrakan-kodeunik-antar-kaki.md
--
-- Skrip ini HANYA-BACA (seluruhnya SELECT; tidak ada INSERT/UPDATE/DELETE/DDL) dan aman
-- dijalankan pada basis data produksi, sebaiknya memakai pengguna read-only.
--
-- Tanda dampak yang dicari: sebuah KAKI yang capnya terisi ("sudah diposting") tetapi
-- TIDAK punya grup jurnal yang menunjuk riwayat itu. Itulah jurnal yang hilang.
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- Q0. Konteks: seberapa banyak tagihan yang benar-benar pernah diposting.
--     Bila hasilnya nol, modul siswa tidak dipakai dan tidak ada dampak sama sekali.
-- -------------------------------------------------------------------------------------
SELECT 'tagihan bercap minimal satu kaki' AS keterangan, count(*) AS jumlah
FROM sekolah.tagihan
WHERE posting_history_id IS NOT NULL
   OR posting_history_denda_id IS NOT NULL
   OR posting_history_diskon_id IS NOT NULL
   OR posting_history_uang_muka_id IS NOT NULL
UNION ALL
SELECT 'tagihan bercap LEBIH DARI SATU kaki (kandidat tabrakan)', count(*)
FROM sekolah.tagihan
WHERE (CASE WHEN posting_history_id IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN posting_history_denda_id IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN posting_history_diskon_id IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN posting_history_uang_muka_id IS NOT NULL THEN 1 ELSE 0 END) > 1
UNION ALL
SELECT 'grup jurnal yang menunjuk tagihan', count(*)
FROM akunting.grup_transaksi WHERE tagihan IS NOT NULL;

-- -------------------------------------------------------------------------------------
-- Q1. RINGKASAN DAMPAK per kaki: cap terisi tetapi jurnalnya tidak ada.
--     Angka di kolom "nilai_seharusnya" adalah taksiran nilai yang tidak pernah masuk
--     buku besar (diambil dari kolom nominal/denda/diskon dokumennya).
-- -------------------------------------------------------------------------------------
WITH hilang AS (
    SELECT 'piutang'  AS kaki, t.id, t.posting_history_id          AS riwayat, t.nominal AS nilai
    FROM sekolah.tagihan t
    WHERE t.posting_history_id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM akunting.grup_transaksi g
                      WHERE g.tagihan = t.id AND g.posting_history = t.posting_history_id)
    UNION ALL
    SELECT 'denda', t.id, t.posting_history_denda_id, t.denda
    FROM sekolah.tagihan t
    WHERE t.posting_history_denda_id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM akunting.grup_transaksi g
                      WHERE g.tagihan = t.id AND g.posting_history = t.posting_history_denda_id)
    UNION ALL
    SELECT 'diskon', t.id, t.posting_history_diskon_id, t.diskon
    FROM sekolah.tagihan t
    WHERE t.posting_history_diskon_id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM akunting.grup_transaksi g
                      WHERE g.tagihan = t.id AND g.posting_history = t.posting_history_diskon_id)
    UNION ALL
    SELECT 'dibayar dimuka', t.id, t.posting_history_uang_muka_id, t.nominal
    FROM sekolah.tagihan t
    WHERE t.posting_history_uang_muka_id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM akunting.grup_transaksi g
                      WHERE g.tagihan = t.id AND g.posting_history = t.posting_history_uang_muka_id)
)
SELECT kaki,
       count(*)                              AS dokumen_terdampak,
       COALESCE(sum(COALESCE(nilai, 0)), 0)  AS nilai_seharusnya
FROM hilang
GROUP BY kaki
ORDER BY kaki;

-- -------------------------------------------------------------------------------------
-- Q2. RINCIAN dokumen terdampak (untuk triase manual). Batasi bila hasilnya besar.
-- -------------------------------------------------------------------------------------
SELECT t.id                                   AS tagihan_id,
       t.informasi,
       t.posting_history_id                   AS cap_piutang,
       t.posting_history_denda_id             AS cap_denda,
       t.posting_history_diskon_id            AS cap_diskon,
       t.posting_history_uang_muka_id         AS cap_dimuka,
       (SELECT count(*) FROM akunting.grup_transaksi g WHERE g.tagihan = t.id) AS jumlah_grup_jurnal,
       (CASE WHEN t.posting_history_id IS NOT NULL THEN 1 ELSE 0 END
      + CASE WHEN t.posting_history_denda_id IS NOT NULL THEN 1 ELSE 0 END
      + CASE WHEN t.posting_history_diskon_id IS NOT NULL THEN 1 ELSE 0 END
      + CASE WHEN t.posting_history_uang_muka_id IS NOT NULL THEN 1 ELSE 0 END) AS jumlah_kaki_tercap
FROM sekolah.tagihan t
WHERE (CASE WHEN t.posting_history_id IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN t.posting_history_denda_id IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN t.posting_history_diskon_id IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN t.posting_history_uang_muka_id IS NOT NULL THEN 1 ELSE 0 END)
      > (SELECT count(*) FROM akunting.grup_transaksi g WHERE g.tagihan = t.id)
ORDER BY t.id
LIMIT 200;

-- -------------------------------------------------------------------------------------
-- Q3. Gejala kedua: LABEL grup berpindah. Grup jurnal yang jenisnya mengikuti riwayat
--     kaki lain — yaitu grup yang capnya TIDAK cocok dengan kaki mana pun yang wajar.
--     Grup ber-ref NULL semestinya milik kaki PIUTANG; bila posting_history-nya justru
--     sama dengan cap denda/diskon/dimuka, itu bekas tabrakan.
-- -------------------------------------------------------------------------------------
SELECT g.id            AS grup_id,
       g.tagihan       AS tagihan_id,
       g.ref,
       ph.jenis        AS jenis_riwayat_terpasang,
       CASE
           WHEN g.posting_history = t.posting_history_denda_id     THEN 'cap DENDA'
           WHEN g.posting_history = t.posting_history_diskon_id    THEN 'cap DISKON'
           WHEN g.posting_history = t.posting_history_uang_muka_id THEN 'cap DIBAYAR DIMUKA'
           ELSE 'lainnya'
       END             AS cap_yang_cocok
FROM akunting.grup_transaksi g
JOIN sekolah.tagihan t       ON t.id = g.tagihan
LEFT JOIN akunting.posting_history ph ON ph.id = g.posting_history
WHERE g.ref IS NULL
  AND g.posting_history IS NOT NULL
  AND g.posting_history <> COALESCE(t.posting_history_id, -1)
ORDER BY g.tagihan, g.id
LIMIT 200;

-- -------------------------------------------------------------------------------------
-- Q4. Cacat LAIN dari kampanye yang sama: kaki LogPembayaran saling menghapus.
--     Sampai r78672, membatalkan kaki biaya administrasi ATAU biaya payment gateway
--     menghapus jurnal kaki lain (kolom referensi sama, tanpa pembeda) sementara capnya
--     tetap terpasang. Gejalanya sama: cap terisi tanpa jurnal.
--     Rincian: docs/pos/70-audit-dokumen-berkaki-ganda.md §2.1
-- -------------------------------------------------------------------------------------
SELECT 'log pembayaran - biaya administrasi' AS kaki, count(*) AS dokumen_terdampak
FROM public.log_pembayaran lp
WHERE lp.posting_history IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM akunting.grup_transaksi g
                  WHERE g.log_pembayaran = lp.id AND g.posting_history = lp.posting_history)
UNION ALL
SELECT 'log pembayaran - biaya payment gateway', count(*)
FROM public.log_pembayaran lp
WHERE lp.posting_history_payment_gateway IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM akunting.grup_transaksi g
                  WHERE g.log_pembayaran = lp.id
                    AND g.posting_history = lp.posting_history_payment_gateway);

-- -------------------------------------------------------------------------------------
-- Q5. Cacat LAIN: jurnal YATIM pada cicilan mahasiswa.
--     Sampai r78673, tombol batal di layar cicilan memakai saringan "ref != 'dimuka'"
--     yang tidak pernah mengenai baris ber-ref NULL (semantik tiga-nilai SQL), sehingga
--     capnya dilepas tetapi jurnalnya tertinggal di buku besar.
--     Rincian: docs/pos/70-audit-dokumen-berkaki-ganda.md §2.2
-- -------------------------------------------------------------------------------------
SELECT count(*) AS jurnal_yatim_cicilan,
       COALESCE(sum(x.nilai), 0) AS nilai_masih_di_buku_besar
FROM (
    SELECT g.id,
           (SELECT COALESCE(sum(tr.debet), 0) FROM akunting.transaksi tr
            WHERE tr.grup_transaksi = g.id) AS nilai
    FROM akunting.grup_transaksi g
    JOIN public.cicilan_pembayaran cp ON cp.id = g.cicilan_pembayaran
    WHERE g.ref IS NULL
      AND g.closing IS NULL
      AND cp.posting_history IS NULL      -- cap sudah dilepas, tetapi jurnalnya masih ada
) x;

-- -------------------------------------------------------------------------------------
-- Q6. Cacat LAIN: jurnal pengembalian uang muka yang saling meniadakan (Dr X / Cr X).
--     Sampai r78539, kedua tombol layar ZK menulis akun yang SAMA di kedua sisi.
--     Jurnal semacam itu tidak mengubah saldo apa pun -- pengembaliannya tidak pernah
--     benar-benar tercatat. Rincian: docs/pos/54-posting-pengembalian-uang-muka.md §2a
-- -------------------------------------------------------------------------------------
SELECT count(*) AS grup_dr_x_cr_x,
       COALESCE(sum(x.nilai), 0) AS nilai_yang_tidak_pernah_tercatat
FROM (
    SELECT g.id, sum(tr.debet) AS nilai
    FROM akunting.grup_transaksi g
    JOIN akunting.transaksi tr ON tr.grup_transaksi = g.id
    WHERE g.ref = 'pengembalian'
    GROUP BY g.id
    HAVING count(DISTINCT tr.akun) = 1        -- seluruh barisnya memakai satu akun saja
       AND sum(tr.debet) > 0 AND sum(tr.kredit) > 0
) x;

-- =====================================================================================
-- CARA MEMBACA HASIL
--
-- * Q0 nol pada baris "grup jurnal yang menunjuk tagihan"  -> modul siswa tidak pernah
--   diposting di basis data ini; TIDAK ADA dampak, selesai.
-- * Q1 semua nol                                            -> tidak ada jurnal yang hilang.
-- * Q1 ada isinya                                           -> tiap baris adalah kaki yang
--   dicap "sudah diposting" tetapi jurnalnya tidak pernah ada. "nilai_seharusnya" adalah
--   taksiran nilai yang belum masuk buku besar untuk kaki itu.
-- * Q3 ada isinya                                           -> grup kaki piutang yang
--   labelnya terlanjur berpindah ke jenis kaki lain (gejala kedua tabrakan yang sama).
-- * Q4 ada isinya  -> kaki biaya administrasi / payment gateway yang jurnalnya terhapus
--   oleh pembatalan kaki sebelahnya. Pemulihan: lepas capnya, lalu posting ulang.
-- * Q5 ada isinya  -> jurnal cicilan yang tertinggal padahal postingnya sudah dibatalkan;
--   nilainya MASIH ikut terhitung di buku besar. Pemulihan: hapus grup jurnal yatim itu
--   (hanya yang closing-nya kosong), atau posting ulang dokumennya bila memang seharusnya
--   terposting.
-- * Q6 ada isinya  -> jurnal pengembalian uang muka bernilai nol-efek (Dr dan Cr pada akun
--   yang sama). Pemulihan: batalkan posting pengembaliannya lalu posting ulang; sejak
--   r78539 pasangan akunnya sudah benar.
--
-- CATATAN PENTING: Q1 juga akan memunculkan kaki yang capnya terpasang padahal jurnalnya
-- GAGAL disimpan karena sebab lain (idiom layar ZK lama mengecap dokumen meskipun
-- saveTransaksi gagal). Keduanya sama-sama "dicap terposting tanpa jurnal" dan sama-sama
-- perlu diposting ulang, jadi tidak perlu dipisahkan untuk keperluan pembersihan.
--
-- LANGKAH PEMULIHAN (sesudah angkanya diketahui, dan HANYA atas persetujuan bagian
-- keuangan): untuk tiap kaki terdampak, lepaskan capnya agar dokumen kembali menjadi draf,
-- lalu jalankan posting ulang dari layar/dasbor kaki tersebut. Sejak r78693 tiap kaki
-- memakai ref sendiri sehingga posting ulang menghasilkan jurnal terpisah dan tidak lagi
-- bertabrakan. Jangan menyunting jurnal lama secara manual, dan jangan menyentuh periode
-- yang sudah ditutup buku tanpa membukanya lebih dulu.
-- =====================================================================================
