package ais.action.master.library.util;

import java.net.URLEncoder;
import java.text.NumberFormat;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.Books.Volumes.List;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;

import ais.common.Common;

/**
 * A sample application that demonstrates how Google Books Client Library for
 * Java can be used to query Google Books. It accepts queries in the command
 * line, and prints the results to the console.
 * 
 * $ java com.google.sample.books.BooksSample [--author|--isbn|--title]
 * "<query>"
 * 
 * Please start by reviewing the Google Books API documentation at:
 * http://code.google.com/apis/books/docs/getting_started.html
 */
public class BooksSample {

	/**
	 * Be sure to specify the name of your application. If the application name
	 * is {@code null} or blank, the application will log a warning. Suggested
	 * format is "MyCompany-ProductName/1.0".
	 */
	private static final String APPLICATION_NAME = "Perpustakaan Online";

	private static final ThreadLocal<NumberFormat> CURRENCY_FORMATTER = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			return NumberFormat.getCurrencyInstance();
		}
	};
	private static final ThreadLocal<NumberFormat> PERCENT_FORMATTER = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			return NumberFormat.getPercentInstance();
		}
	};

	public static Volumes queryGoogleBooks(JsonFactory jsonFactory, String query, Integer many) throws Exception {
		return queryGoogleBooks(jsonFactory, query, many, null);
	}

	public static Volumes queryGoogleBooks(JsonFactory jsonFactory, String query, Integer start, Integer many)
			throws Exception {

		// Set up Books client.
		final Books books = new Books.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, null)
				.setApplicationName(APPLICATION_NAME)
				.setGoogleClientRequestInitializer(new BooksRequestInitializer(ClientCredentials.KEY_PERPUS)).build();

		if (many == null) {
			many = Common.ROWS_COUNT_ON_PAGE;
		}
		if (start == null) {
			// start bisa null (mis. dipanggil dari PerpustakaanResource) -> cegah NPE start.longValue().
			start = 0;
		}

		// Set query string and filter only Google eBooks.
		System.out.println("Query: [" + query + "], start = " + start + ", many = " + many);
		List volumesList = books.volumes().list(query).setStartIndex(start.longValue()).setMaxResults(many.longValue());
		// volumesList.setFilter("ebooks");

		// Execute the query.
		Volumes volumes = volumesList.execute();
		if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
			System.out.println("No matches found.");
			// getItems() bisa null → loop di bawah akan NPE. Kembalikan hasil kosong lebih awal.
			return volumes;
		}

		// Output results.
		for (Volume volume : volumes.getItems()) {
			Volume.VolumeInfo volumeInfo = volume.getVolumeInfo();
			Volume.SaleInfo saleInfo = volume.getSaleInfo();
			System.out.println("==========");
			// Title.
			System.out.println("Title: " + volumeInfo.getTitle());
			// Author(s).
			java.util.List<String> authors = volumeInfo.getAuthors();
			if (authors != null && !authors.isEmpty()) {
				System.out.print("Author(s): ");
				for (int i = 0; i < authors.size(); ++i) {
					System.out.print(authors.get(i));
					if (i < authors.size() - 1) {
						System.out.print(", ");
					}
				}
				
			}
			// Description (if any).
			if (volumeInfo.getDescription() != null && volumeInfo.getDescription().length() > 0) {
				System.out.println("Description: " + volumeInfo.getDescription());
			}
			// Ratings (if any).
			if (volumeInfo.getRatingsCount() != null && volumeInfo.getRatingsCount() > 0) {
				int fullRating = (int) Math.round(volumeInfo.getAverageRating().doubleValue());
				System.out.print("User Rating: ");
				for (int i = 0; i < fullRating; ++i) {
					System.out.print("*");
				}
				System.out.println(" (" + volumeInfo.getRatingsCount() + " rating(s))");
			}
			// Price (if any).
			if (saleInfo != null && "FOR_SALE".equals(saleInfo.getSaleability())) {
				double save = saleInfo.getListPrice().getAmount() - saleInfo.getRetailPrice().getAmount();
				if (save > 0.0) {
					System.out.print("List: " + CURRENCY_FORMATTER.get().format(saleInfo.getListPrice().getAmount()) + "  ");
				}
				System.out.print(
						"Google eBooks Price: " + CURRENCY_FORMATTER.get().format(saleInfo.getRetailPrice().getAmount()));
				if (save > 0.0) {
					System.out.print("  You Save: " + CURRENCY_FORMATTER.get().format(save) + " ("
							+ PERCENT_FORMATTER.get().format(save / saleInfo.getListPrice().getAmount()) + ")");
				}
				
			}
			// Access status.
			String accessViewStatus = volume.getAccessInfo().getAccessViewStatus();
			String message = "Additional information about this book is available from Google eBooks at:";
			if ("FULL_PUBLIC_DOMAIN".equals(accessViewStatus)) {
				message = "This public domain book is available for free from Google eBooks at:";
			} else if ("SAMPLE".equals(accessViewStatus)) {
				message = "A preview of this book is available from Google eBooks at:";
			}
			System.out.println(message);
			// Link to Google eBooks.
			System.out.println(volumeInfo.getInfoLink());
		}
		System.out.println("==========");
		System.out.println(volumes.getTotalItems() + " total results at http://books.google.com/ebooks?q="
				+ URLEncoder.encode(query, "UTF-8"));

		return volumes;
	}

}