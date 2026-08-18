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

	/**
	 * LANGKAH 4 (integrasi POS outlet): lookup ringan utk KASIR outlet -- gerbang
	 * menu "kasir" di PosApi, BUKAN kunci hotel_* (least privilege: kasir cukup
	 * bisa memilih tamu in-house utk menagih penjualan ke folio, tanpa diberi
	 * menu front-desk). Data yang dibuka minimal: nama tamu, nomor kamar,
	 * properti. {@code properti_id} opsional utk memfilter.
	 */
	public static void roomChargeLookup(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Long propertiId = idDari(request, "properti_id");
			JSONArray properti = new JSONArray();
			List daftarProperti = session.createCriteria(PropertiHotel.class)
					.add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("nama")).list();
			for (int i = 0; i < daftarProperti.size(); i++) {
				PropertiHotel p = (PropertiHotel) daftarProperti.get(i);
				JSONObject o = new JSONObject();
				o.put("id", p.getId());
				o.put("nama", p.getNama());
				properti.put(o);
			}
			org.hibernate.Criteria kriteria = session
					.createCriteria(ais.database.model.hotel.MenginapTamu.class)
					.add(Restrictions.eq("status", ais.database.model.hotel.MenginapTamu.STATUS_IN_HOUSE))
					.addOrder(Order.desc("id")).setMaxResults(300);
			if (propertiId != null) {
				PropertiHotel p = (PropertiHotel) session.createCriteria(PropertiHotel.class)
						.add(Restrictions.idEq(propertiId)).uniqueResult();
				if (p == null) {
					tolak(hasil, "Properti tidak ditemukan.");
					return;
				}
				kriteria.add(Restrictions.eq("properti", p));
			}
			JSONArray stay = new JSONArray();
			List daftarStay = kriteria.list();
			for (int i = 0; i < daftarStay.size(); i++) {
				ais.database.model.hotel.MenginapTamu m = (ais.database.model.hotel.MenginapTamu) daftarStay.get(i);
				JSONObject o = new JSONObject();
				o.put("id", m.getId());
				o.put("tamu_nama", m.getTamu() == null ? null : m.getTamu().getNama());
				o.put("kamar_nomor", m.getKamar() == null ? null : m.getKamar().getNomor());
				o.put("properti_id", m.getProperti() == null ? null : m.getProperti().getId());
				o.put("properti_nama", m.getProperti() == null ? null : m.getProperti().getNama());
				stay.put(o);
			}
			hasil.put("status", "00");
			hasil.put("properti", properti);
			hasil.put("stay", stay);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * LANGKAH 4: validasi payload "bayar" yang meminta room charge (field
	 * {@code hotel_menginap_id}) -- dipanggil KantinHelper.bayar SEBELUM tulisan
	 * pertama transaksi. Mengembalikan null bila boleh lanjut (termasuk bila
	 * payload tidak meminta room charge); selain itu pesan penolakan status 91.
	 * Menolak utuh lebih awal jauh lebih aman daripada penjualan tersimpan tapi
	 * bebannya gagal masuk folio.
	 */
	public static String periksaRoomChargePenjualan(Session session, JSONObject payload) {
		Long menginapId = idDari(payload, "hotel_menginap_id");
		if (menginapId == null) return null;
		ais.database.model.hotel.MenginapTamu stay = (ais.database.model.hotel.MenginapTamu) session
				.createCriteria(ais.database.model.hotel.MenginapTamu.class)
				.add(Restrictions.idEq(menginapId)).uniqueResult();
		if (stay == null) {
			return "Tagihan kamar ditolak: data menginap tidak ditemukan (hotel_menginap_id=" + menginapId + ").";
		}
		if (!ais.database.model.hotel.MenginapTamu.STATUS_IN_HOUSE.equals(stay.getStatus())) {
			return "Tagihan kamar ditolak: tamu sudah check-out. Pilih tamu in-house atau pakai pembayaran biasa.";
		}
		Long propertiId = idDari(payload, "hotel_properti_id");
		if (propertiId != null && stay.getProperti() != null
				&& !propertiId.equals(stay.getProperti().getId())) {
			return "Tagihan kamar ditolak: tamu bukan milik properti yang dipilih.";
		}
		ais.database.model.hotel.Folio folio = folioDariStay(session, stay);
		if (folio == null || !ais.database.model.hotel.Folio.STATUS_OPEN.equals(folio.getStatus())) {
			return "Tagihan kamar ditolak: folio tamu sudah ditutup.";
		}
		return null;
	}

	/**
	 * LANGKAH 4: posting POS_CHARGE ke folio SETELAH penjualan final tersimpan --
	 * dipanggil KantinHelper.bayar pada titik side-effect yang sama dengan
	 * KantinAssetSyncUtil (penjualan sudah commit; kegagalan di sini tidak
	 * membatalkannya, pemanggil yang memutuskan pelaporannya). Idempoten per bill
	 * lewat {@code referensi} "POSSALE-"+kodeUnik -- retry pengiriman transaksi
	 * outbox tidak menggandakan beban. Transaksi DB dikelola sendiri pada session
	 * pemanggil (pola blok draft-update bayar).
	 */
	public static void rekamRoomChargePenjualan(Session session, JSONObject payload, String kodeUnik,
			double total, Tbmuser kasir, JSONObject hasil) throws Exception {
		Long menginapId = idDari(payload, "hotel_menginap_id");
		if (menginapId == null) return;
		ais.database.model.hotel.MenginapTamu stay = (ais.database.model.hotel.MenginapTamu) session
				.createCriteria(ais.database.model.hotel.MenginapTamu.class)
				.add(Restrictions.idEq(menginapId)).uniqueResult();
		if (stay == null) {
			throw new IllegalStateException("Room charge gagal: menginap " + menginapId + " tidak ditemukan.");
		}
		ais.database.model.hotel.Folio folio = folioDariStay(session, stay);
		if (folio == null || !ais.database.model.hotel.Folio.STATUS_OPEN.equals(folio.getStatus())) {
			throw new IllegalStateException(
					"Room charge gagal: folio tamu sudah ditutup (menginap " + menginapId + ").");
		}
		String referensi = "POSSALE-" + kodeUnik;
		ais.database.model.hotel.FolioTransaksi ada = (ais.database.model.hotel.FolioTransaksi) session
				.createCriteria(ais.database.model.hotel.FolioTransaksi.class)
				.add(Restrictions.eq("folio", folio))
				.add(Restrictions.eq("referensi", referensi))
				.setMaxResults(1).uniqueResult();
		if (ada != null) {
			hasil.put("hotel_room_charge", "IDEMPOTENT");
			hasil.put("hotel_folio_id", folio.getId());
			return;
		}
		boolean kelola = !session.getTransaction().isActive();
		if (kelola) session.getTransaction().begin();
		try {
			tambahTransaksiFolio(session, folio,
					ais.database.model.hotel.FolioTransaksi.JENIS_POS_CHARGE,
					"Penjualan POS " + kodeUnik, total, referensi,
					ais.ui.util.WaktuUtil.getDate(), kasir);
			if (kelola) session.getTransaction().commit();
		} catch (RuntimeException e) {
			if (kelola && session.getTransaction().isActive()) session.getTransaction().rollback();
			throw e;
		}
		hasil.put("hotel_room_charge", "TERCATAT");
		hasil.put("hotel_folio_id", folio.getId());
	}

	// ------------------------------------------------------------------ langkah 5: tiket dapur

	/**
	 * Transisi status tiket dapur yang SAH (LANGKAH 5) -- QUEUED -&gt; PREPARING -&gt; READY -&gt;
	 * SERVED; pembatalan hanya dari QUEUED/PREPARING. Urutan mengikuti KITCHEN_TRANSITIONS
	 * versi Node, tapi DIVALIDASI DI SERVER (Node melakukan upsert tanpa validasi -- celah
	 * yang sengaja ditutup di port ini; jangan percaya klien).
	 */
	private static boolean transisiDapurBoleh(String dari, String ke) {
		if (ais.database.model.hotel.TiketDapur.STATUS_QUEUED.equals(dari)) {
			return ais.database.model.hotel.TiketDapur.STATUS_PREPARING.equals(ke)
					|| ais.database.model.hotel.TiketDapur.STATUS_CANCELLED.equals(ke);
		}
		if (ais.database.model.hotel.TiketDapur.STATUS_PREPARING.equals(dari)) {
			return ais.database.model.hotel.TiketDapur.STATUS_READY.equals(ke)
					|| ais.database.model.hotel.TiketDapur.STATUS_CANCELLED.equals(ke);
		}
		if (ais.database.model.hotel.TiketDapur.STATUS_READY.equals(dari)) {
			return ais.database.model.hotel.TiketDapur.STATUS_SERVED.equals(ke);
		}
		return false; // SERVED / CANCELLED = terminal
	}

	private static String formatWaktu(java.util.Date d) {
		if (d == null) return null;
		return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
	}

	/**
	 * LANGKAH 5: buat tiket dapur QUEUED utk satu nota POS -- dipanggil KantinHelper.bayar
	 * (payload {@code hotel_tiket_dapur=true}) pada titik side-effect fail-safe yang sama
	 * dengan room charge. Idempoten per nota lewat kolom unik {@code pembelian} -- retry
	 * pengiriman outbox tidak menggandakan tiket. Tanpa gerbang kunci hotel_: pembuatan
	 * menumpang izin kasir atas penjualan itu sendiri; kunci {@code hotel_tiket_dapur}
	 * hanya utk layar dapur (list/update).
	 */
	public static void rekamTiketDapurPenjualan(Session session, JSONObject payload,
			ais.database.model.koperasi.PembelianAnggotaKoperasi pembelian, Tbmuser kasir, JSONObject hasil)
			throws Exception {
		if (pembelian == null || pembelian.getId() == null) return;
		ais.database.model.hotel.TiketDapur ada = (ais.database.model.hotel.TiketDapur) session
				.createCriteria(ais.database.model.hotel.TiketDapur.class)
				.add(Restrictions.eq("pembelian", pembelian)).setMaxResults(1).uniqueResult();
		if (ada != null) {
			hasil.put("hotel_tiket_dapur", "IDEMPOTENT");
			hasil.put("hotel_tiket_dapur_id", ada.getId());
			return;
		}
		ais.database.model.hotel.TiketDapur t = new ais.database.model.hotel.TiketDapur();
		Long propertiId = idDari(payload, "hotel_properti_id");
		if (propertiId != null) {
			t.setProperti((PropertiHotel) session.get(PropertiHotel.class, propertiId));
		}
		t.setPembelian(pembelian);
		t.setStatus(ais.database.model.hotel.TiketDapur.STATUS_QUEUED);
		String catatan = payload.optString("hotel_tiket_dapur_catatan", "").trim();
		if (catatan.length() > 0) t.setCatatan(catatan);
		if (kasir != null) {
			t.setOlehId(kasir.getUserId());
			t.setOleh(kasir.getUserNama());
		}
		boolean kelola = !session.getTransaction().isActive();
		if (kelola) session.getTransaction().begin();
		try {
			session.save(t);
			if (kelola) session.getTransaction().commit();
		} catch (RuntimeException e) {
			if (kelola && session.getTransaction().isActive()) session.getTransaction().rollback();
			throw e;
		}
		hasil.put("hotel_tiket_dapur", "DIBUAT");
		hasil.put("hotel_tiket_dapur_id", t.getId());
	}

	/**
	 * {@code hotel_kitchen_ticket_list {properti_id?, status? = AKTIF(default)|SEMUA|<status>}}.
	 * AKTIF = belum SERVED/CANCELLED (antrean layar dapur). Menyertakan rincian item nota
	 * (nama + qty dari koperasi.pembelian) supaya dapur tahu apa yang dimasak.
	 */
	public static void kitchenTicketList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_tiket_dapur", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Tiket Dapur.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ais.database.model.hotel.TiketDapur.class)
					.addOrder(Order.asc("id")).setMaxResults(300);
			Long propertiId = idDari(request, "properti_id");
			if (propertiId != null) {
				PropertiHotel p = (PropertiHotel) session.get(PropertiHotel.class, propertiId);
				if (p == null) {
					tolak(hasil, "Properti tidak ditemukan.");
					return;
				}
				c.add(Restrictions.eq("properti", p));
			}
			String status = request.optString("status", "AKTIF").trim();
			if ("AKTIF".equalsIgnoreCase(status)) {
				c.add(Restrictions.not(Restrictions.in("status", new String[] {
						ais.database.model.hotel.TiketDapur.STATUS_SERVED,
						ais.database.model.hotel.TiketDapur.STATUS_CANCELLED })));
			} else if (!"SEMUA".equalsIgnoreCase(status) && status.length() > 0) {
				c.add(Restrictions.eq("status", status));
			}
			JSONArray arr = new JSONArray();
			List daftar = c.list();
			for (int i = 0; i < daftar.size(); i++) {
				ais.database.model.hotel.TiketDapur t = (ais.database.model.hotel.TiketDapur) daftar.get(i);
				JSONObject o = new JSONObject();
				o.put("id", t.getId());
				o.put("status", t.getStatus());
				o.put("catatan", t.getCatatan());
				o.put("mulai_pada", formatWaktu(t.getMulaiPada()));
				o.put("siap_pada", formatWaktu(t.getSiapPada()));
				o.put("disajikan_pada", formatWaktu(t.getDisajikanPada()));
				ais.database.model.koperasi.PembelianAnggotaKoperasi nota = t.getPembelian();
				if (nota != null) {
					o.put("pembelian_id", nota.getId());
					o.put("kode_nota", nota.getKode());
					o.put("waktu_nota", formatWaktu(nota.getTanggalPembayaran()));
					JSONArray item = new JSONArray();
					List rinci = session.createCriteria(ais.database.model.inventory.Pembelian.class)
							.add(Restrictions.eq("pembelianAnggotaKoperasi", nota)).addOrder(Order.asc("id")).list();
					for (int j = 0; j < rinci.size(); j++) {
						ais.database.model.inventory.Pembelian r = (ais.database.model.inventory.Pembelian) rinci.get(j);
						JSONObject ri = new JSONObject();
						ri.put("nama", r.getNama());
						ri.put("qty", r.getQty());
						item.put(ri);
					}
					o.put("item", item);
				}
				o.put("properti_id", t.getProperti() == null ? null : t.getProperti().getId());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** {@code hotel_kitchen_ticket_update {id*, status*}} -- transisi divalidasi {@link #transisiDapurBoleh}. */
	public static void kitchenTicketUpdate(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_tiket_dapur", "update")) {
			tolak(hasil, "Anda tidak memiliki hak memperbarui Tiket Dapur.");
			return;
		}
		Long id = idDari(request, "id");
		String ke = request.optString("status", "").trim().toUpperCase();
		if (id == null || ke.isEmpty()) {
			tolak(hasil, "id dan status tujuan wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.hotel.TiketDapur t = (ais.database.model.hotel.TiketDapur) session
					.get(ais.database.model.hotel.TiketDapur.class, id);
			if (t == null) {
				tolak(hasil, "Tiket dapur tidak ditemukan.");
				return;
			}
			String dari = t.getStatus();
			if (!transisiDapurBoleh(dari, ke)) {
				tolak(hasil, "Transisi status " + dari + " -> " + ke + " tidak diizinkan.");
				return;
			}
			java.util.Date kini = ais.ui.util.WaktuUtil.getDate();
			t.setStatus(ke);
			// Timestamp fase diisi SEKALI (pola COALESCE Node) -- tidak ditimpa bila sudah ada.
			if (ais.database.model.hotel.TiketDapur.STATUS_PREPARING.equals(ke) && t.getMulaiPada() == null) t.setMulaiPada(kini);
			if (ais.database.model.hotel.TiketDapur.STATUS_READY.equals(ke) && t.getSiapPada() == null) t.setSiapPada(kini);
			if (ais.database.model.hotel.TiketDapur.STATUS_SERVED.equals(ke) && t.getDisajikanPada() == null) t.setDisajikanPada(kini);
			if (ais.database.model.hotel.TiketDapur.STATUS_CANCELLED.equals(ke) && t.getDibatalkanPada() == null) t.setDibatalkanPada(kini);
			if (tbmuser != null) {
				t.setOlehId(tbmuser.getUserId());
				t.setOleh(tbmuser.getUserNama());
			}
			session.beginTransaction();
			session.saveOrUpdate(t);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", t.getId());
			hasil.put("status_tiket", t.getStatus());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ------------------------------------------------------------------ langkah 5: kontrak & laporan pemilik

	/** {@code hotel_kontrak_pemilik_list {properti_id*}}. */
	public static void kontrakPemilikList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_kontrak_pemilik", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Kontrak Pemilik.");
			return;
		}
		Long propertiId = idDari(request, "properti_id");
		if (propertiId == null) {
			tolak(hasil, "properti_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PropertiHotel p = (PropertiHotel) session.get(PropertiHotel.class, propertiId);
			if (p == null) {
				tolak(hasil, "Properti tidak ditemukan.");
				return;
			}
			JSONArray arr = new JSONArray();
			List daftar = session.createCriteria(ais.database.model.hotel.KontrakPemilik.class)
					.add(Restrictions.eq("properti", p)).addOrder(Order.desc("berlakuDari")).setMaxResults(300).list();
			java.text.SimpleDateFormat tgl = new java.text.SimpleDateFormat("yyyy-MM-dd");
			for (int i = 0; i < daftar.size(); i++) {
				ais.database.model.hotel.KontrakPemilik k = (ais.database.model.hotel.KontrakPemilik) daftar.get(i);
				JSONObject o = new JSONObject();
				o.put("id", k.getId());
				o.put("kamar_id", k.getKamar() == null ? null : k.getKamar().getId());
				o.put("kamar_nomor", k.getKamar() == null ? null : k.getKamar().getNomor());
				o.put("nama_pemilik", k.getNamaPemilik());
				o.put("referensi_pemilik", k.getReferensiPemilik());
				o.put("persen_komisi", k.getPersenKomisi());
				o.put("berlaku_dari", k.getBerlakuDari() == null ? null : tgl.format(k.getBerlakuDari()));
				o.put("berlaku_sampai", k.getBerlakuSampai() == null ? null : tgl.format(k.getBerlakuSampai()));
				o.put("aktif", k.getAktif());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * {@code hotel_kontrak_pemilik_simpan {id?, properti_id*, kamar_id*, nama_pemilik*,
	 * referensi_pemilik?, persen_komisi* (0..100), berlaku_dari* (yyyy-MM-dd), berlaku_sampai?, aktif?}}.
	 */
	public static void kontrakPemilikSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = idDari(request, "id");
		if (!boleh(tbmuser, "hotel_kontrak_pemilik", id == null ? "create" : "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengelola Kontrak Pemilik.");
			return;
		}
		Long propertiId = idDari(request, "properti_id");
		Long kamarId = idDari(request, "kamar_id");
		String namaPemilik = request.optString("nama_pemilik", "").trim();
		java.util.Date berlakuDari = tanggalDari(request, "berlaku_dari");
		double persen = request.optDouble("persen_komisi", -1);
		if (propertiId == null || kamarId == null || namaPemilik.isEmpty() || berlakuDari == null) {
			tolak(hasil, "properti_id, kamar_id, nama_pemilik, dan berlaku_dari wajib diisi.");
			return;
		}
		if (persen < 0 || persen > 100) {
			tolak(hasil, "persen_komisi wajib 0..100.");
			return;
		}
		java.util.Date berlakuSampai = tanggalDari(request, "berlaku_sampai");
		if (berlakuSampai != null && berlakuSampai.before(berlakuDari)) {
			tolak(hasil, "berlaku_sampai tidak boleh sebelum berlaku_dari.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PropertiHotel p = (PropertiHotel) session.get(PropertiHotel.class, propertiId);
			Kamar kamar = (Kamar) session.get(Kamar.class, kamarId);
			if (p == null || kamar == null) {
				tolak(hasil, "Properti / kamar tidak ditemukan.");
				return;
			}
			if (kamar.getProperti() == null || !p.getId().equals(kamar.getProperti().getId())) {
				tolak(hasil, "Kamar bukan milik properti yang dipilih.");
				return;
			}
			ais.database.model.hotel.KontrakPemilik k;
			if (id == null) {
				k = new ais.database.model.hotel.KontrakPemilik();
			} else {
				k = (ais.database.model.hotel.KontrakPemilik) session
						.get(ais.database.model.hotel.KontrakPemilik.class, id);
				if (k == null) {
					tolak(hasil, "Kontrak tidak ditemukan.");
					return;
				}
			}
			k.setProperti(p);
			k.setKamar(kamar);
			k.setNamaPemilik(namaPemilik);
			k.setReferensiPemilik(request.optString("referensi_pemilik", "").trim());
			k.setPersenKomisi(Double.valueOf(persen));
			k.setBerlakuDari(berlakuDari);
			k.setBerlakuSampai(berlakuSampai);
			k.setAktif(Boolean.valueOf(!request.has("aktif") || request.optBoolean("aktif", true)));
			if (tbmuser != null) {
				k.setOlehId(tbmuser.getUserId());
				k.setOleh(tbmuser.getUserNama());
			}
			session.beginTransaction();
			session.saveOrUpdate(k);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", k.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * {@code hotel_laporan_pemilik_generate {kontrak_id*, periode_mulai*, periode_selesai*
	 * (yyyy-MM-dd, inklusif), biaya?}} -- SELURUH angka dihitung server dari baris ROOM_CHARGE
	 * {@link ais.database.model.hotel.FolioTransaksi} stay kamar kontrak dalam periode (beda
	 * disengaja dari Node yang menerima angka klien). Snapshot JSON + SHA-256 disimpan sebagai
	 * bukti dokumen. Idempoten per (kontrak, periode): generate ulang periode yang sama
	 * mengembalikan baris yang sudah ada ({@code idempotent:true}) -- pakai periode berbeda
	 * atau hapus manual bila memang perlu terbit ulang.
	 */
	public static void laporanPemilikGenerate(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_laporan_pemilik", "create")) {
			tolak(hasil, "Anda tidak memiliki hak menerbitkan Laporan Pemilik.");
			return;
		}
		Long kontrakId = idDari(request, "kontrak_id");
		java.util.Date mulai = tanggalDari(request, "periode_mulai");
		java.util.Date selesai = tanggalDari(request, "periode_selesai");
		if (kontrakId == null || mulai == null || selesai == null) {
			tolak(hasil, "kontrak_id, periode_mulai, dan periode_selesai wajib diisi (yyyy-MM-dd).");
			return;
		}
		if (selesai.before(mulai)) {
			tolak(hasil, "periode_selesai tidak boleh sebelum periode_mulai.");
			return;
		}
		double biaya = Math.max(0, request.optDouble("biaya", 0));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.hotel.KontrakPemilik k = (ais.database.model.hotel.KontrakPemilik) session
					.get(ais.database.model.hotel.KontrakPemilik.class, kontrakId);
			if (k == null) {
				tolak(hasil, "Kontrak tidak ditemukan.");
				return;
			}
			ais.database.model.hotel.LaporanPemilik lama = (ais.database.model.hotel.LaporanPemilik) session
					.createCriteria(ais.database.model.hotel.LaporanPemilik.class)
					.add(Restrictions.eq("kontrak", k))
					.add(Restrictions.eq("periodeMulai", mulai))
					.add(Restrictions.eq("periodeSelesai", selesai))
					.setMaxResults(1).uniqueResult();
			if (lama != null) {
				hasil.put("status", "00");
				hasil.put("idempotent", true);
				hasil.put("id", lama.getId());
				hasil.put("dokumen_hash", lama.getDokumenHash());
				return;
			}
			// Batas akhir eksklusif = selesai + 1 hari (periode inklusif harian).
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTime(selesai);
			cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
			java.util.Date batasEksklusif = cal.getTime();
			List trx = session.createCriteria(ais.database.model.hotel.FolioTransaksi.class)
					.createAlias("folio", "f")
					.createAlias("f.menginap", "m")
					.add(Restrictions.eq("m.kamar", k.getKamar()))
					.add(Restrictions.eq("jenis", ais.database.model.hotel.FolioTransaksi.JENIS_ROOM_CHARGE))
					.add(Restrictions.ge("waktu", mulai))
					.add(Restrictions.lt("waktu", batasEksklusif))
					.addOrder(Order.asc("waktu")).list();
			double kotor = 0;
			JSONArray rincian = new JSONArray();
			for (int i = 0; i < trx.size(); i++) {
				ais.database.model.hotel.FolioTransaksi t = (ais.database.model.hotel.FolioTransaksi) trx.get(i);
				kotor += t.getJumlah() == null ? 0 : t.getJumlah().doubleValue();
				JSONObject r = new JSONObject();
				r.put("transaksi_id", t.getId());
				r.put("waktu", formatWaktu(t.getWaktu()));
				r.put("jumlah", t.getJumlah());
				r.put("referensi", t.getReferensi());
				r.put("folio_id", t.getFolio() == null ? null : t.getFolio().getId());
				rincian.put(r);
			}
			double persen = k.getPersenKomisi() == null ? 0 : k.getPersenKomisi().doubleValue();
			double komisi = kotor * persen / 100.0;
			double bersih = kotor - komisi - biaya;
			java.text.SimpleDateFormat tgl = new java.text.SimpleDateFormat("yyyy-MM-dd");
			JSONObject snapshot = new JSONObject();
			snapshot.put("kontrak_id", k.getId());
			snapshot.put("kamar_id", k.getKamar() == null ? null : k.getKamar().getId());
			snapshot.put("kamar_nomor", k.getKamar() == null ? null : k.getKamar().getNomor());
			snapshot.put("nama_pemilik", k.getNamaPemilik());
			snapshot.put("persen_komisi", persen);
			snapshot.put("periode_mulai", tgl.format(mulai));
			snapshot.put("periode_selesai", tgl.format(selesai));
			snapshot.put("pendapatan_kotor", kotor);
			snapshot.put("komisi", komisi);
			snapshot.put("biaya", biaya);
			snapshot.put("bersih_dibayarkan", bersih);
			snapshot.put("transaksi", rincian);
			snapshot.put("dihitung_pada", formatWaktu(ais.ui.util.WaktuUtil.getDate()));
			String snap = snapshot.toString();
			ais.database.model.hotel.LaporanPemilik lp = new ais.database.model.hotel.LaporanPemilik();
			lp.setKontrak(k);
			lp.setPeriodeMulai(mulai);
			lp.setPeriodeSelesai(selesai);
			lp.setPendapatanKotor(Double.valueOf(kotor));
			lp.setKomisi(Double.valueOf(komisi));
			lp.setBiaya(Double.valueOf(biaya));
			lp.setBersihDibayarkan(Double.valueOf(bersih));
			lp.setSnapshot(snap);
			lp.setDokumenHash(sha256Hex(snap));
			if (tbmuser != null) {
				lp.setOlehId(tbmuser.getUserId());
				lp.setOleh(tbmuser.getUserNama());
			}
			session.beginTransaction();
			session.save(lp);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", lp.getId());
			hasil.put("pendapatan_kotor", kotor);
			hasil.put("komisi", komisi);
			hasil.put("biaya", biaya);
			hasil.put("bersih_dibayarkan", bersih);
			hasil.put("jumlah_transaksi", rincian.length());
			hasil.put("dokumen_hash", lp.getDokumenHash());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** {@code hotel_laporan_pemilik_list {kontrak_id | properti_id}}. */
	public static void laporanPemilikList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!boleh(tbmuser, "hotel_laporan_pemilik", null)) {
			tolak(hasil, "Anda tidak memiliki akses menu Laporan Pemilik.");
			return;
		}
		Long kontrakId = idDari(request, "kontrak_id");
		Long propertiId = idDari(request, "properti_id");
		if (kontrakId == null && propertiId == null) {
			tolak(hasil, "kontrak_id atau properti_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ais.database.model.hotel.LaporanPemilik.class)
					.createAlias("kontrak", "k")
					.addOrder(Order.desc("id")).setMaxResults(300);
			if (kontrakId != null) {
				c.add(Restrictions.eq("k.id", kontrakId));
			} else {
				PropertiHotel p = (PropertiHotel) session.get(PropertiHotel.class, propertiId);
				if (p == null) {
					tolak(hasil, "Properti tidak ditemukan.");
					return;
				}
				c.add(Restrictions.eq("k.properti", p));
			}
			java.text.SimpleDateFormat tgl = new java.text.SimpleDateFormat("yyyy-MM-dd");
			JSONArray arr = new JSONArray();
			List daftar = c.list();
			for (int i = 0; i < daftar.size(); i++) {
				ais.database.model.hotel.LaporanPemilik lp = (ais.database.model.hotel.LaporanPemilik) daftar.get(i);
				JSONObject o = new JSONObject();
				o.put("id", lp.getId());
				o.put("kontrak_id", lp.getKontrak() == null ? null : lp.getKontrak().getId());
				o.put("nama_pemilik", lp.getKontrak() == null ? null : lp.getKontrak().getNamaPemilik());
				o.put("kamar_nomor", lp.getKontrak() == null || lp.getKontrak().getKamar() == null
						? null : lp.getKontrak().getKamar().getNomor());
				o.put("periode_mulai", lp.getPeriodeMulai() == null ? null : tgl.format(lp.getPeriodeMulai()));
				o.put("periode_selesai", lp.getPeriodeSelesai() == null ? null : tgl.format(lp.getPeriodeSelesai()));
				o.put("pendapatan_kotor", lp.getPendapatanKotor());
				o.put("komisi", lp.getKomisi());
				o.put("biaya", lp.getBiaya());
				o.put("bersih_dibayarkan", lp.getBersihDibayarkan());
				o.put("dokumen_hash", lp.getDokumenHash());
				o.put("snapshot", lp.getSnapshot());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** SHA-256 hex 64 char -- bukti snapshot laporan tidak berubah (pola fingerprint RetailIdempotencyUtil). */
	private static String sha256Hex(String nilai) throws Exception {
		byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(nilai.getBytes("UTF-8"));
		StringBuilder sb = new StringBuilder(64);
		for (int i = 0; i < digest.length; i++) {
			sb.append(String.format("%02x", Integer.valueOf(digest[i] & 0xff)));
		}
		return sb.toString();
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
		if ("hotel_room_charge_lookup".equals(action)) { roomChargeLookup(tbmuser, request, hasil); return true; }
		if ("hotel_kitchen_ticket_list".equals(action)) { kitchenTicketList(tbmuser, request, hasil); return true; }
		if ("hotel_kitchen_ticket_update".equals(action)) { kitchenTicketUpdate(tbmuser, request, hasil); return true; }
		if ("hotel_kontrak_pemilik_list".equals(action)) { kontrakPemilikList(tbmuser, request, hasil); return true; }
		if ("hotel_kontrak_pemilik_simpan".equals(action)) { kontrakPemilikSimpan(tbmuser, request, hasil); return true; }
		if ("hotel_laporan_pemilik_generate".equals(action)) { laporanPemilikGenerate(tbmuser, request, hasil); return true; }
		if ("hotel_laporan_pemilik_list".equals(action)) { laporanPemilikList(tbmuser, request, hasil); return true; }
		return false;
	}
}
