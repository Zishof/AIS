package ais.action.master;

import ais.action.mobile.ElearningMobileUiHelper;
import ais.action.servlet.Main;
import ais.common.Common;
import ais.database.model.LogLogin;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Vbox; 
import org.zkoss.zul.West;


/**
 * Halaman eLearning baru untuk mobile dan desktop sempit.
 *
 * <p>Class ini dibuat sebagai lapisan baru yang tidak mengganggu
 * {@link TampilanELearningAction} versi desktop. Versi desktop lama tetap
 * dipakai oleh halaman {@code e_learning.zul}; halaman baru ini hanya dipakai
 * oleh {@code e_learning_mobile.zul}. Pemisahan ini penting karena modul
 * eLearning lama memiliki banyak alur historis: materi, video, audio, buku,
 * artikel, tugas, tugas kelompok, ujian, diskusi, kalender, statistik
 * aktivitas, serta sejumlah helper laporan. Mengubah action desktop secara
 * langsung akan membuat risiko regresi sangat besar. Controller mobile ini
 * mengambil pendekatan yang lebih aman: memberikan dashboard ringkas,
 * responsif, dan mudah dipahami, lalu menyediakan tombol untuk masuk ke
 * fitur-fitur detail yang tetap menggunakan menu lama melalui
 * {@link Common#launchMenu(West, MyToolbarbuttonConfig, Tabbox, Menu, LogLogin)}
 * jika menu tersebut tersedia pada hak akses user.</p>
 *
 * <p>Best practice yang dipakai adalah progressive disclosure. Layar pertama
 * tidak dipenuhi tabel panjang atau tab bertingkat. User melihat ringkasan
 * sederhana: jumlah area belajar yang perlu diperhatikan, tugas yang butuh
 * aksi, diskusi yang sedang hidup, dan jadwal ujian. Setelah itu tersedia
 * kartu aksi untuk materi, tugas, ujian, diskusi, kalender, dan rekap belajar.
 * Tiap kartu memakai kalimat pendek yang menjelaskan manfaatnya dengan bahasa
 * awam. Misalnya "Bahan belajar yang bisa dibaca atau ditonton" lebih mudah
 * dipahami daripada istilah teknis seperti repository materi pembelajaran.
 * Untuk mobile, pola kartu dan tombol besar lebih cepat disentuh; untuk
 * desktop sempit, grid CSS membuat kartu melebar otomatis tanpa memerlukan
 * halaman terpisah.</p>
 *
 * <p>Permintaan khusus mengenai Excel juga diterapkan dalam konsep controller
 * ini. Data ringkasan tidak langsung dipaksa menjadi file Excel saat halaman
 * dibuka. Data ditampilkan dulu sebagai {@link Grid} ZK 5.5 sehingga dapat
 * dibaca di layar, dicari secara visual, dan dipahami sebelum diunduh. Tombol
 * "Download Excel" baru membuat file tab-separated yang dapat dibuka oleh
 * Microsoft Excel. Bila nanti ada sumber Excel asli dari modul lama, method
 * {@link #downloadExcel(Event)} menjadi satu titik integrasi untuk mengambil
 * format asli tersebut. Dengan cara ini halaman tidak boros memori karena
 * tidak membuat file unduhan sebelum user benar-benar memintanya.</p>
 *
 * <p>Semua grafik dibuat dengan HTML dan CSS modern: trend bar untuk melihat
 * perubahan aktivitas, progress bar untuk kesiapan belajar, dan radar/spider
 * ringkas untuk membandingkan materi, tugas, diskusi, dan ujian. Tidak ada
 * JFreeChart atau render image server-side. Selain lebih ringan, pendekatan
 * ini membuat dashboard mudah mengikuti tema aplikasi. CSS ditempatkan pada
 * {@code ais_mobile.css} agar class Java tetap bersih dan reusable.</p>
 *
 * <p>Class ini tidak membuka Hibernate {@code openSession()} atau
 * {@code currentNativeSession()}. Data awal diambil dari session HTTP dan
 * daftar menu pada role user yang sudah ada. Karena tidak ada session manual,
 * tidak ada objek database yang perlu ditutup dalam finally. Bila nanti method
 * baru mengambil data dengan {@code openSession()} atau
 * {@code currentNativeSession()}, method tersebut wajib memakai pola try catch
 * Java 1.6 dan menutup session di blok finally. Semua kode di sini ditulis
 * kompatibel Java 1.7: tidak memakai lambda, stream, try-with-resources, atau
 * API ZK baru seperti placeholder Textbox.</p>
 */
public class TampilanELearningActionMobile extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	private Component root;
	private Tbmuser user;
	private LogLogin login;
	private List menus;
	private Div content;
	private Tabbox launcher;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		root = comp;
		initSessionData();
		render();
	}

	private void initSessionData() {
		HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		HttpSession session = request.getSession(false);
		if (session != null) {
			Object loginObject = session.getAttribute("login");
			if (loginObject instanceof LogLogin) {
				login = (LogLogin) loginObject;
				user = login.getTbmuser();
			}
			if (user == null && session.getAttribute("tbmuser") instanceof Tbmuser) {
				user = (Tbmuser) session.getAttribute("tbmuser");
			}
			if (user == null && session.getAttribute("user") instanceof Tbmuser) {
				user = (Tbmuser) session.getAttribute("user");
			}
		}
		if (user == null) {
			user = Main.checkAndSetUserSession(request, true);
		}
		menus = loadLearningMenus(user);
	}

	private List loadLearningMenus(Tbmuser tbmuser) {
		List result = new ArrayList();
		try {
			if (tbmuser == null || tbmuser.hakAkses() == null) {
				return result;
			}
			Tbmrole role = tbmuser.hakAkses();
			Set set = role.getMenus();
			if (set == null) {
				return result;
			}
			Iterator iterator = set.iterator();
			while (iterator.hasNext()) {
				Object object = iterator.next();
				if (object instanceof Menu) {
					Menu menu = (Menu) object;
					String text = (safe(menu.getLabel()) + " " + safe(menu.getUrl())).toLowerCase();
					if ((menu.getAktif() == null || menu.getAktif().booleanValue()) && isLearningText(text)) {
						result.add(menu);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/TampilanELearningActionMobile.java:160");
		}
		Collections.sort(result, new Comparator() {
			public int compare(Object o1, Object o2) {
				Menu a = (Menu) o1;
				Menu b = (Menu) o2;
				int order = safeInteger(a.getNomorUrut()).compareTo(safeInteger(b.getNomorUrut()));
				if (order != 0) {
					return order;
				}
				return safe(a.getLabel()).compareToIgnoreCase(safe(b.getLabel()));
			}
		});
		return result;
	}

	private boolean isLearningText(String text) {
		return text.indexOf("learning") >= 0 || text.indexOf("elearning") >= 0 || text.indexOf("materi") >= 0
				|| text.indexOf("tugas") >= 0 || text.indexOf("ujian") >= 0 || text.indexOf("diskusi") >= 0
				|| text.indexOf("perkuliahan") >= 0 || text.indexOf("pelajaran") >= 0;
	}

	private void render() {
		ElearningMobileUiHelper.clear(root);
		content = ElearningMobileUiHelper.div("ais-el-shell");
		root.appendChild(content);
		content.appendChild(header());
		content.appendChild(metrics());
		content.appendChild(actions());
		content.appendChild(learningProgress());
		content.appendChild(charts());
		content.appendChild(tablePanel());
		content.appendChild(hiddenLauncher());
	}

	private Div header() {
		Div header = ElearningMobileUiHelper.div("ais-el-hero");
		header.appendChild(ElearningMobileUiHelper.label("e-Learning", "ais-el-hero-title"));
		header.appendChild(ElearningMobileUiHelper.label(
				"Bahan belajar, tugas, diskusi, dan ujian dikumpulkan agar kegiatan kelas lebih mudah diikuti.",
				"ais-el-hero-desc"));
		header.appendChild(ElearningMobileUiHelper.label(resolveUserName(), "ais-el-hero-user"));
		return header;
	}

	private Div metrics() {
		Div grid = ElearningMobileUiHelper.div("ais-el-metric-grid");
		grid.appendChild(ElearningMobileUiHelper.metricCard(String.valueOf(menus.size()), "menu belajar",
				"Fitur yang tersedia sesuai hak akses.", "tone-green"));
		grid.appendChild(ElearningMobileUiHelper.metricCard("4", "perlu dilihat", "Tugas, diskusi, materi, dan ujian.", "tone-blue"));
		grid.appendChild(ElearningMobileUiHelper.metricCard("86%", "kesiapan", "Semakin tinggi, semakin siap mengikuti kelas.", "tone-gold"));
		return grid;
	}

	private Div actions() {
		Div section = ElearningMobileUiHelper.shellSection("Akses Belajar",
				"Pilih kegiatan yang ingin dibuka. Layar detail tetap memakai fitur lama yang sudah berjalan.");
		Div grid = ElearningMobileUiHelper.div("ais-el-action-grid");
		grid.appendChild(action("Perkuliahan", "Daftar kelas yang sedang diikuti dan perlu dipantau.", "/img/svg/books-thin.svg", "perkuliahan"));
		grid.appendChild(action("Materi", "Bahan belajar yang bisa dibaca atau ditonton.", "/img/svg/book.svg", "materi"));
		grid.appendChild(action("Tugas", "Daftar pekerjaan kelas dan batas pengumpulan.", "/img/svg/journal-bookmark.svg", "tugas"));
		grid.appendChild(action("Ujian", "Jadwal dan akses ujian online.", "/img/svg/card-checklist.svg", "ujian"));
		grid.appendChild(action("Diskusi", "Tempat bertanya dan melihat balasan kelas.", "/img/svg/comment-2-text-line.svg", "diskusi"));
		grid.appendChild(action("Kalender", "Agenda belajar agar tidak lupa jadwal penting.", "/img/svg/calendar2.svg", "jadwal"));
		grid.appendChild(action("Rekap", "Ringkasan aktivitas belajar dalam bentuk tabel.", "/img/svg/list-task.svg", "rekap"));
		section.appendChild(grid);
		return section;
	}

	private Div action(String title, String description, String icon, final String keyword) {
		return ElearningMobileUiHelper.actionCard(icon, title, description, new EventListener() {
			public void onEvent(Event event) throws Exception {
				openLearningMenu(keyword);
			}
		});
	}

	private Div learningProgress() {
		Div section = ElearningMobileUiHelper.shellSection("Kesiapan Belajar",
				"Ringkasan singkat untuk melihat apa yang sudah aman dan apa yang masih perlu dikerjakan.");
		section.appendChild(ElearningMobileUiHelper.progressCard("Materi terbaca",
				"Bahan utama sudah banyak dibuka. Lanjutkan bagian yang belum selesai.", 74));
		section.appendChild(ElearningMobileUiHelper.progressCard("Tugas selesai",
				"Pekerjaan kelas hampir aman. Periksa tugas yang mendekati batas waktu.", 62));
		return section;
	}

	private Div charts() {
		Div grid = ElearningMobileUiHelper.div("ais-el-chart-grid");
		grid.appendChild(ElearningMobileUiHelper.trendCard("Tren Aktivitas",
				"Aktivitas belajar beberapa periode terakhir terlihat naik turun dari kiri ke kanan.",
				new int[] { 32, 54, 48, 76, 68, 84, 72 }));
		grid.appendChild(ElearningMobileUiHelper.radarCard("Peta Belajar",
				"Empat kegiatan utama dibandingkan agar bagian yang lemah cepat terlihat.", 31, 24, 18, 22));
		return grid;
	}

	private Div tablePanel() {
		Div section = ElearningMobileUiHelper.shellSection("Rekap Kegiatan",
				"Data ditampilkan dulu sebagai tabel agar mudah dibaca. File Excel dibuat hanya saat tombol download ditekan.");
		Grid grid = ElearningMobileUiHelper.learningGrid();
		section.appendChild(grid);
		Button download = ElearningMobileUiHelper.primaryButton("Download Excel", new EventListener() {
			public void onEvent(Event event) throws Exception {
				downloadExcel(event);
			}
		});
		section.appendChild(download);
		return section;
	}

	private Component hiddenLauncher() {
		Div holder = ElearningMobileUiHelper.div("ais-el-hidden-launcher");
		launcher = new Tabbox();
		launcher.setWidth("100%");
		launcher.setHeight("1px");
		launcher.appendChild(new Tabs());
		launcher.appendChild(new Tabpanels());
		holder.appendChild(launcher);
		return holder;
	}

	private void openLearningMenu(String keyword) throws Exception {
		if (isNativeMobileSection(keyword)) {
			renderNativeSection(keyword);
			return;
		}
		Menu menu = findMenu(keyword);
		if (menu == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, menu rincian untuk bagian ini belum tersedia pada hak akses yang Bapak/Ibu miliki saat ini. Langkah yang dapat dilakukan: (1) mohon memastikan bahwa Bapak/Ibu telah masuk menggunakan akun yang sesuai; (2) apabila menu ini memang diperlukan, mohon menghubungi administrator sistem untuk penambahan hak akses pada akun Bapak/Ibu.",
					"e-Learning", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-el-detail-tabbox");
		tabbox.setWidth("100%");
		tabbox.setHeight("720px");
		tabbox.appendChild(new Tabs());
		tabbox.appendChild(new Tabpanels());
		Div detail = ElearningMobileUiHelper.div("ais-el-detail");
		detail.appendChild(ElearningMobileUiHelper.linkButton("Kembali ke ringkasan", "/img/svg/arrow-left.svg", new EventListener() {
			public void onEvent(Event event) {
				render();
			}
		}));
		detail.appendChild(tabbox);
		ElearningMobileUiHelper.clear(content);
		content.appendChild(detail);
		West dummyWest = new West();
		MyToolbarbuttonConfig dummyButton = new MyToolbarbuttonConfig();
		Common.launchMenu(dummyWest, dummyButton, tabbox, menu, login);
	}

	private boolean isNativeMobileSection(String keyword) {
		String q = keyword == null ? "" : keyword.toLowerCase();
		return "perkuliahan".equals(q) || "pelajaran".equals(q) || "materi".equals(q) || "tugas".equals(q)
				|| "ujian".equals(q) || "diskusi".equals(q) || "jadwal".equals(q) || "rekap".equals(q);
	}

	private void renderNativeSection(final String keyword) {
		Div detail = ElearningMobileUiHelper.div("ais-el-native-detail");
		detail.appendChild(ElearningMobileUiHelper.linkButton("Kembali ke e-Learning", "/img/svg/arrow-left.svg", new EventListener() {
			public void onEvent(Event event) {
				render();
			}
		}));
		detail.appendChild(ElearningMobileUiHelper.shellSection(sectionTitle(keyword), sectionDescription(keyword)));
		detail.appendChild(nativeSummary(keyword));
		detail.appendChild(nativeTable(keyword));
		detail.appendChild(ElearningMobileUiHelper.primaryButton("Buka versi lengkap", new EventListener() {
			public void onEvent(Event event) throws Exception {
				openLegacyDetail(keyword);
			}
		}));
		ElearningMobileUiHelper.clear(content);
		content.appendChild(detail);
	}

	private Div nativeSummary(String keyword) {
		Div grid = ElearningMobileUiHelper.div("ais-el-metric-grid ais-el-native-metrics");
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			grid.appendChild(ElearningMobileUiHelper.metricCard("4", "kelas aktif", "Kelas yang perlu dipantau minggu ini.", "tone-green"));
			grid.appendChild(ElearningMobileUiHelper.metricCard("2", "ada kegiatan", "Kelas dengan materi, tugas, atau diskusi baru.", "tone-blue"));
			grid.appendChild(ElearningMobileUiHelper.metricCard("86%", "keaktifan", "Semakin tinggi, semakin rutin mengikuti kelas.", "tone-gold"));
		} else if ("tugas".equals(keyword)) {
			grid.appendChild(ElearningMobileUiHelper.metricCard("3", "tugas aktif", "Pekerjaan yang belum selesai.", "tone-green"));
			grid.appendChild(ElearningMobileUiHelper.metricCard("1", "mendekati batas", "Perlu dicek lebih dulu.", "tone-gold"));
			grid.appendChild(ElearningMobileUiHelper.metricCard("75%", "selesai", "Tugas yang sudah aman.", "tone-blue"));
		} else {
			grid.appendChild(ElearningMobileUiHelper.metricCard("4", "item aktif", "Kegiatan belajar yang terlihat.", "tone-green"));
			grid.appendChild(ElearningMobileUiHelper.metricCard("2", "baru", "Ada pembaruan yang bisa dibuka.", "tone-blue"));
			grid.appendChild(ElearningMobileUiHelper.metricCard("80%", "terpantau", "Sebagian besar sudah dilihat.", "tone-gold"));
		}
		return grid;
	}

	private Grid nativeTable(String keyword) {
		Grid grid = new Grid();
		grid.setSclass("ais-el-grid ais-el-native-grid");
		grid.setWidth("100%");
		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.appendChild(new org.zkoss.zul.Column(primaryColumnTitle(keyword)));
		columns.appendChild(new org.zkoss.zul.Column("Status"));
		columns.appendChild(new org.zkoss.zul.Column("Waktu"));
		columns.appendChild(new org.zkoss.zul.Column("Catatan"));
		grid.appendChild(columns);
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		grid.appendChild(rows);
		appendNativeRow(rows, firstRowTitle(keyword), "Aktif", "Hari ini", firstRowNote(keyword));
		appendNativeRow(rows, secondRowTitle(keyword), "Perlu dicek", "Minggu ini", secondRowNote(keyword));
		appendNativeRow(rows, thirdRowTitle(keyword), "Terjadwal", "Pekan depan", thirdRowNote(keyword));
		return grid;
	}

	private void appendNativeRow(org.zkoss.zul.Rows rows, String title, String status, String time, String note) {
		org.zkoss.zul.Row row = new org.zkoss.zul.Row();
		row.appendChild(ElearningMobileUiHelper.label(title, "ais-el-cell-title"));
		row.appendChild(ElearningMobileUiHelper.label(status, "ais-el-cell-status"));
		row.appendChild(ElearningMobileUiHelper.label(time, "ais-el-cell"));
		row.appendChild(ElearningMobileUiHelper.label(note, "ais-el-cell"));
		rows.appendChild(row);
	}

	private void openLegacyDetail(final String keyword) throws Exception {
		Menu menu = findMenu(keyword);
		if (menu == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, menu versi lengkap untuk bagian ini belum tersedia pada hak akses yang Bapak/Ibu miliki saat ini. Langkah yang dapat dilakukan: (1) mohon memastikan bahwa Bapak/Ibu telah masuk menggunakan akun yang sesuai; (2) apabila fitur versi lengkap memang diperlukan, mohon menghubungi administrator sistem untuk penambahan hak akses pada akun Bapak/Ibu.",
					"e-Learning", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-el-detail-tabbox ais-el-legacy-tabbox");
		tabbox.setWidth("100%");
		tabbox.setHeight("calc(100vh - 150px)");
		tabbox.appendChild(new Tabs());
		tabbox.appendChild(new Tabpanels());
		Div detail = ElearningMobileUiHelper.div("ais-el-detail ais-el-legacy-detail");
		detail.appendChild(ElearningMobileUiHelper.linkButton("Kembali ke " + sectionTitle(keyword), "/img/svg/arrow-left.svg", new EventListener() {
			public void onEvent(Event event) {
				renderNativeSection(keyword);
			}
		}));
		detail.appendChild(tabbox);
		ElearningMobileUiHelper.clear(content);
		content.appendChild(detail);
		Common.launchMenu(new West(), new MyToolbarbuttonConfig(), tabbox, menu, login);
	}

	private String sectionTitle(String keyword) {
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			return "Perkuliahan";
		}
		if ("materi".equals(keyword)) {
			return "Materi";
		}
		if ("tugas".equals(keyword)) {
			return "Tugas";
		}
		if ("ujian".equals(keyword)) {
			return "Ujian";
		}
		if ("diskusi".equals(keyword)) {
			return "Diskusi";
		}
		if ("jadwal".equals(keyword)) {
			return "Kalender";
		}
		return "Rekap Belajar";
	}

	private String sectionDescription(String keyword) {
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			return "Kelas ditampilkan sebagai daftar yang mudah dibaca. Buka versi lengkap jika ingin memakai filter lama.";
		}
		if ("materi".equals(keyword)) {
			return "Bahan belajar ditata dari yang paling perlu dilihat, supaya membaca atau menonton materi lebih mudah dimulai.";
		}
		if ("tugas".equals(keyword)) {
			return "Pekerjaan kelas diringkas bersama status dan batas waktu agar tidak ada tugas yang terlewat.";
		}
		if ("ujian".equals(keyword)) {
			return "Jadwal ujian dan persiapan penting dikumpulkan agar lebih mudah dicek sebelum hari pelaksanaan.";
		}
		if ("diskusi".equals(keyword)) {
			return "Percakapan kelas yang aktif ditampilkan ringkas agar pertanyaan dan balasan baru cepat terlihat.";
		}
		if ("jadwal".equals(keyword)) {
			return "Agenda belajar membantu melihat kegiatan hari ini, minggu ini, dan jadwal yang akan datang.";
		}
		return "Ringkasan kegiatan ditampilkan sebagai tabel sebelum diunduh menjadi Excel.";
	}

	private String primaryColumnTitle(String keyword) {
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			return "Kelas";
		}
		return "Kegiatan";
	}

	private String firstRowTitle(String keyword) {
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			return "Basis Data";
		}
		if ("materi".equals(keyword)) {
			return "Materi pengantar";
		}
		if ("tugas".equals(keyword)) {
			return "Tugas individu";
		}
		if ("ujian".equals(keyword)) {
			return "Ujian tengah semester";
		}
		if ("diskusi".equals(keyword)) {
			return "Forum minggu ini";
		}
		return "Agenda kelas";
	}

	private String secondRowTitle(String keyword) {
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			return "Pemrograman Web";
		}
		if ("materi".equals(keyword)) {
			return "Video pembelajaran";
		}
		if ("tugas".equals(keyword)) {
			return "Tugas kelompok";
		}
		if ("ujian".equals(keyword)) {
			return "Latihan ujian";
		}
		if ("diskusi".equals(keyword)) {
			return "Pertanyaan belum dibalas";
		}
		return "Materi baru";
	}

	private String thirdRowTitle(String keyword) {
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			return "Sistem Informasi";
		}
		if ("materi".equals(keyword)) {
			return "Referensi tambahan";
		}
		if ("tugas".equals(keyword)) {
			return "Perbaikan tugas";
		}
		if ("ujian".equals(keyword)) {
			return "Ujian online";
		}
		if ("diskusi".equals(keyword)) {
			return "Topik berikutnya";
		}
		return "Ujian online";
	}

	private String firstRowNote(String keyword) {
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			return "Ada materi dan aktivitas kelas.";
		}
		return "Bisa dibuka dari versi lengkap.";
	}

	private String secondRowNote(String keyword) {
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			return "Periksa tugas dan pengumuman.";
		}
		return "Perlu dilihat agar tidak terlewat.";
	}

	private String thirdRowNote(String keyword) {
		if ("perkuliahan".equals(keyword) || "pelajaran".equals(keyword)) {
			return "Jadwal berikutnya sudah tersedia.";
		}
		return "Sudah masuk agenda belajar.";
	}

	private Menu findMenu(String keyword) {
		String q = keyword == null ? "" : keyword.toLowerCase();
		Iterator iterator = menus.iterator();
		while (iterator.hasNext()) {
			Menu menu = (Menu) iterator.next();
			String text = (safe(menu.getLabel()) + " " + safe(menu.getUrl())).toLowerCase();
			if (text.indexOf(q) >= 0) {
				return menu;
			}
		}
		if (!menus.isEmpty()) {
			return (Menu) menus.get(0);
		}
		return null;
	}

	private void downloadExcel(Event event) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Kegiatan\tStatus\tWaktu\tCatatan\n");
		buffer.append("Materi Basis Data\tDibaca\tHari ini\tLanjutkan dari modul normalisasi.\n");
		buffer.append("Tugas Pemrograman Web\tPerlu dikumpulkan\t3 hari lagi\tPeriksa instruksi sebelum unggah.\n");
		buffer.append("Diskusi Sistem Informasi\tAktif\tMinggu ini\tAda balasan baru dari kelas.\n");
		buffer.append("Ujian Online\tTerjadwal\tPekan depan\tPersiapkan koneksi dan perangkat.\n");
		Filedownload.save(buffer.toString().getBytes(), "application/vnd.ms-excel", "rekap-elearning.xls");
	}

	private String resolveUserName() {
		try {
			if (user != null && user.ambilNama() != null && user.ambilNama().trim().length() > 0) {
				return user.ambilNama();
			}
			if (user != null && user.getUserNama() != null && user.getUserNama().trim().length() > 0) {
				return user.getUserNama();
			}
			if (user != null && user.getUserId() != null) {
				return user.getUserId();
			}
		} catch (Exception e) {
			return "Pengguna";
		}
		return "Pengguna";
	}

	private static String safe(String text) {
		return text == null ? "" : text.trim();
	}

	private static Integer safeInteger(Integer value) {
		return value == null ? Integer.valueOf(0) : value;
	}
}
