package ais.action.master.helper.util;

import java.net.InetAddress;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;

import ais.action.master.library.util.BooksSample;
import ais.action.servlet.CheckISBN;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.library.Item;

public class GoogleBookSynchronized extends TimerTask {

	private String localIp = "";

	public static JsonFactory jsonFactory = new JacksonFactory();

	public GoogleBookSynchronized() {
		InetAddress thisIp;
		try {
			thisIp = InetAddress.getLocalHost();
			localIp = thisIp.getHostName();
			System.out.println("IP:" + localIp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

	}

	@Override
	public void run() {
		doProcess();
	}

	private void doProcess() {

		if (true) {
			return;
		}

	}

	@SuppressWarnings({})
	public static void process(Item item) throws Exception {
		if (Common.bolehKonfigurasi("terintegrasi_dengan_google_book_untuk_isbn", Konfigurasi.TIDAK_AKTIF)) {

			if (item != null && item.getIsbn() != null && !item.getIsbn().trim().isEmpty()) {

				String isbn = item.getIsbn().trim();

				try {

					isbn = org.apache.commons.lang3.StringUtils.replace(isbn, "-", "");

					String query = "isbn:" + isbn;

					Volumes volumes = BooksSample.queryGoogleBooks(jsonFactory, query, 0, 1);

					if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
						return;
					}

					Volume volume = volumes.getItems().get(0);
					CheckISBN.simpanVolume(volume, item);

				} catch (Exception e) {
					System.err.println(e.getMessage());
				}

				if (item.getGoogleBookChecked() == null || !item.getGoogleBookChecked()) {
					Session session = HibernateUtil.currentNativeSession();
					item.setGoogleBookChecked(true);
					session.getTransaction().begin();
					Common.refreshUpdate(session, (item));
					session.getTransaction().commit();

					HibernateUtil.closeSession();
				}
			}

			if (item != null && item.getIsbn10() != null && !item.getIsbn10().trim().isEmpty()) {

				String isbn = item.getIsbn10().trim();

				try {

					isbn = org.apache.commons.lang3.StringUtils.replace(isbn, "-", "");

					String query = "isbn:" + isbn;

					Volumes volumes = BooksSample.queryGoogleBooks(jsonFactory, query, 0, 1);

					if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
						return;
					}

					Volume volume = volumes.getItems().get(0);
					CheckISBN.simpanVolume(volume, item);

				} catch (Exception e) {
					System.err.println(e.getMessage());
				}

				if (item.getGoogleBookChecked() == null || !item.getGoogleBookChecked()) {
					Session session = HibernateUtil.currentNativeSession();
					item.setGoogleBookChecked(true);
					session.getTransaction().begin();
					Common.refreshUpdate(session, (item));
					session.getTransaction().commit();

					HibernateUtil.closeSession();
				}
			}
		}
	}

	@SuppressWarnings({})
	public static void processByTitle(Item item) throws Exception {

		if (item == null || item.getIsbn() == null || item.getIsbn().trim().equals("")) {
			return;
		}

		String nama = item.getNama().trim();

		try {

			nama = org.apache.commons.lang3.StringUtils.replace(nama, "-", "");

			String query = "intitle:" + nama;

			Volumes volumes = BooksSample.queryGoogleBooks(jsonFactory, query, 0, 1);

			if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
				return;
			}

			Volume volume = volumes.getItems().get(0);
			CheckISBN.simpanVolume(volume, item);

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		if (item.getGoogleBookChecked() == null || !item.getGoogleBookChecked()) {
			Session session = HibernateUtil.currentNativeSession();
			item.setGoogleBookChecked(true);
			session.getTransaction().begin();
			Common.refreshUpdate(session, (item));
			session.getTransaction().commit();

			HibernateUtil.closeSession();
		}

	}

}
