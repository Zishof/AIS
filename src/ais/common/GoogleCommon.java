package ais.common;

import java.util.HashMap;
import java.util.Map;

public class GoogleCommon {

	public static final String APPLICATION_NAME = "Siakad";

	private static String google_calendar_client_id = "659282761898-oo3jg967aasck5vf8cducc4tc1ddf7ri.apps.googleusercontent.com";
	private static String google_calendar_key = "{\"web\":{\"client_id\":\"659282761898-oo3jg967aasck5vf8cducc4tc1ddf7ri.apps.googleusercontent.com\",\"project_id\":\"sustained-tree-118704\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"client_secret\":\"FDq7IeqDt2bbGSyMEdKlKRVt\",\"redirect_uris\":[\"http://ecampus.id/code\"],\"javascript_origins\":[\"http://ecampus.id\"]}}";

	private static String google_drive_client_id_https = "659282761898-4bb2jhann2npbpok88mej2f2nu4dk4a4.apps.googleusercontent.com";
	private static String google_drive_key_https = "{\"web\":{\"client_id\":\"659282761898-4bb2jhann2npbpok88mej2f2nu4dk4a4.apps.googleusercontent.com\",\"project_id\":\"sustained-tree-118704\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"client_secret\":\"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\",\"redirect_uris\":[\"https://ecampus.id/code\"],\"javascript_origins\":[\"https://ecampus.id\"]}}";

	private static String redirect_url_calendar_https = "https://ecampus.id/code";
	private static String redirect_url_drive_https = "https://ecampus.id/code";

	private static String google_classroom_client_id = "277118763031-89mjntmnhthiovptsjh2o7ldtj97bfoh.apps.googleusercontent.com";
	private static String google_classroom_key = "{\"web\":{\"client_id\":\"277118763031-89mjntmnhthiovptsjh2o7ldtj97bfoh.apps.googleusercontent.com\",\"project_id\":\"classroom-277617\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"client_secret\":\"9AZK1c6jbn0UdLGClQN1X_-O\",\"redirect_uris\":[\"https://ecampus.id\",\"https://ecampus.id/code\"],\"javascript_origins\":[\"https://ecampus.id\"]}}";

	private static String redirect_url_classroom = "https://ecampus.id/code";

	private static boolean hasInit = false;

	private static void init() {
		if (!hasInit) {
			hasInit = true;
			google_calendar_client_id = Common.getKonfigurasi("google_calendar_client_id", google_calendar_client_id)
					.getNilai();
			google_calendar_key = Common.getKonfigurasi("google_calendar_key", google_calendar_key).getNilai();

			google_drive_client_id_https = Common.getKonfigurasi("google_drive_client_id_https", google_drive_client_id_https).getNilai();
			google_drive_key_https = Common.getKonfigurasi("google_drive_key_https", google_drive_key_https).getNilai();

			google_classroom_client_id = Common
					.getKonfigurasi("google_classroom_client_id_baru", google_classroom_client_id).getNilai();
			google_classroom_key = Common.getKonfigurasi("google_classroom_key_baru", google_classroom_key).getNilai();

			redirect_url_calendar_https = Common.getKonfigurasi("redirect_url_calendar_https", redirect_url_calendar_https).getNilai();
			redirect_url_drive_https = Common.getKonfigurasi("redirect_url_drive_https", redirect_url_drive_https).getNilai();
			redirect_url_classroom = Common.getKonfigurasi("redirect_url_classroom", redirect_url_classroom).getNilai();
		}
	}
	public static Map<String, String> codesAzure = new HashMap<String, String>();
	public static Map<String, String> codes = new HashMap<String, String>();
	@SuppressWarnings("rawtypes")
	public static Map<String, Map> codesDropbox = new HashMap<String, Map>();

	public static String getGoogle_calendar_client_id() {
		init();
		return google_calendar_client_id;
	}

	public static void setGoogle_calendar_client_id(String google_calendar_client_id) {
		GoogleCommon.google_calendar_client_id = google_calendar_client_id;
	}

	public static String getGoogle_calendar_key() {
		init();
		return google_calendar_key;
	}

	public static void setGoogle_calendar_key(String google_calendar_key) {
		GoogleCommon.google_calendar_key = google_calendar_key;
	}

	public static String getGoogle_drive_client_id() {
		init();
		return google_drive_client_id_https;
	}

	public static void setGoogle_drive_client_id(String google_drive_client_id_https) {
		GoogleCommon.google_drive_client_id_https = google_drive_client_id_https;
	}

	public static String getGoogle_drive_key() {
		init();
		return google_drive_key_https;
	}

	public static void setGoogle_drive_key(String google_drive_key_https) {
		GoogleCommon.google_drive_key_https = google_drive_key_https;
	}

	public static String getRedirect_url_calendar() {
		init();
		return redirect_url_calendar_https;
	}

	public static void setRedirect_url_calendar(String redirect_url_calendar_https) {
		GoogleCommon.redirect_url_calendar_https = redirect_url_calendar_https;
	}

	public static String getRedirect_url_drive() {
		init();
		return redirect_url_drive_https;
	}

	public static void setRedirect_url_drive(String redirect_url_drive_https) {
		GoogleCommon.redirect_url_drive_https = redirect_url_drive_https;
	}

	public static String getGoogle_classroom_client_id() {
		init();
		return google_classroom_client_id;
	}

	public static void setGoogle_classroom_client_id(String google_classroom_client_id) {
		GoogleCommon.google_classroom_client_id = google_classroom_client_id;
	}

	public static String getGoogle_classroom_key() {
		init();
		return google_classroom_key;
	}

	public static void setGoogle_classroom_key(String google_classroom_key) {
		GoogleCommon.google_classroom_key = google_classroom_key;
	}

	public static String getRedirect_url_classroom() {
		init();
		return redirect_url_classroom;
	}

	public static void setRedirect_url_classroom(String redirect_url_classroom) {
		GoogleCommon.redirect_url_classroom = redirect_url_classroom;
	}

}
