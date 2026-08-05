package ais.common.sinta;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.PerguruanTinggi;
import ais.database.model.SintaArticle;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.penelitiandanpengabdian.TahapanPenyusunanArtikel;

public class SintaPtCrawler {

	@SuppressWarnings("unchecked")
	public static void singkronkan(final Label label, PerguruanTinggi perguruanTinggi) {
		if (perguruanTinggi == null || perguruanTinggi.getKodeSinta().isEmpty()) {
			return;
		}

		JSONArray data = new JSONArray();
		try {
			populateKodeSintaDosen(data, perguruanTinggi.getKodeSinta(), 1, label);
		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:40");
		}

		System.out.println("data -> " + data);

		if (data.length() == 0) {
			label.setValue("");
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		List<Dosen> dosenSinta = new ArrayList<Dosen>();
		for (int i = 0; i < data.length(); i++) {
			try {
				JSONObject jsonObject = data.getJSONObject(i);
				if (!jsonObject.isNull("nidn")) {
					List<Dosen> dosens = ConstantValues.simpleList(session.createCriteria(Dosen.class)
							.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
							.add(Restrictions.eq("nidn", jsonObject.getString("nidn"))), Dosen.class);
					for (Dosen dosen : dosens) {
						String id = jsonObject.getString("id");
						label.setValue("update data dosen -> " + dosen.getNama() + " dengan id SINTA " + id);
						dosen.setKodeSinta(id);
						session.getTransaction().begin();
						Common.refreshUpdate(session, dosen);
						session.getTransaction().commit();
						dosenSinta.add(dosen);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:70");
			}

		}

		TahapanPenyusunanArtikel tahapanPenyusunanArtikel = (TahapanPenyusunanArtikel) session
				.createCriteria(TahapanPenyusunanArtikel.class).add(Restrictions.eq("nama", "Dicetak (terbit)"))
				.setMaxResults(1).uniqueResult();

		for (Dosen dosen : dosenSinta) {
			try {
				singkronkanArtikel(dosen, label, session, tahapanPenyusunanArtikel);
			} catch (Exception eArtikel) {
				// Jangan biarkan 1 dosen gagal (mis. artikel SINTA-nya format tak terduga)
				// menghentikan sinkronisasi artikel utk SISA dosen dalam batch ini.
				ais.common.ErrorAuditUtil.record(eArtikel,
						"auto-audit src/ais/common/sinta/SintaPtCrawler.java:singkronkanArtikelBaris dosen="
								+ (dosen == null ? "-" : dosen.getNama()));
			}
		}
		HibernateUtil.closeSession();

		label.setValue("");
	}

	public static void singkronkanArtikel(Dosen dosen, Label label, Session session,
			TahapanPenyusunanArtikel tahapanPenyusunanArtikel) {

		JSONArray dataArtikel = new JSONArray();
		try {
			SintaCrawler.populateData(dataArtikel, dosen.getKodeSinta(), 1, label, dosen);
			System.out.println(dataArtikel);
			for (int i = 0; i < dataArtikel.length(); i++) {
				try {
					JSONObject jsonObject = dataArtikel.getJSONObject(i);

					SintaArticle sintaArticle = (SintaArticle) session.createCriteria(SintaArticle.class)
							.add(Restrictions.eq("dosen", dosen))
							.add(Restrictions.ilike("link", jsonObject.getString("link")))
							.add(Restrictions.ilike("nama", jsonObject.getString("judul"))).uniqueResult();
					if (sintaArticle == null) {
						sintaArticle = new SintaArticle();
					}
					sintaArticle.setKeterangan(jsonObject.toString());
					sintaArticle.setDosen(dosen);
					try {
						sintaArticle.setLink(jsonObject.getString("link"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:109");
						// TODO: handle exception
					}
					try {
						sintaArticle.setAuthor(jsonObject.getString("author"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:114");
						// TODO: handle exception
					}
					try {
						sintaArticle.setVol(jsonObject.getString("vol"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:119");
						// TODO: handle exception
					}
					try {
						sintaArticle.setIssue(jsonObject.getString("issue"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:124");
						// TODO: handle exception
					}
					try {
						sintaArticle.setTahun(Integer.parseInt(jsonObject.getString("tahun")));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:129");
						// TODO: handle exception
					}

					try {
						sintaArticle.setJurnal(jsonObject.getString("jurnal"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:135");
						// TODO: handle exception
					}

					try {
						sintaArticle.setNama(jsonObject.getString("judul"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:141");
						// TODO: handle exception
					}

					try {
						sintaArticle.setPage(jsonObject.getString("page"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:147");
						// TODO: handle exception
					}

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, sintaArticle);
					session.getTransaction().commit();

					Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(
							session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("dosen", dosen)).setMaxResults(1),
							Tbmuser.class);
					if (tbmuser != null) {
						String namaJurnal = sintaArticle.getJurnal().isEmpty() ? "Jurnal Default"
								: sintaArticle.getJurnal();

						String path = namaJurnal.toLowerCase().trim().replaceAll(" ", "_");

						JurnalPenelitian jurnalPenelitian = (JurnalPenelitian) ConstantValues
								.simpleObject(
										session.createCriteria(JurnalPenelitian.class)
												.add(Restrictions.eq("path", path)).setMaxResults(1),
										JurnalPenelitian.class);
						if (jurnalPenelitian == null) {
							jurnalPenelitian = new JurnalPenelitian();
							jurnalPenelitian.setJudul(namaJurnal);
							jurnalPenelitian.setPath(path);
							session.getTransaction().begin();
							session.save(jurnalPenelitian);
							session.getTransaction().commit();
						}

						Artikel artikel = (Artikel) session.createCriteria(Artikel.class)
								.add(Restrictions.eq("sintaArticle", sintaArticle)).uniqueResult();
						if (artikel == null) {
							artikel = new Artikel();
						}
						artikel.setTbmuser(tbmuser);
						artikel.setSintaArticle(sintaArticle);
						artikel.setTahapanPenyusunanArtikel(tahapanPenyusunanArtikel);
						artikel.setJurnalPenelitian(jurnalPenelitian);
						session.getTransaction().begin();
						session.saveOrUpdate(artikel);
						session.getTransaction().commit();

					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:193");
				}

			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:198");
		}
	}

	public static void populateKodeSintaDosen(JSONArray data, String kode, int page, Label label) throws Exception {
		Document doc = Jsoup.connect("https://sinta.kemdikbud.go.id/affiliations/profile")
				.data("page", page + "", "view", "authors", "id", kode, "sort", "year2").userAgent("Mozilla")
				.timeout(3000).get();
		Elements articles = doc.select("dl[class=uk-description-list-line]");
		if (articles.isEmpty()) {
			return;
		}

		for (Element article : articles) {
			JSONObject jsonObject = new JSONObject();
			String text = article.select("a[class=text-blue]").text();
			jsonObject.put("nama", text);

			String articleLink = article.select("a[class=text-blue]").select("a[href]").attr("href");
			jsonObject.put("link", articleLink);
			String id = null;
			try {
				String[] pairs = articleLink.split("&");

				for (String pair : pairs) {
					try {
						int idx = pair.indexOf("=");
						String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
						String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
						// System.out.println("key => "+key);
						if (key.endsWith("id"))
							id = value.trim();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:230");
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:234");
			}
			jsonObject.put("id", id);

			// System.out.println("---------------------------------------");
			for (Element all : article.getAllElements()) {
				try {
					String nidn = all.text();
					if (nidn.toLowerCase().trim().contains("nidn")) {
						String[] a = StringUtils.split(nidn, ":");
						nidn = a[a.length - 1].trim();
						jsonObject.put("nidn", nidn);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:247");
					// TODO: handle exception
				}

			}
			label.setValue("Ambil data dari SINTA -> " + jsonObject.toString());
			data.put(jsonObject);
		}
		populateKodeSintaDosen(data, kode, ++page, label);
	}

	public static void main(String[] argv) throws Exception {

		Document doc = Jsoup.connect("https://sinta.kemdikbud.go.id/affiliations/profile/626")
				.data("page", "1", "view", "authors", "id", "8443", "sort", "year2").userAgent("Mozilla").timeout(3000)
				.get();

		// Document doc =
		// Jsoup.connect("https://sinta.kemdikbud.go.id/affiliations/profile?page=1&view=authors&id=8443&sort=year2")
		// .timeout(3000)
		// .get();

		// System.out.println(doc.html());

		Elements articles = doc.select("dl[class=uk-description-list-line]");
		// System.out.println(articles.html());

		JSONArray jsonArray = new JSONArray();
		for (Element article : articles) {
			JSONObject jsonObject = new JSONObject();
			String text = article.select("a[class=text-blue]").text();
			jsonObject.put("nama", text);

			String articleLink = article.select("a[class=text-blue]").select("a[href]").attr("href");
			jsonObject.put("link", articleLink);
			String id = null;
			try {
				String[] pairs = articleLink.split("&");

				for (String pair : pairs) {
					try {
						int idx = pair.indexOf("=");
						String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
						String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
						// System.out.println("key => "+key);
						if (key.endsWith("id"))
							id = value.trim();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:294");
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:298");
			}
			jsonObject.put("id", id);

			// System.out.println("---------------------------------------");
			for (Element all : article.getAllElements()) {
				try {
					String nidn = all.text();
					if (nidn.toLowerCase().trim().contains("nidn")) {
						String[] a = StringUtils.split(nidn, ":");
						nidn = a[a.length - 1].trim();
						jsonObject.put("nidn", nidn);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:311");
					// TODO: handle exception
				}

			}

			jsonArray.put(jsonObject);
		}

		System.out.println(jsonArray.toString());
	}

}
