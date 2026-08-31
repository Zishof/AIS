package ais.action.master.helper.generic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.dropbox.UploadDropboxUtil;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoCalonPegawai;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGambarProduk;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranBeasiswaMahasiswa;
import ais.database.model.file.LampiranKknMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.database.model.file.LampiranPklMahasiswa;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeter;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data lampiran file lain. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code int MAX_ENCODED_BASE_EXT_BYTES}, {@code
 * MyGrid grid}, {@code EventListener eventListener}, {@code MyTextbox nama}, {@code MyTextbox myjenis}, {@code
 * String jenis}, {@code Integer cutomUkuranUpload}, {@code String keterangan}; inisialisasi/lifecycle ({@code
 * initCriteria()}); pembacaan/pencarian ({@code ambilFile()}, {@code tampilkanTombolUpload()}, {@code
 * tampilkanTombolUploadGDrive()}, {@code tampilkanTombolUploadDropbox()}, {@code onSearchDefault()}, {@code
 * setEventListener()}); validasi/perhitungan ({@code bolehDriveAtauLink()}); mutasi data ({@code simpanData()});
 * operasi domain lain ({@code namaFileAmanUntukFilesystem()}, {@code createScanFoto()}, {@code
 * createScanLayar()}, {@code mappingInstanceData()}, {@code createRekamAudio()}, {@code display()}); konfigurasi
 * constructor: {@code tbmuser}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataLampiranFileLain extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;

	/**
	 * KE-FIX (UiException/IOException "File name too long"): nama file asli upload
	 * (mis. unicode panjang) tanpa batas ditambah timestamp+URLEncoder.encode() bisa
	 * melebihi batas panjang nama file filesystem. Potong bagian nama (bukan
	 * ekstensi) agar aman, sebelum di-encode & dipakai membangun File tujuan.
	 *
	 * CATATAN (perbaikan lanjutan): pemotongan HANYA berdasar jumlah karakter
	 * (150) tidak cukup -- caller membungkus hasil fungsi ini dengan
	 * URLEncoder.encode(..., "UTF-8") sebelum dipakai sebagai nama file fisik di
	 * ext4 (batas 255 BYTE). Karakter non-ASCII (multibyte UTF-8) dan bahkan
	 * spasi/simbol biasa membengkak jadi "%XX" (3 byte ASCII) per byte aslinya
	 * saat di-encode, jadi 150 karakter unicode bisa menghasilkan nama fisik
	 * >255 byte walau lolos batas karakter. Di sini kita potong berdasarkan
	 * PANJANG HASIL ENCODE, bukan panjang karakter mentah, dan juga membuang
	 * karakter berbahaya untuk path lebih dulu.
	 */
	private static final int MAX_ENCODED_BASE_EXT_BYTES = 200;

	private static String namaFileAmanUntukFilesystem(String namaAsli) {
		if (namaAsli == null) {
			return "file";
		}
		// Buang karakter yang berbahaya untuk path (separator, karakter kontrol)
		// sebelum diproses lebih lanjut -- jaga-jaga bila belum tersaring di hulu.
		String bersih = namaAsli.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_");

		int dot = bersih.lastIndexOf('.');
		String ext = dot >= 0 ? bersih.substring(dot) : "";
		String base = dot >= 0 ? bersih.substring(0, dot) : bersih;

		// Batas kasar berbasis karakter dulu (menghindari loop pemangkasan yang
		// terlalu panjang untuk nama yang memang sudah sangat panjang).
		if (base.length() > 150) {
			base = base.substring(0, 150);
		}

		try {
			String extEncoded = URLEncoder.encode(ext, "UTF-8");
			int budget = MAX_ENCODED_BASE_EXT_BYTES - extEncoded.length();
			if (budget < 1) {
				budget = 1;
			}
			while (base.length() > 0) {
				String baseEncoded = URLEncoder.encode(base, "UTF-8");
				if (baseEncoded.length() <= budget) {
					break;
				}
				base = base.substring(0, base.length() - 1);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:namaFileAmanUntukFilesystem");
			// Encoding UTF-8 semestinya selalu tersedia; sebagai jaga-jaga saja
			// potong sangat konservatif berbasis karakter.
			if (base.length() > 50) {
				base = base.substring(0, 50);
			}
		}

		return base + ext;
	}

	private MyGrid grid;
	private EventListener eventListener;

	private MyTextbox nama;
	private MyTextbox myjenis;
	private String jenis;
	private Integer cutomUkuranUpload;
	private String keterangan;
	private Combobox jurusan;
	private Boolean harusPdf;
	private Serializable ref;
	private Boolean usingId;
	private Tbmuser tbmuser;
	// private Paging paging;
	@SuppressWarnings("rawtypes")
	private Class clazz;

	@SuppressWarnings("rawtypes")
	public AmbilDataLampiranFileLain(String jenis, Integer cutomUkuranUpload, String keterangan, Combobox jurusan,
			Boolean harusPdf, Serializable ref, Boolean usingId, Class clazz, boolean display) {
		super();
		this.jenis = jenis;
		this.cutomUkuranUpload = cutomUkuranUpload;
		this.keterangan = keterangan;
		this.jurusan = jurusan;
		this.harusPdf = harusPdf;
		this.ref = ref;
		this.usingId = usingId;
		tbmuser = Common.getCurrentUser();
		this.clazz = clazz;

		if (display) {
			display();
			onSearchDefault(null);
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataLampiranFileLain}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataLampiranFileLain} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataLampiranFileLain
	 */
	class FileFotoLainRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final FileFotoLain fileFotoLain = (FileFotoLain) arg1;
			arg0.setAttribute("fileFotoLain", fileFotoLain);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			final Radio checkbox = new Radio();
			checkbox.setParent(hbox);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
						Event myEvent = new Event("myEvent", arg0.getTarget(), fileFotoLain);
						eventListener.onEvent(myEvent);
					}
					AmbilDataLampiranFileLain.this.detach();
				}
			});
			String n = fileFotoLain.getNama() != null && (fileFotoLain.getNama().trim().equalsIgnoreCase("link")
					|| fileFotoLain.getNama().trim().equalsIgnoreCase("Berupa link file")) ? fileFotoLain.getLink()
							: fileFotoLain.getNama();

			String icon = FileFoto.icon(n);

			Toolbarbutton downloadButton = new MyToolbarbuttonConfig(n.length() > 30 ? n.substring(0, 30) + "..." : n,
					icon);
			downloadButton.setTooltiptext("Download \"" + fileFotoLain.getNama() + "\"");
			downloadButton.setAttribute("janganDisabled", true);
			hbox.appendChild(downloadButton);
			downloadButton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (fileFotoLain.getGdrive() != null) {
						fileFotoLain.tampilGDrive(null);
					} else {

						File f = fileFotoLain.ambilFile();
						if (f != null && f.getName().toLowerCase().endsWith("zip")) {
							Filedownload.save(f, "application/zip");
						} else if (f != null && f.getName().toLowerCase().endsWith("rar")) {
							Filedownload.save(f, "application/rar");
						} else if (f != null && f.getName().toLowerCase().endsWith("tar")) {
							Filedownload.save(f, "application/tar");
						} else if (f != null && f.getName().toLowerCase().endsWith("gz")) {
							Filedownload.save(f, "application/tar.gz");
						} else {
							String link = fileFotoLain == null ? null
									: (fileFotoLain.getLink() == null || fileFotoLain.getLink().isEmpty()
											? FileFotoLain.ambilLinkLampiranLain(fileFotoLain, usingId, true, clazz)
											: fileFotoLain.getLink());
							if (fileFotoLain != null && link != null && !link.trim().isEmpty()) {

								if (fileFotoLain.bisaPreview()) {
									Common.displayWindow(fileFotoLain.merupakanGambar(), link, true, "95%", "95%", true,
											fileFotoLain);
								} else {
									if (Common.isMobile()) {
										ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
									} else {
										Clients.evalJavaScript(
												"popupCenter({url: '" + link + "', title: 'data', w: 1200, h: 600});");
									}
								}
							} else {
								MyMessageboxConfig.show(
										"Mohon maaf, berkas yang Anda akses tidak ditemukan. Langkah yang dapat dilakukan: (1) muat ulang halaman lalu coba kembali; (2) pastikan berkas belum dihapus atau dipindahkan; (3) hubungi administrator apabila berkas seharusnya tersedia.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				}
			});
			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new Label(fileFotoLain.getTanggal_dirubah() == null ? ""
					: Common.dateFormat5.get().format(fileFotoLain.getTanggal_dirubah())).setParent(vbox1);
			new Label(fileFotoLain.getJenis()).setParent(vbox1);

			String olehId = fileFotoLain.getOlehId();
			String oleh = fileFotoLain.getOleh();

			Common.infoDiuploadOleh(olehId, oleh, arg0);

		}

	}

	// Slot handoff upload foto/GDrive: servlet mengisi, ZK Timer polling per rand.
	// LRU BER-BATAS (bukan HashMap polos): key = Long acak BARU per dialog upload dan
	// entri lama tidak pernah dihapus — sebelumnya tumbuh monoton seumur JVM
	// (optimasi RAM Fase 1). 500 slot upload aktif bersamaan lebih dari cukup;
	// entri tertua otomatis tersingkir.
	public static Map<Long, Map<String, Object>> fotoDrive = java.util.Collections
			.synchronizedMap(new java.util.LinkedHashMap<Long, Map<String, Object>>(16, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<Long, Map<String, Object>> eldest) {
					return size() > 500;
				}
			});

	public MyToolbarbuttonConfig createScanFoto() {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Gunakan Kamera", "/img/camera-icon.png");

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event a) throws Exception {
				final Long rand = Common.randLong();
				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(rand);
						if (da != null) {

							if (da.get("fileFotoLain") != null) {

								FileFotoLain a = (FileFotoLain) da.get("fileFotoLain");
								Event myEvent = new Event("Baru", arg0.getTarget(), a);
								eventListener.onEvent(myEvent);
								AmbilDataLampiranFileLain.this.detach();
								timer.detach();
							} else if (da.get("drive") != null) {

								String d = (String) da.get("drive");
								String file_name = (String) da.get("file_name");
								if (d != null && !d.trim().isEmpty()) {

									File f = null;
									try {
										f = new File(d);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:280");
										// TODO: handle exception
									}

									if (f != null && f.exists()) {
										FileFotoLain a = simpanData(null, null, f, file_name, null, null, null);
										Event myEvent = new Event("Baru", arg0.getTarget(), a);
										eventListener.onEvent(myEvent);
										AmbilDataLampiranFileLain.this.detach();
									} else {
										FileFotoLain a = simpanData(null, null, null, file_name, d, null, null);
										Event myEvent = new Event("Baru", arg0.getTarget(), a);
										eventListener.onEvent(myEvent);
										AmbilDataLampiranFileLain.this.detach();
									}

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				fotoDrive.put(rand, null);
				String q = "&rand=" + rand + "&clazz=" + clazz.getName() + "&jenis="
						+ URLEncoder.encode(jenis, "UTF-8");

				try {

					final MyWindow window = new MyWindow(linkAtauDive ? "Ambil Foto / Video" : "Ambil Foto", "none",
							true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					String src = Common.getRequestHostWithProtocol() + "/capture.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;
					String src1 = Common.getRequestHostWithProtocol() + "/capture_video.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;

					Tabbox tabbox = new Tabbox();
					tabbox.setHeight("100%");
					tabbox.setWidth("100%");
					tabbox.setParent(center);
					Tabs myTabs = new Tabs();
					myTabs.setParent(tabbox);

					Tabpanels mytabpanels = new Tabpanels();
					mytabpanels.setParent(tabbox);

					Tab tabUtama = new Tab("Foto");
					myTabs.appendChild(tabUtama);

					boolean mobile = Common.isMobile();
					String tinggi = mobile ? "850px" : "550px";

					Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
					tabpanelUtama.setHeight(tinggi);
					tabpanelUtama.setWidth("100%");
					tabpanelUtama.setParent(mytabpanels);

					Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
							+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html.setHeight(tinggi);
					Common.tampilanScroll(tabpanelUtama).appendChild(html);

					Tab tabUtama1 = new Tab("Video");
					tabUtama1.setVisible(linkAtauDive);
					myTabs.appendChild(tabUtama1);

					Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
					tabpanelUtama1.setVisible(linkAtauDive);
					tabpanelUtama1.setHeight(tinggi);
					tabpanelUtama1.setWidth("100%");
					tabpanelUtama1.setParent(mytabpanels);

					Html html1 = new ais.ui.util.MyHtml("<iframe src=\"" + src1 + "\" style=\"width:100%;height:"
							+ tinggi + ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html1.setHeight(tinggi);
					Common.tampilanScroll(tabpanelUtama1).appendChild(html1);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					window.setVisible(true);
					window.setHeight("97%");
					window.setWidth(mobile ? "97%" : "550px");
					window.onModal();

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:388");
				}
			}
		});

		return toolbarbutton;
	}

	public MyToolbarbuttonConfig createScanLayar() {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Rekam Layar", "/img/Monitor-3-icon.png");
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event a) throws Exception {
				final Long rand = Common.randLong();
				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(rand);
						if (da != null) {
							if (da.get("fileFotoLain") != null) {

								FileFotoLain a = (FileFotoLain) da.get("fileFotoLain");
								Event myEvent = new Event("Baru", arg0.getTarget(), a);
								eventListener.onEvent(myEvent);
								AmbilDataLampiranFileLain.this.detach();
								timer.detach();
							} else if (da.get("drive") != null) {
								String d = (String) da.get("drive");
								String file_name = (String) da.get("file_name");
								if (d != null && !d.trim().isEmpty()) {

									File f = null;
									try {
										f = new File(d);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:428");
										// TODO: handle exception
									}

									if (f != null && f.exists()) {
										FileFotoLain a = simpanData(null, null, f, file_name, null, null, null);
										Event myEvent = new Event("Baru", arg0.getTarget(), a);
										eventListener.onEvent(myEvent);
										AmbilDataLampiranFileLain.this.detach();
									} else {
										FileFotoLain a = simpanData(null, null, null, file_name, d, null, null);
										Event myEvent = new Event("Baru", arg0.getTarget(), a);
										eventListener.onEvent(myEvent);
										AmbilDataLampiranFileLain.this.detach();
									}

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				fotoDrive.put(rand, null);
				String q = "&rand=" + rand + "&clazz=" + clazz.getName() + "&jenis="
						+ URLEncoder.encode(jenis, "UTF-8");

				try {

					final MyWindow window = new MyWindow("Rekam Layar", "none", true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					String src = Common.getRequestHostWithProtocol() + "/capture_screen.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;

					boolean mobile = Common.isMobile();
					String tinggi = mobile ? "850px" : "550px";

					Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
							+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html.setHeight(tinggi);
					Common.tampilanScroll(center).appendChild(html);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					window.setVisible(true);
					window.setHeight("97%");
					window.setWidth(mobile ? "97%" : "550px");
					window.onModal();

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:500");
				}
			}
		});

		return toolbarbutton;
	}

	@SuppressWarnings("deprecation")
	private FileFotoLain simpanData(UploadEvent uploadEvent, Jurusan selectedJurusan, File f, String file_name,
			String gdrive, String link, String type) throws Exception {

		// 1. Evaluasi objek user hanya satu kali di awal
		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
		String olehId = Common.generateOlehId(tbmuser);

		String namaOleh = "external_update";
		if (tbmuser != null) {
			if (mahasiswa != null)
				namaOleh = mahasiswa.getNama();
			else if (dosen != null)
				namaOleh = dosen.getNama();
			else if (pegawai != null)
				namaOleh = pegawai.getNama();
			else
				namaOleh = tbmuser.getUserNama();
		}

		// 2. Hindari evaluasi ternary yang berulang-ulang, simpan di variabel
		String namaFile = uploadEvent == null ? (f != null ? f.getName() : file_name)
				: (uploadEvent.getMedia() == null ? file_name : uploadEvent.getMedia().getName());
		if (namaFile == null || namaFile.trim().length() == 0) {
			namaFile = "lampiran_" + System.currentTimeMillis();
		}
		String keteranganFile = type != null ? type
				: (uploadEvent == null ? "image/jpg" : uploadEvent.getMedia().getContentType());
		String gdriveUser = tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId();

		FileFotoLain a = (FileFotoLain) clazz.newInstance();
		Session streamingSession = null;

		try {
			streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			FileFotoLain.hapusAtauUpdate(a, streamingSession, usingId, ref, jenis);

			// 3. Panggil method terpisah untuk memetakan data kelas spesifik
			mappingInstanceData(a, ref, selectedJurusan, jenis, link, namaFile, keteranganFile, olehId, namaOleh);

			a.setGdrive(gdrive);
			a.setGdriveUsername(gdriveUser);

			streamingSession.getTransaction().begin();

			// 4. Efisiensi: Set BLOB sebelum save pertama kali agar tidak terjadi 2x
			// transaksi
			if (f != null && f.exists()) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(f.getAbsolutePath());
					a.setFoto(new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(fis)));
				} finally {
					if (fis != null) {
						try {
							fis.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:566");
						}
					}
				}
			}

			streamingSession.saveOrUpdate(a);
			streamingSession.getTransaction().commit();

		} catch (Exception e) {
			if (streamingSession != null && streamingSession.getTransaction().isActive()) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
			}
			Common.tampilErrorJikaAdmin(e);
			throw e; // Opsional: Direkomendasikan dilempar agar sistem mendeteksi kegagalan
		} finally {
			// 5. Wajib berada di finally untuk mencegah resource/connection leak
			if (streamingSession != null) {
				try {
					streamingSession.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:586");
				}
				try {
					if (streamingSession.isOpen()) {
						streamingSession.disconnect();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:592");
				}
				try {
					if (streamingSession.isOpen()) {
						streamingSession.close();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:598");
				}
			}
			try {
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:603");
			}
		}

		return a;
	}
	
	@SuppressWarnings("rawtypes")
	public static FileFotoLain ambilFile(Serializable ref, Class clazz) {
	    if (ref == null || clazz == null) {
	        return null;
	    }

	    // 1. Tentukan nama kolom (property) referensi berdasarkan class
	    String propertyName = null;
	    String className = clazz.getSimpleName();

	    if (className.equals("LampiranLain")) propertyName = "ref";
	    else if (className.equals("FotoGambarProduk")) propertyName = "produk";
	    else if (className.equals("LampiranLainMahasiswa") || className.equals("FotoMahasiswa")) propertyName = "mahasiswa";
	    else if (className.equals("LampiranLainBiodataCalonMahasiswa") || className.equals("FotoBiodataCalonMahasiswa")) propertyName = "biodataCalonMahasiswa";
	    else if (className.equals("FotoDosen")) propertyName = "dosen";
	    else if (className.equals("FotoPegawai")) propertyName = "pegawai";
	    else if (className.equals("FotoGuru")) propertyName = "guru";
	    else if (className.equals("FotoCalonSiswa")) propertyName = "calonSiswa";
	    else if (className.equals("FotoCalonPegawai")) propertyName = "calonPegawai";
	    else if (className.equals("FotoSiswa")) propertyName = "siswa";
	    else if (className.equals("FotoAdmin")) propertyName = "tbmuser";
	    else if (className.equals("LampiranPklMahasiswa")) propertyName = "persyaratanPkl";
	    else if (className.equals("LampiranKknMahasiswa")) propertyName = "persyaratanKkn";
	    else if (className.equals("LampiranBeasiswaMahasiswa")) propertyName = "persyaratanBeasiswa";

	    if (propertyName == null) {
	        System.err.println("Class tidak dikenali dalam mapping ambilFile: " + className);
	        return null; 
	    }

	    Session session = null;
	    FileFotoLain result = null;

	    try {
	        session = StreamingHibernateUtil.getInstance().currentSession();

	        // 2. Susun HQL Query (ORDER BY id DESC memastikan kita mengambil lampiran/foto terbaru jika ada duplikat)
	        String hql = "FROM " + clazz.getName() + " f WHERE f." + propertyName + " = :refVal ORDER BY f.id DESC";
	        org.hibernate.Query query = session.createQuery(hql);

	        // 3. Konversi tipe data parameter (FotoAdmin menggunakan String, sisanya Long)
	        if (className.equals("FotoAdmin")) {
	            query.setParameter("refVal", ref.toString());
	        } else {
	            Long idRef = ref instanceof Long ? (Long) ref : Long.valueOf(ref.toString());
	            query.setParameter("refVal", idRef);
	        }

	        query.setMaxResults(1); 
	        result = (FileFotoLain) query.uniqueResult();

	    } catch (Exception e) {
	        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:662");
	    } finally {
	        // 4. Tutup session di blok finally untuk menghindari memory / connection leak
	        if (session != null && session.isOpen()) {
	            try {
	                session.close();
	            } catch (Exception ex) {
	                ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:669");
	            }
	        }
	    }

	    return result;
	}

	public static void mappingInstanceData(FileFotoLain a, Serializable ref, Jurusan selectedJurusan, String jenis,
			String link, String namaFile, String keteranganFile, String olehId, String namaOleh) {

		// Amankan parameter opsional
		String suffixJenis = jenis + (selectedJurusan == null ? "" : selectedJurusan.getId());
		Long idRef = ref instanceof Long ? (Long) ref : null;

		if (a instanceof LampiranLain) {
			LampiranLain obj = (LampiranLain) a;
			obj.setRef(idRef);
			obj.setJenis(suffixJenis);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoGambarProduk) {
			FotoGambarProduk obj = (FotoGambarProduk) a;
			obj.setProduk(idRef);
			obj.setJenis(suffixJenis);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof LampiranLainMahasiswa) {
			LampiranLainMahasiswa obj = (LampiranLainMahasiswa) a;
			obj.setMahasiswa(idRef);
			obj.setJenis(suffixJenis);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof LampiranLainBiodataCalonMahasiswa) {
			LampiranLainBiodataCalonMahasiswa obj = (LampiranLainBiodataCalonMahasiswa) a;
			obj.setBiodataCalonMahasiswa(idRef);
			obj.setJenis(suffixJenis);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoMahasiswa) {
			FotoMahasiswa obj = (FotoMahasiswa) a;
			obj.setMahasiswa(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoDosen) {
			FotoDosen obj = (FotoDosen) a;
			obj.setDosen(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoPegawai) {
			FotoPegawai obj = (FotoPegawai) a;
			obj.setPegawai(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoBiodataCalonMahasiswa) {
			FotoBiodataCalonMahasiswa obj = (FotoBiodataCalonMahasiswa) a;
			obj.setBiodataCalonMahasiswa(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoGuru) {
			FotoGuru obj = (FotoGuru) a;
			obj.setGuru(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoCalonSiswa) {
			FotoCalonSiswa obj = (FotoCalonSiswa) a;
			obj.setCalonSiswa(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoCalonPegawai) {
			FotoCalonPegawai obj = (FotoCalonPegawai) a;
			obj.setCalonPegawai(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoSiswa) {
			FotoSiswa obj = (FotoSiswa) a;
			obj.setSiswa(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof FotoAdmin) {
			FotoAdmin obj = (FotoAdmin) a;
			obj.setTbmuser(ref != null ? ref.toString() : null);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof LampiranPklMahasiswa) {
			LampiranPklMahasiswa obj = (LampiranPklMahasiswa) a;
			obj.setPersyaratanPkl(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof LampiranKknMahasiswa) {
			LampiranKknMahasiswa obj = (LampiranKknMahasiswa) a;
			obj.setPersyaratanKkn(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		} else if (a instanceof LampiranBeasiswaMahasiswa) {
			LampiranBeasiswaMahasiswa obj = (LampiranBeasiswaMahasiswa) a;
			obj.setPersyaratanBeasiswa(idRef);
			obj.setNama(namaFile);
			obj.setLink(link);
			obj.setKeterangan(keteranganFile);
			obj.setOlehId(olehId);
			obj.setOleh(namaOleh);
		}
	}

	public MyToolbarbuttonConfig createRekamAudio() {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Rekam Audio", "/img/Record-Normal-icon.png");

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event a) throws Exception {
				final Long rand = Common.randLong();
				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(rand);
						if (da != null) {
							if (da.get("fileFotoLain") != null) {

								FileFotoLain a = (FileFotoLain) da.get("fileFotoLain");
								Event myEvent = new Event("Baru", arg0.getTarget(), a);
								eventListener.onEvent(myEvent);
								AmbilDataLampiranFileLain.this.detach();
								timer.detach();
							} else if (da.get("drive") != null) {
								String d = (String) da.get("drive");
								String file_name = (String) da.get("file_name");
								if (d != null && !d.trim().isEmpty()) {

									File f = null;
									try {
										f = new File(d);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:852");
										// TODO: handle exception
									}

									if (f != null && f.exists()) {
										FileFotoLain a = simpanData(null, null, f, file_name, null, null, null);
										Event myEvent = new Event("Baru", arg0.getTarget(), a);
										eventListener.onEvent(myEvent);
										AmbilDataLampiranFileLain.this.detach();
									} else {
										FileFotoLain a = simpanData(null, null, null, file_name, d, null, null);
										Event myEvent = new Event("Baru", arg0.getTarget(), a);
										eventListener.onEvent(myEvent);
										AmbilDataLampiranFileLain.this.detach();
									}

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				fotoDrive.put(rand, null);
				String q = "&rand=" + rand + "&clazz=" + clazz.getName() + "&jenis="
						+ URLEncoder.encode(jenis, "UTF-8");

				try {

					final MyWindow window = new MyWindow("Rekam Audio", "none", true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					String src = Common.getRequestHostWithProtocol() + "/capture_audio.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;

					boolean mobile = Common.isMobile();
					String tinggi = mobile ? "850px" : "550px";

					Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
							+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html.setHeight(tinggi);
					Common.tampilanScroll(center).appendChild(html);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					window.setVisible(true);
					window.setHeight("97%");
					window.setWidth(mobile ? "97%" : "550px");
					window.onModal();

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:924");
				}
			}
		});

		return toolbarbutton;
	}

	public MyToolbarbuttonConfig tampilkanTombolUpload(String tambahan) {
		Integer cutomUkuranUpload = null;
		try {
			// NOTE: JANGAN pakai ternary "cond ? 50000 : this.cutomUkuranUpload" -
			// literal int 50000 memaksa Java melakukan numeric promotion pada
			// KEDUA operand ternary menjadi int (JLS 15.25), sehingga
			// this.cutomUkuranUpload (Integer) di-unbox TANPA SYARAT walau
			// cabang itu tidak dipilih. Jika field-nya null -> NPE rutin.
			if (ref != null && ref.equals(LampiranLain.ID_SKIN)) {
				cutomUkuranUpload = 50000;
			} else {
				cutomUkuranUpload = this.cutomUkuranUpload;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:936");

		}
		linkAtauDive = bolehDriveAtauLink(clazz, jenis, keterangan);
		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
				"Upload " + tambahan + keterangan + Common.ukuranLabelFileUpload(cutomUkuranUpload),
				"/img/File-Upload-icon.png");

		upload.setTooltiptext("Upload " + keterangan);
		upload.setAttribute("janganDisabled", true);
		upload.setUpload(Common.ukuranFileUpload(cutomUkuranUpload));
		upload.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;

				if (media.getName().toLowerCase().endsWith(".jsp")) {
					MyMessageboxConfig.show("File JSP tidak boleh Anda upload", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				if (media.getName().toLowerCase().endsWith(".jspx")) {
					MyMessageboxConfig.show("File JSPX tidak boleh Anda upload", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				if (!linkAtauDive) {
					boolean isJrxml = jenis != null && (jenis.toLowerCase().contains("jrxml")
							|| jenis.toLowerCase().contains("jasper"));
					boolean isSkin  = jenis != null && jenis.toLowerCase().contains("skin");

					if (isJrxml) {
						if (!media.getName().toLowerCase().endsWith(".jrxml")
								&& !media.getName().toLowerCase().endsWith(".jasper")) {
							MyMessageboxConfig.show("File harus dalam bentuk file *.jrxml atau *.jasper",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
					} else if (!isSkin) {
						// Semua upload non-link yang bukan JRXML dan bukan skin wajib berupa gambar.
						// validasiFoto() cek ekstensi (jpg/jpeg/png/gif/svg/webp) DAN MIME type (image/*).
						if (!AmbilDataTugasFileContent.validasiFoto(media)) return;
					}
				}

				Jurusan selectedJurusan = (Jurusan) (jurusan == null || jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue());

				if (!harusPdf || (jenis != null && jenis.equals(LampiranLain.ALUR_REGISTRASI_PMB))
						|| media.getName().toLowerCase().endsWith("pdf")) {

					try {

						File folder = CommonMedia.getMediaDirectory();
						if (!folder.exists()) {
							folder.mkdirs();
						}

						File f = new File(folder.getAbsolutePath() + "/"
								+ URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_"
										+ namaFileAmanUntukFilesystem(uploadEvent.getMedia().getName()), "UTF-8"));

						f.createNewFile();
						FileOutputStream fileOutputStream = new FileOutputStream(f);
						try {
							IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
						} catch (Exception e) {
							try {
								IOUtils.write(media.getStringData(), fileOutputStream);
							} catch (Exception ee) {
								IOUtils.write(media.getByteData(), fileOutputStream);
							}
						}

						fileOutputStream.close();

						FileFotoLain a = simpanData(uploadEvent, selectedJurusan, f, null, null, null, null);

						Event myEvent = new Event("Baru", event.getTarget(), a);
						eventListener.onEvent(myEvent);
						AmbilDataLampiranFileLain.this.detach();
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						MyMessageboxConfig.showFormat(
								"Mohon maaf, terjadi kesalahan saat mengunggah berkas {V1}.\nRincian teknis kesalahan: {V2}. Langkah yang dapat dilakukan: (1) periksa kembali berkas yang diunggah; (2) pastikan koneksi stabil lalu coba lagi; (3) hubungi administrator apabila kendala berlanjut.",
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR,
								media, e.getMessage());
					}

				} else {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, berkas yang Anda unggah harus berformat PDF. Berkas yang dipilih: {V1}. Langkah yang dapat dilakukan: (1) konversi berkas ke format PDF terlebih dahulu; (2) pastikan ekstensi berkas adalah .pdf; (3) unggah kembali berkas tersebut.",
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR,
							media);
				}
			}
		});

		return upload;
	}

	private FileFotoLain lainMahasiswa = null;
	private MyCheckboxConfig tampilkan;
	private MyBorderlayout myBorderlayout1;
	public boolean linkAtauDive = true;
	private Toolbar toolbar;

	public Toolbarbutton tampilkanTombolUploadGDrive(String tambahan) {

		int maxDrive = 300;
		try {
			maxDrive = Integer.parseInt(Common.getKonfigurasi("max_upload_via_drive_baru", "300").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:1054");
			// TODO: handle exception
		}

		Toolbarbutton upload = new ais.ui.util.MyToolbarbuttonConfig(
				Common.getBahasaConfig("Upload " + tambahan + keterangan + " ke Drive (maks " + maxDrive + " Mb)"),
				"/img/Google-Drive-icon.png");
		upload.setTooltiptext("Upload " + keterangan);
		upload.setAttribute("janganDisabled", true);
		upload.setUpload("true,maxsize=" + (1024 * maxDrive));
		upload.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				lainMahasiswa = null;
				File folder = CommonMedia.getMediaDirectory();
				if (!folder.exists()) {
					folder.mkdirs();
				}
				final UploadEvent uploadEvent = (UploadEvent) event;
				final Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;

				if (!harusPdf || media.getName().toLowerCase().endsWith("pdf")) {

					File f = new File(folder.getAbsolutePath() + "/"
							+ URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_"
									+ namaFileAmanUntukFilesystem(uploadEvent.getMedia().getName()), "UTF-8"));

					f.createNewFile();
					FileOutputStream fileOutputStream = new FileOutputStream(f);
					try {
						IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
					} catch (Exception e) {
						try {
							IOUtils.write(media.getStringData(), fileOutputStream);
						} catch (Exception ee) {
							IOUtils.write(media.getByteData(), fileOutputStream);
						}
					}

					fileOutputStream.close();

					GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);

					driveUtilPerPengguna.prosesBackup(f, clazz.getSimpleName(), keterangan, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
									.getData();

							if (fileUpload != null && fileUpload.getId() != null) {

								Jurusan selectedJurusan = (Jurusan) (jurusan == null
										|| jurusan.getSelectedItem() == null
										|| jurusan.getSelectedItem().getValue() == null ? null
												: jurusan.getSelectedItem().getValue());

								lainMahasiswa = simpanData(uploadEvent, selectedJurusan, null, null, fileUpload.getId(),
										null, null);
							}
						}
					});

					final Timer timer = new Timer(1000);
					timer.setRepeats(true);
					timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.addEventListener("onTimer", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (lainMahasiswa != null && lainMahasiswa.getId() != null) {

								Event myEvent = new Event("Baru", null, lainMahasiswa);

								try {
									eventListener.onEvent(myEvent);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:1136");
								}
								AmbilDataLampiranFileLain.this.detach();
								timer.stop();
								timer.detach();
							}
						}
					});
					timer.start();

				} else {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, berkas yang Anda unggah harus berformat PDF. Berkas yang dipilih: {V1}. Langkah yang dapat dilakukan: (1) konversi berkas ke format PDF terlebih dahulu; (2) pastikan ekstensi berkas adalah .pdf; (3) unggah kembali berkas tersebut.",
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR,
							media);
				}
			}
		});

		return upload;
	}

	public MyToolbarbuttonConfig tampilkanTombolUploadDropbox(String tambahan) {
		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
				"Upload " + tambahan + keterangan + " ke Dropbox (maks 500 Mb)", FileFoto.icon("dropbox"));
		upload.setVisible(false);
		upload.setTooltiptext("Upload " + keterangan);
		upload.setAttribute("janganDisabled", true);
		upload.setUpload("true,maxsize=" + (1024 * 500));
		upload.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				lainMahasiswa = null;
				File folder = CommonMedia.getMediaDirectory();
				if (!folder.exists()) {
					folder.mkdirs();
				}
				final UploadEvent uploadEvent = (UploadEvent) event;
				final Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;

				if (!harusPdf || media.getName().toLowerCase().endsWith("pdf")) {

					File f = new File(folder.getAbsolutePath() + "/"
							+ URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_"
									+ namaFileAmanUntukFilesystem(uploadEvent.getMedia().getName()), "UTF-8"));

					f.createNewFile();
					FileOutputStream fileOutputStream = new FileOutputStream(f);
					try {
						IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
					} catch (Exception e) {
						try {
							IOUtils.write(media.getStringData(), fileOutputStream);
						} catch (Exception ee) {
							IOUtils.write(media.getByteData(), fileOutputStream);
						}
					}

					fileOutputStream.close();

					UploadDropboxUtil.prosesBackup(f, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							String urlLink = (String) arg0.getData();

							if (urlLink != null && !urlLink.trim().isEmpty()) {
								Jurusan selectedJurusan = (Jurusan) (jurusan == null
										|| jurusan.getSelectedItem() == null
										|| jurusan.getSelectedItem().getValue() == null ? null
												: jurusan.getSelectedItem().getValue());
								lainMahasiswa = simpanData(uploadEvent, selectedJurusan, null, null, null, urlLink,
										media.getContentType());
							}
						}
					});

					final Timer timer = new Timer(1000);
					timer.setRepeats(true);
					timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.addEventListener("onTimer", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (lainMahasiswa != null && lainMahasiswa.getId() != null) {

								Event myEvent = new Event("Baru", null, lainMahasiswa);

								try {
									eventListener.onEvent(myEvent);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:1232");
								}
								AmbilDataLampiranFileLain.this.detach();
								timer.stop();
								timer.detach();
							}
						}
					});
					timer.start();

				} else {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, berkas yang Anda unggah harus berformat PDF. Berkas yang dipilih: {V1}. Langkah yang dapat dilakukan: (1) konversi berkas ke format PDF terlebih dahulu; (2) pastikan ekstensi berkas adalah .pdf; (3) unggah kembali berkas tersebut.",
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR,
							media);
				}
			}
		});

		return upload;
	}

	@SuppressWarnings("rawtypes")
	public static boolean bolehDriveAtauLink(Class clazz, String jenis, String keterangan) {
		if (clazz != null
				&& (clazz.getName().equals(FotoMahasiswa.class.getName())
						|| clazz.getName().equals(FotoMahasiswaLulus.class.getName())
						|| clazz.getName().equals(FotoBiodataCalonMahasiswa.class.getName())
						|| clazz.getName().equals(FotoDosen.class.getName())
						|| clazz.getName().equals(FotoPegawai.class.getName())
						|| clazz.getName().equals(FotoSiswa.class.getName())
						|| clazz.getName().equals(FotoCalonSiswa.class.getName())
						|| clazz.getName().equals(KlasifikasiSuratKeluarParemeter.class.getName()))
				|| clazz.getName().equals(FotoCalonPegawai.class.getName())
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_DOSEN))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_ALUMNI_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_ANGGOTA_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_MAHASISWA_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_PEGAWAI_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_SISWA_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_PEGAWAI))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.SKIN))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.KOP_FAKULTAS))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.KOP_JURUSAN))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.STEMPEL_JURUSAN))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.KOP_PT))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.KOP_BAWAH_PT))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.KOP_SEKOLAH))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.KOP_BAWAH_SEKOLAH))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.KOP_YAYASAN))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.FOOT_FAKULTAS))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BACKGROUND_DEPAN_1_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BACKGROUND_DEPAN_2_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BACKGROUND_DEPAN_PESANTREN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BACKGROUND_LOGIN_PT))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BACKGROUND_LOGIN_SEKOLAH))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BACKGROUND_PT))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BACKGROUND_SEKOLAH))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BACKGROUND_YAYASAN))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_1_KARTU_SISWA_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_2_KARTU_SISWA_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_1_KARTU_ALUMNI_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_1_KARTU_ANGGOTA_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_1_KARTU_MAHASISWA_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_1_KARTU_PEGAWAI_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_2_KARTU_ALUMNI_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_2_KARTU_ALUMNI_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_2_KARTU_ANGGOTA_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_2_KARTU_MAHASISWA_PERPUSTAKAAN_STR))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT))
				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.FILE_JRXML_LAYOUT_SURAT))
				|| (jenis != null && jenis.toLowerCase().contains("stempel"))
				|| (jenis != null && jenis.toLowerCase().contains("kop"))
				|| (jenis != null && jenis.toLowerCase().contains("jrxml"))
				|| (jenis != null && jenis.toLowerCase().contains("jasper"))
				|| (jenis != null && jenis.toLowerCase().contains("ttd"))
				|| (jenis != null && jenis.toLowerCase().contains("galery"))
				|| (jenis != null && jenis.toLowerCase().contains("logo"))
				|| (jenis != null && jenis.toLowerCase().contains("gambar"))
				|| (jenis != null && jenis.toLowerCase().contains("banner"))
				|| (jenis != null && jenis.toLowerCase().contains("footer"))

				|| (keterangan != null && keterangan.toLowerCase().contains("banner"))
				|| (keterangan != null && keterangan.toLowerCase().contains("stempel"))
				|| (keterangan != null && keterangan.toLowerCase().contains("kop"))
				|| (keterangan != null && keterangan.toLowerCase().contains("footer"))
				|| (keterangan != null && keterangan.toLowerCase().contains("jrxml"))
				|| (keterangan != null && keterangan.toLowerCase().contains("jasper"))
				|| (keterangan != null && keterangan.toLowerCase().contains("ttd"))
				|| (keterangan != null && keterangan.toLowerCase().contains("galery"))
				|| (keterangan != null && keterangan.toLowerCase().contains("logo"))
				|| (keterangan != null && keterangan.toLowerCase().contains("gambar"))
				|| (keterangan != null && keterangan.toLowerCase().contains("background"))
				|| (keterangan != null && keterangan.toLowerCase().contains("banner"))
				|| (keterangan != null && keterangan.toLowerCase().contains("header"))

				|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.BG_2_KARTU_PEGAWAI_PERPUSTAKAAN_STR))) {

			return false;
		}
		return true;
	}

	public void display() {

		// paging = new Paging();
		// Common.initPaging(paging, new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// onSearchDefault(null);
		// }
		// });

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setParent(this);
		radiogroup.setHeight("100%");
		radiogroup.setWidth("100%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(radiogroup);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);
		myBorderlayout1.setVisible(false);
		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		North myNorth = new North();
		ais.ui.util.ZkCompat.setFlex(myNorth, true);
		myNorth.setParent(myBorderlayout1);

		Grid searchgrid = new Grid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(myNorth);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama File"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(myjenis = new MyTextbox(jenis));
		myjenis.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		MyToolbarbuttonConfig buttonCari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		buttonCari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		buttonCari.setParent(row);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setHeight("415px");

		toolbar = new Toolbar();

		toolbar.setParent(north);

		linkAtauDive = bolehDriveAtauLink(clazz, jenis, keterangan);

		toolbar.appendChild(createScanFoto());

		if (!linkAtauDive || Common.bolehKonfigurasi("boleh_upload_file_langsung")) {
			toolbar.appendChild(tampilkanTombolUpload(" "));
		}

		if (linkAtauDive) {
			toolbar.appendChild(tampilkanTombolUploadGDrive(" "));
			toolbar.appendChild(tampilkanTombolUploadDropbox(" "));
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Link " + keterangan, "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					final Window myWindow = new Window("Tambah link " + keterangan, "none", true);
					myWindow.setHeight("95%");
					myWindow.setWidth("850px");
					myWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
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

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBold("Link " + keterangan));

					row = new MyFormRow();
					row.setParent(rows);
					final Textbox isia;
					row.appendChild(isia = new Textbox());
					isia.setValue("");
					isia.setWidth("90%");
					isia.setRows(3);
					isia.select();

					row = new MyFormRow();
					row.setParent(rows);

					row.appendChild(new Image(FileFoto.icon("drive.google")));

					Common.initKeteranganSatuKolom(rows,
							"Contoh link jika menggunakan google drive : https://drive.google.com/file/d/1jqqlH3bqCE9IcShsooF_RqOYpLehKiRV/view?usp=sharing");
					Common.initKeteranganSatuKolom(rows,
							"atau contoh di google drive : atau juga bisa https://drive.google.com/open?id=1jqqlH3bqCE9IcShsooF_RqOYpLehKiRV");

					Common.initKeteranganSatuKolom(rows,
							"Contoh link di dalam folder drive : https://drive.google.com/drive/folders/0B1iqp0kGPjWsNDg5NWFlZjEtN2IwZC00NmZiLWE3MjktYTE2ZjZjNTZiMDY2");

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new Image(FileFoto.icon("youtube")));
					Common.initKeteranganSatuKolom(rows,
							"Contoh link jika menggunakan youtube : https://www.youtube.com/watch?v=Ed8Uw9b_jyk");

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new Image(FileFoto.icon("instagram")));
					Common.initKeteranganSatuKolom(rows,
							"Contoh link jika menggunakan Instagram : https://www.instagram.com/p/fA9uwTtkSN");

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new Image(FileFoto.icon("dropbox")));
					Common.initKeteranganSatuKolom(rows,
							"Contoh link jika menggunakan dropbox : https://www.dropbox.com/s/fshcbd82hnj0f60/1590735986510_Funny%2BCat%2BFaces%2BCompilation%2B2014%2B%255BNEW%255D.mp4?dl=0");

					row = new MyFormRow();
					row.setParent(rows);

					row.appendChild(new Image(FileFoto.icon("mp3")));
					Common.initKeteranganSatuKolom(rows,
							"Contoh link MP3 : https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_700KB.mp3");

					row = new MyFormRow();
					row.setParent(rows);

					row.appendChild(new Image(FileFoto.icon("pdf")));
					Common.initKeteranganSatuKolom(rows,
							"Contoh link pdf : https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");

					row = new MyFormRow();
					row.setParent(rows);

					row.appendChild(new Image(FileFoto.icon("facebook")));

					Common.initKeteranganSatuKolom(rows,
							"Contoh post facebook : https://www.facebook.com/20531316728/posts/10154009990506729/");

					row = new MyFormRow();
					row.setParent(rows);

					row.appendChild(new Image(FileFoto.icon("twitter")));

					Common.initKeteranganSatuKolom(rows,
							"Contoh post twitter : https://twitter.com/Interior/status/463440424141459456");

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
							myWindow.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan Link", "/img/save.gif");
					save.setTooltiptext("Simpan");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							List<String> urls = Common.getUrls(isia.getValue().trim());
							if (urls.isEmpty()) {
								MyMessageboxConfig.show(
										"Masukkan link secara valid, perhatikan beberapa contoh link di bawah ini.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return;
							}

							String type = "";
							try {
								URL url = new URL(urls.get(0));
								HttpURLConnection huc = (HttpURLConnection) url.openConnection();
								type = huc.getHeaderField("Content-Type");
//								System.out.println("type => " + type);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataLampiranFileLain.java:1579");
							}

							Jurusan selectedJurusan = (Jurusan) (jurusan == null || jurusan.getSelectedItem() == null
									|| jurusan.getSelectedItem().getValue() == null ? null
											: jurusan.getSelectedItem().getValue());

							try {

								FileFotoLain a = simpanData(null, selectedJurusan, null, null, null, urls.get(0), type);

								Event myEvent = new Event("myEvent", event.getTarget(), a);
								eventListener.onEvent(myEvent);
								AmbilDataLampiranFileLain.this.detach();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}

							StreamingHibernateUtil.getInstance().closeSession();

							myWindow.detach();
						}
					});
					save.setParent(toolbar);
					borderlayout.setParent(myWindow);
					myWindow.onModal();
				}

			});
			button.setParent(toolbar);

			if (!Common.isMobile())
				toolbar.appendChild(createScanLayar());
			toolbar.appendChild(createRekamAudio());
		}

		tampilkan = new MyCheckboxConfig("Tampilkan file-file yg telah diupload sebelumnya");
		toolbar.appendChild(tampilkan);
		tampilkan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				onSearchDefault(arg0);
			}
		});

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataLampiranFileLain.this.detach();
			}
		});
		cancel.setParent(toolbar);

		myCenter1.setTitle("Pilih daftar \"" + keterangan + "\" yang sebelumnya pernah di-upload, jika file \""
				+ keterangan + "\" lebih besar dari " + Common.ukuranLabelFileUpload() + ", maka \"" + keterangan
				+ "\" harus di-upload di tempat lain dan klik tambahkan link di atas, kemudian masukkan link dari file yang baru saja di-upload.");

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(myCenter1);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("File");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu / Jenis Upload");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diupload oleh");
		column.setWidth("15%");

	}

	public Criteria initCriteria(boolean order, Session session) {

		boolean hanyaBolehMelihatlampirannyaSendiri = Common.bolehKonfigurasi("hanya_boleh_melihat_lampirannya_sendiri", Konfigurasi.TIDAK_AKTIF);

		BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();
		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
		String olehId = tbmuser == null ? "external_update;"
				: biodataCalonMahasiswa != null
						? biodataCalonMahasiswa.getNoRegistrasi() + ";" + BiodataCalonMahasiswa.class.getName()
						: mahasiswa != null ? mahasiswa.getNim() + ";" + Mahasiswa.class.getName()
								: dosen != null ? tbmuser.getUserId() + ";" + Dosen.class.getName()
										: pegawai != null ? tbmuser.getUserId() + ";" + Pegawai.class.getName()
												: (tbmuser.getUserId() + ";" + Tbmuser.class.getName());

		Criteria criteria = session.createCriteria(clazz)
				.add(!linkAtauDive ? Restrictions.isNotNull("foto") : Restrictions.sqlRestriction("true"))
				.add(dosen != null || mahasiswa != null || biodataCalonMahasiswa != null
						|| tbmuser.getPesertaKursus() != null || hanyaBolehMelihatlampirannyaSendiri
								? Restrictions.ilike("olehId", olehId, MatchMode.START)
								: Restrictions.sqlRestriction("true"))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("link", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(myjenis.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("jenis", myjenis.getValue().trim(), MatchMode.ANYWHERE));

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onSearchDefault(Event event) {

		toolbar.setHeight("200px");

		boolean mobile = Common.isMobile();
		if (tampilkan.isChecked()) {
			toolbar.setOrient("vertical");
			myBorderlayout1.setVisible(true);
			AmbilDataLampiranFileLain.this.setWidth("90%");
			AmbilDataLampiranFileLain.this.setHeight("95%");
			AmbilDataLampiranFileLain.this.setPosition("left,top");
			Session session = StreamingHibernateUtil.getInstance().currentSession();
			List<FileFotoLain> myFileFotoLain = initCriteria(true, session).setMaxResults(Common.ROWS_COUNT_ON_PAGE_100)
					.list();

			ListModel strset = new SimpleListModel(myFileFotoLain);
			grid.setRowRenderer(new FileFotoLainRenderer());
			grid.setModelCheckMobile(strset);

			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			StreamingHibernateUtil.getInstance().closeSession();
		} else {

			myBorderlayout1.setVisible(false);
			toolbar.setOrient("vertical");
			AmbilDataLampiranFileLain.this.setHeight("420px");
			AmbilDataLampiranFileLain.this.setWidth(mobile ? "95%" : "850px");

			ListModel strset = new SimpleListModel(new ArrayList());
			grid.setRowRenderer(new FileFotoLainRenderer());
			grid.setModelCheckMobile(strset);

		}
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
