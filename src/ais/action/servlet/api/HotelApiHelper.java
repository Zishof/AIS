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

	// ------------------------------------------------------------------ tamu

	public static void tamuList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_reservasi", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Reservasi/Tamu.");
			return;
		}
		Long propertiId = idDari(request, "properti_id");
		if (propertiId == null) {
			tolak(hasil, "properti_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ais.database.model.hotel.Tamu.class)
					.add(Restrictions.eq("properti", session.load(PropertiHotel.class, propertiId)));
			String cari = request.optString("cari", "").trim();
			if (!cari.isEmpty()) {
				c.add(Restrictions.or(
						Restrictions.ilike("nama", cari, org.hibernate.criterion.MatchMode.ANYWHERE),
						Restrictions.ilike("telp", cari, org.hibernate.criterion.MatchMode.ANYWHERE)));
			}
			c.addOrder(Order.asc("nama")).setMaxResults(200);
			@SuppressWarnings("unchecked")
			List<ais.database.model.hotel.Tamu> daftar = c.list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.hotel.Tamu t : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", t.getId());
				o.put("nama", t.getNama());
				o.put("jenis_identitas", t.getJenisIdentitas() == null ? "" : t.getJenisIdentitas());
				o.put("no_identitas", t.getNoIdentitas() == null ? "" : t.getNoIdentitas());
				o.put("telp", t.getTelp() == null ? "" : t.getTelp());
				o.put("email", t.getEmail() == null ? "" : t.getEmail());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void tamuSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = idDari(request, "id");
		if (!boleh(tbmuser, "hotel_reservasi", id == null ? "create" : "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengelola Tamu.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		Long propertiId = idDari(request, "properti_id");
		if (nama.isEmpty() || propertiId == null) {
			tolak(hasil, "Nama tamu dan properti_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PropertiHotel properti = (PropertiHotel) session.get(PropertiHotel.class, propertiId);
			if (properti == null) {
				tolak(hasil, "Properti tidak ditemukan.");
				return;
			}
			ais.database.model.hotel.Tamu t;
			if (id == null) {
				t = new ais.database.model.hotel.Tamu();
				t.setProperti(properti);
			} else {
				t = (ais.database.model.hotel.Tamu) session.get(ais.database.model.hotel.Tamu.class, id);
				if (t == null) {
					tolak(hasil, "Tamu tidak ditemukan.");
					return;
				}
			}
			t.setNama(nama);
			t.setJenisIdentitas(request.optString("jenis_identitas", ""));
			t.setNoIdentitas(request.optString("no_identitas", ""));
			t.setTelp(request.optString("telp", ""));
			t.setEmail(request.optString("email", ""));
			t.setAlamat(request.optString("alamat", ""));
			t.setKeterangan(request.optString("keterangan", ""));
			session.beginTransaction();
			session.saveOrUpdate(t);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", t.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ------------------------------------------------------------------ reservasi

	public static void reservasiList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_reservasi", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Reservasi.");
			return;
		}
		Long propertiId = idDari(request, "properti_id");
		if (propertiId == null) {
			tolak(hasil, "properti_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ais.database.model.hotel.ReservasiKamar.class)
					.add(Restrictions.eq("properti", session.load(PropertiHotel.class, propertiId)));
			String status = request.optString("status", "").trim();
			if (!status.isEmpty()) {
				c.add(Restrictions.eq("status", status));
			}
			c.addOrder(Order.desc("id")).setMaxResults(300);
			@SuppressWarnings("unchecked")
			List<ais.database.model.hotel.ReservasiKamar> daftar = c.list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.hotel.ReservasiKamar r : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", r.getId());
				o.put("kode", r.getKode());
				o.put("tamu_id", r.getTamu().getId());
				o.put("tamu_nama", r.getTamu().getNama());
				o.put("tipe_kamar_id", r.getTipeKamar().getId());
				o.put("tipe_kamar_nama", r.getTipeKamar().getNama());
				o.put("kamar_id", r.getKamar() == null ? JSONObject.NULL : r.getKamar().getId());
				o.put("kamar_nomor", r.getKamar() == null ? "" : r.getKamar().getNomor());
				o.put("tanggal_checkin", String.valueOf(r.getTanggalCheckin()));
				o.put("tanggal_checkout", String.valueOf(r.getTanggalCheckout()));
				o.put("jumlah_tamu", r.getJumlahTamu() == null ? JSONObject.NULL : r.getJumlahTamu());
				o.put("harga_per_malam", r.getHargaPerMalam() == null ? JSONObject.NULL : r.getHargaPerMalam());
				o.put("status", r.getStatus());
				o.put("catatan", r.getCatatan() == null ? "" : r.getCatatan());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void reservasiBuat(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_reservasi", "create")) {
			tolak(hasil, "Anda tidak memiliki hak membuat Reservasi.");
			return;
		}
		Long propertiId = idDari(request, "properti_id");
		Long tamuId = idDari(request, "tamu_id");
		Long tipeId = idDari(request, "tipe_kamar_id");
		java.util.Date masuk = tanggalDari(request, "tanggal_checkin");
		java.util.Date keluar = tanggalDari(request, "tanggal_checkout");
		if (propertiId == null || tamuId == null || tipeId == null || masuk == null || keluar == null) {
			tolak(hasil, "properti_id, tamu_id, tipe_kamar_id, tanggal_checkin, tanggal_checkout wajib diisi.");
			return;
		}
		if (!keluar.after(masuk)) {
			tolak(hasil, "Tanggal checkout harus setelah tanggal checkin.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PropertiHotel properti = (PropertiHotel) session.get(PropertiHotel.class, propertiId);
			ais.database.model.hotel.Tamu tamu = (ais.database.model.hotel.Tamu) session
					.get(ais.database.model.hotel.Tamu.class, tamuId);
			TipeKamar tipe = (TipeKamar) session.get(TipeKamar.class, tipeId);
			if (properti == null || tamu == null || tipe == null) {
				tolak(hasil, "Properti / tamu / tipe kamar tidak ditemukan.");
				return;
			}
			if (!propertiId.equals(tamu.getProperti().getId()) || !propertiId.equals(tipe.getProperti().getId())) {
				tolak(hasil, "Tamu / tipe kamar bukan milik properti ini.");
				return;
			}
			ais.database.model.hotel.ReservasiKamar r = new ais.database.model.hotel.ReservasiKamar();
			r.setProperti(properti);
			r.setTamu(tamu);
			r.setTipeKamar(tipe);
			r.setKode("RSV-" + System.currentTimeMillis());
			r.setTanggalCheckin(masuk);
			r.setTanggalCheckout(keluar);
			r.setJumlahTamu(request.isNull("jumlah_tamu") ? null : Integer.valueOf(request.optInt("jumlah_tamu", 1)));
			// Snapshot harga saat booking -- perubahan master tidak menyentuh reservasi berjalan.
			r.setHargaPerMalam(request.has("harga_per_malam") && !request.isNull("harga_per_malam")
					? Double.valueOf(request.optDouble("harga_per_malam", 0)) : tipe.getHargaDasar());
			r.setStatus(ais.database.model.hotel.ReservasiKamar.STATUS_BOOKED);
			r.setCatatan(request.optString("catatan", ""));
			session.beginTransaction();
			session.save(r);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", r.getId());
			hasil.put("kode", r.getKode());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void reservasiBatalkan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_reservasi", "update")) {
			tolak(hasil, "Anda tidak memiliki hak membatalkan Reservasi.");
			return;
		}
		Long id = idDari(request, "id");
		if (id == null) {
			tolak(hasil, "Id reservasi wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.hotel.ReservasiKamar r = (ais.database.model.hotel.ReservasiKamar) session
					.get(ais.database.model.hotel.ReservasiKamar.class, id);
			if (r == null) {
				tolak(hasil, "Reservasi tidak ditemukan.");
				return;
			}
			// Transisi divalidasi server: hanya BOOKED/CONFIRMED yang boleh dibatalkan.
			if (!ais.database.model.hotel.ReservasiKamar.STATUS_BOOKED.equals(r.getStatus())
					&& !ais.database.model.hotel.ReservasiKamar.STATUS_CONFIRMED.equals(r.getStatus())) {
				tolak(hasil, "Reservasi berstatus " + r.getStatus() + " tidak dapat dibatalkan.");
				return;
			}
			r.setStatus(ais.database.model.hotel.ReservasiKamar.STATUS_CANCELLED);
			String alasan = request.optString("alasan", "").trim();
			if (!alasan.isEmpty()) {
				r.setCatatan(((r.getCatatan() == null ? "" : r.getCatatan() + " | ") + "Dibatalkan: " + alasan));
			}
			session.beginTransaction();
			session.saveOrUpdate(r);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ------------------------------------------------------------------ check-in / check-out / pindah

	/** Check-in dari reservasi ATAU walk-in (tamu_id langsung). Atomik: stay + folio + status kamar. */
	public static void checkin(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_checkin", "create")) {
			tolak(hasil, "Anda tidak memiliki hak Check-in.");
			return;
		}
		Long kamarId = idDari(request, "kamar_id");
		Long reservasiId = idDari(request, "reservasi_id");
		Long tamuId = idDari(request, "tamu_id");
		if (kamarId == null || (reservasiId == null && tamuId == null)) {
			tolak(hasil, "kamar_id dan (reservasi_id atau tamu_id utk walk-in) wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Kamar kamar = (Kamar) session.get(Kamar.class, kamarId);
			if (kamar == null || !Boolean.TRUE.equals(kamar.getAktif())) {
				tolak(hasil, "Kamar tidak ditemukan / tidak aktif.");
				return;
			}
			if (!Kamar.HUNIAN_VACANT.equals(kamar.getStatusHunian())) {
				tolak(hasil, "Kamar " + kamar.getNomor() + " berstatus " + kamar.getStatusHunian()
						+ " -- hanya kamar VACANT yang bisa di-check-in.");
				return;
			}
			ais.database.model.hotel.ReservasiKamar reservasi = null;
			ais.database.model.hotel.Tamu tamu;
			Double harga;
			if (reservasiId != null) {
				reservasi = (ais.database.model.hotel.ReservasiKamar) session
						.get(ais.database.model.hotel.ReservasiKamar.class, reservasiId);
				if (reservasi == null) {
					tolak(hasil, "Reservasi tidak ditemukan.");
					return;
				}
				if (!ais.database.model.hotel.ReservasiKamar.STATUS_BOOKED.equals(reservasi.getStatus())
						&& !ais.database.model.hotel.ReservasiKamar.STATUS_CONFIRMED.equals(reservasi.getStatus())) {
					tolak(hasil, "Reservasi berstatus " + reservasi.getStatus() + " tidak dapat check-in.");
					return;
				}
				if (!reservasi.getProperti().getId().equals(kamar.getProperti().getId())) {
					tolak(hasil, "Reservasi bukan milik properti kamar ini.");
					return;
				}
				tamu = reservasi.getTamu();
				harga = reservasi.getHargaPerMalam();
			} else {
				tamu = (ais.database.model.hotel.Tamu) session.get(ais.database.model.hotel.Tamu.class, tamuId);
				if (tamu == null || !tamu.getProperti().getId().equals(kamar.getProperti().getId())) {
					tolak(hasil, "Tamu tidak ditemukan / bukan milik properti kamar ini.");
					return;
				}
				harga = request.has("harga_per_malam") && !request.isNull("harga_per_malam")
						? Double.valueOf(request.optDouble("harga_per_malam", 0))
						: (kamar.getTipeKamar() == null ? null : kamar.getTipeKamar().getHargaDasar());
			}
			java.util.Date kini = new java.util.Date();
			ais.database.model.hotel.MenginapTamu stay = new ais.database.model.hotel.MenginapTamu();
			stay.setProperti(kamar.getProperti());
			stay.setReservasi(reservasi);
			stay.setTamu(tamu);
			stay.setKamar(kamar);
			stay.setCheckinPada(kini);
			stay.setHargaPerMalam(harga);
			stay.setStatus(ais.database.model.hotel.MenginapTamu.STATUS_IN_HOUSE);
			stay.setCatatan(request.optString("catatan", ""));

			session.beginTransaction();
			session.save(stay);
			ais.database.model.hotel.Folio folio = new ais.database.model.hotel.Folio();
			folio.setProperti(kamar.getProperti());
			folio.setMenginap(stay);
			folio.setStatus(ais.database.model.hotel.Folio.STATUS_OPEN);
			folio.setDibukaPada(kini);
			session.save(folio);
			kamar.setStatusHunian(Kamar.HUNIAN_OCCUPIED);
			session.saveOrUpdate(kamar);
			if (reservasi != null) {
				reservasi.setStatus(ais.database.model.hotel.ReservasiKamar.STATUS_CHECKED_IN);
				reservasi.setKamar(kamar);
				session.saveOrUpdate(reservasi);
			}
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("menginap_id", stay.getId());
			hasil.put("folio_id", folio.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void pindahKamar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_checkin", "update")) {
			tolak(hasil, "Anda tidak memiliki hak Pindah Kamar.");
			return;
		}
		Long stayId = idDari(request, "menginap_id");
		Long kamarBaruId = idDari(request, "kamar_baru_id");
		if (stayId == null || kamarBaruId == null) {
			tolak(hasil, "menginap_id dan kamar_baru_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.hotel.MenginapTamu stay = (ais.database.model.hotel.MenginapTamu) session
					.get(ais.database.model.hotel.MenginapTamu.class, stayId);
			Kamar baru = (Kamar) session.get(Kamar.class, kamarBaruId);
			if (stay == null || baru == null) {
				tolak(hasil, "Data menginap / kamar tujuan tidak ditemukan.");
				return;
			}
			if (!ais.database.model.hotel.MenginapTamu.STATUS_IN_HOUSE.equals(stay.getStatus())) {
				tolak(hasil, "Tamu sudah tidak menginap (status " + stay.getStatus() + ").");
				return;
			}
			if (!Boolean.TRUE.equals(baru.getAktif()) || !Kamar.HUNIAN_VACANT.equals(baru.getStatusHunian())
					|| !baru.getProperti().getId().equals(stay.getProperti().getId())) {
				tolak(hasil, "Kamar tujuan harus VACANT, aktif, dan milik properti yang sama.");
				return;
			}
			Kamar lama = stay.getKamar();
			session.beginTransaction();
			lama.setStatusHunian(Kamar.HUNIAN_DIRTY);
			session.saveOrUpdate(lama);
			baru.setStatusHunian(Kamar.HUNIAN_OCCUPIED);
			session.saveOrUpdate(baru);
			stay.setKamar(baru);
			stay.setCatatan(((stay.getCatatan() == null ? "" : stay.getCatatan() + " | ")
					+ "Pindah kamar " + lama.getNomor() + " -> " + baru.getNomor()));
			session.saveOrUpdate(stay);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Check-out atomik: posting ROOM_CHARGE (malam x harga snapshot), pembayaran opsional
	 * {@code bayar_sekarang}, WAJIB saldo folio &lt;= 0, tutup folio + stay, kamar jadi DIRTY.
	 */
	public static void checkout(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_checkin", "update")) {
			tolak(hasil, "Anda tidak memiliki hak Check-out.");
			return;
		}
		Long stayId = idDari(request, "menginap_id");
		if (stayId == null) {
			tolak(hasil, "menginap_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.hotel.MenginapTamu stay = (ais.database.model.hotel.MenginapTamu) session
					.get(ais.database.model.hotel.MenginapTamu.class, stayId);
			if (stay == null) {
				tolak(hasil, "Data menginap tidak ditemukan.");
				return;
			}
			if (!ais.database.model.hotel.MenginapTamu.STATUS_IN_HOUSE.equals(stay.getStatus())) {
				tolak(hasil, "Tamu sudah check-out (status " + stay.getStatus() + ").");
				return;
			}
			ais.database.model.hotel.Folio folio = folioDariStay(session, stay);
			if (folio == null || !ais.database.model.hotel.Folio.STATUS_OPEN.equals(folio.getStatus())) {
				tolak(hasil, "Folio tidak ditemukan / sudah ditutup.");
				return;
			}
			java.util.Date kini = new java.util.Date();
			long malam = (kini.getTime() - stay.getCheckinPada().getTime() + 86399999L) / 86400000L;
			if (malam < 1) malam = 1;
			double hargaMalam = stay.getHargaPerMalam() == null ? 0 : stay.getHargaPerMalam().doubleValue();
			double roomCharge = malam * hargaMalam;

			session.beginTransaction();
			if (roomCharge > 0) {
				tambahTransaksiFolio(session, folio,
						ais.database.model.hotel.FolioTransaksi.JENIS_ROOM_CHARGE,
						"Sewa kamar " + stay.getKamar().getNomor() + " x " + malam + " malam",
						roomCharge, "ROOMCHARGE-CHECKOUT-" + stay.getId(), kini, tbmuser);
			}
			double bayar = request.optDouble("bayar_sekarang", 0);
			if (bayar > 0) {
				tambahTransaksiFolio(session, folio,
						ais.database.model.hotel.FolioTransaksi.JENIS_PAYMENT,
						request.optString("metode_bayar", "Tunai"),
						-bayar, null, kini, tbmuser);
			}
			session.flush();
			double saldo = saldoFolio(session, folio);
			if (saldo > 0.009) {
				session.getTransaction().rollback();
				hasil.put("status", "91");
				hasil.put("description", "Saldo folio masih Rp " + String.valueOf(Math.round(saldo))
						+ " -- lunasi dulu (kirim bayar_sekarang atau tambah PAYMENT ke folio).");
				hasil.put("saldo", saldo);
				return;
			}
			folio.setStatus(ais.database.model.hotel.Folio.STATUS_CLOSED);
			folio.setDitutupPada(kini);
			session.saveOrUpdate(folio);
			stay.setStatus(ais.database.model.hotel.MenginapTamu.STATUS_CHECKED_OUT);
			stay.setCheckoutPada(kini);
			session.saveOrUpdate(stay);
			Kamar kamar = stay.getKamar();
			kamar.setStatusHunian(Kamar.HUNIAN_DIRTY);
			session.saveOrUpdate(kamar);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("malam", malam);
			hasil.put("room_charge", roomCharge);
			hasil.put("saldo_akhir", saldo);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ------------------------------------------------------------------ folio

	public static void folioGet(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_folio", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Folio.");
			return;
		}
		Long folioId = idDari(request, "folio_id");
		Long stayId = idDari(request, "menginap_id");
		if (folioId == null && stayId == null) {
			tolak(hasil, "folio_id atau menginap_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.hotel.Folio folio;
			if (folioId != null) {
				folio = (ais.database.model.hotel.Folio) session.get(ais.database.model.hotel.Folio.class, folioId);
			} else {
				ais.database.model.hotel.MenginapTamu stay = (ais.database.model.hotel.MenginapTamu) session
						.get(ais.database.model.hotel.MenginapTamu.class, stayId);
				folio = stay == null ? null : folioDariStay(session, stay);
			}
			if (folio == null) {
				tolak(hasil, "Folio tidak ditemukan.");
				return;
			}
			@SuppressWarnings("unchecked")
			List<ais.database.model.hotel.FolioTransaksi> trx = session
					.createCriteria(ais.database.model.hotel.FolioTransaksi.class)
					.add(Restrictions.eq("folio", folio)).addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.hotel.FolioTransaksi t : trx) {
				JSONObject o = new JSONObject();
				o.put("id", t.getId());
				o.put("jenis", t.getJenis());
				o.put("keterangan", t.getKeterangan() == null ? "" : t.getKeterangan());
				o.put("jumlah", t.getJumlah());
				o.put("referensi", t.getReferensi() == null ? "" : t.getReferensi());
				o.put("waktu", String.valueOf(t.getWaktu()));
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("folio_id", folio.getId());
			hasil.put("menginap_id", folio.getMenginap().getId());
			hasil.put("status_folio", folio.getStatus());
			hasil.put("saldo", saldoFolio(session, folio));
			hasil.put("transaksi", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Tambah PAYMENT/ADJUSTMENT ke folio OPEN. Idempoten by {@code referensi} bila diisi. */
	public static void folioTransaksiTambah(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_folio", "create")) {
			tolak(hasil, "Anda tidak memiliki hak menambah transaksi Folio.");
			return;
		}
		Long folioId = idDari(request, "folio_id");
		String jenis = request.optString("jenis", "").trim().toUpperCase();
		double jumlah = request.optDouble("jumlah", 0);
		if (folioId == null || jumlah <= 0
				|| (!ais.database.model.hotel.FolioTransaksi.JENIS_PAYMENT.equals(jenis)
					&& !ais.database.model.hotel.FolioTransaksi.JENIS_ADJUSTMENT.equals(jenis))) {
			tolak(hasil, "folio_id, jenis (PAYMENT/ADJUSTMENT), dan jumlah > 0 wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.hotel.Folio folio = (ais.database.model.hotel.Folio) session
					.get(ais.database.model.hotel.Folio.class, folioId);
			if (folio == null || !ais.database.model.hotel.Folio.STATUS_OPEN.equals(folio.getStatus())) {
				tolak(hasil, "Folio tidak ditemukan / sudah ditutup.");
				return;
			}
			String referensi = request.optString("referensi", "").trim();
			if (!referensi.isEmpty()) {
				Number ada = (Number) session.createCriteria(ais.database.model.hotel.FolioTransaksi.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("folio", folio))
						.add(Restrictions.eq("referensi", referensi)).uniqueResult();
				if (ada != null && ada.intValue() > 0) {
					hasil.put("status", "00");
					hasil.put("idempotent", true);
					hasil.put("saldo", saldoFolio(session, folio));
					return;
				}
			}
			double bertanda = ais.database.model.hotel.FolioTransaksi.JENIS_PAYMENT.equals(jenis)
					? -jumlah
					: (request.optBoolean("mengurangi", false) ? -jumlah : jumlah);
			session.beginTransaction();
			tambahTransaksiFolio(session, folio, jenis, request.optString("keterangan", jenis),
					bertanda, referensi.isEmpty() ? null : referensi, new java.util.Date(), tbmuser);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("saldo", saldoFolio(session, folio));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Daftar stay per properti (filter status opsional, default IN_HOUSE) -- utk layar check-out/folio. */
	public static void menginapList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_checkin", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Check-in/Check-out.");
			return;
		}
		Long propertiId = idDari(request, "properti_id");
		if (propertiId == null) {
			tolak(hasil, "properti_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String status = request.optString("status", ais.database.model.hotel.MenginapTamu.STATUS_IN_HOUSE).trim();
			org.hibernate.Criteria c = session.createCriteria(ais.database.model.hotel.MenginapTamu.class)
					.add(Restrictions.eq("properti", session.load(PropertiHotel.class, propertiId)));
			if (!status.isEmpty() && !"SEMUA".equalsIgnoreCase(status)) {
				c.add(Restrictions.eq("status", status));
			}
			c.addOrder(Order.desc("id")).setMaxResults(300);
			@SuppressWarnings("unchecked")
			List<ais.database.model.hotel.MenginapTamu> daftar = c.list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.hotel.MenginapTamu m : daftar) {
				ais.database.model.hotel.Folio folio = folioDariStay(session, m);
				JSONObject o = new JSONObject();
				o.put("id", m.getId());
				o.put("tamu_nama", m.getTamu().getNama());
				o.put("kamar_nomor", m.getKamar().getNomor());
				o.put("checkin_pada", String.valueOf(m.getCheckinPada()));
				o.put("checkout_pada", m.getCheckoutPada() == null ? "" : String.valueOf(m.getCheckoutPada()));
				o.put("harga_per_malam", m.getHargaPerMalam() == null ? JSONObject.NULL : m.getHargaPerMalam());
				o.put("status", m.getStatus());
				o.put("folio_id", folio == null ? JSONObject.NULL : folio.getId());
				o.put("saldo_folio", folio == null ? JSONObject.NULL : saldoFolio(session, folio));
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static ais.database.model.hotel.Folio folioDariStay(Session session,
			ais.database.model.hotel.MenginapTamu stay) {
		return (ais.database.model.hotel.Folio) session
				.createCriteria(ais.database.model.hotel.Folio.class)
				.add(Restrictions.eq("menginap", stay))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
	}

	private static double saldoFolio(Session session, ais.database.model.hotel.Folio folio) {
		Number n = (Number) session.createCriteria(ais.database.model.hotel.FolioTransaksi.class)
				.setProjection(Projections.sum("jumlah"))
				.add(Restrictions.eq("folio", folio)).uniqueResult();
		return n == null ? 0 : n.doubleValue();
	}

	private static void tambahTransaksiFolio(Session session, ais.database.model.hotel.Folio folio,
			String jenis, String keterangan, double jumlah, String referensi, java.util.Date waktu,
			Tbmuser tbmuser) {
		ais.database.model.hotel.FolioTransaksi t = new ais.database.model.hotel.FolioTransaksi();
		t.setFolio(folio);
		t.setJenis(jenis);
		t.setKeterangan(keterangan);
		t.setJumlah(Double.valueOf(jumlah));
		t.setReferensi(referensi);
		t.setWaktu(waktu);
		if (tbmuser != null) {
			t.setOlehId(tbmuser.getUserId());
			t.setOleh(tbmuser.getUserNama());
		}
		session.save(t);
	}

	private static java.util.Date tanggalDari(JSONObject request, String field) {
		if (request == null || request.isNull(field)) return null;
		String v = request.optString(field, "").trim();
		if (v.isEmpty()) return null;
		try {
			java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd");
			f.setLenient(false);
			return f.parse(v);
		} catch (Exception e) {
			return null;
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
		if ("hotel_tamu_list".equals(action)) { tamuList(tbmuser, request, hasil); return true; }
		if ("hotel_tamu_simpan".equals(action)) { tamuSimpan(tbmuser, request, hasil); return true; }
		if ("hotel_reservasi_list".equals(action)) { reservasiList(tbmuser, request, hasil); return true; }
		if ("hotel_reservasi_buat".equals(action)) { reservasiBuat(tbmuser, request, hasil); return true; }
		if ("hotel_reservasi_batalkan".equals(action)) { reservasiBatalkan(tbmuser, request, hasil); return true; }
		if ("hotel_menginap_list".equals(action)) { menginapList(tbmuser, request, hasil); return true; }
		if ("hotel_checkin".equals(action)) { checkin(tbmuser, request, hasil); return true; }
		if ("hotel_checkout".equals(action)) { checkout(tbmuser, request, hasil); return true; }
		if ("hotel_pindah_kamar".equals(action)) { pindahKamar(tbmuser, request, hasil); return true; }
		if ("hotel_folio_get".equals(action)) { folioGet(tbmuser, request, hasil); return true; }
		if ("hotel_folio_transaksi_tambah".equals(action)) { folioTransaksiTambah(tbmuser, request, hasil); return true; }
		return false;
	}
}
