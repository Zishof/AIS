package ais.service.tenant;

import java.util.List;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Rekonsiliasi drift shared ↔ schema tenant (P9) -- gerbang keamanan sebelum TENANT_ONLY.</h3>
 *
 * <p>Cutover (P8) menulis dual-write, tetapi sebelum operator memindahkan platform ke
 * {@code TENANT_ONLY} secara produksi ia WAJIB membuktikan schema tenant sudah lengkap &amp;
 * konsisten dengan sumber shared. Service ini membandingkan baris milik pemilik tenant di tabel
 * shared ({@code public.brand}, {@code koperasi.toko}, {@code koperasi.pedagang}) terhadap mirror
 * di schema tenant, melaporkan:</p>
 * <ul>
 *   <li>{@code hilangDiTenant} -- ada di shared, belum ada di schema tenant (butuh sinkron);</li>
 *   <li>{@code bedaField} -- ada di keduanya tetapi nilai kunci berbeda (mis. nama/aktif);</li>
 *   <li>{@code yatimDiTenant} -- ada di schema tenant tetapi TIDAK di shared (anomali; hanya
 *       dilaporkan, TIDAK dihapus otomatis -- penghapusan data adalah keputusan manual).</li>
 * </ul>
 *
 * <p>{@link #reconcile} = laporan saja (read-only). {@link #repair} = jalankan
 * {@link TenantDataPlaneService#sinkronDariShared} (idempoten) utk menutup {@code hilangDiTenant}
 * dan mem-{@code mirror}-ulang baris {@code bedaField} (shared = sumber kebenaran saat repair) --
 * TIDAK menghapus yatim. Aman diulang.</p>
 */
public final class TenantDataReconciliationService {

	private TenantDataReconciliationService() {
	}

	/** Laporan drift satu tenant (read-only). @return JSON ringkas per tabel + total. */
	public static JSONObject reconcile(Long pendaftarId) throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String schema = TenantDataPlaneService.schemaTenantMilik(session, pendaftarId);
			if (schema == null) {
				hasil.put("status", "91");
				hasil.put("code", "NO_TENANT_SCHEMA");
				hasil.put("description", "Pendaftar ini tidak memiliki schema tenant terprovision.");
				return hasil;
			}
			JSONObject brand = driftBrand(session, schema, pendaftarId);
			JSONObject toko = driftToko(session, schema, pendaftarId);
			JSONObject pedagang = driftPedagang(session, schema, pendaftarId);
			int totalDrift = brand.getInt("driftTotal") + toko.getInt("driftTotal")
					+ pedagang.getInt("driftTotal");
			hasil.put("status", "00");
			hasil.put("schema", schema);
			hasil.put("brand", brand);
			hasil.put("toko", toko);
			hasil.put("pedagang", pedagang);
			hasil.put("driftTotal", totalDrift);
			hasil.put("konsisten", totalDrift == 0);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Perbaiki drift (sinkron + mirror-ulang beda) lalu laporkan ulang. Idempoten. */
	public static JSONObject repair(Long pendaftarId) throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String schema = TenantDataPlaneService.schemaTenantMilik(session, pendaftarId);
			if (schema == null) {
				hasil.put("status", "91");
				hasil.put("code", "NO_TENANT_SCHEMA");
				hasil.put("description", "Pendaftar ini tidak memiliki schema tenant terprovision.");
				return hasil;
			}
			session.beginTransaction();
			// 1) Tutup baris hilang (sinkronDariShared idempoten -- id-sama, +audit ADD).
			TenantDataPlaneService.sinkronDariShared(session, schema, pendaftarId);
			// 2) Mirror-ulang baris yang BEDA field (shared = sumber kebenaran) -- +audit MOD.
			int diperbaiki = mirrorUlangBeda(session, schema, pendaftarId);
			session.getTransaction().commit();
			hasil = reconcile(pendaftarId);
			hasil.put("diperbaikiBeda", diperbaiki);
			hasil.put("code", "RECONCILED");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) TenantDataReconciliationService.repair.rollback"); }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	// =====================================================================
	// DRIFT per tabel
	// =====================================================================

	private static JSONObject driftBrand(Session session, String schema, Long pendaftarId) throws Exception {
		String s = TenantSchemaService.pastikanAman(schema);
		long shared = jumlah(session, "SELECT COUNT(*) FROM public.brand WHERE pendaftar = :p", pendaftarId);
		long tenant = jumlah(session, "SELECT COUNT(*) FROM \"" + s + "\".brand", null);
		long hilang = jumlah(session, "SELECT COUNT(*) FROM public.brand b WHERE b.pendaftar = :p "
				+ "AND NOT EXISTS (SELECT 1 FROM \"" + s + "\".brand t WHERE t.id = b.id)", pendaftarId);
		long beda = jumlah(session, "SELECT COUNT(*) FROM public.brand b JOIN \"" + s
				+ "\".brand t ON t.id = b.id WHERE b.pendaftar = :p AND (COALESCE(b.nama,'') <> "
				+ "COALESCE(t.nama,'') OR COALESCE(b.aktif,true) <> COALESCE(t.aktif,true))", pendaftarId);
		long yatim = jumlah(session, "SELECT COUNT(*) FROM \"" + s + "\".brand t WHERE NOT EXISTS "
				+ "(SELECT 1 FROM public.brand b WHERE b.id = t.id AND b.pendaftar = :p)", pendaftarId);
		return ringkas(shared, tenant, hilang, beda, yatim);
	}

	private static JSONObject driftToko(Session session, String schema, Long pendaftarId) throws Exception {
		String s = TenantSchemaService.pastikanAman(schema);
		long shared = jumlah(session, "SELECT COUNT(*) FROM koperasi.toko WHERE pendaftar = :p", pendaftarId);
		long tenant = jumlah(session, "SELECT COUNT(*) FROM \"" + s + "\".toko", null);
		long hilang = jumlah(session, "SELECT COUNT(*) FROM koperasi.toko b WHERE b.pendaftar = :p "
				+ "AND NOT EXISTS (SELECT 1 FROM \"" + s + "\".toko t WHERE t.id = b.id)", pendaftarId);
		long beda = jumlah(session, "SELECT COUNT(*) FROM koperasi.toko b JOIN \"" + s
				+ "\".toko t ON t.id = b.id WHERE b.pendaftar = :p AND (COALESCE(b.nama,'') <> "
				+ "COALESCE(t.nama,'') OR COALESCE(b.aktif,true) <> COALESCE(t.aktif,true) "
				+ "OR COALESCE(b.kota,'') <> COALESCE(t.kota,''))", pendaftarId);
		long yatim = jumlah(session, "SELECT COUNT(*) FROM \"" + s + "\".toko t WHERE NOT EXISTS "
				+ "(SELECT 1 FROM koperasi.toko b WHERE b.id = t.id AND b.pendaftar = :p)", pendaftarId);
		return ringkas(shared, tenant, hilang, beda, yatim);
	}

	private static JSONObject driftPedagang(Session session, String schema, Long pendaftarId) throws Exception {
		String s = TenantSchemaService.pastikanAman(schema);
		long shared = jumlah(session, "SELECT COUNT(*) FROM koperasi.pedagang pd JOIN koperasi.toko t "
				+ "ON t.id = pd.toko WHERE t.pendaftar = :p", pendaftarId);
		long tenant = jumlah(session, "SELECT COUNT(*) FROM \"" + s + "\".pedagang", null);
		long hilang = jumlah(session, "SELECT COUNT(*) FROM koperasi.pedagang pd JOIN koperasi.toko t "
				+ "ON t.id = pd.toko WHERE t.pendaftar = :p AND NOT EXISTS (SELECT 1 FROM \"" + s
				+ "\".pedagang x WHERE x.id = pd.id)", pendaftarId);
		long beda = jumlah(session, "SELECT COUNT(*) FROM koperasi.pedagang pd JOIN koperasi.toko t "
				+ "ON t.id = pd.toko JOIN \"" + s + "\".pedagang x ON x.id = pd.id WHERE t.pendaftar = :p "
				+ "AND (COALESCE(pd.userid,'') <> COALESCE(x.userid,'') OR COALESCE(pd.nama,'') <> "
				+ "COALESCE(x.nama,'') OR COALESCE(pd.aktif,true) <> COALESCE(x.aktif,true))", pendaftarId);
		long yatim = jumlah(session, "SELECT COUNT(*) FROM \"" + s + "\".pedagang x WHERE NOT EXISTS "
				+ "(SELECT 1 FROM koperasi.pedagang pd JOIN koperasi.toko t ON t.id = pd.toko "
				+ "WHERE pd.id = x.id AND t.pendaftar = :p)", pendaftarId);
		return ringkas(shared, tenant, hilang, beda, yatim);
	}

	// =====================================================================
	// REPAIR: mirror-ulang baris yang BEDA (shared = sumber kebenaran)
	// =====================================================================

	@SuppressWarnings("rawtypes")
	private static int mirrorUlangBeda(Session session, String schema, Long pendaftarId) {
		String s = TenantSchemaService.pastikanAman(schema);
		int n = 0;
		List brands = session.createSQLQuery("SELECT b.id, b.nama, b.aktif FROM public.brand b JOIN \""
				+ s + "\".brand t ON t.id = b.id WHERE b.pendaftar = :p AND (COALESCE(b.nama,'') <> "
				+ "COALESCE(t.nama,'') OR COALESCE(b.aktif,true) <> COALESCE(t.aktif,true))")
				.setParameter("p", pendaftarId).list();
		for (int i = 0; i < brands.size(); i++) {
			Object[] r = (Object[]) brands.get(i);
			TenantDataPlaneService.mirrorBrand(session, s, Long.valueOf(((Number) r[0]).longValue()),
					String.valueOf(r[1]), r[2] == null ? Boolean.TRUE : (Boolean) r[2], false);
			n++;
		}
		List tokos = session.createSQLQuery("SELECT b.id, b.nama, b.brand, b.alamat, b.kota, b.telp, "
				+ "b.aktif FROM koperasi.toko b JOIN \"" + s + "\".toko t ON t.id = b.id WHERE "
				+ "b.pendaftar = :p AND (COALESCE(b.nama,'') <> COALESCE(t.nama,'') OR "
				+ "COALESCE(b.aktif,true) <> COALESCE(t.aktif,true) OR COALESCE(b.kota,'') <> "
				+ "COALESCE(t.kota,''))").setParameter("p", pendaftarId).list();
		for (int i = 0; i < tokos.size(); i++) {
			Object[] r = (Object[]) tokos.get(i);
			TenantDataPlaneService.mirrorToko(session, s, Long.valueOf(((Number) r[0]).longValue()),
					String.valueOf(r[1]), r[2] == null ? null : Long.valueOf(((Number) r[2]).longValue()),
					r[3] == null ? "" : String.valueOf(r[3]), r[4] == null ? "" : String.valueOf(r[4]),
					r[5] == null ? "" : String.valueOf(r[5]),
					r[6] == null ? Boolean.TRUE : (Boolean) r[6], false);
			n++;
		}
		List pedagangs = session.createSQLQuery("SELECT pd.id, pd.userid, pd.pass, pd.nama, pd.toko, "
				+ "pd.supervisor, pd.aktif FROM koperasi.pedagang pd JOIN koperasi.toko t ON t.id = pd.toko "
				+ "JOIN \"" + s + "\".pedagang x ON x.id = pd.id WHERE t.pendaftar = :p AND "
				+ "(COALESCE(pd.userid,'') <> COALESCE(x.userid,'') OR COALESCE(pd.nama,'') <> "
				+ "COALESCE(x.nama,'') OR COALESCE(pd.aktif,true) <> COALESCE(x.aktif,true))")
				.setParameter("p", pendaftarId).list();
		for (int i = 0; i < pedagangs.size(); i++) {
			Object[] r = (Object[]) pedagangs.get(i);
			TenantDataPlaneService.mirrorPedagang(session, s, Long.valueOf(((Number) r[0]).longValue()),
					String.valueOf(r[1]), r[2] == null ? "" : String.valueOf(r[2]),
					r[3] == null ? "" : String.valueOf(r[3]),
					r[4] == null ? null : Long.valueOf(((Number) r[4]).longValue()),
					r[5] == null ? Boolean.FALSE : (Boolean) r[5],
					r[6] == null ? Boolean.TRUE : (Boolean) r[6], false);
			n++;
		}
		return n;
	}

	// =====================================================================
	// UTIL
	// =====================================================================

	private static long jumlah(Session session, String sql, Long pendaftarId) {
		org.hibernate.SQLQuery q = session.createSQLQuery(sql);
		if (pendaftarId != null) {
			q.setParameter("p", pendaftarId);
		}
		Number n = (Number) q.uniqueResult();
		return n == null ? 0 : n.longValue();
	}

	private static JSONObject ringkas(long shared, long tenant, long hilang, long beda, long yatim)
			throws Exception {
		JSONObject j = new JSONObject();
		j.put("shared", shared);
		j.put("tenant", tenant);
		j.put("hilangDiTenant", hilang);
		j.put("bedaField", beda);
		j.put("yatimDiTenant", yatim);
		// yatim TIDAK dihitung sbg drift yang di-repair (tidak dihapus otomatis) tapi dilaporkan.
		j.put("driftTotal", (int) (hilang + beda));
		return j;
	}

	/** Entry-point utilitas operator: rekonsiliasi semua tenant ber-schema (mode HYBRID/TENANT_ONLY). */
	@SuppressWarnings("rawtypes")
	public static JSONArray reconcileSemua() throws Exception {
		JSONArray arr = new JSONArray();
		Session session = HibernateUtil.getSessionFactory().openSession();
		List owners;
		try {
			owners = session.createSQLQuery("SELECT DISTINCT owner_pendaftar_id FROM public.tenant_registry "
					+ "WHERE schema_name IS NOT NULL AND status IN ('READY','ACTIVE')").list();
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		for (int i = 0; i < owners.size(); i++) {
			Long pid = Long.valueOf(((Number) owners.get(i)).longValue());
			JSONObject r = reconcile(pid);
			r.put("pendaftarId", pid);
			arr.put(r);
		}
		return arr;
	}

	/** Mode saat ini (utk panel admin). */
	public static String modePlatformSekarang() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			return TenantDataPlaneService.modePlatform(session);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static final String[] MODE_VALID = { TenantRegistry.MODE_LEGACY, TenantRegistry.MODE_HYBRID,
			TenantRegistry.MODE_TENANT_ONLY };
}
