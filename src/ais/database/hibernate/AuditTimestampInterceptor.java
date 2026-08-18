package ais.database.hibernate;

import java.io.Serializable;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.type.Type;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.common.Common;
import ais.common.EntityIdentityMap;
import ais.common.RequestContext;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

public class AuditTimestampInterceptor extends EmptyInterceptor {

	public static AuditTimestampInterceptor instance = new AuditTimestampInterceptor();

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * [WAJIB — Titik masuk utama] Hibernate minta instance baru untuk entity yang akan
	 * dimuat dari DB. Jika canonical sudah ada di EntityIdentityMap, kembalikan dia;
	 * Hibernate akan mengisi ulang semua field-nya dengan data segar dari DB.
	 *
	 * Akibatnya: pemanggil (session.get, criteria.list, lazy-init proxy, dsb.)
	 * langsung mendapat canonical — BUKAN instance baru yang berbeda.
	 * Satu object per ID di JVM terjamin dari sisi load.
	 */
	@Override
	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) {
		if (EntityMode.POJO.equals(entityMode) && id instanceof Long) {
			try {
				Class<?> clazz = Class.forName(entityName);
				if (GeneralValueObject.class.isAssignableFrom(clazz)) {
					@SuppressWarnings("unchecked")
					GeneralValueObject canonical = EntityIdentityMap.get(
							(Class<? extends GeneralValueObject>) clazz, (Long) id);
					// JANGAN kembalikan HibernateProxy sebagai implementasi: bila canonical adalah proxy
					// yang sama dengan proxy yang sedang di-inisialisasi, ia menjadi implementasi dirinya
					// sendiri → rekursi getId() tak henti (StackOverflowError). Hanya kembalikan instance
					// asli (non-proxy) sebagai canonical; selain itu biarkan Hibernate membuat instance baru.
					if (canonical != null && !(canonical instanceof org.hibernate.proxy.HibernateProxy)) {
						// Hibernate isi ulang semua field canonical dengan data DB terbaru.
						// Session lama tidak relevan: semua asosiasi lazy di-proxy ulang
						// untuk session aktif.
						return canonical;
					}
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTimestampInterceptor.java:62");
				// Class.forName gagal (bukan entity kita) — biarkan Hibernate buat sendiri
			}
		}
		return null; // null = Hibernate buat instance baru via no-arg constructor
	}

	/**
	 * Setelah Hibernate memuat entitas dari DB, daftarkan ke EntityIdentityMap.
	 * Untuk load PERTAMA (canonical belum ada): entity ini menjadi canonical.
	 * Untuk load berikutnya (instantiate() sudah kembalikan canonical): entity == canonical,
	 * tidak ada kerja ekstra.
	 */
	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) {
		if (entity instanceof GeneralValueObject && id instanceof Long) {
			EntityIdentityMap.canonical((GeneralValueObject) entity);
			// Titik LOAD HIBERNATE SEBENARNYA (query/lazy-init nyata ke DB, bukan cache-hit generik
			// DataUtil) -- dicatat ke EntityAccessCache sbg riwayat akses 3-hari, dipakai
			// InitDataHelper.doInitData utk preload boot selanjutnya. EntityAccessCache.catat sendiri
			// yang memfilter kelas relevan & skip bila thread ini sedang preload/batch massal
			// (EntityAccessCache.sedangPreload) -- lihat javadoc di sana.
			try {
				String namaKelas = org.apache.commons.lang.StringUtils.split(entity.getClass().getName(), "_")[0];
				ais.common.EntityAccessCache.catat(namaKelas, id.toString());
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTimestampInterceptor.java:88");
			}
		}
		return false;
	}

	/**
	 * Saat Hibernate flush entity yang dirty (akan di-UPDATE ke DB), pastikan entity
	 * tersebut terdaftar sebagai canonical. Menangkap kasus update yang melewati
	 * CommonHibernateHelper (mis. direct session.update()).
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		if (entity instanceof GeneralValueObject && id instanceof Long) {
			EntityIdentityMap.canonical((GeneralValueObject) entity);
		}
		// Lanjutkan ke logika audit timestamp yang sudah ada
		return onFlushDirtyAudit(entity, id, currentState, previousState, propertyNames, types);
	}

	/**
	 * Setelah INSERT baru di-flush (ID sudah di-generate DB), daftarkan entity baru
	 * ke EntityIdentityMap. postFlush dipanggil setelah semua SQL flush selesai.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void postFlush(Iterator entities) {
		try {
			while (entities.hasNext()) {
				Object entity = entities.next();
				if (entity instanceof GeneralValueObject) {
					GeneralValueObject gvo = (GeneralValueObject) entity;
					if (gvo.getId() != null) {
						EntityIdentityMap.canonical(gvo);
					}
				}
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTimestampInterceptor.java:127");
		}
	}

	private boolean onFlushDirtyAudit(Object entity, Serializable id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {

		if (!(entity instanceof GeneralValueObject)) {
			AuditTrailHelper.debug("AuditTimestampInterceptor.skip non-GeneralValueObject " + AuditTrailHelper.describeEntity(entity, id));
			return false;
		}

		Boolean updateDecision = AuditTrailHelper.peekUpdateDecision(entity, id);
		boolean hasBusinessChange = updateDecision == null
				? AuditTrailHelper.hasBusinessChange(currentState, previousState, propertyNames)
				: updateDecision.booleanValue();

		if (!hasBusinessChange) {
			/*
			 * Jika yang berubah hanya tanggal_dirubah/olehId/oleh, jangan biarkan
			 * Hibernate tetap mengeksekusi SQL UPDATE. State audit dikembalikan ke
			 * snapshot sebelumnya agar dirty-check menjadi bersih.
			 */
			AuditTrailHelper.restoreIgnoredUpdateProperties(entity, currentState, previousState, propertyNames);
			AuditTrailHelper.debug("AuditTimestampInterceptor.cancel update audit-only "
					+ AuditTrailHelper.describeEntity(entity, id));
			return false;
		}

		boolean metadataUpdated = isiMetadataAudit(entity, currentState, propertyNames);
		AuditTrailHelper.debug("AuditTimestampInterceptor.update metadata=" + metadataUpdated + " "
				+ AuditTrailHelper.describeEntity(entity, id));
		return metadataUpdated;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (!(entity instanceof GeneralValueObject)) {
			AuditTrailHelper.debug("AuditTimestampInterceptor.skip onSave non-GeneralValueObject "
					+ AuditTrailHelper.describeEntity(entity, id));
			return false;
		}
		// Daftarkan entity baru ke identity map (ID sudah ada dari sequence PostgreSQL)
		if (id instanceof Long) {
			EntityIdentityMap.canonical((GeneralValueObject) entity);
		}
		boolean metadataUpdated = isiMetadataAudit(entity, state, propertyNames);
		AuditTrailHelper.debug("AuditTimestampInterceptor.onSave metadata=" + metadataUpdated + " "
				+ AuditTrailHelper.describeEntity(entity, id));
		return metadataUpdated;
	}

	/**
	 * <b>Historis (2026-08-12, superseded).</b> Sebelumnya method ini ({@code
	 * olehOlehIdAdalahDataBisnis}, sudah DIHAPUS) mengecualikan {@code SesiKasKasir} di sini agar
	 * {@code oleh}/{@code olehId}-nya tidak ditimpa -- root cause bug "Kas Terbuka tapi checkout
	 * ditolak" saat itu adalah {@code SesiKasKasir} menyalahgunakan nama kolom audit generik ini utk
	 * data bisnisnya sendiri (kasir mana yang membuka sesi). Pengecualian instanceof-based itu RAPUH
	 * (terbukti: baris yang sama juga perlu dikecualikan terpisah di {@link #ubah}, jalur
	 * {@code SaveEventListener} yang berbeda dari method ini -- mudah lupa dikecualikan lagi kalau
	 * entity lain di masa depan melakukan hal serupa). Diperbaiki dgn benar dgn memberi {@code
	 * SesiKasKasir} kolom sendiri ({@code kasirNama}/{@code kasirUserId}, lihat javadoc {@link
	 * ais.database.model.inventory.SesiKasKasir#getKasirNama()}) -- {@code oleh}/{@code olehId}
	 * sekarang murni metadata audit generik lagi utk SEMUA entity tanpa kecuali, sesuai maksud
	 * aslinya.
	 */
	private boolean isiMetadataAudit(Object entity, Object[] state, String[] propertyNames) {
		boolean berubah = false;
		if (state == null || propertyNames == null) {
			return false;
		}
		for (int i = 0; i < propertyNames.length && i < state.length; i++) {
			if (AuditTrailHelper.PROP_TANGGAL_DIRUBAH.equals(propertyNames[i])) {
				state[i] = ais.ui.util.WaktuUtil.getDate();
				berubah = true;
			} else if (AuditTrailHelper.PROP_OLEH_ID.equals(propertyNames[i])) {
				state[i] = AuditTimestampInterceptor.olehId();
				berubah = true;
			} else if (AuditTrailHelper.PROP_OLEH.equals(propertyNames[i])) {
				state[i] = AuditTimestampInterceptor.oleh();
				berubah = true;
			}
		}
		return berubah;
	}

	public static void ubah(Object o) {

		if (o != null && o instanceof GeneralValueObject) {
			Boolean updateDecision = AuditTrailHelper.peekUpdateDecision(o, AuditTrailHelper.safeIdentifier(o));
			if (updateDecision != null && !updateDecision.booleanValue()) {
				AuditTrailHelper.debug("AuditTimestampInterceptor.ubah skip tanpa perubahan bisnis "
						+ AuditTrailHelper.describeEntity(o, AuditTrailHelper.safeIdentifier(o)));
				return;
			}
			GeneralValueObject generalValueObject = (GeneralValueObject) o;
			generalValueObject.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
			generalValueObject.setOleh(AuditTimestampInterceptor.oleh());
			generalValueObject.setOlehId(AuditTimestampInterceptor.olehId());
			AuditTrailHelper.debug("AuditTimestampInterceptor.ubah metadata entity="
					+ AuditTrailHelper.describeEntity(o, AuditTrailHelper.safeIdentifier(o)));
		}
	}

	/**
	 * Identitas pengguna utk permintaan PosApi (Desktop/Android). Klien POS tidak
	 * lewat sesi web ZK/JSP sehingga {@code Common.getCurrentUser} selalu null dan
	 * audit jatuh ke "external_update"; PosApi menaruh Tbmuser hasil autentikasi
	 * token perangkat sebagai atribut request supaya oleh/olehId mencatat kasir
	 * yang sedang login di aplikasi. Atribut mati bersama request (tanpa risiko
	 * bocor antar-thread pool seperti ThreadLocal).
	 */
	public static final String ATTR_PENGGUNA_POS = "ais.pos.tbmuser";

	private static Tbmuser penggunaPosApi(HttpServletRequest request) {
		try {
			Object o = request == null ? null : request.getAttribute(ATTR_PENGGUNA_POS);
			return o instanceof Tbmuser ? (Tbmuser) o : null;
		} catch (Exception e) {
			return null;
		}
	}

	public static String oleh() {
		Tbmuser tbmuser = null;
		try {
			HttpServletRequest request = currentRequest();
			if (request != null) {
				tbmuser = Common.getCurrentUser(request);
				if (tbmuser == null) {
					tbmuser = penggunaPosApi(request);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTimestampInterceptor.java:224");
			// Abaikan agar proses audit tidak mengganggu proses simpan data.
		}

		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
		Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();

		String oleh = tbmuser == null ? "external_update"
				: mahasiswa != null ? mahasiswa.getNama()
						: siswa != null ? siswa.getNama()
								: dosen != null ? dosen.getNama()
										: guru != null ? guru.getNama()
												: pegawai != null ? pegawai.getNama() : (tbmuser.getUserNama());
		return oleh;

	}

	public static String olehId() {
		Tbmuser tbmuser = null;
		String ip = "";
		String callFrom = buildAuditCallFrom(150);
		try {
			HttpServletRequest request = currentRequest();
			if (request != null) {
				ip = readClientIp(request);
				try {
					tbmuser = Common.getCurrentUser(request);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTimestampInterceptor.java:254");
					// Abaikan agar proses audit tidak mengganggu proses simpan data.
				}
				if (tbmuser == null) {
					tbmuser = penggunaPosApi(request);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditTimestampInterceptor.java:258");
			// Abaikan agar proses audit tidak mengganggu proses simpan data.
		}

		StringBuilder olehId = new StringBuilder();
		try {
			olehId.append(Common.generateOlehId(tbmuser));
		} catch (Exception e) {
			olehId.append("external_update");
		}
		appendOlehIdPart(olehId, callFrom);
		appendOlehIdPart(olehId, ip);
		return olehId.toString();
	}

	private static HttpServletRequest currentRequest() {
		HttpServletRequest request = null;
		try {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		} catch (Exception e) {
			request = null;
		}
		if (request == null) {
			try {
				request = RequestContext.get();
			} catch (Exception e) {
				request = null;
			}
		}
		return request;
	}

	private static String readClientIp(HttpServletRequest request) {
		if (request == null) {
			return "";
		}
		String ip = firstNotEmpty(request.getHeader("CF-Connecting-IP"), request.getHeader("Cf-Connecting-Ip"),
				request.getHeader("X-Forwarded-For"), request.getHeader("X-Real-IP"), request.getRemoteAddr());
		if (ip == null) {
			return "";
		}
		int comma = ip.indexOf(',');
		if (comma >= 0) {
			ip = ip.substring(0, comma);
		}
		return limitText(ip.trim(), 45);
	}

	private static String firstNotEmpty(String a, String b, String c, String d, String e) {
		if (a != null && a.trim().length() > 0) return a.trim();
		if (b != null && b.trim().length() > 0) return b.trim();
		if (c != null && c.trim().length() > 0) return c.trim();
		if (d != null && d.trim().length() > 0) return d.trim();
		if (e != null && e.trim().length() > 0) return e.trim();
		return "";
	}

	private static void appendOlehIdPart(StringBuilder sb, String value) {
		if (sb == null || value == null) {
			return;
		}
		String text = value.trim();
		if (text.length() == 0) {
			return;
		}
		if (sb.length() > 0) {
			sb.append(";");
		}
		sb.append(text);
	}

	private static String buildAuditCallFrom(int maxLength) {
		StringBuilder sb = new StringBuilder();
		try {
			StackTraceElement[] elements = Thread.currentThread().getStackTrace();
			for (int i = 0; elements != null && i < elements.length; i++) {
				StackTraceElement element = elements[i];
				if (element == null || element.getClassName() == null) {
					continue;
				}
				String className = element.getClassName();
				if (!className.startsWith("ais.")) {
					continue;
				}
				if (isAuditInternalStackElement(className)) {
					continue;
				}
				String item = simpleClassName(className) + ":" + element.getLineNumber();
				if (item.length() == 0) {
					continue;
				}
				if (sb.length() > 0) {
					if (sb.length() + 1 + item.length() > maxLength) {
						break;
					}
					sb.append(";");
				} else if (item.length() > maxLength) {
					sb.append(limitText(item, maxLength));
					break;
				}
				sb.append(item);
				if (sb.length() >= maxLength) {
					break;
				}
			}
		} catch (Exception e) {
			return "";
		}
		return limitText(sb.toString(), maxLength);
	}

	private static String simpleClassName(String className) {
		if (className == null) {
			return "";
		}
		String text = className.trim();
		int dot = text.lastIndexOf('.');
		if (dot >= 0 && dot + 1 < text.length()) {
			text = text.substring(dot + 1);
		}
		return text;
	}

	private static boolean isAuditInternalStackElement(String className) {
		if (className == null) {
			return true;
		}
		if (className.equals(AuditTimestampInterceptor.class.getName())) {
			return true;
		}
		if (className.equals(AuditTrailHelper.class.getName())) {
			return true;
		}
		if (className.startsWith("ais.database.hibernate.")
				&& (className.endsWith("EventListener") || className.endsWith("Interceptor")
						|| className.endsWith("AuditListener"))) {
			return true;
		}
		return false;
	}

	private static String limitText(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		String text = value.trim();
		if (maxLength <= 0 || text.length() <= maxLength) {
			return text;
		}
		if (maxLength <= 3) {
			return text.substring(0, maxLength);
		}
		return text.substring(0, maxLength - 3) + "...";
	}
}