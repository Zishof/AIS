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
		// Gelombang 2 (2026-08-19): KebijakanRetur & GrupAturanDiskon kini @Audited
		// (revisi mulai terekam sejak deploy ini); sisanya sudah lama ter-audit.
		ENTITAS.put("kebijakan_retur", ais.database.model.inventory.KebijakanRetur.class);
		ENTITAS.put("diskon_grup", ais.database.model.koperasi.GrupAturanDiskon.class);
		ENTITAS.put("produk_batch", ais.database.model.inventory.ProdukBatch.class);
		ENTITAS.put("si_customer", ais.database.model.koperasi.CustomerInventoryProfile.class);
		ENTITAS.put("hotel_properti", ais.database.model.hotel.PropertiHotel.class);
		ENTITAS.put("hotel_kontrak", ais.database.model.hotel.KontrakPemilik.class);
		ENTITAS.put("hotel_tamu", ais.database.model.hotel.Tamu.class);
		ENTITAS.put("hotel_tipe_kamar", ais.database.model.hotel.TipeKamar.class);
		ENTITAS.put("hotel_kamar", ais.database.model.hotel.Kamar.class);
		// Gelombang 3 (2026-08-19): melengkapi layar yang sudah baca lokal-dulu
		// tapi belum punya tombol Riwayat (master Sales/Supplier IS, pencairan
		// diskon, profil item apotik).
		ENTITAS.put("si_sales", ais.database.model.koperasi.SalesInventory.class);
		ENTITAS.put("si_supplier", ais.database.model.koperasi.SupplierInventoryProfile.class);
		ENTITAS.put("pencairan_diskon", ais.database.model.koperasi.PencairanDiskon.class);
		ENTITAS.put("apotik_item", ais.database.model.sirs.ApotikItemProfile.class);
		// Gelombang 4 (2026-08-21): entitas PESANAN kantin. Ditambahkan setelah
		// pesanan Agustus hilang dari layar Pesanan -- baris yang terhapus hanya
		// dapat ditelusuri lewat tabel audit, dan tanpa entri ini kode entitasnya
		// tidak dikenal oleh API riwayat.
		ENTITAS.put("pesanan", ais.database.model.koperasi.DraftPembelianAnggotaKoperasi.class);
		ENTITAS.put("pesanan_item", ais.database.model.inventory.DraftPembelian.class);
		ENTITAS.put("pembelian", ais.database.model.inventory.Pembelian.class);
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

	/** Nama properti relasi yang layak ditampilkan sbg penanda baris saat jelajah. */
	private static final String[] RELASI_PENANDA = new String[] {
			"toko", "anggotaKoperasi", "tbmuser", "mejaKantin", "caraPembayaranKoperasi", "lunas" };

	private static boolean punyaProperti(ClassMetadata meta, String nama) {
		if (meta == null) return false;
		String[] props = meta.getPropertyNames();
		for (int i = 0; i < props.length; i++) {
			if (props[i].equals(nama)) return true;
		}
		return false;
	}

	/**
	 * Batas rentang jelajah. {@code akhirHari} mendorong jam ke 23:59:59.999 supaya
	 * "sampai 31 Agustus" benar-benar memuat seluruh 31 Agustus -- tanpa itu batas
	 * atas jatuh di tengah malam dan data seharian penuh ikut hilang dari hasil.
	 */
	private static Date batasTanggal(JSONObject request, String kunci, boolean akhirHari,
			JSONObject hasil) throws Exception {
		String s = request == null ? "" : request.optString(kunci, "").trim();
		if (s.length() == 0) {
			tolak(hasil, "Rentang tanggal (" + kunci + ") wajib diisi.");
			return null;
		}
		java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd");
		f.setLenient(false);
		Date d;
		try {
			d = f.parse(s.length() > 10 ? s.substring(0, 10) : s);
		} catch (Exception e) {
			tolak(hasil, "Tanggal " + s + " tidak dikenali (format yyyy-MM-dd).");
			return null;
		}
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(d);
		c.set(java.util.Calendar.HOUR_OF_DAY, akhirHari ? 23 : 0);
		c.set(java.util.Calendar.MINUTE, akhirHari ? 59 : 0);
		c.set(java.util.Calendar.SECOND, akhirHari ? 59 : 0);
		c.set(java.util.Calendar.MILLISECOND, akhirHari ? 999 : 0);
		return c.getTime();
	}

	private static RevisionType tipeRevisi(String kode) {
		if ("TAMBAH".equalsIgnoreCase(kode)) return RevisionType.ADD;
		if ("UBAH".equalsIgnoreCase(kode)) return RevisionType.MOD;
		if ("HAPUS".equalsIgnoreCase(kode)) return RevisionType.DEL;
		return null; // "SEMUA" atau kosong -> tanpa saringan tipe.
	}

	/** Daftar kode entitas yang boleh dijelajah -- dipakai klien utk mengisi combo. */
	public static void entitas(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null) { tolak(hasil, "Sesi tidak dikenali."); return; }
		JSONArray arr = new JSONArray();
		java.util.Iterator it = ENTITAS.keySet().iterator();
		while (it.hasNext()) {
			String kode = (String) it.next();
			JSONObject j = new JSONObject();
			j.put("kode", kode);
			Class c = (Class) ENTITAS.get(kode);
			j.put("kelas", c == null ? "" : c.getSimpleName());
			arr.put(j);
		}
		hasil.put("status", "00");
		hasil.put("data", arr);
		hasil.put("bolehJelajah", Common.getApakahAdminLain(tbmuser));
	}

	/**
	 * <h3>Jelajah tabel audit LINTAS baris -- padanan tab "Semua" pada
	 * {@code GenericRevisiHelper} (ZK).</h3>
	 *
	 * <p>Berbeda dari {@link #daftar} yang menuntut satu {@code id}: di sini id-nya
	 * justru yang dicari. Baris yang <b>sudah terhapus</b> tidak lagi muncul di layar
	 * mana pun, sehingga tidak ada tombol riwayat yang bisa diklik untuknya -- satu-
	 * satunya jalan menemukannya kembali adalah menyapu tabel audit menurut rentang
	 * tanggal. Itulah kasus yang melahirkan aksi ini.</p>
	 *
	 * <p>Rentang tanggal <b>wajib</b>, sama seperti versi ZK: tabel audit menyimpan
	 * seluruh sejarah dan menyapunya tanpa batas akan menarik jutaan baris.</p>
	 *
	 * <p>Karena keluarannya memuat data yang sudah dihapus dari seluruh toko --
	 * melewati pembatasan toko/pendaftar yang berlaku di layar biasa -- aksi ini
	 * dibatasi ADMINISTRATOR. Riwayat per baris ({@code revisi_daftar}) tetap terbuka
	 * untuk semua pengguna.</p>
	 */
	@SuppressWarnings("unchecked")
	public static void jelajah(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null) { tolak(hasil, "Sesi tidak dikenali."); return; }
		if (!Common.getApakahAdminLain(tbmuser)) {
			tolak(hasil, "Jelajah riwayat lintas-baris hanya untuk ADMINISTRATOR, karena "
					+ "menampilkan data terhapus dari seluruh toko. Riwayat satu baris tetap "
					+ "bisa dibuka lewat tombol jam pada tabel.");
			return;
		}
		Class clazz = kelasDari(request, hasil);
		if (clazz == null) return;
		Date dari = batasTanggal(request, "dari", false, hasil);
		if (dari == null) return;
		Date sampai = batasTanggal(request, "sampai", true, hasil);
		if (sampai == null) return;
		if (dari.after(sampai)) {
			tolak(hasil, "Tanggal awal melewati tanggal akhir.");
			return;
		}
		RevisionType tipeSaring = tipeRevisi(request.optString("tipe", ""));
		Long tokoId = null;
		if (request != null && !request.isNull("toko")) {
			String t = (request.get("toko") + "").trim();
			if (t.length() > 0 && !"null".equals(t)) tokoId = Long.valueOf(t);
		}
		int batas = Math.min(300, Math.max(1, request.optInt("batas", 100)));
		int mulai = Math.max(0, request.optInt("mulai", 0));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AuditReader reader = AuditReaderFactory.get(session);
			// Rentang TANGGAL diterjemahkan lebih dulu ke rentang NOMOR revisi. Nomor
			// revisi terindeks, sedangkan menyaring tanggal di sisi Java berarti menarik
			// seluruh tabel audit ke memori.
			Number revAwal;
			try {
				revAwal = reader.getRevisionNumberForDate(dari);
			} catch (Exception belumAda) {
				// Belum ada revisi apa pun sebelum tanggal awal -> mulai dari paling awal.
				revAwal = Integer.valueOf(0);
			}
			Number revAkhir;
			try {
				revAkhir = reader.getRevisionNumberForDate(sampai);
			} catch (Exception belumAda) {
				// Tidak ada satu pun revisi sampai tanggal akhir -> pasti kosong.
				hasil.put("status", "00");
				hasil.put("data", new JSONArray());
				hasil.put("adaLagi", Boolean.FALSE);
				return;
			}

			ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
			org.hibernate.envers.query.AuditQuery q = reader.createQuery()
					.forRevisionsOfEntity(clazz, false, true)
					.add(AuditEntity.revisionNumber().ge(revAwal))
					.add(AuditEntity.revisionNumber().le(revAkhir));
			if (tipeSaring != null) q.add(AuditEntity.revisionType().eq(tipeSaring));
			if (tokoId != null && punyaProperti(meta, "toko")) {
				q.add(AuditEntity.relatedId("toko").eq(tokoId));
			}
			q.addOrder(AuditEntity.revisionNumber().desc());
			q.setFirstResult(mulai);
			// +1 baris semata utk mengetahui masih ada halaman berikutnya; tidak dikirim.
			q.setMaxResults(batas + 1);
			List baris = q.getResultList();

			boolean adaLagi = baris.size() > batas;
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String[] props = meta.getPropertyNames();
			Type[] tipeProp = meta.getPropertyTypes();
			JSONArray arr = new JSONArray();
			for (int i = 0; i < baris.size() && arr.length() < batas; i++) {
				Object[] b = (Object[]) baris.get(i);
				Object entitasBaris = b[0];
				org.hibernate.envers.DefaultRevisionEntity rev =
						(org.hibernate.envers.DefaultRevisionEntity) b[1];
				RevisionType tipe = (RevisionType) b[2];
				// revAwal adalah revisi PADA-ATAU-SEBELUM tanggal awal, jadi batas bawah
				// tadi sedikit longgar. Saring ulang di sini supaya hasil benar-benar
				// berada dalam rentang yang diminta.
				Date waktu = rev.getRevisionDate();
				if (waktu == null || waktu.before(dari) || waktu.after(sampai)) continue;

				JSONObject j = new JSONObject();
				try {
					j.put("id", meta.getIdentifier(entitasBaris, EntityMode.POJO));
				} catch (Throwable abaikan) {
					// tanpa id baris tetap ditampilkan; kolomnya saja yang kosong.
				}
				j.put("rev", rev.getId());
				j.put("tanggal", fmt.format(waktu));
				j.put("tipe", tipe == RevisionType.ADD ? "TAMBAH"
						: tipe == RevisionType.DEL ? "HAPUS" : "UBAH");

				JSONObject ringkas = new JSONObject();
				for (int p = 0; p < props.length; p++) {
					if (tipeProp[p].isCollectionType() || tipeProp[p].isEntityType()) continue;
					try {
						Object nilai = meta.getPropertyValue(entitasBaris, props[p], EntityMode.POJO);
						if (nilai != null) ringkas.put(props[p], nilaiRingkas(nilai));
					} catch (Throwable abaikan) {
						// satu properti tak terbaca tidak boleh menggagalkan seluruh baris.
					}
				}
				// Relasi sengaja dibatasi beberapa nama saja: membuka SELURUH relasi utk
				// ratusan baris berarti ratusan query proxy, sedangkan yang dibutuhkan
				// penelusuran hanyalah penanda "punya siapa / di toko mana".
				for (int r = 0; r < RELASI_PENANDA.length; r++) {
					String nama = RELASI_PENANDA[r];
					if (!punyaProperti(meta, nama)) continue;
					try {
						Object relasi = meta.getPropertyValue(entitasBaris, nama, EntityMode.POJO);
						if (relasi == null) continue;
						ClassMetadata metaRelasi = HibernateUtil.getSessionFactory()
								.getClassMetadata(relasi.getClass());
						if (metaRelasi == null) continue;
						Serializable idRelasi = metaRelasi.getIdentifier(relasi, EntityMode.POJO);
						if (idRelasi != null) ringkas.put(nama, idRelasi);
					} catch (Throwable abaikan) {
						// relasi yang barisnya sudah lenyap -- dilewati, bukan digagalkan.
					}
				}
				j.put("ringkas", ringkas);
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("adaLagi", adaLagi ? Boolean.TRUE : Boolean.FALSE);
			hasil.put("bolehPulihkan", Common.getApakahAdminLain(tbmuser));
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
		if ("revisi_jelajah".equals(action)) { jelajah(tbmuser, request, hasil); return true; }
		if ("revisi_entitas".equals(action)) { entitas(tbmuser, request, hasil); return true; }
		return false;
	}
}
