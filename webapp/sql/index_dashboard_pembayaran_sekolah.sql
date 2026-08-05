-- =====================================================================
-- INDEX OPTIMIZER UNTUK DasboardPembayaranSekolah.java
-- Target utama:
--   loadPembayaranDashboardData()
--   getTagihanAggregate()
--   countTagihan(...)
--   buildBaseTagihanCriteria(...)
--   popup detail / panel piutang per sekolah, item biaya, angkatan, kelas
--
-- DB target: PostgreSQL
-- Aman dijalankan berulang karena memakai CREATE INDEX IF NOT EXISTS.
-- Script ini juga mencoba mendeteksi variasi nama kolom:
--   tahun_ajaran / tahunajaran
--   item_biaya_id / item_biaya_sekolah_id
--   kelas_siswa_id / kelas_siswa
--   tahun_angkatan / tahunangkatan
-- Kolom/tabel yang tidak ditemukan akan dilewati dengan NOTICE.
-- =====================================================================

-- Jalankan pada jam sepi. CREATE INDEX biasa tetap dapat menahan lock write pendek.
-- Setelah selesai, jalankan ANALYZE agar planner segera memakai index baru.

DO $$
BEGIN
    BEGIN
        EXECUTE 'CREATE EXTENSION IF NOT EXISTS pg_trgm';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'pg_trgm tidak dapat dibuat oleh user ini: %. Index trigram ILIKE akan dilewati jika extension belum tersedia.', SQLERRM;
    END;
END $$;

CREATE OR REPLACE FUNCTION pg_temp._col_exists(p_schema text, p_table text, p_col text)
RETURNS boolean AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = p_schema
          AND table_name = p_table
          AND column_name = p_col
    );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION pg_temp._pick_col(p_schema text, p_table text, p_candidates text[])
RETURNS text AS $$
DECLARE
    c text;
BEGIN
    FOREACH c IN ARRAY p_candidates LOOP
        IF pg_temp._col_exists(p_schema, p_table, c) THEN
            RETURN c;
        END IF;
    END LOOP;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION pg_temp._table_exists(p_schema text, p_table text)
RETURNS boolean AS $$
BEGIN
    RETURN to_regclass(format('%I.%I', p_schema, p_table)) IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION pg_temp._create_index(p_index_name text, p_sql text)
RETURNS void AS $$
BEGIN
    IF to_regclass(p_index_name) IS NULL THEN
        EXECUTE p_sql;
        RAISE NOTICE 'Created index: %', p_index_name;
    ELSE
        RAISE NOTICE 'Index already exists: %', p_index_name;
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Skip/create index failed [%]: %', p_index_name, SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 1) INDEX UTAMA TABEL sekolah.tagihan
-- =====================================================================
DO $$
DECLARE
    sch text := 'sekolah';
    tbl text := 'tagihan';

    c_id text;
    c_tahun text;
    c_sekolah text;
    c_item text;
    c_kelas text;
    c_angkatan text;
    c_siswa text;
    c_calon text;
    c_aktif text;
    c_bukan text;
    c_nominal text;
    c_dibayar text;

    pred text := 'TRUE';
    cols text;
BEGIN
    IF NOT pg_temp._table_exists(sch, tbl) THEN
        RAISE NOTICE 'Tabel %.% tidak ditemukan. Index tagihan dilewati.', sch, tbl;
        RETURN;
    END IF;

    c_id       := pg_temp._pick_col(sch, tbl, ARRAY['id']);
    c_tahun    := pg_temp._pick_col(sch, tbl, ARRAY['tahun_ajaran','tahunajaran','tahun_ajaran_id','tahun_akademik','tahunakademik']);
    c_sekolah  := pg_temp._pick_col(sch, tbl, ARRAY['sekolah_id','sekolah']);
    c_item     := pg_temp._pick_col(sch, tbl, ARRAY['item_biaya_id','item_biaya_sekolah_id','itembiayasekolah_id','item_biaya_sekolah']);
    c_kelas    := pg_temp._pick_col(sch, tbl, ARRAY['kelas_siswa_id','kelassiswa_id','kelas_siswa']);
    c_angkatan := pg_temp._pick_col(sch, tbl, ARRAY['tahun_angkatan','tahunangkatan']);
    c_siswa    := pg_temp._pick_col(sch, tbl, ARRAY['siswa_id','siswa']);
    c_calon    := pg_temp._pick_col(sch, tbl, ARRAY['calon_siswa_id','calonsiswa_id','calon_siswa']);
    c_aktif    := pg_temp._pick_col(sch, tbl, ARRAY['aktif']);
    c_bukan    := pg_temp._pick_col(sch, tbl, ARRAY['bukan_tagihan','bukantagihan']);
    c_nominal  := pg_temp._pick_col(sch, tbl, ARRAY['nominal']);
    c_dibayar  := pg_temp._pick_col(sch, tbl, ARRAY['dibayar']);

    IF c_aktif IS NOT NULL THEN
        pred := pred || format(' AND (%I IS NULL OR %I = true)', c_aktif, c_aktif);
    END IF;
    IF c_bukan IS NOT NULL THEN
        pred := pred || format(' AND %I = false', c_bukan);
    END IF;
    IF c_nominal IS NOT NULL THEN
        pred := pred || format(' AND %I > 0.1', c_nominal);
    END IF;

    -- Base dashboard: tahun ajaran + sekolah + item biaya + order id desc.
    IF c_tahun IS NOT NULL AND c_sekolah IS NOT NULL AND c_item IS NOT NULL AND c_id IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_dash_tahun_sekolah_item_id',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I, %I DESC) WHERE %s',
                   'idx_tagihan_dash_tahun_sekolah_item_id', sch, tbl, c_tahun, c_sekolah, c_item, c_id, pred));
    END IF;

    -- Ketika sekolah tidak difilter tetapi item biaya/tahun ajaran difilter.
    IF c_tahun IS NOT NULL AND c_item IS NOT NULL AND c_id IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_dash_tahun_item_id',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I DESC) WHERE %s',
                   'idx_tagihan_dash_tahun_item_id', sch, tbl, c_tahun, c_item, c_id, pred));
    END IF;

    -- Aggregate umum rowCount + sum(nominal) + sum(dibayar).
    IF c_tahun IS NOT NULL AND c_sekolah IS NOT NULL AND c_item IS NOT NULL AND c_nominal IS NOT NULL AND c_dibayar IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_dash_cover_sum',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I) INCLUDE (%I, %I) WHERE %s',
                   'idx_tagihan_dash_cover_sum', sch, tbl, c_tahun, c_sekolah, c_item, c_nominal, c_dibayar, pred));
    END IF;

    -- Fallback untuk PostgreSQL lama yang belum support INCLUDE.
    -- Jika index INCLUDE gagal, index biasa ini tetap berguna.
    IF c_tahun IS NOT NULL AND c_sekolah IS NOT NULL AND c_item IS NOT NULL AND c_nominal IS NOT NULL AND c_dibayar IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_dash_sum_fallback',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I, %I, %I) WHERE %s',
                   'idx_tagihan_dash_sum_fallback', sch, tbl, c_tahun, c_sekolah, c_item, c_nominal, c_dibayar, pred));
    END IF;

    -- Panel existing: group per sekolah + item biaya.
    IF c_tahun IS NOT NULL AND c_sekolah IS NOT NULL AND c_item IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_group_sekolah_item',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I) WHERE %s AND %I IS NOT NULL AND %I IS NOT NULL',
                   'idx_tagihan_group_sekolah_item', sch, tbl, c_tahun, c_sekolah, c_item, pred, c_sekolah, c_item));
    END IF;

    -- Panel existing: group per angkatan + item biaya.
    IF c_tahun IS NOT NULL AND c_angkatan IS NOT NULL AND c_item IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_group_angkatan_item',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I) WHERE %s AND %I IS NOT NULL AND %I IS NOT NULL',
                   'idx_tagihan_group_angkatan_item', sch, tbl, c_tahun, c_angkatan, c_item, pred, c_angkatan, c_item));
    END IF;

    -- Panel existing: group per kelas + item biaya.
    IF c_tahun IS NOT NULL AND c_kelas IS NOT NULL AND c_item IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_group_kelas_item',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I) WHERE %s AND %I IS NOT NULL AND %I IS NOT NULL',
                   'idx_tagihan_group_kelas_item', sch, tbl, c_tahun, c_kelas, c_item, pred, c_kelas, c_item));
    END IF;

    -- Status dashboard: sudah bayar.
    IF c_tahun IS NOT NULL AND c_sekolah IS NOT NULL AND c_item IS NOT NULL AND c_dibayar IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_status_sudah_bayar',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I) WHERE %s AND COALESCE(%I,0) > 0.1',
                   'idx_tagihan_status_sudah_bayar', sch, tbl, c_tahun, c_sekolah, c_item, pred, c_dibayar));
    END IF;

    -- Status dashboard: belum bayar.
    IF c_tahun IS NOT NULL AND c_sekolah IS NOT NULL AND c_item IS NOT NULL AND c_dibayar IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_status_belum_bayar',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I) WHERE %s AND COALESCE(%I,0) <= 0.1',
                   'idx_tagihan_status_belum_bayar', sch, tbl, c_tahun, c_sekolah, c_item, pred, c_dibayar));
    END IF;

    -- Status dashboard: cicilan / parsial.
    IF c_tahun IS NOT NULL AND c_sekolah IS NOT NULL AND c_item IS NOT NULL AND c_nominal IS NOT NULL AND c_dibayar IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_status_cicilan',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I) WHERE %s AND COALESCE(%I,0) > 0.1 AND COALESCE(%I,0) < COALESCE(%I,0)',
                   'idx_tagihan_status_cicilan', sch, tbl, c_tahun, c_sekolah, c_item, pred, c_dibayar, c_dibayar, c_nominal));
    END IF;

    -- Status dashboard: lunas.
    IF c_tahun IS NOT NULL AND c_sekolah IS NOT NULL AND c_item IS NOT NULL AND c_nominal IS NOT NULL AND c_dibayar IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_status_lunas',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I) WHERE %s AND COALESCE(%I,0) >= COALESCE(%I,0)',
                   'idx_tagihan_status_lunas', sch, tbl, c_tahun, c_sekolah, c_item, pred, c_dibayar, c_nominal));
    END IF;

    -- Status dashboard: lebih bayar.
    IF c_tahun IS NOT NULL AND c_sekolah IS NOT NULL AND c_item IS NOT NULL AND c_nominal IS NOT NULL AND c_dibayar IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_status_lebih_bayar',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I, %I) WHERE %s AND COALESCE(%I,0) > COALESCE(%I,0)',
                   'idx_tagihan_status_lebih_bayar', sch, tbl, c_tahun, c_sekolah, c_item, pred, c_dibayar, c_nominal));
    END IF;

    -- Popup detail berdasarkan siswa/calon siswa.
    IF c_siswa IS NOT NULL AND c_tahun IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_siswa_tahun',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I) WHERE %s AND %I IS NOT NULL',
                   'idx_tagihan_siswa_tahun', sch, tbl, c_siswa, c_tahun, pred, c_siswa));
    END IF;

    IF c_calon IS NOT NULL AND c_tahun IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_calon_tahun',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, %I) WHERE %s AND %I IS NOT NULL',
                   'idx_tagihan_calon_tahun', sch, tbl, c_calon, c_tahun, pred, c_calon));
    END IF;

    -- Expression index untuk predicate COALESCE(dibayar,0) dan COALESCE(nominal,0).
    IF c_tahun IS NOT NULL AND c_nominal IS NOT NULL AND c_dibayar IS NOT NULL THEN
        PERFORM pg_temp._create_index('sekolah.idx_tagihan_expr_nominal_dibayar',
            format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I, (COALESCE(%I,0)), (COALESCE(%I,0))) WHERE %s',
                   'idx_tagihan_expr_nominal_dibayar', sch, tbl, c_tahun, c_dibayar, c_nominal, pred));
    END IF;
END $$;

-- =====================================================================
-- 2) INDEX UNTUK KEYWORD SEARCH / ILIKE
-- Dipakai filter dashboardKeyword dan trend pembayaran:
-- siswa.nama_siswa, calon_siswa.nama_siswa, sekolah.nama, item_biaya_sekolah.nama
-- =====================================================================
DO $$
DECLARE
    has_trgm boolean := EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm');
    sch text := 'sekolah';
    t text;
    c text;
    idx text;
BEGIN
    IF NOT has_trgm THEN
        RAISE NOTICE 'pg_trgm belum tersedia. Index GIN trigram dilewati.';
        RETURN;
    END IF;

    -- siswa: nama_siswa / nama dan nomor_induk
    IF pg_temp._table_exists(sch, 'siswa') THEN
        c := pg_temp._pick_col(sch, 'siswa', ARRAY['nama_siswa','nama']);
        IF c IS NOT NULL THEN
            idx := 'sekolah.idx_siswa_nama_trgm';
            PERFORM pg_temp._create_index(idx,
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I USING gin (lower(%I) gin_trgm_ops)',
                       'idx_siswa_nama_trgm', sch, 'siswa', c));
        END IF;

        c := pg_temp._pick_col(sch, 'siswa', ARRAY['nomor_induk','nis','no_induk']);
        IF c IS NOT NULL THEN
            idx := 'sekolah.idx_siswa_nomor_induk_trgm';
            PERFORM pg_temp._create_index(idx,
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I USING gin (lower(%I) gin_trgm_ops)',
                       'idx_siswa_nomor_induk_trgm', sch, 'siswa', c));
        END IF;
    END IF;

    -- calon_siswa: nama_siswa / nama dan nomor_induk
    IF pg_temp._table_exists(sch, 'calon_siswa') THEN
        c := pg_temp._pick_col(sch, 'calon_siswa', ARRAY['nama_siswa','nama']);
        IF c IS NOT NULL THEN
            idx := 'sekolah.idx_calon_siswa_nama_trgm';
            PERFORM pg_temp._create_index(idx,
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I USING gin (lower(%I) gin_trgm_ops)',
                       'idx_calon_siswa_nama_trgm', sch, 'calon_siswa', c));
        END IF;

        c := pg_temp._pick_col(sch, 'calon_siswa', ARRAY['nomor_induk','nomor_registrasi','no_reg','nis']);
        IF c IS NOT NULL THEN
            idx := 'sekolah.idx_calon_siswa_nomor_trgm';
            PERFORM pg_temp._create_index(idx,
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I USING gin (lower(%I) gin_trgm_ops)',
                       'idx_calon_siswa_nomor_trgm', sch, 'calon_siswa', c));
        END IF;
    END IF;

    -- sekolah.nama
    IF pg_temp._table_exists(sch, 'sekolah') THEN
        c := pg_temp._pick_col(sch, 'sekolah', ARRAY['nama']);
        IF c IS NOT NULL THEN
            idx := 'sekolah.idx_sekolah_nama_trgm';
            PERFORM pg_temp._create_index(idx,
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I USING gin (lower(%I) gin_trgm_ops)',
                       'idx_sekolah_nama_trgm', sch, 'sekolah', c));
        END IF;
    END IF;

    -- item_biaya_sekolah.nama
    IF pg_temp._table_exists(sch, 'item_biaya_sekolah') THEN
        c := pg_temp._pick_col(sch, 'item_biaya_sekolah', ARRAY['nama','kode']);
        IF c IS NOT NULL THEN
            idx := 'sekolah.idx_item_biaya_nama_trgm';
            PERFORM pg_temp._create_index(idx,
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I USING gin (lower(%I) gin_trgm_ops)',
                       'idx_item_biaya_nama_trgm', sch, 'item_biaya_sekolah', c));
        END IF;
    END IF;
END $$;

-- =====================================================================
-- 3) INDEX UNTUK TREND RIWAYAT PEMBAYARAN SISWA
-- Berdasarkan refreshTrendData():
-- PembayaranSiswaDetail -> PembayaranSiswa -> Tagihan -> NominalBiaya -> PengaturanBiaya -> JenisBiayaSekolah
-- Nama tabel/kolom bisa berbeda per mapping, maka script akan skip jika tidak ditemukan.
-- =====================================================================
DO $$
DECLARE
    sch text := 'sekolah';
    c text;
BEGIN
    IF pg_temp._table_exists(sch, 'pembayaran_siswa_detail') THEN
        c := pg_temp._pick_col(sch, 'pembayaran_siswa_detail', ARRAY['pembayaran_siswa_id','pembayaransiswa_id']);
        IF c IS NOT NULL THEN
            PERFORM pg_temp._create_index('sekolah.idx_psd_pembayaran_siswa',
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I)',
                       'idx_psd_pembayaran_siswa', sch, 'pembayaran_siswa_detail', c));
        END IF;

        c := pg_temp._pick_col(sch, 'pembayaran_siswa_detail', ARRAY['tagihan_id','tagihan']);
        IF c IS NOT NULL THEN
            PERFORM pg_temp._create_index('sekolah.idx_psd_tagihan',
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I)',
                       'idx_psd_tagihan', sch, 'pembayaran_siswa_detail', c));
        END IF;
    END IF;

    IF pg_temp._table_exists(sch, 'pembayaran_siswa') THEN
        c := pg_temp._pick_col(sch, 'pembayaran_siswa', ARRAY['tanggal','tgl','waktu']);
        IF c IS NOT NULL THEN
            PERFORM pg_temp._create_index('sekolah.idx_pembayaran_siswa_tanggal',
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I) WHERE %I IS NOT NULL',
                       'idx_pembayaran_siswa_tanggal', sch, 'pembayaran_siswa', c, c));
        END IF;

        c := pg_temp._pick_col(sch, 'pembayaran_siswa', ARRAY['siswa_id','siswa']);
        IF c IS NOT NULL THEN
            PERFORM pg_temp._create_index('sekolah.idx_pembayaran_siswa_siswa',
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I)',
                       'idx_pembayaran_siswa_siswa', sch, 'pembayaran_siswa', c));
        END IF;

        c := pg_temp._pick_col(sch, 'pembayaran_siswa', ARRAY['calon_siswa_id','calonsiswa_id','calon_siswa']);
        IF c IS NOT NULL THEN
            PERFORM pg_temp._create_index('sekolah.idx_pembayaran_siswa_calon',
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I)',
                       'idx_pembayaran_siswa_calon', sch, 'pembayaran_siswa', c));
        END IF;
    END IF;

    IF pg_temp._table_exists(sch, 'nominal_biaya') THEN
        c := pg_temp._pick_col(sch, 'nominal_biaya', ARRAY['pengaturan_biaya_id','pengaturanbiaya_id','pengaturan_biaya']);
        IF c IS NOT NULL THEN
            PERFORM pg_temp._create_index('sekolah.idx_nominal_biaya_pengaturan',
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I)',
                       'idx_nominal_biaya_pengaturan', sch, 'nominal_biaya', c));
        END IF;
    END IF;

    IF pg_temp._table_exists(sch, 'pengaturan_biaya') THEN
        c := pg_temp._pick_col(sch, 'pengaturan_biaya', ARRAY['jenis_biaya_sekolah_id','jenisbiayasekolah_id','jenis_biaya_sekolah']);
        IF c IS NOT NULL THEN
            PERFORM pg_temp._create_index('sekolah.idx_pengaturan_biaya_jenis',
                format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%I)',
                       'idx_pengaturan_biaya_jenis', sch, 'pengaturan_biaya', c));
        END IF;
    END IF;
END $$;

-- =====================================================================
-- 4) Refresh statistik planner
-- =====================================================================
ANALYZE sekolah.tagihan;

DO $$
BEGIN
    IF to_regclass('sekolah.siswa') IS NOT NULL THEN
        EXECUTE 'ANALYZE sekolah.siswa';
    END IF;
    IF to_regclass('sekolah.calon_siswa') IS NOT NULL THEN
        EXECUTE 'ANALYZE sekolah.calon_siswa';
    END IF;
    IF to_regclass('sekolah.item_biaya_sekolah') IS NOT NULL THEN
        EXECUTE 'ANALYZE sekolah.item_biaya_sekolah';
    END IF;
    IF to_regclass('sekolah.pembayaran_siswa') IS NOT NULL THEN
        EXECUTE 'ANALYZE sekolah.pembayaran_siswa';
    END IF;
    IF to_regclass('sekolah.pembayaran_siswa_detail') IS NOT NULL THEN
        EXECUTE 'ANALYZE sekolah.pembayaran_siswa_detail';
    END IF;
END $$;

-- =====================================================================
-- Opsional cek cepat:
-- EXPLAIN (ANALYZE, BUFFERS)
-- SELECT count(*), sum(nominal), sum(dibayar)
-- FROM sekolah.tagihan
-- WHERE (aktif IS NULL OR aktif = true)
--   AND bukan_tagihan = false
--   AND nominal > 0.1
--   AND tahun_ajaran = '2025/2026';
-- =====================================================================
