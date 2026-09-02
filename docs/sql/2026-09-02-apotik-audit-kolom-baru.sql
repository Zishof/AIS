/*
 * SINKRONISASI TABEL AUDIT UNTUK KOLOM BARU MODUL APOTIK
 * Tanggal : 2 September 2026
 * Berlaku : WAJIB dijalankan SEBELUM restart Tomcat yang memuat kode
 *           IR-01 / IR-02 / IR-11.
 *
 * LATAR BELAKANG
 * --------------
 * hibernate.cfg.xml sudah memperingatkan hal ini (lihat catatan Envers di
 * sana): `hbm2ddl.auto=update` menambahkan kolom baru ke tabel utama, tetapi
 * TIDAK selalu menambahkannya ke tabel audit `new_audit.<tabel>__audit`.
 * Untuk entitas @Audited akibatnya bukan sekadar audit yang tidak lengkap:
 * INSERT ke tabel audit GAGAL, transaksi induknya ikut ter-rollback, dan
 * penyimpanan data yang tampak wajar di layar justru hilang.
 *
 * Tabel yang BARU dibuat aman (Envers membuat tabel auditnya sekaligus).
 * Yang berbahaya adalah kolom yang DITAMBAHKAN ke tabel audit yang sudah ada.
 *
 * KOLOM YANG DITAMBAHKAN
 * ----------------------
 *   sirs.apotik_item_profile   : bentuk_sediaan, kekuatan, high_alert,
 *                                cold_chain                        (IR-01)
 *   sirs.kadaluarsa            : status_lot                        (IR-02)
 *   sirs.apotik_pembayaran_transaksi : tunai, kembalian            (IR-11)
 *
 * sirs.kadaluarsa adalah tabel LAMA yang dipakai seluruh modul persediaan
 * medis, jadi justru itu yang paling perlu diperhatikan.
 *
 * SIFAT SKRIP
 * -----------
 * Idempoten (IF NOT EXISTS) dan aman dijalankan ulang. Tidak mengubah satu
 * baris data pun -- hanya menambah kolom nullable.
 *
 * CARA MENJALANKAN
 * ----------------
 *   psql -h <host> -U <user> -d <database> -f 2026-09-02-apotik-audit-kolom-baru.sql
 *
 * Setelah itu barulah restart Tomcat.
 */

BEGIN;

-- ---------------------------------------------------------------- IR-01
ALTER TABLE sirs.apotik_item_profile
    ADD COLUMN IF NOT EXISTS bentuk_sediaan varchar(60),
    ADD COLUMN IF NOT EXISTS kekuatan       varchar(60),
    ADD COLUMN IF NOT EXISTS high_alert     boolean,
    ADD COLUMN IF NOT EXISTS cold_chain     boolean;

ALTER TABLE new_audit.apotik_item_profile__audit
    ADD COLUMN IF NOT EXISTS bentuk_sediaan varchar(60),
    ADD COLUMN IF NOT EXISTS kekuatan       varchar(60),
    ADD COLUMN IF NOT EXISTS high_alert     boolean,
    ADD COLUMN IF NOT EXISTS cold_chain     boolean;

-- ---------------------------------------------------------------- IR-02
ALTER TABLE sirs.kadaluarsa
    ADD COLUMN IF NOT EXISTS status_lot varchar(24);

ALTER TABLE new_audit.kadaluarsa__audit
    ADD COLUMN IF NOT EXISTS status_lot varchar(24);

-- ---------------------------------------------------------------- IR-11
ALTER TABLE sirs.apotik_pembayaran_transaksi
    ADD COLUMN IF NOT EXISTS tunai      double precision,
    ADD COLUMN IF NOT EXISTS kembalian  double precision;

ALTER TABLE new_audit.apotik_pembayaran_transaksi__audit
    ADD COLUMN IF NOT EXISTS tunai      double precision,
    ADD COLUMN IF NOT EXISTS kembalian  double precision;

COMMIT;

/*
 * VERIFIKASI (jalankan setelah COMMIT; harus mengembalikan 11 baris)
 *
 * SELECT table_schema, table_name, column_name
 *   FROM information_schema.columns
 *  WHERE (table_schema = 'sirs' AND table_name = 'apotik_item_profile'
 *         AND column_name IN ('bentuk_sediaan','kekuatan','high_alert','cold_chain'))
 *     OR (table_schema = 'new_audit' AND table_name = 'apotik_item_profile__audit'
 *         AND column_name IN ('bentuk_sediaan','kekuatan','high_alert','cold_chain'))
 *     OR (table_schema = 'sirs' AND table_name = 'kadaluarsa'
 *         AND column_name = 'status_lot')
 *     OR (table_schema = 'new_audit' AND table_name = 'kadaluarsa__audit'
 *         AND column_name = 'status_lot')
 *     OR (table_schema = 'sirs' AND table_name = 'apotik_pembayaran_transaksi'
 *         AND column_name IN ('tunai','kembalian'))
 *     OR (table_schema = 'new_audit' AND table_name = 'apotik_pembayaran_transaksi__audit'
 *         AND column_name IN ('tunai','kembalian'))
 *  ORDER BY table_schema, table_name, column_name;
 *
 * Bila tabel new_audit.apotik_pembayaran_transaksi__audit belum ada, berarti
 * kode IR-07 belum pernah dijalankan di basis data ini: lewati bagian IR-11,
 * jalankan aplikasi sekali agar Envers membuat tabelnya, lalu jalankan ulang
 * skrip ini.
 */
