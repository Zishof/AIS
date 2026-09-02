-- =====================================================================================
-- Uji kesetaraan §17: bagan akun tenant -- pembuatan akun, dan saldo normalnya
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v18:
--   psql -h 127.0.0.1 -p 55516 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-bagan-akun.sql
--
-- Berkas ini mengandaikan schema tenant bernama coa18.
--
-- LATAR
--   Sampai §17, jalur tenant MENOLAK pembuatan akun. Tidak ada penyemai bagan akun di katalog
--   migrasi dan coaSave satu-satunya penulis tabel akun, sehingga tabelnya kosong selamanya --
--   dan jurnal_detail.akun_id bersifat NOT NULL REFERENCES akun(id). Yang tertutup bukan satu
--   layar master, melainkan seluruh pembukuan tenant.
--
-- LIMA HAL YANG DIUJI
--   1. Kelas akun diwarisi dari induknya. Itu definisi, bukan tebakan.
--   2. saldo_normal BUKAN turunan dari tipe: akun lawan berkelas ASET bersaldo normal KREDIT.
--      Blok 2 penjaganya -- menurunkannya dari kelas akan membalik tandanya.
--   3. Kolom kelima coaList wajib bertipe integer. Pembacanya memanggil rs.getInt(5), dan
--      ekspresi LAMA bertipe character varying: getInt atasnya melempar "Bad value for type
--      int", termasuk saat kolomnya kosong.
--   4. Penjaga lingkaran pada penggantian induk.
--   5. Pembaruan "ganti bila diberi": permintaan yang hanya mengubah nama tidak boleh
--      menghapus induk dan saldo normal akun. Blok 5 penjaganya.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\pset format aligned

TRUNCATE coa18.akun CASCADE;

-- Bagan uji. Kolom dan nilainya persis yang dikeluarkan sisipAkun().
INSERT INTO coa18.akun (kode, nama, tipe, induk_id, saldo_normal, posting_diizinkan, aktif,
                        dibuat_pada, oleh)
     VALUES ('1000', 'Kas',             'ASET',      NULL, 'D', true, true, now(), 'uji');
INSERT INTO coa18.akun (kode, nama, tipe, induk_id, saldo_normal, posting_diizinkan, aktif,
                        dibuat_pada, oleh)
     SELECT '1100', 'Kas Kecil', a.tipe, a.id, 'D', true, true, now(), 'uji'
       FROM coa18.akun a WHERE a.kode = '1000';
INSERT INTO coa18.akun (kode, nama, tipe, induk_id, saldo_normal, posting_diizinkan, aktif,
                        dibuat_pada, oleh)
     VALUES ('1900', 'Akm. Penyusutan', 'ASET',      NULL, 'K', true, true, now(), 'uji');
INSERT INTO coa18.akun (kode, nama, tipe, induk_id, saldo_normal, posting_diizinkan, aktif,
                        dibuat_pada, oleh)
     VALUES ('2000', 'Hutang Usaha',    'KEWAJIBAN', NULL, 'K', true, true, now(), 'uji');

\echo ''
\echo '== BLOK 1: kelas akun diwarisi dari induk ========================================='
\echo '   1100 dibuat TANPA menyebut tipe; kelasnya datang dari 1000.'

SELECT anak.kode AS anak, anak.tipe AS "kelas anak", induk.kode AS induk,
       induk.tipe AS "kelas induk",
       CASE WHEN anak.tipe = induk.tipe THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM coa18.akun anak JOIN coa18.akun induk ON anak.induk_id = induk.id
 WHERE anak.kode = '1100';

\echo ''
\echo '== BLOK 2 (PENJAGA): saldo_normal bukan turunan dari tipe ========================='
\echo '   1900 Akumulasi Penyusutan berkelas ASET tetapi bersaldo normal KREDIT.'
\echo '   Kalau saldo normal diturunkan dari kelasnya, ia menjadi D -- tandanya terbalik.'
\echo '   Kolom "bila diturunkan" HARUS berbeda dari "tersimpan"; kalau sama, contoh ini'
\echo '   tidak membuktikan apa pun.'

SELECT a.kode, a.tipe, a.saldo_normal AS "tersimpan",
       CASE WHEN a.tipe IN ('ASET','BEBAN') THEN 'D' ELSE 'K' END AS "bila diturunkan",
       CASE WHEN a.saldo_normal <> CASE WHEN a.tipe IN ('ASET','BEBAN') THEN 'D' ELSE 'K' END
            THEN 'LULUS (menyimpannya memang perlu)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil
  FROM coa18.akun a WHERE a.kode = '1900';

\echo ''
\echo '== BLOK 3: kolom kelima coaList wajib integer ====================================='
\echo '   Pembacanya memanggil rs.getInt(5). Ekspresi LAMA bertipe character varying --'
\echo '   getInt atasnya melempar "Bad value for type int", juga saat kolomnya kosong.'

SELECT pg_typeof(COALESCE(a.saldo_normal,''))::text AS "tipe ekspresi LAMA",
       pg_typeof(CASE WHEN a.saldo_normal = 'D' THEN 1
                      WHEN a.saldo_normal = 'K' THEN -1 ELSE 0 END)::text AS "tipe ekspresi BARU",
       CASE WHEN pg_typeof(COALESCE(a.saldo_normal,''))::text <> 'integer'
             AND pg_typeof(CASE WHEN a.saldo_normal = 'D' THEN 1
                                WHEN a.saldo_normal = 'K' THEN -1 ELSE 0 END)::text = 'integer'
            THEN 'LULUS (yang lama bukan bilangan, yang baru integer)'
            ELSE 'GAGAL' END AS hasil
  FROM coa18.akun a WHERE a.kode = '1000';

\echo '   Sandinya mengikuti Akun.DEBET=1 / Akun.CREDIT=-1, dan 0 untuk yang tak diisi --'
\echo '   persis yang dikembalikan getInt atas kolom legacy yang NULL.'

SELECT a.kode, a.saldo_normal,
       CASE WHEN a.saldo_normal = 'D' THEN 1
            WHEN a.saldo_normal = 'K' THEN -1 ELSE 0 END AS "debetCredit",
       CASE WHEN (a.kode = '1000' AND a.saldo_normal = 'D') OR (a.kode = '2000' AND a.saldo_normal = 'K')
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM coa18.akun a WHERE a.kode IN ('1000','2000') ORDER BY a.kode;

\echo ''
\echo '== BLOK 4: penjaga lingkaran pada penggantian induk ==============================='
\echo '   Memasang 1100 (anak) sebagai induk 1000 akan membentuk lingkaran; 2000 tidak.'

WITH RECURSIVE turunan(id, dalam) AS (
      SELECT id, 1 FROM coa18.akun WHERE induk_id = (SELECT id FROM coa18.akun WHERE kode = '1000')
      UNION ALL
      SELECT a.id, t.dalam + 1 FROM coa18.akun a JOIN turunan t ON a.induk_id = t.id
       WHERE t.dalam < 64)
SELECT (SELECT COUNT(*) FROM turunan
         WHERE id = (SELECT id FROM coa18.akun WHERE kode = '1100')) AS "1100 keturunan 1000",
       (SELECT COUNT(*) FROM turunan
         WHERE id = (SELECT id FROM coa18.akun WHERE kode = '2000')) AS "2000 keturunan 1000",
       CASE WHEN (SELECT COUNT(*) FROM turunan
                   WHERE id = (SELECT id FROM coa18.akun WHERE kode = '1100')) = 1
             AND (SELECT COUNT(*) FROM turunan
                   WHERE id = (SELECT id FROM coa18.akun WHERE kode = '2000')) = 0
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 5 (PENJAGA): "ganti bila diberi" pada pembaruan ==========================='
\echo '   Permintaan yang hanya mengganti nama mengirim induk & saldo normal sebagai NULL.'
\echo '   Dengan COALESCE keduanya bertahan. Tanpanya, akunnya kehilangan induk dan saldo'
\echo '   normalnya -- angka "sesudah polos" di bawah HARUS 0/kosong, kalau tidak, contoh ini'
\echo '   tidak membedakan apa pun.'

UPDATE coa18.akun SET kode = '1100', nama = 'Kas Kecil Cabang',
       induk_id = COALESCE(NULL::bigint, induk_id),
       saldo_normal = COALESCE(NULL::varchar, saldo_normal),
       tanggal_dirubah = now(), oleh = 'uji'
 WHERE kode = '1100';

SELECT a.kode, a.nama, COALESCE(a.induk_id,0) AS "induk sesudah COALESCE",
       COALESCE(a.saldo_normal,'(kosong)') AS "saldo sesudah COALESCE",
       (SELECT COUNT(*) FROM coa18.akun b
         WHERE b.kode = '1100' AND b.induk_id IS NOT NULL) AS "masih punya induk",
       CASE WHEN a.induk_id IS NOT NULL AND a.saldo_normal = 'D' AND a.nama = 'Kas Kecil Cabang'
            THEN 'LULUS (nama berubah, induk & saldo bertahan)'
            ELSE 'GAGAL' END AS hasil
  FROM coa18.akun a WHERE a.kode = '1100';

\echo '   Penjaganya: pola polos "induk_id = ?" dengan NULL pada baris salinan.'

CREATE TEMP TABLE polos AS SELECT * FROM coa18.akun WHERE kode = '1100';
UPDATE polos SET induk_id = NULL::bigint, saldo_normal = NULL::varchar;

SELECT COALESCE(p.induk_id,0) AS "induk sesudah polos",
       COALESCE(p.saldo_normal,'(kosong)') AS "saldo sesudah polos",
       CASE WHEN p.induk_id IS NULL AND p.saldo_normal IS NULL
            THEN 'LULUS (pola polos memang menghapusnya)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil
  FROM polos p;

\echo ''
\echo '== BLOK 6: level sengaja tidak disimpan ==========================================='
\echo '   level adalah kedalaman pada pohon induk_id -- turunan penuh tanpa kekecualian.'
\echo '   Menyimpannya berarti menanggungnya selamanya: memindahkan satu akun membuat level'
\echo '   seluruh keturunannya salah, dan salahnya baru terlihat saat pohonnya digambar.'

SELECT COUNT(*) AS "baris akun", COUNT(level) AS "yang ber-level",
       CASE WHEN COUNT(level) = 0 THEN 'LULUS (dibiarkan NULL, dihitung dari induk_id)'
            ELSE 'GAGAL' END AS hasil
  FROM coa18.akun;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
