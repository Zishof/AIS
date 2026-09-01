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

public class GoogleScholarCrawler extends HtmlDataExtractor {

	private Label label;
	private boolean triedNewCookie = false;

	public GoogleScholarCrawler(Label label) {
		this.label = label;
	}

	private String cookieFileName = "GoogleScholarCookie.xml";
	private String BaseURL = "https://scholar.google.com";
	private String language = "id";

	private Map<String, String> getCookies(String fileName) throws IOException {
		Map<String, String> cookies = readCookies(fileName);
		if (cookies == null) {
			cookies = requestNewCookie(fileName);
		}
		return cookies;
	}

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
