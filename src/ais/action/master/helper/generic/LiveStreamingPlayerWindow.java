package ais.action.master.helper.generic;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.East;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.VideoConverterPerkecil;
import ais.common.YouTubeHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk live streaming player window. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Map steams}, {@code Pertemuan
 * pertemuan}, {@code EventListener eventListener}, {@code String kodeStreamParam}; mutasi data ({@code
 * simpanVideo()}); operasi domain lain ({@code langkahLangkah()}, {@code displayYoutube()}, {@code
 * displayFacebook()}, {@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LiveStreamingPlayerWindow extends MyWindow {

	public static Map<String, Object[]> steams;

	public static void simpanVideo(String kodeStream, String host, Pertemuan pertemuan) {
		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			VideoPertemuan videoPertemuan = (VideoPertemuan) session.createCriteria(VideoPertemuan.class)
					.add(Restrictions.eq("nama", kodeStream)).setMaxResults(1).uniqueResult();
			if (videoPertemuan == null) {
				videoPertemuan = new VideoPertemuan();
			}
			videoPertemuan.setLokasiSimpan(null);
			videoPertemuan.setNama(kodeStream);

			videoPertemuan.setPertemuan(pertemuan.getId());
			if (pertemuan.getPerkuliahan() != null) {
				if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getJurusan() != null)
					videoPertemuan.setJurusan(pertemuan.getPerkuliahan().getJurusan().getId());
				videoPertemuan.setTahunAkademik(pertemuan.getPerkuliahan().getTahunAjaran());
			} else {
				videoPertemuan.setTahunAkademik(Common.getCurrentTahunAkademik());
			}
			videoPertemuan.setKeteranganTambahan("Rekaman video dari " + pertemuan.getTopik()
					+ (pertemuan.getPerkuliahan() != null ? "" : ". " + pertemuan.info()));
			videoPertemuan.setRtmp(kodeStream + ".flv");
			session.getTransaction().begin();
			session.save(videoPertemuan);
			session.getTransaction().commit();

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private Tabbox langkahLangkah(String alamatStream, String kodeStream) throws Exception {

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabUmum = new MyTabConfig("Menggunakan Komputer dengan merekam aktifitas di desktop");
		tabUmum.setParent(tabs);

		tabUmum = new MyTabConfig("Menggunakan Komputer dengan merekam via webcam");
		tabUmum.setParent(tabs);

		tabUmum = new MyTabConfig("Menggunakan Komputer dengan merekam via Android");
		tabUmum.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setParent(tabpanelUtama);
		Rows rows = new Rows();
		rows.setParent(grid);

		String liveUrl = Common.getRequestHostWithProtocol() + "/component/live/live.jsp?app="
				+ URLEncoder.encode(kodeStream, "UTF-8") + "&urlRtmp=" + URLEncoder.encode(alamatStream, "UTF-8");

		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.setValign("top");

		String alamatLengkap = alamatStream + "/" + kodeStream;
		String contentInfo = "<h3>Langkah-langkah memulai live streaming menggunakan laptop/Komputer guna merekam aktifitas di desktop:</h3>"
				+ "<ol style='font-size:12px'>"
				+ "<li>Untuk dapat menggunakan live streaming, pertama Anda harus mengunduh aplikasi <i>RTMP Broadcaster</i>, contoh \"OBS Studio\" di <a href=\"https://obsproject.com/\">https://obsproject.com</a></li>, kemudian pilih sesuai sistem operasi yang Anda gunakan, jika Anda menggunakan Windows, klik Windows"
				+ "<li>Install aplikasi \"OBS Studio\" di komputer Anda, ikuti langkah langkah install<br>"
				+ "<img height=\"250\" src='" + Common.getRequestHostWithProtocol() + "/img/obs_setup.jpg'/></li>"
				+ "<li>Setelah berhasil install, buka aplikasi \"OBS Studio\", kemudian klik Settings, klik Stream, input URL <b>"
				+ alamatStream + "</b>, setelah itu, masukkan Streaming dengan kode <b style='color:red'>" + kodeStream
				+ "</b>, selanjutnya klik OK.<br><img height=\"350\" src='" + Common.getRequestHostWithProtocol()
				+ "/img/input_stream_key_obs.jpg'/></li>"
				+ "<li>Kemudian ke menu Source, klik tanda plus (+), pilih display capture.<br><img height=\"350\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/display_capture.jpg'/></li>"
				+ "<li>Pastikan capture atau tampilan screen komputer Anda tampil seperti gambar di bawah ini.<br><img height=\"450\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/display_capture_tampil.jpg'/></li>"
				+ "<li>Kemudian klik tombol Start Streaming.<br><img height=\"450\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/start_recording.jpg'/></li>"
				+ "<li>Untuk hasil lebih baik, anda bisa menggunakan microphone headset (Headset with microphone) agar tidak ada suara noise.<br>"
				+ "<img height=\"200\" src='https://i5.walmartimages.com/asr/28574d3d-79ef-4a29-a3c7-70179a11697d_1.ff077d4b21d1c4efd0fd2d63fe9d04c8.jpeg'/></li>"
				+ "<li>Setelah semua selesai, klik tombol <b>Mulai Sekarang</b> di bawah ini</li>"
				+ "<li>Saat pertama kali menggunakan fitur Live Streaming, kemungkinan browser Anda akan memberikan informasi atau pertanyaan tentang penggunkana Flash, klik Allow (Izinkan). Kemudian kembali lagi ke menu e-learning dan halaman Live streaming."
				+ "<br><img height=\"250\" src='" + Common.getRequestHostWithProtocol()
				+ "/img/flash_info.jpg'/><br>Atau selalu izinkan flash dengan cara seperti gambar berikut<br><img height=\"250\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/diizinkan_flash.jpg'/></li>"
				+ "<li>Alamat lengkap RTMP <a target='_blank' href='" + liveUrl + "'>" + alamatLengkap + "</a></li>"
				+ "</ol>";

		Html html = new ais.ui.util.MyHtml(contentInfo);
		html.setWidth("100%");
		html.setParent(row);

		tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setParent(tabpanelUtama);
		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();

		row.setParent(rows);
		row.setValign("top");

		contentInfo = "<h3>Langkah-langkah memulai live streaming menggunakan laptop/Komputer yang terintegrasi dengan Webcam:</h3>"
				+ "<ol style='font-size:12px'>"
				+ "<li>Untuk dapat menggunakan live streaming, pertama Anda harus mengunduh aplikasi <i>RTMP Broadcaster</i>, contoh \"Adobe Flash Media Live Encoder\" di <a href=\"http://ecampus.id/flashmedialiveencoder_3.2_wwe_signed.msi\">sini</a></li>"
				+ "<li>Install aplikasi \"Adobe Flash Media Live Encoder\" di komputer Anda, ikuti langkah langkah install"
				+ "<br><img height=\"250\" src='" + Common.getRequestHostWithProtocol() + "/img/langkah1.jpg'/></li>"
				+ "<li>Setelah berhasil install, buka aplikasi \"Adobe Flash Media Live Encoder\", dan masukkan FMS URL <b>"
				+ alamatStream + "</b>, setelah itu, masukkan Streaming dengan kode <b style='color:red'>" + kodeStream
				+ "</b>, selanjutnya klik tombol Start di bawah untuk memulai live sreaming.<br><img height=\"450\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/langkah2.jpg'/></li>"
				+ "<li>Pastikan status di aplikasi \"Adobe Flash Media Live Encoder\" telah sukses.<br><img height=\"450\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/langkah3.jpg'/></li>"
				+ "<li>Untuk hasil lebih baik, anda bisa menggunakan microphone headset (Headset with microphone) agar tidak ada suara noise.<br>"
				+ "<img height=\"200\" src='https://i5.walmartimages.com/asr/28574d3d-79ef-4a29-a3c7-70179a11697d_1.ff077d4b21d1c4efd0fd2d63fe9d04c8.jpeg'/></li>"
				+ "<li>Setelah semua selesai, klik tombol <b>Mulai Sekarang</b> di bawah ini</li>"
				+ "<li>Saat pertama kali menggunakan fitur Live Streaming, kemungkinan browser Anda akan memberikan informasi atau pertanyaan tentang penggunkana Flash, klik Allow (Izinkan). Kemudian kembali lagi ke menu e-learning dan halaman Live streaming."
				+ "<br><img height=\"250\" src='" + Common.getRequestHostWithProtocol()
				+ "/img/flash_info.jpg'/><br>Atau selalu izinkan flash dengan cara seperti gambar berikut<br><img height=\"250\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/diizinkan_flash.jpg'/></li>"
				+ "<li>Alamat lengkap RTMP <a target='_blank' href='" + liveUrl + "'>" + alamatLengkap + "</a></li>"
				+ "</ol>";

		html = new ais.ui.util.MyHtml(contentInfo);
		html.setWidth("100%");
		html.setParent(row);

		tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setParent(tabpanelUtama);
		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();

		row.setParent(rows);
		row.setValign("top");

		contentInfo = "<h3>Langkah-langkah memulai live streaming menggunakan Android:</h3>"
				+ "<ol style='font-size:12px'>"
				+ "<li>Untuk dapat menggunakan live streaming, pertama Anda harus mengunduh aplikasi <i>RTMP Broadcaster</i> untuk Android, contoh \"RTMP Camera\" di <a target='_blank' href=\"https://play.google.com/store/apps/details?id=com.miv.rtmpcamera\">sini</a></li>"
				+ "<li>Install aplikasi \"RTMP Camera\" di Android Anda, ikuti langkah langkah install.</li>"
				+ "<li>Setelah berhasil install, buka aplikasi \"RTMP Camera\", kemudian buka menu pengaturan <i>(Options)</i>, pilih menu <i>Publish Address</i> dan masukkan <i>Stream Name</i> <b style='color:red'>"
				+ kodeStream + "</b>, setelah itu, masukkan <i>RTMP Server URL</i> dengan link <b>" + alamatStream
				+ "</b>, kemudian kembali ke menu utama.<br><img height=\"250\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/android_langkah_1.jpg'/></li>"
				+ "<li>Pilih menu <i>Start</i> di halaman utama.<br><img height=\"350\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/android_langkah_2.jpg'/></li>"
				+ "<li>Setelah semua selesai, klik tombol <b>Mulai Sekarang</b> di bawah ini, pastikan gambar Anda tampil di layar.<br><img height=\"350\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/android_langkah_3.jpg'/></li>"
				+ "<li>Saat pertama kali menggunakan fitur Live Streaming, kemungkinan browser Anda akan memberikan informasi atau pertanyaan tentang penggunkana Flash, klik Allow (Izinkan). Kemudian kembali lagi ke menu e-learning dan halaman Live streaming."
				+ "<br><img height=\"250\" src='" + Common.getRequestHostWithProtocol()
				+ "/img/flash_info.jpg'/><br>Atau selalu izinkan flash dengan cara seperti gambar berikut<br><img height=\"250\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/diizinkan_flash.jpg'/></li>"
				+ "<li>Alamat lengkap RTMP <a target='_blank' href='" + liveUrl + "'>" + alamatLengkap + "</a></li>"
				+ "</ol>";

		html = new ais.ui.util.MyHtml(contentInfo);
		html.setWidth("100%");

		html.setParent(row);

		return tabbox;
	}

	static {
		steams = new HashMap<String, Object[]>();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7041626862427552460L;

	private Pertemuan pertemuan;

	private EventListener eventListener;

	private String kodeStreamParam = null;

	public LiveStreamingPlayerWindow(Pertemuan pertemuan, EventListener eventListener, String kodeStreamParam)
			throws Exception {
		super();
		this.pertemuan = pertemuan;
		this.eventListener = eventListener;
		this.kodeStreamParam = kodeStreamParam;
		display();
	}

	public LiveStreamingPlayerWindow(Pertemuan pertemuan, EventListener eventListener, String kodeStreamParam,
			String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		this.pertemuan = pertemuan;
		this.eventListener = eventListener;
		this.kodeStreamParam = kodeStreamParam;
		display();
	}

	private void displayYoutube(Tabpanel tabpanelUtama) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);

		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		//
		// Columns columns = new Columns();
		// columns.setParent(grid);
		//
		// MyColumnConfig column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setWidth("60%");
		//
		// column = new MyColumnConfig();
		// column.setParent(columns);
		//
		Rows rows = new Rows();
		rows.setParent(grid);
		//
		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.setValign("top");
		//
		// Iframe iframe = new Iframe("https://www.youtube.com/webcam");
		// iframe.setHeight("640px");
		// iframe.setWidth("100%");
		// row.appendChild(iframe);

		String contentInfo = "<h3>Langkah-langkah memulai live streaming menggunakan Youtube:</h3>"
				+ "<ol style='font-size:12px'>"
				+ "<li>Buka atau klik alamat website <a target='_blank' href=\"https://www.youtube.com/webcam\">https://www.youtube.com/webcam</a></li>"
				+ "<li>Masukkan judul streaming seperti gambar berikut<br>" + "<img height=\"250\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/masukkan_judul.jpg'/>, klik Berikutnya</li>"
				+ "<li>Kemudian klik Bagikan / Share dan copy ID Streaming dan masukkan ke kode ID Streaming Youtube di Ecampus. ID Streaming bisa di dapatkan seperti gambar berikut<br><img height=\"450\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/masukkan_id_streaming.jpg'/></li>"
				+ "<li>Klik Go Live di halaman Streaming Youtube</li><li>Kembali lagi ke halaman ecampus, selanjutnya klik Mulai Sekarang, dan paste atau masukkan ID Streaming yang tadi di copy sebelumnya</li>"
				+ "</ol>";

		Html html = new ais.ui.util.MyHtml(contentInfo);
		html.setWidth("100%");
		html.setParent(row);

		final MyToolbarbuttonConfig mulaiSekarang = new MyToolbarbuttonConfig("Mulai Sekarang",
				"/img/Button-Play-icon_kecil.png");

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				mulaiSekarang.setVisible(false);
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(center);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				VideoPertemuan videoPertemuan = (VideoPertemuan) arg0.getData();

				String contentVideo = "<iframe style=\"width:98%;height:98%\" src=\"https://www.youtube.com/embed/"
						+ videoPertemuan.getYoutube().trim()
						+ "\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>";
				new ais.ui.util.MyHtml(contentVideo).setParent(center);
			}
		};

		South south = new South();
		south.setParent(borderlayout);

		Toolbar hbox = new Toolbar();
		hbox.setHeight("30px");
		hbox.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				LiveStreamingPlayerWindow.this.eventListener.onEvent(null);
				LiveStreamingPlayerWindow.this.detach();
			}
		});
		cancel.setParent(hbox);

		mulaiSekarang.setParent(hbox);

		mulaiSekarang.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow myWindow = new MyWindow("Tambah Video Youtube", "none", true);
				myWindow.setHeight("300px");
				myWindow.setWidth("450px");
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

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("30%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("70%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Masukkan Link Youtube")));
				final Textbox isi;
				row.appendChild(isi = new Textbox());
				isi.setValue("");
				isi.setRows(2);
				isi.setWidth("90%");
				isi.select();

				Common.initKeterangan(rows, "Contoh : https://www.youtube.com/watch?v=yYeSRrN40-Q");

				// row = new MyFormRow();
				//				// ;
				// row.setParent(rows);
				// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cara melihat ID live")));
				// Image img;
				// row.appendChild(img = new Image("/img/id_youtube_live.jpg"));
				// img.setWidth("100%");

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
				final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						try {
							String idY = YouTubeHelper.extractVideoIdFromUrl(isi.getValue().trim());
							System.out.println("idY => " + idY);
							if (!isi.getValue().trim().toLowerCase().contains("yout") && (idY == null
									|| idY.trim().isEmpty() || idY.trim().toLowerCase().startsWith("http"))) {
								MyMessageboxConfig.show(
										"Link YouTube yang Anda masukkan tidak sesuai, harap periksa kembali",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return;
							}

							VideoPertemuan videoPertemuan = new VideoPertemuan();
							videoPertemuan.setKeterangan("link");
							videoPertemuan.setType("link");
							videoPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
							videoPertemuan.setOleh(Common.getCurrentUser().getUserId());
							videoPertemuan.setNama("link");
							videoPertemuan.setYoutube(idY);

							if (pertemuan != null) {
								videoPertemuan.setPertemuan(pertemuan.getId());
								if (pertemuan.getPerkuliahan() != null
										&& pertemuan.getPerkuliahan().getJurusan() != null)
									videoPertemuan.setJurusan(pertemuan.getPerkuliahan().getJurusan().getId());
								videoPertemuan.setTahunAkademik(pertemuan.getPerkuliahan().getTahunAjaran());
							}

							Session session = StreamingHibernateUtil.getInstance().currentSession();
							session.getTransaction().begin();
							session.save(videoPertemuan);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();

							eventListener.onEvent(new Event("", save, videoPertemuan));
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
						myWindow.detach();
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(myWindow);
				myWindow.onModal();

			}
		});
	}

	private void displayFacebook(Tabpanel tabpanelUtama) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);

		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		//
		// Columns columns = new Columns();
		// columns.setParent(grid);
		//
		// MyColumnConfig column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setWidth("60%");
		//
		// column = new MyColumnConfig();
		// column.setParent(columns);
		//
		Rows rows = new Rows();
		rows.setParent(grid);
		//
		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.setValign("top");
		//
		// Iframe iframe = new Iframe("https://www.youtube.com/webcam");
		// iframe.setHeight("640px");
		// iframe.setWidth("100%");
		// row.appendChild(iframe);

		String contentInfo = "<h3>Langkah-langkah memulai live streaming menggunakan Facebook:</h3>"
				+ "<ol style='font-size:12px'>"
				+ "<li>Buka halaman facebook Anda, kemudian klik Siaran Langsung.<br><img height=\"250\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/siaran_langsung.jpg'/></li>"
				+ "<li>Masukkan deskripsi live streaming Anda, dan klik siarkan langsung.<br>"
				+ "<img height=\"250\" src='" + Common.getRequestHostWithProtocol()
				+ "/img/siarkan_langsung.jpg'/></li>"
				+ "<li>Buka halaman facebook yang baru, kemudian cari video live sreaming Anda, klik kanan dan tampilkan URL.<br>"
				+ "<img height=\"250\" src='" + Common.getRequestHostWithProtocol() + "/img/tampilkan_url.jpg'/></li>"
				+ "<li>Kemudian copy URL Streaming dan masukkan ke URL ID Streaming Facebook di Ecampus. URL Streaming bisa di dapatkan seperti gambar berikut<br><img height=\"450\" src='"
				+ Common.getRequestHostWithProtocol() + "/img/copy_url_facebook_lalu.jpg'/></li>"
				+ "<li>Klik Go Live di halaman Streaming Facebook</li><li>Kembali lagi ke halaman ecampus, selanjutnya klik Mulai Sekarang, dan paste atau masukkan URL Streaming yang tadi di copy sebelumnya</li>"
				+ "</ol>";

		Html html = new ais.ui.util.MyHtml(contentInfo);
		html.setWidth("100%");
		html.setParent(row);

		final MyToolbarbuttonConfig mulaiSekarang = new MyToolbarbuttonConfig("Mulai Sekarang",
				"/img/Button-Play-icon_kecil.png");

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				mulaiSekarang.setVisible(false);
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(center);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				VideoPertemuan videoPertemuan = (VideoPertemuan) arg0.getData();

				String contentVideo = "<iframe src=\"https://web.facebook.com/plugins/video.php?href="
						+ URLEncoder.encode(videoPertemuan.getFacebook().trim(), "UTF-8")
						+ "&show_text=0&height=315\" width=\"560\" "
						+ (Common.isMobile() ? "style=\"height:600px\"" : "height=\"460\"")
						+ " style=\"border:none;overflow:hidden\" scrolling=\"no\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"></iframe>";
				new ais.ui.util.MyHtml(contentVideo).setParent(center);
			}
		};

		South south = new South();
		south.setParent(borderlayout);

		Toolbar hbox = new Toolbar();
		hbox.setHeight("30px");
		hbox.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				LiveStreamingPlayerWindow.this.eventListener.onEvent(null);
				LiveStreamingPlayerWindow.this.detach();
			}
		});
		cancel.setParent(hbox);

		mulaiSekarang.setParent(hbox);

		mulaiSekarang.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow myWindow = new MyWindow("Tambah URL Facebook", "none", true);
				myWindow.setHeight("95%");
				myWindow.setWidth("750px");
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

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("30%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("70%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");

				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Masukkan URL Facebook")));
				final Textbox isi;
				row.appendChild(isi = new Textbox());
				isi.setValue("");
				isi.setWidth("90%");
				isi.select();

				row = new MyFormRow();

				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cara melihat dan copy URL Facebook live")));
				Image img;
				row.appendChild(img = new Image("/img/copy_url_facebook_lalu.jpg"));
				img.setWidth("100%");

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
				final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						try {
							VideoPertemuan videoPertemuan = new VideoPertemuan();
							videoPertemuan.setKeterangan("link");
							videoPertemuan.setType("link");
							videoPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
							videoPertemuan.setOleh(Common.getCurrentUser().getUserId());
							videoPertemuan.setNama("link");
							videoPertemuan.setFacebook(isi.getValue().trim());

							if (pertemuan != null) {
								videoPertemuan.setPertemuan(pertemuan.getId());
								if (pertemuan.getPerkuliahan() != null
										&& pertemuan.getPerkuliahan().getJurusan() != null)
									videoPertemuan.setJurusan(pertemuan.getPerkuliahan().getJurusan().getId());
								videoPertemuan.setTahunAkademik(pertemuan.getPerkuliahan().getTahunAjaran());
							}

							Session session = StreamingHibernateUtil.getInstance().currentSession();
							session.getTransaction().begin();
							session.save(videoPertemuan);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();

							eventListener.onEvent(new Event("", save, videoPertemuan));
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
						myWindow.detach();
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(myWindow);
				myWindow.onModal();

			}
		});
	}

	private void display() throws Exception {

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(this);

		setSizable(true);
		Common.clear(this);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		// MyTabConfig tabUmum = new MyTabConfig("Live Streaming Menggunakan
		// Ecampus");
		// tabUmum.setParent(tabs);

		final MyTabConfig tabDosen = new MyTabConfig("Live Streaming Menggunakan Youtube");
		tabDosen.setSelected(true);
		tabDosen.setParent(tabs);

		final MyTabConfig tabDosenFacebook = new MyTabConfig("Live Streaming Menggunakan Facebook");
		tabDosenFacebook.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		setPosition("center");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		// borderlayout.setParent(tabpanelUtama);

		// Tabpanel tabpanelKedua = new ais.ui.util.MyTabpanel();
		// tabpanelKedua.setParent(tabpanels);

		displayYoutube(tabpanelUtama);

		Tabpanel tabpanelKetiga = new ais.ui.util.MyTabpanel();
		tabpanelKetiga.setParent(tabpanels);

		displayFacebook(tabpanelKetiga);

		final East east = new East();
		east.setWidth("0px");
		borderlayout.appendChild(east);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(east);
		grid.setWidth("100%");
		grid.setHeight("100%");

		final Rows rowsPeserta = new Rows();
		rowsPeserta.setParent(grid);

		final String host = Common.getKonfigurasi("rtmp_server", "live.ecampus.id").getNilai();
		final String app = "live";
		final String kodeStream = kodeStreamParam == null || kodeStreamParam.trim().isEmpty()
				? (Common.simpleDateFormat1.get().format(ais.ui.util.WaktuUtil.getDate()) + "p" + pertemuan.getId())
				: kodeStreamParam;
		final String alamatStream = "rtmp://" + host + "/" + app;
		final String alamatLengkap = alamatStream + "/" + kodeStream;

		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		final MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Ikut Menjadi Peserta Konferensi Video",
				"/img/new.gif");
		tambah.setVisible(false);
		final MyToolbarbuttonConfig rekam = new MyToolbarbuttonConfig("Rekam Video", "/img/Record-Normal-icon.png");
		rekam.setVisible(false);
		final MyToolbarbuttonConfig akhiri = new MyToolbarbuttonConfig("Akhiri Konferensi Video", "/img/Stop-icon.png");
		akhiri.setVisible(false);
		final Label labelAtas = new Label(ais.common.Common.getBahasaConfig("Siap merekam .."));
		labelAtas.setVisible(false);

		final MyToolbarbuttonConfig mulaiSekarang = new MyToolbarbuttonConfig("Mulai Sekarang",
				"/img/Button-Play-icon_kecil.png");
		mulaiSekarang.setVisible(false);
		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				mulaiSekarang.setVisible(false);
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(center);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				if (Common.isMobile()) {
					String urlDownload = alamatStream + "/" + kodeStream;
					A a = new A("Mainkan video " + kodeStream, "/img/Button-Play-icon_rtmp.png");
					center.appendChild(a);
					a.setHref(urlDownload);
				} else {

					// String liveUrl = Common.getRequestHostWithProtocol() +
					// "/component/live/live.jsp?app="
					// + URLEncoder.encode(kodeStream, "UTF-8") + "&urlRtmp="
					// + URLEncoder.encode(alamatStream, "UTF-8");

					String liveUrl = Common.getRequestHostWithProtocol() + "/component/jw/vod.jsp?app="
							+ URLEncoder.encode(kodeStream, "UTF-8") + "&urlRtmp="
							+ URLEncoder.encode(alamatStream, "UTF-8") + "&height=540&autoplay=true";

					Iframe iframe = new Iframe(liveUrl);
					center.appendChild(iframe);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);
					A a = new A(alamatLengkap);
					a.setHref(liveUrl);
					a.setTarget("_blank");
					south.appendChild(a);
				}

				Session session = HibernateUtil.currentNativeSession();
				pertemuan.setPublikasikanStreaming(true);
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, pertemuan);
				session.getTransaction().commit();
				HibernateUtil.closeSession();

				Tbmuser tbmuser = Common.getCurrentUser();

				rekam.setVisible(true && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null
						&& Common.bolehKonfigurasi("aktifkan_rekam_video_manual", Konfigurasi.TIDAK_AKTIF));
				labelAtas.setVisible(rekam.isVisible());
				akhiri.setVisible(true && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null);
				tambah.setVisible(true);

				simpanVideo(kodeStream, host, pertemuan);
			}
		};

		final Label statusLive = new Label(ais.common.Common.getBahasaConfig("Check status.."));
		if (!steams.containsKey(kodeStream)) {

			langkahLangkah(alamatStream, kodeStream).setParent(center);

		} else {
			statusLive.setValue("Sukses");
			eventListener.onEvent(null);
		}

		final Set<String> datasPeserta = new HashSet<String>();

		final Timer timerPeserta = new Timer(2000);
		timerPeserta.setParent(this);
		timerPeserta.setRepeats(true);
		timerPeserta.addEventListener("onTimer", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				synchronized (steams) {
					Object[] o = steams.get(kodeStream);
					if (o != null) {

						List<String> dataTambahan = (List<String>) o[0];
						List<Label> dataTambahanLabel = (List<Label>) o[1];

						if (dataTambahan != null) {

							int index = 0;
							for (String kodeStream : dataTambahan) {
								if (!datasPeserta.contains(kodeStream)) {

									Label statusLive = dataTambahanLabel.get(index);
									index++;
									System.out.printf("kodeStream " + kodeStream + ", statusLive: %s\n",
											statusLive.getValue());

									east.setWidth("300px");

									// String str = " <script
									// type=\"text/javascript\" src=\""
									// +
									// Executions.getCurrent().getContextPath()
									// +
									// "/component/live/flowplayer-3.2.11.min.js\"></script>\n";
									// str += "<div id=\"" + kodeStream
									// + "\"
									// style=\"width:100%;height:240px;margin:0
									// auto;text-align:center\">\n"
									// + "</div>\n" + "<script>\n" + "$f(\"" +
									// kodeStream + "\", \""
									// +
									// Executions.getCurrent().getContextPath()
									// +
									// "/component/live/flowplayer-3.2.15.swf\",
									// {\n" + " clip: {\n"
									// + " url: '" + kodeStream + "',\n" + "
									// scaling: 'fit',\n"
									// + " live: true,\n" + " autoPlay: true,\n"
									// + " provider: 'hddn'\n"
									// + " },\n" + " plugins: {\n" + " hddn:
									// {\n"
									// + " url: \"" +
									// Executions.getCurrent().getContextPath()
									// +
									// "/component/live/flowplayer.rtmp-3.2.11.swf\",\n"
									// + " netConnectionUrl: '" + alamatStream +
									// "'\n" + " }\n"
									// + " },\n" + " canvas: {\n" + "
									// backgroundGradient: 'none'\n"
									// + " }\n" + "});\n" + "$f(\"" + kodeStream
									// + "\").play();\n"
									// + "</script>\n";
									//
									// System.out.println(str);

									MyFormRow row = new MyFormRow();row.setValign("top");

									row.setParent(rowsPeserta);

									Div div = new Div();
									div.setParent(row);

									String liveUrl = Common.getRequestHostWithProtocol()
											+ "/component/live/live.jsp?app=" + URLEncoder.encode(kodeStream, "UTF-8")
											+ "&urlRtmp=" + URLEncoder.encode(alamatStream, "UTF-8");

									Iframe iframe = new Iframe(liveUrl);
									iframe.setHeight("250px");
									iframe.setWidth("100%");
									div.appendChild(iframe);

									// Html html = new ais.ui.util.MyHtml(str);
									// html.setHeight("100%");
									// html.setWidth("100%");
									// html.setParent(div);

									String alamatLengkap = alamatStream + "/" + kodeStream;

									A a = new A(alamatLengkap);
									a.setHref(liveUrl);
									a.setTarget("_blank");
									div.appendChild(a);

									datasPeserta.add(kodeStream);
									simpanVideo(kodeStream, host, pertemuan);

								}
							}
						}
					}
				}

			}
		});
		timerPeserta.start();

		South south = new South();
		south.setParent(borderlayout);

		Toolbar hbox = new Toolbar();
		hbox.setHeight("30px");
		hbox.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				LiveStreamingPlayerWindow.this.eventListener.onEvent(null);
				LiveStreamingPlayerWindow.this.detach();
			}
		});
		cancel.setParent(hbox);

		mulaiSekarang.setParent(hbox);

		mulaiSekarang.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				statusLive.setValue("Sukses");
				synchronized (steams) {
					steams.put(kodeStream, new Object[] { new ArrayList<String>(), new ArrayList<Label>() });
				}
				eventListener.onEvent(null);
			}
		});

		rekam.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final MyWindow myWindow = new MyWindow("Rekam Video Live Streaming", "none", true);
				myWindow.setHeight("300px");
				myWindow.setWidth("550px");
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

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("30%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("70%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");

				if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
					row = new MyFormRow();

					row.setParent(rows);
					row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perkuliahan")));
					row.appendChild(new Label(pertemuan.info()));
				}

				row = new MyFormRow();

				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Topik pembahasan")));
				row.appendChild(new Label(pertemuan.getTopik()));

				if (pertemuan.getTanggal() != null) {

					row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Tanggal"));
					row.appendChild(new Label(Common.dateFormat6.get().format(pertemuan.getTanggal())));

				}

				row = new MyFormRow();

				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Deskripsi live streaming")));
				final Textbox isi;
				row.appendChild(isi = new Textbox());
				isi.setWidth("90%");
				isi.setRows(3);
				isi.select();

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Rekam", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						rekam.setDisabled(true);

						labelAtas.setValue("Siap merekam ..");
						File folder = CommonMedia.getMediaDirectory();
						final File newFile = new File(folder.getAbsolutePath() + "/"
								+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + ".mp4");
						System.out.println("newFile = " + newFile.getAbsolutePath());

						final Label label = new Label(ais.common.Common.getBahasaConfig("Siap merekam .."));

						final Timer timer = new Timer(500);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								labelAtas.setValue(label.getValue());
								if (label.getValue().equalsIgnoreCase("Error")) {
									timer.detach();
								} else if (label.getValue().isEmpty()) {

									rekam.setDisabled(false);

									timer.detach();
								}

							}
						});
						timer.start();

						new Thread(new Runnable() {

							@Override
							public void run() {

								try {
									VideoConverterPerkecil videoConverter = new VideoConverterPerkecil("ffmpeg");
									videoConverter.convert(alamatLengkap, newFile.getAbsolutePath(), label);

									try {
										Session session = StreamingHibernateUtil.getInstance().currentSession();

										VideoPertemuan videoPertemuan = (VideoPertemuan) session
												.createCriteria(VideoPertemuan.class)
												.add(Restrictions.eq("lokasiSimpan", newFile.getAbsolutePath()))
												.setMaxResults(1).uniqueResult();
										if (videoPertemuan == null) {
											videoPertemuan = new VideoPertemuan();
										}
										videoPertemuan.setLokasiSimpan(newFile.getAbsolutePath());
										videoPertemuan.setNama(newFile.getName());

										videoPertemuan.setPertemuan(pertemuan.getId());
										if (pertemuan.getPerkuliahan() != null) {
											if (pertemuan.getPerkuliahan() != null
													&& pertemuan.getPerkuliahan().getJurusan() != null)
												videoPertemuan
														.setJurusan(pertemuan.getPerkuliahan().getJurusan().getId());
											videoPertemuan
													.setTahunAkademik(pertemuan.getPerkuliahan().getTahunAjaran());
										} else {
											videoPertemuan.setTahunAkademik(Common.getCurrentTahunAkademik());
										}
										videoPertemuan.setKeteranganTambahan(isi.getValue());

										session.getTransaction().begin();
										session.save(videoPertemuan);
										session.getTransaction().commit();

										StreamingHibernateUtil.getInstance().closeSession();
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

								} catch (Exception e) {
									label.setValue("Error");
									Common.tampilErrorJikaAdmin(e);
									return;
								}
							}
						}).start();

						myWindow.detach();
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(myWindow);
				myWindow.onModal();
			}

		});

		tambah.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "deprecation" })
			@Override
			public void onEvent(Event event) throws Exception {

				Object[] o = steams.get(kodeStream);
				if (o != null) {

					List<String> dataTambahan = (List<String>) o[0];

					if (dataTambahan != null) {
						final MyWindow myWindow = new MyWindow("Ikut Peserta Konferensi Video", "none", true);
						myWindow.setHeight("95%");
						myWindow.setWidth("90%");
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

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig();
						column.setParent(columns);
						column.setWidth("30%");

						column = new MyColumnConfig();
						column.setParent(columns);
						column.setWidth("70%");

						Rows rows = new Rows();
						rows.setParent(grid);

						MyFormRow row = new MyFormRow();row.setValign("top");

						if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
							row = new MyFormRow();

							row.setParent(rows);
							row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perkuliahan")));
							row.appendChild(new Label(pertemuan.info()));
						}

						row = new MyFormRow();

						row.setParent(rows);
						row.appendChild(new Label(ais.common.Common.getBahasaConfig("Topik pembahasan")));
						row.appendChild(new Label(pertemuan.getTopik()));

						if (pertemuan.getTanggal() != null) {

							row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Tanggal"));
							row.appendChild(new Label(Common.dateFormat6.get().format(pertemuan.getTanggal())));

						}

						System.out.println("Size => " + dataTambahan.size());

						final String kodeSreamBaru = kodeStream + "_"
								+ (dataTambahan == null ? 1 : (1 + dataTambahan.size()));

						row = new MyFormRow();

						row.setParent(rows);
						row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode STREAM")));
						row.appendChild(new MyLabelBold(kodeSreamBaru));

						row = new MyFormRow();

						row.setParent(rows);
						ais.ui.util.ZkCompat.setSpans(row, "2");
						row.appendChild(langkahLangkah(alamatStream, kodeSreamBaru));

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
						MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Mulai Sekarang",
								"/img/Button-Play-icon_kecil.png");
						save.setTooltiptext("Simpan");
						save.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {

								Object[] o = steams.get(kodeStream);
								if (o != null) {

									List<String> dataTambahan = (List<String>) o[0];
									List<Label> dataTambahanLabel = (List<Label>) o[1];

									dataTambahan.add(kodeSreamBaru);
									dataTambahanLabel.add(new Label(ais.common.Common.getBahasaConfig("Siap..")));
								}

								// tambah.setVisible(false);

								myWindow.detach();
							}
						});
						save.setParent(toolbar);
						borderlayout.setParent(myWindow);
						myWindow.onModal();
					}
				} else {
					MyMessageboxConfig.show("Konferensi video belum siap", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
				}
			}

		});
		tambah.setParent(hbox);
		hbox.appendChild(new Space());

		akhiri.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin meng-akhiri konrefernsi video ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentNativeSession();
									pertemuan.setPublikasikanStreaming(false);
									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, pertemuan);
									session.getTransaction().commit();
									HibernateUtil.closeSession();

									rekam.setVisible(false);
									labelAtas.setVisible(false);
									akhiri.setVisible(false);

									synchronized (LiveStreamingPlayerWindow.steams) {
										for (String k : LiveStreamingPlayerWindow.steams.keySet()) {
											if (k.startsWith(kodeStream)) {
												LiveStreamingPlayerWindow.steams.remove(k);
											}
										}
									}

									LiveStreamingPlayerWindow.this.detach();
									LiveStreamingPlayerWindow.this.eventListener.onEvent(null);
								}

							}
						});

			}
		});

		akhiri.setParent(hbox);

		rekam.setParent(hbox);
		hbox.appendChild(new Space());
		hbox.appendChild(new Space());
		hbox.appendChild(new Space());
		hbox.appendChild(labelAtas);

		// final MyToolbarbuttonConfig keluar = new MyToolbarbuttonConfig(
		// "Batalkan publikasikan live streaming ke mahasiswa",
		// "/img/Stop-icon.png");
		// keluar.setVisible(pertemuan.getPublikasikanStreaming());
		// final MyToolbarbuttonConfig masuk = new
		// MyToolbarbuttonConfig("Publikasikan live streaming ke mahasiswa",
		// "/img/Start-icon.png");
		// masuk.setVisible(!pertemuan.getPublikasikanStreaming());
		// masuk.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// pertemuan.setPublikasikanStreaming(true);
		// Common.refreshSaveOrUpdate(pertemuan);
		// masuk.setVisible(!pertemuan.getPublikasikanStreaming());
		// keluar.setVisible(pertemuan.getPublikasikanStreaming());
		// }
		// });
		//
		// keluar.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// pertemuan.setPublikasikanStreaming(false);
		// Common.refreshSaveOrUpdate(pertemuan);
		// masuk.setVisible(!pertemuan.getPublikasikanStreaming());
		// keluar.setVisible(pertemuan.getPublikasikanStreaming());
		// }
		// });
		//
		// masuk.setParent(hbox);
		// keluar.setParent(hbox);
	}

}
