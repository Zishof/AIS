package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardDataNilaiDanIPKMahasiswa;
import ais.action.master.dashboard.admin.DashboardDataNilaiIPKMahasiswaPerTahunAngkatan;
import ais.action.master.dashboard.admin.DashboardDataNilaiMahasiswa;
import ais.action.master.dashboard.admin.DashboardDataNilaiMahasiswaPerTahunAngkatan;
import ais.action.master.dashboard.admin.DashboardGradePenilaianMahasiswa;
import ais.action.master.dashboard.admin.DashboardNilaiMahasiswa;
import ais.action.master.dashboard.admin.DashboardPenilaianMahasiswa;
import ais.action.master.dashboard.admin.DashboardRekapNilaiMahasiswa;
import ais.action.master.dashboard.admin.DashboardStatistikPenilaian;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.DetailperkuliahanForPenilaianHelper;
import ais.action.master.helper.FormatPenilaianHelper;
import ais.action.master.helper.PengecualianJadwalPenilaianAdminHelper;
import ais.action.master.helper.PengecualianJadwalPenilaianDosenHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiHistoryDetailPerkuliahanHelper;
import ais.action.master.helper.SyncHelper;
import ais.action.master.helper.TampilDetailNilaiInterface;
import ais.action.master.helper.penilaian.DownloadNilaiMahasiswa;
import ais.action.master.helper.penilaian.DownloadNilaiMahasiswaFormatEpsbed;
import ais.action.master.helper.penilaian.UploadNilaiMahasiswa;
import ais.action.master.helper.penilaian.UploadNilaiMahasiswaFormatEpsbed;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.format1.akademik.LaporanPenilaianOlehDosenPerDosenWindow;
import ais.common.AsyncTaskManager;
import ais.common.Common;
import ais.common.CommonPenilaian;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.KomentarPerkuliahan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.PembombotanNilai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PenilaianAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */

	protected static final long serialVersionUID = 3786091220301468178L;
	protected MyWindow addWindow;
	protected Paging paging;
	protected MyGrid grid;
	protected AmbilDataMatakuliahBanbox searchmatakuliah;
	protected AmbilDataDosenBanbox searchdosen;

	protected Textbox searchkelas;
	protected MyCheckboxConfig searchparalel;
	protected MyCheckboxConfig searchtanpakelas;

	protected MyCheckboxConfig searchdikunci;
	protected MyCheckboxConfig searchbelumdikunci;

	protected AmbilDataRuangBanbox searchruang;

	protected Boolean merupakanRemedial = false;

	protected Combobox searchhari;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	protected Combobox searchsemester;
	protected AmbilDataMasaPerkuliahanBanbox searchmasaperkulaiahan;
	protected Combobox searchprogram;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	protected AmbilDataKurikulumBanbox searchkurikulum;
	protected AmbilDataMahasiswaBanbox searchasisten;
	protected Textbox searchnamamhs;
	protected Textbox searchnim;
	protected Combobox searchTahap;

	protected Textbox searchnamadsn;
	protected Textbox searchnamamk;
	protected Textbox searchKeterangan;
	protected Textbox searchnamaasisten;

//	protected Combobox matakuliah;
//	protected Combobox dosen1;
//	protected Combobox dosen2;
//	protected Textbox ruang;
//	protected Decimalbox semester;

	protected Perkuliahan perkuliahan;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected MyToolbarbuttonConfig buka;
	protected MyToolbarbuttonConfig tutup;
	/** Khusus admin: tombol aktif/non-aktifkan penilaian untuk TA &amp; Jenis Smt terpilih (mendukung SP). */
	protected MyToolbarbuttonConfig toggleAktifPenilaian;

	protected MyToolbarbuttonConfig optimize;
	protected MyToolbarbuttonConfig optimizeSekarang;

	protected Integer semesterPendek = null;
	protected Boolean merupakanPraPerkuliahan = false;

	// protected Konfigurasi konfigurasi;
	protected MyToolbarbuttonConfig aktif;
	protected MyToolbarbuttonConfig pengecualian;
	protected MyToolbarbuttonConfig pengecualianAdmin;

	protected MyTabConfig tabUploadDanDownloadNilai;
	protected MyTabConfig tabUploadDanDownloadNilaiFormatEpsbed;
	protected Tabpanel tabpanelUploadDanDownloadNilai;
	protected Tabpanel tabpanelUploadDanDownloadNilaiFormatEpsbed;

	protected Tabpanel dashboardStatistikPenilaian;
	protected Tabpanel dashboardPenilaianMahasiswa;
	protected Tabpanel dashboardGradePenilaianMahasiswa;
	protected Tabpanel dashboardRekapNilaiMahasiswa;
	protected Tabpanel dashboardDataNilai;
	protected Tabpanel dashboardDataNilaiPerTahunAngkatan;
	protected Tabpanel dashboardDataNilaiIPKPerTahunAngkatan;
	protected Tabpanel dashboardPenilaianOlehDosenPerDosen;
	protected Tabpanel nilaiSp;

	protected Tabpanel praPerkuliahanTab;

	protected Integer ekstrakurikuler = null;

	private boolean edit = false;

	protected Tabpanel jenis;

	public void onJenis(Event event) {
		// Master Jenis Evaluasi mengendalikan pilihan "Jenis Penilaian" pada setiap
		// komponen nilai. CRUD ini hanya boleh dimuat oleh administrator, bukan sekadar
		// disembunyikan lewat tab, agar event yang dipanggil langsung tetap aman.
		if (!Common.getApakahAdmin()) {
			return;
		}

		if (jenis.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(jenis);
			include.setSrc("/pages/master/jenis_evaluasi.zul");
		}
	}

	protected Tabpanel nilaiHurufTab;

	/**
	 * Memuat halaman "Nilai Huruf" (/pages/master/nilai_huruf.zul) ke dalam tab-nya secara lazy
	 * saat pertama kali diklik. Tab hanya tampil untuk pengguna NON-dosen & NON-mahasiswa
	 * (admin/staf) - pengaturan visibilitas ada di doAfterCompose.
	 */
	public void onNilaiHuruf(Event event) {

		if (nilaiHurufTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(nilaiHurufTab);
			include.setSrc("/pages/master/nilai_huruf.zul");
		}
	}

	protected Tabpanel pengajuan;

	public void onPengajuan(Event event) {

		if (pengajuan.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(pengajuan);
			include.setSrc("/pages/master/pengecualian_jadwal_penilaian_dosen.zul");
		}
	}

	protected Tabpanel statistik;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardNilaiMahasiswa include = new DashboardNilaiMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik Nilai Mahasiswa",
				"Distribusi dan tren nilai mahasiswa seluruh mata kuliah.");
		}
	}

	protected Tabpanel history;

	public void onHistory(Event event) throws Exception {

		if (history.getChildren().size() == 0) {
			RevisiHistoryDetailPerkuliahanHelper include = new RevisiHistoryDetailPerkuliahanHelper();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(history);
		}
	}

	protected Tabpanel jadwalRemedial;

	public void onJadwalRemedial(Event event) {

		if (jadwalRemedial.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(jadwalRemedial);
			include.setSrc("/pages/master/penilaian_remedial.zul");
		}
	}

	protected Tabpanel ekstrakurikulerTab;

	public void onEkstrakurikuler(Event event) {

		if (ekstrakurikulerTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(ekstrakurikulerTab);
			include.setSrc("/pages/master/penilaian_ekstrakurikuler.zul");
		}
	}

	public void onNilaiSp(Event event) {

		if (nilaiSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(nilaiSp);
			include.setSrc("/pages/master/penilaian_sp.zul");
		}
	}

	public void onPraPerkuliahan(Event event) {

		if (praPerkuliahanTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(praPerkuliahanTab);
			include.setSrc("/pages/master/penilaian_pra_perkuliahan.zul");
		}
	}

	public void onDashboardStatistikPenilaian(Event event) throws Exception {

		if (dashboardStatistikPenilaian.getChildren().size() == 0) {
			DashboardStatistikPenilaian laporan = new DashboardStatistikPenilaian();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardStatistikPenilaian,
				"Analisis Penilaian",
				"Gambaran menyeluruh nilai dan hasil penilaian mahasiswa.");
		}
	}

	public void onLaporanPenilaianOlehDosenPerDosen(Event event) throws Exception {

		if (dashboardPenilaianOlehDosenPerDosen.getChildren().size() == 0) {
			LaporanPenilaianOlehDosenPerDosenWindow laporan = new LaporanPenilaianOlehDosenPerDosenWindow();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(dashboardPenilaianOlehDosenPerDosen);
		}
	}

	public void onDashboardPenilaianMahasiswa(Event event) throws Exception {

		if (dashboardPenilaianMahasiswa.getChildren().size() == 0) {
			DashboardPenilaianMahasiswa laporan = new DashboardPenilaianMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardPenilaianMahasiswa,
				"Penilaian Mahasiswa",
				"Rekap hasil penilaian yang telah diinput dosen untuk tiap mahasiswa.");
		}
	}

	public void onDashboardGradePenilaianMahasiswa(Event event) throws Exception {

		if (dashboardGradePenilaianMahasiswa.getChildren().size() == 0) {
			DashboardGradePenilaianMahasiswa laporan = new DashboardGradePenilaianMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardGradePenilaianMahasiswa,
				"Grade Nilai Mahasiswa",
				"Distribusi grade A/B/C/D/E yang diperoleh mahasiswa per mata kuliah.");
		}
	}

	public void onDashboardRekapNilaiMahasiswa(Event event) throws Exception {

		if (dashboardRekapNilaiMahasiswa.getChildren().size() == 0) {
			DashboardRekapNilaiMahasiswa laporan = new DashboardRekapNilaiMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardRekapNilaiMahasiswa,
				"Rekap Nilai Mahasiswa",
				"Ringkasan nilai akhir mahasiswa beserta perbandingan antar semester.");
		}
	}

	public void onDashboardDataNilai(Event event) throws Exception {

		if (dashboardDataNilai.getChildren().size() == 0) {

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(dashboardDataNilai);

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tab1 = new MyTabConfig("Data Nilai");
			tab1.setParent(tabs);

			MyTabConfig tab2 = new MyTabConfig("Data IPK");
			tab2.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);

			Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setVisible(tab2.isVisible());
			tabpanel2.setParent(tabpanels);

			DashboardDataNilaiMahasiswa laporan = new DashboardDataNilaiMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, tabpanel1,
				"Data Nilai Mahasiswa", "Distribusi dan tren nilai mahasiswa per mata kuliah dan semester.");

			DashboardDataNilaiDanIPKMahasiswa laporan1 = new DashboardDataNilaiDanIPKMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan1, tabpanel2,
				"Data Nilai & IPK", "Sebaran nilai dan indeks prestasi kumulatif mahasiswa.");
		}
	}

	public void onDashboardDataNilaiPerTahunAngkatan(Event event) throws Exception {

		if (dashboardDataNilaiPerTahunAngkatan.getChildren().size() == 0) {
			DashboardDataNilaiMahasiswaPerTahunAngkatan laporan = new DashboardDataNilaiMahasiswaPerTahunAngkatan();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardDataNilaiPerTahunAngkatan,
				"Nilai per Tahun Angkatan", "Perbandingan rata-rata nilai mahasiswa antar tahun angkatan.");
		}
	}

	public void onDashboardDataNilaiIPKPerTahunAngkatan(Event event) throws Exception {

		if (dashboardDataNilaiIPKPerTahunAngkatan.getChildren().size() == 0) {
			DashboardDataNilaiIPKMahasiswaPerTahunAngkatan laporan = new DashboardDataNilaiIPKMahasiswaPerTahunAngkatan();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardDataNilaiIPKPerTahunAngkatan,
				"IPK per Tahun Angkatan", "Tren indeks prestasi kumulatif mahasiswa dari tahun ke tahun.");
		}
	}

	public void onUploadDanDownloadNilai(Event event) {

		if (tabpanelUploadDanDownloadNilai.getChildren().size() == 0) {

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(tabpanelUploadDanDownloadNilai);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tab1 = new MyTabConfig("Download Nilai");
			tab1.setParent(tabs);

			MyTabConfig tab2 = new MyTabConfig("Upload Nilai");
			tab2.setParent(tabs);

			tab2.setVisible(Common.bolehKonfigurasi("aktifkan_upload_nilai_pada_menu_download_dan_upload_nilai_konversi"));

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);

			Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setVisible(tab2.isVisible());
			tabpanel2.setParent(tabpanels);

			DownloadNilaiMahasiswa laporan = new DownloadNilaiMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(tabpanel1);

			UploadNilaiMahasiswa upload = new UploadNilaiMahasiswa();
			upload.setHeight("100%");
			upload.setWidth("100%");
			upload.setParent(tabpanel2);

			// Iframe iframe = new Iframe();
			// iframe.setHeight("100%");
			// iframe.setWidth("100%");
			// iframe.setParent(tabpanel2);
			// iframe.setSrc("/pages/master/epsbed/master_mahasiswa.zul");
		}
	}

	public void onUploadDanDownloadNilaiFormatEpsbed(Event event) {

		if (tabpanelUploadDanDownloadNilaiFormatEpsbed.getChildren().size() == 0) {

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(tabpanelUploadDanDownloadNilaiFormatEpsbed);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tab1 = new MyTabConfig("Download Nilai");
			tab1.setParent(tabs);

			MyTabConfig tab2 = new MyTabConfig("Upload Nilai");
			tab2.setParent(tabs);

			tab2.setVisible(Common.bolehKonfigurasi("aktifkan_upload_nilai_pada_menu_download_dan_upload_nilai"));

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);

			Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setVisible(tab2.isVisible());
			tabpanel2.setParent(tabpanels);

			DownloadNilaiMahasiswaFormatEpsbed laporan = new DownloadNilaiMahasiswaFormatEpsbed();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(tabpanel1);

			UploadNilaiMahasiswaFormatEpsbed upload = new UploadNilaiMahasiswaFormatEpsbed();
			upload.setHeight("100%");
			upload.setWidth("100%");
			upload.setParent(tabpanel2);

		}
	}

	public void onPengecualianJadwalPenilaianDosen(Event event) throws Exception {
		PengecualianJadwalPenilaianDosenHelper pengecualianJadwalPenilaianDosenHelper = new PengecualianJadwalPenilaianDosenHelper();
		pengecualianJadwalPenilaianDosenHelper.display();
	}

	public void onPengecualianJadwalPenilaianTbmuser(Event event) throws Exception {
		PengecualianJadwalPenilaianAdminHelper pengecualianJadwalPenilaianAdminHelper = new PengecualianJadwalPenilaianAdminHelper();
		pengecualianJadwalPenilaianAdminHelper.display();
	}

	protected Dosen dosen;
	protected Boolean aktifPenilaian = false;
	protected Tabs tabsNilai;
	private North mynorth;
	private Combobox ta;
	private Combobox smt;
	private Combobox hari;
	private Textbox keyword;

	private List<Perkuliahan> perkuliahans = null;
	private PerguruanTinggi perguruanTinggi;

	private static double persenVeridikasi = 0.0;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		String path = page.getRequestPath();
		System.out.println("path => " + path);
		if (path == null || !path.contains("common")) {
			Common.doCheckSecurity();
		}
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings({ "unchecked", "unused" })
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		if (Common.bolehKonfigurasi("hanya_dosen_yg_boleh_entry_nilai", Konfigurasi.TIDAK_AKTIF)) {
			if (tbmuser != null && tbmuser.ambilDosen() == null) {
				edit = false;
			}
		}

		tbmuser = Common.getCurrentUser();
		// Tombol "Aktif/Non-aktifkan Penilaian" hanya tampil untuk admin.
		if (toggleAktifPenilaian != null) {
			toggleAktifPenilaian.setVisible(Common.getApakahAdmin());
		}
		dosen = tbmuser == null ? null : tbmuser.ambilDosen();

		MyToolbarbuttonConfig kbb = new MyToolbarbuttonConfig("KBM", "/img/group.gif");
		kbb.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PertemuanAction.dataKBM.onEvent(new Event("", null, perkuliahans));
			}
		});

		MyToolbarbuttonConfig rekap = new MyToolbarbuttonConfig("Rekap Pemb.", "/img/group.gif");
		rekap.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PertemuanAction.datarekapPembelajaran.onEvent(new Event("", null, perkuliahans));
			}
		});

		if (mynorth != null && tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {

			Common.clear(mynorth);

			ta = new Combobox();
			smt = new Combobox();
			hari = new Combobox();

			Comboitem comboitem;
			for (String h : Common.haris) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(h);
				comboitem.setValue(h);
				hari.appendChild(comboitem);
			}
			comboitem = new Comboitem();
			comboitem.setLabel("=hari=");
			comboitem.setValue(null);
			hari.appendChild(comboitem);
			hari.setReadonly(true);
			hari.setSelectedItem(comboitem);

			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			smt.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			smt.appendChild(comboitem);

			MyComboitemConfig comboitemSp = new MyComboitemConfig();
			comboitemSp.setLabel(Perkuliahan.SP);
			comboitemSp.setValue(Perkuliahan.SP);
			smt.appendChild(comboitemSp);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("=smt=");
			comboitem.setValue(null);
			smt.appendChild(comboitem);
			smt.setReadonly(true);

			if (merupakanPraPerkuliahan) {
				smt.setVisible(false);
			}

			if (semesterPendek != null) {
				smt.setSelectedItem(comboitemSp);
				smt.setDisabled(true);
			} else {
				smt.setSelectedItem(comboitem);
			}
			Common.generateTahunAjaranDanSemua(ta);
			Common.selectComboItem(ta, Common.getCurrentTahunAkademik());
			keyword = new Textbox();
			keyword.setCols(Common.isMobile() ? 5 : 10);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(new Event("cari"));
				}
			};

			ta.addEventListener("onChange", eventListener);
			smt.addEventListener("onChange", eventListener);
			keyword.addEventListener("onOK", eventListener);
			hari.addEventListener("onChange", eventListener);

			int jumlahDataDalamSatuHalamanElearning = 10;
			Common.initPagingCustom(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			}, jumlahDataDalamSatuHalamanElearning);

			Toolbar toolbar = new Toolbar();
			if (!Common.isMobile())
				toolbar.appendChild(new MyLabelBoldConfig("TA :"));
			else
				ta.setCols(3);
			toolbar.appendChild(ta);

			if (!Common.isMobile() && !merupakanPraPerkuliahan) {
				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelBoldConfig("Smt :"));
			} else
				smt.setCols(2);
			toolbar.appendChild(smt);
			if (!Common.isMobile()) {
				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelBoldConfig("Hari :"));
			} else
				hari.setCols(3);
			toolbar.appendChild(hari);

			if (!Common.isMobile()) {
				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelBoldConfig("Dosen/Mk :"));
			}
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
			toolbar.appendChild(keyword);
			toolbar.appendChild(button);
			toolbar.appendChild(refresh);
			toolbar.appendChild(new Space());
			toolbar.appendChild(kbb);
			toolbar.appendChild(rekap);
			toolbar.setParent(mynorth);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});

			refresh.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(new Event("cari"));
				}
			});

		} else {

			searchJenisSemester.setReadonly(true);
			searchTahunAjaran.setReadonly(true);

			searchkurikulum.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			if (searchtanpakelas != null) {
				searchtanpakelas.setVisible(
						Common.bolehKonfigurasi("tampilkan_search_kelas_di_penjadwalan", Konfigurasi.TIDAK_AKTIF));
			}

			if (searchasisten != null) {

				if (tbmuser != null && tbmuser.getMahasiswa() != null) {
					searchasisten.setAttribute("mahasiswa", tbmuser.getMahasiswa());
					searchasisten.setValue(tbmuser.getMahasiswa().getNama());
					searchasisten.setDisabled(true);

					if (tabsNilai != null) {
						List<Tab> tabs = tabsNilai.getChildren();
						for (Tab tab : tabs) {
							tab.setVisible(tab.getLabel().trim().equalsIgnoreCase("Nilai")
									|| tab.getLabel().trim().equalsIgnoreCase("Nilai SP"));
						}
					}
				}

				searchasisten.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}

			searchmatakuliah.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});
			searchdosen.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			if (tbmuser.getMahasiswa() == null) {
				if (dashboardPenilaianOlehDosenPerDosen != null) {
					Boolean adaProsesVerifikasiNilai = Common.bolehKonfigurasi("ada_proses_verifikasi_penilaian_kepada_dosen", Konfigurasi.TIDAK_AKTIF);
					dashboardPenilaianOlehDosenPerDosen.getLinkedTab().setVisible(adaProsesVerifikasiNilai);
					dashboardPenilaianOlehDosenPerDosen.setVisible(adaProsesVerifikasiNilai);
				}
			}

			searchruang.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semester Pendek (SP)");
			comboitem.setValue(Perkuliahan.SP);
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchJenisSemester.appendChild(comboitem);

			// Default Jenis Semester = SEMESTER SAAT INI (abaikan konfigurasi
			// 'pilihan_semester_di_perkuliahan_dibuat_default_semua_aja'; selalu pakai isNowSemensterGanjil()).
			Common.selectComboItem(searchJenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(searchsemester);
					searchsemester.setSelectedItem(null);

					if (searchJenisSemester.getSelectedItem() == null) {
						return;
					}
					if (searchJenisSemester.getSelectedItem().getValue() == null
							|| Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue())) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
						searchsemester.appendChild(comboitem);
						for (int i = 1; i < 30; i++) {
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					} else {
						Boolean genap = searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
						searchsemester.appendChild(comboitem);
						if (genap) {
							for (int i : Common.genap) {
								if (i == 0)
									continue;
								comboitem = new MyComboitemConfig();
								comboitem.setLabel(i + "");
								comboitem.setValue(i);
								searchsemester.appendChild(comboitem);
							}
						} else {
							for (int i : Common.ganjil) {
								comboitem = new MyComboitemConfig();
								comboitem.setLabel(i + "");
								comboitem.setValue(i);
								searchsemester.appendChild(comboitem);
							}
						}
					}

					searchsemester.setSelectedIndex(0);
					searchsemester.setReadonly(true);
				}
			};

			searchJenisSemester.addEventListener("onChange", eventListener);
			eventListener.onEvent(null);

			Common.generateTahunAjaranDanSemua(searchTahunAjaran);
			Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

			searchmasaperkulaiahan.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			Common.initPrograms(searchprogram);

			for (String h : Common.haris) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(h);
				comboitem.setValue(h);
				searchhari.appendChild(comboitem);
			}

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchhari.appendChild(comboitem);
			searchhari.setSelectedItem(comboitem);
			searchhari.setReadonly(true);

			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
				// Common.selectComboItem(searchdosen, dosen);
				searchdosen.setValue(dosen.getNama());
				searchdosen.setAttribute("myValue", dosen);
				searchdosen.setDisabled(true);
			}

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			boolean adminLainBoleh = false;
			String admLain = Common.getKonfigurasi("admin_lain_bisa_menambah_pengecualian_penilaian", "").getNilai();

			String[] aa = admLain.split(";");
			for (String a : aa) {
				try {
					adminLainBoleh = a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
					if (adminLainBoleh) {
						break;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			// Visibilitas tab "Pengajuan/Jenis" (= halaman PENGECUALIAN JADWAL PENILAIAN DOSEN,
			// yaitu fitur agar dosen dapat memperbaiki/melengkapi nilai setelah jadwal penilaian
			// ditutup) DIHITUNG LANGSUNG DARI ROLE. Sebelumnya nilainya diambil dari
			// pengecualian.isVisible(); padahal 'pengecualian' adalah komponen toolbar yang TIDAK ADA
			// di penilaian.zul (selalu null) -> pengecualianVisible selalu false -> tab tidak pernah
			// muncul untuk siapa pun (termasuk ADMINISTRATOR/DIKJAR). Dihitung null-safe dari role.
			boolean pengecualianVisible = adminLainBoleh
					|| (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
							&& (Tbmrole.ADMINISTRATOR.equalsIgnoreCase(tbmuser.hakAkses().getRoleId())
									|| Tbmrole.DIKJAR.equalsIgnoreCase(tbmuser.hakAkses().getRoleId())));
			if (pengecualian != null) {
				pengecualian.setVisible(pengecualianVisible);
			}
			if (pengajuan != null) {
				pengajuan.setVisible(pengecualianVisible);
				if (pengajuan.getLinkedTab() != null)
					pengajuan.getLinkedTab().setVisible(pengecualianVisible);
			}

			// Tab "Jenis Penilaian" adalah CRUD master jenis_evaluasi yang menjadi sumber
			// dropdown jenis penilaian pada Format Nilai. Tampil khusus administrator dan
			// diletakkan setelah Nilai Huruf pada penilaian.zul.
			if (jenis != null) {
				boolean bolehKelolaJenisPenilaian = Common.getApakahAdmin();
				jenis.setVisible(bolehKelolaJenisPenilaian);
				if (jenis.getLinkedTab() != null)
					jenis.getLinkedTab().setVisible(bolehKelolaJenisPenilaian);
			}

			// Tab "Nilai Huruf": tampil HANYA untuk pengguna NON-dosen & NON-mahasiswa (admin/staf).
			if (nilaiHurufTab != null) {
				boolean bolehNilaiHuruf = tbmuser != null && tbmuser.ambilDosen() == null
						&& tbmuser.getMahasiswa() == null;
				nilaiHurufTab.setVisible(bolehNilaiHuruf);
				if (nilaiHurufTab.getLinkedTab() != null) {
					nilaiHurufTab.getLinkedTab().setVisible(bolehNilaiHuruf);
				}
			}

			if (pengecualianAdmin != null)
				pengecualianAdmin.setVisible(adminLainBoleh || (tbmuser != null && tbmuser != null
					&& tbmuser.hakAkses() != null && tbmuser.hakAkses() != null && tbmuser != null
					&& tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
					&& (tbmuser != null && tbmuser.hakAkses() != null
							&& Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)
							|| Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.DIKJAR))));

			if (aktif != null)
				aktif.setVisible(false && tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
					&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
					&& tbmuser.hakAkses().getRoleId() != null
					&& (tbmuser != null && tbmuser.hakAkses() != null
							&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)
							|| Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.DIKJAR)));

			if (tabUploadDanDownloadNilai != null) {
				tabUploadDanDownloadNilai.setVisible(tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
						&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
						&& tbmuser.hakAkses().getRoleId() != null && (tbmuser != null && tbmuser.hakAkses() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));

				tabUploadDanDownloadNilai
						.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
			}

			if (tabpanelUploadDanDownloadNilai != null) {
				tabpanelUploadDanDownloadNilai.setVisible(tbmuser != null && tbmuser != null
						&& tbmuser.hakAkses() != null && tbmuser.hakAkses() != null && tbmuser != null
						&& tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
						&& (tbmuser != null && tbmuser.hakAkses() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));

				tabpanelUploadDanDownloadNilai
						.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
			}

			if (tabUploadDanDownloadNilaiFormatEpsbed != null) {
				tabUploadDanDownloadNilaiFormatEpsbed
						.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
			}

			if (tabpanelUploadDanDownloadNilaiFormatEpsbed != null) {
				tabpanelUploadDanDownloadNilaiFormatEpsbed
						.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
			}

			// optimize/optimizeSekarang/aktif adalah komponen autowire yang bisa null bila
			// id tidak ada di ZUL varian tertentu → guard agar tidak NPE.
			boolean aktifVisible = aktif != null && aktif.isVisible();
			if (optimize != null)
				optimize.setVisible(aktifVisible);
			if (optimizeSekarang != null)
				optimizeSekarang.setVisible(aktifVisible);

			if (optimize != null)
				optimize.setVisible(false);
			if (optimizeSekarang != null)
				optimizeSekarang.setVisible(false);

			Common.initPaging(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);

				}
			});

			if (ConstantValues.aktifkanTahapanKurikulum) {
				searchTahap = new Combobox();
				if (ConstantValues.jumlahTahapan.isEmpty()) {
					ConstantValues.initJumlahTahapan();
				}

				MyComboitemConfig comboitemSemua = new MyComboitemConfig("Semua tahap");
				comboitemSemua.setValue(-1);
				searchTahap.appendChild(comboitemSemua);

				for (int i = 1; i <= 15; i++) {
					comboitem = new MyComboitemConfig("Tahap " + i);
					comboitem.setValue(i);
					searchTahap.appendChild(comboitem);
				}
				comboitem = new MyComboitemConfig("Tanpa tahap");
				comboitem.setValue(null);
				searchTahap.appendChild(comboitem);

				searchTahap.setSelectedItem(comboitemSemua);
				Common.appendKeToolbar(searchTahap, pengecualianAdmin, comp);
				searchTahap.setReadonly(true);
				searchTahap.setWidth("100px");
				searchTahap.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}
			if (searchasisten != null && searchasisten.getAttribute("mahasiswa") != null) {
				searchfakultas.setDisabled(false);
				searchjurusan.setDisabled(false);

				searchfakultas.setSelectedIndex(-1);
				searchjurusan.setSelectedIndex(-1);
			}
			// if (semesterPendek == null) {
			// onSyncronisasiHanyaYangBelumDapatNilai(null);
			// } else {
			// onSearchDefault(null);
			// }

			if (tbmuser.getMahasiswa() == null) {
				boolean adaProsesVerifikasiNilai = Common.bolehKonfigurasi("ada_proses_verifikasi_penilaian_kepada_dosen", Konfigurasi.TIDAK_AKTIF);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
						persenVeridikasi > 0.1 && persenVeridikasi < 99.5
								? "Masih proses verifikasi " + Common.numberFormat.get().format(persenVeridikasi) + "%"
								: "Verifikasi Semua",
						"/img/svg/check2.svg");
				button.setVisible(dosen == null && adaProsesVerifikasiNilai
						&& Common.bolehKonfigurasi("aktifkan_tombol_verifikasi_semua_baru"));
				button.setDisabled(persenVeridikasi > 0.1 && persenVeridikasi < 99.5);
				Common.appendKeToolbar(button, pengecualianAdmin, comp);
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin melakukan verifikasi semua nilai ini ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {

											final Label label = new Label(
													ais.common.Common.getBahasaConfig("Proses verifikasi nilai sedang berlangsung, harap menunggu.."));

											new Thread(new Runnable() {

												@Override
												public void run() {
													try {
													persenVeridikasi = 0.0;
													List<Perkuliahan> perkuliahans = ConstantValues
															.simpleList(initCriteria(true), Perkuliahan.class);
													int size = perkuliahans.size();
													int iverifikasi = 0;
													for (Perkuliahan perkuliahan : perkuliahans) {
														iverifikasi++;
														try {
															persenVeridikasi = iverifikasi * 100.0 / size;
															if (label != null) {
																label.setValue(
																		Common.numberFormat.get().format(persenVeridikasi)
																				+ "% .. Verifikasi nilai "
																				+ perkuliahan.infoSimple());

															}
															Session session1 = HibernateUtil.currentNativeSession();
															List<FormatNilai> formatNilais = Common
																	.getFormatNilais(session1, perkuliahan);
															ais.common.Common.closeOpenedSession(session1);

															Collection<Long> detailperkuliahans = perkuliahan
																	.ambilDetailperkuliahan();
															for (Long detailperkuliahanid : detailperkuliahans) {
																Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
																		.ambilData(Detailperkuliahan.class,
																				detailperkuliahanid.toString());
																if (detailperkuliahan != null) {
																	try {
																		Session session = HibernateUtil
																				.currentNativeSession();
																		session.refresh(detailperkuliahan);
																		boolean adayangBelumVerified = false;
																		for (FormatNilai formatNilai : formatNilais) {
																			Double jumlah = detailperkuliahan
																					.retreiveDetailNilaiBelumVerify(
																							formatNilai);
																			if (jumlah < 0.01) {
																				adayangBelumVerified = true;
																			} else {
																				detailperkuliahan.populateDetailNilai(
																						formatNilai, null, jumlah, true,
																						perkuliahan
																								.getSembunyikanNilaiJikaBelumDiverifikasi(),
																						tbmuser);
																			}
																		}

																		detailperkuliahan.setVerify(adayangBelumVerified
																				? Detailperkuliahan.NOT_VERIFIED
																				: Detailperkuliahan.VERIFIED);
																		detailperkuliahan
																				.setVerifikator(tbmuser.getUserNama());
																		detailperkuliahan.setWaktuVerifikasi(
																				ais.ui.util.WaktuUtil.getDate());
																		session.getTransaction().begin();
																		Common.refreshUpdate(session,
																				detailperkuliahan);
																		session.getTransaction().commit();

																		// session.disconnect();
																		if (session.isOpen()) {
																			session.disconnect();
																			session.close();
																		}

																	} catch (Exception e) {
																		ais.common.Common.tampilErrorJikaAdmin(e);
																	}
																	HibernateUtil.closeSession();
																}
															}
															detailperkuliahans = null;
														} catch (Exception e) {
															ais.common.Common.tampilErrorJikaAdmin(e);
														}

													}
													label.setValue("");
																									} finally {
														ais.database.hibernate.HibernateUtil.closeSession();
													}
												}
											}).start();

											final Timer timer = new Timer(500);
											timer.setParent(
													ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
											timer.setRepeats(true);
											timer.addEventListener("onTimer", new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													Clients.showBusy(label.getValue());
													if (label.getValue().isEmpty()) {
														Clients.clearBusy();
														MyMessageboxConfig.show("Verifikasi nilai telah selesai",
																"Pemberitahuan", MyMessageboxConfig.OK,
																MyMessageboxConfig.INFORMATION);
														timer.detach();
													}

												}
											});
											timer.start();

										}

									}
								});
					}

				});
			}

			boolean isSinkronSemuaAktif = Common.bolehKonfigurasi("aktifkan_tombol_sinkronkan_semua");
			// pengecualianAdmin (autowire) bisa null di ZUL varian tertentu → jangan deref
			// .getParent() langsung. Pakai parent-nya bila ada, jika tidak fallback ke comp.
			org.zkoss.zk.ui.Component parentSinkron = (pengecualianAdmin != null
					&& pengecualianAdmin.getParent() != null) ? pengecualianAdmin.getParent() : comp;
			if (parentSinkron != null) {
				SyncHelper.buatTombolSinkronisasi(parentSinkron, "Singkronkan", "/img/svg/check2.svg",
						isSinkronSemuaAktif, "Proses singkronisasi perkuliahan", new SyncHelper.SyncDataProvider() {
							@Override
							public List<Perkuliahan> getData() throws Exception {
								return ConstantValues.simpleList(initCriteria(true), Perkuliahan.class);
							}
						}, new SyncHelper.SyncAction() {
							@Override
							public void execute(Perkuliahan p, Session session) throws Exception {
								p.singkronkan(session);
							}
						});
			}

			MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Hitung Ulang", "/img/svg/check2.svg");
			cetakSksDosen.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_hitung_ulang_semua"));
			Common.appendKeToolbar(cetakSksDosen, pengecualianAdmin, comp);
			cetakSksDosen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses Hitung Ulang perkuliahan"));

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {
									List<Perkuliahan> perkuliahans = ConstantValues.simpleList(initCriteria(true),
											Perkuliahan.class);

									int i = 0;
									int size = perkuliahans.size();
									for (Perkuliahan perkuliahan : perkuliahans) {
										Session session = HibernateUtil.currentNativeSession();
										List<FormatNilai> formatNilais = perkuliahan.ambilFormatNilai(session);
										HibernateUtil.closeSession();

										for (Long detailperkuliahanid : perkuliahan.ambilDetailperkuliahan()) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
													.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
											if (detailperkuliahan != null) {
												detailperkuliahan.reloadFormatNilai(formatNilais,
														perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi());
												Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);

												Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
														? detailperkuliahan.getPerkuliahan().getMatakuliah()
														: detailperkuliahan.getMatakuliahKonversi();

												NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
														detailperkuliahan.getMahasiswa().getTahunangkatan(),
														detailperkuliahan.getMahasiswa().getJurusan(),
														detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
														detailperkuliahan.getTahunAkademik(),
														detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
																: Perkuliahan.GANJIL,
														matakuliah == null ? "" : matakuliah.getKode(),
														matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
												detailperkuliahan.setTotalIP(
														nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
												detailperkuliahan.setTotalNilai(total);
												detailperkuliahan.setNilaiHuruf(
														nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
												detailperkuliahan
														.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

												Double totalSementara = detailperkuliahan
														.hitungTotalNilaiSementara(true, formatNilais);
												nilaiHuruf = Common.getNilaiHuruf(totalSementara,
														detailperkuliahan.getMahasiswa().getTahunangkatan(),
														detailperkuliahan.getMahasiswa().getJurusan(),
														detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
														detailperkuliahan.getTahunAkademik(),
														detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
																: Perkuliahan.GANJIL,
														matakuliah == null ? "" : matakuliah.getKode(),
														matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

												detailperkuliahan.setTotalNilaiSementara(totalSementara);
												detailperkuliahan.setNilaiHurufSementara(
														nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
												detailperkuliahan.setTotalIPSementara(
														nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

												Session s = HibernateUtil.currentNativeSession();
												s.getTransaction().begin();
												Common.refreshUpdate(s, detailperkuliahan);
												s.getTransaction().commit();
												HibernateUtil.closeSession();
											}
										}

										if (label != null) {
											label.setValue("(" + (Common.numberFormat.get().format(i * 100.0 / size))
													+ " %) Hitung Ulang data perkuliahan " + perkuliahan + " ..");
										}
										i++;
									}
									label.setValue("");

																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

							final Timer timer = new Timer(500);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									// System.out.println("process = " +
									// label.getValue());
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										Clients.clearBusy();
										MyMessageboxConfig.show("Hitung Ulang perkuliahan berhasil dilakukan",
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
										timer.detach();
									}

								}
							});
							timer.start();

						}
					});
				}
			});

			Common.appendKeToolbar(kbb, pengecualianAdmin, comp);
			Common.appendKeToolbar(rekap, pengecualianAdmin, comp);

		}
		// Tombol feeder di toolbar (Kirim ke Feeder + Ambil dari Feeder), seperti di perkuliahan.zul.
		// pengecualianAdmin tidak di-wire di penilaian.zul → fallback ke parent tombol "find"
		// (mengikuti pola Common.appendKeToolbar) agar tombol tetap tampil.
		Component feederToolbar = null;
		if (pengecualianAdmin != null) {
			feederToolbar = pengecualianAdmin.getParent();
		}
		if (feederToolbar == null) {
			try {
				Component f = comp.getFellowIfAny("find");
				if (f == null) {
					f = comp.getFellowIfAny("add");
				}
				if (f != null) {
					feederToolbar = f.getParent();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PenilaianAction.java:1388");
			}
		}
		if (feederToolbar != null) {
			PerkuliahanAction.tampilanExportKeFeeder(feederToolbar, tbmuser, this, this);
			PerkuliahanAction.tampilanImportKelasDariFeeder(feederToolbar, tbmuser, searchTahunAjaran,
					searchJenisSemester, searchjurusan, this);
		}
		onSearchDefault(null);

	        FilterLanjutHelper.setup(comp);
}

	// public void onSyncronisasiHanyaYangBelumDapatNilai(Event event) {
	//
	// final Label label = Common.displayLoadBar(new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	// onSearchDefault(null);
	// }
	// });
	// label.setValue("Proses singkronisasi nilai sedang berlangsung, harap
	// menunggu..");
	//
	// new Thread(new Runnable() {
	//
	// @Override
	// public void run() {
	// Common.synNilaiHuruf(label, true);
	// }
	// }).start();
	//
	// }

	@SuppressWarnings("unchecked")
	public void onBuka(Event event) throws Exception {
		DetailperkuliahanForPenilaianHelper detailperkuliahanHelper = new DetailperkuliahanForPenilaianHelper(edit);
		List<Row> rows = grid.getRows().getChildren();
		for (Row row : rows) {
			MyDetail detail = MyDetail.dari(row);
			if (detail == null) {
				continue;
			}
			detail.setOpen(true);
			Common.clear(detail);
			final Perkuliahan perkuliahan = (Perkuliahan) detail.getAttribute("perkuliahan");
			detailperkuliahanHelper.display(perkuliahan, detail, null, null, aktifPenilaian);
		}
	}

	@SuppressWarnings("unchecked")
	public void onTutup(Event event) {
		List<Row> rows = grid.getRows().getChildren();
		for (Row row : rows) {
			MyDetail detail = MyDetail.dari(row);
			if (detail == null) {
				continue;
			}
			detail.setOpen(false);
			Common.clear(detail);
		}
	}

	class PenilaianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Perkuliahan perkuliahan = (Perkuliahan) arg1;
			Perkuliahan kuliyah = perkuliahan.getMerupakan_paralel() && perkuliahan.getPerkuliahan_paralel() != null
					? perkuliahan.getPerkuliahan_paralel()
					: perkuliahan;

			String jenisSemester = perkuliahan.getGanjilGenap();
			String tahunAkademik = perkuliahan.getTahunAjaran();

			final Konfigurasi konfigurasi = CommonPenilaian.getKonfigurasi(tahunAkademik, jenisSemester,
					semesterPendek);

			final MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Ubah Format",
					"/img/svg/edit-box-line.svg");

			final Html label = new ais.ui.util.MyHtml(kuliyah.populateInfoPersetujuan());

			final Html formatNilai = new ais.ui.util.MyHtml("..");
			final Label komentar = new Label();
			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Perkuliahan kuliyah = perkuliahan.getMerupakan_paralel()
							&& perkuliahan.getPerkuliahan_paralel() != null ? perkuliahan.getPerkuliahan_paralel()
									: perkuliahan;

					label.setContent(kuliyah.populateInfoPersetujuan());

					Session session = HibernateUtil.currentSession();
					int jml = ((Number) session.createCriteria(KomentarPerkuliahan.class)
							.setProjection(Projections.rowCount()).add(Restrictions.eq("perkuliahan", kuliyah))
							.uniqueResult()).intValue();
					komentar.setValue(jml == 0 ? "Tidak ada" : Common.numberFormat.get().format(jml) + " komentar");

				}
			};

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setAttribute("perkuliahan", perkuliahan);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (detail.isOpen()) {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								eventListener.onEvent(null);
								DetailperkuliahanForPenilaianHelper detailperkuliahanHelper = new DetailperkuliahanForPenilaianHelper(
										edit);
								detailperkuliahanHelper.display(perkuliahan, detail, eventListener, buttonFormatNilai,
										aktifPenilaian);
							}
						});
					}
				}
			});
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahan);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
				if (perkuliahan.getFeeder() != null && !perkuliahan.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}
			}
			PerkuliahanAction.tampilanExportKeFeederSatuan(vbox, tbmuser, PenilaianAction.this, perkuliahan);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new ais.ui.util.MyHtml(perkuliahan.getMatakuliah().getKode() + " - " + perkuliahan.getMatakuliah().getNama()
					+ (perkuliahan.getMerupakan_paralel() != null && perkuliahan.getMerupakan_paralel()
							? " <font style='font-weight:bold;color:blue;'>(Paralel)</font>"
							: "")
					+ " (" + perkuliahan.getMatakuliah().getSks() + " sks) ").setParent(vbox);
			MatakuliahPrasyaratAction.tampilPrasyarat(vbox, perkuliahan.getMatakuliah());

			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(arg0, perkuliahan, true);

			Vbox myVbox = new Vbox();
			myVbox.setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(myVbox);
			formatNilai.setParent(hbox);
			PembombotanNilai pembombotanNilai = perkuliahan.getPembombotanNilai();
			boolean tampil = !((pembombotanNilai != null
					&& pembombotanNilai.getWajibDitahunAkademikDanSemesterTertentu()
					&& perkuliahan.getTahunAjaran() != null && pembombotanNilai.getTahunAkadmeik() != null
					&& pembombotanNilai.getSemester() != null && perkuliahan.getGanjilGenap() != null
					&& perkuliahan.getTahunAjaran().equalsIgnoreCase(pembombotanNilai.getTahunAkadmeik())
					&& pembombotanNilai.getSemester().equals(perkuliahan.getGanjilGenap())));

			if (!perkuliahan.getSembunyikanFormatPenilaian()) {
				if (tampil) {
					buttonFormatNilai.setParent(hbox);
				}
			}

			if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(),
					perkuliahan.getGanjilGenap())) {
				buttonFormatNilai.setVisible(false);
				tampil = false;
			}

			if (tampil && Common.getApakahAdmin()) {

				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Sembunyikan format nilai");
				checkbox.setStyle("font-size:9px");
				checkbox.setDisabled(!edit);
				checkbox.setChecked(perkuliahan.getSembunyikanFormatPenilaian());
				checkbox.setParent(myVbox);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						perkuliahan.setSembunyikanFormatPenilaian(checkbox.isChecked());
						Common.refreshSaveOrUpdate(perkuliahan);
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});

			}

			RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahan,
					perkuliahan.getSemester()
							+ (perkuliahan.getKelas() == null || perkuliahan.getKelas().equals("") ? ""
									: " " + perkuliahan.getKelas())
							+ " (" + Common.labelJenisSemester(perkuliahan) + ")")
					.setParent(arg0);

			label.setParent(arg0);

			Vbox vbox3 = new Vbox();
			vbox3.setParent(arg0);

			try {

				formatNilai.setContent(PembombotanNilai.tampilkanFormat(kuliyah));

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PenilaianAction.java:1615");
				// TODO: handle exception
			}

			String content;
			if (perkuliahan.getDikunci() != null) {
				content = "<div style='font-size:9px;color:blue;'>Nilai telah dikunci oleh "
						+ perkuliahan.getDikunci().getUserNama() + "</div>";
			} else {
				content = "<div style='font-size:9px;color:red;'>Nilai belum dikunci</div>";
			}
			myVbox.appendChild(new ais.ui.util.MyHtml(content));

			Session session = HibernateUtil.currentSession();
			int jml = ((Number) session.createCriteria(KomentarPerkuliahan.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("perkuliahan", kuliyah)).uniqueResult()).intValue();
			komentar.setValue(jml == 0 ? "Tidak ada" : Common.numberFormat.get().format(jml) + " komentar");

			komentar.setParent(vbox3);

			buttonFormatNilai.setOrient("vertical");
			buttonFormatNilai.addEventListener("onClick", new EventListener() {

				FormatPenilaianHelper formatPenilaianHelper = new FormatPenilaianHelper();

				@Override
				public void onEvent(Event event) throws Exception {

					formatPenilaianHelper.display(perkuliahan, addWindow, new TampilDetailNilaiInterface() {

						@Override
						public void realoadNilai(final Perkuliahan perkuliahan) {

							Common.realoadNilai(perkuliahan, perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											Perkuliahan kuliyah = perkuliahan.getMerupakan_paralel()
													&& perkuliahan.getPerkuliahan_paralel() != null
															? perkuliahan.getPerkuliahan_paralel()
															: perkuliahan;
											formatNilai.setContent(PembombotanNilai.tampilkanFormat(kuliyah));

											DetailperkuliahanForPenilaianHelper detailperkuliahanHelper = new DetailperkuliahanForPenilaianHelper(
													edit);
											detailperkuliahanHelper.display(perkuliahan, detail, eventListener,
													buttonFormatNilai, aktifPenilaian);
										}
									}, null);

						}
					});
				}

			});

			boolean mahasiswaBolehUbah = false;
			if (searchasisten != null && searchasisten.isDisabled()) {
				if (tbmuser != null && tbmuser.getMahasiswa() != null) {
					mahasiswaBolehUbah = perkuliahan.merupakanAsistenNilai(tbmuser.getMahasiswa());
					buttonFormatNilai.setVisible(perkuliahan.getDikunci() == null && mahasiswaBolehUbah);
				}
			} else {
				buttonFormatNilai.setVisible(perkuliahan.getDikunci() == null);
			}

			buttonFormatNilai
					.setDisabled(!edit || (!aktifPenilaian && !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)));
			if (aktifPenilaian) {
				buttonFormatNilai.setDisabled(false);
			}
		}

	}

	protected void initAktivasiPenilaianDosen() throws InterruptedException {
		if (searchJenisSemester.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih salah satu semester !",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.EXCLAMATION);
			aktifPenilaian = false;
			return;
		}
		if (searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null) {
			// MyMessageboxConfig.show("Pilih salah satu tahun akademik !",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.EXCLAMATION);
			aktifPenilaian = false;
			return;
		}

		String akademik = (String) searchTahunAjaran.getSelectedItem().getValue();

		String jenisSemester = (String) searchJenisSemester.getSelectedItem().getValue();
		initAktivasiPenilaianDosen(akademik, jenisSemester);
	}

	protected void initAktivasiPenilaianDosen(String akademik, String jenisSemester) throws InterruptedException {
		dosen = tbmuser == null ? null : tbmuser.ambilDosen();

		aktifPenilaian = Common.checkApakahDosenBolehMenilai(dosen, tbmuser, akademik,
				semesterPendek == null ? jenisSemester : Perkuliahan.SP);

		System.out.println("aktifPenilaian = " + aktifPenilaian);
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return initCriteria(session, order);
	}

	public Criteria initCriteria(Session session, boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select perkuliahan from detailperkuliahan a inner join mahasiswa b on (a.mahasiswa = b.id) where perkuliahan is not null and b.nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nim ilike '%" + searchnim.getValue().trim()
					+ "%' group by perkuliahan)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		if ((searchasisten != null && searchasisten.getAttribute("mahasiswa") != null)
				|| !searchnamaasisten.getValue().trim().isEmpty()) {
			Mahasiswa mahasiswa = (Mahasiswa) searchasisten.getAttribute("mahasiswa");
			String sql = "this_.id in (select perkuliahan from mahasiswa_jadi_asisten a inner join mahasiswa b on (a.mahasiswa=b.id) where perkuliahan is not null and a.aktif=true "
					+ (mahasiswa == null ? "" : "and a.mahasiswa=" + mahasiswa.getId())
					+ (searchnamaasisten.getValue().trim().isEmpty() ? ""
							: "and (b.nama ilike '%" + searchnamaasisten.getValue().trim() + "%' or b.nim ilike '%"
									+ searchnamaasisten.getValue().trim() + "%')")
					+ " group by perkuliahan)";
			criterionMhs = Restrictions.and(criterionMhs, Restrictions.sqlRestriction(sql));
		}

		Criteria criteria = session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(merupakanRemedial ? Restrictions.eq("merupakanRemedial", true)
						: Restrictions.or(Restrictions.isNull("merupakanRemedial"),
								Restrictions.eq("merupakanRemedial", false)))

				.add(searchtanpakelas.isChecked()
						? Restrictions.or(Restrictions.eq("kelas", ""), Restrictions.isNull("kelas"))
						: Restrictions.sqlRestriction("1=1"))
				.add(searchKeterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchKeterangan.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.isNull("perkuliahan_paralel"));

		if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {
			criteria.createAlias("matakuliah", "matakuliah").add(Restrictions.eq("matakuliah.extraKulikuler", true));
		} else {
			criteria.createAlias("matakuliah", "matakuliah")
					.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
							Restrictions.eq("matakuliah.extraKulikuler", false)));
		}

		if (ConstantValues.aktifkanTahapanKurikulum) {
			criteria.createAlias("kurikulumPunyaMatakuliah", "kurikulumPunyaMatakuliah", Criteria.LEFT_JOIN)
					.add((searchTahap != null && searchTahap.getSelectedItem() != null
							&& searchTahap.getSelectedItem().getValue() != null
							&& searchTahap.getSelectedItem().getValue().equals(-1))
									? Restrictions.sqlRestriction("true")
									: (searchTahap != null && searchTahap.getSelectedItem() != null
											&& searchTahap.getSelectedItem().getValue() != null
													? Restrictions.eq("kurikulumPunyaMatakuliah.tahap",
															searchTahap.getSelectedItem().getValue())
													: Restrictions.or(Restrictions.isNull("kurikulumPunyaMatakuliah"),
															Restrictions.isNull("kurikulumPunyaMatakuliah.tahap"))));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		Criterion criterion = Restrictions.sqlRestriction("1=1");
		Dosen dsn = (Dosen) searchdosen.getAttribute("myValue");
		if (dsn != null) {

			criterion = Restrictions.or(Restrictions.eq("dosen1", dsn), Restrictions.eq("dosen2", dsn));

			criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dsn));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dsn));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dsn));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dsn));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dsn));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dsn));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dsn));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dsn));

		}

		Criterion criterionNamaDosn = Restrictions.sqlRestriction("1=1");
		if (!searchnamadsn.getValue().trim().isEmpty()) {
			criteria.createAlias("dosen1", "dosen1", Criteria.LEFT_JOIN)
					.createAlias("dosen2", "dosen2", Criteria.LEFT_JOIN)
					.createAlias("dosen3", "dosen3", Criteria.LEFT_JOIN)
					.createAlias("dosen4", "dosen4", Criteria.LEFT_JOIN)
					.createAlias("dosen5", "dosen5", Criteria.LEFT_JOIN)
					.createAlias("dosen6", "dosen6", Criteria.LEFT_JOIN)
					.createAlias("dosen7", "dosen7", Criteria.LEFT_JOIN)
					.createAlias("dosen8", "dosen8", Criteria.LEFT_JOIN)
					.createAlias("dosen9", "dosen9", Criteria.LEFT_JOIN)
					.createAlias("dosen10", "dosen10", Criteria.LEFT_JOIN);

			criterionNamaDosn = Restrictions.ilike("dosen1.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE);

			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen2.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen3.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen4.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen5.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen6.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen7.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen8.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen9.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen10.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
		}

		criteria

				.add(criterionNamaDosn)

				.add(criterionMhs)

				.add(searchnamamk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("matakuliah.kode", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliah.nama", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add(merupakanPraPerkuliahan || searchsemester.getSelectedItem() == null
						|| searchsemester.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsemester.getSelectedItem().getValue()))

				.add((searchkurikulum == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkurikulum.getAttribute("kurikulum") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kurikulum", searchkurikulum.getAttribute("kurikulum"))))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(Restrictions.ilike("kelas", searchkelas.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchparalel != null && searchparalel.isChecked() ? Restrictions.or(Restrictions.sqlRestriction(
						"this_.id in (select perkuliahan_paralel from perkuliahan where perkuliahan_paralel is not null)"),
						Restrictions.eq("merupakan_paralel", true)) : Restrictions.sqlRestriction("1=1"))

				.add(searchdikunci != null && searchdikunci.isChecked() ? Restrictions.isNotNull("dikunci")
						: Restrictions.sqlRestriction("1=1"))

				.add(searchbelumdikunci != null && searchbelumdikunci.isChecked() ? Restrictions.isNull("dikunci")
						: Restrictions.sqlRestriction("1=1"))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add((searchmasaperkulaiahan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmasaperkulaiahan.getAttribute("masaPerkuliahan") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("masaPerkuliahan", searchmasaperkulaiahan.getAttribute("masaPerkuliahan"))))

				.add(criterion)

				.add((searchmatakuliah == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah"))))

				.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(merupakanPraPerkuliahan || searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null
						|| Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue())
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ganjilGenap", searchJenisSemester.getSelectedItem().getValue()))

				.add(merupakanPraPerkuliahan ? Restrictions.eq("merupakanPraPerkuliahan", true)
						: Restrictions.or(Restrictions.eq("merupakanPraPerkuliahan", false),
								Restrictions.isNull("merupakanPraPerkuliahan")))

				.add(merupakanRemedial ? Restrictions.sqlRestriction("true")
						: (searchJenisSemester.getSelectedItem() != null
								&& Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue()))
										? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
										: semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
												: Restrictions.eq("statusSemesterPendek", semesterPendek))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(merupakanPraPerkuliahan ? Restrictions.sqlRestriction("1=1") : CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		if (perguruanTinggi != null) {
			criteria.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));
		}

		return criteria;
	}

	/**
	 * <b>Khusus admin</b>: aktif/non-aktifkan <b>penilaian</b> (input nilai oleh dosen) untuk
	 * <b>Tahun Akademik</b> &amp; <b>Jenis Smt</b> yang dipilih pada filter. Menoggle konfigurasi
	 * {@link ais.common.CommonPenilaian#getKonfigurasi}. Mendukung <b>Semester Pendek (SP)</b>: bila
	 * filter "Jenis Smt" = "Semester Pendek (SP)" ({@code Perkuliahan.SP}) maka yang ditoggle adalah
	 * konfigurasi {@code PENILAIAN_SP}; untuk Ganjil/Genap konfigurasi {@code PENILAIAN}. "Semua"
	 * (tak menunjuk satu semester) diminta memilih Jenis Smt dahulu.
	 */
	public void onToggleAktifPenilaian(Event event) throws Exception {
		if (!Common.getApakahAdmin()) {
			return;
		}
		String ta = searchTahunAjaran.getSelectedItem() == null ? null
				: (String) searchTahunAjaran.getSelectedItem().getValue();
		if (ta == null || ta.trim().isEmpty()) {
			MyMessageboxConfig.show("Pilih Tahun Akademik pada filter terlebih dahulu.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Object js = searchJenisSemester.getSelectedItem() == null ? null
				: searchJenisSemester.getSelectedItem().getValue();
		String sem;
		Integer semPendek;
		if (Perkuliahan.SP.equals(js)) {
			sem = Perkuliahan.SP;
			semPendek = Perkuliahan.SEMESTER_PENDEK;
		} else if (Perkuliahan.GANJIL.equals(js) || Perkuliahan.GENAP.equals(js)) {
			sem = (String) js;
			semPendek = null;
		} else {
			MyMessageboxConfig.show(
					"Pilih Jenis Smt (Ganjil / Genap / Semester Pendek) pada filter terlebih dahulu — "
							+ "\"Semua\" tidak menunjuk satu semester tertentu.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Konfigurasi konfigurasi = CommonPenilaian.getKonfigurasi(ta, sem, semPendek);
		boolean sekarangAktif = Konfigurasi.AKTIF.equals(konfigurasi.getNilai());
		konfigurasi.setNilai(sekarangAktif ? Konfigurasi.TIDAK_AKTIF : Konfigurasi.AKTIF);
		Common.refreshSaveOrUpdate(konfigurasi);
		if (sekarangAktif) {
			MyMessageboxConfig.showFormat(
					"Dengan hormat, penilaian (input nilai oleh dosen) untuk Tahun Akademik \"{V1}\" semester "
							+ "\"{V2}\" telah berhasil DINONAKTIFKAN. Untuk sementara, dosen tidak dapat menginput "
							+ "atau mengubah nilai pada periode tersebut. Apabila periode ini seharusnya masih terbuka, "
							+ "mohon aktifkan kembali melalui tombol \"Aktif/Non-aktifkan Penilaian\" pada layar ini. "
							+ "Terima kasih atas perhatian Bapak/Ibu.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, sem);
		} else {
			MyMessageboxConfig.showFormat(
					"Dengan hormat, penilaian (input nilai oleh dosen) untuk Tahun Akademik \"{V1}\" semester "
							+ "\"{V2}\" telah berhasil DIAKTIFKAN. Dosen kininya dapat menginput dan mengubah nilai "
							+ "pada periode tersebut. Terima kasih atas perhatian Bapak/Ibu.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, sem);
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(final Event event) {
		final boolean isEventCari = event != null && event.getName() != null
				&& event.getName().equalsIgnoreCase("cari");
		final String tahunAkademik = (ta != null && ta.getSelectedItem() != null
				&& ta.getSelectedItem().getValue() != null) ? ta.getSelectedItem().getValue().toString() : null;
		final String jenisSemester = (smt != null && smt.getSelectedItem() != null
				&& smt.getSelectedItem().getValue() != null) ? smt.getSelectedItem().getValue().toString() : null;
		final String hr = (hari != null && hari.getSelectedItem() != null && hari.getSelectedItem().getValue() != null)
				? hari.getSelectedItem().getValue().toString() : null;
		final String keywordVal = keyword != null && keyword.getValue() != null ? keyword.getValue().trim() : "";
		final int activePage = paging == null ? 0 : paging.getActivePage();
		final int jumlahDataDalamSatuHalamanElearning = 10;
		final boolean isDosenArea = mynorth != null && tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen");

		try {
			if (isDosenArea) {
				if (isEventCari) {
					try {
						dosen.reInitPerkuliahan(HibernateUtil.currentSession());
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				Object[] objects = dosen.ambilPerkuliahanDanParalel(HibernateUtil.currentSession(), tahunAkademik,
						jenisSemester, hr, keywordVal, "", merupakanPraPerkuliahan, ekstrakurikuler, true,
						merupakanRemedial, false, true, true, true, true, true, true, true, true, true,
						TampilanELearningAction.PERKULIAHAN, jumlahDataDalamSatuHalamanElearning * activePage,
						jumlahDataDalamSatuHalamanElearning);
				perkuliahans = (List<Perkuliahan>) objects[0];
				int totalSize = (Integer) objects[1];
				initAktivasiPenilaianDosen(tahunAkademik, jenisSemester);
				if (paging != null) {
					paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
					paging.setMold("os");
					paging.setTotalSize(totalSize);
					paging.setVisible(totalSize > jumlahDataDalamSatuHalamanElearning);
					try {
						if (paging.getParent() instanceof South) {
							((South) paging.getParent()).setHeight(paging.isVisible() ? "30px" : "0px");
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			} else {
				initAktivasiPenilaianDosen();
				Common.initPaging(initCriteria(false), paging);
				perkuliahans = ConstantValues.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
								.setFirstResult(Common.ROWS_COUNT_ON_PAGE * activePage),
						Perkuliahan.class);
			}
			if (grid != null) {
				ListModel strset = new SimpleListModel(
						perkuliahans != null ? perkuliahans : new ArrayList<Perkuliahan>());
				grid.setRowRenderer(new PenilaianRenderer());
				grid.setModelCheckMobile(strset);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
