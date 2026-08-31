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

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantSqlExecutor() {
	}

	/**
	 * Implementasi kanonik substitusi templat SQL ber-schema tenant — satu-satunya tempat
	 * yang benar-benar mengganti {@link #PENANDA_DATA}/{@link #PENANDA_AUDIT} dengan nama
	 * schema. Dipakai langsung oleh pemanggil yang hanya butuh teks SQL jadi (mis. untuk
	 * logging/debug), dan secara internal oleh {@link #sql(Session, TenantContext, String)}
	 * dan {@link #sqlHalaman(Session, TenantContext, String, int, int)}.
	 *
	 * <p>
	 * Setiap penanda yang muncul pada {@code templat} diganti lewat {@link #kutip(String,
	 * boolean)}, yang memvalidasi ULANG nama schema dari {@code ctx} (bukan sekadar
	 * memercayai nilai yang sudah tersimpan di objek) sebelum mengutipnya dengan tanda kutip
	 * ganda. Penanda yang tidak muncul pada {@code templat} tidak diproses sama sekali —
	 * substitusinya dilewati begitu saja untuk penanda itu. Ini TIDAK membebaskan pemanggil
	 * dari kewajiban menyertakan {@code ctx}: method ini menolak {@code ctx} {@code null}
	 * tanpa syarat di awal, terlepas apakah {@code templat} memuat penanda atau tidak —
	 * pemanggilan tanpa konteks tenant dianggap kesalahan pemanggil, bukan kasus yang perlu
	 * ditoleransi diam-diam.
	 * </p>
	 *
	 * @param ctx     konteks tenant aktif; TIDAK boleh {@code null}
	 * @param templat teks SQL mentah yang mungkin memuat {@link #PENANDA_DATA}/
	 *                {@link #PENANDA_AUDIT}; nilai lain (parameter kueri) TIDAK boleh
	 *                disambung langsung ke sini — tetap wajib lewat parameter terikat
	 * @return teks SQL dengan penanda tersubstitusi nama schema ber-kutip-ganda
	 * @throws IllegalArgumentException bila {@code templat} {@code null}
	 * @throws TenantAccessException    bila {@code ctx} {@code null}
	 *                                   ({@link TenantAccessException#TENANT_SELECTION_REQUIRED}),
	 *                                   atau bila templat memuat penanda schema sedangkan
	 *                                   konteksnya berjalan pada mode LEGACY tanpa schema
	 *                                   ({@link TenantAccessException#TENANT_SCHEMA_INVALID}) —
	 *                                   lihat catatan "Mengapa aman dari injeksi" pada javadoc
	 *                                   kelas untuk alasan validasi ulang dilakukan di sini
	 */
	public static String siapkan(TenantContext ctx, String templat) {
		if (templat == null) {
			throw new IllegalArgumentException("Templat SQL kosong.");
		}
		if (ctx == null) {
			throw new TenantAccessException(TenantAccessException.TENANT_SELECTION_REQUIRED,
					"Konteks tenant belum dibentuk.");
		}
		String hasil = templat;
		if (hasil.indexOf(PENANDA_DATA) >= 0) {
			hasil = hasil.replace(PENANDA_DATA, kutip(ctx.getSchemaName(), false));
		}
		if (hasil.indexOf(PENANDA_AUDIT) >= 0) {
			hasil = hasil.replace(PENANDA_AUDIT, kutip(ctx.getAuditSchemaName(), true));
		}
		return hasil;
	}

	/**
	 * Bentuk {@link SQLQuery} siap-parameter dari templat ber-schema tenant. Kombinasi
	 * {@link #siapkan(TenantContext, String)} + {@code session.createSQLQuery} — varian yang
	 * paling umum dipakai pemanggil yang langsung butuh objek query Hibernate, bukan sekadar
	 * teks SQL. Parameter kueri (mis. {@code :toko}, {@code :k}) TIDAK diikat di sini; itu
	 * tanggung jawab pemanggil lewat {@code setParameter} pada objek yang dikembalikan.
	 *
	 * @param session sesi Hibernate aktif
	 * @param ctx     konteks tenant aktif; TIDAK boleh {@code null}
	 * @param templat teks SQL mentah, lihat {@link #siapkan(TenantContext, String)}
	 * @return {@link SQLQuery} dengan penanda schema sudah tersubstitusi
	 * @throws IllegalArgumentException bila {@code templat} {@code null}
	 * @throws TenantAccessException    diteruskan dari {@link #siapkan(TenantContext, String)}
	 */
	public static SQLQuery sql(Session session, TenantContext ctx, String templat) {
		return session.createSQLQuery(siapkan(ctx, templat));
	}

	/**
	 * Benar bila templat ini menuntut schema tenant. Berguna bagi pemanggil yang menyediakan
	 * dua templat -- jalur schema tenant dan jalur shared lama -- lalu memilih salah satunya
	 * berdasarkan apakah tenant aktif berjalan pada mode HYBRID/TENANT_ONLY (butuh schema)
	 * atau LEGACY (tidak). Pemeriksaan murni tekstual, tidak menyentuh {@code ctx} maupun
	 * basis data; {@code null} dianggap tidak menuntut schema.
	 *
	 * @param templat teks SQL yang diperiksa
	 * @return {@code true} bila {@code templat} memuat {@link #PENANDA_DATA} atau
	 *         {@link #PENANDA_AUDIT}
	 */
	public static boolean butuhSchema(String templat) {
		return templat != null
				&& (templat.indexOf(PENANDA_DATA) >= 0 || templat.indexOf(PENANDA_AUDIT) >= 0);
	}

	/**
	 * Validasi ulang dan kutip satu nama schema (data atau audit) sebelum disisipkan ke SQL.
	 * Dipanggil oleh {@link #siapkan(TenantContext, String)} untuk kedua penanda — inilah
	 * satu-satunya tempat nama schema benar-benar disambung ke teks kueri. Validasi ulang di
	 * sini murah dan menutup kemungkinan {@code ctx} dibentuk lewat jalur lain yang lolos
	 * dari pemeriksaan awal (lihat catatan "Mengapa aman dari injeksi" pada javadoc kelas).
	 *
	 * @param schema nama schema mentah dari {@link TenantContext} (belum dikutip)
	 * @param audit  {@code true} bila {@code schema} adalah nama schema audit — memakai jalur
	 *               validasi {@link TenantSchemaLocator#pastikanAmanAudit}, yang membolehkan
	 *               panjang basis lebih pendek karena akhiran {@code __audit} tujuh karakter;
	 *               {@code false} memakai {@link TenantSchemaService#pastikanAman}
	 * @return nama schema terkutip ganda, siap disisipkan langsung ke teks SQL
	 * @throws TenantAccessException bila {@code schema} kosong/{@code null}
	 *                                ({@link TenantAccessException#TENANT_SCHEMA_INVALID}, tenant
	 *                                berjalan tanpa schema padahal kueri menuntutnya), atau
	 *                                bila validasi ulang menolaknya (konfigurasi schema tidak
	 *                                sah)
	 */
	private static String kutip(String schema, boolean audit) {
		if (schema == null || schema.trim().length() == 0) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Kueri ini menuntut schema tenant, tetapi tenant berjalan tanpa schema.");
		}
		// Validasi ulang: murah, dan menutup kemungkinan konteks dibentuk lewat jalur lain.
		String aman;
		try {
			// Nama audit divalidasi lewat basisnya -- pola pastikanAman membatasi 31 karakter
			// sedangkan akhiran __audit menambah tujuh. Lihat TenantSchemaLocator.pastikanAmanAudit.
			aman = audit ? TenantSchemaLocator.pastikanAmanAudit(schema.trim())
					: TenantSchemaService.pastikanAman(schema.trim());
		} catch (IllegalArgumentException e) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Konfigurasi schema tenant tidak sah.", e);
		}
		return "\"" + aman + "\"";
	}
	/**
	 * Bentuk {@link SQLQuery} berhalaman ber-schema tenant. Kombinasi
	 * {@link #sql(Session, TenantContext, String)} dengan pemasangan
	 * {@code setFirstResult}/{@code setMaxResults} Hibernate. {@code limit} dan
	 * {@code offset} <b>tidak</b> disambung ke teks SQL melainkan diserahkan ke API Hibernate
	 * tersebut, sehingga tidak ada jalan angka liar masuk ke SQL — lihat juga
	 * {@link #batasiLimit(int, int, int)} untuk menegakkan batas atas {@code limit} sebelum
	 * dipanggilkan ke sini.
	 *
	 * @param session sesi Hibernate aktif
	 * @param ctx     konteks tenant aktif; TIDAK boleh {@code null}
	 * @param templat teks SQL mentah, lihat {@link #siapkan(TenantContext, String)}
	 * @param limit   maksimum baris; nilai &le; 0 diabaikan (tidak memanggil
	 *                {@code setMaxResults}, artinya tidak ada batas dari method ini)
	 * @param offset  baris pertama; nilai &le; 0 diabaikan (mulai dari baris pertama)
	 * @return {@link SQLQuery} dengan penanda schema tersubstitusi dan batas halaman terpasang
	 * @throws IllegalArgumentException bila {@code templat} {@code null}
	 * @throws TenantAccessException    diteruskan dari {@link #siapkan(TenantContext, String)}
	 */
	public static SQLQuery sqlHalaman(Session session, TenantContext ctx, String templat,
			int limit, int offset) {
		SQLQuery q = sql(session, ctx, templat);
		if (offset > 0) {
			q.setFirstResult(offset);
		}
		if (limit > 0) {
			q.setMaxResults(limit);
		}
		return q;
	}

	/**
	 * Tegakkan batas halaman yang wajar sebelum {@code limit} diteruskan ke
	 * {@link #sqlHalaman(Session, TenantContext, String, int, int)}. Klien yang meminta
	 * sepuluh juta baris hampir selalu keliru, dan melayaninya berarti satu request
	 * menghabiskan memori seluruh kontainer — method ini adalah gerbang murah untuk
	 * mencegahnya tanpa menyentuh basis data.
	 *
	 * @param diminta  jumlah baris yang diminta klien; nilai &le; 0 dianggap "tidak menentukan
	 *                 apa-apa" dan diganti {@code bawaan}
	 * @param bawaan   nilai default yang dipakai bila {@code diminta} &le; 0
	 * @param maksimum batas atas mutlak; {@code diminta} yang melebihinya dipotong ke nilai ini
	 * @return {@code bawaan} bila {@code diminta} &le; 0; jika tidak, {@code diminta} sendiri
	 *         atau {@code maksimum} — mana yang lebih kecil
	 */
	public static int batasiLimit(int diminta, int bawaan, int maksimum) {
		if (diminta <= 0) {
			return bawaan;
		}
		return diminta > maksimum ? maksimum : diminta;
	}
}
