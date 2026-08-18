package ais.action.servlet.api;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.hotel.Kamar;
import ais.database.model.hotel.PropertiHotel;
import ais.database.model.hotel.ReservasiKamar;
import ais.database.model.hotel.Tamu;
import ais.database.model.hotel.TipeKamar;
import ais.database.model.koperasi.KodePembayaranOnline;

/**
 * <h3>Portal publik MitraInap -- LANGKAH 6 (situs booking tanpa token staf).</h3>
 *
 * <p>Dipanggil {@code MitraInapPublikServlet} (anonim, rate-limited pola
 * {@code PendaftaranTenantServlet}); TIDAK ada parameter {@code Tbmuser} -- seluruh method di
 * sini hanya boleh membuka data yang memang publik (katalog kamar, ketersediaan agregat) dan
 * menulis lewat jalur yang divalidasi penuh server (booking BOOKED + tagihan).</p>
 *
 * <p><b>Pembayaran (keputusan handover: "pembayaran penuh online").</b> Booking menerbitkan
 * tagihan {@link KodePembayaranOnline} (kode acak 64 hex, nominal dihitung SERVER = malam x
 * harga dasar tipe) dengan penanda {@code keterangan = "MITRAINAP-BOOKING:<kode>"} -- SENGAJA
 * tanpa kolom baru pada entity ber-audit lama (gotcha Envers). Baris "lunas" ditandai
 * {@code logPembayaran} terisi: oleh webhook kanal bank (wiring menyusul keputusan kanal
 * VA/QRIS) ATAU aksi staf {@code hotel_booking_konfirmasi_bayar} (verifikasi manual transfer).
 * Endpoint status mempromosikan BOOKED -&gt; CONFIRMED begitu tagihannya lunas.</p>
 *
 * <p><b>Ketersediaan (heuristik MVP, terdokumentasi).</b> tersedia = kamar aktif tipe tsb
 * MINUS reservasi BOOKED/CONFIRMED yang beririsan rentang MINUS kamar OCCUPIED bila rentang
 * mencakup hari ini (stay walk-in tanpa tanggal pulang terencana hanya memblok hari berjalan).
 * Hasil di-clamp &gt;= 0. Overbooking tak mungkin lolos diam-diam: check-in tetap mensyaratkan
 * kamar VACANT (state machine server).</p>
 */
public final class MitraInapPublikHelper {

	private MitraInapPublikHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static Long idDari(JSONObject request, String field) {
		if (request == null || request.isNull(field)) return null;
		String v = String.valueOf(request.opt(field)).trim();
		if (v.isEmpty()) return null;
		try {
			return Long.valueOf(Long.parseLong(v));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static java.util.Date tanggal(JSONObject request, String field) {
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

	private static java.util.Date hariIni() {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private static int selisihMalam(java.util.Date checkin, java.util.Date checkout) {
		long ms = checkout.getTime() - checkin.getTime();
		return (int) Math.round(ms / 86400000.0);
	}

	/** Hitung kamar tipe {@code tipe} yang tersedia pada [checkin, checkout) -- lihat javadoc kelas. */
	private static int hitungTersedia(Session session, TipeKamar tipe,
			java.util.Date checkin, java.util.Date checkout) {
		Number totalKamar = (Number) session.createCriteria(Kamar.class)
				.add(Restrictions.eq("tipeKamar", tipe))
				.add(Restrictions.eq("aktif", Boolean.TRUE))
				.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
		int total = totalKamar == null ? 0 : totalKamar.intValue();
		if (total == 0) return 0;
		Number bentrok = (Number) session.createCriteria(ReservasiKamar.class)
				.add(Restrictions.eq("tipeKamar", tipe))
				.add(Restrictions.in("status", new String[] {
						ReservasiKamar.STATUS_BOOKED, ReservasiKamar.STATUS_CONFIRMED }))
				.add(Restrictions.lt("tanggalCheckin", checkout))
				.add(Restrictions.gt("tanggalCheckout", checkin))
				.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
		int terpesan = bentrok == null ? 0 : bentrok.intValue();
		int terisi = 0;
		java.util.Date ini = hariIni();
		if (checkin.getTime() <= ini.getTime() && checkout.getTime() > ini.getTime()) {
			Number occupied = (Number) session.createCriteria(Kamar.class)
					.add(Restrictions.eq("tipeKamar", tipe))
					.add(Restrictions.eq("statusHunian", Kamar.HUNIAN_OCCUPIED))
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			terisi = occupied == null ? 0 : occupied.intValue();
		}
		int tersedia = total - terpesan - terisi;
		return tersedia < 0 ? 0 : tersedia;
	}

	/** {@code mode=katalog}: properti aktif + tipe kamar + harga dasar (data publik saja). */
	public static void katalog(JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(PropertiHotel.class)
					.add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("nama"));
			Long propertiId = idDari(request, "properti_id");
			if (propertiId != null) c.add(Restrictions.idEq(propertiId));
			JSONArray arr = new JSONArray();
			List daftar = c.list();
			for (int i = 0; i < daftar.size(); i++) {
				PropertiHotel p = (PropertiHotel) daftar.get(i);
				JSONObject o = new JSONObject();
				o.put("id", p.getId());
				o.put("nama", p.getNama());
				o.put("kota", p.getKota());
				o.put("alamat", p.getAlamat());
				o.put("telp", p.getTelp());
				JSONArray tipeArr = new JSONArray();
				List tipe = session.createCriteria(TipeKamar.class)
						.add(Restrictions.eq("properti", p))
						.add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("nama")).list();
				for (int j = 0; j < tipe.size(); j++) {
					TipeKamar t = (TipeKamar) tipe.get(j);
					JSONObject to = new JSONObject();
					to.put("id", t.getId());
					to.put("nama", t.getNama());
					to.put("harga_per_malam", t.getHargaDasar());
					to.put("kapasitas", t.getKapasitas());
					tipeArr.put(to);
				}
				o.put("tipe_kamar", tipeArr);
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** {@code mode=ketersediaan}: jumlah kamar tersedia + total harga rentang (dihitung server). */
	public static void ketersediaan(JSONObject request, JSONObject hasil) throws Exception {
		Long tipeId = idDari(request, "tipe_kamar_id");
		java.util.Date checkin = tanggal(request, "checkin");
		java.util.Date checkout = tanggal(request, "checkout");
		if (tipeId == null || checkin == null || checkout == null) {
			tolak(hasil, "tipe_kamar_id, checkin, dan checkout (yyyy-MM-dd) wajib diisi.");
			return;
		}
		if (!checkout.after(checkin)) {
			tolak(hasil, "checkout harus setelah checkin.");
			return;
		}
		int malam = selisihMalam(checkin, checkout);
		if (malam > 30) {
			tolak(hasil, "Maksimal 30 malam per booking online.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			TipeKamar tipe = (TipeKamar) session.get(TipeKamar.class, tipeId);
			if (tipe == null || !Boolean.TRUE.equals(tipe.getAktif())) {
				tolak(hasil, "Tipe kamar tidak ditemukan.");
				return;
			}
			int tersedia = hitungTersedia(session, tipe, checkin, checkout);
			double harga = tipe.getHargaDasar() == null ? 0 : tipe.getHargaDasar().doubleValue();
			hasil.put("status", "00");
			hasil.put("tersedia", tersedia);
			hasil.put("malam", malam);
			hasil.put("harga_per_malam", harga);
			hasil.put("total", harga * malam);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * {@code mode=booking} (POST): buat Tamu + ReservasiKamar BOOKED + tagihan. Idempoten lewat
	 * {@link RetailIdempotencyUtil} (kunci {@code idempotency_key} dari klien) -- klik ganda /
	 * retry jaringan mengembalikan respons booking yang sama, bukan booking kedua.
	 */
	public static void booking(JSONObject request, JSONObject hasil) throws Exception {
		String nama = request.optString("nama", "").trim();
		String telp = request.optString("telp", "").trim();
		String email = request.optString("email", "").trim();
		Long propertiId = idDari(request, "properti_id");
		Long tipeId = idDari(request, "tipe_kamar_id");
		java.util.Date checkin = tanggal(request, "checkin");
		java.util.Date checkout = tanggal(request, "checkout");
		String kunciIdem = request.optString("idempotency_key", "").trim();
		if (nama.isEmpty() || telp.isEmpty() || propertiId == null || tipeId == null
				|| checkin == null || checkout == null || kunciIdem.isEmpty()) {
			tolak(hasil, "nama, telp, properti_id, tipe_kamar_id, checkin, checkout, dan idempotency_key wajib diisi.");
			return;
		}
		if (!checkout.after(checkin)) {
			tolak(hasil, "checkout harus setelah checkin.");
			return;
		}
		if (checkin.before(hariIni())) {
			tolak(hasil, "checkin tidak boleh di masa lalu.");
			return;
		}
		int malam = selisihMalam(checkin, checkout);
		if (malam > 30) {
			tolak(hasil, "Maksimal 30 malam per booking online.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			JSONObject lama = RetailIdempotencyUtil.mulai(session, "mitrainap_booking_publik",
					kunciIdem, request.toString());
			if (lama != null) {
				session.getTransaction().commit();
				RetailIdempotencyUtil.salin(lama, hasil);
				hasil.put("idempotent", true);
				return;
			}
			PropertiHotel properti = (PropertiHotel) session.get(PropertiHotel.class, propertiId);
			TipeKamar tipe = (TipeKamar) session.get(TipeKamar.class, tipeId);
			if (properti == null || !Boolean.TRUE.equals(properti.getAktif())
					|| tipe == null || !Boolean.TRUE.equals(tipe.getAktif())
					|| tipe.getProperti() == null || !propertiId.equals(tipe.getProperti().getId())) {
				session.getTransaction().rollback();
				tolak(hasil, "Properti / tipe kamar tidak ditemukan.");
				return;
			}
			if (hitungTersedia(session, tipe, checkin, checkout) <= 0) {
				session.getTransaction().rollback();
				tolak(hasil, "Kamar tipe ini penuh pada tanggal tersebut.");
				return;
			}
			double harga = tipe.getHargaDasar() == null ? 0 : tipe.getHargaDasar().doubleValue();
			double total = harga * malam;

			Tamu tamu = new Tamu();
			tamu.setProperti(properti);
			tamu.setNama(nama);
			tamu.setTelp(telp);
			tamu.setEmail(email.isEmpty() ? null : email);
			tamu.setKeterangan("Booking online publik");
			tamu.setAktif(Boolean.TRUE);
			session.save(tamu);

			String kodeBooking = "BOOK-" + ais.common.security.PasswordHashService.tokenAcakHex(5).toUpperCase();
			ReservasiKamar r = new ReservasiKamar();
			r.setProperti(properti);
			r.setTamu(tamu);
			r.setTipeKamar(tipe);
			r.setKode(kodeBooking);
			r.setTanggalCheckin(checkin);
			r.setTanggalCheckout(checkout);
			r.setJumlahTamu(request.isNull("jumlah_tamu") ? null
					: Integer.valueOf(request.optInt("jumlah_tamu", 1)));
			r.setHargaPerMalam(Double.valueOf(harga));
			r.setStatus(ReservasiKamar.STATUS_BOOKED);
			r.setCatatan("Booking online publik; menunggu pembayaran.");
			session.save(r);

			// Tagihan: kode acak 64 hex (kontrak lama "kode >= 50 char"), lunas = logPembayaran terisi.
			KodePembayaranOnline tagihan = new KodePembayaranOnline();
			tagihan.setKode(ais.common.security.PasswordHashService.tokenAcakHex(32));
			tagihan.setWaktu(ais.ui.util.WaktuUtil.getDate());
			tagihan.setNominal(Double.valueOf(total));
			tagihan.setNama(nama);
			tagihan.setKeterangan("MITRAINAP-BOOKING:" + kodeBooking);
			tagihan.setAktif(Boolean.FALSE);
			session.save(tagihan);

			hasil.put("status", "00");
			hasil.put("kode_booking", kodeBooking);
			hasil.put("malam", malam);
			hasil.put("harga_per_malam", harga);
			hasil.put("total", total);
			hasil.put("kode_pembayaran", tagihan.getKode());
			hasil.put("description",
					"Booking diterima. Selesaikan pembayaran sebesar total agar booking terkonfirmasi.");
			RetailIdempotencyUtil.selesai(session, "mitrainap_booking_publik", kunciIdem,
					kodeBooking, hasil);
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
			} catch (Exception abaikan) {
				ais.common.ErrorAuditUtil.record(abaikan,
						"auto-audit(empty-catch) MitraInapPublikHelper.booking rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * {@code mode=status}: status booking by kode + telp (verifikasi kepemilikan ringan).
	 * Auto-promosi BOOKED -&gt; CONFIRMED begitu tagihan lunas (logPembayaran terisi) --
	 * aman dipanggil siapa pun: hanya membaca record miliknya sendiri dan promosi
	 * hanya terjadi dari bukti bayar yang dicatat server.
	 */
	public static void status(JSONObject request, JSONObject hasil) throws Exception {
		String kode = request.optString("kode_booking", "").trim();
		String telp = request.optString("telp", "").trim();
		if (kode.isEmpty() || telp.isEmpty()) {
			tolak(hasil, "kode_booking dan telp wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ReservasiKamar r = (ReservasiKamar) session.createCriteria(ReservasiKamar.class)
					.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
			if (r == null || r.getTamu() == null || r.getTamu().getTelp() == null
					|| !telp.equals(r.getTamu().getTelp())) {
				tolak(hasil, "Booking tidak ditemukan (periksa kode dan nomor telepon).");
				return;
			}
			KodePembayaranOnline tagihan = (KodePembayaranOnline) session
					.createCriteria(KodePembayaranOnline.class)
					.add(Restrictions.eq("keterangan", "MITRAINAP-BOOKING:" + kode))
					.setMaxResults(1).uniqueResult();
			boolean lunas = tagihan != null && tagihan.getLogPembayaran() != null
					&& tagihan.getLogPembayaran().trim().length() > 0;
			if (lunas && ReservasiKamar.STATUS_BOOKED.equals(r.getStatus())) {
				r.setStatus(ReservasiKamar.STATUS_CONFIRMED);
				session.beginTransaction();
				session.saveOrUpdate(r);
				session.getTransaction().commit();
			}
			java.text.SimpleDateFormat tgl = new java.text.SimpleDateFormat("yyyy-MM-dd");
			hasil.put("status", "00");
			hasil.put("kode_booking", r.getKode());
			hasil.put("status_booking", r.getStatus());
			hasil.put("lunas", lunas);
			hasil.put("tanggal_checkin", r.getTanggalCheckin() == null ? null : tgl.format(r.getTanggalCheckin()));
			hasil.put("tanggal_checkout", r.getTanggalCheckout() == null ? null : tgl.format(r.getTanggalCheckout()));
			hasil.put("total", tagihan == null ? null : tagihan.getNominal());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
