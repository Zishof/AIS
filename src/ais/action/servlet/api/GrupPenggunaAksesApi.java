package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;
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
 * Kontrak native untuk editor menu dan privilege pada Grup Pengguna.
 *
 * <p>CRUD field dasar {@link Tbmrole} tetap dilayani Generic CRUD. Kelas ini
 * khusus menggantikan dua editor tambahan di {@code TbmroleAction}: relasi
 * {@code job_has_menu} dan enam flag {@code role_privilage}. Mutasi selalu
 * online dan otorisasi ditegakkan lagi di server.</p>
 *
 * <p>Administrator boleh mengelola seluruh menu. Pengguna non-admin hanya
 * boleh mengelola menu yang dimiliki role aktifnya dan tidak dapat memberikan
 * privilege yang lebih tinggi daripada miliknya sendiri. Relasi target yang
 * berada di luar kewenangan pemanggil dipertahankan ketika menyimpan.</p>
 */
public final class GrupPenggunaAksesApi {

	public static final long MENU_GRUP_PENGGUNA = 2L;
	private static final int MAKS_MENU = 5000;

	private GrupPenggunaAksesApi() {
	}

	public static JSONObject daftar(HttpServletRequest req, JSONObject request) {
		Session session = null;
		try {
			Tbmuser actor = ApiUtil.currentUser(request, req);
			if (actor == null || actor.getUserId() == null) {
				return ApiHelperSupport.status("97", "Token tidak sesuai");
			}
			session = HibernateUtil.getSessionFactory().openSession();
			Izin izin = izin(session, actor);
			if (!izin.baca) {
				return ApiHelperSupport.status("93", "Anda tidak memiliki hak membaca Grup Pengguna");
			}

			String roleId = ApiHelperSupport.optString(request, "targetRoleId").trim();
			Tbmrole target = role(session, roleId);
			if (target == null) {
				return ApiHelperSupport.status("91", "Grup Pengguna tidak ditemukan");
			}
			if (!izin.admin && Tbmrole.ADMINISTRATOR.equalsIgnoreCase(roleId)) {
				return ApiHelperSupport.status("93", "Hanya Administrator yang dapat mengubah grup Administrator");
			}

			JSONObject hasil = ApiHelperSupport.status(ApiHelperSupport.STATUS_OK,
					"Hak akses Grup Pengguna berhasil dimuat");
			hasil.put("targetRoleId", target.getRoleId());
			hasil.put("targetRoleName", aman(target.getRoleName()));
			hasil.put("izin", izin.json());
			hasil.put("data", daftarMenu(session, target, izin));
			return hasil;
		} catch (Exception e) {
			return ApiHelperSupport.errorResponse(Common.tampilErrorJikaAdmin(e));
		} finally {
			tutup(session);
		}
	}

	public static JSONObject simpan(HttpServletRequest req, JSONObject request) {
		Session session = null;
		Transaction tx = null;
		Tbmrole target = null;
		try {
			Tbmuser actor = ApiUtil.currentUser(request, req);
			if (actor == null || actor.getUserId() == null) {
				return ApiHelperSupport.status("97", "Token tidak sesuai");
			}
			session = HibernateUtil.getSessionFactory().openSession();
			Izin izin = izin(session, actor);
			if (!izin.ubah) {
				return ApiHelperSupport.status("93", "Anda tidak memiliki hak mengubah Grup Pengguna");
			}

			String roleId = ApiHelperSupport.optString(request, "targetRoleId").trim();
			target = role(session, roleId);
			if (target == null) {
				return ApiHelperSupport.status("91", "Grup Pengguna tidak ditemukan");
			}
			if (!izin.admin && Tbmrole.ADMINISTRATOR.equalsIgnoreCase(roleId)) {
				return ApiHelperSupport.status("93", "Hanya Administrator yang dapat mengubah grup Administrator");
			}
			if (!izin.admin && izin.roleAktif != null
					&& roleId.equalsIgnoreCase(izin.roleAktif.getRoleId())) {
				return ApiHelperSupport.status("93",
						"Untuk mencegah akun terkunci, hak akses grup yang sedang dipakai hanya dapat diubah Administrator");
			}

			JSONArray akses = array(request, "akses");
			if (akses == null) {
				return ApiHelperSupport.status("91", "Daftar hak akses belum dikirim");
			}
			if (akses.length() > MAKS_MENU) {
				return ApiHelperSupport.status("91", "Jumlah menu melebihi batas aman");
			}

			Map<Long, Menu> menuAktif = menuAktif(session);
			Map<Long, Hak> hakActor = izin.admin
					? hakAdministrator(menuAktif)
					: hakPerMenu(session, izin.roleAktif, true);
			Map<Long, Hak> diminta = new HashMap<Long, Hak>();
			for (int i = 0; i < akses.length(); i++) {
				JSONObject item = akses.optJSONObject(i);
				if (item == null || !item.optBoolean("dipilih", false)) {
					continue;
				}
				Long menuId = longValue(item.opt("menuId"));
				if (menuId == null || !menuAktif.containsKey(menuId)) {
					return ApiHelperSupport.status("91", "Terdapat menu yang tidak valid atau sudah tidak aktif");
				}
				Hak batas = hakActor.get(menuId);
				if (batas == null || !batas.baca) {
					return ApiHelperSupport.status("93", "Anda mencoba memberikan menu di luar kewenangan Anda");
				}
				Hak hak = Hak.dari(item);
				if (!izin.admin && !hak.diDalam(batas)) {
					return ApiHelperSupport.status("93", "Privilege yang diberikan melebihi hak akun Anda");
				}
				diminta.put(menuId, hak);
			}

			tx = session.beginTransaction();
			Set<Menu> hasilMenu = new HashSet<Menu>();
			if (!izin.admin && target.getMenus() != null) {
				for (Menu lama : target.getMenus()) {
					if (lama != null && lama.getId() != null && !hakActor.containsKey(lama.getId())) {
						hasilMenu.add(lama);
					}
				}
			}
			for (Long id : diminta.keySet()) {
				hasilMenu.add(menuAktif.get(id));
			}
			target.setMenus(hasilMenu);
			session.update(target);

			List<RolePrivilage> semuaPrivilege = ConstantValues.simpleList(
					session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", target)),
					RolePrivilage.class);
			Map<Long, RolePrivilage> lamaPerMenu = new HashMap<Long, RolePrivilage>();
			List<RolePrivilage> duplikat = new ArrayList<RolePrivilage>();
			if (semuaPrivilege != null) {
				for (RolePrivilage p : semuaPrivilege) {
					Long id = idMenu(p);
					if (id == null || (!izin.admin && !hakActor.containsKey(id))) {
						continue;
					}
					if (lamaPerMenu.containsKey(id)) {
						duplikat.add(p);
					} else {
						lamaPerMenu.put(id, p);
					}
				}
			}
			for (Map.Entry<Long, Hak> entry : diminta.entrySet()) {
				RolePrivilage p = lamaPerMenu.remove(entry.getKey());
				if (p == null) {
					p = new RolePrivilage();
					p.setRole(target);
					p.setMenu(menuAktif.get(entry.getKey()));
				}
				entry.getValue().terapkan(p);
				session.saveOrUpdate(p);
			}
			for (RolePrivilage p : lamaPerMenu.values()) {
				session.delete(p);
			}
			for (RolePrivilage p : duplikat) {
				session.delete(p);
			}
			tx.commit();

			ais.common.newui.NewUiCacheInvalidator.invalidateRole(target.getRoleId());
			Tbmuser.refreshHakAksesUntukRole(target);
			return ApiHelperSupport.status(ApiHelperSupport.STATUS_OK,
					"Hak akses Grup Pengguna berhasil disimpan");
		} catch (Exception e) {
			ApiHelperSupport.rollbackQuietly(tx);
			return ApiHelperSupport.errorResponse(Common.tampilErrorJikaAdmin(e));
		} finally {
			tutup(session);
		}
	}

	private static JSONArray daftarMenu(Session session, Tbmrole target, Izin izin) throws Exception {
		Map<Long, Hak> targetHak = hakPerMenu(session, target, true);
		Map<Long, Hak> actorHak = izin.admin ? null : hakPerMenu(session, izin.roleAktif, true);
		List<Menu> menus = ConstantValues.simpleList(session.createCriteria(Menu.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("root")).addOrder(Order.asc("child")),
				Menu.class);
		JSONArray data = new JSONArray();
		if (menus == null) {
			return data;
		}
		for (Menu menu : menus) {
			if (menu == null || menu.getId() == null || (!izin.admin && !actorHak.containsKey(menu.getId()))) {
				continue;
			}
			Hak hak = targetHak.get(menu.getId());
			JSONObject row = new JSONObject();
			row.put("menuId", menu.getId());
			row.put("label", aman(menu.getLabel()));
			row.put("root", menu.getRoot());
			row.put("child", menu.getChild());
			row.put("urut", menu.getNomorUrut());
			row.put("dipilih", Boolean.valueOf(hak != null));
			row.put("hak", (hak == null ? Hak.kosong() : hak).json());
			if (!izin.admin) {
				row.put("batas", actorHak.get(menu.getId()).json());
			}
			data.put(row);
			if (data.length() >= MAKS_MENU) {
				break;
			}
		}
		return data;
	}

	private static Map<Long, Menu> menuAktif(Session session) {
		Map<Long, Menu> hasil = new HashMap<Long, Menu>();
		List<Menu> menus = ConstantValues.simpleList(session.createCriteria(Menu.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE))),
				Menu.class);
		if (menus != null) {
			for (Menu menu : menus) {
				if (menu != null && menu.getId() != null) {
					hasil.put(menu.getId(), menu);
				}
			}
		}
		return hasil;
	}

	private static Map<Long, Hak> hakAdministrator(Map<Long, Menu> menus) {
		Map<Long, Hak> hasil = new HashMap<Long, Hak>();
		for (Long id : menus.keySet()) {
			hasil.put(id, Hak.semua());
		}
		return hasil;
	}

	private static Map<Long, Hak> hakPerMenu(Session session, Tbmrole role, boolean defaultBaca) {
		Map<Long, Hak> hasil = new HashMap<Long, Hak>();
		if (role == null || role.getRoleId() == null) {
			return hasil;
		}
		Tbmrole roleDb = role(session, role.getRoleId());
		if (roleDb == null) {
			return hasil;
		}
		if (roleDb.getMenus() != null) {
			for (Menu menu : roleDb.getMenus()) {
				if (menu != null && menu.getId() != null && !Boolean.FALSE.equals(menu.getAktif())) {
					hasil.put(menu.getId(), defaultBaca ? Hak.baca() : Hak.kosong());
				}
			}
		}
		List<RolePrivilage> daftar = ConstantValues.simpleList(
				session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", roleDb)),
				RolePrivilage.class);
		if (daftar != null) {
			for (RolePrivilage p : daftar) {
				Long menuId = idMenu(p);
				if (menuId != null && hasil.containsKey(menuId)) {
					Hak gabung = hasil.get(menuId);
					gabung.gabung(Hak.dari(p));
				}
			}
		}
		return hasil;
	}

	private static Izin izin(Session session, Tbmuser actor) {
		Izin hasil = new Izin();
		try {
			Set<?> ids = actor.ambilRolesId();
			if (ids != null) {
				for (Object id : ids) {
					if (id != null && Tbmrole.ADMINISTRATOR.equalsIgnoreCase(String.valueOf(id))) {
						hasil.admin = hasil.baca = hasil.ubah = true;
						return hasil;
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "GrupPenggunaAksesApi.izin.admin");
		}
		try {
			hasil.roleAktif = actor.hakAkses();
		} catch (Exception e) {
			return hasil;
		}
		Map<Long, Hak> hak = hakPerMenu(session, hasil.roleAktif, true);
		Hak menu = hak.get(Long.valueOf(MENU_GRUP_PENGGUNA));
		if (menu != null) {
			hasil.baca = menu.baca;
			hasil.ubah = menu.ubah;
		}
		return hasil;
	}

	private static Tbmrole role(Session session, String id) {
		if (session == null || !ApiHelperSupport.hasText(id)) {
			return null;
		}
		return (Tbmrole) session.get(Tbmrole.class, id.trim());
	}

	private static Long idMenu(RolePrivilage p) {
		try {
			return p == null || p.getMenu() == null ? null : p.getMenu().getId();
		} catch (Exception e) {
			return null;
		}
	}

	private static JSONArray array(JSONObject request, String key) {
		JSONArray direct = request.optJSONArray(key);
		if (direct != null) {
			return direct;
		}
		String text = ApiHelperSupport.optString(request, key);
		try {
			return ApiHelperSupport.hasText(text) ? new JSONArray(text) : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static Long longValue(Object value) {
		try {
			return value == null ? null : Long.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return null;
		}
	}

	private static String aman(String value) {
		return value == null ? "" : value.trim();
	}

	private static void tutup(Session session) {
		ApiHelperSupport.closeOpenedSession(session, false);
	}

	private static final class Izin {
		boolean admin;
		boolean baca;
		boolean ubah;
		Tbmrole roleAktif;

		JSONObject json() {
			JSONObject o = new JSONObject();
			ApiHelperSupport.put(o, "admin", Boolean.valueOf(admin));
			ApiHelperSupport.put(o, "baca", Boolean.valueOf(baca));
			ApiHelperSupport.put(o, "ubah", Boolean.valueOf(ubah));
			return o;
		}
	}

	private static final class Hak {
		boolean baca;
		boolean tambah;
		boolean ubah;
		boolean hapus;
		boolean setuju;
		boolean tolak;

		static Hak kosong() { return new Hak(); }
		static Hak baca() { Hak h = new Hak(); h.baca = true; return h; }
		static Hak semua() {
			Hak h = new Hak();
			h.baca = h.tambah = h.ubah = h.hapus = h.setuju = h.tolak = true;
			return h;
		}
		static Hak dari(JSONObject o) {
			JSONObject h = o.optJSONObject("hak");
			if (h == null) h = o;
			Hak r = new Hak();
			r.baca = bool(h, "r"); r.tambah = bool(h, "c"); r.ubah = bool(h, "u");
			r.hapus = bool(h, "d"); r.setuju = bool(h, "a"); r.tolak = bool(h, "j");
			return r;
		}
		static Hak dari(RolePrivilage p) {
			Hak h = new Hak();
			h.baca = positif(p.getRead()); h.tambah = positif(p.getCreate());
			h.ubah = positif(p.getUpdate()); h.hapus = positif(p.getDelete());
			h.setuju = positif(p.getApprove()); h.tolak = positif(p.getReject());
			return h;
		}
		void gabung(Hak h) {
			baca |= h.baca; tambah |= h.tambah; ubah |= h.ubah;
			hapus |= h.hapus; setuju |= h.setuju; tolak |= h.tolak;
		}
		boolean diDalam(Hak batas) {
			return (!baca || batas.baca) && (!tambah || batas.tambah) && (!ubah || batas.ubah)
					&& (!hapus || batas.hapus) && (!setuju || batas.setuju) && (!tolak || batas.tolak);
		}
		void terapkan(RolePrivilage p) {
			p.setRead(angka(baca)); p.setCreate(angka(tambah)); p.setUpdate(angka(ubah));
			p.setDelete(angka(hapus)); p.setApprove(angka(setuju)); p.setReject(angka(tolak));
		}
		JSONObject json() {
			JSONObject o = new JSONObject();
			ApiHelperSupport.put(o, "r", Boolean.valueOf(baca));
			ApiHelperSupport.put(o, "c", Boolean.valueOf(tambah));
			ApiHelperSupport.put(o, "u", Boolean.valueOf(ubah));
			ApiHelperSupport.put(o, "d", Boolean.valueOf(hapus));
			ApiHelperSupport.put(o, "a", Boolean.valueOf(setuju));
			ApiHelperSupport.put(o, "j", Boolean.valueOf(tolak));
			return o;
		}
		private static boolean bool(JSONObject o, String key) {
			Object v = o.opt(key);
			return Boolean.TRUE.equals(v) || "1".equals(String.valueOf(v))
					|| "true".equalsIgnoreCase(String.valueOf(v));
		}
		private static boolean positif(Integer v) { return v != null && v.intValue() > 0; }
		private static Integer angka(boolean v) { return Integer.valueOf(v ? 1 : 0); }
	}
}
