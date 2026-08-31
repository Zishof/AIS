package ais.common;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Row;
import org.zkoss.zul.Toolbarbutton;

import ais.database.model.GeneralValueObject;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PengumumanPerkuliahan;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyToolbarbuttonConfig;

// Asumsi import kelas utilitas internal Anda ada di sini (Common, MyToolbarbuttonConfig, dll)

/**
 * Helper terfokus untuk url display. Tipe ini membungkus satu variasi kecil dari alur yang lebih
 * umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String CACHE_DIR}; pembacaan/pencarian
 * ({@code getStyleContent()}, {@code getStyleContent1()}, {@code getStyleContentAudio()}, {@code
 * getStyleContentHeighMaxKecil()}, {@code getStyleContentHeighMax()}, {@code createDownloadButton()}); mutasi
 * data ({@code setContainerMinHeight()}); operasi domain lain ({@code isProtectedEcampusLampiranUrl()}, {@code
 * escapeAttr()}, {@code displayUrlContent()}, {@code handleGoogleDocs()}, {@code handleGoogleBooks()}, {@code
 * extractGoogleBookId()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class UrlDisplayHelper {

	private static final String CACHE_DIR = "/opt/ecampus/";

	private static boolean isProtectedEcampusLampiranUrl(String url) {
		if (url == null) {
			return false;
		}
		String lower = url.trim().toLowerCase();
		return lower.contains("/al?d=") || lower.contains("ambillampiran");
	}

	private static String escapeAttr(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}
	
	public static String getStyleContent() {
		String s = "style=\"min-height: 430px;min-width: 360px;width:100%;height:95%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		if (Common.isMobile()) {
			s = "style=\"min-height: 420px;min-width: 260px;width:100%;height:95%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		}
		return s;
	}

	public static String getStyleContent1() {
		String s = "style=\"min-height: 530px;min-width: 360px;width:100%;height:95%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		if (Common.isMobile()) {
			s = "style=\"min-height: 520px;min-width: 260px;width:100%;height:95%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		}
		return s;
	}

	public static String getStyleContentAudio() {
		String s = "style=\"min-height: 72px;min-width: 560px;width:100%;height:95%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		if (Common.isMobile()) {
			s = "style=\"min-height: 72px;min-width: 260px;width:100%;height:95%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		}
		return s;
	}
	
	public static String getStyleContentHeighMaxKecil() {
		String s = "style=\"min-height: 430px;max-height: 430px;min-width: 260px;width:100%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		if (Common.isMobile()) {
			s = "style=\"min-height: 420px;max-height: 420px;min-width: 260px;width:100%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		}
		return s;
	}

	public static String getStyleContentHeighMax() {
		String s = "style=\"min-height: 430px;max-height: 430px;min-width: 560px;width:100%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		if (Common.isMobile()) {
			s = "style=\"min-height: 420px;max-height: 420px;min-width: 260px;width:100%;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
		}
		return s;
	}

	public static void displayUrlContent(String u, Component vboxa) {
		if (u == null || u.trim().isEmpty()) {
			return;
		}

		// Setup container
		Row vbox = (Row) ((vboxa instanceof Row) ? vboxa : Common.tampilanScroll2(vboxa));
		if (vbox.getGrid() != null) {
			vbox.getGrid().setWidth("99%");
		}

		String urlLower = u.toLowerCase().trim();
		String urlTrim = u.trim();

		try {
			if (urlLower.contains("docs.google.com")) {
				handleGoogleDocs(urlTrim, vbox);
			} else if (urlLower.contains("dropbox")) {
				handleDropbox(urlTrim, urlLower, vbox);
			} else if (urlLower.contains("instagram")) {
				handleOembed(urlTrim, vbox, "instagram", "https://api.instagram.com/oembed?url=");
			} else if (urlLower.contains("twitter")) {
				handleOembed(urlTrim, vbox, "twitter", "https://publish.twitter.com/oembed?url=");
			} else if (urlLower.contains("facebook")) {
				handleFacebook(urlTrim, urlLower, vbox);
			} else if (urlLower.endsWith(".mp3")) {
				handleAudio(urlTrim, vbox);
			} else if (urlLower.startsWith("https://drive.google.com")) {
				handleGoogleDrive(urlTrim, urlLower, vbox);
			} else if (urlLower.contains("youtu")) { // youtube or youtu.be
				handleYoutube(urlTrim, vbox);
			} else if (urlLower.contains("books.google") || urlLower.contains("google.com/books")) {
				// Buku Google Books → ditanam (embed) langsung memakai Google Books
				// Embedded Viewer API, bukan sekadar tautan yang membuka jendela baru.
				handleGoogleBooks(urlTrim, vbox);
			} else if (LampiranLain.merupakanDokumen(urlTrim)) {
				handleGenericDocument(urlTrim, urlLower, vbox);
			} else if (LampiranLain.merupakanGambar(urlTrim)) {
				handleGenericImage(urlTrim, vbox);
			} else {
				// Tautan umum (mis. tugas mahasiswa "berupa link") yang tidak cocok kategori
				// khusus di atas. Sebelumnya cabang ini KOSONG sehingga popup tampil BLANK.
				handleGenericUrl(urlTrim, vbox);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/UrlDisplayHelper.java:124"); // Log error dengan benar di production
		}
	}

	// --- Specific Handlers ---

	private static void handleGoogleDocs(final String u, Component container) {
		if (Common.isMobile()) {
			createDownloadButton("Buka Dokumen", "/img/google-docs-icon.png", u, container);
		} else {
			String content = "<iframe src=\"" + u + "\" " + getStyleString(430, 360) + "></iframe>";
			addHtmlComponent(content, container, 470, 560);
		}
	}

	/**
	 * Menanam (embed) buku Google Books langsung ke dalam halaman memakai
	 * <b>Google Books Embedded Viewer API</b> ({@code https://www.google.com/books/jsapi.js}).
	 *
	 * <p><b>Mengapa bukan {@code <iframe>} biasa?</b> Halaman {@code books.google.com/books?id=...}
	 * mengirim header {@code X-Frame-Options: SAMEORIGIN} sehingga TIDAK dapat ditanam via iframe
	 * (akan tampil kosong / diblokir browser). Satu-satunya cara resmi menampilkan pratinjau buku
	 * di situs lain adalah Embedded Viewer API: memuat {@code jsapi.js}, memanggil
	 * {@code google.books.load()}, lalu membuat {@code google.books.DefaultViewer} pada sebuah
	 * elemen {@code <div>}.</p>
	 *
	 * <p><b>Mengapa memakai {@code <iframe srcdoc>} dan bukan {@code Clients.evalJavaScript}?</b>
	 * Tag {@code <script>} yang disisipkan via {@code innerHTML} (komponen {@code Html} ZK) TIDAK
	 * dieksekusi browser. Pendekatan awal menjalankan skrip via {@code Clients.evalJavaScript}, namun
	 * itu bergantung pada timing render ZK: pada pemuatan PERTAMA setelah restart Tomcat, skrip
	 * kadang berjalan sebelum elemen ada di DOM / sebelum layout settle, sehingga viewer melapor
	 * {@code notfound} dan buku "tidak keluar" sampai tombol Refresh ditekan. Karena itu seluruh
	 * dokumen viewer (termasuk {@code <script>}-nya) dibungkus dalam {@code <iframe srcdoc>}: skrip di
	 * dalam {@code srcdoc} PASTI dieksekusi browser begitu iframe terpasang, independen dari siklus
	 * AU ZK — sehingga buku tampil otomatis pada pemuatan pertama tanpa perlu Refresh.</p>
	 *
	 * <p><b>Alur kerja skrip (di dalam srcdoc):</b></p>
	 * <ol>
	 *   <li>Memuat {@code jsapi.js} (tag {@code <script src>} di {@code <head>} dokumen srcdoc).</li>
	 *   <li>Polling sampai {@code google.books} siap, lalu {@code load()} + {@code setOnLoadCallback}.</li>
	 *   <li>Membuat {@code DefaultViewer} pada {@code div} dan memuat buku berdasar ID.</li>
	 *   <li>Bila viewer melapor {@code notfound}, RETRY beberapa kali (notfound transien umum pada
	 *       percobaan pertama); setelah batas retry, menampilkan pesan ramah + tautan cadangan.</li>
	 * </ol>
	 *
	 * <p>Bila ID buku tidak dapat dideteksi dari URL, fungsi jatuh-balik (fallback) ke
	 * {@link #handleGenericUrl(String, Component)} sehingga perilaku lama (membuka tautan)
	 * tetap tersedia dan tidak ada regresi.</p>
	 *
	 * @param u         URL buku Google Books (mis. {@code https://books.google.co.id/books?id=8IM8EAAAQBAJ&...}).
	 * @param container komponen induk (Row) tempat viewer ditanam.
	 */
	private static void handleGoogleBooks(final String u, Component container) {
		String bookId = extractGoogleBookId(u);
		if (bookId == null || bookId.isEmpty()) {
			// Tidak bisa mengenali ID buku → pakai penanganan tautan umum (perilaku lama).
			handleGenericUrl(u, container);
			return;
		}

		boolean mobile = Common.isMobile();
		int tinggi = mobile ? 480 : 620;

		// Dokumen MANDIRI (di dalam iframe srcdoc) yang memuat Google Books Embedded Viewer API.
		// Skrip di dalam srcdoc PASTI dieksekusi browser saat iframe terpasang ke DOM — termasuk
		// pada pemuatan halaman PERTAMA setelah restart Tomcat — sehingga buku tampil otomatis
		// tanpa perlu menekan Refresh. Skrip juga me-RETRY beberapa kali bila viewer sempat
		// melapor notfound transien (umum pada percobaan pertama sebelum layout settle).
		StringBuilder doc = new StringBuilder();
		doc.append("<!doctype html><html><head><meta charset='utf-8'>");
		doc.append("<script src='https://www.google.com/books/jsapi.js'></script>");
		doc.append("<style>html,body{margin:0;height:100%;font-family:Arial,Helvetica,sans-serif}");
		doc.append("#gbv{width:100%;height:100%}.gbmsg{padding:24px;color:#64748b;font-size:13px}</style></head>");
		doc.append("<body><div id='gbv'></div><script>");
		doc.append("var attempt=0;");
		doc.append("function pesanGagal(){var e=document.getElementById('gbv');if(e){e.className='gbmsg';");
		doc.append("e.innerHTML='Pratinjau buku tidak tersedia untuk publik. Silakan gunakan tautan di atas.';}}");
		doc.append("function muat(){var e=document.getElementById('gbv');if(!e){return;}e.innerHTML='';try{");
		doc.append("var v=new google.books.DefaultViewer(e);v.load('").append(bookId).append("',function(){");
		doc.append("if(attempt++<3){setTimeout(muat,800);}else{pesanGagal();}});}catch(x){");
		doc.append("if(attempt++<3){setTimeout(muat,800);}else{pesanGagal();}}}");
		doc.append("function go(){try{google.books.load();google.books.setOnLoadCallback(muat);}catch(x){pesanGagal();}}");
		doc.append("if(window.google&&window.google.books){go();}else{var t=0,iv=setInterval(function(){");
		doc.append("if(window.google&&window.google.books){clearInterval(iv);go();}");
		doc.append("else if(t++>60){clearInterval(iv);pesanGagal();}},150);}");
		doc.append("</script></body></html>");
		// Aman untuk nilai atribut srcdoc (dibungkus tanda kutip ganda): escape & dan ".
		String srcdoc = doc.toString().replace("&", "&amp;").replace("\"", "&quot;");

		StringBuilder html = new StringBuilder();
		html.append("<div style=\"width:100%;\">");
		// Tautan cadangan diletakkan DI ATAS viewer agar konsisten dengan pesan "gunakan tautan di atas".
		html.append("<div style=\"margin:0 0 6px;font-size:12px;\">");
		html.append("<a href=\"").append(escapeHtmlAttr(u)).append("\" target=\"_blank\" rel=\"noopener noreferrer\" ");
		html.append("style=\"color:var(--theme-primary,#1a4087);text-decoration:none;font-weight:600;\">");
		html.append("Buka di Google Books &raquo;</a></div>");
		html.append("<iframe srcdoc=\"").append(srcdoc).append("\" ");
		html.append("style=\"width:100%;height:").append(tinggi).append("px;border:1px solid var(--theme-border,#e2e8f0);");
		html.append("border-radius:10px;background:var(--theme-light,#f1f5f9);\" frameborder=\"0\" scrolling=\"no\"></iframe>");
		html.append("</div>");
		addHtmlComponent(html.toString(), container, tinggi + 44, 0);
	}

	/**
	 * Mengekstrak ID buku Google Books dari URL. ID adalah nilai parameter kueri {@code id}
	 * (mis. {@code 8IM8EAAAQBAJ}). Mengembalikan {@code null} bila tidak ditemukan.
	 *
	 * @param u URL buku.
	 * @return ID buku, atau {@code null} bila tidak terdeteksi.
	 */
	private static String extractGoogleBookId(String u) {
		if (u == null) {
			return null;
		}
		try {
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("[?&]id=([A-Za-z0-9_-]+)").matcher(u);
			if (m.find()) {
				return m.group(1);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/UrlDisplayHelper.java:243");
			// abaikan — kembalikan null sebagai penanda gagal deteksi
		}
		return null;
	}

	/**
	 * Meng-escape teks agar aman dipakai sebagai nilai atribut HTML (mis. {@code href}).
	 *
	 * @param s teks mentah.
	 * @return teks ter-escape ({@code &}, {@code "}, {@code <}, {@code >}).
	 */
	private static String escapeHtmlAttr(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static void handleDropbox(final String u, String urlLower, Component container) {
		// Convert Dropbox link to raw/direct link
		String rawUrl = StringUtils.replace(StringUtils.replace(u, "dl=0", "raw=1"), "dl=1", "raw=1");

		if (urlLower.contains(".mp4")) {
			String html = "<video " + getStyleString(430, 560) + " controls><source src=\"" + rawUrl
					+ "\" type=\"video/mp4\" /></video>";
			addHtmlComponent(html, container, 470, 560);

		} else if (urlLower.contains(".mp3")) {
			String html = "<audio controls style='width:100%'><source src=\"" + rawUrl
					+ "\" type=\"audio/mpeg\">Browser not supported</audio>";
			addHtmlComponent(html, container, 325, 0);

		} else if (urlLower.matches(".*\\.(jpg|jpeg|png|gif).*")) {
			Image image = new Image(rawUrl);
			image.setAttribute("lampiran_tambahan", true);

			if (!Common.isMobile()) {
				image.setHeight("350px");
				setContainerMinHeight(container, 300);
			} else {
				image.setWidth("100%");
			}
			container.appendChild(image);

		} else {
			Toolbarbutton btn = new MyToolbarbuttonConfig("Klik di-sini untuk lihat " + u, FileFoto.icon("dropbox"));
			btn.setStyle("font-size:8px");
			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					openUrl(u, "Dropbox");
				}
			});
			btn.setParent(container);
			setContainerMinHeight(container, 35);
		}
	}

	private static void handleOembed(String u, Component container, String platform, String apiUrl) {
		try {
			boolean mobile = Common.isMobile();
			String fileHash = URLEncoder.encode(MD5.crypt(u), "UTF-8");
			File fileCache = new File(CACHE_DIR + platform + "_" + fileHash + ".html");

			// Ensure directory exists
			if (!fileCache.getParentFile().exists()) {
				fileCache.getParentFile().mkdirs();
			}

			// Fetch if not exists or empty
			if (!fileCache.exists() || fileCache.length() == 0) {
				String reqUrl = apiUrl + URLEncoder.encode(u, "UTF-8");
				if ("instagram".equals(platform)) {
					reqUrl += "&maxwidth=" + (mobile ? "320" : "550") + "&hidecaption=true";
				} else if ("twitter".equals(platform)) {
					reqUrl += "&conversation=none";
				}

				try {
					FileUtils.copyURLToFile(new URL(reqUrl), fileCache);
					// Parse JSON and resave only HTML content
					String jsonText = ais.common.BacaTulisUtil.baca(fileCache);
					JSONObject o = new JSONObject(jsonText);
					ais.common.BacaTulisUtil.tulis(fileCache, o.getString("html"));
				} catch (Exception ex) {
					// Fail silently or log
					return;
				}
			}

			String text = ais.common.BacaTulisUtil.baca(fileCache);
			if (text == null || text.isEmpty())
				return;

			text = text.replaceAll("\"", "'");

			// Script untuk resize iframe otomatis sering diperlukan untuk oembed
			String minWidth = mobile ? "min-width:330px;" : "min-width:570px;";
			String htmlContent = "<iframe srcdoc=\"" + text + "\" style='width:100%;min-height:610px;height:100%;"
					+ minWidth + "' "
					+ "frameborder=\"0\" scrolling=\"no\" onload=\"resizeIframe(this)\" allowtransparency=\"true\"></iframe>";

			addHtmlComponent(htmlContent, container, 630, 0);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/UrlDisplayHelper.java:349");
		}
	}

	private static void handleFacebook(String u, String urlLower, Component container) throws Exception {
		if (urlLower.startsWith("http") && urlLower.contains("video")) {
			// Facebook Video Embed
			String src = "https://web.facebook.com/plugins/video.php?href=" + URLEncoder.encode(u, "UTF-8")
					+ "&show_text=0&height=470";
			String style = "border:none;overflow:hidden;width:100%;min-height: 470px;min-width: 560px;";
			String content = "<iframe src=\"" + src + "\" style=\"" + style
					+ "\" scrolling=\"no\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"></iframe>";

			// Note: Original code used "new Html().setParent()", here we standardize
			addHtmlComponent(content, container, 470, 560);
		} else {
			// Facebook Post Embed via SDK (Simplified)
			boolean mobile = Common.isMobile();
			String width = mobile ? "320" : "550";
			String c = "<html><head><script async defer src='https://connect.facebook.net/en_US/sdk.js#xfbml=1&version=v3.2'></script></head>"
					+ "<body><div class='fb-post' data-href='" + u + "' data-width='" + width
					+ "'></div></body></html>";

			String iframe = "<iframe srcdoc=\"" + c + "\" style='width:100%;min-height:610px;height:100%;"
					+ (mobile ? "min-width:330px;" : "min-width:570px;")
					+ "' frameborder=\"0\" scrolling=\"no\" onload=\"resizeIframe(this)\"></iframe>";
			addHtmlComponent(iframe, container, 630, 0);
		}
	}

	private static void handleAudio(String u, Component container) {
		String html = "<audio controls><source src=\"" + u
				+ "\" type=\"audio/mpeg\">Your browser does not support the audio element.</audio>";
		addHtmlComponent(html, container, 325, 0);
	}

	private static void handleGoogleDrive(String u, String urlLower, Component container) {
		String cleanId = "";
		String srcUrl = "";

		if (urlLower.contains("/folders")) {
			cleanId = StringUtils.substringAfter(u, "folders/");
			cleanId = StringUtils.split(cleanId, "&")[0]; // Safe split
			cleanId = StringUtils.split(cleanId, "?")[0];
			srcUrl = "https://drive.google.com/embeddedfolderview?id=" + cleanId + "#list";
		} else if (urlLower.contains("open?id=")) {
			cleanId = StringUtils.substringAfter(u, "id=");
			cleanId = StringUtils.split(cleanId, "&")[0];
			srcUrl = "https://drive.google.com/file/d/" + cleanId + "/preview";
		} else if (urlLower.contains("/file/d/")) {
			cleanId = StringUtils.substringAfter(u, "/file/d/");
			cleanId = StringUtils.split(cleanId, "/")[0];
			srcUrl = "https://drive.google.com/file/d/" + cleanId + "/preview";
		}

		if (!srcUrl.isEmpty()) {
			String content = "<iframe src=\"" + srcUrl + "\" " + getStyleString(430, 360) + "></iframe>";
			addHtmlComponent(content, container, 470, 560);
		}
	}

	private static void handleYoutube(String u, Component container) {
		try {
			String embedUrl = YouTubeHelper.convertoToEmbed(u); // Asumsi kelas ini ada
			addHtmlComponent(embedUrl, container, 315, 560);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/UrlDisplayHelper.java:415");
		}
	}

	private static void handleGenericDocument(String u, String urlLower, Component container) throws Exception {
		if (urlLower.endsWith("pdf") && !Common.isMobile()) {
			Iframe iframe = new Iframe(u);
			iframe.setHeight("400px");
			iframe.setWidth("90%");
			iframe.setStyle("border:none");
			iframe.setAttribute("lampiran_tambahan", true);
			container.appendChild(iframe);
			setContainerMinHeight(container, 410);
		} else if (isProtectedEcampusLampiranUrl(u)) {
			String html = "<div style='margin:8px 0;padding:12px 14px;font-family:Arial,sans-serif;"
					+ "color:#334155;background:#f8fafc;border:1px solid #cbd5e1;border-radius:8px;line-height:1.45;'>"
					+ "<b>Preview dokumen Office tidak tersedia di sini.</b><br/>"
					+ "Berkas ini dilindungi login eCampus, sehingga Google Viewer tidak dapat membacanya dengan aman."
					+ "<div style='margin-top:8px;'><a href='" + escapeAttr(u)
					+ "' target='_blank' rel='noopener noreferrer' "
					+ "style='display:inline-block;padding:6px 10px;border-radius:4px;background:#1d4ed8;"
					+ "color:#fff;text-decoration:none;font-weight:600;'>Buka / unduh lewat eCampus</a></div>"
					+ "</div>";
			Html info = new ais.ui.util.MyHtml(html);
			info.setAttribute("lampiran_tambahan", true);
			container.appendChild(info);
			setContainerMinHeight(container, 110);
		} else {
			// Google Docs Viewer fallback for other docs or mobile PDF
			String gViewUrl = "https://docs.google.com/gview?embedded=true&url=" + URLEncoder.encode(u, "UTF-8");
			Iframe iframe = new Iframe(gViewUrl);
			iframe.setHeight("400px");
			iframe.setWidth("90%");
			iframe.setStyle("border:none");
			iframe.setAttribute("lampiran_tambahan", true);
			container.appendChild(iframe);
			setContainerMinHeight(container, 410);
		}
	}

	private static void handleGenericImage(String u, Component container) {
		org.zkoss.zul.Image image = new org.zkoss.zul.Image(u);
		image.setHeight("300px");
		image.setAttribute("lampiran_tambahan", true);
		image.setParent(container);
		setContainerMinHeight(container, 310);
	}

	/**
	 * Menampilkan tautan UMUM yang tidak termasuk kategori khusus (Google Docs/Drive,
	 * Dropbox, YouTube, media sosial, dokumen, atau gambar).
	 *
	 * <p><b>Latar belakang.</b> Tugas mahasiswa "berupa link" sering berisi URL biasa
	 * (blog, portal berita, situs pribadi, dsb.). Sebelumnya cabang fallback ini dibiarkan
	 * kosong sehingga popup "Link" tampil putih/blank. Method ini menampilkan: (1) tautan
	 * yang dapat <b>diklik</b> (membuka tab baru) agar selalu bisa diakses meski situs
	 * tujuan menolak di-embed, dan (2) <b>pratinjau</b> halaman via {@code <iframe>} bila
	 * situs tujuan mengizinkannya.</p>
	 *
	 * @param u         URL tautan yang dikirim
	 * @param container komponen induk tempat konten ditambahkan
	 */
	private static void handleGenericUrl(String u, Component container) {
		String aman = escHtml(u);
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"padding:14px 16px;font-family:Arial,sans-serif;\">");
		sb.append("<div style=\"font-weight:bold;margin-bottom:8px;color:#0f172a;\">Tautan yang dikirim:</div>");
		sb.append("<div style=\"margin-bottom:14px;word-break:break-all;\">");
		sb.append("<a href=\"").append(aman).append("\" target=\"_blank\" rel=\"noopener noreferrer\" ")
				.append("style=\"color:#2563eb;text-decoration:underline;font-size:14px;\">").append(aman).append("</a>");
		sb.append("</div>");
		sb.append("<iframe src=\"").append(aman).append("\" ")
				.append("style=\"width:100%;height:62vh;min-height:420px;border:1px solid #e2e8f0;border-radius:8px;\" ")
				.append("referrerpolicy=\"no-referrer\"></iframe>");
		sb.append("<div style=\"margin-top:8px;color:#64748b;font-size:11px;\">")
				.append("Jika pratinjau di atas kosong, situs tujuan tidak mengizinkan ditampilkan di dalam halaman; ")
				.append("silakan klik tautan untuk membukanya di tab baru.</div>");
		sb.append("</div>");
		addHtmlComponent(sb.toString(), container, 460, 0);
	}

	/** Meng-escape karakter HTML berbahaya agar URL aman ditempel ke markup. */
	private static String escHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	// --- Helper Methods ---

	private static void addHtmlComponent(String content, Component container, int minH, int minW) {
		Html html = new Html(content);
		html.setAttribute("lampiran_tambahan", true);
		html.setParent(container);

		if (container instanceof HtmlBasedComponent) {
			String style = "min-height: " + minH + "px;";
			if (!Common.isMobile() && minW > 0) {
				style += "min-width: " + minW + "px;";
			}
			((HtmlBasedComponent) container).setStyle(style);
		}
	}

	private static void setContainerMinHeight(Component container, int minH) {
		if (container instanceof HtmlBasedComponent) {
			((HtmlBasedComponent) container).setStyle("min-height: " + minH + "px;");
		}
	}

	private static void createDownloadButton(String label, String icon, final String url, Component container) {
		Toolbarbutton btn = new MyToolbarbuttonConfig(label, icon);
		btn.setParent(container);
		btn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
			}
		});
	}

	public static void openUrl(String url, String title) {
		if (Common.isMobile()) {
			ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
		} else {
			Clients.evalJavaScript("popupCenter({url: '" + url + "', title: '" + title + "', w: 1200, h: 600});");
		}
	}

	// Penyerdehanaan Style Generator
	public static String getStyleString(int minHeight, int minWidth) {
		boolean mobile = Common.isMobile();
		int finalH = mobile ? (minHeight - 10) : minHeight;
		int finalW = mobile ? (minWidth - 100) : minWidth; // Logic kasar menyesuaikan original
		if (finalW < 260)
			finalW = 260;

		return "style=\"min-height: " + finalH + "px;" + "min-width: " + finalW + "px;width:100%;height:95%;\" "
				+ "scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"";
	}

	// --- Tampil Online & Video Conference ---

	public static void tampilOnline(GeneralValueObject pertemuan, Component vbox) {
		try {
			TreeMap<String, String> d = pertemuan.ambilData("online", null);
			if (d != null && !d.isEmpty()) {
				StringBuilder onl = new StringBuilder();
				for (String user : d.keySet()) {
					try {
						String jam = d.get(user);
						String[] u = user.split("-");
						if (onl.length() > 0)
							onl.append(",");
						// Pastikan array u memiliki cukup elemen
						String nama = (u.length > 0) ? u[0] : "Unknown";
						String nim = (u.length > 2) ? u[2] : "";
						onl.append(nama).append(" / ").append(nim).append(" (").append(jam).append(")");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/UrlDisplayHelper.java:561");
						// ignore bad formatting for one user
					}
				}

				MyLabelKecilBold s = new MyLabelKecilBold("Online : " + onl.toString());
				s.setStyle("font-size:8px;font-weight: bolder;color:blue");
				vbox.appendChild(s);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/UrlDisplayHelper.java:571");
		}
	}

	public static Button createVideoConference(final GeneralValueObject generalValueObject, Component hbox,
			boolean vertical, boolean isButton, final EventListener externalListener) throws Exception {

		final Button btn = isButton ? new MyButtonConfig("Online", "/img/svg/user-group.svg")
				: new MyToolbarbuttonConfig("Online", "/img/svg/user-group.svg");

		if (vertical)
			btn.setOrient("vertical");
		btn.setParent(hbox);

		final String hangoutLink = generalValueObject.retreive("hangoutLink");

		if (hangoutLink != null && !hangoutLink.trim().isEmpty()) {
			btn.setImage("/img/meet-google.png");
			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {
					generalValueObject.masukkanData("online");
					String server = hangoutLink + (hangoutLink.contains("?") ? "&" : "?") + "hs=122&ijlm="
							+ System.currentTimeMillis();
					openUrl(server, "Video Conference");
					if (externalListener != null)
						externalListener.onEvent(null);
				}
			});
		} else {
			// Jitsi / Default Logic
			TreeMap<String, String> d = generalValueObject.ambilData("online", null);
			if (d != null && d.size() > 0) {
				btn.setImage("/img/online-red-icon.png");
			}

			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {
					generalValueObject.masukkanData("online");
					btn.setImage("/img/online-red-icon.png");

					String id = generalValueObject.getNama() + "_" + generalValueObject.getId();
					// Casting check logic retained from original
					if (generalValueObject instanceof PengumumanAkademis) {
						id = ((PengumumanAkademis) generalValueObject).getJudul() + "_" + generalValueObject.getId();
					} else if (generalValueObject instanceof PengumumanPerkuliahan) {
						id = ((PengumumanPerkuliahan) generalValueObject).getJudul() + "_" + generalValueObject.getId();
					}

					String contextPath = Common.ROOT; // Cara ZK standar ambil context path
					// Jika ExecutionsCtrl.getCurrent().getNativeRequest() wajib digunakan:
					// HttpServletRequest req = (HttpServletRequest)
					// ExecutionsCtrl.getCurrent().getNativeRequest();
					// String contextPath = req.getContextPath();

					String prefix = URLEncoder.encode(StringUtils.replace(contextPath, "/", ""), "UTF-8");
					String rawStreamCode = prefix + "_" + id;

					// Sanitize stream code
					String kodeStream = rawStreamCode.replaceAll("[^a-zA-Z0-9 ]", "_").toLowerCase().trim()
							.replaceAll("\\s+", "_");
					while (kodeStream.contains("__")) {
						kodeStream = kodeStream.replace("__", "_");
					}

					String baseServer = Common.getKonfigurasi("alamat_server_video_conference", "https://meet.jit.si")
							.getNilai();
					String server = baseServer + (baseServer.endsWith("/") ? "" : "/") + kodeStream;

					openUrl(server, "Video Conference");

					if (externalListener != null)
						externalListener.onEvent(null);
				}
			});
		}
		return btn;
	}
}
