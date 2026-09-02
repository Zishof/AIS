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

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantSchemaMigrasiSelfTest() {
	}

	/**
	 * Patokan checksum bundel yang sudah dirilis: setiap baris {@code {versionCode, checksum}}
	 * (12 karakter pertama checksum kanonik, cukup untuk mendeteksi perubahan tanpa membuat
	 * tabel ini panjang). Diperiksa oleh {@link #periksaPatokan()}. Lihat catatan javadoc
	 * kelas sebelum mengubah — hanya boleh diubah bila versi tersebut belum terpasang pada
	 * tenant mana pun; sesudah itu, satu-satunya jalan yang benar adalah menambah baris BARU
	 * di akhir tabel untuk versi migrasi baru.
	 */
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
			{ "v10-celah-p4-erp", "0f15688a9184" },
	};

	/** Penghitung kegagalan lintas kedua blok uji; bukan JUnit sehingga dikelola manual. */
	private static int gagal;

	/**
	 * Titik masuk harness manual (bukan JUnit) untuk penjaga katalog migrasi tenant. Jalankan
	 * dengan {@code java ais.service.tenant.test.TenantSchemaMigrasiSelfTest}; tidak
	 * menyentuh basis data sama sekali — seluruh pemeriksaan murni terhadap definisi Java di
	 * memori ({@link TenantSchemaMigrations#SEMUA}). Menjalankan {@link #periksaPatokan()}
	 * lalu {@link #periksaStruktur()}, kemudian bila {@link #gagal} &gt; 0 melempar
	 * {@link IllegalStateException} berisi jumlah masalah; bila lolos, mencetak ringkasan
	 * jumlah migrasi dan versi terkini lalu memanggil {@code System.exit(0)} eksplisit.
	 *
	 * @param a tidak dipakai
	 * @throws Exception {@link IllegalStateException} bila ada satu atau lebih pemeriksaan
	 *                    yang gagal
	 */
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

	/**
	 * Catat satu kegagalan: cetak baris {@code GAGAL: <pesan>} ke {@code System.out} dan
	 * naikkan {@link #gagal}. Pengganti {@code assertTrue}/{@code fail} JUnit pada harness
	 * ini — tidak menghentikan eksekusi, hanya menambah hitungan, sehingga satu jalan uji
	 * dapat melaporkan banyak pelanggaran struktural sekaligus.
	 *
	 * @param pesan penjelasan kegagalan, ditulis apa adanya ke keluaran konsol
	 */
	private static void salah(String pesan) {
		System.out.println("  GAGAL: " + pesan);
		gagal++;
	}

	/**
	 * Bandingkan checksum kanonik saat ini terhadap {@link #PATOKAN} untuk setiap versi yang
	 * sudah dirilis. Dua kegagalan yang dideteksi: versi terpatok hilang dari katalog
	 * (pelanggaran append-only — versi yang sudah dirilis tidak boleh dihapus), atau
	 * checksum-nya berubah (DDL versi yang sudah dirilis disunting alih-alih menambah versi
	 * baru). Lihat catatan javadoc kelas mengenai bahaya menyunting konstanta bersama seperti
	 * {@code JEJAK} yang mengubah banyak pernyataan sekaligus tanpa terlihat pada diff sempit.
	 */
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

	/**
	 * Cari entri {@link TenantSchemaMigrations#SEMUA} berdasarkan {@code versionCode}.
	 * Helper privat dipakai {@link #periksaPatokan()} untuk mencocokkan tiap baris
	 * {@link #PATOKAN} dengan definisi kanonik saat ini.
	 *
	 * @param versi {@code versionCode} yang dicari (mis. {@code "v4-inventory-purchase-ap-erp"})
	 * @return entri {@link TenantSchemaMigrations.Migrasi} yang cocok, atau {@code null} bila
	 *         versi tersebut tidak ada dalam katalog
	 */
	private static TenantSchemaMigrations.Migrasi cari(String versi) {
		for (int i = 0; i < TenantSchemaMigrations.SEMUA.length; i++) {
			if (TenantSchemaMigrations.SEMUA[i].versionCode.equals(versi)) {
				return TenantSchemaMigrations.SEMUA[i];
			}
		}
		return null;
	}

	/**
	 * Pemeriksaan struktural yang hanya terlihat bila seluruh katalog dibaca sekaligus, lewat
	 * pemindaian regex atas teks DDL mentah tiap pernyataan (tanpa parser SQL sungguhan; lihat
	 * pola {@code pCreate}/{@code pRef}/{@code pIdx}/{@code pAlter}/{@code pSisa} lokal pada
	 * method ini). Dijalankan berurutan mengikuti definisi katalog agar referensi maju (FK ke
	 * tabel yang baru dibuat di versi berikutnya) benar-benar terdeteksi sebagai kesalahan:
	 *
	 * <ul>
	 * <li>{@code versionCode} ganda dalam katalog;</li>
	 * <li>bundel kosong ({@code m.ddl.length == 0}) — checksum-nya sudah tercatat di tenant
	 *     lama sehingga menambah DDL ke slot ini kelak akan menggagalkan migrasi;</li>
	 * <li>nama tabel ganda ({@code CREATE TABLE} untuk nama yang sudah dipakai);</li>
	 * <li>{@code REFERENCES}/{@code ALTER TABLE} yang menunjuk tabel yang belum dibuat pada
	 *     titik itu dalam urutan katalog;</li>
	 * <li>nama indeks ganda dalam satu schema;</li>
	 * <li>kurung buka/tutup tidak seimbang per pernyataan;</li>
	 * <li>placeholder {@code {...}} yang tersisa setelah substitusi {@code {S}}/{@code {A}}/
	 *     {@code {SU}} — berarti ada penanda yang salah ketik atau belum didukung;</li>
	 * <li>sintaks yang tidak didukung PostgreSQL 9.3: {@code jsonb}, {@code ON CONFLICT},
	 *     {@code CREATE INDEX IF NOT EXISTS} (baru di 9.5), {@code ADD COLUMN IF NOT EXISTS}
	 *     (baru di 9.6);</li>
	 * <li>kolom uang/kuantitas bertipe {@code double precision}/{@code float} alih-alih
	 *     {@code numeric};</li>
	 * <li>{@code TABEL_WAJIB_ERP}/{@code TABEL_WAJIB_AUDIT} yang menyebut tabel yang tidak
	 *     pernah benar-benar dibuat oleh katalog (kecuali {@code tenant_schema_migration},
	 *     yang dibuat {@code terapkanMigrasi}, bukan oleh bundel DDL manapun).</li>
	 * </ul>
	 *
	 * <p>Setiap pelanggaran dilaporkan lewat {@link #salah(String)}; method ini tidak
	 * melempar sendiri dan tidak berhenti pada pelanggaran pertama.</p>
	 */
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

	/**
	 * Potong teks SQL panjang agar pesan {@link #salah(String)} tetap terbaca di konsol.
	 *
	 * @param s teks SQL mentah (satu pernyataan)
	 * @return {@code s} apa adanya bila &le; 90 karakter, jika tidak 90 karakter pertama
	 *         diikuti {@code "..."}
	 */
	private static String potong(String s) {
		return s.length() > 90 ? s.substring(0, 90) + "..." : s;
	}
}
