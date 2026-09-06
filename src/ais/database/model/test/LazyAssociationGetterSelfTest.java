package ais.database.model.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pemeriksaan statis untuk mencegah getter relasi LAZY mengembalikan proxy mentah.
 *
 * <p>Setiap getter {@code @ManyToOne}/{@code @OneToOne(fetch = LAZY)} WAJIB memanggil
 * {@code check(...)}, {@code chek(...)}, atau {@code resolveLazy(...)} dan menugaskan hasilnya
 * kembali ke field. Pengecualian hanya diperbolehkan bila badan getter memuat penanda
 * {@code LAZY_GETTER_CHECK_EXCEPTION:} beserta alasan teknisnya.</p>
 *
 * <p>Jalankan dari root checkout:</p>
 * <pre>
 * java ais.database.model.test.LazyAssociationGetterSelfTest src/ais/database/model
 * </pre>
 */
public final class LazyAssociationGetterSelfTest {

	/** Pola regex baris anotasi yang menandai relasi {@code @ManyToOne}/{@code @OneToOne} ber-fetch {@code LAZY} — inilah yang memicu pemeriksaan getter berikutnya. */
	private static final Pattern LAZY_RELATION = Pattern.compile(
			"^\\s*@(javax\\.persistence\\.)?(ManyToOne|OneToOne)\\s*\\(.*"
					+ "fetch\\s*=\\s*(javax\\.persistence\\.)?FetchType\\.LAZY");
	/** Pola regex tanda tangan method getter/is publik — dipakai mencari method PERTAMA setelah anotasi {@link #LAZY_RELATION} yang cocok. */
	private static final Pattern GETTER = Pattern.compile(
			"^\\s*public\\s+[^=;]+\\s+(get|is)[A-Za-z0-9_]+\\s*\\([^)]*\\)\\s*\\{");

	/** Kelas utilitas murni method statis — konstruktor privat mencegah instansiasi. */
	private LazyAssociationGetterSelfTest() {
	}

	/**
	 * Titik masuk baris perintah — pindai direktori {@link #tentukanRoot(String[])} secara
	 * rekursif, kumpulkan setiap pelanggaran (getter relasi LAZY yang tidak memanggil
	 * {@code check(...)}/{@code chek(...)}/{@code resolveLazy(...)} dan tidak mencantumkan
	 * penanda pengecualian {@code LAZY_GETTER_CHECK_EXCEPTION:}), lalu keluar dengan status 1
	 * dan daftar pelanggaran ke {@code System.err} bila ditemukan minimal satu, atau status 0
	 * dengan ringkasan jumlah getter yang diperiksa ke {@code System.out} bila bersih. Dirancang
	 * dipanggil dari skrip build/CI sebagai gerbang kualitas otomatis terhadap pola arsitektur
	 * "getter relasi lazy yang lupa di-resolve" — pola yang SUDAH terbukti berulang kali menjadi
	 * akar bug (`LazyInitializationException` di luar sesi Hibernate) di puluhan entity
	 * sepanjang inisiatif dokumentasi Javadoc proyek ini (lihat memori proyek: pola
	 * getter-destruktif/getter-menulis-balik yang justru MEMANGGIL {@code check(...)} adalah
	 * bentuk YANG BENAR dari pola ini, meski efek sampingnya — menulis balik hasil resolusi ke
	 * field — punya konsekuensi arsitektur tersendiri yang tercatat terpisah).
	 *
	 * @param args argumen baris perintah opsional; elemen pertama (bila ada) menimpa direktori
	 *        akar yang dipindai, lihat {@link #tentukanRoot(String[])}
	 */
	public static void main(String[] args) throws Exception {
		File root = tentukanRoot(args);
		List<String> violations = new ArrayList<String>();
		int checked = scan(root, violations);

		if (!violations.isEmpty()) {
			System.err.println("GAGAL: getter relasi LAZY wajib memakai check(...) dan assign-back.");
			for (String violation : violations) {
				System.err.println(" - " + violation);
			}
			System.err.println("Gunakan: field = check(field); return field;");
			System.exit(1);
		}

		System.out.println("LazyAssociationGetterSelfTest OK: " + checked
				+ " getter relasi LAZY diperiksa.");
	}

	/**
	 * Menentukan direktori akar yang dipindai: argumen baris perintah pertama bila diberikan,
	 * atau tebakan {@code src/ais/database/model} (dijalankan dari root checkout SVN) diikuti
	 * fallback {@code src/main/src/ais/database/model} (struktur direktori WC proyek ini).
	 */
	private static File tentukanRoot(String[] args) {
		if (args != null && args.length > 0) {
			return validasiRoot(new File(args[0]));
		}
		File rootGit = new File("src/ais/database/model");
		if (rootGit.isDirectory()) {
			return rootGit;
		}
		return validasiRoot(new File("src/main/src/ais/database/model"));
	}

	/** Melempar {@link IllegalArgumentException} bila {@code root} bukan direktori yang benar-benar ada; dipakai memvalidasi hasil {@link #tentukanRoot(String[])}. */
	private static File validasiRoot(File root) {
		if (!root.isDirectory()) {
			throw new IllegalArgumentException("Root model tidak ditemukan: " + root.getAbsolutePath());
		}
		return root;
	}

	/**
	 * Rekursi berjalan-pohon-direktori: turun ke sub-direktori, dan untuk tiap berkas
	 * {@code .java} panggil {@link #scanJava(File, List)}. Mengembalikan total getter relasi
	 * LAZY yang diperiksa di seluruh subtree (bukan jumlah file).
	 */
	private static int scan(File file, List<String> violations) throws IOException {
		if (file.isDirectory()) {
			int total = 0;
			File[] children = file.listFiles();
			if (children == null) {
				return 0;
			}
			for (File child : children) {
				total += scan(child, violations);
			}
			return total;
		}
		if (!file.getName().endsWith(".java")) {
			return 0;
		}
		return scanJava(file, violations);
	}

	/**
	 * Memindai SATU berkas {@code .java} baris-per-baris: setiap kali baris cocok
	 * {@link #LAZY_RELATION}, cari method getter berikutnya dalam jarak maksimal 12 baris
	 * (lihat {@link #cariGetter(List, int)}) lalu periksa isinya lewat {@link #aman(String)}.
	 * Getter yang tidak lolos ditambahkan ke {@code violations} dengan lokasi
	 * {@code path:baris}. Mengembalikan jumlah getter LAZY yang berhasil diperiksa di berkas
	 * ini (termasuk yang lolos maupun gagal).
	 */
	private static int scanJava(File file, List<String> violations) throws IOException {
		List<String> lines = bacaBaris(file);
		int checked = 0;
		for (int i = 0; i < lines.size(); i++) {
			if (!LAZY_RELATION.matcher(lines.get(i)).find()) {
				continue;
			}
			int getterLine = cariGetter(lines, i + 1);
			if (getterLine < 0) {
				continue;
			}
			String method = bacaMethod(lines, getterLine);
			checked++;
			if (!aman(method)) {
				violations.add(file.getPath() + ":" + (getterLine + 1) + " "
						+ lines.get(getterLine).trim());
			}
		}
		return checked;
	}

	/**
	 * Mencari baris method getter/is publik PERTAMA yang cocok {@link #GETTER} dalam jendela
	 * maksimal 12 baris mulai dari {@code start} — jarak ini mengasumsikan javadoc/anotasi lain
	 * di antara deklarasi field ber-anotasi relasi dan method getternya tidak lebih dari 12
	 * baris (konvensi penulisan entity di codebase ini). Mengembalikan indeks baris (0-based),
	 * atau {@code -1} bila tidak ditemukan dalam jendela tersebut (mis. anotasi menempel pada
	 * field tanpa getter langsung mengikutinya).
	 */
	private static int cariGetter(List<String> lines, int start) {
		int end = Math.min(lines.size(), start + 12);
		for (int i = start; i < end; i++) {
			if (GETTER.matcher(lines.get(i)).find()) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Membaca seluruh badan method mulai baris {@code start} sampai kurung kurawal penutup yang
	 * cocok (penghitungan kedalaman {@code {}/}} naif berbasis karakter — tidak menangani kurung
	 * kurawal di dalam string literal/komentar, cukup akurat untuk kode entity yang diproduksi
	 * hbm2java/gaya seragam di codebase ini). Mengembalikan teks lengkap method sebagai satu
	 * string untuk diperiksa {@link #aman(String)}.
	 */
	private static String bacaMethod(List<String> lines, int start) {
		StringBuilder method = new StringBuilder();
		int depth = 0;
		boolean started = false;
		for (int i = start; i < lines.size(); i++) {
			String line = lines.get(i);
			method.append(line).append('\n');
			for (int c = 0; c < line.length(); c++) {
				char ch = line.charAt(c);
				if (ch == '{') {
					depth++;
					started = true;
				} else if (ch == '}') {
					depth--;
				}
			}
			if (started && depth == 0) {
				break;
			}
		}
		return method.toString();
	}

	/**
	 * Menentukan apakah badan method getter dianggap AMAN: harus mengandung panggilan ke salah
	 * satu {@code check(}/{@code chek(} (dua ejaan berbeda ditemukan dipakai bergantian di
	 * codebase — bukan typo yang perlu diseragamkan, keduanya sengaja diterima) /
	 * {@code resolveLazy(}, ATAU mencantumkan penanda pengecualian eksplisit
	 * {@code LAZY_GETTER_CHECK_EXCEPTION:} (dipakai bila memang ada alasan teknis sah untuk
	 * TIDAK me-resolve proxy, mis. getter yang sengaja mengembalikan proxy mentah untuk
	 * pemeriksaan {@code == null} murni tanpa memicu inisialisasi). Pencarian substring naif
	 * (bukan parsing AST) — cukup untuk gaya kode seragam di seluruh entity AIS.
	 */
	private static boolean aman(String method) {
		return method.indexOf("check(") >= 0
				|| method.indexOf("chek(") >= 0
				|| method.indexOf("resolveLazy(") >= 0
				|| method.indexOf("LAZY_GETTER_CHECK_EXCEPTION:") >= 0;
	}

	/** Membaca seluruh isi {@code file} sebagai daftar baris teks (satu elemen per baris, tanpa karakter newline). */
	private static List<String> bacaBaris(File file) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		} finally {
			reader.close();
		}
		return lines;
	}
}
