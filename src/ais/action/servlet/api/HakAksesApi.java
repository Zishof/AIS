package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
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
 * Endpoint <b>{@code hakAksesSaya}</b> — mengirimkan hak akses pengguna dalam
 * bentuk terstruktur untuk dipakai aplikasi mobile.
 *
 * <p>
 * Latar belakang: sampai sebelum ini, satu-satunya informasi hak akses yang
 * sampai ke mobile adalah blok {@code user.userRole.*} pada respons
 * {@code login}/{@code loginInfo}. Blok itu memuat flag boolean modul dengan
 * benar, tapi daftar menunya ikut sebagai hasil {@code toString()} dari
 * {@code PersistentSet} Hibernate — sebuah string ±25 KB berisi
 * {@code "id-label, id-label, ..."} tanpa {@code root}/{@code child},
 * tanpa {@code nomorUrut}, dan tanpa privilege CRUD. Mobile jadi tidak bisa
 * menyusun hierarki menu maupun menegakkan hak akses per-aksi.
 *
 * <p>
 * Endpoint ini mengirimkan ketiga lapis model akses sekaligus:
 * <ol>
 * <li>{@code modul} — flag boolean dari {@link Tbmrole}. Diambil lewat
 * <b>getter</b>, bukan kolom mentah, karena banyak getter berisi logika
 * hardcode berdasarkan {@code roleId} (mis. {@code getElearning()} selalu
 * {@code true} untuk {@code mhs}/{@code Dosen}). Membaca kolom langsung akan
 * memberi hasil berbeda dengan yang dipakai sisi web.</li>
 * <li>{@code menu} — isi {@code job_has_menu} untuk role tersebut, lengkap
 * dengan {@code root}/{@code child} sehingga mobile bisa menyusun hierarki
 * memakai aturan yang sama dengan web: <b>{@code anak.root == induk.child}</b>
 * (lihat {@code MenuHelper.buildMegaMenuItem}), bukan {@code anak.root ==
 * induk.id}.</li>
 * <li>{@code priv} per menu — isi {@code role_privilage}
 * ({@code _read}/{@code _create}/{@code _update}/{@code _delete}/
 * {@code _approve}/{@code _reject}).</li>
 * </ol>
 *
 * <p>
 * Endpoint ini <b>hanya membaca</b>; tidak ada satu pun operasi tulis, dan
 * tidak ada perilaku endpoint lama yang diubah. Aplikasi mobile versi lama yang
 * tidak memanggilnya tetap berjalan seperti sebelumnya.
 *
 * <p>
 * Parameter request:
 * <ul>
 * <li>{@code token} — wajib, token sesi hasil login.</li>
 * <li>{@code roleId} — opsional. Dipakai saat pengguna memilih salah satu dari
 * {@code userRole2..userRole5} miliknya. Nilai yang bukan milik pengguna
 * <b>ditolak</b> (status {@code 93}) supaya tidak bisa dipakai menaikkan hak
 * akses.</li>
 * </ul>
 *
 * <p>
 * Kompatibel Java 1.7 — tanpa lambda maupun try-with-resources, mengikuti
 * catatan di {@link ApiHelperSupport}.
 */
public class HakAksesApi {

	/** Batas aman jumlah menu yang dikirim dalam satu respons. */
	private static final int MAKS_MENU = 5000;

	public static JSONObject hakAksesSaya(HttpServletRequest req, JSONObject request) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				return ApiHelperSupport.status("97", "Token tidak sesuai");
			}

			List<Tbmrole> rolesUser = ambilRolesAman(tbmuser);

			String roleIdDiminta = ApiHelperSupport.optString(request, "roleId");
			Tbmrole role = pilihRole(tbmuser, rolesUser, roleIdDiminta);

			if (ApiHelperSupport.hasText(roleIdDiminta) && role == null) {
				// roleId dikirim tapi bukan milik pengguna ini.
				return ApiHelperSupport.status("93", "Role tidak tersedia untuk pengguna ini");
			}

			hasil.put("status", ApiHelperSupport.STATUS_OK);
			hasil.put("description", "Pengambilan hak akses berhasil");
			hasil.put("userId", tbmuser.getUserId());

			// `role` selalu dikirim (walau kosong) agar klien tidak perlu
			// membedakan bentuk respons. Pengguna yang login sebagai
			// Mahasiswa/Siswa langsung memang tidak punya Tbmrole.
			JSONObject roleJson = new JSONObject();
			roleJson.put("roleId", role == null ? "" : ApiHelperSupport.safeString(role.getRoleId()));
			roleJson.put("roleName", role == null ? "" : ApiHelperSupport.safeString(role.getRoleName()));
			roleJson.put("kode", role == null ? "" : ApiHelperSupport.safeString(role.getKode()));
			hasil.put("role", roleJson);

			hasil.put("rolesTersedia", daftarRole(rolesUser));

			if (role == null) {
				hasil.put("modul", new JSONObject());
				hasil.put("menu", new JSONArray());
				hasil.put("tanpaRole", true);
				return hasil;
			}

			hasil.put("modul", modulRole(role));
			hasil.put("halamanUtama", ApiHelperSupport.safeString(role.getHalamanUtama()));

			session = HibernateUtil.getSessionFactory().openSession();
			hasil.put("menu", daftarMenu(session, role));

			return hasil;
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			return ApiHelperSupport.errorResponse(err);
		} finally {
			tutupSessionDiam(session);
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:closeSession");
			}
		}
	}

	// ── Role ────────────────────────────────────────────────────────────────

	private static List<Tbmrole> ambilRolesAman(Tbmuser tbmuser) {
		try {
			List<Tbmrole> roles = tbmuser.ambilRoles();
			return roles == null ? new ArrayList<Tbmrole>() : roles;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:ambilRolesAman");
			return new ArrayList<Tbmrole>();
		}
	}

	/**
	 * Menentukan role yang dipakai. Bila {@code roleIdDiminta} diisi, role
	 * tersebut wajib ada di antara {@code userRole..userRole5} milik pengguna;
	 * kalau tidak, method ini mengembalikan {@code null} dan pemanggil menolak
	 * request. Ini mencegah klien menaikkan hak aksesnya sendiri hanya dengan
	 * mengirim {@code roleId} lain.
	 */
	private static Tbmrole pilihRole(Tbmuser tbmuser, List<Tbmrole> rolesUser, String roleIdDiminta) {
		if (ApiHelperSupport.hasText(roleIdDiminta)) {
			String diminta = roleIdDiminta.trim();
			for (int i = 0; i < rolesUser.size(); i++) {
				Tbmrole kandidat = rolesUser.get(i);
				if (kandidat != null && kandidat.getRoleId() != null
						&& kandidat.getRoleId().equalsIgnoreCase(diminta)) {
					return kandidat;
				}
			}
			return null;
		}

		try {
			Tbmrole role = tbmuser.hakAkses();
			if (role != null) {
				return role;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:pilihRole");
		}

		try {
			return tbmuser.getUserRole();
		} catch (Exception e) {
			return null;
		}
	}

	private static JSONArray daftarRole(List<Tbmrole> roles) {
		JSONArray array = new JSONArray();
		for (int i = 0; i < roles.size(); i++) {
			Tbmrole role = roles.get(i);
			if (role == null || role.getRoleId() == null) {
				continue;
			}
			try {
				JSONObject item = new JSONObject();
				item.put("roleId", role.getRoleId());
				item.put("roleName", ApiHelperSupport.safeString(role.getRoleName()));
				array.put(item);
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:daftarRole");
			}
		}
		return array;
	}

	// ── Flag modul ──────────────────────────────────────────────────────────

	/**
	 * Seluruh flag boolean {@link Tbmrole}, dibaca lewat getter agar logika
	 * hardcode per-{@code roleId} di dalamnya ikut terpakai. Nama kunci sengaja
	 * dibuat sama persis dengan nama properti Java supaya klien bisa
	 * memetakannya tanpa tabel terjemahan.
	 */
	private static JSONObject modulRole(Tbmrole role) {
		JSONObject modul = new JSONObject();
		put(modul, "elearning", role.getElearning());
		put(modul, "pustaka", role.getPustaka());
		put(modul, "dashboard", role.getDashboard());
		put(modul, "workflow", role.getWorkflow());
		put(modul, "kegiatanDanPrestasi", role.getKegiatanDanPrestasi());
		put(modul, "administrasi", role.getAdministrasi());
		put(modul, "pengadaan", role.getPengadaan());
		put(modul, "keuangan", role.getKeuangan());
		put(modul, "kepegawaian", role.getKepegawaian());
		put(modul, "presensiKehadiran", role.getPresensiKehadiran());
		put(modul, "absenLangsung", role.getAbsenLangsung());
		put(modul, "pembayaran", role.getPembayaran());
		put(modul, "kalenderAkademik", role.getKalenderAkademik());
		put(modul, "bolehAksesFeeder", role.getBolehAksesFeeder());
		put(modul, "bolehAksesSister", role.getBolehAksesSister());
		put(modul, "infoKegiatan", role.getInfoKegiatan());
		put(modul, "dasborRepository", role.getDasborRepository());
		put(modul, "dasboardAntarJemput", role.getDasboardAntarJemput());
		put(modul, "tampilkanSpmi", role.getTampilkanSpmi());
		put(modul, "tampilkanGaji", role.getTampilkanGaji());
		put(modul, "melihatDataPegawaiLain", role.getMelihatDataPegawaiLain());
		put(modul, "mengajukanPengajuanPegawaiLain", role.getMengajukanPengajuanPegawaiLain());
		put(modul, "melihatDataSatkerLain", role.getMelihatDataSatkerLain());
		put(modul, "melihatSemuaSurat", role.getMelihatSemuaSurat());
		put(modul, "melihatSemuaSop", role.getMelihatSemuaSop());
		put(modul, "updateFormatLaporan", role.getUpdateFormatLaporan());
		put(modul, "akunting", role.getAkunting());
		put(modul, "kinerja", role.getKinerja());
		put(modul, "kantin", role.getKantin());
		put(modul, "tampilPos", role.getTampilPos());
		put(modul, "dashboardKoperasi", role.getDashboardKoperasi());
		// Flag landing/modul ini bagian dari Tbmrole.ebisnisMenu (JSON konsolidasi) -- lihat
		// ais.common.EbisnisMenuKatalog. Nama key JSON API di sini SENGAJA dipertahankan sama persis
		// (kantinMemberLandingPage/aksesSupervisorKantin/aksesBerandaKantin) demi kompatibilitas
		// pemanggil existing, walau sumber datanya sekarang satu kolom JSON.
		org.json.JSONObject ebisnisMenuRole = ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu());
		put(modul, "kantinMemberLandingPage", Boolean.valueOf(ebisnisMenuRole.optBoolean("landingKantin", false)));
		put(modul, "inventorySalesLandingPage", Boolean.valueOf(ebisnisMenuRole.optBoolean("landingInventory", false)));
		put(modul, "aksesSupervisorKantin", Boolean.valueOf(ebisnisMenuRole.optBoolean("supervisor", false)));
		put(modul, "aksesBerandaKantin", Boolean.valueOf(ebisnisMenuRole.optBoolean("berandaKantin", false)));
		put(modul, "emedic", role.getEmedic());
		put(modul, "bolehEntryTopup", role.getBolehEntryTopup());
		return modul;
	}

	private static void put(JSONObject object, String key, Boolean value) {
		ApiHelperSupport.put(object, key, Boolean.valueOf(value != null && value.booleanValue()));
	}

	// ── Menu + privilege ────────────────────────────────────────────────────

	/**
	 * Menu milik role dari {@code job_has_menu}, digabung dengan privilege CRUD
	 * dari {@code role_privilage}. Menu yang {@code aktif == false} dibuang,
	 * mengikuti perilaku renderer web {@code MenuHelper.buildMegaMenuItem}.
	 */
	private static JSONArray daftarMenu(Session session, Tbmrole role) {
		JSONArray array = new JSONArray();

		Tbmrole roleDb = role;
		try {
			Object dariDb = session.get(Tbmrole.class, role.getRoleId());
			if (dariDb != null) {
				roleDb = (Tbmrole) dariDb;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:getRole");
		}

		List<Menu> menus = new ArrayList<Menu>();
		try {
			if (roleDb.getMenus() != null) {
				menus.addAll(roleDb.getMenus());
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:getMenus");
		}

		try {
			// Menu.compareTo mengurutkan berdasarkan nomorUrut, lalu root, lalu child —
			// sama dengan urutan yang dipakai renderer menu web.
			Collections.sort(menus);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:sortMenu");
		}

		Map<Long, RolePrivilage> privilegePerMenu = privilegePerMenu(session, roleDb);

		int dikirim = 0;
		for (int i = 0; i < menus.size(); i++) {
			Menu menu = menus.get(i);
			if (menu == null || menu.getId() == null) {
				continue;
			}
			if (menu.getAktif() != null && !menu.getAktif().booleanValue()) {
				continue;
			}
			if (dikirim >= MAKS_MENU) {
				break;
			}
			try {
				JSONObject item = new JSONObject();
				item.put("id", menu.getId());
				item.put("label", ApiHelperSupport.safeString(menu.getLabel()));
				item.put("root", menu.getRoot());
				item.put("child", menu.getChild());
				item.put("urut", menu.getNomorUrut());
				item.put("icon", ApiHelperSupport.safeString(menu.getIcon()));
				item.put("bigIcon", ApiHelperSupport.safeString(menu.getBigIcon()));
				item.put("url", ApiHelperSupport.safeString(menu.getUrl()));
				item.put("bukaHalamanBaru", Boolean.valueOf(Boolean.TRUE.equals(menu.getBukaHalamanBaru())));
				item.put("priv", privilegeJson(privilegePerMenu.get(menu.getId())));
				array.put(item);
				dikirim++;
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:menuItem");
			}
		}

		return array;
	}

	/**
	 * Seluruh baris {@code role_privilage} milik role, diindeks per id menu.
	 * Diambil sekali dalam satu query supaya tidak menimbulkan N+1 pada role
	 * yang punya ratusan menu.
	 */
	private static Map<Long, RolePrivilage> privilegePerMenu(Session session, Tbmrole role) {
		Map<Long, RolePrivilage> map = new HashMap<Long, RolePrivilage>();
		try {
			List<RolePrivilage> daftar = ConstantValues.simpleList(
					session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", role)),
					RolePrivilage.class);
			if (daftar == null) {
				return map;
			}
			for (int i = 0; i < daftar.size(); i++) {
				Object o = daftar.get(i);
				if (!(o instanceof RolePrivilage)) {
					continue;
				}
				RolePrivilage privilage = (RolePrivilage) o;
				try {
					Menu menu = privilage.getMenu();
					if (menu != null && menu.getId() != null) {
						map.put(menu.getId(), privilage);
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:privMenu");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:privilegePerMenu");
		}
		return map;
	}

	/**
	 * Bentuk ringkas privilege: {@code r}=read, {@code c}=create, {@code u}=update,
	 * {@code d}=delete, {@code a}=approve, {@code j}=reject.
	 *
	 * <p>
	 * Bila role tidak punya baris {@code role_privilage} untuk menu tersebut,
	 * menu dianggap <b>boleh dibaca</b> tapi tidak boleh diubah. Ini mengikuti
	 * kenyataan data di lapangan: menu bisa masuk {@code job_has_menu} tanpa
	 * baris privilege pasangannya, dan sisi web pun tetap menampilkannya.
	 */
	private static JSONObject privilegeJson(RolePrivilage privilage) {
		JSONObject json = new JSONObject();
		if (privilage == null) {
			ApiHelperSupport.put(json, "r", Integer.valueOf(1));
			ApiHelperSupport.put(json, "c", Integer.valueOf(0));
			ApiHelperSupport.put(json, "u", Integer.valueOf(0));
			ApiHelperSupport.put(json, "d", Integer.valueOf(0));
			ApiHelperSupport.put(json, "a", Integer.valueOf(0));
			ApiHelperSupport.put(json, "j", Integer.valueOf(0));
			ApiHelperSupport.put(json, "default", Boolean.TRUE);
			return json;
		}
		ApiHelperSupport.put(json, "r", angka(privilage.getRead()));
		ApiHelperSupport.put(json, "c", angka(privilage.getCreate()));
		ApiHelperSupport.put(json, "u", angka(privilage.getUpdate()));
		ApiHelperSupport.put(json, "d", angka(privilage.getDelete()));
		ApiHelperSupport.put(json, "a", angka(privilage.getApprove()));
		ApiHelperSupport.put(json, "j", angka(privilage.getReject()));
		return json;
	}

	private static Integer angka(Integer nilai) {
		return Integer.valueOf(nilai == null || nilai.intValue() <= 0 ? 0 : 1);
	}

	// ── Util ────────────────────────────────────────────────────────────────

	private static void tutupSessionDiam(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:clear");
				}
				try {
					session.disconnect();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:disconnect");
				}
				try {
					session.close();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:close");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/HakAksesApi.java:tutupSession");
		}
	}
}
