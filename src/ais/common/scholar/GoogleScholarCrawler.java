package ais.common.scholar;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.docear.metadata.data.MetaData;
import org.docear.metadata.data.ScholarMetaData.ScholarSource;
import org.docear.metadata.events.CaptchaEvent;
import org.docear.metadata.events.MetaDataListener;
import org.docear.metadata.extractors.HtmlDataExtractor;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.common.GzipUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ScholarArticle;
import ais.database.model.ScholarAuthor;

/**
 * Web scraper (bukan pemanggil API resmi) untuk halaman hasil pencarian Google Scholar
 * ({@code https://scholar.google.com/scholar}), dipakai modul kepustakaan/publikasi AIS untuk
 * mengimpor metadata artikel ilmiah (judul, tautan, deskripsi/abstrak singkat, jumlah sitasi, daftar
 * penulis) ke tabel {@link ScholarArticle}/{@link ScholarAuthor} berdasarkan kata kunci pencarian.
 * Kelas ini memperluas {@code HtmlDataExtractor} dari pustaka pihak ketiga <i>docear</i> (metadata
 * extraction framework) dan memakai Jsoup untuk mem-parse HTML mentah hasil pencarian.
 *
 * <h2>Apa yang di-scrape</h2>
 * <p>
 * Untuk setiap halaman hasil (satu halaman = satu nilai {@code start} pada parameter URL, diproses
 * per iterasi lewat {@link #startCrawl(int, String)}), method ini mencari seluruh elemen
 * {@code <div class="gs_r gs_or gs_scl">} — kontainer satu entri hasil pencarian pada tata letak
 * Google Scholar — lalu untuk masing-masing entri mengekstrak: judul artikel dan tautannya (dari
 * {@code h3.gs_rt}), cuplikan deskripsi (dari {@code div.gs_rs}), tautan dan jumlah "dikutip oleh"
 * (dari elemen {@code a} pertama pada {@code div.gs_fl}), serta daftar penulis (dari
 * {@code .gs_a>a}, atau bila tidak ada tautan penulis, dari teks polos {@code div.gs_a}). Data
 * mentah HTML halaman hasil disimpan sementara sebagai berkas gzip di
 * {@code /opt/temporary_crawling/} (dinamai dari kata kunci + nomor iterasi) sebelum di-parse,
 * berfungsi sebagai cache agar halaman yang sama tidak diambil ulang dari internet bila proses
 * diulang.
 * </p>
 *
 * <h2>Penanganan cookie, CAPTCHA, dan rate-limit</h2>
 * <p>
 * Google Scholar menerapkan proteksi anti-bot yang agresif. Kelas ini menyiasatinya dengan:
 * menyimpan/memuat ulang cookie sesi dari berkas {@link #cookieFileName} lewat {@link #getCookies}
 * (meminta cookie baru via {@link #requestNewCookie} bila belum ada), dan menangani dua kode status
 * HTTP kegagalan secara berbeda di {@link #startCrawl(int, String)}: status 503 memicu
 * {@link #handleCaptchaRequest(HttpStatusException)} yang mengunduh gambar CAPTCHA, meminta
 * penyelesaiannya (secara interaktif dari {@link System#in} bila tidak ada
 * {@link MetaDataListener} terdaftar, atau lewat event {@link CaptchaEvent} ke listener bila ada),
 * mengirim balik jawaban CAPTCHA lewat form yang di-parse dari halaman, lalu <b>mengulang crawl
 * dari awal secara rekursif</b> bila berhasil; status 403 memicu permintaan cookie baru dan
 * pengulangan rekursif satu kali (dijaga oleh {@link #triedNewCookie} agar tidak mengulang tanpa
 * batas).
 * </p>
 *
 * <h2>Risiko fragility (kerapuhan) parsing HTML</h2>
 * <p>
 * <b>Seluruh logika ekstraksi bergantung penuh pada nama kelas CSS internal Google Scholar</b>
 * (mis. {@code gs_r}, {@code gs_or}, {@code gs_scl}, {@code gs_ri}, {@code gs_rt}, {@code gs_rs},
 * {@code gs_fl}, {@code gs_a}) yang <b>tidak didokumentasikan secara publik dan dapat berubah kapan
 * saja</b> tanpa pemberitahuan pada perubahan tata letak halaman Google Scholar. Bila struktur HTML
 * berubah, seluruh selektor CSS di {@link #startCrawl(int, String)} akan gagal mencocokkan elemen
 * (umumnya menghasilkan string kosong atau {@link NullPointerException} pada pemanggilan
 * {@code .first()} yang mengasumsikan elemen selalu ditemukan) tanpa peringatan eksplisit, sehingga
 * hasil crawl dapat diam-diam menjadi kosong/tidak lengkap. Selain itu, sebagai scraper yang
 * mengambil data dari situs pihak ketiga, kelas ini juga rentan terhadap perubahan kebijakan
 * anti-bot (blokir IP, perubahan struktur cookie/CAPTCHA) yang dapat menghentikan fungsinya sewaktu-
 * waktu di luar kendali kode ini. Tidak ada mekanisme <i>throttling</i>/jeda eksplisit antar
 * permintaan di dalam kelas ini sendiri, sehingga penggunaan yang terlalu agresif berisiko memicu
 * pemblokiran oleh Google.
 * </p>
 */
public class GoogleScholarCrawler extends HtmlDataExtractor {

	/** Komponen ZK yang diperbarui dengan progres crawl (judul artikel + persentase) selama {@link #startCrawl(int, String)} berjalan. */
	private Label label;
	/** Penanda agar permintaan cookie baru pada kegagalan HTTP 403 hanya diulang sekali (mencegah rekursi tanpa batas). */
	private boolean triedNewCookie = false;

	/**
	 * Membuat crawler baru yang melaporkan progresnya ke komponen ZK {@code label} yang diberikan.
	 *
	 * @param label komponen label ZK tempat progres pencarian ditampilkan ke pengguna
	 */
	public GoogleScholarCrawler(Label label) {
		this.label = label;
	}

	/** Nama berkas penyimpanan cookie sesi Google Scholar (format XML, dibaca/ditulis lewat {@code readCookies}/{@code saveCookies} milik kelas induk). */
	private String cookieFileName = "GoogleScholarCookie.xml";
	/** URL dasar Google Scholar tempat seluruh permintaan pencarian/CAPTCHA diarahkan. */
	private String BaseURL = "https://scholar.google.com";
	/** Kode bahasa (parameter {@code hl}) yang dikirim pada permintaan pencarian; default Indonesia ("id"). */
	private String language = "id";

	/**
	 * Mengambil cookie sesi dari berkas {@code fileName}; bila belum ada/tidak terbaca, meminta
	 * cookie baru dari server lewat {@link #requestNewCookie(String)}.
	 *
	 * @param fileName nama berkas penyimpanan cookie
	 * @return peta cookie siap pakai untuk permintaan berikutnya ke Google Scholar
	 * @throws IOException diteruskan dari operasi baca berkas cookie
	 */
	private Map<String, String> getCookies(String fileName) throws IOException {
		Map<String, String> cookies = readCookies(fileName);
		if (cookies == null) {
			cookies = requestNewCookie(fileName);
		}
		return cookies;
	}

	/**
	 * Meminta cookie sesi baru dari {@link #BaseURL} (abaikan error HTTP agar respons tetap terbaca
	 * walau status bukan 200), lalu menyesuaikan nilai cookie {@code GSP} dengan menambahkan
	 * {@code :CF=4} — parameter tersembunyi yang mengaktifkan tautan ekspor BibTeX pada daftar hasil
	 * pencarian. Cookie yang diperoleh disimpan ke berkas {@code fileName} untuk dipakai ulang pada
	 * pemanggilan berikutnya.
	 *
	 * @param fileName nama berkas tujuan penyimpanan cookie
	 * @return peta cookie baru, atau {@code null} bila permintaan gagal (kegagalan dicatat ke logger,
	 *         tidak dilempar sebagai exception)
	 */
	private Map<String, String> requestNewCookie(String fileName) {
		Map<String, String> cookies = null;
		try {
			Response response = getConnection(BaseURL).ignoreHttpErrors(true).execute();
			cookies = response.cookies();
			String gsp = cookies.get("GSP");
			cookies.put("GSP", gsp + ":CF=4"); // :CF=4 enables the export to
												// BibTex Link in the result
												// list
			saveCookies(cookies, fileName);
		} catch (IOException e) {
			logger.info(e.getMessage(), e);
		}
		return cookies;
	}

	/**
	 * Menangani halaman tantangan CAPTCHA yang dikembalikan Google Scholar (biasanya sebagai respons
	 * status 503). Mengambil ulang halaman tantangan dari {@code e.getUrl()}, mencari gambar CAPTCHA
	 * pertama di halaman, lalu meminta penyelesaiannya: secara interaktif lewat konsol
	 * ({@link System#in}) bila tidak ada {@link MetaDataListener} terdaftar pada crawler ini, atau
	 * lewat event {@link CaptchaEvent} yang disiarkan ke seluruh listener terdaftar bila ada. Jawaban
	 * CAPTCHA yang diperoleh disisipkan ke data form tantangan (menggantikan field bernama
	 * {@code "captcha"}) dan dikirim balik ke server; bila berhasil, cookie hasil respons
	 * digabungkan ke cookie tersimpan dan disimpan ulang.
	 *
	 * @param e exception status HTTP yang memicu penanganan CAPTCHA (dipakai untuk mengambil
	 *          {@code getUrl()} halaman tantangan)
	 * @return {@code true} bila CAPTCHA berhasil diselesaikan dan dikirim (pemanggil dapat mengulang
	 *         permintaan asli); {@code false} bila gagal pada tahap mana pun (tidak ada gambar,
	 *         CAPTCHA tidak dijawab, atau kegagalan IO)
	 */
	private boolean handleCaptchaRequest(HttpStatusException e) {
		try {
			Response response = getConnection(e.getUrl()).ignoreHttpErrors(true).execute();

			final Document doc = response.parse();

			Iterator<Element> imgElements = doc.select("img").iterator();
			if (imgElements.hasNext()) {
				Element imgElement = imgElements.next();
				if (imgElement.hasAttr("src")) {
					String imgURL = imgElement.attr("src");
					Response imgResponse = getConnection(BaseURL + imgURL).execute();
					final Map<String, String> imgCookie = imgResponse.cookies();
					BufferedImage img = ais.common.CommonFileMediaHelper.bacaGambarAman(imgResponse.bodyAsBytes());
					String captcha = null;
					if (getListeners().size() <= 0) {
						ImageIO.write(img, "jpg", new File(getPath("captcha.jpg")));
						System.out.println("Enter Captcha here : ");
						BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
						captcha = bufferRead.readLine();
					} else {
						CaptchaEvent event = new CaptchaEvent(ScholarSource.GOOGLESCHOLAR, img);
						for (MetaDataListener listener : this.getListeners()) {
							listener.onCaptchaRequested(event);
						}
						if (!event.isCanceled() && event.getSolvedCaptcha() != null
								&& !event.getSolvedCaptcha().isEmpty()) {
							captcha = event.getSolvedCaptcha();
						}
					}
					if (captcha != null && !captcha.isEmpty()) {
						Iterator<Element> formElements = doc.select("form").iterator();
						if (formElements.hasNext()) {
							Element formElement = formElements.next();
							String formURL = "";
							if (formElement.hasAttr("action")) {
								formURL = formElement.attr("action");
							}
							HashMap<String, String> formData = new HashMap<String, String>();
							Elements inputElements = formElement.select("input");
							for (Element inputElement : inputElements) {
								if (!inputElement.attr("name").equals("captcha")) {
									formData.put(inputElement.attr("name"), inputElement.attr("value"));
								} else {
									formData.put(inputElement.attr("name"), captcha);
								}
							}
							Response captchaResponse = getConnection(BaseURL + "/sorry/" + formURL).data(formData)
									.ignoreHttpErrors(true).referrer(e.getUrl()).cookies(imgCookie).execute();

							Map<String, String> cookies = getCookies(cookieFileName);
							cookies.putAll(captchaResponse.cookies());
							saveCookies(cookies, cookieFileName);
							return true;
						}
					}
				}
			}
		} catch (IOException ex) {
			logger.info(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Menjalankan satu iterasi crawl pencarian Google Scholar untuk {@code keywords} pada halaman
	 * hasil ke-{@code iter} (parameter {@code start} pada URL pencarian, biasanya kelipatan 10).
	 * Alurnya: (1) bangun/muat dari cache berkas gzip HTML mentah hasil pencarian; bila belum ada
	 * cache, unduh dari Google Scholar memakai cookie sesi yang berlaku; (2) parse HTML dengan Jsoup
	 * dan iterasi tiap entri hasil ({@code div.gs_r.gs_or.gs_scl}) untuk mengekstrak judul, tautan,
	 * deskripsi, jumlah sitasi, dan daftar penulis (lihat catatan selektor CSS di Javadoc kelas); (3)
	 * untuk tiap penulis dan artikel yang ditemukan, cari record {@link ScholarAuthor}/
	 * {@link ScholarArticle} existing (berdasarkan userid/nama penulis atau tautan artikel) dan
	 * simpan/perbarui (saveOrUpdate) dalam transaksi Hibernate native tersendiri per entitas; (4)
	 * setelah seluruh entri diproses, panggil {@code GoogleScholarCrawlerByUser#updateDataAuthor}
	 * untuk memperbarui data detail setiap penulis unik yang ditemukan pada iterasi ini.
	 *
	 * <p>
	 * Pada kegagalan HTTP, method ini menangani dua skenario secara otomatis dengan rekursi diri:
	 * status 503 memicu alur penyelesaian CAPTCHA ({@link #handleCaptchaRequest}) lalu mengulang
	 * crawl bila berhasil; status 403 memicu permintaan cookie baru lalu mengulang crawl (maksimal
	 * satu kali pengulangan, dijaga {@link #triedNewCookie}). Kegagalan lain per-entri (mis.
	 * exception saat parsing satu artikel) ditangkap dan dicatat, tidak menghentikan pemrosesan
	 * entri lain di halaman yang sama.
	 * </p>
	 *
	 * @param iter     indeks halaman hasil pencarian (nilai parameter {@code start})
	 * @param keywords kata kunci pencarian
	 * @return daftar {@link ScholarArticle} yang berhasil diekstrak dan disimpan/diperbarui pada
	 *         iterasi ini
	 * @throws Exception diteruskan dari kegagalan IO/parsing yang tidak tertangani secara internal
	 */
	@SuppressWarnings({})
	public List<ScholarArticle> startCrawl(int iter, String keywords) throws Exception {

		// creating url
		String query = URLEncoder.encode(keywords, StandardCharsets.UTF_8.name());

		File file = new File("/opt/temporary_crawling/" + query + "_" + iter + ".txt.gz");
		if (!file.exists()) {
			try {
				file.getParentFile().mkdir();
				Map<String, String> cookies = getCookies(cookieFileName);
				Response response = getConnection(BaseURL + "/scholar")
						.data("q", keywords, "hl", this.language, "start", iter + "").cookies(cookies).execute();
				String simpanbody = response.body();
				FileUtils.writeByteArrayToFile(file, GzipUtil.zip(simpanbody));
			} catch (HttpStatusException e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/scholar/GoogleScholarCrawler.java:164");
				if (e.getStatusCode() == 503) {
					if (handleCaptchaRequest(e))
						return startCrawl(iter, keywords);
				} else if (e.getStatusCode() == 403) {
					if (requestNewCookie(cookieFileName) != null && !triedNewCookie)
						return startCrawl(iter, keywords);
				}
			}
		}

		if (triedNewCookie) {
			triedNewCookie = false;
		}

		Document doc = Jsoup.parse(GzipUtil.unzip(FileUtils.readFileToByteArray(file)));

		final Map<String, ScholarAuthor> allScholarAuthor = new HashMap<String, ScholarAuthor>();
		List<ScholarArticle> articleList = new ArrayList<ScholarArticle>();
		// checking did you mean proposal.

		// if there is no misspelling, parsing of html document start here.
		// all articles have gs_r tag.
		Elements articles = doc.select("div[class=gs_r gs_or gs_scl]");
		int index = 1;
		for (Element article : articles) {
			// we took articles. now getting informations article by article
			try {
				String articleName = article.select("div[class=gs_ri]").select("h3[class=gs_rt]").text();
				System.out.println("articleName -> " + articleName);
				String articleLink = article.select("div[class=gs_ri]").select("h3[class=gs_rt]").select("a[href]")
						.attr("href");

				label.setValue(articleName + " (" + Common.numberFormat.get().format((index++) * 100.0 / 10) + "%)");

				System.out.println("articleLink -> " + articleLink);
				String articleDesc = article.select("div[class=gs_ri]").select("div[class=gs_rs]").text();
				System.out.println("articleDesc -> " + articleDesc);
				String citingLink = "https://scholar.google.com" + article.select("div[class=gs_ri]")
						.select("div[class=gs_fl]").select("a[href]").first().attr("href");
				System.out.println("citingLink -> " + citingLink);
				int citingNumber;
				try {
					citingNumber = Integer.parseInt(article.select("div[class=gs_ri]").select("div[class=gs_fl]")
							.select("a[href]").first().text().replaceAll("\\D+", ""));
				} catch (NumberFormatException ex) {
					citingNumber = 0;
				}
				System.out.println("citingNumber -> " + citingNumber);
				Elements authors = article.select("div[class=gs_ri]").select(".gs_a>a");
				Set<ScholarAuthor> authorList = new HashSet<ScholarAuthor>();

				String authorName;
				if (authors.isEmpty()) {
					authorName = article.select("div[class=gs_ri]").select("div[class=gs_a]").text();
					Session session = HibernateUtil.currentNativeSession();
					ScholarAuthor tempAuthor = (ScholarAuthor) session.createCriteria(ScholarAuthor.class)
							.add(Restrictions.eq("nama", authorName)).addOrder(Order.desc("id")).setMaxResults(1)
							.uniqueResult();
					if (tempAuthor == null) {
						tempAuthor = new ScholarAuthor();
					}

					tempAuthor.setNama(authorName);
					System.out.println("authorName -> " + authorName);
					tempAuthor.setKeterangan("empty");

					session.getTransaction().begin();
					session.saveOrUpdate(tempAuthor);
					session.getTransaction().commit();
					HibernateUtil.closeSession();

					authorList.add(tempAuthor);
				} else {

					for (Element a : authors) {
						String aa = a.text();
						String authorLink = "https://scholar.google.com" + a.attr("href");

						System.out.println("authorName -> " + aa);
						System.out.println("authorLink -> " + authorLink);

						String userid = ScholarAuthor.ambilId(authorLink);
						System.out.println("userid -> " + userid);
						if (userid != null && !userid.trim().isEmpty()) {
							Session session = HibernateUtil.currentNativeSession();
							ScholarAuthor tempAuthor = (ScholarAuthor) session.createCriteria(ScholarAuthor.class)
									.add(Restrictions.eq("userid", userid)).addOrder(Order.desc("id")).setMaxResults(1)
									.uniqueResult();
							if (tempAuthor == null) {
								tempAuthor = new ScholarAuthor();
							}
							tempAuthor.setUserid(userid);
							tempAuthor.setNama(aa);
							tempAuthor.setKeterangan(authorLink);

							session.getTransaction().begin();
							session.saveOrUpdate(tempAuthor);
							session.getTransaction().commit();
							HibernateUtil.closeSession();

							allScholarAuthor.put(userid, tempAuthor);
							authorList.add(tempAuthor);

						}
					}
				}

				if (articleLink != null && !articleLink.trim().isEmpty()) {

					Session session = HibernateUtil.currentNativeSession();
					ScholarArticle tempArticle = (ScholarArticle) session.createCriteria(ScholarArticle.class)
							.add(Restrictions.eq("link", articleLink)).addOrder(Order.desc("id")).setMaxResults(1)
							.uniqueResult();
					if (tempArticle == null) {
						tempArticle = new ScholarArticle();
						tempArticle.setKeterangan(articleDesc);
						tempArticle.setScholarAuthors(authorList);
					}

					if (tempArticle.getHeaders() == null) {
						try {
							URL url = new URL(articleLink);
							URLConnection ua = url.openConnection();
							Map<String, List<String>> headers = ua.getHeaderFields();
							tempArticle.setHeaders(headers.toString());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/scholar/GoogleScholarCrawler.java:290");
							// TODO: handle exception
						}
					}

					tempArticle.setNama(articleName);
					tempArticle.setLink(articleLink);

					if (keywords != null && !keywords.trim().isEmpty() && keywords.trim().length() > 3
							&& !tempArticle.getKewords().toLowerCase().contains(keywords.trim().toLowerCase())) {
						String newKey = tempArticle.getKewords().isEmpty() ? keywords.trim()
								: tempArticle.getKewords() + ", " + keywords.trim();
						tempArticle.setKewords(newKey);
					}

					session.getTransaction().begin();
					session.saveOrUpdate(tempArticle);
					session.getTransaction().commit();
					HibernateUtil.closeSession();

					articleList.add(tempArticle);

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/scholar/GoogleScholarCrawler.java:314");
			}

			GoogleScholarCrawlerByUser googleScholarCrawlerByUser = new GoogleScholarCrawlerByUser(label);
			for (ScholarAuthor scholarAuthor : allScholarAuthor.values()) {
				try {
					googleScholarCrawlerByUser.updateDataAuthor(scholarAuthor);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/scholar/GoogleScholarCrawler.java:322");
				}
			}

			label.setValue("");

		}
		return articleList;
	}

	/**
	 * Implementasi kontrak {@code HtmlDataExtractor#search(String)} milik pustaka docear. Tidak
	 * diimplementasikan (selalu mengembalikan {@code null}) — pencarian sesungguhnya dilakukan lewat
	 * {@link #startCrawl(int, String)}, bukan lewat method ini.
	 *
	 * @param arg0 kata kunci pencarian (tidak dipakai)
	 * @return selalu {@code null}
	 */
	@Override
	public Collection<MetaData> search(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Implementasi kontrak {@link java.util.concurrent.Callable#call()} yang diwarisi lewat
	 * {@code HtmlDataExtractor}. Tidak diimplementasikan (selalu mengembalikan {@code null}).
	 *
	 * @return selalu {@code null}
	 * @throws Exception tidak pernah dilempar oleh implementasi saat ini
	 */
	@Override
	public Collection<MetaData> call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
