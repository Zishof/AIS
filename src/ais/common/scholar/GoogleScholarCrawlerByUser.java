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

public class GoogleScholarCrawlerByUser extends HtmlDataExtractor {

	private Label label;

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

	public GoogleScholarCrawlerByUser(Label label) {
		this.label = label;
	}

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

	private String cookieFileName = "GoogleScholarCookie.xml";
	private String BaseURL = "https://scholar.google.com";
	private String language = "id";
	private Boolean triedNewCookie = false;

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

	public Collection<MetaData> call() throws Exception {
		return search(searchValue);
	}

}
