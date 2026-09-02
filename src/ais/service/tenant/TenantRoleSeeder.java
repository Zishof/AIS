package ais.service.tenant;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 * <h3>Menyemai delapan peran bawaan ke <code>{S}.role_tenant</code>.</h3>
 *
 * <p>{@link TenantRbac} menentukan <b>apa</b> yang boleh dilakukan tiap peran, tetapi
 * matriksnya hidup di dalam kode dan tidak terlihat oleh siapa pun yang membuka basis data.
 * Tabel {@code role_tenant} adalah sisi yang terlihat itu: dari sanalah layar pengelolaan
 * pengguna mengambil pilihan peran, dan ke sanalah {@code user_role_tenant} menunjuk.</p>
 *
 * <p>Tanpa penyemaian ini tabelnya kosong pada tenant yang baru di-provision, sehingga tidak
 * ada satu peran pun yang dapat diberikan kepada pengguna -- RBAC-nya lengkap di kode namun
 * tidak dapat dipakai.</p>
 *
 * <h4>Idempoten, dan sengaja begitu</h4>
 * <p>{@code ON CONFLICT (kode) DO NOTHING} membuat penyemaian aman dijalankan berulang: saat
 * provisioning, saat migrasi menambah peran baru, atau saat pemulihan. Yang sudah ada
 * <b>tidak</b> ditimpa -- nama dan keterangan yang sudah disunting pemilik tenant harus
 * bertahan, sebab menimpanya berarti membatalkan penyesuaian mereka setiap kali kode ini
 * jalan.</p>
 *
 * <h4>Keterangan mengikuti matriks yang sebenarnya</h4>
 * <p>Teks keterangan di bawah diturunkan dari {@code TenantRbac} baris 105-166. Bila matriks
 * itu berubah, ubah juga teksnya -- keterangan yang berbohong tentang kewenangan lebih
 * berbahaya daripada tidak ada keterangan sama sekali.</p>
 */
public final class TenantRoleSeeder {

	/** kode, nama tampil, keterangan. Urutannya = urutan tampil yang wajar. */
	private static final String[][] BAWAAN = {
			{ TenantRbac.OWNER, "Pemilik",
					"Seluruh area, termasuk menyetujui. Peran tertinggi pada tenant." },
			{ TenantRbac.PEMILIK_SALES_INVENTORY, "Pemilik Sales & Inventory",
					"Sama luasnya dengan Pemilik untuk seluruh area Sales dan Inventory." },
			{ TenantRbac.ADMIN_TENANT, "Admin Tenant",
					"Seluruh area kecuali impor data legacy." },
			{ TenantRbac.GUDANG, "Gudang",
					"Stok dan produk: mengubah dan menyetujui stok, mengubah produk. "
							+ "Pembelian, penjualan, dan mitra hanya dibaca. Tidak menyentuh uang." },
			{ TenantRbac.PEMBELIAN, "Pembelian",
					"Sisi pemasok: pembelian beserta persetujuannya, hutang, mitra, dan harga. "
							+ "Produk, stok, dan laporan hanya dibaca." },
			{ TenantRbac.SALES_KELILING, "Sales Keliling",
					"Perjalanan, penjualan, dan piutangnya. Harga hanya dibaca -- nota di "
							+ "lapangan tidak boleh mengubah harga jual." },
			{ TenantRbac.KEUANGAN, "Keuangan",
					"Kas, jurnal, akun, biaya, hutang, dan piutang beserta persetujuannya, "
							+ "serta memposting laporan. Transaksi asalnya hanya dibaca." },
			{ TenantRbac.AUDITOR, "Auditor",
					"MEMBACA SAJA, seluruh area. Tidak ada satu pun kewenangan menulis." } };

	/**
	 * Menyisip bila belum ada. Memakai {@code WHERE NOT EXISTS}, <b>bukan</b>
	 * {@code ON CONFLICT}: lapisan tenant konsisten bergaya PostgreSQL 9.3, sebagaimana
	 * dinyatakan {@link TenantDataPlaneService} dan ditegakkan penjaga struktural pada katalog
	 * migrasi. Semula kelas ini memakai {@code ON CONFLICT} dan luput dari penjaga itu karena
	 * penjaganya hanya memeriksa bundel migrasi, bukan SQL runtime.
	 */
	private static final String SQL_SISIP = "INSERT INTO {S}.role_tenant"
			+ " (kode, nama, keterangan, bawaan, aktif, dibuat_pada, oleh)"
			+ " SELECT :kode, :nama, :ket, true, true, now(), :oleh"
			+ " WHERE NOT EXISTS (SELECT 1 FROM {S}.role_tenant WHERE kode = :kode)";

	private TenantRoleSeeder() {
	}

	/**
	 * Menyemai peran yang belum ada. Mengembalikan jumlah baris yang benar-benar disisipkan;
	 * nol berarti kedelapan peran sudah lengkap, bukan berarti gagal.
	 *
	 * <p>Tidak membuka maupun menutup Session: pemanggilnya yang memiliki transaksi, sebab
	 * penyemaian ini bagian dari provisioning yang harus utuh atau batal seluruhnya.</p>
	 *
	 * @param oleh penanda pelaku untuk kolom jejak; boleh {@code null}.
	 */
	public static int seed(Session session, TenantContext ctx, String oleh) {
		if (session == null || ctx == null) {
			throw new IllegalArgumentException("Session dan TenantContext wajib ada.");
		}
		int disisipkan = 0;
		for (int i = 0; i < BAWAAN.length; i++) {
			SQLQuery q = TenantSqlExecutor.sql(session, ctx, SQL_SISIP);
			q.setParameter("kode", BAWAAN[i][0]);
			q.setParameter("nama", BAWAAN[i][1]);
			q.setParameter("ket", BAWAAN[i][2]);
			q.setParameter("oleh", oleh == null ? "sistem" : oleh);
			disisipkan += q.executeUpdate();
		}
		return disisipkan;
	}

	/**
	 * Penyemaian untuk operasi sistem yang hanya memegang nama schema -- provisioning,
	 * misalnya, yang berjalan sebelum ada aktor manusia mana pun.
	 *
	 * <p>Konteks yang dibentuk di sini <b>hanya</b> membawa nama schema, sebab
	 * {@link TenantSqlExecutor#siapkan} memang tidak membaca yang lain. Ia sengaja TIDAK
	 * dipakai untuk apa pun selain penyemaian ini: konteks tanpa membership tidak boleh
	 * bocor ke jalur yang menentukan kewenangan.</p>
	 *
	 * <p>Substitusi dan validasi nama schema tetap dikerjakan {@code TenantSqlExecutor},
	 * bukan diulang di sini -- dua tempat yang mengutip nama schema dengan caranya
	 * masing-masing adalah cara termudah melahirkan lubang injeksi.</p>
	 */
	public static int seedSchema(Session session, String schemaName, String oleh) {
		TenantContext ctx = TenantContext.builder().schemaName(schemaName).build();
		return seed(session, ctx, oleh);
	}

	/** Kode peran bawaan, untuk uji dan untuk pemeriksaan kelengkapan. */
	public static String[] kodeBawaan() {
		String[] k = new String[BAWAAN.length];
		for (int i = 0; i < BAWAAN.length; i++) {
			k[i] = BAWAAN[i][0];
		}
		return k;
	}

	/** Templat SQL penyisipan, supaya alat verifikasi dapat merendernya tanpa Session. */
	public static String templatSisip() {
		return SQL_SISIP;
	}
}
