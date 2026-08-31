package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.file.FileBukuBahanAjar;
import ais.database.model.file.FileBukuBahanAjarText;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPDFTextStripper;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK untuk mengelola koleksi berkas ({@link FileBukuBahanAjar}, disimpan BLOB) milik satu
 * {@link BukuBahanAjar} (buku/bahan ajar referensi mata kuliah). Grid menampilkan pratinjau,
 * nama, format, tipe, tanggal unggah, dan keterangan tambahan yang dapat diedit langsung di
 * baris (bila {@link #delete} aktif). Konstruktor menerima flag {@link #delete} yang mengatur
 * visibilitas seluruh aksi ubah data (unggah, edit keterangan, hapus) — membedakan tampilan
 * pengelola dari tampilan baca-saja.
 *
 * <p>
 * Kelas dalam {@link BukuAjarRunnable} (saat ini tidak dipakai/dipicu di method publik manapun
 * pada file ini — ditandai {@code @SuppressWarnings("unused")}) menyiapkan ekstraksi teks per
 * halaman dari berkas PDF ({@link org.apache.pdfbox.pdmodel.PDDocument} + {@link
 * ais.ui.util.MyPDFTextStripper}) ke {@link FileBukuBahanAjarText}, kemungkinan untuk mendukung
 * pencarian isi buku di fitur lain.
 * </p>
 */
public class FileBukuAjarHelper implements DataLoader {

	private MyGrid grid;
	private BukuBahanAjar bukuBahanAjar;
	private Boolean delete = false;
	private String tabTitle;
	private Tab tab;

	/** @param delete bila {@code true}, aksi unggah/edit/hapus ditampilkan; bila {@code false}, tampilan menjadi baca-saja. */
	public FileBukuAjarHelper(Boolean delete) {
		this.delete = delete;
	}

	/** Renderer baris berkas: pratinjau + nama, keterangan, tipe, tanggal ubah, keterangan tambahan (editable bila {@link #delete}), serta tombol download dan hapus. */
	class DetailBukuBahanAjarRenderer extends ais.ui.util.MyRowRenderer {

		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final FileBukuBahanAjar fileBukuBahanAjar = (FileBukuBahanAjar) data;

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			new Label(fileBukuBahanAjar.getNama()).setParent(vbox);
			vbox.setWidth("100%");
			CommonMedia.preview(fileBukuBahanAjar, vbox);
			new Label(fileBukuBahanAjar.getKeterangan()).setParent(row);
			new Label(fileBukuBahanAjar.getType()).setParent(row);

			new Label(fileBukuBahanAjar.getTanggal_dirubah() == null ? ""
					: Common.dateFormat3.get().format(fileBukuBahanAjar.getTanggal_dirubah())).setParent(row);

			if (delete) {
				final Textbox keteranganTambahan = new Textbox(fileBukuBahanAjar.getKeteranganTambahan());
				keteranganTambahan.setReadonly(!delete);
				keteranganTambahan.setParent(row);
				keteranganTambahan.setWidth("90%");
				keteranganTambahan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();
							fileBukuBahanAjar.setKeteranganTambahan(keteranganTambahan.getValue());
							session.getTransaction().begin();
							Common.refreshUpdate(session, (fileBukuBahanAjar));
							session.getTransaction().commit();
							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
			} else {
				new Label(fileBukuBahanAjar.getKeteranganTambahan()).setParent(row);
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download",
					fileBukuBahanAjar.iconDonwload());

			toolbarbutton.setOrient("vertical");
			toolbarbutton.setParent(toolbar);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					// File file = new File(Sessions
					// .getCurrent()
					// .getWebApp()
					// .getRealPath(
					// "/tmp/" + fileBukuBahanAjar.getId() + "_"
					// + fileBukuBahanAjar.getNama()));

					Session session = StreamingHibernateUtil.getInstance().currentSession();

					FileBukuBahanAjar myfileBukuBahanAjar = (FileBukuBahanAjar) session
							.createCriteria(FileBukuBahanAjar.class).add(Restrictions.idEq(fileBukuBahanAjar.getId()))
							.uniqueResult();

					Filedownload.save(myfileBukuBahanAjar.ambilFile(), myfileBukuBahanAjar.getType());

					StreamingHibernateUtil.getInstance().closeSession();

					// if (file != null && file.exists()) {
					// Filedownload.save(
					// Common.getFileFoto(fileBukuBahanAjar),
					// fileBukuBahanAjar.getType());
					// } else {
					//
					// try {
					//
					// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/FileBukuAjarHelper.java:143");
					// StreamingHibernateUtil.getInstance()
					// .rollbackTransaction();
					// Common.tampilErrorJikaAdmin(e);
					// }
					// }

				}

			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(delete);
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = StreamingHibernateUtil.getInstance().currentSession();

											session.getTransaction().begin();
											Common.refreshDelete((fileBukuBahanAjar));
											session.getTransaction().commit();

											StreamingHibernateUtil.getInstance().closeSession();
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											StreamingHibernateUtil.getInstance().rollbackTransaction();
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
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

	/** Implementasi {@link DataLoader#loadData}: memuat ulang seluruh berkas milik {@link #bukuBahanAjar} dan memperbarui label jumlah pada tab (bila tampilan dibungkus {@link Tabpanel}). */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<FileBukuBahanAjar> fileBukuBahanAjars = session.createCriteria(FileBukuBahanAjar.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("bukuBahanAjar", bukuBahanAjar.getId())).list();

			if (tab != null) {
				tab.setLabel(tabTitle + " (" + fileBukuBahanAjars.size() + " buku)");
			}

			ListModel strset = new SimpleListModel(fileBukuBahanAjars);
			grid.setRowRenderer(new DetailBukuBahanAjarRenderer());
			grid.setModelCheckMobile(strset);

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

	}

	/**
	 * Tugas latar belakang (belum dipakai di file ini — tidak ada pemanggil {@code new
	 * BukuAjarRunnable(...)}) untuk mengekstrak teks tiap halaman PDF ke baris {@link
	 * FileBukuBahanAjarText} lewat {@link ais.ui.util.MyPDFTextStripper}, kemungkinan disiapkan
	 * untuk mendukung pencarian isi buku di fitur lain.
	 */
	@SuppressWarnings("unused")
	private class BukuAjarRunnable implements Runnable {

		private FileBukuBahanAjar fileBukuBahanAjar;
		private File pdfFile;

		public BukuAjarRunnable(FileBukuBahanAjar fileBukuBahanAjar, File pdfFile) {
			this.fileBukuBahanAjar = fileBukuBahanAjar;
			this.pdfFile = pdfFile;
		}

		@SuppressWarnings({ "deprecation" })
		@Override
		public void run() {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			try {

				PDDocument document = PDDocument.load(pdfFile);
				// PDFTextStripper textStripper = new PDFTextStripper();
				// textStripper.getText(textStripper);
				// List<PDPage> pages = document.getDocumentCatalog()
				// .getAllPages();

				MyPDFTextStripper myPDFTextStripper = new MyPDFTextStripper();

				int i = 0;
				for (; i < document.getPageCount(); i++) {
					try {
						String str = myPDFTextStripper.getText(document, i);

						System.out.println("Proses halaman = " + i);
						FileBukuBahanAjarText fileBukuBahanAjarText = new FileBukuBahanAjarText();
						fileBukuBahanAjarText.setFileBukuBahanAjar(fileBukuBahanAjar);
						fileBukuBahanAjarText.setHalaman(i);
						fileBukuBahanAjarText.setIsi(str);
						fileBukuBahanAjarText.setNama(fileBukuBahanAjar.getNama() + " halaman " + i);

						session.getTransaction().begin();
						session.save(fileBukuBahanAjarText);
						session.getTransaction().commit();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}
					// i++;

					// page.get
					// PDFTextStripper textStripper = new PDFTextStripper();

					// textStripper.se
				}
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
			StreamingHibernateUtil.getInstance().closeSession();

		}

	}

	/**
	 * Titik masuk utama: membangun panel daftar berkas bahan ajar untuk {@code bukuBahanAjar} —
	 * toolbar "Tambah File Buku" (unggah, hanya tampil bila {@link #delete} true) diikuti grid
	 * berpaging.
	 *
	 * @param bukuBahanAjar buku/bahan ajar yang berkasnya dikelola
	 * @param component     komponen induk (dibersihkan lebih dulu); bila berupa {@link Tabpanel}, tab terkait dipakai untuk menampilkan jumlah berkas
	 */
	public void display(final BukuBahanAjar bukuBahanAjar, final Component component) {
		this.bukuBahanAjar = bukuBahanAjar;
		if (component instanceof Tabpanel) {
			tab = ((Tabpanel) component).getLinkedTab();
			this.tabTitle = tab.getLabel();
		}

		Common.clear(component);

		MyPanel panel = new MyPanel();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("450px");
		panel.setTitle("Daftar Buku Bahan Ajar");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(panel);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah File Buku" + Common.ukuranLabelFileUpload(),
				"/img/new.gif");
		button.setUpload(Common.ukuranFileUpload());
		button.setVisible(delete);
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;

				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					FileBukuBahanAjar fileBukuBahanAjar = new FileBukuBahanAjar();
					fileBukuBahanAjar.setFoto(Common.getBlobFromMedia(media));
					fileBukuBahanAjar.setKeterangan(media.getFormat());
					fileBukuBahanAjar.setBukuBahanAjar(bukuBahanAjar.getId());
					fileBukuBahanAjar.setType(media.getContentType());
					fileBukuBahanAjar.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
					fileBukuBahanAjar.setOleh(Common.getCurrentUser().getUserId());
					fileBukuBahanAjar.setNama(media.getName());

					session.getTransaction().begin();
					session.save(fileBukuBahanAjar);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();

					loadData(null);

				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("33%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Format");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tipe");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu Upload");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

}
