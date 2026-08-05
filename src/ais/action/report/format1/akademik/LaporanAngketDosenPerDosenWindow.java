package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.dashboard.helper.DashboardRekapParameterTambahanUmum;
import ais.action.master.dashboard.helper.DashboardRekapParameterTambahanUmumData;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.RangkingIpd;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class LaporanAngketDosenPerDosenWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox tahunAkademik;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox semesterAbsensi;
	private AmbilDataDosenBanbox dosen;
	private Toolbar toolbar;
	private Center center;
	private Dosen dsn;
	private Combobox program;

	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig aktif;

	public LaporanAngketDosenPerDosenWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Dosen Per Dosen Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanAngketDosenPerDosenWindow(Dosen dsn) {
		super();
		this.dsn = dsn;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Dosen Per Dosen Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanAngketDosenPerDosenWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null) {
			dsn = tbmuser.ambilDosen();
		}

		boolean[] ptYa = Common.chekPtAtauSekolah();
		final boolean pt = ptYa != null && ptYa.length > 0 && ptYa[0];
		final boolean ya = ptYa != null && ptYa.length > 1 && ptYa[1];

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(RangkingIpd.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			RangkingIpd rangkingIpd = new RangkingIpd();
			rangkingIpd.setNama("Sangat Baik");
			rangkingIpd.setMinimal(91.0);
			session.save(rangkingIpd);

			rangkingIpd = new RangkingIpd();
			rangkingIpd.setNama("Baik");
			rangkingIpd.setMinimal(76.0);
			session.save(rangkingIpd);

			rangkingIpd = new RangkingIpd();
			rangkingIpd.setNama("Cukup");
			rangkingIpd.setMinimal(61.0);
			session.save(rangkingIpd);

			rangkingIpd = new RangkingIpd();
			rangkingIpd.setNama("Kurang");
			rangkingIpd.setMinimal(51.0);
			session.save(rangkingIpd);

			rangkingIpd = new RangkingIpd();
			rangkingIpd.setNama("Sangat Kurang");
			rangkingIpd.setMinimal(0.0);
			session.save(rangkingIpd);
		}

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanAngketDosenPerDosen(event);
			}
		};

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);


		final MyTabConfig tabDosen = new MyTabConfig("Angket Dosen");
		tabDosen.setVisible(pt);
		tabDosen.setParent(tabs);

		MyTabConfig tabUmum = new MyTabConfig("Angket Umum");
		tabUmum.setVisible(true);
		tabUmum.setParent(tabs);

		final MyTabConfig tabGuru = new MyTabConfig("Angket Guru");
		tabGuru.setVisible(ya);
		tabGuru.setParent(tabs);

		MyTabConfig tabParameterUmum = new MyTabConfig("Rekap Parameter Umum");
		tabParameterUmum.setVisible(Common.getCurrentUser().ambilDosen() == null && dsn == null);
		tabParameterUmum.setParent(tabs);

		MyTabConfig tabParameterUmumData = new MyTabConfig("Data Parameter Umum");
		tabParameterUmumData.setVisible(Common.getCurrentUser().ambilDosen() == null && dsn == null);
		tabParameterUmumData.setParent(tabs);

		MyTabConfig tabDataDosen = new MyTabConfig("Data Angket Dosen");
		tabDataDosen.setParent(tabs);

		MyTabConfig tabDataUmum = new MyTabConfig("Data Angket Umum");
		tabDataUmum.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		final Tabpanel tabpanelUmum = new ais.ui.util.MyTabpanel();
		tabpanelUmum.setParent(tabpanels);
		tabUmum.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// if (tabpanelUmum.getChildren().size() == 0) {
				Common.clear(tabpanelUmum);

				LaporanAngketUmumWindow laporanAngketUmumWindow = new LaporanAngketUmumWindow();
				tabpanelUmum.appendChild(laporanAngketUmumWindow);

			}
		});

		final Tabpanel tabpanelGuru = new ais.ui.util.MyTabpanel();
		tabpanelGuru.setHeight("100%");
		tabpanelGuru.setStyle("overflow:auto;");
		tabpanelGuru.setParent(tabpanels);
		tabGuru.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelGuru.getChildren().isEmpty()) {
					tabpanelGuru.appendChild(new LaporanAngketGuruDashboardWindow());
				}
			}
		});

		final Tabpanel tabpanelParameterUmum = new ais.ui.util.MyTabpanel();
		tabpanelParameterUmum.setParent(tabpanels);
		tabParameterUmum.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// if (tabpanelUmum.getChildren().size() == 0) {
				Common.clear(tabpanelParameterUmum);

				DashboardRekapParameterTambahanUmum laporanAngketUmumWindow = new DashboardRekapParameterTambahanUmum();
				tabpanelParameterUmum.appendChild(laporanAngketUmumWindow);

			}
		});

		final Tabpanel tabpanelParameterUmumData = new ais.ui.util.MyTabpanel();
		tabpanelParameterUmumData.setParent(tabpanels);
		tabParameterUmumData.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// if (tabpanelUmum.getChildren().size() == 0) {
				Common.clear(tabpanelParameterUmumData);

				DashboardRekapParameterTambahanUmumData laporanAngketUmumWindow = new DashboardRekapParameterTambahanUmumData();
				tabpanelParameterUmumData.appendChild(laporanAngketUmumWindow);

			}
		});

		final Tabpanel tabpanelDataDosen = new ais.ui.util.MyTabpanel();
		tabpanelDataDosen.setParent(tabpanels);
		tabDataDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelDataDosen.getChildren().size() == 0) {
					LaporanRekapAngketDosenPerMahasiswa laporanAngketUmumWindow = new LaporanRekapAngketDosenPerMahasiswa();
					tabpanelDataDosen.appendChild(laporanAngketUmumWindow);
				}
			}
		});

		final Tabpanel tabpanelDataUmum = new ais.ui.util.MyTabpanel();
		tabpanelDataUmum.setParent(tabpanels);
		tabDataUmum.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelDataUmum.getChildren().size() == 0) {
					LaporanChecklistHasilPenilaianUmumPerPengguna laporanAngketUmumWindow = new LaporanChecklistHasilPenilaianUmumPerPengguna();
					tabpanelDataUmum.appendChild(laporanAngketUmumWindow);
				}
			}
		});

		Tabbox mytabbox = new Tabbox();
		mytabbox.setParent(tabpanelUtama);
		mytabbox.setHeight("100%");
		mytabbox.setWidth("100%");

		Tabs mytabs = new Tabs();
		mytabs.setParent(mytabbox);

		final MyTabConfig mytabDasborDosen = new MyTabConfig("Dasbor");
		mytabDasborDosen.setParent(mytabs);

		final MyTabConfig mytabDosen1 = new MyTabConfig("Angket Per Dosen");
		mytabDosen1.setParent(mytabs);

		final MyTabConfig mytabDosen5Rangking = new MyTabConfig("Rangking");
		mytabDosen5Rangking.setParent(mytabs);

		Tabbox mytabboxRangking = new Tabbox();
		mytabboxRangking.setHeight("100%");
		mytabboxRangking.setWidth("100%");

		Tabs mytabsRangking = new Tabs();
		mytabsRangking.setParent(mytabboxRangking);

		final MyTabConfig mytabDosen12 = new MyTabConfig("Angket Perbandingan");
		mytabDosen12
				.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_perbandingan"));
		mytabDosen12.setParent(mytabsRangking);

		final MyTabConfig mytabDosen2 = new MyTabConfig("Per Perkuliahan");
		mytabDosen2.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_perkuliahan"));
		mytabDosen2.setParent(mytabs);

		final MyTabConfig mytabDosen221 = new MyTabConfig("Rekap");
		mytabDosen221.setVisible(dsn == null);
		mytabDosen221.setParent(mytabs);

		final MyTabConfig mytabDosen22 = new MyTabConfig("Keterangan");
		mytabDosen22.setParent(mytabs);

		final MyTabConfig mytabDosen23 = new MyTabConfig("Masukan");
		mytabDosen23.setParent(mytabs);

		final MyTabConfig mytabDosen21 = new MyTabConfig("Penilaian Mahasiswa");
		mytabDosen21.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen", Konfigurasi.TIDAK_AKTIF));
		mytabDosen21.setParent(mytabs);

		final MyTabConfig mytabDosen3 = new MyTabConfig("Angket Per Matakuliah");
		mytabDosen3.setVisible(Common.getCurrentUser().ambilDosen() == null && dsn == null
				&& Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_matakuliah"));
		mytabDosen3.setParent(mytabs);

		final MyTabConfig mytabDosen4 = new MyTabConfig("Angket Per " + Common.getBahasaConfig("Jurusan"));
		mytabDosen4.setVisible(Common.getCurrentUser().ambilDosen() == null && dsn == null
				&& Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_prodi"));
		mytabDosen4.setParent(mytabs);

		final MyTabConfig mytabDosen41 = new MyTabConfig(
				"Angket Per " + Common.getBahasaConfig("Jurusan") + " untuk Dosen");
		mytabDosen41.setVisible(Common.getCurrentUser().ambilDosen() == null && dsn == null
				&& Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_prodi_untuk_dosen"));
		mytabDosen41.setParent(mytabs);

		final MyTabConfig mytabDosen5 = new MyTabConfig("Rangking Angket Dosen");
		mytabDosen5.setVisible(Common.getCurrentUser().ambilDosen() == null && dsn == null
				&& Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_rangking_angket_dosen"));
		mytabDosen5.setParent(mytabsRangking);

		final MyTabConfig mytabDosen6 = new MyTabConfig(
				"Rangking Angket Dosen Per " + Common.getBahasaConfig("Jurusan"));
		mytabDosen6.setVisible(Common.getCurrentUser().ambilDosen() == null && dsn == null
				&& Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_rangking_angket_jurusan"));
		mytabDosen6.setParent(mytabsRangking);

		final MyTabConfig mytabDosen61 = new MyTabConfig("Mahasiswa yg mengisi angket");
		mytabDosen61.setVisible(Common.getCurrentUser().ambilDosen() == null && dsn == null
				&& Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_tampil_mahasiswa"));
		mytabDosen61.setParent(mytabs);

		final MyTabConfig mytabDosen7 = new MyTabConfig("Rekap Per Dosen dan Matakuliah");
		mytabDosen7
				.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_rekap_angket_per_dosen_dan_matakuliah"));
		mytabDosen7.setParent(mytabs);

		final MyTabConfig mytabDosen8 = new MyTabConfig("Rangking IPD");
		mytabDosen8.setVisible(Common.getCurrentUser().ambilDosen() == null && dsn == null);
		mytabDosen8.setParent(mytabsRangking);

		Tabpanels mytabpanels = new Tabpanels();
		mytabpanels.setParent(mytabbox);

		Tabpanel mytabpanelDasborDosen = new ais.ui.util.MyTabpanel();
		mytabpanelDasborDosen.setHeight("100%");
		mytabpanelDasborDosen.setStyle("overflow:auto;");
		mytabpanelDasborDosen.setParent(mytabpanels);
		mytabpanelDasborDosen.appendChild(new LaporanAngketDosenDashboardWindow(dsn));

		Tabpanel mytabpanelUtama = new ais.ui.util.MyTabpanel();
		mytabpanelUtama.setParent(mytabpanels);

		Tabpanels mytabpanelsRangking = new Tabpanels();
		mytabpanelsRangking.setParent(mytabboxRangking);

		Tabpanel mytabpanelUtamaRangking = new ais.ui.util.MyTabpanel();
		mytabpanelUtamaRangking.setParent(mytabpanels);

		mytabboxRangking.setParent(mytabpanelUtamaRangking);

		Tabpanel tabpanelAngketPerDosen21 = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen21.setParent(mytabpanelsRangking);
		tabpanelAngketPerDosen21
				.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_perbandingan"));
		tabpanelAngketPerDosen21.appendChild(new LaporanAngketPerbandinganDosenWindow(dsn));

		Tabpanel tabpanelAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen.setParent(mytabpanels);
		tabpanelAngketPerDosen
				.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_perkuliahan"));
		tabpanelAngketPerDosen.appendChild(new LaporanAngketDosenPerDosenSajaWindow(dsn));

		Tabpanel tabpanelLaporanRekapAngketDosen = new ais.ui.util.MyTabpanel();
		tabpanelLaporanRekapAngketDosen.setParent(mytabpanels);
		tabpanelLaporanRekapAngketDosen.setVisible(dsn == null);
		tabpanelLaporanRekapAngketDosen.appendChild(new LaporanRekapAngketDosen(dsn));

		Tabpanel tabpanelKeteranganAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelKeteranganAngketPerDosen.setParent(mytabpanels);
		tabpanelKeteranganAngketPerDosen.appendChild(new LaporanKeteranganAngketDosenPerDosenWindow(dsn));

		Tabpanel tabpanelMasukanAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelMasukanAngketPerDosen.setParent(mytabpanels);
		tabpanelMasukanAngketPerDosen.appendChild(new LaporanMasukanAngketDosenPerDosenWindow(dsn));

		Tabpanel tabpanelAngketPerDosen2 = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen2
				.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen", Konfigurasi.TIDAK_AKTIF));
		tabpanelAngketPerDosen2.setParent(mytabpanels);
		tabpanelAngketPerDosen2.appendChild(new LaporanAngketDosenPerDosenJenisLainWindow(dsn));

		tabpanelAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen.setParent(mytabpanels);
		tabpanelAngketPerDosen
				.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_matakuliah"));
		tabpanelAngketPerDosen.appendChild(new LaporanAngketDosenPerMatakuliahWindow());

		tabpanelAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen.setParent(mytabpanels);
		tabpanelAngketPerDosen
				.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_prodi"));
		tabpanelAngketPerDosen.appendChild(new LaporanAngketDosenPerJurusanWindow());

		tabpanelAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen.setParent(mytabpanels);
		tabpanelAngketPerDosen.setVisible(
				Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_per_prodi_untuk_dosen"));
		tabpanelAngketPerDosen.appendChild(new LaporanAngketDosenPerJurusanUntukDosenWindow());

		tabpanelAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen.setVisible(
				Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_rangking_angket_dosen"));
		tabpanelAngketPerDosen.setParent(mytabpanelsRangking);
		tabpanelAngketPerDosen.appendChild(new LaporanRekapAngketDosenWindow());

		tabpanelAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen.setVisible(
				Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_rangking_angket_jurusan"));
		tabpanelAngketPerDosen.setParent(mytabpanelsRangking);
		tabpanelAngketPerDosen.appendChild(new LaporanRekapAngketDosenPerJurusanWindow());

		tabpanelAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen
				.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_tampil_mahasiswa"));
		tabpanelAngketPerDosen.setParent(mytabpanels);
		tabpanelAngketPerDosen.appendChild(new LaporanRekapAngketPerMahasiswaWindow());

		tabpanelAngketPerDosen = new ais.ui.util.MyTabpanel();
		tabpanelAngketPerDosen
				.setVisible(Common.bolehKonfigurasi("penilaian_mahasiswa_thd_dosen_rekap_angket_per_dosen_dan_matakuliah"));
		tabpanelAngketPerDosen.setParent(mytabpanels);
		tabpanelAngketPerDosen.appendChild(new LaporanRekapAngketPerDosenDanMatakuliahWindow(dsn));

		final Tabpanel tabpanelRangking = new ais.ui.util.MyTabpanel();
		tabpanelRangking.setParent(mytabpanelsRangking);
		mytabDosen8.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelRangking.getChildren().isEmpty()) {
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("100%");
					window.setWidth("100%");
					window.setParent(tabpanelRangking);
					MyInclude iframe = new MyInclude("/pages/master/rangking_ipd.zul");
					iframe.setParent(window);
				}
			}
		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(mytabpanelUtama);

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

		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		program = Common.initPrograms(null);
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");

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
//		comboitem = new MyComboitemConfig();
//		comboitem.setLabel("Semua");
//		comboitem.setValue(null);
//		semesterAbsensi.appendChild(comboitem);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jurusan.getSelectedItem() != null) {
					masaPerkuliahan.setJurusanSelected((Jurusan) jurusan.getSelectedItem().getValue());
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.aktif = new MyCheckboxConfig("Tampilkan Hanya Angket Aktif"));
		aktif.setChecked(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen")));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");
		dosen.setReadonly(true);

		if (dsn != null) {
			dosen.setAttribute("dosen", dsn);
			dosen.setAttribute("myValue", dsn);
			dosen.setValue(dsn.getNama());
		}

		Common.initKeterangan(rows, "Jika dosen tidak dipilih, maka akan tampil data semua dosen");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		MyButtonConfig tombol;
		hbox.appendChild(tombol = new MyButtonConfig("Lihat Laporan", "/img/print.png"));
		hbox.appendChild(LaporanRekapAngketDosenPerJurusanWindow.hitungUlangAngket(tahunAkademik, semesterAbsensi,
				masaPerkuliahan, aktif));
		tombol.addEventListener("onClick", eventListener);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "rekap_angket_dosen_per_dosen_saja", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanAngketDosenPerDosen(arg0);
			}
		}));

		if (Common.bolehKonfigurasi("tampilkan_fitur_hapus_atau_reset_angket", Konfigurasi.TIDAK_AKTIF)) {
			if (Common.getApakahAdmin()) {
				row = new MyFormRow();
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				hbox = new Hbox();
				hbox.setParent(row);

				hbox.appendChild(
						tombol = new MyButtonConfig("Hapus / Reset Angket Penilaian Dosen", "/img/svg/trash.svg"));
				tombol.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin menghapus / Reset Angket Penilaian Dosen ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												String ta = (String) tahunAkademik.getSelectedItem().getValue();
												String smt = (String) semesterAbsensi.getSelectedItem().getValue();

												Session session = ais.action.report.Report.openNativeSession();
												List<Long> ids = session
														.createCriteria(ChecklistBaruPenilaianDosenOlehMahasiswa.class)
														.setProjection(Projections.property("id"))
														.createAlias("perkuliahan", "perkuliahan")
														.add(Restrictions.eq("perkuliahan.tahunAjaran", ta))
														.add(Restrictions.eq("perkuliahan.ganjilGenap", smt)).list();
												// session.disconnect();
												if (session.isOpen()) {
													session.disconnect();
													session.close();
												}
												ais.action.report.Report.closeCurrentSessionQuietly();

												String sql = "";
												for (Long id : ids) {
													sql += sql.isEmpty() ? id + "" : "," + id;
												}

												if (!sql.isEmpty()) {
													session = ais.action.report.Report.openNativeSession();
													session.createSQLQuery(
															"delete from checklist_baru_penilaian_dosen_oleh_mahasiswa where id in ("
																	+ sql + ")")
															.executeUpdate();
													// session.disconnect();
													if (session.isOpen()) {
														session.disconnect();
														session.close();
													}
													ais.action.report.Report.closeCurrentSessionQuietly();
												}

												MyMessageboxConfig.showFormat(
														"Data angket sejumlah {V1} baris telah berhasil dihapus. Terima kasih atas kesediaan Bapak/Ibu menggunakan layanan ini.",
														"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ids.size());

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.showFormat(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan sistem: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang terkait dengan data ini; (2) periksa kembali keterkaitan antar-data; (3) hubungi admin apabila memerlukan bantuan lebih lanjut.",
														"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, e.getMessage());
											}

										}

									}
								});

					}
				});
			}
		}
		if (!pt) {
			try {
				tabbox.setSelectedIndex(1);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketDosenPerDosenWindow.java:720");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Dosen Per Dosen Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
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

		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());

		Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");

		Map parameters = new java.util.HashMap();
		parameters.put("genapGanjil", genapGanjil == null ? "Semua" : genapGanjil);
		parameters.put("tahun_akademik", tahunAkademik == null ? "Semua" : tahunAkademik);
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
		parameters.put("nama_dosen", dosen == null ? "" : dosen.getNama());
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("jurusan_nama", jurusan == null ? "" : jurusan.getNama());
		parameters.put("program", program == null ? "-1" : program);
		parameters.put("fakultas_nama", fakultas == null ? "" : fakultas.getNama());

		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		parameters.put("aktif", aktif.isChecked());

		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);
		parameters.put("masa_perkuliahan", masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());

		return parameters;

	}

	@SuppressWarnings({})
	public void onLaporanAngketDosenPerDosen(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"rekap_angket_dosen_per_dosen_saja", ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});
	}

}
