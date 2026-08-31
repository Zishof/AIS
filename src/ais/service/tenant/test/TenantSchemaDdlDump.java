package ais.service.tenant.test;

import ais.service.tenant.TenantSchemaMigrations;

/**
 * <h3>Mencetak seluruh DDL katalog tenant sebagai skrip SQL siap jalan.</h3>
 *
 * <p>Alat verifikasi, bukan bagian jalur runtime. Gunanya satu: menjalankan katalog
 * migrasi pada PostgreSQL sungguhan <b>tanpa</b> menyalakan Hibernate, container, atau
 * menyentuh basis data produksi -- sehingga kesalahan sintaks DDL ketahuan sebelum ada
 * tenant yang terlanjur di-provision.</p>
 *
 * <h4>Substitusi identifier</h4>
 * <p>Meniru persis {@code TenantSchemaService.terapkanMigrasi}: {@code {S}} dan {@code {A}}
 * menjadi nama schema <b>ber-kutip-ganda</b>, sedangkan {@code {SU}} dipakai apa adanya
 * karena ia hanya menyusun nama indeks. Bila aturan di sana berubah, ubah juga di sini --
 * skrip yang berbeda dari runtime tidak membuktikan apa pun.</p>
 *
 * <h4>Cara pakai</h4>
 * <pre>
 * javac -sourcepath src/main/java -d out src/main/java/ais/service/tenant/test/TenantSchemaDdlDump.java
 * java  -cp out ais.service.tenant.test.TenantSchemaDdlDump uji_tenant uji_tenant__audit &gt; katalog.sql
 * psql -v ON_ERROR_STOP=1 -f katalog.sql
 * </pre>
 *
 * <p>Tabel riwayat {@code <schema>.tenant_schema_migration} <b>tidak</b> ikut tercetak: ia
 * dibuat oleh {@code TenantSchemaService} sendiri, bukan oleh bundel DDL. Jadi jumlah tabel
 * hasil skrip ini adalah {@code TABEL_WAJIB_ERP.length - 1}.</p>
 */
public final class TenantSchemaDdlDump {

	private TenantSchemaDdlDump() {
	}

	public static void main(String[] args) {
		String erp = args.length > 0 ? args[0] : "uji_tenant";
		String audit = args.length > 1 ? args[1] : erp + "__audit";
		System.out.println("-- Dihasilkan TenantSchemaDdlDump. Jangan disunting tangan.");
		System.out.println("-- Schema ERP: " + erp + " | Schema audit: " + audit);
		System.out.println("CREATE SCHEMA IF NOT EXISTS \"" + erp + "\";");
		System.out.println("CREATE SCHEMA IF NOT EXISTS \"" + audit + "\";");
		for (int i = 0; i < TenantSchemaMigrations.SEMUA.length; i++) {
			TenantSchemaMigrations.Migrasi m = TenantSchemaMigrations.SEMUA[i];
			System.out.println();
			System.out.println("-- === " + m.versionCode + " (" + m.target + ") -- "
					+ m.ddl.length + " pernyataan, checksum "
					+ m.checksum().substring(0, 12) + " ===");
			for (int d = 0; d < m.ddl.length; d++) {
				String sql = m.ddl[d].replace("{S}", "\"" + erp + "\"")
						.replace("{A}", "\"" + audit + "\"")
						.replace("{SU}", erp);
				System.out.println(sql + ";");
			}
		}
	}
}
