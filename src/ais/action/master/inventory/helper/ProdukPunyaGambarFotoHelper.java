package ais.action.master.inventory.helper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoGambarProduk;
import ais.database.model.inventory.Produk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI untuk mengelola galeri foto satu {@link Produk} modul inventory (mis. untuk kasir POS
 * mengenali barang dengan cepat), ditampilkan sebagai grid unggah/hapus di dalam layar detail
 * produk. Unggahan gambar dikonversi ke JPEG dan disimpan dua kali: sebagai berkas fisik di direktori
 * konfigurasi {@code lokasi_penyimpanan_lampiran_perpustakaan} (nama berkas dari timestamp + nama
 * asli, di-encode URL) dan sebagai BLOB pada baris {@link FotoGambarProduk} lewat sesi terpisah
 * {@link StreamingHibernateUtil} (dipakai khusus operasi berorientasi BLOB/stream, bukan sesi
 * Hibernate biasa). Gambar pertama yang diunggah pada produk yang sudah tersimpan otomatis dijadikan
 * {@code imagePath} thumbnail produk. Hapus baris menghapus baris {@link FotoGambarProduk} dari
 * database (bila sudah tersimpan) setelah konfirmasi. Visibilitas tombol tambah/hapus mengikuti hak
 * akses pengguna.
 */
public class ProdukPunyaGambarFotoHelper {

	private MyGrid gridPengarang;
	private boolean add = false;
	private boolean delete = false;

	/** Membuat helper terikat ke {@code gridPengarang}, menentukan visibilitas tombol tambah/hapus dari hak akses pengguna saat ini. */
	public ProdukPunyaGambarFotoHelper(MyGrid gridPengarang) {
		this.gridPengarang = gridPengarang;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun tata letak grid galeri foto lengkap dengan toolbar unggah, tiga kolom (gambar, nama,
	 * hapus), dan langsung memuat foto {@code produk} yang sudah ada. Unggahan baru dikonversi ke
	 * JPEG, disimpan sebagai berkas fisik dan baris {@link FotoGambarProduk}; bila produk sudah
	 * tersimpan, foto pertama otomatis dijadikan {@code imagePath} thumbnail produk.
	 *
	 * @param produk produk yang galerinya akan ditampilkan/dikelola
	 * @return tata letak {@link Borderlayout} siap ditempel ke komponen induk
	 */
	public Borderlayout initDetail(final Produk produk) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);
		buildInfoHtmlInventoryV1("Galeri Produk", "Foto produk membantu kasir dan pelanggan mengenali barang dengan lebih cepat. Gambar yang rapi juga mengurangi risiko salah pilih produk saat transaksi POS.").setParent(toolbar);

		Fileupload fileupload = new MyFileUploadConfig("Tambah Gambar Produk", "/img/svg/add.svg");
		fileupload.setVisible(ProdukPunyaGambarFotoHelper.this.add);
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah");

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						File folder = new File(
								Common.getKonfigurasi("lokasi_penyimpanan_lampiran_perpustakaan", "/opt/gambar_perpus")
										.getNilai() + "/");
						if (!folder.exists()) {
							folder.mkdirs();
						}

						File f = new File(folder.getAbsolutePath() + "/"
								+ URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_"
										+ uploadEvent.getMedia().getName(), "UTF-8")
								+ ".jpg");
						boolean exist = f.exists();

						if (!exist) {
							BufferedImage img;
							img = ImageIO.read(uploadEvent.getMedia().getStreamData());

							f.getParentFile().mkdirs();
							if (!ImageIO.write(img, "JPEG", f)) {

							}
						}

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoGambarProduk fotoGambarProduk = new FotoGambarProduk();
						fotoGambarProduk.setNama(uploadEvent.getMedia().getName());
						fotoGambarProduk.setKeterangan(uploadEvent.getMedia().getContentType());
						fotoGambarProduk.setProduk(
								produk.getId() == null ? new Random(Long.MIN_VALUE).nextLong() : produk.getId());

						Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(f)));
						fotoGambarProduk.setFoto(blob);
						fotoGambarProduk.setPath(f.getAbsolutePath());

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoGambarProduk);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						if (produk.getId() != null && fotoGambarProduk.getPath() != null
								&& !fotoGambarProduk.getPath().trim().isEmpty()
								&& new File(fotoGambarProduk.getPath()).exists()) {
							Session session = HibernateUtil.currentSession();
							session.refresh(produk);
							produk.setImagePath(fotoGambarProduk.getPath());
							Common.refreshUpdate(session, produk);
						}

						Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
						rows.setParent(gridPengarang);
						Row row = new Row();
						row.setValign("top");
						row.setParent(rows);
						initRow(row, fotoGambarProduk);
					}
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				}

			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridPengarang);
		gridPengarang.setParent(center);
		gridPengarang.setWidth("100%");
		gridPengarang.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridPengarang);

		MyColumnConfig column = new MyColumnConfig("Gambar");
		column.setParent(columns);

		column = new MyColumnConfig("Nama");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(produk);

		return borderlayout;
	}

	/** Memuat seluruh foto tersimpan milik {@code produk} (diurutkan id terbaru dulu) ke grid, atau tidak menambah baris apa pun bila produk belum tersimpan. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Produk produk) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoGambarProduk> fotoGambarProduks = produk == null || produk.getId() == null
				? new ArrayList<FotoGambarProduk>()
				: session.createCriteria(FotoGambarProduk.class).add(Restrictions.eq("produk", produk.getId()))
						.addOrder(Order.desc("id")).list();

		Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
		rows.setParent(gridPengarang);

		for (FotoGambarProduk fotoGambarProduk : fotoGambarProduks) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoGambarProduk);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	/** Mengisi satu baris grid dengan thumbnail (256x256, lewat {@link CommonMedia#getUrlFotoProduk}), nama berkas, dan tombol hapus (dengan konfirmasi) yang, saat disetujui, menghapus baris dari database bila sudah tersimpan lalu menyembunyikan/melepas baris dari grid. */
	public void initRow(final Row row, final FotoGambarProduk fotoGambarProduk) throws Exception {
		row.setValign("top");
		row.setAttribute("fotoGambarProduk", fotoGambarProduk);
		Image image = new Image(
				CommonMedia.getUrlFotoProduk(fotoGambarProduk.getId(), fotoGambarProduk.getProduk(), 256, 256));
		image.setWidth("100%");
		image.setParent(row);

		new Label(fotoGambarProduk.getNama()).setParent(row);

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
									if (fotoGambarProduk.getId() != null) {
										Session session = StreamingHibernateUtil.getInstance().currentSession();
										session.getTransaction().begin();
										session.delete(fotoGambarProduk);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();
									}
									row.setVisible(false);
									row.detach();
								}

							}
						});

			}
		});
	}


	/** Membangun kotak info HTML kecil (judul + deskripsi) yang ditampilkan di toolbar galeri, dengan konten di-escape lewat {@link #escapeHtmlInventoryV1(String)}. */
	private org.zkoss.zul.Html buildInfoHtmlInventoryV1(String judul, String deskripsi) {
		return new org.zkoss.zul.Html("<div style=\"padding:10px 12px;margin:4px 0;border-radius:12px;"
				+ "background:#f8fafc;border:1px solid #e2e8f0;color:#475569;font-size:11.5px;line-height:1.55;\">"
				+ "<b style=\"color:#0f172a;\">" + escapeHtmlInventoryV1(judul) + "</b><br/>"
				+ escapeHtmlInventoryV1(deskripsi) + "</div>");
	}

	/** Melakukan escape karakter HTML dasar ({@code & < > " '}) pada {@code value} agar aman disisipkan ke markup {@link org.zkoss.zul.Html}; mengembalikan string kosong bila {@code value} {@code null}. */
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
