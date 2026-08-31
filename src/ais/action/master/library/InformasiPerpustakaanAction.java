package ais.action.master.library;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.InformasiPerpustakaanPunyaFotoHelper;
import ais.action.master.library.helper.InformasiPerpustakaanPunyaKomentarHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.InformasiPerpustakaanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoInformasiPerpustakaan;
import ais.database.model.library.InformasiPerpustakaan;
import ais.database.model.library.JenisInformasiPerpustakaan;
import ais.database.model.library.Perpustakaan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk informasi perpustakaan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code AmbilDataSatuanKerjaBanbox satuanKerja},
 * {@code AmbilDataPerpustakaanBanbox perpustakaan}, {@code Combobox jenisInformasiPerpustakaan}, {@code
 * MyDatebox mulai}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code
 * initDetail()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap
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
public class InformasiPerpustakaanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Combobox jenisInformasiPerpustakaan;

	private MyDatebox mulai;
	private MyDatebox sampai;
	private MyCkEditor content = new MyCkEditor();

	private boolean edit = false;
	private boolean delete = false;

	private InformasiPerpustakaan informasiPerpustakaan;
	private MyToolbarbuttonConfig add;
	private MyGrid gridDocument;
	@SuppressWarnings("unused")
	private MyGrid gridKomentar;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		@SuppressWarnings("unused")
		JenisInformasiPerpustakaan informasiPerpustakaan = LibraryUtil.INFORMASI;

		add.setVisible(CommonPrivilages
				.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link InformasiPerpustakaanAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link InformasiPerpustakaanAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see InformasiPerpustakaanAction
	 */
	class InformasiPerpustakaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final InformasiPerpustakaan informasiPerpustakaan = (InformasiPerpustakaan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("450px");
						window.setWidth("100%");
						window.setParent(detail);
						initDetail(informasiPerpustakaan, window);
					}
				}
			});

			RevisiHelper
					.createNewRevisi(
							InformasiPerpustakaan.class,
							informasiPerpustakaan,
							informasiPerpustakaan.getSatuanKerja() == null ? ""
									: informasiPerpustakaan.getSatuanKerja()
											.toString()).setParent(arg0);

			new Label(informasiPerpustakaan.getPerpustakaan() == null ? ""
					: informasiPerpustakaan.getPerpustakaan().getNama())
					.setParent(arg0);

			new Label(
					informasiPerpustakaan.getJenisInformasiPerpustakaan() == null ? ""
							: informasiPerpustakaan
									.getJenisInformasiPerpustakaan().getNama())
					.setParent(arg0);

			new Label(informasiPerpustakaan.getMulai() == null ? ""
					: Common.dateFormat6.get().format(informasiPerpustakaan
							.getMulai())).setParent(arg0);

			new Label(informasiPerpustakaan.getSampai() == null ? ""
					: Common.dateFormat6.get().format(informasiPerpustakaan
							.getSampai())).setParent(arg0);

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
					+ informasiPerpustakaan.getContent() + "</font>")
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(informasiPerpustakaan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											

											Common.refreshDelete(informasiPerpustakaan);
											
											
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new InformasiPerpustakaan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(
			final InformasiPerpustakaan informasiPerpustakaan,
			Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabInformasi = new MyTabConfig("Isi Informasi");
		tabInformasi.setParent(tabs);

		final MyTabConfig tabDocument = new MyTabConfig("File Lampiran");
		tabDocument.setParent(tabs);

		final MyTabConfig tabKomentar = new MyTabConfig("Komentar-Komentar");
		tabKomentar.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		content.setValue(informasiPerpustakaan.getContent());
		content.setHeight("100%");
		content.setWidth("100%");

		final Tabpanel tabpanelInformasi = new ais.ui.util.MyTabpanel();
		tabpanelInformasi.setParent(tabpanels);
		tabpanelInformasi.appendChild(content);

		final Tabpanel tabpanelDocument = new ais.ui.util.MyTabpanel();
		tabpanelDocument.setParent(tabpanels);

		final Tabpanel tabpanelKomentar = new ais.ui.util.MyTabpanel();
		tabpanelKomentar.setParent(tabpanels);

		tabpanelDocument.appendChild(new InformasiPerpustakaanPunyaFotoHelper(
				gridDocument = new MyGrid()).initDetail(informasiPerpustakaan));

		tabpanelKomentar
				.appendChild(new InformasiPerpustakaanPunyaKomentarHelper(
						gridKomentar = new MyGrid())
						.initDetail(informasiPerpustakaan));
		//
		// tabpanelBarcode.appendChild(new
		// InformasiPerpustakaanPunyaBarcodeHelper(
		// gridBarcode = new MyGrid()).initDetail(informasiPerpustakaan));

	}

	private void init(InformasiPerpustakaan informasiPerpustakaan)
			throws Exception {
		this.informasiPerpustakaan = informasiPerpustakaan;
		addWindow.setTitle(informasiPerpustakaan.getId() == null ? "Tambah Informasi Perpustakaan" : "Ubah Informasi Perpustakaan");
		Common.clear(addWindow);

		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setAttribute("satuanKerja",
				informasiPerpustakaan.getSatuanKerja());
		satuanKerja
				.setValue(informasiPerpustakaan.getSatuanKerja() == null ? ""
						: informasiPerpustakaan.getSatuanKerja().toString());

		perpustakaan = new AmbilDataPerpustakaanBanbox();
		perpustakaan.setAttribute("perpustakaan",
				informasiPerpustakaan.getPerpustakaan());
		perpustakaan
				.setValue(informasiPerpustakaan.getPerpustakaan() == null ? ""
						: informasiPerpustakaan.getPerpustakaan().getNama());

		mulai = new MyDatebox(informasiPerpustakaan.getMulai());
		sampai = new MyDatebox(informasiPerpustakaan.getSampai());
		mulai.setFormat(Common.dateFormat6.get().toPattern());
		sampai.setFormat(Common.dateFormat6.get().toPattern());

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja
						.getAttribute("satuanKerja");
				perpustakaan.setSatuanKerja(mySatuanKerja);
			}
		};
		satuanKerja.setEventListener(myEventListener);
		myEventListener.onEvent(null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setWidth("70%");
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setParent(borderlayout);
		initDetail(informasiPerpustakaan, east);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai);
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai);
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Informasi"));
		row.appendChild(jenisInformasiPerpustakaan = new Combobox());
		Common.insertCombo(jenisInformasiPerpustakaan, "nama",
				JenisInformasiPerpustakaan.class);
		Common.selectComboItem(jenisInformasiPerpustakaan,
				informasiPerpustakaan.getJenisInformasiPerpustakaan());
		jenisInformasiPerpustakaan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan);
		perpustakaan.setWidth("90%");

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
		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Mulai terbit harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jenisInformasiPerpustakaan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis informasi harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (content.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Content harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan kerja harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (perpustakaan.getAttribute("perpustakaan") == null) {
			MyMessageboxConfig.show("Perpustakaan harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsDocument = gridDocument.getRows().getChildren();
		for (Row row : rowsDocument) {
			FotoInformasiPerpustakaan fotoInformasiPerpustakaan = (FotoInformasiPerpustakaan) row
					.getAttribute("fotoInformasiPerpustakaan");
			if (fotoInformasiPerpustakaan.getInformasiPerpustakaan() == null) {
				MyMessageboxConfig.show("File harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		InformasiPerpustakaanDao informasiPerpustakaanDao = DaoFactory
				.getInstance().getInformasiPerpustakaanDao();
		if (informasiPerpustakaan.getId() != null) {
			informasiPerpustakaan = informasiPerpustakaanDao
					.load(informasiPerpustakaan.getId());

		}

		informasiPerpustakaan
				.setJenisInformasiPerpustakaan((JenisInformasiPerpustakaan) jenisInformasiPerpustakaan
						.getSelectedItem().getValue());
		informasiPerpustakaan.setMulai(mulai.getValue());
		informasiPerpustakaan.setSampai(sampai.getValue());
		informasiPerpustakaan.setSatuanKerja((SatuanKerja) satuanKerja
				.getAttribute("satuanKerja"));

		informasiPerpustakaan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
		informasiPerpustakaan.setPerpustakaan((Perpustakaan) perpustakaan
				.getAttribute("perpustakaan"));
		informasiPerpustakaan.setContent(content.getValue());

		if (informasiPerpustakaan.getId() != null) {
			informasiPerpustakaanDao.update(informasiPerpustakaan);
		} else {
			informasiPerpustakaanDao.save(informasiPerpustakaan);
		}

		Session mysession = StreamingHibernateUtil.getInstance()
				.currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsDocument) {
				FotoInformasiPerpustakaan fotoInformasiPerpustakaan = (FotoInformasiPerpustakaan) row
						.getAttribute("fotoInformasiPerpustakaan");
				fotoInformasiPerpustakaan
						.setInformasiPerpustakaan(informasiPerpustakaan.getId());
				mysession.saveOrUpdate(fotoInformasiPerpustakaan);
			}
			mysession.getTransaction().commit();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e); 
		}

		StreamingHibernateUtil.getInstance().closeSession();

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(InformasiPerpustakaan.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(Restrictions.ilike("content", searchnama.getValue(),
				MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<InformasiPerpustakaan> informasiPerpustakaan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(informasiPerpustakaan);
		grid.setRowRenderer(new InformasiPerpustakaanRenderer());
		grid.setModelCheckMobile(strset);

		

	}

}
