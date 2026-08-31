package ais.action.report.lkp;
import ais.common.PesanFormalHelper;
 
import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.maintenance.MainAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan realisasi lkp detail window. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahunAkademik}, {@code
 * Combobox semesterAbsensi}, {@code AmbilDataPegawaiBanbox pegawai}, {@code AmbilDataSatuanKerjaBanbox
 * searchparent}, {@code SatuanKerjaTreeModel satuanKerjaTreeModel}, {@code Toolbar toolbar}, {@code Center
 * center}, {@code org.zkoss.zul.Div resultContainer}; inisialisasi/lifecycle ({@code init()});
 * pembacaan/pencarian ({@code loadDashboardData()}, {@code getPegawaiFilterLabel()}, {@code
 * getSatuanKerjaFilterLabel()}, {@code buildTargetRealisasiChart()}); validasi/perhitungan ({@code
 * bolehMelihatDataPegawaiLain()}); pelaporan/ekspor ({@code renderDashboardPreview()}, {@code
 * renderDashboardHeader()}, {@code renderSummaryCards()}, {@code renderCharts()}, {@code renderInsight()},
 * {@code renderDetailTable()}); operasi domain lain ({@code generateParameter()}, {@code
 * onLaporanAngketDosenPerDosen()}, {@code buildSqlLkpPegawai()}, {@code resolvePegawaiFilterId()}, {@code
 * buildSatuanKerjaSqlRestriction()}, {@code appendMetricCard()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRealisasiLkpDetailWindow extends MyWindow { 

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private AmbilDataPegawaiBanbox pegawai;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Toolbar toolbar;
	private Center center;
	private org.zkoss.zul.Div resultContainer;
	private MyIntbox mulai;
	private MyIntbox banyak;

	public LaporanRealisasiLkpDetailWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Realisasi Lkp Detail Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRealisasiLkpDetailWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() throws Exception {

		final EventListener dashboardListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				renderDashboardPreview();
			}
		};

		final EventListener laporanAsliListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanAngketDosenPerDosen(event);
			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setStyle("width:100%; height:100%; min-height:700px; background:#f6f8fb; overflow:hidden;");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				borderlayout.setStyle("width:100%; height:" + (desktopHeight * 0.9)
						+ "px; min-height:700px; background:#f6f8fb; overflow:hidden;");
			}
		}

		/*
		 * Semua komponen sengaja diletakkan di dalam Center.
		 * Tidak memakai West/North lagi agar tinggi halaman mengikuti satu area scroll utama
		 * dan dashboard tidak terpotong oleh pembagian region Borderlayout.
		 */
		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setAutoscroll(true);
		center.setHflex("1");
		center.setVflex("1");
		center.setStyle("background:#f6f8fb; overflow:auto; padding:0;");

		org.zkoss.zul.Div pageScroll = new org.zkoss.zul.Div();
		pageScroll.setWidth("100%");
		pageScroll.setHeight("100%");
		pageScroll.setStyle("width:100%; height:100%; max-width:100%; overflow:auto; box-sizing:border-box; background:#f6f8fb;");
		pageScroll.setParent(center);

		org.zkoss.zul.Vbox pageWrapper = new org.zkoss.zul.Vbox();
		pageWrapper.setWidth("100%");
		pageWrapper.setSpacing("12px");
		pageWrapper.setStyle("width:100%; min-width:0; max-width:100%; box-sizing:border-box; padding:12px; background:#f6f8fb;");
		pageWrapper.setParent(pageScroll);

		org.zkoss.zul.Div parameterPanel = new org.zkoss.zul.Div();
		parameterPanel.setWidth("100%");
		parameterPanel.setStyle("width:100%; max-width:100%; box-sizing:border-box; background:#ffffff; border:1px solid #e9ecef; "
				+ "border-radius:18px; overflow:hidden; box-shadow:0 4px 18px rgba(0,0,0,.05);");
		parameterPanel.setParent(pageWrapper);

		org.zkoss.zul.Div toolbarWrapper = new org.zkoss.zul.Div();
		toolbarWrapper.setWidth("100%");
		toolbarWrapper.setStyle("width:100%; box-sizing:border-box; padding:6px 10px 4px 10px; text-align:right; border-bottom:1px solid #eef1f5; overflow:auto;");
		toolbarWrapper.setParent(parameterPanel);
		toolbarWrapper.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "lkp_pegawai_detail", null, laporanAsliListener));

		Html menuHeader = new Html("<div style='padding:12px 14px 6px 14px;'>"
				+ "<div style='background:linear-gradient(135deg,#0d6efd,#20c997); color:white; border-radius:16px; padding:14px; box-shadow:0 6px 18px rgba(13,110,253,.18);'>"
				+ "<div style='font-size:12px; opacity:.85; text-transform:uppercase; letter-spacing:.4px;'>Dashboard LKP</div>"
				+ "<div style='font-size:18px; font-weight:900; margin-top:3px;'>Rincian Capaian LKP</div>"
				+ "<div style='font-size:12px; opacity:.9; margin-top:6px;'>Preview analitik mengikuti data laporan resmi lkp_pegawai_detail.jrxml.</div>"
				+ "</div></div>");
		menuHeader.setParent(parameterPanel);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(parameterPanel);
		grid.setHeight("100%");
		grid.setStyle("border:0; padding:0 12px 12px 12px; box-sizing:border-box;");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("18%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i);
			semesterAbsensi.appendChild(comboitem);
		}

		Common.selectComboItem(semesterAbsensi, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH));
		semesterAbsensi.setReadonly(true);

		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = tahun - 10; i < tahun + 2; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahunAkademik.appendChild(comboitem);
		}

		Common.selectComboItem(tahunAkademik, tahun);
		tahunAkademik.setReadonly(true);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("98%");
		tahunAkademik.setReadonly(true);
		tahunAkademik.addEventListener("onChange", dashboardListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bulan"));
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("98%");
		semesterAbsensi.setReadonly(true);
		semesterAbsensi.addEventListener("onChange", dashboardListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox(true));
		pegawai.setWidth("98%");
		pegawai.setReadonly(true);

		if (pegawai.getAttribute("pegawai") == null) {
			Common.initKeterangan(rows,
					"Jika pegawai tidak dipilih, dashboard mengikuti hak akses: admin/pimpinan melihat semua pegawai, user pegawai hanya melihat data sendiri.");
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchparent = new AmbilDataSatuanKerjaBanbox());
		searchparent.setWidth("98%");
		searchparent.setEventListener(dashboardListener);
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new MyIntbox(0));
		mulai.setWidth("98%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Banyak"));
		row.appendChild(banyak = new MyIntbox(10));
		banyak.setWidth("98%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aksi"));
		org.zkoss.zul.Hbox actionBox = new org.zkoss.zul.Hbox();
		actionBox.setWidth("100%");
		actionBox.setStyle("gap:8px; flex-wrap:wrap; align-items:center;");
		row.appendChild(actionBox);

		MyButtonConfig tombolDashboard = new MyButtonConfig("Tampilkan Dashboard");
		tombolDashboard.setStyle("font-weight:bold; background:#0d6efd; color:#ffffff; border-radius:8px; padding:6px 12px;");
		tombolDashboard.addEventListener("onClick", dashboardListener);
		actionBox.appendChild(tombolDashboard);

		MyButtonConfig tombolLaporanAsli = new MyButtonConfig("Download / Lihat Laporan Asli");
		tombolLaporanAsli.setStyle("font-weight:bold; background:#198754; color:#ffffff; border-radius:8px; padding:6px 12px;");
		tombolLaporanAsli.addEventListener("onClick", laporanAsliListener);
		actionBox.appendChild(tombolLaporanAsli);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Html info = new Html("<div style='margin:8px 0 4px 0; padding:10px; border-radius:12px; background:#f8f9fa; color:#6c757d; font-size:12px; line-height:1.45;'>"
				+ "Semua parameter, toolbar laporan, dashboard, dan preview laporan berada dalam satu area <b>Center</b>. "
				+ "Karena itu scroll vertikal halaman menjadi satu jalur dan tidak terpotong oleh region West/North. "
				+ "Tombol laporan asli tetap membuka/export format resmi Jasper <b>lkp_pegawai_detail.jrxml</b>.</div>");
		row.appendChild(info);

		resultContainer = new org.zkoss.zul.Div();
		resultContainer.setWidth("100%");
		resultContainer.setStyle("width:100%; max-width:100%; min-height:360px; box-sizing:border-box; overflow:visible;");
		resultContainer.setParent(pageWrapper);

		Common.createDefaultTimer(dashboardListener);
	}

	@SuppressWarnings({ "rawtypes" })
	private Map generateParameter() throws Exception {

		Double prosentasi_nilai_skp_kuantitas = 70.0;
		Double prosentasi_nilai_skp_kualitas = 10.0;
		Double prosentasi_nilai_skp_waktu = 20.0;

		try {
			prosentasi_nilai_skp_kuantitas = Double
					.parseDouble(Common.getKonfigurasi("prosentasi_nilai_skp_kuantitas", "70").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/lkp/LaporanRealisasiLkpDetailWindow.java:299");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Realisasi Lkp Detail Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});

		}

		try {
			prosentasi_nilai_skp_kualitas = Double
					.parseDouble(Common.getKonfigurasi("prosentasi_nilai_skp_kualitas", "10").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/lkp/LaporanRealisasiLkpDetailWindow.java:306");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Realisasi Lkp Detail Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});

		}

		try {
			prosentasi_nilai_skp_waktu = Double
					.parseDouble(Common.getKonfigurasi("prosentasi_nilai_skp_waktu", "20").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/lkp/LaporanRealisasiLkpDetailWindow.java:313");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Realisasi Lkp Detail Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});

		}

		Pegawai myDosen = (Pegawai) pegawai.getAttribute("pegawai");
		Long effectiveDosen = resolvePegawaiFilterId();
		Map<String, Serializable> parameters = new HashMap<String, Serializable>();
		parameters.put("tahun", (Integer) tahunAkademik.getSelectedItem().getValue());
		parameters.put("bulan", (Integer) semesterAbsensi.getSelectedItem().getValue());
		parameters.put("bulan_str", Common.BULAN[(Integer) semesterAbsensi.getSelectedItem().getValue()]);
		parameters.put("dosen", effectiveDosen == null ? -1L : effectiveDosen);

		parameters.put("prosentasi_nilai_skp_kuantitas_double", prosentasi_nilai_skp_kuantitas);
		parameters.put("prosentasi_nilai_skp_kualitas_double", prosentasi_nilai_skp_kualitas);
		parameters.put("prosentasi_nilai_skp_waktu_double", prosentasi_nilai_skp_waktu);

		parameters.put("mulai", mulai.getValue() == null ? 0 : mulai.getValue());
		parameters.put("banyak", banyak.getValue() == null ? 5 : banyak.getValue());

		if (myDosen != null) {
			myDosen.putPhoto(parameters);
		}

		return parameters;

	}

	@SuppressWarnings({})
	public void onLaporanAngketDosenPerDosen(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "lkp_pegawai_detail",
						ais.ui.util.WaktuUtil.getDate(), toolbar);
				org.zkoss.zk.ui.Component target = resultContainer == null ? center : resultContainer;
				CommonReport.tampilkanReportPDF(target, file);
			}
		});
	}

	private void renderDashboardPreview() throws Exception {
		if (resultContainer == null) {
			return;
		}
		Common.clear(resultContainer);

		org.zkoss.zul.Div scrollContainer = new org.zkoss.zul.Div();
		scrollContainer.setWidth("100%");
		scrollContainer.setStyle("width:100%; max-width:100%; overflow:visible; box-sizing:border-box; background:#f6f8fb;");
		scrollContainer.setParent(resultContainer);

		org.zkoss.zul.Vbox wrapper = new org.zkoss.zul.Vbox();
		wrapper.setWidth("100%");
		wrapper.setSpacing("12px");
		wrapper.setStyle("width:100%; min-width:0; max-width:100%; box-sizing:border-box; padding:0; background:#f6f8fb; overflow:visible;");
		wrapper.setParent(scrollContainer);

		Html loading = new Html("<div style='padding:12px; text-align:center; color:#6c757d;'>"
				+ "<i class='fa fa-spinner fa-spin'></i> Mengambil data capaian LKP...</div>");
		loading.setParent(wrapper);

		DashboardData data = loadDashboardData();
		wrapper.removeChild(loading);

		renderDashboardHeader(wrapper, data);
		renderSummaryCards(wrapper, data);
		renderCharts(wrapper, data);
		renderInsight(wrapper, data);
		renderDetailTable(wrapper, data);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private DashboardData loadDashboardData() throws Exception {
		Map parameters = generateParameter();
		int tahun = ((Integer) parameters.get("tahun")).intValue();
		int bulan = ((Integer) parameters.get("bulan")).intValue();
		Long dosen = resolvePegawaiFilterId();
		String satuanKerjaSql = buildSatuanKerjaSqlRestriction(dosen);
		double persenKuantitas = toDouble(parameters.get("prosentasi_nilai_skp_kuantitas_double"));
		double persenKualitas = toDouble(parameters.get("prosentasi_nilai_skp_kualitas_double"));
		double persenWaktu = toDouble(parameters.get("prosentasi_nilai_skp_waktu_double"));

		Session session = HibernateUtil.currentSession();
		SQLQuery query = session.createSQLQuery(buildSqlLkpPegawai(satuanKerjaSql));
		query.setInteger("tahun", tahun);
		query.setInteger("bulan", bulan);
		query.setLong("dosen", dosen == null ? -1L : dosen.longValue());

		List<Object[]> rows = query.list();
		DashboardData data = new DashboardData();
		data.tahun = tahun;
		data.bulan = bulan;
		data.bulanStr = (String) parameters.get("bulan_str");
		data.persenKuantitas = persenKuantitas;
		data.persenKualitas = persenKualitas;
		data.persenWaktu = persenWaktu;
		data.rows = new ArrayList<LkpRow>();
		data.mapPegawai = new LinkedHashMap<Long, PegawaiSummary>();

		for (Object[] row : rows) {
			LkpRow r = new LkpRow(row, persenKuantitas, persenKualitas, persenWaktu);
			data.rows.add(r);

			Long key = r.pegawaiId == null ? Long.valueOf(-1L - data.mapPegawai.size()) : r.pegawaiId;
			PegawaiSummary summary = data.mapPegawai.get(key);
			if (summary == null) {
				summary = new PegawaiSummary();
				summary.pegawaiId = key;
				summary.nama = r.nama;
				summary.nip = r.nip;
				summary.satuanKerja = r.satuanKerja;
				summary.jabatanFungsional = r.jabatanFungsional;
				data.mapPegawai.put(key, summary);
			}
			summary.add(r);

			data.totalTargetKuantitas += r.targetKuantitas;
			data.totalTargetWaktu += r.targetWaktu;
			data.totalTargetBiaya += r.targetBiaya;
			data.totalRealisasiKuantitas += r.realisasiKuantitas;
			data.totalRealisasiWaktu += r.realisasiWaktu;
			data.totalRealisasiBiaya += r.realisasiBiaya;
			data.sumCapaianKuantitas += r.capaianKuantitas;
			data.sumCapaianKualitas += r.capaianKualitas;
			data.sumCapaianWaktu += r.capaianWaktu;
			data.sumNilaiSkp += r.nilaiSkp;
			if (r.nilaiSkp >= 100.0) {
				data.jumlahTercapai++;
			}
			if (r.nilaiSkp < 75.0) {
				data.jumlahPerluPerhatian++;
			}
		}

		for (PegawaiSummary summary : data.mapPegawai.values()) {
			summary.finish();
		}

		if (!data.rows.isEmpty()) {
			data.avgCapaianKuantitas = data.sumCapaianKuantitas / data.rows.size();
			data.avgCapaianKualitas = data.sumCapaianKualitas / data.rows.size();
			data.avgCapaianWaktu = data.sumCapaianWaktu / data.rows.size();
			data.avgNilaiSkp = data.sumNilaiSkp / data.rows.size();
		}
		return data;
	}

	private String buildSqlLkpPegawai(String satuanKerjaSql) {
		return "select "
				+ "a.bulan, a.tahun, a.pegawai, a.id as target, c.nama as nama, c.code as nip, "
				+ "d.nama as spesifikasi_jabatan, e.nama as golongan, c.jabatan, "
				+ "f.nama as jabatan_struktural, g.nama as jabatan_fungsional, h.nama as satuan_kerja, "
				+ "c.telp, c.email, b.nama as kegiatan, b.angkakredit, a.kuantitas, "
				+ "i.nama as satuan_kuantitas, a.kualitas, a.waktu, b.satuanwaktu, a.biaya, "
				+ "j.kuantitas as realisasi_kuantitas, a.kualitasrealisasi, "
				+ "case when j.waktu is null then 0 else j.waktu end as realisasi_waktu, "
				+ "case when j.biaya is null then 0 else j.biaya end as realisasi_biaya, "
				+ "k.nama as asesor, k.nip as asesor_nip, jj.keterangan as deskripsi_keterangan, "
				+ "jj.tanggalwaktu, jj.kuantitas as kuantitas_detail, jj.waktu as waktu_detail, jj.biaya as biaya_detail "
				+ "from target_kerja_pegawai a "
				+ "inner join kegiatan_tugas_jabatan b on (a.kegiatan_tugas_jabatan=b.id) "
				+ "inner join pegawai c on (a.pegawai = c.id) "
				+ "left join jabatan d on (d.id = c.spesifikasi_jabatan) "
				+ "left join employ.golongan e on (c.golongan_pegawai=e.id) "
				+ "left join employ.jabatan_struktural f on (f.id = c.jabatan_struktural) "
				+ "left join employ.jabatan_fungsional g on (g.id = c.jabatan_fungsional) "
				+ "left join rab.satuan_kerja h on (h.id = b.satuan_kerja) "
				+ "left join satuan_kegiatan_tugas_jabatan i on (i.id=b.satuan_kuantitas) "
				+ "left join realisasi_kerja_pegawai jj on (jj.target_kerja_pegawai=a.id) "
				+ "left join (select aa.target_kerja_pegawai, sum(aa.kuantitas) as kuantitas, "
				+ "sum(aa.waktu) as waktu, sum(aa.biaya) as biaya "
				+ "from realisasi_kerja_pegawai aa group by aa.target_kerja_pegawai) j on (a.id=j.target_kerja_pegawai) "
				+ "left join (select aa.pegawai, max(case when dd.nama is null then cc.usernama else dd.nama end) as nama, "
				+ "max(dd.code) as nip from asesor_pegawai aa "
				+ "inner join asesor bb on (aa.asesor = bb.id) "
				+ "inner join tbmuser cc on (bb.tbmuser=cc.userid) "
				+ "left join pegawai dd on (dd.id=cc.pegawai) group by aa.pegawai) k on (k.pegawai=a.pegawai) "
				+ "where a.tahun=:tahun and a.bulan=:bulan and (:dosen=-1 or a.pegawai=:dosen) "
				+ "and " + satuanKerjaSql + " "
				+ "order by c.id, a.id, jj.tanggalwaktu";
	}


	private Long resolvePegawaiFilterId() {
		try {
			Pegawai selected = pegawai == null ? null : (Pegawai) pegawai.getAttribute("pegawai");
			if (selected != null && selected.getId() != null) {
				return selected.getId();
			}
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.getPegawai() != null && !bolehMelihatDataPegawaiLain(tbmuser)) {
				return tbmuser.getPegawai().getId();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Realisasi Lkp Detail Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		return Long.valueOf(-1L);
	}

	private boolean bolehMelihatDataPegawaiLain(Tbmuser tbmuser) {
		try {
			return tbmuser == null || tbmuser.getPegawai() == null || (tbmuser.hakAkses() != null
					&& Boolean.TRUE.equals(tbmuser.hakAkses().getMelihatDataPegawaiLain()));
		} catch (Exception e) {
			return false;
		}
	}

	private String buildSatuanKerjaSqlRestriction(Long effectiveDosen) throws Exception {
		if (effectiveDosen != null && effectiveDosen.longValue() != -1L) {
			return "true";
		}
		SatuanKerja parent = searchparent == null ? null : (SatuanKerja) searchparent.getAttribute("satuanKerja");
		if (parent == null) {
			return "true";
		}

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (satuanKerjas == null) {
			satuanKerjas = new java.util.HashSet<SatuanKerja>();
		}
		satuanKerjas.clear();
		satuanKerjas.add(parent);
		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}
		satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);

		String inSatker = "";
		for (SatuanKerja satuanKerja : satuanKerjas) {
			if (satuanKerja != null && satuanKerja.getId() != null) {
				inSatker += inSatker.isEmpty() ? satuanKerja.getId().toString() : "," + satuanKerja.getId();
			}
		}
		return inSatker.isEmpty() ? "true" : "c.satuan_kerja in (" + inSatker + ")";
	}

	private String getPegawaiFilterLabel() {
		try {
			Pegawai selected = pegawai == null ? null : (Pegawai) pegawai.getAttribute("pegawai");
			if (selected != null && selected.getNama() != null) {
				return selected.getNama();
			}
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.getPegawai() != null && !bolehMelihatDataPegawaiLain(tbmuser)) {
				return tbmuser.getPegawai().getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/lkp/LaporanRealisasiLkpDetailWindow.java:562");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Realisasi Lkp Detail Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		return "Semua Pegawai";
	}

	private String getSatuanKerjaFilterLabel() {
		try {
			Long effectiveDosen = resolvePegawaiFilterId();
			if (effectiveDosen != null && effectiveDosen.longValue() != -1L) {
				return "Tidak digunakan saat pegawai spesifik";
			}
			SatuanKerja parent = searchparent == null ? null : (SatuanKerja) searchparent.getAttribute("satuanKerja");
			return parent == null ? "Semua Satuan Kerja" : parent.getNama();
		} catch (Exception e) {
			return "Semua Satuan Kerja";
		}
	}

	private void renderDashboardHeader(org.zkoss.zul.Vbox wrapper, DashboardData data) {
		String pegawaiText = getPegawaiFilterLabel();
		String satuanKerjaText = getSatuanKerjaFilterLabel();

		String html = "<div style='width:100%; max-width:100%; box-sizing:border-box; background:linear-gradient(135deg,#0d6efd,#6610f2); color:#fff; border-radius:20px; "
				+ "padding:20px; box-shadow:0 10px 28px rgba(13,110,253,.20);'>"
				+ "<div style='display:flex; justify-content:space-between; align-items:flex-start; gap:16px; flex-wrap:wrap;'>"
				+ "<div>"
				+ "<div style='font-size:12px; opacity:.86; text-transform:uppercase; letter-spacing:.5px;'>Dashboard Rincian Capaian LKP</div>"
				+ "<div style='font-size:27px; font-weight:900; margin-top:4px;'>Rincian Target, Catatan Realisasi, dan Nilai SKP</div>"
				+ "<div style='font-size:13px; opacity:.92; margin-top:8px;'>Periode: " + escapeHtml(data.bulanStr)
				+ " " + data.tahun + " &nbsp; | &nbsp; Pegawai: " + escapeHtml(pegawaiText)
				+ " &nbsp; | &nbsp; Satuan Kerja: " + escapeHtml(satuanKerjaText)
				+ " &nbsp; | &nbsp; Bobot: Kuantitas " + formatAngka(data.persenKuantitas) + "% · Kualitas "
				+ formatAngka(data.persenKualitas) + "% · Waktu " + formatAngka(data.persenWaktu) + "%</div>"
				+ "</div>"
				+ "<div style='text-align:right; min-width:210px;'>"
				+ "<div style='font-size:12px; opacity:.85;'>Rata-rata Nilai Capaian SKP</div>"
				+ "<div style='font-size:34px; font-weight:900; line-height:1.1;'>" + formatAngka(data.avgNilaiSkp) + "</div>"
				+ "<div style='font-size:12px; opacity:.86;'>" + escapeHtml(kategoriNilai(data.avgNilaiSkp)) + "</div>"
				+ "</div>"
				+ "</div></div>";
		Html header = new Html(html);
		header.setParent(wrapper);
	}

	private void renderSummaryCards(org.zkoss.zul.Vbox wrapper, DashboardData data) {
		StringBuffer sb = new StringBuffer();
		sb.append("<div style='width:100%; max-width:100%; box-sizing:border-box; display:grid; grid-template-columns:repeat(auto-fit,minmax(165px,1fr)); gap:12px;'>");
		appendMetricCard(sb, "Pegawai", formatAngka(data.mapPegawai.size()), "pegawai pada filter", "#0d6efd");
		appendMetricCard(sb, "Target Kegiatan", formatAngka(data.rows.size()), "baris kegiatan jabatan", "#6f42c1");
		appendMetricCard(sb, "Target Kuantitas", formatAngka(data.totalTargetKuantitas), "akumulasi target", "#20c997");
		appendMetricCard(sb, "Realisasi Kuantitas", formatAngka(data.totalRealisasiKuantitas), "akumulasi realisasi", "#198754");
		appendMetricCard(sb, "Capaian Kuantitas", formatAngka(data.avgCapaianKuantitas) + "%", "rata-rata capaian", warnaPersen(data.avgCapaianKuantitas));
		appendMetricCard(sb, "Capaian Kualitas", formatAngka(data.avgCapaianKualitas) + "%", "rata-rata capaian", warnaPersen(data.avgCapaianKualitas));
		appendMetricCard(sb, "Capaian Waktu", formatAngka(data.avgCapaianWaktu) + "%", "rata-rata capaian", warnaPersen(data.avgCapaianWaktu));
		appendMetricCard(sb, "Perlu Perhatian", formatAngka(data.jumlahPerluPerhatian), "kegiatan nilai < 75", "#dc3545");
		sb.append("</div>");
		Html cards = new Html(sb.toString());
		cards.setParent(wrapper);
	}

	private void appendMetricCard(StringBuffer sb, String title, String value, String desc, String color) {
		sb.append("<div style='background:#fff; border:1px solid #e9ecef; border-radius:16px; padding:14px; "
				+ "box-shadow:0 4px 16px rgba(0,0,0,.045); position:relative; overflow:hidden;'>");
		sb.append("<div style='position:absolute; right:-18px; top:-18px; width:72px; height:72px; border-radius:50%; background:")
				.append(color).append("; opacity:.12;'></div>");
		sb.append("<div style='font-size:12px; color:#6c757d; font-weight:800; text-transform:uppercase;'>")
				.append(escapeHtml(title)).append("</div>");
		sb.append("<div style='font-size:25px; font-weight:900; color:").append(color)
				.append("; margin-top:4px;'>").append(escapeHtml(value)).append("</div>");
		sb.append("<div style='font-size:12px; color:#6c757d; margin-top:2px;'>").append(escapeHtml(desc))
				.append("</div>");
		sb.append("</div>");
	}

	private void renderCharts(org.zkoss.zul.Vbox wrapper, DashboardData data) {
		StringBuffer sb = new StringBuffer();
		sb.append("<div style='width:100%; max-width:100%; box-sizing:border-box; display:grid; grid-template-columns:repeat(auto-fit,minmax(310px,1fr)); gap:12px;'>");
		sb.append(buildPanelHtml("Grafik Capaian SKP", buildCapaianSkpChart(data),
				"Rata-rata capaian kuantitas, kualitas, dan waktu sesuai formula lkp_pegawai_detail.jrxml."));
		sb.append(buildPanelHtml("Top 10 Nilai Capaian Pegawai", buildTopPegawaiChart(data),
				"Pegawai dengan nilai capaian SKP tertinggi pada periode terpilih."));
		sb.append(buildPanelHtml("Target vs Realisasi", buildTargetRealisasiChart(data),
				"Perbandingan akumulasi target dan realisasi kuantitas, waktu, dan biaya."));
		sb.append(buildPanelHtml("Kegiatan Perlu Perhatian", buildKegiatanPerhatianChart(data),
				"Kegiatan dengan nilai capaian terendah untuk prioritas evaluasi."));
		sb.append("</div>");
		Html charts = new Html(sb.toString());
		charts.setParent(wrapper);
	}

	private String buildPanelHtml(String title, String body, String caption) {
		return "<div style='background:#fff; border:1px solid #e9ecef; border-radius:16px; padding:14px; "
				+ "box-shadow:0 4px 16px rgba(0,0,0,.045); overflow:hidden; box-sizing:border-box;'>"
				+ "<div style='font-size:16px; font-weight:900; color:#212529; margin-bottom:3px;'>" + escapeHtml(title)
				+ "</div>"
				+ "<div style='font-size:12px; color:#6c757d; margin-bottom:12px;'>" + escapeHtml(caption) + "</div>"
				+ body + "</div>";
	}

	private String buildCapaianSkpChart(DashboardData data) {
		StringBuffer sb = new StringBuffer();
		appendBar(sb, "Kuantitas", data.avgCapaianKuantitas, Math.max(100.0, maxCapaian(data)), "#20c997",
				formatAngka(data.avgCapaianKuantitas) + "% · bobot " + formatAngka(data.persenKuantitas) + "%");
		appendBar(sb, "Kualitas", data.avgCapaianKualitas, Math.max(100.0, maxCapaian(data)), "#0d6efd",
				formatAngka(data.avgCapaianKualitas) + "% · bobot " + formatAngka(data.persenKualitas) + "%");
		appendBar(sb, "Waktu", data.avgCapaianWaktu, Math.max(100.0, maxCapaian(data)), "#fd7e14",
				formatAngka(data.avgCapaianWaktu) + "% · bobot " + formatAngka(data.persenWaktu) + "%");
		appendBar(sb, "Nilai SKP", data.avgNilaiSkp, Math.max(100.0, maxCapaian(data)), warnaPersen(data.avgNilaiSkp),
				formatAngka(data.avgNilaiSkp) + " · " + kategoriNilai(data.avgNilaiSkp));
		return sb.toString();
	}

	private String buildTopPegawaiChart(DashboardData data) {
		List<PegawaiSummary> list = new ArrayList<PegawaiSummary>(data.mapPegawai.values());
		Collections.sort(list, new Comparator<PegawaiSummary>() {
			public int compare(PegawaiSummary a, PegawaiSummary b) {
				if (b.nilaiSkp > a.nilaiSkp)
					return 1;
				if (b.nilaiSkp < a.nilaiSkp)
					return -1;
				return a.nama.compareToIgnoreCase(b.nama);
			}
		});

		double max = 1.0;
		for (PegawaiSummary h : list) {
			if (h.nilaiSkp > max) {
				max = h.nilaiSkp;
			}
		}

		StringBuffer sb = new StringBuffer();
		int no = 0;
		for (PegawaiSummary h : list) {
			if (h.targetCount <= 0) {
				continue;
			}
			no++;
			appendRankBar(sb, no, h.nama, h.nilaiSkp, Math.max(100.0, max), warnaPersen(h.nilaiSkp),
					formatAngka(h.nilaiSkp) + " · " + h.targetCount + " kegiatan");
			if (no >= 10) {
				break;
			}
		}
		if (no == 0) {
			sb.append(emptyChartMessage("Belum ada data target/realisasi pada periode ini."));
		}
		return sb.toString();
	}

	private String buildTargetRealisasiChart(DashboardData data) {
		StringBuffer sb = new StringBuffer();
		double maxKuantitas = Math.max(1.0, Math.max(data.totalTargetKuantitas, data.totalRealisasiKuantitas));
		double maxWaktu = Math.max(1.0, Math.max(data.totalTargetWaktu, data.totalRealisasiWaktu));
		double maxBiaya = Math.max(1.0, Math.max(data.totalTargetBiaya, data.totalRealisasiBiaya));
		sb.append("<div style='display:grid; gap:12px;'>");
		sb.append(buildCompareBar("Kuantitas", data.totalTargetKuantitas, data.totalRealisasiKuantitas, maxKuantitas));
		sb.append(buildCompareBar("Waktu", data.totalTargetWaktu, data.totalRealisasiWaktu, maxWaktu));
		sb.append(buildCompareBar("Biaya", data.totalTargetBiaya, data.totalRealisasiBiaya, maxBiaya));
		sb.append("</div>");
		return sb.toString();
	}

	private String buildKegiatanPerhatianChart(DashboardData data) {
		List<LkpRow> list = new ArrayList<LkpRow>(data.rows);
		Collections.sort(list, new Comparator<LkpRow>() {
			public int compare(LkpRow a, LkpRow b) {
				if (a.nilaiSkp > b.nilaiSkp)
					return 1;
				if (a.nilaiSkp < b.nilaiSkp)
					return -1;
				return a.kegiatan.compareToIgnoreCase(b.kegiatan);
			}
		});
		StringBuffer sb = new StringBuffer();
		int no = 0;
		for (LkpRow r : list) {
			if (r.nilaiSkp >= 90.0) {
				continue;
			}
			no++;
			appendRankBar(sb, no, r.kegiatan, r.nilaiSkp, 100.0, warnaPersen(r.nilaiSkp),
					escapeHtml(r.nama) + " · nilai " + formatAngka(r.nilaiSkp));
			if (no >= 10) {
				break;
			}
		}
		if (no == 0) {
			sb.append(emptyChartMessage("Semua kegiatan berada pada kategori baik/sangat baik."));
		}
		return sb.toString();
	}

	private String buildCompareBar(String title, double target, double realisasi, double max) {
		double targetWidth = Math.max(2.0, Math.min(100.0, target * 100.0 / max));
		double realisasiWidth = Math.max(2.0, Math.min(100.0, realisasi * 100.0 / max));
		StringBuffer sb = new StringBuffer();
		sb.append("<div>");
		sb.append("<div style='display:flex; justify-content:space-between; font-size:12px; color:#495057; margin-bottom:4px;'>")
				.append("<b>").append(escapeHtml(title)).append("</b>")
				.append("<span>Target ").append(formatAngka(target)).append(" · Realisasi ").append(formatAngka(realisasi)).append("</span></div>");
		sb.append("<div style='height:10px; background:#f1f3f5; border-radius:999px; overflow:hidden; margin-bottom:3px;'>")
				.append("<div style='height:10px; width:").append(formatAngka(targetWidth)).append("%; background:#adb5bd;'></div></div>");
		sb.append("<div style='height:12px; background:#f1f3f5; border-radius:999px; overflow:hidden;'>")
				.append("<div style='height:12px; width:").append(formatAngka(realisasiWidth)).append("%; background:#0d6efd;'></div></div>");
		sb.append("</div>");
		return sb.toString();
	}

	private void appendBar(StringBuffer sb, String title, double value, double max, String color, String desc) {
		double width = max <= 0 ? 0.0 : value * 100.0 / max;
		if (width > 100.0) {
			width = 100.0;
		}
		if (width < 2.0 && value > 0.0) {
			width = 2.0;
		}
		sb.append("<div style='margin-bottom:10px;'>");
		sb.append("<div style='display:flex; justify-content:space-between; gap:10px; font-size:12px; color:#495057; margin-bottom:4px;'>")
				.append("<b>").append(escapeHtml(title)).append("</b><span>").append(escapeHtml(desc)).append("</span></div>");
		sb.append("<div style='height:13px; background:#f1f3f5; border-radius:999px; overflow:hidden;'>")
				.append("<div style='height:13px; width:").append(formatAngka(width)).append("%; background:").append(color)
				.append("; border-radius:999px;'></div></div>");
		sb.append("</div>");
	}

	private void appendRankBar(StringBuffer sb, int no, String title, double value, double max, String color, String desc) {
		double width = max <= 0 ? 0.0 : value * 100.0 / max;
		if (width > 100.0) {
			width = 100.0;
		}
		if (width < 2.0 && value > 0.0) {
			width = 2.0;
		}
		sb.append("<div style='display:grid; grid-template-columns:28px 1fr; gap:8px; align-items:center; margin-bottom:10px;'>");
		sb.append("<div style='width:24px; height:24px; border-radius:50%; background:").append(color)
				.append("; color:#fff; font-size:12px; font-weight:800; display:flex; align-items:center; justify-content:center;'>")
				.append(no).append("</div>");
		sb.append("<div>");
		sb.append("<div style='display:flex; justify-content:space-between; gap:10px; font-size:12px; color:#495057; margin-bottom:3px;'>")
				.append("<b>").append(escapeHtml(limitText(title, 65))).append("</b><span>").append(desc).append("</span></div>");
		sb.append("<div style='height:10px; background:#f1f3f5; border-radius:999px; overflow:hidden;'>")
				.append("<div style='height:10px; width:").append(formatAngka(width)).append("%; background:").append(color)
				.append("; border-radius:999px;'></div></div>");
		sb.append("</div></div>");
	}

	private String emptyChartMessage(String message) {
		return "<div style='padding:18px; border-radius:12px; background:#f8f9fa; color:#6c757d; text-align:center;'>"
				+ escapeHtml(message) + "</div>";
	}

	private void renderInsight(org.zkoss.zul.Vbox wrapper, DashboardData data) {
		PegawaiSummary terbaik = null;
		PegawaiSummary perhatian = null;
		for (PegawaiSummary h : data.mapPegawai.values()) {
			if (terbaik == null || h.nilaiSkp > terbaik.nilaiSkp) {
				terbaik = h;
			}
			if (perhatian == null || h.nilaiSkp < perhatian.nilaiSkp) {
				perhatian = h;
			}
		}
		String insight1 = data.avgNilaiSkp >= 90.0 ? "Capaian umum sudah sangat baik. Pertahankan konsistensi realisasi kegiatan."
				: data.avgNilaiSkp >= 75.0 ? "Capaian umum cukup baik, namun masih ada ruang peningkatan pada kegiatan bernilai rendah."
						: "Capaian umum perlu perhatian. Prioritaskan monitoring target dengan gap realisasi terbesar.";
		String html = "<div style='background:#fff; border:1px solid #e9ecef; border-radius:16px; padding:15px; box-shadow:0 4px 16px rgba(0,0,0,.045); overflow:hidden; box-sizing:border-box;'>"
				+ "<div style='font-size:17px; font-weight:900; color:#212529; margin-bottom:8px;'>Rekap Summary & Insight Analitik</div>"
				+ "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(240px,1fr)); gap:10px;'>"
				+ buildInsightItem("Status Umum", insight1, warnaPersen(data.avgNilaiSkp))
				+ buildInsightItem("Pegawai Tertinggi", terbaik == null ? "Belum ada data" : terbaik.nama + " · " + formatAngka(terbaik.nilaiSkp), "#198754")
				+ buildInsightItem("Prioritas Evaluasi", perhatian == null ? "Belum ada data" : perhatian.nama + " · " + formatAngka(perhatian.nilaiSkp), "#dc3545")
				+ buildInsightItem("Komposisi Data", data.rows.size() + " target kegiatan dari " + data.mapPegawai.size() + " pegawai", "#0d6efd")
				+ "</div></div>";
		Html insight = new Html(html);
		insight.setParent(wrapper);
	}

	private String buildInsightItem(String title, String value, String color) {
		return "<div style='padding:12px; border-radius:12px; background:#f8f9fa; border-left:5px solid " + color + ";'>"
				+ "<div style='font-size:12px; color:#6c757d; font-weight:800; text-transform:uppercase;'>" + escapeHtml(title)
				+ "</div><div style='font-size:14px; color:#212529; font-weight:700; margin-top:4px;'>" + escapeHtml(value)
				+ "</div></div>";
	}

	private void renderDetailTable(org.zkoss.zul.Vbox wrapper, DashboardData data) {
		org.zkoss.zul.Div tableWrapper = new org.zkoss.zul.Div();
		tableWrapper.setWidth("100%");
		tableWrapper.setStyle("width:100%; max-width:100%; box-sizing:border-box; background:#fff; border:1px solid #e9ecef; border-radius:16px; padding:14px; box-shadow:0 4px 16px rgba(0,0,0,.045); overflow:hidden;");
		tableWrapper.setParent(wrapper);

		int offset = mulai.getValue() == null ? 0 : mulai.getValue().intValue();
		int limit = banyak.getValue() == null ? 10 : banyak.getValue().intValue();
		if (offset < 0) {
			offset = 0;
		}
		if (limit <= 0) {
			limit = 10;
		}
		int end = Math.min(data.rows.size(), offset + limit);

		Html title = new Html("<div style='display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:10px; flex-wrap:wrap;'>"
				+ "<div><div style='font-size:18px; font-weight:900; color:#212529;'>Detail Catatan Realisasi LKP</div>"
				+ "<div style='font-size:12px; color:#6c757d;'>Tampilan layar mengikuti data rincian lkp_pegawai_detail.jrxml, termasuk tanggal, keterangan, dan realisasi detail. Laporan resmi tetap melalui tombol Download/Lihat Laporan Asli.</div></div>"
				+ "<div style='font-size:12px; color:#6c757d; background:#f8f9fa; border-radius:999px; padding:6px 12px;'>Baris "
				+ (data.rows.isEmpty() ? 0 : offset + 1) + " - " + end + " dari " + data.rows.size() + "</div></div>");
		title.setParent(tableWrapper);

		org.zkoss.zul.Div gridScroll = new org.zkoss.zul.Div();
		gridScroll.setWidth("100%");
		gridScroll.setStyle("width:100%; max-width:100%; overflow:auto; box-sizing:border-box; border-radius:12px;");
		gridScroll.setParent(tableWrapper);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("1780px");
		grid.setStyle("border-radius:12px; overflow:hidden;");
		grid.setParent(gridScroll);

		Columns columns = new Columns();
		columns.setParent(grid);
		addColumn(columns, "No", "45px", "center");
		addColumn(columns, "Pegawai", "170px", "left");
		addColumn(columns, "Tanggal", "120px", "left");
		addColumn(columns, "Kegiatan", "260px", "left");
		addColumn(columns, "Keterangan Detail", "260px", "left");
		addColumn(columns, "Target Kuantitas", "110px", "right");
		addColumn(columns, "Detail Kuantitas", "115px", "right");
		addColumn(columns, "Realisasi Total", "115px", "right");
		addColumn(columns, "% Kuantitas", "95px", "right");
		addColumn(columns, "% Kualitas", "95px", "right");
		addColumn(columns, "% Waktu", "95px", "right");
		addColumn(columns, "Detail Waktu", "100px", "right");
		addColumn(columns, "Detail Biaya", "100px", "right");
		addColumn(columns, "Nilai SKP", "95px", "right");
		addColumn(columns, "Status", "120px", "left");

		Rows rows = new Rows();
		rows.setParent(grid);

		if (data.rows.isEmpty()) {
			MyFormRow r = new MyFormRow();
			r.setParent(rows);
			r.appendChild(new Label("Belum ada data target/realisasi untuk filter ini."));
			return;
		}

		for (int i = offset; i < end; i++) {
			LkpRow d = data.rows.get(i);
			MyFormRow r = new MyFormRow();
			r.setParent(rows);
			r.appendChild(new Label(String.valueOf(i + 1)));
			r.appendChild(new Label(nullToBlank(d.nama)));
			r.appendChild(new Label(d.tanggalWaktu == null ? "-" : Common.dateFormat1.get().format(d.tanggalWaktu)));
			r.appendChild(new Label(limitText(d.kegiatan, 95)));
			r.appendChild(new Label(limitText(d.keteranganDetail, 95)));
			r.appendChild(new Label(formatAngka(d.targetKuantitas) + " " + nullToBlank(d.satuanKuantitas)));
			r.appendChild(new Label(formatAngka(d.detailKuantitas) + " " + nullToBlank(d.satuanKuantitas)));
			r.appendChild(new Label(formatAngka(d.realisasiKuantitas) + " " + nullToBlank(d.satuanKuantitas)));
			appendBadgeCell(r, formatAngka(d.capaianKuantitas) + "%", warnaPersen(d.capaianKuantitas));
			appendBadgeCell(r, formatAngka(d.capaianKualitas) + "%", warnaPersen(d.capaianKualitas));
			appendBadgeCell(r, formatAngka(d.capaianWaktu) + "%", warnaPersen(d.capaianWaktu));
			r.appendChild(new Label(formatAngka(d.detailWaktu)));
			r.appendChild(new Label(formatAngka(d.detailBiaya)));
			appendBadgeCell(r, formatAngka(d.nilaiSkp), warnaPersen(d.nilaiSkp));
			r.appendChild(new Label(kategoriNilai(d.nilaiSkp)));
		}
	}

	private void addColumn(Columns columns, String label, String width, String align) {
		MyColumnConfig column = new MyColumnConfig(label);
		column.setParent(columns);
		column.setWidth(width);
		if (align != null) {
			column.setAlign(align);
		}
	}

	private void appendBadgeCell(Row row, String text, String color) {
		Html html = new Html("<span style='display:inline-block; min-width:62px; text-align:center; padding:3px 8px; "
				+ "border-radius:999px; background:" + color + "; color:#fff; font-size:12px; font-weight:800;'>"
				+ escapeHtml(text) + "</span>");
		row.appendChild(html);
	}

	private double maxCapaian(DashboardData data) {
		return Math.max(Math.max(data.avgCapaianKuantitas, data.avgCapaianKualitas), Math.max(data.avgCapaianWaktu, data.avgNilaiSkp));
	}

	private String warnaPersen(double value) {
		if (value >= 100.0) {
			return "#198754";
		}
		if (value >= 90.0) {
			return "#20c997";
		}
		if (value >= 75.0) {
			return "#fd7e14";
		}
		return "#dc3545";
	}

	private String kategoriNilai(double nilai) {
		if (nilai >= 100.0) {
			return "Sangat Baik";
		}
		if (nilai >= 90.0) {
			return "Baik";
		}
		if (nilai >= 75.0) {
			return "Cukup";
		}
		return "Perlu Perhatian";
	}

	private double safePercent(double realisasi, double target) {
		if (target == 0.0) {
			return 0.0;
		}
		return (realisasi * 100.0) / target;
	}

	private double toDouble(Object value) {
		if (value == null) {
			return 0.0;
		}
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).doubleValue();
		}
		if (value instanceof BigInteger) {
			return ((BigInteger) value).doubleValue();
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(value));
		} catch (Exception e) {
			return 0.0;
		}
	}

	private Long toLong(Object value) {
		if (value == null) {
			return null;
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
		try {
			return Long.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return null;
		}
	}

	private String toStringValue(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private String nullToBlank(String text) {
		return text == null ? "" : text;
	}

	private String formatAngka(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(Math.round(value * 100.0) / 100.0);
		}
	}

	private String escapeHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	private String limitText(String text, int max) {
		if (text == null) {
			return "";
		}
		if (text.length() <= max) {
			return text;
		}
		return text.substring(0, Math.max(0, max - 3)) + "...";
	}

	/**
	 * Pembawa data/helper lokal milik {@link LaporanRealisasiLkpDetailWindow} untuk dashboard data. Tipe ini
	 * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanRealisasiLkpDetailWindow} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int tahun}, {@code int bulan}, {@code
	 * String bulanStr}, {@code double persenKuantitas}, {@code double persenKualitas}, {@code double persenWaktu},
	 * {@code List rows}, {@code Map mapPegawai}. Aturan bisnis bersama tetap berada pada kelas induk atau service
	 * yang dipanggilnya.</p>
	 *
	 * @see LaporanRealisasiLkpDetailWindow
	 */
	private class DashboardData {
		int tahun;
		int bulan;
		String bulanStr;
		double persenKuantitas;
		double persenKualitas;
		double persenWaktu;
		List<LkpRow> rows;
		Map<Long, PegawaiSummary> mapPegawai;
		double totalTargetKuantitas;
		double totalTargetWaktu;
		double totalTargetBiaya;
		double totalRealisasiKuantitas;
		double totalRealisasiWaktu;
		double totalRealisasiBiaya;
		double sumCapaianKuantitas;
		double sumCapaianKualitas;
		double sumCapaianWaktu;
		double sumNilaiSkp;
		double avgCapaianKuantitas;
		double avgCapaianKualitas;
		double avgCapaianWaktu;
		double avgNilaiSkp;
		int jumlahTercapai;
		int jumlahPerluPerhatian;
	}

	/**
	 * Pembawa data/helper lokal milik {@link LaporanRealisasiLkpDetailWindow} untuk pegawai summary. Tipe ini
	 * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanRealisasiLkpDetailWindow} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long pegawaiId}, {@code String nama},
	 * {@code String nip}, {@code String satuanKerja}, {@code String jabatanFungsional}, {@code int targetCount},
	 * {@code double totalTargetKuantitas}, {@code double totalRealisasiKuantitas}; operasi lokal: {@code add()},
	 * {@code finish}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see LaporanRealisasiLkpDetailWindow
	 */
	private class PegawaiSummary {
		Long pegawaiId;
		String nama = "";
		String nip = "";
		String satuanKerja = "";
		String jabatanFungsional = "";
		int targetCount;
		double totalTargetKuantitas;
		double totalRealisasiKuantitas;
		double totalTargetWaktu;
		double totalRealisasiWaktu;
		double sumCapaianKuantitas;
		double sumCapaianKualitas;
		double sumCapaianWaktu;
		double sumNilaiSkp;
		double nilaiSkp;

		void add(LkpRow r) {
			targetCount++;
			totalTargetKuantitas += r.targetKuantitas;
			totalRealisasiKuantitas += r.realisasiKuantitas;
			totalTargetWaktu += r.targetWaktu;
			totalRealisasiWaktu += r.realisasiWaktu;
			sumCapaianKuantitas += r.capaianKuantitas;
			sumCapaianKualitas += r.capaianKualitas;
			sumCapaianWaktu += r.capaianWaktu;
			sumNilaiSkp += r.nilaiSkp;
		}

		void finish() {
			if (targetCount > 0) {
				nilaiSkp = sumNilaiSkp / targetCount;
			}
		}
	}

	/**
	 * Tipe implementasi bersarang {@link LkpRow} milik {@link LaporanRealisasiLkpDetailWindow}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanRealisasiLkpDetailWindow} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Integer bulan}, {@code Integer
	 * tahun}, {@code Long pegawaiId}, {@code Long targetId}, {@code String nama}, {@code String nip}, {@code
	 * String jabatanFungsional}, {@code String satuanKerja}. Aturan bisnis bersama tetap berada pada kelas induk
	 * atau service yang dipanggilnya.</p>
	 *
	 * @see LaporanRealisasiLkpDetailWindow
	 */
	private class LkpRow {
		Integer bulan;
		Integer tahun;
		Long pegawaiId;
		Long targetId;
		String nama;
		String nip;
		String jabatanFungsional;
		String satuanKerja;
		String kegiatan;
		double angkaKredit;
		double targetKuantitas;
		String satuanKuantitas;
		double targetKualitas;
		double targetWaktu;
		String satuanWaktu;
		double targetBiaya;
		double realisasiKuantitas;
		double realisasiKualitas;
		double realisasiWaktu;
		double realisasiBiaya;
		String asesor;
		String asesorNip;
		String keteranganDetail;
		java.util.Date tanggalWaktu;
		double detailKuantitas;
		double detailWaktu;
		double detailBiaya;
		double capaianKuantitas;
		double capaianKualitas;
		double capaianWaktu;
		double nilaiSkp;

		LkpRow(Object[] row, double bobotKuantitas, double bobotKualitas, double bobotWaktu) {
			bulan = row[0] == null ? null : Integer.valueOf((int) toDouble(row[0]));
			tahun = row[1] == null ? null : Integer.valueOf((int) toDouble(row[1]));
			pegawaiId = toLong(row[2]);
			targetId = toLong(row[3]);
			nama = toStringValue(row[4]);
			nip = toStringValue(row[5]);
			jabatanFungsional = toStringValue(row[10]);
			satuanKerja = toStringValue(row[11]);
			kegiatan = toStringValue(row[14]);
			angkaKredit = toDouble(row[15]);
			targetKuantitas = toDouble(row[16]);
			satuanKuantitas = toStringValue(row[17]);
			targetKualitas = toDouble(row[18]);
			targetWaktu = toDouble(row[19]);
			satuanWaktu = toStringValue(row[20]);
			targetBiaya = toDouble(row[21]);
			realisasiKuantitas = toDouble(row[22]);
			realisasiKualitas = toDouble(row[23]);
			realisasiWaktu = toDouble(row[24]);
			realisasiBiaya = toDouble(row[25]);
			asesor = toStringValue(row[26]);
			asesorNip = toStringValue(row[27]);
			keteranganDetail = toStringValue(row[28]);
			if (row[29] instanceof java.util.Date) {
				tanggalWaktu = (java.util.Date) row[29];
			}
			detailKuantitas = toDouble(row[30]);
			detailWaktu = toDouble(row[31]);
			detailBiaya = toDouble(row[32]);
			capaianKuantitas = safePercent(realisasiKuantitas, targetKuantitas);
			capaianKualitas = safePercent(realisasiKualitas, targetKualitas);
			capaianWaktu = safePercent(realisasiWaktu, targetWaktu);
			nilaiSkp = (capaianKuantitas * (bobotKuantitas / 100.0))
					+ (capaianKualitas * (bobotKualitas / 100.0))
					+ (capaianWaktu * (bobotWaktu / 100.0));
		}
	}
}
