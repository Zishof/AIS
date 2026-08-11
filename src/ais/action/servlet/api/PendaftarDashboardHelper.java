package ais.action.servlet.api;

import java.security.SecureRandom;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.AkunManajemen;
import ais.database.model.Brand;
import ais.database.model.Investor;
import ais.database.model.Pendaftar;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Toko;
import ais.ui.util.WaktuUtil;

/**
 * <b>PendaftarDashboardHelper</b> -- aksi-aksi dashboard self-service Pendaftar ebisnis.id
 * SETELAH login (kelola Brand, Toko/Gerai, Mesin POS per toko, Investor, dan Akun Manajemen).
 * Dipanggil dari {@code EbisnisPublicServlet}, yang WAJIB meng-resolve {@link Pendaftar} dari
 * {@code HttpSession} (bukan dari parameter request client) sebelum memanggil method manapun
 * di sini -- desain IDOR-safe yang sama dgn {@code KantinHelper.resolveTokoId}: setiap method
 * di bawah menerima {@code Pendaftar pendaftar} yang sudah dipercaya, lalu SETIAP query
 * Brand/Toko/Investor/AkunManajemen selalu difilter ulang {@code pendaftar = :pendaftarId} di
 * sisi server -- klien tidak pernah bisa menyentuh data milik Pendaftar lain sekadar dgn
 * menebak/mengubah id di payload.
 *
 * <h3>Kredensial Mesin POS / Investor / Manajemen</h3>
 * <p>Ketiganya memakai skema plaintext {@code userid}/{@code pass} yang SAMA dgn
 * {@code inventory.Pedagang} yang sudah dipakai seluruh ekosistem POS (bukan hash PBKDF2 spt
 * {@link Pendaftar} sendiri -- itu utk pendaftaran mandiri publik, ini akun yang DIBUAT oleh
 * pemilik bisnis utk stafnya/mesinnya, tingkat kepercayaan sama dgn akun kasir biasa). Ini
 * jugalah yang membuat "login via QR-Code" sederhana: QR cukup meng-encode
 * {@code userid:password}, discan lalu diisikan otomatis ke form login yang sudah ada --
 * tidak perlu mekanisme token/sesi baru.</p>
 */
public class PendaftarDashboardHelper {

	private static final String KARAKTER_PASSWORD = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";

	// ==================================================================
	// RINGKASAN
	// ==================================================================

	public static void ringkasan(Pendaftar pendaftar, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			long jumlahBrand = hitung(session, Brand.class, pendaftar.getId());
			long jumlahToko = hitung(session, Toko.class, pendaftar.getId());
			long jumlahInvestor = hitung(session, Investor.class, pendaftar.getId());
			long jumlahManajemen = hitung(session, AkunManajemen.class, pendaftar.getId());

			List<?> tokoIds = session.createCriteria(Toko.class)
					.add(Restrictions.eq("pendaftar.id", pendaftar.getId()))
					.setProjection(org.hibernate.criterion.Projections.property("id")).list();
			long jumlahMesinPos = 0;
			if (!tokoIds.isEmpty()) {
				jumlahMesinPos = ((Number) session.createCriteria(Pedagang.class)
						.add(Restrictions.in("toko.id", tokoIds))
						.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult()).longValue();
			}

			hasil.put("status", "00");
			hasil.put("namaBisnis", pendaftar.getNama());
			hasil.put("jumlahBrand", jumlahBrand);
			hasil.put("jumlahToko", jumlahToko);
			hasil.put("jumlahMesinPos", jumlahMesinPos);
			hasil.put("jumlahInvestor", jumlahInvestor);
			hasil.put("jumlahManajemen", jumlahManajemen);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static long hitung(Session session, Class<?> kelas, Long pendaftarId) {
		Number n = (Number) session.createCriteria(kelas)
				.add(Restrictions.eq("pendaftar.id", pendaftarId))
				.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
		return n == null ? 0 : n.longValue();
	}

	// ==================================================================
	// BRAND
	// ==================================================================

	public static void brandList(Pendaftar pendaftar, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List<?> daftar = session.createCriteria(Brand.class)
					.add(Restrictions.eq("pendaftar.id", pendaftar.getId()))
					.addOrder(Order.asc("nama")).list();
			JSONArray arr = new JSONArray();
			for (Object o : daftar) {
				Brand b = (Brand) o;
				JSONObject j = new JSONObject();
				j.put("id", b.getId());
				j.put("nama", b.getNama());
				j.put("aktif", Boolean.TRUE.equals(b.getAktif()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void brandTambah(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama brand/merek wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Pendaftar pRef = (Pendaftar) session.load(Pendaftar.class, pendaftar.getId());
			Brand b = new Brand();
			b.setNama(nama);
			b.setPendaftar(pRef);
			b.setAktif(true);
			b.setDibuatPada(WaktuUtil.getDate());
			session.beginTransaction();
			session.save(b);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", b.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================
	// TOKO / GERAI / CAFE
	// ==================================================================

	public static void tokoList(Pendaftar pendaftar, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List<?> daftar = session.createCriteria(Toko.class)
					.add(Restrictions.eq("pendaftar.id", pendaftar.getId()))
					.addOrder(Order.asc("nama")).list();
			JSONArray arr = new JSONArray();
			for (Object o : daftar) {
				Toko t = (Toko) o;
				JSONObject j = new JSONObject();
				j.put("id", t.getId());
				j.put("nama", t.getNama());
				j.put("brandNama", t.getBrand() == null ? null : t.getBrand().getNama());
				j.put("kota", t.getKota());
				j.put("aktif", Boolean.TRUE.equals(t.getAktif()));
				Number jumlahMesin = (Number) session.createCriteria(Pedagang.class)
						.add(Restrictions.eq("toko.id", t.getId()))
						.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
				j.put("jumlahMesinPos", jumlahMesin == null ? 0 : jumlahMesin.longValue());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void tokoTambah(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama toko/gerai/cafe wajib diisi.");
			return;
		}
		Long brandId = request.isNull("brandId") || request.optString("brandId", "").trim().isEmpty() ? null
				: Long.valueOf(request.get("brandId") + "");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			if (brandId != null) {
				Brand brandCek = (Brand) session.get(Brand.class, brandId);
				if (brandCek == null || brandCek.getPendaftar() == null
						|| !pendaftar.getId().equals(brandCek.getPendaftar().getId())) {
					hasil.put("status", "91");
					hasil.put("description", "Brand tidak ditemukan atau bukan milik Anda.");
					return;
				}
			}
			Pendaftar pRef = (Pendaftar) session.load(Pendaftar.class, pendaftar.getId());
			Toko t = new Toko();
			t.setNama(nama);
			t.setPendaftar(pRef);
			if (brandId != null) {
				t.setBrand((Brand) session.load(Brand.class, brandId));
			}
			t.setAlamat(request.optString("alamat", ""));
			t.setKota(request.optString("kota", ""));
			t.setTelp(request.optString("telp", ""));
			t.setAktif(true);
			session.beginTransaction();
			session.save(t);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", t.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================
	// MESIN POS (per toko) -- reuse inventory.Pedagang, akun standalone
	// ==================================================================

	public static void mesinPosList(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = request.isNull("tokoId") ? null : Long.valueOf(request.get("tokoId") + "");
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (!milikPendaftar(toko, pendaftar)) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan atau bukan milik Anda.");
				return;
			}
			List<?> daftar = session.createCriteria(Pedagang.class)
					.add(Restrictions.eq("toko.id", tokoId))
					.addOrder(Order.asc("nama")).list();
			JSONArray arr = new JSONArray();
			for (Object o : daftar) {
				Pedagang p = (Pedagang) o;
				JSONObject j = new JSONObject();
				j.put("id", p.getId());
				j.put("nama", p.getNama());
				j.put("userid", p.getUserid());
				j.put("aktif", Boolean.TRUE.equals(p.getAktif()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Tambah 1 "Mesin POS" baru = 1 baris {@code Pedagang} standalone baru per mesin (userid/
	 * password auto-generate, ditampilkan SEKALI di respons ini -- tidak disimpan ulang di
	 * tempat lain dalam bentuk plaintext selain kolom {@code pass} milik baris itu sendiri,
	 * konsisten dgn cara kolom itu sudah dipakai di seluruh sistem POS).
	 */
	public static void mesinPosTambah(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = request.isNull("tokoId") ? null : Long.valueOf(request.get("tokoId") + "");
		String nama = request.optString("nama", "").trim();
		if (tokoId == null || nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Toko dan nama mesin POS wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (!milikPendaftar(toko, pendaftar)) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan atau bukan milik Anda.");
				return;
			}
			String userid = buatUseridUnik(session, toko.getKode() != null ? toko.getKode() : toko.getNama());
			String password = buatPasswordAcak(8);

			Pedagang p = new Pedagang();
			p.setUserid(userid);
			p.setPass(password);
			p.setNama(nama);
			p.setToko(toko);
			p.setAktif(true);
			p.setSupervisor(false);
			session.beginTransaction();
			session.save(p);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", p.getId());
			hasil.put("userid", userid);
			hasil.put("password", password);
			hasil.put("qrData", userid + ":" + password);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static boolean milikPendaftar(Toko toko, Pendaftar pendaftar) {
		return toko != null && toko.getPendaftar() != null
				&& pendaftar.getId().equals(toko.getPendaftar().getId());
	}

	// ==================================================================
	// INVESTOR
	// ==================================================================

	public static void investorList(Pendaftar pendaftar, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List<?> daftar = session.createCriteria(Investor.class)
					.add(Restrictions.eq("pendaftar.id", pendaftar.getId()))
					.addOrder(Order.asc("nama")).list();
			JSONArray arr = new JSONArray();
			for (Object o : daftar) {
				Investor inv = (Investor) o;
				JSONObject j = new JSONObject();
				j.put("id", inv.getId());
				j.put("nama", inv.getNama());
				j.put("email", inv.getEmail());
				j.put("userid", inv.getUserid());
				j.put("kepemilikanJson", inv.getKepemilikanJson());
				j.put("aktif", Boolean.TRUE.equals(inv.getAktif()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void investorTambah(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama investor wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Pendaftar pRef = (Pendaftar) session.load(Pendaftar.class, pendaftar.getId());
			String userid = buatUseridUnik(session, "inv-" + nama);
			String password = buatPasswordAcak(8);

			Investor inv = new Investor();
			inv.setNama(nama);
			inv.setEmail(request.optString("email", ""));
			inv.setTelp(request.optString("telp", ""));
			inv.setPendaftar(pRef);
			inv.setUserid(userid);
			inv.setPass(password);
			inv.setKepemilikanJson(request.optString("kepemilikanJson", "[]"));
			inv.setAktif(true);
			inv.setDibuatPada(WaktuUtil.getDate());
			session.beginTransaction();
			session.save(inv);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", inv.getId());
			hasil.put("userid", userid);
			hasil.put("password", password);
			hasil.put("qrData", userid + ":" + password);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================
	// AKUN MANAJEMEN
	// ==================================================================

	public static void manajemenList(Pendaftar pendaftar, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List<?> daftar = session.createCriteria(AkunManajemen.class)
					.add(Restrictions.eq("pendaftar.id", pendaftar.getId()))
					.addOrder(Order.asc("nama")).list();
			JSONArray arr = new JSONArray();
			for (Object o : daftar) {
				AkunManajemen a = (AkunManajemen) o;
				JSONObject j = new JSONObject();
				j.put("id", a.getId());
				j.put("nama", a.getNama());
				j.put("jabatan", a.getJabatan());
				j.put("userid", a.getUserid());
				j.put("aktif", Boolean.TRUE.equals(a.getAktif()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void manajemenTambah(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		String nama = request.optString("nama", "").trim();
		String jabatan = request.optString("jabatan", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama akun manajemen wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Pendaftar pRef = (Pendaftar) session.load(Pendaftar.class, pendaftar.getId());
			String userid = buatUseridUnik(session, "mgr-" + nama);
			String password = buatPasswordAcak(8);

			AkunManajemen a = new AkunManajemen();
			a.setNama(nama);
			a.setJabatan(jabatan);
			a.setPendaftar(pRef);
			a.setUserid(userid);
			a.setPass(password);
			a.setAktif(true);
			a.setDibuatPada(WaktuUtil.getDate());
			session.beginTransaction();
			session.save(a);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", a.getId());
			hasil.put("userid", userid);
			hasil.put("password", password);
			hasil.put("qrData", userid + ":" + password);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================
	// UBAH / NONAKTIF (program pendaftaran tenant §11.2 -- IDOR-safe pola sama:
	// resolve entity by id LALU cocokkan pendaftar pemilik; bukan percaya klien)
	// ==================================================================

	public static void brandUbah(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		Long id = request.isNull("id") ? null : Long.valueOf(request.get("id") + "");
		String nama = request.optString("nama", "").trim();
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "Brand wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Brand b = (Brand) session.get(Brand.class, id);
			if (b == null || b.getPendaftar() == null
					|| !pendaftar.getId().equals(b.getPendaftar().getId())) {
				hasil.put("status", "91");
				hasil.put("description", "Brand tidak ditemukan atau bukan milik Anda.");
				return;
			}
			if (!nama.isEmpty()) {
				b.setNama(nama);
			}
			if (request.has("aktif")) {
				b.setAktif(Boolean.valueOf("true".equals(request.optString("aktif"))));
			}
			session.beginTransaction();
			session.saveOrUpdate(b);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void tokoUbah(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		Long id = request.isNull("id") ? null : Long.valueOf(request.get("id") + "");
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko t = (Toko) session.get(Toko.class, id);
			if (!milikPendaftar(t, pendaftar)) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan atau bukan milik Anda.");
				return;
			}
			String nama = request.optString("nama", "").trim();
			if (!nama.isEmpty()) {
				t.setNama(nama);
			}
			if (request.has("alamat")) {
				t.setAlamat(request.optString("alamat", ""));
			}
			if (request.has("kota")) {
				t.setKota(request.optString("kota", ""));
			}
			if (request.has("telp")) {
				t.setTelp(request.optString("telp", ""));
			}
			if (request.has("aktif")) {
				t.setAktif(Boolean.valueOf("true".equals(request.optString("aktif"))));
			}
			session.beginTransaction();
			session.saveOrUpdate(t);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Nonaktif/aktifkan kembali mesin POS (baris Pedagang) -- kepemilikan via toko.pendaftar. */
	public static void mesinPosNonaktif(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		Long id = request.isNull("id") ? null : Long.valueOf(request.get("id") + "");
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "Mesin POS wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Pedagang p = (Pedagang) session.get(Pedagang.class, id);
			if (p == null || !milikPendaftar(p.getToko(), pendaftar)) {
				hasil.put("status", "91");
				hasil.put("description", "Mesin POS tidak ditemukan atau bukan milik Anda.");
				return;
			}
			p.setAktif(Boolean.valueOf("true".equals(request.optString("aktif", "false"))));
			session.beginTransaction();
			session.saveOrUpdate(p);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void investorNonaktif(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		Long id = request.isNull("id") ? null : Long.valueOf(request.get("id") + "");
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "Investor wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Investor inv = (Investor) session.get(Investor.class, id);
			if (inv == null || inv.getPendaftar() == null
					|| !pendaftar.getId().equals(inv.getPendaftar().getId())) {
				hasil.put("status", "91");
				hasil.put("description", "Investor tidak ditemukan atau bukan milik Anda.");
				return;
			}
			inv.setAktif(Boolean.valueOf("true".equals(request.optString("aktif", "false"))));
			session.beginTransaction();
			session.saveOrUpdate(inv);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void manajemenNonaktif(Pendaftar pendaftar, JSONObject request, JSONObject hasil) throws Exception {
		Long id = request.isNull("id") ? null : Long.valueOf(request.get("id") + "");
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "Akun manajemen wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AkunManajemen a = (AkunManajemen) session.get(AkunManajemen.class, id);
			if (a == null || a.getPendaftar() == null
					|| !pendaftar.getId().equals(a.getPendaftar().getId())) {
				hasil.put("status", "91");
				hasil.put("description", "Akun manajemen tidak ditemukan atau bukan milik Anda.");
				return;
			}
			a.setAktif(Boolean.valueOf("true".equals(request.optString("aktif", "false"))));
			session.beginTransaction();
			session.saveOrUpdate(a);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================
	// UTIL BERSAMA
	// ==================================================================

	/**
	 * Userid unik lintas {@code Pedagang}/{@code Investor}/{@code AkunManajemen} -- ketiganya
	 * login lewat tabel berbeda TAPI userid harus tetap gampang diingat & tidak bentrok scr
	 * kasat mata, jadi dicek ke tabel {@code koperasi.pedagang} SAJA (tabel dgn constraint unique
	 * yang sudah ada & paling ramai) plus tabel entitas yang sedang diisi sendiri lewat Hibernate
	 * Criteria -- cukup utk mencegah userid tabrakan dalam praktik tanpa perlu tabel penomoran
	 * terpusat baru.
	 */
	private static String buatUseridUnik(Session session, String basis) {
		String dasar = basis == null ? "akun" : basis.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
		if (dasar.isEmpty()) {
			dasar = "akun";
		}
		if (dasar.length() > 30) {
			dasar = dasar.substring(0, 30);
		}
		SecureRandom acak = new SecureRandom();
		String kandidat;
		do {
			kandidat = dasar + "-" + (1000 + acak.nextInt(9000));
		} while (useridDipakai(session, kandidat));
		return kandidat;
	}

	private static boolean useridDipakai(Session session, String userid) {
		Number diPedagang = (Number) session.createSQLQuery("SELECT COUNT(*) FROM koperasi.pedagang WHERE userid = :u")
				.setParameter("u", userid).uniqueResult();
		if (diPedagang != null && diPedagang.longValue() > 0) {
			return true;
		}
		Number diInvestor = (Number) session.createCriteria(Investor.class)
				.add(Restrictions.eq("userid", userid))
				.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
		if (diInvestor != null && diInvestor.longValue() > 0) {
			return true;
		}
		Number diManajemen = (Number) session.createCriteria(AkunManajemen.class)
				.add(Restrictions.eq("userid", userid))
				.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
		return diManajemen != null && diManajemen.longValue() > 0;
	}

	private static String buatPasswordAcak(int panjang) {
		SecureRandom acak = new SecureRandom();
		StringBuilder sb = new StringBuilder(panjang);
		for (int i = 0; i < panjang; i++) {
			sb.append(KARAKTER_PASSWORD.charAt(acak.nextInt(KARAKTER_PASSWORD.length())));
		}
		return sb.toString();
	}
}
