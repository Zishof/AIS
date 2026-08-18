package ais.action.master.library.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.library.util.LibraryUtil;
import ais.action.report.format1.library.LaporanBarcodeItemLama;
import ais.action.report.format1.library.LaporanNoPunggungDanBarcodeSaldoAwal;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.PenerimaanPengadaanItem;
import ais.database.model.library.PenerimaanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.SaldoAwal;
import ais.database.model.library.SaldoAwalDetail;
import ais.database.model.library.TipeItem;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

public class ItemPunyaBarcodeHelper {

	/**
	 * Pastikan session masih terbuka sebelum dipakai begin()/commit()/query.
	 * Callee di tengah proses (BarcodeCommon.generateCode, initRow, lazy-load
	 * getter) dapat menutup session thread-local sehingga pemanggilan
	 * getTransaction() berikutnya gagal "Session is closed!".
	 */
	private static Session pastikanSessionTerbuka(Session session) {
		if (session == null || !session.isOpen()) {
			return HibernateUtil.currentNativeSession();
		}
		return session;
	}

	private MyGrid gridBarcode;
	private boolean add = false;
	private boolean delete = false;
	private Textbox barcode;
	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Item item;

	public ItemPunyaBarcodeHelper(MyGrid gridBarcode) {
		this.gridBarcode = gridBarcode;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Mengaktifkan/menonaktifkan tombol "Tambah Barcode" secara EKSPLISIT (mengabaikan hasil
	 * {@code checkPrevilages(CREATE)}). Dipakai pemanggil — mis. dialog "Ubah Item"
	 * (ItemAction) — yang sudah berhak mengubah item sehingga tombol tambah barcode perlu
	 * tampil walau privilege CREATE pada menu saat ini tidak terbaca. WAJIB dipanggil SEBELUM
	 * {@link #initDetail(Item)} karena visibilitas tombol ditentukan saat render. Mengembalikan
	 * {@code this} agar bisa dirangkai.
	 *
	 * @param add {@code true} untuk menampilkan tombol "Tambah Barcode".
	 * @return objek ini (chaining).
	 */
	public ItemPunyaBarcodeHelper setAdd(boolean add) {
		this.add = add;
		return this;
	}

	public Borderlayout initDetail(final Item item) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Hbox hbox = new Hbox();
		hbox.setHeight("30px");
		hbox.setParent(north);

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

		new Label(ais.common.Common.getBahasaConfig("Barcode")).setParent(hbox);
		new Space().setParent(hbox);
		barcode = new Textbox();
		barcode.setParent(hbox);
		barcode.addEventListener("onOK", new EventListener() {

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

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Barcode", "/img/new.gif");
		add.setVisible(ItemPunyaBarcodeHelper.this.add);
		add.setParent(toolbar);
		add.setDisabled(item.getId() == null);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			private Row rowSaldoAwal;
			private Row rowPenerimaanPengadaanItem;

			@Override
			public void onEvent(Event event) throws Exception {
				final MyWindow window = new MyWindow("Item Batch", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("240px");
				window.setWidth("550px");

				final AmbilDataPerpustakaanBanbox perpustakaan = new AmbilDataPerpustakaanBanbox();

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
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

				final BatchItemPunyaBarcode batchItemPunyaBarcode = new BatchItemPunyaBarcode();

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Kode Batch"));
				final Textbox kode;
				row.appendChild(kode = new Textbox(batchItemPunyaBarcode.getKode()));
				kode.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
				row.appendChild(perpustakaan);
				perpustakaan.setWidth("90%");
				perpustakaan.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Berasal dari"));
				final Combobox berasalDari;
				row.appendChild(berasalDari = new Combobox());
				berasalDari.setWidth("90%");
				berasalDari.setReadonly(true);
				MyComboitemConfig comboitem = new MyComboitemConfig(BatchItemPunyaBarcode.PEMBELIAN);
				comboitem.setValue(BatchItemPunyaBarcode.PEMBELIAN);
				berasalDari.appendChild(comboitem);

				comboitem = new MyComboitemConfig(BatchItemPunyaBarcode.SALDO_AWAL);
				comboitem.setValue(BatchItemPunyaBarcode.SALDO_AWAL);
				berasalDari.appendChild(comboitem);

				berasalDari.setReadonly(true);
				Common.selectComboItem(berasalDari, BatchItemPunyaBarcode.SALDO_AWAL);

				rowSaldoAwal = new MyFormRow();
				rowSaldoAwal.setStyle("border:0px;background: transparent;");
				rowSaldoAwal.setParent(rows);
				rowSaldoAwal.appendChild(new Label(ais.common.Common.getBahasaConfig("Saldo Awal")));
				final Combobox saldoAwal;
				rowSaldoAwal.appendChild(saldoAwal = new Combobox());
				saldoAwal.setWidth("90%");
				saldoAwal.setReadonly(true);

				rowPenerimaanPengadaanItem = new MyFormRow();
				rowPenerimaanPengadaanItem.setStyle("border:0px;background: transparent;");
				rowPenerimaanPengadaanItem.setParent(rows);
				rowPenerimaanPengadaanItem.appendChild(new Label(ais.common.Common.getBahasaConfig("Pembelian")));
				final Combobox penerimaanPengadaanItem;
				rowPenerimaanPengadaanItem.appendChild(penerimaanPengadaanItem = new Combobox());
				penerimaanPengadaanItem.setWidth("90%");
				penerimaanPengadaanItem.setReadonly(true);

				EventListener perpustakaanEventListener = new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(saldoAwal);
						Common.clear(penerimaanPengadaanItem);
						if (perpustakaan.getAttribute("perpustakaan") == null) {
							return;
						}

						Session session = HibernateUtil.currentSession();

						List<SaldoAwal> saldoAwals = session.createCriteria(SaldoAwal.class).addOrder(Order.desc("id"))
								.add(Restrictions.and(
										Restrictions.eq("perpustakaan", perpustakaan.getAttribute("perpustakaan")),
										Restrictions.isNotNull("disetujuiOleh")))
								.list();

						List<PenerimaanPengadaanItem> penerimaanPengadaanItems = session
								.createCriteria(PenerimaanPengadaanItem.class).addOrder(Order.desc("id"))
								.add(Restrictions.and(
										Restrictions.eq("perpustakaan", perpustakaan.getAttribute("perpustakaan")),
										Restrictions.isNotNull("disetujuiOleh")))
								.list();

						Common.insertComboItems(saldoAwal, "kode", "keterangan", saldoAwals);

						if (!saldoAwal.getChildren().isEmpty()) {
							saldoAwal.setSelectedIndex(0);
						}

						Common.insertComboItems(penerimaanPengadaanItem, "kode", "keterangan",
								penerimaanPengadaanItems);

						if (!penerimaanPengadaanItem.getChildren().isEmpty()) {
							penerimaanPengadaanItem.setSelectedIndex(0);
						}

					}
				};

				perpustakaan.setEventListener(perpustakaanEventListener);
				perpustakaanEventListener.onEvent(null);

				EventListener jenisPenerimaanEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						rowSaldoAwal.setVisible(
								berasalDari.getSelectedItem().getValue().equals(BatchItemPunyaBarcode.SALDO_AWAL));
						rowPenerimaanPengadaanItem.setVisible(
								berasalDari.getSelectedItem().getValue().equals(BatchItemPunyaBarcode.PEMBELIAN));
					}
				};

				jenisPenerimaanEventListener.onEvent(null);
				berasalDari.addEventListener("onChange", jenisPenerimaanEventListener);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Tambahan Barcode Baru"));
				final MyIntbox jumlah;
				row.appendChild(jumlah = new MyIntbox(1));
				jumlah.setWidth("90%");

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Buat Barcode", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (berasalDari.getSelectedItem().getValue().equals(BatchItemPunyaBarcode.SALDO_AWAL)
								&& saldoAwal.getSelectedItem() == null) {
							MyMessageboxConfig.show("Saldo Awal harus dipilih", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}
						if (berasalDari.getSelectedItem().getValue().equals(BatchItemPunyaBarcode.PEMBELIAN)
								&& penerimaanPengadaanItem.getSelectedItem() == null) {
							MyMessageboxConfig.show("Pembelian harus dipilih", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}
						if (perpustakaan.getAttribute("perpustakaan") == null) {
							MyMessageboxConfig.show("Perpustakaan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}
						if (jumlah.getValue() == null || jumlah.getValue() < 1) {
							MyMessageboxConfig.show("Jumlah penambahan harus diisi", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}

						batchItemPunyaBarcode.setKode(kode.getValue().trim());
						batchItemPunyaBarcode.setBerasalDari((String) berasalDari.getSelectedItem().getValue());
						batchItemPunyaBarcode.setItem(item);
						batchItemPunyaBarcode.setDibuatOleh(Common.getCurrentUser());
						batchItemPunyaBarcode.setPenerimaanPengadaanItem(
								(PenerimaanPengadaanItem) (penerimaanPengadaanItem.getSelectedItem() == null ? null
										: penerimaanPengadaanItem.getSelectedItem().getValue()));
						batchItemPunyaBarcode.setSaldoAwal((SaldoAwal) (saldoAwal.getSelectedItem() == null ? null
								: saldoAwal.getSelectedItem().getValue()));

						Session session = HibernateUtil.currentNativeSession();

						session.getTransaction().begin();
						session.save(batchItemPunyaBarcode);
						session.getTransaction().commit();

						SaldoAwalDetail saldoAwalDetail = null;
						PenerimaanPengadaanItemDetail penerimaanPengadaanItemDetail = null;

						if (berasalDari.getSelectedItem().getValue().equals(BatchItemPunyaBarcode.SALDO_AWAL)) {

							saldoAwalDetail = (SaldoAwalDetail) session.createCriteria(SaldoAwalDetail.class)
									.add(Restrictions.eq("saldoAwal", batchItemPunyaBarcode.getSaldoAwal()))
									.add(Restrictions.eq("item", item)).uniqueResult();

							if (saldoAwalDetail == null) {
								saldoAwalDetail = new SaldoAwalDetail();
								saldoAwalDetail.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
								saldoAwalDetail.setDataPerItem(true);
								saldoAwalDetail.setItem(item);
								saldoAwalDetail.setJumlah(jumlah.getValue().doubleValue());
								saldoAwalDetail.setSaldoAwal(batchItemPunyaBarcode.getSaldoAwal());
								session.getTransaction().begin();
								session.save(saldoAwalDetail);
								session.getTransaction().commit();
							}

						} else if (berasalDari.getSelectedItem().getValue().equals(BatchItemPunyaBarcode.PEMBELIAN)) {
							penerimaanPengadaanItemDetail = (PenerimaanPengadaanItemDetail) session
									.createCriteria(PenerimaanPengadaanItemDetail.class)
									.add(Restrictions.eq("penerimaanPengadaanItem",
											batchItemPunyaBarcode.getPenerimaanPengadaanItem()))
									.add(Restrictions.eq("item", item)).uniqueResult();
							if (penerimaanPengadaanItemDetail == null) {
								penerimaanPengadaanItemDetail = new PenerimaanPengadaanItemDetail();
								penerimaanPengadaanItemDetail.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
								penerimaanPengadaanItemDetail.setItem(item);
								penerimaanPengadaanItemDetail.setJumlah(jumlah.getValue().doubleValue());
								penerimaanPengadaanItemDetail
										.setPenerimaanPengadaanItem(batchItemPunyaBarcode.getPenerimaanPengadaanItem());
								session.getTransaction().begin();
								session.save(penerimaanPengadaanItemDetail);
								session.getTransaction().commit();
							} else {
								penerimaanPengadaanItemDetail.setJumlah(
										penerimaanPengadaanItemDetail.getJumlah() + jumlah.getValue().doubleValue());
								session.getTransaction().begin();
								session.update(penerimaanPengadaanItemDetail);
								session.getTransaction().commit();
							}

						}

						Rows rows = gridBarcode.getRows() == null ? new Rows() : gridBarcode.getRows();
						rows.setParent(gridBarcode);

						ItemPunyaBarcodeHelper.this.batchItemPunyaBarcode = null;
						for (int i = 0; i < jumlah.getValue(); i++) {
							/* initRow() pada iterasi sebelumnya dapat menutup session. */
							session = pastikanSessionTerbuka(session);
							ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) session
									.createCriteria(ItemPunyaBarcode.class)
									.add(Restrictions.eq("batchItemPunyaBarcode", batchItemPunyaBarcode))
									.add(Restrictions.eq("item", item)).add(Restrictions.eq("indexke", i))
									.setMaxResults(1).uniqueResult();
							if (itemPunyaBarcode == null) {
								itemPunyaBarcode = new ItemPunyaBarcode();
								String kodeBarcode = BarcodeCommon.generateCode(batchItemPunyaBarcode);
								if (kodeBarcode == null || kodeBarcode.trim().isEmpty()) {
									kodeBarcode = Common.getGeneratedBarCode();
								}

								session = pastikanSessionTerbuka(session);
								BatchItemPunyaBarcode batchAktif = batchItemPunyaBarcode;
								if (batchItemPunyaBarcode.getId() != null) {
									batchAktif = (BatchItemPunyaBarcode) session.get(BatchItemPunyaBarcode.class,
											batchItemPunyaBarcode.getId());
								}
								Item itemAktif = item;
								if (item.getId() != null) {
									itemAktif = (Item) session.get(Item.class, item.getId());
								}
								Perpustakaan perpustakaanAktif = (Perpustakaan) perpustakaan
										.getAttribute("perpustakaan");
								if (perpustakaanAktif != null && perpustakaanAktif.getId() != null) {
									perpustakaanAktif = (Perpustakaan) session.get(Perpustakaan.class,
											perpustakaanAktif.getId());
								}
								SaldoAwalDetail saldoAwalDetailAktif = saldoAwalDetail;
								if (saldoAwalDetailAktif != null && saldoAwalDetailAktif.getId() != null) {
									saldoAwalDetailAktif = (SaldoAwalDetail) session.get(SaldoAwalDetail.class,
											saldoAwalDetailAktif.getId());
								}
								PenerimaanPengadaanItemDetail penerimaanPengadaanItemDetailAktif = penerimaanPengadaanItemDetail;
								if (penerimaanPengadaanItemDetailAktif != null
										&& penerimaanPengadaanItemDetailAktif.getId() != null) {
									penerimaanPengadaanItemDetailAktif = (PenerimaanPengadaanItemDetail) session.get(
											PenerimaanPengadaanItemDetail.class,
											penerimaanPengadaanItemDetailAktif.getId());
								}

								itemPunyaBarcode.setIndexke(i);
								itemPunyaBarcode.setItem(itemAktif);
								itemPunyaBarcode.setBatchItemPunyaBarcode(batchAktif);
								itemPunyaBarcode.setBarcode(kodeBarcode);
								itemPunyaBarcode.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
								itemPunyaBarcode.setPerpustakaan(perpustakaanAktif);

								/* BarcodeCommon.generateCode() di atas dapat memakai/menutup
								 * session thread-local — inilah titik error
								 * "Session is closed!" yang tercatat di log. */
								session = pastikanSessionTerbuka(session);
								session.getTransaction().begin();
								session.save(itemPunyaBarcode);
								session.getTransaction().commit();

								DetailTransaksi detailTransaksi = new DetailTransaksi();
								detailTransaksi.setSaldoAwalDetail(saldoAwalDetailAktif);
								detailTransaksi.setPenerimaanPengadaanItemDetail(penerimaanPengadaanItemDetailAktif);
								detailTransaksi.setItemPunyaBarcode(itemPunyaBarcode);
								detailTransaksi.setQtyBonus(0.0);

								/* Jalur PEMBELIAN: saldoAwalDetail memang null — pakai item
								 * yang sama (nilainya identik) agar tidak NullPointerException. */
								detailTransaksi.setItem(itemAktif);

								if (berasalDari.getSelectedItem().getValue().equals(BatchItemPunyaBarcode.SALDO_AWAL)) {
									detailTransaksi.setKodeTransaksi(LibraryUtil.SALDO_AWAL);
									detailTransaksi.setKeterangan("Transaksi Saldo Awal");
								} else if (berasalDari.getSelectedItem().getValue()
										.equals(BatchItemPunyaBarcode.PEMBELIAN)) {
									detailTransaksi.setKodeTransaksi(LibraryUtil.BELI_MASUK);
									detailTransaksi.setKeterangan("Transaksi Pembelian");
								}

								detailTransaksi.setPerpustakaan(itemPunyaBarcode.getPerpustakaan());
								detailTransaksi.setQty(1.0);
								detailTransaksi.setTanggal(ais.ui.util.WaktuUtil.getDate());
								detailTransaksi.setTanggalDanWaktu(ais.ui.util.WaktuUtil.getDate());

								session = pastikanSessionTerbuka(session);
								session.getTransaction().begin();
								session.save(detailTransaksi);
								session.getTransaction().commit();

								MyFormRow row = new MyFormRow();row.setValign("top");
								try {
									initRow(rows, row, itemPunyaBarcode);
									row.setParent(rows);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}

						}

						/* initRow() pada iterasi terakhir loop di atas juga dapat
						 * menutup session — pastikan terbuka sebelum dipakai lagi. */
						session = pastikanSessionTerbuka(session);
						// Hapus batch orphan: yang TIDAK direferensi item_punya_barcode DAN TIDAK
						// direferensi saldo_awal_detail (FK fk6915f5193d93303f). Tanpa exclude
						// saldo_awal_detail, delete melanggar FK ("still referenced from saldo_awal_detail").
						try {
							session.getTransaction().begin();
							session.createSQLQuery(
									"delete from library.batch_item_punya_barcode where id not in (select batch_item_punya_barcode from library.item_punya_barcode group by batch_item_punya_barcode) and id not in (select batch_item_punya_barcode from library.saldo_awal_detail where batch_item_punya_barcode is not null)")
									.executeUpdate();
							session.getTransaction().commit();
						} catch (Exception eHapusOrphan) {
							// Batch masih direferensi tabel lain → lewati pembersihan orphan, jangan
							// gagalkan keseluruhan proses. Sesi dibuka ulang agar operasi berikutnya aman.
							Common.tampilErrorJikaAdmin(eHapusOrphan);
							try {
								session.getTransaction().rollback();
							} catch (Exception rollbackException) {
								ais.common.ErrorAuditUtil.record(rollbackException,
										"auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaBarcodeHelper.java:tambahBarcode-rollbackOrphan");
							}
							session = pastikanSessionTerbuka(session);
						}

						if (batchItemPunyaBarcode.getSaldoAwal() != null) {
							double itemPunyaBarcodeCount = ((Number) session.createCriteria(ItemPunyaBarcode.class)
									.createAlias("batchItemPunyaBarcode", "batchItemPunyaBarcode")
									.add(Restrictions.eq("batchItemPunyaBarcode.saldoAwal",
											batchItemPunyaBarcode.getSaldoAwal()))
									.add(Restrictions.eq("item", item)).setProjection(Projections.rowCount())
									.uniqueResult()).doubleValue();
							saldoAwalDetail.setJumlah(itemPunyaBarcodeCount);
							session.getTransaction().begin();
							session.update(saldoAwalDetail);
							session.getTransaction().commit();
						} else if (batchItemPunyaBarcode.getPenerimaanPengadaanItem() != null) {
							double itemPunyaBarcodeCount = ((Number) session.createCriteria(ItemPunyaBarcode.class)
									.createAlias("batchItemPunyaBarcode", "batchItemPunyaBarcode")
									.add(Restrictions.eq("batchItemPunyaBarcode.penerimaanPengadaanItem",
											batchItemPunyaBarcode.getPenerimaanPengadaanItem()))
									.add(Restrictions.eq("item", item)).setProjection(Projections.rowCount())
									.uniqueResult()).doubleValue();
							if (penerimaanPengadaanItemDetail != null) {
								penerimaanPengadaanItemDetail.setJumlah(itemPunyaBarcodeCount);
								session.getTransaction().begin();
								session.update(penerimaanPengadaanItemDetail);
								session.getTransaction().commit();
							}
						}

						HibernateUtil.closeSession();

						window.detach();
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDetail(item);
							}
						});
					}
				});
				save.setParent(toolbar);

				window.onModal();
			}
		});

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Barcode", "/img/print.png");
		cetak.setParent(toolbar);
		cetak.setVisible(item.getId() != null);
		cetak.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanBarcodeItemLama laporanBarcodeItem = new LaporanBarcodeItemLama(item);
				laporanBarcodeItem.setTitle("Cetak Barcode");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanBarcodeItem);
				laporanBarcodeItem.setHeight("95%");
				laporanBarcodeItem.setWidth("90%");
				laporanBarcodeItem.setClosable(true);
				laporanBarcodeItem.onModal();
			}
		});

		cetak = new MyToolbarbuttonConfig("Punggung", "/img/print.png");
		cetak.setParent(toolbar);
		cetak.setVisible(item.getId() != null);
		cetak.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanNoPunggungDanBarcodeSaldoAwal laporanBarcodeItem = new LaporanNoPunggungDanBarcodeSaldoAwal(
						item);
				laporanBarcodeItem.setTitle("Cetak Barcode");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanBarcodeItem);
				laporanBarcodeItem.setHeight("95%");
				laporanBarcodeItem.setWidth("90%");
				laporanBarcodeItem.setClosable(true);
				laporanBarcodeItem.onModal();
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

		Common.clear(gridBarcode);
		gridBarcode.setParent(center);
		gridBarcode.setWidth("100%");
		gridBarcode.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridBarcode);

		MyColumnConfig column = new MyColumnConfig("Barcode");
		column.setParent(columns);

		column = new MyColumnConfig("Image");
		column.setParent(columns);

		column = new MyColumnConfig("Tipe Item");
		column.setParent(columns);

		column = new MyColumnConfig("Perpustakaan");
		column.setParent(columns);

		column = new MyColumnConfig("Tgl Dibuat");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Dipinjan");
		column.setParent(columns);
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(item);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) {
		this.item = item;
		List<ItemPunyaBarcode> itemPunyaBarcodes = item == null || item.getId() == null
				? new ArrayList<ItemPunyaBarcode>()
				: HibernateUtil.currentSession().createCriteria(ItemPunyaBarcode.class)
						.addOrder(Order.desc("batchItemPunyaBarcode")).addOrder(Order.desc("id"))
						.add(perpustakaan.getAttribute("perpustakaan") == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("perpustakaan", perpustakaan.getAttribute("perpustakaan")))
						.add(barcode.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("barcode", barcode.getValue().trim(), MatchMode.ANYWHERE))
						.add(Restrictions.eq("item", item)).list();

		Rows rows = gridBarcode.getRows() == null ? new Rows() : gridBarcode.getRows();
		Common.clear(rows);
		rows.setParent(gridBarcode);

		batchItemPunyaBarcode = null;
		for (ItemPunyaBarcode itemPunyaBarcode : itemPunyaBarcodes) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			try {
				initRow(rows, row, itemPunyaBarcode);
				row.setParent(rows);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	private BatchItemPunyaBarcode batchItemPunyaBarcode = null;

	public void initRow(Rows rows, final Row row, final ItemPunyaBarcode itemPunyaBarcode) throws Exception {

		if (batchItemPunyaBarcode == null
				|| !batchItemPunyaBarcode.getId().equals(itemPunyaBarcode.getBatchItemPunyaBarcode().getId())) {

			batchItemPunyaBarcode = itemPunyaBarcode.getBatchItemPunyaBarcode();
			Group group = new ais.ui.util.MyGroupConfig();
			group.setLabel(batchItemPunyaBarcode.getKode() + " - " + batchItemPunyaBarcode.getBerasalDari() + " - "
					+ (batchItemPunyaBarcode.getPenerimaanPengadaanItem() == null ? ""
							: batchItemPunyaBarcode.getPenerimaanPengadaanItem().getKode())
					+ (batchItemPunyaBarcode.getSaldoAwal() == null ? ""
							: batchItemPunyaBarcode.getSaldoAwal().getKode())
					+ "  - " + (batchItemPunyaBarcode.getTanggal() == null ? ""
							: Common.dateFormat5.get().format(batchItemPunyaBarcode.getTanggal())));
			group.setParent(rows);

		}

		row.setValign("top");row.setAttribute("itemPunyaBarcode", itemPunyaBarcode);

		new Label(itemPunyaBarcode.getBarcode()).setParent(row);
		final File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_"
				+ itemPunyaBarcode.getBarcode() + ".png");
		if (!myfilebarcode.exists()) {
			Barcode mybarcode = BarcodeFactory.createCode128B(itemPunyaBarcode.getBarcode());
			BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
		}
		Image image = new Image("/report/" + myfilebarcode.getName());
		image.setWidth("90%");
		image.setParent(row);

		new Label(itemPunyaBarcode.getTipeItem() == null ? "" : itemPunyaBarcode.getTipeItem().getNama())
				.setParent(row);

		new Label(itemPunyaBarcode.getPerpustakaan() == null ? "" : itemPunyaBarcode.getPerpustakaan().getNama())
				.setParent(row);

		new Label(itemPunyaBarcode.getTanggal_dirubah() == null ? ""
				: Common.dateFormat.get().format(itemPunyaBarcode.getTanggal_dirubah())).setParent(row);

		Integer count = 0;
		if (itemPunyaBarcode.getId() != null) {
			Session session = HibernateUtil.currentSession();
			count = ((Number) session.createCriteria(DetailTransaksi.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("itemPunyaBarcode", itemPunyaBarcode))
					.add(Restrictions.isNotNull("peminjamanPengadaanItemDetail")).uniqueResult()).intValue();
		}
		new Label(Common.numberFormat.get().format(count)).setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
		button.setTooltiptext("Ubah Data");
		// button.setVisible(false);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Edit Barcode", "none", true);
				window.setHeight("200px");
				window.setWidth("300px");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Barcode"));
				final Textbox barcodeText;
				row.appendChild(barcodeText = new Textbox(itemPunyaBarcode.getBarcode()));
				barcodeText.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Item"));
				final Combobox tipeItem;
				row.appendChild(tipeItem = new Combobox());
				tipeItem.setWidth("90%");
				tipeItem.setReadonly(true);
				Common.insertCombo(tipeItem, "nama", TipeItem.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				Common.selectComboItem(tipeItem, item.getTipeItem());

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
						Session session = HibernateUtil.currentSession();

						Integer kotaCount = ((Number) session.createCriteria(ItemPunyaBarcode.class)
								.setProjection(Projections.rowCount())
								.add(Restrictions.eq("barcode", barcodeText.getValue().trim()))
								.add(itemPunyaBarcode.getId() == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ne("id", itemPunyaBarcode.getId()))
								.uniqueResult()).intValue();

						boolean i = !kotaCount.equals(0);
						if (i) {
							MyMessageboxConfig.show("Barcode sudah digunakan, coba ganti dengan barcode yang lain",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						session.refresh(itemPunyaBarcode);
						itemPunyaBarcode.setBarcode(barcodeText.getValue().trim());
						itemPunyaBarcode.setTipeItem((TipeItem) tipeItem.getSelectedItem().getValue());
						Common.refreshUpdate(session, itemPunyaBarcode);
						window.detach();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDetail(item);
							}
						});
					}
				});
				save.setParent(toolbar);

				window.onModal();
			}
		});

		button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete && count.equals(0));
		// button.setVisible(false);
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
									if (itemPunyaBarcode.getId() != null) {

										Session session = HibernateUtil.currentNativeSession();
										session.getTransaction().begin();
										session.delete(itemPunyaBarcode);
										session.getTransaction().commit();

										session.createSQLQuery(
												"delete from library.batch_item_punya_barcode where id not in (select batch_item_punya_barcode from library.item_punya_barcode group by batch_item_punya_barcode)")
												.executeUpdate();

										BatchItemPunyaBarcode batchItemPunyaBarcode = itemPunyaBarcode
												.getBatchItemPunyaBarcode();
										if (batchItemPunyaBarcode != null) {
											SaldoAwalDetail saldoAwalDetail = (SaldoAwalDetail) session
													.createCriteria(SaldoAwalDetail.class)
													.add(Restrictions.eq("saldoAwal",
															batchItemPunyaBarcode.getSaldoAwal()))
													.add(Restrictions.eq("item", itemPunyaBarcode.getItem()))
													.uniqueResult();

											if (saldoAwalDetail != null) {

												if (batchItemPunyaBarcode.getSaldoAwal() != null) {
													double itemPunyaBarcodeCount = ((Number) session
															.createCriteria(ItemPunyaBarcode.class)
															.createAlias("batchItemPunyaBarcode",
																	"batchItemPunyaBarcode")
															.add(Restrictions.eq("batchItemPunyaBarcode.saldoAwal",
																	batchItemPunyaBarcode.getSaldoAwal()))
															.add(Restrictions.eq("item", item))
															.setProjection(Projections.rowCount()).uniqueResult())
																	.doubleValue();
													saldoAwalDetail.setJumlah(itemPunyaBarcodeCount);
													session.getTransaction().begin();
													session.update(saldoAwalDetail);
													session.getTransaction().commit();
												} else if (batchItemPunyaBarcode.getPenerimaanPengadaanItem() != null) {
													double itemPunyaBarcodeCount = ((Number) session
															.createCriteria(ItemPunyaBarcode.class)
															.createAlias("batchItemPunyaBarcode",
																	"batchItemPunyaBarcode")
															.add(Restrictions.eq(
																	"batchItemPunyaBarcode.penerimaanPengadaanItem",
																	batchItemPunyaBarcode.getPenerimaanPengadaanItem()))
															.add(Restrictions.eq("item", item))
															.setProjection(Projections.rowCount()).uniqueResult())
																	.doubleValue();
													saldoAwalDetail.setJumlah(itemPunyaBarcodeCount);
													session.getTransaction().begin();
													session.update(saldoAwalDetail);
													session.getTransaction().commit();
												}
											}
										}
										HibernateUtil.closeSession();

									}
	row.setVisible(false);row.detach();

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											loadDataDetail(item);
										}
									});
								}

							}
						});

			}
		});
	}

}
