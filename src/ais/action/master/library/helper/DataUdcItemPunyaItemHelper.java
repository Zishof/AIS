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
import ais.database.model.library.DataUdcItem;
import ais.database.model.library.DataUdcItemDetail;
import ais.database.model.library.Item;
import ais.ui.util.MyTextbox;

/**
 * Helper UI ZK untuk mengelola relasi item pustaka ({@link Item}, lewat {@link
 * DataUdcItemDetail}) yang tergabung dalam satu {@link DataUdcItem} (kelompok klasifikasi UDC —
 * Universal Decimal Classification) di modul perpustakaan. Dipasang pada panel detail satu
 * DataUdcItem, menampilkan daftar item anggota sebagai grid dengan keterangan yang dapat diedit
 * inline dan tombol tambah/hapus.
 *
 * <p>
 * Tombol tambah membuka dialog {@link AmbilDataItemBanyak} (pemilih item multi-pilih, dengan
 * item yang sudah menjadi anggota dikecualikan dari pilihan); setiap item yang dipilih langsung
 * disimpan sebagai baris {@link DataUdcItemDetail} baru (bila kelompok sudah tersimpan) dan
 * kolom UDC pada entitas {@link Item} itu sendiri ikut diperbarui mengikuti klasifikasi kelompok
 * ini. Menghapus baris juga mengosongkan kembali kolom UDC pada {@link Item} terkait. Kolom
 * keterangan per baris memperbarui database langsung saat berubah, dinonaktifkan bila pengguna
 * tidak memiliki hak ubah. Visibilitas tombol tambah/hapus dan status edit keterangan mengikuti
 * privilese pengguna saat ini ({@link CommonPrivilages}).
 * </p>
 */
public class DataUdcItemPunyaItemHelper {

	private MyGrid gridItem;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;

	/** Membangun helper terikat pada {@code gridItem} dan menghitung hak tambah/ubah/hapus pengguna saat ini. */
	public DataUdcItemPunyaItemHelper(MyGrid gridItem) {
		this.gridItem = gridItem;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun panel (border layout) berisi toolbar "Tambah Item"/"Refresh" dan grid daftar
	 * item anggota untuk {@code dataUdcItem}, lalu memuat data item yang sudah tersimpan.
	 *
	 * @param dataUdcItem kelompok klasifikasi UDC yang detail anggotanya ditampilkan/dikelola
	 * @return border layout siap disisipkan sebagai konten panel detail
	 */
	public Borderlayout initDetail(final DataUdcItem dataUdcItem) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Item", "/img/new.gif");
		add.setVisible(DataUdcItemPunyaItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Item> items = new ArrayList<Item>();
				List<Row> myrows = gridItem.getRows().getChildren();
				for (Row row : myrows) {
					items.add(((DataUdcItemDetail) row.getAttribute("dataUdcItemDetail")).getItem());
				}
				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items, false, true);
				ambilDataItemBanyak.setHeight("95%");
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						for (Item item : items) {
							DataUdcItemDetail dataUdcItemDetail = new DataUdcItemDetail();
							dataUdcItemDetail.setItem(item);
							dataUdcItemDetail.setKeterangan("");
							dataUdcItemDetail.setDataUdcItem(dataUdcItem);

							if (dataUdcItem.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(dataUdcItemDetail);

								item.setUdcItem(dataUdcItem.getUdcItem());
								session.update(item);
							}

							Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
							rows.setParent(gridItem);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, dataUdcItemDetail);
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
				loadDataDetail(dataUdcItem);
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

		loadDataDetail(dataUdcItem);

		return borderlayout;
	}

	/** Memuat baris-baris item anggota tersimpan untuk {@code dataUdcItem} dari database dan merendernya ke grid. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final DataUdcItem dataUdcItem) throws Exception {

		List<DataUdcItemDetail> dataUdcItemDetails = dataUdcItem == null || dataUdcItem.getId() == null
				? new ArrayList<DataUdcItemDetail>()
				: HibernateUtil.currentSession().createCriteria(DataUdcItemDetail.class)
						.add(Restrictions.eq("dataUdcItem", dataUdcItem)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (DataUdcItemDetail dataUdcItemDetail : dataUdcItemDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, dataUdcItemDetail);
		}
	}

	/**
	 * Mengisi {@code row} dengan identitas item (ISBN/ISSN), tautan riwayat revisi, kolom
	 * keterangan yang dapat diedit inline (memperbarui database langsung saat berubah, bila
	 * pengguna berhak ubah), dan tombol hapus (bila pengguna berhak); tombol hapus meminta
	 * konfirmasi, lalu mengosongkan kolom UDC pada {@link Item} terkait dan menghapus baris
	 * relasi dari database (bila sudah tersimpan).
	 */
	public void initRow(final Row row, final DataUdcItemDetail dataUdcItemDetail) throws Exception {
		row.setValign("top");row.setAttribute("dataUdcItemDetail", dataUdcItemDetail);

		new Label(dataUdcItemDetail.getItem() == null ? ""
				: dataUdcItemDetail.getItem().getIsbn() + " " + dataUdcItemDetail.getItem().getIssn()).setParent(row);

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
				dataUdcItemDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("dataUdcItemDetail", dataUdcItemDetail);
				if (dataUdcItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (dataUdcItemDetail));
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
							if (dataUdcItemDetail.getId() != null) {
								Session session = HibernateUtil.currentSession();

								Item item = dataUdcItemDetail.getItem();
								item.setUdcItem(null);
								session.update(item);

								session.delete(dataUdcItemDetail);
							}
							row.setVisible(false);
						}

					}
				});

			}
		});
	}

}
