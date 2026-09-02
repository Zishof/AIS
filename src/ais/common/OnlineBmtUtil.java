package ais.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.KanalPembayaran;
import ais.database.model.sekolah.Sekolah;

/**
 * Kontrak bersama kanal pembayaran Online BMT.
 *
 * <p>Online BMT berbeda dari payment-link seperti e-Smartlink. AIS tidak mengirim
 * permintaan pembuatan tagihan ke BMT. AIS menerbitkan satu nomor invoice lokal
 * pada {@link VirtualAccountBank}; kemudian server BMT memanggil endpoint
 * {@code /OnlineBmt} untuk INQUIRY, PAYMENT, atau CHECK_STATUS_PAYMENT. Karena itu
 * pembuatan invoice tidak boleh dianggap sebagai pembayaran dan tidak boleh
 * mengubah saldo, cicilan, maupun status mahasiswa.</p>
 *
 * <p>Aktivasi selalu fail-closed. Sakelar global dan sakelar tenant harus aktif.
 * Instalasi lama yang belum memiliki kolom sekolah/kanal akan dibaca sebagai
 * {@code false} oleh getter model. Kunci rahasia hanya dibaca oleh servlet dan
 * tidak pernah ditaruh pada request/response {@link VirtualAccountBank}.</p>
 */
public final class OnlineBmtUtil {

	public static final String BANK_NAME = "Online BMT";
	public static final String PARAM_KEY = "online_bmt";
	public static final String MARKER = "online_bmt:true";

	private OnlineBmtUtil() {
	}

	/** Mengubah input form kosong menjadi null agar mekanisme pewarisan tetap bekerja. */
	public static String emptyToNull(String value) {
		if (value == null || value.trim().length() == 0) {
			return null;
		}
		return value.trim();
	}

	/**
	 * Memvalidasi satu lapis override Online BMT (Sekolah atau Kanal Pembayaran).
	 * Credential diperlakukan atomik: semuanya kosong berarti mewarisi parent,
	 * sedangkan override wajib mengisi API key, AES key, dan HMAC key sekaligus.
	 * Aturan ini mencegah API key suatu tenant dipasangkan tanpa sengaja dengan
	 * encryption key tenant lain saat callback mencoba membuka DATA terenkripsi.
	 */
	public static void validateOverrides(String prefix, Double administrationFee, String apiKey,
			String encryptionKey, String hmacKey, Integer requestTimeTolerance) {
		String normalizedPrefix = emptyToNull(prefix);
		if (normalizedPrefix != null && !normalizedPrefix.matches("[A-Za-z0-9]{1,8}")) {
			throw new IllegalArgumentException("Prefix invoice Online BMT harus berupa 1-8 huruf/angka tanpa spasi.");
		}
		if (administrationFee != null && administrationFee.doubleValue() < 0.0) {
			throw new IllegalArgumentException("Biaya administrasi Online BMT tidak boleh negatif.");
		}
		int securityValues = (emptyToNull(apiKey) == null ? 0 : 1)
				+ (emptyToNull(encryptionKey) == null ? 0 : 1)
				+ (emptyToNull(hmacKey) == null ? 0 : 1);
		if (securityValues != 0 && securityValues != 3) {
			throw new IllegalArgumentException("API key, encryption key AES, dan HMAC key Online BMT harus diisi bersama-sama, atau dikosongkan seluruhnya untuk mengikuti konfigurasi induk.");
		}
		if (requestTimeTolerance != null
				&& (requestTimeTolerance.intValue() < 30 || requestTimeTolerance.intValue() > 3600)) {
			throw new IllegalArgumentException("Toleransi waktu request Online BMT harus antara 30 sampai 3600 detik.");
		}
	}

	/** Membaca toleransi opsional dari form; string kosong berarti mewarisi parent. */
	public static Integer parseOptionalTolerance(String value) {
		String normalized = emptyToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return Integer.valueOf(normalized);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Toleransi waktu request Online BMT harus berupa angka bulat.");
		}
	}

	public static boolean isGlobalEnabled() {
		return Common.bolehKonfigurasi(Konfigurasi.ONLINE_BMT_AKTIF, Konfigurasi.TIDAK_AKTIF);
	}

	public static boolean isPerguruanTinggiEnabled(Long ptId) {
		if (!isGlobalEnabled() || ptId == null) {
			return false;
		}
		return Common.bolehKonfigurasi(Konfigurasi.ONLINE_BMT_AKTIF_PT_PREFIX + ptId,
				Konfigurasi.TIDAK_AKTIF);
	}

	public static boolean isSekolahEnabled(Sekolah sekolah, KanalPembayaran kanal) {
		if (!isGlobalEnabled() || sekolah == null
				|| !Boolean.TRUE.equals(sekolah.getAktfkanPembayaranViaOnlineBmt())) {
			return false;
		}
		return kanal == null || Boolean.TRUE.equals(kanal.getAktfkanPembayaranViaOnlineBmt());
	}

	/**
	 * Snapshot konfigurasi efektif untuk satu pemilik invoice. Snapshot sengaja
	 * tidak menyimpan entity Hibernate supaya aman digunakan sesudah session ditutup.
	 * Urutan pewarisan adalah Kanal Pembayaran, Sekolah, lalu konfigurasi global.
	 */
	public static final class Settings {
		private final String source;
		private final String prefixInvoice;
		private final double administrationFee;
		private final String kodeMitra;
		private final String namaMitra;
		private final String kodeMerchant;
		private final String namaMerchant;
		private final String apiKey;
		private final String encryptionKey;
		private final String hmacKey;
		private final int requestTimeTolerance;
		private final boolean securityOverrideValid;

		private Settings(String source, String prefixInvoice, double administrationFee,
				String kodeMitra, String namaMitra, String kodeMerchant, String namaMerchant,
				String apiKey, String encryptionKey, String hmacKey, int requestTimeTolerance,
				boolean securityOverrideValid) {
			this.source = source;
			this.prefixInvoice = normalizePrefix(prefixInvoice);
			this.administrationFee = administrationFee;
			this.kodeMitra = clean(kodeMitra);
			this.namaMitra = clean(namaMitra);
			this.kodeMerchant = clean(kodeMerchant);
			this.namaMerchant = clean(namaMerchant);
			this.apiKey = clean(apiKey);
			this.encryptionKey = clean(encryptionKey);
			this.hmacKey = clean(hmacKey);
			this.requestTimeTolerance = normalizeTolerance(requestTimeTolerance);
			this.securityOverrideValid = securityOverrideValid;
		}

		public String getSource() { return source; }
		public String getPrefixInvoice() { return prefixInvoice; }
		public double getAdministrationFee() { return administrationFee; }
		public String getKodeMitra() { return kodeMitra; }
		public String getNamaMitra() { return namaMitra; }
		public String getKodeMerchant() { return kodeMerchant; }
		public String getNamaMerchant() { return namaMerchant; }
		public String getApiKey() { return apiKey; }
		public String getEncryptionKey() { return encryptionKey; }
		public String getHmacKey() { return hmacKey; }
		public int getRequestTimeTolerance() { return requestTimeTolerance; }

		public boolean isSecurityComplete() {
			return securityOverrideValid && apiKey.length() > 0
					&& encryptionKey.length() > 0 && hmacKey.length() > 0;
		}

		public boolean hasCompleteMerchantIdentity() {
			return kodeMitra.length() > 0 && namaMitra.length() > 0
					&& kodeMerchant.length() > 0 && namaMerchant.length() > 0;
		}

		/** Credential harus identik setelah invoice diketahui, bukan API key saja. */
		public boolean sameSecurity(Settings other) {
			return other != null && apiKey.equals(other.apiKey)
					&& encryptionKey.equals(other.encryptionKey) && hmacKey.equals(other.hmacKey);
		}
	}

	/** Menghasilkan konfigurasi global yang menjadi fallback seluruh tenant. */
	public static Settings globalSettings() {
		double fee = 0.0;
		try { fee = Double.parseDouble(config(Konfigurasi.ONLINE_BMT_BIAYA_ADMINISTRASI)); }
		catch (Exception ignore) { fee = 0.0; }
		int tolerance = 300;
		try { tolerance = Integer.parseInt(config(Konfigurasi.ONLINE_BMT_REQUEST_TIME_TOLERANCE)); }
		catch (Exception ignore) { tolerance = 300; }
		return new Settings("global", configOrDefault(Konfigurasi.ONLINE_BMT_PREFIX_INVOICE, "BMT"), fee,
				config(Konfigurasi.ONLINE_BMT_KODE_MITRA), config(Konfigurasi.ONLINE_BMT_NAMA_MITRA),
				config(Konfigurasi.ONLINE_BMT_KODE_MERCHANT), config(Konfigurasi.ONLINE_BMT_NAMA_MERCHANT),
				config(Konfigurasi.ONLINE_BMT_API_KEY), config(Konfigurasi.ONLINE_BMT_ENCRYPTION_KEY),
				config(Konfigurasi.ONLINE_BMT_HMAC_KEY), tolerance, true);
	}

	/**
	 * Menggabungkan konfigurasi tanpa menyentuh database. Nilai String kosong dan
	 * angka null berarti inherit. Biaya nol tetap dipertahankan sebagai override sah.
	 */
	public static Settings resolveSettings(Sekolah sekolah, KanalPembayaran kanal) {
		Settings result = globalSettings();
		if (sekolah != null) result = overlaySchool(result, sekolah);
		if (kanal != null) result = overlayChannel(result, kanal);
		return result;
	}

	/** Menentukan konfigurasi efektif dari relasi pemilik yang sudah dihidrasi. */
	public static Settings resolveSettings(VirtualAccountBank invoice) {
		Sekolah sekolah = null;
		KanalPembayaran kanal = invoice == null ? null : invoice.getKanalPembayaran();
		if (invoice != null && invoice.getSiswa() != null) sekolah = invoice.getSiswa().getSekolah();
		else if (invoice != null && invoice.getCalonSiswa() != null) sekolah = invoice.getCalonSiswa().getSekolah();
		else if (kanal != null) sekolah = kanal.getSekolah();
		return resolveSettings(sekolah, kanal);
	}

	/**
	 * Mencari kandidat credential berdasarkan API key luar sebelum DATA dapat dibuka.
	 * Kandidat global selalu dipertimbangkan; override sekolah/kanal dicari lewat
	 * Hibernate. Setelah dekripsi, servlet tetap wajib mencocokkan ketiga credential
	 * kandidat dengan konfigurasi efektif pemilik invoice untuk mencegah lintas tenant.
	 */
	@SuppressWarnings("unchecked")
	public static List<Settings> findCredentialCandidates(String receivedApiKey) {
		List<Settings> result = new ArrayList<Settings>();
		Settings global = globalSettings();
		if (global.getApiKey().equals(clean(receivedApiKey)) && global.isSecurityComplete()) result.add(global);
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			List<Sekolah> schools = session.createCriteria(Sekolah.class)
					.add(Restrictions.eq("aktfkanPembayaranViaOnlineBmt", true))
					.add(Restrictions.eq("onlineBmtApiKey", clean(receivedApiKey))).list();
			for (Sekolah school : schools) {
				Settings candidate = overlaySchool(global, school);
				if (candidate.isSecurityComplete()) result.add(candidate);
			}
			List<KanalPembayaran> channels = session.createCriteria(KanalPembayaran.class)
					.add(Restrictions.eq("aktfkanPembayaranViaOnlineBmt", true))
					.add(Restrictions.eq("onlineBmtApiKey", clean(receivedApiKey))).list();
			for (KanalPembayaran channel : channels) {
				Sekolah school = channel.getSekolah();
				Settings parent = school == null ? global : overlaySchool(global, school);
				Settings candidate = overlayChannel(parent, channel);
				if (candidate.isSecurityComplete()) result.add(candidate);
			}
		} catch (Exception e) {
			ErrorAuditUtil.record(e, "OnlineBmtUtil.findCredentialCandidates");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return result;
	}

	private static Settings overlaySchool(Settings parent, Sekolah value) {
		String[] security = securityOverride(parent, value.getOnlineBmtApiKey(),
				value.getOnlineBmtEncryptionKey(), value.getOnlineBmtHmacKey());
		return new Settings("sekolah:" + value.getId(), inherit(value.getOnlineBmtPrefixInvoice(), parent.prefixInvoice),
				value.getOnlineBmtBiayaAdministrasi() == null ? parent.administrationFee : value.getOnlineBmtBiayaAdministrasi(),
				inherit(value.getOnlineBmtKodeMitra(), parent.kodeMitra), inherit(value.getOnlineBmtNamaMitra(), parent.namaMitra),
				inherit(value.getOnlineBmtKodeMerchant(), parent.kodeMerchant), inherit(value.getOnlineBmtNamaMerchant(), parent.namaMerchant),
				security[0], security[1], security[2], value.getOnlineBmtRequestTimeTolerance() == null
						? parent.requestTimeTolerance : value.getOnlineBmtRequestTimeTolerance(), "1".equals(security[3]));
	}

	private static Settings overlayChannel(Settings parent, KanalPembayaran value) {
		String[] security = securityOverride(parent, value.getOnlineBmtApiKey(),
				value.getOnlineBmtEncryptionKey(), value.getOnlineBmtHmacKey());
		return new Settings("kanal:" + value.getId(), inherit(value.getOnlineBmtPrefixInvoice(), parent.prefixInvoice),
				value.getOnlineBmtBiayaAdministrasi() == null ? parent.administrationFee : value.getOnlineBmtBiayaAdministrasi(),
				inherit(value.getOnlineBmtKodeMitra(), parent.kodeMitra), inherit(value.getOnlineBmtNamaMitra(), parent.namaMitra),
				inherit(value.getOnlineBmtKodeMerchant(), parent.kodeMerchant), inherit(value.getOnlineBmtNamaMerchant(), parent.namaMerchant),
				security[0], security[1], security[2], value.getOnlineBmtRequestTimeTolerance() == null
						? parent.requestTimeTolerance : value.getOnlineBmtRequestTimeTolerance(), "1".equals(security[3]));
	}

	private static String[] securityOverride(Settings parent, String apiKey, String encryptionKey, String hmacKey) {
		String api = clean(apiKey), encryption = clean(encryptionKey), hmac = clean(hmacKey);
		int count = (api.length() == 0 ? 0 : 1) + (encryption.length() == 0 ? 0 : 1)
				+ (hmac.length() == 0 ? 0 : 1);
		if (count == 0) return new String[] { parent.apiKey, parent.encryptionKey, parent.hmacKey,
				parent.securityOverrideValid ? "1" : "0" };
		return new String[] { api, encryption, hmac, count == 3 ? "1" : "0" };
	}

	private static String inherit(String value, String parent) {
		return clean(value).length() == 0 ? clean(parent) : clean(value);
	}

	private static String clean(String value) { return value == null ? "" : value.trim(); }

	private static int normalizeTolerance(int value) { return value < 30 || value > 3600 ? 300 : value; }

	private static String normalizePrefix(String value) {
		String prefix = clean(value).replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ENGLISH);
		if (prefix.length() == 0) prefix = "BMT";
		return prefix.length() > 8 ? prefix.substring(0, 8) : prefix;
	}

	private static String config(String key) {
		Konfigurasi value = Common.getKonfigurasi(key, "");
		return value == null ? "" : clean(value.getNilai());
	}

	private static String configOrDefault(String key, String defaultValue) {
		String value = config(key);
		return value.length() == 0 ? defaultValue : value;
	}

	/** Menyiapkan invoice lokal; method ini tidak melakukan posting finansial. */
	public static void prepareInvoice(VirtualAccountBank invoice) {
		prepareInvoice(invoice, globalSettings());
	}

	/** Menyiapkan invoice memakai override kanal/sekolah yang efektif. */
	public static void prepareInvoice(VirtualAccountBank invoice, Sekolah sekolah, KanalPembayaran kanal) {
		prepareInvoice(invoice, resolveSettings(sekolah, kanal));
	}

	private static void prepareInvoice(VirtualAccountBank invoice, Settings settings) {
		if (invoice == null) {
			throw new IllegalArgumentException("Invoice Online BMT tidak boleh null");
		}
		String prefix = settings.getPrefixInvoice();
		String kode = prefix + Common.getGeneratedBarCode(30 - prefix.length());
		invoice.setKode(kode);
		invoice.setBiayaAdmin(settings.getAdministrationFee());
		invoice.setBank(BANK_NAME);
		invoice.setChannel("ONLINE_BMT");
		invoice.setLink("");

		try {
			JSONObject audit = new JSONObject();
			audit.put("provider", "ONLINE_BMT");
			audit.put("NO_INVOICE", kode);
			audit.put("state", "WAITING_PAYMENT");
			invoice.setRequest(audit.toString());
		} catch (Exception e) {
			throw new IllegalStateException("Gagal menyiapkan audit invoice Online BMT", e);
		}
	}

	public static double payableAmount(VirtualAccountBank invoice) {
		if (invoice == null) {
			return 0.0;
		}
		return (invoice.getTotal() == null ? 0.0 : invoice.getTotal().doubleValue())
				+ (invoice.getBiayaAdmin() == null ? 0.0 : invoice.getBiayaAdmin().doubleValue());
	}

	/** Menambahkan nama kanal pada daftar CSV tanpa duplikasi; OFF tidak mengubah konfigurasi lama. */
	public static String appendToConfiguredBanks(String configured, boolean enabled) {
		String value = configured == null ? "" : configured.trim();
		if (!enabled) return value;
		String[] banks = value.split(",");
		for (int i = 0; i < banks.length; i++) {
			if (BANK_NAME.equalsIgnoreCase(banks[i].trim())) return value;
		}
		return value.length() == 0 ? BANK_NAME : value + "," + BANK_NAME;
	}
}
