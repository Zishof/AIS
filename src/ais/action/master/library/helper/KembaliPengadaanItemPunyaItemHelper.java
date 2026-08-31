package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DendaKeterlambatanItem;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.PeminjamanPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI ZK modul perpustakaan untuk memproses pengembalian item pinjaman
 * ({@link KembaliPengadaanItem}), lewat baris {@link KembaliPengadaanItemDetail} yang menautkan
 * kembali ke {@link PeminjamanPengadaanItemDetail} asal. Alur kerja utamanya berbasis pemindaian
 * barcode: petugas memindai barcode fisik item, dan baris yang sesuai pada daftar pinjaman aktif
 * dicentang otomatis sebagai dikembalikan.
 *
 * <p>
 * Dua sumber pengisian baris: {@link #setPeminjamanPengadaanItem} memuat seluruh pinjaman aktif
 * anggota (belum ada {@link KembaliPengadaanItemDetail} terkait) sebagai baris belum dicentang;
 * {@link #initDetail} memuat baris pengembalian yang sudah tersimpan untuk transaksi yang sudah
 * ada. Mencentang checkbox baris (manual atau via scan barcode di {@link #loadBarcode}) memicu
 * perhitungan ulang denda keterlambatan ({@code LibraryUtil#hitungDendaItem}) ditambah biaya
 * penggantian/kerusakan (disimpan terenkode dalam kolom {@code ketDenda}, format
 * {@code "[BIAYA_PENGGANTIAN=nilai]"}) dan kondisi fisik item (disimpan terenkode dalam kolom
 * {@code keterangan}, format {@code "[KONDISI=BAIK|RUSAK|HILANG|PERBAIKAN] catatan"}) — kedua
 * pola encoding string-dalam-kolom ini dipakai karena tidak ada kolom terpisah pada entitas.
 * Tombol Perpanjang/Batal Perpanjang memicu {@code LibraryUtil#onPerpanjang}/{@code
 * onBatalPerpanjang} dan menghitung ulang tampilan; tombol Batalkan (nonaktif) mengarahkan
 * pengguna ke jalur reversal transaksi resmi karena detail pengembalian yang sudah terposting
 * tidak boleh dihapus langsung.
 * </p>
 */
public class KembaliPengadaanItemPunyaItemHelper {

	private MyGrid gridItem;
	private boolean edit = false;
	private boolean delete = false;
	// private Textbox barcode;

	private Perpustakaan perpustakaan;
	private PeminjamanPengadaanItem peminjamanPengadaanItem;
	private KembaliPengadaanItem kembaliPengadaanItem;
	private Textbox barcode;

	/** Membangun helper terikat pada {@code gridItem} dan menghitung hak ubah/hapus pengguna saat ini. */
	public KembaliPengadaanItemPunyaItemHelper(MyGrid gridItem) {
		this.gridItem = gridItem;
		CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun panel (border layout) berisi toolbar pemindaian barcode dan grid daftar item
	 * pengembalian untuk {@code kembaliPengadaanItem}, lalu memuat data yang sudah tersimpan.
	 * Bila {@code barcodeItem} diberikan, langsung memproses barcode tersebut layaknya baru
	 * dipindai; selain itu fokus diarahkan ke kolom input barcode.
	 *
	 * @param kembaliPengadaanItem transaksi pengembalian yang menjadi konteks detail
	 * @param barcodeItem          barcode yang langsung diproses saat panel dibuka, boleh {@code null}
	 * @return border layout siap disisipkan sebagai konten form pengembalian
	 */
	public Borderlayout initDetail(final KembaliPengadaanItem kembaliPengadaanItem, final String barcodeItem)
			throws Exception {
		this.kembaliPengadaanItem = kembaliPengadaanItem;
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);
		//
		// new Label("Barcode/ISBN/ISSN").setParent(toolbar);
		// new Space().setParent(toolbar);
		// barcode = new Textbox();
		// barcode.setDisabled(kembaliPengadaanItem.getDisetujuiOleh() != null);
		// barcode.setParent(toolbar);
		// barcode.addEventListener("onOK", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// loadBarcode(kembaliPengadaanItem);
		// }
		// });
		//

		new Label(ais.common.Common.getBahasaConfig("Scan Barcode Item yang dikembalikan disini : ")).setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setStyle("font-size:xx-large");
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				loadBarcode(kembaliPengadaanItem);

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

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(gridItem.getRows());
				loadDataDetail(kembaliPengadaanItem, true);
				loadDataDetailFromPeminjaman();
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
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("ISBN/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Barcode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Deskripsi");
		column.setWidth("22%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl. Kembali");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadDataDetail(kembaliPengadaanItem, false);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (barcodeItem != null && !barcodeItem.trim().isEmpty()) {
					barcode.setValue(barcodeItem.trim());
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadBarcode(kembaliPengadaanItem);
						}
					});

				} else {
					barcode.focus();
					barcode.select();
				}
			}
		});
		return borderlayout;
	}

	/**
	 * Memuat baris-baris pengembalian tersimpan untuk {@code kembaliPengadaanItem} dari
	 * database. Bila {@code refresh} bernilai {@code true}, denda tiap baris dihitung ulang
	 * ({@code LibraryUtil#hitungDendaItem} ditambah biaya penggantian) dan disimpan bila
	 * berbeda dari nilai tersimpan, sebelum baris dirender ke grid.
	 */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final KembaliPengadaanItem kembaliPengadaanItem, boolean refresh) throws Exception {

		List<KembaliPengadaanItemDetail> kembaliPengadaanItemDetails = kembaliPengadaanItem == null
				|| kembaliPengadaanItem.getId() == null ? new ArrayList<KembaliPengadaanItemDetail>()
						: HibernateUtil.currentSession().createCriteria(KembaliPengadaanItemDetail.class)
								.add(Restrictions.eq("kembaliPengadaanItem", kembaliPengadaanItem)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (KembaliPengadaanItemDetail kembaliPengadaanItemDetail : kembaliPengadaanItemDetails) {

			if (refresh && kembaliPengadaanItemDetail.getId() != null) {
				try {

					PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = kembaliPengadaanItemDetail
							.getPeminjamanPengadaanItemDetail();
					DendaKeterlambatanItem dendaPerItem = LibraryUtil.hitungDendaItem(peminjamanPengadaanItemDetail);

					Double denda = dendaPerItem == null ? 0.0 : dendaPerItem.getDenda();
					denda = denda * peminjamanPengadaanItemDetail.getJumlah();
					denda += replacementCharge(kembaliPengadaanItemDetail.getKetDenda());

					if (denda.intValue() != kembaliPengadaanItemDetail.getDenda().intValue()) {
						Session session = HibernateUtil.currentNativeSession();
						kembaliPengadaanItemDetail.setDenda(denda);
						session.getTransaction().begin();
						session.update(kembaliPengadaanItemDetail);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {
							session.disconnect();
							session.close();
						}
						HibernateUtil.closeSession();
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/library/helper/KembaliPengadaanItemPunyaItemHelper.java:248");
				}

			}

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, kembaliPengadaanItemDetail);
		}
	}

	/**
	 * Mengisi {@code row} dengan checkbox "dikembalikan" (tercentang dan terkunci bila baris
	 * sudah tersimpan), gambar sampul item, tautan riwayat revisi, barcode, ringkasan detail
	 * pinjaman (tanggal pinjam/setujui, batas dan lama pinjam, jumlah perpanjangan,
	 * keterlambatan, denda berjalan), tanggal kembali, status/nominal pembayaran denda, kondisi
	 * fisik item dan biaya penggantian/kerusakan, keterangan, serta tombol Perpanjang/Batal
	 * Perpanjang/Batalkan (nonaktif, mengarahkan ke jalur reversal). Mencentang/melepas checkbox
	 * atau mengubah tanggal memicu perhitungan ulang denda dan pembaruan seluruh label ringkasan
	 * terkait secara langsung di UI; field terkunci mengikuti status persetujuan transaksi dan
	 * hak ubah pengguna.
	 */
	public void initRow(final Row row, final KembaliPengadaanItemDetail kembaliPengadaanItemDetail) throws Exception {
		row.setValign("top");
		row.setAttribute("kembaliPengadaanItemDetail", kembaliPengadaanItemDetail);

		final PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = kembaliPengadaanItemDetail
				.getPeminjamanPengadaanItemDetail();
		Integer jumlahMaksimalPerpanjanganPeminjaman = Integer.valueOf(0);
		if (peminjamanPengadaanItemDetail != null
				&& peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem() != null) {
			jumlahMaksimalPerpanjanganPeminjaman = Integer.valueOf(LibraryUtil
					.getJumlahMaksimalPerpanjanganPeminjaman(peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem()));
			peminjamanPengadaanItemDetail.setJumlahMaxPerpanjangan(jumlahMaksimalPerpanjanganPeminjaman);
		}

		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		checkbox.setParent(row);
		row.setValign("top");
		row.setAttribute("checkbox", checkbox);
		checkbox.setChecked(kembaliPengadaanItemDetail.getId() != null);
		checkbox.setDisabled(kembaliPengadaanItemDetail.getId() != null);

		Image image = LibraryUtil.generateImage(kembaliPengadaanItemDetail.getItem());
		image.setWidth("100%");
		image.setParent(row);

		RevisiHelper.createNewRevisi(KembaliPengadaanItemDetail.class, kembaliPengadaanItemDetail,
				(kembaliPengadaanItemDetail.getItem() == null ? "" : kembaliPengadaanItemDetail.getItem().getIsbn())
						+ " \n" + (kembaliPengadaanItemDetail.getItem() == null ? ""
								: kembaliPengadaanItemDetail.getItem().getNama()))
				.setParent(row);

		new Label(kembaliPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
				: kembaliPengadaanItemDetail.getItemPunyaBarcode().getBarcode()).setParent(row);

		final Label perpanjang = new Label(
				"Perpanjang: " + peminjamanPengadaanItemDetail.getJumlahPerpanjangan() + " kali");

		Vbox vbox = new Vbox();
		vbox.appendChild(new MyLabelKecil("Tgl Pinjam: " + Common.dateFormat4.get()
				.format(peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getTanggalPembuatan())));
		vbox.appendChild(new MyLabelKecil("Tgl Setujui: " + Common.dateFormat4.get()
				.format(peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getTanggalPersetujuan())));
		final MyLabelKecil batasPengembalian;
		vbox.appendChild(batasPengembalian = new MyLabelKecil("Hrs kembali: "
				+ Common.dateFormat4.get().format(peminjamanPengadaanItemDetail.getBatasWaktupengembalian())));
		final MyLabelKecil batas;
		vbox.appendChild(
				batas = new MyLabelKecil("Batas: " + peminjamanPengadaanItemDetail.getJumlahHariBatas() + " hari"));

		final MyLabelKecil lama;
		vbox.appendChild(lama = new MyLabelKecil(
				"Lama pinjam: " + peminjamanPengadaanItemDetail.getJumlahSelisihHari() + " hari"));

		vbox.appendChild(perpanjang);
		final MyLabelKecil terlambat;
		vbox.appendChild(terlambat = new MyLabelKecil(
				"Terlambat: " + peminjamanPengadaanItemDetail.getJumlahHariTerlambat() + " hari"));
		vbox.setParent(row);

		final MyLabelKecil textDenda = new MyLabelKecil(
				"Denda: " + Common.numberFormat.get().format(kembaliPengadaanItemDetail.getDenda()));
		vbox.appendChild(textDenda);

		vbox = new Vbox();
		vbox.setParent(row);
		final ais.ui.util.MyDatebox tanggal = new ais.ui.util.MyDatebox(kembaliPengadaanItemDetail.getTanggal());
		tanggal.setDisabled(!checkbox.isChecked()
				|| (kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit));
		tanggal.setWidth("90%");
		tanggal.setParent(vbox);

		final MyCheckboxConfig telahDibayar = new MyCheckboxConfig("Telah dibayar sejumlah :");
		telahDibayar.setDisabled(!checkbox.isChecked()
				|| (kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit));
		telahDibayar.setChecked(kembaliPengadaanItemDetail.getTelahDibayar());
		telahDibayar.setParent(vbox);

		final ais.ui.util.MyDoublebox dibayarSejumlah = new ais.ui.util.MyDoublebox(
				kembaliPengadaanItemDetail.getDibayarSejumlah());
		// dibayarSejumlah.setDisabled(telahDibayar.isChecked() &&
		// (!checkbox.isChecked()
		// ||
		// (kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh()
		// != null || !edit)));
		dibayarSejumlah.setWidth("90%");
		dibayarSejumlah.setParent(vbox);

		dibayarSejumlah.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				telahDibayar.setChecked(dibayarSejumlah.getValue() != null && dibayarSejumlah.getValue() > 0.1);
			}
		});

		row.setValign("top");
		row.setAttribute("telahDibayar", telahDibayar);
		row.setValign("top");
		row.setAttribute("dibayarSejumlah", dibayarSejumlah);
		row.setValign("top");
		row.setAttribute("tanggal", tanggal);

		telahDibayar.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dibayarSejumlah.setDisabled(telahDibayar.isChecked());
				if (telahDibayar.isChecked()) {
					DendaKeterlambatanItem dendaPerItem = LibraryUtil.hitungDendaItem(peminjamanPengadaanItemDetail);

					Double denda = dendaPerItem == null ? 0.0 : dendaPerItem.getDenda();
					denda = denda * peminjamanPengadaanItemDetail.getJumlah();
					Object chargeField = row.getAttribute("biayaPenggantian");
					if (chargeField instanceof ais.ui.util.MyDoublebox
							&& ((ais.ui.util.MyDoublebox) chargeField).getValue() != null) {
						denda += ((ais.ui.util.MyDoublebox) chargeField).getValue();
					}
					dibayarSejumlah.setValue(denda);
				} else {
					dibayarSejumlah.setValue(0.0);
				}
			}
		});

		final String existingNote = kembaliPengadaanItemDetail.getKeterangan() == null ? ""
				: kembaliPengadaanItemDetail.getKeterangan();
		final Combobox kondisi = new Combobox();
		kondisi.setReadonly(true);
		kondisi.setWidth("90%");
		kondisi.setDisabled(kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit);
		String[] kondisiValues = new String[] { "BAIK", "RUSAK", "HILANG", "PERBAIKAN" };
		for (String value : kondisiValues) {
			Comboitem option = new Comboitem(value.substring(0, 1) + value.substring(1).toLowerCase());
			option.setValue(value); option.setParent(kondisi);
		}
		String initialCondition = existingNote.startsWith("[KONDISI=RUSAK]") ? "RUSAK"
				: existingNote.startsWith("[KONDISI=HILANG]") ? "HILANG"
						: existingNote.startsWith("[KONDISI=PERBAIKAN]") ? "PERBAIKAN" : "BAIK";
		for (Object child : kondisi.getItems()) {
			Comboitem option = (Comboitem) child;
			if (initialCondition.equals(option.getValue())) { kondisi.setSelectedItem(option); break; }
		}
		kondisi.setParent(vbox);
		new MyLabelKecil("Biaya penggantian/kerusakan").setParent(vbox);
		final ais.ui.util.MyDoublebox biayaPenggantian = new ais.ui.util.MyDoublebox(
				replacementCharge(kembaliPengadaanItemDetail.getKetDenda()));
		biayaPenggantian.setWidth("90%");
		biayaPenggantian.setDisabled(kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit);
		biayaPenggantian.setParent(vbox);
		final MyTextbox keterangan = new MyTextbox(existingNote.replaceFirst("^\\[KONDISI=[A-Z]+\\]\\s*", ""));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setParent(vbox);
		row.setAttribute("kondisi", kondisi);
		row.setAttribute("biayaPenggantian", biayaPenggantian);
		row.setAttribute("keteranganKondisi", keterangan);
		keterangan
				.setDisabled(kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String value = kondisi.getSelectedItem() == null ? "BAIK"
						: String.valueOf(kondisi.getSelectedItem().getValue());
				kembaliPengadaanItemDetail.setKeterangan("[KONDISI=" + value + "] " + keterangan.getValue());

				row.setValign("top");
				row.setAttribute("kembaliPengadaanItemDetail", kembaliPengadaanItemDetail);
				if (kembaliPengadaanItemDetail.getId() != null) {
					Common.refreshUpdate(kembaliPengadaanItemDetail);
				}
			}
		});
		kondisi.addEventListener(Events.ON_CHANGE, new EventListener() {
			@Override public void onEvent(Event event) throws Exception {
				String value = kondisi.getSelectedItem() == null ? "BAIK"
						: String.valueOf(kondisi.getSelectedItem().getValue());
				kembaliPengadaanItemDetail.setKeterangan("[KONDISI=" + value + "] " + keterangan.getValue());
				row.setAttribute("kembaliPengadaanItemDetail", kembaliPengadaanItemDetail);
			}
		});
		biayaPenggantian.addEventListener(Events.ON_CHANGE, new EventListener() {
			@Override public void onEvent(Event event) throws Exception {
				Double amount = biayaPenggantian.getValue() == null ? 0.0 : biayaPenggantian.getValue();
				kembaliPengadaanItemDetail.setKetDenda("[BIAYA_PENGGANTIAN=" + amount + "]");
				row.setAttribute("kembaliPengadaanItemDetail", kembaliPengadaanItemDetail);
			}
		});

		final EventListener tanggalEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (tanggal.getValue() == null) {
					tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
				}

				kembaliPengadaanItemDetail.setTanggal(tanggal.getValue());

				PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = kembaliPengadaanItemDetail
						.getPeminjamanPengadaanItemDetail();

				peminjamanPengadaanItemDetail.setTanggalKembali(checkbox.isChecked() ? tanggal.getValue() : null);

				peminjamanPengadaanItemDetail.setKembaliPengadaanItemDetail(kembaliPengadaanItemDetail);

				perpanjang.setValue("Perpanjang: " + peminjamanPengadaanItemDetail.getJumlahPerpanjangan() + " kali");

				terlambat.setValue("Terlambat " + peminjamanPengadaanItemDetail.getJumlahHariTerlambat() + " hari");

				DendaKeterlambatanItem dendaPerItem = LibraryUtil.hitungDendaItem(peminjamanPengadaanItemDetail);

				Double denda = dendaPerItem == null ? 0.0 : dendaPerItem.getDenda();
				denda = denda * peminjamanPengadaanItemDetail.getJumlah();
				Double replacement = biayaPenggantian.getValue() == null ? 0.0 : biayaPenggantian.getValue();
				denda += replacement;

				if (dendaPerItem != null && !dendaPerItem.getKeterangan().isEmpty()) {
					textDenda.setValue(dendaPerItem.getKeterangan());
				} else {
					textDenda.setValue("Denda: " + Common.numberFormat.get().format(denda));
				}

				if (denda > 0.1) {
					textDenda.setStyle("color:red");
				} else {
					textDenda.setStyle("color:black");
				}

				kembaliPengadaanItemDetail.setDenda(denda);
				kembaliPengadaanItemDetail.setKetDenda("[BIAYA_PENGGANTIAN=" + replacement + "]");
				kembaliPengadaanItemDetail.setPeminjamanPengadaanItemDetail(peminjamanPengadaanItemDetail);
				kembaliPengadaanItemDetail.setTelahDibayar(telahDibayar.isChecked());
				kembaliPengadaanItemDetail.setDibayarSejumlah(dibayarSejumlah.getValue());

				row.setValign("top");
				row.setAttribute("kembaliPengadaanItemDetail", kembaliPengadaanItemDetail);
				lama.setValue("Lama pinjam " + peminjamanPengadaanItemDetail.getJumlahSelisihHari() + " hari");
				batasPengembalian.setValue("Hrs kembali: "
						+ Common.dateFormat4.get().format(peminjamanPengadaanItemDetail.getBatasWaktupengembalian()));

				if (peminjamanPengadaanItemDetail.getJumlahHariTerlambat() > 0) {
					terlambat.setStyle("color:red");
				} else {
					terlambat.setStyle("color:black");
				}

				batas.setValue("Batas: " + peminjamanPengadaanItemDetail.getJumlahHariBatas() + " hari");
			}
		};

		final MyToolbarbuttonConfig tombolPerpanjang = new MyToolbarbuttonConfig("Perpanjang", "/img/corner.gif");
		final MyToolbarbuttonConfig batalPerpanjang = new MyToolbarbuttonConfig("Batal Perpanjang",
				"/img/svg/warning-outline.svg");

		EventListener checkEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean locked = !checkbox.isChecked()
						|| kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit;
				tanggal.setDisabled(locked);
				keterangan.setDisabled(locked);
				kondisi.setDisabled(locked);
				biayaPenggantian.setDisabled(locked);
				telahDibayar.setDisabled(locked);
				dibayarSejumlah.setDisabled(locked);

				tanggalEventListener.onEvent(arg0);
			}
		};

		checkbox.setAttribute("checkEventListener", checkEventListener);
		checkbox.addEventListener("onClick", checkEventListener);

		dibayarSejumlah.addEventListener("onChange", checkEventListener);
		telahDibayar.addEventListener("onClick", checkEventListener);

		tanggalEventListener.onEvent(null);
		tanggal.addEventListener("onChange", tanggalEventListener);

		final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
				new java.util.ArrayList<org.zkoss.zk.ui.Component>();

		tombolPerpanjang.setOrient("vertical");
		tombolPerpanjang.setTooltiptext("Perpanjang");
//		tombolPerpanjang.setDisabled(kembaliPengadaanItemDetail.getId() != null || !edit);
		tombolPerpanjang.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LibraryUtil.onPerpanjang(peminjamanPengadaanItemDetail);
				batalPerpanjang.setVisible(peminjamanPengadaanItemDetail.getJumlahPerpanjangan() > 0);
				tombolPerpanjang.setVisible(peminjamanPengadaanItemDetail
						.getJumlahMaxPerpanjangan() > (peminjamanPengadaanItemDetail.getJumlahPerpanjangan()));
				Common.createDefaultTimer(tanggalEventListener);
			}

		});
		aksiButtons.add(tombolPerpanjang);

		batalPerpanjang.setOrient("vertical");
		batalPerpanjang.setTooltiptext("Batal Perpanjang");
//		batalPerpanjang.setDisabled(kembaliPengadaanItemDetail.getId() != null || !edit);
		batalPerpanjang.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LibraryUtil.onBatalPerpanjang(peminjamanPengadaanItemDetail);
				tanggalEventListener.onEvent(event);
				batalPerpanjang.setVisible(peminjamanPengadaanItemDetail.getJumlahPerpanjangan() > 0);
				tombolPerpanjang.setVisible(peminjamanPengadaanItemDetail
						.getJumlahMaxPerpanjangan() > (peminjamanPengadaanItemDetail.getJumlahPerpanjangan()));

				Common.createDefaultTimer(tanggalEventListener);
			}

		});
		aksiButtons.add(batalPerpanjang);

		batalPerpanjang.setVisible(peminjamanPengadaanItemDetail.getJumlahPerpanjangan() > 0);
		tombolPerpanjang.setVisible(peminjamanPengadaanItemDetail
				.getJumlahMaxPerpanjangan() > (peminjamanPengadaanItemDetail.getJumlahPerpanjangan()));

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batalkan", "/img/svg/trash.svg");
		button.setTooltiptext("Gunakan reversal transaksi; detail terposting tidak dapat dihapus");
		button.setOrient("vertical");
		button.setVisible(false);
		aksiButtons.add(button);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show(
						"Detail pengembalian terposting tidak boleh dihapus. Batalkan transaksi dari layar pengembalian agar reversal dan audit tercatat.",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

			}
		});

		ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);
	}

	public Perpustakaan getPerpustakaan() {
		return perpustakaan;
	}

	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	public PeminjamanPengadaanItem getPeminjamanPengadaanItem() {
		return peminjamanPengadaanItem;
	}

	/** Menetapkan transaksi peminjaman sumber dan langsung memuat item-item aktifnya sebagai baris pengembalian lewat {@link #loadDataDetailFromPeminjaman()}. */
	public void setPeminjamanPengadaanItem(PeminjamanPengadaanItem peminjamanPengadaanItem) {
		this.peminjamanPengadaanItem = peminjamanPengadaanItem;
		try {
			loadDataDetailFromPeminjaman();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memuat seluruh detail pinjaman aktif ({@link PeminjamanPengadaanItemDetail} tanpa
	 * {@code kembaliPengadaanItemDetail} terkait) milik anggota yang sama dengan
	 * {@link #peminjamanPengadaanItem}, membangun baris {@link KembaliPengadaanItemDetail} baru
	 * (belum tersimpan) untuk masing-masing, dan merendernya ke grid.
	 */
	@SuppressWarnings("unchecked")
	public void loadDataDetailFromPeminjaman() throws Exception {

		List<PeminjamanPengadaanItemDetail> peminjamanPengadaanItemDetails = HibernateUtil.currentSession()
				.createCriteria(PeminjamanPengadaanItemDetail.class)
				.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
				.add(Restrictions.eq("peminjamanPengadaanItem.anggota", peminjamanPengadaanItem.getAnggota()))
				.add(Restrictions.isNull("kembaliPengadaanItemDetail")).list();

		kembaliPengadaanItem.setPeminjamanPengadaanItem(peminjamanPengadaanItem);

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : peminjamanPengadaanItemDetails) {

			KembaliPengadaanItemDetail kembaliPengadaanItemDetail = peminjamanPengadaanItemDetail
					.getKembaliPengadaanItemDetail();

			if (kembaliPengadaanItemDetail == null) {
				kembaliPengadaanItemDetail = new KembaliPengadaanItemDetail();

				kembaliPengadaanItemDetail.setItem(peminjamanPengadaanItemDetail.getItem());
				kembaliPengadaanItemDetail.setDikembali(peminjamanPengadaanItemDetail.getJumlah());
				kembaliPengadaanItemDetail.setKeterangan("");
				kembaliPengadaanItemDetail.setKembaliPengadaanItem(kembaliPengadaanItem);
				kembaliPengadaanItemDetail.setItemPunyaBarcode(peminjamanPengadaanItemDetail.getItemPunyaBarcode());
				kembaliPengadaanItemDetail.setPeminjamanPengadaanItemDetail(peminjamanPengadaanItemDetail);

				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				initRow(row, kembaliPengadaanItemDetail);
			}
		}
	}

	/**
	 * Memproses satu pemindaian barcode: mencari {@link ItemPunyaBarcode} yang cocok persis
	 * (fallback ke pencarian {@link Item} lewat ISBN10/ISBN/ISSN bila barcode tidak ditemukan
	 * langsung, namun tetap mensyaratkan {@code itemPunyaBarcode} valid), lalu mencari baris di
	 * grid yang barcode-nya cocok dan mencentang checkbox-nya (memicu listener perhitungan ulang
	 * yang sama seperti mencentang manual). Menampilkan pesan peringatan pada setiap kegagalan
	 * (barcode kosong, tidak ditemukan, grid belum berisi data, atau ditemukan di master item
	 * tapi tidak ada dalam daftar pinjaman aktif) dan selalu mengembalikan fokus ke kolom
	 * barcode setelahnya.
	 */
	@SuppressWarnings("unchecked")
	public void loadBarcode(KembaliPengadaanItem kembaliPengadaanItem) throws Exception {
		String barcodeText = barcode == null ? null : barcode.getText();
		barcodeText = barcodeText == null ? "" : barcodeText.trim();
		if (barcodeText.length() == 0) {
			MyMessageboxConfig.show("Barcode harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			focusBarcodeInput();
			return;
		}

		Session session = HibernateUtil.currentSession();
		ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.ilike("barcode", barcodeText, MatchMode.EXACT)).setMaxResults(1).uniqueResult();

		Item item = null;
		if (itemPunyaBarcode != null) {
			item = itemPunyaBarcode.getItem();
		} else {
			item = (Item) session.createCriteria(Item.class)
					.add(Restrictions.or(Restrictions.ilike("isbn10", barcodeText, MatchMode.EXACT),
							Restrictions.or(Restrictions.ilike("isbn", barcodeText, MatchMode.EXACT),
									Restrictions.ilike("issn", barcodeText, MatchMode.EXACT))))
					.setMaxResults(1).uniqueResult();
		}

		if (item == null || itemPunyaBarcode == null || itemPunyaBarcode.getId() == null) {
			MyMessageboxConfig.show("Barcode " + barcodeText + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			focusBarcodeInput();
			return;
		}

		Rows rows = gridItem == null ? null : gridItem.getRows();
		if (rows == null || rows.getChildren() == null || rows.getChildren().isEmpty()) {
			MyMessageboxConfig.show("Daftar item pengembalian belum tersedia. Silakan pilih data peminjaman terlebih dahulu.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			focusBarcodeInput();
			return;
		}

		boolean ditemukanDiDaftar = false;
		List children = new ArrayList(rows.getChildren());
		for (int i = 0; i < children.size(); i++) {
			Object child = children.get(i);
			if (!(child instanceof Row)) {
				continue;
			}

			Row row = (Row) child;
			KembaliPengadaanItemDetail kembaliPengadaanItemDetail = getDetailFromRow(row);
			if (!samaBarcode(itemPunyaBarcode, kembaliPengadaanItemDetail)) {
				continue;
			}

			ditemukanDiDaftar = true;
			MyCheckboxConfig checkbox = getCheckboxFromRow(row);
			if (checkbox == null) {
				MyMessageboxConfig.show("Barcode " + barcodeText
						+ " ditemukan, tetapi baris item belum memiliki checkbox pengembalian. Silakan reload data peminjaman.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				break;
			}

			checkbox.setChecked(true);
			EventListener checkEventListener = (EventListener) checkbox.getAttribute("checkEventListener");
			if (checkEventListener != null) {
				checkEventListener.onEvent(null);
			}
			break;
		}

		if (!ditemukanDiDaftar) {
			MyMessageboxConfig.show("Barcode " + barcodeText
					+ " ditemukan di master item, tetapi tidak termasuk dalam daftar item yang sedang dipinjam/dikembalikan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		}

		focusBarcodeInput();
	}

	/** Mengambil attribute {@code kembaliPengadaanItemDetail} dari {@code row}, atau {@code null} bila tidak ada/tidak bertipe sesuai. */
	private KembaliPengadaanItemDetail getDetailFromRow(Row row) {
		if (row == null) {
			return null;
		}
		Object object = row.getAttribute("kembaliPengadaanItemDetail");
		if (object instanceof KembaliPengadaanItemDetail) {
			return (KembaliPengadaanItemDetail) object;
		}
		return null;
	}

	/** Mengambil attribute {@code checkbox} dari {@code row}, atau {@code null} bila tidak ada/tidak bertipe sesuai. */
	private MyCheckboxConfig getCheckboxFromRow(Row row) {
		if (row == null) {
			return null;
		}
		Object object = row.getAttribute("checkbox");
		if (object instanceof MyCheckboxConfig) {
			return (MyCheckboxConfig) object;
		}
		return null;
	}

	/** Mengecek apakah {@code itemPunyaBarcode} adalah barcode yang sama (id sama) dengan yang tercatat pada {@code kembaliPengadaanItemDetail}. */
	private boolean samaBarcode(ItemPunyaBarcode itemPunyaBarcode,
			KembaliPengadaanItemDetail kembaliPengadaanItemDetail) {
		if (itemPunyaBarcode == null || itemPunyaBarcode.getId() == null || kembaliPengadaanItemDetail == null
				|| kembaliPengadaanItemDetail.getItemPunyaBarcode() == null
				|| kembaliPengadaanItemDetail.getItemPunyaBarcode().getId() == null) {
			return false;
		}
		return itemPunyaBarcode.getId().equals(kembaliPengadaanItemDetail.getItemPunyaBarcode().getId());
	}

	/** Mengembalikan fokus dan menyeleksi isi kolom input barcode lewat timer default (memastikan komponen sudah siap sebelum fokus diberikan). */
	private void focusBarcodeInput() {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (KembaliPengadaanItemPunyaItemHelper.this.barcode != null) {
					KembaliPengadaanItemPunyaItemHelper.this.barcode.focus();
					KembaliPengadaanItemPunyaItemHelper.this.barcode.select();
				}
			}
		});
	}

	/** Mengekstrak nilai biaya penggantian dari string terenkode {@code "[BIAYA_PENGGANTIAN=nilai]"} pada {@code value}, atau {@code 0.0} bila polanya tidak cocok/{@code value} {@code null}. */
	private static Double replacementCharge(String value) {
		if (value == null) return 0.0;
		try {
			java.util.regex.Matcher matcher = java.util.regex.Pattern
					.compile("\\[BIAYA_PENGGANTIAN=([0-9]+(?:\\.[0-9]+)?)\\]").matcher(value);
			return matcher.find() ? Double.valueOf(matcher.group(1)) : 0.0;
		} catch (Exception ignored) {
			return 0.0;
		}
	}

}
