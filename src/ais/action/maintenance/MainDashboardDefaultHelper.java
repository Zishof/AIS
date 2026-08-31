package ais.action.maintenance;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zul.Comboitem;

import ais.common.Common;
import ais.ui.util.MyWindow;

/**
 * Helper paket (bukan API publik) yang membangun daftar pilihan "dasbor/beranda default" bagi
 * pengguna: gabungan modul utama tetap (mis. e-Learning, Pustaka, Workflow/SOP, Toko, POS,
 * Kepegawaian — masing-masing berkode {@code main:<nama>}) dan seluruh kelas jendela dasbor
 * kustom ({@code *DashboardWindow}) yang ditemukan secara otomatis lewat pemindaian berkas sumber
 * {@code .java} (atau, sebagai cadangan bila sumber tidak tersedia di lingkungan deploy, berkas
 * {@code .class} terkompilasi) di bawah paket {@code ais.action} (dikode {@code class:<nama
 * kelas lengkap>}). Dipakai oleh layar pengaturan agar admin dapat memilih tampilan apa yang
 * muncul sebagai beranda default tanpa perlu mendaftarkan setiap kelas dasbor secara manual.
 *
 * <p>
 * {@link #createWindow(String)} membuat instance {@link MyWindow} dari kode pilihan berawalan
 * {@code class:} lewat refleksi (constructor tanpa-argumen, termasuk yang privat, diaktifkan lewat
 * {@code setAccessible(true)}), dengan pengaman: hanya kelas yang benar-benar merupakan turunan
 * {@link MyWindow} yang diinstansiasi. Nilai yang diinstansiasi berasal dari daftar pilihan yang
 * dibangun sendiri oleh kelas ini (hasil pemindaian berkas), bukan input bebas dari pengguna akhir.
 * </p>
 */
final class MainDashboardDefaultHelper {

	static final String MAIN_ELEARNING = "main:elearning";
	static final String MAIN_PRESTASI = "main:prestasi";
	static final String MAIN_PUSTAKA = "main:pustaka";
	static final String MAIN_WORKFLOW = "main:workflow";
	static final String MAIN_REPOSITORY = "main:repository";
	static final String MAIN_ANTAR_JEMPUT = "main:antarJemput";
	static final String MAIN_SPMI = "main:spmi";
	static final String MAIN_TOKO = "main:toko";
	static final String MAIN_POS = "main:pos";
	static final String MAIN_KOPERASI = "main:koperasi";
	static final String MAIN_EMEDIC = "main:emedic";
	static final String MAIN_GAJI = "main:gaji";
	static final String MAIN_AKADEMIK = "main:akademik";
	static final String MAIN_ADMINISTRASI = "main:administrasi";
	static final String MAIN_PENGADAAN = "main:pengadaan";
	static final String MAIN_PEMBAYARAN = "main:pembayaran";
	static final String MAIN_AKUNTANSI = "main:akuntansi";
	static final String MAIN_KINERJA = "main:kinerja";
	static final String MAIN_KEPEGAWAIAN = "main:kepegawaian";
	static final String MAIN_KEUANGAN = "main:keuangan";
	static final String MAIN_PRESENSI = "main:presensi";
	static final String MAIN_KALENDER = "main:kalenderAkademik";
	static final String MAIN_INFO_KEGIATAN = "main:infoKegiatan";
	static final String MAIN_FEEDER = "main:feeder";
	static final String MAIN_SISTER = "main:sister";

	private static final String CLASS_PREFIX = "class:";

	private MainDashboardDefaultHelper() {
	}

	/** @return seluruh pilihan dasbor default: modul utama tetap diikuti kelas {@code *DashboardWindow} hasil pemindaian, tanpa duplikat (dikunci per {@code value} lewat {@link LinkedHashMap}). */
	static List<Option> options() {
		LinkedHashMap<String, Option> map = new LinkedHashMap<String, Option>();
		addMain(map, MAIN_ELEARNING, "e-Learning");
		addMain(map, MAIN_PRESTASI, "Prestasi");
		addMain(map, MAIN_PUSTAKA, "Pustaka");
		addMain(map, MAIN_WORKFLOW, "Workflow / SOP");
		addMain(map, MAIN_REPOSITORY, "Repository");
		addMain(map, MAIN_ANTAR_JEMPUT, "Antar Jemput");
		addMain(map, MAIN_SPMI, "SPMI");
		addMain(map, MAIN_TOKO, "Toko");
		addMain(map, MAIN_POS, "POS");
		addMain(map, MAIN_KOPERASI, "Koperasi");
		addMain(map, MAIN_EMEDIC, "eMedic");
		addMain(map, MAIN_GAJI, "Gaji");
		addMain(map, MAIN_AKADEMIK, "Akademik");
		addMain(map, MAIN_ADMINISTRASI, "Administrasi / Surat Menyurat");
		addMain(map, MAIN_PENGADAAN, "Pengadaan Barang/Jasa");
		addMain(map, MAIN_PEMBAYARAN, "Piutang dan Pembayaran");
		addMain(map, MAIN_AKUNTANSI, "Akuntansi");
		addMain(map, MAIN_KINERJA, "Kinerja");
		addMain(map, MAIN_KEPEGAWAIAN, "Kepegawaian");
		addMain(map, MAIN_KEUANGAN, "Keuangan");
		addMain(map, MAIN_PRESENSI, "Presensi");
		addMain(map, MAIN_KALENDER, "Kalender Akademik");
		addMain(map, MAIN_INFO_KEGIATAN, "Info Kegiatan");
		addMain(map, MAIN_FEEDER, "Neo Feeder");
		addMain(map, MAIN_SISTER, "SISTER");

		List<Option> classOptions = scanDashboardWindowClasses();
		classOptions.addAll(scanCompiledDashboardWindowClasses());
		Collections.sort(classOptions, new Comparator<Option>() {
			public int compare(Option a, Option b) {
				return a.label.compareToIgnoreCase(b.label);
			}
		});
		for (Option option : classOptions) {
			if (!map.containsKey(option.value)) {
				map.put(option.value, option);
			}
		}
		return new ArrayList<Option>(map.values());
	}

	/** @return label tampilan untuk kode pilihan {@code value}: dicari di {@link #options()}, atau nama kelas sederhana bila berupa {@code class:...}, atau nilai apa adanya bila tidak dikenali. */
	static String label(String value) {
		if (value == null || value.trim().isEmpty()) {
			return "";
		}
		for (Option option : options()) {
			if (value.equals(option.value)) {
				return option.label;
			}
		}
		return value.startsWith(CLASS_PREFIX) ? simpleLabel(value.substring(CLASS_PREFIX.length())) : value;
	}

	static boolean isClassOption(String value) {
		return value != null && value.startsWith(CLASS_PREFIX);
	}

	static String className(String value) {
		return isClassOption(value) ? value.substring(CLASS_PREFIX.length()) : null;
	}

	/**
	 * Membuat instance {@link MyWindow} dari kode pilihan {@code value} bila berupa opsi kelas
	 * kustom ({@code class:<nama kelas>}), lewat refleksi (constructor tanpa-argumen). Mengembalikan
	 * {@code null} bila {@code value} bukan opsi kelas, atau bila kelasnya bukan turunan
	 * {@link MyWindow}.
	 *
	 * @param value kode pilihan (dari {@link #options()})
	 * @return jendela dasbor yang diinstansiasi, atau {@code null}
	 */
	static MyWindow createWindow(String value) throws Exception {
		String className = className(value);
		if (className == null) {
			return null;
		}
		Class<?> clazz = Class.forName(className);
		if (!MyWindow.class.isAssignableFrom(clazz)) {
			return null;
		}
		Constructor<?> constructor = clazz.getDeclaredConstructor();
		constructor.setAccessible(true);
		return (MyWindow) constructor.newInstance();
	}

	static void addMain(LinkedHashMap<String, Option> map, String value, String label) {
		map.put(value, new Option(value, "Main - " + label));
	}

	private static List<Option> scanDashboardWindowClasses() {
		List<Option> result = new ArrayList<Option>();
		File root = resolveSourceRoot();
		if (root == null || !root.exists()) {
			return result;
		}
		scanJava(root, root, result);
		return result;
	}

	private static List<Option> scanCompiledDashboardWindowClasses() {
		List<Option> result = new ArrayList<Option>();
		try {
			URL url = MainDashboardDefaultHelper.class.getClassLoader().getResource("ais/action");
			if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
				return result;
			}
			File root = new File(URLDecoder.decode(url.getPath(), "UTF-8"));
			if (!root.exists()) {
				return result;
			}
			scanClassFiles(root, root, "ais.action", result);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "scan compiled default dashboard classes");
		}
		return result;
	}

	private static File resolveSourceRoot() {
		File userDir = new File(System.getProperty("user.dir", "."));
		File candidate = new File(userDir, "src/main/src");
		if (candidate.exists()) {
			return candidate;
		}
		if (Common.REAL_PATH != null) {
			File webapp = new File(Common.REAL_PATH);
			File project = webapp.getParentFile() == null ? null : webapp.getParentFile().getParentFile();
			if (project != null) {
				candidate = new File(project, "src/main/src");
				if (candidate.exists()) {
					return candidate;
				}
			}
		}
		return null;
	}

	private static void scanJava(File root, File file, List<Option> result) {
		File[] files = file.listFiles();
		if (files == null) {
			return;
		}
		for (File child : files) {
			if (child.isDirectory()) {
				scanJava(root, child, result);
				continue;
			}
			String name = child.getName();
			if (!name.endsWith(".java") || !(name.contains("Dasbor") || name.contains("Dashboard")
					|| name.contains("Statistik"))) {
				continue;
			}
			try {
				String text = new String(Files.readAllBytes(child.toPath()), Charset.forName("UTF-8"));
				if (text == null || !text.contains("extends MyWindow")) {
					continue;
				}
				String className = toClassName(root, child);
				result.add(new Option(CLASS_PREFIX + className, "Class - " + simpleLabel(className)));
			} catch (Exception ignored) {
				ais.common.ErrorAuditUtil.record(ignored,
						"scan default dashboard class " + child.getAbsolutePath());
			}
		}
	}

	private static void scanClassFiles(File root, File file, String packageName, List<Option> result) {
		File[] files = file.listFiles();
		if (files == null) {
			return;
		}
		for (File child : files) {
			if (child.isDirectory()) {
				String nextPackage = packageName + "." + child.getName();
				scanClassFiles(root, child, nextPackage, result);
				continue;
			}
			String name = child.getName();
			if (!name.endsWith(".class") || name.indexOf('$') >= 0 || !(name.contains("Dasbor")
					|| name.contains("Dashboard") || name.contains("Statistik"))) {
				continue;
			}
			String simple = name.substring(0, name.length() - 6);
			String className = packageName + "." + simple;
			try {
				Class<?> clazz = Class.forName(className, false, MainDashboardDefaultHelper.class.getClassLoader());
				if (MyWindow.class.isAssignableFrom(clazz)) {
					result.add(new Option(CLASS_PREFIX + className, "Class - " + simpleLabel(simple)));
				}
			} catch (Throwable ignored) {
				ais.common.ErrorAuditUtil.record(new Exception(ignored),
						"scan compiled default dashboard class " + className);
			}
		}
	}

	private static String toClassName(File root, File file) {
		String rootPath = root.getAbsolutePath();
		String path = file.getAbsolutePath();
		if (path.startsWith(rootPath)) {
			path = path.substring(rootPath.length());
		}
		while (path.startsWith(File.separator)) {
			path = path.substring(1);
		}
		if (path.endsWith(".java")) {
			path = path.substring(0, path.length() - 5);
		}
		return path.replace(File.separatorChar, '.');
	}

	private static String simpleLabel(String className) {
		String simple = className == null ? "" : className;
		int idx = simple.lastIndexOf('.');
		if (idx >= 0) {
			simple = simple.substring(idx + 1);
		}
		return simple.replaceAll("([a-z])([A-Z])", "$1 $2").replace("Dasboard", "Dasbor")
				.replace("Dashboard", "Dashboard").trim();
	}

	static final class Option {
		final String value;
		final String label;

		Option(String value, String label) {
			this.value = value;
			this.label = label;
		}

		Comboitem toComboitem() {
			Comboitem item = new Comboitem(label);
			item.setValue(value);
			return item;
		}
	}
}
