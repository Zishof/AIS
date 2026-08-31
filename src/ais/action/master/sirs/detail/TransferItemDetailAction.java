package ais.action.master.sirs.detail;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanbox;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyakBerdasarkanStok;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.TransferItem;
import ais.database.model.sirs.TransferItemDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk transfer item detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code TransferItem transferItem}, {@code
 * Window addWindow}, {@code Grid grid}, {@code AmbilDataItemMedisBanbox item}, {@code MyDoublebox jumlah},
 * {@code MyTextbox keterangan}, {@code Lokasi myLokasi}, {@code TransferItemDetail transferItemDetail};
 * inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian ({@code loadData()}); mutasi data ({@code
 * onSave()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class TransferItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private TransferItem transferItem;
	private Window addWindow;
	private Grid grid;

	private AmbilDataItemMedisBanbox item;
	private MyDoublebox jumlah;
	private MyTextbox keterangan;

	private Lokasi myLokasi;

	private TransferItemDetail transferItemDetail;

	public TransferItemDetailAction(TransferItem transferItem, Lokasi myLokasi) {
		super();
		this.myLokasi = myLokasi;
		this.transferItem = transferItem;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(TransferItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TransferItemDetailAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TransferItemDetailAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TransferItemDetailAction
	 */
	class TransferItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public TransferItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final TransferItemDetail transferItemDetail = (TransferItemDetail) data;

			new Label(transferItemDetail.getItem() == null ? "" : transferItemDetail.getItem().getKode())
					.setParent(row);

			RevisiHelper
					.createNewRevisi(TransferItemDetail.class, transferItemDetail,
							transferItemDetail.getItem() == null ? "" : transferItemDetail.getItem().getNama())
					.setParent(row);

			final Label total = new Label(Common.numberFormat.get()
					.format((transferItemDetail.getJumlah() == null ? 0.0 : transferItemDetail.getJumlah())
							* (transferItemDetail.getHargaJual() == null ? 0.0 : transferItemDetail.getHargaJual())));

			final Label totalTerima = new Label(Common.numberFormat.get().format(
					(transferItemDetail.getJumlahDiterima() == null ? 0.0 : transferItemDetail.getJumlahDiterima())
							* (transferItemDetail.getHargaJual() == null ? 0.0 : transferItemDetail.getHargaJual())));

			final Label totalSelisih = new Label(Common.numberFormat.get()
					.format((transferItemDetail.getSelisih() == null ? 0.0 : transferItemDetail.getSelisih())
							* (transferItemDetail.getHargaJual() == null ? 0.0 : transferItemDetail.getHargaJual())));

			final Label selisih = new Label(Common.numberFormat.get()
					.format((transferItemDetail.getSelisih() == null ? 0.0 : transferItemDetail.getSelisih())));

			final MyDoublebox jumlah;
			(jumlah = new MyDoublebox(transferItemDetail.getJumlah() == null ? 0.0 : transferItemDetail.getJumlah()))
					.setParent(row);
			jumlah.setDisabled(transferItemDetail.getTransferItem().getDisetujuiOleh() != null || myLokasi == null
					|| !myLokasi.getId().equals(transferItem.getLokasi().getId()));

			final MyDoublebox diterima;
			diterima = new MyDoublebox(
					transferItemDetail.getJumlahDiterima() == null ? 0.0 : transferItemDetail.getJumlahDiterima());

			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					transferItemDetail.setJumlah(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
					transferItemDetail.setJumlahDiterima(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
					transferItemDetail.setSelisih(0.0);
					selisih.setValue(Common.numberFormat.get().format(transferItemDetail.getSelisih()));

					Common.refreshUpdate(session, (transferItemDetail));
					diterima.setValue(jumlah.getValue());

					total.setValue(Common.numberFormat.get().format((transferItemDetail.getJumlah() == null ? 0.0
							: transferItemDetail.getJumlah())
							* (transferItemDetail.getHargaJual() == null ? 0.0 : transferItemDetail.getHargaJual())));
					totalTerima.setValue(Common.numberFormat.get().format((transferItemDetail.getJumlahDiterima() == null
							? 0.0
							: transferItemDetail.getJumlahDiterima())
							* (transferItemDetail.getHargaJual() == null ? 0.0 : transferItemDetail.getHargaJual())));

					totalSelisih.setValue(Common.numberFormat.get().format((transferItemDetail.getSelisih() == null ? 0.0
							: transferItemDetail.getSelisih())
							* (transferItemDetail.getHargaJual() == null ? 0.0 : transferItemDetail.getHargaJual())));
				}
			});

			diterima.setParent(row);
			diterima.setDisabled(transferItemDetail.getTransferItem().getDiterimaOleh() != null
					|| transferItemDetail.getTransferItem().getDisetujuiOleh() == null || myLokasi == null
					|| !myLokasi.getId().equals(transferItem.getLokasiTujuan().getId()));
			diterima.setStyle("text-align:right");
			diterima.setWidth("90%");
			diterima.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					transferItemDetail.setJumlahDiterima(diterima.getValue() == null ? 0.0 : diterima.getValue());

					transferItemDetail
							.setSelisih(transferItemDetail.getJumlah() - transferItemDetail.getJumlahDiterima());
					selisih.setValue(Common.numberFormat.get().format(transferItemDetail.getSelisih()));

					Common.refreshUpdate(session, (transferItemDetail));

					total.setValue(Common.numberFormat.get().format((transferItemDetail.getJumlah() == null ? 0.0
							: transferItemDetail.getJumlah())
							* (transferItemDetail.getHargaJual() == null ? 0.0 : transferItemDetail.getHargaJual())));
					totalTerima.setValue(Common.numberFormat.get().format((transferItemDetail.getJumlahDiterima() == null
							? 0.0
							: transferItemDetail.getJumlahDiterima())
							* (transferItemDetail.getHargaJual() == null ? 0.0 : transferItemDetail.getHargaJual())));

					totalSelisih.setValue(Common.numberFormat.get().format((transferItemDetail.getSelisih() == null ? 0.0
							: transferItemDetail.getSelisih())
							* (transferItemDetail.getHargaJual() == null ? 0.0 : transferItemDetail.getHargaJual())));
				}
			});

			selisih.setParent(row);

			new Label(transferItemDetail.getSatuanItem() == null ? "" : transferItemDetail.getSatuanItem().getNama())
					.setParent(row);

			if (transferItemDetail.getHargaJual() == null) {
				Session session = HibernateUtil.currentSession();
				HargaJualItem hargaJualItem = (HargaJualItem) session.createCriteria(HargaJualItem.class)
						.add(Restrictions.eq("item", transferItemDetail.getItem()))
						.add(Restrictions.eq("kelasPerawatan", ConstantValues.kelasNormal)).setMaxResults(1)
						.uniqueResult();
				transferItemDetail.setHargaJual(
						hargaJualItem == null || hargaJualItem.getId() == null ? 0.0 : hargaJualItem.getHargaJual());
				Common.refreshUpdate(session, (transferItemDetail));

				DetailTransaksiPasien detailTransaksi = (DetailTransaksiPasien) session.createCriteria(DetailTransaksiPasien.class)
						.add(Restrictions.eq("transferItemDetail", transferItemDetail)).setMaxResults(1).uniqueResult();
				if (detailTransaksi != null) {
					detailTransaksi.setAmount(transferItemDetail.getHargaJual());
					Common.refreshUpdate(session, (detailTransaksi));
				}

			}

			new Label(transferItemDetail.getHargaJual() == null ? ""
					: Common.numberFormat.get().format(transferItemDetail.getHargaJual())).setParent(row);

			total.setParent(row);
			totalTerima.setParent(row);
			totalSelisih.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					transferItemDetail.getKeterangan() == null ? "" : transferItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(transferItemDetail.getTransferItem().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					transferItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (transferItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setDisabled(transferItemDetail.getTransferItem().getDisetujuiOleh() != null);
			button.setTooltiptext("Rubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow = new Window();
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
					init(transferItemDetail);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(transferItemDetail.getTransferItem().getDisetujuiOleh() != null);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(transferItemDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/TransferItemDetailAction.java:277");
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(row);

		}
	}

	private void init(TransferItemDetail transferItemDetail) {
		this.transferItemDetail = transferItemDetail;
		addWindow.setTitle(transferItemDetail.getId() == null ? "Ambil Item" : "Rubah Item");
		addWindow.setHeight("300px");
		addWindow.setWidth("600px");
		Common.clear(addWindow);
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Item")));
		row.appendChild(item = new AmbilDataItemMedisBanbox());
		item.setValue(transferItemDetail.getItem() == null ? "" : transferItemDetail.getItem().getKode());
		item.setAttribute("item", transferItemDetail.getItem());
		item.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Item")));
		final Label namaItem;
		row.appendChild(namaItem = new Label(transferItemDetail.getItem() == null ? ""
				: transferItemDetail.getItem().getNama() == null ? "" : transferItemDetail.getItem().getNama()));
		item.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ItemMedis item = (ItemMedis) TransferItemDetailAction.this.item.getAttribute("item");
				namaItem.setValue(item == null ? "" : item.getNama() == null ? "" : item.getNama());
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Item")));
		row.appendChild(jumlah = new MyDoublebox(
				transferItemDetail.getJumlah() == null ? 0.0 : transferItemDetail.getJumlah()));
		jumlah.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				transferItemDetail.getKeterangan() == null ? "" : transferItemDetail.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					loadData(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (item.getAttribute("item") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Item belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu item yang akan ditransfer sebelum menyimpan data.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jumlah.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Jumlah permintaan belum diisi. Mohon Bapak/Ibu mengisi jumlah item yang akan ditransfer sebelum menyimpan data.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (transferItemDetail.getId() != null) {
			transferItemDetail = (TransferItemDetail) session.load(TransferItemDetail.class,
					transferItemDetail.getId());
		}
		transferItemDetail.setItem((ItemMedis) item.getAttribute("item"));
		transferItemDetail.setJumlah(jumlah.getValue());
		transferItemDetail.setKeterangan(keterangan.getValue());
		transferItemDetail.setTransferItem(transferItem);
		transferItemDetail.setSatuanItem(transferItemDetail.getItem().getSatuanItem());

		HargaJualItem hargaJualItem = (HargaJualItem) session.createCriteria(HargaJualItem.class)
				.add(Restrictions.eq("item", transferItemDetail.getItem()))
				.add(Restrictions.eq("kelasPerawatan", ConstantValues.kelasNormal)).setMaxResults(1).uniqueResult();
		transferItemDetail.setHargaJual(
				hargaJualItem == null || hargaJualItem.getId() == null ? 0.0 : hargaJualItem.getHargaJual());
		Common.refreshSaveOrUpdate(session, transferItemDetail);
		return true;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<TransferItemDetail> transferItemDetails = session.createCriteria(TransferItemDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("transferItem", transferItem)).list();

		ListModel strset = new SimpleListModel(transferItemDetails);
		grid.setRowRenderer(new TransferItemDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public void display() {
		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item yang ditransfer");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(panel);
		// Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Item",
		// "/img/add_item.png");
		// button.setDisabled(transferItem.getDisetujuiOleh() != null);
		// button.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event event) throws Exception {
		// addWindow = new Window();
		// ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
		// .appendChild(addWindow);
		// init(new TransferItemDetail());
		// addWindow.setVisible(true);
		// addWindow.onModal();
		// }
		//
		// });
		// button.setParent(toolbar);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Item Dari Lokasi " + transferItem.getLokasi().getNama(),
				"/img/add_item.png");
		button.setDisabled(transferItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(TransferItemDetail.class)
						.setProjection(Projections.groupProperty("item.id"))
						.add(Restrictions.eq("transferItem", transferItem)), ItemMedis.class, false);

				AmbilDataItemMedisBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemMedisBanyakBerdasarkanStok(
						items, transferItem.getLokasi(), true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {

							HargaJualItem hargaJualItem = (HargaJualItem) session.createCriteria(HargaJualItem.class)
									.add(Restrictions.eq("item", item))
									.add(Restrictions.eq("kelasPerawatan", ConstantValues.kelasNormal)).setMaxResults(1)
									.uniqueResult();

							TransferItemDetail transferItemDetail = new TransferItemDetail();
							transferItemDetail.setItem(item);
							transferItemDetail.setJumlah(0.0);
							transferItemDetail.setHargaJual(hargaJualItem == null || hargaJualItem.getId() == null ? 0.0
									: hargaJualItem.getHargaJual());
							transferItemDetail.setKeterangan("");
							transferItemDetail.setTransferItem(transferItem);
							transferItemDetail.setSatuanItem(item.getSatuanItem());
							session.save(transferItemDetail);
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

		// AmbilDataItemBanyak

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode Item");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Item");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty Krm");
		column.setAlign("right");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty Trm");
		column.setAlign("right");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Selisih");
		column.setAlign("right");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hrg Jual");
		column.setAlign("right");
		column.setWidth("7%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ttl Krm");
		column.setAlign("right");
		column.setWidth("7%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ttl Trm");
		column.setAlign("right");
		column.setWidth("7%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ttl Sls");
		column.setAlign("right");
		column.setWidth("7%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

}
