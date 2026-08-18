package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaTerbit;
import ais.database.model.library.Perpustakaan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ItemPunyaTerbitHelper {

	private MyGrid gridTerbit;
	private boolean add = false;
	private boolean delete = false;
	private boolean edit = false;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Item item;

	public ItemPunyaTerbitHelper(MyGrid gridTerbit) {
		this.gridTerbit = gridTerbit;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
	}

	public Borderlayout initDetail(final Item item) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		this.item = item;
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Hbox hbox = new Hbox();
		hbox.setHeight("30px");
		hbox.setParent(north);

		new Label(ais.common.Common.getBahasaConfig("Satuan Kerja")).setParent(hbox);
		new Space().setParent(hbox);
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setParent(hbox);
		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				perpustakaan.setSatuanKerja((SatuanKerja) satuanKerja
						.getAttribute("satuanKerja"));
				loadDataDetail(item);
			}
		});

		new Label(ais.common.Common.getBahasaConfig("Perpustakaan")).setParent(hbox);
		new Space().setParent(hbox);
		perpustakaan = new AmbilDataPerpustakaanBanbox();
		perpustakaan.setParent(hbox);
		perpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(item);
			}
		});

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Terbit", "/img/new.gif");
		add.setVisible(ItemPunyaTerbitHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new ItemPunyaTerbit());
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(item);
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridTerbit);
		gridTerbit.setParent(center);
		gridTerbit.setWidth("100%");
		gridTerbit.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridTerbit);

		MyColumnConfig column = new MyColumnConfig("Satuan Kerja");
		column.setParent(columns);

		column = new MyColumnConfig("Perpustakaan");
		column.setParent(columns);

		column = new MyColumnConfig("Mulai");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Sampai");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Tgl Dibuat");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(item);

		return borderlayout;
	}

	private void init(final ItemPunyaTerbit itemPunyaTerbit) throws Exception {
		final MyWindow window = new MyWindow("Item Terbit", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage()
				.getFirstRoot());
		window.setHeight("97%");
		window.setWidth("97%");

		final AmbilDataSatuanKerjaBanbox satuanKerja = new AmbilDataSatuanKerjaBanbox(
				true);
		satuanKerja.setAttribute("satuanKerja",
				itemPunyaTerbit.getSatuanKerja());
		satuanKerja.setValue(itemPunyaTerbit.getSatuanKerja() == null ? ""
				: itemPunyaTerbit.getSatuanKerja().toString());
		final AmbilDataPerpustakaanBanbox perpustakaan = new AmbilDataPerpustakaanBanbox();
		perpustakaan.setAttribute("perpustakaan",
				itemPunyaTerbit.getPerpustakaan());
		perpustakaan.setValue(itemPunyaTerbit.getPerpustakaan() == null ? ""
				: itemPunyaTerbit.getPerpustakaan().getNama());

		final MyDatebox mulai = new MyDatebox(itemPunyaTerbit.getMulai());
		mulai.setFormat(Common.dateFormat6.get().toPattern());
		final MyDatebox sampai = new MyDatebox(itemPunyaTerbit.getSampai());
		sampai.setFormat(Common.dateFormat6.get().toPattern());

		final MyCkEditor content = new MyCkEditor();
		content.setValue(itemPunyaTerbit.getContent() == null
				|| itemPunyaTerbit.getContent().trim().equals("") ? (item
				.getAbstrak() != null && !item.getAbstrak().trim().equals("") ? item
				.getAbstrak() : item.getAbstrakEn() != null
				&& !item.getAbstrakEn().trim().equals("") ? item.getAbstrakEn()
				: item.getCatatan())
				: itemPunyaTerbit.getContent());
		content.setHeight("100%");
		content.setWidth("100%");

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
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setWidth("70%");
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setParent(borderlayout);
		east.appendChild(content);

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
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (mulai.getValue() == null) {
					MyMessageboxConfig.show("Mulai terbit harus diisi", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (content.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Content harus diisi", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (satuanKerja.getAttribute("satuanKerja") == null) {
					MyMessageboxConfig.show("Satuan kerja harus dipilih", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (perpustakaan.getAttribute("perpustakaan") == null) {
					MyMessageboxConfig.show("Perpustakaan harus dipilih", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Rows rows = gridTerbit.getRows() == null ? new Rows()
						: gridTerbit.getRows();
				rows.setParent(gridTerbit);

				itemPunyaTerbit.setItem(item);
				itemPunyaTerbit.setMulai(mulai.getValue());
				itemPunyaTerbit.setSampai(sampai.getValue());
				itemPunyaTerbit.setSatuanKerja((SatuanKerja) satuanKerja
						.getAttribute("satuanKerja"));

				itemPunyaTerbit.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
				itemPunyaTerbit.setPerpustakaan((Perpustakaan) perpustakaan
						.getAttribute("perpustakaan"));
				itemPunyaTerbit.setContent(content.getValue());

				if (item.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.saveOrUpdate(itemPunyaTerbit);
				}

				if (item.getId() == null) {
					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setParent(rows);
					try {
						initRow(row, itemPunyaTerbit);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
					}
				} else {
					loadDataDetail(item);
				}
				window.detach();
			}
		});
		save.setParent(toolbar);

		window.onModal();
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) {

		List<ItemPunyaTerbit> itemPunyaTerbits = item == null
				|| item.getId() == null ? new ArrayList<ItemPunyaTerbit>()
				: HibernateUtil
						.currentSession()
						.createCriteria(ItemPunyaTerbit.class)
						.addOrder(Order.desc("id"))
						.add(satuanKerja.getAttribute("satuanKerja") == null ? Restrictions
								.sqlRestriction("1=1") : Restrictions.eq(
								"satuanKerja",
								satuanKerja.getAttribute("satuanKerja")))
						.add(perpustakaan.getAttribute("perpustakaan") == null ? Restrictions
								.sqlRestriction("1=1") : Restrictions.eq(
								"perpustakaan",
								perpustakaan.getAttribute("perpustakaan")))
						.add(Restrictions.eq("item", item)).list();

		Rows rows = gridTerbit.getRows() == null ? new Rows() : gridTerbit
				.getRows();
		Common.clear(rows);
		rows.setParent(gridTerbit);

		for (ItemPunyaTerbit itemPunyaTerbit : itemPunyaTerbits) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			try {
				initRow(row, itemPunyaTerbit);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}
		}
	}

	public void initRow(final Row row, final ItemPunyaTerbit itemPunyaTerbit)
			throws Exception {
		row.setValign("top");row.setAttribute("itemPunyaTerbit", itemPunyaTerbit);

		new Label(itemPunyaTerbit.getSatuanKerja() == null ? ""
				: itemPunyaTerbit.getSatuanKerja().toString()).setParent(row);

		new Label(itemPunyaTerbit.getPerpustakaan() == null ? ""
				: itemPunyaTerbit.getPerpustakaan().getNama()).setParent(row);

		new Label(itemPunyaTerbit.getMulai() == null ? ""
				: Common.dateFormat6.get().format(itemPunyaTerbit.getMulai()))
				.setParent(row);

		new Label(itemPunyaTerbit.getSampai() == null ? ""
				: Common.dateFormat6.get().format(itemPunyaTerbit.getSampai()))
				.setParent(row);

		new Label(
				itemPunyaTerbit.getTanggal_dirubah() == null ? ""
						: Common.dateFormat.get().format(itemPunyaTerbit
								.getTanggal_dirubah())).setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
		button.setTooltiptext("Edit Data");
		button.setVisible(edit);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init(itemPunyaTerbit);
			}
		});

		button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (itemPunyaTerbit.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(itemPunyaTerbit);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
