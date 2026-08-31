package ais.database.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.DataUtil;
import ais.common.EntityIdentityMap;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Kelas dasar value object/model AIS untuk general value object. Tipe ini menyatukan identitas,
 * metadata umum, representasi, serta perilaku lintas entity yang benar-benar berlaku bagi
 * turunannya.
 *
 * <p><b>Kontrak identitas dan utilitas bersama:</b> {@link #equals(Object)} membandingkan {@code id} ketika kedua
 * object memilikinya; {@link #compareTo(GeneralValueObject)} mengurutkan berjenjang menurut nomor urut, NIM,
 * nama, lalu keterangan. {@link #check(Object)}, {@link #chek(Object)}, dan {@link #resolveLazy(Object)} adalah
 * alias resolusi proxy lazy yang dapat membuka session khusus untuk memuat ulang object detached. Operasi
 * {@code read/write/delete} mengelola cache JSON/file sementara milik object—bukan menghapus baris database.
 * Gunakan kontrak ini sebelum membuat helper identitas, lazy-loading, atau cache paralel pada entity turunan.</p>
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DataUtil}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code GeneralValueObject copyDari}, {@code
 * Long id}, {@code String kode}, {@code String nama}, {@code String nim}, {@code String keterangan}, {@code
 * Integer nomorUrut}, {@code String oleh}; inisialisasi/lifecycle ({@code isInitializedSafely()}, {@code
 * initializeWithAvailableSession()}, {@code reInitChecklistHasilPenilaianUmum()}, {@code
 * reInitIsiAngketParameterUmum()}); pembacaan/pencarian ({@code getJson()}, {@code getCopyDari()}, {@code
 * getId()}, {@code getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}); validasi/perhitungan ({@code
 * filterTidakBolehSederhana()}, {@code filterTidakBoleh()}, {@code closeSessionCreatedByCheckQuietly()}, {@code
 * check()}); mutasi data ({@code onUpdate()}, {@code setCopyDari()}, {@code setId()}, {@code setOlehId()},
 * {@code setOleh()}, {@code setTanggal_dirubah()}); penghapusan/pembatalan ({@code delete()}, {@code
 * removeIsiAngketParameterUmum()}); operasi domain lain ({@code clone()}, {@code toString()}, {@code chek()},
 * {@code resolveLazy()}, {@code equals()}, {@code compareTo()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see DataUtil
 */
public abstract class GeneralValueObject extends DataUtil
		implements Serializable, Cloneable, Comparable<GeneralValueObject> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3124378662823718355L;

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public GeneralValueObject getJson() {
		return this;
	}

	public GeneralValueObject clone() {
		GeneralValueObject generalValueObject = null;
		try {
			generalValueObject = (GeneralValueObject) super.clone();
		} catch (CloneNotSupportedException e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return generalValueObject;
	}

	protected abstract void onUpdate();

	private GeneralValueObject copyDari = null;

	public GeneralValueObject getCopyDari() {
		return copyDari;
	}

	public void setCopyDari(GeneralValueObject copyDari) {
		this.copyDari = copyDari;
	}

	public String toString() {
		return (getKode() == null ? "" : getKode() + " - ") + getNama();
	}

	public GeneralValueObject() {

	}

	public GeneralValueObject(Long id) {
		setId(id);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	private Long id;
	private String kode;
	private String nama;
	private String nim;
	private String keterangan;
	private Integer nomorUrut;

	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public static String filterTidakBolehSederhana(String isi) {
		try {
			isi = isi == null ? "" : isi;
			if (!isi.isEmpty()) {
				if (isi != null && isi.toLowerCase().contains("script")) {
					isi = isi.replaceAll("(?i)script", "__S__");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:153");
		}
		return isi;
	}

	public static String filterTidakBoleh(String isi) {
		try {
			isi = isi == null ? "" : isi;
			if (!isi.isEmpty()) {
				for (String s : ConstantValues.filter_tidak_boleh_ada.split(";")) {
					if (isi.toUpperCase().contains(s)) {
						isi = isi.toUpperCase().replaceAll(s, "");
					}
				}
//				isi = isi.replaceAll("\\<.*?\\>", "");
				if (isi != null && isi.toLowerCase().contains("script")) {
					isi = isi.replaceAll("(?i)script", "__S__");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:173");
		}
		return isi;
	}


	private boolean initData = false;

	private static String getCacheClassName(Object data) {
		if (data == null) {
			return null;
		}

		try {
			if (data instanceof HibernateProxy) {
				LazyInitializer initializer = ((HibernateProxy) data).getHibernateLazyInitializer();
				if (initializer != null && initializer.getPersistentClass() != null) {
					return initializer.getPersistentClass().getName();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:193");
			// Abaikan, lanjut fallback ke nama class biasa.
		}

		String className = data.getClass().getName();
		int proxyIndex = className.indexOf("_$$_");
		if (proxyIndex > 0) {
			return className.substring(0, proxyIndex);
		}

		proxyIndex = className.indexOf("$$");
		if (proxyIndex > 0) {
			return className.substring(0, proxyIndex);
		}

		proxyIndex = className.indexOf("_");
		if (proxyIndex > 0 && className.indexOf("ais.database.model.") == 0) {
			return className.substring(0, proxyIndex);
		}

		return className;
	}

	private static Serializable getCacheLookupId(GeneralValueObject object) {
		if (object == null) {
			return null;
		}

		try {
			if (object instanceof HibernateProxy) {
				LazyInitializer initializer = ((HibernateProxy) object).getHibernateLazyInitializer();
				if (initializer != null && initializer.getIdentifier() instanceof Serializable) {
					return (Serializable) initializer.getIdentifier();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:228");
			// Abaikan, lanjut fallback ke getter biasa.
		}

		try {
			if (object instanceof Tbmuser) {
				return ((Tbmuser) object).getUserId();
			} else if (object instanceof Tbmrole) {
				return ((Tbmrole) object).getRoleId();
			}
		} catch (Exception e) {
			// Proxy detached dapat error saat getter id non-standar dipanggil.
			return null;
		}

		try {
			return object.getId();
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean isInitializedSafely(Object data) {
		if (data == null) {
			return true;
		}
		try {
			return Hibernate.isInitialized(data);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean initializeWithAvailableSession(Object data) {
		if (data == null) {
			return true;
		}
		try {
			if (!Hibernate.isInitialized(data)) {
				Hibernate.initialize(data);
			}
			return Hibernate.isInitialized(data);
		} catch (Exception e) {
			return false;
		}
	}

	private static GeneralValueObject ambilDariCacheJikaAman(String cacheClassName, Serializable lookupId) {
		if (cacheClassName == null || cacheClassName.trim().length() == 0 || lookupId == null) {
			return null;
		}
		try {
			GeneralValueObject fetchedObject = ConstantValues.ambil(cacheClassName, lookupId, false);
			if (fetchedObject == null) {
				return null;
			}

			if (initializeWithAvailableSession(fetchedObject)) {
				fetchedObject.initData = true;
				return fetchedObject;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:289");
			// Cache tidak boleh membuat proses getter gagal.
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private static GeneralValueObject reloadDetachedObject(String cacheClassName, Serializable lookupId) {
		if (cacheClassName == null || cacheClassName.trim().length() == 0 || lookupId == null) {
			return null;
		}

		Session session = null;
		try {
			Class entityClass = Class.forName(cacheClassName);
			session = HibernateUtil.getSessionFactory().openSession();
			Object loaded = session.get(entityClass, lookupId);
			if (loaded instanceof GeneralValueObject) {
				try {
					Hibernate.initialize(loaded);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:309");
					// session.get(...) normalnya sudah menginisiasi entity utama.
				}

				GeneralValueObject loadedObject = (GeneralValueObject) loaded;
				loadedObject.initData = true;
				return loadedObject;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:317");
			/*
			 * Method check(...) dipanggil sangat sering dari getter entity. Jangan tampilkan
			 * error berulang-ulang di sini karena fallback ini hanya penjaga proxy detached.
			 */
		} finally {
			closeSessionCreatedByCheckQuietly(session);
		}
		return null;
	}

	private static void closeSessionCreatedByCheckQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:334");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:338");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:344");
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T check(T data) {
		if (data == null || !(data instanceof GeneralValueObject)) {
			return data;
		}

		GeneralValueObject object = (GeneralValueObject) data;
		try {
			if (object.initData && isInitializedSafely(object)) {
				return (T) object;
			}

			Serializable lookupId = getCacheLookupId(object);
			String cacheClassName = getCacheClassName(object);

			/*
			 * 0) EntityIdentityMap: satu Java object per entity ID di seluruh JVM.
			 *    Jika canonical sudah terdaftar (mis. NominalBiaya yang baru disimpan),
			 *    kembalikan langsung tanpa lazy-init atau query. Ini menjamin perubahan
			 *    scalar (mis. bukanTagihan=true) langsung terlihat oleh SEMUA pemegang
			 *    referensi ke entity dengan ID yang sama.
			 */
			if (lookupId instanceof Long && cacheClassName != null) {
				try {
					Class<?> realClass = Class.forName(cacheClassName);
					if (GeneralValueObject.class.isAssignableFrom(realClass)) {
						@SuppressWarnings("unchecked")
						GeneralValueObject canonical = EntityIdentityMap.get(
								(Class<? extends GeneralValueObject>) realClass, (Long) lookupId);
						if (canonical != null) {
							return (T) canonical;
						}
					}
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:381");
				}
			}

			/*
			 * 1) Coba ambil dari cache existing. Jika cache berisi object yang sudah aman,
			 *    kembalikan object tersebut agar tidak perlu query database.
			 */
			GeneralValueObject fetchedObject = ambilDariCacheJikaAman(cacheClassName, lookupId);
			if (fetchedObject != null) {
				return (T) fetchedObject;
			}

			/*
			 * 2) Jika object masih attached pada session aktif, initialize normal.
			 *    Jika sudah detached, bagian ini akan gagal diam-diam dan lanjut ke reload.
			 */
			if (initializeWithAvailableSession(object)) {
				object.initData = true;
				return (T) object;
			}

			/*
			 * 3) Inilah enhancement utama: saat proxy lazy sudah detached dan cache tidak
			 *    menyediakan object yang aman, baca ulang entity berdasarkan identifier
			 *    menggunakan openSession() khusus, initialize entity utamanya, lalu tutup
			 *    session di finally. Dengan begitu getter pemanggil menerima object asli,
			 *    bukan proxy detached yang memicu LazyInitializationException.
			 */
			GeneralValueObject reloadedObject = reloadDetachedObject(cacheClassName, lookupId);
			if (reloadedObject != null) {
				return (T) reloadedObject;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:414");
			// check(...) tidak boleh membuat getter entity gagal.
		}

		return data;
	}

	public static <T> T chek(T data) {
		return (T) check(data);
	}

	public static <T> T resolveLazy(T data) {
		return (T) check(data);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GeneralValueObject && getId() != null) {
			GeneralValueObject workspace = (GeneralValueObject) obj;
			if (workspace.getId() == null) {
				return super.equals(obj);
			} else {
				return getId().equals(workspace.getId());
			}
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {

			if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:456");

		}

		return 0;
	}

	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	public String getNim() {
		return nim;
	}

	public void setNim(String nim) {
		this.nim = nim;
	}

	public Integer getNomorUrut() {
		return nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	public JSONObject read() {
		if (this instanceof Tbmrole && ((Tbmrole) this).getRoleId() != null) {
			return Common.getJSONTemporary(this, ((Tbmrole) this).getRoleId());
		} else if (this instanceof Tbmuser && ((Tbmuser) this).getUserId() != null) {
			return Common.getJSONTemporary(this, ((Tbmuser) this).getUserId());
		} else if (getId() != null) {
			return Common.getJSONTemporary(this, getId().toString());
		}
		return new JSONObject();
	}

	public void delete() {
		if (this instanceof Tbmrole && ((Tbmrole) this).getRoleId() != null) {
			File file = Common.getFileLocation(this, ((Tbmrole) this).getRoleId());
			if (file != null && file.exists()) {
				BacaTulisUtil.hapus(file);
				boolean hapus = file.delete();
				System.out.println("Hapus " + file.getAbsolutePath() + " - " + hapus);
			}
		} else if (this instanceof Tbmuser && ((Tbmuser) this).getUserId() != null) {
			File file = Common.getFileLocation(this, ((Tbmuser) this).getUserId());
			if (file != null && file.exists()) {
				BacaTulisUtil.hapus(file);
				boolean hapus = file.delete();
				System.out.println("Hapus " + file.getAbsolutePath() + " - " + hapus);
			}
		} else if (getId() != null) {
			File file = Common.getFileLocation(this, getId().toString());
			if (file != null && file.exists()) {
				BacaTulisUtil.hapus(file);
				boolean hapus = file.delete();
				System.out.println("Hapus " + file.getAbsolutePath() + " - " + hapus);
			}
		}
	}

	public File write(String... strings) {
		Integer indexke = 0;
		return write(indexke, strings);
	}

	public File write(Integer indexke, String... strings) {
		if (((this instanceof PertemuanPunyaUjian) || (this instanceof Perkuliahan) || (this instanceof Skripsi)
				|| (this instanceof MahasiswaRequestTugasAkhir) || (this instanceof JadwalUjianPMB)
				|| (this instanceof PengumumanAkademis) || (this instanceof Ujian)) && getId() != null) {
			try {
				return Common.setJSONTemporary(this, getId().toString(),
						Common.convertToJsonObject(indexke, this, strings));
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:552");
			}
		} else if (!ConstantValues.classExist(this.getClass())) {
			if (this instanceof Tbmrole && ((Tbmrole) this).getRoleId() != null && indexke < 30) {
				try {
					return Common.setJSONTemporary(this, ((Tbmrole) this).getRoleId(),
							Common.convertToJsonObject(indexke, this, strings));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:560");
				}
			} else if (this instanceof Tbmuser && ((Tbmuser) this).getUserId() != null && indexke < 30) {
				try {
					return Common.setJSONTemporary(this, ((Tbmuser) this).getUserId(),
							Common.convertToJsonObject(indexke, this, strings));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:567");
				}
			} else if (getId() != null && indexke < 30) {
				try {
					return Common.setJSONTemporary(this, getId().toString(),
							Common.convertToJsonObject(indexke, this, strings));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:574");
				}
			}
		}

		String lokasiFileTemprorary = ConstantValues.ambilLokasiFileTemprorary(this.getClass());

		return new File(lokasiFileTemprorary + this.getClass().getName());
	}

	public boolean udah() {
		return udah("");
	}

	public void belum() {
		belum("");
	}

	public boolean udah(String tambahan) {
		try {

			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			File file = Common.getFileLocation(this, getClass().getName() + "_" + tambahan + "udah_" + id);
			String data = ais.common.BacaTulisUtil.baca(file);
			if (data == null || data.trim().isEmpty()) {
				ais.common.BacaTulisUtil.tulis(file, "true");
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:610");
		}
		return true;
	}

	public void belum(String tambahan) {
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			File file = Common.getFileLocation(this, getClass().getName() + "_" + tambahan + "udah_" + id);
			if (file != null && file.exists()) {
				BacaTulisUtil.hapus(file);
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:629");
		}
	}

	public void put(String data) {
		put(data, "");
	}

	public void put(String data, String tambahan) {
		// Saat STARTUP: LEWATI tulis-file. Hibernate memanggil SETTER ter-map saat HYDRATION
		// (TwoPhaseLoad.initializeEntity), mis. Mahasiswa.setBatasStudi -> put(...). Memuat
		// 1 entity = 1 tulis-file; saat init memuat RIBUAN entity -> ribuan tulis-file
		// (penyebab startup macet di thread "main", lihat thread dump). Mirror file tidak
		// perlu diperbarui saat load (kolom DB tetap jadi fallback getter). Setelah startup,
		// perilaku tulis normal kembali.
		if (ais.common.AppStartupListener.isStartupInProgress()) {
			return;
		}
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			if (id != null && !id.isEmpty()) {
				File file = Common.getFileLocation(this, getClass().getName() + "_data_" + tambahan + id);
				if (data == null) {
					ais.common.BacaTulisUtil.tulis(file, "");
					ais.common.BacaTulisUtil.hapus(file);
				} else {
					ais.common.BacaTulisUtil.tulis(file, data);
				}
				// jaga cache retreive tetap konsisten dengan tulisan terbaru
				retreiveCache().put(id + "|" + tambahan, data == null ? "" : data);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:666");
		}
	}

	// THREAD-SAFETY (lihat ConcurrentModificationException & "Unterminated string" audit
	// KRS/elearning, GeneralValueObject.putBaru/tulisPutBaru dipanggil dari AuditListener.
	// prosesUntukElearning): map ini di-share OLEH SELURUH THREAD/REQUEST dalam satu JVM
	// (bukan per-request), sengaja — dipakai sebagai akumulator "batch" ketika satu proses
	// sinkronisasi (mis. Dosen.singkronkanKrsMahasiswa) memanggil putBaru() berkali-kali
	// untuk entity yang sama (dosen/mahasiswa) SEBELUM tulisPutBaru() dipanggil sekali di
	// akhir (lihat voMahasiswaDosens di Dosen.java). Karena map & JSONObject di dalamnya
	// bukan thread-safe (HashMap biasa), DUA request BERBEDA yang kebetulan menyentuh
	// dosen/mahasiswa YANG SAMA secara bersamaan bisa saling mem-mutasi & men-serialize
	// JSONObject yang SAMA di saat bersamaan, menyebabkan:
	//  - ConcurrentModificationException saat toString() meng-iterasi HashMap yang sedang
	//    dimutasi thread lain, ATAU
	//  - file .json di disk tertimpa dua tulisan yang beririsan (masing-masing dari
	//    FileUtils.writeStringToFile terpisah tanpa penguncian) sehingga isinya terpotong
	//    di tengah string ("Unterminated string ...") saat dibaca ulang oleh putBaru().
	// Fix: ConcurrentHashMap (agar operasi get/put/remove pada map sendiri aman) DITAMBAH
	// synchronized(key.intern()) di kedua method supaya seluruh rangkaian get-or-create +
	// mutasi + serialize + tulis-berkas untuk SATU key (id+kelas+tambahan) tidak pernah
	// tumpang tindih antar-thread, sementara key BERBEDA (entity lain) tetap berjalan
	// paralel tanpa saling menunggu. ConcurrentHashMap TIDAK mengizinkan value null,
	// sehingga "penanda sudah ditulis" di tulisPutBaru() memakai remove(key), bukan
	// put(key, null) seperti sebelumnya.
	private static final Map<String, JSONObject> datatemporary = new java.util.concurrent.ConcurrentHashMap<String, JSONObject>();

	public void putBaru(String data, String tambahan) {
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			if (!id.isEmpty()) {

				String key = id + "_" + getClass().getSimpleName() + "_" + tambahan;
				synchronized (key.intern()) {
					JSONObject jsonObject = datatemporary.get(key);
					if (jsonObject == null) {
						try {
							File file = Common.getFileLocation(this, getClass().getSimpleName() + "_" + tambahan + "_put");
							String fileContent = ais.common.BacaTulisUtil.baca(file);
							jsonObject = fileContent == null || fileContent.trim().isEmpty() ? new JSONObject()
									: new JSONObject(fileContent);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:691");
							jsonObject = new JSONObject();
						}
						datatemporary.put(key, jsonObject);
					}
					jsonObject.put(data, "");
				}

			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:700");
		}
	}

	public void tulisPutBaru(String tambahan) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		} else if (this instanceof Tbmrole) {
			id = ((Tbmrole) this).getRoleId();
		}
		if (!id.isEmpty()) {
			String key = id + "_" + getClass().getSimpleName() + "_" + tambahan;
			synchronized (key.intern()) {
				JSONObject jsonObject = datatemporary.get(key);
				if (jsonObject != null) {
					try {
						File file = Common.getFileLocation(this, getClass().getSimpleName() + "_" + tambahan + "_put");
						ais.common.BacaTulisUtil.tulis(file, jsonObject.toString());
						datatemporary.remove(key);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:719");
//						e.printStackTrace();
					}
				}
			}
		}
	}

	public void bersihkanPutBaru(String tambahan) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		} else if (this instanceof Tbmrole) {
			id = ((Tbmrole) this).getRoleId();
		}
		if (!id.isEmpty()) {
			File file = Common.getFileLocation(this, getClass().getSimpleName() + "_" + tambahan + "_put");
			ais.common.BacaTulisUtil.tulis(file, new JSONObject().toString());
		}
	}

	public String retreiveBaru() {
		return retreiveBaru("");
	}

	public String retreiveBaru(String tambahan) {
		String data = "";
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			if (!id.isEmpty()) {
				File file = Common.getFileLocation(this, getClass().getSimpleName() + "_" + tambahan + "_put");
				String fileContent = ais.common.BacaTulisUtil.baca(file);
				JSONObject jsonObject;
				try {
					jsonObject = fileContent == null || fileContent.trim().isEmpty() ? new JSONObject()
							: new JSONObject(fileContent);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:760");
					jsonObject = new JSONObject();
				}
				data = jsonObject.isNull(id) ? "" : jsonObject.getString(id);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:766");
		}
		return data;
	}

	public String retreive() {
		return retreive("");
	}

	public String retreive(String tambahan) {
		// CACHE per-instance: hindari File.exists/baca berulang. Sebelumnya getter Hibernate
		// (mis. Dosen.getIdfinger -> retreive("idfinger")) memicu I/O file SETIAP auto-flush
		// untuk SETIAP entity dirty -> startup & operasi normal melambat parah (thread dump:
		// main macet di File.exists saat AppStartupListener init). Nilai yang dikembalikan
		// TETAP SAMA, hanya tidak membaca ulang dari disk pada pemanggilan berikutnya.
		String id = "";
		try {
			id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:789");
		}
		if (id == null || id.isEmpty()) {
			return "";
		}
		String cacheKey = id + "|" + tambahan;
		java.util.Map<String, String> cache = retreiveCache();
		String cached = cache.get(cacheKey);
		if (cached != null) {
			return cached;
		}
		// Saat STARTUP: lewati I/O file (nilai file tidak dibutuhkan untuk dirty-check
		// auto-flush Hibernate). JANGAN cache "" agar setelah startup nilai asli tetap terbaca.
		if (ais.common.AppStartupListener.isStartupInProgress()) {
			return "";
		}
		String data = "";
		try {
			File file = Common.getFileLocation(this, getClass().getName() + "_data_" + tambahan + id);
			data = ais.common.BacaTulisUtil.baca(file);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:809");
//			e.printStackTrace();
		}
		if (data == null) {
			data = "";
		}
		cache.put(cacheKey, data);
		return data;
	}

	/** Cache lazy per-instance untuk {@link #retreive(String)} (transient: tidak dipersist). */
	private transient java.util.Map<String, String> retreiveCacheMap;

	private java.util.Map<String, String> retreiveCache() {
		java.util.Map<String, String> m = retreiveCacheMap;
		if (m == null) {
			synchronized (this) {
				m = retreiveCacheMap;
				if (m == null) {
					m = new java.util.concurrent.ConcurrentHashMap<String, String>();
					retreiveCacheMap = m;
				}
			}
		}
		return m;
	}
	
	@SuppressWarnings("unchecked")
	public List<String> retreiveAll(String tambahan) {
		List<String> strings = new ArrayList<String>();
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			if (!id.isEmpty()) {

				String lokasiFileTemprorary = ConstantValues.ambilLokasiFileTemprorary(this.getClass());

				String fileLocation = lokasiFileTemprorary + this.getClass().getSimpleName() + "/" + this.getId() + "/"
						+ getClass().getSimpleName() + "_" + tambahan + "_put.json";
				File fileBaru = new File(fileLocation);

				String fileLocationUdah = lokasiFileTemprorary + this.getClass().getSimpleName() + "/" + this.getId()
						+ "/" + getClass().getSimpleName() + "_" + tambahan + "_put_udah.json";
				File fileBaruUdah = new File(fileLocationUdah);

				JSONObject jsonObject;
				try {
					String fileContent = ais.common.BacaTulisUtil.baca(fileBaru);
					jsonObject = fileContent == null || fileContent.trim().isEmpty() ? new JSONObject()
							: new JSONObject(fileContent);
				} catch (Exception e) {
					jsonObject = new JSONObject();
				}

				int size = jsonObject.length();
				
				// Memastikan flag DB ikut membaca dari Database, bukan cuma eksistensi file fisik
				boolean isUdah = fileBaruUdah.exists() || "1".equals(ais.common.BacaTulisUtil.baca(fileBaruUdah));

				if (size > 0 || isUdah) {
					Iterator<String> keys = jsonObject.keys();
					while (keys.hasNext()) {
						String key = keys.next();
						// FIX NumberFormatException di caller (mis. Dosen.ambilPerkuliahanDanParalel
						// yang langsung Long.parseLong(s)): key kosong/blank tidak boleh diteruskan,
						// konsisten dengan guard !data.trim().isEmpty() pada 2 cabang lain method ini.
						if (key != null && !key.trim().isEmpty()) {
							strings.add(key);
						}
					}
					jsonObject = null;
				} else {
					String key = getClass().getName() + "_data_" + tambahan;
					File file = Common.getFileLocation(this, key + id);
					File parentFolder = file.getParentFile();
					
					// =========================================================================
					// CEK FLAG: DATABASE vs CARA LAMA (FILE SYSTEM)
					// =========================================================================
					if (ais.common.BacaTulisUtil.flagDataMenggunakandatabase) {
						
						// CARA BARU (Memanggil Query dari Database melalui BacaTulisUtil)
						List<String> dataList = ais.common.BacaTulisUtil.ambilSemuaValueDenganPrefixFile(parentFolder, key);
						for (String data : dataList) {
							try {
								if (data != null && !data.trim().isEmpty()) {
									if (data.endsWith("json")) {
										String[] argv = org.apache.commons.lang.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(data, "\\", "_"), "/", "_").split("_");
										data = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
									}
									putBaru(data, tambahan);
									strings.add(data);
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:902");
							}
						}
						
					} else {
						
						// CARA LAMA (Murni membaca dari File System)
						if (parentFolder != null && parentFolder.exists() && parentFolder.isDirectory()) {
							File[] files = parentFolder.listFiles();
							
							// Proteksi NullPointerException jika folder tidak dapat diakses OS
							if (files != null) {
								for (File s : files) {
									String data = "";
									try {
										if (s.getName().toLowerCase().startsWith(key.toLowerCase())) {
											data = ais.common.BacaTulisUtil.baca(s);
											if (!data.trim().isEmpty()) {
												if (data.endsWith("json")) {
													String[] argv = org.apache.commons.lang.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(data, "\\", "_"), "/", "_").split("_");
													data = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
												}
												putBaru(data, tambahan);
												strings.add(data);
											}
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:929");
									}
								}
							}
						}
						
					}
					
					ais.common.BacaTulisUtil.tulis(fileBaruUdah, "1");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:941");
		}
		return strings;
	}
	
	
	public void masukkanData(String jenis) {
		Tbmuser tbmuser = Common.getCurrentUser();
		masukkanData(jenis, tbmuser);
	}

	public void masukkanData(String jenis, Tbmuser tbmuser) {

		if (tbmuser != null) {
			String user = tbmuser
					.getCalonSiswa() != null
							? (tbmuser.getCalonSiswa().getNama() == null ? ""
									: tbmuser.getCalonSiswa().getNama().replaceAll("-", "")) + "-"
									+ tbmuser.getCalonSiswa().getId() + "-CalonSiswa"
							: tbmuser
									.getBiodataCalonMahasiswa() != null
											? (tbmuser.getBiodataCalonMahasiswa().getNama() == null ? ""
													: tbmuser.getBiodataCalonMahasiswa().getNama().replaceAll("-", ""))
													+ "-" + tbmuser.getBiodataCalonMahasiswa().getId()
													+ "-CalonMahasiswa"
											: tbmuser
													.getMahasiswa() != null
															? (tbmuser.getMahasiswa().getNama() == null ? ""
																	: tbmuser.getMahasiswa().getNama().replaceAll("-",
																			""))
																	+ "-" + tbmuser.getMahasiswa().getId()
																	+ "-Mahasiswa"
															: (tbmuser.ambilDosen() != null
																	? (tbmuser.ambilDosen().getNama() == null
																			? ""
																			: tbmuser
																					.getDosen().getNama()
																					.replaceAll("-", ""))
																			+ "-" + tbmuser.getDosen().getId()
																			+ "-Dosen"
																	: tbmuser.getSiswa() != null
																			? (tbmuser.getSiswa().getNama() == null
																					? ""
																					: tbmuser
																							.getSiswa().getNama()
																							.replaceAll("-", ""))
																					+ "-" + tbmuser.getSiswa().getId()
																					+ "-Siswa"
																			: tbmuser.getSiswa() != null ? (tbmuser
																					.ambilGuru().getNama() == null
																							? ""
																							: tbmuser.ambilGuru()
																									.getNama()
																									.replaceAll("-",
																											""))
																					+ "-" + tbmuser.getGuru().getId()
																					+ "-Guru"
																					: tbmuser.ambilPegawai() != null
																							? (tbmuser.ambilPegawai()
																									.getNama() == null
																											? ""
																											: tbmuser
																													.getPegawai()
																													.getNama()
																													.replaceAll(
																															"-",
																															""))
																									+ "-"
																									+ tbmuser
																											.getPegawai()
																											.getId()
																									+ "-Pegawai"
																							: tbmuser.getUserNama()
																									+ "-1-" + tbmuser
																											.hakAkses());
//			System.out.println("Masukkan data -> " + user);
			if (!user.isEmpty()) {
				masukkanData(jenis, user, Common.dateFormat3.get().format(ais.ui.util.WaktuUtil.getDate()));
			}
		} else {
			masukkanData(jenis, "User", Common.dateFormat3.get().format(ais.ui.util.WaktuUtil.getDate()));
		}
	}

	public void masukkanData(String jenis, String user, String jam) {

		try {

			if (user != null && jam != null) {
				jam = org.apache.commons.lang3.StringUtils.replace(jam, ",", ".");
				user = org.apache.commons.lang3.StringUtils.replace(user, ",", ".");
				jenis = org.apache.commons.lang3.StringUtils.replace(jenis, ",", ".");
				String[] nilais = retreive().split(";");
				Boolean ada = false;
				String formatBaru = "";
				for (String nn : nilais) {
					try {
						String aformatBaru = "";
						String[] s = nn.split(",");
						if (!s[0].trim().isEmpty()) {
							String userId = s[0];
							String jenisId = s[1];
							if (user.equalsIgnoreCase(userId) && jenis.equalsIgnoreCase(jenisId)) {
								aformatBaru = user + "," + jenis + "," + jam;
								ada = true;
							} else {
								aformatBaru = nn;
							}
							if (!aformatBaru.trim().isEmpty()) {
								formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (!ada) {
					String aformatBaru = user + "," + jenis + "," + jam;
					formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
				}

				// System.out.println("masukkan data => " + formatBaru);

				put(formatBaru);
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1068");
			// TODO: handle exception
		}
	}

	public boolean apakahSedang(String jenis) {
		int jumlah = 0;
		TreeMap<String, String> d = ambilData(jenis, null);
		for (String user : d.keySet()) {
			try {
				String[] u = user.split("-");
				if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
						|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
						|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
					jumlah++;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1085");
			}
		}
		d = null;
		return jumlah > 0;
	}

	public TreeMap<String, String> ambilData(String jenis, String user) {
		return ambilData(jenis, user, "", null, null);
	}

	public TreeMap<String, String> ambilData(String jenis, String user, Date mulai, Date sampai) {
		return ambilData(jenis, user, "", mulai, sampai);
	}

	public TreeMap<String, String> ambilData(String jenis, String user, String posfix) {
		return ambilData(jenis, user, posfix, null, null);
	}

	public TreeMap<String, String> ambilData(String jenis, String user, String posfix, Date mulai, Date sampai) {
		return ambilData(jenis, user, posfix, mulai, sampai,
				new String[] { "Mahasiswa", "CalonMahasiswa", "Dosen", "Siswa", "CalonSiswa", "Guru" });
	}

	public TreeMap<String, String> ambilData(String jenis, String user, String posfix, Date mulai, Date sampai,
			String[] jenisPengguna) {
		TreeMap<String, String> treeMap = new TreeMap<String, String>();
		try {
			jenis = org.apache.commons.lang3.StringUtils.replace(jenis, ",", ".");
			String[] nilais = retreive().split(";");

			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					// fallback aman: data tidak lengkap (kurang dari 3 bagian setelah split) dilewati,
					// bukan melempar ArrayIndexOutOfBoundsException saat mengakses s[1]/s[2].
					if (s.length > 2 && !s[0].trim().isEmpty()) {
						String userId = s[0];
						String jenisId = s[1];
						String jamId = s[2];

						if (jenisPengguna != null) {
							try {
								boolean ada = false;
								String[] u = userId.split("-");
								// fallback aman: bila userId tidak punya bagian ke-3 (mis. tanpa "-tipe"),
								// anggap tipe kosong (tidak akan cocok dengan jenisPengguna manapun)
								// alih-alih melempar ArrayIndexOutOfBoundsException.
								String tipe = u.length > 2 ? u[2] : "";
								for (String d : jenisPengguna) {
									if (d.trim().equalsIgnoreCase(tipe.trim())) {
										ada = true;
										break;
									}
								}
								if (!ada) {
									continue;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1138");
							}
						}

						if (jamId != null && !jamId.trim().isEmpty() && mulai != null && sampai != null) {
							try {
								Date waktu = Common.dateFormat3.get().parse(jamId);
								boolean masuk = mulai.before(waktu) && sampai.after(waktu);
								// System.out.println("ambilData " + jenis + " user
								// " + user + " mulai "
								// + Common.dateFormat3.get().format(mulai) + " s.d " +
								// Common.dateFormat3.get().format(sampai)
								// + ", dibandingkan dengan " + waktu + ", masuk " +
								// masuk);
								if (!masuk) {
									continue;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1155");
							}
						} else if (jamId != null && !jamId.trim().isEmpty() && mulai != null) {
							try {
								Date waktu = Common.dateFormat3.get().parse(jamId);
								boolean masuk = mulai.before(waktu);
								// System.out.println("ambilData " + jenis + " user
								// " + user + " mulai "
								// + Common.dateFormat3.get().format(mulai) + ",
								// dibandingkan dengan " + waktu + ", masuk "
								// + masuk);
								if (!masuk) {
									continue;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1169");
							}
						} else if (jamId != null && !jamId.trim().isEmpty() && sampai != null) {
							try {
								Date waktu = Common.dateFormat3.get().parse(jamId);
								boolean masuk = sampai.after(waktu);
								// System.out.println("ambilData " + jenis + " user
								// " + user + " sampai "
								// + Common.dateFormat3.get().format(sampai) + ",
								// dibandingkan dengan " + waktu + ", masuk "
								// + masuk);
								if (!masuk) {
									continue;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1183");
							}
						}

						if (user != null && !user.trim().isEmpty() && jenisId.startsWith(jenis)) {
							String[] u = userId.split("-");
							try {
								String id = u[1];
								if (id.equalsIgnoreCase(user)) {
									treeMap.put(userId + jenisId + posfix, jamId);
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1194");
							}
						} else if (jenis.equalsIgnoreCase(jenisId)) {
							treeMap.put(userId + posfix, jamId);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1201");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1205");
		}
		return treeMap;
	}

	public String ambilLokasiChecklistHasilPenilaianUmum(Long pertemuanId, String userId) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "checklist_hasil_penilaian_umum_" + id
				+ (userId == null ? "" : "_" + userId) + (pertemuanId == null ? "" : "_" + pertemuanId));
		try {
			// System.out.println("baca file dari " + file.getAbsolutePath());
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1221");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiChecklistHasilPenilaianUmum(String data, Long pertemuanId, String userId) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "checklist_hasil_penilaian_umum_" + id
				+ (userId == null ? "" : "_" + userId) + (pertemuanId == null ? "" : "_" + pertemuanId));
		try {
			// System.out.println("tulis file ke " + file.getAbsolutePath());
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1236");
			// TODO Auto-generated catch block

		}
	}

	public void bersihkanLokasiChecklistHasilPenilaianUmum(Long pertemuanId, String userId) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "checklist_hasil_penilaian_umum_" + id
				+ (userId == null ? "" : "_" + userId) + (pertemuanId == null ? "" : "_" + pertemuanId));
		BacaTulisUtil.doHapus(file, "checklist_hasil_penilaian_umum");

	}

	@SuppressWarnings("unchecked")
	public void reInitChecklistHasilPenilaianUmum(Session session, Long pertemuanId, String userId) {

		String kolomName = "";
		if (this instanceof Tbmuser) {
			kolomName = "tbmuser";
		} else if (this instanceof Dosen) {
			kolomName = "dosen";
		} else if (this instanceof Mahasiswa) {
			kolomName = "mahasiswa";
		} else if (this instanceof Siswa) {
			kolomName = "siswa";
		} else if (this instanceof Guru) {
			kolomName = "guru";
		}

		if (!kolomName.trim().isEmpty()) {

			List<Long> checklistHasilPenilaianUmumsId = session.createCriteria(ChecklistHasilPenilaianUmum.class)
					.add(userId == null ? Restrictions.isNull("tbmuserDinilai")
							: Restrictions.eq("tbmuserDinilai.userId", userId))
					.add(pertemuanId == null ? Restrictions.isNull("pertemuanId")
							: Restrictions.eq("pertemuanId", pertemuanId))
					.addOrder(Order.asc("id")).setProjection(Projections.property("id"))
					.add(Restrictions.eq(kolomName, this)).list();

//			System.out.println("checklistHasilPenilaianUmumsId ->  " + checklistHasilPenilaianUmumsId + " userId "
//					+ userId + " this " + this);

			bersihkanLokasiChecklistHasilPenilaianUmum(pertemuanId, userId);
			tulisLokasiChecklistHasilPenilaianUmum(new JSONObject().toString(), pertemuanId, userId);
			for (Long checklistHasilPenilaianUmumId : checklistHasilPenilaianUmumsId) {
				ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum = (ChecklistHasilPenilaianUmum) ambilData(
						ChecklistHasilPenilaianUmum.class, checklistHasilPenilaianUmumId.toString());
				if (checklistHasilPenilaianUmum == null) {
					checklistHasilPenilaianUmum = (ChecklistHasilPenilaianUmum) session
							.createCriteria(ChecklistHasilPenilaianUmum.class)
							.add(Restrictions.idEq(checklistHasilPenilaianUmumId)).uniqueResult();
					masukkanData(ChecklistHasilPenilaianUmum.class, checklistHasilPenilaianUmum);
				}
				populateChecklistHasilPenilaianUmum(checklistHasilPenilaianUmum, pertemuanId, userId);

			}
			checklistHasilPenilaianUmumsId = null;
		}
	}

	public void removeChecklistHasilPenilaianUmum(Serializable id, Long pertemuanId, String userId) {
		try {
			JSONObject c = new JSONObject(ambilLokasiChecklistHasilPenilaianUmum(pertemuanId, userId));
			c.put(id.toString(), "");
			tulisLokasiChecklistHasilPenilaianUmum(c.toString(), pertemuanId, userId);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1305");

		}
	}

	public void populateChecklistHasilPenilaianUmum(ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum,
			Long pertemuanId, String userId) {
		try {
			if (checklistHasilPenilaianUmum == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiChecklistHasilPenilaianUmum(pertemuanId, userId));
			c.put(checklistHasilPenilaianUmum.getId().toString(), checklistHasilPenilaianUmum.getId().toString());
			tulisLokasiChecklistHasilPenilaianUmum(c.toString(), pertemuanId, userId);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1320");
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<ChecklistHasilPenilaianUmum> ambilChecklistHasilPenilaianUmum(Session session, Long pertemuanId,
			String userId, boolean refresh) {
		if (refresh || !udah("ChecklistHasilPenilaianUmum_baru" + (userId == null ? "" : "_" + userId)
				+ (pertemuanId == null ? "" : "_" + pertemuanId))) {
			reInitChecklistHasilPenilaianUmum(session, pertemuanId, userId);
		}

		Map<Long, ChecklistHasilPenilaianUmum> maps = new HashMap<Long, ChecklistHasilPenilaianUmum>();
		try {
			JSONObject c = new JSONObject(ambilLokasiChecklistHasilPenilaianUmum(pertemuanId, userId));
			Iterator<String> keys = c.keys();
			Set<String> keyData = new HashSet<String>();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						keyData.add(key);
						ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum = (ChecklistHasilPenilaianUmum) ambilData(
								ChecklistHasilPenilaianUmum.class, key, true);
						if (checklistHasilPenilaianUmum != null
								&& (pertemuanId == null || (checklistHasilPenilaianUmum.getPertemuanId() != null
										&& pertemuanId.equals(checklistHasilPenilaianUmum.getPertemuanId())))

								&& (userId == null || (checklistHasilPenilaianUmum.getTbmuserDinilai() != null
										&& checklistHasilPenilaianUmum.getTbmuserDinilai().getUserId() != null
										&& userId.equals(checklistHasilPenilaianUmum.getTbmuserDinilai().getUserId())))

								&& checklistHasilPenilaianUmum.getChecklistPenilaianUmum() != null) {
							if (this instanceof Tbmuser) {
								checklistHasilPenilaianUmum.setTbmuser((Tbmuser) this);
							} else if (this instanceof Dosen) {
								checklistHasilPenilaianUmum.setDosen((Dosen) this);
							} else if (this instanceof Mahasiswa) {
								checklistHasilPenilaianUmum.setMahasiswa((Mahasiswa) this);
							} else if (this instanceof Guru) {
								checklistHasilPenilaianUmum.setGuru((Guru) this);
							} else if (this instanceof Siswa) {
								checklistHasilPenilaianUmum.setSiswa((Siswa) this);
							}
							maps.put(checklistHasilPenilaianUmum.getId(), checklistHasilPenilaianUmum);
						}

					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1370");
				}
			}
			System.out.println("maps data ->  " + maps + " keys " + keyData + " c " + c);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1375");
		}

		return maps.values();
	}

	public String ambilLokasiIsiAngketParameterUmum() {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "isi_angket_parameter_umum_" + id);
		try {
			System.out.println("baca file dari " + file.getAbsolutePath());
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1391");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiIsiAngketParameterUmum(String data) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "isi_angket_parameter_umum_" + id);
		try {
			System.out.println("tulis file ke " + file.getAbsolutePath());
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1405");
			// TODO Auto-generated catch block

		}
	}

	public void bersihkanLokasiIsiAngketParameterUmum() {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "isi_angket_parameter_umum_" + id);
		BacaTulisUtil.doHapus(file, "isi_angket_parameter_umum");

	}

	@SuppressWarnings("unchecked")
	public void reInitIsiAngketParameterUmum(Session session) {

		String kolomName = "";
		if (this instanceof Tbmuser) {
			kolomName = "tbmuser";
		} else if (this instanceof Dosen) {
			kolomName = "dosen";
		} else if (this instanceof Mahasiswa) {
			kolomName = "mahasiswa";
		}

		if (!kolomName.trim().isEmpty()) {

			List<IsiAngketParameterUmum> isiAngketParameterUmums = session.createCriteria(IsiAngketParameterUmum.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq(kolomName, this)).list();
			bersihkanLokasiIsiAngketParameterUmum();
			tulisLokasiIsiAngketParameterUmum(new JSONObject().toString());
			for (IsiAngketParameterUmum isiAngketParameterUmum : isiAngketParameterUmums) {
				populateIsiAngketParameterUmum(isiAngketParameterUmum);
			}
			isiAngketParameterUmums = null;
		}
	}

	public void removeIsiAngketParameterUmum(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiIsiAngketParameterUmum());
			c.put(id.toString(), "");
			tulisLokasiIsiAngketParameterUmum(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1451");

		}
	}

	public void populateIsiAngketParameterUmum(IsiAngketParameterUmum isiAngketParameterUmum) {
		try {
			if (isiAngketParameterUmum == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiIsiAngketParameterUmum());
			c.put(isiAngketParameterUmum.getId().toString(), isiAngketParameterUmum.write().getAbsolutePath());
			tulisLokasiIsiAngketParameterUmum(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1465");
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<IsiAngketParameterUmum> ambilIsiAngketParameterUmum(Session session, boolean refresh) {
		if (refresh || !udah("IsiAngketParameterUmum")) {
			reInitIsiAngketParameterUmum(session);
		}

		Map<Long, IsiAngketParameterUmum> maps = new HashMap<Long, IsiAngketParameterUmum>();
		try {
			JSONObject c = new JSONObject(ambilLokasiIsiAngketParameterUmum());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						File file = new File(s);
						if (file != null && file.exists()) {
							IsiAngketParameterUmum isiAngketParameterUmum = (IsiAngketParameterUmum) Common
									.convertToObject(new JSONObject(ais.common.BacaTulisUtil.baca(file)),
											IsiAngketParameterUmum.class);
							if (isiAngketParameterUmum != null
									&& isiAngketParameterUmum.getJadwalChecklistPenilaianUmum() != null) {
								if (this instanceof Tbmuser) {
									isiAngketParameterUmum.setTbmuser((Tbmuser) this);
								} else if (this instanceof Dosen) {
									isiAngketParameterUmum.setDosen((Dosen) this);
								} else if (this instanceof Mahasiswa) {
									isiAngketParameterUmum.setMahasiswa((Mahasiswa) this);
								}
								maps.put(isiAngketParameterUmum.getJadwalChecklistPenilaianUmum().getId(),
										isiAngketParameterUmum);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1504");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1508");
		}

		return maps.values();
	}

	public static void tampilKunci(Component toolbar, VoKunci voKunci, Tbmuser tbmuser, EventListener eventListener) {
		tampilKunci(toolbar, voKunci, tbmuser, eventListener, true);
	}

	public static void tampilKunci(Component toolbar, final VoKunci voKunci, Tbmuser tbmuser,
			final EventListener eventListener, final boolean tampilLabel) {
		final MyToolbarbuttonConfig bukaKunci = new MyToolbarbuttonConfig(tampilLabel ? "Buka" : "",
				"/img/svg/unlock.svg");
		final MyToolbarbuttonConfig kunci = new MyToolbarbuttonConfig(tampilLabel ? "Kunci" : "", "/img/svg/lock.svg");

		bukaKunci.setTooltiptext("Buka kunci");
		kunci.setTooltiptext("Tutup kunci");

		if (tbmuser.getSiswa() == null && voKunci != null) {

			kunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengunci data ini ?.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										voKunci.setDikunci(Common.getCurrentUser());
										Common.refreshUpdate(voKunci);

										kunci.setVisible(voKunci.getDikunci() == null);
										bukaKunci.setVisible(voKunci.getDikunci() != null);
										if (voKunci.getDikunci() != null && tampilLabel) {
											bukaKunci.setLabel(
													"Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
										}
										if (voKunci.getDikunci() != null) {
											bukaKunci.setTooltiptext(
													"Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
										}
										Common.createDefaultTimer(eventListener);
									}

								}
							});
				}
			});

			kunci.setVisible(voKunci.getDikunci() == null);
			kunci.setDisabled(!Common.getApakahAdminBolehKunci());

			bukaKunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membuka kunci data ini ?.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										voKunci.setDikunci(null);
										Common.refreshUpdate(voKunci);

										kunci.setVisible(voKunci.getDikunci() == null);
										bukaKunci.setVisible(voKunci.getDikunci() != null);

										Common.createDefaultTimer(eventListener);
									}

								}
							});
				}
			});
			bukaKunci.setVisible(voKunci.getDikunci() != null);
			if (voKunci.getDikunci() != null && tampilLabel) {
				bukaKunci.setLabel("Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
			}

			if (voKunci.getDikunci() != null) {
				bukaKunci.setTooltiptext("Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
			}
			bukaKunci.setDisabled((voKunci.getDikunci() != null && Common.getCurrentUser().getUserId() != null
					&& !voKunci.getDikunci().getUserId().equals(Common.getCurrentUser().getUserId()))

					|| !Common.getApakahAdminBolehKunci());

			// Tambahkan ke popup kebab jika tersedia; fallback ke toolbar langsung.
			Object popupAttrKunci = toolbar.getAttribute("ais_row_actions_popup");
			if (popupAttrKunci instanceof org.zkoss.zul.Div) {
				org.zkoss.zul.Div popupContent = (org.zkoss.zul.Div) popupAttrKunci;
				org.zkoss.zul.Div divider = new org.zkoss.zul.Div();
				divider.setSclass("ais-row-popup-divider");
				divider.setParent(popupContent);
				kunci.setSclass("ais-row-popup-item ais-row-popup-item-kunci");
				bukaKunci.setSclass("ais-row-popup-item ais-row-popup-item-kunci");
				// Pastikan label selalu tampil di menu popup
				if (voKunci.getDikunci() != null) {
					bukaKunci.setLabel("Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
				} else {
					bukaKunci.setLabel("Buka Kunci");
				}
				kunci.setLabel("Kunci Data");
				kunci.setParent(popupContent);
				bukaKunci.setParent(popupContent);
			} else {
				kunci.setSclass("ais-row-action-btn ais-row-action-kunci");
				kunci.setOrient("vertical");
				kunci.setStyle("font-size:9px;");
				bukaKunci.setSclass("ais-row-action-btn ais-row-action-kunci");
				bukaKunci.setOrient("vertical");
				bukaKunci.setStyle("font-size:9px;");
				kunci.setParent(toolbar);
				bukaKunci.setParent(toolbar);
			}

		}
	}
	
	
	private static boolean isNilaiKosong(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String) {
			return ((String) value).trim().isEmpty();
		}
		if (value instanceof GeneralValueObject) {
			return ((GeneralValueObject) value).getId() == null;
		}
		return false;
	}

	private static boolean isNilaiSama(Object valData, Object expData) {
		if (valData instanceof GeneralValueObject && expData instanceof GeneralValueObject) {
			GeneralValueObject vObj = (GeneralValueObject) valData;
			GeneralValueObject eObj = (GeneralValueObject) expData;
			return vObj.getId() != null && eObj.getId() != null && vObj.getId().equals(eObj.getId());
		}
		if (valData instanceof java.util.Date && expData instanceof java.util.Date) {
			return ais.common.Common.dateFormat3.get().format((java.util.Date) valData)
					.equals(ais.common.Common.dateFormat3.get().format((java.util.Date) expData));
		}
		if (valData instanceof Number && expData instanceof Number) {
			return Double.compare(((Number) valData).doubleValue(), ((Number) expData).doubleValue()) == 0;
		}
		return valData.toString().trim().equalsIgnoreCase(expData.toString().trim());
	}

	@SuppressWarnings("rawtypes")
	public static GeneralValueObject ambilSatuData(Class clazz, List<? extends GeneralValueObject> generalValueObjects,
			String[] properties, Object[] datas) {

		if (generalValueObjects == null || generalValueObjects.isEmpty()) {
			return null;
		}

		GeneralValueObject bestMatch = null;
		int highestScore = -1;

		org.hibernate.metadata.ClassMetadata classMetadata = null;
		try {
			classMetadata = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
		} catch (Exception e) {
			classMetadata = HibernateUtil.getClassMetadata(clazz);
		}

		if (classMetadata == null) {
			return null;
		}

		int panjangFilter = properties == null || datas == null ? 0 : Math.min(properties.length, datas.length);
		for (GeneralValueObject d : generalValueObjects) {
			if (d == null) continue;

			int currentScore = 0;
			boolean isEligible = true;

			for (int i = 0; i < panjangFilter; i++) {
				String property = properties[i];
				if (property == null || property.trim().isEmpty()) {
					continue;
				}
				Object expData = datas[i];

				Object valData = null;
				try {
					valData = classMetadata.getPropertyValue(d, property, org.hibernate.EntityMode.POJO);
				} catch (Exception e) {
					isEligible = false;
					break;
				}

				if (isNilaiKosong(valData)) {
					continue;
				}

				if (isNilaiKosong(expData)) {
					isEligible = false;
					break;
				}

				if (isNilaiSama(valData, expData)) {
					currentScore++;
				} else {
					isEligible = false;
					break;
				}
			}

			if (isEligible && currentScore > highestScore) {
				highestScore = currentScore;
				bestMatch = d;
			}
		}

		return bestMatch;
	}
}
