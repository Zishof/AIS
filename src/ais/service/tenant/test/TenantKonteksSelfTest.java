package ais.service.tenant.test;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import ais.service.tenant.TenantAccessException;
import ais.service.tenant.TenantContext;
import ais.service.tenant.TenantSchemaLocator;
import ais.service.tenant.TenantSqlExecutor;

/**
 * <h3>Penjaga konteks tenant — nama schema, substitusi SQL, dan kebocoran ke klien.</h3>
 *
 * <p>Tiga hal yang tidak terlihat pada kompilasi tetapi mahal bila salah: nama schema audit
 * yang panjang, nama schema yang bocor ke response, dan penanda SQL yang tidak tersubstitusi.</p>
 *
 * <p>Jalankan: {@code java ais.service.tenant.test.TenantKonteksSelfTest}</p>
 *
 * <p><b>Berisik tanpa konfigurasi.</b> {@code pastikanAman} memanggil
 * {@code PendaftaranValidationService.usernameReserved}, yang membaca konfigurasi dan karena
 * itu menyentuh Hibernate. Tanpa {@code hibernate.cfg.xml} pada classpath, jejak tumpukan
 * akan membanjiri keluaran — perilakunya fail-open sehingga validasi tetap benar. Baca baris
 * {@code GAGAL:} dan baris terakhir, abaikan sisanya.</p>
 */
public final class TenantKonteksSelfTest {

	private TenantKonteksSelfTest() {
	}

	private static int gagal;

	private static void salah(String pesan) {
		System.out.println("  GAGAL: " + pesan);
		gagal++;
	}

	private static void benar(boolean syarat, String pesan) {
		if (!syarat) {
			salah(pesan);
		}
	}

	public static void main(String[] a) throws Exception {
		ujiNamaSchemaAudit();
		ujiTidakBocorKeKlien();
		ujiSubstitusiSql();
		if (gagal > 0) {
			throw new IllegalStateException(gagal + " masalah pada konteks tenant.");
		}
		System.out.println("TenantKonteksSelfTest OK");
		System.exit(0);
	}

	/**
	 * Regresi: pola {@code pastikanAman} membatasi 31 karakter, sedangkan nama audit adalah
	 * nama data + {@code __audit} (tujuh karakter). Slug 25 karakter ke atas karena itu
	 * <b>lolos provisioning</b> dan schema auditnya benar-benar dibuat, tetapi nama turunannya
	 * ditolak bila divalidasi dengan pola yang sama — setiap kueri audit tenant itu gagal
	 * padahal schema-nya ada. Contoh pada dokumen master hanya satu karakter di bawah batas.
	 */
	private static void ujiNamaSchemaAudit() {
		// Panjang 24 -> audit 31, tepat di batas pola lama. Harus lolos.
		terima("caruban_medika_nusantara__audit");
		// Panjang 27 -> audit 34. Pola lama MENOLAK ini; validator audit harus menerimanya.
		terima("apotek_sumber_sehat_sentosa__audit");
		// Panjang 30 -> audit 37, basis masih sah.
		terima("perusahaan_dagang_maju_jaya_ab__audit");

		// Tanpa akhiran baku -> ditolak.
		tolak("caruban_medika_nusantara");
		// Basis tidak sah (diawali angka) -> ditolak.
		tolak("1caruban__audit");
		// Basis 31 karakter masih SAH: pola [a-z] + {2,30} menerima 3..31. Batasnya di 32.
		terima("perusahaan_dagang_maju_jaya_abc__audit");
		// Basis 32 karakter -> ditolak.
		tolak("perusahaan_dagang_maju_jaya_abcd__audit");
		// Basis memuat karakter terlarang -> ditolak.
		tolak("tenant-a__audit");
		tolak(null);
	}

	private static void terima(String nama) {
		try {
			String hasil = TenantSchemaLocator.pastikanAmanAudit(nama);
			benar(nama.equals(hasil), "pastikanAmanAudit mengubah nama: " + nama + " -> " + hasil);
		} catch (IllegalArgumentException e) {
			salah("nama audit sah justru DITOLAK: " + nama);
		}
	}

	private static void tolak(String nama) {
		try {
			TenantSchemaLocator.pastikanAmanAudit(nama);
			salah("nama audit tidak sah justru DITERIMA: " + nama);
		} catch (IllegalArgumentException diharapkan) {
			// benar
		}
	}

	/**
	 * §4.7 dan §7.2: nama schema tidak boleh sampai ke klien. Diuji atas teks JSON-nya, bukan
	 * atas daftar medan — penambahan medan baru kelak tetap tertangkap.
	 */
	private static void ujiTidakBocorKeKlien() throws Exception {
		Set<String> modul = new HashSet<String>();
		modul.add("INVENTORY_SALES");
		TenantContext ctx = TenantContext.builder()
				.tenantId(Long.valueOf(12))
				.tenantCode("TEN-2026-000012")
				.tenantName("Caruban Medika Nusantara")
				.tenantStatus("ACTIVE")
				.tenantMode("TENANT_ONLY")
				.membershipId(Long.valueOf(7))
				.membershipRole("OWNER")
				.activeTbmuserId("budi")
				.schemaName("caruban_medika_nusantara")
				.auditSchemaName("caruban_medika_nusantara__audit")
				.schemaVersion("v8-inventory-import")
				.timezone("Asia/Jakarta")
				.locale("id_ID")
				.moduleEntitlements(modul)
				.build();

		JSONObject j = ctx.toJsonKlien();
		String teks = j.toString();
		benar(teks.indexOf("caruban_medika_nusantara") < 0,
				"nama schema BOCOR ke toJsonKlien(): " + teks);
		benar(teks.indexOf("__audit") < 0, "nama schema audit BOCOR ke toJsonKlien(): " + teks);
		benar(teks.indexOf("v8-inventory-import") < 0,
				"schemaVersion BOCOR ke toJsonKlien(): " + teks);
		// yang memang harus ada
		benar(teks.indexOf("TEN-2026-000012") >= 0, "tenantCode hilang dari toJsonKlien()");
		benar(teks.indexOf("OWNER") >= 0, "membershipRole hilang dari toJsonKlien()");
		benar(ctx.punyaModul("inventory_sales"), "punyaModul harus abai huruf besar-kecil");
		benar(!ctx.punyaModul("apotik"), "punyaModul mengaku punya modul yang tidak ada");
	}

	private static void ujiSubstitusiSql() {
		Set<String> modul = new HashSet<String>();
		TenantContext tenant = TenantContext.builder()
				.tenantId(Long.valueOf(1)).tenantMode("TENANT_ONLY")
				.schemaName("tenant_uji").auditSchemaName("tenant_uji__audit")
				.moduleEntitlements(modul).build();

		String sql = TenantSqlExecutor.siapkan(tenant, "SELECT id FROM {t}.produk WHERE kode = :k");
		benar(sql.indexOf("\"tenant_uji\".produk") >= 0, "penanda {t} tidak tersubstitusi: " + sql);
		benar(sql.indexOf("{") < 0, "masih ada penanda tersisa: " + sql);

		String sqlAudit = TenantSqlExecutor.siapkan(tenant, "SELECT rev FROM {a}.revinfo");
		benar(sqlAudit.indexOf("\"tenant_uji__audit\".revinfo") >= 0,
				"penanda {a} tidak tersubstitusi: " + sqlAudit);

		benar(TenantSqlExecutor.butuhSchema("SELECT 1 FROM {t}.produk"), "butuhSchema salah");
		benar(!TenantSqlExecutor.butuhSchema("SELECT 1 FROM koperasi.produk"),
				"butuhSchema keliru menandai SQL tanpa penanda");

		// Mode LEGACY: kueri ber-penanda harus DITOLAK, bukan diam-diam jatuh ke schema shared.
		TenantContext legacy = TenantContext.builder()
				.tenantId(Long.valueOf(2)).tenantMode("LEGACY")
				.moduleEntitlements(modul).build();
		benar(!legacy.pakaiSchemaTenant(), "tenant LEGACY tidak boleh mengaku pakai schema tenant");
		try {
			TenantSqlExecutor.siapkan(legacy, "SELECT 1 FROM {t}.produk");
			salah("kueri ber-penanda pada tenant LEGACY justru DITERIMA -- ini fallback senyap"
					+ " ke schema shared yang §12.4 larang");
		} catch (TenantAccessException diharapkan) {
			benar(TenantAccessException.TENANT_SCHEMA_INVALID.equals(diharapkan.getKode()),
					"kode galat salah: " + diharapkan.getKode());
		}

		// Batas halaman
		benar(TenantSqlExecutor.batasiLimit(0, 50, 500) == 50, "limit bawaan salah");
		benar(TenantSqlExecutor.batasiLimit(10000, 50, 500) == 500, "limit maksimum tidak ditegakkan");
		benar(TenantSqlExecutor.batasiLimit(100, 50, 500) == 100, "limit sah diubah");
	}
}
