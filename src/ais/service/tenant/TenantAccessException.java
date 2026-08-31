package ais.service.tenant;

/**
 * <h3>Penolakan akses tenant (P1).</h3>
 *
 * <p>Dilempar ketika konteks tenant tidak dapat dibentuk atau aktor tidak berhak atasnya.</p>
 *
 * <p><b>{@link #getKode()} memakai kontrak kode galat baku dokumen master &sect;7.2</b> --
 * daftarnya tetap dan dibaca mesin; klien memetakannya ke pesan. Jangan menambah kode di luar
 * daftar itu tanpa memperbarui kontraknya, sebab klien lama akan menerima kode yang tidak
 * dikenalnya.</p>
 *
 * <p>{@link #getMessage()} untuk dibaca manusia. <b>Jangan</b> memasukkan nama schema, SQL,
 * jejak tumpukan, URL basis data, maupun kredensial ke pesan -- &sect;7.2 melarangnya keluar
 * pada response publik.</p>
 */
public class TenantAccessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** Aktor punya lebih dari satu tenant dan belum memilih salah satunya. */
	public static final String TENANT_SELECTION_REQUIRED = "TENANT_SELECTION_REQUIRED";
	/** tenantId pada header berbeda dengan yang di body. */
	public static final String TENANT_CONTEXT_MISMATCH = "TENANT_CONTEXT_MISMATCH";
	/** Tenant bukan milik aktor: bukan anggota, keanggotaan kedaluwarsa, atau tenant tak dikenal. */
	public static final String TENANT_ACCESS_DENIED = "TENANT_ACCESS_DENIED";
	/** Tenant ada tetapi belum READY/ACTIVE (mis. masih PROVISIONING). */
	public static final String TENANT_NOT_READY = "TENANT_NOT_READY";
	/** Tenant dihentikan sementara. */
	public static final String TENANT_SUSPENDED = "TENANT_SUSPENDED";
	/** Modul yang diminta tidak aktif bagi tenant ini. */
	public static final String TENANT_MODULE_DISABLED = "TENANT_MODULE_DISABLED";
	/** Nama schema hilang atau tidak lolos validasi -- fail-closed. */
	public static final String TENANT_SCHEMA_INVALID = "TENANT_SCHEMA_INVALID";
	/** Versi schema tenant tertinggal dari versi yang dituntut aplikasi. */
	public static final String TENANT_SCHEMA_MIGRATION_REQUIRED = "TENANT_SCHEMA_MIGRATION_REQUIRED";
	/** Data lokal perangkat terikat tenant lain. Dipakai jalur Flutter/sinkronisasi. */
	public static final String TENANT_LOCAL_DATA_CONFLICT = "TENANT_LOCAL_DATA_CONFLICT";

	private final String kode;

	/**
	 * Membentuk galat tenant tanpa sebab (cause) berantai.
	 *
	 * @param kode  salah satu konstanta kode galat baku di kelas ini (kontrak &sect;7.2); klien
	 *              memetakan kode ini ke pesan tampilannya sendiri, jadi jangan memberi nilai di
	 *              luar daftar konstanta tanpa memperbarui kontraknya.
	 * @param pesan pesan yang aman dibaca manusia -- tidak boleh memuat nama schema, SQL, jejak
	 *              tumpukan, URL basis data, maupun kredensial (lihat larangan &sect;7.2 di
	 *              javadoc kelas).
	 */
	public TenantAccessException(String kode, String pesan) {
		super(pesan);
		this.kode = kode;
	}

	/**
	 * Sama seperti {@link #TenantAccessException(String, String)}, dengan tambahan
	 * {@code sebab} (cause) untuk melampirkan galat asal -- dipakai saat exception ini membungkus
	 * kegagalan lain (mis. validasi nama schema yang gagal) supaya jejak asalnya tidak hilang di
	 * log server, tanpa membocorkannya ke {@link #getMessage()}.
	 *
	 * @param kode  salah satu konstanta kode galat baku di kelas ini.
	 * @param pesan pesan yang aman dibaca manusia.
	 * @param sebab galat asal yang memicu penolakan akses ini.
	 */
	public TenantAccessException(String kode, String pesan, Throwable sebab) {
		super(pesan, sebab);
		this.kode = kode;
	}

	/**
	 * Kode galat baku (kontrak &sect;7.2) yang dibaca mesin oleh klien untuk memutuskan
	 * penanganannya (mis. tampilkan pemilih tenant, arahkan ke halaman suspend, dsb.) --
	 * berbeda dari {@link #getMessage()} yang murni untuk dibaca manusia.
	 *
	 * @return salah satu konstanta {@code TENANT_*} di kelas ini.
	 */
	public String getKode() {
		return kode;
	}
}
