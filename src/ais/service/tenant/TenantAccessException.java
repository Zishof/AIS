package ais.service.tenant;

/**
 * <h3>Penolakan akses tenant (P1).</h3>
 *
 * <p>Dilempar ketika konteks tenant tidak dapat dibentuk atau aktor tidak berhak atasnya:
 * tenant tidak dikenal, tenant tidak aktif, keanggotaan tidak ada/kedaluwarsa, atau schema
 * belum tersedia padahal mode menuntutnya.</p>
 *
 * <p><b>Sengaja RuntimeException.</b> Pemanggilnya adalah jalur servlet/dispatcher yang sudah
 * menangkap {@code Exception} secara menyeluruh; menjadikannya checked hanya menambah
 * {@code throws} di sepanjang rantai tanpa menambah keamanan.</p>
 *
 * <p>{@link #getKode()} dimaksudkan untuk dibaca mesin (klien memetakannya ke pesan), sedangkan
 * {@link #getMessage()} untuk dibaca manusia. <b>Jangan</b> memasukkan nama schema ke pesan:
 * nama schema tidak boleh sampai ke klien.</p>
 */
public class TenantAccessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** Tenant tidak ditemukan pada registry. */
	public static final String KODE_TENANT_TIDAK_DIKENAL = "TENANT_TIDAK_DIKENAL";
	/** Tenant ada tetapi statusnya bukan ACTIVE/READY. */
	public static final String KODE_TENANT_TIDAK_AKTIF = "TENANT_TIDAK_AKTIF";
	/** Aktor tidak punya keanggotaan aktif pada tenant tersebut. */
	public static final String KODE_BUKAN_ANGGOTA = "BUKAN_ANGGOTA";
	/** Keanggotaan ada tetapi di luar rentang validFrom/validUntil. */
	public static final String KODE_KEANGGOTAAN_KEDALUWARSA = "KEANGGOTAAN_KEDALUWARSA";
	/** Mode menuntut schema tenant tetapi schemaName kosong/belum diprovisioning. */
	public static final String KODE_SCHEMA_BELUM_SIAP = "SCHEMA_BELUM_SIAP";
	/** Aktor tidak dinyatakan (tbmuser maupun pendaftar kosong). */
	public static final String KODE_AKTOR_TIDAK_DIKENAL = "AKTOR_TIDAK_DIKENAL";
	/** Platform admin tidak memilih tenant secara eksplisit. */
	public static final String KODE_TENANT_BELUM_DIPILIH = "TENANT_BELUM_DIPILIH";

	private final String kode;

	public TenantAccessException(String kode, String pesan) {
		super(pesan);
		this.kode = kode;
	}

	public TenantAccessException(String kode, String pesan, Throwable sebab) {
		super(pesan, sebab);
		this.kode = kode;
	}

	public String getKode() {
		return kode;
	}
}
