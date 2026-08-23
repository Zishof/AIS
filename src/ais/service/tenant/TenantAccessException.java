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
