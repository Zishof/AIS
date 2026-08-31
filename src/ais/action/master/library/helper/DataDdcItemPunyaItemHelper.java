package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DataDdcItem;
import ais.database.model.library.DataDdcItemDetail;
import ais.database.model.library.Item;
import ais.ui.util.MyTextbox;

/**
 * Helper ZK untuk mengelola daftar item ({@link Item}, lewat baris penghubung
 * {@link DataDdcItemDetail}) yang tergabung dalam satu klasifikasi DDC (Dewey Decimal Classification)
 * — {@link DataDdcItem} — pada modul perpustakaan (relasi "punya banyak"). Menyediakan toolbar
 * tambah (membuka dialog pemilihan item {@code AmbilDataItemBanyak}, dengan daftar item yang sudah
 * ada di grid dikecualikan dari pilihan) dan refresh, keduanya disembunyikan/diaktifkan sesuai hak
 * akses ({@link CommonPrivilages#CREATE}/{@code UPDATE}/{@code DELETE}). Menambahkan item ke
 * klasifikasi ini juga menuliskan balik referensi klasifikasi DDC ke entitas {@link Item} itu sendiri
 * ({@code item.setDdcItem(...)}), dan menghapus baris mengosongkannya kembali — menjaga agar setiap
 * item hanya tergabung pada satu klasifikasi DDC pada satu waktu.
 */
public class DataDdcItemPunyaItemHelper {

	private MyGrid gridItem;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;

	/**
	 * Membuat helper terikat pada satu komponen grid target, sekaligus mengevaluasi hak akses
	 * tambah, ubah, dan hapus pengguna saat ini.
	 *
	 * @param gridItem komponen grid ZK tempat baris item dirender
	 */
	public DataDdcItemPunyaItemHelper(MyGrid gridItem) {
		this.gridItem = gridItem;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun kerangka layout detail (toolbar tambah/refresh + kolom grid) dan langsung memuat
	 * data item untuk klasifikasi DDC yang diberikan. Tombol tambah membuka dialog pemilihan item
	 * banyak sekaligus, mengecualikan item yang sudah ada di grid; setiap item terpilih disimpan
	 * sebagai baris {@link DataDdcItemDetail} baru dan referensi klasifikasi DDC-nya dituliskan
	 * balik ke entitas {@link Item}.
	 *
	 * @param dataDdcItem klasifikasi DDC yang daftar itemnya ditampilkan/dikelola
	 * @return komponen {@link Borderlayout} berisi toolbar dan grid item yang siap dipasang ke layar pemanggil
	 * @throws Exception diteruskan apa adanya dari kegagalan pemuatan/pembangunan komponen
	 */
	public Borderlayout initDetail(final DataDdcItem dataDdcItem) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Item", "/img/new.gif");
		add.setVisible(DataDdcItemPunyaItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Item> items = new ArrayList<Item>();
				List<Row> myrows = gridItem.getRows().getChildren();
				for (Row row : myrows) {
					items.add(((DataDdcItemDetail) row.getAttribute("dataDdcItemDetail")).getItem());
				}
				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items, true, false);
				ambilDataItemBanyak.setHeight("95%");
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						for (Item item : items) {
							DataDdcItemDetail dataDdcItemDetail = new DataDdcItemDetail();
							dataDdcItemDetail.setItem(item);
							dataDdcItemDetail.setKeterangan("");
							dataDdcItemDetail.setDataDdcItem(dataDdcItem);

							if (dataDdcItem.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(dataDdcItemDetail);
								item.setDdcItem(dataDdcItem.getDdcItem());
								session.update(item);

							}

							Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
							rows.setParent(gridItem);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, dataDdcItemDetail);
						}
					}
				});

				ambilDataItemBanyak.onModal();

			}
		});

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(dataDdcItem);
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridItem);
		gridItem.setParent(center);
		gridItem.setWidth("100%");
		gridItem.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridItem);

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

		loadDataDetail(dataDdcItem);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final DataDdcItem dataDdcItem) throws Exception {

		List<DataDdcItemDetail> dataDdcItemDetails = dataDdcItem == null || dataDdcItem.getId() == null
				? new ArrayList<DataDdcItemDetail>()
				: HibernateUtil.currentSession().createCriteria(DataDdcItemDetail.class)
						.add(Restrictions.eq("dataDdcItem", dataDdcItem)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (DataDdcItemDetail dataDdcItemDetail : dataDdcItemDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, dataDdcItemDetail);
		}
	}

	/**
	 * Mengisi satu baris grid dengan data item (ISBN/ISSN, nama dengan link riwayat revisi), kolom
	 * keterangan yang dapat diedit langsung (tersimpan otomatis saat berubah, bila hak ubah dimiliki),
	 * dan tombol hapus beserta event handler-nya (dialog konfirmasi; menghapus baris penghubung dan
	 * mengosongkan kembali referensi klasifikasi DDC pada item terkait bila dikonfirmasi).
	 *
	 * @param row               baris grid yang diisi
	 * @param dataDdcItemDetail baris penghubung item-klasifikasi DDC yang direpresentasikan baris ini
	 * @throws Exception diteruskan apa adanya dari kegagalan pembangunan komponen
	 */
	public void initRow(final Row row, final DataDdcItemDetail dataDdcItemDetail) throws Exception {
		row.setValign("top");row.setAttribute("dataDdcItemDetail", dataDdcItemDetail);

		new Label(dataDdcItemDetail.getItem() == null ? ""
				: dataDdcItemDetail.getItem().getIsbn() + " " + dataDdcItemDetail.getItem().getIssn()).setParent(row);

		RevisiHelper
				.createNewRevisi(DataDdcItemDetail.class, dataDdcItemDetail,
						dataDdcItemDetail.getItem() == null ? "" : dataDdcItemDetail.getItem().getNama())
				.setParent(row);

		final MyTextbox keterangan = new MyTextbox(
				dataDdcItemDetail.getKeterangan() == null ? "" : dataDdcItemDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);
		keterangan.setDisabled(!edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dataDdcItemDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("dataDdcItemDetail", dataDdcItemDetail);
				if (dataDdcItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (dataDdcItemDetail));
				}
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							if (dataDdcItemDetail.getId() != null) {
								Session session = HibernateUtil.currentSession();
								Item item = dataDdcItemDetail.getItem();
								item.setDdcItem(null);
								session.update(item);

								session.delete(dataDdcItemDetail);
							}
							row.setVisible(false);
						}

					}
				});

			}
		});
	}

}
