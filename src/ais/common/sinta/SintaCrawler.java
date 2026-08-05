package ais.common.sinta;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.zkoss.zul.Label;

import ais.database.model.Dosen;

public class SintaCrawler {

	// public static void data(JSONObject jsonObject) throws Exception {
	// if (!jsonObject.isNull("link")) {
	// String link = jsonObject.getString("link");
	// try {
	// Document doc =
	// Jsoup.connect(link).userAgent("Mozilla").timeout(3000).get();
	// // System.out.println("-------------------\n" + link +
	// // "\n-------------------------------\n" + doc.html());
	// String articleLink =
	// doc.select("meta[http-equiv]").attr("content").trim().split(";", 2)[1]
	// .replaceAll("url=", "");
	// System.out.println("-------------------\n" + articleLink +
	// "\n-------------------------------\n");
	// jsonObject.put("articleLink", articleLink);
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:30");
	// e.printStackTrace();
	// }
	// }
	// }

	public static void populateData(JSONArray data, String kode, int page, Label label, Dosen dosen) throws Exception {
		Document doc = Jsoup.connect("http://sinta.ristekbrin.go.id/authors/detail")
				.data("page", page + "", "view", "documentsgs", "id", kode).userAgent("Mozilla").timeout(3000).get();

		Elements articles = doc.select("dl[class=uk-description-list-line]");
		if (articles.isEmpty()) {
			return;
		}
		for (Element article : articles) {

			// System.out.println("--------------------------------------------------\n"
			// + article.html());

			JSONObject jsonObject = new JSONObject();
			String judul = article.select("dt[class=uk-text-primary]").text();
			jsonObject.put("judul", judul);
			String articleLink = article.select("dt[class=uk-text-primary]").select("a[href]").attr("href");
			jsonObject.put("link", articleLink);

			try {
				String author = article.select("dd").html().split("\n")[0];
				jsonObject.put("author", author);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:58");
			}

			String indexedby = article.select("dd[class=indexed-by]").text();
			// jsonObject.put("indexedby", indexedby);
			String[] s = StringUtils.split(indexedby, "|");
			try {
				String jurnal = s[0].split(",", 2)[0].trim();
				jsonObject.put("jurnal", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:67");
			}
			try {
				String jurnal = s[0].split(",", 2)[1].trim();
				jsonObject.put("page", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:72");
			}

			try {
				String jurnal = s[1].split(":", 2)[1].trim();
				jsonObject.put("vol", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:78");
			}
			try {
				String jurnal = s[2].split(":", 2)[1].trim();
				jsonObject.put("issue", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:83");
			}

			try {
				String jurnal = s[3].trim();
				jsonObject.put("tahun", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:89");
			}

			// data(jsonObject);

			label.setValue("Ambil artikel milik \"" + dosen.getNama() + "\" -> " + judul);

			data.put(jsonObject);
		}

		populateData(data, kode, ++page, label, dosen);
	}

	public static void main(String[] argv) throws Exception {

		JSONArray data = new JSONArray();
		populateData(data, "5983166", 1, new Label(), new Dosen());
		System.out.println(data);
	}

}
