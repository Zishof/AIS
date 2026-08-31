package ais.action.master.surat;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.master.surat.helper.AmbilDataAlurPersetujuanSuratKeluarBanbox;
import ais.action.master.surat.helper.AmbilDataNomorSuratBanbox;
import ais.action.master.surat.helper.KlasifikasiSuratKeluarParameterHelper;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.OnSaveListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.database.model.surat.KelompokNomorSurat;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeter;
import ais.database.model.surat.KlasifikasiSuratKeluarUntuk;
import ais.database.model.surat.NomorSurat;
import ais.database.model.surat.VariableSuratKeluar;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk klasifikasi surat keluar. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code Combobox searchyayasan}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initDetail()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code getDefaultParameter()}, {@code
 * ambilPathGambarParameter()}, {@code onSearchDefault()}); mutasi data ({@code onSave()}); pelaporan/ekspor
 * ({@code generateReport()}); operasi domain lain ({@code onNomorSurat()}, {@code onAdd()}, {@code
 * layoutSurat()}, {@code layoutDisposisi()}, {@code masukkanTabelKeParameter()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KlasifikasiSuratKeluarAction extends GenericAutowireComposer
		implements OnSaveListener, DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Checkbox searchaktif;
	protected Combobox searchkelompokNomorSurat;

	private AmbilDataSatuanKerjaBanbox searchparent;

	private Textbox kode;
	private Textbox nama;
	private Combobox klasifikasiSuratKeluarUntuk;
	private AmbilDataAlurPersetujuanSuratKeluarBanbox alurPersetujuanSuratKeluar;
	private AmbilDataNomorSuratBanbox nomorSurat;
	private Combobox sifat;
	private Combobox fakultas;
	private Combobox jurusan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KlasifikasiSuratKeluar klasifikasiSuratKeluar;
	private MyToolbarbuttonConfig add;
	@SuppressWarnings("unused")
	private KlasifikasiSuratKeluarUntuk umum;

	private MyGrid gridParemeter;
	private Textbox kodeGrupPengguna;

	private Tabpanel manajemenNomorSurat;

	public void onNomorSurat(Event event) {
		if (manajemenNomorSurat.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenNomorSurat);
			MyInclude iframe = new MyInclude("/pages/master/surat/nomor_surat.zul");
			iframe.setParent(window);
		}

	}

	private Textbox kodeItemBiaya;
	private MyCheckboxConfig sekaliBayar;
	private MyCheckboxConfig harusBayarLunasSmtSaatIni;
	private MyCheckboxConfig harusBayarLunasSmtLalu;

	private boolean pt = false;
	private boolean ya = false;
	private Row hbFakultasLabel;
	private Row hbYayasan;
	private Combobox yayasan;
	private Combobox sekolah;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataNomorSuratBanbox nomorAgenda;
	private Textbox perihalDefault;
	private Sekolah sk;
	private MyCheckboxConfig tanpaAlur;
	private MyCheckboxConfig tanpaTemplate;
	private MyCheckboxConfig aktifKuliah;
	private MyDatebox bisaDicetakMulai;
	private MyDatebox bisaDicetakSampai;
	private MyCheckboxConfig tampilkanSemester;

	private String tipe = "surat";
	private MyCheckboxConfig kaitkanDenganSuratLain;
	private Textbox istilahSuratLain;
	private MyLabelConfig istilahSuratLainLabel;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (execution.getParameter("tipe") != null && !execution.getParameter("tipe").trim().isEmpty()) {
			tipe = execution.getParameter("tipe").trim();
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		sk = SekolahUtil.getSekolah();
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		Common.insertComboDanSemua(searchkelompokNomorSurat, "nama", KelompokNomorSurat.class);
		if (searchkelompokNomorSurat != null) { searchkelompokNomorSurat.setReadonly(true); }

		KelompokNomorSuratAction.checkKelompok(searchkelompokNomorSurat);

		umum = SuratUtil.UMUM;

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "nomorSurat", "nomorAgenda", "sifat",
				"klasifikasiSuratKeluarUntuk", "alurPersetujuanSuratKeluar", "fakultas", "jurusan", "yayasan",
				"sekolah", "satuanKerja", "aktif", "kodeGrupPengguna", "perihalDefault", "tanpaAlur", "tanpaTemplate",
				"bisaDicetakMulai", "bisaDicetakSampai", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KlasifikasiSuratKeluar.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class KlasifikasiSuratKeluarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KlasifikasiSuratKeluar klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) arg1;

			if (klasifikasiSuratKeluar.getTipe() == null) {
				klasifikasiSuratKeluar.setTipe(tipe);
				Common.refreshUpdate(klasifikasiSuratKeluar);
			}

			new Label(klasifikasiSuratKeluar.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(KlasifikasiSuratKeluar.class, klasifikasiSuratKeluar,
					klasifikasiSuratKeluar.getNama()).setParent(arg0);
			new Label(klasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getNama()).setParent(arg0);

			Vbox c = new Vbox();
			c.setParent(arg0);
			new Label(klasifikasiSuratKeluar.getNomorSurat() == null ? ""
					: klasifikasiSuratKeluar.getNomorSurat().getContohFormat()).setParent(c);
			new Label(klasifikasiSuratKeluar.getNomorAgenda() == null ? ""
					: klasifikasiSuratKeluar.getNomorAgenda().getContohFormat()).setParent(c);

			new Label(klasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar() == null ? ""
					: klasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar().toString()).setParent(arg0);
			new Label(klasifikasiSuratKeluar.getSifat() == null ? "Tidak Ada" : klasifikasiSuratKeluar.getSifat())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			vbox.appendChild(new Label(klasifikasiSuratKeluar.getSatuanKerja() == null ? ""
					: klasifikasiSuratKeluar.getSatuanKerja().getNama()));
			Hbox hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(
					klasifikasiSuratKeluar.getFakultas() == null ? "" : klasifikasiSuratKeluar.getFakultas().getNama())
					.setParent(hbox);
			new Label(klasifikasiSuratKeluar.getJurusan() == null ? "" : klasifikasiSuratKeluar.getJurusan().getNama())
					.setParent(hbox);

			hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(klasifikasiSuratKeluar.getYayasan() == null ? "" : klasifikasiSuratKeluar.getYayasan().getNama())
					.setParent(hbox);
			new Label(klasifikasiSuratKeluar.getSekolah() == null ? "" : klasifikasiSuratKeluar.getSekolah().getNama())
					.setParent(hbox);

			vbox = new Vbox();
			String[] spl = klasifikasiSuratKeluar.getKodeGrupPengguna().split(";");
			int index = 1;
			for (String s : spl) {
				if (!s.trim().isEmpty()) {
					String tbmrole = (String) HibernateUtil.currentSession().createCriteria(Tbmrole.class)
							.add(Restrictions.eq("roleId", s.trim())).setProjection(Projections.property("roleName"))
							.setMaxResults(1).uniqueResult();
					if (tbmrole != null) {
						vbox.appendChild(new Label(index + ". " + tbmrole));
						index++;
					}

				}
			}
			vbox.setParent(arg0);

			new Label(klasifikasiSuratKeluar.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(klasifikasiSuratKeluar.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					klasifikasiSuratKeluar.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(klasifikasiSuratKeluar);
				}
			});

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, klasifikasiSuratKeluar,
					KlasifikasiSuratKeluarAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					Map parameters = getDefaultParameter(false);
					PDFMergerUtility ut = new PDFMergerUtility();

					for (int index = 1; index <= 15; index++) {
						try {
							LampiranLain lampiranLain = LampiranLain.ambil(klasifikasiSuratKeluar.getId(),
									LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
							if (lampiranLain != null && lampiranLain.getId() != null) {
								try {

									File file = Report.generateCompileFileReport(Report.PDF, parameters,
											lampiranLain.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate(),
											false);
									ut.addSource(file);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}

					}
					try {
						File filePdfBaru = new File(
								Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
						ut.setDestinationStream(new FileOutputStream(filePdfBaru));
						ut.mergeDocuments();
						Report.tampil(filePdfBaru);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

				}

			});
			button.setParent(toolbar);

			toolbar.setParent(arg0);
		}

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) obj;
		init(klasifikasiSuratKeluar);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new KlasifikasiSuratKeluar());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final KlasifikasiSuratKeluar klasifikasiSuratKeluar, Component component)
			throws Exception {
		this.klasifikasiSuratKeluar = klasifikasiSuratKeluar;
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabParameter = new MyTabConfig("Parameter");
		tabParameter.setParent(tabs);

		MyTabConfig tabLayout = new MyTabConfig("Layout Surat Keluar");
		tabLayout.setParent(tabs);

		MyTabConfig tabLayoutDisposisi = new MyTabConfig("Layout Disposisi");
		tabLayoutDisposisi.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelParameter = new ais.ui.util.MyTabpanel();
		tabpanelParameter.setParent(tabpanels);

		Tabpanel tabpanelLayout = new ais.ui.util.MyTabpanel();
		tabpanelLayout.setParent(tabpanels);

		tabpanelParameter.appendChild(new KlasifikasiSuratKeluarParameterHelper(gridParemeter = new MyGrid())
				.initDetail(klasifikasiSuratKeluar, this));

		Tabpanel tabpanelLayoutDisposisi = new ais.ui.util.MyTabpanel();
		tabpanelLayoutDisposisi.setParent(tabpanels);

		layoutSurat(tabLayout, tabpanelLayout);
		layoutDisposisi(tabLayoutDisposisi, tabpanelLayoutDisposisi);
	}

	private void layoutSurat(MyTabConfig tabLayout, Tabpanel tabpanelLayout) {

		Borderlayout borderlayoutAtas = new ais.ui.util.MyBorderlayout();
		borderlayoutAtas.setParent(tabpanelLayout);

		Center centerAtas = new Center();
		centerAtas.setParent(borderlayoutAtas);
		ais.ui.util.ZkCompat.setFlex(centerAtas, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(centerAtas);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		for (int i = 1; i <= 15; i++) {
			Tab tab = new Tab(i + "");
			tabs.appendChild(tab);
		}

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		for (int i = 1; i <= 15; i++) {
			final int index = i;
			Tabpanel tabpanelParameter = new ais.ui.util.MyTabpanel();
			tabpanelParameter.setParent(tabpanels);

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(tabpanelParameter);

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("380px");
		north.setAutoscroll(true);

			final Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(north);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("35%");

			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("File *.jrxml / *.jasper"));
			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, klasifikasiSuratKeluar.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index),
					"Lampiran *.jrxml / *.jasper", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (KlasifikasiSuratKeluarAction.this.klasifikasiSuratKeluar.getId() == null) {
								onSave(arg0);
							}
							if (KlasifikasiSuratKeluarAction.this.klasifikasiSuratKeluar.getId() != null) {
								LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();

								if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
									try {
										Session session = StreamingHibernateUtil.getInstance().currentSession();

										session.refresh(lainMahasiswa);
										lainMahasiswa.setRef(
												KlasifikasiSuratKeluarAction.this.klasifikasiSuratKeluar.getId());

										session.getTransaction().begin();
										session.update(lainMahasiswa);
										session.getTransaction().commit();

										StreamingHibernateUtil.getInstance().closeSession();
									} catch (Exception e) {
										StreamingHibernateUtil.getInstance().rollbackTransaction();
										Common.tampilErrorJikaAdmin(e);
									}

								}

								generateReport(center, lainMahasiswa);
							}
						}
					});
			hbox.setParent(row);

			MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
			print.setStyle("font-size:9px");
			print.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (klasifikasiSuratKeluar.getId() != null) {

						LampiranLain lainMahasiswa = LampiranLain.ambil(klasifikasiSuratKeluar.getId(),
								LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index), true);

						generateReport(center, lainMahasiswa);
					}
				}
			});
			print.setParent(hbox);
		}

	}

	private void layoutDisposisi(MyTabConfig tabLayout, Tabpanel tabpanelLayout) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelLayout);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("380px");
		north.setAutoscroll(true);

		final Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File *.jrxml / *.jasper"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, klasifikasiSuratKeluar.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI, "Lampiran *.jrxml / *.jasper", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (KlasifikasiSuratKeluarAction.this.klasifikasiSuratKeluar.getId() == null) {
							onSave(arg0);
						}

						if (KlasifikasiSuratKeluarAction.this.klasifikasiSuratKeluar.getId() != null) {
							LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();

							if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
								try {
									Session session = StreamingHibernateUtil.getInstance().currentSession();

									session.refresh(lainMahasiswa);
									lainMahasiswa
											.setRef(KlasifikasiSuratKeluarAction.this.klasifikasiSuratKeluar.getId());

									session.getTransaction().begin();
									session.update(lainMahasiswa);
									session.getTransaction().commit();

									StreamingHibernateUtil.getInstance().closeSession();
								} catch (Exception e) {
									StreamingHibernateUtil.getInstance().rollbackTransaction();
									Common.tampilErrorJikaAdmin(e);
								}

							}

							generateReport(center, lainMahasiswa);
						}
					}
				});
		hbox.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.setStyle("font-size:9px");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (klasifikasiSuratKeluar.getId() != null) {

					LampiranLain lainMahasiswa = LampiranLain.ambil(klasifikasiSuratKeluar.getId(),
							LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI, true);

					generateReport(center, lainMahasiswa);
				}
			}
		});
		print.setParent(hbox);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void masukkanTabelKeParameter(Map<String, Object> parameters, String nilai) {
		List maps = new ArrayList();
		if (nilai != null && !nilai.trim().isEmpty()) {
			String[] s = StringUtils.split(nilai, "||");

			for (int i = 0; i < s.length; i++) {
				Map map = new java.util.HashMap();
				String nil = s[i];
				String[] val = nil == null || nil.trim().isEmpty() ? new String[0] : StringUtils.split(nil, "<->");
				int index = 0;
				for (String v : val) {
					map.put("v" + index, v);
					index++;
				}
				maps.add(map);
			}

		}
		parameters.put("maps", maps);
	}

	@SuppressWarnings({ "unchecked" })
	private Map<String, Object> getDefaultParameter(Boolean dariEdit) {
		Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();

		Map<Long, VariableSuratKeluar> variableSuratKeluars = ConstantValues
				.ambilBerdasarClass(VariableSuratKeluar.class);
		for (VariableSuratKeluar variableSuratKeluar : variableSuratKeluars.values()) {
			try {
				parameters.put(variableSuratKeluar.getKey(), variableSuratKeluar.getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/KlasifikasiSuratKeluarAction.java:713");
				// TODO: handle exception
			}
		}
		variableSuratKeluars = null;

		if (dariEdit) {
			List<Row> rowsParameter = gridParemeter.getRows().getChildren();
			for (Row row : rowsParameter) {
				KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter = (KlasifikasiSuratKeluarParemeter) row
						.getAttribute("klasifikasiSuratKeluarParemeter");
				if (klasifikasiSuratKeluarParemeter != null) {
					parameters.put(klasifikasiSuratKeluarParemeter.getKey(),
							klasifikasiSuratKeluarParemeter.getNilai());
					if (klasifikasiSuratKeluarParemeter.getTipe().equals(KlasifikasiSuratKeluarParemeter.DATA)) {
						masukkanTabelKeParameter(parameters, klasifikasiSuratKeluarParemeter.getNilai());
					}
					if (KlasifikasiSuratKeluarParemeter.GAMBAR.equals(klasifikasiSuratKeluarParemeter.getTipe())) {
						parameters.put(klasifikasiSuratKeluarParemeter.getKey(),
								ambilPathGambarParameter(klasifikasiSuratKeluarParemeter));
					}
				}
			}
		} else {
			List<KlasifikasiSuratKeluarParemeter> klasifikasiSuratKeluarParemeters = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(KlasifikasiSuratKeluarParemeter.class)
							.addOrder(Order.asc("nomorUrut")).addOrder(Order.desc("id"))
							.add(Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar)),
					KlasifikasiSuratKeluarParemeter.class);
			for (KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter : klasifikasiSuratKeluarParemeters) {
				parameters.put(klasifikasiSuratKeluarParemeter.getKey(), klasifikasiSuratKeluarParemeter.getNilai());
				if (klasifikasiSuratKeluarParemeter.getTipe().equals(KlasifikasiSuratKeluarParemeter.DATA)) {
					masukkanTabelKeParameter(parameters, klasifikasiSuratKeluarParemeter.getNilai());
				}
				if (KlasifikasiSuratKeluarParemeter.GAMBAR.equals(klasifikasiSuratKeluarParemeter.getTipe())) {
					parameters.put(klasifikasiSuratKeluarParemeter.getKey(),
							ambilPathGambarParameter(klasifikasiSuratKeluarParemeter));
				}
			}
		}
		NomorSurat nomorSurat = (NomorSurat) this.nomorSurat.getAttribute("nomorSurat");

		parameters.put("nomor.surat", nomorSurat == null ? "NOMOR-SURAT" : nomorSurat.getContohFormat());

		parameters.put("tidak_usah_pakai_connection", true);

		return parameters;
	}

	/**
	 * Mengambil lokasi gambar parameter surat secara null-safe. Parameter gambar
	 * boleh belum memiliki lampiran; kondisi tersebut berarti gambar kosong dan
	 * bukan kegagalan sistem yang perlu dicatat sebagai NullPointerException.
	 */
	private String ambilPathGambarParameter(KlasifikasiSuratKeluarParemeter parameter) {
		if (parameter == null || parameter.getId() == null) {
			return null;
		}
		try {
			LampiranLain lampiran = LampiranLain.ambil(parameter.getId(),
					KlasifikasiSuratKeluarParemeter.class.getName());
			File file = lampiran == null ? null : lampiran.ambilFile();
			if (file == null) {
				return null;
			}
			return file.getAbsolutePath();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"Gagal mengambil lampiran gambar parameter klasifikasi surat keluar id=" + parameter.getId());
			return null;
		}
	}

	private void generateReport(Center center, LampiranLain lainMahasiswa) {
		Common.clear(center);
		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {

				File file = Report.generateCompileFileReport(Report.PDF, getDefaultParameter(true),
						lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate(), false);
				CommonReport.tampilkanReportPDF(center, file);

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	private void init(final KlasifikasiSuratKeluar klasifikasiSuratKeluar) throws Exception {
		this.klasifikasiSuratKeluar = klasifikasiSuratKeluar;

		Tbmuser tbmuser = Common.getCurrentUser();
		if (klasifikasiSuratKeluar.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			klasifikasiSuratKeluar.setFakultas(tbmuser.ambilFakultas());
		}

		if (klasifikasiSuratKeluar.getSatuanKerja() == null && tbmuser.ambilSatuanKerja() != null) {
			klasifikasiSuratKeluar.setSatuanKerja(tbmuser.ambilSatuanKerja());
		}

		if (klasifikasiSuratKeluar.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			klasifikasiSuratKeluar.setJurusan(tbmuser.ambilJurusan());
		}

		if (klasifikasiSuratKeluar.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			klasifikasiSuratKeluar.setYayasan(tbmuser.ambilYayasan());
		}

		if (klasifikasiSuratKeluar.getSekolah() == null && tbmuser.ambilSekolah() != null) {
			klasifikasiSuratKeluar.setSekolah(tbmuser.ambilSekolah());
		}

		addWindow.setTitle(klasifikasiSuratKeluar.getId() == null ? "Tambah Klasifikasi Surat Keluar" : "Ubah Klasifikasi Surat Keluar");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		final East east = new East();
		east.setWidth("70%");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		initDetail(klasifikasiSuratKeluar, east);

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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Klasifikasi"));
		row.appendChild(kode = new Textbox(klasifikasiSuratKeluar.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Klasifikasi"));
		row.appendChild(
				nama = new Textbox(klasifikasiSuratKeluar.getNama() == null ? "" : klasifikasiSuratKeluar.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sifat Surat"));
		row.appendChild(sifat = new Combobox());
		String konfigurasiSifat = Common
				.getKonfigurasi("sifat_klasifikasi_surat_keluar", "Segera;Penting;Rahasia;Biasa").getNilai();

		for (String s : konfigurasiSifat.split(";")) {
			MyComboitemConfig comboitem = new MyComboitemConfig(s);
			comboitem.setValue(s);
			sifat.appendChild(comboitem);
		}
		Common.selectComboItem(sifat, klasifikasiSuratKeluar.getSifat());
		sifat.setWidth("90%");
		sifat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(klasifikasiSuratKeluar.getSatuanKerja() == null ? ""
				: klasifikasiSuratKeluar.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", klasifikasiSuratKeluar.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Pengguna"));
		row.appendChild(kodeGrupPengguna = new Textbox(klasifikasiSuratKeluar.getKodeGrupPengguna()));
		kodeGrupPengguna.setWidth("90%");
		kodeGrupPengguna.setRows(2);

		Common.initKeterangan(rows,
				"Masukkan grup pengguna yang bisa menggunakan klasifikasi surat keluar ini, jika grup pengguna yang bisa menggunakan lebih dari satu, berikan tanda semicolon (;). Contoh: am;mhs;dosen;pegawai. Kosongkan jika bisa digunakan semua pengguna.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diperuntukkan"));
		row.appendChild(klasifikasiSuratKeluarUntuk = new Combobox());
		Common.insertCombo(klasifikasiSuratKeluarUntuk, "nama", KlasifikasiSuratKeluarUntuk.class);
		Common.selectComboItem(klasifikasiSuratKeluarUntuk, klasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk());
		klasifikasiSuratKeluarUntuk.setWidth("90%");
		klasifikasiSuratKeluarUntuk.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilkanSemester = new MyCheckboxConfig("Tampilkan Tahun Akademik dan Semester"));
		tampilkanSemester.setChecked(klasifikasiSuratKeluar.getTampilkanSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat"));
		row.appendChild(nomorSurat = new AmbilDataNomorSuratBanbox(tipe));
		nomorSurat.setAttribute("nomorSurat", klasifikasiSuratKeluar.getNomorSurat());
		nomorSurat.setValue(
				klasifikasiSuratKeluar.getNomorSurat() == null ? "" : klasifikasiSuratKeluar.getNomorSurat().getNama());
		nomorSurat.setWidth("90%");
		nomorSurat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Agenda"));
		row.appendChild(nomorAgenda = new AmbilDataNomorSuratBanbox(tipe));
		nomorAgenda.setAttribute("nomorSurat", klasifikasiSuratKeluar.getNomorAgenda());
		nomorAgenda.setValue(klasifikasiSuratKeluar.getNomorAgenda() == null ? ""
				: klasifikasiSuratKeluar.getNomorAgenda().getNama());
		nomorAgenda.setWidth("90%");
		nomorAgenda.setReadonly(true);

		if (klasifikasiSuratKeluar.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			klasifikasiSuratKeluar.setFakultas(tbmuser.ambilFakultas());
		}

		Tbmuser tbmuser1 = Common.getCurrentUser();

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));

		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, klasifikasiSuratKeluar.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", klasifikasiSuratKeluar.getFakultas() == null ? tbmuser.ambilFakultas()
						: klasifikasiSuratKeluar.getFakultas()));

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, klasifikasiSuratKeluar.getJurusan());

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		if (sk != null && klasifikasiSuratKeluar.getYayasan() == null) {
			klasifikasiSuratKeluar.setYayasan(sk.getYayasan());
		}
		if (sk != null && klasifikasiSuratKeluar.getSekolah() == null) {
			klasifikasiSuratKeluar.setSekolah(sk);
		}

		row = new MyFormRow();
		row.setVisible(ya || (sk != null && sk.getId() != null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				klasifikasiSuratKeluar == null || klasifikasiSuratKeluar.getYayasan() == null ? tbmuser1.ambilYayasan()
						: klasifikasiSuratKeluar.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya || (sk != null && sk.getId() != null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				klasifikasiSuratKeluar == null || klasifikasiSuratKeluar.getSekolah() == null ? tbmuser1.ambilSekolah()
						: klasifikasiSuratKeluar.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Persetujuan"));
		row.appendChild(alurPersetujuanSuratKeluar = new AmbilDataAlurPersetujuanSuratKeluarBanbox(false, true, tipe));
		alurPersetujuanSuratKeluar.setAttribute("alurPersetujuanSuratKeluar",
				klasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar());
		alurPersetujuanSuratKeluar.setValue(klasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar() == null ? ""
				: klasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar().toString());
		alurPersetujuanSuratKeluar.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tanpaAlur = new MyCheckboxConfig("Tanpa Alur Persetujuan"));
		tanpaAlur.setChecked(klasifikasiSuratKeluar.getTanpaAlur());

		EventListener eventListenerTanpaAlur = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				alurPersetujuanSuratKeluar.getParent().setVisible(!tanpaAlur.isChecked());

			}
		};

		tanpaAlur.addEventListener("onClick", eventListenerTanpaAlur);
		eventListenerTanpaAlur.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tanpaTemplate = new MyCheckboxConfig("Tanpa Template"));
		tanpaTemplate.setChecked(klasifikasiSuratKeluar.getTanpaTemplate());

		EventListener eventListenerTanpaTemplate = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				east.setVisible(!tanpaTemplate.isChecked());
				addWindow.setWidth(tanpaTemplate.isChecked() ? "400px" : "95%");
			}
		};

		tanpaTemplate.addEventListener("onClick", eventListenerTanpaTemplate);
		eventListenerTanpaTemplate.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perihal Default"));
		row.appendChild(perihalDefault = new Textbox(klasifikasiSuratKeluar.getPerihalDefault()));
		perihalDefault.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(kaitkanDenganSuratLain = new MyCheckboxConfig("Kaitkan Dengan Surat Lain"));
		kaitkanDenganSuratLain.setChecked(klasifikasiSuratKeluar.getTanpaTemplate());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(istilahSuratLainLabel = new ais.ui.util.MyLabelConfig("Istilah Surat Lain"));
		row.appendChild(istilahSuratLain = new Textbox(klasifikasiSuratKeluar.getIstilahSuratLain()));
		istilahSuratLain.setWidth("90%");

		EventListener eventListenerKaitkanDenganSuratLain = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				istilahSuratLainLabel.setVisible(kaitkanDenganSuratLain.isChecked());
				istilahSuratLain.setVisible(kaitkanDenganSuratLain.isChecked());
			}
		};

		kaitkanDenganSuratLain.addEventListener("onClick", eventListenerKaitkanDenganSuratLain);
		eventListenerKaitkanDenganSuratLain.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Biaya"));
		row.appendChild(kodeItemBiaya = new Textbox(klasifikasiSuratKeluar.getKodeItemBiaya()));
		kodeItemBiaya.setWidth("90%");
		kodeItemBiaya.setRows(2);

		Common.initKeterangan(rows,
				"Jika syarat mengikuti seminar harus membayar biaya tertentu, masukkan kode item biaya yang harus dibayar mahasiswa yang mengikuti seminar. Jika item biaya lebih dari satu, pisahkan dengan tanda koma (,), contoh : 502,505,506 dan seterusnya. Dan juga pastikan kode item biaya benar.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(sekaliBayar = new MyCheckboxConfig(
				"Kode item biaya tersebut sekali bayar saja, jadi kalau misalnya mahasiswa membayar di semester 7, tetap bisa mengajukan di semester 8 atau lebih tanpa membayar ulang."));
		sekaliBayar.setChecked(klasifikasiSuratKeluar.getSekaliBayar());

		// private MyCheckboxConfig harusBayarLunasSmtSaatIni;
		// private MyCheckboxConfig harusBayarLunasSmtLalu;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(harusBayarLunasSmtSaatIni = new MyCheckboxConfig("Harus Bayar Lunas Smt Saat Ini"));
		harusBayarLunasSmtSaatIni.setChecked(klasifikasiSuratKeluar.getHarusBayarLunasSmtSaatIni());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(harusBayarLunasSmtLalu = new MyCheckboxConfig("Harus Bayar Lunas Smt Lalu"));
		harusBayarLunasSmtLalu.setChecked(klasifikasiSuratKeluar.getHarusBayarLunasSmtLalu());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(aktifKuliah = new MyCheckboxConfig("Terhubung ke surat keterangan aktif"));
		aktifKuliah.setChecked(klasifikasiSuratKeluar.getAktifKuliah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Boleh dibuat/cetak antara"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		bisaDicetakMulai = new MyDatebox(klasifikasiSuratKeluar.getBisaDicetakMulai());
		bisaDicetakSampai = new MyDatebox(klasifikasiSuratKeluar.getBisaDicetakSampai());
		bisaDicetakMulai.setFormat(Common.dateFormat.get().toPattern());
		bisaDicetakSampai.setFormat(Common.dateFormat.get().toPattern());
		hbox.appendChild(bisaDicetakMulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(bisaDicetakSampai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				klasifikasiSuratKeluar.getKeterangan() == null ? "" : klasifikasiSuratKeluar.getKeterangan()));
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
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Klasifikasi Surat Keluar belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Klasifikasi; (2) isikan nama klasifikasi secara lengkap dan jelas; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nomorSurat.getAttribute("nomorSurat") == null) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Surat belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Nomor Surat; (2) pilih format nomor surat yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (klasifikasiSuratKeluarUntuk.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Peruntukan Klasifikasi Surat Keluar belum dipilih. Langkah yang dapat dilakukan: (1) klik pilihan Peruntukan; (2) pilih peruntukan yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		List<Row> rowsParameter = gridParemeter.getRows().getChildren();
		for (Row row : rowsParameter) {
			KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter = (KlasifikasiSuratKeluarParemeter) row
					.getAttribute("klasifikasiSuratKeluarParemeter");
			if (klasifikasiSuratKeluarParemeter == null) {
				MyMessageboxConfig.show("Mohon maaf, terdapat baris Parameter yang belum diisi. Langkah yang dapat dilakukan: (1) periksa daftar parameter pada tabel; (2) hapus baris yang kosong atau isikan nilai parameter yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (klasifikasiSuratKeluar.getId() != null) {
			klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) session.load(KlasifikasiSuratKeluar.class,
					klasifikasiSuratKeluar.getId());

		}
		klasifikasiSuratKeluar.setTanpaAlur(tanpaAlur.isChecked());
		klasifikasiSuratKeluar.setTanpaTemplate(tanpaTemplate.isChecked());
		klasifikasiSuratKeluar
				.setSifat((String) (sifat.getSelectedItem() == null ? null : sifat.getSelectedItem().getValue()));

		klasifikasiSuratKeluar.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		klasifikasiSuratKeluar.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		klasifikasiSuratKeluar.setAlurPersetujuanSuratKeluar(
				(AlurPersetujuanSuratKeluar) alurPersetujuanSuratKeluar.getAttribute("alurPersetujuanSuratKeluar"));
		klasifikasiSuratKeluar.setKlasifikasiSuratKeluarUntuk(
				(KlasifikasiSuratKeluarUntuk) klasifikasiSuratKeluarUntuk.getSelectedItem().getValue());
		klasifikasiSuratKeluar.setNomorSurat((NomorSurat) nomorSurat.getAttribute("nomorSurat"));

		klasifikasiSuratKeluar.setNomorAgenda((NomorSurat) nomorAgenda.getAttribute("nomorSurat"));

		klasifikasiSuratKeluar.setNama(nama.getValue());
		klasifikasiSuratKeluar.setKode(kode.getValue());
		klasifikasiSuratKeluar.setKeterangan(keterangan.getValue());
		klasifikasiSuratKeluar.setKodeGrupPengguna(kodeGrupPengguna.getValue().trim());
		// klasifikasiSuratKeluar.setTemplate(template.getValue());

		klasifikasiSuratKeluar.setSekaliBayar(sekaliBayar.isChecked());
		klasifikasiSuratKeluar.setKodeItemBiaya(kodeItemBiaya.getValue().trim());
		klasifikasiSuratKeluar.setHarusBayarLunasSmtLalu(harusBayarLunasSmtLalu.isChecked());
		klasifikasiSuratKeluar.setHarusBayarLunasSmtSaatIni(harusBayarLunasSmtSaatIni.isChecked());

		klasifikasiSuratKeluar.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		klasifikasiSuratKeluar.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		klasifikasiSuratKeluar.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		klasifikasiSuratKeluar.setPerihalDefault(perihalDefault.getValue().trim());

		klasifikasiSuratKeluar.setAktifKuliah(aktifKuliah.isChecked());

		klasifikasiSuratKeluar.setTampilkanSemester(tampilkanSemester.isChecked());

		klasifikasiSuratKeluar.setTipe(tipe);
		klasifikasiSuratKeluar.setKaitkanDenganSuratLain(kaitkanDenganSuratLain.isChecked());
		klasifikasiSuratKeluar.setIstilahSuratLain(istilahSuratLain.getValue().trim());

		klasifikasiSuratKeluar.setBisaDicetakMulai(bisaDicetakMulai.getValue());
		klasifikasiSuratKeluar.setBisaDicetakSampai(bisaDicetakSampai.getValue());

		if (klasifikasiSuratKeluar.getId() != null) {
			Common.refreshUpdate(session, klasifikasiSuratKeluar);
		} else {
			session.save(klasifikasiSuratKeluar);
		}

		if (klasifikasiSuratKeluar.getCopyDari() != null) {
			for (Row row : rowsParameter) {
				KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter = (KlasifikasiSuratKeluarParemeter) row
						.getAttribute("klasifikasiSuratKeluarParemeter");
				if (klasifikasiSuratKeluarParemeter != null) {
					klasifikasiSuratKeluarParemeter.setId(null);
					klasifikasiSuratKeluarParemeter.setKlasifikasiSuratKeluar(klasifikasiSuratKeluar);
					session.save(klasifikasiSuratKeluarParemeter);
				}
			}
		} else {
			for (Row row : rowsParameter) {
				KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter = (KlasifikasiSuratKeluarParemeter) row
						.getAttribute("klasifikasiSuratKeluarParemeter");
				if (klasifikasiSuratKeluarParemeter != null) {
					klasifikasiSuratKeluarParemeter.setKlasifikasiSuratKeluar(klasifikasiSuratKeluar);
					Common.refreshSaveOrUpdate(session, klasifikasiSuratKeluarParemeter);
				}
			}
		}
		return true;
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KlasifikasiSuratKeluar.class)

				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		if (searchkelompokNomorSurat != null && searchkelompokNomorSurat.getSelectedItem() != null
				&& searchkelompokNomorSurat.getSelectedItem().getValue() != null) {
			criteria.createAlias("nomorSurat", "nomorSurat").add(Restrictions.eq("nomorSurat.kelompokNomorSurat",
					searchkelompokNomorSurat.getSelectedItem().getValue()));
		}

		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		List<KlasifikasiSuratKeluar> klasifikasiSuratKeluar = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				KlasifikasiSuratKeluar.class);
		ListModel strset = new SimpleListModel(klasifikasiSuratKeluar);
		grid.setRowRenderer(new KlasifikasiSuratKeluarRenderer());
		grid.setModelCheckMobile(strset);

	}

}
