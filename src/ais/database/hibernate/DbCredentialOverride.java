package ais.database.hibernate;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.hibernate.cfg.Configuration;

/**
 * Override kredensial database Hibernate dari berkas properti EKSTERNAL (di luar
 * WAR/repository), sebagai langkah keamanan P0: menghilangkan kebutuhan menyimpan
 * url/username/password plaintext di {@code hibernate*.cfg.xml} yang ikut ter-commit.
 *
 * <h3>Cara kerja</h3>
 * Dipanggil SETELAH {@code configuration.configure(...)} dan SEBELUM
 * {@code buildSessionFactory()}. Bila berkas eksternal ada dan memuat kunci untuk prefix
 * factory bersangkutan, nilai {@code hibernate.connection.url/username/password} pada
 * {@link Configuration} DITIMPA. Bila berkas tidak ada / kunci tidak ada → NO-OP total
 * (nilai dari cfg.xml tetap dipakai) sehingga 100% kompatibel dengan deployment lama.
 *
 * <h3>Lokasi berkas</h3>
 * Default {@code /opt/.g/.h/db.properties} — mengikuti pola konfigurasi eksternal yang
 * sudah dipakai aplikasi ({@code MemoryCacheUtil} membaca {@code /opt/.g/.h/}). Dapat
 * dioverride via system property {@code ais.db.override.file}.
 *
 * <h3>Format kunci</h3>
 * <pre>
 * &lt;prefix&gt;.hibernate.connection.url=jdbc:postgresql://host:5432/nama_db
 * &lt;prefix&gt;.hibernate.connection.username=...
 * &lt;prefix&gt;.hibernate.connection.password=...
 * </pre>
 * dengan prefix: {@code utama}, {@code streaming}, {@code ojs}, {@code radius}.
 *
 * <h3>Keamanan</h3>
 * Kelas ini TIDAK pernah menulis nilai kredensial ke log/stdout — hanya mencatat
 * prefix dan kunci mana yang berhasil dioverride (ya/tidak).
 *
 * <p>CATATAN: factory UTAMA yang dibangun listener zkplus (jalur default ZK) membaca
 * cfg.xml langsung dan TIDAK lewat kelas ini; eksternalisasi penuh DB utama menunggu
 * keputusan deployment (lihat docs/performance/OPTIMIZATION_PLAN.md P0-1).</p>
 */
public final class DbCredentialOverride {

	private static final String SYSTEM_PROPERTY_LOKASI = "ais.db.override.file";
	private static final String LOKASI_DEFAULT = "/opt/.g/.h/db.properties";

	private static final String[] KUNCI_KONEKSI = { "hibernate.connection.url", "hibernate.connection.username",
			"hibernate.connection.password" };

	private static volatile Properties cache = null;

	private DbCredentialOverride() {
	}

	/**
	 * Terapkan override kredensial untuk satu factory. Aman dipanggil kapan pun
	 * (seluruh kegagalan ditelan agar TIDAK pernah menggagalkan pembangunan factory —
	 * fallback selalu ke nilai cfg.xml lama).
	 *
	 * @param configuration konfigurasi Hibernate yang sudah di-{@code configure()}
	 * @param prefix        prefix factory: utama / streaming / ojs / radius
	 */
	public static void terapkan(Configuration configuration, String prefix) {
		try {
			if (configuration == null || prefix == null) {
				return;
			}
			if (terapkanEnvironmentJurnal(configuration, prefix)) {
				System.out.println("DbCredentialOverride: override environment jurnal diterapkan untuk factory '" + prefix + "'");
				return;
			}
			Properties eksternal = muatBerkas();
			if (eksternal == null || eksternal.isEmpty()) {
				return;
			}
			StringBuilder laporan = new StringBuilder();
			for (int i = 0; i < KUNCI_KONEKSI.length; i++) {
				String kunci = KUNCI_KONEKSI[i];
				String nilai = eksternal.getProperty(prefix + "." + kunci);
				if (nilai != null && !nilai.trim().isEmpty()) {
					configuration.setProperty(kunci, nilai.trim());
					laporan.append(kunci).append("=ya ");
				}
			}
			if (laporan.length() > 0) {
				// Hanya nama kunci yang dicatat — JANGAN pernah mencetak nilainya.
				System.out.println("DbCredentialOverride: override eksternal diterapkan untuk factory '" + prefix
						+ "' (" + laporan.toString().trim() + ")");
			}
		} catch (SecurityException wajibGagal) {
			throw wajibGagal;
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "DbCredentialOverride.terapkan:" + prefix);
		}
	}

	/** Kontrak environment SIT/UAT; nilai secret tidak pernah dicetak. */
	private static boolean terapkanEnvironmentJurnal(Configuration configuration, String prefix) {
		boolean main = "utama".equals(prefix);
		boolean streaming = "streaming".equals(prefix);
		if (!main && !streaming) return false;
		String envPrefix = main ? "AIS_JURNAL_DB_" : "AIS_JURNAL_STREAMING_DB_";
		String name = System.getenv(envPrefix + "NAME");
		String user = System.getenv(envPrefix + "USER");
		String password = System.getenv(envPrefix + "PASSWORD");
		if (streaming && adaEnvironmentJurnal() && (name == null || name.trim().isEmpty()
				|| user == null || user.trim().isEmpty() || password == null || password.isEmpty()))
			throw new SecurityException("Mode jurnal SIT/UAT memerlukan database streaming terisolasi.");
		if (name == null || name.trim().isEmpty() || user == null || user.trim().isEmpty()
				|| password == null || password.isEmpty()) return false;
		String host = System.getenv(envPrefix + "HOST");
		String port = System.getenv(envPrefix + "PORT");
		if (host == null || host.trim().isEmpty()) host = "localhost";
		if (port == null || !port.matches("[0-9]{1,5}")) port = "5432";
		if (!name.matches("[A-Za-z0-9_]+") || !host.matches("[A-Za-z0-9_.:-]+"))
			throw new IllegalArgumentException("Konfigurasi database jurnal tidak valid");
		configuration.setProperty("hibernate.connection.url", "jdbc:postgresql://" + host + ":" + port + "/" + name);
		configuration.setProperty("hibernate.connection.username", user);
		configuration.setProperty("hibernate.connection.password", password);
		// SIT/UAT memakai migration SQL dan gate schema eksplisit. SessionFactory
		// legacy memetakan banyak modul di luar jurnal, jadi auto-DDL maupun validasi
		// global tidak boleh mengubah/menggagalkan clone hanya karena modul lain.
        boolean streamingCloneUpdate = streaming
                && "true".equalsIgnoreCase(System.getenv("AIS_JURNAL_STREAMING_SCHEMA_UPDATE"))
                && name.toLowerCase().matches(".*(_sit|_uat|_demo|_fixture)(_[a-z0-9]+)?$");
        configuration.setProperty("hibernate.hbm2ddl.auto", streamingCloneUpdate ? "update" : "none");
		// CVE-2020-25638 requires SQL comments together with unsafe query literals.
		// Journal environments do not need generated SQL comments, so keep the
		// vulnerable precondition disabled even if a parent JVM sets it globally.
		configuration.setProperty("hibernate.use_sql_comments", "false");
		return true;
	}

	/**
	 * Menandai bahwa proses ini secara eksplisit diisolasi ke database jurnal SIT/UAT.
	 * HibernateUtil memakai sinyal ini untuk tidak mewarisi SessionFactory zkplus yang
	 * mungkin sudah dibangun dari konfigurasi deployment lama.
	 */
	static boolean adaEnvironmentJurnal() {
		String name = System.getenv("AIS_JURNAL_DB_NAME");
		String user = System.getenv("AIS_JURNAL_DB_USER");
		String password = System.getenv("AIS_JURNAL_DB_PASSWORD");
		return name != null && !name.trim().isEmpty()
				&& user != null && !user.trim().isEmpty()
				&& password != null && !password.isEmpty();
	}

	private static Properties muatBerkas() {
		Properties hasil = cache;
		if (hasil != null) {
			return hasil;
		}
		synchronized (DbCredentialOverride.class) {
			if (cache != null) {
				return cache;
			}
			hasil = new Properties();
			FileInputStream fis = null;
			try {
				String lokasi = System.getProperty(SYSTEM_PROPERTY_LOKASI, LOKASI_DEFAULT);
				File berkas = new File(lokasi);
				if (berkas.isFile()) {
					fis = new FileInputStream(berkas);
					hasil.load(fis);
				}
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "DbCredentialOverride.muatBerkas");
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "DbCredentialOverride.tutupBerkas");
					}
				}
			}
			cache = hasil;
			return hasil;
		}
	}
}
