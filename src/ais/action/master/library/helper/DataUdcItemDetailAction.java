package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DataUdcItem;
import ais.database.model.library.DataUdcItemDetail;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk data udc item detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code DataUdcItem dataUdcItem}, {@code MyGrid
 * grid}, {@code boolean edit}, {@code boolean delete}, {@code Textbox barcode}; pembacaan/pencarian ({@code
 * loadData()}, {@code loadBarcode()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code
 * delete}, {@code edit}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class DataUdcItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private DataUdcItem dataUdcItem;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;

	public DataUdcItemDetailAction(DataUdcItem dataUdcItem) {
		super();
		this.dataUdcItem = dataUdcItem;
		CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(DataUdcItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class DataUdcItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public DataUdcItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DataUdcItemDetail dataUdcItemDetail = (DataUdcItemDetail) data;

			new Label(dataUdcItemDetail.getItem() == null ? ""
					: dataUdcItemDetail.getItem().getIsbn() + " " + dataUdcItemDetail.getItem().getIssn())
							.setParent(row);

			RevisiHelper
					.createNewRevisi(DataUdcItemDetail.class, dataUdcItemDetail,
							dataUdcItemDetail.getItem() == null ? "" : dataUdcItemDetail.getItem().getNama())
					.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					dataUdcItemDetail.getKeterangan() == null ? "" : dataUdcItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(!edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					dataUdcItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (dataUdcItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(!delete);
			button.setTooltiptext("Hapus Data");
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
									Session session = HibernateUtil.currentSession();

									Item item = dataUdcItemDetail.getItem();
									item.setUdcItem(null);
									session.update(item);

									Common.refreshDelete(session, dataUdcItemDetail);

									loadData(null);
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
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<DataUdcItemDetail> dataUdcItemDetails = session.createCriteria(DataUdcItemDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("dataUdcItem", dataUdcItem)).list();

		ListModel strset = new SimpleListModel(dataUdcItemDetails);
		grid.setRowRenderer(new DataUdcItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("530px");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");

		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(DataUdcItemDetail.class)
						.setProjection(Projections.groupProperty("item"))
						.add(Restrictions.eq("dataUdcItem", dataUdcItem)).list();

				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items, false, true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (Item item : items) {
							DataUdcItemDetail dataUdcItemDetail = new DataUdcItemDetail();
							dataUdcItemDetail.setItem(item);
							dataUdcItemDetail.setKeterangan("");
							dataUdcItemDetail.setDataUdcItem(dataUdcItem);
							session.save(dataUdcItemDetail);

							item.setUdcItem(dataUdcItem.getUdcItem());
							session.update(item);
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("97%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("Barcode/ISBN/ISSN").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Barcode/ISBN/ISSN harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		ItemPunyaBarcode itemPunyaBarcode = barcode == null || barcode.trim().equals("")
				|| barcode.trim().equals("null")
						? null
						: (ItemPunyaBarcode) session.createCriteria(ItemPunyaBarcode.class)
								.add(Restrictions.ilike("barcode", barcode, MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult();

		Item item = null;
		if (itemPunyaBarcode != null) {
			item = itemPunyaBarcode.getItem();
		} else {
			item = (Item) session.createCriteria(Item.class)
					.add(Restrictions.or(Restrictions.ilike("isbn10", barcode, MatchMode.EXACT),
							Restrictions.or(Restrictions.ilike("isbn", barcode, MatchMode.EXACT),
									Restrictions.ilike("issn", barcode, MatchMode.EXACT))))
					.setMaxResults(1).uniqueResult();
		}

		if (item == null) {
			MyMessageboxConfig.show("Barcode " + barcode + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		DataUdcItemDetail dataUdcItemDetail = new DataUdcItemDetail();
		dataUdcItemDetail.setItem(item);
		dataUdcItemDetail.setKeterangan("");
		dataUdcItemDetail.setDataUdcItem(dataUdcItem);
		session.save(dataUdcItemDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
