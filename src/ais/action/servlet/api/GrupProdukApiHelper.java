package ais.action.servlet.api;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.inventory.GrupProdukUtil;
import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.GrupProduk;

/**
 * <h3>API JSON Grup Produk (harga terpusat lintas toko) -- dipakai JSP e-Kantin dan POS
 * Desktop/Android.</h3>
 *
 * <p>Logika penyalinan harga SATU dengan layar ZK admin: {@link GrupProdukUtil} (per-baris
 * agar ter-audit Envers, lihat javadoc di sana). Seluruh handler self-guard menu key
 * {@code grup_produk} + aksi granular {@code EbisnisMenuKatalog.bolehAksi} -- fail-closed
 * ({@code grup_produk} terdaftar di {@code KUNCI_DEFAULT_NONAKTIF}): role existing TIDAK
 * mendadak bisa mengubah harga massal lintas outlet; admin menyalakannya per-role lewat
 * grid CRUD TbmroleAction.</p>
 */
public final class GrupProdukApiHelper {

	private GrupProdukApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/** Menu key grup_produk tampil utk pemanggil? (Read-gate; pola ApotikApiHelper.bolehAksi,
	 *  TETAPI pemanggil anonim ditolak eksplisit -- jangan mengandalkan gate dispatcher.) */
	private static boolean bolehLihat(Tbmuser tbmuser) {
		if (tbmuser == null) {
			return false;
		}
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		JSONObject menu = EbisnisMenuKatalog.urai(role.getEbisnisMenu()).optJSONObject("menu");
		return menu != null && menu.optBoolean("grup_produk", false);
	}

	private static boolean bolehAksi(Tbmuser tbmuser, String aksi) {
		if (tbmuser == null) {
			return false;
		}
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return EbisnisMenuKatalog.bolehAksi(
				EbisnisMenuKatalog.urai(role.getEbisnisMenu()), "grup_produk", aksi);
	}

	/** Daftar grup + jumlah produk anggota. Param opsional: {@code cari} (ilike nama/kode),
	 *  {@code hanya_aktif} (default true). */
	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser)) {
			tolak(hasil, "Menu Grup Produk tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria kriteria = session.createCriteria(GrupProduk.class);
			if (request == null || request.optBoolean("hanya_aktif", true)) {
				kriteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			}
			String cari = request == null ? "" : request.optString("cari", "").trim();
			if (!cari.isEmpty()) {
				kriteria.add(Restrictions.or(
						Restrictions.ilike("nama", cari, MatchMode.ANYWHERE),
						Restrictions.ilike("kode", cari, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.asc("nama"));
			@SuppressWarnings("unchecked")
			List<GrupProduk> daftar = kriteria.list();
			JSONArray arr = new JSONArray();
			for (GrupProduk g : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", g.getId());
				o.put("kode", g.getKode() == null ? "" : g.getKode());
				o.put("nama", g.getNama());
				o.put("keterangan", g.getKeterangan() == null ? "" : g.getKeterangan());
				o.put("harga_beli", g.getHargaBeli() == null ? JSONObject.NULL : g.getHargaBeli());
				o.put("harga_jual", g.getHargaJual() == null ? JSONObject.NULL : g.getHargaJual());
				o.put("aktif", Boolean.TRUE.equals(g.getAktif()));
				o.put("jumlah_anggota", GrupProdukUtil.jumlahAnggota(session, g));
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Tambah ({@code id} kosong, aksi {@code create}) / ubah ({@code id} terisi, aksi
	 * {@code update}) grup, lalu salin harga terisi ke seluruh produk anggota dalam SATU
	 * transaksi. Respons memuat {@code diterapkan} = jumlah produk yang harganya berubah.
	 */
	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = (request == null || request.isNull("id") || (request.get("id") + "").trim().isEmpty())
				? null : Long.valueOf((request.get("id") + "").trim());
		if (!bolehAksi(tbmuser, id == null ? "create" : "update")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak "
					+ (id == null ? "menambah" : "mengubah") + " Grup Produk.");
			return;
		}
		String nama = request == null ? "" : request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama grup wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Number duplikat = (Number) session.createCriteria(GrupProduk.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("nama", nama))
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id))
					.uniqueResult();
			if (duplikat != null && duplikat.intValue() > 0) {
				tolak(hasil, "Nama grup sudah terdaftar; gunakan nama lain.");
				return;
			}
			GrupProduk g;
			if (id != null) {
				g = (GrupProduk) session.get(GrupProduk.class, id);
				if (g == null) {
					tolak(hasil, "Grup Produk tidak ditemukan.");
					return;
				}
			} else {
				g = new GrupProduk();
			}
			g.setKode(request.optString("kode", "").trim());
			g.setNama(nama);
			g.setKeterangan(request.optString("keterangan", "").trim());
			g.setHargaBeli(request.isNull("harga_beli") ? null : Double.valueOf(request.getDouble("harga_beli")));
			g.setHargaJual(request.isNull("harga_jual") ? null : Double.valueOf(request.getDouble("harga_jual")));
			if (!request.isNull("aktif")) {
				g.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			}
			if (tbmuser != null) {
				g.setOleh(tbmuser.getUserNama());
				g.setOlehId(tbmuser.getUserId());
			}

			session.beginTransaction();
			session.saveOrUpdate(g);
			int diterapkan = GrupProdukUtil.terapkanHargaKeAnggota(session, g);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", g.getId());
			hasil.put("diterapkan", diterapkan);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "GrupProdukApiHelper.simpan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Hapus grup -- DITOLAK bila masih punya produk anggota (lepas dulu anggotanya lewat layar
	 *  Produk); pola referential-guard yang sama dengan purge master lain. */
	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menghapus Grup Produk.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			GrupProduk g = (GrupProduk) session.get(GrupProduk.class, id);
			if (g == null) {
				tolak(hasil, "Grup Produk tidak ditemukan.");
				return;
			}
			int anggota = GrupProdukUtil.jumlahAnggota(session, g);
			if (anggota > 0) {
				tolak(hasil, "Grup masih memiliki " + anggota
						+ " produk anggota. Lepaskan dahulu produk dari grup ini (layar Produk), atau nonaktifkan grup.");
				return;
			}
			session.beginTransaction();
			session.delete(g);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "GrupProdukApiHelper.hapus rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Dipakai dispatcher: setiap aksi berawalan {@code grup_produk_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		// Alias: klien Flutter Desktop/Android (commit 1c9d7d7) memanggil grup_produk_list --
		// kontrak & bentuk respons sama persis dgn daftar().
		if ("grup_produk_daftar".equals(action) || "grup_produk_list".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("grup_produk_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("grup_produk_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
