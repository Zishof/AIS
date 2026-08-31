package ais.action.master.library;

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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
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

import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.RakDetailAction;
import ais.action.master.library.helper.RakPunyaItemHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.RakDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.Rak;
import ais.database.model.library.RakDetail;

/**
 * Controller/action ZK untuk rak. Tipe ini merupakan titik masuk UI yang menghubungkan event layar
 * dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code AmbilDataRuangBanbox searchruang}, {@code
 * Combobox searchperpustakaan}, {@code Textbox searchisbn}, {@code Textbox searchjudul}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initDetail()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); validasi/perhitungan ({@code
 * checkNamaRak()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class RakAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataRuangBanbox searchruang;
	private Combobox searchperpustakaan;
	private Textbox searchisbn;
	private Textbox searchjudul;

	private Textbox nama;
	private AmbilDataRuangBanbox ruang;
	private Combobox perpustakaan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Rak rak;
	private MyToolbarbuttonConfig add;
	private MyGrid gridItem;
	protected Perpustakaan currentPerpustakaan;

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

		searchruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.insertCombo(searchperpustakaan, "nama", Perpustakaan.class);

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

	class RakRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Rak rak = (Rak) arg1;

			(new RakDetailAction(rak, searchisbn, searchjudul)).setParent(arg0);

			RevisiHelper.createNewRevisi(Rak.class, rak, rak.getNama())
					.setParent(arg0);
			new Label(rak.getRuang() == null ? "" : rak.getRuang().getNama())
					.setParent(arg0);
			new Label(rak.getPerpustakaan() == null ? "" : rak
					.getPerpustakaan().getNama()).setParent(arg0);
			new Label(rak.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(rak);
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
											
											Common.refreshDelete(rak);
											
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
		init(new Rak());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final Rak rak, Component component)
			throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabDipinjam = new MyTabConfig("Item Rak");
		tabDipinjam.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDipinjam = new ais.ui.util.MyTabpanel();
		tabpanelDipinjam.setParent(tabpanels);
		tabpanelDipinjam.setWidth("100%");

		tabpanelDipinjam.appendChild(new RakPunyaItemHelper(
				gridItem = new MyGrid()).initDetail(rak));

	}

	private void init(final Rak rak) throws Exception {
		this.rak = rak;
		addWindow.setTitle(rak.getId() == null ? "Tambah Rak" : "Ubah Rak");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

		initDetail(rak, east);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Rak"));
		row.appendChild(nama = new Textbox(rak.getNama() == null ? "" : rak
				.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruangan"));
		row.appendChild(ruang = new AmbilDataRuangBanbox());
		ruang.setAttribute("ruang", rak.getRuang());
		ruang.setValue(rak.getRuang() == null ? "" : rak.getRuang().getNama());
		ruang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new Combobox());
		Common.insertCombo(perpustakaan, "nama", Perpustakaan.class);
		Common.selectComboItem(
				perpustakaan,
				rak.getPerpustakaan() == null ? currentPerpustakaan : rak
						.getPerpustakaan());
		perpustakaan.setDisabled(currentPerpustakaan != null);
		perpustakaan.setWidth("90%");
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				currentPerpustakaan = (Perpustakaan) (perpustakaan
						.getSelectedItem() == null ? null : perpustakaan
						.getSelectedItem().getValue());

				rak.setPerpustakaan(currentPerpustakaan);
			}
		};
		perpustakaan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				rak.getKeterangan() == null ? "" : rak.getKeterangan()));
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

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Rak harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		@SuppressWarnings("unchecked")
		List<Row> rowsItem = gridItem.getRows().getChildren();
		for (Row row : rowsItem) {
			RakDetail rakDetail = (RakDetail) row.getAttribute("rakDetail");
			if (rakDetail.getItem() == null) {
				MyMessageboxConfig.show("Item harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		boolean i = checkNamaRak();
		if (i) {
			MyMessageboxConfig.show("Nama Rak sudah ada di database", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		RakDao rakDao = DaoFactory.getInstance().getRakDao();
		if (rak.getId() != null) {
			rak = rakDao.load(rak.getId());

		}

		rak.setPerpustakaan((Perpustakaan) (perpustakaan.getSelectedItem() == null ? null
				: perpustakaan.getSelectedItem().getValue()));
		rak.setNama(nama.getValue());
		rak.setRuang((Ruang) ruang.getAttribute("ruang"));
		rak.setKeterangan(keterangan.getValue());

		if (rak.getId() != null) {
			rakDao.update(rak);
		} else {
			rakDao.save(rak);
		}

		Session session = rakDao.getCurrentSession();
		for (Row row : rowsItem) {
			RakDetail rakDetail = (RakDetail) row.getAttribute("rakDetail");
			rakDetail.setRak(rak);
			session.saveOrUpdate(rakDetail);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Rak.class);

		if (!searchisbn.getValue().trim().isEmpty()
				|| !searchjudul.getValue().trim().isEmpty()) {

			Criterion criterion = Restrictions.sqlRestriction("false");
			if (!searchisbn.getValue().trim().isEmpty()) {
				criterion = Restrictions.or(criterion, Restrictions.ilike(
						"item.isbn", searchisbn.getValue().trim(),
						MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion, Restrictions.ilike(
						"item.isbn10", searchisbn.getValue().trim(),
						MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion, Restrictions.ilike(
						"item.issn", searchisbn.getValue().trim(),
						MatchMode.ANYWHERE));
			}

			criteria = session
					.createCriteria(RakDetail.class)
					.createAlias("item", "item")
					.setProjection(Projections.groupProperty("rak"))
					.add(searchisbn.getValue().trim().isEmpty() ? Restrictions
							.sqlRestriction("true") : criterion)
					.add(searchjudul.getValue().trim().isEmpty() ? Restrictions
							.sqlRestriction("true") : Restrictions.ilike(
							"item.nama", searchjudul.getValue(),
							MatchMode.ANYWHERE)).createCriteria("rak");
		}

		else if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(
				searchnama.getValue().trim().isEmpty() ? Restrictions
						.sqlRestriction("true") : Restrictions.ilike("nama",
						searchnama.getValue(), MatchMode.ANYWHERE))
				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions
						.sqlRestriction("true") : Restrictions.eq("ruang",
						searchruang.getAttribute("ruang"))))

				.add(searchperpustakaan.getSelectedItem() == null ? Restrictions
						.sqlRestriction("true") : Restrictions.eq(
						"perpustakaan", searchperpustakaan.getSelectedItem()
								.getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Rak> rak = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(rak);
		grid.setRowRenderer(new RakRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	public Boolean checkNamaRak() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session
				.createCriteria(Rak.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.rak.getId() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ne("id",
						this.rak.getId())).uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
