package ais.action.master.rab;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import ais.ui.util.MyTabConfig;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AcaraPunyaIndikatorHelper;
import ais.action.master.rab.helper.AcaraPunyaJenisParameterHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.rab.util.WorkspaceSelecter;
import ais.action.report.format1.rab.LaporanRealisasiProgramBulananWorkspace;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.AcaraDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Acara;
import ais.database.model.rab.AcaraPunyaIndikator;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyDatebox;

/**
 * Controller/action ZK untuk acara. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code AmbilDataSatuanKerjaBanbox searchparent},
 * {@code Textbox nama}, {@code MyDatebox ppbegin}, {@code MyDatebox ppend}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); mutasi data ({@code onSave()}); pelaporan/ekspor ({@code onCetakEvaluasi()});
 * operasi domain lain ({@code onAdd()}, {@code createTabbox()}, {@code select()}). Bagian lain dari kontrak
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
public class AcaraAction extends GenericAutowireComposer implements WorkspaceSelecter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private Textbox nama;
	private MyDatebox ppbegin;
	private MyDatebox ppend;
	private AmbilDataWorkspaceBanbox workspace;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Acara acara;
	private MyToolbarbuttonConfig add;
	private MyGrid gridIndikator;
	protected MyGrid gridParemeter;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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
//		if (session.getAttribute("usersTemp") == null
//				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
//			session.removeAttribute("usersTemp");
//			Common.goLogoff();
//			return;
//		}

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public void onCetakEvaluasi(Event event) throws Exception {
		LaporanRealisasiProgramBulananWorkspace laporanPerencanaan = new LaporanRealisasiProgramBulananWorkspace();
		laporanPerencanaan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporanPerencanaan);
		laporanPerencanaan.setHeight("95%");
		laporanPerencanaan.setWidth("90%");
		laporanPerencanaan.setClosable(true);
		laporanPerencanaan.onModal();
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AcaraAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AcaraAction} dan dapat mengakses state kelas
	 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AcaraAction
	 */
	class AcaraRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Acara acara = (Acara) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (detail.getChildren().size() == 0) {
						Tabbox tabbox = createTabbox(acara);
						tabbox.setHeight("450px");
						detail.appendChild(tabbox);
					}
				}
			});

			RevisiHelper.createNewRevisi(Acara.class, acara, acara.getNama()).setParent(arg0);
			new Label(Common.dateFormat4.get().format(acara.getPpbegin())).setParent(arg0);
			new Label(Common.dateFormat4.get().format(acara.getPpend())).setParent(arg0);
			new Label(
					acara.getWorkspace() == null ? ""
							: acara.getWorkspace().toString() + " - "
									+ (Common.numberFormat.get().format(acara.getWorkspace().getHargaTotal())))
											.setParent(arg0);
			new Label(acara.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(acara);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											AcaraDao acaraDao = DaoFactory.getInstance().getAcaraDao();
											// acaraDao.beginTransaction();
											acaraDao.delete((acara));
											// acaraDao.commitTransaction();
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
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Acara());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final Acara acara) throws Exception {
		this.acara = acara;
		addWindow.setTitle(acara.getId() == null ? "Tambah Acara" : "Ubah Acara");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Acara"));
		row.appendChild(nama = new Textbox(acara.getNama() == null ? "" : acara.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(ppbegin = new MyDatebox(acara.getPpbegin()));
		ppbegin.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Selesai"));
		row.appendChild(ppend = new MyDatebox(acara.getPpend()));
		ppend.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program/Kegiatan"));
		row.appendChild(workspace = new AmbilDataWorkspaceBanbox(true));
		workspace.setValue(acara.getWorkspace() == null ? "" : acara.getWorkspace().toString());
		workspace.setAttribute("workspace", acara.getWorkspace());
		workspace.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(acara.getKeterangan() == null ? "" : acara.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		createTabbox(acara).setParent(center);

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

	private Tabbox createTabbox(final Acara acara) {
		Tabbox tabbox = new Tabbox();

		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabRealisasiIndikator = new MyTabConfig("Realisasi Indikator");
		tabRealisasiIndikator.setParent(tabs);

		final MyTabConfig tabKendala = new MyTabConfig("Kendala");
		tabKendala.setParent(tabs);

		final MyTabConfig tabRealisasiParemeter = new MyTabConfig("Realisasi Paremeter");
		tabRealisasiParemeter.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelRealisasiIndikator = new ais.ui.util.MyTabpanel();
		tabpanelRealisasiIndikator.setParent(tabpanels);

		final Tabpanel tabpanelKendala = new ais.ui.util.MyTabpanel();
		tabpanelKendala.setParent(tabpanels);

		final Tabpanel tabpanelRealisasiParemeter = new ais.ui.util.MyTabpanel();
		tabpanelRealisasiParemeter.setParent(tabpanels);

		tabpanelRealisasiIndikator
				.appendChild(new AcaraPunyaIndikatorHelper(gridIndikator = new MyGrid()).initDetail(acara, this));

		tabKendala.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKendala.getChildren().size() == 0) {
					if (onSave(arg0)) {
						Common.clear(tabpanelKendala);
						session.setAttribute("acaraPunyaKendala", acara);
						MyInclude iframe = new MyInclude("/pages/master/rab/acara_punya_kendala.zul");
						iframe.setParent(tabpanelKendala);
					} else {
						tabRealisasiIndikator.setSelected(true);
					}
				}

			}
		});

		tabRealisasiParemeter.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelRealisasiParemeter.getChildren().size() == 0) {
					if (onSave(arg0)) {
						Common.clear(tabpanelRealisasiParemeter);
						tabpanelRealisasiParemeter
								.appendChild(new AcaraPunyaJenisParameterHelper(gridParemeter = new MyGrid())
										.initDetail(acara, AcaraAction.this));
					} else {
						tabRealisasiIndikator.setSelected(true);
					}
				}
			}
		});

		return tabbox;
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama == null) {
			return true;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Acara harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (ppbegin.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Mulai Acara harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (ppend.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Selesai Acara harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (workspace.getAttribute("workspace") == null) {
			MyMessageboxConfig.show("Program / Kegiatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		List<Row> rowsIndikator = gridIndikator.getRows().getChildren();
		for (Row row : rowsIndikator) {
			AcaraPunyaIndikator acaraPunyaIndikator = (AcaraPunyaIndikator) row.getAttribute("acaraPunyaIndikator");
			if (acaraPunyaIndikator.getRealisasi() == null) {
				MyMessageboxConfig.show("Jumlah Realisasi harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		AcaraDao acaraDao = DaoFactory.getInstance().getAcaraDao();
		if (acara.getId() != null) {
			acara = acaraDao.load(acara.getId());

		}

		acara.setNama(nama.getValue());
		acara.setPpbegin(ppbegin.getValue());
		acara.setPpend(ppend.getValue());
		acara.setWorkspace((Workspace) workspace.getAttribute("workspace"));
		acara.setKeterangan(keterangan.getValue());

		if (acara.getId() != null) {
			acaraDao.update(acara);
		} else {
			acaraDao.save(acara);
		}

		Session session = HibernateUtil.currentSession();
		for (Row row : rowsIndikator) {
			AcaraPunyaIndikator acaraPunyaIndikator = (AcaraPunyaIndikator) row.getAttribute("acaraPunyaIndikator");
			session.saveOrUpdate(acaraPunyaIndikator);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Acara.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.createAlias("workspace", "workspace")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("workspace.satuanKerja", satuanKerjas))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Acara> acara = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(acara);
		grid.setRowRenderer(new AcaraRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public AmbilDataWorkspaceBanbox select() {
		return workspace;
	}

}
