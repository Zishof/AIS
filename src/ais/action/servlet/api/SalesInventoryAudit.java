package ais.action.servlet.api;

import org.hibernate.Session;
import org.json.JSONObject;

import ais.service.tenant.TenantAuditWriter;

/**
 * <h3>Satu-satunya tempat helper Sales/Inventory menyusun muatan jejak audit tenant.</h3>
 *
 * <p>Penulisnya sendiri, {@link TenantAuditWriter}, sudah ada di lapisan layanan dan tidak
 * mengurus <i>isi</i> muatan — ia tidak dapat menebak medan mana yang rahasia. Yang diurus kelas
 * ini justru itu: kolom apa yang boleh masuk cuplikan, dan bagaimana cuplikannya dibentuk.</p>
 *
 * <h4>Mengapa dipusatkan</h4>
 * <p>Tujuh entitas dicatat dari lima berkas helper yang berbeda. Menyalin aturan muatannya ke
 * tiap berkas berarti tujuh daftar kolom yang boleh berselisih diam-diam — dan riwayat yang
 * bentuknya berbeda antar-entitas tidak lagi dapat dibandingkan maupun ditelusuri dengan satu
 * pembaca. Alasannya sama dengan {@code SalesInventoryTenantSchema}: aturan yang dipakai banyak
 * tempat hanya boleh punya satu rumusan.</p>
 *
 * <h4>Kolom disebut satu per satu, bukan {@code SELECT *}</h4>
 * <p>Dua sebabnya. Pertama, §11.6 melarang jejak audit memuat rahasia, dan {@code SELECT *} akan
 * menyeret kolom apa pun yang ditambahkan bundel migrasi berikutnya — termasuk yang tidak boleh
 * ikut. Kedua, muatan yang isinya berubah-ubah mengikuti skema membuat riwayat lama dan baru
 * tidak lagi dapat dibandingkan.</p>
 *
 * <h4>Yang sengaja TIDAK ikut</h4>
 * <p>Kolom jejak ({@code oleh}, {@code olehid}, {@code dibuat_pada}, {@code tanggal_dirubah})
 * tidak pernah masuk muatan: "siapa dan kapan" sudah ada pada {@code revinfo}, dan menyalinnya ke
 * sini hanya membuat setiap perubahan tampak berbeda pada kolom yang bukan isi datanya.</p>
 * <p>Kolom ringkasan yang diturunkan ({@code terbayar}, {@code sisa} pada
 * {@code piutang_customer}) juga tidak ikut. Model tenant menghitung sisa dari alokasinya, bukan
 * membacanya dari kolom; memasukkannya ke muatan berarti membekukan angka yang bukan sumber
 * kebenaran, dan riwayatnya akan menunjukkan "perubahan" pada tiap alokasi yang menyentuhnya
 * padahal barisnya sendiri tidak disunting.</p>
 */
final class SalesInventoryAudit {

	private SalesInventoryAudit() {
	}

	/**
	 * Kolom yang membentuk cuplikan tiap entitas. Ditulis sebagai pasangan
	 * {@code {tabel, daftar kolom}} supaya seluruh aturannya terbaca sekaligus dalam satu layar
	 * — daftar yang tersebar adalah daftar yang berselisih.
	 */
	private static final String[][] KOLOM = {
			// master
			{ "supplier", "kode, nama, aktif, status" },
			{ "customer", "kode, nama, aktif" },
			{ "salesperson", "kode, nama, aktif, akun_perkiraan, telp" },
			// transaksional
			{ "sales_order", "nomor_dokumen, tanggal, customer_id, salesperson_id, total, status" },
			{ "piutang_customer",
					"nomor_faktur, tanggal, jatuh_tempo, nilai, status, customer_id" },
			{ "penerimaan_piutang",
					"nomor_dokumen, tanggal, cara_bayar, nilai, status, customer_id,"
							+ " pembalik_dari_id" },
			{ "surat_perintah_sales",
					"nomor_dokumen, tanggal, salesperson_id, gudang_id, status" } };

	/** Benar bila entitas ini punya aturan muatan — yakni jejaknya memang dicatat. */
	static boolean dicatat(String tabel) {
		return kolom(tabel) != null;
	}

	private static String kolom(String tabel) {
		for (int i = 0; i < KOLOM.length; i++) {
			if (KOLOM[i][0].equals(tabel)) {
				return KOLOM[i][1];
			}
		}
		return null;
	}

	/**
	 * Kueri cuplikan satu baris.
	 *
	 * <p>{@code tabel} selalu literal dari kode pemanggil — tidak pernah berasal dari permintaan
	 * — dan tetap dicocokkan dengan daftar di atas, sehingga nama yang tidak dikenal berhenti di
	 * sini alih-alih menjadi bagian SQL.</p>
	 */
	static String sqlCuplikan(String skema, String tabel) {
		String k = kolom(tabel);
		if (k == null) {
			throw new IllegalArgumentException("Entitas audit tidak dikenal: " + tabel);
		}
		return "SELECT " + k + " FROM " + skema + tabel + " WHERE id = ?";
	}

	/**
	 * Cuplikan satu baris sebagai teks JSON, atau {@code null} bila barisnya belum/tidak ada
	 * ({@code null} itulah yang menandai penambahan pada kolom {@code sebelum}).
	 *
	 * <p>Nama kolomnya diambil dari metadata hasilnya, sehingga kuerinya yang menentukan isi
	 * muatan — bukan daftar kedua di sini yang bisa berselisih dengannya.</p>
	 */
	static String cuplikan(Session session, String skema, String tabel, Long id) throws Exception {
		if (id == null) {
			return null;
		}
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				sqlCuplikan(skema, tabel));
		try {
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			String muatan = null;
			if (rs.next()) {
				java.sql.ResultSetMetaData md = rs.getMetaData();
				JSONObject o = new JSONObject();
				for (int i = 1; i <= md.getColumnCount(); i++) {
					Object v = rs.getObject(i);
					o.put(md.getColumnLabel(i), v == null ? JSONObject.NULL : String.valueOf(v));
				}
				muatan = o.toString();
			}
			rs.close();
			return muatan;
		} finally {
			ps.close();
		}
	}

	/**
	 * Terbitkan satu revisi audit untuk satu baris.
	 *
	 * <p>Berjalan pada {@code Session} dan transaksi pemanggil — tidak pernah membuka sendiri.
	 * Baris audit <b>wajib</b> berada di transaksi yang sama dengan perubahan datanya: audit yang
	 * commit terpisah dapat bertahan padahal perubahannya dibatalkan, atau hilang padahal
	 * perubahannya jadi. Karena itu pemanggilannya selalu <b>sebelum</b> {@code tx.commit()}.</p>
	 */
	static void catat(Session session, EbisnisActorContextResolver.ActorContext ctx, String aksi,
			String tabel, Long id, int revtype, String sebelum, String sesudah) {
		TenantAuditWriter.catatTunggal(session, ctx.tenant, new TenantAuditWriter.Jejak(aksi),
				tabel, id, revtype, sebelum, sesudah);
	}

	/**
	 * Jalan pintas untuk penambahan: cuplik keadaan barisnya sesudah tersisip, lalu catat.
	 * {@code sebelum} selalu {@code null} — itulah yang membedakan penambahan dari perubahan.
	 */
	static void catatBaru(Session session, EbisnisActorContextResolver.ActorContext ctx,
			String aksi, String skema, String tabel, Long id) throws Exception {
		catat(session, ctx, aksi, tabel, id, TenantAuditWriter.REVTYPE_ADD, null,
				cuplikan(session, skema, tabel, id));
	}
}
