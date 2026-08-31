package ais.action.master.library.helper;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
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
import ais.database.model.file.FotoGambarItem;
import ais.database.model.library.Item;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI untuk mengelola galeri gambar/cover satu {@link Item} perpustakaan, ditampilkan
 * sebagai grid unggah/hapus di dalam layar detail item. Dibanding helper galeri sejenis (mis.
 * {@link ais.action.master.inventory.helper.ProdukPunyaGambarFotoHelper}), kelas ini punya
 * penanganan unggah yang lebih ketat: gambar dibaca aman lewat
 * {@code CommonFileMediaHelper#bacaGambarAman} (menolak format tak dikenal seperti WEBP dengan
 * pesan ramah, bukan error sistem — lihat {@link #handleUpload}), dikonversi ke RGB murni sebelum
 * ditulis sebagai JPEG (menghindari masalah channel alpha), dan pembaruan cover item
 * ({@link #updateCoverItem}) memeriksa ulang keberadaan baris {@link Item} di database lebih dulu
 * untuk menghindari {@code UnresolvableObjectException} akibat race kondisi hapus-vs-upload oleh
 * pengguna lain. Data BLOB disimpan lewat sesi {@link StreamingHibernateUtil} terpisah.
 */
public class ItemPunyaGambarFotoHelper {

	private MyGrid gridPengarang;
	private boolean add = false;
	private boolean delete = false;

	/** Membuat helper terikat ke {@code gridPengarang}, menentukan visibilitas tombol tambah/hapus dari hak akses pengguna saat ini. */
	public ItemPunyaGambarFotoHelper(MyGrid gridPengarang) {
		this.gridPengarang = gridPengarang;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun tata letak grid galeri gambar lengkap dengan toolbar unggah, tiga kolom (gambar,
	 * nama, hapus), dan langsung memuat gambar {@code item} yang sudah ada.
	 *
	 * @param item item perpustakaan yang galerinya akan ditampilkan/dikelola
	 * @return tata letak {@link Borderlayout} siap ditempel ke komponen induk
	 */
	public Borderlayout initDetail(final Item item) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah Gambar / Cover", "/img/new.gif");
		fileupload.setVisible(ItemPunyaGambarFotoHelper.this.add);
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah gambar/cover item. Format yang disarankan: JPG, JPEG, PNG, WEBP, BMP, GIF.");

		fileupload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				handleUpload(event, item);
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridPengarang);
		gridPengarang.setParent(center);
		gridPengarang.setWidth("100%");
		gridPengarang.setHeight("100%");
		gridPengarang.setStyle("border:0; overflow:auto;");

		Columns columns = new Columns();
		columns.setParent(gridPengarang);

		MyColumnConfig column = new MyColumnConfig("Gambar");
		column.setParent(columns);

		column = new MyColumnConfig("Nama");
		column.setParent(columns);
		column.setWidth("28%");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("90px");

		loadDataDetail(item);

		return borderlayout;
	}

	/**
	 * Menangani event unggah gambar: memvalidasi event dan berkas media, menulis berkas fisik JPEG
	 * lewat {@link #createImageFile}, menyimpan baris {@link FotoGambarItem} (BLOB) via
	 * {@link StreamingHibernateUtil}, memperbarui cover item bila item sudah tersimpan
	 * ({@link #updateCoverItem}), lalu menambahkan baris baru ke grid. Kesalahan format gambar tak
	 * didukung ({@link IllegalArgumentException}, mis. WEBP) ditangani sebagai kesalahan input
	 * pengguna biasa (pesan ramah, TIDAK dicatat ke audit error admin agar log tidak dipenuhi
	 * kejadian normal); kegagalan lain ditangani sebagai error sistem dengan pesan generik.
	 */
	private void handleUpload(Event event, Item item) {
		UploadEvent uploadEvent = null;
		Media media = null;
		File imageFile = null;
		FotoGambarItem fotoGambarItem = null;
		Session streamingSession = null;
		Transaction tx = null;
		try {
			if (!(event instanceof UploadEvent)) {
				showWarning("Upload gambar tidak dapat diproses karena event upload tidak valid.");
				return;
			}
			uploadEvent = (UploadEvent) event;
			media = uploadEvent.getMedia();
			if (media == null) {
				showWarning("File gambar tidak ditemukan. Silakan pilih ulang file gambar/cover.");
				return;
			}

			imageFile = createImageFile(media);
			if (imageFile == null || !imageFile.exists() || imageFile.length() <= 0) {
				showWarning("Gambar gagal disimpan ke folder lampiran perpustakaan. Pastikan folder penyimpanan dapat ditulis oleh server.");
				return;
			}

			streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			tx = streamingSession.beginTransaction();

			fotoGambarItem = new FotoGambarItem();
			fotoGambarItem.setNama(defaultString(media.getName(), imageFile.getName()));
			fotoGambarItem.setKeterangan(defaultString(media.getContentType(), "image/jpeg"));
			fotoGambarItem.setItem(item == null || item.getId() == null ? new Random(Long.MIN_VALUE).nextLong() : item.getId());
			fotoGambarItem.setPath(imageFile.getAbsolutePath());
			fotoGambarItem.setLokasiSimpan(imageFile.getAbsolutePath());

			FileInputStream fotoStream = null;
			try {
				fotoStream = new FileInputStream(imageFile);
				Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(fotoStream));
				fotoGambarItem.setFoto(blob);
			} finally {
				IOUtils.closeQuietly(fotoStream);
			}

			streamingSession.save(fotoGambarItem);
			tx.commit();

			updateCoverItem(item, fotoGambarItem);
			appendRow(fotoGambarItem);
			MyMessageboxConfig.show("Gambar/cover berhasil di-upload.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		} catch (IllegalArgumentException iae) {
			// Format gambar tidak dikenali/tidak didukung (mis. .webp yang tidak bisa dibaca ImageIO
			// bawaan Java). Ini kesalahan INPUT pengguna, bukan error sistem — tampilkan pesan yang
			// jelas dan JANGAN dicatat sebagai error admin agar log tidak dipenuhi kejadian normal.
			try {
				if (tx != null) {
					tx.rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:174");
			}
			showWarning("Format gambar tidak didukung. Silakan unggah cover dalam format JPG, JPEG, atau PNG "
					+ "(format seperti .webp belum didukung — silakan konversi dulu ke JPG/PNG).");
		} catch (Exception e) {
			try {
				if (tx != null) {
					tx.rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:183");
			}
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:187");
			}
			showWarning("Gambar gagal di-upload. Pastikan file adalah gambar yang valid dan ukuran file tidak terlalu besar.");
		} finally {
			try {
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:193");
			}
		}
	}

	/**
	 * Menulis berkas gambar unggahan sebagai JPEG RGB murni ke folder konfigurasi
	 * {@code lokasi_penyimpanan_lampiran_perpustakaan} (nama berkas dari timestamp + nama asli yang
	 * disanitasi, di-encode URL). Melempar {@link IllegalArgumentException} bila berkas bukan
	 * gambar yang dapat dibaca ({@link #readImage}), atau {@link IllegalStateException} bila folder
	 * tujuan tidak dapat ditulis atau penulisan JPEG gagal.
	 */
	private File createImageFile(Media media) throws Exception {
		File folder = getUploadFolder();
		if (!folder.exists()) {
			folder.mkdirs();
		}
		if (!folder.exists() || !folder.isDirectory() || !folder.canWrite()) {
			throw new IllegalStateException("Folder lampiran perpustakaan tidak dapat ditulis: " + folder.getAbsolutePath());
		}

		String fileName = sanitizeFileName(media.getName());
		File file = new File(folder, URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + fileName,
				"UTF-8") + ".jpg");

		BufferedImage image = readImage(media);
		if (image == null) {
			throw new IllegalArgumentException("File yang di-upload bukan gambar valid: " + fileName);
		}

		BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = rgbImage.createGraphics();
		try {
			graphics.drawImage(image, 0, 0, null);
		} finally {
			graphics.dispose();
		}

		if (!ImageIO.write(rgbImage, "JPEG", file)) {
			throw new IllegalStateException("ImageIO tidak dapat menulis gambar sebagai JPEG.");
		}
		return file;
	}

	/** Membaca {@code media} sebagai {@link BufferedImage} secara aman lewat {@code CommonFileMediaHelper#bacaGambarAman}, mencoba lewat stream lebih dulu lalu jatuh ke byte array bila stream gagal; mengembalikan {@code null} bila kedua cara gagal (bukan gambar valid/format tak didukung). */
	private BufferedImage readImage(Media media) throws Exception {
		InputStream inputStream = null;
		try {
			try {
				inputStream = media.getStreamData();
			} catch (Exception e) {
				inputStream = null;
			}
			if (inputStream != null) {
				BufferedImage image = ais.common.CommonFileMediaHelper.bacaGambarAman(inputStream);
				if (image != null) {
					return image;
				}
			}
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		try {
			byte[] bytes = media.getByteData();
			if (bytes != null && bytes.length > 0) {
				return ais.common.CommonFileMediaHelper.bacaGambarAman(bytes);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:252");
		}
		return null;
	}

	/** Mengembalikan folder penyimpanan gambar dari konfigurasi {@code lokasi_penyimpanan_lampiran_perpustakaan}, default {@code /opt/gambar_perpus} bila kosong/gagal dibaca. */
	private File getUploadFolder() {
		String base = "/opt/gambar_perpus";
		try {
			base = Common.getKonfigurasi("lokasi_penyimpanan_lampiran_perpustakaan", base).getNilai();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:261");
		}
		if (base == null || base.trim().length() == 0) {
			base = "/opt/gambar_perpus";
		}
		return new File(base.trim());
	}

	/** Menetralkan karakter yang tidak aman untuk nama berkas (path separator dan karakter terlarang Windows/Unix) dan memotong panjang nama ke maksimum 120 karakter. */
	private String sanitizeFileName(String value) {
		String name = defaultString(value, "cover_item");
		name = name.replace('\\', '_').replace('/', '_').replace(':', '_').replace('*', '_').replace('?', '_')
				.replace('"', '_').replace('<', '_').replace('>', '_').replace('|', '_');
		return name.length() > 120 ? name.substring(0, 120) : name;
	}

	/** Mengembalikan {@code value} yang sudah di-trim, atau {@code fallback} bila {@code value} {@code null}/kosong setelah di-trim. */
	private String defaultString(String value, String fallback) {
		return value == null || value.trim().length() == 0 ? fallback : value.trim();
	}

	/**
	 * Menjadikan {@code fotoGambarItem} sebagai cover ({@code imagePath}) item, bila item sudah
	 * tersimpan dan berkas gambarnya ada. Memeriksa ulang keberadaan baris {@link Item} lewat
	 * {@code session.get} sebelum {@code refresh} — mencegah {@code UnresolvableObjectException}
	 * bila item sudah dihapus pengguna lain sejak halaman ini dimuat (kasus tersebut dilewati
	 * secara halus, tanpa error, lihat komentar kode).
	 */
	private void updateCoverItem(Item item, FotoGambarItem fotoGambarItem) {
		if (item == null || item.getId() == null || fotoGambarItem == null || fotoGambarItem.getPath() == null
				|| fotoGambarItem.getPath().trim().length() == 0 || !new File(fotoGambarItem.getPath()).exists()) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			// KE-FIX (race hapus-vs-upload cover, UnresolvableObjectException): "item" bisa berupa
			// referensi Hibernate yang sudah usang (mis. dari render halaman sebelumnya) yang
			// barisnya sudah dihapus oleh pengguna lain sebelum upload cover ini selesai diproses.
			// session.refresh(item) langsung pada Item yang sudah tidak ada di DB akan melempar
			// UnresolvableObjectException. Cek dulu keberadaannya via session.get() dan lewati update
			// cover secara halus bila item sudah tidak ada, alih-alih bergantung pada refresh buta +
			// exception generik.
			Item itemTerkini = (Item) session.get(Item.class, item.getId());
			if (itemTerkini == null) {
				// Item sudah dihapus oleh pengguna lain sejak halaman ini dimuat — lewati update cover.
				return;
			}
			session.refresh(itemTerkini);
			itemTerkini.setImagePath(fotoGambarItem.getPath());
			Common.refreshUpdate(session, itemTerkini);
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:293");
			}
		}
	}

	/** Menambahkan satu baris grid baru untuk {@code fotoGambarItem} yang baru diunggah; tidak melakukan apa pun bila grid atau data belum siap. */
	private void appendRow(FotoGambarItem fotoGambarItem) throws Exception {
		if (gridPengarang == null || fotoGambarItem == null) {
			return;
		}
		Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
		rows.setParent(gridPengarang);
		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		initRow(row, fotoGambarItem);
	}

	/** Menampilkan {@code message} sebagai kotak peringatan sederhana ke pengguna. */
	private void showWarning(String message) {
		try {
			MyMessageboxConfig.show(message, "Peringatan Upload Gambar", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:314");
		}
	}

	/** Memuat seluruh gambar tersimpan milik {@code item} (diurutkan id terbaru dulu) ke grid, atau tidak menambah baris apa pun bila item belum tersimpan. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) throws Exception {
		Session session = null;
		try {
			session = StreamingHibernateUtil.getInstance().currentSession();
			List<FotoGambarItem> fotoGambarItems = item == null || item.getId() == null ? new ArrayList<FotoGambarItem>()
					: session.createCriteria(FotoGambarItem.class).add(Restrictions.eq("item", item.getId()))
							.addOrder(Order.desc("id")).list();

			Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
			rows.setParent(gridPengarang);

			for (FotoGambarItem fotoGambarItem : fotoGambarItems) {
				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				initRow(row, fotoGambarItem);
			}
		} finally {
			try {
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:339");
			}
		}
	}

	/** Mengisi satu baris grid dengan thumbnail (919x575, lewat {@link CommonMedia#getUrlFotoItem}), nama berkas, dan tombol hapus (dengan konfirmasi, mendelegasikan ke {@link #deleteFoto}). */
	public void initRow(final Row row, final FotoGambarItem fotoGambarItem) throws Exception {
		row.setValign("top");
		row.setAttribute("fotoGambarItem", fotoGambarItem);
		Image image = new Image(CommonMedia.getUrlFotoItem(fotoGambarItem.getId(), fotoGambarItem.getItem(), 919, 575));
		image.setWidth("100%");
		image.setParent(row);

		new Label(fotoGambarItem.getNama()).setParent(row);

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
									deleteFoto(row, fotoGambarItem);
								}
							}
						});
			}
		});
	}

	/** Menghapus baris {@code fotoGambarItem} dari database (bila sudah tersimpan) dalam transaksi eksplisit, lalu melepas {@code row} dari grid. Kegagalan hapus (mis. masih dipakai relasi lain) di-rollback dan ditampilkan sebagai peringatan. */
	private void deleteFoto(Row row, FotoGambarItem fotoGambarItem) {
		Session session = null;
		Transaction tx = null;
		try {
			if (fotoGambarItem != null && fotoGambarItem.getId() != null) {
				session = StreamingHibernateUtil.getInstance().currentSession();
				tx = session.beginTransaction();
				session.delete(fotoGambarItem);
				tx.commit();
			}
			if (row != null) {
				row.setVisible(false);
				row.detach();
			}
		} catch (Exception e) {
			try {
				if (tx != null) {
					tx.rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:398");
			}
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:402");
			}
			showWarning("Gambar gagal dihapus.");
		} finally {
			try {
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/ItemPunyaGambarFotoHelper.java:408");
			}
		}
	}
}
