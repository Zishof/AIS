package ais.action.master.library;

import java.util.List;

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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataAnggotaBanbox;
import ais.action.master.library.helper.AmbilDataItemBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.format1.library.LaporanPesananItem;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.PesananAnggotaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Anggota;
import ais.database.model.library.Item;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.PesananAnggota;
import ais.ui.util.MyDatebox;

public class PesananAnggotaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataPerpustakaanBanbox searchperpustakaan;
	private AmbilDataAnggotaBanbox searchanggota;

	private MyDatebox tanggal;
	private AmbilDataAnggotaBanbox anggota;
	private AmbilDataItemBanbox item;
	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PesananAnggota pesananAnggota;
	private MyToolbarbuttonConfig add;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

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

		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchanggota.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public void onCetak(Event event) throws Exception {
		LaporanPesananItem laporan = new LaporanPesananItem();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	class PesananAnggotaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PesananAnggota pesananAnggota = (PesananAnggota) arg1;
			LibraryUtil.gambarAnggota(pesananAnggota.getAnggota()).setParent(arg0);
			RevisiHelper.createNewRevisi(PesananAnggota.class, pesananAnggota,
					pesananAnggota.getKode()).setParent(arg0);
			new Label(pesananAnggota.getTanggal() == null ? ""
					: Common.dateFormat5.get().format(pesananAnggota.getTanggal()))
					.setParent(arg0);
			new Label(pesananAnggota.getAnggota() == null ? "" : pesananAnggota
					.getAnggota().toString()).setParent(arg0);
			new Label(pesananAnggota.getItem() == null ? "" : pesananAnggota
					.getItem().getNama()).setParent(arg0);
			new Label(pesananAnggota.getPerpustakaan() == null ? ""
					: pesananAnggota.getPerpustakaan().getNama())
					.setParent(arg0);
			new Label(pesananAnggota.getStatus()).setParent(arg0);
			new Label(pesananAnggota.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pesananAnggota);
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
											
											Common.refreshDelete(pesananAnggota);
											
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
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PesananAnggota());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PesananAnggota pesananAnggota) throws Exception {
		this.pesananAnggota = pesananAnggota;
		addWindow.setTitle(pesananAnggota.getId() == null ? "Tambah Pesanan Anggota" : "Ubah Pesanan Anggota");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pemesanan"));
		row.appendChild(tanggal = new MyDatebox(pesananAnggota.getTanggal()));
		tanggal.setFormat(Common.dateFormat.get().toPattern());
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggota"));
		row.appendChild(anggota = new AmbilDataAnggotaBanbox());
		anggota.setAttribute("anggota", pesananAnggota.getAnggota());
		anggota.setValue(pesananAnggota.getAnggota() == null ? ""
				: pesananAnggota.getAnggota().toString());
		anggota.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setAttribute("perpustakaan",
				pesananAnggota.getPerpustakaan());
		perpustakaan.setValue(pesananAnggota.getPerpustakaan() == null ? ""
				: pesananAnggota.getPerpustakaan().getNama());
		perpustakaan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item"));
		row.appendChild(item = new AmbilDataItemBanbox());
		item.setAttribute("item", pesananAnggota.getItem());
		item.setValue(pesananAnggota.getItem() == null ? "" : pesananAnggota
				.getItem().getNama());
		item.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				pesananAnggota.getKeterangan() == null ? "" : pesananAnggota
						.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Pesanan harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (anggota.getAttribute("anggota") == null) {
			MyMessageboxConfig.show("Anggota harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (item.getAttribute("item") == null) {
			MyMessageboxConfig.show("Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (perpustakaan.getAttribute("perpustakaan") == null) {
			MyMessageboxConfig.show("Perpustakaan harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		PesananAnggotaDao pesananAnggotaDao = DaoFactory.getInstance()
				.getPesananAnggotaDao();
		if (pesananAnggota.getId() != null) {
			pesananAnggota = pesananAnggotaDao.load(pesananAnggota.getId());
		} else {
			Item myItem = ((Item) item.getAttribute("item"));
			Perpustakaan myPerpustakaan = ((Perpustakaan) perpustakaan
					.getAttribute("perpustakaan"));
			Anggota myAnggota = (Anggota) anggota.getAttribute("anggota");

			String sqlCheckStok = "select sum((a.qty+a.qtybonus)*b.jenis) as stok "
					+ "from library.detail_transaksi a "
					+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
					+ "where a.item = "
					+ myItem.getId()
					+ " and a.perpustakaan = " + myPerpustakaan.getId();

			Session session = HibernateUtil.currentSession();
			Number jumlah = (Number) session.createSQLQuery(sqlCheckStok)
					.uniqueResult();
			if (jumlah == null || jumlah.intValue() < 1) {
				MyMessageboxConfig.show("Item " + myItem.getNama()
						+ " tidak tersedia di " + myPerpustakaan.getNama(),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			jumlah = (Number) session
					.createCriteria(PesananAnggota.class)
					.add(Restrictions.eq("anggota", myAnggota))
					.add(Restrictions.eq("item", myItem))
					.add(Restrictions.eq("perpustakaan", myPerpustakaan))
					.add(Restrictions.sqlRestriction("date(kadaluarsa) > date('"+Common.databaseDateFormat1.get().format(WaktuUtil.getDate())+"')"))
					
					.setProjection(Projections.rowCount()).uniqueResult();

			if (jumlah != null && jumlah.intValue() > 0) {
				MyMessageboxConfig.show("Item " + myItem.getNama()
						+ " sudah dipesan di " + myPerpustakaan.getNama(),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		pesananAnggota.setPerpustakaan((Perpustakaan) perpustakaan
				.getAttribute("perpustakaan"));
		pesananAnggota.setItem((Item) item.getAttribute("item"));
		pesananAnggota.setAnggota((Anggota) anggota.getAttribute("anggota"));
		pesananAnggota.setTanggal(tanggal.getValue());
		pesananAnggota.setKeterangan(keterangan.getValue());

		if (pesananAnggota.getId() != null) {
			pesananAnggotaDao.update(pesananAnggota);
		} else {
			pesananAnggotaDao.save(pesananAnggota);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PesananAnggota.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(Restrictions.ilike("kode", searchnama.getValue(),
				MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PesananAnggota> pesananAnggota = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(pesananAnggota);
		grid.setRowRenderer(new PesananAnggotaRenderer());
		grid.setModelCheckMobile(strset);

		

	}

}
