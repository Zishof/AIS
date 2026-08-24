package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.tenant.TenantMembership;
import ais.database.model.tenant.TenantModuleEntitlement;
import ais.database.model.tenant.TenantRegistry;
import ais.service.tenant.TenantAccessException;
import ais.service.tenant.TenantContext;
import ais.service.tenant.TenantContextResolver;
import ais.service.tenant.TenantMembershipResolver;

/**
 * <h3>Aksi tenant untuk klien (P2).</h3>
 *
 * <p>Empat aksi aditif: {@code tenant_list}, {@code tenant_context},
 * {@code tenant_validate}, dan {@code tenant_select}.</p>
 *
 * <h4>Sengaja di luar gerbang {@code si_}</h4>
 * <p>Dispatcher ini dipanggil <b>sebelum</b> {@link SalesInventoryApiDispatcher}, dan nama
 * aksinya tidak berawalan {@code si_}. Itu disengaja: {@code tenant_list} harus dapat dipanggil
 * <b>sebelum</b> aktor Inventory/Sales lengkap. Bila ia ikut tergerbang, pengguna yang belum
 * punya profil Inventory/Sales tidak akan pernah dapat melihat daftar tenantnya -- dan karena
 * itu tidak pernah dapat memilih tenant yang justru memberinya profil tersebut.</p>
 *
 * <h4>Tanpa keadaan server</h4>
 * <p>{@code tenant_select} <b>tidak</b> menyimpan tenant aktif di sisi server; ia hanya alias
 * validasi. Klien yang menyimpan pilihannya, lalu mengirimkannya kembali pada tiap request.
 * Menyimpannya di server berarti dua tab atau dua perangkat milik satu pengguna saling
 * mengubah tenant aktif satu sama lain.</p>
 */
public final class TenantApiDispatcher {

	/** Header kontrak dokumen master 7.1. */
	public static final String HEADER_TENANT = "X-Tenant-Id";

	private TenantApiDispatcher() {
	}

	public static boolean dispatch(String action, Tbmuser tbmuser, JSONObject payload,
			JSONObject hasil, HttpServletRequest request) throws Exception {
		if (action == null || !action.startsWith("tenant_")) {
			return false;
		}
		boolean dikenal = "tenant_list".equals(action) || "tenant_context".equals(action)
				|| "tenant_validate".equals(action) || "tenant_select".equals(action);
		if (!dikenal) {
			return false;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String userId = tbmuser == null ? null : tbmuser.getUserId();
			Long pendaftarId = cariPendaftarId(session, userId);

			if ("tenant_list".equals(action)) {
				daftar(session, userId, pendaftarId, hasil);
			} else {
				Long tenantId = TenantContextResolver.selaraskanTenantId(
						angkaHeader(request, HEADER_TENANT), angkaJson(payload, "tenantId"));
				// Instalasi legacy yang belum mengaktifkan tenancy tetap sah. Khusus
				// tenant_context tanpa pilihan eksplisit, kembalikan konteks kosong yang
				// sukses agar klien dapat meneruskan alur lama tanpa mencatat false error.
				// tenant_validate/select dan tenantId eksplisit tetap melalui resolver,
				// sehingga gerbang keamanan tenant tidak dilonggarkan.
				if ("tenant_context".equals(action) && tenantId == null) {
					List<TenantRegistry> tenantAktif = TenantContextResolver.daftarTenantAktif(
							session, userId, pendaftarId);
					if (tenantAktif == null || tenantAktif.isEmpty()) {
						hasil.put("status", "success");
						hasil.put("mode", "legacy");
						hasil.put("data", JSONObject.NULL);
						return true;
					}
				}
				TenantContext ctx = tenantId == null
						? TenantContextResolver.resolveOtomatis(session, userId, pendaftarId)
						: TenantContextResolver.resolve(session, tenantId, userId, pendaftarId);
				hasil.put("status", "success");
				hasil.put("data", ctx.toJsonKlien());
			}
			return true;
		} catch (TenantAccessException e) {
			// Kode dibaca mesin, pesan dibaca manusia. Keduanya sudah bebas nama schema.
			hasil.put("status", "error");
			// Kunci "kode", bukan "code": ApiClient Flutter membaca json['kode'] dan
			// hanya itu. Mengirim "code" berarti kode galat baku §7.2 tidak pernah
			// sampai ke klien, dan penanganan per-kode di sana tidak akan pernah jalan.
			hasil.put("kode", e.getKode());
			hasil.put("message", e.getMessage());
			return true;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Daftar tenant milik aktor. <b>Tiga kueri</b> berapa pun jumlah tenantnya: satu untuk
	 * tenant, satu untuk peran, satu untuk modul. Membentuk TenantContext per tenant akan
	 * berarti dua sampai tiga kueri per baris, dan menolak tenant yang sedang SUSPENDED --
	 * padahal justru itu yang perlu dilihat pengguna.
	 */
	private static void daftar(Session session, String userId, Long pendaftarId, JSONObject hasil)
			throws Exception {
		List<TenantRegistry> tenant = TenantContextResolver.daftarTenantAktif(session, userId,
				pendaftarId);
		List<Long> ids = new ArrayList<Long>();
		for (int i = 0; i < tenant.size(); i++) {
			ids.add(tenant.get(i).getId());
		}
		Map<Long, String> peran = petaPeran(session, userId, pendaftarId);
		Map<Long, Set<String>> modul = petaModul(session, ids);

		JSONArray data = new JSONArray();
		for (int i = 0; i < tenant.size(); i++) {
			TenantRegistry t = tenant.get(i);
			JSONObject o = new JSONObject();
			o.put("tenantId", t.getId());
			o.put("tenantCode", t.getCode() == null ? "" : t.getCode());
			o.put("nama", t.getNama() == null ? "" : t.getNama());
			String r = peran.get(t.getId());
			if (r == null && t.getOwnerPendaftar() != null && pendaftarId != null
					&& pendaftarId.equals(t.getOwnerPendaftar().getId())) {
				r = TenantMembershipResolver.ROLE_OWNER;
			}
			o.put("role", r == null ? "" : r);
			o.put("status", t.getStatus() == null ? "" : t.getStatus());
			Set<String> m = modul.get(t.getId());
			JSONArray am = new JSONArray();
			if (m != null) {
				for (Iterator<String> it = m.iterator(); it.hasNext();) {
					am.put(it.next());
				}
			}
			o.put("modules", am);
			data.put(o);
		}
		hasil.put("status", "success");
		hasil.put("data", data);
	}

	private static Map<Long, String> petaPeran(Session session, String userId, Long pendaftarId) {
		Map<Long, String> peta = new HashMap<Long, String>();
		boolean adaUser = userId != null && userId.trim().length() > 0;
		if (!adaUser && pendaftarId == null) {
			return peta;
		}
		StringBuilder hql = new StringBuilder("FROM TenantMembership m WHERE m.status = :st AND (");
		if (adaUser) {
			hql.append("m.tbmuser.userId = :uid");
		}
		if (adaUser && pendaftarId != null) {
			hql.append(" OR ");
		}
		if (pendaftarId != null) {
			hql.append("m.pendaftar.id = :pid");
		}
		hql.append(")");
		Query q = session.createQuery(hql.toString());
		q.setParameter("st", TenantMembership.STATUS_ACTIVE);
		if (adaUser) {
			q.setParameter("uid", userId.trim());
		}
		if (pendaftarId != null) {
			q.setParameter("pid", pendaftarId);
		}
		List<?> baris = q.list();
		Date sekarang = new Date();
		for (int i = 0; i < baris.size(); i++) {
			TenantMembership m = (TenantMembership) baris.get(i);
			if (m.getValidFrom() != null && sekarang.before(m.getValidFrom())) {
				continue;
			}
			if (m.getValidUntil() != null && sekarang.after(m.getValidUntil())) {
				continue;
			}
			if (m.getTenant() == null || m.getTenant().getId() == null) {
				continue;
			}
			boolean owner = m.getIsOwner() != null && m.getIsOwner().booleanValue();
			String r = owner ? TenantMembershipResolver.ROLE_OWNER
					: (m.getRoleCode() == null ? "" : m.getRoleCode().trim());
			peta.put(m.getTenant().getId(), r);
		}
		return peta;
	}

	private static Map<Long, Set<String>> petaModul(Session session, List<Long> ids) {
		Map<Long, Set<String>> peta = new HashMap<Long, Set<String>>();
		if (ids.isEmpty()) {
			return peta;
		}
		Query q = session.createQuery("SELECT e.tenant.id, e.moduleCode, e.effectiveFrom,"
				+ " e.effectiveUntil FROM TenantModuleEntitlement e"
				+ " WHERE e.tenant.id IN (:ids) AND e.status = :st");
		q.setParameterList("ids", ids);
		q.setParameter("st", TenantModuleEntitlement.STATUS_ACTIVE);
		List<?> baris = q.list();
		Date sekarang = new Date();
		for (int i = 0; i < baris.size(); i++) {
			Object[] r = (Object[]) baris.get(i);
			Long id = (Long) r[0];
			String kode = (String) r[1];
			Date dari = (Date) r[2];
			Date sampai = (Date) r[3];
			if (id == null || kode == null || kode.trim().length() == 0) {
				continue;
			}
			if (dari != null && sekarang.before(dari)) {
				continue;
			}
			if (sampai != null && sekarang.after(sampai)) {
				continue;
			}
			Set<String> s = peta.get(id);
			if (s == null) {
				s = new HashSet<String>();
				peta.put(id, s);
			}
			s.add(kode.trim().toUpperCase());
		}
		return peta;
	}

	/**
	 * Id Pendaftar tempat pengguna ini bernaung, dari {@code tbmuser.pendaftar}.
	 *
	 * <p>Ditanyakan lewat kueri, bukan lewat penelusuran malas: relasinya LAZY dan objek
	 * {@code Tbmuser} datang dari Session autentikasi yang sudah ditutup.</p>
	 *
	 * <p>{@code null} berarti pengguna tidak bernaung pada pendaftar mana pun -- admin pusat
	 * atau akun legacy. Bagi mereka daftar tenant wajar kosong, dan jalur datanya adalah
	 * schema existing.</p>
	 */
	private static Long cariPendaftarId(Session session, String userId) {
		if (userId == null || userId.trim().length() == 0) {
			return null;
		}
		Query q = session.createQuery(
				"SELECT u.pendaftar.id FROM Tbmuser u WHERE u.userId = :uid");
		q.setParameter("uid", userId.trim());
		q.setMaxResults(1);
		return (Long) q.uniqueResult();
	}

	private static Long angkaHeader(HttpServletRequest request, String nama) {
		if (request == null) {
			return null;
		}
		return keLong(request.getHeader(nama));
	}

	private static Long angkaJson(JSONObject payload, String kunci) {
		if (payload == null || !payload.has(kunci) || payload.isNull(kunci)) {
			return null;
		}
		return keLong(payload.optString(kunci, null));
	}

	private static Long keLong(String v) {
		if (v == null || v.trim().length() == 0) {
			return null;
		}
		try {
			return Long.valueOf(v.trim());
		} catch (NumberFormatException e) {
			// tenantId bukan angka diperlakukan sama dengan tidak dikirim; validasi
			// kepemilikannya tetap dikerjakan resolver.
			return null;
		}
	}
}
