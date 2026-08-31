package ais.action.master.sirs;

import java.util.ArrayList;
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
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataGenerikItemBanbox;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyak;
import ais.action.master.sirs.helper.AmbilDataKelasItemBanbox;
import ais.action.master.sirs.helper.AmbilDataSatuanItemBanyak;
import ais.action.master.sirs.util.CommonItem;
import ais.action.master.sirs.util.CommonItem.InitHarga;
import ais.action.report.format1.sirs.inventory.LaporanHargaBeliItem;
import ais.action.report.format1.sirs.inventory.LaporanHargaJualItem;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.OnSave;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.ItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.JenisItem;
import ais.database.model.library.Penyedia;
import ais.database.model.sirs.BahanBakuItem;
import ais.database.model.sirs.GenerikItem;
import ais.database.model.sirs.HargaBeliItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.KelasItem;
import ais.database.model.sirs.KelompokItem;
import ais.database.model.sirs.KonversiSatuanItem;
import ais.database.model.sirs.SatuanItem;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk item medis. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code Grid grid},
 * {@code Paging paging}, {@code Grid gridHargaBeli}, {@code MyTextbox searchnama}, {@code MyTextbox searchkode},
 * {@code MyTextbox searchbarcode}, {@code Combobox searchsatuanItem}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code initHargaBeli()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onUploadBiaya()}, {@code onDownloadBiaya()}, {@code onSearchDefault()}); validasi/perhitungan ({@code
 * checkNamaItem()}); mutasi data ({@code onSave()}); pelaporan/ekspor ({@code onCetakHargaJual()}, {@code
 * onCetakHargaBeli()}); operasi domain lain ({@code onAdd()}, {@code generateCode()}). Bagian lain dari kontrak
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
public class ItemMedisAction extends GenericAutowireComposer implements OnSave {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private Grid gridHargaBeli;

	private MyTextbox searchnama;
	private MyTextbox searchkode;
	private MyTextbox searchbarcode;
	private Combobox searchsatuanItem;
	private Combobox searchjenisItem;
	private AmbilDataKelasItemBanbox searchKelasItem;
	private AmbilDataGenerikItemBanbox searchGenerikItem;
	private MyTextbox searchkandungan;

	private MyTextbox nama;
	private Checkbox bolehretur;

	private Intbox batasMinimalStok;
	private Label kode;
	private MyTextbox barcode;
	private Combobox satuanItem;
	private Combobox jenisItem;
	private Combobox kelompokItem;
	private MyTextbox kandungan;
	private AmbilDataKelasItemBanbox kelasItem;
	private AmbilDataGenerikItemBanbox generikItem;
	private Toolbarbutton add;

	private boolean edit = false;
	private boolean delete = false;

	private ItemMedis item;

	private BahanBakuAction bahanBakuAction = new BahanBakuAction();
	private KonversiSatuanAction konversiSatuanAction = new KonversiSatuanAction();
	private InitHarga initHarga = new InitHarga();

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		searchKelasItem.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchGenerikItem.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.insertCombo(satuanItem = new Combobox(), "nama", SatuanItem.class);
		Common.insertCombo(jenisItem = new Combobox(), "nama", JenisItem.class);
		Common.insertCombo(kelompokItem = new Combobox(), "nama", KelompokItem.class);

		Common.insertCombo(searchsatuanItem, "nama", SatuanItem.class);
		Common.insertCombo(searchjenisItem, "nama", JenisItem.class);

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
	        FilterLanjutHelper.setup(comp);
}
 
	public void onUploadBiaya(Event event) throws Exception {
		CommonItem.onUploadBiaya(event, null);
	}

	public void onDownloadBiaya(Event event) throws Exception {
		CommonItem.onDownloadBiaya(event, null);
	}

	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final ItemMedis item = (ItemMedis) arg1;

			new Label(item.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(ItemMedis.class, item, item.getNama()).setParent(arg0);
			new Label(item.getKandungan()).setParent(arg0);
			new Label(item.getBarcode()).setParent(arg0);

			new Label(item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama()).setParent(arg0);

			new Label(item.getJenisItem() == null ? "" : item.getJenisItem().getNama()).setParent(arg0);
			new Label(item.getKelompokItem() == null ? "" : item.getKelompokItem().getNama()).setParent(arg0);
			new Label(item.getKelasItem() == null ? "" : item.getKelasItem().getNama()).setParent(arg0);
			new Label(item.getGenerikItem() == null ? "" : item.getGenerikItem().getNama()).setParent(arg0);
			new Label(item.getBatasMinimalStok() == null ? "" : Common.numberFormat.get().format(item.getBatasMinimalStok()))
					.setParent(arg0);
			new Label(item.getBolehretur() == null || !item.getBolehretur() ? "Tidak" : "Ya").setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(item);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data item medis ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											ItemDao itemDao = DaoFactory.getInstance().getItemDao();

											itemDao.getCurrentSession().createSQLQuery(
													"delete from sirs.harga_jual_item where item = " + item.getId())
													.executeUpdate();

											itemDao.getCurrentSession().createSQLQuery(
													"delete from sirs.harga_beli_item where item = " + item.getId())
													.executeUpdate();

											itemDao.getCurrentSession()
													.createSQLQuery(
															"delete from sirs.bahan_baku_item where item_induk = "
																	+ item.getId())
													.executeUpdate();

											Common.refreshDelete(item);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data item medis ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
													e.getMessage()));
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
		init(new ItemMedis());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onCetakHargaJual(Event event) throws Exception {
		LaporanHargaJualItem laporanHargaJualItem = new LaporanHargaJualItem();
		laporanHargaJualItem.setTitle("Laporan Harga Jual");
		laporanHargaJualItem.setClosable(true);
		laporanHargaJualItem.setWidth("750px");
		laporanHargaJualItem.setHeight("95%");
		laporanHargaJualItem.setParent(page.getFirstRoot());
		laporanHargaJualItem.onModal();
	}

	public void onCetakHargaBeli(Event event) throws Exception {
		LaporanHargaBeliItem laporanHargaBeliItem = new LaporanHargaBeliItem();
		laporanHargaBeliItem.setTitle("Laporan Harga Beli");
		laporanHargaBeliItem.setClosable(true);
		laporanHargaBeliItem.setWidth("950px");
		laporanHargaBeliItem.setHeight("95%");
		laporanHargaBeliItem.setParent(page.getFirstRoot());
		laporanHargaBeliItem.onModal();
	}

	@SuppressWarnings("unchecked")
	private Borderlayout initHargaBeli(final ItemMedis item) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		gridHargaBeli = new Grid();
		gridHargaBeli.setParent(center);
		gridHargaBeli.setWidth("100%");
		gridHargaBeli.setHeight("100%");
		gridHargaBeli.setMold("paging");
		gridHargaBeli.setPageSize(10);

		Columns columns = new Columns();
		columns.setParent(gridHargaBeli);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		Rows rows = new Rows();
		rows.setParent(gridHargaBeli);

		Session session = HibernateUtil.currentSession();
		List<Penyedia> vendors = ConstantValues
				.simpleList(session.createCriteria(Penyedia.class).addOrder(Order.asc("nama")), Penyedia.class);

		for (Penyedia vendor : vendors) {
			HargaBeliItem hargaBeliItem = (HargaBeliItem) (item == null || item.getId() == null ? null
					: session.createCriteria(HargaBeliItem.class).add(Restrictions.eq("vendor", vendor))
							.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult());

			Row row = new Row();
			row.setAttribute("vendor", vendor);
			row.setAttribute("hargaBeliItem", hargaBeliItem);
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(vendor.getNama()));
			MyDoublebox doublebox = new MyDoublebox();
			doublebox.setValue(
					hargaBeliItem == null || hargaBeliItem.getHargaBeli() == null ? 0.0 : hargaBeliItem.getHargaBeli());
			doublebox.setWidth("90%");
			row.appendChild(doublebox);

		}

		return borderlayout;

	}

	private class BahanBakuAction {

		private Grid gridItem;

		public Borderlayout init() {
			return display();
		}

		public Borderlayout display() {

			Borderlayout borderlayout = new Borderlayout();

			North north = new North();
			ais.ui.util.ZkCompat.setFlex(north, true);
			north.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(north);
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Bahan Baku", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (!ItemMedisAction.this.onSave(event)) {
						return;
					}

					Session session = HibernateUtil.currentSession();

					List<ItemMedis> items = ConstantValues.simpleList(
							session.createCriteria(BahanBakuItem.class)
									.setProjection(Projections.groupProperty("item.id"))
									.add(Restrictions.eq("itemInduk", ItemMedisAction.this.item)),
							ItemMedis.class, false);

					AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items,
							new JenisItemMedis(JenisItemMedis.OBAT));
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
					ambilDataItemBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<ItemMedis> items = (List<ItemMedis>) arg0.getData();

							Session session = HibernateUtil.currentSession();
							for (ItemMedis item : items) {
								BahanBakuItem bahanBakuItem = new BahanBakuItem();
								bahanBakuItem.setItem(item);
								bahanBakuItem.setQty(0.0);
								bahanBakuItem.setKeterangan("");
								bahanBakuItem.setItemInduk(ItemMedisAction.this.item);
								session.save(bahanBakuItem);

							}

							loadData(null);
						}
					});
					ambilDataItemBanyak.setWidth("750px");
					ambilDataItemBanyak.setHeight("97%");
					ambilDataItemBanyak.setVisible(true);
					ambilDataItemBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			gridItem = new Grid();
			gridItem.setMold("paging");
			gridItem.setPageSize(25);
			gridItem.setParent(center);

			Columns columns = new Columns();

			columns.setParent(gridItem);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Kode");
			column.setWidth("15%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama");
			column.setWidth("30%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Qty");
			column.setAlign("right");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Satuan");
			column.setWidth("15%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Keterangan");

			column = new Column();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("10%");
			loadData(null);
			return borderlayout;
		}

		class BahanBakuItemRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(final Row arg0, Object arg1) throws Exception {
				// TODO Auto-generated method stub
				final BahanBakuItem bahanBakuItem = (BahanBakuItem) arg1;
				final ItemMedis item = bahanBakuItem.getItem();

				new Label(item.getKode()).setParent(arg0);
				new Label(item.getNama()).setParent(arg0);

				final MyDoublebox jumlah;
				jumlah = new MyDoublebox(bahanBakuItem.getQty() == null ? 1.0 : bahanBakuItem.getQty());
				jumlah.setParent(arg0);
				jumlah.setStyle("text-align:right");
				jumlah.setWidth("90%");
				jumlah.setWidth("90%");
				jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						bahanBakuItem.setQty(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
						Common.refreshUpdate(session, (bahanBakuItem));

					}
				});

				new Label(item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama()).setParent(arg0);

				final MyTextbox keterangan = new MyTextbox(
						bahanBakuItem.getKeterangan() == null ? "" : bahanBakuItem.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setParent(arg0);

				keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						bahanBakuItem.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(session, (bahanBakuItem));
					}
				});

				Hbox toolbar = new Hbox();
				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data bahan baku ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Common.refreshDelete(bahanBakuItem);
												loadData(null);
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data bahan baku ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
														e.getMessage()));
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

		@SuppressWarnings("unchecked")
		public void loadData(Event event) {
			Session session = HibernateUtil.currentSession();
			List<BahanBakuItem> bahanBakuItems = ItemMedisAction.this.item == null
					|| ItemMedisAction.this.item.getId() == null
							? new ArrayList<BahanBakuItem>()
							: ConstantValues.simpleList(
									session.createCriteria(BahanBakuItem.class).addOrder(Order.desc("id"))
											.add(Restrictions.eq("itemInduk", ItemMedisAction.this.item))
											.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
											.setFirstResult(Common.ROWS_COUNT_ON_PAGE
													* (paging == null ? 0 : paging.getActivePage())),
									BahanBakuItem.class);
			ListModel strset = new SimpleListModel(bahanBakuItems);
			gridItem.setRowRenderer(new BahanBakuItemRenderer());
			gridItem.setModel(strset);
			gridItem.renderAll();

		}

	}

	private class KonversiSatuanAction {

		private Grid gridSatuanItem;

		public Borderlayout init() {
			return display();
		}

		public Borderlayout display() {

			Borderlayout borderlayout = new Borderlayout();

			North north = new North();
			ais.ui.util.ZkCompat.setFlex(north, true);
			north.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(north);
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Satuan", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (!ItemMedisAction.this.onSave(event)) {
						return;
					}

					Session session = HibernateUtil.currentSession();

					List<SatuanItem> satuanItems = ConstantValues
							.simpleList(session.createCriteria(KonversiSatuanItem.class)
									.setProjection(Projections.groupProperty("satuanMenjadi.id"))
									.add(Restrictions.eq("item", ItemMedisAction.this.item)), SatuanItem.class);

					AmbilDataSatuanItemBanyak ambilDataSatuanItemBanyak = new AmbilDataSatuanItemBanyak(satuanItems);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataSatuanItemBanyak);
					ambilDataSatuanItemBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<SatuanItem> satuanItems = (List<SatuanItem>) arg0.getData();

							Session session = HibernateUtil.currentSession();
							for (SatuanItem satuanItem : satuanItems) {
								KonversiSatuanItem konversiSatuanItem = new KonversiSatuanItem();
								konversiSatuanItem.setSatuanMenjadi(satuanItem);
								konversiSatuanItem.setSatuanDari(item.getSatuanItem());
								konversiSatuanItem.setNilaiPersamaan(1.0);
								konversiSatuanItem.setKeterangan("");
								konversiSatuanItem.setItem(item);
								session.save(konversiSatuanItem);

							}

							loadData(null);
						}
					});
					ambilDataSatuanItemBanyak.setWidth("750px");
					ambilDataSatuanItemBanyak.setHeight("97%");
					ambilDataSatuanItemBanyak.setVisible(true);
					ambilDataSatuanItemBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			gridSatuanItem = new Grid();
			gridSatuanItem.setMold("paging");
			gridSatuanItem.setPageSize(25);
			gridSatuanItem.setParent(center);

			Columns columns = new Columns();

			columns.setParent(gridSatuanItem);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Satuan Dari");
			column.setWidth("25%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Satuan Menjadi");
			column.setWidth("25%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nilai Persamaan");
			column.setAlign("right");
			column.setWidth("20%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Keterangan");

			column = new Column();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("10%");
			loadData(null);
			return borderlayout;
		}

		class KonversiSatuanItemRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(final Row arg0, Object arg1) throws Exception {
				// TODO Auto-generated method stub
				final KonversiSatuanItem konversiSatuanItem = (KonversiSatuanItem) arg1;
				final SatuanItem satuanItem = konversiSatuanItem.getSatuanMenjadi();

				new Label(
						konversiSatuanItem.getSatuanDari() == null ? "" : konversiSatuanItem.getSatuanDari().getNama())
						.setParent(arg0);
				new Label(satuanItem == null ? "" : satuanItem.getNama()).setParent(arg0);

				final MyDoublebox jumlah;
				jumlah = new MyDoublebox(
						konversiSatuanItem.getNilaiPersamaan() == null ? 1.0 : konversiSatuanItem.getNilaiPersamaan());
				jumlah.setParent(arg0);
				jumlah.setStyle("text-align:right");
				jumlah.setWidth("90%");
				jumlah.setWidth("90%");
				jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						konversiSatuanItem.setNilaiPersamaan(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
						Common.refreshUpdate(session, (konversiSatuanItem));

					}
				});

				final MyTextbox keterangan = new MyTextbox(
						konversiSatuanItem.getKeterangan() == null ? "" : konversiSatuanItem.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setParent(arg0);

				keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						konversiSatuanItem.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(session, (konversiSatuanItem));
					}
				});

				Hbox toolbar = new Hbox();
				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data konversi satuan ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Common.refreshDelete(konversiSatuanItem);
												loadData(null);
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data konversi satuan ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
														e.getMessage()));
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

		@SuppressWarnings("unchecked")
		public void loadData(Event event) {
			Session session = HibernateUtil.currentSession();
			List<KonversiSatuanItem> konversiSatuanItems = ItemMedisAction.this.item == null
					|| ItemMedisAction.this.item.getSatuanItem() == null
					|| ItemMedisAction.this.item.getSatuanItem().getId() == null
							? new ArrayList<KonversiSatuanItem>()
							: ConstantValues.simpleList(
									session.createCriteria(KonversiSatuanItem.class).addOrder(Order.desc("id"))
											.add(Restrictions.eq("satuanDari",
													ItemMedisAction.this.item.getSatuanItem()))
											.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
											.setFirstResult(Common.ROWS_COUNT_ON_PAGE
													* (paging == null ? 0 : paging.getActivePage())),
									KonversiSatuanItem.class);
			ListModel strset = new SimpleListModel(konversiSatuanItems);
			gridSatuanItem.setRowRenderer(new KonversiSatuanItemRenderer());
			gridSatuanItem.setModel(strset);
			gridSatuanItem.renderAll();

		}

	}

	private MyTextbox keterangan;

	private void init(final ItemMedis item) throws Exception {
		this.item = item;
		addWindow = new Window();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle(item.getId() == null ? "Tambah Item" : "Ubah Item");
		addWindow.setWidth("95%");
		addWindow.setHeight("90%");
		Borderlayout borderlayout = new Borderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Grid grid = new Grid();
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode")));
		row.appendChild(kode = new Label(item.getKode() == null ? "" : item.getKode()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new MyTextbox(item.getNama() == null ? "" : item.getNama()));
		nama.setWidth("90%");
		nama.focus();

		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String mynama = nama.getValue().trim();
				if (mynama.trim().equals("")) {
					MyMessageboxConfig.show("Mohon maaf, Nama item wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama item pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) lanjutkan pengisian data setelah kolom terisi.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					nama.focus();
					return;
				}
				generateCode();
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Barcode")));
		row.appendChild(barcode = new MyTextbox(item.getBarcode() == null ? "" : item.getBarcode()));
		barcode.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan")));
		row.appendChild(satuanItem);
		Common.selectComboItem(satuanItem, item.getSatuanItem());
		satuanItem.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kandungan")));
		row.appendChild(kandungan = new MyTextbox(item.getKandungan()));
		kandungan.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas")));
		row.appendChild(kelasItem = new AmbilDataKelasItemBanbox());
		kelasItem.setValue(item.getKelasItem() == null ? "" : item.getKelasItem().getNama());
		kelasItem.setAttribute("kelasItem", item.getKelasItem());
		kelasItem.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis")));
		row.appendChild(jenisItem);
		Common.selectComboItem(jenisItem, item.getJenisItem());
		jenisItem.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Minimal")));
		row.appendChild(batasMinimalStok = new Intbox(
				item.getBatasMinimalStok() == null ? ItemMedis.DEFAULT_MINIMAL_STOK : item.getBatasMinimalStok()));
		batasMinimalStok.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Generik")));
		row.appendChild(generikItem = new AmbilDataGenerikItemBanbox());
		generikItem.setValue(item.getGenerikItem() == null ? "" : item.getGenerikItem().getNama());
		generikItem.setAttribute("generikItem", item.getGenerikItem());
		generikItem.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Boleh Retur")));
		row.appendChild(bolehretur = new Checkbox());
		bolehretur.setChecked(item.getBolehretur() != null && item.getBolehretur());

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelompok")));
		row.appendChild(kelompokItem);
		Common.selectComboItem(kelompokItem, item.getKelompokItem());
		kelompokItem.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(item.getKeterangan() == null ? "" : item.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("border:0px;background: transparent;");
		tabbox.setHeight("240px");
		tabbox.setWidth("100%");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setStyle("border:0px;background: transparent;");
		tabs.setParent(tabbox);

		final Tab tabPenjualan = new Tab("Harga Penjualan");
		tabPenjualan.setParent(tabs);

		Tab tabPembelian = new Tab("Harga Pembelian");
		tabPembelian.setParent(tabs);

		Tab tabBahanBaku = new Tab("Bahan Baku");
		tabBahanBaku.setParent(tabs);

		Tab tabKonversi = new Tab("Konversi Satuan");
		tabKonversi.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		initHarga.initHargaJual(item, tabpanel, this, null);

		final Tabpanel tabpanelPembelian = new ais.ui.util.MyTabpanel();
		tabpanelPembelian.setParent(tabpanels);

		tabPembelian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPembelian.getChildren().size() == 0) {
					tabpanelPembelian.appendChild(initHargaBeli(item));
				}
			}
		});

		final Tabpanel tabpanelBahanBaku = new ais.ui.util.MyTabpanel();
		tabpanelBahanBaku.setParent(tabpanels);
		tabBahanBaku.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelBahanBaku.getChildren().size() == 0) {
					tabpanelBahanBaku.appendChild(bahanBakuAction.init());
				}
			}
		});

		final Tabpanel tabpanelKonversi = new ais.ui.util.MyTabpanel();
		tabpanelKonversi.setParent(tabpanels);
		tabKonversi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKonversi.getChildren().size() == 0) {
					tabpanelKonversi.appendChild(konversiSatuanAction.init());
				}
			}
		});

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	private void generateCode() {
		String mynama = nama.getValue().trim();
		String key = mynama.length() > 3 ? mynama.substring(0, 3) : mynama;

		String countKey = (String) (HibernateUtil.currentSession().createCriteria(ItemMedis.class)
				.add(Restrictions.ilike("nama", key, MatchMode.START)).setProjection(Projections.max("kode"))
				.uniqueResult());

		Integer count = 0;
		if (countKey != null) {
			try {
				countKey = countKey.toUpperCase();
				count = Integer.parseInt(countKey.substring(countKey.length() - 3, countKey.length()));
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
		++count;
		String num = "00000000000000" + count;

		kode.setValue((key + num.substring(num.length() - 3, num.length())).toUpperCase());
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Item Medis wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama item agar kode dapat dibuat otomatis; (2) pastikan kolom kode tidak kosong; (3) simpan kembali data setelah kode terisi.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Item Medis wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Item Medis pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jenisItem.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Item Medis wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Jenis Item Medis pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Jenis ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (satuanItem.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Item Medis wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Satuan Item Medis pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Satuan ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (checkNamaItem()) {
			MyMessageboxConfig.showFormat("Mohon maaf, data item dengan nama \"{V1}\" sudah terdaftar di dalam sistem. Langkah yang dapat dilakukan: (1) gunakan nama item yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION, nama.getValue());
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (item.getId() != null) {
			item = (ItemMedis) session.load(ItemMedis.class, item.getId());
		}

		item.setKeterangan(keterangan.getValue());
		item.setKandungan(kandungan.getValue().trim());
		item.setGenerikItem((GenerikItem) generikItem.getAttribute("generikItem"));
		item.setKelasItem((KelasItem) kelasItem.getAttribute("kelasItem"));
		item.setKelompokItem((KelompokItem) (kelompokItem.getSelectedItem() == null ? null
				: kelompokItem.getSelectedItem().getValue()));
		item.setSemuahargasama(initHarga.semuahargasama.isChecked());
		item.setBatasMinimalStok(batasMinimalStok.getValue());
		item.setBolehretur(bolehretur.isChecked());
		item.setBarcode(barcode.getValue().trim());
		item.setKode(kode.getValue().trim());
		item.setNama(nama.getValue());
		item.setJenisItem(
				(JenisItemMedis) (jenisItem.getSelectedItem() == null ? null : jenisItem.getSelectedItem().getValue()));
		item.setSatuanItem(
				(SatuanItem) (satuanItem.getSelectedItem() == null ? null : satuanItem.getSelectedItem().getValue()));

		if (item.getId() != null) {
			Common.refreshUpdate(session, item);
		} else {
			generateCode();
			item.setKode(kode.getValue().trim());
			session.save(item);
		}

		if (gridHargaBeli != null) {
			List<Row> rows = gridHargaBeli.getRows().getChildren();
			for (Row row : rows) {
				MyDoublebox doublebox = (MyDoublebox) row.getChildren().get(1);
				Penyedia vendor = (Penyedia) row.getAttribute("vendor");
				HargaBeliItem hargaBeliItem = (HargaBeliItem) row.getAttribute("hargaBeliItem");
				if (hargaBeliItem == null) {
					hargaBeliItem = new HargaBeliItem();
				}
				hargaBeliItem.setPenyedia(vendor);
				hargaBeliItem.setHargaBeli(doublebox.getValue());
				hargaBeliItem.setItem(item);
				Common.refreshUpdate(session, hargaBeliItem);
			}
		}

		return initHarga.saveDetail(item, null);
	}

	private Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ItemMedis.class)

				.add((searchkandungan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkandungan.getValue().trim().equalsIgnoreCase("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kandungan", searchkandungan.getValue().trim(), MatchMode.ANYWHERE)))

				.add((searchGenerikItem == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchGenerikItem.getAttribute("generikItem") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("generikItem", searchGenerikItem.getAttribute("generikItem"))))

				.add((searchKelasItem == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchKelasItem.getAttribute("kelasItem") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kelasItem", searchKelasItem.getAttribute("kelasItem"))))

				.add(searchjenisItem.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisItem", searchjenisItem.getSelectedItem().getValue()))
				.add(searchsatuanItem.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("satuanItem", searchsatuanItem.getSelectedItem().getValue()))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchbarcode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchbarcode.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("barcode", searchbarcode.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<ItemMedis> item = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						ItemMedis.class);
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new ItemRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public Boolean checkNamaItem() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(ItemMedis.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.item.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.item.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
