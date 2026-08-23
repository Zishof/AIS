package ais.service.tenant;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 * <h3>Penjalan SQL ber-schema tenant (P1).</h3>
 *
 * <p>Templat SQL memakai penanda <code>{t}</code> untuk schema data dan <code>{a}</code> untuk
 * schema audit; keduanya diganti nama schema yang <b>sudah divalidasi</b> lalu dikutip ganda:</p>
 *
 * <pre>SELECT id, waktu FROM {t}.pembelian WHERE toko = :toko</pre>
 *
 * <h4>Mengapa penanda, bukan {@code SET search_path}</h4>
 * <p>Kumpulan koneksi (c3p0) mengembalikan koneksi ke kolam beserta {@code search_path}-nya.
 * Satu jalur yang lupa mengembalikannya membuat request tenant berikutnya membaca schema tenant
 * sebelumnya -- tanpa galat, tanpa gejala. Kualifikasi eksplisit tidak punya keadaan yang bisa
 * tertinggal.</p>
 *
 * <h4>Mengapa aman dari injeksi</h4>
 * <p>Nama schema tidak pernah berasal dari input pengguna: ia diambil dari
 * {@link TenantContext} yang dibentuk {@link TenantSchemaLocator}, dan divalidasi ulang di sini
 * terhadap pola ketat {@code TenantSchemaService.pastikanAman}. Nilai selain nama schema tetap
 * wajib lewat parameter terikat -- kelas ini <b>tidak</b> menyambung nilai ke SQL.</p>
 */
public final class TenantSqlExecutor {

	/** Penanda schema data. */
	public static final String PENANDA_DATA = "{t}";
	/** Penanda schema audit. */
	public static final String PENANDA_AUDIT = "{a}";

	private TenantSqlExecutor() {
	}

	/**
	 * Ganti penanda pada templat dengan nama schema tenant yang sudah dikutip.
	 *
	 * @throws TenantAccessException bila templat menuntut schema tenant sedangkan konteksnya
	 *         berjalan pada mode LEGACY (tanpa schema).
	 */
	public static String siapkan(TenantContext ctx, String templat) {
		if (templat == null) {
			throw new IllegalArgumentException("Templat SQL kosong.");
		}
		if (ctx == null) {
			throw new TenantAccessException(TenantAccessException.KODE_TENANT_BELUM_DIPILIH,
					"Konteks tenant belum dibentuk.");
		}
		String hasil = templat;
		if (hasil.indexOf(PENANDA_DATA) >= 0) {
			hasil = hasil.replace(PENANDA_DATA, kutip(ctx.getSchemaName()));
		}
		if (hasil.indexOf(PENANDA_AUDIT) >= 0) {
			hasil = hasil.replace(PENANDA_AUDIT, kutip(ctx.getAuditSchemaName()));
		}
		return hasil;
	}

	/** Bentuk {@link SQLQuery} dari templat. Parameter tetap diikat oleh pemanggil. */
	public static SQLQuery sql(Session session, TenantContext ctx, String templat) {
		return session.createSQLQuery(siapkan(ctx, templat));
	}

	/**
	 * Benar bila templat ini menuntut schema tenant. Berguna bagi pemanggil yang menyediakan
	 * dua templat -- jalur schema tenant dan jalur shared lama -- lalu memilih salah satunya.
	 */
	public static boolean butuhSchema(String templat) {
		return templat != null
				&& (templat.indexOf(PENANDA_DATA) >= 0 || templat.indexOf(PENANDA_AUDIT) >= 0);
	}

	private static String kutip(String schema) {
		if (schema == null || schema.trim().length() == 0) {
			throw new TenantAccessException(TenantAccessException.KODE_SCHEMA_BELUM_SIAP,
					"Kueri ini menuntut schema tenant, tetapi tenant berjalan tanpa schema.");
		}
		// Validasi ulang: murah, dan menutup kemungkinan konteks dibentuk lewat jalur lain.
		String aman;
		try {
			aman = TenantSchemaService.pastikanAman(schema.trim());
		} catch (IllegalArgumentException e) {
			throw new TenantAccessException(TenantAccessException.KODE_SCHEMA_BELUM_SIAP,
					"Konfigurasi schema tenant tidak sah.", e);
		}
		return "\"" + aman + "\"";
	}
}
