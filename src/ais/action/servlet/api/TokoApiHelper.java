package ais.action.servlet.api;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.UnitUsahaKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;

/**
 * <h3>API JSON CRUD Toko utk POS Desktop/Android (aksi {@code toko_kelola_*}) + katalog
 * unit usaha ({@code unit_usaha_katalog}).</h3>
 *
 * <p>Melengkapi kanal ZK ({@code TokoAction}) dan JSP ({@code toko.jsp}) supaya CRUD Toko
 * tersedia di KEEMPAT kanal. Seluruh mutasi admin-only ({@code Common.getApakahAdminLain}) --
 * padanan gate {@code isAdmin} di JSP dan checkbox admin-only ZK. Termasuk field baru
 * {@code unitUsahaJson} (multi unit usaha per toko, katalog {@link UnitUsahaKatalog}) yang
 * menjadi dasar generator data contoh produk per jenis usaha.</p>
 */
public final class TokoApiHelper {

	private TokoApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static boolean admin(Tbmuser tbmuser) {
		return tbmuser != null && Common.getApakahAdminLain(tbmuser);
	}

	/** Katalog unit usaha (kode+label+grup) -- utk membangun checkbox di semua kanal klien. */
	public static void unitUsahaKatalog(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if (tbmuser == null) {
			tolak(hasil, "Sesi tidak dikenali.");
			return;
		}
		JSONArray arr = new JSONArray();
		for (UnitUsahaKatalog.Entri e : UnitUsahaKatalog.DAFTAR) {
			arr.put(new JSONObject().put("kode", e.kode).put("label", e.label).put("grup", e.grup));
		}
		hasil.put("status", "00");
		hasil.put("data", arr);
	}

	/** Daftar seluruh toko + unit usahanya. Param opsional {@code cari}. Admin-only (data
	 *  lintas toko; pedagang biasa memakai {@code daftar_toko_saya}/{@code toko_profil_*}). */
	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!admin(tbmuser)) {
			tolak(hasil, "Hanya admin sistem yang dapat mengelola daftar Toko.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria kriteria = session.createCriteria(Toko.class);
			String cari = request == null ? "" : request.optString("cari", "").trim();
			if (!cari.isEmpty()) {
				kriteria.add(Restrictions.or(
						Restrictions.ilike("nama", cari, MatchMode.ANYWHERE),
						Restrictions.ilike("kode", cari, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.asc("nama"));
			@SuppressWarnings("unchecked")
			List<Toko> daftar = kriteria.list();
			JSONArray arr = new JSONArray();
			for (Toko t : daftar) {
				Set<String> unit = UnitUsahaKatalog.urai(t.getUnitUsahaJson());
				JSONArray unitArr = new JSONArray();
				for (String kode : unit) {
					unitArr.put(new JSONObject().put("kode", kode)
							.put("label", UnitUsahaKatalog.labelDari(kode)));
				}
				JSONObject o = new JSONObject();
				o.put("id", t.getId());
				o.put("kode", t.getKode() == null ? "" : t.getKode());
				o.put("nama", t.getNama());
				o.put("keterangan", t.getKeterangan() == null ? "" : t.getKeterangan());
				o.put("aktif", Boolean.TRUE.equals(t.getAktif()));
				o.put("boleh_melihat_toko_lain", Boolean.TRUE.equals(t.getBolehMelihatTokolain()));
				o.put("boleh_transaksi_stok_habis", Boolean.TRUE.equals(t.getBolehTransaksiStokHabis()));
				o.put("toko_demo", Boolean.TRUE.equals(t.getTokoDemo()));
				o.put("unit_usaha", unitArr);
				// Akun akuntansi per outlet -- menempel di master ini (bukan konfigurasi global)
				// supaya tiap toko bisa berbeda kas/piutang/modal/laba ditahannya.
				o.put("akun_kas_id", t.getAkunKas() == null ? JSONObject.NULL : t.getAkunKas().getId());
				o.put("akun_kas_label", ais.action.master.koperasi.helper.AkunKantinUtil.label(t.getAkunKas()));
				o.put("akun_piutang_id", t.getAkunPiutang() == null ? JSONObject.NULL : t.getAkunPiutang().getId());
				o.put("akun_piutang_label", ais.action.master.koperasi.helper.AkunKantinUtil.label(t.getAkunPiutang()));
				o.put("akun_modal_awal_id", t.getAkunModalAwal() == null ? JSONObject.NULL : t.getAkunModalAwal().getId());
				o.put("akun_modal_awal_label", ais.action.master.koperasi.helper.AkunKantinUtil.label(t.getAkunModalAwal()));
				o.put("akun_laba_ditahan_id", t.getAkunLabaDitahan() == null ? JSONObject.NULL : t.getAkunLabaDitahan().getId());
				o.put("akun_laba_ditahan_label", ais.action.master.koperasi.helper.AkunKantinUtil.label(t.getAkunLabaDitahan()));
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Tambah ({@code id} kosong) / ubah ({@code id} terisi) toko. {@code unit_usaha} = JSON
	 * array kode katalog; kode tak dikenal dibuang diam-diam ({@link UnitUsahaKatalog#keJson}).
	 * {@code toko_demo} hanya diubah admin (selalu admin di sini, tapi tetap eksplisit).
	 */
	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!admin(tbmuser)) {
			tolak(hasil, "Hanya admin sistem yang dapat menambah/mengubah Toko.");
			return;
		}
		String nama = request == null ? "" : request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama toko wajib diisi.");
			return;
		}
		Long id = (request.isNull("id") || (request.get("id") + "").trim().isEmpty())
				? null : Long.valueOf((request.get("id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Number duplikat = (Number) session.createCriteria(Toko.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("nama", nama))
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id))
					.uniqueResult();
			if (duplikat != null && duplikat.intValue() > 0) {
				tolak(hasil, "Nama toko sudah terdaftar; gunakan nama lain.");
				return;
			}
			Toko t;
			if (id != null) {
				t = (Toko) session.get(Toko.class, id);
				if (t == null) {
					tolak(hasil, "Toko tidak ditemukan.");
					return;
				}
			} else {
				t = new Toko();
			}
			t.setNama(nama);
			if (!request.isNull("kode")) t.setKode(request.optString("kode", "").trim());
			if (!request.isNull("keterangan")) t.setKeterangan(request.optString("keterangan", "").trim());
			if (!request.isNull("aktif")) t.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			if (!request.isNull("boleh_melihat_toko_lain")) {
				t.setBolehMelihatTokolain(Boolean.valueOf(request.optBoolean("boleh_melihat_toko_lain", false)));
			}
			if (!request.isNull("boleh_transaksi_stok_habis")) {
				t.setBolehTransaksiStokHabis(Boolean.valueOf(request.optBoolean("boleh_transaksi_stok_habis", false)));
			}
			if (!request.isNull("toko_demo")) {
				t.setTokoDemo(Boolean.valueOf(request.optBoolean("toko_demo", false)));
			}
			if (!request.isNull("unit_usaha")) {
				java.util.Set<String> kodeSet = new java.util.LinkedHashSet<String>();
				JSONArray unitArr = request.optJSONArray("unit_usaha");
				if (unitArr != null) {
					for (int i = 0; i < unitArr.length(); i++) {
						kodeSet.add(unitArr.optString(i, "").trim());
					}
				}
				t.setUnitUsahaJson(UnitUsahaKatalog.keJson(kodeSet));
			}
			// Akun akuntansi outlet; kirim 0/null untuk mengosongkan.
			setAkun(session, request, "akun_kas_id", t, "kas");
			setAkun(session, request, "akun_piutang_id", t, "piutang");
			setAkun(session, request, "akun_modal_awal_id", t, "modal");
			setAkun(session, request, "akun_laba_ditahan_id", t, "laba");
			if (tbmuser != null) {
				t.setOleh(tbmuser.getUserNama());
				t.setOlehId(tbmuser.getUserId());
			}

			session.beginTransaction();
			session.saveOrUpdate(t);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", t.getId());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "TokoApiHelper.simpan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Hapus toko -- DITOLAK bila masih direferensikan produk atau pedagang (referential
	 *  guard; pola sama grup produk/purge master lain). */
	/** Pasang akun akuntansi outlet dari id yang dikirim klien; 0/null berarti dikosongkan. */
	private static void setAkun(Session session, JSONObject request, String kunci, Toko t, String jenis) {
		if (request == null || !request.has(kunci)) {
			return;
		}
		ais.database.model.akunting.Akun akun = null;
		if (!request.isNull(kunci) && request.optLong(kunci, 0) > 0) {
			akun = (ais.database.model.akunting.Akun) session.get(ais.database.model.akunting.Akun.class,
					Long.valueOf(request.optLong(kunci)));
		}
		if ("piutang".equals(jenis)) {
			t.setAkunPiutang(akun);
		} else if ("modal".equals(jenis)) {
			t.setAkunModalAwal(akun);
		} else if ("laba".equals(jenis)) {
			t.setAkunLabaDitahan(akun);
		} else {
			t.setAkunKas(akun);
		}
	}

	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!admin(tbmuser)) {
			tolak(hasil, "Hanya admin sistem yang dapat menghapus Toko.");
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
			Toko t = (Toko) session.get(Toko.class, id);
			if (t == null) {
				tolak(hasil, "Toko tidak ditemukan.");
				return;
			}
			Number produk = (Number) session.createCriteria(ais.database.model.inventory.Produk.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("toko", t)).uniqueResult();
			if (produk != null && produk.intValue() > 0) {
				tolak(hasil, "Toko masih memiliki " + produk.intValue()
						+ " produk. Pindahkan/nonaktifkan produknya dahulu, atau nonaktifkan toko saja.");
				return;
			}
			Number pedagang = (Number) session.createCriteria(ais.database.model.inventory.Pedagang.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("toko", t)).uniqueResult();
			if (pedagang != null && pedagang.intValue() > 0) {
				tolak(hasil, "Toko masih memiliki " + pedagang.intValue()
						+ " akun pedagang. Pindahkan/nonaktifkan pedagangnya dahulu, atau nonaktifkan toko saja.");
				return;
			}
			session.beginTransaction();
			session.delete(t);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "TokoApiHelper.hapus rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Dispatcher: aksi {@code toko_kelola_*} + {@code unit_usaha_katalog} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("unit_usaha_katalog".equals(action)) {
			unitUsahaKatalog(tbmuser, request, hasil);
			return true;
		}
		if ("toko_kelola_list".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("toko_kelola_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("toko_kelola_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
