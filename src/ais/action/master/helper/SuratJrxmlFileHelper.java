package ais.action.master.helper;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
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
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.FormatTemplateSurat;
import ais.database.model.file.SuratJrxmlFile;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper composer untuk mengelola berkas template surat mentah ({@link SuratJrxmlFile} — biasanya
 * berkas {@code .jrxml} JasperReports, walau kelas ini menyimpan berkas apa pun sebagai blob) yang
 * terkait dengan satu {@link FormatTemplateSurat}. Menyediakan unggah berkas baru, daftar
 * berpaging dengan unduh per baris, dan (opsional, dikendalikan flag {@link #delete}) hapus baris.
 *
 * <p>
 * Bila {@code component} yang diberikan ke {@link #display} adalah sebuah {@link Tabpanel}, label
 * tab yang menautkannya diperbarui otomatis di {@link #loadData} untuk menampilkan jumlah berkas
 * (mis. {@code "Format Surat (3 format)"}).
 * </p>
 *
 * <p>
 * Seluruh akses database memakai sesi streaming ({@link StreamingHibernateUtil}), bukan sesi
 * Hibernate biasa — cocok untuk operasi baca/tulis blob berkas berukuran besar.
 * </p>
 */
public class SuratJrxmlFileHelper implements DataLoader {

	private MyGrid grid;
	private FormatTemplateSurat formatTemplateSurat;
	private Boolean delete = false;
	private String tabTitle;
	private Tab tab;

	/** @param delete bila {@code true}, tombol hapus per baris dan tombol tambah berkas ditampilkan; bila {@code false}, tampilan hanya baca (unduh saja). */
	public SuratJrxmlFileHelper(Boolean delete) {
		this.delete = delete;
	}

	/** Perender baris grid: nama/format/tipe/tanggal ubah berkas, tombol unduh (ambil dari cache tmp bila tersedia, jika tidak query ulang lewat sesi streaming), dan tombol hapus (tampil hanya bila {@link #delete} true, dengan konfirmasi). */
	class DetailFormatTemplateSuratRenderer extends ais.ui.util.MyRowRenderer {

		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final SuratJrxmlFile suratJrxmlFile = (SuratJrxmlFile) data;

			new Label(suratJrxmlFile.getNama()).setParent(row);
			new Label(suratJrxmlFile.getKeterangan()).setParent(row);
			new Label(suratJrxmlFile.getType()).setParent(row);

			new Label(suratJrxmlFile.getTanggal_dirubah() == null ? ""
					: Common.dateFormat3.get().format(suratJrxmlFile.getTanggal_dirubah())).setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", suratJrxmlFile.iconDonwload());

			toolbarbutton.setOrient("vertical");
			toolbarbutton.setParent(toolbar);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					File file = new File(Sessions.getCurrent().getWebApp()
							.getRealPath("/tmp/" + suratJrxmlFile.getId() + "_" + suratJrxmlFile.getNama()));

					if (file != null && file.exists()) {
						Filedownload.save(CommonMedia.getFileFotoLangsungOld(suratJrxmlFile, false),
								suratJrxmlFile.getType());
					} else {

						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();

							SuratJrxmlFile mysuratJrxmlFile = (SuratJrxmlFile) session
									.createCriteria(SuratJrxmlFile.class).add(Restrictions.idEq(suratJrxmlFile.getId()))
									.uniqueResult();

							Filedownload.save(CommonMedia.getFileFotoLangsungOld(mysuratJrxmlFile, false),
									mysuratJrxmlFile.getType());

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException(
									"mengunduh berkas format surat ini",
									e,
									new String[] {
											"Periksa apakah berkas format surat ini masih tersedia dan belum dihapus dari sistem.",
											"Muat ulang halaman kemudian coba unduh kembali berkas tersebut.",
											"Jika berkas tetap tidak dapat diunduh, laporkan kepada Administrator disertai tangkapan layar (screenshot)." });
						}
					}

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
											Common.refreshDelete((suratJrxmlFile));
											session.getTransaction().commit();

											StreamingHibernateUtil.getInstance().closeSession();
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											StreamingHibernateUtil.getInstance().rollbackTransaction();
											PesanFormalHelper.tampilkanGagalException(
													"menghapus berkas format surat ini",
													e,
													new String[] {
															"Periksa apakah berkas format surat ini masih berelasi dengan data lain (misalnya template surat yang sedang digunakan) sehingga tidak dapat dihapus.",
															"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
															"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}

	/**
	 * Memuat ulang grid dengan seluruh {@link SuratJrxmlFile} milik {@link #formatTemplateSurat}
	 * yang sedang ditampilkan (terurut id), dan — bila tampilan berada di dalam sebuah
	 * {@link Tabpanel} — memperbarui label tab dengan jumlah berkas terkini. Kontrak
	 * {@link DataLoader#loadData(Object)}; {@code value} tidak dipakai.
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<SuratJrxmlFile> suratJrxmlFiles = session.createCriteria(SuratJrxmlFile.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("formatTemplateSurat", formatTemplateSurat.getId()))
					.list();

			if (tab != null) {
				tab.setLabel(tabTitle + " (" + suratJrxmlFiles.size() + " format)");
			}

			ListModel strset = new SimpleListModel(suratJrxmlFiles);
			grid.setRowRenderer(new DetailFormatTemplateSuratRenderer());
			grid.setModelCheckMobile(strset);

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

	}

	/**
	 * Membangun panel pengelolaan berkas format surat (toolbar unggah + grid berpaging) ke dalam
	 * {@code component}, untuk {@code formatTemplateSurat} yang diberikan. Bila {@code component}
	 * adalah {@link Tabpanel}, tab yang menautkannya diingat untuk pembaruan label jumlah berkas
	 * di {@link #loadData}. Memanggil {@link #loadData} di akhir untuk mengisi grid.
	 */
	public void display(final FormatTemplateSurat formatTemplateSurat, final Component component) {
		this.formatTemplateSurat = formatTemplateSurat;
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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Format Surat" + Common.ukuranLabelFileUpload(),
				"/img/new.gif");
		button.setUpload(Common.ukuranFileUpload());
		button.setVisible(delete);
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				// FIX (broken access control): tombol "Tambah Format Surat" hanya disembunyikan di UI
				// (button.setVisible(delete) di FormatTemplateSuratHelper/SuratJrxmlFileHelper) -- pola
				// ini TIDAK mencegah event onUpload dipicu langsung lewat request AU ZK ke komponen yang
				// masih ada di desktop meski tersembunyi (pola bypass tombol ZK yang sama seperti
				// temuan-temuan sebelumnya di proyek ini). Handler ini sekarang menggerbangi ulang hak
				// akses secara fail-closed, bukan hanya mengandalkan visibilitas tombol.
				if (!(Common.getCurrentUser().getMahasiswa() == null
						&& CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE))) {
					MyMessageboxConfig.show(
							"Mohon maaf, Anda tidak memiliki hak akses untuk mengunggah berkas format surat ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				UploadEvent uploadEvent = (UploadEvent) event;

				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				// FIX (RCE): berkas .jrxml/.jasper yang diunggah di sini dikompilasi & dieksekusi APA
				// ADANYA oleh JasperCompileManager saat laporan dicetak (lihat
				// ais.action.servlet.AmbilLaporanMahasiswa#laporanSurat) -- JRXML JasperReports mengizinkan
				// ekspresi Java arbitrer (queryString/field/variable/scriptlet) yang dikompilasi & dimuat
				// oleh JVM, jadi berkas berbahaya di sini setara RCE penuh di server. Blacklist ini adalah
				// lapisan pertahanan tambahan (bukan sandbox penuh) yang menolak konstruksi Java yang umum
				// dipakai untuk eksekusi proses/reflection/deserialisasi/class-loading dinamis.
				byte[] isiBerkas = null;
				String namaBerkasLower = media.getName() == null ? "" : media.getName().trim().toLowerCase();
				if (namaBerkasLower.endsWith(".jrxml") || namaBerkasLower.endsWith(".jasper")) {
					isiBerkas = bacaSemuaByteMedia(media);
					if (mengandungKonstruksiJavaBerbahaya(isiBerkas)) {
						MyMessageboxConfig.show(
								"Mohon maaf, berkas format surat ini ditolak karena mengandung konstruksi yang tidak "
										+ "diizinkan (mis. pemanggilan proses sistem, reflection, class loading dinamis, "
										+ "atau deserialisasi objek) yang berpotensi disalahgunakan saat berkas ini "
										+ "dikompilasi/dijalankan oleh mesin laporan.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}
				}

				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					SuratJrxmlFile suratJrxmlFile = new SuratJrxmlFile();
					suratJrxmlFile.setFoto(
							isiBerkas != null ? org.hibernate.Hibernate.createBlob(isiBerkas) : Common.getBlobFromMedia(media));
					suratJrxmlFile.setKeterangan(media.getFormat());
					suratJrxmlFile.setFormatTemplateSurat(formatTemplateSurat.getId());
					suratJrxmlFile.setType(media.getContentType());
					suratJrxmlFile.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
					suratJrxmlFile.setOleh(Common.getCurrentUser().getUserId());
					suratJrxmlFile.setNama(media.getName());

					session.getTransaction().begin();
					session.save(suratJrxmlFile);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();

					loadData(null);
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"mengunggah berkas format surat baru",
							e,
							new String[] {
									"Periksa kembali format dan ukuran berkas yang diunggah, pastikan sesuai dengan ketentuan sistem.",
									"Coba ulangi proses unggah berkas.",
									"Jika unggah tetap gagal, laporkan kepada Administrator atau Pengembang disertai tangkapan layar (screenshot)." });
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
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

	/**
	 * Membaca seluruh isi {@link Media} upload menjadi {@code byte[]}, dicoba lewat stream lebih
	 * dulu (hemat memori), lalu fallback ke data string/byte -- meniru urutan percobaan
	 * {@link Common#getBlobFromMedia(Media)} agar isi berkas bisa dipindai (deteksi konstruksi
	 * berbahaya) sebelum sekali lagi dipakai untuk membuat {@link java.sql.Blob} yang disimpan.
	 */
	private static byte[] bacaSemuaByteMedia(Media media) throws Exception {
		try {
			java.io.InputStream in = media.getStreamData();
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int n;
			while ((n = in.read(buffer)) != -1) {
				out.write(buffer, 0, n);
			}
			return out.toByteArray();
		} catch (Exception e) {
			try {
				return media.getStringData().getBytes();
			} catch (Exception ee) {
				return media.getByteData();
			}
		}
	}

	/** Token (huruf kecil) yang menandakan kemungkinan eksekusi proses sistem, reflection, class loading
	 * dinamis, deserialisasi objek, atau scriptlet kustom di dalam berkas JRXML/JASPER -- konstruksi yang
	 * lazim dipakai untuk RCE saat berkas dikompilasi/dijalankan oleh JasperReports. Blacklist berbasis
	 * substring, bukan parser JRXML penuh, jadi bersifat lapisan pertahanan tambahan (defense-in-depth),
	 * bukan jaminan sandbox mutlak. */
	private static final String[] TOKEN_KONSTRUKSI_BERBAHAYA = { "runtime", "processbuilder", "class.forname",
			"classloader", "getdeclaredmethod", ".invoke(", "javax.script", "scriptengine", "objectinputstream",
			"readobject", "<scriptlet", "java.lang.reflect", "javax.naming", "system.load", "system.getenv",
			"urlclassloader", "ldap://", "rmi://" };

	private static boolean mengandungKonstruksiJavaBerbahaya(byte[] isiBerkas) {
		if (isiBerkas == null || isiBerkas.length == 0) {
			return false;
		}
		String isi;
		try {
			// ISO-8859-1: pemetaan 1 byte -> 1 char tanpa gagal decode, aman dipakai untuk berkas
			// biner (.jasper) maupun teks (.jrxml) sekadar untuk pencocokan substring ASCII.
			isi = new String(isiBerkas, "ISO-8859-1").toLowerCase();
		} catch (Exception e) {
			return false;
		}
		for (String token : TOKEN_KONSTRUKSI_BERBAHAYA) {
			if (isi.contains(token)) {
				return true;
			}
		}
		return false;
	}

}
