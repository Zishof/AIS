package ais.action.servlet.api;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * <h3>API JSON riwayat revisi Envers per baris data (AuditTrails) utk klien POS
 * Desktop/Android -- padanan ringan {@code GenericRevisiHelper} (ZK).</h3>
 *
 * <p>Aksi:</p>
 * <ul>
 *   <li>{@code revisi_daftar} {entitas, id} -- daftar revisi baris (rev, tanggal,
 *       tipe ADD/MOD/DEL, oleh) urut terbaru; semua user login boleh melihat.</li>
 *   <li>{@code revisi_detail} {entitas, id, rev} -- nilai properti sederhana pada
 *       revisi tsb (utk tampilan banding/pratinjau restore).</li>
 *   <li>{@code revisi_pulihkan} {entitas, id, rev} -- RESTORE nilai revisi ke data
 *       hidup. HANYA {@code Common.getApakahAdminLain} (paritas tombol Restore di
 *       GenericRevisiHelper yang memang layar admin). Restore DANGKAL: properti
 *       sederhana + relasi ManyToOne di-resolve ke baris LIVE ber-id sama;
 *       koleksi dilewati (pola restoreOneProperty, bukan deep-restore).</li>
 * </ul>
 *
 * <p>Entitas dibatasi WHITELIST {@link #ENTITAS} (kode stabil -> kelas) -- klien
 * tidak pernah bisa menyebut kelas sembarangan. Menambah dukungan = tambah satu
 * baris peta (entitas wajib @Audited).</p>
 */
public final class RevisiApiHelper {

	private RevisiApiHelper() {
	}

	/** kode stabil (dipakai klien) -> kelas entitas @Audited. */
	static final Map<String, Class> ENTITAS = new LinkedHashMap<String, Class>();
	static {
		ENTITAS.put("produk", ais.database.model.inventory.Produk.class);
		ENTITAS.put("jenis_produk", ais.database.model.inventory.JenisProduk.class);
		ENTITAS.put("grup_produk", ais.database.model.inventory.GrupProduk.class);
		ENTITAS.put("toko", ais.database.model.inventory.Toko.class);
		ENTITAS.put("penyedia", ais.database.model.library.Penyedia.class);
		ENTITAS.put("anggota", ais.database.model.koperasi.AnggotaKoperasi.class);
		ENTITAS.put("jenis_anggota", ais.database.model.koperasi.JenisAnggotaKoperasi.class);
		ENTITAS.put("tipe_anggota", ais.database.model.koperasi.TipeAnggotaKoperasi.class);
		ENTITAS.put("cara_bayar", ais.database.model.koperasi.CaraPembayaranKoperasi.class);
		ENTITAS.put("diskon", ais.database.model.koperasi.AturanDiskon.class);
		// Riwayat transaksi (header nota) -- baca-saja utk penelusuran; restore
		// tetap dimungkinkan admin (kasus salah-void diperiksa manual).
		ENTITAS.put("transaksi", ais.database.model.koperasi.PembelianAnggotaKoperasi.class);
		// KebijakanRetur TIDAK terdaftar: entitasnya belum @Audited.
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static Class kelasDari(JSONObject request, JSONObject hasil) throws Exception {
		String kode = request == null ? "" : request.optString("entitas", "").trim();
		Class clazz = (Class) ENTITAS.get(kode);
		if (clazz == null) {
			tolak(hasil, "Entitas '" + kode + "' tidak dikenal utk riwayat revisi.");
		}
		return clazz;
	}

	private static Long idDari(JSONObject request, JSONObject hasil) throws Exception {
		if (request == null || request.isNull("id")) {
			tolak(hasil, "Parameter id wajib diisi.");
			return null;
		}
		return Long.valueOf((request.get("id") + "").trim());
	}

	/** Nilai properti -> bentuk JSON yang aman (tipe sederhana; relasi -> label). */
	private static Object nilaiRingkas(Object nilai) {
		if (nilai == null) return JSONObject.NULL;
		if (nilai instanceof String || nilai instanceof Number || nilai instanceof Boolean) {
			return nilai;
		}
		if (nilai instanceof Date) {
			return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) nilai);
		}
		// Relasi/objek lain: coba label getNama() dulu, jatuh ke toString ringkas.
		try {
			java.lang.reflect.Method m = nilai.getClass().getMethod("getNama");
			Object nama = m.invoke(nilai);
			if (nama != null) return String.valueOf(nama);
		} catch (Throwable abaikan) {
			// tidak semua punya getNama -- bukan masalah.
		}
		String s = String.valueOf(nilai);
		return s.length() > 120 ? s.substring(0, 120) : s;
	}

	@SuppressWarnings("unchecked")
	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null) { tolak(hasil, "Sesi tidak dikenali."); return; }
		Class clazz = kelasDari(request, hasil);
		Long id = clazz == null ? null : idDari(request, hasil);
		if (clazz == null || id == null) return;
		int batas = Math.min(100, Math.max(1, request.optInt("batas", 30)));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AuditReader reader = AuditReaderFactory.get(session);
			List baris = reader.createQuery().forRevisionsOfEntity(clazz, false, true)
					.add(AuditEntity.id().eq(id))
					.addOrder(AuditEntity.revisionNumber().desc())
					.setMaxResults(batas).getResultList();
			ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			JSONArray arr = new JSONArray();
			for (int i = 0; i < baris.size(); i++) {
				Object[] b = (Object[]) baris.get(i);
				Object entitas = b[0];
				org.hibernate.envers.DefaultRevisionEntity rev =
						(org.hibernate.envers.DefaultRevisionEntity) b[1];
				RevisionType tipe = (RevisionType) b[2];
				JSONObject j = new JSONObject();
				j.put("rev", rev.getId());
				j.put("tanggal", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.format(rev.getRevisionDate()));
				j.put("tipe", tipe == RevisionType.ADD ? "TAMBAH"
						: tipe == RevisionType.DEL ? "HAPUS" : "UBAH");
				// "oleh" diambil dari kolom audit entitas itu sendiri (GeneralValueObject
				// menyimpannya per revisi); DefaultRevisionEntity tidak memuat user.
				try {
					Object oleh = meta.getPropertyValue(entitas, "oleh", EntityMode.POJO);
					if (oleh != null) j.put("oleh", String.valueOf(oleh));
				} catch (Throwable abaikan) {
					// entitas tanpa properti oleh -- kolom dikosongkan saja.
				}
				try {
					Object nama = meta.getPropertyValue(entitas, "nama", EntityMode.POJO);
					if (nama != null) j.put("nama", String.valueOf(nama));
				} catch (Throwable abaikan) {
					// idem.
				}
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("bolehPulihkan", Common.getApakahAdminLain(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void detail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null) { tolak(hasil, "Sesi tidak dikenali."); return; }
		Class clazz = kelasDari(request, hasil);
		Long id = clazz == null ? null : idDari(request, hasil);
		if (clazz == null || id == null) return;
		int rev = request.optInt("rev", -1);
		if (rev < 0) { tolak(hasil, "Parameter rev wajib diisi."); return; }
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AuditReader reader = AuditReaderFactory.get(session);
			Object snapshot = reader.find(clazz, id, Integer.valueOf(rev));
			if (snapshot == null) { tolak(hasil, "Revisi tidak ditemukan."); return; }
			ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			String[] props = meta.getPropertyNames();
			Type[] tipe = meta.getPropertyTypes();
			JSONObject nilai = new JSONObject();
			for (int i = 0; i < props.length; i++) {
				if (tipe[i].isCollectionType()) continue; // koleksi di luar riwayat baris.
				try {
					nilai.put(props[i], nilaiRingkas(
							meta.getPropertyValue(snapshot, props[i], EntityMode.POJO)));
				} catch (Throwable abaikan) {
					// proxy audit yang tak bisa dibaca -- lewati properti itu saja.
				}
			}
			hasil.put("status", "00");
			hasil.put("nilai", nilai);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Restore DANGKAL nilai satu revisi ke baris hidup (admin-only). Baris yang
	 * sudah terhapus dihidupkan lagi lewat instance baru ber-id sama. Relasi
	 * ManyToOne diambil ulang dari data LIVE ber-id sama (bukan proxy audit);
	 * relasi yang barisnya sudah tidak ada dilewati (fail-soft per properti).
	 */
	public static void pulihkan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null || !Common.getApakahAdminLain(tbmuser)) {
			tolak(hasil, "Hanya admin sistem yang boleh me-restore data dari riwayat.");
			return;
		}
		Class clazz = kelasDari(request, hasil);
		Long id = clazz == null ? null : idDari(request, hasil);
		if (clazz == null || id == null) return;
		int rev = request.optInt("rev", -1);
		if (rev < 0) { tolak(hasil, "Parameter rev wajib diisi."); return; }
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AuditReader reader = AuditReaderFactory.get(session);
			Object snapshot = reader.find(clazz, id, Integer.valueOf(rev));
			if (snapshot == null) { tolak(hasil, "Revisi tidak ditemukan."); return; }
			ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			Object target = session.get(clazz, id);
			boolean hidupkanLagi = (target == null);
			if (hidupkanLagi) {
				target = clazz.newInstance();
				meta.setIdentifier(target, (Serializable) id, EntityMode.POJO);
			}
			String[] props = meta.getPropertyNames();
			Type[] tipeProp = meta.getPropertyTypes();
			int dilewati = 0;
			for (int i = 0; i < props.length; i++) {
				if (tipeProp[i].isCollectionType()) continue;
				try {
					Object nilai = meta.getPropertyValue(snapshot, props[i], EntityMode.POJO);
					if (tipeProp[i].isEntityType() && nilai != null) {
						Class kelasRelasi = tipeProp[i].getReturnedClass();
						ClassMetadata metaRelasi =
								HibernateUtil.getSessionFactory().getClassMetadata(kelasRelasi);
						Serializable idRelasi =
								metaRelasi.getIdentifier(nilai, EntityMode.POJO);
						nilai = idRelasi == null ? null : session.get(kelasRelasi, idRelasi);
						if (nilai == null && idRelasi != null) { dilewati++; continue; }
					}
					meta.setPropertyValue(target, props[i], nilai, EntityMode.POJO);
				} catch (Throwable abaikan) {
					dilewati++; // satu properti bermasalah tidak menggagalkan restore.
				}
			}
			session.beginTransaction();
			if (hidupkanLagi) {
				session.replicate(target, org.hibernate.ReplicationMode.OVERWRITE);
			} else {
				session.saveOrUpdate(target);
			}
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("dihidupkanLagi", hidupkanLagi);
			hasil.put("propertiDilewati", dilewati);
			hasil.put("description", "Data dipulihkan dari revisi " + rev
					+ (dilewati > 0 ? " (" + dilewati + " properti dilewati)." : "."));
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "RevisiApiHelper.pulihkan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Dispatcher aksi revisi_*. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request,
			JSONObject hasil) throws Exception {
		if ("revisi_daftar".equals(action)) { daftar(tbmuser, request, hasil); return true; }
		if ("revisi_detail".equals(action)) { detail(tbmuser, request, hasil); return true; }
		if ("revisi_pulihkan".equals(action)) { pulihkan(tbmuser, request, hasil); return true; }
		return false;
	}
}
