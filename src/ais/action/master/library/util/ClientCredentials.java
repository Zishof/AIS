package ais.action.master.library.util;

import ais.common.Common;

/**
 * API key found in the <a href="https://code.google.com/apis/console">Google
 * apis console</a>.
 * 
 * <p>
 * Once at the Google apis console, click on "Add project...". If you've already
 * set up a project, you may use that one instead, or create a new one by
 * clicking on the arrow next to the project name and click on "Create..." under
 * "Other projects". For each API you want to use, click on the status switch to
 * flip it to "ON", and agree to the terms of service. Finally, click on "API
 * Access". Look for the section at the bottom called "Simple API Access".
 * </p>
 * 
 * @author Ravi Mistry
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-06):</b> field ini sebelumnya memakai
 * kunci API Google Books nyata sebagai nilai default hardcoded untuk konfigurasi
 * {@code google_book_key}. Default itu sudah diganti string kosong — kunci kini
 * WAJIB diisi lewat konfigurasi database, dan pemanggilan Google Books API akan
 * gagal dengan jelas (bukan diam-diam memakai kunci lama yang sudah bocor) bila
 * konfigurasi belum diisi. Kunci lama yang sebelumnya tertanam sudah lama berada
 * di riwayat SVN dan WAJIB dianggap bocor — perlu dirotasi di Google Cloud
 * Console bila masih dipakai produksi.
 * </p>
 */
public class ClientCredentials {

	/**
	 * Value of the "API key" shown under "Simple API Access".
	 */

	public static String KEY_PERPUS = "";

	static {
		ClientCredentials.KEY_PERPUS = Common.getKonfigurasi("google_book_key", ClientCredentials.KEY_PERPUS)
				.getNilai();
	}
}
