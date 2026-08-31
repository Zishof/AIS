package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.GrupChecklistPenilaianUmumAction;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan angket umum window. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahunAkademik}, {@code
 * Combobox semesterAbsensi}, {@code Toolbar toolbar}, {@code Center center}, {@code Combobox
 * searchdiperuntukkan}; inisialisasi/lifecycle ({@code init()}); operasi domain lain ({@code
 * generateParameter()}, {@code onLaporanAngketDosenPerDosen()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanAngketUmumWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox tahunAkademik;
	private Combobox semesterAbsensi;

	private Toolbar toolbar;
	private Center center;
	private Combobox searchdiperuntukkan;

	public LaporanAngketUmumWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Umum Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanAngketUmumWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {
		setHeight("100%");
		setWidth("100%");
		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanAngketDosenPerDosen(event);
			}
		};

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(this); // FIX tinggi-pasti: saat window ini di-embed sbg sub-tab, rantai height:100% kolaps 0px (lihat LaporanRekapJumlahMahasiswa)
		tabbox.setHeight("2000px");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabDasbor = new MyTabConfig("Dasbor");
		tabDasbor.setParent(tabs);

		MyTabConfig tabDosen = new MyTabConfig("Angket Umum");
		tabDosen.setParent(tabs);

		MyTabConfig tabRekap = new MyTabConfig("Rekap Survey");
		tabRekap.setParent(tabs);

		MyTabConfig tabUmum = new MyTabConfig("Angket Umum Per-" + Common.getBahasaConfig("Jurusan"));
		tabUmum.setParent(tabs);

		MyTabConfig tabDiisiOleh = new MyTabConfig("Pengguna yang mengsi angket umum");
		tabDiisiOleh.setParent(tabs);

		MyTabConfig tabAngketUmumGrup = new MyTabConfig("Angket Umum Grup");
		tabAngketUmumGrup.setParent(tabs);

		MyTabConfig mytabDosen22 = new MyTabConfig("Keterangan / Masukan");
		mytabDosen22.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelDasbor = new ais.ui.util.MyTabpanel();
		tabpanelDasbor.setParent(tabpanels);
		tabpanelDasbor.setHeight("100%");
		tabpanelDasbor.setStyle("overflow:auto;");
		tabpanelDasbor.appendChild(new LaporanAngketUmumDashboardWindow());

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		final Tabpanel tabpanelRekap = new ais.ui.util.MyTabpanel();
		tabpanelRekap.setParent(tabpanels);
		tabRekap.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tabpanelRekap);

				LaporanAngketUmumSurveyWindow laporanAngketUmumSurveyWindow = new LaporanAngketUmumSurveyWindow();
				tabpanelRekap.appendChild(laporanAngketUmumSurveyWindow);

			}
		});

		final Tabpanel tabpanelUmum = new ais.ui.util.MyTabpanel();
		tabpanelUmum.setParent(tabpanels);
		tabUmum.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tabpanelUmum);

				LaporanAngketUmumPerJurusanWindow laporanAngketUmumWindow = new LaporanAngketUmumPerJurusanWindow();
				tabpanelUmum.appendChild(laporanAngketUmumWindow);

			}
		});

		final Tabpanel tabpanelDiisiOleh = new ais.ui.util.MyTabpanel();
		tabpanelDiisiOleh.setParent(tabpanels);
		tabDiisiOleh.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// if (tabpanelUmum.getChildren().size() == 0) {
				Common.clear(tabpanelDiisiOleh);

				LaporanAngketUmumDiisOlehWindow laporanAngketUmumWindow = new LaporanAngketUmumDiisOlehWindow();
				tabpanelDiisiOleh.appendChild(laporanAngketUmumWindow);

			}
		});

		final Tabpanel tabpanelAngketUmumGrup = new ais.ui.util.MyTabpanel();
		tabpanelAngketUmumGrup.setParent(tabpanels);
		tabAngketUmumGrup.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// if (tabpanelUmum.getChildren().size() == 0) {
				Common.clear(tabpanelAngketUmumGrup);

				LaporanAngketGrupUmumWindow laporanAngketUmumWindow = new LaporanAngketGrupUmumWindow();
				tabpanelAngketUmumGrup.appendChild(laporanAngketUmumWindow);

			}
		});

		final Tabpanel tabpanelAngketKeterangan = new ais.ui.util.MyTabpanel();
		tabpanelAngketKeterangan.setParent(tabpanels);
		mytabDosen22.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// if (tabpanelUmum.getChildren().size() == 0) {
				Common.clear(tabpanelAngketKeterangan);

				LaporanKeteranganAngketUmumPerJurusanWindow laporanAngketUmumWindow = new LaporanKeteranganAngketUmumPerJurusanWindow();
				tabpanelAngketKeterangan.appendChild(laporanAngketUmumWindow);

			}
		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", eventListener);
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensi = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.addEventListener("onChange", eventListener);
		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengguna"));
		row.appendChild(searchdiperuntukkan = new Combobox());
		GrupChecklistPenilaianUmumAction.diperuntukkan(searchdiperuntukkan);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchdiperuntukkan.appendChild(comboitem);
		searchdiperuntukkan.setWidth("90%");
		searchdiperuntukkan.addEventListener("onChange", eventListener);
		searchdiperuntukkan.setReadonly(true);

		searchdiperuntukkan.setSelectedItem(comboitem);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "rekap_angket_umum", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanAngketDosenPerDosen(arg0);
			}
		}));

		try {
			onLaporanAngketDosenPerDosen(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Umum Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		String genapGanjil = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? "Semua"
						: semesterAbsensi.getSelectedItem().getValue());

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? "Semua"
						: this.tahunAkademik.getSelectedItem().getValue());

		final Map parameters = new java.util.HashMap();

		String diperuntukkan = (String) (searchdiperuntukkan.getSelectedItem() == null
				|| searchdiperuntukkan.getSelectedItem().getValue() == null ? "Semua"
						: searchdiperuntukkan.getSelectedItem().getValue());
		parameters.put("diperuntukkan", diperuntukkan == null ? "Semua" : diperuntukkan);

		parameters.put("genapGanjil", genapGanjil == null ? "Semua" : genapGanjil);

		parameters.put("tahun_akademik", tahunAkademik == null ? "Semua" : tahunAkademik);

		return parameters;

	}

	@SuppressWarnings({})
	public void onLaporanAngketDosenPerDosen(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "rekap_angket_umum",
						ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

	}

}
