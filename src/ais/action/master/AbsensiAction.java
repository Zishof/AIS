package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.DesktopUnavailableException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardRekapAbsensiMahasiswa;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiMahasiswaPerMatakuliah;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerKelas;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerKelasDanKuliah;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerMahasiswa;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.DetailpertemuanHelper;
import ais.action.master.helper.ProsesKehadiranDosen;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.format1.akademik.LaporanAbsensiMahasiswa;
import ais.action.report.format1.akademik.LaporanKehadiranAsisten;
import ais.action.report.format1.akademik.LaporanKehadiranDosen;
import ais.action.report.format1.akademik.LaporanKehadiranMahasiswa;
import ais.common.AsyncTaskManager;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk absensi. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code AmbilDataMatakuliahBanbox searchmatakuliah}, {@code AmbilDataDosenBanbox
 * searchdosen}, {@code Textbox searchkelas}, {@code MyCheckboxConfig searchparalel}, {@code MyCheckboxConfig
 * searchtanpakelas}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code
 * initCriteria()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code
 * onProsesKehadiran()}); operasi domain lain ({@code onEkstrakurikuler()}, {@code onPraPerkuliahan()}, {@code
 * onAbsensiSp()}, {@code onAbsensiRemedial()}, {@code onLaporanKehadiranDosen()}, {@code
 * onLaporanKehadiranMahasiswa()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class AbsensiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091228301468178L;
	protected MyWindow addWindow;
	protected Paging paging;
	protected MyGrid grid;
	protected AmbilDataMatakuliahBanbox searchmatakuliah;
	protected AmbilDataDosenBanbox searchdosen;

	protected Textbox searchkelas;
	protected MyCheckboxConfig searchparalel;
	protected MyCheckboxConfig searchtanpakelas;
	protected Boolean merupakanRemedial = false;
	protected AmbilDataRuangBanbox searchruang;

	protected Combobox searchhari;
	protected Combobox searchTahunAjaran;
	protected Combobox searchsemester;
	protected Combobox searchJenisSemester;
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

	protected Perkuliahan perkuliahan;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected Tabpanel absensiSp;

	protected Tabpanel absensiRemedial;

	protected Tabs tabsAbsensi;

	protected MyToolbarbuttonConfig find;

	protected Integer semesterPendek = null;
	protected Integer ekstrakurikuler = null;
	protected Boolean merupakanPraPerkuliahan = false;

	protected Tabpanel ekstrakurikulerTab;

	public void onEkstrakurikuler(Event event) {

		if (ekstrakurikulerTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(ekstrakurikulerTab);
			include.setSrc("/pages/master/absensi_ekstrakurikuler.zul");
		}
	}

	protected Tabpanel praPerkuliahanTab;

	public void onPraPerkuliahan(Event event) {

		if (praPerkuliahanTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(praPerkuliahanTab);
			include.setSrc("/pages/master/absensi_pra_perkuliahan.zul");
		}
	}

	public void onAbsensiSp(Event event) {

		if (absensiSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(absensiSp);
			include.setSrc("/pages/master/absensi_sp.zul");
		}
	}

	public void onAbsensiRemedial(Event event) {

		if (absensiRemedial.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(absensiRemedial);
			include.setSrc("/pages/master/absensi_remedial.zul");
		}
	}

	protected Tabpanel laporanAbsensiDosen;

	public void onLaporanKehadiranDosen(Event event) {

		if (laporanAbsensiDosen.getChildren().size() == 0) {
			LaporanKehadiranDosen laporan = new LaporanKehadiranDosen();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiDosen);
		}
	}

	protected Tabpanel laporanRekapAbsensiMahasiswa;

	public void onLaporanKehadiranMahasiswa(Event event) {

		if (laporanRekapAbsensiMahasiswa.getChildren().size() == 0) {
			LaporanKehadiranMahasiswa laporan = new LaporanKehadiranMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanRekapAbsensiMahasiswa);
		}
	}

	protected Tabpanel laporanRekapAbsensiAsisten;

	public void onLaporanKehadiranAsisten(Event event) {

		if (laporanRekapAbsensiAsisten.getChildren().size() == 0) {
			LaporanKehadiranAsisten laporan = new LaporanKehadiranAsisten();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanRekapAbsensiAsisten);
		}
	}

	protected Tabpanel laporanAbsensiMahasiswa;

	public void onLaporanAbsensiMahasiswa(Event event) {

		if (laporanAbsensiMahasiswa.getChildren().size() == 0) {
			LaporanAbsensiMahasiswa laporan = new LaporanAbsensiMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiMahasiswa);
		}
	}

	protected Tabpanel rekapitulasiAbsensiMahasiswa;

	public void onRekapitulasiAbsensiMahasiswa(Event event) {

		if (rekapitulasiAbsensiMahasiswa.getChildren().size() == 0) {
			DashboardRekapAbsensiMahasiswa laporan = new DashboardRekapAbsensiMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiMahasiswa,
				"Rekap Absensi Mahasiswa",
				"Ringkasan kehadiran mahasiswa seluruh mata kuliah dalam satu semester.");
		}
	}

	protected Tabpanel prosesKehadiranTab;

	public void onProsesKehadiran(Event event) {

		if (prosesKehadiranTab.getChildren().size() == 0) {
			ProsesKehadiranDosen laporan = new ProsesKehadiranDosen();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.prosesKehadiranTab);
		}
	}

	protected Tabpanel rekapitulasiAbsensiMatakuliah;

	public void onRekapitulasiAbsensiMatakuliah(Event event) {

		if (rekapitulasiAbsensiMatakuliah.getChildren().size() == 0) {
			DashboardRekapAbsensiMahasiswaPerMatakuliah laporan = new DashboardRekapAbsensiMahasiswaPerMatakuliah();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiMatakuliah,
				"Absensi per Mata Kuliah",
				"Persentase kehadiran mahasiswa dikelompokkan berdasarkan tiap mata kuliah.");
		}
	}

	protected Tabpanel rekapitulasiAbsensiPerMahasiswa;

	public void onRekapitulasiAbsensiPerMahasiswa(Event event) {

		if (rekapitulasiAbsensiPerMahasiswa.getChildren().size() == 0) {
			DashboardRekapAbsensiPerMahasiswa laporan = new DashboardRekapAbsensiPerMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiPerMahasiswa,
				"Absensi per Mahasiswa",
				"Tingkat kehadiran tiap mahasiswa dari seluruh sesi perkuliahan yang dijadwalkan.");
		}
	}

	protected Tabpanel rekapitulasiAbsensiKelas;

	public void onRekapitulasiAbsensiKelas(Event event) {

		if (rekapitulasiAbsensiKelas.getChildren().size() == 0) {
			DashboardRekapAbsensiPerKelas laporan = new DashboardRekapAbsensiPerKelas();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiKelas,
				"Absensi per Kelas",
				"Ringkasan kehadiran mahasiswa diurutkan berdasarkan kelas.");
		}
	}

	protected Tabpanel rekapitulasiAbsensiKelasDanMatakuliah;
	private Dosen dosen;

	public void onRekapitulasiAbsensiKelasDanMatakuliah(Event event) {

		if (rekapitulasiAbsensiKelasDanMatakuliah.getChildren().size() == 0) {
			DashboardRekapAbsensiPerKelasDanKuliah laporan = new DashboardRekapAbsensiPerKelasDanKuliah();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiKelasDanMatakuliah,
				"Absensi Kelas & Mata Kuliah",
				"Kehadiran mahasiswa dilihat dari perpaduan kelas dan mata kuliah.");
		}
	}

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

	private North mynorth;
	private Combobox ta;
	private Combobox smt;
	private Combobox hari;
	private Textbox keyword;

	private List<Perkuliahan> perkuliahans;
	private PerguruanTinggi perguruanTinggi;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (execution.getParameter("user") != null) {
			tbmuser = Common.getCurrentUser();
		} else if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		} else {
			tbmuser = Common.getCurrentUser();
		}
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
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

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");

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
			toolbar.appendChild(keyword);
			toolbar.appendChild(button);
			toolbar.appendChild(new Space());
			toolbar.appendChild(kbb);
			toolbar.appendChild(rekap);
			toolbar.setParent(mynorth);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(new Event("cari"));
				}
			});
			onSearchDefault(null);

		} else {
			if (searchtanpakelas != null) {
				searchtanpakelas.setVisible(
						Common.bolehKonfigurasi("tampilkan_search_kelas_di_penjadwalan", Konfigurasi.TIDAK_AKTIF));
			}

			if (searchasisten != null) {

				if (tbmuser != null && tbmuser.getMahasiswa() != null) {
					searchasisten.setAttribute("mahasiswa", tbmuser.getMahasiswa());
					searchasisten.setValue(tbmuser.getMahasiswa().getNama());
					searchasisten.setDisabled(true);

					if (tabsAbsensi != null) {
						@SuppressWarnings("unchecked")
						List<Tab> tabs = tabsAbsensi.getChildren();
						for (Tab tab : tabs) {
							tab.setVisible(tab.getLabel().trim().equalsIgnoreCase("Absensi")
									|| tab.getLabel().trim().equalsIgnoreCase("Absensi SP"));
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

			searchruang.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			searchkurikulum.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			searchJenisSemester.setReadonly(true);

			Common.generateTahunAjaranDanSemua(searchTahunAjaran);
			Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

			Common.initPrograms(searchprogram);

			MyComboitemConfig comboitem;
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

			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
				searchdosen.setValue(dosen.getNama());
				searchdosen.setAttribute("myValue", dosen);
				searchdosen.setDisabled(true);
			}

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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
				Common.appendKeToolbar(searchTahap, find, comp);
				searchTahap.setReadonly(true);
				searchTahap.setWidth("100px");
				searchTahap.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}

			searchmasaperkulaiahan.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			comboitem = new MyComboitemConfig();
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
			if (searchasisten != null && searchasisten.getAttribute("mahasiswa") != null) {
				searchfakultas.setDisabled(false);
				searchjurusan.setDisabled(false);

				searchfakultas.setSelectedIndex(-1);
				searchjurusan.setSelectedIndex(-1);
			}
			onSearchDefault(null);
			Common.initPaging(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});

			Common.appendKeToolbar(kbb, find, comp);
			Common.appendKeToolbar(rekap, find, comp);
		}

	        FilterLanjutHelper.setup(comp);
}

	class PerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		protected DetailpertemuanHelper detailpertemuanHelper = new DetailpertemuanHelper();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Perkuliahan perkuliahan = (Perkuliahan) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						detailpertemuanHelper.displayDetailPertemuan(perkuliahan, detail);
					}
				}

			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahan);

			Kurikulum kurikulum = perkuliahan.getKurikulum();
			Vbox a = RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahan,
					perkuliahan.getMatakuliah().getKode() + "-" + perkuliahan.getMatakuliah().getNama() + " "
							+ perkuliahan.getMatakuliah().getSks() + " sks "
							+ (kurikulum == null ? "" : " (Kurikulum:" + kurikulum.getTahun() + ")"));
			a.setParent(arg0);

			MatakuliahPrasyaratAction.tampilPrasyarat(a, perkuliahan.getMatakuliah());

			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(arg0, perkuliahan, false);

			new Label(perkuliahan.getSemester()
					+ (perkuliahan.getKelas() == null || perkuliahan.getKelas().equals("") ? ""
							: " " + perkuliahan.getKelas())
					+ " (" + Common.labelJenisSemester(perkuliahan) + ")")
					.setParent(arg0);

			final Html informasi = new ais.ui.util.MyHtml(perkuliahan.populateInfoPersetujuan());
			informasi.setParent(arg0);

		}

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
				.add(searchKeterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchKeterangan.getValue().trim(), MatchMode.ANYWHERE))

				.add(merupakanRemedial ? Restrictions.eq("merupakanRemedial", true)
						: Restrictions.or(Restrictions.isNull("merupakanRemedial"),
								Restrictions.eq("merupakanRemedial", false)))

				.add(searchtanpakelas.isChecked()
						? Restrictions.or(Restrictions.eq("kelas", ""), Restrictions.isNull("kelas"))
						: Restrictions.sqlRestriction("1=1"));

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

		Criterion criterion = searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", searchdosen.getAttribute("myValue")),
						Restrictions.eq("dosen2", searchdosen.getAttribute("myValue")));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", searchdosen.getAttribute("myValue")));

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

				.add((searchkurikulum == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkurikulum.getAttribute("kurikulum") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kurikulum", searchkurikulum.getAttribute("kurikulum"))))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(Restrictions.ilike("kelas", searchkelas.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchparalel.isChecked() ? Restrictions.or(Restrictions.sqlRestriction(
						"this_.id in (select perkuliahan_paralel from perkuliahan where perkuliahan_paralel is not null)"),
						Restrictions.eq("merupakan_paralel", true)) : Restrictions.sqlRestriction("1=1"))
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

				.add(merupakanPraPerkuliahan || searchsemester.getSelectedItem() == null
						|| searchsemester.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsemester.getSelectedItem().getValue()))

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

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(merupakanPraPerkuliahan || searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));

		if (perguruanTinggi != null) {
			criteria.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));
		}

		return criteria;
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
				Common.initPaging(initCriteria(false), paging);
				perkuliahans = ConstantValues.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
								.setFirstResult(Common.ROWS_COUNT_ON_PAGE * activePage),
						Perkuliahan.class);
			}
			if (grid != null) {
				ListModel strset = new SimpleListModel(
						perkuliahans != null ? perkuliahans : new ArrayList<Perkuliahan>());
				grid.setRowRenderer(new PerkuliahanRenderer());
				grid.setModelCheckMobile(strset);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
