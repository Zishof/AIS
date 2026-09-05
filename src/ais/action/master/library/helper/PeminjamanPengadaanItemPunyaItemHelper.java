package ais.action.master.library.helper;

import java.util.ArrayList;
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
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.library.Anggota;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.PeminjamanPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI pengelola item pinjaman pada satu transaksi sirkulasi peminjaman perpustakaan
 * ({@link PeminjamanPengadaanItem}/{@link PeminjamanPengadaanItemDetail}). Dirancang untuk alur
 * kerja pemindaian cepat: kolom barcode besar ({@code font-size:xx-large}) menerima input scanner
 * dan langsung menambahkan item saat Enter ditekan ({@link #loadBarcode}), dengan validasi
 * berlapis sebelum penambahan — anggota dan perpustakaan harus sudah dipilih, batas maksimal
 * jumlah item per anggota ({@code jumlahMaksimalPeminjaman}, dihitung lewat
 * {@link LibraryUtil#getJumlahMaksimalPeminjaman}) tidak boleh terlampaui, item dengan barcode
 * yang sama tidak boleh dipinjam dua kali sekaligus (bila konfigurasi
 * {@code item_yg_disirkulasikan_tidak_boleh_sama} aktif), dan item yang barcode-nya masih dipinjam
 * anggota lain (belum ada catatan pengembalian) ditolak dengan pesan mencantumkan peminjam
 * sebelumnya. Alternatif penambahan lewat picker banyak-pilih terbatas stok
 * ({@code AmbilDataItemBanyakBerdasarkanStok}) juga tersedia. Jumlah pinjam per item dibatasi
 * {@code perpustakaan.getMaxPinjam()}. Permintaan yang sudah disetujui mengunci seluruh input.
 */
public class PeminjamanPengadaanItemPunyaItemHelper {

	private MyGrid gridItem;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;
	private Textbox barcode;

	private Perpustakaan perpustakaan;
	private PeminjamanPengadaanItem peminjamanPengadaanItem;
	private Number jumlahMaksimalPeminjaman;

	/** Membuat helper untuk {@code gridItem} dan menentukan visibilitas/status enable tombol tambah/edit/hapus dari hak akses pengguna saat ini. */
	public PeminjamanPengadaanItemPunyaItemHelper(MyGrid gridItem) {
		this.gridItem = gridItem;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/** Menetapkan anggota dan perpustakaan peminjam pada transaksi, lalu menghitung ulang dan menyimpan batas maksimal jumlah item yang boleh dipinjam anggota tersebut. */
	public void setAnggota(Anggota anggota, Perpustakaan perpustakaan) {
		peminjamanPengadaanItem.setAnggota(anggota);
		peminjamanPengadaanItem.setPerpustakaan(perpustakaan);

		if (anggota != null && perpustakaan != null) {
			jumlahMaksimalPeminjaman = LibraryUtil.getJumlahMaksimalPeminjaman(peminjamanPengadaanItem);
			peminjamanPengadaanItem.setJumlahMaksimalPeminjaman(
					jumlahMaksimalPeminjaman == null ? null : jumlahMaksimalPeminjaman.intValue());
		}
	}

	/**
	 * Membangun tata letak lengkap panel item pinjaman untuk {@code peminjamanPengadaanItem}:
	 * toolbar berisi tombol "Tambah Item" (picker terbatas stok, dengan validasi anggota/
	 * perpustakaan/batas maksimal) dan kolom scan barcode besar (memicu {@link #loadBarcode} saat
	 * Enter, dinonaktifkan bila permintaan sudah disetujui); diikuti grid kolom gambar/kode/
	 * barcode/nama/jumlah/keterangan/hapus yang langsung dimuat dengan data tersimpan
	 * ({@link #loadDataDetail}).
	 *
	 * @param peminjamanPengadaanItem transaksi peminjaman yang daftar itemnya dikelola
	 * @return {@link Borderlayout} siap ditempelkan ke jendela detail transaksi
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen/query data
	 */
	public Borderlayout initDetail(final PeminjamanPengadaanItem peminjamanPengadaanItem) throws Exception {
		this.peminjamanPengadaanItem = peminjamanPengadaanItem;
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Item", "/img/new.gif");
		add.setVisible(PeminjamanPengadaanItemPunyaItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (PeminjamanPengadaanItemPunyaItemHelper.this.peminjamanPengadaanItem.getAnggota() == null) {
					MyMessageboxConfig.show("Pilih salah satu anggota perpustakaan", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (PeminjamanPengadaanItemPunyaItemHelper.this.peminjamanPengadaanItem.getPerpustakaan() == null) {
					MyMessageboxConfig.show("Pilih salah satu perpustakaan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (jumlahMaksimalPeminjaman != null
						&& jumlahMaksimalPeminjaman.intValue() < gridItem.getRows().getChildren().size()) {
					MyMessageboxConfig.show(
							"Jumlah maksimal item yang boleh dipinjam adalah " + jumlahMaksimalPeminjaman + " buah",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				peminjamanPengadaanItem.setJumlahMaksimalPeminjaman(
						jumlahMaksimalPeminjaman == null ? null : jumlahMaksimalPeminjaman.intValue());

				List<Item> items = new ArrayList<Item>();
				List<Row> myrows = gridItem.getRows().getChildren();
				for (Row row : myrows) {
					items.add(((PeminjamanPengadaanItemDetail) row.getAttribute("peminjamanPengadaanItemDetail"))
							.getItem());
				}
				AmbilDataItemBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemBanyakBerdasarkanStok(items,
						peminjamanPengadaanItem.getPerpustakaan(), true);
				ambilDataItemBanyak.setHeight("95%");
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						for (Item item : items) {

							if (Common.bolehKonfigurasi("item_yg_disirkulasikan_tidak_boleh_sama")) {
								Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
								List<Row> rows2 = rows.getChildren();
								for (Row r : rows2) {
									PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = (PeminjamanPengadaanItemDetail) r
											.getAttribute("peminjamanPengadaanItemDetail");
									if (peminjamanPengadaanItemDetail.getItem().getId().equals(item.getId())) {
										MyMessageboxConfig.show("Item \"" + item.getNama() + "\" telah diinput",
												"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														PeminjamanPengadaanItemPunyaItemHelper.this.barcode.focus();
														PeminjamanPengadaanItemPunyaItemHelper.this.barcode.select();
													}
												});
										return;
									}
								}
							}

							PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = new PeminjamanPengadaanItemDetail();
							peminjamanPengadaanItemDetail.setItem(item);
							peminjamanPengadaanItemDetail.setJumlah(1.0);
							peminjamanPengadaanItemDetail.setKeterangan("");
							peminjamanPengadaanItemDetail.setPeminjamanPengadaanItem(peminjamanPengadaanItem);

							if (peminjamanPengadaanItem.getId() != null) {

								peminjamanPengadaanItem.setJumlahMaksimalPeminjaman(
										jumlahMaksimalPeminjaman == null ? null : jumlahMaksimalPeminjaman.intValue());

								Common.refreshSaveOrUpdate(peminjamanPengadaanItem);
								Common.refreshSaveOrUpdate(peminjamanPengadaanItemDetail);

							}

							Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
							rows.setParent(gridItem);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, peminjamanPengadaanItemDetail);
						}
					}
				});

				ambilDataItemBanyak.onModal();

			}
		});

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label(ais.common.Common.getBahasaConfig("Scan Barcode")).setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(peminjamanPengadaanItem.getDisetujuiOleh() != null);
		barcode.setStyle("font-size:xx-large");
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (PeminjamanPengadaanItemPunyaItemHelper.this.peminjamanPengadaanItem.getAnggota() == null) {
					MyMessageboxConfig.show("Pilih salah satu anggota perpustakaan", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (PeminjamanPengadaanItemPunyaItemHelper.this.peminjamanPengadaanItem.getPerpustakaan() == null) {
					MyMessageboxConfig.show("Pilih salah satu perpustakaan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (jumlahMaksimalPeminjaman != null
						&& jumlahMaksimalPeminjaman.intValue() < gridItem.getRows().getChildren().size()) {
					MyMessageboxConfig.show(
							"Jumlah maksimal item yang boleh dipinjam adalah " + jumlahMaksimalPeminjaman + " buah",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				peminjamanPengadaanItem.setJumlahMaksimalPeminjaman(
						jumlahMaksimalPeminjaman == null ? null : jumlahMaksimalPeminjaman.intValue());

				loadBarcode(peminjamanPengadaanItem);

				Common.createDefaultTimerNoBusy(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						barcode.focus();
						barcode.select();
					}
				});
			}
		});

		barcode.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				barcode.select();
			}
		});

		// MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari",
		// "/img/svg/search.svg");
		// cari.setParent(toolbar);
		// cari.setDisabled(peminjamanPengadaanItem.getDisetujuiOleh() != null);
		// cari.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// if
		// (PeminjamanPengadaanItemPunyaItemHelper.this.peminjamanPengadaanItem.getAnggota()
		// == null) {
		// MyMessageboxConfig.show("Pilih salah satu anggota perpustakaan",
		// "Peringatan", MyMessageboxConfig.OK,
		// MyMessageboxConfig.EXCLAMATION);
		// return;
		// }
		//
		// if
		// (PeminjamanPengadaanItemPunyaItemHelper.this.peminjamanPengadaanItem.getPerpustakaan()
		// == null) {
		// MyMessageboxConfig.show("Pilih salah satu perpustakaan",
		// "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.EXCLAMATION);
		// return;
		// }
		//
		// if (jumlahMaksimalPeminjaman != null
		// && jumlahMaksimalPeminjaman.intValue() <
		// gridItem.getRows().getChildren().size()) {
		// MyMessageboxConfig.show(
		// "Jumlah maksimal item yang boleh dipinjam adalah " +
		// jumlahMaksimalPeminjaman + " buah",
		// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		// return;
		// }
		//
		// peminjamanPengadaanItem.setJumlahMaksimalPeminjaman(
		// jumlahMaksimalPeminjaman == null ? null :
		// jumlahMaksimalPeminjaman.intValue());
		//
		// loadBarcode(peminjamanPengadaanItem);
		// }
		// });

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
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Barcode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadDataDetail(peminjamanPengadaanItem);

		return borderlayout;
	}

	/** Memuat seluruh {@link PeminjamanPengadaanItemDetail} tersimpan milik {@code peminjamanPengadaanItem} (kosong bila belum persisten) dan merender masing-masing sebagai baris grid. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PeminjamanPengadaanItem peminjamanPengadaanItem) throws Exception {

		List<PeminjamanPengadaanItemDetail> peminjamanPengadaanItemDetails = peminjamanPengadaanItem == null
				|| peminjamanPengadaanItem.getId() == null ? new ArrayList<PeminjamanPengadaanItemDetail>()
						: HibernateUtil.currentSession().createCriteria(PeminjamanPengadaanItemDetail.class)
								.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : peminjamanPengadaanItemDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, peminjamanPengadaanItemDetail);
		}
	}

	/**
	 * Mengisi satu baris grid dengan gambar item, kode ISBN/ISSN, barcode spesifik yang dipinjam,
	 * label revisi+nama item, input jumlah (divalidasi terhadap {@code perpustakaan.getMaxPinjam()},
	 * direset ke 1 bila melebihi), field keterangan, dan tombol hapus (dengan dialog konfirmasi).
	 * Seluruh input dinonaktifkan bila permintaan induk sudah disetujui atau pengguna tidak punya
	 * hak edit.
	 *
	 * @param row                              baris ZK yang akan diisi
	 * @param peminjamanPengadaanItemDetail    entitas detail item (baru atau tersimpan) yang direpresentasikan baris ini
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen
	 */
	public void initRow(final Row row, final PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail)
			throws Exception {

		Image image = LibraryUtil.generateImage(peminjamanPengadaanItemDetail.getItem());
		image.setWidth("100%");
		image.setParent(row);

		row.setValign("top");row.setAttribute("peminjamanPengadaanItemDetail", peminjamanPengadaanItemDetail);

		final MyDoublebox jumlah = new MyDoublebox(
				peminjamanPengadaanItemDetail.getJumlah() == null ? 0.0 : peminjamanPengadaanItemDetail.getJumlah());

		new Label(peminjamanPengadaanItemDetail.getItem() == null ? ""
				: peminjamanPengadaanItemDetail.getItem().getIsbn() + " "
						+ peminjamanPengadaanItemDetail.getItem().getIssn()).setParent(row);

		new Label(peminjamanPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
				: peminjamanPengadaanItemDetail.getItemPunyaBarcode().getBarcode()).setParent(row);

		RevisiHelper.createNewRevisi(PeminjamanPengadaanItemDetail.class, peminjamanPengadaanItemDetail,
				peminjamanPengadaanItemDetail.getItem() == null ? ""
						: peminjamanPengadaanItemDetail.getItem().getNama())
				.setParent(row);

		(jumlah).setParent(row);
		jumlah.setDisabled(
				peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getDisetujuiOleh() != null || !edit);
		jumlah.setStyle("text-align:right");
		jumlah.setWidth("90%");
		jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (perpustakaan == null) {
					MyMessageboxConfig.show("Pilih salah satu perpustakaan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (jumlah.getValue() != null && jumlah.getValue() > perpustakaan.getMaxPinjam()) {
					MyMessageboxConfig.show(
							"Maksimal peminjaman " + perpustakaan.getNama() + " tidak boleh melebihi "
									+ perpustakaan.getMaxPinjam(),
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					jumlah.setValue(1.0);
					return;
				}

				peminjamanPengadaanItemDetail.setJumlah(jumlah.getValue());
				row.setValign("top");row.setAttribute("peminjamanPengadaanItemDetail", peminjamanPengadaanItemDetail);
				if (peminjamanPengadaanItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.refresh(peminjamanPengadaanItemDetail);
					session.update((peminjamanPengadaanItemDetail));
				}
			}
		});

		final MyTextbox keterangan = new MyTextbox(peminjamanPengadaanItemDetail.getKeterangan() == null ? ""
				: peminjamanPengadaanItemDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);
		keterangan.setDisabled(
				peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getDisetujuiOleh() != null || !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				peminjamanPengadaanItemDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("peminjamanPengadaanItemDetail", peminjamanPengadaanItemDetail);
				if (peminjamanPengadaanItemDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.refresh(peminjamanPengadaanItemDetail);
					session.update(peminjamanPengadaanItemDetail);
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
									if (peminjamanPengadaanItemDetail.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(peminjamanPengadaanItemDetail);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

	/**
	 * Menangani pemindaian/pengetikan satu barcode: mencari {@link ItemPunyaBarcode} persis sesuai
	 * teks (fallback ke ISBN-10/ISBN/ISSN bila tidak ditemukan sebagai barcode — namun penambahan
	 * hanya berlanjut bila keduanya, item maupun {@code itemPunyaBarcode}, ditemukan). Menolak
	 * dengan pesan bila: barcode kosong, item tidak ditemukan, salinan barcode tersebut masih
	 * dipinjam anggota lain (belum ada {@code kembaliPengadaanItemDetail}), eksemplar itu pernah
	 * dikembalikan dalam kondisi HILANG/RUSAK/PERBAIKAN (lihat
	 * {@link LibraryUtil#eksemplarKondisiTidakTersedia(ItemPunyaBarcode)}), atau item yang sama
	 * sudah ada di grid (bila konfigurasi {@code item_yg_disirkulasikan_tidak_boleh_sama} aktif).
	 * Bila valid, menambahkan baris detail baru (jumlah 1, terikat ke barcode spesifik) ke grid dan
	 * menyimpan langsung ke database bila transaksi sudah persisten. Fokus dikembalikan ke kolom
	 * barcode setelah selesai agar pemindaian berikutnya dapat langsung dilakukan.
	 *
	 * @param peminjamanPengadaanItem transaksi peminjaman tempat detail baru ditambahkan
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen/query data
	 */
	@SuppressWarnings("unchecked")
	public void loadBarcode(PeminjamanPengadaanItem peminjamanPengadaanItem) throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Barcode harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							PeminjamanPengadaanItemPunyaItemHelper.this.barcode.focus();
							PeminjamanPengadaanItemPunyaItemHelper.this.barcode.select();
						}
					});
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

		if (item == null || itemPunyaBarcode == null) {
			MyMessageboxConfig.show("Barcode " + barcode + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							PeminjamanPengadaanItemPunyaItemHelper.this.barcode.focus();
							PeminjamanPengadaanItemPunyaItemHelper.this.barcode.select();
						}
					});
			return;
		}

		int count = ((Number) session.createCriteria(PeminjamanPengadaanItemDetail.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("itemPunyaBarcode", itemPunyaBarcode))
				.add(Restrictions.isNull("kembaliPengadaanItemDetail")).uniqueResult()).intValue();
		System.out.println(" count => " + count);
		if (count > 0) {
			PeminjamanPengadaanItemDetail belumkembali = (PeminjamanPengadaanItemDetail) session
					.createCriteria(PeminjamanPengadaanItemDetail.class)
					.add(Restrictions.eq("itemPunyaBarcode", itemPunyaBarcode))
					.add(Restrictions.isNull("kembaliPengadaanItemDetail")).addOrder(Order.desc("id"))
					.setMaxResults(1).uniqueResult();
			if (belumkembali != null) {
				MyMessageboxConfig.show(
						"Item dengan barcode " + barcode + " belum dikembalikan oleh anggota "
								+ belumkembali.getPeminjamanPengadaanItem().getAnggota() + " pada tanggal/waktu "
								+ (belumkembali.getPeminjamanPengadaanItem() == null ? ""
										: Common.dateFormat5.get().format(
												belumkembali.getPeminjamanPengadaanItem().getTanggalPembuatan())),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								PeminjamanPengadaanItemPunyaItemHelper.this.barcode.focus();
								PeminjamanPengadaanItemPunyaItemHelper.this.barcode.select();
							}
						});
				return;
			}
		}

		if (LibraryUtil.eksemplarKondisiTidakTersedia(itemPunyaBarcode)) {
			MyMessageboxConfig.show(
					"Item dengan barcode " + barcode
							+ " tercatat dalam kondisi rusak/hilang pada pengembalian sebelumnya, sehingga tidak dapat dipinjamkan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							PeminjamanPengadaanItemPunyaItemHelper.this.barcode.focus();
							PeminjamanPengadaanItemPunyaItemHelper.this.barcode.select();
						}
					});
			return;
		}

		if (Common.bolehKonfigurasi("item_yg_disirkulasikan_tidak_boleh_sama")) {
			Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
			List<Row> rows2 = rows.getChildren();
			for (Row r : rows2) {
				PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = (PeminjamanPengadaanItemDetail) r
						.getAttribute("peminjamanPengadaanItemDetail");
				if (peminjamanPengadaanItemDetail.getItem().getId().equals(item.getId())) {
					MyMessageboxConfig.show("Item \"" + item.getNama() + "\" telah diinput", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									PeminjamanPengadaanItemPunyaItemHelper.this.barcode.focus();
									PeminjamanPengadaanItemPunyaItemHelper.this.barcode.select();
								}
							});
					return;
				}
			}
		}

		PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = new PeminjamanPengadaanItemDetail();
		peminjamanPengadaanItemDetail.setItem(item);
		peminjamanPengadaanItemDetail.setJumlah(1.0);
		peminjamanPengadaanItemDetail.setKeterangan("");
		peminjamanPengadaanItemDetail.setPeminjamanPengadaanItem(peminjamanPengadaanItem);
		peminjamanPengadaanItemDetail.setItemPunyaBarcode(itemPunyaBarcode);

		if (peminjamanPengadaanItem.getId() != null) {
			session.save(peminjamanPengadaanItemDetail);
		}

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);
		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		initRow(row, peminjamanPengadaanItemDetail);

		this.barcode.focus();
		this.barcode.select();
	}

	/** Mengembalikan perpustakaan yang sedang dipakai sebagai konteks batas pinjam. */
	public Perpustakaan getPerpustakaan() {
		return perpustakaan;
	}

	/** Menetapkan perpustakaan konteks (dipakai untuk validasi {@code maxPinjam} per item, terpisah dari perpustakaan pada entitas transaksi). */
	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

}
