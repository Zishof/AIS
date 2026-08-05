package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataItemMedisBanyak;
import ais.action.master.sirs.util.CommonItem;
import ais.action.master.sirs.util.CommonItem.InitHarga;
import ais.action.master.sirs.util.CommonTarifItem;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Biaya;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.TarifKhusus;
import ais.database.model.sirs.TarifKhususPunyaItem;
import ais.ui.util.MyTextbox;

public class TarifKhususPunyaItemDetailAction extends Tabpanel implements OnSave {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private TarifKhusus tarifKhusus;
	private Paging paging;
	private Grid grid;

	private InitHarga initHarga = new InitHarga();

	public TarifKhususPunyaItemDetailAction(TarifKhusus tarifKhusus) {
		super();
		this.tarifKhusus = tarifKhusus;

	}

	class TarifKhususPunyaItemRenderer extends ais.ui.util.MyRowRenderer {

		public TarifKhususPunyaItemRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final TarifKhususPunyaItem tarifKhususPunyaItem = (TarifKhususPunyaItem) data;

			final ItemMedis item = tarifKhususPunyaItem.getItem();

			new Label(item.getKode()).setParent(arg0);
			new Label(item.getNama()).setParent(arg0);
			new Label(item.getJenisItem() == null ? "" : item.getJenisItem().getNama()).setParent(arg0);

			final MyTextbox keterangan = new MyTextbox(
					tarifKhususPunyaItem.getKeterangan() == null ? "" : tarifKhususPunyaItem.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);

			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					tarifKhususPunyaItem.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (tarifKhususPunyaItem));
				}
			});

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Tarif", "/img/edit.gif");
			button.setTooltiptext("Tarif");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(tarifKhususPunyaItem);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(tarifKhususPunyaItem);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/TarifKhususPunyaItemDetailAction.java:143");
											MyMessageboxConfig.show(Common.pesan(
																"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
																	e.getMessage()));
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

		}
	}

	private MyTextbox kode;
	private MyTextbox nama;

	private TarifKhususPunyaItem tarifKhususPunyaItem;

	private Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("item.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("item.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TarifKhususPunyaItem.class)

				.createAlias("item", "item")

				.add(critKode).add(critNama)

				.add(Restrictions.eq("tarifKhusus", tarifKhusus));
		if (order)
			criteria.addOrder(Order.asc("item.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TarifKhususPunyaItem> tarifKhususPunyaItems = tarifKhusus == null
				|| tarifKhusus.getId() == null
						? new ArrayList<TarifKhususPunyaItem>()
						: ConstantValues
								.simpleList(
										initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
												.setFirstResult(Common.ROWS_COUNT_ON_PAGE
														* (paging == null ? 0 : paging.getActivePage())),
										TarifKhususPunyaItem.class);

		ListModel strset = new SimpleListModel(tarifKhususPunyaItems);
		grid.setRowRenderer(new TarifKhususPunyaItemRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public void display() {

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new Caption("Daftar item dan layanan TarifKhusus"));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Obat-Obatan", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(TarifKhususPunyaItem.class)
						.add(Restrictions.isNotNull("item")).setProjection(Projections.groupProperty("item.id"))
						.add(Restrictions.eq("tarifKhusus", tarifKhusus)), ItemMedis.class, false);

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {
							final TarifKhususPunyaItem tarifKhususPunyaItem = new TarifKhususPunyaItem();
							tarifKhususPunyaItem.setItem(item);
							tarifKhususPunyaItem.setKeterangan("");
							tarifKhususPunyaItem.setTarifKhusus(tarifKhusus);
							session.save(tarifKhususPunyaItem);

							List<KelasPerawatan> kelasPerawatans = session.createCriteria(KelasPerawatan.class)
									.addOrder(Order.asc("id")).list();

							for (KelasPerawatan kelasPerawatan : kelasPerawatans) {
								HargaJualItem hargaJualItemDari = CommonTarifItem.getHargaJualItem(item,
										kelasPerawatan);

								HargaJualItem hargaJualItemKe = (HargaJualItem) hargaJualItemDari.clone();
								hargaJualItemKe.setTarifKhususPunyaItem(tarifKhususPunyaItem);
								hargaJualItemKe.setItem(null);
								session.save(hargaJualItemKe);

								System.out.println("Saving hargaJualItemKe " + hargaJualItemKe);

								List<Biaya> biayasDari = session.createCriteria(Biaya.class)
										.add(Restrictions.isNull("detailTransaksiLayanan"))
										.add(Restrictions.isNull("detailTransaksi"))

										.add(Restrictions.eq("hargaJualItem", hargaJualItemDari)).list();

								System.out.println("Saving " + biayasDari);

								for (Biaya biaya : biayasDari) {
									Biaya newBiaya = (Biaya) biaya.clone();
									newBiaya.setId(null);
									newBiaya.setHargaJualItem(hargaJualItemKe);
									session.save(newBiaya);
								}
							}
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("95%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Download Biaya", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonItem.onDownloadBiaya(event, tarifKhusus);
			}

		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Upload Biaya", "/img/edit.gif");
		button.setUpload("true");
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonItem.onUploadBiaya(event, tarifKhusus);
			}

		});
		button.setParent(toolbar);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
		toolbar.appendChild(kode = new MyTextbox());
		kode.setWidth("80px");
		kode.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton search;
		toolbar.appendChild(search = new ais.ui.util.MyToolbarbuttonConfig("", "/img/search.gif"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);
	}

	@SuppressWarnings("deprecation")
	private void init(final TarifKhususPunyaItem tarifKhususPunyaItem) throws Exception {
		this.tarifKhususPunyaItem = tarifKhususPunyaItem;
		final ItemMedis item = tarifKhususPunyaItem.getItem();
		final Window addWindow = new Window();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Tarif Item atau Layanan Tarif Khusus");
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

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama ")));
		row.appendChild(new Label(item.getNama()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode ")));
		row.appendChild(new Label(item.getKode()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis ")));
		row.appendChild(new Label(item.getJenisItem() == null ? "" : item.getJenisItem().toString()));

		row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "1,1,1,3");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Boleh retur")));
		row.appendChild(new Label(item.getBolehretur() ? "Ya" : "Tidak"));

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(new Label(item.getKeterangan()));

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

		final Tab tabPenjualan = new Tab("Harga");
		tabPenjualan.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		initHarga.initHargaJual(item, tabpanel, this, tarifKhususPunyaItem);

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
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	public boolean onSave(Event event) throws Exception {

		Session session = HibernateUtil.currentSession();

		tarifKhususPunyaItem.setSemuahargasama(initHarga.semuahargasama.isChecked());
		session.update(tarifKhususPunyaItem);

		initHarga.saveDetail(tarifKhususPunyaItem.getItem(), tarifKhususPunyaItem);

		return true;
	}

}
