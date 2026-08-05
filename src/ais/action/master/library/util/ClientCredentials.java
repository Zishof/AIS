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
 */
public class ClientCredentials {

	/**
	 * Value of the "API key" shown under "Simple API Access".
	 */

	public static String KEY_PERPUS = "AIzaSyBNXc8pLbWFN0PvDz7qMqlKIAWQlK8A_C4";

	static {
		ClientCredentials.KEY_PERPUS = Common.getKonfigurasi("google_book_key", ClientCredentials.KEY_PERPUS)
				.getNilai();
	}
}
