package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardKomentarMahasiswaKRS;
import ais.action.master.dashboard.admin.DashboardKrsMahasiswa;
import ais.action.master.dashboard.admin.DashboardMaksimakKrsMahasiswa;
import ais.action.master.dashboard.admin.DashboardPersetujuanKRSMahasiswa;
import ais.action.master.dashboard.admin.DashboardRekapPengambilanKRSMahasiswa;
import ais.action.master.dashboard.admin.DashboardSKSDanIPKMahasiswa;
import ais.action.master.dashboard.admin.DashboardStatistikPengambilanKRSMahasiswa;
import ais.action.master.dashboard.admin.DashboardStatusKRSMahasiswa;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederExporterGenerator;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.KrsDetailHelper;
import ais.action.master.helper.PengecualianJadwalPengisianKRSMahasiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiHistoryKRSDetailPerkuliahanHelper;
import ais.action.master.helper.TampilStudiMahasiswaHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.AsyncTaskManager;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.Komentar;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class MonitorKRSMahasiswaAction extends GenericAutowireComposer implements DataLoader {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220301468178L;
	protected MyGrid grid;
	protected MyGrid gridMahasiswaBelumDisetujui;
	protected MyGrid gridMahasiswaTelahDisetujui;
	protected MyGrid gridKomentarMahasiswa;
	protected Paging paging;

	protected AmbilDataKelasBanbox searchkelas;
	protected Textbox searchnim;
	protected Textbox searchnama;
	protected Textbox cari_mhs;
	protected Textbox cari_mhs_telah;
	protected Textbox cari_mhs_komen;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	protected Decimalbox searchtahun;
	protected Combobox searchstatus;
	protected Combobox searchprogram;
	protected Combobox searchStatusAwalMahasiswa;
	protected Combobox searchjenjang;
	protected AmbilDataDosenBanbox searchdosen;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected Integer semesterPendek = null;
	private boolean edit = false;

	/**
	 * Nilai efektif "semester pendek" untuk daftar utama: bila filter Jns Sem.
	 * bernilai Perkuliahan.SP maka paksa SEMESTER_PENDEK, selain itu ikuti field
	 * semesterPendek yang ada (menggantikan tab "KRS SP" lama). Dibuat lokal agar
	 * tidak mengubah field bersama yang dipakai dialog ekspor/feeder.
	 */
	protected Integer semesterPendekEfektif() {
		if (searchJenisSemester != null && searchJenisSemester.getSelectedItem() != null
				&& Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue())) {
			return Perkuliahan.SEMESTER_PENDEK;
		}
		return semesterPendek;
	}

	protected Tabpanel rekapPengambilanKRSMahasiswa;
	// protected Tabpanel rekapPengambilanKRSMahasiswaPerKelas;
	protected Tabpanel statistikPengambilanKRSMahasiswa;
	// protected Tabpanel dashboardMahasiswaYangSudahMengambilKRS;
	// protected Tabpanel dashboardMahasiswaYangBelumMengambilKRS;
	// protected Tabpanel dashboardMahasiswaYangBelumMengambilKRSSamaSekali;
	// protected Tabpanel dashboardMahasiswaYangSebagianMengambilKRS;
	protected Tabpanel dashboardKomentarMahasiswaKRS;
	protected Tabpanel dashboardMaksimalSKSMahasiswa;
	protected Tabpanel persetujuanKRS;
	protected Tabpanel sksDanIPKMahasiswa;
	protected Tabpanel statusMahasiswaAmbilMatakuliah;

	protected MyToolbarbuttonConfig find;

	// protected Tabpanel jadwalPerkuliahanPerMatakuliah;

	protected MyToolbarbuttonConfig pengecualian;

	public void onPengecualianJadwalPengisianKRSMahasiswa(Event event) throws Exception {
		PengecualianJadwalPengisianKRSMahasiswaHelper pengecualianJadwalPengisianKRSMahasiswaHelper = new PengecualianJadwalPengisianKRSMahasiswaHelper();
		pengecualianJadwalPengisianKRSMahasiswaHelper.display();
	}

	protected Tabpanel history;

	public void onHistory(Event event) throws Exception {

		if (history.getChildren().size() == 0) {
			RevisiHistoryKRSDetailPerkuliahanHelper include = new RevisiHistoryKRSDetailPerkuliahanHelper();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(history);
		}
	}

	protected Tabpanel statistik;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardKrsMahasiswa include = new DashboardKrsMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik KRS Mahasiswa",
				"Rekap pengambilan KRS dan status akademik mahasiswa.");
		}
	}

	protected Tabpanel sejarahKrs;

	public void onSejarah(Event event) {

		if (sejarahKrs.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(sejarahKrs);
			include.setSrc("/pages/master/krs_mahasiswa.zul");
		}
	}

	protected Tabpanel riwayatKrs;

	public void onRiwayat(Event event) {

		if (riwayatKrs.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(riwayatKrs);
			include.setSrc("/pages/master/detailperkuliahan.zul");
		}
	}

	protected Tabpanel krsSp;

	public void onKrsSp(Event event) {

		if (krsSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(krsSp);
			include.setSrc("/pages/master/monitor_krs_mahasiswa_sp.zul");
		}
	}

	protected Integer ekstrakurikuler;
	protected Tabpanel krsEkstrakurikuler;

	public void onKrsEkstrakurikuler(Event event) {

		if (krsEkstrakurikuler.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(krsEkstrakurikuler);
			include.setSrc("/pages/master/monitor_krs_mahasiswa_ekstrakurikuler.zul");
		}
	}

	protected Tabpanel tugasAkhir;

	public void onTugasAkhir(Event event) {

		if (tugasAkhir.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(tugasAkhir);
			include.setSrc("/pages/master/mahasiswa_request_tugas_akhir.zul");
		}
	}

	private Tabpanel manajemenSidang;

	public void onSidang(Event event) {
		if (manajemenSidang.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenSidang);
			MyInclude iframe = new MyInclude("/pages/master/skripsi.zul");
			iframe.setParent(window);
		}
	}

	protected Tabpanel kkn;

	public void onKKN(Event event) {

		if (kkn.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(kkn);
			include.setSrc("/pages/master/kkn/kelompok_kkn.zul");
		}
	}

	protected Tabpanel pkl;

	public void onPKL(Event event) {

		if (pkl.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(pkl);
			include.setSrc("/pages/master/pkl/kelompok_pkl.zul");
		}
	}

	// public void onJadwalPerkuliahanPerMatakuliah(Event event) {
	//
	// if (jadwalPerkuliahanPerMatakuliah.getChildren().size() == 0) {
	// DashboardRekapJadwalPerkulaiahanPerMatakuliah laporan = new
	// DashboardRekapJadwalPerkulaiahanPerMatakuliah();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(jadwalPerkuliahanPerMatakuliah);
	// }
	// }

	public void onStatusMahasiswaAmbilMatakuliah(Event event) {
		if (statusMahasiswaAmbilMatakuliah.getChildren().size() == 0) {
			DashboardStatusKRSMahasiswa laporan = new DashboardStatusKRSMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, statusMahasiswaAmbilMatakuliah,
				"Status KRS Mahasiswa",
				"Tampilan mahasiswa yang sudah dan belum mengambil KRS.");
		}
	}

	public void onMaksimalSKSMahasiswa(Event event) {
		if (dashboardMaksimalSKSMahasiswa.getChildren().size() == 0) {
			DashboardMaksimakKrsMahasiswa laporan = new DashboardMaksimakKrsMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardMaksimalSKSMahasiswa,
				"Batas SKS Mahasiswa",
				"Mahasiswa yang mendekati atau melebihi batas maksimal SKS per semester.");
		}
	}

	public void onSKSdanIPKMahasiswa(Event event) {
		if (sksDanIPKMahasiswa.getChildren().size() == 0) {
			DashboardSKSDanIPKMahasiswa laporan = new DashboardSKSDanIPKMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, sksDanIPKMahasiswa,
				"SKS & IPK Mahasiswa",
				"Distribusi total SKS yang diambil dan nilai IPK mahasiswa.");
		}
	}

	// protected Tabpanel rekapPengambilanKRSPerSmt;

	// public void onDashboardRekapPengambilanKRSPerSmt(Event event) {
	//
	// if (rekapPengambilanKRSPerSmt.getChildren().size() == 0) {
	// DashboardRekapPengambilanKRSPerSmt laporan = new
	// DashboardRekapPengambilanKRSPerSmt();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(rekapPengambilanKRSPerSmt);
	// }
	// }

	// protected Tabpanel rekapTidakAmbilKRSPerSmt;
	//
	// public void onDashboardRekapTidakAmbilKRSPerSmt(Event event) {
	//
	// if (rekapTidakAmbilKRSPerSmt.getChildren().size() == 0) {
	// DashboardRekapTidakAmbilKRSPerSmt laporan = new
	// DashboardRekapTidakAmbilKRSPerSmt();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(rekapTidakAmbilKRSPerSmt);
	// }
	// }

	public void onDashboardRekapPengambilanKRSMahasiswa(Event event) {

		if (rekapPengambilanKRSMahasiswa.getChildren().size() == 0) {
			DashboardRekapPengambilanKRSMahasiswa laporan = new DashboardRekapPengambilanKRSMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapPengambilanKRSMahasiswa,
				"Rekap Pengambilan KRS",
				"Jumlah mahasiswa yang sudah dan belum mengambil KRS per angkatan/prodi.");
		}
	}

	// public void onDashboardRekapPengambilanKRSMahasiswaPerKelas(Event event)
	// {
	//
	// if (rekapPengambilanKRSMahasiswaPerKelas.getChildren().size() == 0) {
	// DashboardRekapPengambilanKRSMahasiswaPerKelas laporan = new
	// DashboardRekapPengambilanKRSMahasiswaPerKelas();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(rekapPengambilanKRSMahasiswaPerKelas);
	// }
	// }

	public void onStatistikPengambilanKRS(Event event) throws Exception {
		if (statistikPengambilanKRSMahasiswa.getChildren().size() == 0) {
			DashboardStatistikPengambilanKRSMahasiswa laporan = new DashboardStatistikPengambilanKRSMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, statistikPengambilanKRSMahasiswa,
				"Statistik Pengambilan KRS",
				"Tren dan distribusi pengambilan KRS dari waktu ke waktu.");
		}
	}

	public void onPersetujuanKRS(Event event) throws Exception {
		if (persetujuanKRS.getChildren().size() == 0) {
			DashboardPersetujuanKRSMahasiswa laporan = new DashboardPersetujuanKRSMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, persetujuanKRS,
				"Persetujuan KRS",
				"Status persetujuan KRS mahasiswa oleh dosen wali.");
		}
	}

	// public void onDashboardMahasiswaYangSudahMengambilKRS(Event event) throws
	// Exception {
	// if (dashboardMahasiswaYangSudahMengambilKRS.getChildren().size() == 0) {
	// DashboardMahasiswaYangSudahMengambilKRS laporan = new
	// DashboardMahasiswaYangSudahMengambilKRS();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(dashboardMahasiswaYangSudahMengambilKRS);
	// }
	// }

	// public void onDashboardMahasiswaYangBelumMengambilKRS(Event event) throws
	// Exception {
	// if (dashboardMahasiswaYangBelumMengambilKRS.getChildren().size() == 0) {
	// DashboardMahasiswaYangBelumMengambilKRS laporan = new
	// DashboardMahasiswaYangBelumMengambilKRS();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(dashboardMahasiswaYangBelumMengambilKRS);
	// }
	// }

	// public void onDashboardMahasiswaYangBelumMengambilKRSSamaSekali(Event
	// event) throws Exception {
	// if
	// (dashboardMahasiswaYangBelumMengambilKRSSamaSekali.getChildren().size()
	// == 0) {
	// DashboardMahasiswaYangBelumMengambilKRSSamaSekali laporan = new
	// DashboardMahasiswaYangBelumMengambilKRSSamaSekali();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(dashboardMahasiswaYangBelumMengambilKRSSamaSekali);
	// }
	// }

	// public void onDashboardMahasiswaYangSebagianMengambilKRS(Event event)
	// throws Exception {
	// if (dashboardMahasiswaYangSebagianMengambilKRS.getChildren().size() == 0)
	// {
	// DashboardMahasiswaYangSebagianMengambilKRS laporan = new
	// DashboardMahasiswaYangSebagianMengambilKRS();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(dashboardMahasiswaYangSebagianMengambilKRS);
	// }
	// }

	public void onDashboardKomentarMahasiswaKRS(Event event) throws Exception {
		if (dashboardKomentarMahasiswaKRS.getChildren().size() == 0) {
			DashboardKomentarMahasiswaKRS laporan = new DashboardKomentarMahasiswaKRS();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardKomentarMahasiswaKRS,
				"Komentar KRS Mahasiswa",
				"Catatan dan masukan dari mahasiswa terkait pengambilan KRS.");
		}
	}

	protected Tabpanel konsultasiMahasiswa;

	public void onKonsultasiMahasiswa(Event event) {

		if (konsultasiMahasiswa.getChildren().size() == 0) {
			MyIframe include = new MyIframe("/pages/master/grup_pertemuan.zul");
			konsultasiMahasiswa.appendChild(include);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		// Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	protected Tab belumDisetujui;

	protected North mynorth;
	protected Textbox keyword;
	private PerguruanTinggi perguruanTinggi;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		tbmuser = Common.getCurrentUser();
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (konsultasiMahasiswa != null) {
			konsultasiMahasiswa.setVisible(true);
			konsultasiMahasiswa.getLinkedTab().setVisible(true);
		}
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (pengecualian != null) pengecualian.setVisible(tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses().getRoleId() != null
				&& (tbmuser != null && tbmuser.hakAkses() != null
						&& Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)
						|| Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.DIKJAR)));

		searchkelas.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (mynorth != null && tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Common.clear(mynorth);
			keyword = new Textbox();
			keyword.setCols(Common.isMobile() ? 10 : 20);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			};
			keyword.addEventListener("onOK", eventListener);

			Toolbar toolbar = new Toolbar();
			toolbar.appendChild(new MyLabelBoldConfig("Mahasiswa :"));

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			toolbar.appendChild(keyword);
			toolbar.appendChild(button);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});
			toolbar.setParent(mynorth);
		} else {

			searchJenisSemester.setReadonly(true);
			searchTahunAjaran.setReadonly(true);

			Common.generateTahunAjaran(searchTahunAjaran);
			Comboitem comboitem = new org.zkoss.zul.Comboitem();
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

			Common.selectComboItem(searchJenisSemester,
					Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			Common.initPrograms(searchprogram);

			Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			Common.insertComboDanSemua(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);

			Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				Dosen mydosen = tbmuser.ambilDosen();
				searchdosen.setValue(mydosen.getNama());
				searchdosen.setAttribute("myValue", mydosen);
				searchdosen.setAttribute("dosen", mydosen);
				searchdosen.setDisabled(true);
			}
			searchdosen.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
					onLoadData(null);
					onLoadDataKomentar(null);
				}
			});

			searchkelas.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onLoadData(null);
					onLoadDataKomentar(null);
				}
			});

			MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton("Download Persetujuan KRS",
					"/img/print.png");
			cetakToolbarbutton.setVisible(tbmuser != null
					&& (tbmuser.ambilDosen() == null || Common.bolehKonfigurasi("tampilkan_tombol_upload_persetujuan_krs_di_dosen"))
					&& tbmuser.getMahasiswa() == null
					&& Common.bolehKonfigurasi("tampilkan_tombol_download_persetujuan_krs"));
			Common.appendKeToolbar(cetakToolbarbutton, find, comp);

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
					"Upload Persetujuan KRS" + Common.ukuranLabelFileUpload(), "/img/excel.png");
			upload.setVisible(tbmuser != null
					&& (tbmuser.ambilDosen() == null || Common.bolehKonfigurasi("tampilkan_tombol_upload_persetujuan_krs_di_dosen"))
					&& tbmuser.getMahasiswa() == null
					&& Common.bolehKonfigurasi("tampilkan_tombol_upload_persetujuan_krs"));
			upload.setUpload(Common.ukuranFileUpload());

			upload.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UploadEvent uploadEvent = (UploadEvent) event;
					Media media = uploadEvent.getMedia();
					if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
						return;
					if (media.getName().toLowerCase().endsWith("xlsx")) {

						InputStream inputStream = media.getStreamData();
						// System.out.println("media = " + media);
						final File file = new File(
								Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						// System.out.println("file = " +
						// file.getAbsolutePath());
						file.getParentFile().mkdirs();
						FileOutputStream fileOutputStream = new FileOutputStream(file);
						int c;
						while ((c = inputStream.read()) != -1) {
							fileOutputStream.write(c);
						}
						fileOutputStream.close();
						inputStream.close();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								uploadDataMahasiswa(file, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
										Clients.clearBusy();
									}
								});
							}
						}, "Harap tunggu.. sedang melakukan proses upload data..");

					} else {
						MyMessageboxConfig.show(
								"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
										+ media,
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					}
				}
			});
			Common.appendKeToolbar(upload, find, comp);

			MyToolbarbuttonConfig bersihkan = bersihkanKrsMahasiswaDouble("Lihat data KRS Double", "/img/excel.png");
			bersihkan.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
					&& Common.bolehKonfigurasi("tampilkan_tombol_bersihkan_krs_double"));

			Common.appendKeToolbar(bersihkan, find, comp);

			MyToolbarbuttonConfig sesuaikan = sesuikanKrsMahasiswaSesuaiTa("Lihat dan Sesuikan Krs Mahasiswa Sesuai TA",
					"/img/excel.png");
			sesuaikan.setVisible(Common.getApakahAdmin());

			Common.appendKeToolbar(sesuaikan, find, comp);

		}

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim AKM ke Feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
					window.setParent(page.getFirstRoot());
					window.setHeight("400px");
					window.setWidth("600px");
					final Combobox tahunAkademik = new Combobox();
					Common.generateTahunAjaran(tahunAkademik);
					final Combobox genapGanjil = new Combobox();
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();

					if (semesterPendek == null) {
						comboitem.setLabel(Perkuliahan.GENAP);
						comboitem.setValue(Perkuliahan.GENAP);
						genapGanjil.appendChild(comboitem);
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(Perkuliahan.GANJIL);
						comboitem.setValue(Perkuliahan.GANJIL);
						genapGanjil.appendChild(comboitem);
					}
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(Perkuliahan.SP);
					comboitem.setValue(Perkuliahan.SP);
					genapGanjil.appendChild(comboitem);
					if (semesterPendek == null) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
						genapGanjil.appendChild(comboitem);
					}
					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setParent(borderlayout);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig column = new MyColumnConfig();
					column.setWidth("20%");
					column.setParent(columns);
					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
					row.appendChild(tahunAkademik);
					tahunAkademik.setWidth("90%");
					tahunAkademik.setReadonly(true);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
					row.appendChild(genapGanjil);
					genapGanjil.setWidth("90%");
					genapGanjil.setReadonly(true);
					if (semesterPendek == null) {
						Common.selectComboItem(genapGanjil,
								Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
					} else {
						genapGanjil.setSelectedItem(comboitem);
					}

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Hitung Ulang Nilai"));
					final MyCheckboxConfig hitungUlang;
					row.appendChild(hitungUlang = new MyCheckboxConfig());
					hitungUlang.setChecked(true);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					// toolbar.setHeight("25px");
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
					save.setTooltiptext("Proses");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();

							String[] kon = EksporFromFeederAction.koneksi();
							final String ip = kon[0];
							final String port = kon[1];
							final String username = kon[2];
							final String password = kon[3];
							final String url = kon[4];

							if (!EksporFromFeederAction.exists(url)) {

								MyMessageboxConfig.show(
										ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}

							final List<String> errorLog = new ArrayList<String>();
							final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (arg0 != null && !arg0.getName().isEmpty()) {
										EksporFromFeederAction.display();
										MyMessageboxConfig.show(arg0.getName(), "Info", MyMessageboxConfig.OK,
												MyMessageboxConfig.EXCLAMATION);
									}

									if (!errorLog.isEmpty()) {
										String err = "";
										for (String s : errorLog) {
											err += err.isEmpty() ? s
													: "\n----------------------------------------------------------------------------------------------------------\n"
															+ s;
										}

										MyMessageboxConfig.show(
												"Error Terjadi, catatan error akan otomatis ter-download",
												"Error Terjadi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

										File file = new File(
												Common.REAL_PATH + "/tmp/error_" + Common.randLong() + ".txt");
										if (!file.getParentFile().exists()) {
											file.getParentFile().mkdirs();
										}
										FileUtils.writeStringToFile(file, err);
										Filedownload.save(file, "text/plain");
									}

									onSearchDefault(null);
								}
							});

							new Thread(new Runnable() {

								@SuppressWarnings("unchecked")
								@Override
								public void run() {
									try {
										FeederConnector feederConnector = new FeederConnector(ip,
												Integer.parseInt(port), myLabelProsesDetail);

										String token = feederConnector.getToken(username, password);
										System.out.println("TOKEN => " + token);

										if (token == null || token.trim().isEmpty()
												|| token.trim().toLowerCase().startsWith("error")) {
											myLabelProsesDetail.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
											return;
										}

										FeederExporter feederImporter = new FeederExporter(feederConnector, token, null,
												null, null);

										List<Mahasiswa> mahasiswas = ConstantValues.simpleList(initCriteria(true),
												Mahasiswa.class);

										String smta = (String) (genapGanjil.getSelectedItem() == null ? null
												: genapGanjil.getSelectedItem().getValue());

										boolean sp = smta != null && smta.equalsIgnoreCase(Perkuliahan.SP);

										int size = mahasiswas.size();
										int index = 1;
										for (Mahasiswa mahasiswa : mahasiswas) {
											myLabelProsesDetail.setValue(
													"Memproses " + mahasiswa.getNim() + " " + mahasiswa.getNama() + " ("
															+ Common.numberFormat.get().format((index * 100.0) / size) + "%");
											index++;
											Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
													(String) tahunAkademik.getSelectedItem().getValue(),
													(String) genapGanjil.getSelectedItem().getValue(),
													mahasiswa.getPindahKeKampusIniMasukSemester(),
													mahasiswa.getSemesterMulai());

											KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt,
													null, sp ? Perkuliahan.SEMESTER_PENDEK : semesterPendek,
													hitungUlang.isChecked());

											System.out.println("mahasiswa " + mahasiswa + " smt " + smt
													+ " semesterPendek " + semesterPendek + " krsMahasiswa "
													+ krsMahasiswa + " id = " + krsMahasiswa.getId());

											kirimKeFeeder(feederImporter, feederConnector, token, krsMahasiswa,
													errorLog);

										}
										mahasiswas.clear();
										mahasiswas = null;

										myLabelProsesDetail.setValue("");
									} catch (Exception e) {
										// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log admin lalu progres diset "" (=SUKSES palsu) di luar try.
										ais.common.Common.tampilErrorJikaAdmin(e);
										myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
												"pengiriman data KRS Mahasiswa ke Neo Feeder",
												null, e,
												new String[] {
														"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
														"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
														"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
												.replace("\n", " "));
									}
								}
							}).start();

						}
					});
					save.setParent(toolbar);

					window.onModal();

				}
			});
			Common.appendKeToolbar(buttonTagihan, find, comp);

			buttonTagihan = new MyToolbarbuttonConfig("Kirim Nilai Transfer ke Feeder",
					"/img/Finance-Invoice-icon.png");

			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					String[] kon = EksporFromFeederAction.koneksi();
					final String ip = kon[0];
					final String port = kon[1];
					final String username = kon[2];
					final String password = kon[3];
					final String url = kon[4];

					if (!EksporFromFeederAction.exists(url)) {

						MyMessageboxConfig.show(ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					final List<String> errorLog = new ArrayList<String>();
					final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (arg0 != null && !arg0.getName().isEmpty()) {
								EksporFromFeederAction.display();
								MyMessageboxConfig.show(arg0.getName(), "Info", MyMessageboxConfig.OK,
										MyMessageboxConfig.EXCLAMATION);
							}

							if (!errorLog.isEmpty()) {
								String err = "";
								for (String s : errorLog) {
									err += err.isEmpty() ? s
											: "\n----------------------------------------------------------------------------------------------------------\n"
													+ s;
								}

								MyMessageboxConfig.show("Error Terjadi, catatan error akan otomatis ter-download",
										"Error Terjadi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

								File file = new File(Common.REAL_PATH + "/tmp/error_" + Common.randLong() + ".txt");
								if (!file.getParentFile().exists()) {
									file.getParentFile().mkdirs();
								}
								FileUtils.writeStringToFile(file, err);
								Filedownload.save(file, "text/plain");
							}

							onSearchDefault(null);
						}
					});

					new Thread(new Runnable() {

						@SuppressWarnings("unchecked")
						@Override
						public void run() {
							try {
								FeederConnector feederConnector = new FeederConnector(ip, Integer.parseInt(port), null);

								String token = feederConnector.getToken(username, password);
								System.out.println("TOKEN => " + token);

								if (token == null || token.trim().isEmpty()
										|| token.trim().toLowerCase().startsWith("error")) {
									myLabelProsesDetail.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
									return;
								}

								FeederExporter feederImporter = new FeederExporter(feederConnector, token, null, null,
										myLabelProsesDetail);

								List<Mahasiswa> mahasiswas = ConstantValues.simpleList(initCriteria(true),
										Mahasiswa.class);

								int size = mahasiswas.size();
								int index = 1;
								for (Mahasiswa mahasiswa : mahasiswas) {
									myLabelProsesDetail
											.setValue("Memproses " + mahasiswa.getNim() + " " + mahasiswa.getNama()
													+ " (" + Common.numberFormat.get().format((index * 100.0) / size) + "%");
									index++;

									List<Long> details = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa,
											null);
									for (Long id : details) {
										Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
												.ambilData(Detailperkuliahan.class, id.toString());
										if (detailperkuliahan.getMahasiswa() == null
												|| detailperkuliahan.getMahasiswa().getIdRegPd() == null) {
											errorLog.add("Mahasiswa " + detailperkuliahan.getMahasiswa()
													+ " belum terdaftar");
										} else if (detailperkuliahan != null) {
											feederImporter.nilaiTransfer(detailperkuliahan, errorLog);
										}
									}
									details.clear();
									details = null;
								}
								mahasiswas.clear();
								mahasiswas = null;

								myLabelProsesDetail.setValue("");
							} catch (Exception e) {
								// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log admin lalu progres diset "" (=SUKSES palsu) di luar try.
								ais.common.Common.tampilErrorJikaAdmin(e);
								myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
										"pengiriman data Nilai Transfer Mahasiswa ke Neo Feeder",
										null, e,
										new String[] {
												"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
												"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
												"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
										.replace("\n", " "));
							}
						}
					}).start();

				}
			});
			Common.appendKeToolbar(buttonTagihan, find, comp);

		}

		MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Download/Hitung AKM", "/img/excel.png");

		buttonTagihan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("400px");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaran(tahunAkademik);
				final Combobox genapGanjil = new Combobox();
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				if (semesterPendek == null) {
					comboitem.setLabel(Perkuliahan.GENAP);
					comboitem.setValue(Perkuliahan.GENAP);
					genapGanjil.appendChild(comboitem);
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(Perkuliahan.GANJIL);
					comboitem.setValue(Perkuliahan.GANJIL);
					genapGanjil.appendChild(comboitem);
				}

				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.SP);
				comboitem.setValue(Perkuliahan.SP);
				genapGanjil.appendChild(comboitem);
				if (semesterPendek == null) {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					genapGanjil.appendChild(comboitem);
				}

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("40%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
				row.appendChild(tahunAkademik);
				tahunAkademik.setWidth("90%");
				tahunAkademik.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
				row.appendChild(genapGanjil);
				genapGanjil.setWidth("90%");
				genapGanjil.setReadonly(true);
				if (semesterPendek == null) {
					Common.selectComboItem(genapGanjil,
							Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
				} else {
					genapGanjil.setSelectedItem(comboitem);
				}
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Hitung Ulang SKS/IPS/IPK (Download data lebih lama)"));
				final MyCheckboxConfig hitungUlang;
				row.appendChild(hitungUlang = new MyCheckboxConfig());

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);

				final String[] contents = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama",
						"mahasiswa.jurusan.nama", "mahasiswa.program", "mahasiswa.statusAwalMahasiswa.nama",
						"tahunAkademik", "semester", "dosenPa.nama", "sksYangDiambil", "sksk", "sksKonversi",
						"sksBukanKonversi", "semesterPendek", "ipk-number", "ips-number", "catatan", "catatanKhs" };

				List<String> columnHeadersAdding = new ArrayList<String>();
				columnHeadersAdding.add("Biaya");

				EventListener dataAdding = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] objects = (Object[]) arg0.getData();
						KrsMahasiswa krsMahasiswa = (KrsMahasiswa) objects[0];

						XSSFRow row = (XSSFRow) objects[2];

						Session session = HibernateUtil.currentNativeSession();

						JSONObject jsonObjectTemporary = FeederExporterGenerator.kuliah_mahasiswa(session,
								krsMahasiswa.getMahasiswa(), krsMahasiswa.getSemester(),
								krsMahasiswa.getSemesterPendek(), krsMahasiswa);
						Double biaya_smt = jsonObjectTemporary.getDouble("biaya_smt");
						row.createCell(contents.length + 0).setCellValue(biaya_smt);

						// session.disconnect();
						ais.common.Common.closeOpenedSession(session);
					}
				};

				MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KrsMahasiswa.class,
						new DataCriteria() {

							@SuppressWarnings("unchecked")
							@Override
							public Criteria initCriteria(boolean order) {

								String smta = (String) (genapGanjil.getSelectedItem() == null ? null
										: genapGanjil.getSelectedItem().getValue());

								boolean sp = smta != null && smta.equalsIgnoreCase(Perkuliahan.SP);

								List<Long> mahasiswas = MonitorKRSMahasiswaAction.this.initCriteria(true)
										.setProjection(Projections.property("id")).list();

								if (hitungUlang.isChecked()) {
									for (Long mhs : mahasiswas) {
										Mahasiswa mahasiswa = (Mahasiswa) ConstantValues
												.ambil(Mahasiswa.class.getName(), mhs);
										if (mahasiswa != null) {
											Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
													(String) tahunAkademik.getSelectedItem().getValue(),
													(String) genapGanjil.getSelectedItem().getValue(),
													mahasiswa.getPindahKeKampusIniMasukSemester(),
													mahasiswa.getSemesterMulai());

											Common.singkronkanKrsMahasiswa(mahasiswa, smt, null,
													sp ? Perkuliahan.SEMESTER_PENDEK : semesterPendek,
													hitungUlang.isChecked());
										}
									}
								}

								Criteria criteria = HibernateUtil.currentSession().createCriteria(KrsMahasiswa.class)
										.add(sp ? Restrictions.eq("semesterPendek", Perkuliahan.SEMESTER_PENDEK)
												: semesterPendek == null ? Restrictions.isNull("semesterPendek")
														: Restrictions.eq("semesterPendek", semesterPendek))
										.add(sp ? Restrictions.sqlRestriction("true")
												: smta == null ? Restrictions.sqlRestriction("true")
														: smta.equals(Perkuliahan.GENAP)
																? Restrictions.in("semester", Common.genap)
																: Restrictions.in("semester", Common.ganjil))
										.add(Restrictions.eq("tahunAkademik",
												tahunAkademik.getSelectedItem().getValue()))
										.createAlias("mahasiswa", "mahasiswa").addOrder(Order.asc("mahasiswa.nim"))
										.add(mahasiswas.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("mahasiswa.id", mahasiswas));

								return criteria;
							}
						}, "Download/Hitung AKM", "/img/print.png", columnHeadersAdding, dataAdding, contents);
				toolbar.appendChild(cetakToolbarbutton);

				window.onModal();

			}
		});
		Common.appendKeToolbar(buttonTagihan, find, comp);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	private MyDetail detailUtama = null;
	private Criterion criteriaStatus;
	protected List<Long> mhss = null;
	private StatusMahasiswa selectedStatusMahasiswa = null;

	public static void kirimKeFeeder(FeederExporter feederImporter, FeederConnector feederConnector, String token,
			KrsMahasiswa krsMahasiswa, List<String> errorLog) {

		if (krsMahasiswa.getSemesterPendek() != null
				&& krsMahasiswa.getSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)) {
			int sksmhss = krsMahasiswa.getSksYangDiambil();
			if (sksmhss == 0) {
				return;
			}
		}

		if (krsMahasiswa != null && krsMahasiswa.getMahasiswa() != null
				&& krsMahasiswa.getMahasiswa().getStatusKeluar() != null
				&& krsMahasiswa.getMahasiswa().getSemesterLulus() != null && krsMahasiswa.getSemester() != null
				&& krsMahasiswa.getSemester() > krsMahasiswa.getMahasiswa().getSemesterLulus()) {
			return;
		}

		if (krsMahasiswa.getMahasiswa() == null || krsMahasiswa.getMahasiswa().getIdRegPd() == null) {
			errorLog.add("Mahasiswa " + krsMahasiswa.getMahasiswa() + " belum terdaftar");
		} else if (krsMahasiswa != null && krsMahasiswa.getMahasiswa() != null
				&& krsMahasiswa.getMahasiswa().getIdRegPd() != null) {

			Session session = HibernateUtil.currentNativeSession();

			try {

				JSONObject jsonObjectTemporary = FeederExporterGenerator.kuliah_mahasiswa(session,
						krsMahasiswa.getMahasiswa(), krsMahasiswa.getSemester(), krsMahasiswa.getSemesterPendek(),
						krsMahasiswa);

				String id_smt = jsonObjectTemporary.getString("id_smt");
				String id_reg_pd = jsonObjectTemporary.getString("id_reg_pd");
				Double ips = jsonObjectTemporary.getDouble("ips");

				Integer sks_smt = jsonObjectTemporary.getInt("sks_smt");
				Double ipk = jsonObjectTemporary.getDouble("ipk");
				Integer sks_total = jsonObjectTemporary.getInt("sks_total");
				String id_stat_mhs = jsonObjectTemporary.getString("id_stat_mhs");
				Double biaya_smt = jsonObjectTemporary.getDouble("biaya_smt");

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id_registrasi_mahasiswa", id_reg_pd);
				jsonObject.put("id_semester", id_smt);
				jsonObject.put("id_status_mahasiswa", id_stat_mhs);
				jsonObject.put("ips", Common.numberFormatEn.get().format(ips));
				jsonObject.put("ipk", Common.numberFormatEn.get().format(ipk));
				jsonObject.put("sks_semester", sks_smt.toString());
				jsonObject.put("total_sks", sks_total.toString());
				jsonObject.put("biaya_kuliah_smt", biaya_smt.intValue() + "");
				jsonObject.put("id_pembiayaan", krsMahasiswa.getMahasiswa().getJenisPembiayaanMahasiswa().getFeeder());

				System.out.println("akan mengirimkan data -> " + jsonObject);

				String filter = "id_semester='" + id_smt + "' AND id_registrasi_mahasiswa='" + id_reg_pd + "'";

				JSONArray dataDetailPerkuliahanMahasiswa = feederConnector.getData("GetDetailPerkuliahanMahasiswa",
						token, filter, "", "1", "");
				System.out.println("results dataDetailPerkuliahanMahasiswa -> " + dataDetailPerkuliahanMahasiswa);

				if (dataDetailPerkuliahanMahasiswa.length() > 0) {
					JSONObject jsonObjectKey = new JSONObject();
					jsonObjectKey.put("id_registrasi_mahasiswa", id_reg_pd);
					jsonObjectKey.put("id_semester", id_smt);

					jsonObject.remove("id_registrasi_mahasiswa");
					jsonObject.remove("id_semester");

					feederConnector.insertOrUpdateRecordBaru(token, jsonObjectKey, "UpdatePerkuliahanMahasiswa",
							jsonObject, errorLog, krsMahasiswa);
				} else {
					feederConnector.insertOrUpdateRecordBaru(token, null, "InsertPerkuliahanMahasiswa", jsonObject,
							errorLog, krsMahasiswa);
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			// session.disconnect();
			ais.common.Common.closeOpenedSession(session);
		}

	}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub

			final Mahasiswa mahasiswa = (Mahasiswa) arg1;

			final MyDetail detail = new MyDetail();
			if (detailUtama == null) {
				detailUtama = detail;
			}
			detail.setParent(arg0);
			arg0.setAttribute("detail", detail);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (detail.isOpen() && detail.getChildren().isEmpty()) {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								TampilStudiMahasiswaHelper tampilStudiMahasiswaHelper = new TampilStudiMahasiswaHelper(
										semesterPendekEfektif(), ekstrakurikuler, true, edit);
								ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
								groupbox.setStyle("min-height: 200px;");
								groupbox.setParent(detail);
								groupbox.appendChild(new MyCaptionStyled("Informasi KRS Mahasiswa -- "
										+ mahasiswa.getNama() + " (" + mahasiswa.getNim() + ")"));

								MyGrid grid = tampilStudiMahasiswaHelper.initMain(mahasiswa, new DataLoader() {

									@Override
									public void loadData(Object value) {

									}
								}, groupbox, null);

								groupbox.appendChild(grid);
							}
						});

					}
				}
			});

			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNim()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(vbox);

			mahasiswa.tampilkanHp(vbox);
			mahasiswa.tampilkanEmail(vbox);

			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

			Integer semester = mahasiswa.currentSemester();
			Integer tahap = (ConstantValues.aktifkanTahapan
					&& ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan()) > 2)
							? mahasiswa.currentTahapan()
							: null;
			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahap, semesterPendekEfektif());
			new Label(krsMahasiswa.getKelas()).setParent(arg0);
			new Label(mahasiswa.getWarganegara()).setParent(arg0);

			final Label label = new Label();
			label.setParent(arg0);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
					.currentStatus(mahasiswa).getStatusMahasiswa();
			new Label((mahasiswa.getStatusKeluar() == null ? "" : mahasiswa.getStatusKeluar().getNama() + "/")
					+ (statusMahasiswa == null ? "" : statusMahasiswa.getNama()) + "/"
					+ (mahasiswa.getStatusAwalMahasiswa() == null ? "" : mahasiswa.getStatusAwalMahasiswa().getNama()))
					.setParent(arg0);

			final Html htmlKrs = new ais.ui.util.MyHtml("");
			htmlKrs.setParent(arg0);

			String krs = mahasiswa.rubahKeteranganPengambilanKRS(krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
					krsMahasiswa.getSemesterPendek(), krsMahasiswa, false);
			htmlKrs.setContent(krs);
			if (mahasiswa.getDosen() == null && krsMahasiswa.getDosenPa() != null) {
				mahasiswa.setDosen(krsMahasiswa.getDosenPa().getId());
			}
			label.setValue(krsMahasiswa.getDosenPa() == null ? "Tidak mempunyai dosen PA"
					: krsMahasiswa.getDosenPa().getNama());
			if (krsMahasiswa.getDosenPa() == null) {
				label.setStyle("font-weight:bold;color:red");
			}

			// Kolom "Aksi": tombol ringkas per baris — "Daftar KRS" (buka rincian KRS) & "Komentar".
			// Seluruh logika popup, kueri komentar, dan gaya (CSS) berada di helper agar renderer tetap
			// ramping dan mudah dipakai ulang. Pemuatan berat (rincian KRS) dilakukan hanya saat diklik.
			ais.action.master.helper.AksiKrsMahasiswaHelper.render(mahasiswa, semesterPendekEfektif(),
					ekstrakurikuler, krsMahasiswa.getSemester(), edit, MonitorKRSMahasiswaAction.this)
					.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return initCriteria(session, order);
	}

	public Criteria initCriteria(Session session, boolean order) {

		if (mynorth != null && tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Criteria criteria = session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("dosen", tbmuser.getDosen().getId()))
					.add(keyword.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.ilike("nim", keyword.getValue().trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("nama", keyword.getValue().trim(), MatchMode.ANYWHERE)));

			if (order)
				criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

			if (perguruanTinggi != null) {
				criteria.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)
						.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
						.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));
			}

			return criteria;
		} else {

			Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");
			Kelas kelas = (Kelas) (searchkelas.getAttribute("kelas"));

			Criteria criteria = session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(dosen != null ? Restrictions.eq("dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))

					.add(kelas != null && !kelas.getNama().trim().isEmpty()
							? Restrictions.ilike("kelas", kelas.getNama().trim(), MatchMode.EXACT)
							: Restrictions.sqlRestriction("true"));

			if (order)
				criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

			criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
					: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
					.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("nim", searchnim.getValue().trim(), MatchMode.ANYWHERE))
					.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))
					.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
					.add(criteriaStatus)
					.add(searchStatusAwalMahasiswa.getSelectedItem() == null
							|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
							|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("statusAwalMahasiswa",
											searchStatusAwalMahasiswa.getSelectedItem().getValue()))
					.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunangkatan", searchtahun.getValue().intValue()))

					.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))
					.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: CommonSearchFilterHelper.eqSelectedWithId("jurusan.jenjang", searchjenjang, false));

			if (perguruanTinggi != null) {
				criteria.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
						.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));
			}

			return criteria;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(final Event event) {
		detailUtama = null;
		mhss = null;

		final int activePage = paging == null ? 0 : paging.getActivePage();
		final String taVal = (searchTahunAjaran != null && searchTahunAjaran.getSelectedItem() != null
				&& searchTahunAjaran.getSelectedItem().getValue() != null)
						? searchTahunAjaran.getSelectedItem().getValue().toString() : null;
		final String jenisSemesterVal = (searchJenisSemester != null && searchJenisSemester.getSelectedItem() != null
				&& searchJenisSemester.getSelectedItem().getValue() != null)
						? searchJenisSemester.getSelectedItem().getValue().toString() : null;

		selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus != null && searchstatus.getSelectedItem() != null
				&& searchstatus.getSelectedItem().getValue() != null ? searchstatus.getSelectedItem().getValue() : null);
		criteriaStatus = Restrictions.sqlRestriction("true");

		try {
			if (selectedStatusMahasiswa != null) {
				List<Long> dataMhs = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.property("id")).list();

				List<Long> tempMhss = new ArrayList<Long>();
				for (Long generalValueObjectid : dataMhs) {
					Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
							generalValueObjectid);
					if (mahasiswa != null) {
						Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), taVal, jenisSemesterVal,
								mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
						KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
						HistoryStatusMahasiswa historyStatusMahasiswa = Common.getHistoryStatusMahasiswa(krsMahasiswa);
						if (historyStatusMahasiswa != null && historyStatusMahasiswa.getStatusMahasiswa() != null
								&& historyStatusMahasiswa.getStatusMahasiswa().getId()
										.equals(selectedStatusMahasiswa.getId())) {
							tempMhss.add(mahasiswa.getId());
						}
					}
				}
				mhss = tempMhss;
				if (tempMhss.isEmpty()) {
					criteriaStatus = Restrictions.sqlRestriction("false");
				} else if (tempMhss.size() > 1000) {
					criteriaStatus = Restrictions.sqlRestriction("true");
				} else {
					criteriaStatus = Restrictions.in("id", tempMhss);
				}
			}

			Common.initPaging(initCriteria(false), paging);
			List<Mahasiswa> mahasiswas = ConstantValues.simpleList(
					initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * activePage),
					Mahasiswa.class);

			if (grid != null) {
				ListModel strset = new SimpleListModel(mahasiswas != null ? mahasiswas : new ArrayList<Mahasiswa>());
				grid.setRowRenderer(new MahasiswaRenderer());
				grid.setModelCheckMobile(strset);
			}
			onLoadData(null);
			onLoadDataKomentar(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void onLoadData(Event event) {

		// Komponen bisa BELUM ter-wire saat onLoadData dipicu dari doAfterCompose/forward awal →
		// hindari NPE (grid/field filter null). Bila belum siap, lewati; akan dimuat ulang saat interaksi.
		if (gridMahasiswaBelumDisetujui == null || gridMahasiswaTelahDisetujui == null || searchdosen == null
				|| searchkelas == null || searchjurusan == null || searchfakultas == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();

		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");
		Kelas kelas = (Kelas) (searchkelas.getAttribute("kelas"));

		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());

		String mhs = " and a.mahasiswa in (-1";
		if (mhss != null && !mhss.isEmpty()) {
			for (Long i : mhss) {
				mhs += "," + i;
			}
			mhs += ") ";
		} else {
			mhs = selectedStatusMahasiswa == null ? "" : " and false ";
		}

		String sql = "select b.id, max(b.nim) nim, max(b.nama) nama, count(a.id) jumlah, a.semester "
				+ " from detailperkuliahan a inner join mahasiswa b on (a.mahasiswa = b.id)  inner join jurusan c on (b.jurusan = c.id) "
				+ " inner join perkuliahan ccc on (a.perkuliahan=ccc.id) where b.status_keluar is null and a.persetujuan = "

				+ ((belumDisetujui != null && belumDisetujui.isSelected()) ? "0" : "1") + "  "

				+ ((belumDisetujui != null && belumDisetujui.isSelected())
						? (cari_mhs == null || cari_mhs.getValue().trim().isEmpty() ? ""
								: " and (b.nim ilike '%" + cari_mhs.getValue().trim() + "%' or b.nama ilike '%"
										+ cari_mhs.getValue().trim() + "%' )")
						: (cari_mhs_telah == null || cari_mhs_telah.getValue().trim().isEmpty() ? ""
								: " and (b.nim ilike '%" + cari_mhs_telah.getValue().trim() + "%' or b.nama ilike '%"
										+ cari_mhs_telah.getValue().trim() + "%' )"))

				+ (dosen != null ? (" and b.dosen = " + dosen.getId() + " ") : "")

				+ (kelas != null ? (" and b.kelas = '" + kelas.getNama() + "' ") : "")

				+ (fakultas != null ? " and c.fakultas = " + fakultas.getId() + " " : "")

				+ (jurusan != null ? " and b.jurusan = " + jurusan.getId() + " " : "")

				+ mhs

				+ (semesterPendek != null && semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK)
						? " and ccc.status_semesterpendek=" + semesterPendek + " "
						: " and ccc.status_semesterpendek is null ")

				+ "group by b.id,a.semester having count(a.id) > 0 order by  count(a.id) desc, a.semester desc limit 10";

		List<Object[]> mahasiswas = session.createSQLQuery(sql).list();
		ListModel strset = new SimpleListModel(mahasiswas);

		if ((belumDisetujui != null && belumDisetujui.isSelected())) {
			gridMahasiswaBelumDisetujui.setRowRenderer(new ais.ui.util.MyRowRenderer() {

				@Override
				public void render(Row arg0, Object arg1) throws Exception {
					arg0.setValign("top");
					final Object[] mahasiswa = (Object[]) arg1;

					EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							TampilStudiMahasiswaHelper tampilStudiMahasiswaHelper = new TampilStudiMahasiswaHelper(null,
									null, false, edit);

							Session session = HibernateUtil.currentSession();
							Mahasiswa mhs = (Mahasiswa) session.createCriteria(Mahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.idEq(((Number) mahasiswa[0]).longValue())).uniqueResult();

							tampilStudiMahasiswaHelper.tampil(mhs, MonitorKRSMahasiswaAction.this, false,
									Integer.parseInt(mahasiswa[4].toString()));

						}
					};

					A a;
					(a = new A(mahasiswa[1] + "-" + mahasiswa[2])).setParent(arg0);
					a.addEventListener("onClick", eventListener);

					A aa = new A(mahasiswa[3] + " matakuliah belum disetujui");
					aa.setParent(arg0);
					aa.addEventListener("onClick", eventListener);

					aa = new A(mahasiswa[4] + "");
					aa.setParent(arg0);
					aa.addEventListener("onClick", eventListener);

				}
			});
			gridMahasiswaBelumDisetujui.setModelCheckMobile(strset);
			gridMahasiswaBelumDisetujui.renderAll();
		} else {
			gridMahasiswaTelahDisetujui.setRowRenderer(new ais.ui.util.MyRowRenderer() {

				@Override
				public void render(Row arg0, Object arg1) throws Exception {
					arg0.setValign("top");
					final Object[] mahasiswa = (Object[]) arg1;

					EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							TampilStudiMahasiswaHelper tampilStudiMahasiswaHelper = new TampilStudiMahasiswaHelper(null,
									null, false, edit);

							Session session = HibernateUtil.currentSession();
							Mahasiswa mhs = (Mahasiswa) session.createCriteria(Mahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.idEq(((Number) mahasiswa[0]).longValue())).uniqueResult();

							tampilStudiMahasiswaHelper.tampil(mhs, MonitorKRSMahasiswaAction.this, false,
									Integer.parseInt(mahasiswa[4].toString()));

						}
					};

					A a;
					(a = new A(mahasiswa[1] + "-" + mahasiswa[2])).setParent(arg0);
					a.addEventListener("onClick", eventListener);

					A aa = new A(mahasiswa[3] + " matakuliah telah disetujui");
					aa.setParent(arg0);
					aa.addEventListener("onClick", eventListener);

					aa = new A(mahasiswa[4] + "");
					aa.setParent(arg0);
					aa.addEventListener("onClick", eventListener);

				}
			});
			gridMahasiswaTelahDisetujui.setModelCheckMobile(strset);
			gridMahasiswaTelahDisetujui.renderAll();
		}
	}

	@SuppressWarnings("unchecked")
	public void onLoadDataKomentar(Event event) {
		Session session = HibernateUtil.currentSession();

		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");
		Kelas kelas = (Kelas) (searchkelas.getAttribute("kelas"));

		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());

		String mhs = " and a.mahasiswa in (-1";
		if (mhss != null && !mhss.isEmpty()) {
			for (Long i : mhss) {
				mhs += "," + i;
			}
			mhs += ") ";
		} else {
			mhs = selectedStatusMahasiswa == null ? "" : " and false ";
		}

		String sql = "select  max(b.id) as id, a.komentar, max(a.tanggal) as tgl, "
				+ "max(b.nim) as nim, max(b.nama) as nama " +

				" from komentar a inner join mahasiswa b on (a.mahasiswa = b.id) inner join jurusan c on (b.jurusan = c.id) "
				+ "where b.status_keluar is null and a.mahasiswa is not null and a.tbmuser is null  "
				+ (cari_mhs_komen == null || cari_mhs_komen.getValue().trim().isEmpty() ? ""
						: " and (b.nim ilike '%" + cari_mhs_komen.getValue().trim() + "%' or b.nama ilike '%"
								+ cari_mhs_komen.getValue().trim() + "%' )")
				+ (dosen != null ? (" and b.dosen = " + dosen.getId() + " ") : "")

				+ (kelas != null ? (" and b.kelas = '" + kelas.getNama() + "' ") : "")

				+ (fakultas != null ? " and c.fakultas = " + fakultas.getId() + " " : "")

				+ (jurusan != null ? " and b.jurusan = " + jurusan.getId() + " " : "")

				+ mhs

				+ " group by a.komentar order by  max(a.tanggal) desc limit 100";

		// Guard: grid komentar bisa belum/ tidak ter-bind (null) saat doAfterCompose pada
		// varian halaman tertentu → setRowRenderer melempar NullPointerException.
		if (gridKomentarMahasiswa == null) {
			return;
		}
		List<Object[]> mahasiswas = session.createSQLQuery(sql).list();
		ListModel strset = new SimpleListModel(mahasiswas);
		gridKomentarMahasiswa.setRowRenderer(new ais.ui.util.MyRowRenderer() {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				final Object[] mahasiswa = (Object[]) arg1;

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						searchprogram.setSelectedItem(null);
						searchnama.setValue("");
						// searchfakultas.setSelectedItem(null);
						// searchjurusan.setSelectedItem(null);
						BigDecimal value = null;
						searchtahun.setValue(value);
						searchstatus.setSelectedItem(null);
						searchStatusAwalMahasiswa.setSelectedItem(null);
						searchjenjang.setSelectedItem(null);

						searchnim.setValue(mahasiswa[3].toString());
						onSearchDefault(arg0);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								List<Component> rows = (List<Component>) (grid.getRows().getChildren().isEmpty() ? null
										: grid.getRows().getChildren());
								if (rows != null) {
									Session session = HibernateUtil.currentSession();
									Mahasiswa mhs = (Mahasiswa) session.createCriteria(Mahasiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.idEq(((Number) mahasiswa[0]).longValue())).uniqueResult();

									try {
										Common.clear(detailUtama);
										detailUtama.setOpen(true);

										final TampilStudiMahasiswaHelper tampilStudiMahasiswaHelper = new TampilStudiMahasiswaHelper(
												semesterPendek, ekstrakurikuler, true, edit);
										ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
										groupbox.setStyle("min-height: 200px;");
										groupbox.setParent(detailUtama);
										groupbox.appendChild(new MyCaptionStyled("Informasi KRS Mahasiswa -- "
												+ mahasiswa[2].toString() + " (" + mahasiswa[1].toString() + ")"));

										groupbox.appendChild(tampilStudiMahasiswaHelper.initMain(mhs, new DataLoader() {

											@Override
											public void loadData(Object value) {

											}
										}, groupbox, null));
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								}
							}
						});
					}
				};

				new ais.ui.util.MyHtml(mahasiswa[1] + " (" + mahasiswa[3] + " - " + mahasiswa[4] + " - "
						+ Common.dateFormat3.get().format(mahasiswa[2])).setParent(arg0);
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
				toolbarbutton.setParent(arg0);
				toolbarbutton.addEventListener("onClick", eventListener);

			}
		});
		gridKomentarMahasiswa.setModelCheckMobile(strset);
		gridKomentarMahasiswa.renderAll();
	}

	@Override
	public void loadData(Object value) {
		onSearchDefault(null);
	}

	public MyToolbarbuttonConfig cetakDataCustomButton(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaran(tahunAkademik);
				final Combobox genapGanjil = new Combobox();
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Perkuliahan.GENAP);
				comboitem.setValue(Perkuliahan.GENAP);
				genapGanjil.appendChild(comboitem);
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.GANJIL);
				comboitem.setValue(Perkuliahan.GANJIL);
				genapGanjil.appendChild(comboitem);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
				row.appendChild(tahunAkademik);
				tahunAkademik.setWidth("90%");
				tahunAkademik.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
				row.appendChild(genapGanjil);
				genapGanjil.setWidth("90%");
				genapGanjil.setReadonly(true);

				Common.selectComboItem(genapGanjil,
						Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Download", "/img/save.gif");
				save.setTooltiptext("Download");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						Clients.showBusy(label.getValue());

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".xlsx");
						final File file;
						(file = new File(filename)).createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								try {

									Clients.showBusy(label.getValue());
									System.out.println("label " + label.getValue());

									if (label.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (label.getValue().isEmpty()) {

										Center center = new Center();
										final MyWindow window = new MyWindow("Cetak Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("97%");
										window.setWidth("90%");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);

										ais.ui.util.ZkCompat.setFlex(center, true);
										center.setParent(borderlayout);

										System.out.println("loading file " + file.getAbsolutePath());
										Common.clear(center);
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										Common.clear(center);
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 1);
										spreadsheet.setMaxcolumns(16);
										ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

										South south = new South();
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										// toolbar.setHeight("25px");
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {

												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}
										});
										print.setParent(toolbar);

										window.setVisible(true);
										window.onModal();

										Clients.clearBusy();
										timer.detach();
									}

								} catch (Exception e) {
									Clients.clearBusy();
								}

							}
						});
						timer.start();

						try {

							Clients.showBusy(label.getValue());

							new Thread(new Runnable() {

								@SuppressWarnings("unchecked")
								@Override
								public void run() {
									try {

									try {
										Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");
										Kelas kelas = (Kelas) (searchkelas.getAttribute("kelas"));

										StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus
												.getSelectedItem() == null
												|| searchstatus.getSelectedItem().getValue() == null ? null
														: searchstatus.getSelectedItem().getValue());

										Criterion criteriaStatus = Restrictions.sqlRestriction("true");
										if (statusMahasiswa != null) {
											String sql = "mahasiswa in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
													+ statusMahasiswa.getId() + " and tahunakademik = '"
													+ Common.getCurrentTahunAkademik() + "' and semester%2="
													+ (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
											System.out.println("sql=>" + sql);
											criteriaStatus = Restrictions.sqlRestriction(sql);
										}
										Session session = HibernateUtil.currentNativeSession();
										Criteria criteria = session.createCriteria(Detailperkuliahan.class)
												.add(criteriaStatus)

												.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)

												.add(semesterPendek != null
														&& semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK)
																? Restrictions.eq("perkuliahan.statusSemesterPendek",
																		semesterPendek)
																: Restrictions
																		.isNull("perkuliahan.statusSemesterPendek"))

												.add(Restrictions.eq("tahunAkademik",
														tahunAkademik.getSelectedItem().getValue()))

												.add(Restrictions.in("semester",
														genapGanjil.getSelectedItem().getValue().equals(
																Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))

												.add(kelas != null && !kelas.getNama().trim().isEmpty() ? Restrictions
														.ilike("kelas", kelas.getNama().trim(), MatchMode.EXACT)
														: Restrictions.sqlRestriction("true"))

												.createCriteria("mahasiswa")

												.add(dosen != null ? Restrictions.eq("dosen", dosen.getId())
														: Restrictions.sqlRestriction("1=1"))

												.addOrder(Order.desc("tahunangkatan"))
												.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))

												.add(searchnama.getValue().trim().isEmpty()
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.ilike("nama", searchnama.getValue(),
																MatchMode.ANYWHERE))
												.add(searchnim.getValue().trim().isEmpty()
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.ilike("nim", searchnim.getValue(),
																MatchMode.ANYWHERE))
												.add(searchprogram.getSelectedItem() == null
														|| searchprogram.getSelectedItem().getValue() == null
																? Restrictions.sqlRestriction("1=1")
																: Restrictions.eq("program",
																		searchprogram.getSelectedItem().getValue()))
												.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

												.add(searchStatusAwalMahasiswa.getSelectedItem() == null
														|| searchStatusAwalMahasiswa.getSelectedItem()
																.getValue() == null
																		? Restrictions.sqlRestriction("1=1")
																		: Restrictions.eq("statusAwalMahasiswa",
																				searchStatusAwalMahasiswa
																						.getSelectedItem().getValue()))
												.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("tahunangkatan",
																searchtahun.getValue().intValue()))

												.createCriteria("jurusan", Criteria.LEFT_JOIN)

												.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
												.add(searchjenjang.getSelectedItem() == null
														|| searchjenjang.getSelectedItem().getValue() == null
																? Restrictions.sqlRestriction("1=1")
																: Restrictions.eq("jenjang",
																		searchjenjang.getSelectedItem().getValue()));

										List data = criteria.setMaxResults(1048576).list();
										intbox.setValue(data.size());
										System.out.println("data = " + data.size());

										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("CETAK DATA");
										sheet.setDefaultColumnWidth(20);
										int rowIndex = 0;

										XSSFRow rowhead = sheet.createRow((short) 0);
										String[] columns = new String[] { "id", "mahasiswa", "perkuliahan",
												"tahun akademik", "semester", "persetujuan", "keterangan", "komentar",
												"kode matakuliah", "nama matakuliah", "sks", "kelas", "prodi", "dosen",
												"nim", "nama" };
										for (int i = 0; i < columns.length; i++) {
											rowhead.createCell(i).setCellValue(columns[i].toUpperCase());
										}

										XSSFCellStyle notLocked = workbook.createCellStyle();
										notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
										notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

										for (Object o : data) {
											try {
												rowIndex++;
												if (o == null) {
													continue;
												}
												label.setValue("Sedang memproses data " + o.toString() + " ("
														+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size())
														+ " %)");

												XSSFRow row = sheet.createRow(rowIndex);

												Detailperkuliahan detailperkuliahan = (Detailperkuliahan) o;

												Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
														&& detailperkuliahan.getPerkuliahan().getMatakuliah() != null
																? detailperkuliahan.getPerkuliahan().getMatakuliah()
																: detailperkuliahan.getMatakuliahKonversi();

												String keterangan = "";
												if (detailperkuliahan.getMatakuliahKonversi() != null) {
													keterangan = "Persetujuan matakuliah \""
															+ detailperkuliahan.getMatakuliahKonversi()
															+ "\"  tidak bisa dibatalkan, karena merupakan matakulian konversi";
												} else if (detailperkuliahan.getTotalNilai() != null
														&& detailperkuliahan.getTotalNilai() > 1.0) {
													keterangan = "Persetujuan tidak bisa dibatalkan, karena telah dinilai "
															+ Common.numberFormat.get()
																	.format(detailperkuliahan.getTotalNilai());
												}

												List<String> komentars = session.createCriteria(Komentar.class)
														.add(Restrictions.eq("detailperkuliahan",
																detailperkuliahan.getId()))
														.setProjection(Projections.property("komentar")).list();

												row.createCell(0).setCellValue(detailperkuliahan.getId());
												row.createCell(1)
														.setCellValue(detailperkuliahan.getMahasiswa() == null ? ""
																: detailperkuliahan.getMahasiswa().getNim() + "-"
																		+ detailperkuliahan.getMahasiswa().getNama());

												row.createCell(2)
														.setCellValue(detailperkuliahan.getPerkuliahan() == null
																? (detailperkuliahan.getMatakuliahKonversi() == null
																		? ""
																		: detailperkuliahan.getMatakuliahKonversi()
																				.getKode()
																				+ "-"
																				+ detailperkuliahan
																						.getMatakuliahKonversi()
																						.getNama())
																: detailperkuliahan.getPerkuliahan().info());

												row.createCell(3).setCellValue(detailperkuliahan.getTahunAkademik());
												row.createCell(4).setCellValue(detailperkuliahan.getSemester());

												XSSFCell cell = row.createCell(5);
												cell.setCellStyle(notLocked);
												cell.setCellValue(detailperkuliahan.getPersetujuan()
														.equals(Detailperkuliahan.DISETUJUI));

												row.createCell(6).setCellValue(keterangan);
												row.createCell(7).setCellValue(komentars.toString());

												row.createCell(8)
														.setCellValue(matakuliah == null ? "" : matakuliah.getKode());
												row.createCell(9)
														.setCellValue(matakuliah == null ? "" : matakuliah.getNama());
												row.createCell(10)
														.setCellValue(matakuliah == null ? 0 : matakuliah.getSks());
												row.createCell(11)
														.setCellValue(detailperkuliahan.getPerkuliahan() == null ? ""
																: detailperkuliahan.getPerkuliahan().getKelas());
												row.createCell(12).setCellValue(
														detailperkuliahan.getPerkuliahan() == null || detailperkuliahan
																.getPerkuliahan().getJurusan() == null ? ""
																		: detailperkuliahan.getPerkuliahan()
																				.getJurusan().getNama());

												String d = "";
												if (detailperkuliahan.getPerkuliahan() != null) {
													for (Dosen dsn : detailperkuliahan.getPerkuliahan()
															.populateDosenBuNama()) {
														d += d.isEmpty() ? dsn.getNama() : ", " + dsn.getNama();
													}
												}
												row.createCell(13).setCellValue(d);

												row.createCell(14)
														.setCellValue(detailperkuliahan.getMahasiswa() == null ? ""
																: detailperkuliahan.getMahasiswa().getNim());
												row.createCell(15)
														.setCellValue(detailperkuliahan.getMahasiswa() == null ? ""
																: detailperkuliahan.getMahasiswa().getNama());

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}

										try {
											FileOutputStream fileOut = new FileOutputStream(filename);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											Common.tampilErrorJikaAdmin(e);
										}

										data.clear();
										data = null;
										label.setValue("");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										label.setValue("-");
									}
									HibernateUtil.closeSession();
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

	public static void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		final Label peringatan = new Label("");
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload KRS Mahasiswa");
		final Label downloadPath = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); }
						catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan"); }
					}
					MyMessageboxConfig.show(
							"Upload data KRS berhasil dilakukan." + report.getRingkasan()
									+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							Detailperkuliahan detailperkuliahan = id == null || id.equals(-1L) ? null
									: (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
											.add(Restrictions.idEq(id)).uniqueResult();

							// System.out.println("detailperkuliahan = " +
							// detailperkuliahan);

							if (detailperkuliahan == null) {
								continue;
							}

							Boolean persetujuan = Common.getSheetContentAsBoolean(sheet, 5, i);

							System.out.println("persetujuan = " + persetujuan);

							detailperkuliahan.setPersetujuan(
									persetujuan ? Detailperkuliahan.DISETUJUI : Detailperkuliahan.BELUM_DISETUJUI);

							session.getTransaction().begin();
							Common.refreshUpdate(session, detailperkuliahan);
							session.getTransaction().commit();

							String nimKrs = (detailperkuliahan.getMahasiswa() != null ? detailperkuliahan.getMahasiswa().getNim() : "-");
							String kodeMkKrs = (detailperkuliahan.getPerkuliahan() != null && detailperkuliahan.getPerkuliahan().getMatakuliah() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah().getKode() : "-");
							report.sukses(i, nimKrs + "/" + kodeMkKrs, "persetujuan=" + persetujuan);
							label.setValue("Upload data \"" + detailperkuliahan + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "row " + i, e, "Periksa data KRS baris " + i);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/MonitorKRSMahasiswaAction.java:2440");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) MonitorKRSMahasiswaAction laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	public MyToolbarbuttonConfig bersihkanKrsMahasiswaDouble(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaranDanSemua(tahunAkademik);
				final Combobox genapGanjil = new Combobox();
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Perkuliahan.GENAP);
				comboitem.setValue(Perkuliahan.GENAP);
				genapGanjil.appendChild(comboitem);
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.GANJIL);
				comboitem.setValue(Perkuliahan.GANJIL);
				genapGanjil.appendChild(comboitem);

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				genapGanjil.appendChild(comboitem);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
				row.appendChild(tahunAkademik);
				tahunAkademik.setWidth("90%");
				tahunAkademik.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
				row.appendChild(genapGanjil);
				genapGanjil.setWidth("90%");
				genapGanjil.setReadonly(true);

				Common.selectComboItem(genapGanjil, null);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig cariID;
				row.appendChild(cariID = new MyCheckboxConfig(
						"Cari kesamaan matakuliah berdasarkan ID, jika tidak dipilih akan mencari berdasar kode matakuliah"));
				cariID.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig abaikanBedaSemester;
				row.appendChild(abaikanBedaSemester = new MyCheckboxConfig("Abaikan beda semester"));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig abaikanBedaTahunAkademik;
				row.appendChild(abaikanBedaTahunAkademik = new MyCheckboxConfig("Abaikan beda tahun akademik"));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig utamakanYangAdaDosenNya;
				row.appendChild(utamakanYangAdaDosenNya = new MyCheckboxConfig("Utamakan yang ada dosen-nya"));
				utamakanYangAdaDosenNya.setChecked(true);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();

						final List<Long> dataDihapus = new ArrayList<Long>();

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						Clients.showBusy(label.getValue());

						final boolean abaikanBedaSmt = abaikanBedaSemester.isChecked();
						final boolean abaikanBedaTA = abaikanBedaTahunAkademik.isChecked();
						final boolean cariBerdasarID = cariID.isChecked();
						final boolean utamakanDosen = utamakanYangAdaDosenNya.isChecked();

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".xlsx");
						final File file;
						(file = new File(filename)).createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								try {

									Clients.showBusy(label.getValue());
									System.out.println("label " + label.getValue());

									if (label.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (label.getValue().isEmpty()) {

										Center center = new Center();
										final MyWindow window = new MyWindow("Cetak Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("97%");
										window.setWidth("90%");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);

										ais.ui.util.ZkCompat.setFlex(center, true);
										center.setParent(borderlayout);

										System.out.println("loading file " + file.getAbsolutePath());
										Common.clear(center);
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										Common.clear(center);
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 3);
										spreadsheet.setMaxcolumns(8);
										ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

										South south = new South();
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										// toolbar.setHeight("25px");
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {

												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}
										});
										print.setParent(toolbar);

										final int jumlahDouble = dataDihapus.size();
										MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig(
												"Proses pembersihan data double", "/img/excel.png");
										proses.setVisible(jumlahDouble > 0);
										proses.setTooltiptext(
												"Menghapus permanen salah satu data KRS ganda yang ditandai warna merah");
										proses.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												final String daftarId;
												{
													StringBuilder sb = new StringBuilder();
													for (Long id : dataDihapus) {
														if (sb.length() > 0) {
															sb.append(",");
														}
														sb.append(id);
													}
													daftarId = sb.toString();
												}
												MyMessageboxConfig.show(
														"Mohon perhatian Bapak/Ibu. Tindakan ini akan MENGHAPUS SECARA PERMANEN sebanyak "
																+ jumlahDouble
																+ " baris data KRS (detail perkuliahan) yang ditandai dengan warna merah, yaitu salah satu dari setiap pasangan/kelompok data ganda (double)."
																+ "\n\nKonsekuensi yang perlu Bapak/Ibu pahami sebelum melanjutkan:"
																+ "\n1. Penghapusan bersifat PERMANEN dan TIDAK DAPAT DIBATALKAN maupun dikembalikan. Berbeda dengan penonaktifan, data yang telah dihapus tidak lagi tersimpan di basis data."
																+ "\n2. Apabila baris KRS yang dihapus ternyata sudah memiliki nilai, kehadiran, atau rekaman akademik lain, keterkaitan tersebut dapat ikut hilang atau menjadi tidak lengkap."
																+ "\n3. Sangat disarankan Bapak/Ibu MENGUNDUH data terlebih dahulu (tombol \"Download Data\") sebagai cadangan sebelum melanjutkan."
																+ "\n\nApakah Bapak/Ibu benar-benar yakin ingin menghapus permanen " + jumlahDouble
																+ " baris data KRS ganda tersebut?",
														"Konfirmasi Penghapusan Permanen Data Ganda",
														org.zkoss.zul.Messagebox.YES | org.zkoss.zul.Messagebox.NO,
														MyMessageboxConfig.QUESTION, new EventListener() {
															@Override
															public void onEvent(Event ev) throws Exception {
																if (!"onYes".equals(ev.getName())) {
																	return;
																}
																if (daftarId.isEmpty()) {
																	return;
																}
																String sql = "delete from detailperkuliahan where id in (" + daftarId + ")";
																HibernateUtil.currentSession().createSQLQuery(sql).executeUpdate();
																onSearchDefault(ev);
																window.detach();
															}
														});
											}
										});
										proses.setParent(toolbar);

										window.setVisible(true);
										window.onModal();

										Clients.clearBusy();
										timer.detach();
									}

								} catch (Exception e) {
									Clients.clearBusy();
								}

							}
						});
						timer.start();

						try {

							Clients.showBusy(label.getValue());

							new Thread(new Runnable() {

								@SuppressWarnings("unchecked")
								@Override
								public void run() {
									try {

									try {
										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("DATA KRS");
										sheet.setDefaultColumnWidth(20);
										XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
										lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
										lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
										lockedNumericStyle.setLocked(true);

										Session session = HibernateUtil.currentNativeSession();

										String sql = "select \na.mahasiswa,"
												+ (cariBerdasarID ? "c.id as kode" : "c.kode") + ","
												+ (abaikanBedaSmt ? "max(a.semester) as semester" : "a.semester") + ","
												+ (abaikanBedaTA ? "max(a.tahunakademik) as tahunakademik"
														: "a.tahunakademik")
												+ " \n" + "from detailperkuliahan a  \n"
												+ "left join perkuliahan b on (a.perkuliahan=b.id) \n"
												+ "inner join matakuliah c on (a.matakuliah_konversi=c.id or b.matakuliah=c.id) \n"
												+ "where  "
												+ (tahunAkademik.getSelectedItem() == null
														|| tahunAkademik.getSelectedItem().getValue() == null
																? "1=1"
																: "a.tahunakademik='"
																		+ tahunAkademik.getSelectedItem().getValue()
																		+ "' ")
												+ (semesterPendek == null ? " and b.status_semesterpendek is null "
														: "  and b.status_semesterpendek = " + semesterPendek + " ")

												+ (genapGanjil.getSelectedItem() == null
														|| genapGanjil.getSelectedItem().getValue() == null
																? ""
																: " and a.semester % 2 = "
																		+ (genapGanjil.getSelectedItem().getValue()
																				.equals(Perkuliahan.GANJIL) ? "1"
																						: "0"))
												+ " \ngroup by a.mahasiswa," + (cariBerdasarID ? "c.id" : "c.kode")
												+ (abaikanBedaSmt ? "" : ",a.semester")
												+ (abaikanBedaTA ? "" : ",a.tahunakademik") + " \n"
												+ "having count(a.id)>1";
										List<Object[]> data = session.createSQLQuery(sql).list();
										intbox.setValue(data.size());
										System.out.println("sql = " + sql + "\n\ndata = " + data.size());

										int rowIndex = 0;

										XSSFRow rowhead = sheet.createRow((short) 0);
										String[] columns = new String[] { "id", "mahasiswa", "matakuliah",
												"tahun akademik", "semester", "nilai", "huruf", "dosen" };
										for (int i = 0; i < columns.length; i++) {
											rowhead.createCell(i).setCellValue(columns[i].toUpperCase());
										}

										for (Object[] o : data) {
											try {
												if (o == null || o[0] == null || o[1] == null || o[2] == null
														|| o[3] == null) {
													continue;
												}

												Long mhsId = Long.parseLong(o[0].toString());
												Integer semester = Integer.parseInt(o[2].toString());
												String kode = o[1].toString().trim();
												String tahunakademik = o[3].toString().trim();

												List<Detailperkuliahan> detailperkuliahans = session
														.createCriteria(Detailperkuliahan.class)
														.add(abaikanBedaSmt ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("semester", semester))
														.add(abaikanBedaTA ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("tahunAkademik", tahunakademik))

														.add(Restrictions.eq("mahasiswa.id", mhsId))
														.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
														.createAlias("perkuliahan.matakuliah", "matakuliah",
																Criteria.LEFT_JOIN)
														.createAlias("matakuliahKonversi", "matakuliahKonversi",
																Criteria.LEFT_JOIN)

														.add(cariBerdasarID ? Restrictions.or(
																Restrictions.eq("matakuliah.id", Long.parseLong(kode)),
																Restrictions.eq("matakuliahKonversi.id",
																		Long.parseLong(kode)))
																: Restrictions.or(
																		Restrictions.eq("matakuliah.kode", kode),
																		Restrictions.eq("matakuliahKonversi.kode",
																				kode)))

														.addOrder(utamakanDosen ? Order.desc("perkuliahan.dosen1")
																: Order.asc("totalNilai"))
														.addOrder(utamakanDosen ? Order.asc("totalNilai")
																: Order.desc("perkuliahan.dosen1"))

														.list();

												System.out.println("mhsId = " + mhsId + " kode = " + kode
														+ ", detailperkuliahans = " + detailperkuliahans.size());

												int index = 0;
												for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {

													rowIndex++;
													XSSFRow row = sheet.createRow(rowIndex);
													XSSFCell cell0 = row.createCell(0);
													if (index < detailperkuliahans.size() - 1) {
														dataDihapus.add(detailperkuliahan.getId());
														cell0.setCellStyle(lockedNumericStyle);
													}
													index++;

													label.setValue("Sedang memproses data " + detailperkuliahan + " ");

													cell0.setCellValue(detailperkuliahan.getId());
													row.createCell(1)
															.setCellValue(detailperkuliahan.getMahasiswa() == null ? ""
																	: detailperkuliahan.getMahasiswa().toString());
													row.createCell(2)
															.setCellValue(detailperkuliahan.getPerkuliahan() == null
																	? (detailperkuliahan.getMatakuliahKonversi() == null
																			? ""
																			: detailperkuliahan.getMatakuliahKonversi()
																					.toString())
																	: detailperkuliahan.getPerkuliahan().toString());
													row.createCell(3)
															.setCellValue(detailperkuliahan.getTahunAkademik());
													row.createCell(4).setCellValue(detailperkuliahan.getSemester());
													row.createCell(5).setCellValue(detailperkuliahan.getTotalNilai());
													row.createCell(6).setCellValue(detailperkuliahan.getNilaiHuruf());

													row.createCell(7)
															.setCellValue(detailperkuliahan.getPerkuliahan() != null
																	? detailperkuliahan.getPerkuliahan().populateDosen()
																			.values().toString()
																	: "");
												}

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}

										try {
											FileOutputStream fileOut = new FileOutputStream(filename);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											Common.tampilErrorJikaAdmin(e);
										}
										System.out.println("Your excel file has been generated! ");
										data.clear();
										data = null;
										label.setValue("");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										label.setValue("-");
									}
									HibernateUtil.closeSession();
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

	public MyToolbarbuttonConfig sesuikanKrsMahasiswaSesuaiTa(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Akademik", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaranDanSemua(tahunAkademik);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
				row.appendChild(tahunAkademik);
				tahunAkademik.setWidth("90%");
				tahunAkademik.setReadonly(true);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();

						final List<Long> dataDihapus = new ArrayList<Long>();

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						Clients.showBusy(label.getValue());

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".xlsx");
						final File file;
						(file = new File(filename)).createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								try {

									Clients.showBusy(label.getValue());
									System.out.println("label " + label.getValue());

									if (label.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (label.getValue().isEmpty()) {

										Center center = new Center();
										final MyWindow window = new MyWindow("Cetak Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("97%");
										window.setWidth("90%");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);

										ais.ui.util.ZkCompat.setFlex(center, true);
										center.setParent(borderlayout);

										System.out.println("loading file " + file.getAbsolutePath());
										Common.clear(center);
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										Common.clear(center);
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 3);
										spreadsheet.setMaxcolumns(8);
										ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

										South south = new South();
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										// toolbar.setHeight("25px");
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {

												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}
										});
										print.setParent(toolbar);

										MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig(
												"Proses penyesuaian KRS sesuai dengan tahun akademik yang diambil",
												"/img/excel.png");
										proses.setVisible(dataDihapus.size() > 0);
										proses.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												Session session = HibernateUtil.currentSession();

												String warning = "";

												for (Long id : dataDihapus) {

													Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
															.createCriteria(Detailperkuliahan.class)
															.add(Restrictions.idEq(id)).uniqueResult();
													if (detailperkuliahan != null
															&& detailperkuliahan.getPerkuliahan() != null) {

														String info = detailperkuliahan.getPerkuliahan().info();

														String ta = detailperkuliahan.getTahunAkademik();

														if (detailperkuliahan != null
																&& detailperkuliahan.getPerkuliahan() != null) {
															detailperkuliahan.setTahunAkademik(detailperkuliahan
																	.getPerkuliahan().getTahunAjaran());

															Perkuliahan perkuliahan = (Perkuliahan) session
																	.createCriteria(Perkuliahan.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("jurusan",
																			detailperkuliahan.getPerkuliahan()
																					.getJurusan()))
																	.add(Restrictions.eq("matakuliah",
																			detailperkuliahan.getPerkuliahan()
																					.getMatakuliah()))
																	.add(Restrictions.eq("tahunAjaran", ta))
																	.add(Restrictions.eq("kelas",
																			detailperkuliahan.getPerkuliahan()
																					.getKelas()))
																	.add(Restrictions.eq("program",
																			detailperkuliahan.getPerkuliahan()
																					.getProgram()))
																	.setMaxResults(1).uniqueResult();

															if (perkuliahan == null) {
																perkuliahan = (Perkuliahan) session
																		.createCriteria(Perkuliahan.class)
																		.add(Restrictions.or(
																				Restrictions.isNull("aktif"),
																				Restrictions.eq("aktif", true)))
																		.add(Restrictions.eq("jurusan",
																				detailperkuliahan.getPerkuliahan()
																						.getJurusan()))
																		.add(Restrictions.eq("matakuliah",
																				detailperkuliahan.getPerkuliahan()
																						.getMatakuliah()))
																		.add(Restrictions.eq("tahunAjaran", ta))
																		.add(Restrictions.eq("program",
																				detailperkuliahan.getPerkuliahan()
																						.getProgram()))
																		.setMaxResults(1).uniqueResult();
															}

															if (perkuliahan == null) {
																perkuliahan = (Perkuliahan) session
																		.createCriteria(Perkuliahan.class)
																		.add(Restrictions.or(
																				Restrictions.isNull("aktif"),
																				Restrictions.eq("aktif", true)))
																		.add(Restrictions.eq("jurusan",
																				detailperkuliahan.getPerkuliahan()
																						.getJurusan()))
																		.add(Restrictions.eq("matakuliah",
																				detailperkuliahan.getPerkuliahan()
																						.getMatakuliah()))
																		.add(Restrictions.eq("tahunAjaran", ta))
																		.add(Restrictions.eq("kelas",
																				detailperkuliahan.getPerkuliahan()
																						.getKelas()))
																		.setMaxResults(1).uniqueResult();
															}

															if (perkuliahan == null) {
																perkuliahan = (Perkuliahan) session
																		.createCriteria(Perkuliahan.class)
																		.add(Restrictions.or(
																				Restrictions.isNull("aktif"),
																				Restrictions.eq("aktif", true)))
																		.add(Restrictions.eq("jurusan",
																				detailperkuliahan.getPerkuliahan()
																						.getJurusan()))
																		.add(Restrictions.eq("matakuliah",
																				detailperkuliahan.getPerkuliahan()
																						.getMatakuliah()))
																		.add(Restrictions.eq("tahunAjaran", ta))
																		.setMaxResults(1).uniqueResult();
															}

															if (perkuliahan != null) {
																Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
																String s = (detailperkuliahan.getPerkuliahan()
																		.getSemester() % 1) == 1 ? Perkuliahan.GANJIL
																				: Perkuliahan.GENAP;
																Integer smt = Common.getSemester(
																		mahasiswa.getTahunangkatan(), ta, s,
																		mahasiswa.getPindahKeKampusIniMasukSemester(),
																		mahasiswa.getSemesterMulai());
																detailperkuliahan.setSemester(smt);
																detailperkuliahan.setPerkuliahan(perkuliahan);
																Common.refreshUpdate(session, detailperkuliahan);
															} else {
																warning += "Perkuliahan => " + info
																		+ " => tidak ditemukan di tahun akademik " + ta
																		+ ".\n\n";
															}
														}

													}

												}

												if (!warning.isEmpty()) {
													MyMessageboxConfig.show(warning, "Peringatan",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												onSearchDefault(event);
												window.detach();

											}
										});
										proses.setParent(toolbar);

										window.setVisible(true);
										window.onModal();

										Clients.clearBusy();
										timer.detach();
									}

								} catch (Exception e) {
									Clients.clearBusy();
								}

							}
						});
						timer.start();

						try {

							Clients.showBusy(label.getValue());

							new Thread(new Runnable() {

								@SuppressWarnings("unchecked")
								@Override
								public void run() {
									try {

									try {
										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("DATA KRS");
										sheet.setDefaultColumnWidth(20);
										XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
										lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
										lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
										lockedNumericStyle.setLocked(true);

										Session session = HibernateUtil.currentNativeSession();

										String sql = "select \n a.id as id_perkuliahan,d.nim||' '||d.nama as mahasiswa,c.kode||' '||c.nama as matakuliah,a.semester,a.tahunakademik,b.semester as smt,b.tahun_ajaran,a.total_nilai,a.nilai_huruf \n"
												+ "from detailperkuliahan a  \n"
												+ "inner join perkuliahan b on (a.perkuliahan=b.id) \n"
												+ "inner join matakuliah c on (c.id=b.matakuliah) \n"
												+ "inner join mahasiswa d on (d.id=a.mahasiswa) \n where "
												+ (tahunAkademik.getSelectedItem() == null
														|| tahunAkademik.getSelectedItem().getValue() == null
																? "1=1"
																: " (a.tahunakademik='"
																		+ tahunAkademik.getSelectedItem().getValue()
																		+ "' or b.tahun_ajaran='"
																		+ tahunAkademik.getSelectedItem().getValue()
																		+ "')")
												+ " and a.tahunakademik!=b.tahun_ajaran";
										List<Object[]> data = session.createSQLQuery(sql).list();
										intbox.setValue(data.size());
										System.out.println("sql = " + sql + "\n\ndata = " + data.size());

										int rowIndex = 0;

										XSSFRow rowhead = sheet.createRow((short) 0);
										String[] columns = new String[] { "id", "mahasiswa", "matakuliah",
												"semester mhs", "tahun akademik mhs", "semester yg diambil",
												"tahun akademik yg diambil" };
										for (int i = 0; i < columns.length; i++) {
											rowhead.createCell(i).setCellValue(columns[i].toUpperCase());
										}

										for (Object[] o : data) {
											try {
												if (o == null || o[0] == null || o[1] == null || o[2] == null
														|| o[3] == null) {
													continue;
												}

												Long mhsId = Long.parseLong(o[0].toString());

												String mahasiswa = o[1].toString().trim();
												String matakuliah = o[2].toString().trim();
												Integer semesterMhs = Integer.parseInt(o[3].toString());

												String tahunakademik = o[4].toString().trim();

												Integer semesterDiambil = Integer.parseInt(o[5].toString());

												String tahunakademikDiambil = o[6].toString().trim();

												rowIndex++;
												XSSFRow row = sheet.createRow(rowIndex);
												XSSFCell cell0 = row.createCell(0);
												dataDihapus.add(mhsId);
												cell0.setCellStyle(lockedNumericStyle);

												label.setValue("Sedang memproses data " + mahasiswa + " ");

												cell0.setCellValue(mhsId);

												row.createCell(1).setCellValue(mahasiswa);
												row.createCell(2).setCellValue(matakuliah);

												row.createCell(3).setCellValue(semesterMhs);

												row.createCell(4).setCellValue(tahunakademik);
												row.createCell(5).setCellValue(semesterDiambil);
												row.createCell(6).setCellValue(tahunakademikDiambil);
												row.createCell(7).setCellValue(o[7] == null ? "" : o[7].toString());
												row.createCell(8).setCellValue(o[8] == null ? "" : o[8].toString());

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}

										try {
											FileOutputStream fileOut = new FileOutputStream(filename);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											Common.tampilErrorJikaAdmin(e);
										}
										System.out.println("Your excel file has been generated! ");
										data.clear();
										data = null;
										label.setValue("");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										label.setValue("-");
									}
									HibernateUtil.closeSession();
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

	public static Hbox tampilkanInfoMahasiswa(KrsMahasiswa krsMahasiswa) throws Exception {
		Hbox hbox = new Hbox();
		Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
		CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

		Vbox vbox = new Vbox();

		vbox.setParent(hbox);

		new Label(mahasiswa.getNim()).setParent(vbox);
		new Label(mahasiswa.getNama()).setParent(vbox);

		new MyLabelKecil("TA/Smt : " + krsMahasiswa.getTahunAkademik() + "/"
				+ (krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)).setParent(vbox);
		new MyLabelKecil("IP/IPK : " + Common.numberFormat.get().format(krsMahasiswa.getIps()) + "/"
				+ Common.numberFormat.get().format(krsMahasiswa.getIpk())).setParent(vbox);
		new MyLabelKecil("SKS/SKSK : " + Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil()) + "/"
				+ Common.numberFormat.get().format(krsMahasiswa.getSksk())).setParent(vbox);
		new MyLabelKecil("Catatan : " + krsMahasiswa.getCatatan() + " " + krsMahasiswa.getCatatanKhs()).setParent(vbox);

		Vbox vbox1 = new Vbox();
		vbox1.setParent(vbox);
		Hbox hbox1 = new Hbox();

		LampiranLain.createDownloadUploadFileLain(hbox1, krsMahasiswa.getId(), "KRS_DISETUJUI", "Catatan", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, false);

		hbox1.setParent(vbox1);

		return hbox;
	}
}
