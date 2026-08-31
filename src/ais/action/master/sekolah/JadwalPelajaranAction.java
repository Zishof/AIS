package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.DetailpertemuanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AktifitasPembelajaranHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataKelasLesSiswaBanbox;
import ais.action.master.sekolah.helper.DetailJadwalMatapelajaranHelper;
import ais.action.master.sekolah.helper.DetailKelasLesSiswaHelper;
import ais.action.report.format1.sekolah.LaporanJadwalPelajaran;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.library.Item;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPelajaranPunyaItem;
import ais.database.model.sekolah.JamPelajaran;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.MasaJadwalPelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.MatapelajaranPunyaBukuBahanAjar;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.SubMatapelajaran;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk jadwal pelajaran. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchhari}, {@code Textbox
 * searchnamaruang}, {@code Textbox searchketerangan}, {@code Textbox searchsiswa}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code
 * initBerdasarKurikulum()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * validasi/perhitungan ({@code checkBentrokBerdasarRuangan()}, {@code checkBentrokBerdasarKelas()}, {@code
 * checkBentrokBerdasarGuru()}); mutasi data ({@code onSave()}); penghapusan/pembatalan ({@code
 * onDeleteJadwalPelajaran()}); operasi domain lain ({@code onMasa()}, {@code onLaporanJadwalPelajaran()}, {@code
 * lihatJadwalBentrok()}, {@code bentrok()}, {@code onAddExternal()}, {@code onAdd()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class JadwalPelajaranAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	protected Combobox searchhari;
	protected Textbox searchnamaruang;
	protected Textbox searchketerangan;
	protected Textbox searchsiswa;

	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchta;
	private Combobox searchsmt;

	private Combobox searchkelas;
	private AmbilDataGuruBanbox searchguru;

	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JadwalPelajaran jadwalPelajaran;
	private Combobox masaJadwalPelajaran;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Combobox matapelajaran;
	private Combobox jamPelajaran;
	private Combobox kelas;
	private AmbilDataGuruBanbox guru;
	private Combobox tahunAjaran;
	private Combobox semester;
	private Combobox hari;

	private AktifitasPembelajaranHelper aktifitasPembelajaranHelper;
	private Combobox hari2;
	private Combobox jamPelajaran2;
	private Combobox hari3;
	private Combobox jamPelajaran3;
	private Combobox hari4;
	private Combobox jamPelajaran4;
	private Combobox hari5;
	private Combobox jamPelajaran5;

	private Tbmuser tbmuser;
	private Row rowMatapelajaran;

	private Grid gridBerdasarkanKurikulum = null;

	private Tabpanel masa;

	public void onMasa(Event event) {
		if (masa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(masa);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/masa_jadwal_pelajaran.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel laporanJadwalPelajaran;
	private AmbilDataGuruBanbox guru2;
	private AmbilDataGuruBanbox guru3;
	private AmbilDataGuruBanbox guru4;
	private AmbilDataGuruBanbox guru5;
	private Combobox hari6;
	private Combobox jamPelajaran6;
	private AmbilDataGuruBanbox guru6;
	private Combobox hari7;
	private Combobox jamPelajaran7;
	private AmbilDataGuruBanbox guru7;
	private Combobox hari8;
	private Combobox jamPelajaran8;
	private AmbilDataGuruBanbox guru8;
	private Combobox hari9;
	private Combobox jamPelajaran9;
	private AmbilDataGuruBanbox guru9;
	private Combobox hari10;
	private Combobox jamPelajaran10;
	private AmbilDataGuruBanbox guru10;
	private MyCheckboxConfig abaikanBentrok;
	private Combobox hari11;
	private Combobox hari12;
	private Combobox jamPelajaran11;
	private Combobox jamPelajaran12;
	private AmbilDataGuruBanbox guru12;
	private AmbilDataGuruBanbox guru11;
	private EventListener eventListener = null;

	private Combobox subMatapelajaran;
	private Combobox subMatapelajaran2;
	private Combobox subMatapelajaran3;
	private Combobox subMatapelajaran4;
	private Combobox subMatapelajaran5;
	private Combobox subMatapelajaran6;
	private Combobox subMatapelajaran7;
	private Combobox subMatapelajaran8;
	private Combobox subMatapelajaran9;
	private Combobox subMatapelajaran10;
	private Combobox subMatapelajaran11;
	private Combobox subMatapelajaran12;
	private AmbilDataKelasLesSiswaBanbox kelasLesSiswa;

	public void onLaporanJadwalPelajaran(Event event) {
		if (laporanJadwalPelajaran.getChildren().size() == 0) {
			LaporanJadwalPelajaran window = new LaporanJadwalPelajaran();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(laporanJadwalPelajaran);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		Common.generateTahunAjaran(searchta);

		Comboitem comboitem = new Comboitem(JadwalPelajaran.GANJIL);
		if (comboitem != null) { comboitem.setValue(1); }
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(JadwalPelajaran.GENAP);
		if (comboitem != null) { comboitem.setValue(2); }
		searchsmt.appendChild(comboitem);
		if (searchsmt != null) { searchsmt.setCols(2); }

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		if (searchsmt != null) { searchsmt.setReadonly(true); }

		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			searchhari.appendChild(comboitem);
		}
		comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchhari.appendChild(comboitem);
		if (searchhari != null) { searchhari.setReadonly(true); }
		if (searchhari != null) { searchhari.setSelectedItem(comboitem); }

		tbmuser = Common.getCurrentUser();

		aktifitasPembelajaranHelper = new AktifitasPembelajaranHelper(tbmuser == null ? null : tbmuser.getSiswa(),
				tbmuser == null ? null : tbmuser.getCalonSiswa());

		EventListener kelasEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				String ta = (String) searchta.getSelectedItem().getValue();
				Sekolah s = tbmuser == null ? null : tbmuser.ambilSekolah();
				System.out.println("s => " + s);
				Common.insertComboDanSemua(searchkelas, new String[] { "nama", "tahunAjaran", "ruang" }, "keterangan",
						KelasSiswa.class,
						Restrictions.and(Restrictions.eq("tahunAjaran", ta), Restrictions.and(
								Restrictions.or(Restrictions.isNull("sekolah"),
										s == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

				Common.selectComboItem(searchkelas, null);
			}
		};

		kelasEvent.onEvent(null);
		searchta.addEventListener("onChange", kelasEvent);

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "tahunAjaran", "semester", "matapelajaran", "abaikanBentrok", "hari",

				"hari2", "hari3", "hari4", "hari5", "hari6", "hari7", "hari8", "hari9", "hari10", "hari11", "hari12",

				"jamPelajaran",

				"jamPelajaran2", "jamPelajaran3", "jamPelajaran4", "jamPelajaran5", "jamPelajaran6", "jamPelajaran7",
				"jamPelajaran8", "jamPelajaran9", "jamPelajaran10", "jamPelajaran11", "jamPelajaran12",

				"kelas", "guru",

				"guru2", "guru3", "guru4", "guru5", "guru6", "guru7", "guru8", "guru9", "guru10", "guru11", "guru12",

				"sekolah", "keterangan"

				, "subMatapelajaran",

				"subMatapelajaran2", "subMatapelajaran3", "subMatapelajaran4", "subMatapelajaran5", "subMatapelajaran6",
				"subMatapelajaran7", "subMatapelajaran8", "subMatapelajaran9", "subMatapelajaran10",
				"subMatapelajaran11", "subMatapelajaran12"

		};
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JadwalPelajaran.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig tambahBerdasarKurikulum = new MyToolbarbuttonConfig("Tambah Berdasarkan Kurikulum",
				"/img/svg/book-open-line.svg");
		if (tambahBerdasarKurikulum != null) {
			tambahBerdasarKurikulum.setVisible(add != null && add.isVisible());
			tambahBerdasarKurikulum.setTooltiptext("Buat jadwal pelajaran berdasarkan kurikulum kelas yang dipilih");
		}
		Common.appendKeToolbar(tambahBerdasarKurikulum, add, comp);
		tambahBerdasarKurikulum.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onAddBerdasarKurikulum(arg0);
			}
		});

		MyToolbarbuttonConfig cetakSksGuru = new MyToolbarbuttonConfig("Generate jadwal dari kurikulum",
				"/img/jadwal.png");
		if (cetakSksGuru != null) { cetakSksGuru.setVisible(false); }
		Common.appendKeToolbar(cetakSksGuru, add, comp);
		cetakSksGuru.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				final Sekolah sekolah = (Sekolah) (JadwalPelajaranAction.this.searchsekolah.getSelectedItem() == null
						? null
						: JadwalPelajaranAction.this.searchsekolah.getSelectedItem().getValue());
				if (sekolah == null) {
					MyMessageboxConfig.show("Sekolah belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Sekolah; (2) pilih sekolah yang sesuai; (3) ulangi proses setelah sekolah dipilih.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
						Session session = HibernateUtil.currentNativeSession();
						List<KelasSiswa> kelasSiswas = session.createCriteria(KelasSiswa.class)
								.add(Restrictions.eq("sekolah", sekolah))
								.add(Restrictions.isNotNull("kurikulumSekolah"))
								.add(Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue())).list();
						int index = 0;
						int size = kelasSiswas.size();
						for (KelasSiswa kelasSiswa : kelasSiswas) {
							index++;
							label.setValue("Memproses data kelas " + kelasSiswa.getNama() + " ("
									+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

							List<Long> longs = kelasSiswa.ambilMk();

							List<KurikulumPunyaMatapelajaran> kurikulumPunyaMatapelajarans = ConstantValues
									.simpleList(
											session.createCriteria(KurikulumPunyaMatapelajaran.class)
													.createAlias("matapelajaran", "matapelajaran")
													.add(longs == null || longs.isEmpty()
															? Restrictions.sqlRestriction("true")
															: Restrictions
																	.not(Restrictions.in("matapelajaran.id", longs)))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("kurikulumSekolah",
															kelasSiswa.getKurikulumSekolah())),
											KurikulumPunyaMatapelajaran.class);

							for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : kurikulumPunyaMatapelajarans) {
								JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) session
										.createCriteria(JadwalPelajaran.class).add(Restrictions.eq("sekolah", sekolah))
										.add(Restrictions.eq("tahunAjaran", kelasSiswa.getTahunAjaran()))
										.add(Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))
										.add(Restrictions.eq("kurikulumPunyaMatapelajaran",
												kurikulumPunyaMatapelajaran))
										.add(Restrictions.eq("kelas", kelasSiswa)).setMaxResults(1).uniqueResult();
								if (jadwalPelajaran == null) {
									jadwalPelajaran = new JadwalPelajaran();
								}
								jadwalPelajaran.setKelas(kelasSiswa);
								jadwalPelajaran.setKurikulumPunyaMatapelajaran(kurikulumPunyaMatapelajaran);
								jadwalPelajaran.setTahunAjaran(kelasSiswa.getTahunAjaran());
								jadwalPelajaran.setSemester((Integer) searchsmt.getSelectedItem().getValue());
								jadwalPelajaran.setRuang(kelasSiswa.getRuang());
								jadwalPelajaran.setMatapelajaran(kurikulumPunyaMatapelajaran.getMatapelajaran());
								jadwalPelajaran.setSekolah(sekolah);
								jadwalPelajaran.setYayasan(sekolah.getYayasan());

								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, jadwalPelajaran);
								session.getTransaction().commit();
							}
							kurikulumPunyaMatapelajarans = null;
						}
						kelasSiswas = null;
						HibernateUtil.closeSession();
						label.setValue("");
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}
		});

		MyToolbarbuttonConfig checkBentrok = new MyToolbarbuttonConfig("Bentrok", "/img/Record-Normal-icon.png");
		Common.appendKeToolbar(checkBentrok, add, comp);
		checkBentrok.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				lihatJadwalBentrok();
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	public static void lihatJadwalBentrok() throws Exception {
		final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
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

		Common.selectComboItem(genapGanjil, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses Lihat Jam Bentrok", "/img/save.gif");
		save.setTooltiptext("Proses");
		save.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();

				final String tahunAjaran = tahunAkademik.getSelectedItem() == null
						|| tahunAkademik.getSelectedItem().getValue() == null ? null
								: tahunAkademik.getSelectedItem().getValue().toString();
				final String smt = genapGanjil.getSelectedItem() == null
						|| genapGanjil.getSelectedItem().getValue() == null ? null
								: genapGanjil.getSelectedItem().getValue().toString();

				final Label label = new Label(ais.common.Common.getBahasaConfig("Sedang mencari jadwal bentrok"));

				final Map<String, Map> bentroks = new HashMap<String, Map>();

				new Thread(new Runnable() {

					@SuppressWarnings({ "unchecked" })
					@Override
					public void run() {
						try {

						try {
							Session session = HibernateUtil.currentNativeSession();
							List<JadwalPelajaran> jadwalPelajarans = ConstantValues.simpleList(
									session.createCriteria(JadwalPelajaran.class).add(Restrictions.isNotNull("sekolah"))
											.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("tahunAjaran", tahunAjaran))
											.add(smt == null ? Restrictions.sqlRestriction("1=1")
													: Restrictions.sqlRestriction("this_.semester % 2 = "
															+ (smt.equals(JadwalPelajaran.GANJIL) ? "1" : "0")))

									, JadwalPelajaran.class);
							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}
							HibernateUtil.closeSession();

							label.setValue("Sedang mencari jadwal bentrok berdasarkan ruangan");
							Map<Long, List<JadwalPelajaran[]>> bentrokRuangans = checkBentrokBerdasarRuangan(
									jadwalPelajarans, jadwalPelajarans);
							label.setValue("Sedang mencari jadwal bentrok berdasarkan guru");
							Map<Guru, List<JadwalPelajaran[]>> bentrokGurus = checkBentrokBerdasarGuru(jadwalPelajarans,
									jadwalPelajarans);
							label.setValue("Sedang mencari jadwal bentrok berdasarkan kelas");
							Map<String, List<JadwalPelajaran[]>> bentrokKelas = checkBentrokBerdasarKelas(
									jadwalPelajarans, jadwalPelajarans);

							if (!bentrokRuangans.isEmpty()) {
								bentroks.put("Bentrok Ruangan", bentrokRuangans);
							}
							if (!bentrokGurus.isEmpty()) {
								bentroks.put("Bentrok Guru", bentrokGurus);
							}
							if (!bentrokKelas.isEmpty()) {
								bentroks.put("Bentrok Kelas", bentrokKelas);
							}

							jadwalPelajarans = null;

						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
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

						// System.out.println("process = " + label.getValue());
						Clients.showBusy(label.getValue());
						if (label.getValue().isEmpty()) {
							Clients.clearBusy();

							if (bentroks.isEmpty()) {
								MyMessageboxConfig.show("Tidak ditemukan jadwal yang bentrok.", "Peringatan", MyMessageboxConfig.OK,
										MyMessageboxConfig.EXCLAMATION);
							} else {

								try {
									XSSFWorkbook workbook = new XSSFWorkbook();

									XSSFSheet sheet = workbook.createSheet("Jadwal bentrok");
									sheet.setDefaultColumnWidth(18);

									XSSFRow rowhead = sheet.createRow((short) 0);

									rowhead.createCell(0).setCellValue("Jenis Bentrok");
									rowhead.createCell(1).setCellValue("Bentrok di");
									int index = 2;
									for (String romawi : Common.ROMAWI_TANPA_NOL) {
										rowhead.createCell(index++).setCellValue("Jadwal " + romawi);
										rowhead.createCell(index++).setCellValue("Bentrok dengan Jadwal " + romawi);
									}

									@SuppressWarnings("unchecked")
									Map<Long, List<JadwalPelajaran[]>> bentrokRuangans = bentroks
											.get("Bentrok Ruangan");
									int rowIndex = 1;
									for (List<JadwalPelajaran[]> jadwals : bentrokRuangans.values()) {
										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellValue("Bentrok Ruangan");

										cell = row.createCell(1);
										cell.setCellValue(jadwals.get(0)[0].getRuang().getNama());

										index = 2;
										for (JadwalPelajaran[] jadwal : jadwals) {
											row.createCell(index++).setCellValue(jadwal[0].infoSimple());

											row.createCell(index++).setCellValue(jadwal[1].infoSimple());
										}

										rowIndex++;
									}

									@SuppressWarnings("unchecked")
									Map<Guru, List<JadwalPelajaran[]>> bentrokGurus = bentroks.get("Bentrok Guru");

									for (Guru guru : bentrokGurus.keySet()) {
										List<JadwalPelajaran[]> jadwals = bentrokGurus.get(guru);
										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellValue("Bentrok Guru");

										cell = row.createCell(1);
										cell.setCellValue(guru.getNama());

										index = 2;
										for (JadwalPelajaran[] jadwal : jadwals) {
											row.createCell(index++).setCellValue(jadwal[0].infoSimple());
											row.createCell(index++).setCellValue(jadwal[1].infoSimple());
										}

										rowIndex++;
									}

									@SuppressWarnings("unchecked")
									Map<Long, List<JadwalPelajaran[]>> bentrokKelas = bentroks.get("Bentrok Kelas");

									for (List<JadwalPelajaran[]> jadwals : bentrokKelas.values()) {
										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellValue("Bentrok Kelas");

										cell = row.createCell(1);
										cell.setCellValue(
												jadwals.get(0)[0].getSemester() + " " + jadwals.get(0)[0].getKelas());

										index = 2;
										for (JadwalPelajaran[] jadwal : jadwals) {
											row.createCell(index++).setCellValue(jadwal[0].infoSimple());

											row.createCell(index++).setCellValue(jadwal[1].infoSimple());
										}

										rowIndex++;
									}

									try {
										String filename = Sessions.getCurrent().getWebApp()
												.getRealPath("/tmp/data_jam_bentrok_"
														+ URLEncoder.encode(Common.datetimeFormat2s.get()
																.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
														+ ".xlsx");
										FileOutputStream fileOut = new FileOutputStream(filename);
										workbook.write(fileOut);
										fileOut.close();

										Filedownload.save(new FileInputStream(filename),
												"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
												"data_jam_bentrok.xlsx");
									} catch (IOException e) {
										// TODO Auto-generated catch block
										Common.tampilErrorJikaAdmin(e);
									}

								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/JadwalPelajaranAction.java:741");
									// TODO: handle exception
								}
								bentroks.clear();

							}

							timer.detach();
						}

					}
				});
				timer.start();
			}
		});
		save.setParent(toolbar);

		window.onModal();
	}

	@SuppressWarnings("unchecked")
	public static boolean bentrok(String tahunAjaran, String smt, JadwalPelajaran jadwalPelajaranBaru)
			throws Exception {

		if (Common.bolehKonfigurasi("chek_bentrok_jadwal_pelajaran")) {

			Session session = HibernateUtil.currentNativeSession();
			List<JadwalPelajaran> jadwalPelajarans = ConstantValues.simpleList(
					session.createCriteria(JadwalPelajaran.class).add(Restrictions.isNotNull("sekolah"))
							.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("tahunAjaran", tahunAjaran))
							.add(smt == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.sqlRestriction("this_.semester % 2 = "
											+ (smt.equals(JadwalPelajaran.GANJIL) ? "1" : "0"))),
					JadwalPelajaran.class);
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();

			List<JadwalPelajaran> jadwalPelajaranslain = new ArrayList<JadwalPelajaran>();
			jadwalPelajaranslain.add(jadwalPelajaranBaru);

			Map<Long, List<JadwalPelajaran[]>> bentrokRuangans = checkBentrokBerdasarRuangan(jadwalPelajarans,
					jadwalPelajaranslain);

			if (!bentrokRuangans.isEmpty()) {

				Map<Long, JadwalPelajaran> jadwalPelajaranss = new HashMap<Long, JadwalPelajaran>();
				for (List<JadwalPelajaran[]> jadwalPelajarans2 : bentrokRuangans.values()) {
					for (JadwalPelajaran[] jadwalPelajarans3 : jadwalPelajarans2) {
						for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans3) {
							jadwalPelajaranss.put(jadwalPelajaran.getId(), jadwalPelajaran);
						}
					}
				}

				String r = "";
				for (JadwalPelajaran jadwalPelajaran : jadwalPelajaranss.values()) {
					r += r.isEmpty() ? jadwalPelajaran.info() : "\n\n" + jadwalPelajaran.info();
				}

				MyMessageboxConfig.showFormat(
						"Terdapat jadwal ruangan yang bentrok sebagai berikut:\n\n{V1}\n\nLangkah yang dapat dilakukan: (1) tinjau jadwal yang tercantum di atas; (2) ubah ruangan atau waktu pada salah satu jadwal yang bentrok; (3) simpan kembali setelah tidak ada jadwal yang bertabrakan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, r);
				return false;
			}

			Map<Guru, List<JadwalPelajaran[]>> bentrokGurus = checkBentrokBerdasarGuru(jadwalPelajarans,
					jadwalPelajaranslain);

			if (!bentrokGurus.isEmpty()) {

				Map<Long, JadwalPelajaran> jadwalPelajaranss = new HashMap<Long, JadwalPelajaran>();
				for (List<JadwalPelajaran[]> jadwalPelajarans2 : bentrokGurus.values()) {
					for (JadwalPelajaran[] jadwalPelajarans3 : jadwalPelajarans2) {
						for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans3) {
							jadwalPelajaranss.put(jadwalPelajaran.getId(), jadwalPelajaran);
						}
					}
				}

				String r = "";
				for (JadwalPelajaran jadwalPelajaran : jadwalPelajaranss.values()) {
					r += r.isEmpty() ? jadwalPelajaran.info() : "\n\n" + jadwalPelajaran.info();
				}

				MyMessageboxConfig.showFormat(
						"Terdapat jadwal guru yang bentrok sebagai berikut:\n\n{V1}\n\nLangkah yang dapat dilakukan: (1) tinjau jadwal yang tercantum di atas; (2) ubah guru atau waktu pada salah satu jadwal yang bentrok; (3) simpan kembali setelah tidak ada jadwal yang bertabrakan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, r);
				return false;
			}

			Map<String, List<JadwalPelajaran[]>> bentrokKelas = checkBentrokBerdasarKelas(jadwalPelajarans,
					jadwalPelajaranslain);

			if (!bentrokKelas.isEmpty()) {

				Map<Long, JadwalPelajaran> jadwalPelajaranss = new HashMap<Long, JadwalPelajaran>();
				for (List<JadwalPelajaran[]> jadwalPelajarans2 : bentrokKelas.values()) {
					for (JadwalPelajaran[] jadwalPelajarans3 : jadwalPelajarans2) {
						for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans3) {
							jadwalPelajaranss.put(jadwalPelajaran.getId(), jadwalPelajaran);
						}
					}
				}

				String r = "";
				for (JadwalPelajaran jadwalPelajaran : jadwalPelajaranss.values()) {
					r += r.isEmpty() ? jadwalPelajaran.info() : "\n\n" + jadwalPelajaran.info();
				}

				MyMessageboxConfig.showFormat(
						"Terdapat jadwal kelas yang bentrok sebagai berikut:\n\n{V1}\n\nLangkah yang dapat dilakukan: (1) tinjau jadwal yang tercantum di atas; (2) ubah kelas atau waktu pada salah satu jadwal yang bentrok; (3) simpan kembali setelah tidak ada jadwal yang bertabrakan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, r);
				return false;
			}

			jadwalPelajarans = null;
		}

		return true;
	}

	public static Map<Long, List<JadwalPelajaran[]>> checkBentrokBerdasarRuangan(List<JadwalPelajaran> jadwalPelajarans,
			List<JadwalPelajaran> jadwalPelajaransLain) {

		Map<Long, List<JadwalPelajaran[]>> bentrokRuangans = new HashMap<Long, List<JadwalPelajaran[]>>();
		Set<String> sudahAda = new HashSet<String>();
		for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
			if (jadwalPelajaran == null || jadwalPelajaran.getRuang() == null) {
				continue;
			}

			if (jadwalPelajaran.getAbaikanBentrok()) {
				continue;
			}

			for (Object[] objects : jadwalPelajaran.populateJamPelajaran()) {
				JamPelajaran jamPelajaran = (JamPelajaran) objects[0];
				String hari = (String) objects[1];

				for (JadwalPelajaran jadwalPelajaranLain : jadwalPelajaransLain) {

					if (jadwalPelajaran == null || jadwalPelajaran.getId().equals(jadwalPelajaranLain.getId())
							|| jadwalPelajaranLain.getRuang() == null) {
						continue;
					}

					if (jadwalPelajaranLain.getAbaikanBentrok()) {
						continue;
					}

					for (Object[] objectsLain : jadwalPelajaranLain.populateJamPelajaran()) {

						JamPelajaran jamPelajaranLain = (JamPelajaran) objectsLain[0];
						String hariLain = (String) objectsLain[1];

						if (hari.equals(hariLain)) {

							String kode = jadwalPelajaran.getId() + "_" + jadwalPelajaranLain.getId();
							String kode1 = jadwalPelajaranLain.getId() + "_" + jadwalPelajaran.getId();

							Long start1 = jamPelajaran.getMulai() == null ? null
									: jamPelajaran.getMulai().getTime() + 60L;
							Long end1 = jamPelajaran.getSelesai() == null ? null
									: jamPelajaran.getSelesai().getTime() - 60L;

							Long start2 = jamPelajaranLain.getMulai() == null ? null
									: jamPelajaranLain.getMulai().getTime() + 60;
							Long end2 = jamPelajaranLain.getSelesai() == null ? null
									: jamPelajaranLain.getSelesai().getTime() - 60L;

							if (start1 == null || end1 == null || start2 == null || end2 == null) {
								continue;
							}

							boolean ada = true;
							if (start1 != null && end1 != null && start2 != null && end2 != null) {
								ada = (start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2);
							}

							if (jadwalPelajaran.getRuang().getId().equals(jadwalPelajaranLain.getRuang().getId()) && ada
									&& (!sudahAda.contains(kode) && !sudahAda.contains(kode1))
									&& ((jadwalPelajaran.getHari() == null ? "" : jadwalPelajaran.getHari())
											.equals(jadwalPelajaranLain.getHari()))
									&& (

									(jamPelajaran.getWaktuMulaiD() >= jamPelajaranLain.getWaktuMulaiD() + 0.01
											&& jamPelajaran.getWaktuMulaiD() <= jamPelajaranLain.getWaktuSelesaiD()
													- 0.01)

											||

											(jamPelajaran.getWaktuSelesaiD() >= jamPelajaranLain.getWaktuMulaiD() + 0.01
													&& jamPelajaran.getWaktuSelesaiD() <= jamPelajaranLain
															.getWaktuSelesaiD() - 0.01)

											||

											(jamPelajaranLain.getWaktuMulaiD() >= jamPelajaran.getWaktuMulaiD() + 0.01
													&& jamPelajaranLain
															.getWaktuMulaiD() <= jamPelajaran.getWaktuSelesaiD() - 0.01)

											||

											(jamPelajaranLain.getWaktuSelesaiD() >= jamPelajaran.getWaktuMulaiD() + 0.01
													&& jamPelajaranLain.getWaktuSelesaiD() <= jamPelajaran
															.getWaktuSelesaiD() - 0.01)

											||

											(Common.numberFormat.get().format(jamPelajaranLain.getWaktuMulaiD())
													.equals(Common.numberFormat.get().format(jamPelajaran.getWaktuMulaiD()))
													&& Common.numberFormat.get().format(jamPelajaranLain.getWaktuSelesaiD())
															.equals(Common.numberFormat.get()
																	.format(jamPelajaran.getWaktuSelesaiD())))

									)) {
								sudahAda.add(kode);
								sudahAda.add(kode1);

								Ruang ruang = jadwalPelajaran.getRuang();
								if (bentrokRuangans.containsKey(ruang.getId())) {
									bentrokRuangans.get(ruang.getId())
											.add(new JadwalPelajaran[] { jadwalPelajaran, jadwalPelajaranLain });
								} else {
									List<JadwalPelajaran[]> pps = new ArrayList<JadwalPelajaran[]>();
									pps.add(new JadwalPelajaran[] { jadwalPelajaran, jadwalPelajaranLain });
									bentrokRuangans.put(ruang.getId(), pps);
								}
							}
						}
					}
				}
			}
		}
		return bentrokRuangans;

	}

	public static Map<String, List<JadwalPelajaran[]>> checkBentrokBerdasarKelas(List<JadwalPelajaran> jadwalPelajarans,
			List<JadwalPelajaran> jadwalPelajaransLain) {

		Map<String, List<JadwalPelajaran[]>> bentrokRuangans = new HashMap<String, List<JadwalPelajaran[]>>();
		Set<String> sudahAda = new HashSet<String>();
		for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
			if (jadwalPelajaran == null || jadwalPelajaran.getKelas() == null) {
				continue;
			}

			if (jadwalPelajaran.getAbaikanBentrok()) {
				continue;
			}

			for (Object[] objects : jadwalPelajaran.populateJamPelajaran()) {
				JamPelajaran jamPelajaran = (JamPelajaran) objects[0];
				String hari = (String) objects[1];
				for (JadwalPelajaran jadwalPelajaranLain : jadwalPelajaransLain) {

					if (jadwalPelajaranLain == null || jadwalPelajaran.getId().equals(jadwalPelajaranLain.getId())
							|| jadwalPelajaranLain.getKelas() == null

					) {
						continue;
					}

					if (jadwalPelajaranLain.getAbaikanBentrok()) {
						continue;
					}

					for (Object[] objectsLain : jadwalPelajaranLain.populateJamPelajaran()) {

						JamPelajaran jamPelajaranLain = (JamPelajaran) objectsLain[0];
						String hariLain = (String) objectsLain[1];

						if (hari.equals(hariLain)) {

							String kode = jadwalPelajaran.getId() + "_" + jadwalPelajaranLain.getId();
							String kode1 = jadwalPelajaranLain.getId() + "_" + jadwalPelajaran.getId();

							Long start1 = jamPelajaran.getMulai() == null ? null
									: jamPelajaran.getMulai().getTime() + 60L;
							Long end1 = jamPelajaran.getSelesai() == null ? null
									: jamPelajaran.getSelesai().getTime() - 60L;

							Long start2 = jamPelajaranLain.getMulai() == null ? null
									: jamPelajaranLain.getMulai().getTime() + 60;
							Long end2 = jamPelajaranLain.getSelesai() == null ? null
									: jamPelajaranLain.getSelesai().getTime() - 60L;

							if (start1 == null || end1 == null || start2 == null || end2 == null) {
								continue;
							}

							boolean ada = true;
							if (start1 != null && end1 != null && start2 != null && end2 != null) {
								ada = (start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2);
							}

							String kelasLengkap = jadwalPelajaran.getKelas() + "-" + jadwalPelajaran.getSemester() + "-"
									+ jadwalPelajaran.getSekolah().getId();
							String kelasLengkapLain = jadwalPelajaranLain.getKelas() + "-"
									+ jadwalPelajaranLain.getSemester() + "-"
									+ jadwalPelajaranLain.getSekolah().getId();

							if (kelasLengkap.equalsIgnoreCase(kelasLengkapLain) && ada
									&& (!sudahAda.contains(kode) && !sudahAda.contains(kode1))
									&& ((jadwalPelajaran.getHari() == null ? "" : jadwalPelajaran.getHari())
											.equals(jadwalPelajaranLain.getHari()))
									&& (

									(jamPelajaran.getWaktuMulaiD() >= jamPelajaranLain.getWaktuMulaiD() + 0.01
											&& jamPelajaran.getWaktuMulaiD() <= jamPelajaranLain.getWaktuSelesaiD()
													- 0.01)

											||

											(jamPelajaran.getWaktuSelesaiD() >= jamPelajaranLain.getWaktuMulaiD() + 0.01
													&& jamPelajaran.getWaktuSelesaiD() <= jamPelajaranLain
															.getWaktuSelesaiD() - 0.01)

											||

											(jamPelajaranLain.getWaktuMulaiD() >= jamPelajaran.getWaktuMulaiD() + 0.01
													&& jamPelajaranLain
															.getWaktuMulaiD() <= jamPelajaran.getWaktuSelesaiD() - 0.01)

											||

											(jamPelajaranLain.getWaktuSelesaiD() >= jamPelajaran.getWaktuMulaiD() + 0.01
													&& jamPelajaranLain.getWaktuSelesaiD() <= jamPelajaran
															.getWaktuSelesaiD() - 0.01)

											||

											(Common.numberFormat.get().format(jamPelajaranLain.getWaktuMulaiD())
													.equals(Common.numberFormat.get().format(jamPelajaran.getWaktuMulaiD()))
													&& Common.numberFormat.get().format(jamPelajaranLain.getWaktuSelesaiD())
															.equals(Common.numberFormat.get()
																	.format(jamPelajaran.getWaktuSelesaiD())))

									)) {
								sudahAda.add(kode);
								sudahAda.add(kode1);

								if (bentrokRuangans.containsKey(kelasLengkap)) {
									bentrokRuangans.get(kelasLengkap)
											.add(new JadwalPelajaran[] { jadwalPelajaran, jadwalPelajaranLain });
								} else {
									List<JadwalPelajaran[]> pps = new ArrayList<JadwalPelajaran[]>();
									pps.add(new JadwalPelajaran[] { jadwalPelajaran, jadwalPelajaranLain });
									bentrokRuangans.put(kelasLengkap, pps);
								}
							}
						}
					}
				}
			}
		}
		return bentrokRuangans;

	}

	public static Map<Guru, List<JadwalPelajaran[]>> checkBentrokBerdasarGuru(List<JadwalPelajaran> jadwalPelajarans,
			List<JadwalPelajaran> jadwalPelajaransLain) {

		Map<Guru, List<JadwalPelajaran[]>> bentrokRuangans = new HashMap<Guru, List<JadwalPelajaran[]>>();
		Set<String> sudahAda = new HashSet<String>();
		for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
			if (jadwalPelajaran == null) {
				continue;
			}

			if (jadwalPelajaran.getAbaikanBentrok()) {
				continue;
			}

			for (Object[] objects : jadwalPelajaran.populateJamPelajaran()) {
				JamPelajaran jamPelajaran = (JamPelajaran) objects[0];
				String hari = (String) objects[1];
				Guru guru = (Guru) objects[2];

				if (guru != null) {

					for (JadwalPelajaran jadwalPelajaranLain : jadwalPelajaransLain) {

						if (jadwalPelajaran == null || jadwalPelajaran.getId().equals(jadwalPelajaranLain.getId())) {
							continue;
						}

						if (jadwalPelajaranLain.getAbaikanBentrok()) {
							continue;
						}

						for (Object[] objectsLain : jadwalPelajaranLain.populateJamPelajaran()) {

							JamPelajaran jamPelajaranLain = (JamPelajaran) objectsLain[0];
							String hariLain = (String) objectsLain[1];
							Guru guruLain = (Guru) objectsLain[2];

							if (hari.equals(hariLain)) {

								if (guruLain != null) {

									if (guru.getId().equals(guruLain.getId())) {

										String kode = jadwalPelajaran.getId() + "_" + jadwalPelajaranLain.getId() + "_"
												+ guru.getId();
										String kode1 = jadwalPelajaranLain.getId() + "_" + jadwalPelajaran.getId() + "_"
												+ guruLain.getId();

										Long start1 = jamPelajaran.getMulai() == null ? null
												: jamPelajaran.getMulai().getTime() + 60L;
										Long end1 = jamPelajaran.getSelesai() == null ? null
												: jamPelajaran.getSelesai().getTime() - 60L;

										Long start2 = jamPelajaranLain.getMulai() == null ? null
												: jamPelajaranLain.getMulai().getTime() + 60;
										Long end2 = jamPelajaranLain.getSelesai() == null ? null
												: jamPelajaranLain.getSelesai().getTime() - 60L;

										if (start1 == null || end1 == null || start2 == null || end2 == null) {
											continue;
										}

										boolean ada = true;
										if (start1 != null && end1 != null && start2 != null && end2 != null) {
											ada = (start1 >= start2 && start1 <= end2)
													|| (end1 >= start2 && end1 <= end2);
										}

										if (guru.getId().equals(guruLain.getId()) && ada
												&& (!sudahAda.contains(kode) && !sudahAda.contains(kode1))
												&& ((jadwalPelajaran.getHari() == null ? "" : jadwalPelajaran.getHari())
														.equals(jadwalPelajaranLain.getHari()))
												&& (

												(jamPelajaran.getWaktuMulaiD() >= jamPelajaranLain.getWaktuMulaiD()
														+ 0.01
														&& jamPelajaran.getWaktuMulaiD() <= jamPelajaranLain
																.getWaktuSelesaiD() - 0.01)

														||

														(jamPelajaran.getWaktuSelesaiD() >= jamPelajaranLain
																.getWaktuMulaiD() + 0.01
																&& jamPelajaran.getWaktuSelesaiD() <= jamPelajaranLain
																		.getWaktuSelesaiD() - 0.01)

														||

														(jamPelajaranLain.getWaktuMulaiD() >= jamPelajaran
																.getWaktuMulaiD() + 0.01
																&& jamPelajaranLain.getWaktuMulaiD() <= jamPelajaran
																		.getWaktuSelesaiD() - 0.01)

														||

														(jamPelajaranLain.getWaktuSelesaiD() >= jamPelajaran
																.getWaktuMulaiD() + 0.01
																&& jamPelajaranLain.getWaktuSelesaiD() <= jamPelajaran
																		.getWaktuSelesaiD() - 0.01)

														||

														(Common.numberFormat.get().format(jamPelajaranLain.getWaktuMulaiD())
																.equals(Common.numberFormat.get()
																		.format(jamPelajaran.getWaktuMulaiD()))
																&& Common.numberFormat.get()
																		.format(jamPelajaranLain.getWaktuSelesaiD())
																		.equals(Common.numberFormat.get().format(
																				jamPelajaran.getWaktuSelesaiD())))

												)) {
											sudahAda.add(kode);
											sudahAda.add(kode1);

											if (bentrokRuangans.containsKey(guru)) {
												bentrokRuangans.get(guru).add(
														new JadwalPelajaran[] { jadwalPelajaran, jadwalPelajaranLain });
											} else {
												List<JadwalPelajaran[]> pps = new ArrayList<JadwalPelajaran[]>();
												pps.add(new JadwalPelajaran[] { jadwalPelajaran, jadwalPelajaranLain });
												bentrokRuangans.put(guru, pps);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return bentrokRuangans;

	}

	class JadwalPelajaranRenderer extends ais.ui.util.MyRowRenderer {

		private DetailJadwalMatapelajaranHelper detailJadwalMatapelajaranHelper = new DetailJadwalMatapelajaranHelper();
		private DetailKelasLesSiswaHelper detailKelasLesSiswaHelper = new DetailKelasLesSiswaHelper();
		private DetailpertemuanHelper detailpertemuanHelper = new DetailpertemuanHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {

						if (jadwalPelajaran.getKelasLesSiswa() != null) {
							detailKelasLesSiswaHelper.displayDetailPA(jadwalPelajaran.getKelasLesSiswa(), detail,
									addWindow);
						} else {

							Tabbox tabbox = new Tabbox();
							tabbox.setParent(detail);
							tabbox.setHeight("100%");
							tabbox.setWidth("100%");

							Tabs tabs = new Tabs();
							tabs.setParent(tabbox);

							final MyTabConfig tabSoal = new MyTabConfig("Manajemen Siswa");
							tabSoal.setParent(tabs);

							MyTabConfig tab1Pertemuan = new MyTabConfig();
							tab1Pertemuan.setParent(tabs);
							tab1Pertemuan.setLabel("Manajemen Pertemuan");

							MyTabConfig tab1Kehadiran = new MyTabConfig();
							tab1Kehadiran.setParent(tabs);
							tab1Kehadiran.setLabel("Manajemen Kehadiran");

							Tabpanels tabpanels = new Tabpanels();
							tabpanels.setParent(tabbox);

							Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
							tabpanelUtama.setStyle("min-height: 200px;");
							tabpanelUtama.setHeight("2000px");
							tabpanelUtama.setParent(tabpanels);

							detailJadwalMatapelajaranHelper.displayDetailPA(jadwalPelajaran, tabpanelUtama, addWindow);

							final Tabpanel tabpanelPertemuan = new ais.ui.util.MyTabpanel();
							tabpanelPertemuan.setStyle("min-height: 200px;");
							tabpanelPertemuan.setHeight("2000px");
							tabpanelPertemuan.setParent(tabpanels);

							tab1Pertemuan.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (tabpanelPertemuan.getChildren().isEmpty()) {
										aktifitasPembelajaranHelper.initDetail(jadwalPelajaran, tabpanelPertemuan, 0,
												1);

									}
								}
							});

							final Tabpanel tabpanelKehadiran = new ais.ui.util.MyTabpanel();
							tabpanelKehadiran.setStyle("min-height: 200px;");
							tabpanelKehadiran.setHeight("2000px");
							tabpanelKehadiran.setParent(tabpanels);

							tab1Kehadiran.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (tabpanelKehadiran.getChildren().isEmpty()) {
										detailpertemuanHelper.displayDetailPertemuan(jadwalPelajaran,
												tabpanelKehadiran);
									}
								}
							});
						}

					}

				}

			});

			new Label(jadwalPelajaran.getTahunAjaran() + "/" + jadwalPelajaran.getSemester()).setParent(arg0);
			RevisiHelper.createNewRevisi(JadwalPelajaran.class, jadwalPelajaran,
					jadwalPelajaran.getMatapelajaran() == null ? "" : jadwalPelajaran.getMatapelajaran().getNama())
					.setParent(arg0);
			new Label(jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama())
					.setParent(arg0);
			new Label(jadwalPelajaran.getKelas() == null ? "" : jadwalPelajaran.getKelas().getNama()).setParent(arg0);

			Common.displayGuruJadwalPelajaran(arg0, jadwalPelajaran, false);
			Common.displayHariJamRuanganJadwalPelajaranUmum(arg0, jadwalPelajaran);

			int jml = ((Number) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();

			new Label(jadwalPelajaran.getKeterangan()).setParent(arg0);
			new Label(Common.numberFormat.get().format(jml)).setParent(arg0);

			Hbox a;
			(a = Common.copyEditDeleteButtons(edit, edit, delete && jml == 0, jadwalPelajaran,
					JadwalPelajaranAction.this, true)).setParent(arg0);

			GeneralValueObject.tampilKunci(a, jadwalPelajaran, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}

			}, true);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Kurikulum", "/img/svg/edit-box-line.svg");
			// sclass ais-row-action-btn wajib supaya seragam dgn tombol aksi baris lain
			// (ukuran ikon 26px, padding, hover) -- lihat catatan di copyEditDeleteButtons.
			button.setSclass("ais-row-action-btn ais-row-action-kurikulum");
			button.setTooltiptext("Ubah Kurikulum");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					initBerdasarKurikulum(jadwalPelajaran);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(a);
		}

	}

	public static void onAddExternal(EventListener eventListener, JadwalPelajaran jadwalPelajaran) throws Exception {
		JadwalPelajaranAction skripsiAction = new JadwalPelajaranAction();
		skripsiAction.tbmuser = Common.getCurrentUser();
		skripsiAction.eventListener = eventListener;
		skripsiAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(skripsiAction.addWindow);
		skripsiAction.addWindow.setHeight("95%");
		skripsiAction.addWindow.setWidth("750px");

		skripsiAction.init(jadwalPelajaran);

		Common.freezeGanti(skripsiAction.tahunAjaran, skripsiAction.semester, skripsiAction.kelas,
				skripsiAction.matapelajaran, skripsiAction.masaJadwalPelajaran);

		skripsiAction.addWindow.setVisible(true);
		skripsiAction.addWindow.setClosable(true);
		skripsiAction.addWindow.onModal();

	}

	public void onAdd(Event event) throws Exception {

		JadwalPelajaran jadwalPelajaran = new JadwalPelajaran();
		jadwalPelajaran.setTahunAjaran((String) searchta.getSelectedItem().getValue());
		jadwalPelajaran.setSemester((Integer) searchsmt.getSelectedItem().getValue());
		jadwalPelajaran.setKelas(
				(KelasSiswa) (searchkelas.getSelectedItem() == null ? null : searchkelas.getSelectedItem().getValue()));

		init(jadwalPelajaran);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAddBerdasarKurikulum(Event event) throws Exception {
		JadwalPelajaran jadwalPelajaran = new JadwalPelajaran();
		jadwalPelajaran.setTahunAjaran((String) searchta.getSelectedItem().getValue());
		jadwalPelajaran.setSemester((Integer) searchsmt.getSelectedItem().getValue());
		jadwalPelajaran.setKelas(
				(KelasSiswa) (searchkelas.getSelectedItem() == null ? null : searchkelas.getSelectedItem().getValue()));
		initBerdasarKurikulum(jadwalPelajaran);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jadwalPelajaran = (JadwalPelajaran) obj;
		init(jadwalPelajaran);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final JadwalPelajaran jadwalPelajaran) throws Exception {
		gridBerdasarkanKurikulum = null;
		this.jadwalPelajaran = jadwalPelajaran;
		addWindow.setTitle(jadwalPelajaran.getId() == null ? "Tambah Jadwal Pelajaran" : "Ubah Jadwal Pelajaran");
		addWindow.setWidth("750px");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				jadwalPelajaran.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester = new Combobox());
		Comboitem comboitem = new Comboitem(JadwalPelajaran.GANJIL);
		comboitem.setValue(1);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(JadwalPelajaran.GENAP);
		comboitem.setValue(2);
		semester.appendChild(comboitem);
		Common.selectComboItem(true, semester, jadwalPelajaran.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, jadwalPelajaran.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, jadwalPelajaran.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getKelasLesSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas *"));
		row.appendChild(kelas = new Combobox());
		kelas.setWidth("90%");
		kelas.setReadonly(true);
		Common.selectComboItem(true, kelas, jadwalPelajaran.getKelas());

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getKelas() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Les Siswa *"));
		row.appendChild(kelasLesSiswa = new AmbilDataKelasLesSiswaBanbox());
		kelasLesSiswa.setAttribute("kelasLesSiswa", jadwalPelajaran.getKelasLesSiswa());
		kelasLesSiswa.setValue(
				jadwalPelajaran.getKelasLesSiswa() == null ? "" : jadwalPelajaran.getKelasLesSiswa().getNama());
		kelasLesSiswa.setWidth("90%");

		if (jadwalPelajaran.getKelasLesSiswa() != null) {
			kelasLesSiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mata Pelajaran *"));
		row.appendChild(matapelajaran = new Combobox());
		matapelajaran.setWidth("90%");
		matapelajaran.setReadonly(true);
		Common.selectComboItem(true, matapelajaran, jadwalPelajaran.getMatapelajaran());

		final EventListener eventListenerPilihKelas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean kelasReguler = kelasLesSiswa == null || kelasLesSiswa.getAttribute("kelasLesSiswa") == null;
				if (tahunAjaran != null && tahunAjaran.getParent() != null) tahunAjaran.getParent().setVisible(kelasReguler);
				if (semester != null && semester.getParent() != null) semester.getParent().setVisible(kelasReguler);
				if (matapelajaran != null && matapelajaran.getParent() != null) matapelajaran.getParent().setVisible(kelasReguler);
				if (kelas != null && kelas.getParent() != null) kelas.getParent().setVisible(kelasReguler);
				boolean kelasBelumDipilih = kelas == null || kelas.getSelectedItem() == null
						|| kelas.getSelectedItem().getValue() == null;
				if (kelasLesSiswa != null && kelasLesSiswa.getParent() != null) {
					kelasLesSiswa.getParent().setVisible(kelasBelumDipilih);
				}
			}
		};

		kelasLesSiswa.setEventListener(eventListenerPilihKelas);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Pembelajaran *"));
		row.appendChild(masaJadwalPelajaran = new Combobox());
		masaJadwalPelajaran.setWidth("90%");
		masaJadwalPelajaran.setReadonly(true);
		Common.selectComboItem(true, masaJadwalPelajaran, jadwalPelajaran.getMasaJadwalPelajaran());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(abaikanBentrok = new MyCheckboxConfig("Abaikan jika ada jadwal bentrok"));
		abaikanBentrok.setChecked(jadwalPelajaran.getAbaikanBentrok());

		List<String> hrs = jadwalPelajaran.populateHari();

		row = new MyFormRow();
		row.setVisible(hrs.size() == 0);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar",
				"/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran I"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hari = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari.appendChild(comboitem);

		Common.selectComboItem(true, hari, jadwalPelajaran.getHari());
		guru = new AmbilDataGuruBanbox();
		Guru guruData = (Guru) guru.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru() != null
				&& !jadwalPelajaran.getGuru().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari()).setParent(hbox);
		} else {
			hbox.appendChild(hari);
		}

		hari.setCols(5);
		hari.setReadonly(true);

		jamPelajaran = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru() != null
				&& !jadwalPelajaran.getGuru().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran() == null ? "" : jadwalPelajaran.getJamPelajaran().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran);
		}

		jamPelajaran.setCols(8);
		jamPelajaran.setReadonly(true);

		subMatapelajaran = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru() != null
				&& !jadwalPelajaran.getGuru().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran);
		}

		subMatapelajaran.setCols(8);
		subMatapelajaran.setReadonly(true);

		if (jadwalPelajaran.getGuru() != null && guru.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru().getNama()).setParent(hbox);
		} else if (guru.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru);
		}
		guru.setAttribute("guru", jadwalPelajaran.getGuru());
		guru.setAttribute("myValue", jadwalPelajaran.getGuru());
		guru.setValue(jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNamaGuru());
		guru.setWidth("90%");
		guru.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 1);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari2.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari2.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari2() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran II"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari2 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari2.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari2.appendChild(comboitem);

		Common.selectComboItem(true, hari2, jadwalPelajaran.getHari2());

		guru2 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru2.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru2() != null
				&& !jadwalPelajaran.getGuru2().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari2()).setParent(hbox);
		} else {
			hbox.appendChild(hari2);
		}

		hari2.setCols(5);
		hari2.setReadonly(true);

		jamPelajaran2 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru2() != null
				&& !jadwalPelajaran.getGuru2().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran2() == null ? "" : jadwalPelajaran.getJamPelajaran2().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran2);
		}

		jamPelajaran2.setCols(8);
		jamPelajaran2.setReadonly(true);

		subMatapelajaran2 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru2() != null
				&& !jadwalPelajaran.getGuru2().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran2() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran2().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran2);
		}

		subMatapelajaran2.setCols(8);
		subMatapelajaran2.setReadonly(true);

		if (jadwalPelajaran.getGuru2() != null && guru2.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru2().getNama()).setParent(hbox);
		} else if (guru2.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru2);
		}

		guru2.setAttribute("guru", jadwalPelajaran.getGuru2());
		guru2.setAttribute("myValue", jadwalPelajaran.getGuru2());
		guru2.setValue(jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getNamaGuru());
		guru2.setWidth("90%");
		guru2.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 2);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari3.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari3.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari3() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran III"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari3 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari3.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari3.appendChild(comboitem);

		Common.selectComboItem(true, hari3, jadwalPelajaran.getHari3());

		guru3 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru3.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru3() != null
				&& !jadwalPelajaran.getGuru3().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari3()).setParent(hbox);
		} else {
			hbox.appendChild(hari3);
		}

		hari3.setCols(5);
		hari3.setReadonly(true);

		jamPelajaran3 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru3() != null
				&& !jadwalPelajaran.getGuru3().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran3() == null ? "" : jadwalPelajaran.getJamPelajaran3().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran3);
		}

		jamPelajaran3.setCols(8);
		jamPelajaran3.setReadonly(true);

		subMatapelajaran3 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru3() != null
				&& !jadwalPelajaran.getGuru3().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran3() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran3().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran3);
		}

		subMatapelajaran3.setCols(8);
		subMatapelajaran3.setReadonly(true);

		if (jadwalPelajaran.getGuru3() != null && guru3.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru3().getNama()).setParent(hbox);
		} else if (guru3.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru3);
		}
		guru3.setAttribute("guru", jadwalPelajaran.getGuru3());
		guru3.setAttribute("myValue", jadwalPelajaran.getGuru3());
		guru3.setValue(jadwalPelajaran.getGuru3() == null ? "" : jadwalPelajaran.getGuru3().getNamaGuru());
		guru3.setWidth("90%");
		guru3.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 3);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari4.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari4.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari4() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran IV"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari4 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari4.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari4.appendChild(comboitem);

		Common.selectComboItem(true, hari4, jadwalPelajaran.getHari4());

		guru4 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru4.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru4() != null
				&& !jadwalPelajaran.getGuru4().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari4()).setParent(hbox);
		} else {
			hbox.appendChild(hari4);
		}

		hari4.setCols(5);
		hari4.setReadonly(true);

		jamPelajaran4 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru4() != null
				&& !jadwalPelajaran.getGuru4().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran4() == null ? "" : jadwalPelajaran.getJamPelajaran4().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran4);
		}

		jamPelajaran4.setCols(8);
		jamPelajaran4.setReadonly(true);

		subMatapelajaran4 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru4() != null
				&& !jadwalPelajaran.getGuru4().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran4() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran4().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran4);
		}

		subMatapelajaran4.setCols(8);
		subMatapelajaran4.setReadonly(true);

		guru4 = new AmbilDataGuruBanbox();
		if (jadwalPelajaran.getGuru4() != null && guru4.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru4().getNama()).setParent(hbox);
		} else if (guru4.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru4);
		}
		guru4.setAttribute("guru", jadwalPelajaran.getGuru4());
		guru4.setAttribute("myValue", jadwalPelajaran.getGuru4());
		guru4.setValue(jadwalPelajaran.getGuru4() == null ? "" : jadwalPelajaran.getGuru4().getNamaGuru());
		guru4.setWidth("90%");
		guru4.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 4);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari5.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari5.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari5() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran V"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari5 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari5.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari5.appendChild(comboitem);

		Common.selectComboItem(true, hari5, jadwalPelajaran.getHari5());
		guru5 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru5.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru5() != null
				&& !jadwalPelajaran.getGuru5().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari5()).setParent(hbox);
		} else {
			hbox.appendChild(hari5);
		}
		hari5.setCols(5);
		hari5.setReadonly(true);

		jamPelajaran5 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru5() != null
				&& !jadwalPelajaran.getGuru5().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran5() == null ? "" : jadwalPelajaran.getJamPelajaran5().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran5);
		}
		jamPelajaran5.setCols(8);
		jamPelajaran5.setReadonly(true);

		subMatapelajaran5 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru5() != null
				&& !jadwalPelajaran.getGuru5().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran5() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran5().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran5);
		}

		subMatapelajaran5.setCols(8);
		subMatapelajaran5.setReadonly(true);

		guru5 = new AmbilDataGuruBanbox();
		if (jadwalPelajaran.getGuru5() != null && guru5.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru5().getNama()).setParent(hbox);
		} else if (guru5.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru5);
		}
		guru5.setAttribute("guru", jadwalPelajaran.getGuru5());
		guru5.setAttribute("myValue", jadwalPelajaran.getGuru5());
		guru5.setValue(jadwalPelajaran.getGuru5() == null ? "" : jadwalPelajaran.getGuru5().getNamaGuru());
		guru5.setWidth("90%");
		guru5.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 5);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari6.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari6.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari6() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran VI"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari6 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari6.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari6.appendChild(comboitem);

		Common.selectComboItem(true, hari6, jadwalPelajaran.getHari6());
		guru6 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru6.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru6() != null
				&& !jadwalPelajaran.getGuru6().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari6()).setParent(hbox);
		} else {
			hbox.appendChild(hari6);
		}
		hari6.setCols(6);
		hari6.setReadonly(true);

		jamPelajaran6 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru6() != null
				&& !jadwalPelajaran.getGuru6().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran6() == null ? "" : jadwalPelajaran.getJamPelajaran6().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran6);
		}

		jamPelajaran6.setCols(8);
		jamPelajaran6.setReadonly(true);

		subMatapelajaran6 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru6() != null
				&& !jadwalPelajaran.getGuru6().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran6() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran6().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran6);
		}

		subMatapelajaran6.setCols(8);
		subMatapelajaran6.setReadonly(true);

		guru6 = new AmbilDataGuruBanbox();

		if (jadwalPelajaran.getGuru6() != null && guru6.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru6().getNama()).setParent(hbox);
		} else if (guru6.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru6);
		}
		guru6.setAttribute("guru", jadwalPelajaran.getGuru6());
		guru6.setAttribute("myValue", jadwalPelajaran.getGuru6());
		guru6.setValue(jadwalPelajaran.getGuru6() == null ? "" : jadwalPelajaran.getGuru6().getNamaGuru());
		guru6.setWidth("90%");
		guru6.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 6);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari7.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari7.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari7() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran VII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari7 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari7.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari7.appendChild(comboitem);

		Common.selectComboItem(true, hari7, jadwalPelajaran.getHari7());

		guru7 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru7.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru7() != null
				&& !jadwalPelajaran.getGuru7().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari7()).setParent(hbox);
		} else {
			hbox.appendChild(hari7);
		}

		hari7.setCols(7);
		hari7.setReadonly(true);

		jamPelajaran7 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru7() != null
				&& !jadwalPelajaran.getGuru7().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran7() == null ? "" : jadwalPelajaran.getJamPelajaran7().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran7);
		}

		jamPelajaran7.setCols(8);
		jamPelajaran7.setReadonly(true);

		subMatapelajaran7 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru7() != null
				&& !jadwalPelajaran.getGuru7().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran7() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran7().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran7);
		}

		subMatapelajaran7.setCols(8);
		subMatapelajaran7.setReadonly(true);

		guru7 = new AmbilDataGuruBanbox();
		if (jadwalPelajaran.getGuru7() != null && guru7.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru7().getNama()).setParent(hbox);
		} else if (guru7.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru7);
		}
		guru7.setAttribute("guru", jadwalPelajaran.getGuru7());
		guru7.setAttribute("myValue", jadwalPelajaran.getGuru7());
		guru7.setValue(jadwalPelajaran.getGuru7() == null ? "" : jadwalPelajaran.getGuru7().getNamaGuru());
		guru7.setWidth("90%");
		guru7.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 7);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari8.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari8.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari8() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran VIII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari8 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari8.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari8.appendChild(comboitem);

		Common.selectComboItem(true, hari8, jadwalPelajaran.getHari8());

		guru8 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru8.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru8() != null
				&& !jadwalPelajaran.getGuru8().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari8()).setParent(hbox);
		} else {
			hbox.appendChild(hari8);
		}

		hari8.setCols(8);
		hari8.setReadonly(true);

		jamPelajaran8 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru8() != null
				&& !jadwalPelajaran.getGuru8().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran8() == null ? "" : jadwalPelajaran.getJamPelajaran8().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran8);
		}

		jamPelajaran8.setCols(8);
		jamPelajaran8.setReadonly(true);

		subMatapelajaran8 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru8() != null
				&& !jadwalPelajaran.getGuru8().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran8() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran8().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran8);
		}

		subMatapelajaran8.setCols(8);
		subMatapelajaran8.setReadonly(true);

		guru8 = new AmbilDataGuruBanbox();
		if (jadwalPelajaran.getGuru8() != null && guru8.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru8().getNama()).setParent(hbox);
		} else if (guru8.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru8);
		}
		guru8.setAttribute("guru", jadwalPelajaran.getGuru8());
		guru8.setAttribute("myValue", jadwalPelajaran.getGuru8());
		guru8.setValue(jadwalPelajaran.getGuru8() == null ? "" : jadwalPelajaran.getGuru8().getNamaGuru());
		guru8.setWidth("90%");
		guru8.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 8);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari9.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari9.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari9() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran IX"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari9 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari9.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari9.appendChild(comboitem);

		Common.selectComboItem(true, hari9, jadwalPelajaran.getHari9());

		guru9 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru9.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru9() != null
				&& !jadwalPelajaran.getGuru9().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari9()).setParent(hbox);
		} else {
			hbox.appendChild(hari9);
		}

		hari9.setCols(9);
		hari9.setReadonly(true);

		jamPelajaran9 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru9() != null
				&& !jadwalPelajaran.getGuru9().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran9() == null ? "" : jadwalPelajaran.getJamPelajaran9().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran9);
		}
		jamPelajaran9.setCols(8);
		jamPelajaran9.setReadonly(true);

		subMatapelajaran9 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru9() != null
				&& !jadwalPelajaran.getGuru9().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran9() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran9().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran9);
		}

		subMatapelajaran9.setCols(8);
		subMatapelajaran9.setReadonly(true);

		guru9 = new AmbilDataGuruBanbox();
		if (jadwalPelajaran.getGuru9() != null && guru9.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru9().getNama()).setParent(hbox);
		} else if (guru9.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru9);
		}
		guru9.setAttribute("guru", jadwalPelajaran.getGuru9());
		guru9.setAttribute("myValue", jadwalPelajaran.getGuru9());
		guru9.setValue(jadwalPelajaran.getGuru9() == null ? "" : jadwalPelajaran.getGuru9().getNamaGuru());
		guru9.setWidth("90%");
		guru9.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 9);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari10.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari10.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari10() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran X"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari10 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari10.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari10.appendChild(comboitem);

		Common.selectComboItem(true, hari10, jadwalPelajaran.getHari10());

		guru10 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru10.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru10() != null
				&& !jadwalPelajaran.getGuru10().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari10()).setParent(hbox);
		} else {
			hbox.appendChild(hari10);
		}

		hari10.setCols(10);
		hari10.setReadonly(true);

		jamPelajaran10 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru10() != null
				&& !jadwalPelajaran.getGuru10().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran10() == null ? "" : jadwalPelajaran.getJamPelajaran10().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran10);
		}

		jamPelajaran10.setCols(8);
		jamPelajaran10.setReadonly(true);

		subMatapelajaran10 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru10() != null
				&& !jadwalPelajaran.getGuru10().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran10() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran10().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran10);
		}

		subMatapelajaran10.setCols(8);
		subMatapelajaran10.setReadonly(true);

		guru10 = new AmbilDataGuruBanbox();
		if (jadwalPelajaran.getGuru10() != null && guru10.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru10().getNama()).setParent(hbox);
		} else if (guru10.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru10);
		}
		guru10.setAttribute("guru", jadwalPelajaran.getGuru10());
		guru10.setAttribute("myValue", jadwalPelajaran.getGuru10());
		guru10.setValue(jadwalPelajaran.getGuru10() == null ? "" : jadwalPelajaran.getGuru10().getNamaGuru());
		guru10.setWidth("90%");
		guru10.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 10);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari11.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari11.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari11() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XI"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari11 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari11.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari11.appendChild(comboitem);

		Common.selectComboItem(true, hari11, jadwalPelajaran.getHari11());

		guru11 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru11.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru11() != null
				&& !jadwalPelajaran.getGuru11().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari11()).setParent(hbox);
		} else {
			hbox.appendChild(hari11);
		}

		hari11.setCols(11);
		hari11.setReadonly(true);

		jamPelajaran11 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru11() != null
				&& !jadwalPelajaran.getGuru11().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran11() == null ? "" : jadwalPelajaran.getJamPelajaran11().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran11);
		}

		jamPelajaran11.setCols(8);
		jamPelajaran11.setReadonly(true);

		subMatapelajaran11 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru11() != null
				&& !jadwalPelajaran.getGuru11().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran11() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran11().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran11);
		}

		subMatapelajaran11.setCols(8);
		subMatapelajaran11.setReadonly(true);

		guru11 = new AmbilDataGuruBanbox();
		if (jadwalPelajaran.getGuru11() != null && guru11.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru11().getNama()).setParent(hbox);
		} else if (guru11.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru11);
		}
		guru11.setAttribute("guru", jadwalPelajaran.getGuru11());
		guru11.setAttribute("myValue", jadwalPelajaran.getGuru11());
		guru11.setValue(jadwalPelajaran.getGuru11() == null ? "" : jadwalPelajaran.getGuru11().getNamaGuru());
		guru11.setWidth("90%");
		guru11.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 11);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari12.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(jadwalPelajaran.getHari12() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari12 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari12.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari12.appendChild(comboitem);

		Common.selectComboItem(true, hari12, jadwalPelajaran.getHari12());

		guru12 = new AmbilDataGuruBanbox();
		guruData = (Guru) guru12.getAttribute("guru");

		if (guruData != null && jadwalPelajaran.getGuru12() != null
				&& !jadwalPelajaran.getGuru12().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getHari12()).setParent(hbox);
		} else {
			hbox.appendChild(hari12);
		}

		hari12.setCols(12);
		hari12.setReadonly(true);

		jamPelajaran12 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru12() != null
				&& !jadwalPelajaran.getGuru12().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getJamPelajaran12() == null ? "" : jadwalPelajaran.getJamPelajaran12().getNama())
					.setParent(hbox);
		} else {
			hbox.appendChild(jamPelajaran12);
		}

		jamPelajaran12.setCols(8);
		jamPelajaran12.setReadonly(true);

		subMatapelajaran12 = new Combobox();
		if (guruData != null && jadwalPelajaran.getGuru12() != null
				&& !jadwalPelajaran.getGuru12().getId().equals(guruData.getId())) {
			new Label(jadwalPelajaran.getSubMatapelajaran12() == null ? ""
					: jadwalPelajaran.getSubMatapelajaran12().getNama()).setParent(hbox);
		} else {
			hbox.appendChild(subMatapelajaran12);
		}

		subMatapelajaran12.setCols(8);
		subMatapelajaran12.setReadonly(true);

		guru12 = new AmbilDataGuruBanbox();
		if (jadwalPelajaran.getGuru12() != null && guru12.getAttribute("guru") != null) {
			new Label(jadwalPelajaran.getGuru12().getNama()).setParent(hbox);
		} else if (guru12.getAttribute("guru") != null) {
			new Label(guruData.getNama()).setParent(hbox);
		} else {
			hbox.appendChild(guru12);
		}
		guru12.setAttribute("guru", jadwalPelajaran.getGuru12());
		guru12.setAttribute("myValue", jadwalPelajaran.getGuru12());
		guru12.setValue(jadwalPelajaran.getGuru12() == null ? "" : jadwalPelajaran.getGuru12().getNamaGuru());
		guru12.setWidth("90%");
		guru12.setReadonly(true);

		hari.setCols(4);
		hari2.setCols(4);
		hari3.setCols(4);
		hari4.setCols(4);
		hari5.setCols(4);
		hari6.setCols(4);
		hari7.setCols(4);
		hari8.setCols(4);
		hari9.setCols(4);
		hari10.setCols(4);
		hari11.setCols(4);
		hari12.setCols(4);

		EventListener eventListenerSekolah = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				String ta = (String) tahunAjaran.getSelectedItem().getValue();
				Integer smt = (Integer) semester.getSelectedItem().getValue();

				Matapelajaran mk = (Matapelajaran) (matapelajaran.getSelectedItem() == null ? null
						: matapelajaran.getSelectedItem().getValue());

				List<SubMatapelajaran> subMatapelajarans = ConstantValues
						.simpleList(
								HibernateUtil.currentSession().createCriteria(SubMatapelajaran.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("matapelajaran", mk)).addOrder(Order.asc("nama")),
								SubMatapelajaran.class);

				if (subMatapelajarans.isEmpty()) {
					subMatapelajaran.setVisible(false);
					subMatapelajaran2.setVisible(false);
					subMatapelajaran3.setVisible(false);
					subMatapelajaran4.setVisible(false);
					subMatapelajaran5.setVisible(false);
					subMatapelajaran6.setVisible(false);
					subMatapelajaran7.setVisible(false);
					subMatapelajaran8.setVisible(false);
					subMatapelajaran9.setVisible(false);
					subMatapelajaran10.setVisible(false);
					subMatapelajaran11.setVisible(false);
					subMatapelajaran12.setVisible(false);
				} else {
					Common.insertComboItems(subMatapelajaran, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran2, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran3, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran4, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran5, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran6, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran7, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran8, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran9, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran10, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran11, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran12, "nama", subMatapelajarans);
				}

				Common.insertCombo(masaJadwalPelajaran, new String[] { "nama", "tahunAjaran", "namaSmt" },
						MasaJadwalPelajaran.class,
						Restrictions.and(
								Restrictions.or(Restrictions.isNull("semester"), Restrictions.eq("semester", smt)),
								Restrictions.and(Restrictions.eq("tahunAjaran", ta),
										Restrictions.and(
												Restrictions.or(Restrictions.isNull("sekolah"),
														Restrictions.eq("sekolah", s)),
												Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true))))));
				Common.selectComboItem(true, masaJadwalPelajaran, jadwalPelajaran.getMasaJadwalPelajaran());

				Common.insertCombo(jamPelajaran, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				MyComboitemConfig comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran, jadwalPelajaran.getJamPelajaran());

				guru.setAttribute("guru", jadwalPelajaran.getGuru());
				guru.setAttribute("myValue", jadwalPelajaran.getGuru());
				guru.setValue(jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNamaGuru());

				Common.insertCombo(jamPelajaran2, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran2.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran2, jadwalPelajaran.getJamPelajaran2());

				guru2.setAttribute("guru", jadwalPelajaran.getGuru2());
				guru2.setAttribute("myValue", jadwalPelajaran.getGuru2());
				guru2.setValue(jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getNamaGuru());

				Common.insertCombo(jamPelajaran3, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran3.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran3, jadwalPelajaran.getJamPelajaran3());

				guru3.setAttribute("guru", jadwalPelajaran.getGuru3());
				guru3.setAttribute("myValue", jadwalPelajaran.getGuru3());
				guru3.setValue(jadwalPelajaran.getGuru3() == null ? "" : jadwalPelajaran.getGuru3().getNamaGuru());

				Common.insertCombo(jamPelajaran4, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran4.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran4, jadwalPelajaran.getJamPelajaran4());

				guru4.setAttribute("guru", jadwalPelajaran.getGuru4());
				guru4.setAttribute("myValue", jadwalPelajaran.getGuru4());
				guru4.setValue(jadwalPelajaran.getGuru4() == null ? "" : jadwalPelajaran.getGuru4().getNamaGuru());

				Common.insertCombo(jamPelajaran5, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran5.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran5, jadwalPelajaran.getJamPelajaran5());

				guru5.setAttribute("guru", jadwalPelajaran.getGuru5());
				guru5.setAttribute("myValue", jadwalPelajaran.getGuru5());
				guru5.setValue(jadwalPelajaran.getGuru5() == null ? "" : jadwalPelajaran.getGuru5().getNamaGuru());

				Common.insertCombo(jamPelajaran6, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran6.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran6, jadwalPelajaran.getJamPelajaran6());

				guru6.setAttribute("guru", jadwalPelajaran.getGuru6());
				guru6.setAttribute("myValue", jadwalPelajaran.getGuru6());
				guru6.setValue(jadwalPelajaran.getGuru6() == null ? "" : jadwalPelajaran.getGuru6().getNamaGuru());

				Common.insertCombo(jamPelajaran7, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran7.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran7, jadwalPelajaran.getJamPelajaran7());

				guru7.setAttribute("guru", jadwalPelajaran.getGuru7());
				guru7.setAttribute("myValue", jadwalPelajaran.getGuru7());
				guru7.setValue(jadwalPelajaran.getGuru7() == null ? "" : jadwalPelajaran.getGuru7().getNamaGuru());

				Common.insertCombo(jamPelajaran8, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran8.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran8, jadwalPelajaran.getJamPelajaran8());

				guru8.setAttribute("guru", jadwalPelajaran.getGuru8());
				guru8.setAttribute("myValue", jadwalPelajaran.getGuru8());
				guru8.setValue(jadwalPelajaran.getGuru8() == null ? "" : jadwalPelajaran.getGuru8().getNamaGuru());

				Common.insertCombo(jamPelajaran9, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran9.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran9, jadwalPelajaran.getJamPelajaran9());

				guru9.setAttribute("guru", jadwalPelajaran.getGuru9());
				guru9.setAttribute("myValue", jadwalPelajaran.getGuru9());
				guru9.setValue(jadwalPelajaran.getGuru9() == null ? "" : jadwalPelajaran.getGuru9().getNamaGuru());

				Common.insertCombo(jamPelajaran10, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran10.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran10, jadwalPelajaran.getJamPelajaran10());

				guru10.setAttribute("guru", jadwalPelajaran.getGuru10());
				guru10.setAttribute("myValue", jadwalPelajaran.getGuru10());
				guru10.setValue(jadwalPelajaran.getGuru10() == null ? "" : jadwalPelajaran.getGuru10().getNamaGuru());

				Common.insertCombo(jamPelajaran11, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran11.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran11, jadwalPelajaran.getJamPelajaran11());

				guru11.setAttribute("guru", jadwalPelajaran.getGuru11());
				guru11.setAttribute("myValue", jadwalPelajaran.getGuru11());
				guru11.setValue(jadwalPelajaran.getGuru11() == null ? "" : jadwalPelajaran.getGuru11().getNamaGuru());

				Common.insertCombo(jamPelajaran12, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran12.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran12, jadwalPelajaran.getJamPelajaran12());

				guru12.setAttribute("guru", jadwalPelajaran.getGuru12());
				guru12.setAttribute("myValue", jadwalPelajaran.getGuru12());
				guru12.setValue(jadwalPelajaran.getGuru12() == null ? "" : jadwalPelajaran.getGuru12().getNamaGuru());

				Common.insertCombo(kelas, new String[] { "nama", "tahunAjaran", "ruang" }, "keterangan",
						KelasSiswa.class,
						Restrictions.and(Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()),
								(Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true))))));
				Common.selectComboItem(true, kelas, jadwalPelajaran.getKelas());
			}
		};

		sekolah.addEventListener("onChange", eventListenerSekolah);
		tahunAjaran.addEventListener("onChange", eventListenerSekolah);

		EventListener kelasListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);
				KelasSiswa myKelasSiswa = (KelasSiswa) (kelas.getSelectedItem() == null ? null
						: kelas.getSelectedItem().getValue());

				List<Long> longs = myKelasSiswa == null ? new ArrayList<Long>() : myKelasSiswa.ambilMk();

				Common.insertCombo(matapelajaran, new String[] { "nama", "jenisPenilaian", "sekolah" },
						Matapelajaran.class,
						Restrictions.and(
								longs.isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.not(Restrictions.in("id", longs)),

								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))));
				Common.selectComboItem(true, matapelajaran, jadwalPelajaran.getMatapelajaran());
				matapelajaran.setReadonly(true);

				eventListenerPilihKelas.onEvent(null);

			}
		};

		kelas.addEventListener("onChange", kelasListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jadwalPelajaran.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					if (eventListener != null) {
						eventListener
								.onEvent(new Event("", event.getTarget(), JadwalPelajaranAction.this.jadwalPelajaran));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		Common.createDefaultTimer(eventListenerSekolah);
		Common.createDefaultTimer(kelasListener);

		if (jadwalPelajaran.getDikunci() != null) {
			save.setVisible(false);
			Common.freezeGanti(center, true);
		}

	}

	@SuppressWarnings("deprecation")
	private void initBerdasarKurikulum(final JadwalPelajaran jadwalPelajaran) throws Exception {
		this.jadwalPelajaran = jadwalPelajaran;
		addWindow.setTitle(jadwalPelajaran.getId() == null ? "Tambah Jadwal Pelajaran" : "Ubah Jadwal Pelajaran");
		addWindow.setWidth("98%");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				jadwalPelajaran.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");
		tahunAjaran.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester = new Combobox());
		Comboitem comboitem = new Comboitem(JadwalPelajaran.GANJIL);
		comboitem.setValue(1);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(JadwalPelajaran.GENAP);
		comboitem.setValue(2);
		semester.appendChild(comboitem);
		Common.selectComboItem(true, semester, jadwalPelajaran.getSemester());
		semester.setWidth("90%");
		semester.setDisabled(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(true, yayasan, jadwalPelajaran.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.selectComboItem(true, sekolah, jadwalPelajaran.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas *"));
		row.appendChild(kelas = new Combobox());
		kelas.setWidth("90%");
		kelas.setReadonly(true);

		rowMatapelajaran = new MyFormRow();
		rowMatapelajaran.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowMatapelajaran, "2");

		final EventListener eventListener1 = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				gridBerdasarkanKurikulum = null;
				Common.clear(rowMatapelajaran);

				KelasSiswa kelasSiswa = (KelasSiswa) (kelas.getSelectedItem() == null ? null
						: kelas.getSelectedItem().getValue());

				if (kelasSiswa == null || kelasSiswa.getKurikulumSekolah() == null) {
					return;
				}
				MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
				myGroupboxStyled.setParent(rowMatapelajaran);

				myGroupboxStyled.appendChild(new MyCaptionStyled("Jadwal Matapelajaran"));

				gridBerdasarkanKurikulum = new Grid();
				gridBerdasarkanKurikulum.setSclass("fgrid");
				gridBerdasarkanKurikulum.setParent(myGroupboxStyled);
				gridBerdasarkanKurikulum.setWidth("100%");

				Columns columns = new Columns();
				columns.setParent(gridBerdasarkanKurikulum);

				MyColumnConfig column = new MyColumnConfig("Mata Pelajaran");
				column.setParent(columns);
				column.setWidth("10%");

				column = new MyColumnConfig("Jadwal I");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal II");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal III");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal IV");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal V");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal VI");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal VII");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal VIII");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal IX");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal X");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal XI");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Jadwal XII");
				column.setParent(columns);
				column.setWidth("6%");

				column = new MyColumnConfig("Masa");
				column.setParent(columns);
				column.setWidth("7%");

				column = new MyColumnConfig("Pert.");
				column.setParent(columns);
				column.setWidth("5%");

				column = new MyColumnConfig("Ket.");
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(gridBerdasarkanKurikulum);

				List<Long> longs = kelasSiswa.ambilMk();

				Session session = HibernateUtil.currentSession();
				List<KurikulumPunyaMatapelajaran> matapelajarans =

						ConstantValues
								.simpleList(
										session.createCriteria(KurikulumPunyaMatapelajaran.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(Restrictions.eq("kurikulumSekolah",
														kelasSiswa.getKurikulumSekolah()))
												.createAlias("matapelajaran", "matapelajaran")
												.add(longs.isEmpty() ? Restrictions.sqlRestriction("true")
														: Restrictions.not(Restrictions.in("matapelajaran.id", longs)))
												.add(Restrictions.eq("matapelajaran.aktif", true))
												.addOrder(Order.asc("matapelajaran.urutan"))
												.addOrder(Order.asc("matapelajaran.nama")),
										KurikulumPunyaMatapelajaran.class);
				for (final KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : matapelajarans) {
					if (kurikulumPunyaMatapelajaran != null && kurikulumPunyaMatapelajaran.getMatapelajaran() != null) {
						JadwalPelajaran a = (JadwalPelajaran) session.createCriteria(JadwalPelajaran.class)
								.add(Restrictions.eq("sekolah", kelasSiswa.getSekolah()))
								.add(Restrictions.eq("matapelajaran", kurikulumPunyaMatapelajaran.getMatapelajaran()))
								.add(Restrictions.eq("kelas", kelasSiswa))
								.add(Restrictions.eq("semester", semester.getSelectedItem().getValue()))
								.add(Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
								.setMaxResults(1).uniqueResult();
						System.out.println("a -> " + a + " " + kurikulumPunyaMatapelajaran.getMatapelajaran());
						if (a == null) {
							a = new JadwalPelajaran();
							a.setMatapelajaran(kurikulumPunyaMatapelajaran.getMatapelajaran());
							a.setKurikulumPunyaMatapelajaran(kurikulumPunyaMatapelajaran);
							a.setKelas(kelasSiswa);
							a.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
							a.setSemester((Integer) semester.getSelectedItem().getValue());
						}

						List<SubMatapelajaran> subMatapelajarans = ConstantValues.simpleList(session
								.createCriteria(SubMatapelajaran.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("matapelajaran", kurikulumPunyaMatapelajaran.getMatapelajaran()))
								.addOrder(Order.asc("nama")), SubMatapelajaran.class);

						System.out.println("guru -> " + a.getGuru());
						final JadwalPelajaran jadwalPelajaran = a;
						final MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						row.setValign("top");
						row.setAttribute("kurikulumPunyaMatapelajaran", kurikulumPunyaMatapelajaran);
						row.setValign("top");
						row.setAttribute("jadwalPelajaran", jadwalPelajaran);

						row.appendChild(new MyLabelKecilBold(kurikulumPunyaMatapelajaran.getMatapelajaran().getNama()));

						final AmbilDataGuruBanbox guru;
						guru = new AmbilDataGuruBanbox();
						guru.setAttribute("guru", jadwalPelajaran.getGuru());
						guru.setAttribute("myValue", jadwalPelajaran.getGuru());
						guru.setValue(jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNamaGuru());
						guru.setWidth("90%");
						guru.setReadonly(true);

						final AmbilDataGuruBanbox guru2;
						guru2 = new AmbilDataGuruBanbox();
						guru2.setAttribute("guru", jadwalPelajaran.getGuru2());
						guru2.setAttribute("myValue", jadwalPelajaran.getGuru2());
						guru2.setValue(
								jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getNamaGuru());
						guru2.setWidth("90%");
						guru2.setReadonly(true);

						final AmbilDataGuruBanbox guru3;
						guru3 = new AmbilDataGuruBanbox();
						guru3.setAttribute("guru", jadwalPelajaran.getGuru3());
						guru3.setAttribute("myValue", jadwalPelajaran.getGuru3());
						guru3.setValue(
								jadwalPelajaran.getGuru3() == null ? "" : jadwalPelajaran.getGuru3().getNamaGuru());
						guru3.setWidth("90%");
						guru3.setReadonly(true);

						final AmbilDataGuruBanbox guru4;
						guru4 = new AmbilDataGuruBanbox();
						guru4.setAttribute("guru", jadwalPelajaran.getGuru4());
						guru4.setAttribute("myValue", jadwalPelajaran.getGuru4());
						guru4.setValue(
								jadwalPelajaran.getGuru4() == null ? "" : jadwalPelajaran.getGuru4().getNamaGuru());
						guru4.setWidth("90%");
						guru4.setReadonly(true);

						final AmbilDataGuruBanbox guru5;
						guru5 = new AmbilDataGuruBanbox();
						guru5.setAttribute("guru", jadwalPelajaran.getGuru5());
						guru5.setAttribute("myValue", jadwalPelajaran.getGuru5());
						guru5.setValue(
								jadwalPelajaran.getGuru5() == null ? "" : jadwalPelajaran.getGuru5().getNamaGuru());
						guru5.setWidth("90%");
						guru5.setReadonly(true);

						final AmbilDataGuruBanbox guru6;
						guru6 = new AmbilDataGuruBanbox();
						guru6.setAttribute("guru", jadwalPelajaran.getGuru6());
						guru6.setAttribute("myValue", jadwalPelajaran.getGuru6());
						guru6.setValue(
								jadwalPelajaran.getGuru6() == null ? "" : jadwalPelajaran.getGuru6().getNamaGuru());
						guru6.setWidth("90%");
						guru6.setReadonly(true);

						final AmbilDataGuruBanbox guru7;
						guru7 = new AmbilDataGuruBanbox();
						guru7.setAttribute("guru", jadwalPelajaran.getGuru7());
						guru7.setAttribute("myValue", jadwalPelajaran.getGuru7());
						guru7.setValue(
								jadwalPelajaran.getGuru7() == null ? "" : jadwalPelajaran.getGuru7().getNamaGuru());
						guru7.setWidth("90%");
						guru7.setReadonly(true);

						final AmbilDataGuruBanbox guru8;
						guru8 = new AmbilDataGuruBanbox();
						guru8.setAttribute("guru", jadwalPelajaran.getGuru8());
						guru8.setAttribute("myValue", jadwalPelajaran.getGuru8());
						guru8.setValue(
								jadwalPelajaran.getGuru8() == null ? "" : jadwalPelajaran.getGuru8().getNamaGuru());
						guru8.setWidth("90%");
						guru8.setReadonly(true);

						final AmbilDataGuruBanbox guru9;
						guru9 = new AmbilDataGuruBanbox();
						guru9.setAttribute("guru", jadwalPelajaran.getGuru9());
						guru9.setAttribute("myValue", jadwalPelajaran.getGuru9());
						guru9.setValue(
								jadwalPelajaran.getGuru9() == null ? "" : jadwalPelajaran.getGuru9().getNamaGuru());
						guru9.setWidth("90%");
						guru9.setReadonly(true);

						final AmbilDataGuruBanbox guru10;
						guru10 = new AmbilDataGuruBanbox();
						guru10.setAttribute("guru", jadwalPelajaran.getGuru10());
						guru10.setAttribute("myValue", jadwalPelajaran.getGuru10());
						guru10.setValue(
								jadwalPelajaran.getGuru10() == null ? "" : jadwalPelajaran.getGuru10().getNamaGuru());
						guru10.setWidth("90%");
						guru10.setReadonly(true);

						final AmbilDataGuruBanbox guru11;
						guru11 = new AmbilDataGuruBanbox();
						guru11.setAttribute("guru", jadwalPelajaran.getGuru11());
						guru11.setAttribute("myValue", jadwalPelajaran.getGuru11());
						guru11.setValue(
								jadwalPelajaran.getGuru11() == null ? "" : jadwalPelajaran.getGuru11().getNamaGuru());
						guru11.setWidth("90%");
						guru11.setReadonly(true);

						final AmbilDataGuruBanbox guru12;
						guru12 = new AmbilDataGuruBanbox();
						guru12.setAttribute("guru", jadwalPelajaran.getGuru12());
						guru12.setAttribute("myValue", jadwalPelajaran.getGuru12());
						guru12.setValue(
								jadwalPelajaran.getGuru12() == null ? "" : jadwalPelajaran.getGuru12().getNamaGuru());
						guru12.setWidth("90%");
						guru12.setReadonly(true);

						guru.setCols(4);
						guru2.setCols(4);
						guru3.setCols(4);
						guru4.setCols(4);
						guru5.setCols(4);
						guru6.setCols(4);
						guru7.setCols(4);
						guru8.setCols(4);
						guru9.setCols(4);
						guru10.setCols(4);
						guru11.setCols(4);
						guru12.setCols(4);

						final Combobox hari = new Combobox();
						hari.setCols(4);
						hari.setReadonly(true);
						final Combobox hari2 = new Combobox();
						hari2.setCols(4);
						hari2.setReadonly(true);
						final Combobox hari3 = new Combobox();
						hari3.setCols(4);
						hari3.setReadonly(true);
						final Combobox hari4 = new Combobox();
						hari4.setCols(4);
						hari4.setReadonly(true);
						final Combobox hari5 = new Combobox();
						hari5.setCols(4);
						hari5.setReadonly(true);

						final Combobox hari6 = new Combobox();
						hari6.setCols(4);
						hari6.setReadonly(true);

						final Combobox hari7 = new Combobox();
						hari7.setCols(4);
						hari7.setReadonly(true);

						final Combobox hari8 = new Combobox();
						hari8.setCols(4);
						hari8.setReadonly(true);

						final Combobox hari9 = new Combobox();
						hari9.setCols(4);
						hari9.setReadonly(true);

						final Combobox hari10 = new Combobox();
						hari10.setCols(4);
						hari10.setReadonly(true);

						final Combobox hari11 = new Combobox();
						hari11.setCols(4);
						hari11.setReadonly(true);

						final Combobox hari12 = new Combobox();
						hari12.setCols(4);
						hari12.setReadonly(true);

						for (String h : Common.haris) {
							MyComboitemConfig comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari2.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari3.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari4.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari5.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari6.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari7.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari8.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari9.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari10.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari11.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(h);
							comboitem.setValue(h);
							hari12.appendChild(comboitem);
						}

						MyComboitemConfig comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari2.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari3.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari4.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari5.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari6.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari7.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari8.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari9.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari10.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari11.appendChild(comboitem);

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						hari12.appendChild(comboitem);

						Common.selectComboItem(true, hari, jadwalPelajaran.getHari());
						Common.selectComboItem(true, hari2, jadwalPelajaran.getHari2());
						Common.selectComboItem(true, hari3, jadwalPelajaran.getHari3());
						Common.selectComboItem(true, hari4, jadwalPelajaran.getHari4());
						Common.selectComboItem(true, hari5, jadwalPelajaran.getHari5());
						Common.selectComboItem(true, hari6, jadwalPelajaran.getHari6());

						Common.selectComboItem(true, hari7, jadwalPelajaran.getHari7());

						Common.selectComboItem(true, hari8, jadwalPelajaran.getHari8());

						Common.selectComboItem(true, hari9, jadwalPelajaran.getHari9());

						Common.selectComboItem(true, hari10, jadwalPelajaran.getHari10());

						Common.selectComboItem(true, hari11, jadwalPelajaran.getHari11());

						Common.selectComboItem(true, hari12, jadwalPelajaran.getHari12());

						final Combobox jamPelajaran = new Combobox();
						jamPelajaran.setCols(4);
						jamPelajaran.setReadonly(true);

						final Combobox jamPelajaran2 = new Combobox();
						jamPelajaran2.setCols(4);
						jamPelajaran2.setReadonly(true);

						final Combobox jamPelajaran3 = new Combobox();
						jamPelajaran3.setCols(4);
						jamPelajaran3.setReadonly(true);

						final Combobox jamPelajaran4 = new Combobox();
						jamPelajaran4.setCols(4);
						jamPelajaran4.setReadonly(true);

						final Combobox jamPelajaran5 = new Combobox();
						jamPelajaran5.setCols(4);
						jamPelajaran5.setReadonly(true);

						final Combobox jamPelajaran6 = new Combobox();
						jamPelajaran6.setCols(4);
						jamPelajaran6.setReadonly(true);

						final Combobox jamPelajaran7 = new Combobox();
						jamPelajaran7.setCols(4);
						jamPelajaran7.setReadonly(true);

						final Combobox jamPelajaran8 = new Combobox();
						jamPelajaran8.setCols(4);
						jamPelajaran8.setReadonly(true);

						final Combobox jamPelajaran9 = new Combobox();
						jamPelajaran9.setCols(4);
						jamPelajaran9.setReadonly(true);

						final Combobox jamPelajaran10 = new Combobox();
						jamPelajaran10.setCols(4);
						jamPelajaran10.setReadonly(true);

						final Combobox jamPelajaran11 = new Combobox();
						jamPelajaran11.setCols(4);
						jamPelajaran11.setReadonly(true);

						final Combobox jamPelajaran12 = new Combobox();
						jamPelajaran12.setCols(4);
						jamPelajaran12.setReadonly(true);

						final Combobox subMatapelajaran = new Combobox();
						final Combobox subMatapelajaran2 = new Combobox();
						final Combobox subMatapelajaran3 = new Combobox();
						final Combobox subMatapelajaran4 = new Combobox();
						final Combobox subMatapelajaran5 = new Combobox();
						final Combobox subMatapelajaran6 = new Combobox();
						final Combobox subMatapelajaran7 = new Combobox();
						final Combobox subMatapelajaran8 = new Combobox();
						final Combobox subMatapelajaran9 = new Combobox();
						final Combobox subMatapelajaran10 = new Combobox();
						final Combobox subMatapelajaran11 = new Combobox();
						final Combobox subMatapelajaran12 = new Combobox();

						subMatapelajaran.setCols(4);
						subMatapelajaran.setReadonly(true);

						subMatapelajaran2.setCols(4);
						subMatapelajaran2.setReadonly(true);

						subMatapelajaran3.setCols(4);
						subMatapelajaran3.setReadonly(true);

						subMatapelajaran4.setCols(4);
						subMatapelajaran4.setReadonly(true);

						subMatapelajaran5.setCols(4);
						subMatapelajaran5.setReadonly(true);

						subMatapelajaran6.setCols(4);
						subMatapelajaran6.setReadonly(true);

						subMatapelajaran7.setCols(4);
						subMatapelajaran7.setReadonly(true);

						subMatapelajaran8.setCols(4);
						subMatapelajaran8.setReadonly(true);

						subMatapelajaran9.setCols(4);
						subMatapelajaran9.setReadonly(true);

						subMatapelajaran10.setCols(4);
						subMatapelajaran10.setReadonly(true);

						subMatapelajaran11.setCols(4);
						subMatapelajaran11.setReadonly(true);

						subMatapelajaran12.setCols(4);
						subMatapelajaran12.setReadonly(true);

						if (subMatapelajarans.isEmpty()) {
							subMatapelajaran.setVisible(false);
							subMatapelajaran2.setVisible(false);
							subMatapelajaran3.setVisible(false);
							subMatapelajaran4.setVisible(false);
							subMatapelajaran5.setVisible(false);
							subMatapelajaran6.setVisible(false);
							subMatapelajaran7.setVisible(false);
							subMatapelajaran8.setVisible(false);
							subMatapelajaran9.setVisible(false);
							subMatapelajaran10.setVisible(false);
							subMatapelajaran11.setVisible(false);
							subMatapelajaran12.setVisible(false);
						} else {
							Common.insertComboItems(subMatapelajaran, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran2, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran3, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran4, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran5, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran6, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran7, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran8, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran9, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran10, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran11, "nama", subMatapelajarans);
							Common.insertComboItems(subMatapelajaran12, "nama", subMatapelajarans);
						}

						Sekolah s = kelasSiswa.getSekolah();

						final Combobox masaJadwalPelajaran = new Combobox();
						masaJadwalPelajaran.setWidth("90%");

						String ta = (String) tahunAjaran.getSelectedItem().getValue();
						Integer smt = (Integer) semester.getSelectedItem().getValue();

						Common.insertCombo(masaJadwalPelajaran, new String[] { "nama", "tahunAjaran", "namaSmt" },
								MasaJadwalPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("semester"),
												Restrictions.eq("semester", smt)),
										Restrictions.and(Restrictions.eq("tahunAjaran", ta),
												Restrictions.and(
														Restrictions.or(Restrictions.isNull("sekolah"),
																Restrictions.eq("sekolah", s)),
														Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true))))));
						Common.selectComboItem(masaJadwalPelajaran, jadwalPelajaran.getMasaJadwalPelajaran());
						masaJadwalPelajaran.setReadonly(true);

						Common.insertCombo(jamPelajaran,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran, jadwalPelajaran.getJamPelajaran());

						Common.insertCombo(jamPelajaran2,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran2.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran2, jadwalPelajaran.getJamPelajaran2());

						Common.insertCombo(jamPelajaran3,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran3.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran3, jadwalPelajaran.getJamPelajaran3());

						Common.insertCombo(jamPelajaran4,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran4.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran4, jadwalPelajaran.getJamPelajaran4());

						Common.insertCombo(jamPelajaran5,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran5.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran5, jadwalPelajaran.getJamPelajaran5());

						Common.insertCombo(jamPelajaran6,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran6.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran6, jadwalPelajaran.getJamPelajaran6());

						Common.insertCombo(jamPelajaran7,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran7.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran7, jadwalPelajaran.getJamPelajaran7());

						Common.insertCombo(jamPelajaran8,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran8.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran8, jadwalPelajaran.getJamPelajaran8());

						Common.insertCombo(jamPelajaran9,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran9.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran9, jadwalPelajaran.getJamPelajaran9());

						Common.insertCombo(jamPelajaran10,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran10.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran10, jadwalPelajaran.getJamPelajaran10());

						Common.insertCombo(jamPelajaran11,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran11.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran11, jadwalPelajaran.getJamPelajaran11());

						Common.insertCombo(jamPelajaran12,
								new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
								JamPelajaran.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						comboitem = new MyComboitemConfig();
						comboitem.setLabel("Tidak ada jadwal");
						comboitem.setValue(null);
						jamPelajaran12.appendChild(comboitem);

						Common.selectComboItem(jamPelajaran12, jadwalPelajaran.getJamPelajaran12());

						final Textbox keterangan = new Textbox(jadwalPelajaran.getKeterangan());
						keterangan.setRows(2);
						keterangan.setWidth("90%");

						EventListener ubah = new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								KelasSiswa kelasSiswa = (KelasSiswa) (kelas.getSelectedItem() == null ? null
										: kelas.getSelectedItem().getValue());

								jadwalPelajaran.setMatapelajaran(kurikulumPunyaMatapelajaran.getMatapelajaran());
								jadwalPelajaran.setKurikulumPunyaMatapelajaran(kurikulumPunyaMatapelajaran);
								jadwalPelajaran.setKelas(kelasSiswa);
								jadwalPelajaran.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
								jadwalPelajaran.setSemester((Integer) semester.getSelectedItem().getValue());

								jadwalPelajaran.setKeterangan(keterangan.getValue().trim());

								jadwalPelajaran.setGuru((Guru) guru.getAttribute("guru"));
								jadwalPelajaran.setGuru2((Guru) guru2.getAttribute("guru"));
								jadwalPelajaran.setGuru3((Guru) guru3.getAttribute("guru"));
								jadwalPelajaran.setGuru4((Guru) guru4.getAttribute("guru"));
								jadwalPelajaran.setGuru5((Guru) guru5.getAttribute("guru"));
								jadwalPelajaran.setGuru6((Guru) guru6.getAttribute("guru"));

								jadwalPelajaran.setGuru7((Guru) guru7.getAttribute("guru"));
								jadwalPelajaran.setGuru8((Guru) guru8.getAttribute("guru"));
								jadwalPelajaran.setGuru9((Guru) guru9.getAttribute("guru"));
								jadwalPelajaran.setGuru10((Guru) guru10.getAttribute("guru"));

								jadwalPelajaran.setGuru11((Guru) guru11.getAttribute("guru"));

								jadwalPelajaran.setGuru12((Guru) guru12.getAttribute("guru"));

								jadwalPelajaran.setHari((String) (hari.getSelectedItem() == null ? null
										: hari.getSelectedItem().getValue()));
								jadwalPelajaran.setHari2((String) (hari2.getSelectedItem() == null ? null
										: hari2.getSelectedItem().getValue()));
								jadwalPelajaran.setHari3((String) (hari3.getSelectedItem() == null ? null
										: hari3.getSelectedItem().getValue()));
								jadwalPelajaran.setHari4((String) (hari4.getSelectedItem() == null ? null
										: hari4.getSelectedItem().getValue()));
								jadwalPelajaran.setHari5((String) (hari5.getSelectedItem() == null ? null
										: hari5.getSelectedItem().getValue()));

								jadwalPelajaran.setHari6((String) (hari6.getSelectedItem() == null ? null
										: hari6.getSelectedItem().getValue()));

								jadwalPelajaran.setHari7((String) (hari7.getSelectedItem() == null ? null
										: hari7.getSelectedItem().getValue()));
								jadwalPelajaran.setHari8((String) (hari8.getSelectedItem() == null ? null
										: hari8.getSelectedItem().getValue()));
								jadwalPelajaran.setHari9((String) (hari9.getSelectedItem() == null ? null
										: hari9.getSelectedItem().getValue()));
								jadwalPelajaran.setHari10((String) (hari10.getSelectedItem() == null ? null
										: hari10.getSelectedItem().getValue()));

								jadwalPelajaran.setHari11((String) (hari11.getSelectedItem() == null ? null
										: hari11.getSelectedItem().getValue()));

								jadwalPelajaran.setHari12((String) (hari12.getSelectedItem() == null ? null
										: hari12.getSelectedItem().getValue()));

								jadwalPelajaran
										.setJamPelajaran((JamPelajaran) (jamPelajaran.getSelectedItem() == null ? null
												: jamPelajaran.getSelectedItem().getValue()));
								jadwalPelajaran
										.setJamPelajaran2((JamPelajaran) (jamPelajaran2.getSelectedItem() == null ? null
												: jamPelajaran2.getSelectedItem().getValue()));

								jadwalPelajaran
										.setJamPelajaran3((JamPelajaran) (jamPelajaran3.getSelectedItem() == null ? null
												: jamPelajaran3.getSelectedItem().getValue()));

								jadwalPelajaran
										.setJamPelajaran4((JamPelajaran) (jamPelajaran4.getSelectedItem() == null ? null
												: jamPelajaran4.getSelectedItem().getValue()));

								jadwalPelajaran
										.setJamPelajaran5((JamPelajaran) (jamPelajaran5.getSelectedItem() == null ? null
												: jamPelajaran5.getSelectedItem().getValue()));

								jadwalPelajaran
										.setJamPelajaran6((JamPelajaran) (jamPelajaran6.getSelectedItem() == null ? null
												: jamPelajaran6.getSelectedItem().getValue()));

								jadwalPelajaran
										.setJamPelajaran7((JamPelajaran) (jamPelajaran7.getSelectedItem() == null ? null
												: jamPelajaran7.getSelectedItem().getValue()));
								jadwalPelajaran
										.setJamPelajaran8((JamPelajaran) (jamPelajaran8.getSelectedItem() == null ? null
												: jamPelajaran8.getSelectedItem().getValue()));
								jadwalPelajaran
										.setJamPelajaran9((JamPelajaran) (jamPelajaran9.getSelectedItem() == null ? null
												: jamPelajaran9.getSelectedItem().getValue()));
								jadwalPelajaran.setJamPelajaran10(
										(JamPelajaran) (jamPelajaran10.getSelectedItem() == null ? null
												: jamPelajaran10.getSelectedItem().getValue()));

								jadwalPelajaran.setJamPelajaran11(
										(JamPelajaran) (jamPelajaran11.getSelectedItem() == null ? null
												: jamPelajaran11.getSelectedItem().getValue()));

								jadwalPelajaran.setJamPelajaran12(
										(JamPelajaran) (jamPelajaran12.getSelectedItem() == null ? null
												: jamPelajaran12.getSelectedItem().getValue()));

								jadwalPelajaran.setMasaJadwalPelajaran(
										(MasaJadwalPelajaran) (masaJadwalPelajaran.getSelectedItem() == null ? null
												: masaJadwalPelajaran.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran(
										(SubMatapelajaran) (subMatapelajaran.getSelectedItem() == null ? null
												: subMatapelajaran.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran2(
										(SubMatapelajaran) (subMatapelajaran2.getSelectedItem() == null ? null
												: subMatapelajaran2.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran3(
										(SubMatapelajaran) (subMatapelajaran3.getSelectedItem() == null ? null
												: subMatapelajaran3.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran4(
										(SubMatapelajaran) (subMatapelajaran4.getSelectedItem() == null ? null
												: subMatapelajaran4.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran5(
										(SubMatapelajaran) (subMatapelajaran5.getSelectedItem() == null ? null
												: subMatapelajaran5.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran6(
										(SubMatapelajaran) (subMatapelajaran6.getSelectedItem() == null ? null
												: subMatapelajaran6.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran7(
										(SubMatapelajaran) (subMatapelajaran7.getSelectedItem() == null ? null
												: subMatapelajaran7.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran8(
										(SubMatapelajaran) (subMatapelajaran8.getSelectedItem() == null ? null
												: subMatapelajaran8.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran9(
										(SubMatapelajaran) (subMatapelajaran9.getSelectedItem() == null ? null
												: subMatapelajaran9.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran10(
										(SubMatapelajaran) (subMatapelajaran10.getSelectedItem() == null ? null
												: subMatapelajaran10.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran11(
										(SubMatapelajaran) (subMatapelajaran11.getSelectedItem() == null ? null
												: subMatapelajaran11.getSelectedItem().getValue()));

								jadwalPelajaran.setSubMatapelajaran12(
										(SubMatapelajaran) (subMatapelajaran12.getSelectedItem() == null ? null
												: subMatapelajaran12.getSelectedItem().getValue()));

								if (!bentrok(jadwalPelajaran.getTahunAjaran(),
										jadwalPelajaran.getSemester().equals(1) ? Perkuliahan.GANJIL
												: Perkuliahan.GENAP,
										jadwalPelajaran)) {
									return;
								}

								row.setValign("top");
								row.setAttribute("jadwalPelajaran", jadwalPelajaran);

								Common.refreshSaveOrUpdate(jadwalPelajaran);
							}
						};

						guru.setEventListener(ubah);
						guru2.setEventListener(ubah);
						guru3.setEventListener(ubah);
						guru4.setEventListener(ubah);
						guru5.setEventListener(ubah);
						guru6.setEventListener(ubah);

						guru7.setEventListener(ubah);
						guru8.setEventListener(ubah);
						guru9.setEventListener(ubah);
						guru10.setEventListener(ubah);

						guru11.setEventListener(ubah);
						guru12.setEventListener(ubah);

						hari.addEventListener("onChange", ubah);
						jamPelajaran.addEventListener("onChange", ubah);

						hari2.addEventListener("onChange", ubah);
						jamPelajaran2.addEventListener("onChange", ubah);

						hari3.addEventListener("onChange", ubah);
						jamPelajaran3.addEventListener("onChange", ubah);

						hari4.addEventListener("onChange", ubah);
						jamPelajaran4.addEventListener("onChange", ubah);

						hari5.addEventListener("onChange", ubah);
						jamPelajaran5.addEventListener("onChange", ubah);

						hari6.addEventListener("onChange", ubah);
						jamPelajaran6.addEventListener("onChange", ubah);

						hari7.addEventListener("onChange", ubah);
						jamPelajaran7.addEventListener("onChange", ubah);

						hari8.addEventListener("onChange", ubah);
						jamPelajaran8.addEventListener("onChange", ubah);

						hari9.addEventListener("onChange", ubah);
						jamPelajaran9.addEventListener("onChange", ubah);

						hari10.addEventListener("onChange", ubah);
						jamPelajaran10.addEventListener("onChange", ubah);

						hari11.addEventListener("onChange", ubah);
						jamPelajaran11.addEventListener("onChange", ubah);

						hari12.addEventListener("onChange", ubah);
						jamPelajaran12.addEventListener("onChange", ubah);

						keterangan.addEventListener("onChange", ubah);

						masaJadwalPelajaran.addEventListener("onChange", ubah);

						Vbox vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari);
						vbox.appendChild(jamPelajaran);

						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran);
						}

						vbox.appendChild(guru);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari2);
						vbox.appendChild(jamPelajaran2);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran2);
						}
						vbox.appendChild(guru2);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari3);
						vbox.appendChild(jamPelajaran3);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran3);
						}
						vbox.appendChild(guru3);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari4);
						vbox.appendChild(jamPelajaran4);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran4);
						}
						vbox.appendChild(guru4);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari5);
						vbox.appendChild(jamPelajaran5);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran5);
						}
						vbox.appendChild(guru5);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari6);
						vbox.appendChild(jamPelajaran6);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran6);
						}
						vbox.appendChild(guru6);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari7);
						vbox.appendChild(jamPelajaran7);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran7);
						}
						vbox.appendChild(guru7);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari8);
						vbox.appendChild(jamPelajaran8);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran8);
						}
						vbox.appendChild(guru8);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari9);
						vbox.appendChild(jamPelajaran9);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran9);
						}
						vbox.appendChild(guru9);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari10);
						vbox.appendChild(jamPelajaran10);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran10);
						}
						vbox.appendChild(guru10);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari11);
						vbox.appendChild(jamPelajaran11);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran11);
						}
						vbox.appendChild(guru11);

						vbox = new Vbox();
						row.appendChild(vbox);
						vbox.appendChild(hari12);
						vbox.appendChild(jamPelajaran12);
						if (!subMatapelajarans.isEmpty()) {
							vbox.appendChild(subMatapelajaran12);
						}
						vbox.appendChild(guru12);

						row.appendChild(masaJadwalPelajaran);

						if (jadwalPelajaran.getId() != null) {
							int jml = ((Number) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();

//							if (jml > 0) {
//								Common.freeze(row, true);
//							}

							row.appendChild(new Label(Common.numberFormat.get().format(jml)));
						} else {
							row.appendChild(new Label());
						}

						row.appendChild(keterangan);

					}
				}
			}
		};

		kelas.addEventListener("onChange", eventListener1);
		eventListener1.onEvent(null);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);
				Common.insertCombo(kelas, new String[] { "nama", "tahunAjaran", "ruang" }, "keterangan",
						KelasSiswa.class,
						Restrictions.and(Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()),
								(Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true))))));
				Common.selectComboItem(true, kelas, jadwalPelajaran.getKelas());

				if (jadwalPelajaran != null && jadwalPelajaran.getId() != null) {
					kelas.setDisabled(true);
					sekolah.setDisabled(true);
					yayasan.setDisabled(true);
					tahunAjaran.setDisabled(true);
					semester.setDisabled(true);
				}

				Common.createDefaultTimer(eventListener1);
			}
		};

		sekolah.addEventListener("onChange", eventListener);
		tahunAjaran.addEventListener("onChange", eventListener);
		Common.createDefaultTimer(eventListener);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (gridBerdasarkanKurikulum != null && gridBerdasarkanKurikulum.getRows() != null) {

			List<Row> rows = gridBerdasarkanKurikulum.getRows().getChildren();
			for (Row row : rows) {
				JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) row.getAttribute("jadwalPelajaran");
				if (jadwalPelajaran != null) {
					Common.refreshSaveOrUpdate(jadwalPelajaran);
				}
			}

		} else {

			if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Yayasan belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Yayasan; (2) pilih yayasan yang sesuai; (3) ulangi proses setelah yayasan dipilih.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
			if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Sekolah belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Sekolah; (2) pilih sekolah yang sesuai; (3) ulangi proses setelah sekolah dipilih.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
			if (masaJadwalPelajaran.getSelectedItem() == null) {
				MyMessageboxConfig.show("Masa Pelajaran belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Masa Pelajaran; (2) pilih masa pelajaran yang sesuai; (3) ulangi proses setelah masa pelajaran dipilih.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (matapelajaran.getSelectedItem() == null && kelasLesSiswa.getAttribute("kelasLesSiswa") == null) {
				MyMessageboxConfig.show("Matapelajaran belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Matapelajaran; (2) pilih matapelajaran yang sesuai; (3) ulangi proses setelah matapelajaran dipilih.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (kelas.getSelectedItem() == null && kelasLesSiswa.getAttribute("kelasLesSiswa") == null) {
				MyMessageboxConfig.show("Kelas belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Kelas; (2) pilih kelas yang sesuai; (3) ulangi proses setelah kelas dipilih.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			KelasLesSiswa kelasLesSiswa = (KelasLesSiswa) this.kelasLesSiswa.getAttribute("kelasLesSiswa");
			KelasSiswa kelasSiswa = (KelasSiswa) (kelas.getSelectedItem() == null ? null
					: kelas.getSelectedItem().getValue());
			Matapelajaran mt = (Matapelajaran) (kelasLesSiswa != null ? kelasLesSiswa.getMatapelajaran()
					: matapelajaran.getSelectedItem().getValue());

			Session session = HibernateUtil.currentSession();
			if (jadwalPelajaran.getId() != null) {
				jadwalPelajaran = (JadwalPelajaran) session.load(JadwalPelajaran.class, jadwalPelajaran.getId());

			} else {

				JadwalPelajaran a = (JadwalPelajaran) ConstantValues.simpleObject(

						kelasLesSiswa != null
								? session.createCriteria(JadwalPelajaran.class)
										.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).setMaxResults(1)
								: session.createCriteria(JadwalPelajaran.class)
										.add(Restrictions.eq("sekolah", kelasSiswa.getSekolah()))
										.add(Restrictions.eq("matapelajaran", mt))
										.add(Restrictions.eq("kelas", kelasSiswa))
										.add(Restrictions.eq("semester", semester.getSelectedItem().getValue()))
										.add(Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
										.setMaxResults(1),
						JadwalPelajaran.class);
				if (a != null) {

					if (kelasLesSiswa != null) {
						MyMessageboxConfig.showFormat(
								"Jadwal kelas les \"{V1}\" sudah ada. Langkah yang dapat dilakukan: (1) periksa kembali data jadwal yang telah tersimpan; (2) ubah kelas les, waktu, atau semester agar tidak sama dengan jadwal yang sudah ada; (3) simpan kembali setelah data tidak duplikat.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, kelasLesSiswa.getNama());
					} else {

						MyMessageboxConfig.showFormat(
								"Jadwal matapelajaran \"{V1}\" kelas \"{V2}\" semester \"{V3}\" tahun ajaran \"{V4}\" sudah ada. Langkah yang dapat dilakukan: (1) periksa kembali data jadwal yang telah tersimpan; (2) ubah matapelajaran, kelas, semester, atau tahun ajaran agar tidak sama dengan jadwal yang sudah ada; (3) simpan kembali setelah data tidak duplikat.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mt.getNama(),
								kelasSiswa.getNama(), semester.getSelectedItem().getValue(),
								tahunAjaran.getSelectedItem().getValue());
					}
					return false;
				}

			}
			jadwalPelajaran
					.setMasaJadwalPelajaran((MasaJadwalPelajaran) masaJadwalPelajaran.getSelectedItem().getValue());
			jadwalPelajaran.setSekolah(
					(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));
			jadwalPelajaran.setYayasan(
					(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
			jadwalPelajaran.setMatapelajaran(mt);

			jadwalPelajaran.setKelas(kelasSiswa);
			jadwalPelajaran.setKelasLesSiswa(kelasLesSiswa);
			jadwalPelajaran.setKeterangan(keterangan.getValue());
			jadwalPelajaran.setTahunAjaran(
					(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()));
			jadwalPelajaran.setSemester(
					(Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));
			jadwalPelajaran.setJamPelajaran((JamPelajaran) (jamPelajaran.getSelectedItem() == null ? null
					: jamPelajaran.getSelectedItem().getValue()));
			jadwalPelajaran
					.setHari((String) (hari.getSelectedItem() == null ? null : hari.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran2((JamPelajaran) (jamPelajaran2.getSelectedItem() == null ? null
					: jamPelajaran2.getSelectedItem().getValue()));
			jadwalPelajaran
					.setHari2((String) (hari2.getSelectedItem() == null ? null : hari2.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran3((JamPelajaran) (jamPelajaran3.getSelectedItem() == null ? null
					: jamPelajaran3.getSelectedItem().getValue()));
			jadwalPelajaran
					.setHari3((String) (hari3.getSelectedItem() == null ? null : hari3.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran4((JamPelajaran) (jamPelajaran4.getSelectedItem() == null ? null
					: jamPelajaran4.getSelectedItem().getValue()));
			jadwalPelajaran
					.setHari4((String) (hari4.getSelectedItem() == null ? null : hari4.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran5((JamPelajaran) (jamPelajaran5.getSelectedItem() == null ? null
					: jamPelajaran5.getSelectedItem().getValue()));
			jadwalPelajaran
					.setHari5((String) (hari5.getSelectedItem() == null ? null : hari5.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran6((JamPelajaran) (jamPelajaran6.getSelectedItem() == null ? null
					: jamPelajaran6.getSelectedItem().getValue()));
			jadwalPelajaran
					.setHari6((String) (hari6.getSelectedItem() == null ? null : hari6.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran7((JamPelajaran) (jamPelajaran7.getSelectedItem() == null ? null
					: jamPelajaran7.getSelectedItem().getValue()));
			jadwalPelajaran
					.setHari7((String) (hari7.getSelectedItem() == null ? null : hari7.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran8((JamPelajaran) (jamPelajaran8.getSelectedItem() == null ? null
					: jamPelajaran8.getSelectedItem().getValue()));
			jadwalPelajaran
					.setHari8((String) (hari8.getSelectedItem() == null ? null : hari8.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran9((JamPelajaran) (jamPelajaran9.getSelectedItem() == null ? null
					: jamPelajaran9.getSelectedItem().getValue()));
			jadwalPelajaran
					.setHari9((String) (hari9.getSelectedItem() == null ? null : hari9.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran10((JamPelajaran) (jamPelajaran10.getSelectedItem() == null ? null
					: jamPelajaran10.getSelectedItem().getValue()));
			jadwalPelajaran.setHari10(
					(String) (hari10.getSelectedItem() == null ? null : hari10.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran11((JamPelajaran) (jamPelajaran11.getSelectedItem() == null ? null
					: jamPelajaran11.getSelectedItem().getValue()));
			jadwalPelajaran.setHari11(
					(String) (hari11.getSelectedItem() == null ? null : hari11.getSelectedItem().getValue()));

			jadwalPelajaran.setJamPelajaran12((JamPelajaran) (jamPelajaran12.getSelectedItem() == null ? null
					: jamPelajaran12.getSelectedItem().getValue()));
			jadwalPelajaran.setHari12(
					(String) (hari12.getSelectedItem() == null ? null : hari12.getSelectedItem().getValue()));

			if (jadwalPelajaran.getHari() != null)
				jadwalPelajaran.setGuru((Guru) guru.getAttribute("guru"));

			if (jadwalPelajaran.getHari2() != null)
				jadwalPelajaran.setGuru2((Guru) guru2.getAttribute("guru"));
			if (jadwalPelajaran.getHari3() != null)
				jadwalPelajaran.setGuru3((Guru) guru3.getAttribute("guru"));

			if (jadwalPelajaran.getHari4() != null)
				jadwalPelajaran.setGuru4((Guru) guru4.getAttribute("guru"));

			if (jadwalPelajaran.getHari5() != null)
				jadwalPelajaran.setGuru5((Guru) guru5.getAttribute("guru"));

			if (jadwalPelajaran.getHari6() != null)
				jadwalPelajaran.setGuru6((Guru) guru6.getAttribute("guru"));

			if (jadwalPelajaran.getHari7() != null)
				jadwalPelajaran.setGuru7((Guru) guru7.getAttribute("guru"));

			if (jadwalPelajaran.getHari8() != null)
				jadwalPelajaran.setGuru8((Guru) guru8.getAttribute("guru"));

			if (jadwalPelajaran.getHari9() != null)
				jadwalPelajaran.setGuru9((Guru) guru9.getAttribute("guru"));

			if (jadwalPelajaran.getHari10() != null)
				jadwalPelajaran.setGuru10((Guru) guru10.getAttribute("guru"));
			if (jadwalPelajaran.getHari11() != null)
				jadwalPelajaran.setGuru11((Guru) guru11.getAttribute("guru"));
			if (jadwalPelajaran.getHari12() != null)
				jadwalPelajaran.setGuru12((Guru) guru12.getAttribute("guru"));

			if (tbmuser != null && tbmuser.ambilGuru() != null) {
				if (jadwalPelajaran.getGuru() == null) {
					jadwalPelajaran.setGuru(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru2() == null) {
					jadwalPelajaran.setGuru2(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru3() == null) {
					jadwalPelajaran.setGuru3(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru4() == null) {
					jadwalPelajaran.setGuru4(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru5() == null) {
					jadwalPelajaran.setGuru5(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru6() == null) {
					jadwalPelajaran.setGuru6(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru7() == null) {
					jadwalPelajaran.setGuru7(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru8() == null) {
					jadwalPelajaran.setGuru8(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru9() == null) {
					jadwalPelajaran.setGuru9(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru10() == null) {
					jadwalPelajaran.setGuru10(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru11() == null) {
					jadwalPelajaran.setGuru11(tbmuser.ambilGuru());
				}
				if (jadwalPelajaran.getGuru12() == null) {
					jadwalPelajaran.setGuru12(tbmuser.ambilGuru());
				}
			}

			jadwalPelajaran.setAbaikanBentrok(abaikanBentrok.isChecked());

			jadwalPelajaran.setSubMatapelajaran((SubMatapelajaran) (subMatapelajaran.getSelectedItem() == null ? null
					: subMatapelajaran.getSelectedItem().getValue()));

			jadwalPelajaran.setSubMatapelajaran2((SubMatapelajaran) (subMatapelajaran2.getSelectedItem() == null ? null
					: subMatapelajaran2.getSelectedItem().getValue()));

			jadwalPelajaran.setSubMatapelajaran3((SubMatapelajaran) (subMatapelajaran3.getSelectedItem() == null ? null
					: subMatapelajaran3.getSelectedItem().getValue()));

			jadwalPelajaran.setSubMatapelajaran4((SubMatapelajaran) (subMatapelajaran4.getSelectedItem() == null ? null
					: subMatapelajaran4.getSelectedItem().getValue()));

			jadwalPelajaran.setSubMatapelajaran5((SubMatapelajaran) (subMatapelajaran5.getSelectedItem() == null ? null
					: subMatapelajaran5.getSelectedItem().getValue()));

			jadwalPelajaran.setSubMatapelajaran6((SubMatapelajaran) (subMatapelajaran6.getSelectedItem() == null ? null
					: subMatapelajaran6.getSelectedItem().getValue()));

			jadwalPelajaran.setSubMatapelajaran7((SubMatapelajaran) (subMatapelajaran7.getSelectedItem() == null ? null
					: subMatapelajaran7.getSelectedItem().getValue()));

			jadwalPelajaran.setSubMatapelajaran8((SubMatapelajaran) (subMatapelajaran8.getSelectedItem() == null ? null
					: subMatapelajaran8.getSelectedItem().getValue()));

			jadwalPelajaran.setSubMatapelajaran9((SubMatapelajaran) (subMatapelajaran9.getSelectedItem() == null ? null
					: subMatapelajaran9.getSelectedItem().getValue()));

			jadwalPelajaran
					.setSubMatapelajaran10((SubMatapelajaran) (subMatapelajaran10.getSelectedItem() == null ? null
							: subMatapelajaran10.getSelectedItem().getValue()));

			jadwalPelajaran
					.setSubMatapelajaran11((SubMatapelajaran) (subMatapelajaran11.getSelectedItem() == null ? null
							: subMatapelajaran11.getSelectedItem().getValue()));

			jadwalPelajaran
					.setSubMatapelajaran12((SubMatapelajaran) (subMatapelajaran12.getSelectedItem() == null ? null
							: subMatapelajaran12.getSelectedItem().getValue()));

			if (!bentrok(jadwalPelajaran.getTahunAjaran(),
					jadwalPelajaran.getSemester().equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
					jadwalPelajaran)) {
				return false;
			}

			Common.refreshSaveOrUpdate(session, jadwalPelajaran);
			session.flush();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalPelajaran.class)

				.createAlias("matapelajaran", "matapelajaran");

		if (!searchnamaruang.getValue().trim().isEmpty()) {
			criteria.createAlias("ruang", "ruang")
					.add(Restrictions.ilike("ruang.nama", searchnamaruang.getValue().trim(), MatchMode.ANYWHERE));
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {

			List<Long> kelas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("kelasSiswa.id"))
					.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa())).list();

			if (!kelas.isEmpty()) {
				criteria.add(Restrictions.in("kelas.id", kelas));
			}

		}

		if (!searchsiswa.getValue().trim().isEmpty()) {
			List<Long> kelas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("kelasSiswa.id")).createAlias("siswa", "siswa")
					.add(Restrictions.or(
							Restrictions.ilike("siswa.nama", searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("siswa.nomorInduk", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE))))
					.list();

			if (!kelas.isEmpty()) {
				criteria.add(Restrictions.in("kelas.id", kelas));
			}
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria

				.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						:

						Restrictions.or(
								Restrictions
										.or(Restrictions.eq("hari", searchhari.getSelectedItem().getValue()),
												Restrictions
														.or(Restrictions.eq("hari5",
																searchhari.getSelectedItem().getValue()),
																Restrictions.or(
																		Restrictions.eq("hari4",
																				searchhari.getSelectedItem()
																						.getValue()),
																		Restrictions.or(
																				Restrictions.eq("hari3",
																						searchhari.getSelectedItem()
																								.getValue()),
																				Restrictions.eq("hari2",
																						searchhari.getSelectedItem()
																								.getValue()))))),

								Restrictions.or(Restrictions.eq("hari6", searchhari.getSelectedItem().getValue()),
										Restrictions.or(
												Restrictions.eq("hari7", searchhari.getSelectedItem().getValue()),
												Restrictions.or(
														Restrictions.eq("hari8",
																searchhari.getSelectedItem().getValue()),
														Restrictions.or(
																Restrictions.eq("hari9",
																		searchhari.getSelectedItem().getValue()),
																Restrictions.eq("hari10",
																		searchhari.getSelectedItem().getValue()))))))

				)

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("matapelajaran.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchguru == null || searchguru.getAttribute("guru") == null) ? Restrictions.sqlRestriction("1=1") :

						Restrictions.or(Restrictions.eq("guru12", searchguru.getAttribute("guru")), Restrictions.or(
								Restrictions.eq("guru11", searchguru.getAttribute("guru")),
								Restrictions.or(Restrictions.eq("guru10", searchguru.getAttribute("guru")), Restrictions
										.or(Restrictions.eq("guru9", searchguru.getAttribute("guru")), Restrictions.or(
												Restrictions.eq("guru8", searchguru.getAttribute("guru")),
												Restrictions.or(
														Restrictions.eq("guru7", searchguru.getAttribute("guru")),
														Restrictions.or(
																Restrictions.eq("guru6",
																		searchguru.getAttribute("guru")),
																Restrictions.or(
																		Restrictions.eq("guru5",
																				searchguru.getAttribute("guru")),
																		Restrictions.or(
																				Restrictions.eq("guru4",
																						searchguru
																								.getAttribute("guru")),
																				Restrictions.or(
																						Restrictions.eq("guru3",
																								searchguru.getAttribute(
																										"guru")),
																						Restrictions.or(Restrictions.eq(
																								"guru",
																								searchguru.getAttribute(
																										"guru")),
																								Restrictions.eq("guru2",
																										searchguru
																												.getAttribute(
																														"guru")))))))))))))

				)

				.add(searchkelas.getSelectedItem() == null || searchkelas.getSelectedItem().getValue() == null
						|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kelas", searchkelas.getSelectedItem().getValue()))

				.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
						|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<JadwalPelajaran> jadwalPelajaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalPelajaran);
		grid.setRowRenderer(new JadwalPelajaranRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings("unchecked")
	public static String generateiIntroductoryText(JadwalPelajaran jadwalPelajaran, TreeMap<String, Long> pertemuans) {
		String introductoryText = "<h3>1. Identitas Matapelajaran</h3><table style='border: 0px solid black;width: 100%;'><tr><td style='width: 20%;'>"
				+ (jadwalPelajaran.getSekolah() == null ? ""
						: "Yayasan" + "</td><td>" + jadwalPelajaran.getSekolah().getYayasan().getNama())
				+ "</td></tr>";

		introductoryText += (jadwalPelajaran.getSekolah() == null ? ""
				: "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Sekolah") + "</td><td>"
						+ jadwalPelajaran.getSekolah().getNama() + "</td></tr>");

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Kode Matapelajaran") + "</td><td>"
				+ jadwalPelajaran.getMatapelajaran().getKode() + "</td></tr>";

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Nama Matapelajaran") + "</td><td>"
				+ jadwalPelajaran.getMatapelajaran().getNama() + "</td></tr>";

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Semester") + "</td><td>"
				+ jadwalPelajaran.getSemester() + "</td></tr>";

		String pengampu = "";
		for (Guru d : jadwalPelajaran.populateGuru().values()) {
			String m = d.getNama();
			pengampu += pengampu.isEmpty() ? m : ",<br></br>" + m;
		}

		introductoryText += "<tr><td style='width: 20%;vertical-align: top;'>"
				+ Common.getBahasaConfig("Pengampu JadwalPelajaran") + "</td><td>" + pengampu + "</td></tr>";
		Session session = HibernateUtil.currentSession();
		List<Object[]> objects = session.createCriteria(KelasSiswaPunyaSiswa.class)
				.add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas())).createAlias("siswa", "siswa")
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.projectionList().add(Projections.property("siswa.nomorInduk"))
						.add(Projections.property("siswa.nama")).add(Projections.property("siswa.id")))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama")).list();
		String peserta = "";
		for (Object[] o : objects) {
			String m = o[0] + " - " + o[1];
			peserta += peserta.isEmpty() ? m : ",<br></br>" + m;
		}

		if (!jadwalPelajaran.getDeskripsiPembelajaran().isEmpty()
				&& jadwalPelajaran.getMatapelajaran().getDeskripsiPembelajaran().isEmpty()) {

		}

		introductoryText += "<tr><td style='width: 20%;vertical-align: top;'>"
				+ Common.getBahasaConfig("Peserta Pembelajaran") + "</td><td>" + peserta + "</td></tr></table>";

		introductoryText += "<h3>2.	" + Common.getBahasaConfig("Deskripsi Pembelajaran") + "</h3><p>"
				+ jadwalPelajaran.getDeskripsiPembelajaran().replaceAll("\n", "<br></br>") + "</p>";

		introductoryText += "<h3>3.	" + Common.getBahasaConfig("Capaian / Kompetensi") + "</h3><p>"
				+ jadwalPelajaran.getCapaianPembelajaranProdi().replaceAll("\n", "<br></br>") + "</p>";

		introductoryText += "<h3>4.	" + Common.getBahasaConfig("Rencana Pembelajaran") + "</h3>";

		introductoryText += "<table style='border: 1px solid black;width: 100%;'>";
		introductoryText += "<tr style='border: 1px solid #ddd;'>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Minggu ke") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>"
				+ Common.getBahasaConfig("Indikator Capaian Pembelajaran") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Bahan Kajian") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Metode Pembelajaran")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Pengalaman Belajar")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Waktu Pembelajaran")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Tugas dan Penilaian")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Sumber Belajar") + "</th>";
		introductoryText += "</tr>";
		int i = 1;
		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				introductoryText += "<tr style='border: 1px solid #ddd;hover {background-color: rgba(169,169,169,0.4);}'>";
				introductoryText += "<td style='border: 1px solid #ddd;text-align:center;'>" + i + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getIndikator().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getTopik().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getMetodePembelajaran().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getPengalamanBelajar().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getWaktupembelajaran().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getTugasDanPenilaian().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getBukuRujukan1().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "</tr>";
				i++;
			}
		}

		introductoryText += "</table>";

		introductoryText += "<h3>5.	" + Common.getBahasaConfig("Daftar Rujukan") + "</h3><ol>";

		List<Item> items = session.createCriteria(JadwalPelajaranPunyaItem.class)
				.setProjection(Projections.groupProperty("item"))
				.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).list();
		for (Item itemData : items) {
			// CSLItemData item = ItemAction.generateCSLItemData(itemData);
			try {
				// String bibl =
				// CSL.makeAdhocBibliography("chicago-author-date",
				// item).makeString();
				introductoryText += "<li>" + itemData.getNama() + " - " + itemData.getPengarangs() + " - "
						+ (itemData.getPenerbit() == null ? "" : itemData.getPenerbit().getNama()) + "-"
						+ (itemData.getTahun() == null || itemData.getTahun().equals(0) ? "" : itemData.getTahun())
						+ "</li>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		List<BukuBahanAjar> bukuBahanAjars = session.createCriteria(MatapelajaranPunyaBukuBahanAjar.class)
				.setProjection(Projections.groupProperty("bukuBahanAjar"))
				.add(Restrictions.eq("matapelajaran", jadwalPelajaran.getMatapelajaran())).list();
		for (BukuBahanAjar itemData : bukuBahanAjars) {
			// CSLItemData item =
			// BukuBahanAjarAction.generateCSLItemData(itemData);
			try {
				// String bibl =
				// CSL.makeAdhocBibliography("chicago-author-date",
				// item).makeString();
				introductoryText += "<li>" + itemData.getNama() + " - " + itemData.getPengarang1() + " - "
						+ (itemData.getPenerbit()) + "-"
						+ (itemData.getTahun() == null || itemData.getTahun().equals(0) ? "" : itemData.getTahun())
						+ "</li>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		List<Artikel> artikels = session.createCriteria(DataPunyaArtikel.class)
				.setProjection(Projections.groupProperty("artikel"))
				.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).list();
		for (Artikel itemData : artikels) {
			// CSLItemData item =
			// ArtikelAction.generateCSLItemData(itemData);
			try {
				// String bibl =
				// CSL.makeAdhocBibliography("chicago-author-date",
				// item).makeString();
				introductoryText += "<li>" + itemData.getJudul() + "</li>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		introductoryText += "</ol><br></br><br></br><br></br><br></br><br></br><br></br>";

		return introductoryText;
	}

	public void onCopyJadwalPelajaran(Event event) throws Exception {
		final MyWindow window = new MyWindow("Copy Jadwal Pelajaran", "normal", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("300px");
		window.setWidth("400px");

		final Combobox tahunAkademikDari = Common.generateTahunAjaran(null);
		if (searchta.getSelectedItem() != null) {
			Common.selectComboItem(tahunAkademikDari, searchta.getSelectedItem().getValue());
		}
		final Combobox tahunAkademikKe = Common.generateTahunAjaran(null);

		final Combobox jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		jenisSemester.appendChild(comboitem);

		final Combobox jenisSemester1 = new Combobox();
		comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		jenisSemester1.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		jenisSemester1.appendChild(comboitem);

		if (searchsmt.getSelectedItem() != null) {
			Common.selectComboItem(jenisSemester, searchsmt.getSelectedItem().getValue());
			Common.selectComboItem(jenisSemester1, searchsmt.getSelectedItem().getValue());
		}

		final Combobox yayasan = new Combobox();
		final Combobox sekolah = new Combobox();

		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		// grid.setOddRowSclass("non-odd");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran Dari"));
		row.appendChild(tahunAkademikDari);
		tahunAkademikDari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester Dari"));
		row.appendChild(jenisSemester1);
		jenisSemester1.setWidth("90%");
		jenisSemester1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran Ke"));
		row.appendChild(tahunAkademikKe);
		tahunAkademikKe.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester Ke"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Copy Jadwal Pelajaran", "/img/svg/edit-copy.svg");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String tahunAkademik1 = (String) (tahunAkademikDari.getSelectedItem() == null ? null
						: tahunAkademikDari.getSelectedItem().getValue());
				final String tahunAkademik2 = (String) (tahunAkademikKe.getSelectedItem() == null ? null
						: tahunAkademikKe.getSelectedItem().getValue());

				final String semester1 = (String) (jenisSemester1.getSelectedItem() == null ? null
						: jenisSemester1.getSelectedItem().getValue());

				final String semester2 = (String) (jenisSemester.getSelectedItem() == null ? null
						: jenisSemester.getSelectedItem().getValue());

				final Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				final Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());

				if (tahunAkademik1 == null) {
					MyMessageboxConfig.show("Tahun Ajaran Dari belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Tahun Ajaran Dari; (2) pilih tahun ajaran yang sesuai; (3) ulangi proses setelah tahun ajaran dipilih.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (tahunAkademik2 == null) {
					MyMessageboxConfig.show("Tahun Ajaran Ke belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Tahun Ajaran Ke; (2) pilih tahun ajaran yang sesuai; (3) ulangi proses setelah tahun ajaran dipilih.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (tahunAkademik1.equals(tahunAkademik2) && semester1.equals(semester2)) {
					MyMessageboxConfig.show("Tahun ajaran dan semester tidak boleh sama antara asal dan tujuan. Langkah yang dapat dilakukan: (1) periksa kembali pilihan tahun ajaran dan semester; (2) pilih kombinasi tujuan yang berbeda dari asal; (3) ulangi proses setelah kombinasinya berbeda.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (mySekolah == null) {
					MyMessageboxConfig.show("Sekolah belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Sekolah; (2) pilih sekolah yang sesuai; (3) ulangi proses setelah sekolah dipilih.", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (semester1 == null) {
					MyMessageboxConfig.show("Jenis Semester dari belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Jenis Semester dari; (2) pilih jenis semester yang sesuai; (3) ulangi proses setelah jenis semester dipilih.", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (semester2 == null) {
					MyMessageboxConfig.show("Jenis Semester ke belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Jenis Semester ke; (2) pilih jenis semester yang sesuai; (3) ulangi proses setelah jenis semester dipilih.", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				final List<JadwalPelajaran> jadwalPelajarans = ConstantValues
						.simpleList(session.createCriteria(JadwalPelajaran.class)

								.add(myYayasan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("yayasan", myYayasan))

								.add(mySekolah == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("sekolah", mySekolah))

								.add(Restrictions.eq("tahunAjaran", tahunAkademik1))
								.add(Restrictions.sqlRestriction(
										semester1.equals(JadwalPelajaran.GANJIL) ? "this_.semester % 2 = 1"
												: "this_.semester % 2 = 0")),
								JadwalPelajaran.class);

				System.out.println("jadwalPelajarans -> " + jadwalPelajarans.size());

				MyMessageboxConfig.show(
						"Apakah Anda yakin ingin melanjutkan proses penyalinan " + jadwalPelajarans.size()
								+ " jadwal pelajaran dari tahun ajaran " + tahunAkademik1 + " semester " + semester1
								+ " ke tahun ajaran " + tahunAkademik2 + " semester " + semester2
								+ "? Jadwal pada periode tujuan akan ditambahkan sesuai hasil penyalinan.",
						"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									String warning = "";
									Session session = HibernateUtil.currentSession();
									for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {

										Integer ada = ((Number) session.createCriteria(JadwalPelajaran.class)

												.add(myYayasan == null ? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("yayasan", myYayasan))
												.add(mySekolah == null ? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("sekolah", mySekolah))
												.add(Restrictions.eq("tahunAjaran", tahunAkademik2))
												.add(Restrictions
														.sqlRestriction(semester2.equals(JadwalPelajaran.GANJIL)
																? "this_.semester % 2 = 1"
																: "this_.semester % 2 = 0"))

												.add(jadwalPelajaran.getJamPelajaran() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran",
																jadwalPelajaran.getJamPelajaran()))

												.add(jadwalPelajaran.getJamPelajaran2() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran2",
																jadwalPelajaran.getJamPelajaran2()))

												.add(jadwalPelajaran.getJamPelajaran3() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran3",
																jadwalPelajaran.getJamPelajaran3()))

												.add(jadwalPelajaran.getJamPelajaran4() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran4",
																jadwalPelajaran.getJamPelajaran4()))

												.add(jadwalPelajaran.getJamPelajaran5() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran5",
																jadwalPelajaran.getJamPelajaran5()))

												.add(jadwalPelajaran.getJamPelajaran6() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran6",
																jadwalPelajaran.getJamPelajaran6()))

												.add(jadwalPelajaran.getJamPelajaran7() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran7",
																jadwalPelajaran.getJamPelajaran7()))

												.add(jadwalPelajaran.getJamPelajaran8() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran8",
																jadwalPelajaran.getJamPelajaran8()))

												.add(jadwalPelajaran.getJamPelajaran9() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran9",
																jadwalPelajaran.getJamPelajaran9()))

												.add(jadwalPelajaran.getJamPelajaran10() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran10",
																jadwalPelajaran.getJamPelajaran10()))

												.add(jadwalPelajaran.getJamPelajaran11() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran11",
																jadwalPelajaran.getJamPelajaran11()))

												.add(jadwalPelajaran.getJamPelajaran12() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran12",
																jadwalPelajaran.getJamPelajaran12()))

												.add(jadwalPelajaran.getGuru() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru", jadwalPelajaran.getGuru()))

												.add(jadwalPelajaran.getGuru2() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru2", jadwalPelajaran.getGuru2()))

												.add(jadwalPelajaran.getGuru3() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru3", jadwalPelajaran.getGuru3()))

												.add(jadwalPelajaran.getGuru4() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru4", jadwalPelajaran.getGuru4()))

												.add(jadwalPelajaran.getGuru5() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru5", jadwalPelajaran.getGuru5()))

												.add(jadwalPelajaran.getGuru6() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru6", jadwalPelajaran.getGuru6()))

												.add(jadwalPelajaran.getGuru7() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru7", jadwalPelajaran.getGuru7()))

												.add(jadwalPelajaran.getGuru8() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru8", jadwalPelajaran.getGuru8()))

												.add(jadwalPelajaran.getGuru9() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru9", jadwalPelajaran.getGuru9()))

												.add(jadwalPelajaran.getGuru10() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru10", jadwalPelajaran.getGuru10()))

												.add(jadwalPelajaran.getGuru11() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru11", jadwalPelajaran.getGuru11()))

												.add(jadwalPelajaran.getGuru12() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru12", jadwalPelajaran.getGuru12()))

												.add(jadwalPelajaran.getHari() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari", jadwalPelajaran.getHari()))

												.add(jadwalPelajaran.getHari2() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari2", jadwalPelajaran.getHari2()))

												.add(jadwalPelajaran.getHari3() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari3", jadwalPelajaran.getHari3()))

												.add(jadwalPelajaran.getHari4() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari4", jadwalPelajaran.getHari4()))

												.add(jadwalPelajaran.getHari5() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari5", jadwalPelajaran.getHari5()))

												.add(jadwalPelajaran.getHari6() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari6", jadwalPelajaran.getHari6()))

												.add(jadwalPelajaran.getHari7() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari7", jadwalPelajaran.getHari7()))

												.add(jadwalPelajaran.getHari8() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari8", jadwalPelajaran.getHari8()))

												.add(jadwalPelajaran.getHari9() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari9", jadwalPelajaran.getHari9()))

												.add(jadwalPelajaran.getHari10() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari10", jadwalPelajaran.getHari10()))

												.add(jadwalPelajaran.getHari11() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari11", jadwalPelajaran.getHari11()))

												.add(jadwalPelajaran.getHari12() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari12", jadwalPelajaran.getHari12()))

												.add(jadwalPelajaran.getProgram() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("program", jadwalPelajaran.getProgram()))

												.add(jadwalPelajaran.getMatapelajaran() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("matapelajaran",
																jadwalPelajaran.getMatapelajaran()))

												.createAlias("kelas", "kelas")

												.add(jadwalPelajaran.getKelas() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.ilike("kelas.nama",
																jadwalPelajaran.getKelas().getNama(), MatchMode.EXACT))

												.setProjection(Projections.rowCount()).uniqueResult()).intValue();

										System.out.println("jadwalPelajaran -> " + jadwalPelajaran + ", ada " + ada);

										if (ada.equals(0)) {

											KelasSiswa kelasSiswa = (KelasSiswa) session
													.createCriteria(KelasSiswa.class)
													.add(Restrictions.eq("sekolah", mySekolah))
													.add(Restrictions.eq("tahunAjaran", tahunAkademik2))
													.add(Restrictions.ilike("nama",
															jadwalPelajaran.getKelas().getNama(), MatchMode.EXACT))
													.setMaxResults(1).uniqueResult();

											if (kelasSiswa != null) {
												JadwalPelajaran copyJadwalPelajaran = (JadwalPelajaran) jadwalPelajaran
														.clone();

												copyJadwalPelajaran.setId(null);
												copyJadwalPelajaran.setTahunAjaran(tahunAkademik2);
												copyJadwalPelajaran
														.setSemester(semester2.equals(Perkuliahan.GANJIL) ? 1 : 2);
												copyJadwalPelajaran.setKelas(kelasSiswa);

												session.save(copyJadwalPelajaran);
											} else {
												warning += "\n\nKelas " + jadwalPelajaran.getKelas().getNama()
														+ " di tahun ajaran " + tahunAkademik2 + " tidak ditemukan";
											}

										}

									}

									MyMessageboxConfig.show("Proses penyalinan jadwal pelajaran telah selesai dilakukan." + warning,
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													window.detach();
													onSearchDefault(arg0);
												}
											});

								}

							}
						});

			}
		});

		save.setParent(toolbar);

		window.onModal();
	}

	public void onDeleteJadwalPelajaran(Event event) throws Exception {
		final MyWindow window = new MyWindow("Hapus Jadwal Pelajaran", "normal", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("300px");
		window.setWidth("850px");

		final Combobox tahunAkademikDari = Common.generateTahunAjaran(null);
		if (searchta.getSelectedItem() != null) {
			Common.selectComboItem(tahunAkademikDari, searchta.getSelectedItem().getValue());
		}

		final Combobox jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		jenisSemester.appendChild(comboitem);

		final Combobox yayasan = new Combobox();
		final Combobox sekolah = new Combobox();

		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		// grid.setOddRowSclass("non-odd");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademikDari);
		tahunAkademikDari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setWidth("90%");

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Hapus Jadwal Pelajaran", "/img/svg/trash.svg");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String tahunAkademik1 = (String) (tahunAkademikDari.getSelectedItem() == null ? null
						: tahunAkademikDari.getSelectedItem().getValue());

				final String semester = (String) (jenisSemester.getSelectedItem() == null ? null
						: jenisSemester.getSelectedItem().getValue());

				final Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				final Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());

				if (tahunAkademik1 == null) {
					MyMessageboxConfig.show("Tahun Akademik Dari belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Tahun Akademik Dari; (2) pilih tahun akademik yang sesuai; (3) ulangi proses setelah tahun akademik dipilih.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (semester == null) {
					MyMessageboxConfig.show("Jenis Semester belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Jenis Semester; (2) pilih jenis semester yang sesuai; (3) ulangi proses setelah jenis semester dipilih.", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				final List<JadwalPelajaran> jadwalPelajarans = ConstantValues
						.simpleList(session.createCriteria(JadwalPelajaran.class)

								.add(myYayasan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("yayasan", myYayasan))

								.add(mySekolah == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("sekolah", mySekolah))
								.add(Restrictions.eq("tahunAjaran", tahunAkademik1))
								.add(Restrictions.sqlRestriction(
										semester.equals(JadwalPelajaran.GANJIL) ? "this_.semester % 2 = 1"
												: "this_.semester % 2 = 0")),
								JadwalPelajaran.class);

				// Htm peringatan = "";

				MyMessageboxConfig.show(
						"Apakah Anda yakin ingin menghapus jadwal pelajaran ini? Data yang telah dihapus tidak dapat dikembalikan.\nCatatan: jadwal pelajaran yang sudah dibuatkan agenda pertemuan dan memiliki data mahasiswa tidak dapat dihapus.",
						"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentSession();
									for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {

										Integer count1 = jadwalPelajaran.ambilJumlahPertemuan();

										if (count1.equals(0)) {
											session.delete(jadwalPelajaran);
										}

									}

									MyMessageboxConfig.show("Jadwal pelajaran berhasil dihapus.", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													window.detach();
													onSearchDefault(arg0);

												}
											});

									window.detach();

								}

							}
						});

			}
		});

		save.setParent(toolbar);

		window.onModal();
	}

}
