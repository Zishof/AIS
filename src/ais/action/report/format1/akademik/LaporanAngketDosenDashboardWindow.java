package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;
import ais.ui.util.DashboardGridExportHelper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Query;
import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Jendela dasbor "Angket Dosen": merangkum hasil kuesioner penilaian dosen oleh mahasiswa
 * ({@link ChecklistBaruPenilaianDosenOlehMahasiswa}) menjadi satu tampilan visual, difilter
 * opsional per tahun akademik, semester (termasuk semester pendek), fakultas/jurusan, dosen
 * tertentu, dan masa perkuliahan. Ekspor grid didukung lewat {@link DashboardGridExportHelper}.
 *
 * <h2>Struktur dasbor</h2>
 * <p>
 * {@link #renderDashboard(Filter)} menyusun dasbor dalam urutan: kartu ringkasan
 * ({@link #renderCards}), visualisasi berbasis CSS murni tanpa library chart eksternal — distribusi
 * nilai, tren, dan spider/radar per aspek penilaian ({@link #renderCssVisualizations} dan turunannya
 * {@link #renderCssDistributionVisual}, {@link #renderCssTrendVisual}, {@link #renderCssSpiderVisual}),
 * distribusi detail ({@link #renderDistribution}), ringkasan per kelompok pertanyaan
 * ({@link #renderGroupSummary}), daftar dosen dengan nilai tertinggi dan terendah
 * ({@link #renderDosenSummary}), ringkasan per mata kuliah/kelas ({@link #renderPerkuliahanSummary})
 * dan per program studi ({@link #renderProdiSummary}), pertanyaan dengan skor terendah
 * ({@link #renderQuestionLowSummary}), serta masukan/komentar terbaru dari mahasiswa
 * ({@link #renderMasukanTerbaru}). Setiap kartu/label ringkasan dapat diklik untuk membuka
 * {@link #showDataPopup popup rincian} baris data penyusunnya.
 * </p>
 * <p>
 * Pengambilan data ({@link #loadDashboardData}) membaca seluruh jawaban kuesioner yang cocok
 * filter ({@link #listJawaban}) beserta peta pertanyaan aktif ({@link #loadQuestionMap}), lalu
 * mengagregasinya di sisi Java (bukan SQL agregat murni) memakai struktur batin
 * {@link Accumulator}/{@link MahasiswaKey}/{@link QuestionInfo} untuk menghitung rata-rata,
 * distribusi, dan peringkat per dosen/kelas/prodi/pertanyaan. Perubahan filter pada toolbar memicu
 * {@link #reloadDashboard()} yang membaca ulang filter ({@link #readFilter()}) dan memanggil ulang
 * {@link #renderDashboard(Filter)}. Setiap sesi Hibernate native yang dibuka ditutup lewat
 * {@link #closeSession(Session)} pada blok {@code finally} untuk mencegah kebocoran koneksi.
 * </p>
 * <p>
 * Seluruh method render/hitung bersifat privat (murni detail implementasi tampilan); satu-satunya
 * API publik kelas ini adalah tiga konstruktornya.
 * </p>
 */
public class LaporanAngketDosenDashboardWindow extends MyWindow {

	private static final long serialVersionUID = 6409442098743220321L;

	private Combobox tahunAkademik;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox semesterAbsensi;
	private Combobox program;
	private AmbilDataDosenBanbox dosen;
	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig aktif;
	private Center center;
	private Div dashboardContent;
	private Dosen dsn;

	/** Membuka dasbor tanpa filter dosen awal (menampilkan data seluruh dosen sesuai filter default). */
	public LaporanAngketDosenDashboardWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/** Membuka dasbor dengan filter dosen awal terpasang ke {@code dsn} (mis. dosen membuka dasbor hasil penilaian dirinya sendiri). */
	public LaporanAngketDosenDashboardWindow(Dosen dsn) {
		super();
		this.dsn = dsn;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/** Membuka dasbor dengan judul/border/closable kustom untuk jendela. */
	public LaporanAngketDosenDashboardWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Laporan Angket Dosen Dashboard Window");
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

		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		program = Common.initPrograms(null);
		program.setWidth("110px");
		program.addEventListener("onChange", reloadListener);
		program.setParent(toolbar);

		semesterPendek = new MyCheckboxConfig("SP");
		semesterPendek.addEventListener("onCheck", reloadListener);
		semesterPendek.setParent(toolbar);

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

		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("95%");
		fakultas.setReadonly(true);
		fakultas.addEventListener("onChange", reloadListener);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("95%");
		jurusan.setReadonly(true);
		jurusan.addEventListener("onChange", reloadListener);
		jurusan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (jurusan.getSelectedItem() != null && jurusan.getSelectedItem().getValue() instanceof Jurusan) {
					masaPerkuliahan.setJurusanSelected((Jurusan) jurusan.getSelectedItem().getValue());
				}
			}
		});

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox();
		masaPerkuliahan.setWidth("95%");
		masaPerkuliahan.addEventListener("onChange", reloadListener);
		row.appendChild(masaPerkuliahan);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen")));
		dosen = new AmbilDataDosenBanbox();
		dosen.setWidth("95%");
		dosen.setReadonly(true);
		dosen.addEventListener("onChange", reloadListener);
		row.appendChild(dosen);

		if (dsn != null) {
			dosen.setAttribute("dosen", dsn);
			dosen.setAttribute("myValue", dsn);
			dosen.setValue(dsn.getNama() == null ? "" : dsn.getNama());
		}

		Common.initKeterangan(rows, "Jika dosen tidak dipilih, maka akan tampil data semua dosen");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		MyButtonConfig tombol = new MyButtonConfig("Muat Ulang Dasbor", "/img/svg/refresh.svg");
		tombol.addEventListener("onClick", reloadListener);
		hbox.appendChild(tombol);
		hbox.appendChild(LaporanRekapAngketDosenPerJurusanWindow.hitungUlangAngket(tahunAkademik, semesterAbsensi,
				masaPerkuliahan, aktif));

		Html info = new Html("<div style='padding:10px;line-height:1.6;color:#374151;'>"
				+ "Dasbor ini membaca data hasil angket dosen yang tersimpan pada jawaban mahasiswa, "
				+ "menghitung distribusi nilai, rerata dosen, rerata per aspek, per prodi, per mata kuliah, dan masukan terbaru."
				+ "</div>");
		info.setParent(filterWrapper);

		dashboardContent = new Div();
		dashboardContent.setStyle("width:100%;box-sizing:border-box;");
		dashboardContent.setParent(mainWrapper);

		reloadDashboard();
	}

	private void reloadDashboard() throws Exception {
		Common.clear(dashboardContent);
		Html loading = new Html("<div style='padding:14px;color:#555;'><i class='fa fa-spinner fa-spin'></i> Memuat dasbor angket dosen...</div>");
		loading.setParent(dashboardContent);
		try {
			final Filter filter = readFilter();
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					renderDashboard(filter);
				}
			});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private Filter readFilter() {
		Filter filter = new Filter();
		filter.tahunAkademik = selectedString(tahunAkademik, "Semua");
		filter.semester = selectedString(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		if (semesterPendek != null && semesterPendek.isChecked()) {
			filter.semester = Perkuliahan.SP;
		}
		filter.program = selectedString(program, "Semua");
		filter.fakultas = (Fakultas) selectedValue(fakultas, Fakultas.class);
		filter.jurusan = (Jurusan) selectedValue(jurusan, Jurusan.class);
		filter.masaPerkuliahan = selectedMasaPerkuliahan();
		filter.dosen = selectedDosen();
		filter.onlyActive = aktif == null || aktif.isChecked();
		return filter;
	}

	private Object selectedValue(Combobox combo, Class expectedClass) {
		try {
			if (combo == null || combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null) {
				return null;
			}
			Object value = combo.getSelectedItem().getValue();
			if (expectedClass.isInstance(value)) {
				return value;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketDosenDashboardWindow.java:315");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		return null;
	}

	private String selectedString(Combobox combo, String defaultValue) {
		try {
			if (combo == null || combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null) {
				return defaultValue;
			}
			Object value = combo.getSelectedItem().getValue();
			return value == null ? defaultValue : String.valueOf(value);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private Dosen selectedDosen() {
		if (dsn != null) {
			return dsn;
		}
		try {
			Object value = dosen == null ? null : dosen.getAttribute("dosen");
			if (value instanceof Dosen) {
				return (Dosen) value;
			}
			value = dosen == null ? null : dosen.getAttribute("myValue");
			if (value instanceof Dosen) {
				return (Dosen) value;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketDosenDashboardWindow.java:345");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		return null;
	}

	private MasaPerkuliahan selectedMasaPerkuliahan() {
		try {
			Object value = masaPerkuliahan == null ? null : masaPerkuliahan.getAttribute("masaPerkuliahan");
			if (value instanceof MasaPerkuliahan) {
				return (MasaPerkuliahan) value;
			}
			value = masaPerkuliahan == null ? null : masaPerkuliahan.getAttribute("myValue");
			if (value instanceof MasaPerkuliahan) {
				return (MasaPerkuliahan) value;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketDosenDashboardWindow.java:360");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		return null;
	}

	private void renderDashboard(Filter filter) {
		Session session = null;
		try {
			session = ais.action.report.Report.openNativeSession();
			DashboardData data = loadDashboardData(session, filter);
			Common.clear(dashboardContent);

			Div wrapper = new Div();
			wrapper.setStyle("padding:14px;overflow:auto;box-sizing:border-box;");
			wrapper.setParent(dashboardContent);

			Html title = new Html("<div style='margin-bottom:12px;'>"
					+ "<div style='font-size:18px;font-weight:bold;color:#163d7a;'>Dasbor Angket Dosen</div>"
					+ "<div style='color:#666;font-size:12px;'>Tahun Akademik: <b>" + html(filter.tahunAkademik)
					+ "</b> &nbsp; Semester: <b>" + html(filter.semester) + "</b> &nbsp; Prodi: <b>"
					+ html(filter.jurusan == null ? "Semua" : filter.jurusan.getNama()) + "</b> &nbsp; Dosen: <b>"
					+ html(filter.dosen == null ? "Semua" : filter.dosen.getNama()) + "</b></div></div>");
			title.setParent(wrapper);

			renderCards(wrapper, data);
			renderCssVisualizations(wrapper, data);
			renderDistribution(wrapper, data);
			renderGroupSummary(wrapper, data.groupRows);
			renderDosenSummary(wrapper, data.dosenRows, true);
			renderDosenSummary(wrapper, data.dosenRowsBottom, false);
			renderPerkuliahanSummary(wrapper, data.perkuliahanRows);
			renderProdiSummary(wrapper, data.prodiRows);
			renderQuestionLowSummary(wrapper, data.questionLowRows);
			renderMasukanTerbaru(wrapper, data.masukanRows);
		} catch (Exception e) {
			Common.clear(dashboardContent);
			Html error = new Html("<div style='padding:14px;color:#b91c1c;background:#fee2e2;border:1px solid #fecaca;'>"
					+ "Gagal memuat dasbor angket dosen. Detail error: " + html(e.getMessage()) + "</div>");
			error.setParent(dashboardContent);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
	}

	@SuppressWarnings("unchecked")
	private DashboardData loadDashboardData(Session session, Filter filter) {
		DashboardData data = new DashboardData();
		Map questionMap = loadQuestionMap(session, filter.onlyActive);
		List hasil = listJawaban(session, filter);
		int maxScan = readIntKonfigurasi("maksimal_data_dasbor_angket_dosen", 20000);

		for (int i = 0; i < hasil.size() && i < maxScan; i++) {
			ChecklistBaruPenilaianDosenOlehMahasiswa h = (ChecklistBaruPenilaianDosenOlehMahasiswa) hasil.get(i);
			if (h == null) {
				continue;
			}
			data.totalAngket++;
			Dosen d = safeDosen(h);
			Perkuliahan p = safePerkuliahan(h);
			MahasiswaKey mahasiswaKey = new MahasiswaKey(h);
			if (mahasiswaKey.value != null) {
				data.mahasiswaUnik.put(mahasiswaKey.value, mahasiswaKey.value);
			}
			if (d != null && d.getId() != null) {
				data.dosenUnik.put(d.getId(), d.getId());
			}
			if (p != null && p.getId() != null) {
				data.perkuliahanUnik.put(p.getId(), p.getId());
			}
			Jurusan jur = safeJurusan(p);
			Matakuliah mk = safeMatakuliah(p);
			String dosenLabel = d == null ? "Tanpa Dosen" : safe(d.getNama());
			String prodiLabel = jur == null ? "Tanpa Prodi" : safe(jur.getNama());
			String perkuliahanLabel = p == null ? "Tanpa Perkuliahan" : safe(p.infoSimple());
			if (perkuliahanLabel.length() == 0 && mk != null) {
				perkuliahanLabel = safe(mk.getKode()) + " - " + safe(mk.getNama());
			}
			data.formRows.add(new String[] { safeDate(h.getTanggal_dirubah()), dosenLabel, prodiLabel, perkuliahanLabel });
			Accumulator dosenAcc = (Accumulator) data.dosenAcc.get(dosenLabel);
			if (dosenAcc == null) {
				dosenAcc = new Accumulator(dosenLabel);
				data.dosenAcc.put(dosenLabel, dosenAcc);
			}
			Accumulator prodiAcc = (Accumulator) data.prodiAcc.get(prodiLabel);
			if (prodiAcc == null) {
				prodiAcc = new Accumulator(prodiLabel);
				data.prodiAcc.put(prodiLabel, prodiAcc);
			}
			Accumulator perkuliahanAcc = (Accumulator) data.perkuliahanAcc.get(perkuliahanLabel);
			if (perkuliahanAcc == null) {
				perkuliahanAcc = new Accumulator(perkuliahanLabel);
				data.perkuliahanAcc.put(perkuliahanLabel, perkuliahanAcc);
			}

			List values = h.ambilValue();
			for (int j = 0; j < values.size(); j++) {
				Object[] arr = toArray(values.get(j));
				Long checklistId = longObject(arr.length > 0 ? arr[0] : null);
				Integer nilai = integerValue(arr.length > 1 ? arr[1] : null);
				String ket = arr.length > 2 && arr[2] != null ? String.valueOf(arr[2]) : "";
				if (nilai == null || nilai.intValue() <= 0) {
					continue;
				}
				QuestionInfo question = (QuestionInfo) questionMap.get(checklistId);
				if (question == null && filter.onlyActive) {
					continue;
				}
				if (question == null) {
					question = new QuestionInfo(checklistId, "Pertanyaan ID " + checklistId, "Tanpa Kelompok", 1.0);
				}
				data.totalNilai++;
				data.totalSkor += nilai.doubleValue();
				data.totalSkorBerbobot += nilai.doubleValue() * question.bobot;
				data.totalBobot += question.bobot;
				addNilaiDistribution(data.nilaiCount, nilai);
				dosenAcc.add(nilai.doubleValue());
				prodiAcc.add(nilai.doubleValue());
				perkuliahanAcc.add(nilai.doubleValue());

				Accumulator groupAcc = (Accumulator) data.groupAcc.get(question.groupName);
				if (groupAcc == null) {
					groupAcc = new Accumulator(question.groupName);
					data.groupAcc.put(question.groupName, groupAcc);
				}
				groupAcc.add(nilai.doubleValue());

				Accumulator qAcc = (Accumulator) data.questionAcc.get(question.label);
				if (qAcc == null) {
					qAcc = new Accumulator(question.label);
					data.questionAcc.put(question.label, qAcc);
				}
				qAcc.add(nilai.doubleValue());
				String[] detailRow = new String[] { safeDate(h.getTanggal_dirubah()), dosenLabel, prodiLabel, perkuliahanLabel,
						question.label, String.valueOf(nilai), ket == null ? "" : ket.trim() };
				data.detailRows.add(detailRow);
				dosenAcc.details.add(detailRow);
				prodiAcc.details.add(detailRow);
				perkuliahanAcc.details.add(detailRow);
				groupAcc.details.add(detailRow);
				qAcc.details.add(detailRow);
				List nilaiDetail = (List) data.nilaiDetails.get(nilai);
				if (nilaiDetail == null) {
					nilaiDetail = new ArrayList();
					data.nilaiDetails.put(nilai, nilaiDetail);
				}
				nilaiDetail.add(detailRow);

				if (ket != null && ket.trim().length() > 0) {
					data.masukanRows.add(new String[] { safeDate(h.getTanggal_dirubah()), dosenLabel, prodiLabel,
							question.label, ket.trim() });
				}
			}

			String masukan = h.getMasukan();
			if (masukan != null && masukan.trim().length() > 0) {
				data.masukanRows.add(new String[] { safeDate(h.getTanggal_dirubah()), dosenLabel, prodiLabel, "Masukan Umum",
						masukan.trim() });
			}
		}

		data.totalPertanyaan = questionMap.size();
		data.rataRata = data.totalNilai <= 0 ? 0.0 : data.totalSkor / data.totalNilai;
		data.rataRataBerbobot = data.totalBobot <= 0 ? 0.0 : data.totalSkorBerbobot / data.totalBobot;
		data.groupRows = topRows(data.groupAcc, 20, true);
		data.dosenRows = topRows(data.dosenAcc, 20, true);
		data.dosenRowsBottom = topRows(data.dosenAcc, 10, false);
		data.perkuliahanRows = topRows(data.perkuliahanAcc, 20, true);
		data.prodiRows = topRows(data.prodiAcc, 20, true);
		data.questionLowRows = topRows(data.questionAcc, 10, false);
		return data;
	}

	@SuppressWarnings("rawtypes")
	private List listJawaban(Session session, Filter filter) {
		StringBuffer hql = new StringBuffer();
		hql.append("select distinct h from ChecklistBaruPenilaianDosenOlehMahasiswa h ");
		hql.append("left join fetch h.mahasiswa m ");
		hql.append("left join fetch h.dosen d ");
		hql.append("left join fetch h.perkuliahan p ");
		hql.append("left join fetch p.jurusan j ");
		hql.append("left join fetch p.matakuliah mk ");
		hql.append("where 1=1 ");
		if (!isSemua(filter.tahunAkademik)) {
			hql.append("and p.tahunAjaran = :tahunAkademik ");
		}
		if (!isSemua(filter.semester)) {
			hql.append("and p.ganjilGenap = :semester ");
		}
		if (filter.fakultas != null) {
			hql.append("and j.fakultas = :fakultas ");
		}
		if (filter.jurusan != null) {
			hql.append("and j = :jurusan ");
		}
		if (!isSemua(filter.program)) {
			hql.append("and p.program = :program ");
		}
		if (filter.masaPerkuliahan != null) {
			hql.append("and p.masaPerkuliahan = :masaPerkuliahan ");
		}
		if (filter.dosen != null) {
			hql.append("and h.dosen = :dosen ");
		}
		hql.append("order by h.id desc");

		Query query = session.createQuery(hql.toString());
		if (!isSemua(filter.tahunAkademik)) {
			query.setParameter("tahunAkademik", filter.tahunAkademik);
		}
		if (!isSemua(filter.semester)) {
			query.setParameter("semester", filter.semester);
		}
		if (filter.fakultas != null) {
			query.setParameter("fakultas", filter.fakultas);
		}
		if (filter.jurusan != null) {
			query.setParameter("jurusan", filter.jurusan);
		}
		if (!isSemua(filter.program)) {
			query.setParameter("program", filter.program);
		}
		if (filter.masaPerkuliahan != null) {
			query.setParameter("masaPerkuliahan", filter.masaPerkuliahan);
		}
		if (filter.dosen != null) {
			query.setParameter("dosen", filter.dosen);
		}
		query.setMaxResults(readIntKonfigurasi("maksimal_data_dasbor_angket_dosen", 20000));
		return query.list();
	}

	@SuppressWarnings("rawtypes")
	private Map loadQuestionMap(Session session, boolean onlyActive) {
		Map map = new HashMap();
		String hql = "select c from ChecklistPenilaianDosen c left join fetch c.grupChecklistPenilaianDosen g where 1=1";
		if (onlyActive) {
			hql += " and (c.aktif is null or c.aktif = true) and (g.id is null or g.aktif is null or g.aktif = true)";
		}
		List rows = session.createQuery(hql).list();
		for (int i = 0; i < rows.size(); i++) {
			ChecklistPenilaianDosen c = (ChecklistPenilaianDosen) rows.get(i);
			if (c == null || c.getId() == null) {
				continue;
			}
			GrupChecklistPenilaianDosen g = c.getGrupChecklistPenilaianDosen();
			String groupName = g == null ? "Tanpa Kelompok" : safe(g.getIsi());
			map.put(c.getId(), new QuestionInfo(c.getId(), safe(c.getIsi()), groupName, c.getBobot().doubleValue()));
		}
		return map;
	}

	private void renderCards(Div parent, DashboardData data) {
		Div cards = new Div();
		cards.setStyle("display:flex;flex-wrap:wrap;gap:10px;margin-bottom:12px;");
		cards.setParent(parent);
		card(cards, "Angket Terisi", format(data.totalAngket), "Jumlah formulir/jawaban angket dosen", data.formRows,
				new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan" });
		card(cards, "Mahasiswa Mengisi", format(data.mahasiswaUnik.size()), "Responden unik",
				popupRows(new Object[] { "Mahasiswa Mengisi", format(data.mahasiswaUnik.size()) }), new String[] { "Data", "Nilai" });
		card(cards, "Dosen Dinilai", format(data.dosenUnik.size()), "Dosen unik dalam filter", data.dosenRows,
				new String[] { "Dosen", "Jumlah Nilai", "Rata-rata" });
		card(cards, "Perkuliahan", format(data.perkuliahanUnik.size()), "Kelas/perkuliahan terkait", data.perkuliahanRows,
				new String[] { "Perkuliahan", "Jumlah Nilai", "Rata-rata" });
		card(cards, "Pertanyaan Aktif", format(data.totalPertanyaan), "Butir pertanyaan yang dihitung", data.questionLowRows,
				new String[] { "Pertanyaan", "Jumlah Nilai", "Rata-rata" });
		card(cards, "Rata-rata", formatDecimal(data.rataRata), "Rerata nilai 1-5", data.detailRows,
				new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" });
		card(cards, "Rata-rata Berbobot", formatDecimal(data.rataRataBerbobot), "Memperhitungkan bobot pertanyaan", data.detailRows,
				new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" });
	}

	private void card(Div parent, final String title, String value, String subtitle, final List rowsData, final String[] headers) {
		Div card = new Div();
		card.setStyle("min-width:150px;flex:1;padding:13px;border:1px solid #dbeafe;border-radius:10px;"
				+ "background:linear-gradient(135deg,#f8fbff,#eef5ff);box-shadow:0 1px 3px rgba(0,0,0,.06);cursor:pointer;");
		card.setTooltiptext("Klik untuk melihat detail " + title);
		card.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDataPopup(title, headers, rowsData);
			}
		});
		card.setParent(parent);
		new Html("<div style='font-size:12px;color:#475569;'>" + html(title) + "</div>"
				+ "<div style='font-size:23px;font-weight:bold;color:#163d7a;margin-top:3px;'>" + html(value) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:3px;'>" + html(subtitle) + "</div>").setParent(card);
	}


	private void renderCssVisualizations(Div parent, DashboardData data) {
		Div charts = new Div();
		charts.setStyle("display:flex;flex-wrap:wrap;gap:12px;margin-bottom:12px;");
		charts.setParent(parent);
		renderCssDistributionVisual(charts, data);
		renderCssTrendVisual(charts, data);
		renderCssSpiderVisual(charts, data);
	}

	private void renderCssDistributionVisual(Div parent, DashboardData data) {
		Div box = visualBox(parent, "Grafik Distribusi Nilai", "Bar CSS jumlah jawaban nilai 1 sampai 5.");
		long max = 1L;
		for (int i = 1; i <= 5; i++) {
			Long value = (Long) data.nilaiCount.get(Integer.valueOf(i));
			if (value != null && value.longValue() > max) {
				max = value.longValue();
			}
		}
		for (int i = 1; i <= 5; i++) {
			long jumlah = data.nilaiCount.get(Integer.valueOf(i)) == null ? 0L : ((Long) data.nilaiCount.get(Integer.valueOf(i))).longValue();
			int percent = clampPercent(max <= 0 ? 0D : jumlah * 100.0D / max);
			List detail = (List) data.nilaiDetails.get(Integer.valueOf(i));
			if (detail == null) {
				detail = popupRows(new Object[] { "Nilai " + i, format(jumlah), String.valueOf(percent) + "%" });
			}
			appendCssBar(box, "Nilai " + i, jumlah, percent, "Detail Grafik Nilai " + i,
					new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail);
		}
	}

	private void renderCssTrendVisual(Div parent, DashboardData data) {
		Div box = visualBox(parent, "Trend Rata-rata Dosen", "Trend CSS dari rata-rata dosen tertinggi pada filter aktif.");
		int added = 0;
		for (int i = 0; data.dosenRows != null && i < data.dosenRows.size() && added < 8; i++) {
			Accumulator a = (Accumulator) data.dosenRows.get(i);
			int percent = clampPercent(a.average() * 20.0D);
			List detail = a.details == null || a.details.isEmpty() ? popupRows(new Object[] { a.name, format(a.count), formatDecimal(a.average()) }) : a.details;
			appendCssBar(box, a.name, a.average(), percent, "Detail Trend " + a.name,
					new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail);
			added++;
		}
		if (added == 0) {
			new Html("<div style='font-size:12px;color:#64748b;'>Belum ada data trend dosen pada filter ini.</div>").setParent(box);
		}
	}

	private void renderCssSpiderVisual(Div parent, DashboardData data) {
		Div box = visualBox(parent, "Spider Web Aspek Penilaian", "Radar/spider web CSS berdasarkan rata-rata kelompok/aspek.");
		double total = 0D;
		int count = 0;
		StringBuffer labels = new StringBuffer();
		for (int i = 0; data.groupRows != null && i < data.groupRows.size() && count < 5; i++) {
			Accumulator a = (Accumulator) data.groupRows.get(i);
			total += a.average();
			count++;
			labels.append("<div style='display:flex;justify-content:space-between;gap:8px;border-bottom:1px dashed #e5e7eb;padding:3px 0;'>")
					.append("<span>").append(html(limit(a.name, 34))).append("</span>")
					.append("<b>").append(html(formatDecimal(a.average()))).append("</b></div>");
		}
		double avg = count <= 0 ? 0D : total / count;
		int radar = clampPercent(avg * 20.0D);
		new Html("<div style='display:flex;align-items:center;gap:12px;'>"
				+ "<div style='position:relative;width:150px;height:150px;border-radius:50%;"
				+ "background:radial-gradient(circle,transparent 0 22%,#e5e7eb 23% 24%,transparent 25% 45%,#e5e7eb 46% 47%,transparent 48% 68%,#e5e7eb 69% 70%,transparent 71% 88%,#bfdbfe 89% 90%),"
				+ "conic-gradient(#7c3aed 0 " + radar + "%,#ede9fe " + radar + "% 100%);border:1px solid #ddd6fe;'>"
				+ "<div style='position:absolute;left:31px;top:52px;width:88px;text-align:center;background:rgba(255,255,255,.9);border-radius:12px;padding:6px 0;'>"
				+ "<div style='font-size:11px;color:#64748b;'>Rata-rata</div><div style='font-size:20px;font-weight:bold;color:#6d28d9;'>" + html(formatDecimal(avg)) + "</div></div></div>"
				+ "<div style='flex:1;font-size:12px;color:#334155;'>" + labels.toString() + "</div></div>").setParent(box);
	}

	private Div visualBox(Div parent, String title, String subtitle) {
		Div div = new Div();
		div.setStyle("flex:1;min-width:280px;background:#ffffff;border:1px solid #dbeafe;border-radius:12px;padding:12px;box-shadow:0 2px 8px rgba(15,23,42,.06);box-sizing:border-box;");
		div.setParent(parent);
		new Html("<div style='font-weight:bold;color:#163d7a;margin-bottom:2px;'>" + html(title) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>" + html(subtitle) + "</div>").setParent(div);
		return div;
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
		String nilai = Math.abs(value - Math.round(value)) < 0.0001D ? format(Math.round(value)) : formatDecimal(value);
		new Html("<div style='display:flex;justify-content:space-between;font-size:12px;color:#334155;margin-bottom:3px;'>"
				+ "<span>" + html(limit(label, 38)) + "</span><b>" + html(nilai) + "</b></div>"
				+ "<div style='height:10px;background:#eef2ff;border-radius:999px;overflow:hidden;'>"
				+ "<div style='height:10px;width:" + percent + "%;background:linear-gradient(90deg,#2563eb,#7c3aed);border-radius:999px;'></div></div>").setParent(row);
	}

	private int clampPercent(double value) {
		if (value < 0D) {
			return 0;
		}
		if (value > 100D) {
			return 100;
		}
		return (int) Math.round(value);
	}

	private void renderDistribution(Div parent, DashboardData data) {
		MyGrid grid = standardGrid(section(parent, "Distribusi Nilai"), new String[] { "Nilai", "Jumlah", "Persentase Relatif" }, new String[] { "35%", "30%", "35%" });
		Rows body = new Rows();
		body.setParent(grid);
		long max = 1;
		for (int i = 1; i <= 5; i++) {
			Long val = (Long) data.nilaiCount.get(Integer.valueOf(i));
			if (val != null && val.longValue() > max) {
				max = val.longValue();
			}
		}
		for (int i = 1; i <= 5; i++) {
			long jumlah = data.nilaiCount.get(Integer.valueOf(i)) == null ? 0 : ((Long) data.nilaiCount.get(Integer.valueOf(i))).longValue();
			int pct = (int) Math.round((jumlah * 100.0) / max);
			List detail = (List) data.nilaiDetails.get(Integer.valueOf(i));
			if (detail == null) {
				detail = popupRows(new Object[] { "Nilai " + i, format(jumlah), String.valueOf(pct) + "%" });
			}
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(body);
			clickableLabel("Nilai " + i, "Detail Nilai " + i, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(format(jumlah), "Detail Nilai " + i, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(String.valueOf(pct) + "%", "Detail Nilai " + i, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
		}
	}

	private void renderGroupSummary(Div parent, List rows) {
		MyGrid grid = standardGrid(section(parent, "Rata-rata per Kelompok/Aspek"), new String[] { "Kelompok/Aspek", "Jumlah Nilai", "Rata-rata" }, new String[] { "60%", "20%", "20%" });
		Rows body = new Rows();
		body.setParent(grid);
		for (int i = 0; i < rows.size(); i++) {
			Accumulator a = (Accumulator) rows.get(i);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(body);
			List detail = a.details == null || a.details.isEmpty() ? popupRows(new Object[] { a.name, format(a.count), formatDecimal(a.average()) }) : a.details;
			clickableLabel(a.name, "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(format(a.count), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(formatDecimal(a.average()), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
		}
		if (rows.isEmpty()) {
			emptyRow(body, 3, "Belum ada data kelompok/aspek untuk filter ini.");
		}
	}

	private void renderDosenSummary(Div parent, List rows, boolean terbaik) {
		String title = terbaik ? "Dosen dengan Rata-rata Tertinggi" : "Dosen yang Perlu Perhatian";
		MyGrid grid = standardGrid(section(parent, title), new String[] { "Dosen", "Jumlah Nilai", "Rata-rata" }, new String[] { "60%", "20%", "20%" });
		Rows body = new Rows();
		body.setParent(grid);
		for (int i = 0; i < rows.size(); i++) {
			Accumulator a = (Accumulator) rows.get(i);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(body);
			if (!terbaik && a.average() < 3.0) {
				row.setStyle("background:#fff1f2;color:#991b1b;");
			}
			List detail = a.details == null || a.details.isEmpty() ? popupRows(new Object[] { a.name, format(a.count), formatDecimal(a.average()) }) : a.details;
			clickableLabel(a.name, "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(format(a.count), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(formatDecimal(a.average()), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
		}
		if (rows.isEmpty()) {
			emptyRow(body, 3, "Belum ada data dosen untuk filter ini.");
		}
	}

	private void renderPerkuliahanSummary(Div parent, List rows) {
		MyGrid grid = standardGrid(section(parent, "Rata-rata per Perkuliahan/Mata Kuliah"), new String[] { "Perkuliahan", "Jumlah Nilai", "Rata-rata" }, new String[] { "65%", "15%", "20%" });
		Rows body = new Rows();
		body.setParent(grid);
		for (int i = 0; i < rows.size(); i++) {
			Accumulator a = (Accumulator) rows.get(i);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(body);
			List detail = a.details == null || a.details.isEmpty() ? popupRows(new Object[] { a.name, format(a.count), formatDecimal(a.average()) }) : a.details;
			clickableLabel(a.name, "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(format(a.count), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(formatDecimal(a.average()), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
		}
		if (rows.isEmpty()) {
			emptyRow(body, 3, "Belum ada data perkuliahan untuk filter ini.");
		}
	}

	private void renderProdiSummary(Div parent, List rows) {
		MyGrid grid = standardGrid(section(parent, "Rata-rata per Prodi"), new String[] { "Prodi", "Jumlah Nilai", "Rata-rata" }, new String[] { "65%", "15%", "20%" });
		Rows body = new Rows();
		body.setParent(grid);
		for (int i = 0; i < rows.size(); i++) {
			Accumulator a = (Accumulator) rows.get(i);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(body);
			List detail = a.details == null || a.details.isEmpty() ? popupRows(new Object[] { a.name, format(a.count), formatDecimal(a.average()) }) : a.details;
			clickableLabel(a.name, "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(format(a.count), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(formatDecimal(a.average()), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
		}
		if (rows.isEmpty()) {
			emptyRow(body, 3, "Belum ada data prodi untuk filter ini.");
		}
	}

	private void renderQuestionLowSummary(Div parent, List rows) {
		MyGrid grid = standardGrid(section(parent, "Butir Pertanyaan dengan Rata-rata Terendah"), new String[] { "Pertanyaan", "Jumlah Nilai", "Rata-rata" }, new String[] { "70%", "15%", "15%" });
		Rows body = new Rows();
		body.setParent(grid);
		for (int i = 0; i < rows.size(); i++) {
			Accumulator a = (Accumulator) rows.get(i);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(body);
			if (a.average() < 3.0) {
				row.setStyle("background:#fff1f2;color:#991b1b;");
			}
			List detail = a.details == null || a.details.isEmpty() ? popupRows(new Object[] { a.name, format(a.count), formatDecimal(a.average()) }) : a.details;
			clickableLabel(a.name, "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(format(a.count), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
			clickableLabel(formatDecimal(a.average()), "Detail " + a.name, new String[] { "Tanggal", "Dosen", "Prodi", "Perkuliahan", "Pertanyaan", "Nilai", "Keterangan" }, detail).setParent(row);
		}
		if (rows.isEmpty()) {
			emptyRow(body, 3, "Belum ada data butir pertanyaan untuk filter ini.");
		}
	}

	private void renderMasukanTerbaru(Div parent, List rows) {
		MyGrid grid = standardGrid(section(parent, "Masukan/Keterangan Terbaru"), new String[] { "Tanggal", "Dosen", "Prodi", "Butir", "Masukan" }, new String[] { "13%", "18%", "16%", "23%", "30%" });
		Rows body = new Rows();
		body.setParent(grid);
		int max = Math.min(rows.size(), 20);
		for (int i = 0; i < max; i++) {
			String[] r = (String[]) rows.get(i);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(body);
			List detail = popupRows(r);
			clickableLabel(r[0], "Detail Masukan", new String[] { "Tanggal", "Dosen", "Prodi", "Butir", "Masukan" }, detail).setParent(row);
			clickableLabel(r[1], "Detail Masukan", new String[] { "Tanggal", "Dosen", "Prodi", "Butir", "Masukan" }, detail).setParent(row);
			clickableLabel(r[2], "Detail Masukan", new String[] { "Tanggal", "Dosen", "Prodi", "Butir", "Masukan" }, detail).setParent(row);
			clickableLabel(r[3], "Detail Masukan", new String[] { "Tanggal", "Dosen", "Prodi", "Butir", "Masukan" }, detail).setParent(row);
			clickableLabel(r[4], "Detail Masukan", new String[] { "Tanggal", "Dosen", "Prodi", "Butir", "Masukan" }, detail).setParent(row);
		}
		if (rows.isEmpty()) {
			emptyRow(body, 5, "Belum ada masukan/keterangan untuk filter ini.");
		}
	}

	private Div section(Div parent, String title) {
		Div div = new Div();
		div.setStyle("margin-top:12px;padding:12px;border:1px solid #e5e7eb;border-radius:10px;background:#ffffff;box-shadow:0 1px 3px rgba(0,0,0,.04);");
		div.setParent(parent);
		new Html("<div style='font-weight:bold;color:#163d7a;margin-bottom:4px;font-size:14px;'>" + html(title) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;line-height:1.45;margin-bottom:8px;'>"
				+ html(sectionDescription(title)) + "</div>").setParent(div);
		return div;
	}

	private String sectionDescription(String title) {
		if (title == null) {
			return "Panel ini membantu membaca data angket secara ringkas agar pengguna tidak perlu menghitung manual dari tabel.";
		}
		String t = title.toLowerCase();
		if (t.indexOf("distribusi") >= 0) {
			return "Panel ini memperlihatkan jumlah jawaban pada setiap nilai. Pengguna dapat melihat apakah penilaian cenderung tinggi, sedang, atau rendah.";
		}
		if (t.indexOf("kelompok") >= 0 || t.indexOf("aspek") >= 0) {
			return "Panel ini merangkum nilai berdasarkan kelompok/aspek penilaian. Data ini membantu melihat bagian layanan pembelajaran yang paling kuat dan yang perlu diperbaiki.";
		}
		if (t.indexOf("tertinggi") >= 0) {
			return "Panel ini menampilkan dosen dengan rata-rata nilai tertinggi. Informasi ini dapat menjadi contoh praktik baik pembelajaran.";
		}
		if (t.indexOf("perhatian") >= 0 || t.indexOf("terendah") >= 0) {
			return "Panel ini menandai nilai yang perlu diperhatikan. Gunakan sebagai bahan evaluasi dan tindak lanjut pembinaan.";
		}
		if (t.indexOf("perkuliahan") >= 0 || t.indexOf("mata kuliah") >= 0) {
			return "Panel ini membandingkan hasil angket per perkuliahan atau mata kuliah. Pengguna dapat melihat kelas mana yang perlu dipertahankan atau dievaluasi.";
		}
		if (t.indexOf("prodi") >= 0) {
			return "Panel ini merangkum nilai per program studi. Data ini membantu prodi membaca kecenderungan penilaian mahasiswa.";
		}
		if (t.indexOf("masukan") >= 0 || t.indexOf("keterangan") >= 0) {
			return "Panel ini menampilkan komentar terbaru dari pengisi angket. Masukan ini dapat menjadi bahan tindak lanjut yang lebih spesifik.";
		}
		return "Panel ini membantu membaca data angket secara ringkas agar pengguna tidak perlu menghitung manual dari tabel.";
	}

	private MyGrid standardGrid(Div parent, String[] labels, String[] widths) {
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(parent);
		Columns columns = new Columns();
		columns.setParent(grid);
		for (int i = 0; i < labels.length; i++) {
			MyColumnConfig c = new MyColumnConfig();
			c.setLabel(labels[i]);
			if (widths != null && i < widths.length) {
				c.setWidth(widths[i]);
			}
			c.setParent(columns);
		}
		return grid;
	}

	private void emptyRow(Rows rows, int spans, String message) {
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.setSpans(String.valueOf(spans));
		new Label(message).setParent(row);
	}

	private List topRows(Map map, int max, boolean desc) {
		List list = new ArrayList(map.values());
		java.util.Collections.sort(list, new AccumulatorComparator(desc));
		if (list.size() > max) {
			return new ArrayList(list.subList(0, max));
		}
		return list;
	}

	private void addNilaiDistribution(Map map, Integer nilai) {
		Integer key = nilai;
		Long current = (Long) map.get(key);
		map.put(key, Long.valueOf((current == null ? 0 : current.longValue()) + 1));
	}

	private Dosen safeDosen(ChecklistBaruPenilaianDosenOlehMahasiswa h) {
		try {
			return h.getDosen();
		} catch (Exception e) {
			return null;
		}
	}

	private Perkuliahan safePerkuliahan(ChecklistBaruPenilaianDosenOlehMahasiswa h) {
		try {
			return h.getPerkuliahan();
		} catch (Exception e) {
			return null;
		}
	}

	private Jurusan safeJurusan(Perkuliahan p) {
		try {
			return p == null ? null : p.getJurusan();
		} catch (Exception e) {
			return null;
		}
	}

	private Matakuliah safeMatakuliah(Perkuliahan p) {
		try {
			return p == null ? null : p.getMatakuliah();
		} catch (Exception e) {
			return null;
		}
	}

	private Object[] toArray(Object obj) {
		if (obj instanceof Object[]) {
			return (Object[]) obj;
		}
		return new Object[] { obj };
	}

	private boolean isSemua(String value) {
		return value == null || value.trim().length() == 0 || "Semua".equalsIgnoreCase(value.trim());
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private String limit(String value, int max) {
		if (value == null) {
			return "";
		}
		String text = value.trim();
		if (max <= 0 || text.length() <= max) {
			return text;
		}
		if (max <= 3) {
			return text.substring(0, max);
		}
		return text.substring(0, max - 3) + "...";
	}

	private String html(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private String safeDate(Date date) {
		try {
			return date == null ? "" : Common.dateFormat5.get().format(date);
		} catch (Exception e) {
			return "";
		}
	}

	private String format(long value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatDecimal(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private Integer integerValue(Object value) {
		try {
			if (value instanceof Integer) {
				return (Integer) value;
			}
			if (value instanceof Number) {
				return Integer.valueOf(((Number) value).intValue());
			}
			return Integer.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return null;
		}
	}

	private Long longObject(Object value) {
		try {
			if (value instanceof Long) {
				return (Long) value;
			}
			if (value instanceof BigInteger) {
				return Long.valueOf(((BigInteger) value).longValue());
			}
			if (value instanceof BigDecimal) {
				return Long.valueOf(((BigDecimal) value).longValue());
			}
			if (value instanceof Number) {
				return Long.valueOf(((Number) value).longValue());
			}
			return Long.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return null;
		}
	}

	private int readIntKonfigurasi(String key, int defaultValue) {
		try {
			return Integer.parseInt(Common.getKonfigurasi(key, String.valueOf(defaultValue)).getNilai());
		} catch (Exception e) {
			return defaultValue;
		}
	}


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
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void closeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketDosenDashboardWindow.java:1200");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketDosenDashboardWindow.java:1204");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		try {
			session.close();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketDosenDashboardWindow.java:1208");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	/**
	 * Pembawa data/helper lokal milik {@link LaporanAngketDosenDashboardWindow} untuk filter. Tipe ini
	 * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * LaporanAngketDosenDashboardWindow}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String tahunAkademik}, {@code String
	 * semester}, {@code String program}, {@code Fakultas fakultas}, {@code Jurusan jurusan}, {@code
	 * MasaPerkuliahan masaPerkuliahan}, {@code Dosen dosen}, {@code boolean onlyActive}. Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see LaporanAngketDosenDashboardWindow
	 */
	private static class Filter implements Serializable {
		private static final long serialVersionUID = 1L;
		String tahunAkademik;
		String semester;
		String program;
		Fakultas fakultas;
		Jurusan jurusan;
		MasaPerkuliahan masaPerkuliahan;
		Dosen dosen;
		boolean onlyActive;
	}

	/**
	 * Pembawa data/helper lokal milik {@link LaporanAngketDosenDashboardWindow} untuk dashboard data. Tipe ini
	 * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * LaporanAngketDosenDashboardWindow}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long totalAngket}, {@code long
	 * totalNilai}, {@code int totalPertanyaan}, {@code double totalSkor}, {@code double totalSkorBerbobot}, {@code
	 * double totalBobot}, {@code double rataRata}, {@code double rataRataBerbobot}. Aturan bisnis bersama tetap
	 * berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see LaporanAngketDosenDashboardWindow
	 */
	private static class DashboardData implements Serializable {
		private static final long serialVersionUID = 1L;
		long totalAngket;
		long totalNilai;
		int totalPertanyaan;
		double totalSkor;
		double totalSkorBerbobot;
		double totalBobot;
		double rataRata;
		double rataRataBerbobot;
		Map nilaiCount = new TreeMap();
		Map nilaiDetails = new HashMap();
		Map mahasiswaUnik = new HashMap();
		Map dosenUnik = new HashMap();
		Map perkuliahanUnik = new HashMap();
		Map groupAcc = new LinkedHashMap();
		Map dosenAcc = new LinkedHashMap();
		Map prodiAcc = new LinkedHashMap();
		Map perkuliahanAcc = new LinkedHashMap();
		Map questionAcc = new LinkedHashMap();
		List groupRows = new ArrayList();
		List dosenRows = new ArrayList();
		List dosenRowsBottom = new ArrayList();
		List prodiRows = new ArrayList();
		List perkuliahanRows = new ArrayList();
		List questionLowRows = new ArrayList();
		List masukanRows = new ArrayList();
		List detailRows = new ArrayList();
		List formRows = new ArrayList();
	}

	/**
	 * Tipe implementasi bersarang {@link QuestionInfo} milik {@link LaporanAngketDosenDashboardWindow}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * LaporanAngketDosenDashboardWindow}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code String label},
	 * {@code String groupName}, {@code double bobot}. Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 *
	 * @see LaporanAngketDosenDashboardWindow
	 */
	private static class QuestionInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		Long id;
		String label;
		String groupName;
		double bobot;

		QuestionInfo(Long id, String label, String groupName, double bobot) {
			this.id = id;
			this.label = label == null || label.trim().length() == 0 ? "Pertanyaan ID " + id : label;
			this.groupName = groupName == null || groupName.trim().length() == 0 ? "Tanpa Kelompok" : groupName;
			this.bobot = bobot <= 0 ? 1.0 : bobot;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link Accumulator} milik {@link LaporanAngketDosenDashboardWindow}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * LaporanAngketDosenDashboardWindow}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String name}, {@code long count},
	 * {@code double sum}, {@code List details}; operasi lokal: {@code add()}, {@code average}(). Aturan bisnis
	 * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see LaporanAngketDosenDashboardWindow
	 */
	private static class Accumulator implements Serializable {
		private static final long serialVersionUID = 1L;
		String name;
		long count;
		double sum;
		List details = new ArrayList();

		Accumulator(String name) {
			this.name = name;
		}

		void add(double value) {
			count++;
			sum += value;
		}

		double average() {
			return count <= 0 ? 0.0 : sum / count;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link AccumulatorComparator} milik {@link LaporanAngketDosenDashboardWindow}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * LaporanAngketDosenDashboardWindow}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean desc}; operasi lokal: {@code
	 * compare}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see LaporanAngketDosenDashboardWindow
	 */
	private static class AccumulatorComparator implements java.util.Comparator, Serializable {
		private static final long serialVersionUID = 1L;
		boolean desc;

		AccumulatorComparator(boolean desc) {
			this.desc = desc;
		}

		@Override
		public int compare(Object o1, Object o2) {
			Accumulator a = (Accumulator) o1;
			Accumulator b = (Accumulator) o2;
			int avg = Double.compare(a.average(), b.average());
			if (desc) {
				avg = -avg;
			}
			if (avg != 0) {
				return avg;
			}
			return a.name.compareToIgnoreCase(b.name);
		}
	}

	/**
	 * Tipe implementasi bersarang {@link MahasiswaKey} milik {@link LaporanAngketDosenDashboardWindow}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * LaporanAngketDosenDashboardWindow}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long value}. Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see LaporanAngketDosenDashboardWindow
	 */
	private static class MahasiswaKey implements Serializable {
		private static final long serialVersionUID = 1L;
		Long value;

		MahasiswaKey(ChecklistBaruPenilaianDosenOlehMahasiswa h) {
			try {
				if (h != null && h.getMahasiswa() != null) {
					value = h.getMahasiswa().getId();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketDosenDashboardWindow.java:1323");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}
	}
}
