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

	@Override
	public Collection<MetaData> search(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<MetaData> call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
