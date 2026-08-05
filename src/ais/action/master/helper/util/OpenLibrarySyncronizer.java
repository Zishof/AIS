package ais.action.master.helper.util;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.library.DdcItem;
import ais.database.model.library.Item;

public class OpenLibrarySyncronizer {

	public static void process(Item item) {

		if (Common.bolehKonfigurasi("terintegrasi_dengan_google_book_baru", Konfigurasi.TIDAK_AKTIF)) {

			if (item.getIsbnFix() != null && !item.getIsbnFix().isEmpty() && item.getIsbnIsNumber()) {

				String url = "http://openlibrary.org/api/volumes/brief/isbn/" + item.getIsbnFix() + ".json";

				// System.out.println("OpenLibrarySyncronizer => url = " + url);

				try {
					JSONObject all = Common.getJsonObject(url);
					JSONObject records = all.isNull("records") ? null : all.getJSONObject("records");

					JSONObject bookRecord = records == null || !records.keys().hasNext() ? null
							: records.getJSONObject(records.keys().next().toString());

					JSONObject dataRecords = bookRecord == null || bookRecord.isNull("data") ? null
							: bookRecord.getJSONObject("data");

					JSONObject bookDetails = bookRecord == null || bookRecord.isNull("details") ? null
							: bookRecord.getJSONObject("details");
					JSONObject bookSubDetails = bookDetails == null || bookDetails.isNull("details") ? null
							: bookDetails.getJSONObject("details");

					JSONArray items = all.isNull("item") ? null : all.getJSONArray("item");
					// System.out.println("OpenLibrarySyncronizer => records = "
					// + records);
					// System.out
					// .println("OpenLibrarySyncronizer => items = " + items);
					//
					// System.out.println("OpenLibrarySyncronizer => bookRecord
					// = "
					// + bookRecord);
					// System.out.println("OpenLibrarySyncronizer => dataRecords
					// = "
					// + dataRecords);
					//
					// System.out.println("OpenLibrarySyncronizer => bookDetails
					// = "
					// + bookDetails);

					// System.out
					// .println("OpenLibrarySyncronizer => bookSubDetails = "
					// + bookSubDetails);

					JSONObject jsonItem = items == null || items.length() == 0 ? null : items.getJSONObject(0);
					Session session = HibernateUtil.currentNativeSession();

					item.setRecordURL(bookRecord == null || bookRecord.isNull("recordURL") ? item.getRecordURL()
							: bookRecord.getString("recordURL"));
					item.setOclcs(bookRecord == null || bookRecord.isNull("oclcs") ? item.getOclcs()
							: bookRecord.getString("oclcs"));
					item.setLccn(bookRecord == null || bookRecord.isNull("lccns") ? item.getLccn()
							: bookRecord.getString("lccns"));
					item.setIssn(bookRecord == null || bookRecord.isNull("issns") ? item.getIssn()
							: bookRecord.getString("issns"));
					item.setPenaklikan(bookSubDetails == null || bookSubDetails.isNull("pagination")
							? item.getPenaklikan() : bookSubDetails.getString("pagination"));
					item.setClassifications(bookSubDetails == null || bookSubDetails.isNull("classifications")
							? item.getClassifications() : bookSubDetails.getString("classifications"));

					String dewey_decimal_class = bookSubDetails == null || bookSubDetails.isNull("dewey_decimal_class")
							? "" : bookSubDetails.getString("dewey_decimal_class");
					if (!dewey_decimal_class.isEmpty()) {
						JSONArray ddc = bookSubDetails.getJSONArray("dewey_decimal_class");
						if (ddc.length() > 0) {
							item.setDeweyDecimalClass(ddc.getString(0));
						}
					}

					item.setDewey_decimal_class(bookSubDetails == null || bookSubDetails.isNull("dewey_decimal_class")
							? item.getDeweyDecimalClass() : bookSubDetails.getString("dewey_decimal_class"));

					item.setSubjects(dataRecords == null || dataRecords.isNull("subjects") ? item.getSubjects()
							: dataRecords.getString("subjects"));

					item.setEbooks(
							dataRecords == null || dataRecords.isNull("ebooks") ? "" : dataRecords.getString("ebooks"));

					if (!item.getEbooks().trim().isEmpty()) {
						JSONArray ebooks = dataRecords.getJSONArray("ebooks");
						// System.out.println("ebooks = " + ebooks);
						if (ebooks.length() > 0) {
							JSONObject ebook = ebooks.getJSONObject(0);
							item.setEbooksLink(ebook.isNull("read_url") ? "" : ebook.getString("read_url"));

							if (item.getEbooksLink().trim().isEmpty()) {
								item.setEbooksLink(ebook.isNull("preview_url") ? "" : ebook.getString("preview_url"));
							}

							JSONObject formats = ebook == null || ebook.isNull("formats") ? null
									: ebook.getJSONObject("formats");
							JSONObject pdf = formats == null || formats.isNull("pdf") ? null
									: formats.getJSONObject("pdf");
							item.setEbooksLinkPdf(pdf == null || pdf.isNull("url") ? "" : pdf.getString("url"));
						}
					}

					if (!item.getDeweyDecimalClass().trim().isEmpty()) {
						DdcItem ddcItem = (DdcItem) session.createCriteria(DdcItem.class)
								.add(Restrictions.ilike("kode", item.getDeweyDecimalClass().trim(), MatchMode.EXACT))
								.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
						item.setDdcItem(ddcItem == null ? item.getDdcItem() : ddcItem);
					}

					item.setOpenLibraryBookChecked(true);
					item.setInfoOpenLibrary(all.toString());
					item.setItemURL(jsonItem == null || jsonItem.isNull("itemURL") ? item.getItemURL()
							: jsonItem.getString("itemURL"));
					session.getTransaction().begin();
					session.saveOrUpdate(item);
					session.getTransaction().commit();

					HibernateUtil.closeSession();

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/OpenLibrarySyncronizer.java:134");
					// Common.tampilErrorJikaAdmin(e); 
				}
			}
		}
	}
}
