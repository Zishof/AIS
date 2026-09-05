package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.format1.library.LaporanBarcodeTerimaPengadaanItem;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.TerimaPengadaanItem;
import ais.database.model.library.TerimaPengadaanItemDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk terima pengadaan item detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code TerimaPengadaanItem
 * terimaPengadaanItem}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}; pembacaan/pencarian
 * ({@code loadData()}); operasi domain lain ({@code display()}, {@code generateBarcode()}); konfigurasi
 * constructor: {@code delete}, {@code edit}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class TerimaPengadaanItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private TerimaPengadaanItem terimaPengadaanItem;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	public TerimaPengadaanItemDetailAction(TerimaPengadaanItem terimaPengadaanItem) {
		super();
		this.terimaPengadaanItem = terimaPengadaanItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(TerimaPengadaanItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TerimaPengadaanItemDetailAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TerimaPengadaanItemDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TerimaPengadaanItemDetailAction
	 */
	class TerimaPengadaanItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public TerimaPengadaanItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final TerimaPengadaanItemDetail terimaPengadaanItemDetail = (TerimaPengadaanItemDetail) data;

			Image image = LibraryUtil.generateImage(terimaPengadaanItemDetail.getItem());
			image.setWidth("100%");
			image.setParent(row);

			// final Label jumlah = new Label(
			// terimaPengadaanItemDetail.getTransferPengadaanItemDetail().getJumlah() ==
			// null ? "0.0"
			// : Common.numberFormat.get()
			// .get().format(terimaPengadaanItemDetail.getTransferPengadaanItemDetail().getJumlah()));

			final MyDoublebox diterima = new MyDoublebox(
					terimaPengadaanItemDetail.getDiterima() == null ? 0.0 : terimaPengadaanItemDetail.getDiterima());

			// final Label sisa = new
			// Label(terimaPengadaanItemDetail.getTransferPengadaanItemDetail().getJumlah()
			// == null
			// || terimaPengadaanItemDetail.getDiterima() == null
			// ? ""
			// : Common.numberFormat.get()
			// .get().format(terimaPengadaanItemDetail.getTransferPengadaanItemDetail().getJumlah()
			// - terimaPengadaanItemDetail.getDiterima()));

			new Label(terimaPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
					: terimaPengadaanItemDetail.getItemPunyaBarcode().getBarcode()).setParent(row);

			new Label(terimaPengadaanItemDetail.getItem() == null ? ""
					: terimaPengadaanItemDetail.getItem().getIsbn() + " "
							+ terimaPengadaanItemDetail.getItem().getIssn()).setParent(row);

			RevisiHelper.createNewRevisi(TerimaPengadaanItemDetail.class, terimaPengadaanItemDetail,
					terimaPengadaanItemDetail.getItem() == null ? "" : terimaPengadaanItemDetail.getItem().getNama())
					.setParent(row);

			(diterima).setParent(row);
			diterima.setDisabled(
					terimaPengadaanItemDetail.getTerimaPengadaanItem().getDisetujuiOleh() != null || !edit);
			diterima.setStyle("text-align:right");
			diterima.setWidth("90%");
			diterima.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					terimaPengadaanItemDetail.setDiterima(diterima.getValue());
					Common.refreshUpdate(session, (terimaPengadaanItemDetail));

				}
			});

			final MyTextbox keterangan = new MyTextbox(
					terimaPengadaanItemDetail.getKeterangan() == null ? "" : terimaPengadaanItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					terimaPengadaanItemDetail.getTerimaPengadaanItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					terimaPengadaanItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (terimaPengadaanItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(
					terimaPengadaanItemDetail.getTerimaPengadaanItem().getDisetujuiOleh() != null || !delete);
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

											Common.refreshDelete(terimaPengadaanItemDetail);

											loadData(null);

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
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<TerimaPengadaanItemDetail> terimaPengadaanItemDetails = session
				.createCriteria(TerimaPengadaanItemDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("terimaPengadaanItem", terimaPengadaanItem)).list();

		ListModel strset = new SimpleListModel(terimaPengadaanItemDetails);
		grid.setRowRenderer(new TerimaPengadaanItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("760px");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);

		MyToolbarbuttonConfig barcode = new MyToolbarbuttonConfig("Generate Barcode", "/img/album.png");
		barcode.setParent(toolbar);
		barcode.setDisabled(terimaPengadaanItem.getDisetujuiOleh() == null);
		barcode.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				generateBarcode(terimaPengadaanItem);

			}
		});

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabJawaban = new MyTabConfig("Item");
		tabJawaban.setParent(tabs);

		final MyTabConfig tabSoal = new MyTabConfig("Barcode");
		tabSoal.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();
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
		column.setLabel("Barcode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("ISBN/ISSN");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diterima");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);

		final Tabpanel tabpanelKedua = new ais.ui.util.MyTabpanel();
		tabpanelKedua.setParent(tabpanels);

		tabSoal.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKedua.getChildren().isEmpty()) {
					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(tabpanelKedua);

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					MyIframe include = new MyIframe("/pages/master/library/barcode_item.zul?terimaPengadaanItem="
							+ (terimaPengadaanItem == null || terimaPengadaanItem.getId() == null ? -1L : terimaPengadaanItem.getId()));
					include.setParent(center);
				}
			}
		});
	}

	protected void generateBarcode(final TerimaPengadaanItem terimaPengadaanItem) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser dibuatOleh = Common.getCurrentUser();
				Session session = HibernateUtil.currentNativeSession();

				@SuppressWarnings("unchecked")
				List<TerimaPengadaanItemDetail> terimaPengadaanItemDetails = session
						.createCriteria(TerimaPengadaanItemDetail.class).addOrder(Order.desc("id"))
						.add(Restrictions.eq("terimaPengadaanItem", terimaPengadaanItem)).list();
				for (TerimaPengadaanItemDetail terimaPengadaanItemDetail : terimaPengadaanItemDetails) {
					System.out.println("terimaPengadaanItemDetail => " + terimaPengadaanItemDetail);
					BatchItemPunyaBarcode batchItemPunyaBarcode = (BatchItemPunyaBarcode) session
							.createCriteria(BatchItemPunyaBarcode.class)
							.add(Restrictions.eq("terimaPengadaanItem", terimaPengadaanItem))
							.add(Restrictions.eq("item", terimaPengadaanItemDetail.getItem())).setMaxResults(1)
							.uniqueResult();
					if (batchItemPunyaBarcode == null) {
						batchItemPunyaBarcode = new BatchItemPunyaBarcode();
						batchItemPunyaBarcode.setBerasalDari(BatchItemPunyaBarcode.TERIMA);
						batchItemPunyaBarcode.setDibuatOleh(dibuatOleh);
						batchItemPunyaBarcode.setItem(terimaPengadaanItemDetail.getItem());
						batchItemPunyaBarcode.setTerimaPengadaanItem(terimaPengadaanItem);
						batchItemPunyaBarcode.setTanggal(terimaPengadaanItem.getTanggalPersetujuan());

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, batchItemPunyaBarcode);
						session.getTransaction().commit();
					}

					terimaPengadaanItemDetail.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, terimaPengadaanItemDetail);
					session.getTransaction().commit();

					Item item = terimaPengadaanItemDetail.getItem();

					ItemPunyaBarcode itemPunyaBarcode = terimaPengadaanItemDetail.getItemPunyaBarcode();

					if (itemPunyaBarcode == null) {
						itemPunyaBarcode = (ItemPunyaBarcode) (session.createCriteria(ItemPunyaBarcode.class)
								.add(Restrictions.eq("batchItemPunyaBarcode", batchItemPunyaBarcode))
								.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult());
					}
					if (itemPunyaBarcode == null) {
						itemPunyaBarcode = new ItemPunyaBarcode();
						itemPunyaBarcode.setIndexke(1);
						itemPunyaBarcode.setItem(item);
						itemPunyaBarcode.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
						itemPunyaBarcode.setBarcode(BarcodeCommon.generateCode(batchItemPunyaBarcode));
						itemPunyaBarcode.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
						itemPunyaBarcode.setPerpustakaan(terimaPengadaanItem.getPerpustakaan());

					} else {
						itemPunyaBarcode.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
					}

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, itemPunyaBarcode);
					session.getTransaction().commit();

				}

				HibernateUtil.closeSession();

				LaporanBarcodeTerimaPengadaanItem laporanBarcodeItem = new LaporanBarcodeTerimaPengadaanItem(
						terimaPengadaanItem);
				laporanBarcodeItem.setTitle("Cetak Barcode");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanBarcodeItem);
				laporanBarcodeItem.setHeight("95%");
				laporanBarcodeItem.setWidth("90%");
				laporanBarcodeItem.setClosable(true);
				laporanBarcodeItem.onModal();
			}
		});

	}

}
