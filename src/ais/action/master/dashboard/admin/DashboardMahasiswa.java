package ais.action.master.dashboard.admin;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard mahasiswa. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Intbox mulai}, {@code Intbox sampai},
 * {@code Combobox searchStatusAwalMahasiswa}, {@code Combobox searchstatus}, {@code Combobox searchprogram},
 * {@code Center center}, {@code Combobox searchfakultas}, {@code Combobox searchjurusan}; inisialisasi/lifecycle
 * ({@code init()}); pembacaan/pencarian ({@code reload()}, {@code doreload()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchstatus;
	private Combobox searchprogram;
	private Center center;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchTahunAjaran;
	private Combobox searchJenisSemester;
	private int width = 750;
	private int height = 100;
	private Combobox searchStatusKeluar;

	public DashboardMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabDasbor = new MyTabConfig("Dasbor");
		tabDasbor.setParent(tabs);

		MyTabConfig tab1 = new MyTabConfig("Data");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("KRS");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Nilai");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("IPK");
		tab4.setParent(tabs);

		MyTabConfig tab5 = new MyTabConfig("SKS");
		tab5.setParent(tabs);

		MyTabConfig tab6 = new MyTabConfig("Kegiatan");
		tab6.setParent(tabs);

		MyTabConfig tab7 = new MyTabConfig("Organisasi");
		tab7.setParent(tabs);

		MyTabConfig tab8 = new MyTabConfig("Prestasi");
		tab8.setParent(tabs);

		MyTabConfig tab9 = new MyTabConfig("Kecamatan");
		tab9.setParent(tabs);

		MyTabConfig tab10 = new MyTabConfig("Kota/Kab.");
		tab10.setParent(tabs);

		MyTabConfig tab11 = new MyTabConfig("Propinsi");
		tab11.setParent(tabs);

		MyTabConfig tab12 = new MyTabConfig("Pekerjaan Ortu");
		tab12.setParent(tabs);

		MyTabConfig tab13 = new MyTabConfig("Penghasilan Ortu");
		tab13.setParent(tabs);

		MyTabConfig tab14 = new MyTabConfig("Pendidikan Ortu");
		tab14.setParent(tabs);

		MyTabConfig tab15 = new MyTabConfig("Agama");
		tab15.setParent(tabs);

		MyTabConfig tab16 = new MyTabConfig("Asal Sekolah");
		tab16.setParent(tabs);
		
		MyTabConfig tab17 = new MyTabConfig("Jenis Kelamin");
		tab17.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDasbor = new ais.ui.util.MyTabpanel();
		tabpanelDasbor.setParent(tabpanels);
		// Tab "Dasbor": pakai setHeight (properti tinggi ZK) PERSIS pola tab "Data"
		// (tabpanel1.setHeight("30330px")) — inilah yang benar-benar dihormati mold tabbox ZK
		// sehingga panel jadi 20000px & konten tampil penuh. (setStyle("min-height:..") TIDAK cukup
		// karena ZK menyetel tinggi div-konten internal ke tinggi viewport, mengabaikan min-height.)
		tabpanelDasbor.setHeight("20000px");
		tabDasbor.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelDasbor.getChildren().size() == 0) {
					DashboardRingkasanMahasiswa d = new DashboardRingkasanMahasiswa();
					d.setWidth("100%");
					d.setParent(tabpanelDasbor);
				}
			}
		});
		// Auto-load: Dasbor adalah tab pertama (default terpilih) sehingga
		// tidak ada onClick yang memicunya — instantiasi langsung di sini.
		DashboardRingkasanMahasiswa dRingkasanMhs = new DashboardRingkasanMahasiswa();
		dRingkasanMhs.setWidth("100%");
		dRingkasanMhs.setParent(tabpanelDasbor);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("30330px");

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					DashboardKrsMahasiswa dashboardKrsMahasiswa = new DashboardKrsMahasiswa();
					dashboardKrsMahasiswa.setHeight("100%");
					dashboardKrsMahasiswa.setWidth("100%");
					dashboardKrsMahasiswa.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().size() == 0) {
					DashboardNilaiMahasiswa dashboardNilaiMahasiswa = new DashboardNilaiMahasiswa();
					dashboardNilaiMahasiswa.setHeight("100%");
					dashboardNilaiMahasiswa.setWidth("100%");

					dashboardNilaiMahasiswa.setParent(tabpanel3);
				}
			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().size() == 0) {
					DashboardIpkMahasiswa dashboardIpkMahasiswa = new DashboardIpkMahasiswa();
					dashboardIpkMahasiswa.setHeight("100%");
					dashboardIpkMahasiswa.setWidth("100%");

					dashboardIpkMahasiswa.setParent(tabpanel4);
				}
			}
		});

		final Tabpanel tabpanel5 = new ais.ui.util.MyTabpanel();
		tabpanel5.setParent(tabpanels);
		tab5.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel5.getChildren().size() == 0) {
					DashboardSksKumulatifMahasiswa dashboardSksKumulatifMahasiswa = new DashboardSksKumulatifMahasiswa();
					dashboardSksKumulatifMahasiswa.setHeight("100%");
					dashboardSksKumulatifMahasiswa.setWidth("100%");

					dashboardSksKumulatifMahasiswa.setParent(tabpanel5);
				}
			}
		});

		final Tabpanel tabpanel6 = new ais.ui.util.MyTabpanel();
		tabpanel6.setParent(tabpanels);
		tab6.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel6.getChildren().size() == 0) {
					DashboardKegiatanKemahasiswaanUmum dashboardKegiatanKemahasiswaanUmum = new DashboardKegiatanKemahasiswaanUmum();
					dashboardKegiatanKemahasiswaanUmum.setHeight("100%");
					dashboardKegiatanKemahasiswaanUmum.setWidth("100%");

					dashboardKegiatanKemahasiswaanUmum.setParent(tabpanel6);
				}
			}
		});

		final Tabpanel tabpanel7 = new ais.ui.util.MyTabpanel();
		tabpanel7.setParent(tabpanels);
		tab7.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel7.getChildren().size() == 0) {
					DashboardOrganisasiIntraKampusUmum dashboardOrganisasiIntraKampusUmum = new DashboardOrganisasiIntraKampusUmum();
					dashboardOrganisasiIntraKampusUmum.setHeight("100%");
					dashboardOrganisasiIntraKampusUmum.setWidth("100%");

					dashboardOrganisasiIntraKampusUmum.setParent(tabpanel7);
				}
			}
		});

		final Tabpanel tabpanel8 = new ais.ui.util.MyTabpanel();
		tabpanel8.setParent(tabpanels);
		tab8.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel8.getChildren().size() == 0) {
					DashboardPrestasiMahasiswaUmum dashboardPrestasiMahasiswaUmum = new DashboardPrestasiMahasiswaUmum();
					dashboardPrestasiMahasiswaUmum.setHeight("100%");
					dashboardPrestasiMahasiswaUmum.setWidth("100%");

					dashboardPrestasiMahasiswaUmum.setParent(tabpanel8);
				}
			}
		});

		final Tabpanel tabpanel9 = new ais.ui.util.MyTabpanel();
		tabpanel9.setParent(tabpanels);
		tab9.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel9.getChildren().size() == 0) {
					DashboardMahasiswaKecamatan dashboardMahasiswaKecamatan = new DashboardMahasiswaKecamatan();
					dashboardMahasiswaKecamatan.setHeight("100%");
					dashboardMahasiswaKecamatan.setWidth("100%");
					dashboardMahasiswaKecamatan.setParent(tabpanel9);
				}
			}
		});

		final Tabpanel tabpanel10 = new ais.ui.util.MyTabpanel();
		tabpanel10.setParent(tabpanels);
		tab10.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel10.getChildren().size() == 0) {
					DashboardMahasiswaKota dashboardMahasiswaKota = new DashboardMahasiswaKota();
					dashboardMahasiswaKota.setHeight("100%");
					dashboardMahasiswaKota.setWidth("100%");
					dashboardMahasiswaKota.setParent(tabpanel10);
				}
			}
		});

		final Tabpanel tabpanel11 = new ais.ui.util.MyTabpanel();
		tabpanel11.setParent(tabpanels);
		tab11.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel11.getChildren().size() == 0) {
					DashboardMahasiswaPropinsi dashboardMahasiswaPropinsi = new DashboardMahasiswaPropinsi();
					dashboardMahasiswaPropinsi.setHeight("100%");
					dashboardMahasiswaPropinsi.setWidth("100%");
					dashboardMahasiswaPropinsi.setParent(tabpanel11);
				}
			}
		});

		final Tabpanel tabpanel12 = new ais.ui.util.MyTabpanel();
		tabpanel12.setParent(tabpanels);
		tab12.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel12.getChildren().size() == 0) {
					DashboardMahasiswaPekerjaanOrtu dashboardMahasiswaPekerjaan = new DashboardMahasiswaPekerjaanOrtu();
					dashboardMahasiswaPekerjaan.setHeight("100%");
					dashboardMahasiswaPekerjaan.setWidth("100%");
					dashboardMahasiswaPekerjaan.setParent(tabpanel12);
				}
			}
		});

		final Tabpanel tabpanel13 = new ais.ui.util.MyTabpanel();
		tabpanel13.setParent(tabpanels);
		tab13.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel13.getChildren().size() == 0) {
					DashboardMahasiswaPenghasilanOrtu dashboardMahasiswaPenghasilan = new DashboardMahasiswaPenghasilanOrtu();
					dashboardMahasiswaPenghasilan.setHeight("100%");
					dashboardMahasiswaPenghasilan.setWidth("100%");
					dashboardMahasiswaPenghasilan.setParent(tabpanel13);
				}
			}
		});

		final Tabpanel tabpanel14 = new ais.ui.util.MyTabpanel();
		tabpanel14.setParent(tabpanels);
		tab14.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel14.getChildren().size() == 0) {
					DashboardMahasiswaPendidikanOrtu dashboardMahasiswaPendidikan = new DashboardMahasiswaPendidikanOrtu();
					dashboardMahasiswaPendidikan.setHeight("100%");
					dashboardMahasiswaPendidikan.setWidth("100%");
					dashboardMahasiswaPendidikan.setParent(tabpanel14);
				}
			}
		});

		final Tabpanel tabpanel15 = new ais.ui.util.MyTabpanel();
		tabpanel15.setParent(tabpanels);
		tab15.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel15.getChildren().size() == 0) {
					DashboardMahasiswaAgama dashboardMahasiswaAgama = new DashboardMahasiswaAgama();
					dashboardMahasiswaAgama.setHeight("100%");
					dashboardMahasiswaAgama.setWidth("100%");
					dashboardMahasiswaAgama.setParent(tabpanel15);
				}
			}
		});

		final Tabpanel tabpanel16 = new ais.ui.util.MyTabpanel();
		tabpanel16.setParent(tabpanels);
		tab16.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel16.getChildren().size() == 0) {
					DashboardMahasiswaAsalSekolah dashboardMahasiswaAsalSekolah = new DashboardMahasiswaAsalSekolah();
					dashboardMahasiswaAsalSekolah.setHeight("100%");
					dashboardMahasiswaAsalSekolah.setWidth("100%");
					dashboardMahasiswaAsalSekolah.setParent(tabpanel16);
				}
			}
		});
		
		
		final Tabpanel tabpanel17 = new ais.ui.util.MyTabpanel();
		tabpanel17.setParent(tabpanels);
		tab17.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel17.getChildren().size() == 0) {
					DashboardJenisKelaminMahasiswa dashboardMahasiswaAsalSekolah = new DashboardJenisKelaminMahasiswa();
					dashboardMahasiswaAsalSekolah.setHeight("100%");
					dashboardMahasiswaAsalSekolah.setWidth("100%");
					dashboardMahasiswaAsalSekolah.setParent(tabpanel17);
				}
			}
		});


		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanel1);
		borderlayout.setStyle("min-height:25000px");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				borderlayout.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
			}
		}

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Number m = (Number) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.max("tahunangkatan")).uniqueResult();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Angkatan"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		mulai = new Intbox((m == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : m.intValue()) - 7);
		mulai.setCols(2);
		hbox.appendChild(mulai);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		sampai = new Intbox(mulai.getValue() + 7);
		sampai.setCols(2);
		hbox.appendChild(sampai);

		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

		searchStatusAwalMahasiswa = new Combobox();
		row.appendChild(new MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa);
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchStatusAwalMahasiswa.setWidth("90%");

		searchstatus = new Combobox();
		row.appendChild(new MyLabelConfig("Status/TA/Smt"));

		hbox = new Hbox();
		hbox.setParent(row);

		hbox.appendChild(searchstatus);
		Common.insertComboDanSemua(searchstatus, "nama", StatusMahasiswa.class);
		searchstatus.setCols(1);

		hbox.appendChild(searchTahunAjaran = new Combobox());
		Common.generateTahunAjaran(searchTahunAjaran);
		searchTahunAjaran.setCols(1);

		hbox.appendChild(searchJenisSemester = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		searchJenisSemester.setCols(1);
		searchJenisSemester.setReadonly(true);

		searchTahunAjaran.addEventListener("onChange", eventListener);
		searchJenisSemester.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		searchStatusKeluar = new Combobox();
		row.appendChild(new MyLabelConfig("Status Keluar"));
		row.appendChild(searchStatusKeluar);
		Common.insertComboDanSemua(searchStatusKeluar, "nama", StatusKeluar.class);
		searchStatusKeluar.setWidth("90%");
		row.setParent(rows);
		searchStatusKeluar.addEventListener("onChange", eventListener);

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		searchStatusAwalMahasiswa.addEventListener("onChange", eventListener);
		searchstatus.addEventListener("onChange", eventListener);
		searchprogram.addEventListener("onChange", eventListener);

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		row.appendChild(new MyLabelConfig("Fakultas / Prodi"));
		hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(searchfakultas);
		hbox.appendChild(searchjurusan);
		searchfakultas.addEventListener("onChange", eventListener);
		searchjurusan.addEventListener("onChange", eventListener);
		searchfakultas.setCols(2);
		searchjurusan.setCols(2);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		row = new MyFormRow();
		row.setParent(rows);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardMahasiswa.this.grid);
			}
		});

		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				reload();
			}
		});
	}

	private Criterion criteriaStatus;
	private StatusMahasiswa selectedStatusMahasiswa;
	protected Grid grid;

	private void reload() {
		criteriaStatus = Restrictions.sqlRestriction("true");
		selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		if (selectedStatusMahasiswa != null) {

			final Label label = Common.displayLoadBar(new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					doreload();
				}
			});

			new Thread(new Runnable() {

				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					try {
						String ta = (String) searchTahunAjaran.getSelectedItem().getValue();
						String jenisSemester = (String) searchJenisSemester.getSelectedItem().getValue();
						List<Long> dataMhs = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.property("id"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

						int size = dataMhs.size();
						int rowIndex = 1;

						List<Long> mhss = new ArrayList<Long>();

						for (Long generalValueObjectid : dataMhs) {
							Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
									generalValueObjectid);
							rowIndex++;
							label.setValue("Sedang memproses status " + mahasiswa.toString() + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

							Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta, jenisSemester,
									mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

							HistoryStatusMahasiswa historyStatusMahasiswa = Common
									.getHistoryStatusMahasiswa(krsMahasiswa);
							if (selectedStatusMahasiswa == null || (historyStatusMahasiswa != null
									&& historyStatusMahasiswa.getStatusMahasiswa() != null && historyStatusMahasiswa
											.getStatusMahasiswa().getId().equals(selectedStatusMahasiswa.getId()))) {
								mhss.add(mahasiswa.getId());
							}
						}

						if (mhss.isEmpty()) {
							criteriaStatus = Restrictions.sqlRestriction("false");
						} else {
							criteriaStatus = Restrictions.in("id", mhss);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardMahasiswa.java:668");
					}
					label.setValue("");
				}
			}).start();

		} else {
			doreload();
		}
	}

	@SuppressWarnings("unchecked")
	private void doreload() {
		Common.clear(center);

		final StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());
		final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
				.getSelectedItem() == null || searchStatusAwalMahasiswa.getSelectedItem().getValue() == null ? null
						: searchStatusAwalMahasiswa.getSelectedItem().getValue());

		final StatusKeluar statusKeluar = (StatusKeluar) (searchStatusKeluar.getSelectedItem() == null
				|| searchStatusKeluar.getSelectedItem().getValue() == null ? null
						: searchStatusKeluar.getSelectedItem().getValue());

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Map<Long, List<Object[]>> datas = new HashMap<Long, List<Object[]>>();

		Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		final List<Jurusan> jurusans = HibernateUtil.currentSession().createCriteria(Jurusan.class)
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fak))
				.addOrder(Order.asc("fakultas"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		final int mul = mulai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 7
				: mulai.getValue();
		final int sam = sampai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
				: sampai.getValue();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				grid = new Grid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);

				column.setWidth("15%");

				for (int tahun = mul; tahun <= sam; tahun++) {
					column.setParent(columns);
					column = new MyColumnConfig(tahun + "");
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModel = new SimpleCategoryModel();
				categoryModel.clear();

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					List<Object[]> data = datas.get(jurusan.getId());

					for (int tahun = mul; tahun <= sam; tahun++) {
						final int thn = tahun;

						Number jumlah = 0;
						for (Object[] o : data) {
							Object tahunangkatan = o[1];
							if (tahunangkatan != null && Integer.parseInt(tahunangkatan.toString()) == tahun) {
								jumlah = (Number) o[0];
								break;
							}
						}

						A a = new A(Common.numberFormat.get().format(jumlah));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Common.displayWindow(
										"/common/dashboard/mahasiswa.zul?jurusan=" + jurusan.getId()
												+ (statusAwalMahasiswa == null ? ""
														: "&statusAwalMahasiswa=" + statusAwalMahasiswa.getId())
												+ (selectedStatusMahasiswa == null ? ""
														: "&selectedStatusMahasiswa=" + selectedStatusMahasiswa.getId())

												+ (statusKeluar == null ? "" : "&statusKeluar=" + statusKeluar.getId())

												+ "&tahunangkatan=" + thn
												+ (program == null ? ""
														: "&program=" + URLEncoder.encode(program, "UTF-8")),
										true, "95%", "95%");

							}
						});

						categoryModel.setValue(jurusan.getNama(), tahun, jumlah.intValue());

					}
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.setSpans(((sam - mul) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Mahasiswa", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				int i = 1;
				for (final Jurusan jurusan : jurusans) {
					label.setValue("Sedang memproses data di prodi " + jurusan.getNama() + " ("
							+ Common.numberFormat.get().format((i * 100.0) / jurusans.size()) + ")");
					i++;
					List<Object[]> data = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(criteriaStatus)

							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

							.add(statusKeluar == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("statusKeluar", statusKeluar))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))

							.setProjection(Projections.projectionList().add(Projections.rowCount())
									.add(Projections.groupProperty("tahunangkatan")))

							.add(Restrictions.eq("jurusan", jurusan))
							.add(Restrictions.between("tahunangkatan", mul, sam)).list();

					datas.put(jurusan.getId(), data);
				}
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
