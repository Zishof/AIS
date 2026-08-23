package ais.service.tenant.test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ais.service.tenant.TenantSchemaMigrations;

/**
 * <h3>Penjaga katalog migrasi tenant — dijalankan tanpa basis data.</h3>
 *
 * <p>Dua tugas. Pertama, <b>mematok checksum</b> bundel yang sudah dirilis: DDL kanonik
 * masuk ke checksum, dan checksum yang berubah membuat {@code terapkanMigrasi} <b>gagal keras
 * di setiap tenant yang sudah memasangnya</b>. Bahayanya halus — menyunting satu konstanta
 * bersama seperti {@code JEJAK} mengubah belasan pernyataan sekaligus tanpa terlihat pada
 * diff yang sempit. Uji ini membuat perubahan semacam itu berisik.</p>
 *
 * <p>Kedua, memeriksa hal-hal yang hanya terlihat bila seluruh katalog dibaca sekaligus:
 * referensi FK yang menunjuk tabel yang belum dibuat, nama tabel/indeks ganda, kurung tidak
 * seimbang, placeholder yang tidak tersubstitusi, sintaks yang tidak didukung PostgreSQL 9.3,
 * dan {@code TABEL_WAJIB_*} yang menyebut tabel yang tidak pernah dibuat.</p>
 *
 * <h4>Mengubah patokan</h4>
 * <p>Boleh <b>hanya</b> bila versi tersebut belum terpasang pada tenant mana pun. Sesudah
 * terpasang, satu-satunya jalan yang benar adalah menambah versi BARU di akhir katalog.</p>
 *
 * <p>Jalankan: {@code java ais.service.tenant.test.TenantSchemaMigrasiSelfTest}</p>
 */
public final class TenantSchemaMigrasiSelfTest {

	private TenantSchemaMigrasiSelfTest() {
	}

	/** versionCode diikuti checksum yang dipatok. Lihat catatan kelas sebelum mengubah. */
	private static final String[][] PATOKAN = {
			{ "v1-core-pos-erp", "4b8bff529787" },
			{ "v1-core-pos-audit", "c8809f088a65" },
			{ "v2-inventory-master-erp", "71da30d6bde0" },
			{ "v2-inventory-master-audit", "fad929a513ba" },
			{ "v3-inventory-stock-erp", "d927f23f616e" },
			{ "v4-inventory-purchase-ap-erp", "01c8591d365c" },
			{ "v5-inventory-sales-ar-erp", "3c2d15be476a" },
			{ "v6-inventory-trip-erp", "ba337bc0c2f5" },
			{ "v7-inventory-accounting-erp", "4f03b3b8dc12" },
			{ "v8-inventory-import-erp", "7f1cacf8f3c4" },
			{ "v9-pos-ebisnis-erp", "866f7b5e4324" },
	};

	private static int gagal;

	public static void main(String[] a) throws Exception {
		periksaPatokan();
		periksaStruktur();
		if (gagal > 0) {
			throw new IllegalStateException(gagal + " masalah pada katalog migrasi tenant.");
		}
		System.out.println("TenantSchemaMigrasiSelfTest OK ("
				+ TenantSchemaMigrations.SEMUA.length + " migrasi, versi terkini "
				+ TenantSchemaMigrations.VERSI_TERKINI + ")");
		System.exit(0);
	}

	private static void salah(String pesan) {
		System.out.println("  GAGAL: " + pesan);
		gagal++;
	}

	private static void periksaPatokan() {
		for (int i = 0; i < PATOKAN.length; i++) {
			String versi = PATOKAN[i][0];
			String diharapkan = PATOKAN[i][1];
			TenantSchemaMigrations.Migrasi m = cari(versi);
			if (m == null) {
				salah("versi terpatok hilang dari katalog: " + versi
						+ " -- menghapus versi yang sudah dirilis melanggar append-only");
				continue;
			}
			String nyata = m.checksum().substring(0, diharapkan.length());
			if (!diharapkan.equals(nyata)) {
				salah("checksum " + versi + " berubah: " + diharapkan + " -> " + nyata
						+ " -- DDL versi yang sudah dirilis tidak boleh disunting;"
						+ " tambahkan versi BARU di akhir katalog");
			}
		}
	}

	private static TenantSchemaMigrations.Migrasi cari(String versi) {
		for (int i = 0; i < TenantSchemaMigrations.SEMUA.length; i++) {
			if (TenantSchemaMigrations.SEMUA[i].versionCode.equals(versi)) {
				return TenantSchemaMigrations.SEMUA[i];
			}
		}
		return null;
	}

	private static void periksaStruktur() {
		String erp = "tenant_uji";
		String audit = erp + "__audit";
		Set<String> tabelErp = new HashSet<String>();
		Set<String> tabelAudit = new HashSet<String>();
		Set<String> indeks = new HashSet<String>();
		Set<String> versi = new HashSet<String>();

		Pattern pCreate = Pattern.compile("CREATE TABLE (IF NOT EXISTS )?\\{([SA])\\}\\.([a-z_]+)");
		Pattern pRef = Pattern.compile("REFERENCES \\{([SA])\\}\\.([a-z_]+)");
		Pattern pIdx = Pattern.compile("CREATE (?:UNIQUE )?INDEX ([a-zA-Z0-9_{}]+) ON \\{([SA])\\}\\.([a-z_]+)");
		Pattern pAlter = Pattern.compile("ALTER TABLE \\{([SA])\\}\\.([a-z_]+)");
		Pattern pSisa = Pattern.compile("\\{[^}]*\\}");

		for (int i = 0; i < TenantSchemaMigrations.SEMUA.length; i++) {
			TenantSchemaMigrations.Migrasi m = TenantSchemaMigrations.SEMUA[i];
			if (!versi.add(m.versionCode)) {
				salah("versionCode ganda: " + m.versionCode);
			}
			if (m.ddl.length == 0) {
				salah("bundel kosong " + m.versionCode + " -- checksum-nya akan tercatat pada"
						+ " tenant, sehingga menambah DDL ke slot ini kelak menggagalkan migrasi");
			}
			for (int d = 0; d < m.ddl.length; d++) {
				String sql = m.ddl[d];

				Matcher mc = pCreate.matcher(sql);
				while (mc.find()) {
					Set<String> ke = "S".equals(mc.group(2)) ? tabelErp : tabelAudit;
					if (!ke.add(mc.group(3))) {
						salah("tabel ganda: " + mc.group(3) + " (" + m.versionCode + ")");
					}
				}
				Matcher mr = pRef.matcher(sql);
				while (mr.find()) {
					Set<String> ada = "S".equals(mr.group(1)) ? tabelErp : tabelAudit;
					if (!ada.contains(mr.group(2))) {
						salah("FK menunjuk tabel yang belum dibuat: " + mr.group(2)
								+ " (" + m.versionCode + ")");
					}
				}
				Matcher ma = pAlter.matcher(sql);
				while (ma.find()) {
					Set<String> ada = "S".equals(ma.group(1)) ? tabelErp : tabelAudit;
					if (!ada.contains(ma.group(2))) {
						salah("ALTER atas tabel yang belum dibuat: " + ma.group(2));
					}
				}
				Matcher mi = pIdx.matcher(sql);
				while (mi.find()) {
					Set<String> ada = "S".equals(mi.group(2)) ? tabelErp : tabelAudit;
					if (!ada.contains(mi.group(3))) {
						salah("indeks atas tabel yang belum dibuat: " + mi.group(3));
					}
					if (!indeks.add(mi.group(1) + "@" + mi.group(2))) {
						salah("nama indeks ganda dalam satu schema: " + mi.group(1));
					}
				}

				int buka = 0;
				for (int c = 0; c < sql.length(); c++) {
					char ch = sql.charAt(c);
					if (ch == '(') {
						buka++;
					} else if (ch == ')') {
						buka--;
					}
				}
				if (buka != 0) {
					salah("kurung tidak seimbang pada " + m.versionCode + ": " + potong(sql));
				}

				String jadi = sql.replace("{S}", "\"" + erp + "\"")
						.replace("{A}", "\"" + audit + "\"").replace("{SU}", erp);
				Matcher ms = pSisa.matcher(jadi);
				if (ms.find()) {
					salah("placeholder tersisa " + ms.group() + ": " + potong(sql));
				}

				String u = sql.toUpperCase();
				if (u.indexOf("JSONB") >= 0) {
					salah("jsonb tidak ada di PostgreSQL 9.3: " + potong(sql));
				}
				if (u.indexOf("ON CONFLICT") >= 0) {
					salah("ON CONFLICT tidak ada di 9.3: " + potong(sql));
				}
				if (u.indexOf("CREATE INDEX IF NOT EXISTS") >= 0) {
					salah("CREATE INDEX IF NOT EXISTS baru ada di 9.5: " + potong(sql));
				}
				if (u.indexOf("ADD COLUMN IF NOT EXISTS") >= 0) {
					salah("ADD COLUMN IF NOT EXISTS baru ada di 9.6: " + potong(sql));
				}
				if (u.indexOf(" DOUBLE PRECISION") >= 0 || u.indexOf(" FLOAT ") >= 0) {
					salah("uang/kuantitas wajib numeric, bukan float: " + potong(sql));
				}
			}
		}

		for (int i = 0; i < TenantSchemaMigrations.TABEL_WAJIB_ERP.length; i++) {
			String t = TenantSchemaMigrations.TABEL_WAJIB_ERP[i];
			if ("tenant_schema_migration".equals(t)) {
				continue; // dibuat terapkanMigrasi, bukan oleh katalog
			}
			if (!tabelErp.contains(t)) {
				salah("TABEL_WAJIB_ERP menyebut tabel yang tidak pernah dibuat: " + t);
			}
		}
		for (int i = 0; i < TenantSchemaMigrations.TABEL_WAJIB_AUDIT.length; i++) {
			String t = TenantSchemaMigrations.TABEL_WAJIB_AUDIT[i];
			if (!tabelAudit.contains(t)) {
				salah("TABEL_WAJIB_AUDIT menyebut tabel yang tidak pernah dibuat: " + t);
			}
		}
	}

	private static String potong(String s) {
		return s.length() > 90 ? s.substring(0, 90) + "..." : s;
	}
}
