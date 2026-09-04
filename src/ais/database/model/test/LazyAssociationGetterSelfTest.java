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

	private static final Pattern LAZY_RELATION = Pattern.compile(
			"^\\s*@(javax\\.persistence\\.)?(ManyToOne|OneToOne)\\s*\\(.*"
					+ "fetch\\s*=\\s*(javax\\.persistence\\.)?FetchType\\.LAZY");
	private static final Pattern GETTER = Pattern.compile(
			"^\\s*public\\s+[^=;]+\\s+(get|is)[A-Za-z0-9_]+\\s*\\([^)]*\\)\\s*\\{");

	private LazyAssociationGetterSelfTest() {
	}

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

	private static File validasiRoot(File root) {
		if (!root.isDirectory()) {
			throw new IllegalArgumentException("Root model tidak ditemukan: " + root.getAbsolutePath());
		}
		return root;
	}

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

	private static int cariGetter(List<String> lines, int start) {
		int end = Math.min(lines.size(), start + 12);
		for (int i = start; i < end; i++) {
			if (GETTER.matcher(lines.get(i)).find()) {
				return i;
			}
		}
		return -1;
	}

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

	private static boolean aman(String method) {
		return method.indexOf("check(") >= 0
				|| method.indexOf("chek(") >= 0
				|| method.indexOf("resolveLazy(") >= 0
				|| method.indexOf("LAZY_GETTER_CHECK_EXCEPTION:") >= 0;
	}

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
