package ais.service.tenant;

/**
 * <h3>Migrasi tenant v9 — tabel POS eBisnis yang tidak ada di arsip FoxPro (P4 prasyarat).</h3>
 *
 * <p>Bundel v2–v8 diturunkan dari <b>§11.3 dokumen master</b>, yang menggambarkan domain
 * Inventory &amp; Sales dari arsip DBF. Permukaan {@code si_*} juga menyentuh tabel POS
 * eBisnis yang sudah berjalan — sesi kas kasir, retur, keranjang draft, foto produk — dan
 * semuanya tidak ada di dunia FoxPro itu. Sepuluh tabel di sini menutup selisihnya.</p>
 *
 * <p>Pemetaan lengkap beserta alasannya: {@code docs/tenant-inventory-sales/05-pemetaan-tabel.md}.</p>
 *
 * <h4>Dua ALTER yang menutup jebakan pemetaan</h4>
 * <ul>
 * <li><b>J-2</b> — {@code kategori_produk} mendapat {@code maksimal_harian}. Tanpa itu,
 *     pemetaan {@code koperasi.jenis_produk} &rarr; {@code kategori_produk} menghilangkan
 *     batas pembelian harian: pengguna dapat membeli melampaui plafon dan tidak ada yang
 *     menolak.</li>
 * <li><b>J-3</b> — {@code customer_anggota_profile} menampung medan keanggotaan yang tidak
 *     muat di {@code customer}: jenis anggota, identitas, saldo minimal, aturan pelanggaran.</li>
 * </ul>
 *
 * <h4>Kata sandi tidak ikut</h4>
 * <p>{@code koperasi.anggota_koperasi} memuat kolom {@code pass}. <b>Tidak ada kolom di sini
 * yang menampungnya</b>, dan itu disengaja (§24 butir 13). Anggota yang memang punya login
 * diselaraskan lewat {@code pengguna_tenant} (v2).</p>
 *
 * <h4>Tetap bergaya 9.3 walau produksi lebih baru</h4>
 * <p>Penelusuran menemukan kode aktif memakai {@code ON CONFLICT}, {@code SKIP LOCKED}, dan
 * {@code jsonb} — sintaks 9.4/9.5 — sehingga produksi pasti 9.5 ke atas. Bundel ini tetap
 * memakai gaya yang sama dengan v2–v8: konsisten, tetap sah di 9.5+, dan tidak menuntut
 * penjaga {@code TenantSchemaMigrasiSelfTest} dilonggarkan. Bila kelak ada kebutuhan nyata
 * atas sintaks yang lebih baru, longgarkan penjaganya bersamaan dengan bundel yang
 * memakainya — bukan sebelumnya.</p>
 */
public final class TenantSchemaMigrationsV9 {

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantSchemaMigrationsV9() {
	}

	// PERINGATAN: masuk ke teks DDL kanonik -- mengubahnya mengubah checksum v9 dan
	// menggagalkan migrasi di seluruh tenant yang sudah memasangnya. Buat bundel versi BARU.
	/** Fragmen kolom jejak audit ringan (pembuat/pengubah + waktunya), dipakai seluruh tabel. */
	private static final String JEJAK =
			"dibuat_pada timestamp, tanggal_dirubah timestamp, "
			+ "oleh varchar(255), olehid varchar(255)";

	/**
	 * Fragmen kolom provenans impor legacy, dipakai tabel yang punya padanan di
	 * {@code koperasi.*} (mis. {@code cara_pembayaran}, {@code jenis_customer},
	 * {@code customer_anggota_profile}, {@code retur_penjualan}): nama berkas dan nomor
	 * baris sumber, hash baris, id run impor, dan penanda {@code legacy_deleted}.
	 */
	private static final String LEGACY =
			"legacy_source_file varchar(128), legacy_source_record_no integer, "
			+ "legacy_row_hash varchar(64), legacy_import_run_id bigint, "
			+ "legacy_deleted boolean DEFAULT false";

	/**
	 * Katalog DDL kanonik migrasi v9: satu {@code ALTER TABLE} (J-2, menambah
	 * {@code maksimal_harian} pada {@code kategori_produk}) diikuti {@code CREATE TABLE}/
	 * {@code CREATE INDEX} berurutan untuk sepuluh tabel POS eBisnis yang tidak berasal dari
	 * arsip FoxPro — metode pembayaran ({@code cara_pembayaran}), keanggotaan
	 * ({@code jenis_customer}, {@code customer_anggota_profile} — J-3, tanpa kolom kata
	 * sandi), sesi kas kasir, keranjang draft (+ detail), retur penjualan, pemakaian bahan
	 * baku, survei kepuasan, ack cadangan transaksi, dan foto produk. Lihat javadoc kelas dan
	 * {@code docs/tenant-inventory-sales/05-pemetaan-tabel.md} untuk alasan pemetaan tiap
	 * tabel, serta komentar inline di setiap blok array ini untuk catatan spesifik per tabel.
	 * Dikonsumsi oleh {@link TenantSchemaMigrations#SEMUA} lewat entri bertarget
	 * {@code TARGET_ERP}; penanda {@code {S}}/{@code {A}}/{@code {SU}} disubstitusi saat
	 * migrasi diterapkan oleh {@code TenantSchemaService#terapkanMigrasi}. Isi array ini
	 * bagian dari checksum kanonik v9 — lihat peringatan di atas sebelum mengubah elemen
	 * mana pun.
	 */
	public static final String[] ERP = {
			// ---------- J-2: batas harian per kategori ----------
			"ALTER TABLE {S}.kategori_produk ADD COLUMN maksimal_harian numeric(18,2)",

			// ---------- Metode pembayaran POS ----------
			// Berbeda dari cara_bayar bebas-teks pada dokumen v4/v5: yang ini master
			// ber-akun buku besar, dan perilakunya menentukan alur kasir.
			"CREATE TABLE {S}.cara_pembayaran ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "keterangan text, "
					+ "akun_id bigint REFERENCES {S}.akun(id), "
					+ "kanal varchar(64), "
					+ "manual boolean DEFAULT false, "
					+ "online boolean DEFAULT false, "
					+ "memotong_deposit boolean DEFAULT false, "
					+ "masuk_sebagai_hutang boolean DEFAULT false, "
					+ "ada_kembalian boolean DEFAULT true, "
					+ "wajib_pilih_member boolean DEFAULT false, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", " + LEGACY + ", "
					+ "CONSTRAINT uq_{SU}_cara_pembayaran_kode UNIQUE (kode))",
			"CREATE INDEX idx_{SU}_cara_pembayaran_akun ON {S}.cara_pembayaran (akun_id)",
			"CREATE INDEX idx_{SU}_cara_pembayaran_aktif ON {S}.cara_pembayaran (aktif)",

			// ---------- Jenis pelanggan (jenis anggota) ----------
			"CREATE TABLE {S}.jenis_customer ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "keterangan text, "
					+ "dipilih boolean DEFAULT false, "
					+ "boleh_entry_topup_oleh_admin boolean DEFAULT false, "
					+ "cara_pembayaran_diizinkan text, "
					+ "istilah_sisa_saldo varchar(64), tampilkan_sisa_saldo boolean DEFAULT true, "
					+ "istilah_cashback varchar(64), tampilkan_cashback boolean DEFAULT true, "
					+ "minimal_saldo numeric(18,2), "
					+ "wajib_belanja_rutin boolean DEFAULT false, "
					+ "target_frekuensi_belanja integer, "
					+ "maksimal_pelanggaran integer, "
					+ "wajib_pin boolean DEFAULT false, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", " + LEGACY + ", "
					+ "CONSTRAINT uq_{SU}_jenis_customer_kode UNIQUE (kode))",

			// ---------- J-3: profil keanggotaan ----------
			// TIDAK ADA kolom kata sandi di sini, dan itu disengaja (24 butir 13).
			"CREATE TABLE {S}.customer_anggota_profile ("
					+ "id bigserial PRIMARY KEY, "
					+ "customer_id bigint NOT NULL REFERENCES {S}.customer(id), "
					+ "jenis_customer_id bigint REFERENCES {S}.jenis_customer(id), "
					+ "userid varchar(100), "
					+ "kode_identitas varchar(64), jenis_identitas varchar(64), "
					+ "tipe varchar(64), "
					+ "telp varchar(50), hp varchar(50), hp_normalisasi varchar(50), "
					+ "email varchar(255), "
					+ "tanggal_kadaluarsa date, "
					+ "tanggal_berhenti date, alasan_berhenti text, "
					+ "jumlah_peringatan integer DEFAULT 0, "
					+ "pihak_terkait boolean DEFAULT false, "
					+ JEJAK + ", " + LEGACY + ", "
					+ "CONSTRAINT uq_{SU}_customer_anggota_profile UNIQUE (customer_id))",
			"CREATE INDEX idx_{SU}_cust_anggota_jenis ON {S}.customer_anggota_profile (jenis_customer_id)",
			"CREATE INDEX idx_{SU}_cust_anggota_userid ON {S}.customer_anggota_profile (userid)",
			"CREATE INDEX idx_{SU}_cust_anggota_hp ON {S}.customer_anggota_profile (hp_normalisasi)",

			// ---------- Sesi kas kasir ----------
			"CREATE TABLE {S}.sesi_kas_kasir ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64), "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "kasir_user_id varchar(100), kasir_nama varchar(255), "
					+ "id_perangkat varchar(128), nama_perangkat varchar(255), "
					+ "waktu_buka timestamp, waktu_tutup timestamp, "
					+ "modal_awal numeric(18,2) DEFAULT 0, "
					+ "total_tunai numeric(18,2) DEFAULT 0, "
					+ "total_non_tunai numeric(18,2) DEFAULT 0, "
					+ "uang_fisik numeric(18,2), "
					+ "selisih numeric(18,2), "
					+ "status varchar(32) DEFAULT 'TERBUKA', "
					+ "keterangan text, "
					+ "laporan_tutup_json text, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_sesi_kas_toko ON {S}.sesi_kas_kasir (toko_id)",
			"CREATE INDEX idx_{SU}_sesi_kas_kasir ON {S}.sesi_kas_kasir (kasir_user_id)",
			"CREATE INDEX idx_{SU}_sesi_kas_status ON {S}.sesi_kas_kasir (status)",
			"CREATE INDEX idx_{SU}_sesi_kas_buka ON {S}.sesi_kas_kasir (waktu_buka)",
			"CREATE INDEX idx_{SU}_sesi_kas_perangkat ON {S}.sesi_kas_kasir (id_perangkat)",

			// ---------- Keranjang kasir (draft) ----------
			"CREATE TABLE {S}.draft_penjualan ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64), "
					+ "urutan bigint, "
					+ "customer_id bigint REFERENCES {S}.customer(id), "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "cara_pembayaran_id bigint REFERENCES {S}.cara_pembayaran(id), "
					+ "sesi_kas_kasir_id bigint REFERENCES {S}.sesi_kas_kasir(id), "
					+ "meja varchar(64), lokasi varchar(128), "
					+ "kasir_login_nama varchar(255), nama_mesin varchar(255), "
					+ "keterangan text, "
					+ "faktur_penjualan_id bigint REFERENCES {S}.faktur_penjualan(id), "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_draft_jual_toko ON {S}.draft_penjualan (toko_id)",
			"CREATE INDEX idx_{SU}_draft_jual_customer ON {S}.draft_penjualan (customer_id)",
			"CREATE INDEX idx_{SU}_draft_jual_sesi ON {S}.draft_penjualan (sesi_kas_kasir_id)",
			"CREATE INDEX idx_{SU}_draft_jual_kode ON {S}.draft_penjualan (kode)",

			"CREATE TABLE {S}.draft_penjualan_detail ("
					+ "id bigserial PRIMARY KEY, "
					+ "draft_penjualan_id bigint REFERENCES {S}.draft_penjualan(id), "
					+ "induk_id bigint REFERENCES {S}.draft_penjualan_detail(id), "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "kuantitas numeric(18,4) NOT NULL DEFAULT 0, "
					+ "harga_satuan numeric(18,2), harga_jual numeric(18,2), "
					+ "diskon numeric(18,2) DEFAULT 0, total numeric(18,2) DEFAULT 0, "
					+ "cashback numeric(18,2) DEFAULT 0, "
					+ "aturan_diskon varchar(128), "
					+ "terlayani boolean DEFAULT false, "
					+ "keterangan text, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_draft_detail_induk ON {S}.draft_penjualan_detail (draft_penjualan_id)",
			"CREATE INDEX idx_{SU}_draft_detail_produk ON {S}.draft_penjualan_detail (produk_id)",

			// ---------- Retur penjualan ----------
			"CREATE TABLE {S}.retur_penjualan ("
					+ "id bigserial PRIMARY KEY, "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "customer_id bigint REFERENCES {S}.customer(id), "
					+ "faktur_penjualan_id bigint REFERENCES {S}.faktur_penjualan(id), "
					+ "faktur_penjualan_detail_id bigint REFERENCES {S}.faktur_penjualan_detail(id), "
					+ "kode_transaksi_asal varchar(64), "
					+ "nama_pembeli varchar(255), "
					+ "kuantitas numeric(18,4) NOT NULL, "
					+ "harga_satuan numeric(18,2), total_nilai numeric(18,2), "
					+ "alasan text, kondisi_barang varchar(64), "
					+ "kembalikan_ke_stok boolean DEFAULT false, "
					+ "metode_pengembalian varchar(64), "
					+ "keterangan text, "
					+ "waktu timestamp, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_retur_produk ON {S}.retur_penjualan (produk_id)",
			"CREATE INDEX idx_{SU}_retur_faktur ON {S}.retur_penjualan (faktur_penjualan_id)",
			"CREATE INDEX idx_{SU}_retur_customer ON {S}.retur_penjualan (customer_id)",
			"CREATE INDEX idx_{SU}_retur_waktu ON {S}.retur_penjualan (waktu)",
			"CREATE INDEX idx_{SU}_retur_kode_asal ON {S}.retur_penjualan (kode_transaksi_asal)",

			// ---------- Pemakaian bahan baku ----------
			"CREATE TABLE {S}.pemakaian_bahan_baku ("
					+ "id bigserial PRIMARY KEY, "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "faktur_penjualan_id bigint REFERENCES {S}.faktur_penjualan(id), "
					+ "kuantitas numeric(18,4) NOT NULL, "
					+ "waktu timestamp, "
					+ "keterangan text, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_bahan_baku_produk ON {S}.pemakaian_bahan_baku (produk_id)",
			"CREATE INDEX idx_{SU}_bahan_baku_faktur ON {S}.pemakaian_bahan_baku (faktur_penjualan_id)",
			"CREATE INDEX idx_{SU}_bahan_baku_waktu ON {S}.pemakaian_bahan_baku (waktu)",

			// ---------- Survei kepuasan ----------
			"CREATE TABLE {S}.survey_kepuasan ("
					+ "id bigserial PRIMARY KEY, "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "rating integer, "
					+ "catatan text, "
					+ "waktu timestamp, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_survey_toko ON {S}.survey_kepuasan (toko_id)",
			"CREATE INDEX idx_{SU}_survey_waktu ON {S}.survey_kepuasan (waktu)",

			// ---------- Ack cadangan transaksi ----------
			// Tidak punya entitas Hibernate -- bentuknya dibaca dari SQL mentah di
			// PosApi:3871. Kunci uniknya FUNGSIONAL atas lower(kode_transaksi), jadi harus
			// berupa unique INDEX, bukan constraint tabel.
			"CREATE TABLE {S}.transaksi_backup_ack ("
					+ "id bigserial PRIMARY KEY, "
					+ "toko bigint, "
					+ "kode_transaksi varchar(128) NOT NULL, "
					+ "id_perangkat varchar(128), "
					+ "nama_mesin varchar(255), "
					+ "kasir_user_id varchar(100), kasir_nama varchar(255), "
					+ "waktu timestamp)",
			"CREATE UNIQUE INDEX uq_{SU}_backup_ack ON {S}.transaksi_backup_ack "
					+ "(toko, lower(kode_transaksi), id_perangkat)",
			"CREATE INDEX idx_{SU}_backup_ack_waktu ON {S}.transaksi_backup_ack (waktu)",

			// ---------- Foto produk ----------
			// Kolom foto (bytea) DISEDIAKAN supaya tidak ada data yang hilang bila produksi
			// ternyata menyimpan gambar di basis data. Tetapi bila yang dipakai path/url,
			// impor WAJIB memindahkan metadatanya saja -- menyalin blob per tenant
			// menggandakan ukuran basis data tanpa manfaat. Belum diverifikasi ke produksi.
			"CREATE TABLE {S}.foto_produk ("
					+ "id bigserial PRIMARY KEY, "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "nama varchar(255), "
					+ "keterangan text, "
					+ "lokasi_simpan varchar(64), "
					+ "path text, url text, link text, "
					+ "gdrive text, gdrive_username varchar(255), "
					+ "foto bytea, "
					+ "urutan integer, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_foto_produk_produk ON {S}.foto_produk (produk_id)",
	};
}
