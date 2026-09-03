-- Integritas finansial: cegah satu akun terpetakan ke lebih dari satu baris
-- akunting.kelompok_laporan_punya_akun pada JENIS LAPORAN yang sama (Neraca /
-- Rugi Laba / Arus Kas). Duplikasi semacam ini melipatgandakan nominal di
-- LaporanKeuanganCoaHelper, dashboard akuntansi (DashboardAkuntingHelper dkk.),
-- dan -- paling berat -- jurnal penutup TutupBukuHelper, karena seluruh
-- konsumen tersebut melakukan INNER JOIN pada kolom akun tanpa membatasi ke
-- satu kelompok laporan tertentu.
--
-- Penjaga sisi aplikasi (KelompokLaporanPunyaAkunAction.onSave,
-- KelompokLaporanDanDetailAction.onSave, dan fitur "copy dari" di keduanya)
-- sudah menolak duplikasi baru sejak commit yang menyertakan migrasi ini.
-- Skrip ini menutup jalur yang TIDAK melalui kedua onSave tersebut (impor
-- Excel, cek_pemetaan_akun.jsp, PemetaanAkunHelper, dan penulis masa depan)
-- lewat trigger DB, plus indeks unik untuk pasangan (akun, kelompok_laporan)
-- persis.
--
-- Aman dijalankan berulang (CREATE OR REPLACE / IF NOT EXISTS di semua DDL).
-- Trigger HANYA menolak baris BARU yang bentrok; baris duplikat historis yang
-- sudah ada TIDAK disentuh/dihapus oleh skrip ini -- lihat AUDIT di bagian
-- akhir untuk mengukur dan membersihkannya secara terpisah setelah ditinjau.

BEGIN;

-- Trigger: tolak INSERT/UPDATE yang membuat akun yang sama muncul di lebih
-- dari satu kelompok laporan pada jenis laporan yang sama.
CREATE OR REPLACE FUNCTION akunting.f_cegah_duplikasi_kelompok_laporan_punya_akun()
RETURNS trigger AS $$
DECLARE
    v_jenis_laporan_baru bigint;
    v_bentrok_id bigint;
    v_bentrok_keterangan text;
BEGIN
    SELECT jenis_laporan INTO v_jenis_laporan_baru
      FROM akunting.kelompok_laporan
     WHERE id = NEW.kelompok_laporan;

    IF v_jenis_laporan_baru IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT k.id, k.keterangan INTO v_bentrok_id, v_bentrok_keterangan
      FROM akunting.kelompok_laporan_punya_akun p
      JOIN akunting.kelompok_laporan k ON k.id = p.kelompok_laporan
     WHERE p.akun = NEW.akun
       AND k.jenis_laporan = v_jenis_laporan_baru
       AND p.id IS DISTINCT FROM NEW.id
     LIMIT 1;

    IF v_bentrok_id IS NOT NULL THEN
        RAISE EXCEPTION 'Akun % sudah terpetakan ke Kelompok Laporan % (%) pada jenis laporan yang sama; satu akun tidak boleh terhitung di dua baris pada jenis laporan yang sama.',
            NEW.akun, v_bentrok_id, COALESCE(v_bentrok_keterangan, '-');
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cegah_duplikasi_kelompok_laporan_punya_akun
    ON akunting.kelompok_laporan_punya_akun;
CREATE TRIGGER trg_cegah_duplikasi_kelompok_laporan_punya_akun
    BEFORE INSERT OR UPDATE OF akun, kelompok_laporan
    ON akunting.kelompok_laporan_punya_akun
    FOR EACH ROW
    EXECUTE PROCEDURE akunting.f_cegah_duplikasi_kelompok_laporan_punya_akun();

-- Indeks unik untuk pasangan (akun, kelompok_laporan) persis -- rem tambahan
-- yang murah dan tidak bergantung pada trigger. Dibungkus DO agar migrasi
-- tetap aman dijalankan meski pasangan duplikat PERSIS SAMA sudah ada di data
-- lama (jarang -- kebanyakan duplikasi historis lintas KELOMPOK berbeda, bukan
-- kelompok yang sama persis); bila memang ada, indeks dilewati dan dilaporkan
-- lewat RAISE NOTICE, bukan menggagalkan seluruh migrasi.
DO $$
DECLARE
    v_dupe_pasangan_persis bigint;
BEGIN
    SELECT count(*) INTO v_dupe_pasangan_persis
      FROM (
          SELECT akun, kelompok_laporan
            FROM akunting.kelompok_laporan_punya_akun
           GROUP BY akun, kelompok_laporan
          HAVING count(*) > 1
      ) d;

    IF v_dupe_pasangan_persis = 0 THEN
        IF NOT EXISTS (SELECT 1 FROM pg_indexes
                        WHERE schemaname = 'akunting'
                          AND indexname = 'uq_kelompok_laporan_punya_akun_pasangan') THEN
            CREATE UNIQUE INDEX uq_kelompok_laporan_punya_akun_pasangan
                ON akunting.kelompok_laporan_punya_akun (akun, kelompok_laporan);
        END IF;
    ELSE
        RAISE NOTICE 'Indeks unik (akun, kelompok_laporan) DILEWATI: % pasangan persis-sama duplikat ditemukan. Bersihkan dulu (lihat query AUDIT 1 di bawah), lalu jalankan ulang skrip ini.',
            v_dupe_pasangan_persis;
    END IF;
END $$;

COMMIT;

-- ===================== AUDIT (jalankan terpisah, hanya-baca) =====================

-- AUDIT 1: pasangan (akun, kelompok_laporan) PERSIS SAMA yang terduplikasi
-- (baris sungguh kembar -- kandidat DELETE langsung setelah ditinjau, sisakan
-- salah satu id per grup).
SELECT akun, kelompok_laporan, count(*) AS jumlah_baris, array_agg(id ORDER BY id) AS id_baris
  FROM akunting.kelompok_laporan_punya_akun
 GROUP BY akun, kelompok_laporan
HAVING count(*) > 1
 ORDER BY count(*) DESC;

-- AUDIT 2: akun yang terpetakan ke lebih dari satu KELOMPOK BERBEDA pada JENIS
-- LAPORAN YANG SAMA -- inilah yang melipatgandakan nominal di
-- LaporanKeuanganCoaHelper/dashboard/TutupBukuHelper. Kolom kelompok_ids /
-- keterangan_kelompok menunjukkan ke mana saja akun ini "bocor".
SELECT p.akun, a.kode AS kode_akun, a.nama AS nama_akun,
       k.jenis_laporan, jl.keterangan AS jenis_laporan_keterangan,
       count(DISTINCT p.kelompok_laporan) AS jumlah_kelompok,
       array_agg(DISTINCT p.kelompok_laporan) AS kelompok_ids,
       array_agg(DISTINCT k.keterangan) AS keterangan_kelompok
  FROM akunting.kelompok_laporan_punya_akun p
  JOIN akunting.kelompok_laporan k ON k.id = p.kelompok_laporan
  JOIN akunting.jenis_laporan jl ON jl.id = k.jenis_laporan
  LEFT JOIN akunting.akun a ON a.id = p.akun
 WHERE (k.aktif IS NULL OR k.aktif)
 GROUP BY p.akun, a.kode, a.nama, k.jenis_laporan, jl.keterangan
HAVING count(DISTINCT p.kelompok_laporan) > 1
 ORDER BY jumlah_kelompok DESC;

-- AUDIT 3: dampak langsung pada jurnal penutup -- akun dari AUDIT 2 yang jenis
-- laporannya "Rugi Laba" (persis filter TutupBukuHelper) DAN sudah punya
-- transaksi terposting. Setiap baris di sini berarti tutup buku yang PERNAH
-- dijalankan sebelum perbaikan ini kemungkinan memposting nominal berlipat
-- untuk akun tersebut -- perlu ditinjau manual (bandingkan jurnal penutup yang
-- sudah terposting dengan saldo akun sesungguhnya pada periode terkait).
SELECT DISTINCT p.akun, a.kode AS kode_akun, a.nama AS nama_akun
  FROM akunting.kelompok_laporan_punya_akun p
  JOIN akunting.kelompok_laporan k ON k.id = p.kelompok_laporan
  JOIN akunting.jenis_laporan jl ON jl.id = k.jenis_laporan
  LEFT JOIN akunting.akun a ON a.id = p.akun
 WHERE (k.aktif IS NULL OR k.aktif)
   AND (lower(coalesce(jl.keterangan,'')) LIKE '%laba%' OR lower(coalesce(jl.keterangan,'')) LIKE '%rugi%')
   AND p.akun IN (
       SELECT p2.akun
         FROM akunting.kelompok_laporan_punya_akun p2
         JOIN akunting.kelompok_laporan k2 ON k2.id = p2.kelompok_laporan
        WHERE (k2.aktif IS NULL OR k2.aktif)
        GROUP BY p2.akun
       HAVING count(DISTINCT p2.kelompok_laporan) > 1
   )
   AND EXISTS (SELECT 1 FROM akunting.transaksi t WHERE t.akun = p.akun)
 ORDER BY kode_akun;
