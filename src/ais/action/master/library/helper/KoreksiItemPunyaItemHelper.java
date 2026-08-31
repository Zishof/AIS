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
import org.zkoss.zul.Combobox;
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
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.KodeTransaksi;
import ais.database.model.library.KoreksiItem;
import ais.database.model.library.KoreksiItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Helper UI (bukan entitas/aksi tersendiri) untuk mengelola detail koreksi stok item pustaka
 * ({@link KoreksiItemDetail}) pada sebuah {@link KoreksiItem} (dokumen koreksi/adjustment stok):
 * menghitung stok berjalan tiap item lewat SQL agregasi transaksi ({@code library.detail_transaksi}
 * dikalikan {@code jenis} kode transaksinya) sebelum baris ditambahkan, memungkinkan penambahan
 * item manual ({@code AmbilDataItemBanyak}) maupun berdasarkan filter stok
 * ({@code AmbilDataItemBanyakBerdasarkanStok}), dan menghitung ulang "stok menjadi" (stok saat ini
 * + koreksi bertanda sesuai jenis penambahan/pengurangan) setiap kali jenis koreksi atau jumlah
 * diubah. Seluruh kolom baris (jenis koreksi, jumlah, keterangan) dan tombol hapus dinonaktifkan
 * begitu {@code koreksiItem} sudah {@code disetujuiOleh} (telah disetujui). Perpustakaan wajib
 * dipilih ({@code getPerpustakaan}) sebelum item dapat ditambahkan. Visibilitas tombol tambah/
 * hapus mengikuti hak akses {@link CommonPrivilages#CREATE}/{@link CommonPrivilages#DELETE}.
 */
public class KoreksiItemPunyaItemHelper {

	private MyGrid gridItem;
	private boolean add = false;
	private boolean delete = false;

	private List<KodeTransaksi> kodeTransaksis = new ArrayList<KodeTransaksi>();

	/** @param gridItem grid yang akan diisi/dikelola helper ini */
	public KoreksiItemPunyaItemHelper(MyGrid gridItem) {
		this.gridItem = gridItem;
		kodeTransaksis.add(LibraryUtil.adjustmentPenambahan);
		kodeTransaksis.add(LibraryUtil.adjustmentPengurangan);
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Menyusun tata letak (toolbar dua tombol tambah — manual dan berdasar stok — plus tombol
	 * refresh yang dinonaktifkan bila sudah disetujui, dan grid detail koreksi dengan kolom
	 * Kode/ISBN/Nama/Jenis Koreksi/Jumlah/Stok/Menjadi/Keterangan/Hapus) dan langsung memuat
	 * detail {@code koreksiItem} yang sudah tersimpan.
	 *
	 * @param koreksiItem     dokumen koreksi stok yang detailnya dikelola
	 * @param getPerpustakaan komponen pemilih perpustakaan; nilainya wajib diisi sebelum item dapat ditambahkan
	 * @return komponen tata letak siap pakai untuk ditempelkan ke jendela detail
	 */
	public Borderlayout initDetail(final KoreksiItem koreksiItem, final AmbilDataPerpustakaanBanbox getPerpustakaan)
			throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Item", "/img/new.gif");
		add.setVisible(KoreksiItemPunyaItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (getPerpustakaan.getAttribute("perpustakaan") == null) {
					MyMessageboxConfig.show("Pilih salah satu perpustakaan", "Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				koreksiItem.setPerpustakaan((Perpustakaan) getPerpustakaan.getAttribute("perpustakaan"));

				List<Item> items = new ArrayList<Item>();
				List<Row> myrows = gridItem.getRows().getChildren();
				for (Row row : myrows) {
					items.add(((KoreksiItemDetail) row.getAttribute("koreksiItemDetail")).getItem());
				}
				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
				ambilDataItemBanyak.setHeight("95%");
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<Item> items = (List<Item>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (Item item : items) {

							String sql = "select sum((a.qty+a.qtybonus)*b.jenis) as stok from library.detail_transaksi a inner join library.kode_transaksi b on (a.kode_transaksi = b.id) where a.item = "
									+ item.getId() + " and a.perpustakaan = " + (koreksiItem.getPerpustakaan() == null
											? "-1" : koreksiItem.getPerpustakaan().getId())
									+ ";";
							Number number = (Number) session.createSQLQuery(sql).uniqueResult();

							KoreksiItemDetail koreksiItemDetail = new KoreksiItemDetail();
							koreksiItemDetail.setItem(item);
							koreksiItemDetail.setJumlah(0.0);
							koreksiItemDetail.setStok(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setStokmenjadi(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setKeterangan("");
							koreksiItemDetail.setKoreksiItem(koreksiItem);

							if (koreksiItem.getId() != null) {
								session.save(koreksiItemDetail);
							}

							Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
							rows.setParent(gridItem);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, koreksiItemDetail);
						}
					}
				});

				ambilDataItemBanyak.onModal();

			}
		});

		add = new MyToolbarbuttonConfig("Tambah Item Berdasar Stok", "/img/new.gif");
		add.setVisible(KoreksiItemPunyaItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (getPerpustakaan.getAttribute("perpustakaan") == null) {
					MyMessageboxConfig.show("Pilih salah satu perpustakaan", "Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				koreksiItem.setPerpustakaan((Perpustakaan) getPerpustakaan.getAttribute("perpustakaan"));

				List<Item> items = new ArrayList<Item>();
				List<Row> myrows = gridItem.getRows().getChildren();
				for (Row row : myrows) {
					items.add(((KoreksiItemDetail) row.getAttribute("koreksiItemDetail")).getItem());
				}
				AmbilDataItemBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemBanyakBerdasarkanStok(items,
						koreksiItem.getPerpustakaan(), false);
				ambilDataItemBanyak.setHeight("95%");
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (Item item : items) {

							String sql = "select sum((a.qty+a.qtybonus)*b.jenis) as stok from library.detail_transaksi a inner join library.kode_transaksi b on (a.kode_transaksi = b.id) where a.item = "
									+ item.getId() + " and a.perpustakaan = " + (koreksiItem.getPerpustakaan() == null
											? "-1" : koreksiItem.getPerpustakaan().getId())
									+ ";";
							Number number = (Number) session.createSQLQuery(sql).uniqueResult();

							KoreksiItemDetail koreksiItemDetail = new KoreksiItemDetail();
							koreksiItemDetail.setItem(item);
							koreksiItemDetail.setJumlah(0.0);
							koreksiItemDetail.setStok(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setStokmenjadi(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setKeterangan("");
							koreksiItemDetail.setKoreksiItem(koreksiItem);

							if (koreksiItem.getId() != null) {
								session.save(koreksiItemDetail);
							}

							Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
							rows.setParent(gridItem);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, koreksiItemDetail);
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
		cari.setDisabled(koreksiItem.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(koreksiItem);
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
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Koreksi");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Stok");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Menjadi");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadDataDetail(koreksiItem);

		return borderlayout;
	}

	/** Memuat baris {@link KoreksiItemDetail} tersimpan milik {@code koreksiItem} ke dalam grid (kosong bila dokumen belum tersimpan). */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final KoreksiItem koreksiItem) throws Exception {

		List<KoreksiItemDetail> koreksiItemDetails = koreksiItem == null || koreksiItem.getId() == null
				? new ArrayList<KoreksiItemDetail>()
				: HibernateUtil.currentSession().createCriteria(KoreksiItemDetail.class)
						.add(Restrictions.eq("koreksiItem", koreksiItem)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (KoreksiItemDetail koreksiItemDetail : koreksiItemDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, koreksiItemDetail);
		}
	}

	/**
	 * Mengisi satu baris grid dengan kode/nama item (via {@link RevisiHelper}), stok berjalan,
	 * kombo jenis koreksi (Penambahan/Pengurangan), jumlah, dan keterangan — setiap perubahan
	 * jenis/jumlah menghitung ulang {@code jumlah} bertanda (mutlak dikali {@code jenis} kode
	 * transaksi) dan {@code stokmenjadi} (stok + jumlah bertanda), lalu menyimpan langsung bila
	 * baris sudah tersimpan. Semua kolom dan tombol hapus dinonaktifkan bila
	 * {@code koreksiItem} sudah disetujui.
	 *
	 * @param row               baris grid yang diisi
	 * @param koreksiItemDetail data detail koreksi untuk baris ini
	 */
	public void initRow(final Row row, final KoreksiItemDetail koreksiItemDetail) throws Exception {
		row.setValign("top");row.setAttribute("koreksiItemDetail", koreksiItemDetail);

		new Label(koreksiItemDetail.getItem() == null ? ""
				: koreksiItemDetail.getItem().getIsbn() + " " + koreksiItemDetail.getItem().getIssn()).setParent(row);

		RevisiHelper
				.createNewRevisi(KoreksiItemDetail.class, koreksiItemDetail,
						koreksiItemDetail.getItem() == null ? "" : koreksiItemDetail.getItem().getNama())
				.setParent(row);

		final Label stok = new Label(
				Common.numberFormat.get().format((koreksiItemDetail.getStok() == null ? 0.0 : koreksiItemDetail.getStok())));

		final Label stokMenjadi = new Label(Common.numberFormat.get()
				.format((koreksiItemDetail.getStokmenjadi() == null ? 0.0 : koreksiItemDetail.getStokmenjadi())));

		final MyDoublebox jumlah;
		jumlah = new MyDoublebox(koreksiItemDetail.getJumlah() == null ? 0.0 : koreksiItemDetail.getJumlah());

		final Combobox ajdusment = new Combobox();
		Common.insertComboItems(ajdusment, "nama", kodeTransaksis);
		Common.selectComboItem(ajdusment, koreksiItemDetail.getKodeTransaksi());

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KodeTransaksi kodeTransaksi = (KodeTransaksi) (ajdusment.getSelectedItem() == null ? null
						: ajdusment.getSelectedItem().getValue());
				if (kodeTransaksi == null) {
					MyMessageboxConfig.show("Pilih salah satu jenis koreksi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					jumlah.setValue(0.0);
					return;
				}
				if (jumlah.getValue() == null) {
					jumlah.setValue(0.0);
				}

				koreksiItemDetail.setKodeTransaksi(kodeTransaksi);
				koreksiItemDetail.setJumlah(Math.abs(jumlah.getValue()) * kodeTransaksi.getJenis().doubleValue());
				jumlah.setValue(koreksiItemDetail.getJumlah());

				Double menjadi = koreksiItemDetail.getJumlah() + koreksiItemDetail.getStok();
				stokMenjadi.setValue(Common.numberFormat.get().format(menjadi));
				koreksiItemDetail.setStokmenjadi(menjadi);

				row.setValign("top");row.setAttribute("koreksiItemDetail", koreksiItemDetail);
				if (koreksiItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (koreksiItemDetail));
				}

			}
		};

		ajdusment.setWidth("90%");
		ajdusment.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
		ajdusment.setParent(row);
		ajdusment.addEventListener("onChange", eventListener);

		jumlah.setParent(row);
		jumlah.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
		jumlah.setStyle("text-align:right");
		jumlah.setWidth("90%");
		jumlah.setWidth("90%");
		jumlah.addEventListener(Events.ON_CHANGE, eventListener);

		stok.setParent(row);
		stokMenjadi.setParent(row);

		final MyTextbox keterangan = new MyTextbox(
				koreksiItemDetail.getKeterangan() == null ? "" : koreksiItemDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);
		keterangan.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				koreksiItemDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("koreksiItemDetail", koreksiItemDetail);
				if (koreksiItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (koreksiItemDetail));
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
							if (koreksiItemDetail.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.delete(koreksiItemDetail);
							}
							row.setVisible(false);
						}

					}
				});

			}
		});
	}

}
