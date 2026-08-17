package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.EbisnisMenuKatalog;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.SalesInventory;

/**
 * <h3>Resolver konteks aktor eBisnis -- FAIL-CLOSED (fondasi P1 varian "Inventory &amp; Sales").</h3>
 *
 * <p>Menggantikan asumsi berbahaya yang tersebar di jalur lama ({@code KantinHelper}:
 * {@code adminGlobal = tbmuser.getPedagang() == null}, ~30 lokasi) KHUSUS untuk permukaan API baru
 * ber-prefix {@code si_} -- jalur POS lama TIDAK diubah sama sekali (backward compat). Aturan
 * PERINTAH_MASTER &sect;3.5: user Sales Keliling boleh TIDAK punya {@code Pedagang}; "toko == null"
 * TIDAK PERNAH berarti admin. User tanpa scope yang sah DITOLAK, bukan dinaikkan jadi admin.</p>
 *
 * <h3>Penentuan {@code actorType} (urutan evaluasi)</h3>
 * <ol>
 *   <li>{@link #ACTOR_SALES} -- role aktif = {@link #ROLE_SALES_KELILING}, ATAU punya profil
 *       {@link SalesInventory} aktif (assignment adalah bukti terkuat).</li>
 *   <li>{@link #ACTOR_PEMILIK} -- role aktif = {@link #ROLE_PEMILIK}.</li>
 *   <li>{@link #ACTOR_ADMIN} -- admin sesungguhnya: TANPA baris Pedagang DAN (tanpa role sama
 *       sekali ATAU role ber-flag {@code supervisor} di {@code ebisnisMenu}). Ini identik dgn
 *       populasi yang HARI INI sudah diperlakukan admin oleh gate lama ({@code bolehAksesActionKantin}
 *       meloloskan {@code role == null} dan flag supervisor), jadi tidak ada akun existing yang
 *       kehilangan akses -- tapi user baru ber-role non-supervisor TIDAK lagi otomatis admin.</li>
 *   <li>{@link #ACTOR_POS} -- sisanya (kasir/pedagang POS biasa). Permukaan {@code si_} menolak
 *       aktor ini kecuali menu role-nya secara eksplisit mengaktifkan kunci Inventory &amp; Sales.</li>
 * </ol>
 */
public final class EbisnisActorContextResolver {

	public static final String ROLE_PEMILIK = "pemilik_sales_inventory";
	public static final String ROLE_SALES_KELILING = "sales_keliling";

	public static final String ACTOR_ADMIN = "ADMIN";
	public static final String ACTOR_PEMILIK = "PEMILIK_SALES_INVENTORY";
	public static final String ACTOR_SALES = "SALES_KELILING";
	public static final String ACTOR_POS = "POS_LAINNYA";

	private EbisnisActorContextResolver() {
	}

	/** Hasil resolusi -- immutable secukupnya (field diisi sekali oleh {@link #resolve}). */
	public static final class ActorContext {
		public String userId;
		public String actorType = ACTOR_POS;
		public String activeRoleId;
		public List<String> roleIds = new ArrayList<String>();
		public boolean admin;
		public boolean supervisor;
		public Long tokoId;
		public String tokoNama;
		public Long salesId;
		public String salesKode;
		public String salesNama;
		/** Hasil {@link EbisnisMenuKatalog#urai(String)} utk role aktif -- selalu non-null. */
		public JSONObject permissions;

		public boolean bolehMenu(String kunci) {
			if (admin || supervisor) {
				return true;
			}
			JSONObject menu = permissions == null ? null : permissions.optJSONObject("menu");
			// Fail-closed: kunci tak dikenal / permissions kosong = TIDAK boleh (kebalikan
			// default-true katalog lama -- permukaan si_ memang sengaja opt-in).
			return menu != null && menu.optBoolean(kunci, false);
		}

		public boolean bolehAksi(String kunciMenu, String aksi) {
			if (admin || supervisor) {
				return true;
			}
			if (!bolehMenu(kunciMenu)) {
				return false;
			}
			JSONObject crud = permissions == null ? null : permissions.optJSONObject("crud");
			JSONObject baris = crud == null ? null : crud.optJSONObject(kunciMenu);
			return baris != null && baris.optBoolean(aksi, false);
		}

		public JSONObject toJson() throws Exception {
			JSONObject j = new JSONObject();
			j.put("userId", userId == null ? "" : userId);
			j.put("actorType", actorType);
			j.put("activeRoleId", activeRoleId == null ? JSONObject.NULL : activeRoleId);
			j.put("roleIds", new JSONArray(roleIds));
			j.put("isAdminInventorySales", admin);
			j.put("supervisor", supervisor);
			j.put("tokoId", tokoId == null ? JSONObject.NULL : tokoId);
			j.put("tokoNama", tokoNama == null ? "" : tokoNama);
			j.put("salesId", salesId == null ? JSONObject.NULL : salesId);
			j.put("salesKode", salesKode == null ? "" : salesKode);
			j.put("salesNama", salesNama == null ? "" : salesNama);
			j.put("permissions", permissions == null ? EbisnisMenuKatalog.urai(null) : permissions);
			// Trip aktif diisi mulai P5 (Nota Sales session) -- field SUDAH ada di kontrak sejak P1
			// supaya klien tidak perlu migrasi bentuk response lagi; null = tidak ada sesi berjalan.
			j.put("currentTripId", JSONObject.NULL);
			JSONArray fitur = new JSONArray();
			fitur.put("pos");
			if (!ACTOR_POS.equals(actorType)) {
				fitur.put("inventory_sales");
			}
			j.put("featureProfile", fitur);
			return j;
		}
	}

	/** Buka session sendiri (util utk pemanggil tanpa session aktif). */
	public static ActorContext resolve(Tbmuser tbmuser) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			return resolve(session, tbmuser);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static ActorContext resolve(Session session, Tbmuser tbmuser) {
		ActorContext ctx = new ActorContext();
		if (tbmuser == null || tbmuser.getUserId() == null) {
			ctx.permissions = EbisnisMenuKatalog.urai(null);
			return ctx; // ACTOR_POS tanpa identitas -- pemanggil (dispatcher) yang menolak.
		}
		ctx.userId = tbmuser.getUserId();

		Tbmrole role = null;
		try {
			role = tbmuser.hakAkses();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "EbisnisActorContextResolver: hakAkses gagal, fail-closed ke POS_LAINNYA");
		}
		ctx.activeRoleId = role == null ? null : role.getRoleId();
		kumpulkanRoleIds(tbmuser, ctx.roleIds);
		ctx.permissions = EbisnisMenuKatalog.urai(role == null ? null : role.getEbisnisMenu());
		ctx.supervisor = ctx.permissions.optBoolean("supervisor", false);

		Pedagang pedagang = null;
		try {
			pedagang = tbmuser.getPedagang();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "EbisnisActorContextResolver: getPedagang gagal (lazy?) -- dianggap tanpa pedagang");
		}
		if (ctx.supervisor && pedagang == null) {
			// flag supervisor role tanpa keterikatan toko = perlakuan admin (paritas gate lama).
		}

		SalesInventory profil = salesProfilAktif(session, tbmuser);
		if (profil != null) {
			ctx.salesId = profil.getId();
			ctx.salesKode = profil.getKode();
			ctx.salesNama = profil.getNama();
		}

		// Administrator resmi selalu menang atas relasi Sales/Pedagang/Toko. Relasi bisnis
		// menentukan scope data, bukan menurunkan hak administrator menjadi aktor biasa.
		if (Common.getApakahAdminLain(tbmuser)) {
			ctx.actorType = ACTOR_ADMIN;
			ctx.admin = true;
			isiTokoDariPedagangAtauMultiToko(ctx, tbmuser, pedagang);
		// -- actorType lain, urutan sesuai JavaDoc kelas --
		} else if (ROLE_SALES_KELILING.equals(ctx.activeRoleId) || profil != null) {
			ctx.actorType = ACTOR_SALES;
			Toko tokoSales = profil == null ? null : profil.getToko();
			if (tokoSales != null) {
				ctx.tokoId = tokoSales.getId();
				ctx.tokoNama = tokoSales.getNama();
			}
			// Sales TANPA profil aktif: actorType tetap SALES (landing "tugas mendatang"),
			// tapi salesId/tokoId null -> seluruh operasi ber-scope sales DITOLAK dispatcher.
		} else if (ROLE_PEMILIK.equals(ctx.activeRoleId)) {
			ctx.actorType = ACTOR_PEMILIK;
			isiTokoDariPedagangAtauMultiToko(ctx, tbmuser, pedagang);
		} else if (pedagang == null && (role == null || ctx.supervisor)) {
			ctx.actorType = ACTOR_ADMIN;
			ctx.admin = true;
		} else {
			ctx.actorType = ACTOR_POS;
			isiTokoDariPedagangAtauMultiToko(ctx, tbmuser, pedagang);
		}
		return ctx;
	}

	private static void isiTokoDariPedagangAtauMultiToko(ActorContext ctx, Tbmuser tbmuser, Pedagang pedagang) {
		try {
			if (pedagang != null && pedagang.getToko() != null) {
				ctx.tokoId = pedagang.getToko().getId();
				ctx.tokoNama = pedagang.getToko().getNama();
				return;
			}
			Long aktif = tbmuser.getTokoAktifMultiToko();
			if (aktif != null) {
				ctx.tokoId = aktif;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "EbisnisActorContextResolver: resolusi toko gagal -- scope dibiarkan kosong (fail-closed)");
		}
	}

	private static SalesInventory salesProfilAktif(Session session, Tbmuser tbmuser) {
		try {
			return (SalesInventory) session.createCriteria(SalesInventory.class)
					.add(Restrictions.eq("tbmuser", tbmuser))
					.add(Restrictions.eq("aktif", Boolean.TRUE))
					.setMaxResults(1).uniqueResult();
		} catch (Exception e) {
			// Tabel belum tercipta pada start pertama (hbm2ddl belum jalan) / query gagal --
			// fail-closed: dianggap tidak punya profil, JANGAN meledakkan aksi konfigurasi.
			ais.common.ErrorAuditUtil.record(e, "EbisnisActorContextResolver: lookup SalesInventory gagal (dianggap tanpa profil)");
			return null;
		}
	}

	private static void kumpulkanRoleIds(Tbmuser tbmuser, List<String> tampung) {
		Tbmrole[] semua = new Tbmrole[5];
		try { semua[0] = tbmuser.getUserRole(); } catch (Exception e) { /* slot kosong/lazy gagal -- lewati */ }
		try { semua[1] = tbmuser.getUserRole2(); } catch (Exception e) { /* idem */ }
		try { semua[2] = tbmuser.getUserRole3(); } catch (Exception e) { /* idem */ }
		try { semua[3] = tbmuser.getUserRole4(); } catch (Exception e) { /* idem */ }
		try { semua[4] = tbmuser.getUserRole5(); } catch (Exception e) { /* idem */ }
		for (int i = 0; i < semua.length; i++) {
			if (semua[i] != null && semua[i].getRoleId() != null && !tampung.contains(semua[i].getRoleId())) {
				tampung.add(semua[i].getRoleId());
			}
		}
	}
}
