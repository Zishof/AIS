package ais.service.tenant;

/**
 * <h3>Migrasi tenant v15 — master kategori biaya trip, dan medan biaya yang masih kurang.</h3>
 *
 * <p>Satu tabel baru dan tiga kolom. Semuanya melayani satu konsep — <b>pencatatan biaya
 * perjalanan</b> — dan ketiga aksi yang menunggunya: {@code expenseCategoryList},
 * {@code expenseCategorySave}, dan {@code expenseCreate}.</p>
 *
 * <h4>Kategori biaya adalah master, bukan teks bebas</h4>
 * <p>{@code sales_trip_biaya.kategori} selama ini {@code varchar(64)} tanpa acuan. Jalur legacy
 * memakai <b>entitas</b> {@code KategoriBiayaSales} dengan kode unik, dan mesin posting membaca
 * akun beban dari kategori itu — sesuatu yang mustahil dilakukan teks bebas.</p>
 * <p>Karena itu v15 menambahkan tabelnya berikut kolom penunjuk
 * {@code sales_trip_biaya.kategori_biaya_id}.</p>
 *
 * <h4>Kolom teks lama TIDAK dihapus, dan itu disengaja</h4>
 * <p>{@code kategori} yang lama dibiarkan. Baris hasil impor legacy mungkin sudah memuat teks di
 * sana tanpa padanan pada master, dan menghapus kolomnya berarti membuang keterangan yang tidak
 * dapat dipulihkan.</p>
 * <p>Pembacanya memakai {@code COALESCE(k.kode, b.kategori)}: baris baru menjawab lewat
 * penunjuknya, baris lama menjawab lewat teksnya. Ini <b>bukan</b> dua sumber untuk satu angka —
 * keduanya tidak pernah terisi bersamaan pada baris yang sama, dan penunjuk selalu menang.</p>
 *
 * <h4>Dua medan yang selama ini hilang tanpa disadari</h4>
 * <p>{@code expenseCreate} legacy menyimpan {@code penerima} (kepada siapa uang diserahkan) dan
 * {@code nomorBukti}. Keduanya tidak punya kolom pada {@code sales_trip_biaya}, dan ketiadaannya
 * baru terlihat saat aksinya hendak dipindahkan.</p>
 * <p>Untuk biaya tunai lapangan, penerima adalah satu-satunya keterangan tentang ke mana uangnya
 * pergi. Menghilangkannya membuat rekonsiliasi hanya bisa menyatakan bahwa uang berkurang, tanpa
 * bisa menyatakan kepada siapa.</p>
 *
 * <h4>Sembilan kategori bawaan disemai di sini</h4>
 * <p>Jalur legacy menyemainya sekali per pemuatan servlet ke schema bersama. Untuk tenant,
 * penyemaian ikut migrasinya — sekali per tenant saat provisioning, dan otomatis menyusul pada
 * tenant yang sudah berdiri sebab katalognya append-only.</p>
 * <p>Penyemaiannya <b>menyisip bila belum ada</b>, tidak menimpa: pemilik tenant boleh mengganti
 * nama kategori bawaan, dan penyemaian ulang tidak boleh membatalkan perubahan itu.</p>
 * <p>Idempotensinya memakai {@code WHERE NOT EXISTS}, <b>bukan</b> {@code ON CONFLICT}. Seluruh
 * bundel katalog ini bergaya PostgreSQL 9.3, dan {@code ON CONFLICT} baru ada di 9.5 — penjaga
 * struktural pada {@code TenantSchemaMigrasiSelfTest} menolaknya, dan penolakan itu benar.</p>
 *
 * <h4>Apa yang TIDAK dilakukan bundel ini</h4>
 * <p>Tidak ada penautan surut: baris biaya lama tetap {@code NULL} pada
 * {@code kategori_biaya_id}. Mencocokkan teks bebas ke kode master secara otomatis akan menebak,
 * dan tebakan yang salah menempelkan akun beban yang keliru pada biaya yang sudah dibukukan.</p>
 */
public final class TenantSchemaMigrationsV15 {

	private TenantSchemaMigrationsV15() {
	}

	/** Kategori bawaan; sama persis dengan yang disemai jalur legacy. */
	private static final String[][] BAWAAN = { { "BBM", "Bensin/BBM" }, { "TOL", "Tol" },
			{ "PARKIR", "Parkir" }, { "MAKAN", "Makan/Uang Harian" },
			{ "BONGKAR_MUAT", "Bongkar Muat" }, { "PENGINAPAN", "Penginapan" },
			{ "SERVIS", "Servis Darurat" }, { "ADMIN", "Administrasi" },
			{ "LAINNYA", "Lain-lain" } };

	public static final String[] ERP = susun();

	private static String[] susun() {
		java.util.List<String> ddl = new java.util.ArrayList<String>();

		ddl.add("CREATE TABLE {S}.kategori_biaya_sales ("
				+ "id bigserial PRIMARY KEY, "
				+ "kode varchar(64) NOT NULL, "
				+ "nama varchar(255) NOT NULL, "
				+ "akun_id bigint REFERENCES {S}.akun(id), "
				+ "aktif boolean DEFAULT true, "
				+ "dibuat_pada timestamp, "
				+ "tanggal_dirubah timestamp, "
				+ "oleh varchar(255), "
				+ "olehid varchar(255), "
				+ "CONSTRAINT uq_{SU}_kategori_biaya_kode UNIQUE (kode))");

		ddl.add("CREATE INDEX idx_{SU}_kategori_biaya_aktif ON {S}.kategori_biaya_sales (aktif)");

		// ---------- biaya trip: penunjuk kategori dan dua medan yang hilang ----------
		ddl.add("ALTER TABLE {S}.sales_trip_biaya ADD COLUMN kategori_biaya_id bigint"
				+ " REFERENCES {S}.kategori_biaya_sales(id)");

		ddl.add("CREATE INDEX idx_{SU}_sales_trip_biaya_kategori ON {S}.sales_trip_biaya"
				+ " (kategori_biaya_id)");

		ddl.add("ALTER TABLE {S}.sales_trip_biaya ADD COLUMN penerima varchar(255)");

		ddl.add("ALTER TABLE {S}.sales_trip_biaya ADD COLUMN nomor_bukti varchar(64)");

		// ---------- sembilan kategori bawaan ----------
		for (int i = 0; i < BAWAAN.length; i++) {
			// PG 9.3: TANPA ON CONFLICT. Idempotensinya lewat WHERE NOT EXISTS -- pola yang
			// sama dipakai TenantDataPlaneService pada lapisan tenant.
			ddl.add("INSERT INTO {S}.kategori_biaya_sales (kode, nama, aktif, dibuat_pada, oleh)"
					+ " SELECT '" + BAWAAN[i][0] + "', '" + BAWAAN[i][1] + "', true, now(),"
					+ " 'migrasi-v15'"
					+ " WHERE NOT EXISTS (SELECT 1 FROM {S}.kategori_biaya_sales"
					+ " WHERE kode = '" + BAWAAN[i][0] + "')");
		}

		return ddl.toArray(new String[ddl.size()]);
	}
}
