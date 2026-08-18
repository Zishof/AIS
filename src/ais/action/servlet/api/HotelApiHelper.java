package ais.action.servlet.api;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.hotel.Kamar;
import ais.database.model.hotel.PropertiHotel;
import ais.database.model.hotel.TipeKamar;

/**
 * <h3>API vertikal MitraInap (hotel) -- MVP langkah 2: master Properti / Tipe Kamar / Kamar.</h3>
 *
 * <p>Pola persis {@link GrupProdukApiHelper}: satu helper per vertikal, dispatcher prefix
 * ({@code hotel_}) di {@code PosApi}, gerbang menu fail-closed (kunci {@code hotel_properti} /
 * {@code hotel_kamar} terdaftar di {@link EbisnisMenuKatalog#KUNCI_DEFAULT_NONAKTIF}) + aksi CRUD
 * granular. Diskriminator baris: semua data ber-FK {@link PropertiHotel} (keputusan 5.1 handover
 * MitraInap 2026-08-18); reservasi/check-in/folio menyusul fase berikutnya.</p>
 */
public final class HotelApiHelper {

	private HotelApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/** Admin global lolos; selain itu kunci menu (aksi null) / aksi CRUD granular harus dinyalakan. */
	private static boolean boleh(Tbmuser tbmuser, String kunci, String aksi) {
		ais.database.model.inventory.Pedagang pedagang = tbmuser == null ? null : tbmuser.getPedagang();
		if (tbmuser != null && pedagang == null) {
			return true;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return false;
		}
		JSONObject izin = EbisnisMenuKatalog.urai(role.getEbisnisMenu());
		if (aksi == null) {
			JSONObject menu = izin.optJSONObject("menu");
			return menu != null && menu.optBoolean(kunci, false);
		}
		return EbisnisMenuKatalog.bolehAksi(izin, kunci, aksi);
	}

	private static Long idDari(JSONObject request, String field) {
		if (request == null || request.isNull(field)) return null;
		String v = (request.opt(field) + "").trim();
		return v.isEmpty() ? null : Long.valueOf(v);
	}

	// ------------------------------------------------------------------ properti

	public static void propertiList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_properti", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Properti Hotel.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(PropertiHotel.class);
			if (request == null || !request.optBoolean("termasuk_nonaktif", false)) {
				c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			}
			c.addOrder(Order.asc("nama"));
			@SuppressWarnings("unchecked")
			List<PropertiHotel> daftar = c.list();
			JSONArray arr = new JSONArray();
			for (PropertiHotel p : daftar) {
				Number kamar = (Number) session.createCriteria(Kamar.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("properti", p)).uniqueResult();
				JSONObject o = new JSONObject();
				o.put("id", p.getId());
				o.put("kode", p.getKode() == null ? "" : p.getKode());
				o.put("nama", p.getNama());
				o.put("alamat", p.getAlamat() == null ? "" : p.getAlamat());
				o.put("kota", p.getKota() == null ? "" : p.getKota());
				o.put("telp", p.getTelp() == null ? "" : p.getTelp());
				o.put("email", p.getEmail() == null ? "" : p.getEmail());
				o.put("keterangan", p.getKeterangan() == null ? "" : p.getKeterangan());
				o.put("jumlah_lantai", p.getJumlahLantai() == null ? JSONObject.NULL : p.getJumlahLantai());
				o.put("aktif", Boolean.TRUE.equals(p.getAktif()));
				o.put("jumlah_kamar", kamar == null ? 0 : kamar.intValue());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void propertiSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = idDari(request, "id");
		if (!boleh(tbmuser, "hotel_properti", id == null ? "create" : "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengelola Properti Hotel.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama properti wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PropertiHotel p;
			if (id == null) {
				p = new PropertiHotel();
			} else {
				p = (PropertiHotel) session.get(PropertiHotel.class, id);
				if (p == null) {
					tolak(hasil, "Properti tidak ditemukan.");
					return;
				}
			}
			p.setKode(request.optString("kode", "").trim());
			p.setNama(nama);
			p.setAlamat(request.optString("alamat", ""));
			p.setKota(request.optString("kota", ""));
			p.setTelp(request.optString("telp", ""));
			p.setEmail(request.optString("email", ""));
			p.setKeterangan(request.optString("keterangan", ""));
			p.setJumlahLantai(request.isNull("jumlah_lantai") ? null
					: Integer.valueOf(request.optInt("jumlah_lantai", 1)));
			p.setAktif(Boolean.valueOf(!request.has("aktif") || request.optBoolean("aktif", true)));
			session.beginTransaction();
			session.saveOrUpdate(p);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", p.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ------------------------------------------------------------------ tipe kamar

	public static void tipeKamarList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_kamar", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Kamar.");
			return;
		}
		Long propertiId = idDari(request, "properti_id");
		if (propertiId == null) {
			tolak(hasil, "properti_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(TipeKamar.class)
					.add(Restrictions.eq("properti", session.load(PropertiHotel.class, propertiId)));
			c.addOrder(Order.asc("nama"));
			@SuppressWarnings("unchecked")
			List<TipeKamar> daftar = c.list();
			JSONArray arr = new JSONArray();
			for (TipeKamar t : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", t.getId());
				o.put("kode", t.getKode() == null ? "" : t.getKode());
				o.put("nama", t.getNama());
				o.put("keterangan", t.getKeterangan() == null ? "" : t.getKeterangan());
				o.put("harga_dasar", t.getHargaDasar() == null ? JSONObject.NULL : t.getHargaDasar());
				o.put("kapasitas", t.getKapasitas() == null ? JSONObject.NULL : t.getKapasitas());
				o.put("aktif", Boolean.TRUE.equals(t.getAktif()));
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void tipeKamarSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = idDari(request, "id");
		if (!boleh(tbmuser, "hotel_kamar", id == null ? "create" : "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengelola Tipe Kamar.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		Long propertiId = idDari(request, "properti_id");
		if (nama.isEmpty() || propertiId == null) {
			tolak(hasil, "Nama tipe kamar dan properti_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PropertiHotel properti = (PropertiHotel) session.get(PropertiHotel.class, propertiId);
			if (properti == null) {
				tolak(hasil, "Properti tidak ditemukan.");
				return;
			}
			TipeKamar t;
			if (id == null) {
				t = new TipeKamar();
			} else {
				t = (TipeKamar) session.get(TipeKamar.class, id);
				if (t == null) {
					tolak(hasil, "Tipe kamar tidak ditemukan.");
					return;
				}
			}
			t.setProperti(properti);
			t.setKode(request.optString("kode", "").trim());
			t.setNama(nama);
			t.setKeterangan(request.optString("keterangan", ""));
			t.setHargaDasar(request.isNull("harga_dasar") || !request.has("harga_dasar")
					? null : Double.valueOf(request.optDouble("harga_dasar", 0)));
			t.setKapasitas(request.isNull("kapasitas") ? null : Integer.valueOf(request.optInt("kapasitas", 2)));
			t.setAktif(Boolean.valueOf(!request.has("aktif") || request.optBoolean("aktif", true)));
			session.beginTransaction();
			session.saveOrUpdate(t);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", t.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ------------------------------------------------------------------ kamar

	public static void kamarList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_kamar", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Kamar.");
			return;
		}
		Long propertiId = idDari(request, "properti_id");
		if (propertiId == null) {
			tolak(hasil, "properti_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(Kamar.class)
					.add(Restrictions.eq("properti", session.load(PropertiHotel.class, propertiId)));
			c.addOrder(Order.asc("nomor"));
			@SuppressWarnings("unchecked")
			List<Kamar> daftar = c.list();
			JSONArray arr = new JSONArray();
			for (Kamar k : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", k.getId());
				o.put("nomor", k.getNomor());
				o.put("lantai", k.getLantai() == null ? JSONObject.NULL : k.getLantai());
				o.put("tipe_kamar_id", k.getTipeKamar() == null ? JSONObject.NULL : k.getTipeKamar().getId());
				o.put("tipe_kamar_nama", k.getTipeKamar() == null ? "" : k.getTipeKamar().getNama());
				o.put("status_hunian", k.getStatusHunian());
				o.put("keterangan", k.getKeterangan() == null ? "" : k.getKeterangan());
				o.put("aktif", Boolean.TRUE.equals(k.getAktif()));
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void kamarSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = idDari(request, "id");
		if (!boleh(tbmuser, "hotel_kamar", id == null ? "create" : "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengelola Kamar.");
			return;
		}
		String nomor = request.optString("nomor", "").trim();
		Long propertiId = idDari(request, "properti_id");
		Long tipeId = idDari(request, "tipe_kamar_id");
		if (nomor.isEmpty() || propertiId == null || tipeId == null) {
			tolak(hasil, "Nomor kamar, properti_id, dan tipe_kamar_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PropertiHotel properti = (PropertiHotel) session.get(PropertiHotel.class, propertiId);
			TipeKamar tipe = (TipeKamar) session.get(TipeKamar.class, tipeId);
			if (properti == null || tipe == null) {
				tolak(hasil, "Properti / tipe kamar tidak ditemukan.");
				return;
			}
			if (tipe.getProperti() == null || !properti.getId().equals(tipe.getProperti().getId())) {
				tolak(hasil, "Tipe kamar bukan milik properti ini.");
				return;
			}
			Number duplikat = (Number) session.createCriteria(Kamar.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("properti", properti))
					.add(Restrictions.eq("nomor", nomor).ignoreCase())
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id))
					.uniqueResult();
			if (duplikat != null && duplikat.intValue() > 0) {
				tolak(hasil, "Nomor kamar sudah dipakai di properti ini.");
				return;
			}
			Kamar k;
			if (id == null) {
				k = new Kamar();
			} else {
				k = (Kamar) session.get(Kamar.class, id);
				if (k == null) {
					tolak(hasil, "Kamar tidak ditemukan.");
					return;
				}
			}
			k.setProperti(properti);
			k.setTipeKamar(tipe);
			k.setNomor(nomor);
			k.setLantai(request.isNull("lantai") ? null : Integer.valueOf(request.optInt("lantai", 1)));
			k.setKeterangan(request.optString("keterangan", ""));
			k.setAktif(Boolean.valueOf(!request.has("aktif") || request.optBoolean("aktif", true)));
			session.beginTransaction();
			session.saveOrUpdate(k);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", k.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Dispatcher: setiap aksi berawalan {@code hotel_} diarahkan ke sini (lihat PosApi). */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("hotel_properti_list".equals(action)) { propertiList(tbmuser, request, hasil); return true; }
		if ("hotel_properti_simpan".equals(action)) { propertiSimpan(tbmuser, request, hasil); return true; }
		if ("hotel_tipe_kamar_list".equals(action)) { tipeKamarList(tbmuser, request, hasil); return true; }
		if ("hotel_tipe_kamar_simpan".equals(action)) { tipeKamarSimpan(tbmuser, request, hasil); return true; }
		if ("hotel_kamar_list".equals(action)) { kamarList(tbmuser, request, hasil); return true; }
		if ("hotel_kamar_simpan".equals(action)) { kamarSimpan(tbmuser, request, hasil); return true; }
		return false;
	}
}
