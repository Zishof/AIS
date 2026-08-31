package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;
import ais.ui.util.DashboardGridExportHelper;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.ChecklistBaruPenilaianGuruOlehSiswa;
import ais.database.model.sekolah.ChecklistPenilaianGuru;
import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Dasbor (bukan laporan cetak PDF seperti kelas {@code Laporan*} lain di paket ini) yang
 * merangkum hasil angket penilaian guru oleh siswa ({@link ChecklistBaruPenilaianGuruOlehSiswa}),
 * dirender sebagai kartu statistik dan visualisasi HTML/CSS ringan (distribusi nilai, tren, radar
 * per kelompok pertanyaan) langsung di halaman, tanpa mesin cetak laporan. Filter tersedia untuk
 * tahun akademik, semester, guru tertentu, yayasan/sekolah, dan opsi "hanya angket aktif" (hanya
 * menghitung {@link ChecklistPenilaianGuru}/{@link GrupChecklistPenilaianGuru} yang berstatus
 * aktif). Setiap perubahan filter memuat ulang seluruh dasbor dari basis data.
 *
 * <h2>Alur pengambilan data ({@link #loadDashboardData})</h2>
 * Untuk setiap baris {@link ChecklistBaruPenilaianGuruOlehSiswa} (dibatasi jumlah maksimum
 * pemindaian lewat {@link #readMaxScan()}), baris dilewati bila jadwal/tahun akademik/
 * semesternya tidak cocok filter ({@link #matchJadwal}) atau sekolah/yayasannya tidak cocok
 * ({@link #matchSekolahYayasan}). Nilai jawaban per pertanyaan diambil lewat
 * {@link #ambilValueGuruAman}, yang mengutamakan parsing kolom {@code keterangan} berformat
 * legacy (baris dipisah {@code "___"}, tiap baris diawali penanda {@code "DATA"} lalu diproses
 * lewat {@link #parseAngketValueGuru}) dan baru jatuh kembali ke method model
 * {@code hasilAngket.ambilValue()} bila kolom keterangan kosong — memastikan kompatibilitas
 * mundur dengan dua skema penyimpanan jawaban angket yang pernah dipakai. Hasil akhirnya
 * diakumulasi ke objek {@link DashboardData} (statistik per kelompok pertanyaan, per guru,
 * distribusi nilai, dan daftar masukan/komentar terbaru).
 */
public class LaporanAngketGuruDashboardWindow extends MyWindow {

	private static final long serialVersionUID = 6409442098743220322L;
	private static final ThreadLocal<NumberFormat> DECIMAL = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			return new DecimalFormat("#,##0.00");
		}
	};
	private static final ThreadLocal<NumberFormat> INTEGER = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			return new DecimalFormat("#,##0");
		}
	};

	private Combobox tahunAkademik;
	private Combobox semesterAbsensi;
	private AmbilDataGuruBanbox guru;
	private MyCheckboxConfig aktif;
	private Center center;
	private Div dashboardContent;
	private Combobox yayasan;
	private Combobox sekolah;

	/** Membuat jendela dasbor dan langsung menyusun tampilan filter serta memuat dasbor awal. */
	public LaporanAngketGuruDashboardWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Guru Dashboard Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/**
	 * Membuat jendela dasbor dengan judul, gaya border, dan status dapat-ditutup kustom.
	 *
	 * @param title    judul jendela
	 * @param border   gaya border jendela
	 * @param closable apakah jendela dapat ditutup pengguna
	 */
	public LaporanAngketGuruDashboardWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	/** Menyusun toolbar filter (tahun akademik, semester, hanya-aktif, refresh, yayasan/sekolah/guru, tombol hitung ulang) dan area konten dasbor, lalu memuat dasbor awal lewat {@link #reloadDashboard()}. */
	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Laporan Angket Guru Dashboard Window");
		setHeight("100%");
		setWidth("100%");

		final EventListener reloadListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reloadDashboard();
			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setAutoscroll(true);

		Div mainWrapper = new Div();
		mainWrapper.setStyle("width:100%;height:100%;overflow:auto;box-sizing:border-box;background:#f6f8fb;");
		mainWrapper.setParent(center);

		Vbox northBox = new Vbox();
		northBox.setWidth("100%");
		northBox.setStyle("box-sizing:border-box;padding:10px 12px;background:#f8fafc;border-bottom:1px solid #e5e7eb;");
		northBox.setParent(mainWrapper);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(northBox);
		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setReadonly(true);
		tahunAkademik.setWidth("145px");
		tahunAkademik.addEventListener("onChange", reloadListener);
		tahunAkademik.setParent(toolbar);

		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		semesterAbsensi = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		semesterAbsensi.appendChild(comboitem);
		Common.selectComboItem(semesterAbsensi,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);
		semesterAbsensi.setWidth("100px");
		semesterAbsensi.addEventListener("onChange", reloadListener);
		semesterAbsensi.setParent(toolbar);

		aktif = new MyCheckboxConfig("Hanya Angket Aktif");
		aktif.setChecked(true);
		aktif.addEventListener("onCheck", reloadListener);
		aktif.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
		refresh.addEventListener("onClick", reloadListener);
		refresh.setParent(toolbar);

		Div filterWrapper = new Div();
		filterWrapper.setStyle("width:100%;box-sizing:border-box;padding:6px 10px;background:#f8fafc;border-top:1px solid #e5e7eb;");
		filterWrapper.setParent(northBox);

		MyGrid filterGrid = new MyGrid();
		filterGrid.setWidth("100%");
		filterGrid.setParent(filterWrapper);

		Columns filterColumns = new Columns();
		filterColumns.setParent(filterGrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(filterColumns);
		column = new MyColumnConfig();
		column.setParent(filterColumns);

		Rows rows = new Rows();
		rows.setParent(filterGrid);

		Common.initYayasanDanSekolahDanSemua(yayasan = new Combobox(), sekolah = new Combobox(), null, null);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("95%");
		yayasan.setReadonly(true);
		yayasan.addEventListener("onChange", reloadListener);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		sekolah.setWidth("95%");
		sekolah.setReadonly(true);
		sekolah.addEventListener("onChange", reloadListener);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_guru")));
		guru = new AmbilDataGuruBanbox();
		guru.setWidth("95%");
		guru.setReadonly(true);
		guru.addEventListener("onChange", reloadListener);
		row.appendChild(guru);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Toolbar toolbarAksi = new Toolbar();
		row.appendChild(toolbarAksi);
		MyToolbarbuttonConfig hitungUlang = new MyToolbarbuttonConfig("Hitung Ulang Penilaian", "/img/options.png");
		hitungUlang.setTooltiptext("Menghitung ulang tampilan dasbor berdasarkan data angket guru terbaru.");
		hitungUlang.addEventListener("onClick", reloadListener);
		hitungUlang.setParent(toolbarAksi);

		Common.initKeterangan(rows, "Dasbor ini membaca hasil angket guru dari data siswa yang telah mengisi penilaian guru. Tombol Hitung Ulang Penilaian akan memuat ulang seluruh perhitungan dasbor dari data jawaban terbaru.");

		dashboardContent = new Div();
		dashboardContent.setStyle("width:100%;box-sizing:border-box;");
		dashboardContent.setParent(mainWrapper);

		reloadDashboard();
	}

	/** Menampilkan indikator memuat, mengambil data terbaru lewat {@link #loadDashboardData} dalam sesi Hibernate mandiri, lalu merender dasbor ({@link #renderDashboard}); menampilkan pesan galat pada area konten bila gagal. */
	private void reloadDashboard() {
		Session session = null;
		try {
			if (center == null) {
				return;
			}
			Common.clear(dashboardContent);
			Div loading = new Div();
			loading.setStyle("padding:16px;color:#666;text-align:center;");
			loading.appendChild(new Html("<i class='fa fa-spinner fa-spin'></i> Mengambil data dasbor angket guru..."));
			loading.setParent(dashboardContent);

			session = ais.action.report.Report.openNativeSession();
			DashboardData data = loadDashboardData(session);
			Common.clear(dashboardContent);
			renderDashboard(data);
		} catch (Exception e) {
			Common.clear(dashboardContent);
			Div error = new Div();
			error.setStyle("padding:15px;color:#b00020;background:#fff3f3;border:1px solid #f3c2c2;");
			error.appendChild(new Label("Gagal mengambil data dasbor angket guru: " + e.getMessage()));
			error.setParent(dashboardContent);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
	}

	/**
	 * Mengambil dan mengagregasi seluruh data dasbor sesuai filter aktif. Lihat javadoc kelas
	 * untuk alur lengkap (penyaringan jadwal/sekolah, parsing jawaban dua-skema, agregasi ke
	 * {@link DashboardData}).
	 *
	 * @param session sesi Hibernate untuk query
	 * @return data teragregasi siap dirender
	 */
	@SuppressWarnings("unchecked")
	private DashboardData loadDashboardData(Session session) throws Exception {
		DashboardData data = new DashboardData();
		data.tahunAkademik = selectedString(tahunAkademik, "Semua");
		data.semester = selectedString(semesterAbsensi, "Semua");
		data.guru = selectedGuru();
		data.yayasan = selectedValue(yayasan);
		data.sekolah = selectedValue(sekolah);

		boolean onlyAktif = aktif == null || aktif.isChecked();
		Criteria questionCriteria = session.createCriteria(ChecklistPenilaianGuru.class);
		if (onlyAktif) {
			questionCriteria.add(Restrictions.or(Restrictions.eq("aktif", Boolean.TRUE), Restrictions.isNull("aktif")));
		}
		List<ChecklistPenilaianGuru> questions = questionCriteria.list();
		Map<Long, ChecklistPenilaianGuru> questionMap = new HashMap<Long, ChecklistPenilaianGuru>();
		for (ChecklistPenilaianGuru question : questions) {
			if (question != null && question.getId() != null) {
				questionMap.put(question.getId(), question);
			}
		}
		data.totalPertanyaan = questionMap.size();

		Criteria groupCriteria = session.createCriteria(GrupChecklistPenilaianGuru.class);
		if (onlyAktif) {
			groupCriteria.add(Restrictions.or(Restrictions.eq("aktif", Boolean.TRUE), Restrictions.isNull("aktif")));
		}
		List<GrupChecklistPenilaianGuru> groups = groupCriteria.list();
		data.totalKelompok = groups == null ? 0 : groups.size();

		Criteria criteria = session.createCriteria(ChecklistBaruPenilaianGuruOlehSiswa.class);
		criteria.addOrder(Order.desc("tanggal_dirubah"));
		Guru selectedGuru = data.guru;
		if (selectedGuru != null && selectedGuru.getId() != null) {
			criteria.createAlias("guru", "guruFilter");
			criteria.add(Restrictions.eq("guruFilter.id", selectedGuru.getId()));
		}
		criteria.setMaxResults(readMaxScan());
		List<ChecklistBaruPenilaianGuruOlehSiswa> hasil = criteria.list();

		for (ChecklistBaruPenilaianGuruOlehSiswa hasilAngket : hasil) {
			if (hasilAngket == null || !matchJadwal(hasilAngket, data.tahunAkademik, data.semester)) {
				continue;
			}
			Object jp = safeCall(hasilAngket, "getJadwalPelajaran");
			if (!matchSekolahYayasan(jp, data.yayasan, data.sekolah)) {
				continue;
			}
			data.totalAngket++;
			Guru g = safeGuru(hasilAngket);
			Siswa s = safeSiswa(hasilAngket);
			if (g != null && g.getId() != null) {
				data.guruIds.add(g.getId());
				data.addGuru(g);
			}
			if (s != null && s.getId() != null) {
				data.siswaIds.add(s.getId());
			}
			Long jadwalId = safeId(jp);
			if (jadwalId != null) {
				data.jadwalIds.add(jadwalId);
			}
			data.formRows.add(new String[] { safeToString(g), safeToString(s), jp == null ? "" : safeToString(jp) });

			String masukan = hasilAngket.getMasukan();
			if (masukan != null && masukan.trim().length() > 0 && data.masukanTerbaru.size() < 10) {
				data.masukanTerbaru.add(new String[] { safeToString(g), safeToString(s), masukan.trim() });
			}

			List<Object[]> values = ambilValueGuruAman(hasilAngket);
			for (Object[] value : values) {
				if (value == null || value.length < 2 || value[0] == null || value[1] == null) {
					continue;
				}
				Long questionId = toLong(value[0]);
				ChecklistPenilaianGuru question = questionMap.get(questionId);
				if (question == null) {
					continue;
				}
				Integer nilai = toInteger(value[1]);
				if (nilai == null) {
					continue;
				}
				data.addNilai(nilai);
				if (g != null && g.getId() != null) {
					data.addGuruNilai(g, nilai);
				}
				GrupChecklistPenilaianGuru group = question.getGrupChecklistPenilaianGuru();
				String groupName = null;
				if (group != null) {
					groupName = group.getIsi();
					data.addGroup(groupName, nilai);
				}
				String ket = value.length > 2 && value[2] != null ? String.valueOf(value[2]) : "";
				String[] detailRow = new String[] { safeToString(g), question.getIsi(), String.valueOf(nilai), ket };
				data.detailRows.add(detailRow);
				data.addDetailToGroup(groupName, detailRow);
				data.addDetailToGuru(g, detailRow);
				List nilaiDetail = data.nilaiDetails.get(Integer.valueOf(nilai));
				if (nilaiDetail == null) {
					nilaiDetail = new ArrayList<String[]>();
					data.nilaiDetails.put(Integer.valueOf(nilai), nilaiDetail);
				}
				nilaiDetail.add(detailRow);
				if (ket.trim().length() > 0 && data.masukanTerbaru.size() < 10) {
					data.masukanTerbaru.add(new String[] { safeToString(g), question.getIsi(), ket.trim() });
				}
			}
		}
		return data;
	}


	/** Mengambil jawaban satu baris angket: mengutamakan parsing kolom {@code keterangan} legacy (lihat javadoc kelas), jatuh kembali ke {@code hasilAngket.ambilValue()} bila kolom itu kosong atau gagal diparsing. */
	private List<Object[]> ambilValueGuruAman(ChecklistBaruPenilaianGuruOlehSiswa hasilAngket) {
		List<Object[]> values = new ArrayList<Object[]>();
		if (hasilAngket == null) {
			return values;
		}
		String ket = "";
		try {
			Object obj = safeCall(hasilAngket, "getKeterangan");
			ket = obj == null ? "" : String.valueOf(obj);
		} catch (Exception e) {
			ket = "";
		}
		if (ket == null || ket.trim().length() == 0) {
			try {
				List<Object[]> lama = hasilAngket.ambilValue();
				return lama == null ? values : lama;
			} catch (Exception e) {
				return values;
			}
		}
		String[] rows = ket.split("___");
		for (int i = 0; i < rows.length; i++) {
			Object[] parsed = parseAngketValueGuru(rows[i]);
			if (parsed != null) {
				values.add(parsed);
			}
		}
		return values;
	}

	/** Mem-parsing satu segmen jawaban legacy berformat {@code "DATA<idPertanyaan>;<nilai><>keterangan"} (penanda {@code "DATA"} di awal, id dan nilai dipisah {@code ";"}, keterangan opsional dipisah {@code "<>"}); mengembalikan {@code null} bila format tidak dikenali. */
	private Object[] parseAngketValueGuru(String value) {
		if (value == null) {
			return null;
		}
		String s = value.trim();
		if (s.length() == 0 || !s.startsWith("DATA")) {
			return null;
		}
		int titikKoma = s.indexOf(';');
		if (titikKoma <= 4) {
			return null;
		}
		int separatorKeterangan = s.indexOf("<>", titikKoma + 1);
		if (separatorKeterangan < 0) {
			separatorKeterangan = s.length();
		}
		try {
			Object[] row = new Object[3];
			row[0] = Long.valueOf(s.substring(4, titikKoma).trim());
			row[1] = Integer.valueOf(s.substring(titikKoma + 1, separatorKeterangan).trim());
			row[2] = separatorKeterangan >= s.length() ? "" : s.substring(separatorKeterangan + 2);
			return row;
		} catch (Exception e) {
			return null;
		}
	}

	/** Merangkai seluruh bagian dasbor (kartu ringkasan, visualisasi CSS, distribusi nilai, ranking guru, masukan terbaru) ke {@link #dashboardContent}. */
	private void renderDashboard(DashboardData data) {
		Div container = new Div();
		container.setStyle("width:100%;height:100%;overflow:auto;box-sizing:border-box;padding:12px;background:#f6f8fb;");
		container.setParent(dashboardContent);

		container.appendChild(new Html("<div style='padding:16px;border-radius:14px;background:linear-gradient(135deg,#0f2763,#2959d9);color:white;margin-bottom:12px;'>"
				+ "<div style='font-size:20px;font-weight:bold;'>Dasbor Angket Guru</div>"
				+ "<div style='font-size:12px;opacity:.9;'>Ringkasan evaluasi guru berdasarkan jawaban siswa, kelompok/aspek, distribusi nilai, dan masukan terbaru.</div>"
				+ "</div>"));

		Div cards = new Div();
		cards.setStyle("display:flex;flex-wrap:wrap;gap:10px;margin-bottom:12px;");
		cards.setParent(container);
		cards.appendChild(card("Angket Terisi", INTEGER.get().format(data.totalAngket), "Jumlah formulir angket yang masuk", data.formRows, new String[] { "Guru", "Siswa", "Jadwal" }));
		cards.appendChild(card("Siswa Pengisi", INTEGER.get().format(data.siswaIds.size()), "Responden unik", popupRows(new Object[] { "Siswa Pengisi", INTEGER.get().format(data.siswaIds.size()) }), new String[] { "Data", "Nilai" }));
		cards.appendChild(card("Guru Dinilai", INTEGER.get().format(data.guruIds.size()), "Guru unik", data.getGuruRanking(true), new String[] { "Guru", "Rata-rata", "Jumlah" }));
		cards.appendChild(card("Jadwal Terkait", INTEGER.get().format(data.jadwalIds.size()), "Kelas/jadwal yang dinilai", popupRows(new Object[] { "Jadwal Terkait", INTEGER.get().format(data.jadwalIds.size()) }), new String[] { "Data", "Nilai" }));
		cards.appendChild(card("Pertanyaan Aktif", INTEGER.get().format(data.totalPertanyaan), "Butir angket", popupRows(new Object[] { "Pertanyaan Aktif", INTEGER.get().format(data.totalPertanyaan) }), new String[] { "Data", "Nilai" }));
		cards.appendChild(card("Rata-rata", DECIMAL.get().format(data.getAverage()), "Skala 1 sampai 5", data.detailRows, new String[] { "Guru", "Siswa/Pertanyaan", "Nilai", "Catatan" }));

		renderCssVisualizations(container, data);

		Div left = new Div();
		left.setStyle("display:flex;flex-wrap:wrap;gap:12px;");
		left.setParent(container);
		renderDistribution(left, data);
		renderStatGrid(left, "Rata-rata per Kelompok/Aspek", "Kelompok", new ArrayList<Stat>(data.groupStats.values()), 10);
		renderStatGrid(left, "Guru dengan Nilai Tertinggi", "Guru", data.getGuruRanking(true), 10);
		renderStatGrid(left, "Guru Perlu Perhatian", "Guru", data.getGuruRanking(false), 10);
		renderMasukan(left, data);
	}

	/** Membuat satu kartu statistik (judul, nilai besar, deskripsi); nilai dapat diklik untuk membuka popup rincian ({@link #showDataPopup}) bila {@code rowsData} diberikan. */
	private Div card(final String title, String value, String desc, final List rowsData, final String[] headers) {
		Div card = new Div();
		card.setStyle("min-width:155px;flex:1;background:white;border:1px solid #dde5f2;border-radius:12px;padding:12px;box-shadow:0 2px 8px rgba(0,0,0,.05);cursor:pointer;");
		card.setTooltiptext("Klik untuk melihat detail " + title);
		card.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDataPopup(title, headers, rowsData);
			}
		});
		card.appendChild(new Html("<div style='font-size:12px;color:#667085;'>" + esc(title) + "</div>"
				+ "<div style='font-size:24px;font-weight:bold;color:#0f2763;margin:3px 0;'>" + esc(value) + "</div>"
				+ "<div style='font-size:11px;color:#777;'>" + esc(desc) + "</div>"));
		return card;
	}


	/** Menyusun ketiga visualisasi HTML/CSS (distribusi nilai, tren, radar per kelompok) berdampingan. */
	private void renderCssVisualizations(Div parent, DashboardData data) {
		Div charts = new Div();
		charts.setStyle("display:flex;flex-wrap:wrap;gap:12px;margin-bottom:12px;");
		charts.setParent(parent);
		renderCssDistributionVisual(charts, data);
		renderCssTrendVisual(charts, data);
		renderCssSpiderVisual(charts, data);
	}

	/** Menggambar bar distribusi jumlah jawaban per nilai (1-5) sebagai bar CSS proporsional. */
	private void renderCssDistributionVisual(Div parent, DashboardData data) {
		Div box = visualBox(parent, "Grafik Distribusi Nilai", "Bar CSS jumlah jawaban nilai 1 sampai 5.");
		int max = 1;
		for (int i = 1; i <= 5; i++) {
			Integer val = data.distribusi.get(Integer.valueOf(i));
			if (val != null && val.intValue() > max) {
				max = val.intValue();
			}
		}
		for (int i = 1; i <= 5; i++) {
			int jumlah = data.distribusi.containsKey(Integer.valueOf(i)) ? data.distribusi.get(Integer.valueOf(i)).intValue() : 0;
			int percent = clampPercent(max <= 0 ? 0D : jumlah * 100.0D / max);
			List detail = data.nilaiDetails.get(Integer.valueOf(i));
			if (detail == null) {
				detail = popupRows(new Object[] { "Nilai " + i, INTEGER.get().format(jumlah), String.valueOf(percent) + "%" });
			}
			appendCssBar(box, "Nilai " + i, jumlah, percent, "Detail Grafik Nilai " + i,
					new String[] { "Guru", "Siswa/Pertanyaan", "Nilai", "Catatan" }, detail);
		}
	}

	/** Menggambar bar tren rata-rata nilai per guru (ranking) sebagai bar CSS. */
	private void renderCssTrendVisual(Div parent, DashboardData data) {
		Div box = visualBox(parent, "Trend Rata-rata Guru", "Trend CSS dari rata-rata guru pada filter aktif.");
		List<Stat> stats = data.getGuruRanking(true);
		int added = 0;
		for (int i = 0; stats != null && i < stats.size() && added < 8; i++) {
			Stat stat = stats.get(i);
			int percent = clampPercent(stat.average() * 20.0D);
			List detail = stat.details == null || stat.details.isEmpty() ? popupRows(new Object[] { stat.name, DECIMAL.get().format(stat.average()), INTEGER.get().format(stat.count) }) : stat.details;
			appendCssBar(box, stat.name, stat.average(), percent, "Detail Trend " + stat.name,
					new String[] { "Guru", "Siswa/Pertanyaan", "Nilai", "Catatan" }, detail);
			added++;
		}
		if (added == 0) {
			new Html("<div style='font-size:12px;color:#64748b;'>Belum ada data trend guru pada filter ini.</div>").setParent(box);
		}
	}

	/** Menggambar bar rata-rata nilai per kelompok pertanyaan ({@link GrupChecklistPenilaianGuru}) sebagai gauge CSS ("radar" sederhana berbentuk daftar bar, bukan diagram radar sesungguhnya). */
	private void renderCssSpiderVisual(Div parent, DashboardData data) {
		Div box = visualBox(parent, "Spider Web Aspek Guru", "Radar/spider web CSS berdasarkan rata-rata kelompok/aspek.");
		List<Stat> stats = new ArrayList<Stat>(data.groupStats.values());
		double total = 0D;
		int count = 0;
		StringBuffer labels = new StringBuffer();
		for (int i = 0; stats != null && i < stats.size() && count < 5; i++) {
			Stat stat = stats.get(i);
			total += stat.average();
			count++;
			labels.append("<div style='display:flex;justify-content:space-between;gap:8px;border-bottom:1px dashed #e5e7eb;padding:3px 0;'>")
					.append("<span>").append(esc(limit(stat.name, 34))).append("</span>")
					.append("<b>").append(esc(DECIMAL.get().format(stat.average()))).append("</b></div>");
		}
		double avg = count <= 0 ? 0D : total / count;
		int radar = clampPercent(avg * 20.0D);
		new Html("<div style='display:flex;align-items:center;gap:12px;'>"
				+ "<div style='position:relative;width:150px;height:150px;border-radius:50%;"
				+ "background:radial-gradient(circle,transparent 0 22%,#e5e7eb 23% 24%,transparent 25% 45%,#e5e7eb 46% 47%,transparent 48% 68%,#e5e7eb 69% 70%,transparent 71% 88%,#bbf7d0 89% 90%),"
				+ "conic-gradient(#16a34a 0 " + radar + "%,#dcfce7 " + radar + "% 100%);border:1px solid #bbf7d0;'>"
				+ "<div style='position:absolute;left:31px;top:52px;width:88px;text-align:center;background:rgba(255,255,255,.9);border-radius:12px;padding:6px 0;'>"
				+ "<div style='font-size:11px;color:#64748b;'>Rata-rata</div><div style='font-size:20px;font-weight:bold;color:#15803d;'>" + esc(DECIMAL.get().format(avg)) + "</div></div></div>"
				+ "<div style='flex:1;font-size:12px;color:#334155;'>" + labels.toString() + "</div></div>").setParent(box);
	}

	/** @return kontainer kartu visualisasi dengan judul+subjudul, ditempel ke {@code parent}. */
	private Div visualBox(Div parent, String title, String subtitle) {
		Div box = new Div();
		box.setStyle("background:white;border:1px solid #dde5f2;border-radius:12px;padding:12px;min-width:280px;flex:1;box-sizing:border-box;box-shadow:0 2px 8px rgba(0,0,0,.05);");
		box.setParent(parent);
		box.appendChild(new Html("<div style='font-weight:bold;color:#0f2763;margin-bottom:2px;'>" + esc(title) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>" + esc(subtitle) + "</div>"));
		return box;
	}

	private void appendCssBar(Div parent, String label, double value, int percent, final String popupTitle,
			final String[] headers, final List detail) {
		Div row = new Div();
		row.setStyle("margin:7px 0;cursor:pointer;");
		row.setTooltiptext("Klik untuk melihat detail");
		row.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDataPopup(popupTitle, headers, detail);
			}
		});
		row.setParent(parent);
		String nilai = Math.abs(value - Math.round(value)) < 0.0001D ? INTEGER.get().format(Math.round(value)) : DECIMAL.get().format(value);
		new Html("<div style='display:flex;justify-content:space-between;font-size:12px;color:#334155;margin-bottom:3px;'>"
				+ "<span>" + esc(limit(label, 38)) + "</span><b>" + esc(nilai) + "</b></div>"
				+ "<div style='height:10px;background:#ecfdf5;border-radius:999px;overflow:hidden;'>"
				+ "<div style='height:10px;width:" + percent + "%;background:linear-gradient(90deg,#0ea5e9,#22c55e);border-radius:999px;'></div></div>").setParent(row);
	}

	/** @return {@code value} dibatasi ke rentang 0-100 (dibulatkan), dipakai sebagai lebar bar CSS persentase. */
	private int clampPercent(double value) {
		if (value < 0D) {
			return 0;
		}
		if (value > 100D) {
			return 100;
		}
		return (int) Math.round(value);
	}

	/** Menampilkan tabel ringkasan distribusi nilai (jumlah dan persentase tiap nilai 1-5), tiap baris dapat diklik untuk membuka rincian jawaban bernilai tersebut. */
	private void renderDistribution(Div parent, DashboardData data) {
		Div box = section(parent, "Distribusi Nilai");
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(box);
		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig col = new MyColumnConfig();
		col.setLabel("Nilai");
		col.setParent(columns);
		col = new MyColumnConfig();
		col.setLabel("Jumlah");
		col.setParent(columns);
		col = new MyColumnConfig();
		col.setLabel("Persentase");
		col.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);
		for (int i = 5; i >= 1; i--) {
			int count = data.distribusi.containsKey(Integer.valueOf(i)) ? data.distribusi.get(Integer.valueOf(i)).intValue() : 0;
			double pct = data.totalNilai == 0 ? 0.0 : (count * 100.0 / data.totalNilai);
			List detail = data.nilaiDetails.get(Integer.valueOf(i));
			if (detail == null) {
				detail = popupRows(new Object[] { "Nilai " + i, INTEGER.get().format(count), DECIMAL.get().format(pct) + "%" });
			}
			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			clickableLabel("Nilai " + i, "Detail Nilai " + i, new String[] { "Guru", "Siswa/Pertanyaan", "Nilai", "Catatan" }, detail).setParent(row);
			clickableLabel(INTEGER.get().format(count), "Detail Nilai " + i, new String[] { "Guru", "Siswa/Pertanyaan", "Nilai", "Catatan" }, detail).setParent(row);
			clickableLabel(DECIMAL.get().format(pct) + "%", "Detail Nilai " + i, new String[] { "Guru", "Siswa/Pertanyaan", "Nilai", "Catatan" }, detail).setParent(row);
		}
	}

	/** Menampilkan tabel ranking (nama, jumlah jawaban, rata-rata, bar proporsional), dibatasi {@code max} baris teratas; tiap baris dapat diklik untuk membuka rincian jawaban. */
	private void renderStatGrid(Div parent, String title, String firstHeader, List<Stat> stats, int max) {
		Div box = section(parent, title);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(box);
		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig col = new MyColumnConfig();
		col.setLabel(firstHeader);
		col.setParent(columns);
		col = new MyColumnConfig();
		col.setLabel("Rata-rata");
		col.setWidth("90px");
		col.setParent(columns);
		col = new MyColumnConfig();
		col.setLabel("Jumlah");
		col.setWidth("80px");
		col.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);
		int added = 0;
		for (Stat stat : stats) {
			if (added >= max) {
				break;
			}
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			List detail = stat.details == null || stat.details.isEmpty() ? popupRows(new Object[] { stat.name, DECIMAL.get().format(stat.average()), INTEGER.get().format(stat.count) }) : stat.details;
			clickableLabel(stat.name, "Detail " + stat.name, new String[] { "Guru", "Siswa/Pertanyaan", "Nilai", "Catatan" }, detail).setParent(row);
			clickableLabel(DECIMAL.get().format(stat.average()), "Detail " + stat.name, new String[] { "Guru", "Siswa/Pertanyaan", "Nilai", "Catatan" }, detail).setParent(row);
			clickableLabel(INTEGER.get().format(stat.count), "Detail " + stat.name, new String[] { "Guru", "Siswa/Pertanyaan", "Nilai", "Catatan" }, detail).setParent(row);
			added++;
		}
		if (added == 0) {
			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "3");
			new Label(ais.common.Common.getBahasaConfig("Belum ada data.")).setParent(row);
		}
	}

	/** Menampilkan hingga 10 masukan/komentar terbaru dari siswa (guru, siswa/pertanyaan, isi masukan). */
	private void renderMasukan(Div parent, DashboardData data) {
		Div box = section(parent, "Masukan / Catatan Terbaru");
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(box);
		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig col = new MyColumnConfig();
		col.setLabel("Guru / Pertanyaan");
		col.setWidth("28%");
		col.setParent(columns);
		col = new MyColumnConfig();
		col.setLabel("Pengisi / Catatan");
		col.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);
		int added = 0;
		for (String[] m : data.masukanTerbaru) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			List detail = popupRows(m);
			clickableLabel(m[0] == null ? "" : m[0], "Detail Masukan", new String[] { "Guru/Pertanyaan", "Pengisi", "Catatan" }, detail).setParent(row);
			clickableLabel((m[1] == null ? "" : m[1]) + " - " + (m[2] == null ? "" : m[2]), "Detail Masukan", new String[] { "Guru/Pertanyaan", "Pengisi", "Catatan" }, detail).setParent(row);
			added++;
			if (added >= 10) {
				break;
			}
		}
		if (added == 0) {
			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			new Label("Belum ada masukan/keterangan.").setParent(row);
		}
	}

	/** @return kontainer panel bergaya kartu dengan judul dan deskripsi penjelas otomatis ({@link #sectionDescription}), ditempel ke {@code parent}. */
	private Div section(Div parent, String title) {
		Div box = new Div();
		box.setStyle("background:white;border:1px solid #dde5f2;border-radius:12px;padding:12px;min-width:360px;flex:1;box-sizing:border-box;margin-bottom:12px;");
		box.setParent(parent);
		box.appendChild(new Html("<div style='font-weight:bold;color:#0f2763;margin-bottom:4px;'>" + esc(title) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;line-height:1.45;margin-bottom:8px;'>"
				+ esc(sectionDescription(title)) + "</div>"));
		return box;
	}

	/** @return kalimat penjelas awam untuk panel {@code title} (dicocokkan lewat kata kunci dalam judul), untuk membantu pembaca non-teknis memahami tiap panel dasbor. */
	private String sectionDescription(String title) {
		if (title == null) {
			return "Panel ini membantu membaca hasil angket guru secara ringkas tanpa menghitung data secara manual.";
		}
		String t = title.toLowerCase();
		if (t.indexOf("distribusi") >= 0) {
			return "Panel ini menampilkan sebaran nilai yang diberikan siswa. Pengguna dapat melihat apakah penilaian guru cenderung tinggi atau perlu perhatian.";
		}
		if (t.indexOf("kelompok") >= 0 || t.indexOf("aspek") >= 0) {
			return "Panel ini merangkum nilai berdasarkan aspek penilaian guru. Informasi ini membantu sekolah menentukan aspek pembelajaran yang perlu diperkuat.";
		}
		if (t.indexOf("tertinggi") >= 0) {
			return "Panel ini menampilkan guru dengan rata-rata penilaian terbaik sebagai bahan apresiasi dan contoh praktik baik.";
		}
		if (t.indexOf("perhatian") >= 0) {
			return "Panel ini menunjukkan guru atau aspek yang perlu tindak lanjut. Data ini dapat digunakan untuk pembinaan yang lebih tepat sasaran.";
		}
		if (t.indexOf("masukan") >= 0) {
			return "Panel ini menampilkan komentar siswa terbaru. Masukan ini berguna untuk memahami alasan di balik angka penilaian.";
		}
		return "Panel ini membantu membaca hasil angket guru secara ringkas tanpa menghitung data secara manual.";
	}

	private String selectedString(Combobox combo, String def) {
		try {
			Object value = combo == null || combo.getSelectedItem() == null ? null : combo.getSelectedItem().getValue();
			return value == null ? def : String.valueOf(value);
		} catch (Exception e) {
			return def;
		}
	}

	private Object selectedValue(Combobox combo) {
		try {
			Object value = combo == null || combo.getSelectedItem() == null ? null : combo.getSelectedItem().getValue();
			if (value instanceof String && isSemua(String.valueOf(value))) {
				return null;
			}
			return value;
		} catch (Exception e) {
			return null;
		}
	}

	/** @return guru terpilih pada filter {@link #guru} (dibaca dari atribut {@code "guru"} lalu {@code "myValue"}), atau {@code null} bila tidak dipilih/gagal dibaca. */
	private Guru selectedGuru() {
		try {
			Object value = guru == null ? null : guru.getAttribute("guru");
			if (value instanceof Guru) {
				return (Guru) value;
			}
			value = guru == null ? null : guru.getAttribute("myValue");
			if (value instanceof Guru) {
				return (Guru) value;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketGuruDashboardWindow.java:755");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Guru Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		return null;
	}

	/**
	 * @return {@code true} bila belum ada filter sekolah/yayasan dipilih, atau sekolah/yayasan
	 *         hasil resolusi dari {@code jadwal} (dicoba dari beberapa jalur relasi berbeda:
	 *         langsung, lewat kelas, lewat rombongan belajar, lewat siswa) cocok dengan yang dipilih
	 */
	private boolean matchSekolahYayasan(Object jadwal, Object selectedYayasan, Object selectedSekolah) {
		if (selectedYayasan == null && selectedSekolah == null) {
			return true;
		}
		Object sekolahAktif = firstNonNull(safeCall(jadwal, "getSekolah"), safeCall(safeCall(jadwal, "getKelas"), "getSekolah"),
				safeCall(safeCall(jadwal, "getRombonganBelajar"), "getSekolah"), safeCall(safeCall(jadwal, "getSiswa"), "getSekolah"));
		if (selectedSekolah != null && !sameId(sekolahAktif, selectedSekolah)) {
			return false;
		}
		Object yayasanAktif = firstNonNull(safeCall(jadwal, "getYayasan"), safeCall(sekolahAktif, "getYayasan"),
				safeCall(safeCall(jadwal, "getSekolah"), "getYayasan"));
		if (selectedYayasan != null && !sameId(yayasanAktif, selectedYayasan)) {
			return false;
		}
		return true;
	}

	private Object firstNonNull(Object a, Object b, Object c) {
		return firstNonNull(a, b, c, null);
	}

	private Object firstNonNull(Object a, Object b, Object c, Object d) {
		if (a != null) return a;
		if (b != null) return b;
		if (c != null) return c;
		return d;
	}

	private boolean sameId(Object a, Object b) {
		Long idA = safeId(a);
		Long idB = safeId(b);
		if (idA == null || idB == null) {
			return a == b || (a != null && a.equals(b));
		}
		return idA.equals(idB);
	}

	/**
	 * @return {@code true} bila baris angket tidak punya jadwal terkait, atau tahun ajaran/semester
	 *         jadwalnya (dibaca lewat beberapa nama getter berbeda karena variasi model jadwal)
	 *         cocok dengan filter {@code ta}/{@code smt} yang dipilih (filter kosong/"Semua" selalu cocok)
	 */
	private boolean matchJadwal(ChecklistBaruPenilaianGuruOlehSiswa hasil, String ta, String smt) {
		Object jadwal = safeCall(hasil, "getJadwalPelajaran");
		if (jadwal == null) {
			return true;
		}
		if (!isSemua(ta)) {
			String tahun = firstNonEmpty(safeCallString(jadwal, "getTahunAjaran"), safeCallString(jadwal, "getTahunAkademik"),
					safeCallString(jadwal, "getTahunPelajaran"));
			if (tahun != null && tahun.trim().length() > 0 && !ta.equalsIgnoreCase(tahun.trim())) {
				return false;
			}
		}
		if (!isSemua(smt)) {
			String semester = firstNonEmpty(safeCallString(jadwal, "getGanjilGenap"), safeCallString(jadwal, "getSemester"),
					safeCallString(jadwal, "getSemesterAbsensi"));
			if (semester != null && semester.trim().length() > 0 && !smt.equalsIgnoreCase(semester.trim())) {
				return false;
			}
		}
		return true;
	}

	private boolean isSemua(String value) {
		return value == null || value.trim().length() == 0 || "Semua".equalsIgnoreCase(value.trim());
	}

	private String firstNonEmpty(String a, String b, String c) {
		if (a != null && a.trim().length() > 0) return a;
		if (b != null && b.trim().length() > 0) return b;
		if (c != null && c.trim().length() > 0) return c;
		return null;
	}

	private Object safeCall(Object target, String method) {
		try {
			if (target == null) return null;
			Method m = target.getClass().getMethod(method, new Class[0]);
			return m.invoke(target, new Object[0]);
		} catch (Exception e) {
			return null;
		}
	}

	private String safeCallString(Object target, String method) {
		Object o = safeCall(target, method);
		return o == null ? null : String.valueOf(o);
	}

	private Guru safeGuru(ChecklistBaruPenilaianGuruOlehSiswa hasil) {
		try {
			return hasil.getGuru();
		} catch (Exception e) {
			return null;
		}
	}

	private Siswa safeSiswa(ChecklistBaruPenilaianGuruOlehSiswa hasil) {
		try {
			return hasil.getSiswa();
		} catch (Exception e) {
			return null;
		}
	}

	private Long safeId(Object obj) {
		try {
			Object id = safeCall(obj, "getId");
			return toLong(id);
		} catch (Exception e) {
			return null;
		}
	}

	private Long toLong(Object value) {
		try {
			if (value instanceof Long) return (Long) value;
			if (value instanceof Number) return Long.valueOf(((Number) value).longValue());
			return Long.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return null;
		}
	}

	private Integer toInteger(Object value) {
		try {
			if (value instanceof Integer) return (Integer) value;
			if (value instanceof Number) return Integer.valueOf(((Number) value).intValue());
			return Integer.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return null;
		}
	}

	private String safeToString(Object object) {
		try {
			return object == null ? "" : object.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private String limit(String value, int max) {
		if (value == null) {
			return "";
		}
		String text = value.replace('\n', ' ').replace('\r', ' ').trim();
		if (text.length() <= max) {
			return text;
		}
		return text.substring(0, max) + "...";
	}

	private String esc(String value) {
		if (value == null) return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/** @return batas maksimum baris angket yang dipindai per pemuatan dasbor, dari konfigurasi {@code maksimal_data_dasbor_angket_guru} (default dan fallback 20000, dipaksa minimal 100). */
	private int readMaxScan() {
		try {
			String nilai = Common.getKonfigurasi("maksimal_data_dasbor_angket_guru", "20000").getNilai();
			int max = Integer.parseInt(nilai);
			return max < 100 ? 100 : max;
		} catch (Exception e) {
			return 20000;
		}
	}


	/** @return label bergaya tautan yang membuka popup rincian ({@link #showDataPopup}) saat diklik. */
	private Label clickableLabel(String text, final String title, final String[] headers, final List rowsData) {
		Label label = new Label(text == null ? "" : text);
		label.setStyle("cursor:pointer;color:#1d4ed8;text-decoration:underline;");
		label.setTooltiptext("Klik untuk melihat detail");
		label.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDataPopup(title, headers, rowsData);
			}
		});
		return label;
	}

	private List popupRows(Object data) {
		List rows = new ArrayList();
		rows.add(data);
		return rows;
	}

	/** Menampilkan jendela modal berisi tabel rincian ({@code headers} sebagai kolom, {@code rowsData} sebagai baris) untuk drill-down dari kartu/label dasbor yang diklik. */
	private void showDataPopup(String title, String[] headers, List rowsData) {
		try {
			MyWindow window = new MyWindow();
			window.setTitle(title == null ? "Detail Data" : title);
			window.setWidth("84%");
			window.setHeight("78%");
			window.setClosable(true);
			window.setParent(this);
			Div wrap = new Div();
			wrap.setStyle("height:100%;overflow:auto;padding:10px;box-sizing:border-box;");
			wrap.setParent(window);
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.setParent(wrap);
			Columns columns = new Columns();
			columns.setParent(grid);
			if (headers == null || headers.length == 0) {
				headers = new String[] { "Data" };
			}
			for (int i = 0; i < headers.length; i++) {
				MyColumnConfig c = new MyColumnConfig();
				c.setLabel(headers[i]);
				c.setParent(columns);
			}
			Rows rows = new Rows();
			rows.setParent(grid);
			if (rowsData == null || rowsData.isEmpty()) {
				MyFormRow row = new MyFormRow();
				row.setParent(rows);
				row.setSpans(String.valueOf(headers.length));
				new Label(ais.common.Common.getBahasaConfig("Tidak ada data detail.")).setParent(row);
			} else {
				for (int i = 0; i < rowsData.size(); i++) {
					Object[] arr = toArray(rowsData.get(i));
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					for (int j = 0; j < headers.length; j++) {
						new Label(j < arr.length && arr[j] != null ? String.valueOf(arr[j]) : "").setParent(row);
					}
				}
			}
			window.setVisible(true);
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Guru Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private Object[] toArray(Object data) {
		if (data instanceof Object[]) {
			return (Object[]) data;
		}
		return new Object[] { data };
	}

	private void closeSession(Session session) {
		if (session == null) return;
		try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketGuruDashboardWindow.java:1004");}
		try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketGuruDashboardWindow.java:1005");}
		try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketGuruDashboardWindow.java:1006");}
	}

	/** Akumulator statistik sederhana (jumlah, total, rata-rata) untuk satu entitas (kelompok pertanyaan atau guru), beserta baris detail jawaban yang menyusunnya. */
	private static class Stat {
		String name;
		double total;
		int count;
		List details = new ArrayList();
		Stat(String name) { this.name = name; }
		void add(double value) { total += value; count++; }
		double average() { return count == 0 ? 0.0 : total / count; }
	}

	/** Kumpulan data teragregasi hasil {@link #loadDashboardData}: filter yang dipakai, statistik keseluruhan, distribusi nilai, statistik per kelompok pertanyaan dan per guru, serta daftar baris untuk drill-down (masukan terbaru, detail jawaban, baris pengisi). */
	private static class DashboardData {
		String tahunAkademik;
		String semester;
		Guru guru;
		Object yayasan;
		Object sekolah;
		int totalAngket;
		int totalKelompok;
		int totalPertanyaan;
		int totalNilai;
		double totalSkor;
		Set<Long> siswaIds = new HashSet<Long>();
		Set<Long> guruIds = new HashSet<Long>();
		Set<Long> jadwalIds = new HashSet<Long>();
		Map<Integer, Integer> distribusi = new TreeMap<Integer, Integer>();
		Map<Integer, List> nilaiDetails = new TreeMap<Integer, List>();
		Map<String, Stat> groupStats = new LinkedHashMap<String, Stat>();
		Map<Long, Stat> guruStats = new LinkedHashMap<Long, Stat>();
		Map<Long, String> guruNames = new HashMap<Long, String>();
		List<String[]> masukanTerbaru = new ArrayList<String[]>();
		List<String[]> detailRows = new ArrayList<String[]>();
		List<String[]> formRows = new ArrayList<String[]>();

		void addNilai(int nilai) {
			totalNilai++;
			totalSkor += nilai;
			Integer key = Integer.valueOf(nilai);
			distribusi.put(key, Integer.valueOf(distribusi.containsKey(key) ? distribusi.get(key).intValue() + 1 : 1));
		}
		double getAverage() { return totalNilai == 0 ? 0.0 : totalSkor / totalNilai; }
		void addGroup(String groupName, int nilai) {
			if (groupName == null || groupName.trim().length() == 0) groupName = "Tanpa Kelompok";
			Stat stat = groupStats.get(groupName);
			if (stat == null) { stat = new Stat(groupName); groupStats.put(groupName, stat); }
			stat.add(nilai);
		}
		void addGuru(Guru guru) {
			try {
				if (guru != null && guru.getId() != null) guruNames.put(guru.getId(), guru.toString());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketGuruDashboardWindow.java:1058");}
		}
		void addGuruNilai(Guru guru, int nilai) {
			if (guru == null || guru.getId() == null) return;
			Long id = guru.getId();
			Stat stat = guruStats.get(id);
			if (stat == null) { stat = new Stat(guruNames.containsKey(id) ? guruNames.get(id) : guru.toString()); guruStats.put(id, stat); }
			stat.add(nilai);
		}
		void addDetailToGroup(String groupName, Object detail) {
			if (groupName == null || groupName.trim().length() == 0) groupName = "Tanpa Kelompok";
			Stat stat = groupStats.get(groupName);
			if (stat != null) stat.details.add(detail);
		}
		void addDetailToGuru(Guru guru, Object detail) {
			if (guru == null || guru.getId() == null) return;
			Stat stat = guruStats.get(guru.getId());
			if (stat != null) stat.details.add(detail);
		}
		List<Stat> getGuruRanking(final boolean descending) {
			List<Stat> stats = new ArrayList<Stat>(guruStats.values());
			Collections.sort(stats, new Comparator<Stat>() {
				@Override public int compare(Stat o1, Stat o2) {
					int c = Double.compare(o1.average(), o2.average());
					return descending ? -c : c;
				}
			});
			return stats;
		}
	}
}
