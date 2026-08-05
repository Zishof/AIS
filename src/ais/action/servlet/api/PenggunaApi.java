package ais.action.servlet.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Endpoint mobile untuk halaman <b>Pengguna</b> (menu server {@value #MENU_PENGGUNA})
 * — padanan {@code ais.action.maintenance.TbmuserAction}.
 *
 * <p>
 * <b>Penegakan hak akses.</b> Inilah pemakaian nyata pertama dari tabel
 * {@code role_privilage}. Setiap aksi diperiksa terhadap privilege role
 * pemanggil pada menu "Pengguna":
 * <ul>
 * <li>{@code daftarPengguna}, {@code detailPengguna}, {@code daftarRolePengguna}
 * → butuh <b>read</b></li>
 * <li>{@code simpanPengguna} tanpa {@code userId} (tambah baru) → butuh
 * <b>create</b></li>
 * <li>{@code simpanPengguna} dengan {@code userId} (ubah) → butuh
 * <b>update</b></li>
 * <li>{@code hapusPengguna} → butuh <b>delete</b></li>
 * </ul>
 * Respons {@code daftarPengguna} juga menyertakan blok {@code izin} sehingga
 * klien bisa menyembunyikan tombol yang memang tidak boleh dipakai. Itu
 * semata-mata kenyamanan tampilan — keputusan sebenarnya tetap di sini.
 *
 * <p>
 * <b>Ubah profil sendiri</b> ({@code simpanProfilSaya}) sengaja dipisah dan
 * TIDAK menuntut privilege menu Pengguna: setiap orang boleh memperbarui
 * datanya sendiri. Sebagai gantinya, aksi itu hanya menyentuh baris milik
 * pemanggil dan hanya field yang aman — {@code userRole}, {@code aktif}, dan
 * {@code userId} tidak dapat diubah lewat jalur ini, supaya tidak ada yang bisa
 * menaikkan hak aksesnya sendiri.
 *
 * <p>
 * <b>Pembaruan bersifat parsial.</b> Form mobile hanya memuat sebagian field
 * dari layar web (tanpa pegawai, penyedia, toko, satuan kerja, unit organisasi,
 * dan foto). Karena itu {@code simpanPengguna} HANYA menulis field yang benar-
 * benar dikirim; field yang tidak dikirim dibiarkan apa adanya. Menyalin pola
 * web yang menetapkan seluruh field akan menghapus data yang tidak pernah
 * ditampilkan mobile.
 *
 * <p>
 * Kompatibel Java 1.7 — tanpa lambda maupun try-with-resources.
 */
public class PenggunaApi {

	/** `menu.id` untuk menu "Pengguna". */
	public static final long MENU_PENGGUNA = 3L;

	private static final int MAKS_BARIS = 100;

	private PenggunaApi() {
	}

	// ── Daftar ──────────────────────────────────────────────────────────────

	public static JSONObject daftar(HttpServletRequest req, JSONObject request) {
		Session session = null;
		try {
			Tbmuser pemanggil = ApiUtil.currentUser(request, req);
			session = HibernateUtil.getSessionFactory().openSession();

			Izin izin = izin(session, pemanggil);
			JSONObject tolak = tolakBila(!izin.baca, pemanggil);
			if (tolak != null) {
				return tolak;
			}

			String cari = ApiHelperSupport.optString(request, "cari").trim();
			int halaman = angka(request, "halaman", 0);
			int jumlah = angka(request, "jumlah", 25);
			if (jumlah <= 0 || jumlah > MAKS_BARIS) {
				jumlah = MAKS_BARIS;
			}

			Criteria criteria = session.createCriteria(Tbmuser.class);
			if (ApiHelperSupport.hasText(cari)) {
				criteria.add(Restrictions.or(
						Restrictions.ilike("userId", cari, MatchMode.ANYWHERE),
						Restrictions.ilike("userNama", cari, MatchMode.ANYWHERE)));
			}
			criteria.addOrder(Order.asc("userId"));
			criteria.setFirstResult(halaman * jumlah);
			criteria.setMaxResults(jumlah);

			List<Tbmuser> daftar = ConstantValues.simpleList(criteria, Tbmuser.class);

			JSONArray data = new JSONArray();
			if (daftar != null) {
				for (int i = 0; i < daftar.size(); i++) {
					Tbmuser u = daftar.get(i);
					if (u == null || u.getUserId() == null) {
						continue;
					}
					data.put(ringkas(u));
				}
			}

			JSONObject hasil = new JSONObject();
			hasil.put("status", ApiHelperSupport.STATUS_OK);
			hasil.put("description", "Pengambilan data berhasil");
			hasil.put("data", data);
			hasil.put("halaman", halaman);
			hasil.put("jumlah", jumlah);
			hasil.put("izin", izin.toJson());
			return hasil;
		} catch (Exception e) {
			return ApiHelperSupport.errorResponse(Common.tampilErrorJikaAdmin(e));
		} finally {
			tutup(session);
		}
	}

	// ── Detail ──────────────────────────────────────────────────────────────

	public static JSONObject detail(HttpServletRequest req, JSONObject request) {
		Session session = null;
		try {
			Tbmuser pemanggil = ApiUtil.currentUser(request, req);
			session = HibernateUtil.getSessionFactory().openSession();

			Izin izin = izin(session, pemanggil);
			JSONObject tolak = tolakBila(!izin.baca, pemanggil);
			if (tolak != null) {
				return tolak;
			}

			String userId = ApiHelperSupport.optString(request, "userId").trim();
			if (!ApiHelperSupport.hasText(userId)) {
				return ApiHelperSupport.status("91", "User ID belum diisi");
			}

			Tbmuser u = (Tbmuser) session.get(Tbmuser.class, userId);
			if (u == null) {
				return ApiHelperSupport.status("91", "Data pengguna tidak ditemukan");
			}

			JSONObject hasil = new JSONObject();
			hasil.put("status", ApiHelperSupport.STATUS_OK);
			hasil.put("description", "Pengambilan data berhasil");
			hasil.put("data", lengkap(u));
			hasil.put("izin", izin.toJson());
			return hasil;
		} catch (Exception e) {
			return ApiHelperSupport.errorResponse(Common.tampilErrorJikaAdmin(e));
		} finally {
			tutup(session);
		}
	}

	// ── Daftar role untuk pilihan grup pengguna ─────────────────────────────

	public static JSONObject daftarRole(HttpServletRequest req, JSONObject request) {
		Session session = null;
		try {
			Tbmuser pemanggil = ApiUtil.currentUser(request, req);
			session = HibernateUtil.getSessionFactory().openSession();

			Izin izin = izin(session, pemanggil);
			JSONObject tolak = tolakBila(!izin.baca, pemanggil);
			if (tolak != null) {
				return tolak;
			}

			List<Tbmrole> roles = ConstantValues.simpleList(session.createCriteria(Tbmrole.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.addOrder(Order.asc("roleName")), Tbmrole.class);

			JSONArray data = new JSONArray();
			if (roles != null) {
				for (int i = 0; i < roles.size(); i++) {
					Tbmrole r = roles.get(i);
					if (r == null || r.getRoleId() == null) {
						continue;
					}
					JSONObject o = new JSONObject();
					o.put("roleId", r.getRoleId());
					o.put("roleName", teks(r.getRoleName()));
					data.put(o);
				}
			}

			JSONObject hasil = new JSONObject();
			hasil.put("status", ApiHelperSupport.STATUS_OK);
			hasil.put("description", "Pengambilan data berhasil");
			hasil.put("data", data);
			return hasil;
		} catch (Exception e) {
			return ApiHelperSupport.errorResponse(Common.tampilErrorJikaAdmin(e));
		} finally {
			tutup(session);
		}
	}

	// ── Simpan (tambah / ubah) ──────────────────────────────────────────────

	public static JSONObject simpan(HttpServletRequest req, JSONObject request) {
		Session session = null;
		try {
			Tbmuser pemanggil = ApiUtil.currentUser(request, req);
			session = HibernateUtil.getSessionFactory().openSession();

			Izin izin = izin(session, pemanggil);
			String userId = ApiHelperSupport.optString(request, "userId").trim();
			boolean baru = !ApiHelperSupport.hasText(userId);

			JSONObject tolak = tolakBila(baru ? !izin.tambah : !izin.ubah, pemanggil);
			if (tolak != null) {
				return tolak;
			}

			String userNama = ApiHelperSupport.optString(request, "userNama").trim();
			String password = ApiHelperSupport.optString(request, "userPassword");
			String email = ApiHelperSupport.optString(request, "email").trim();
			String roleId = ApiHelperSupport.optString(request, "roleId").trim();

			if (baru) {
				userId = ApiHelperSupport.optString(request, "userIdBaru").trim();
				if (!ApiHelperSupport.hasText(userId)) {
					return ApiHelperSupport.status("91", "ID Pengguna belum diisi");
				}
			}
			if (!ApiHelperSupport.hasText(userNama)) {
				return ApiHelperSupport.status("91", "Nama Lengkap belum diisi");
			}
			if (baru && !ApiHelperSupport.hasText(password)) {
				return ApiHelperSupport.status("91", "Password belum diisi");
			}
			if (!ApiHelperSupport.hasText(roleId)) {
				return ApiHelperSupport.status("91", "Grup Pengguna belum dipilih");
			}
			if (ApiHelperSupport.hasText(email) && !Common.isValidEmailAddress(email)) {
				return ApiHelperSupport.status("91", "Format email tidak valid");
			}

			Tbmrole role1 = role(session, roleId);
			if (role1 == null) {
				return ApiHelperSupport.status("91", "Grup Pengguna tidak dikenal");
			}
			Tbmrole role2 = role(session, ApiHelperSupport.optString(request, "roleId2"));
			Tbmrole role3 = role(session, ApiHelperSupport.optString(request, "roleId3"));
			Tbmrole role4 = role(session, ApiHelperSupport.optString(request, "roleId4"));
			Tbmrole role5 = role(session, ApiHelperSupport.optString(request, "roleId5"));

			JSONObject tolakRole = tolakBilaRoleTerlarang(pemanggil,
					new Tbmrole[] { role1, role2, role3, role4, role5 });
			if (tolakRole != null) {
				return tolakRole;
			}

			Tbmuser u;
			if (baru) {
				try {
					if (Boolean.TRUE.equals(Common.checkUsername(userId, null, null))) {
						return ApiHelperSupport.status("91", "Username sudah digunakan pengguna lain");
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/servlet/api/PenggunaApi.java:checkUsername");
				}
				if (session.get(Tbmuser.class, userId) != null) {
					return ApiHelperSupport.status("91", "Username sudah digunakan pengguna lain");
				}
				u = new Tbmuser();
				u.setUserId(userId);
				u.setUserShow(Integer.valueOf(1));
			} else {
				u = (Tbmuser) session.get(Tbmuser.class, userId);
				if (u == null) {
					return ApiHelperSupport.status("91", "Data pengguna tidak ditemukan");
				}
			}

			u.setUserNama(userNama);
			u.setUserRole(role1);
			u.setUserRole2(role2);
			u.setUserRole3(role3);
			u.setUserRole4(role4);
			u.setUserRole5(role5);

			if (ApiHelperSupport.hasText(password)) {
				u.setUserPassword(Common.desEncrypter.get().encrypt(password.trim()));
				u.setIs_encripted(Boolean.TRUE);
			}

			// Field opsional: hanya ditulis bila memang dikirim, supaya data yang
			// tidak ditampilkan form mobile tidak ikut terhapus.
			if (ada(request, "email")) {
				u.setEmail(email);
			}
			if (ada(request, "hp")) {
				u.setHp(ApiHelperSupport.optString(request, "hp").trim());
			}
			if (ada(request, "bahasa")) {
				u.setBahasa(ApiHelperSupport.optString(request, "bahasa").trim());
			}
			if (ada(request, "usernameOjs")) {
				u.setUsernameOjs(ApiHelperSupport.optString(request, "usernameOjs").trim());
			}
			if (ada(request, "aktif")) {
				u.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			}
			if (ada(request, "memilikiHakAksesTambahan")) {
				u.setMemilikiHakAksesTambahan(
						Boolean.valueOf(request.optBoolean("memilikiHakAksesTambahan", false)));
			}

			simpanEntitas(session, u, baru);

			JSONObject hasil = new JSONObject();
			hasil.put("status", ApiHelperSupport.STATUS_OK);
			hasil.put("description", baru ? "Data pengguna berhasil ditambahkan"
					: "Data pengguna berhasil disimpan");
			hasil.put("data", lengkap(u));
			return hasil;
		} catch (Exception e) {
			return ApiHelperSupport.errorResponse(Common.tampilErrorJikaAdmin(e));
		} finally {
			tutup(session);
		}
	}

	// ── Ubah profil sendiri ─────────────────────────────────────────────────

	/**
	 * Memperbarui data pengguna yang sedang login.
	 *
	 * <p>
	 * Tidak menuntut privilege menu Pengguna — memperbarui data sendiri adalah
	 * hak setiap pengguna. Sebagai gantinya cakupannya dipersempit: hanya baris
	 * milik pemanggil, dan hanya field yang tidak berkaitan dengan hak akses.
	 * {@code userRole*}, {@code aktif}, dan {@code userId} sengaja TIDAK dapat
	 * diubah dari sini.
	 */
	public static JSONObject simpanProfilSaya(HttpServletRequest req, JSONObject request) {
		Session session = null;
		try {
			Tbmuser pemanggil = ApiUtil.currentUser(request, req);
			if (pemanggil == null || pemanggil.getUserId() == null) {
				return ApiHelperSupport.status("97", "Token tidak sesuai");
			}

			session = HibernateUtil.getSessionFactory().openSession();
			Tbmuser u = (Tbmuser) session.get(Tbmuser.class, pemanggil.getUserId());
			if (u == null) {
				return ApiHelperSupport.status("91", "Data pengguna tidak ditemukan");
			}

			String userNama = ApiHelperSupport.optString(request, "userNama").trim();
			if (!ApiHelperSupport.hasText(userNama)) {
				return ApiHelperSupport.status("91", "Nama Lengkap belum diisi");
			}
			String email = ApiHelperSupport.optString(request, "email").trim();
			if (ApiHelperSupport.hasText(email) && !Common.isValidEmailAddress(email)) {
				return ApiHelperSupport.status("91", "Format email tidak valid");
			}

			u.setUserNama(userNama);
			if (ada(request, "email")) {
				u.setEmail(email);
			}
			if (ada(request, "hp")) {
				u.setHp(ApiHelperSupport.optString(request, "hp").trim());
			}
			if (ada(request, "bahasa")) {
				u.setBahasa(ApiHelperSupport.optString(request, "bahasa").trim());
			}
			String password = ApiHelperSupport.optString(request, "userPassword");
			if (ApiHelperSupport.hasText(password)) {
				u.setUserPassword(Common.desEncrypter.get().encrypt(password.trim()));
				u.setIs_encripted(Boolean.TRUE);
			}

			simpanEntitas(session, u, false);

			JSONObject hasil = new JSONObject();
			hasil.put("status", ApiHelperSupport.STATUS_OK);
			hasil.put("description", "Profil berhasil disimpan");
			hasil.put("data", lengkap(u));
			return hasil;
		} catch (Exception e) {
			return ApiHelperSupport.errorResponse(Common.tampilErrorJikaAdmin(e));
		} finally {
			tutup(session);
		}
	}

	// ── Hapus ───────────────────────────────────────────────────────────────

	public static JSONObject hapus(HttpServletRequest req, JSONObject request) {
		Session session = null;
		try {
			Tbmuser pemanggil = ApiUtil.currentUser(request, req);
			session = HibernateUtil.getSessionFactory().openSession();

			Izin izin = izin(session, pemanggil);
			JSONObject tolak = tolakBila(!izin.hapus, pemanggil);
			if (tolak != null) {
				return tolak;
			}

			String userId = ApiHelperSupport.optString(request, "userId").trim();
			if (!ApiHelperSupport.hasText(userId)) {
				return ApiHelperSupport.status("91", "User ID belum diisi");
			}
			if (userId.equalsIgnoreCase(pemanggil.getUserId())) {
				return ApiHelperSupport.status("91", "Anda tidak dapat menghapus akun Anda sendiri");
			}

			Tbmuser u = (Tbmuser) session.get(Tbmuser.class, userId);
			if (u == null) {
				return ApiHelperSupport.status("91", "Data pengguna tidak ditemukan");
			}

			session.getTransaction().begin();
			try {
				session.delete(u);
				session.getTransaction().commit();
			} catch (Exception e) {
				batalkan(session);
				// Umumnya karena akun masih dipakai data lain (foreign key).
				return ApiHelperSupport.status("92",
						"Data pengguna tidak dapat dihapus karena masih terkait data lain. "
								+ "Nonaktifkan akun ini sebagai gantinya.");
			}

			return ApiHelperSupport.status(ApiHelperSupport.STATUS_OK, "Data pengguna berhasil dihapus");
		} catch (Exception e) {
			return ApiHelperSupport.errorResponse(Common.tampilErrorJikaAdmin(e));
		} finally {
			tutup(session);
		}
	}

	// ── Hak akses ───────────────────────────────────────────────────────────

	/** Privilege role pemanggil pada menu "Pengguna". */
	static class Izin {
		boolean baca;
		boolean tambah;
		boolean ubah;
		boolean hapus;

		JSONObject toJson() {
			JSONObject o = new JSONObject();
			ApiHelperSupport.put(o, "baca", Boolean.valueOf(baca));
			ApiHelperSupport.put(o, "tambah", Boolean.valueOf(tambah));
			ApiHelperSupport.put(o, "ubah", Boolean.valueOf(ubah));
			ApiHelperSupport.put(o, "hapus", Boolean.valueOf(hapus));
			return o;
		}
	}

	/**
	 * Membaca privilege dari {@code role_privilage} untuk pasangan
	 * (role pemanggil, menu Pengguna).
	 *
	 * <p>
	 * Bila menu tidak dimiliki role, semua izin bernilai false. Bila menu
	 * dimiliki tapi tidak ada baris privilege-nya, hanya <b>baca</b> yang
	 * diberikan — aturan yang sama dengan {@code HakAksesApi}, karena di data
	 * nyata banyak menu masuk {@code job_has_menu} tanpa baris privilege
	 * pasangannya dan web tetap menampilkannya.
	 */
	private static Izin izin(Session session, Tbmuser pemanggil) {
		Izin izin = new Izin();
		if (pemanggil == null || pemanggil.getUserId() == null) {
			return izin;
		}

		Tbmrole role;
		try {
			role = pemanggil.hakAkses();
		} catch (Exception e) {
			return izin;
		}
		if (role == null || role.getRoleId() == null) {
			return izin;
		}

		Tbmrole roleDb;
		try {
			Object o = session.get(Tbmrole.class, role.getRoleId());
			roleDb = (o == null) ? role : (Tbmrole) o;
		} catch (Exception e) {
			roleDb = role;
		}

		boolean punyaMenu = false;
		Menu menuPengguna = null;
		try {
			if (roleDb.getMenus() != null) {
				for (Object o : roleDb.getMenus()) {
					if (!(o instanceof Menu)) {
						continue;
					}
					Menu m = (Menu) o;
					if (m.getId() != null && m.getId().longValue() == MENU_PENGGUNA) {
						punyaMenu = true;
						menuPengguna = m;
						break;
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/PenggunaApi.java:punyaMenu");
		}
		if (!punyaMenu) {
			return izin;
		}

		// Default saat tidak ada baris role_privilage: baca saja.
		izin.baca = true;

		try {
			List<RolePrivilage> daftar = ConstantValues.simpleList(
					session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", roleDb))
							.add(Restrictions.eq("menu", menuPengguna)),
					RolePrivilage.class);
			if (daftar != null && !daftar.isEmpty()) {
				izin.baca = false;
				for (int i = 0; i < daftar.size(); i++) {
					RolePrivilage p = daftar.get(i);
					if (p == null) {
						continue;
					}
					izin.baca = izin.baca || positif(p.getRead());
					izin.tambah = izin.tambah || positif(p.getCreate());
					izin.ubah = izin.ubah || positif(p.getUpdate());
					izin.hapus = izin.hapus || positif(p.getDelete());
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/PenggunaApi.java:izin");
		}
		return izin;
	}

	private static JSONObject tolakBila(boolean tidakBoleh, Tbmuser pemanggil) {
		if (pemanggil == null || pemanggil.getUserId() == null) {
			return ApiHelperSupport.status("97", "Token tidak sesuai");
		}
		if (tidakBoleh) {
			return ApiHelperSupport.status("93", "Anda tidak memiliki hak akses untuk tindakan ini");
		}
		return null;
	}

	/**
	 * Menyalin pembatasan {@code hanya_admin_yg_boleh_ubah_admin} dari web:
	 * bila konfigurasi itu aktif, pengguna non-admin hanya boleh memberi role
	 * Dosen/Guru/Pegawai. Tanpa ini, mobile jadi jalan pintas untuk membuat akun
	 * berhak tinggi yang di web dilarang.
	 */
	private static JSONObject tolakBilaRoleTerlarang(Tbmuser pemanggil, Tbmrole[] roles) {
		try {
			if (!Common.bolehKonfigurasi("hanya_admin_yg_boleh_ubah_admin")) {
				return null;
			}
			Tbmrole rolePemanggil = pemanggil == null ? null : pemanggil.hakAkses();
			boolean admin = rolePemanggil != null && rolePemanggil.getRoleId() != null
					&& Tbmrole.ADMINISTRATOR.equalsIgnoreCase(rolePemanggil.getRoleId());
			if (!admin && rolePemanggil != null && rolePemanggil.getRoleId() != null) {
				String daftarBoleh = Common
						.getKonfigurasi("admin_yg_boleh_ubah_dan_tambah_admin", "").getNilai();
				if (daftarBoleh != null && daftarBoleh.toLowerCase().trim()
						.contains(rolePemanggil.getRoleId().toLowerCase().trim())) {
					admin = true;
				}
			}
			if (admin) {
				return null;
			}
			for (int i = 0; i < roles.length; i++) {
				Tbmrole r = roles[i];
				if (r == null || r.getRoleId() == null) {
					continue;
				}
				String id = r.getRoleId();
				String nama = teks(r.getRoleName());
				boolean bolehDiberikan = "Dosen".equalsIgnoreCase(id) || "Guru".equalsIgnoreCase(id)
						|| "Pegawai".equalsIgnoreCase(nama);
				if (!bolehDiberikan) {
					return ApiHelperSupport.status("93",
							"Akun Anda hanya dapat membuat pengguna dengan grup Dosen, Guru, atau Pegawai");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/PenggunaApi.java:roleTerlarang");
		}
		return null;
	}

	// ── Serialisasi ─────────────────────────────────────────────────────────

	private static JSONObject ringkas(Tbmuser u) {
		JSONObject o = new JSONObject();
		ApiHelperSupport.put(o, "userId", u.getUserId());
		ApiHelperSupport.put(o, "userNama", teks(u.getUserNama()));
		ApiHelperSupport.put(o, "email", teks(u.getEmail()));
		ApiHelperSupport.put(o, "aktif", Boolean.valueOf(!Boolean.FALSE.equals(u.getAktif())));
		Tbmrole r = null;
		try {
			r = u.getUserRole();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/PenggunaApi.java:ringkasRole");
		}
		ApiHelperSupport.put(o, "roleId", r == null ? "" : teks(r.getRoleId()));
		ApiHelperSupport.put(o, "roleName", r == null ? "" : teks(r.getRoleName()));
		return o;
	}

	private static JSONObject lengkap(Tbmuser u) {
		JSONObject o = ringkas(u);
		ApiHelperSupport.put(o, "hp", teks(u.getHp()));
		ApiHelperSupport.put(o, "bahasa", teks(u.getBahasa()));
		ApiHelperSupport.put(o, "usernameOjs", teks(u.getUsernameOjs()));
		ApiHelperSupport.put(o, "memilikiHakAksesTambahan",
				Boolean.valueOf(Boolean.TRUE.equals(u.getMemilikiHakAksesTambahan())));
		ApiHelperSupport.put(o, "roleId2", roleId(u.getUserRole2()));
		ApiHelperSupport.put(o, "roleId3", roleId(u.getUserRole3()));
		ApiHelperSupport.put(o, "roleId4", roleId(u.getUserRole4()));
		ApiHelperSupport.put(o, "roleId5", roleId(u.getUserRole5()));
		// Keterkaitan entitas: ditampilkan sebagai informasi, TIDAK bisa diubah
		// dari mobile karena form-nya belum memuat pemilih entitas.
		ApiHelperSupport.put(o, "adaPegawai", Boolean.valueOf(punya(u, "pegawai")));
		ApiHelperSupport.put(o, "adaDosen", Boolean.valueOf(punya(u, "dosen")));
		ApiHelperSupport.put(o, "adaGuru", Boolean.valueOf(punya(u, "guru")));
		return o;
	}

	private static boolean punya(Tbmuser u, String jenis) {
		try {
			if ("pegawai".equals(jenis)) {
				return u.getPegawai() != null;
			}
			if ("dosen".equals(jenis)) {
				return u.getDosen() != null;
			}
			if ("guru".equals(jenis)) {
				return u.getGuru() != null;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/PenggunaApi.java:punya");
		}
		return false;
	}

	private static String roleId(Tbmrole r) {
		try {
			return (r == null || r.getRoleId() == null) ? "" : r.getRoleId();
		} catch (Exception e) {
			return "";
		}
	}

	// ── Util ────────────────────────────────────────────────────────────────

	private static void simpanEntitas(Session session, Tbmuser u, boolean baru) throws Exception {
		session.getTransaction().begin();
		try {
			if (baru) {
				session.save(u);
			} else {
				session.update(u);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			batalkan(session);
			throw e;
		}
	}

	private static void batalkan(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/PenggunaApi.java:batalkan");
		}
	}

	private static Tbmrole role(Session session, String roleId) {
		if (!ApiHelperSupport.hasText(roleId)) {
			return null;
		}
		try {
			Object o = session.get(Tbmrole.class, roleId.trim());
			return (o == null) ? null : (Tbmrole) o;
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean positif(Integer v) {
		return v != null && v.intValue() > 0;
	}

	private static boolean ada(JSONObject request, String kunci) {
		return request != null && request.has(kunci) && !request.isNull(kunci);
	}

	private static int angka(JSONObject request, String kunci, int bawaan) {
		try {
			return request.isNull(kunci) ? bawaan : request.getInt(kunci);
		} catch (Exception e) {
			return bawaan;
		}
	}

	private static String teks(String v) {
		return v == null ? "" : v.trim();
	}

	private static void tutup(Session session) {
		if (session != null) {
			try {
				if (session.isOpen()) {
					try {
						session.clear();
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenggunaApi:clear");
					}
					try {
						session.disconnect();
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenggunaApi:disconnect");
					}
					try {
						session.close();
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenggunaApi:close");
					}
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenggunaApi:tutup");
			}
		}
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenggunaApi:closeSession");
		}
	}
}
