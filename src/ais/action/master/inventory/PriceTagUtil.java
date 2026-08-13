package ais.action.master.inventory;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Produk;

/**
 * Backend bersama untuk fitur <b>Cetak Price Tag / POP</b> (ukuran A2/A4/A5).
 *
 * <p>
 * Menyediakan kueri produk yang dipakai <b>bersama</b> oleh versi JSP
 * ({@code kantin/barang/pricetag_service.jsp}) dan versi ZK ({@code PriceTagKantinAction}), sehingga
 * daftar produk yang muncul untuk dicetak labelnya konsisten di kedua tampilan dan mudah dirawat di
 * satu tempat. Pembuatan tampilan cetak (HTML/CSS {@code @page} + barcode) dipusatkan terpisah di
 * berkas JavaScript bersama {@code webapp/js/ais_pricetag_print.js}.
 * </p>
 *
 * <p>
 * Sesi mengikuti pemanggil: bila {@code session} {@code null} dipakai
 * {@link HibernateUtil#currentSession()} (dikelola kerangka kerja, tidak ditutup manual). Tidak ada
 * {@code openSession()}/{@code currentNativeSession()} yang dibuka sehingga tidak ada sesi
 * menggantung. Kompatibel Java 1.7 (tanpa lambda/stream).
 * </p>
 *
 * @author AIS
 */
public final class PriceTagUtil {

	private PriceTagUtil() {
	}

	/**
	 * Daftar produk aktif milik sebuah toko (untuk dibuatkan price tag), bisa disaring kata kunci.
	 *
	 * @param session sesi Hibernate; bila {@code null} dipakai {@code currentSession()}.
	 * @param tokoId  id toko/kios (wajib); bila {@code null} dikembalikan daftar kosong.
	 * @param q       kata kunci cari pada nama/kode/barcode (opsional).
	 * @return SELURUH produk yang cocok, terurut nama (tanpa batas jumlah -- lihat JavaDoc overload
	 *         5-argumen soal alasan batas 1000 sebelumnya DIHAPUS); tidak pernah {@code null}.
	 */
	public static List<Produk> listProduk(Session session, Long tokoId, String q) {
		return listProduk(session, tokoId, q, false, false);
	}

	/**
	 * Overload gap-closure "opsi tampilkan barang milik semua toko atau toko == null" (layar admin
	 * Produk Desktop/Android) -- SENGAJA method terpisah, BUKAN mengubah perilaku default overload
	 * 3-argumen di atas (dipakai fitur Price Tag yang HARUS selalu toko-scoped ketat).
	 *
	 * <p>Batasan keamanan: {@code semuaToko=true} utk akun TERIKAT satu toko (pedagang non-null)
	 * TIDAK PERNAH menampilkan data toko LAIN -- hanya menambahkan produk {@code toko IS NULL}
	 * (orphan/salah input) ke hasil toko sendiri. Melihat katalog toko lain (data harga/stok
	 * proprietary) hanya utk admin global ({@code adminGlobal=true}, tanpa Pedagang terkait).</p>
	 *
	 * @param semuaToko   true = jangan batasi ketat ke satu toko (lihat catatan keamanan di atas).
	 * @param adminGlobal true bila pemanggil admin global (tanpa Pedagang) -- menentukan APAKAH
	 *                    {@code semuaToko} boleh membuka data lintas-toko sungguhan atau hanya orphan.
	 */
	@SuppressWarnings("unchecked")
	public static List<Produk> listProduk(Session session, Long tokoId, String q, boolean semuaToko, boolean adminGlobal) {
		return listProduk(session, tokoId, q, semuaToko, adminGlobal, null);
	}

	/**
	 * Overload gap-closure "Jenis Item" (Produk vs Bahan Baku) -- SENGAJA method terpisah (bukan
	 * mengubah overload 5-argumen di atas) supaya pemanggil lama yang belum peduli Jenis Item
	 * (mis. Price Tag, yang boleh mencetak label utk Bahan Baku juga) tidak perlu ikut berubah.
	 *
	 * @param jenisItemFilter {@code null}/kosong = tanpa filter (perilaku lama, dipakai layar admin
	 *                        Produk yang harus melihat SEMUA baris apa pun jenisnya); {@code "JUAL"}
	 *                        = exclude baris {@code jenis_item IN ('BAHAN','EKSTRA')} (dipakai
	 *                        katalog Kasir -- bahan baku & ekstra tidak boleh dijual/dicari langsung
	 *                        sbg baris mandiri); {@code "BAHAN"} = HANYA baris
	 *                        {@code jenis_item="BAHAN"} (dipakai picker "Pilih Bahan Baku" pada
	 *                        editor Resep/HPP); {@code "EKSTRA"} = HANYA baris
	 *                        {@code jenis_item="EKSTRA"} (dipakai picker "Pilih Ekstra" pada produk
	 *                        dasar). Filter exclude WAJIB pola
	 *                        {@code OR isNull} -- {@link Produk#getJenisItem()} memperlakukan NULL
	 *                        sbg {@code "JUAL"}, tapi filter Hibernate/SQL beroperasi di kolom DB
	 *                        mentah, dan {@code Restrictions.ne}/{@code <>} TIDAK PERNAH match NULL
	 *                        di Postgres -- pola {@code ne} polos akan diam-diam menyembunyikan
	 *                        SELURUH produk lama (dibuat sebelum kolom ini ada) dari Kasir.
	 */
	@SuppressWarnings("unchecked")
	public static List<Produk> listProduk(Session session, Long tokoId, String q, boolean semuaToko, boolean adminGlobal,
			String jenisItemFilter) {
		return listProduk(session, tokoId, q, semuaToko, adminGlobal, jenisItemFilter, -1, -1);
	}

	@SuppressWarnings("unchecked")
	public static List<Produk> listProduk(Session session, Long tokoId, String q, boolean semuaToko,
			boolean adminGlobal, String jenisItemFilter, int offset, int limit) {
		return listProduk(session, tokoId, q, semuaToko, adminGlobal, jenisItemFilter, null, offset, limit);
	}

	@SuppressWarnings("unchecked")
	public static List<Produk> listProduk(Session session, Long tokoId, String q, boolean semuaToko,
			boolean adminGlobal, String jenisItemFilter, Long kategoriId, int offset, int limit) {
		if (tokoId == null && !(semuaToko && adminGlobal)) {
			return new ArrayList<Produk>();
		}
		Session s = session == null ? HibernateUtil.currentSession() : session;
		// Gap-closure: batas 1000 SEBELUMNYA di sini diam-diam memotong katalog toko dgn lebih dari
		// 1000 produk aktif (nyata terjadi -- toko dgn puluhan ribu produk hasil impor Excel massal)
		// -- method ini kini dipakai jg sbg sumber SATU-SATUNYA sinkron cache lokal Kasir (Desktop+
		// Android, lihat PosApi.prosesKatalog), yang WAJIB mengembalikan SELURUH katalog, bukan hanya
		// sebagian. Fitur Price Tag (pemanggil ASLI batas ini) tetap aman tanpa batas krn pemakainya
		// selalu menyaring dulu lewat `q` sebelum mencetak.
		Criteria c = buatCriteria(s, tokoId, q, semuaToko, adminGlobal, jenisItemFilter, kategoriId)
				.addOrder(Order.asc("nama"));
		if (offset >= 0) c.setFirstResult(offset);
		if (limit > 0) c.setMaxResults(limit);
		return c.list();
	}

	public static long countProduk(Session session, Long tokoId, String q, boolean semuaToko,
			boolean adminGlobal, String jenisItemFilter) {
		return countProduk(session, tokoId, q, semuaToko, adminGlobal, jenisItemFilter, null);
	}

	public static long countProduk(Session session, Long tokoId, String q, boolean semuaToko,
			boolean adminGlobal, String jenisItemFilter, Long kategoriId) {
		if (tokoId == null && !(semuaToko && adminGlobal)) return 0L;
		Session s = session == null ? HibernateUtil.currentSession() : session;
		Object nilai = buatCriteria(s, tokoId, q, semuaToko, adminGlobal, jenisItemFilter, kategoriId)
				.setProjection(Projections.rowCount()).uniqueResult();
		return nilai instanceof Number ? ((Number) nilai).longValue() : 0L;
	}

	private static Criteria buatCriteria(Session s, Long tokoId, String q, boolean semuaToko,
			boolean adminGlobal, String jenisItemFilter, Long kategoriId) {
		Criteria c = s.createCriteria(Produk.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (semuaToko) {
			if (!adminGlobal) {
				c.add(Restrictions.or(Restrictions.eq("toko.id", tokoId), Restrictions.isNull("toko")));
			} // adminGlobal+semuaToko: TIDAK ada batasan toko sama sekali (diagnostik lintas-sistem).
		} else {
			c.add(Restrictions.eq("toko.id", tokoId));
		}
		if (q != null && q.trim().length() > 0) {
			// OR ke barcode jg (gap-closure) -- kotak cari yg sama dipakai kasir utk ketik nama/kode
			// MAUPUN scan barcode fisik produk, jadi kata kunci itu harus dicocokkan ke ketiganya.
			c.add(Restrictions.or(Restrictions.or(Restrictions.ilike("nama", q.trim(), MatchMode.ANYWHERE),
					Restrictions.ilike("kode", q.trim(), MatchMode.ANYWHERE)),
					Restrictions.ilike("barcode", q.trim(), MatchMode.ANYWHERE)));
		}
		if (kategoriId != null) c.add(Restrictions.eq("jenisProduk.id", kategoriId));
		if ("JUAL".equals(jenisItemFilter)) {
			// Gap-closure "Produk Ekstra" -- Ekstra jg dikeluarkan dari katalog Kasir/admin-JUAL
			// persis spt Bahan Baku (hanya boleh dipilih via picker "Pilih Ekstra" pada produk dasar).
			c.add(Restrictions.or(Restrictions.isNull("jenisItem"),
					Restrictions.and(Restrictions.ne("jenisItem", "BAHAN"), Restrictions.ne("jenisItem", "EKSTRA"))));
		} else if ("BAHAN".equals(jenisItemFilter)) {
			c.add(Restrictions.eq("jenisItem", "BAHAN"));
		} else if ("EKSTRA".equals(jenisItemFilter)) {
			c.add(Restrictions.eq("jenisItem", "EKSTRA"));
		}
		return c;
	}
}
