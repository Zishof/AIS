package ais.action.master.surat;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.util.List;
import java.util.Set;

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
import ais.action.master.surat.helper.AmbilDataAlurPersetujuanSuratMasukBanbox;
import ais.action.master.surat.helper.AmbilDataNomorSuratBanbox;
import ais.action.master.surat.helper.KlasifikasiSuratMasukParameterHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.surat.KlasifikasiSuratMasukDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.database.model.surat.KlasifikasiSuratMasuk;
import ais.database.model.surat.KlasifikasiSuratMasukParemeter;
import ais.database.model.surat.MasaBerlakuSurat;
import ais.database.model.surat.NomorSurat;
import ais.database.model.surat.SifatSurat;
import ais.database.model.surat.StatusDipertahankan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk klasifikasi surat masuk. Tipe ini merupakan titik masuk UI yang
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
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initDetail()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()});
 * pelaporan/ekspor ({@code generateReport()}); operasi domain lain ({@code onMasaBerlaku()}, {@code
 * onStatusDipertahankan()}, {@code onSifatSurat()}, {@code layoutDisposisi()}, {@code onAdd()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KlasifikasiSuratMasukAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

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

	private AmbilDataSatuanKerjaBanbox searchparent;

	private Textbox kode;
	private Textbox nama;
	private AmbilDataAlurPersetujuanSuratMasukBanbox alurPersetujuanSuratMasuk;
//	private Combobox sifat;
	private Textbox prefix;
	private Textbox postfix;
	private Combobox fakultas;
	private Combobox jurusan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KlasifikasiSuratMasuk klasifikasiSuratMasuk;
	private MyToolbarbuttonConfig add;
	private MyGrid gridParemeter;
	private Textbox kodeGrupPengguna;
	private AmbilDataNomorSuratBanbox nomorSurat;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox yayasan;
	private Combobox sekolah;

	private Row hbFakultasLabel;
	private Row hbYayasan;

	private boolean pt = false;
	private boolean ya = false;
	private Textbox perihalDefault;
	private Sekolah sk;
	private MyCheckboxConfig tanpaAlur;

	private Tabpanel manajemenMasaBerlaku;
	private Combobox masaBerlakuSurat;
	private Combobox sifatSurat;

	private Combobox statusDipertahankan;

	public void onMasaBerlaku(Event event) {
		if (manajemenMasaBerlaku.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenMasaBerlaku);
			MyInclude iframe = new MyInclude("/pages/master/surat/masa_berlaku_surat.zul");
			iframe.setParent(window);
		}

	}

	private Tabpanel manajemenStatusDipertahankan;

	public void onStatusDipertahankan(Event event) {
		if (manajemenStatusDipertahankan.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenStatusDipertahankan);
			MyInclude iframe = new MyInclude("/pages/master/surat/status_dipertahankan.zul");
			iframe.setParent(window);
		}

	}

	private String tipe = "surat";
	private Tabpanel manajemenSifatSurat;
	private MyCheckboxConfig bolehDipinjam;
	private MyIntbox maksimalHariPinjam;
	private MyIntbox maksimalJumlahPerpanjaangan;

	public void onSifatSurat(Event event) {
		if (manajemenSifatSurat.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenSifatSurat);
			MyInclude iframe = new MyInclude("/pages/master/surat/sifat_surat.zul");
			iframe.setParent(window);
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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "sifatSurat", "alurPersetujuanSuratMasuk", "fakultas",
				"jurusan", "yayasan", "sekolah", "satuanKerja", "aktif", "perihalDefault", "kodeGrupPengguna",
				"tanpaAlur", "masaBerlakuSurat", "statusDipertahankan", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KlasifikasiSuratMasuk.class, contents);
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

	/**
	 * Renderer lokal untuk layar/komponen {@link KlasifikasiSuratMasukAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KlasifikasiSuratMasukAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KlasifikasiSuratMasukAction
	 */
	class KlasifikasiSuratMasukRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KlasifikasiSuratMasuk klasifikasiSuratMasuk = (KlasifikasiSuratMasuk) arg1;

			if (klasifikasiSuratMasuk.getTipe() == null) {
				klasifikasiSuratMasuk.setTipe(tipe);
				Common.refreshUpdate(klasifikasiSuratMasuk);
			}

			new Label(klasifikasiSuratMasuk.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(KlasifikasiSuratMasuk.class, klasifikasiSuratMasuk,
					klasifikasiSuratMasuk.getNama()).setParent(arg0);
			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			new Label(klasifikasiSuratMasuk.getNomorSurat() == null ? ""
					: klasifikasiSuratMasuk.getNomorSurat().getContohFormat()).setParent(hbox);
			new Label(klasifikasiSuratMasuk.getPrefix()).setParent(hbox);
			new Label(klasifikasiSuratMasuk.getPostfix()).setParent(hbox);
			new Label(klasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk() == null ? ""
					: klasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk().toString()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(klasifikasiSuratMasuk.getSifatSurat() == null ? ""
					: klasifikasiSuratMasuk.getSifatSurat().getNama()).setParent(vbox);
			vbox.appendChild(new Label(klasifikasiSuratMasuk.getStatusDipertahankan() == null ? ""
					: klasifikasiSuratMasuk.getStatusDipertahankan().getNama()));

			vbox = new Vbox();
			vbox.setParent(arg0);
			vbox.appendChild(new Label(klasifikasiSuratMasuk.getSatuanKerja() == null ? ""
					: klasifikasiSuratMasuk.getSatuanKerja().getNama()));

			vbox.appendChild(new Label(klasifikasiSuratMasuk.getMasaBerlakuSurat() == null ? ""
					: klasifikasiSuratMasuk.getMasaBerlakuSurat().getNama()));

			hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(klasifikasiSuratMasuk.getFakultas() == null ? "" : klasifikasiSuratMasuk.getFakultas().getNama())
					.setParent(hbox);
			new Label(klasifikasiSuratMasuk.getJurusan() == null ? "" : klasifikasiSuratMasuk.getJurusan().getNama())
					.setParent(hbox);

			hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(klasifikasiSuratMasuk.getYayasan() == null ? "" : klasifikasiSuratMasuk.getYayasan().getNama())
					.setParent(hbox);
			new Label(klasifikasiSuratMasuk.getSekolah() == null ? "" : klasifikasiSuratMasuk.getSekolah().getNama())
					.setParent(hbox);

			vbox = new Vbox();
			String[] spl = klasifikasiSuratMasuk.getKodeGrupPengguna().split(";");
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

			new Label(klasifikasiSuratMasuk.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(klasifikasiSuratMasuk.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					klasifikasiSuratMasuk.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(klasifikasiSuratMasuk);
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(klasifikasiSuratMasuk);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(klasifikasiSuratMasuk);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	protected void initDetail(final KlasifikasiSuratMasuk klasifikasiSuratMasuk, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabParameter = new MyTabConfig("Parameter");
		tabParameter.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelParameter = new ais.ui.util.MyTabpanel();
		tabpanelParameter.setParent(tabpanels);

		MyTabConfig tabLayoutDisposisi = new MyTabConfig("Layout Disposisi");
		tabLayoutDisposisi.setParent(tabs);

		tabpanelParameter.appendChild(new KlasifikasiSuratMasukParameterHelper(gridParemeter = new MyGrid())
				.initDetail(klasifikasiSuratMasuk));

		Tabpanel tabpanelLayoutDisposisi = new ais.ui.util.MyTabpanel();
		tabpanelLayoutDisposisi.setParent(tabpanels);

		layoutDisposisi(tabLayoutDisposisi, tabpanelLayoutDisposisi);
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
		LampiranLain.createDownloadUploadFileLain(hbox, klasifikasiSuratMasuk.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI_MASUK, "Lampiran *.jrxml / *.jasper", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (onSave(arg0)) {
							LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();

							if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
								try {
									Session session = StreamingHibernateUtil.getInstance().currentSession();

									session.refresh(lainMahasiswa);
									lainMahasiswa
											.setRef(KlasifikasiSuratMasukAction.this.klasifikasiSuratMasuk.getId());

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

		tabLayout.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (KlasifikasiSuratMasukAction.this.klasifikasiSuratMasuk.getId() != null) {

					LampiranLain lainMahasiswa = LampiranLain.ambil(
							KlasifikasiSuratMasukAction.this.klasifikasiSuratMasuk.getId(),
							LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI_MASUK, true);

					generateReport(center, lainMahasiswa);
				}

			}
		});
	}

	private void generateReport(Center center, LampiranLain lainMahasiswa) {
		Common.clear(center);
		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {

				File file = Report.generateCompileFileReport(Report.PDF,
						ais.common.HashMapGenerator.getRandStringObject(), lainMahasiswa.ambilFile().getAbsolutePath(),
						ais.ui.util.WaktuUtil.getDate(), false);
				CommonReport.tampilkanReportPDF(center, file);

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new KlasifikasiSuratMasuk());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KlasifikasiSuratMasuk klasifikasiSuratMasuk) throws Exception {
		this.klasifikasiSuratMasuk = klasifikasiSuratMasuk;

		Tbmuser tbmuser = Common.getCurrentUser();
		if (klasifikasiSuratMasuk.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			klasifikasiSuratMasuk.setFakultas(tbmuser.ambilFakultas());
		}

		if (klasifikasiSuratMasuk.getSatuanKerja() == null && tbmuser.ambilSatuanKerja() != null) {
			klasifikasiSuratMasuk.setSatuanKerja(tbmuser.ambilSatuanKerja());
		}

		if (klasifikasiSuratMasuk.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			klasifikasiSuratMasuk.setJurusan(tbmuser.ambilJurusan());
		}

		if (klasifikasiSuratMasuk.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			klasifikasiSuratMasuk.setYayasan(tbmuser.ambilYayasan());
		}

		if (klasifikasiSuratMasuk.getSekolah() == null && tbmuser.ambilSekolah() != null) {
			klasifikasiSuratMasuk.setSekolah(tbmuser.ambilSekolah());
		}

		addWindow.setTitle(klasifikasiSuratMasuk.getId() == null ? "Tambah Klasifikasi Surat Masuk" : "Ubah Klasifikasi Surat Masuk");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setWidth("70%");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		initDetail(klasifikasiSuratMasuk, east);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(klasifikasiSuratMasuk.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Klasifikasi Surat Masuk *"));
		row.appendChild(
				nama = new Textbox(klasifikasiSuratMasuk.getNama() == null ? "" : klasifikasiSuratMasuk.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sifat Surat"));
		Common.insertComboDanSemua(sifatSurat = new Combobox(), new String[] { "nama" }, "keterangan", SifatSurat.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.selectComboItem(sifatSurat, klasifikasiSuratMasuk.getSifatSurat());
		row.appendChild(sifatSurat);
		sifatSurat.setWidth("90%");

//		row.appendChild(sifat = new Combobox());
//		String konfigurasiSifat = Common.getKonfigurasi("sifat_klasifikasi_surat_masuk", "Segera;Penting;Rahasia;Biasa")
//				.getNilai();
//
//		for (String s : konfigurasiSifat.split(";")) {
//			MyComboitemConfig comboitem = new MyComboitemConfig(s);
//			comboitem.setValue(s);
//			sifat.appendChild(comboitem);
//		}
//		Common.selectComboItem(sifat, klasifikasiSuratMasuk.getSifat());
//		sifat.setWidth("90%");
//		sifat.setReadonly(true);

		if (klasifikasiSuratMasuk.getId() == null && klasifikasiSuratMasuk.getSatuanKerja() == null) {
			klasifikasiSuratMasuk.setSatuanKerja(Common.getSatuanKerja());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false);
		satuanKerja.setValue(
				klasifikasiSuratMasuk.getSatuanKerja() == null ? "" : klasifikasiSuratMasuk.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", klasifikasiSuratMasuk.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Pengguna"));
		row.appendChild(kodeGrupPengguna = new Textbox(klasifikasiSuratMasuk.getKodeGrupPengguna()));
		kodeGrupPengguna.setWidth("90%");
		kodeGrupPengguna.setRows(2);

		Common.initKeterangan(rows,
				"Masukkan grup pengguna yang bisa menggunakan klasifikasi surat masuk ini, jika grup pengguna yang bisa menggunakan lebih dari satu, berikan tanda semicolon (;). Contoh: am;mhs;dosen;pegawai. Kosongkan jika bisa digunakan semua pengguna.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gunakan Nomor Surat sebagai Nomor Agenda"));
		row.appendChild(nomorSurat = new AmbilDataNomorSuratBanbox(tipe));
		nomorSurat.setAttribute("nomorSurat", klasifikasiSuratMasuk.getNomorSurat());
		nomorSurat.setValue(
				klasifikasiSuratMasuk.getNomorSurat() == null ? "" : klasifikasiSuratMasuk.getNomorSurat().getNama());
		nomorSurat.setWidth("90%");
		nomorSurat.setReadonly(true);

		Common.initKeterangan(rows,
				"Format penomoran surat dapat digunakan sebagai nomor agenda surat, jika ingin menggunakan nomor agenda dari format nomor surat, pilih salah satu.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prefix Nomor Agenda"));
		row.appendChild(prefix = new Textbox(klasifikasiSuratMasuk.getPrefix()));
		prefix.setWidth("90%");

		Common.initKeterangan(rows,
				"Prefix ini digunakan untuk awalan nomor agenda surat masuk. Jika format nomor surat dipilih, prefix tidak berfungsi.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Postfix Nomor Agenda"));
		row.appendChild(postfix = new Textbox(klasifikasiSuratMasuk.getPostfix()));
		postfix.setWidth("90%");

		Common.initKeterangan(rows,
				"Postfix ini digunakan untuk akhiran nomor agenda surat masuk. Jika format nomor surat dipilih, postfix tidak berfungsi.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Disposisi *"));
		row.appendChild(alurPersetujuanSuratMasuk = new AmbilDataAlurPersetujuanSuratMasukBanbox(false, true, tipe));
		alurPersetujuanSuratMasuk.setAttribute("alurPersetujuanSuratMasuk",
				klasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk());
		alurPersetujuanSuratMasuk.setValue(klasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk() == null ? ""
				: klasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk().toString());
		alurPersetujuanSuratMasuk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tanpaAlur = new MyCheckboxConfig("Tanpa Alur Disposisi"));
		tanpaAlur.setChecked(klasifikasiSuratMasuk.getTanpaAlur());

		EventListener eventListenerTanpaAlur = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				alurPersetujuanSuratMasuk.getParent().setVisible(!tanpaAlur.isChecked());

			}
		};

		tanpaAlur.addEventListener("onClick", eventListenerTanpaAlur);
		eventListenerTanpaAlur.onEvent(null);

		if (klasifikasiSuratMasuk.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			klasifikasiSuratMasuk.setFakultas(tbmuser.ambilFakultas());
		}

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), searchfakultas,
				searchjurusan);
		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, klasifikasiSuratMasuk.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", klasifikasiSuratMasuk.getFakultas() == null ? tbmuser.ambilFakultas()
						: klasifikasiSuratMasuk.getFakultas()));

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, klasifikasiSuratMasuk.getJurusan());

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Tbmuser tbmuser1 = Common.getCurrentUser();

		if (sk != null && klasifikasiSuratMasuk.getYayasan() == null) {
			klasifikasiSuratMasuk.setYayasan(sk.getYayasan());
		}
		if (sk != null && klasifikasiSuratMasuk.getSekolah() == null) {
			klasifikasiSuratMasuk.setSekolah(sk);
		}

		row = new MyFormRow();
		row.setVisible(ya || (sk != null && sk.getId() != null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				klasifikasiSuratMasuk == null || klasifikasiSuratMasuk.getYayasan() == null ? tbmuser1.ambilYayasan()
						: klasifikasiSuratMasuk.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Berlaku Surat"));
		Common.insertComboDanSemua(masaBerlakuSurat = new Combobox(), new String[] { "nama", "kode" }, "keterangan",
				MasaBerlakuSurat.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.selectComboItem(masaBerlakuSurat, klasifikasiSuratMasuk.getMasaBerlakuSurat());
		row.appendChild(masaBerlakuSurat);
		masaBerlakuSurat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Dipertahankan"));
		Common.insertComboDanSemua(statusDipertahankan = new Combobox(), new String[] { "nama", "kode" }, "keterangan",
				StatusDipertahankan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.selectComboItem(statusDipertahankan, klasifikasiSuratMasuk.getStatusDipertahankan());
		row.appendChild(statusDipertahankan);
		statusDipertahankan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya || (sk != null && sk.getId() != null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				klasifikasiSuratMasuk == null || klasifikasiSuratMasuk.getSekolah() == null ? tbmuser1.ambilSekolah()
						: klasifikasiSuratMasuk.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perihal Default"));
		row.appendChild(perihalDefault = new Textbox(klasifikasiSuratMasuk.getPerihalDefault()));
		perihalDefault.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(bolehDipinjam = new MyCheckboxConfig(tipe + " boleh dipinjam"));
		bolehDipinjam.setChecked(klasifikasiSuratMasuk.getBolehDipinjam());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Batas waktu peminjaman " + tipe));
		row.appendChild(maksimalHariPinjam = new MyIntbox(klasifikasiSuratMasuk.getMaksimalHariPinjam()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Batas perpanjangan peminjaman " + tipe));
		row.appendChild(
				maksimalJumlahPerpanjaangan = new MyIntbox(klasifikasiSuratMasuk.getMaksimalJumlahPerpanjaangan()));

		EventListener eventListenerBolehDipinjam = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				maksimalHariPinjam.getParent().setVisible(bolehDipinjam.isChecked());
				maksimalJumlahPerpanjaangan.getParent().setVisible(bolehDipinjam.isChecked());
			}
		};

		bolehDipinjam.addEventListener("onClick", eventListenerBolehDipinjam);
		eventListenerBolehDipinjam.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				klasifikasiSuratMasuk.getKeterangan() == null ? "" : klasifikasiSuratMasuk.getKeterangan()));
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
			MyMessageboxConfig.show("Mohon maaf, Nama Klasifikasi Surat Masuk belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Klasifikasi; (2) isikan nama klasifikasi secara lengkap dan jelas; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (!tanpaAlur.isChecked() && alurPersetujuanSuratMasuk.getAttribute("alurPersetujuanSuratMasuk") == null) {
			MyMessageboxConfig.show("Mohon maaf, Alur Disposisi Klasifikasi Surat Masuk belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Alur Disposisi; (2) pilih alur disposisi yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		List<Row> rowsParameter = gridParemeter.getRows().getChildren();
		for (Row row : rowsParameter) {
			KlasifikasiSuratMasukParemeter klasifikasiSuratMasukParemeter = (KlasifikasiSuratMasukParemeter) row
					.getAttribute("klasifikasiSuratMasukParemeter");
			if (klasifikasiSuratMasukParemeter == null) {
				MyMessageboxConfig.show("Mohon maaf, terdapat baris Parameter yang belum diisi. Langkah yang dapat dilakukan: (1) periksa daftar parameter pada tabel; (2) hapus baris yang kosong atau isikan nilai parameter yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		KlasifikasiSuratMasukDao klasifikasiSuratMasukDao = DaoFactory.getInstance().getKlasifikasiSuratMasukDao();
		if (klasifikasiSuratMasuk.getId() != null) {
			klasifikasiSuratMasuk = klasifikasiSuratMasukDao.load(klasifikasiSuratMasuk.getId());

		}
		klasifikasiSuratMasuk.setTanpaAlur(tanpaAlur.isChecked());

		klasifikasiSuratMasuk.setSifatSurat(
				(SifatSurat) (sifatSurat.getSelectedItem() == null ? null : sifatSurat.getSelectedItem().getValue()));

		klasifikasiSuratMasuk.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		klasifikasiSuratMasuk.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		klasifikasiSuratMasuk.setAlurPersetujuanSuratMasuk(
				(AlurPersetujuanSuratMasuk) alurPersetujuanSuratMasuk.getAttribute("alurPersetujuanSuratMasuk"));
		klasifikasiSuratMasuk.setPostfix(postfix.getValue().trim());
		klasifikasiSuratMasuk.setPrefix(prefix.getValue().trim());
		klasifikasiSuratMasuk.setKode(kode.getValue());
		klasifikasiSuratMasuk.setNama(nama.getValue());
		klasifikasiSuratMasuk.setKeterangan(keterangan.getValue());
		klasifikasiSuratMasuk.setKodeGrupPengguna(kodeGrupPengguna.getValue().trim());
		klasifikasiSuratMasuk.setNomorSurat((NomorSurat) nomorSurat.getAttribute("nomorSurat"));
		klasifikasiSuratMasuk.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		klasifikasiSuratMasuk.setPerihalDefault(perihalDefault.getValue().trim());

		klasifikasiSuratMasuk.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		klasifikasiSuratMasuk.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		klasifikasiSuratMasuk.setMasaBerlakuSurat((MasaBerlakuSurat) (masaBerlakuSurat.getSelectedItem() == null ? null
				: masaBerlakuSurat.getSelectedItem().getValue()));

		klasifikasiSuratMasuk
				.setStatusDipertahankan((StatusDipertahankan) (statusDipertahankan.getSelectedItem() == null ? null
						: statusDipertahankan.getSelectedItem().getValue()));

		klasifikasiSuratMasuk.setTipe(tipe);

		klasifikasiSuratMasuk.setBolehDipinjam(bolehDipinjam.isChecked());
		klasifikasiSuratMasuk.setMaksimalHariPinjam(maksimalHariPinjam.getValue());
		klasifikasiSuratMasuk.setMaksimalJumlahPerpanjaangan(maksimalJumlahPerpanjaangan.getValue());

		if (klasifikasiSuratMasuk.getId() != null) {
			Common.refreshUpdate(klasifikasiSuratMasuk);
		} else {
			klasifikasiSuratMasukDao.save(klasifikasiSuratMasuk);
		}

		Session session = klasifikasiSuratMasukDao.getCurrentSession();
		for (Row row : rowsParameter) {
			KlasifikasiSuratMasukParemeter klasifikasiSuratMasukParemeter = (KlasifikasiSuratMasukParemeter) row
					.getAttribute("klasifikasiSuratMasukParemeter");
			if (klasifikasiSuratMasukParemeter != null) {
				klasifikasiSuratMasukParemeter.setKlasifikasiSuratMasuk(klasifikasiSuratMasuk);
				Common.refreshUpdate(session, klasifikasiSuratMasukParemeter);
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
		Criteria criteria = session.createCriteria(KlasifikasiSuratMasuk.class)

				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

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

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KlasifikasiSuratMasuk> klasifikasiSuratMasuk = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				KlasifikasiSuratMasuk.class);
		ListModel strset = new SimpleListModel(klasifikasiSuratMasuk);
		grid.setRowRenderer(new KlasifikasiSuratMasukRenderer());
		grid.setModelCheckMobile(strset);

	}

}
