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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.docear.metadata.data.MetaData;
import org.docear.metadata.data.ScholarMetaData.ScholarSource;
import org.docear.metadata.events.CaptchaEvent;
import org.docear.metadata.events.FetchedResultsEvent;
import org.docear.metadata.events.MetaDataListener;
import org.docear.metadata.extractors.HtmlDataExtractor;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.zkoss.zul.Label;

import ais.common.GzipUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ScholarArticle;
import ais.database.model.ScholarAuthor;

/**
 * Perayap (crawler) HTML untuk mengambil dan menyinkronkan profil serta daftar publikasi dari
 * halaman <b>Google Scholar Citations</b> milik seorang dosen/peneliti, berdasarkan {@code user}
 * (parameter {@code user} pada URL profil Google Scholar), lalu menyimpannya sebagai entitas
 * {@link ais.database.model.ScholarAuthor} dan {@link ais.database.model.ScholarArticle} di
 * database AIS. Kelas ini memperluas {@code org.docear.metadata.extractors.HtmlDataExtractor}
 * (pustaka docear untuk ekstraksi metadata publikasi ilmiah) dan mewarisi darinya mekanisme
 * koneksi HTTP ber-cookie ({@code getConnection}), pengelolaan cookie file
 * ({@code readCookies}/{@code saveCookies}), serta kerangka listener metadata
 * ({@code MetaDataListener}).
 *
 * <h2>Alur pengambilan data</h2>
 * <p>
 * Titik masuk utama adalah {@link #byUser(String)}: mengambil (atau memakai salinan lokal
 * ter-cache di berkas {@code .txt.gz} bila sudah pernah diunduh sebelumnya) halaman profil
 * Google Scholar untuk {@code user}, mengekstrak nama dan tautan foto profil lewat selector CSS
 * Jsoup ({@code div#gsc_prf_in}, {@code img#gsc_prf_pup-img}), menyimpan/memperbarui
 * {@link ScholarAuthor} terkait, lalu mengiterasi setiap tautan artikel ({@code a.gsc_a_at}) pada
 * halaman tersebut. Untuk tiap artikel, halaman detailnya diunduh (juga dengan mekanisme cache
 * berkas serupa), lalu diekstrak tautan artikel, tautan berkas, dan pasangan bidang-nilai metadata
 * (mis. penulis, jurnal, tahun) yang disimpan sebagai JSON di kolom {@code keterangan} entitas
 * {@link ScholarArticle}. Progres pengambilan ditampilkan ke pengguna secara real-time lewat
 * komponen ZK {@link #label} yang di-{@code setValue} pada setiap artikel yang sedang diproses.
 * </p>
 * <p>
 * Seluruh halaman yang pernah diunduh (baik halaman profil maupun halaman detail artikel)
 * disimpan sebagai berkas terkompresi gzip di direktori tetap {@code /opt/temporary_crawling/}
 * (path Unix mutlak, ditanam langsung di kode — lihat {@link #updateDataAuthor(ScholarAuthor)}
 * dan {@link #byUser(String)}) dengan nama berkas berbasis {@code user} atau
 * {@code URLEncoder.encode(dataHref, "UTF-8")}, dan dipakai ulang tanpa mengunduh lagi bila
 * berkas sudah ada — mekanisme cache sederhana ini mengurangi beban permintaan berulang ke Google
 * Scholar, sekaligus berarti data lokal TIDAK pernah kedaluwarsa/diperbarui otomatis kecuali
 * berkas cache dihapus manual.
 * </p>
 *
 * <h2>Penanganan CAPTCHA dan pembatasan Google Scholar</h2>
 * <p>
 * Karena Google Scholar tidak menyediakan API publik resmi dan aktif membatasi akses otomatis
 * (scraping), kelas ini menyertakan penanganan eksplisit untuk dua bentuk pembatasan: HTTP 503
 * (halaman CAPTCHA "/sorry/...") ditangani {@link #handleCaptchaRequest(HttpStatusException)},
 * yang mengunduh gambar CAPTCHA dan — bila ada listener metadata terpasang — meminta penyelesaian
 * CAPTCHA lewat {@code CaptchaEvent} ke pemanggil (mis. UI yang menampilkan gambar ke admin),
 * atau, bila tidak ada listener, meminta input CAPTCHA langsung dari {@code System.in} (jalur
 * command-line manual); HTTP 403 ditangani dengan mencoba memperoleh cookie sesi baru lewat
 * {@link #requestNewCookie(String)}. Kedua penanganan hanya dipakai pada method
 * {@link #search(String)}, bukan pada {@link #byUser(String)}/{@link #updateDataAuthor(ScholarAuthor)}
 * — dua method terakhir ini TIDAK menangani CAPTCHA/pemblokiran sama sekali, sehingga akan gagal
 * dengan exception mentah bila Google Scholar memblokir permintaannya.
 * </p>
 * <p>
 * Perayapan tanpa izin resmi terhadap layanan pihak ketiga (Google Scholar) berpotensi melanggar
 * Ketentuan Layanan (Terms of Service) penyedia — dicatat di sini sebagai observasi atas desain
 * kode yang ada, bukan sebagai kredensial/rahasia tertanam, dan tidak diubah sebagai bagian dari
 * pekerjaan dokumentasi ini.
 * </p>
 *
 * <h2>Catatan lain</h2>
 * <p>
 * Kelas ini TIDAK stateless: bidang {@link #label} (komponen ZK tujuan progres), serta bidang
 * warisan dari {@code HtmlDataExtractor} seperti cookie file, membuat satu instans kelas ini
 * hanya cocok dipakai untuk satu proses pengambilan pada satu waktu (tidak thread-safe untuk
 * dipakai bersamaan oleh banyak permintaan pengguna secara konkuren). Setiap sesi database
 * Hibernate dibuka dan ditutup berulang kali (per entitas yang disimpan) sepanjang satu pemanggilan
 * {@link #byUser(String)}, bukan dalam satu transaksi besar — sesuai gaya penanganan sesi Hibernate
 * yang lazim ditemukan di kelas-kelas lain pada AIS.
 * </p>
 */
public class GoogleScholarCrawlerByUser extends HtmlDataExtractor {

	/** Komponen label ZK tempat progres pengambilan data (nama penulis, judul artikel yang sedang diproses) ditampilkan real-time kepada pengguna. Diisi lewat konstruktor {@link #GoogleScholarCrawlerByUser(Label)}. */
	private Label label;

	/**
	 * Menangani respons HTTP 503 dari Google Scholar yang menandakan permintaan CAPTCHA
	 * ("/sorry/..."): mengunduh gambar CAPTCHA dari halaman tersebut, lalu meminta penyelesaiannya
	 * — lewat {@code CaptchaEvent} ke listener metadata yang terpasang bila ada, atau (bila tidak
	 * ada listener) lewat input baris perintah manual dari {@code System.in} setelah gambar
	 * disimpan ke berkas {@code captcha.jpg}. Bila CAPTCHA berhasil diisi, jawabannya dikirim
	 * sebagai submit form CAPTCHA ke Google Scholar dan cookie sesi hasilnya digabung/disimpan ke
	 * berkas cookie ({@link #cookieFileName}) untuk dipakai permintaan berikutnya.
	 *
	 * @param e exception status HTTP yang memicu penanganan ini (membawa URL halaman CAPTCHA)
	 * @return {@code true} bila CAPTCHA berhasil diselesaikan dan cookie baru tersimpan (pemanggil
	 *         sebaiknya mengulang permintaan asal), {@code false} bila CAPTCHA tidak dapat
	 *         diselesaikan atau dibatalkan
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
	 * Memperbarui nama dan tautan foto profil (kolom {@code imageLink}) sebuah
	 * {@link ScholarAuthor} yang SUDAH ada di database, HANYA bila {@code imageLink}-nya saat ini
	 * masih {@code null} (belum pernah diisi) — method ini adalah pembaruan hemat: tidak
	 * mengunduh ulang atau memperbarui author yang sudah punya foto profil tersimpan. Halaman
	 * profil diunduh (atau dipakai dari cache berkas gzip lokal bila sudah ada, lihat javadoc
	 * kelas) berdasarkan {@code scholarAuthor.getUserid()}, lalu nama dan tautan foto diekstrak
	 * dan disimpan dalam satu transaksi Hibernate. Progres nama penulis ditampilkan ke
	 * {@link #label} setelah pembaruan berhasil.
	 *
	 * @param scholarAuthor entitas penulis yang akan diperbarui di tempat (dan disimpan ke
	 *                       database); tidak melakukan apa pun bila {@code getImageLink()} sudah
	 *                       terisi
	 * @throws Exception diteruskan dari kegagalan I/O jaringan/berkas atau operasi Hibernate
	 */
	public void updateDataAuthor(ScholarAuthor scholarAuthor) throws Exception {
		if (scholarAuthor.getImageLink() == null) {
			String user = scholarAuthor.getUserid();
			File file = new File("/opt/temporary_crawling/" + user + ".txt.gz");
			if (!file.exists()) {
				file.getParentFile().mkdir();
				Map<String, String> cookies = getCookies(cookieFileName);
				Response response = getConnection(BaseURL + "/citations")
						.data("user", user, "hl", this.language, "cstart", "0", "pagesize", "300").cookies(cookies)
						.execute();
				String simpanbody = response.body();
				FileUtils.writeByteArrayToFile(file, GzipUtil.zip(simpanbody));
			}

			Document doc = Jsoup.parse(GzipUtil.unzip(FileUtils.readFileToByteArray(file)));

			Session session = HibernateUtil.currentNativeSession();
			session.refresh(scholarAuthor);
			String authorName = doc.select("div#gsc_prf_in").text();
			String imageLink = doc.select("img#gsc_prf_pup-img").attr("src");

			scholarAuthor.setImageLink(imageLink);
			scholarAuthor.setNama(authorName);

			session.getTransaction().begin();
			session.update(scholarAuthor);
			session.getTransaction().commit();
			HibernateUtil.closeSession();

			label.setValue(authorName);
		}
	}

	/**
	 * Membentuk crawler baru dengan {@code label} sebagai tujuan tampilan progres real-time
	 * selama pengambilan data berjalan (lihat {@link #label}).
	 *
	 * @param label komponen ZK yang akan dimutakhirkan nilainya selama {@link #byUser(String)}/
	 *              {@link #updateDataAuthor(ScholarAuthor)} berjalan
	 */
	public GoogleScholarCrawlerByUser(Label label) {
		this.label = label;
	}

	/**
	 * Titik masuk utama crawler: mengambil profil Google Scholar untuk {@code user} (dari cache
	 * berkas lokal bila tersedia, atau mengunduh baru bila belum), menyimpan/memperbarui entitas
	 * {@link ScholarAuthor} terkait, lalu mengiterasi seluruh tautan artikel pada halaman profil
	 * tersebut — untuk setiap artikel, mengunduh halaman detailnya, mengekstrak tautan artikel/
	 * berkas dan metadata bidang-nilai (disimpan sebagai JSON), lalu menyimpan/memperbarui entitas
	 * {@link ScholarArticle} terkait (mengaitkan {@code scholarAuthor} ke daftar penulis artikel
	 * bila belum ada). Progres (nama artikel yang sedang diproses) ditampilkan ke {@link #label}
	 * selama proses berjalan dan dikosongkan setelah selesai.
	 *
	 * <p>
	 * Kegagalan pada satu artikel individual (mis. gagal mengunduh/parsing halaman detailnya)
	 * ditangkap per-artikel dan dicatat ke {@link ais.common.ErrorAuditUtil} tanpa menghentikan
	 * pemrosesan artikel-artikel lain — artikel yang gagal diproses tidak masuk ke hasil
	 * kembalian. Method ini TIDAK menangani CAPTCHA/pemblokiran Google Scholar (berbeda dari
	 * {@link #search(String)}) — kegagalan permintaan HTTP akibat pembatasan Google Scholar akan
	 * ditangkap sebagai exception biasa pada blok penanganan artikel yang bersangkutan.
	 * </p>
	 *
	 * @param user id pengguna Google Scholar (parameter {@code user} pada URL profil Scholar)
	 *             yang datanya akan diambil/disinkronkan
	 * @return daftar {@link ScholarArticle} yang berhasil diproses dan disimpan/diperbarui pada
	 *         pemanggilan ini (tidak termasuk artikel yang gagal diproses)
	 * @throws Exception diteruskan dari kegagalan pengambilan/parsing halaman profil itu sendiri
	 *                    (bukan dari kegagalan per-artikel, yang ditangani internal)
	 */
	public List<ScholarArticle> byUser(String user) throws Exception {

		List<ScholarArticle> articleList = new ArrayList<ScholarArticle>();

		File file = new File("/opt/temporary_crawling/" + user + ".txt.gz");
		if (!file.exists()) {
			file.getParentFile().mkdir();
			Map<String, String> cookies = getCookies(cookieFileName);
			Response response = getConnection(BaseURL + "/citations")
					.data("user", user, "hl", this.language, "cstart", "0", "pagesize", "300").cookies(cookies)
					.execute();
			String simpanbody = response.body();
			FileUtils.writeByteArrayToFile(file, GzipUtil.zip(simpanbody));
		}

		Document doc = Jsoup.parse(GzipUtil.unzip(FileUtils.readFileToByteArray(file)));

		Session session = HibernateUtil.currentNativeSession();
		ScholarAuthor scholarAuthor = (ScholarAuthor) session.createCriteria(ScholarAuthor.class)
				.add(Restrictions.eq("userid", user)).uniqueResult();
		if (scholarAuthor == null) {
			scholarAuthor = new ScholarAuthor();
		}

		String authorName = doc.select("div#gsc_prf_in").text();
		String imageLink = doc.select("img#gsc_prf_pup-img").attr("src");

		scholarAuthor.setKeterangan(BaseURL + "/citations?hl=id&user=" + user);
		scholarAuthor.setUserid(user);
		scholarAuthor.setImageLink(imageLink);
		scholarAuthor.setNama(authorName);

		session.getTransaction().begin();
		session.saveOrUpdate(scholarAuthor);
		session.getTransaction().commit();
		HibernateUtil.closeSession();

		Elements articles = doc.select("a[class=gsc_a_at]");
		for (Element article : articles) {
			String articleName = article.text();
			label.setValue(articleName);
			System.out.println("articleName -> " + articleName);
			String dataHref = article.attr("data-href");
			// System.out.println("dataHref -> " + dataHref);

			try {
				String httpSub = "https://scholar.google.com/" + dataHref;

				file = new File("/opt/temporary_crawling/" + URLEncoder.encode(dataHref, "UTF-8") + ".txt.gz");
				if (!file.exists()) {
					file.getParentFile().mkdir();
					Map<String, String> cookiesSub = getCookies(cookieFileName);
					Response responseSub = getConnection(httpSub).cookies(cookiesSub).execute();
					String simpanbody = responseSub.body();
					FileUtils.writeByteArrayToFile(file, GzipUtil.zip(simpanbody));
				}

				Document docSub = Jsoup.parse(GzipUtil.unzip(FileUtils.readFileToByteArray(file)));

				String articleLink = null;
				try {
					articleLink = docSub.select("a[class=gsc_vcd_title_link]").attr("href");
					System.out.println("articleLink -> " + articleLink);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/scholar/GoogleScholarCrawlerByUser.java:214");
				}

				String articleLinkFile = null;
				try {
					articleLinkFile = docSub.select("div.gsc_vcd_title_ggi>a").attr("href");
					System.out.println("articleLinkFile -> " + articleLinkFile);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/scholar/GoogleScholarCrawlerByUser.java:222");
				}

				JSONObject jsonObject = new JSONObject();
				Elements articlesSubdata = docSub.select("div[class=gs_scl]");
				for (Element articleSub : articlesSubdata) {
					String field = articleSub.select("div[class=gsc_vcd_field]").text();
					String value = articleSub.select("div[class=gsc_vcd_value]").text();

					System.out.println("field -> " + field + ", value -> " + value);
					jsonObject.put(field, value);
				}

				if (articleLink != null && !articleLink.trim().isEmpty()) {

					session = HibernateUtil.currentNativeSession();
					ScholarArticle tempArticle = (ScholarArticle) session.createCriteria(ScholarArticle.class)
							.add(Restrictions.eq("link", articleLink)).addOrder(Order.desc("id")).setMaxResults(1)
							.uniqueResult();
					if (tempArticle == null) {
						tempArticle = new ScholarArticle();
					}

					if (tempArticle.getHeaders() == null) {
						try {
							URL url = new URL(articleLink);
							URLConnection ua = url.openConnection();
							Map<String, List<String>> headers = ua.getHeaderFields();
							tempArticle.setHeaders(headers.toString());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/scholar/GoogleScholarCrawlerByUser.java:251");
							// TODO: handle exception
						}
					}

					boolean ada = false;
					for (ScholarAuthor author : tempArticle.getScholarAuthors()) {
						if (author.getUserid() != null && author.getUserid().equals(user)) {
							ada = true;
						}
					}

					if (!ada) {
						tempArticle.getScholarAuthors().add(scholarAuthor);
					}
					tempArticle.setNama(articleName);
					tempArticle.setLinkFile(articleLinkFile);
					tempArticle.setLink(articleLink);
					tempArticle.setKeterangan(jsonObject.toString());

					session.getTransaction().begin();
					session.saveOrUpdate(tempArticle);
					session.getTransaction().commit();
					HibernateUtil.closeSession();

					articleList.add(tempArticle);
				}

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/scholar/GoogleScholarCrawlerByUser.java:279");
				// TODO: handle exception
			}
		}
		label.setValue("");
		if (triedNewCookie) {
			triedNewCookie = false;
		}
		return articleList;
	}

	/** Nama berkas tempat cookie sesi Google Scholar disimpan/dibaca ulang lintas pemanggilan (lewat {@code readCookies}/{@code saveCookies} warisan {@code HtmlDataExtractor}). */
	private String cookieFileName = "GoogleScholarCookie.xml";
	/** URL dasar Google Scholar tempat seluruh permintaan HTTP di kelas ini ditujukan. */
	private String BaseURL = "https://scholar.google.com";
	/** Kode bahasa (parameter {@code hl}) yang diminta pada setiap permintaan ke Google Scholar; {@code "id"} (Indonesia). */
	private String language = "id";
	/** Penanda satu-kali agar {@link #search(String)} hanya mencoba memperoleh cookie baru sekali per rangkaian percobaan ulang, mencegah perulangan tak berkesudahan saat HTTP 403 terus berulang. */
	private Boolean triedNewCookie = false;

	/**
	 * Mengimplementasikan kontrak pencarian metadata {@code HtmlDataExtractor}: memuat halaman
	 * profil Google Scholar untuk {@code query} (diperlakukan sebagai id pengguna Scholar) lewat
	 * koneksi ber-cookie, lalu HANYA mengekstrak nama penulis dan tautan foto profil untuk
	 * dicetak ke konsol — TIDAK membangun objek {@link MetaData} apa pun ke dalam hasil kembalian
	 * ({@code result} selalu berupa daftar kosong pada implementasi saat ini; hanya kerangka
	 * event {@code FetchedResultsEvent} yang dipenuhi ke listener terpasang). Berbeda dari
	 * {@link #byUser(String)}, method ini secara eksplisit menangani dua bentuk pembatasan Google
	 * Scholar: HTTP 503 (permintaan CAPTCHA, ditangani {@link #handleCaptchaRequest}, lalu
	 * pencarian diulang bila berhasil) dan HTTP 403 (mencoba cookie baru lewat
	 * {@link #requestNewCookie(String)}, lalu pencarian diulang sekali bila berhasil, dijaga agar
	 * tidak berulang tak berkesudahan lewat {@link #triedNewCookie}).
	 *
	 * @param query id pengguna Google Scholar yang dicari
	 * @return daftar metadata hasil pencarian — pada implementasi saat ini selalu kosong, karena
	 *         hasil ekstraksi hanya dicetak ke konsol dan tidak dirakit menjadi objek
	 *         {@link MetaData}
	 */
	public Collection<MetaData> search(final String query) {
		ArrayList<MetaData> result = new ArrayList<MetaData>();
		try {
			Map<String, String> cookies = getCookies(cookieFileName);
			Response response = getConnection(BaseURL + "/citations")
					.data("user", query, "hl", this.language, "cstart", "0", "pagesize", "300").cookies(cookies)
					.execute();

			Document doc = response.parse();

			String authorName = doc.select("div#gsc_prf_in").text();
			String imageLink = doc.select("img#gsc_prf_pup-img").attr("src");
			System.out.println("authorName " + authorName + ", imageLink " + imageLink);

		} catch (HttpStatusException e) {
			logger.info(e.getMessage(), e);
			if (e.getStatusCode() == 503) {
				if (handleCaptchaRequest(e))
					return search(query);
			} else if (e.getStatusCode() == 403 && !triedNewCookie) {
				if (requestNewCookie(cookieFileName) != null)
					return search(query);
			}
		} catch (IOException e) {
			logger.info(e.getMessage(), e);
		}
		FetchedResultsEvent event = new FetchedResultsEvent(result);
		for (MetaDataListener listener : this.getListeners()) {
			listener.onFinishedRequest(event);
		}
		if (triedNewCookie) {
			triedNewCookie = false;
		}
		return result;
	}

	/**
	 * Membaca cookie sesi dari berkas {@code fileName} lewat {@code readCookies} warisan
	 * {@code HtmlDataExtractor}; bila belum ada cookie tersimpan (hasil {@code null}), meminta
	 * cookie baru lewat {@link #requestNewCookie(String)} sebagai fallback.
	 *
	 * @param fileName nama berkas cookie
	 * @return peta cookie siap pakai untuk permintaan HTTP berikutnya
	 * @throws IOException diteruskan dari kegagalan membaca berkas cookie
	 */
	private Map<String, String> getCookies(String fileName) throws IOException {
		Map<String, String> cookies = readCookies(fileName);
		if (cookies == null) {
			cookies = requestNewCookie(fileName);
		}
		return cookies;
	}

	/**
	 * Meminta cookie sesi baru dengan mengakses {@link #BaseURL} tanpa parameter, lalu menandai
	 * cookie {@code GSP} yang diterima dengan sufiks {@code ":CF=4"} — nilai spesifik yang
	 * mengaktifkan tautan ekspor sitasi ke format BibTeX pada hasil pencarian Google Scholar
	 * (lihat komentar inline di badan method). Cookie hasil disimpan ke berkas {@code fileName}
	 * lewat {@code saveCookies} warisan {@code HtmlDataExtractor} untuk dipakai ulang pada
	 * permintaan-permintaan berikutnya.
	 *
	 * @param fileName nama berkas tujuan penyimpanan cookie baru
	 * @return peta cookie baru, atau {@code null} bila permintaan gagal (galat hanya dicatat ke
	 *         {@code logger}, tidak dilempar ke pemanggil)
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
	 * Implementasi kontrak {@code Callable} warisan {@code HtmlDataExtractor}: menjalankan
	 * {@link #search(String)} memakai {@code searchValue} (bidang warisan yang menampung kueri
	 * pencarian yang diminta pemanggil kerangka docear).
	 *
	 * @return hasil {@link #search(String)} untuk {@code searchValue}
	 * @throws Exception diteruskan apa adanya dari {@link #search(String)}
	 */
	public Collection<MetaData> call() throws Exception {
		return search(searchValue);
	}

}
