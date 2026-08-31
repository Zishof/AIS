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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.PermintaanPengadaanItemDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.Penyedia;
import ais.database.model.library.PermintaanPengadaanItem;
import ais.database.model.library.PermintaanPengadaanItemDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk permintaan pengadaan item detail. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PermintaanPengadaanItem
 * permintaanPengadaanItem}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}, {@code Textbox
 * barcode}; pembacaan/pencarian ({@code loadData()}, {@code loadBarcode()}); operasi domain lain ({@code
 * display()}); konfigurasi constructor: {@code delete}, {@code edit}. Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class PermintaanPengadaanItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PermintaanPengadaanItem permintaanPengadaanItem;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;

	public PermintaanPengadaanItemDetailAction(PermintaanPengadaanItem permintaanPengadaanItem) {
		super();
		this.permintaanPengadaanItem = permintaanPengadaanItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PermintaanPengadaanItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class PermintaanPengadaanItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PermintaanPengadaanItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PermintaanPengadaanItemDetail permintaanPengadaanItemDetail = (PermintaanPengadaanItemDetail) data;
			LibraryUtil.checkRef(permintaanPengadaanItemDetail.getItem());

			Image image = LibraryUtil.generateImage(permintaanPengadaanItemDetail.getItem());
			image.setWidth("100%");
			image.setParent(row);

			final MyDoublebox jumlah = new MyDoublebox(permintaanPengadaanItemDetail.getJumlah() == null ? 0.0
					: permintaanPengadaanItemDetail.getJumlah());

			new Label(permintaanPengadaanItemDetail.getItem() == null ? ""
					: permintaanPengadaanItemDetail.getItem().getIsbn() + " "
							+ permintaanPengadaanItemDetail.getItem().getIssn()).setParent(row);

			RevisiHelper.createNewRevisi(PermintaanPengadaanItemDetail.class, permintaanPengadaanItemDetail,
					permintaanPengadaanItemDetail.getItem() == null ? ""
							: permintaanPengadaanItemDetail.getItem().getNama())
					.setParent(row);

			(jumlah).setParent(row);
			jumlah.setDisabled(
					permintaanPengadaanItemDetail.getPermintaanPengadaanItem().getDisetujuiOleh() != null || !edit);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					permintaanPengadaanItemDetail.setJumlah(jumlah.getValue());
					Common.refreshUpdate(session, (permintaanPengadaanItemDetail));
				}
			});

			final AmbilDataPenyediaBanbox penyedia = new AmbilDataPenyediaBanbox();
			penyedia.setParent(row);
			penyedia.setWidth("90%");
			penyedia.setDisabled(
					permintaanPengadaanItemDetail.getPermintaanPengadaanItem().getDisetujuiOleh() != null || !edit);
			penyedia.setAttribute("penyedia", permintaanPengadaanItemDetail.getPenyedia());
			penyedia.setValue(permintaanPengadaanItemDetail.getPenyedia() == null ? ""
					: permintaanPengadaanItemDetail.getPenyedia().getNama());
			penyedia.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Penyedia myPenyedia = (Penyedia) penyedia.getAttribute("penyedia");
					Session session = HibernateUtil.currentSession();
					permintaanPengadaanItemDetail.setPenyedia(myPenyedia);
					Common.refreshUpdate(session, (permintaanPengadaanItemDetail));

					permintaanPengadaanItemDetail.getItem().setDefaultPenyedia(myPenyedia);
					Common.refreshUpdate(session, (permintaanPengadaanItemDetail.getItem()));
				}
			});

			final MyTextbox keterangan = new MyTextbox(permintaanPengadaanItemDetail.getKeterangan() == null ? ""
					: permintaanPengadaanItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					permintaanPengadaanItemDetail.getPermintaanPengadaanItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					permintaanPengadaanItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (permintaanPengadaanItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(
					permintaanPengadaanItemDetail.getPermintaanPengadaanItem().getDisetujuiOleh() != null || !delete);
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

									Common.refreshDelete(permintaanPengadaanItemDetail);

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
		List<PermintaanPengadaanItemDetail> permintaanPengadaanItemDetails = session
				.createCriteria(PermintaanPengadaanItemDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("permintaanPengadaanItem", permintaanPengadaanItem)).list();

		ListModel strset = new SimpleListModel(permintaanPengadaanItemDetails);
		grid.setRowRenderer(new PermintaanPengadaanItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Permintaan Pengadaan Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(permintaanPengadaanItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(PermintaanPengadaanItemDetail.class)
						.setProjection(Projections.groupProperty("item"))
						.add(Restrictions.eq("permintaanPengadaanItem", permintaanPengadaanItem)).list();

				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						PermintaanPengadaanItemDetailDao permintaanPengadaanItemDetailDao = DaoFactory.getInstance()
								.getPermintaanPengadaanItemDetailDao();
						for (Item item : items) {
							PermintaanPengadaanItemDetail permintaanPengadaanItemDetail = new PermintaanPengadaanItemDetail();
							permintaanPengadaanItemDetail.setItem(item);
							permintaanPengadaanItemDetail.setPenyedia(item.getDefaultPenyedia());
							permintaanPengadaanItemDetail.setJumlah(0.0);
							permintaanPengadaanItemDetail.setKeterangan("");
							permintaanPengadaanItemDetail.setPermintaanPengadaanItem(permintaanPengadaanItem);
							permintaanPengadaanItemDetailDao.save(permintaanPengadaanItemDetail);
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

		button = new MyToolbarbuttonConfig("Ambil Data Item Berdasarkan Stok", "/img/add_item.png");
		button.setDisabled(permintaanPengadaanItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(PermintaanPengadaanItemDetail.class)
						.setProjection(Projections.groupProperty("item"))
						.add(Restrictions.eq("permintaanPengadaanItem", permintaanPengadaanItem)).list();

				AmbilDataItemBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemBanyakBerdasarkanStok(items,
						permintaanPengadaanItem.getPerpustakaan(), false);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						PermintaanPengadaanItemDetailDao permintaanPengadaanItemDetailDao = DaoFactory.getInstance()
								.getPermintaanPengadaanItemDetailDao();
						for (Item item : items) {
							PermintaanPengadaanItemDetail permintaanPengadaanItemDetail = new PermintaanPengadaanItemDetail();
							permintaanPengadaanItemDetail.setItem(item);
							permintaanPengadaanItemDetail.setPenyedia(item.getDefaultPenyedia());
							permintaanPengadaanItemDetail.setJumlah(0.0);
							permintaanPengadaanItemDetail.setKeterangan("");
							permintaanPengadaanItemDetail.setPermintaanPengadaanItem(permintaanPengadaanItem);
							permintaanPengadaanItemDetailDao.save(permintaanPengadaanItemDetail);
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
		barcode.setDisabled(permintaanPengadaanItem.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setDisabled(permintaanPengadaanItem.getDisetujuiOleh() != null);
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penyedia");

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
		ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.ilike("barcode", barcode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();

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

		PermintaanPengadaanItemDetail permintaanPengadaanItemDetail = new PermintaanPengadaanItemDetail();
		permintaanPengadaanItemDetail.setItem(item);
		permintaanPengadaanItemDetail.setPenyedia(item.getDefaultPenyedia());
		permintaanPengadaanItemDetail.setJumlah(1.0);
		permintaanPengadaanItemDetail.setKeterangan("");
		permintaanPengadaanItemDetail.setPermintaanPengadaanItem(permintaanPengadaanItem);
		session.save(permintaanPengadaanItemDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
