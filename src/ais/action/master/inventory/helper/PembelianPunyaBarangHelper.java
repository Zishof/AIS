package ais.action.master.inventory.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class PembelianPunyaBarangHelper {

	private MyGrid gridItem;
	private boolean add = false;
	private boolean delete = false;
	private Textbox barcode;
	private Label totalBelanja;
	public double total = 0.0;

	List<Pembelian> pembelians;
	private Combobox toko;

	public PembelianPunyaBarangHelper(MyGrid gridItem, Combobox toko) {
		this.gridItem = gridItem;
		this.toko = toko;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	@SuppressWarnings("unchecked")
	public Borderlayout initDetail(final Pembelian pembelian) throws Exception {
		pembelians = new ArrayList<Pembelian>();
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);
		buildInfoHtmlInventoryV1("Daftar Barang Dibeli", "Bagian ini digunakan untuk memilih produk, mengisi jumlah barang, dan menghitung total belanja. Informasi ini membantu kasir memastikan item yang masuk ke transaksi sudah sesuai sebelum disimpan.").setParent(toolbar);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Barang", "/img/svg/add.svg");
		add.setVisible(PembelianPunyaBarangHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (toko.getSelectedItem() == null || toko.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu diminta untuk memilih Toko / Pedagang terlebih dahulu sebelum melanjutkan proses. Langkah yang dapat dilakukan: (1) buka daftar Toko / Pedagang; (2) pilih salah satu Toko / Pedagang yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				List<Produk> produks = new ArrayList<Produk>();

				for (Pembelian row : pembelians) {
					produks.add(row.getProduk());
				}

				AmbilDataProdukBanyak ambilDataProdukBanyak = new AmbilDataProdukBanyak(produks,
						(Toko) (toko.getSelectedItem() == null ? null : toko.getSelectedItem().getValue()));
				ambilDataProdukBanyak.setHeight("95%");
				ambilDataProdukBanyak.setWidth("90%");
				ambilDataProdukBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataProdukBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Produk> produks = (List<Produk>) arg0.getData();
						for (Produk produk : produks) {

							Pembelian barangBeli = new Pembelian();
							barangBeli.setProduk(produk);
							barangBeli.setKios(pembelian.getKios());
							barangBeli.setKode(pembelian.getKode());
							barangBeli.setKeterangan(pembelian.getKeterangan());
							barangBeli.setSiswa(pembelian.getSiswa());
							barangBeli.setMember(
									pembelian.getSiswa() != null ? pembelian.getSiswa().getNomorInduk() : "1");
							barangBeli.setToko((Toko) toko.getSelectedItem().getValue());
							loadBarcode(barangBeli, produk.getKode());
						}
					}
				});

				ambilDataProdukBanyak.onModal();

			}
		});

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label(ais.common.Common.getBahasaConfig("Scan Barcode")).setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		// barcode.setDisabled(pembelian.getDisetujuiOleh() != null);
		barcode.setStyle("font-size:xx-large");
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (toko.getSelectedItem() == null || toko.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu diminta untuk memilih Toko / Pedagang terlebih dahulu sebelum melanjutkan proses. Langkah yang dapat dilakukan: (1) buka daftar Toko / Pedagang; (2) pilih salah satu Toko / Pedagang yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				}

				Pembelian barangBeli = new Pembelian();
				barangBeli.setKios(pembelian.getKios());
				barangBeli.setKode(pembelian.getKode());
				barangBeli.setKeterangan(pembelian.getKeterangan());
				barangBeli.setSiswa(pembelian.getSiswa());
				barangBeli.setMember(pembelian.getSiswa() != null ? pembelian.getSiswa().getNomorInduk() : "1");
				barangBeli.setToko((Toko) toko.getSelectedItem().getValue());
				loadBarcode(barangBeli, null);
			}
		});

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		totalBelanja = new MyLabelBolder("TOTAL : Rp. " + Common.numberFormat.get().format(total));
		totalBelanja.setParent(toolbar);
		

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
		column.setLabel("Kode/Barcode");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setWidth("15%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("50px");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("15%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		if (pembelian != null && pembelian.getId() != null) {
			Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
			rows.setParent(gridItem);
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, pembelian);
			hitungUlangTotal();
		}

		return borderlayout;
	}

	public void initRow(final Row row, final Pembelian pembelian) throws Exception {

		row.setValign("top");
		row.setAttribute("pembelian", pembelian);

		new Label(pembelian.getProduk() == null ? "" : pembelian.getProduk().getKode()).setParent(row);

		new Label(pembelian.getProduk() == null ? "" : pembelian.getProduk().getNama()).setParent(row);

		new Label(pembelian.getProduk() == null ? "" : "Rp. " + pembelian.getProduk().getHargaJual()).setParent(row);

		final MyDoublebox jumlah = new MyDoublebox(pembelian.getQty() == null ? 0.0 : pembelian.getQty());

		final Label total = new Label(pembelian.getProduk() == null ? ""
				: "Rp. " + (pembelian.getProduk().getHargaJual() * jumlah.getValue()));

		(jumlah).setParent(row);
		jumlah.setStyle("text-align:right");
		jumlah.setWidth("90%");
		jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				pembelian.setQty(jumlah.getValue());
				pembelian.setHargaSatuan(pembelian.getProduk().getHargaJual());
				pembelian.setHargaJual((pembelian.getProduk().getHargaJual() * jumlah.getValue()));
				row.setValign("top");
				row.setAttribute("pembelian", pembelian);

				total.setValue(pembelian.getProduk() == null ? ""
						: "Rp. " + (pembelian.getProduk().getHargaJual() * jumlah.getValue()));

				hitungUlangTotal();
			}
		});

		total.setParent(row);

		final MyTextbox keterangan = new MyTextbox(pembelian.getKeterangan() == null ? "" : pembelian.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);

		// keterangan.setDisabled(pembelianDetail.getpembelian().getDisetujuiOleh()
		// != null || !edit);
		// keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// pembelianDetail.setKeterangan(keterangan.getValue());
		// row.setValign("top");row.setAttribute("pembelianDetail", pembelianDetail);
		// if (pembelianDetail.getId() != null) {
		// Session session = HibernateUtil.currentSession();
		// session.refresh(pembelianDetail);
		// session.update(pembelianDetail);
		// }
		// }
		// });

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diperhatikan bahwa data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (pembelian.getId() != null) {
										Session session = HibernateUtil.currentSession();
										ais.action.master.koperasi.helper.PembelianReferenceCleanupUtil
												.lepasDraftPembelianLunas(session, pembelian.getId());
										session.delete(pembelian);
									}
									row.setVisible(false);
									row.detach();
								}

							}
						});

			}
		});
	}

	public void loadBarcode(Pembelian pembelian, String barcode) throws Exception {

		if (toko.getSelectedItem() == null || toko.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu diminta untuk memilih Toko / Pedagang terlebih dahulu sebelum melanjutkan proses. Langkah yang dapat dilakukan: (1) buka daftar Toko / Pedagang; (2) pilih salah satu Toko / Pedagang yang sesuai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		if (barcode == null) {
			barcode = this.barcode.getText().trim();
		}

		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Barcode belum diisi. Langkah yang dapat dilakukan: (1) masukkan Barcode pada kolom yang tersedia; (2) pastikan Barcode sesuai dengan produk yang dimaksud; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							PembelianPunyaBarangHelper.this.barcode.focus();
							PembelianPunyaBarangHelper.this.barcode.select();
						}
					});
			return;
		}

		Session session = HibernateUtil.currentSession();
		Produk produk = (Produk) session
				.createCriteria(
						Produk.class)
				.add(Restrictions.or(
						Restrictions.eq("toko",
								toko.getSelectedItem() == null ? null : toko.getSelectedItem().getValue()),
						Restrictions.isNull("toko")))
				.add(Restrictions.ilike("kode", barcode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();

		if (produk == null) {
			MyMessageboxConfig.showFormatCb(
					"Mohon maaf, Barcode {V1} tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali Barcode yang dimasukkan; (2) pastikan produk telah terdaftar pada Toko / Pedagang terkait; (3) ulangi kembali proses ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							PembelianPunyaBarangHelper.this.barcode.focus();
							PembelianPunyaBarangHelper.this.barcode.select();
						}
					}, barcode);
			return;
		}

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		boolean isSudahAda = false;

		for (int i = 0; i < pembelians.size(); i++) {
			Long id_1 = pembelians.get(i).getProduk().getId();
			Long id_2 = produk.getId();
			if (id_1.equals(id_2)) {
				isSudahAda = true;
				pembelian = pembelians.get(i);
				double qty = pembelian.getQty() + 1;
				pembelian.setQty(qty);
				Row r = (Row) rows.getChildren().get(i);
				((MyDoublebox) r.getChildren().get(3)).setValue(pembelian.getQty());
				((Label) r.getChildren().get(4)).setValue("Rp. " + pembelian.getQty() * produk.getHargaJual());
				total = total + produk.getHargaJual();
				pembelians.get(i).setQty(qty);
			}
			if (isSudahAda) {
				break;
			}
		}

		if (!isSudahAda) {
			pembelian.setProduk(produk);
			pembelian.setQty(1.0);
			total = total + produk.getHargaJual();
			pembelians.add(pembelian);

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, pembelian);
		}

		hitungUlangTotal();

		this.barcode.setValue("");
		this.barcode.focus();
		this.barcode.select();
	}

	@SuppressWarnings("unchecked")
	private void hitungUlangTotal() {
		total = 0.0;
		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		List<Row> rows2 = rows.getChildren();
		for (Row r : rows2) {
			Pembelian pembelian = (Pembelian) r.getAttribute("pembelian");
			total += pembelian.getHargaJual();
		}
		totalBelanja.setValue("TOTAL : Rp. " + Common.numberFormat.get().format(total));
	}

	public List<Pembelian> tambahan() {
		List<Pembelian> tambah = new ArrayList<Pembelian>();
		for (Pembelian pembelian : pembelians) {
			if (pembelian.getId() == null && pembelian.getProduk() != null) {
				tambah.add(pembelian);
			}
		}
		return tambah;
	}

	public void simpan(Pembelian pembelianZ) throws Exception {
		for (int i = 0; i < pembelians.size(); i++) {

			Session session = HibernateUtil.currentSession();
			Pembelian pembelian = pembelians.get(i);

			if (pembelian.getSiswa() != null && pembelian.getKode() != null && !pembelian.getKode().trim().isEmpty()) {
				int count = ((Number) HibernateUtil.currentSession().createCriteria(Pembelian.class)
						.add(Restrictions.ne("siswa", pembelian.getSiswa()))
						.add(Restrictions.ilike("kode", pembelian.getKode(), MatchMode.EXACT))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				if (count > 0) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Kode invoice {V1} tidak boleh sama dengan siswa yang lain. Langkah yang dapat dilakukan: (1) periksa kembali Kode invoice yang dimasukkan; (2) gunakan Kode invoice yang berbeda; (3) ulangi kembali proses penyimpanan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, pembelian.getKode());
					return;
				}
			}

			if (pembelian.getMahasiswa() != null && pembelian.getKode() != null
					&& !pembelian.getKode().trim().isEmpty()) {
				int count = ((Number) HibernateUtil.currentSession().createCriteria(Pembelian.class)
						.add(Restrictions.ne("mahasiswa", pembelian.getMahasiswa()))
						.add(Restrictions.ilike("kode", pembelian.getKode(), MatchMode.EXACT))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				if (count > 0) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Kode invoice {V1} tidak boleh sama dengan mahasiswa yang lain. Langkah yang dapat dilakukan: (1) periksa kembali Kode invoice yang dimasukkan; (2) gunakan Kode invoice yang berbeda; (3) ulangi kembali proses penyimpanan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, pembelian.getKode());
					return;
				}
			}

			pembelian.setToko(pembelianZ.getToko());
			pembelian.setWaktu(ais.ui.util.WaktuUtil.getDate());
			session.save(pembelian);
			Common.refreshSaveOrUpdate(session, pembelian);

		}
	}


	private org.zkoss.zul.Html buildInfoHtmlInventoryV1(String judul, String deskripsi) {
		return new org.zkoss.zul.Html("<div style=\"padding:10px 12px;margin:4px 0;border-radius:12px;"
				+ "background:#f8fafc;border:1px solid #e2e8f0;color:#475569;font-size:11.5px;line-height:1.55;\">"
				+ "<b style=\"color:#0f172a;\">" + escapeHtmlInventoryV1(judul) + "</b><br/>"
				+ escapeHtmlInventoryV1(deskripsi) + "</div>");
	}

	private String escapeHtmlInventoryV1(String value) {
		if (value == null) {
			return "";
		}
		String s = value;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}

}
