-- =====================================================================================
-- Uji kesetaraan siklus status giro (payableBgStatus / collectionBgStatus)
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v17:
--   psql -h 127.0.0.1 -p 55512 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-status-giro.sql
--
-- Berkas ini mengandaikan schema tenant bernama giro17.
--
-- EMPAT HAL YANG DIUJI
--   1. NULL berarti DITERIMA -- giro baru diterima, belum ada kabarnya.
--   2. tanggal_bg dan tanggal_status_bg BERBEDA dan tidak boleh tertukar.
--   3. Giro DITOLAK menerbitkan pembalikan yang memulihkan sisa piutang.
--      Blok 4 penjaganya: tanpa pembalikan, sisanya TIDAK pulih.
--   4. Status yang sudah final terbaca final, sehingga penjaga sisi Java punya dasar
--      untuk menolak perubahan kedua.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

-- Dijalankan berulang: kembalikan ke keadaan awal.
DELETE FROM giro17.alokasi_penerimaan_piutang WHERE penerimaan_piutang_id <> 11;
DELETE FROM giro17.penerimaan_piutang WHERE id <> 11;
UPDATE giro17.penerimaan_piutang SET status_bg = NULL, tanggal_status_bg = NULL,
       status = 'AKTIF' WHERE id = 11;
UPDATE giro17.pembayaran_hutang SET status_bg = NULL, tanggal_status_bg = NULL WHERE id = 1;

\pset format aligned

\echo ''
\echo '== BLOK 1: NULL berarti DITERIMA -- belum ada kabarnya ============================'
\echo '   Dokumen giro yang baru dicatat tidak boleh mengaku sudah cair maupun ditolak.'

SELECT COALESCE(cara_bayar,'') AS "metode", status_bg AS "status giro",
       CASE WHEN cara_bayar = 'GIRO' AND status_bg IS NULL
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM giro17.pembayaran_hutang WHERE id = 1;

-- Giro hutang dinyatakan CAIR pada 10 Desember; lembarnya bertanggal 30 Desember.
UPDATE giro17.pembayaran_hutang SET status_bg = 'CAIR', tanggal_status_bg = DATE '2026-12-10',
       tanggal_dirubah = now() WHERE id = 1;

\echo ''
\echo '== BLOK 2: tanggal lembar giro dan tanggal kabarnya BERBEDA ======================='
\echo '   Lembar bertanggal 30 Des, kabarnya diketahui 10 Des. Menyatukannya menghilangkan satu.'

SELECT tanggal_bg AS "tanggal lembar", tanggal_status_bg AS "tanggal kabar",
       CASE WHEN tanggal_bg = DATE '2026-12-30' AND tanggal_status_bg = DATE '2026-12-10'
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM giro17.pembayaran_hutang WHERE id = 1;

\echo ''
\echo '== BLOK 3: status yang sudah final TERBACA final ================================='
\echo '   Penjaga sisi Java menolak perubahan kedua; ia butuh nilai ini untuk melakukannya.'

SELECT status_bg AS "status giro",
       CASE WHEN status_bg = 'CAIR' THEN 'LULUS (penjaga punya dasar menolak)'
            ELSE 'GAGAL' END AS hasil
FROM giro17.pembayaran_hutang WHERE id = 1;

-- =====================================================================================
-- Sisi piutang: giro DITOLAK, lalu pembalikannya diterbitkan -- persis urutan bgStatus
-- diikuti collectionReverse.
-- =====================================================================================
UPDATE giro17.penerimaan_piutang SET status_bg = 'TOLAK', tanggal_status_bg = DATE '2026-12-11',
       tanggal_dirubah = now() WHERE id = 11;

\echo ''
\echo '== BLOK 4 (PENJAGA): SEBELUM dibalik, sisa piutang masih berkurang ==============='
\echo '   Giro ditolak berarti uangnya tak pernah berpindah -- tetapi sisanya belum pulih.'
\echo '   Sisa harus masih 300.000 di sini; kalau sudah 600.000, blok 5 tidak membuktikan apa pun.'

SELECT (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
          FROM giro17.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0)
        FROM giro17.piutang_customer d WHERE d.id = 900) AS "sisa sebelum dibalik",
       CASE WHEN (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
                    FROM giro17.alokasi_penerimaan_piutang a
                   WHERE a.piutang_customer_id = d.id),0)
                  FROM giro17.piutang_customer d WHERE d.id = 900) = 300000
            THEN 'LULUS (contoh benar-benar membedakan)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

-- Pembalikan otomatis: dokumen cermin negatif + alokasi negatif + asal DIBATALKAN.
INSERT INTO giro17.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id, salesperson_id,
    cara_bayar, nilai, keterangan, idempotency_key, pembalik_dari_id, status, dibuat_pada, oleh)
  VALUES (12, 'REV-KWT-11', DATE '2026-12-11', 5, 7, 'GIRO', -300000,
          'REVERSAL kwitansi KWT-11: BG ditolak bank', 'REV-KWT-11', 11, 'REVERSAL', now(), 'uji');
INSERT INTO giro17.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id, nilai,
    dibuat_pada, oleh)
  SELECT 12, a.piutang_customer_id, -a.nilai, now(), 'uji'
    FROM giro17.alokasi_penerimaan_piutang a WHERE a.penerimaan_piutang_id = 11;
UPDATE giro17.penerimaan_piutang SET status = 'DIBATALKAN', dibatalkan = true,
       dibatalkan_pada = now(), alasan_batal = 'BG ditolak bank' WHERE id = 11;

\echo ''
\echo '== BLOK 5: sesudah dibalik, sisa piutang PULIH penuh =============================='

SELECT (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
          FROM giro17.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0)
        FROM giro17.piutang_customer d WHERE d.id = 900) AS "sisa sesudah dibalik",
       (SELECT status FROM giro17.penerimaan_piutang WHERE id = 11) AS "status asal",
       (SELECT status_bg FROM giro17.penerimaan_piutang WHERE id = 11) AS "status giro",
       CASE WHEN (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
                    FROM giro17.alokasi_penerimaan_piutang a
                   WHERE a.piutang_customer_id = d.id),0)
                  FROM giro17.piutang_customer d WHERE d.id = 900) = 600000
             AND (SELECT status FROM giro17.penerimaan_piutang WHERE id = 11) = 'DIBATALKAN'
             AND (SELECT status_bg FROM giro17.penerimaan_piutang WHERE id = 11) = 'TOLAK'
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 6: status giro ASAL tetap TOLAK, bukan ikut terhapus pembalikan =========='
\echo '   Dokumen dibatalkan, tetapi sebab pembatalannya harus tetap terbaca.'

SELECT status AS "status dokumen", status_bg AS "status giro",
       CASE WHEN status = 'DIBATALKAN' AND status_bg = 'TOLAK'
            THEN 'LULUS (sebab pembatalan tetap terbaca)' ELSE 'GAGAL' END AS hasil
FROM giro17.penerimaan_piutang WHERE id = 11;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
