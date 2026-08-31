package ais.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Kelas pemegang (holder) konfigurasi kredensial OAuth 2.0 Google untuk tiga integrasi yang dipakai
 * AIS: Google Calendar, Google Drive, dan Google Classroom. Menyediakan client id, JSON kredensial
 * klien lengkap ({@code client_secret} dsb.), dan URL redirect OAuth untuk masing-masing integrasi,
 * beserta dua peta cache ({@link #codes}, {@link #codesAzure}) dan satu peta bertingkat
 * ({@link #codesDropbox}) yang dipakai kode lain untuk menyimpan kode otorisasi/token sementara
 * selama alur OAuth berlangsung (nama {@code codesAzure}/{@code codesDropbox} tampaknya dipakai
 * ulang secara longgar untuk penyimpanan kode otorisasi provider lain juga, bukan hanya Google —
 * kelas ini pada praktiknya berfungsi sebagai titik kumpul konfigurasi OAuth lintas provider,
 * meskipun namanya menyiratkan khusus Google).
 *
 * <h2>Pola inisialisasi malas (lazy init) dari konfigurasi runtime</h2>
 * <p>
 * Setiap nilai kredensial disimpan sebagai field statis privat dengan <b>nilai default tertanam
 * langsung di kode sumber</b> (lihat peringatan keamanan di bawah), lalu ditimpa sekali saat
 * pertama kali diakses lewat {@link #init()}: method privat ini dipanggil di awal setiap getter
 * publik (mis. {@link #getGoogle_calendar_client_id()}), memeriksa flag {@link #hasInit}, dan bila
 * belum pernah dijalankan, membaca ulang seluruh nilai dari tabel konfigurasi aplikasi lewat
 * {@code Common.getKonfigurasi(kunci, nilaiDefault)} — sehingga instalasi/tenant yang berbeda dapat
 * memakai kredensial OAuth Google yang berbeda tanpa perlu deploy ulang kode, cukup mengubah baris
 * konfigurasi terkait. Flag {@link #hasInit} membuat proses pembacaan konfigurasi hanya terjadi
 * sekali per siklus hidup JVM (bukan setiap pemanggilan getter), namun ini juga berarti perubahan
 * konfigurasi di database <b>tidak akan terbaca ulang tanpa restart aplikasi</b> setelah
 * {@link #init()} pernah dijalankan sekali.
 * </p>
 *
 * <h2>Peringatan keamanan — kredensial OAuth tertanam sebagai nilai default</h2>
 * <p>
 * <b>Field-field berikut menyimpan client secret OAuth Google dalam bentuk teks polos yang ditulis
 * langsung di kode sumber sebagai nilai default (dipakai bila konfigurasi database belum diisi):</b>
 * </p>
 * <ul>
 * <li>{@code google_calendar_key} — JSON kredensial klien Calendar, memuat
 * {@code "client_secret":"FDq7IeqDt2bbGSyMEdKlKRVt"} untuk client id
 * {@code 659282761898-oo3jg967aasck5vf8cducc4tc1ddf7ri.apps.googleusercontent.com}.</li>
 * <li>{@code google_drive_key_https} — JSON kredensial klien Drive, memuat
 * {@code "client_secret":"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"} untuk client id
 * {@code 659282761898-4bb2jhann2npbpok88mej2f2nu4dk4a4.apps.googleusercontent.com}.</li>
 * <li>{@code google_classroom_key} — JSON kredensial klien Classroom, memuat
 * {@code "client_secret":"9AZK1c6jbn0UdLGClQN1X_-O"} untuk client id
 * {@code 277118763031-89mjntmnhthiovptsjh2o7ldtj97bfoh.apps.googleusercontent.com}.</li>
 * </ul>
 * <p>
 * Sesuai instruksi tugas dokumentasi ini, kredensial tersebut TIDAK diubah/dihapus di sini —
 * temuan ini dilaporkan agar dapat ditindaklanjuti terpisah (mis. pemindahan ke penyimpanan
 * rahasia/vault dan rotasi client secret di Google Cloud Console, karena secret ini sudah
 * ter-commit ke riwayat kode sumber dan harus dianggap bocor).
 * </p>
 */
public class GoogleCommon {

	/** Nama aplikasi yang dikirim ke API Google (mis. sebagai {@code applicationName} pada klien Calendar/Drive/Classroom). */
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

	/**
	 * Memuat seluruh nilai kredensial/URL redirect dari konfigurasi aplikasi ({@code
	 * Common.getKonfigurasi}), menimpa nilai default tertanam di masing-masing field. Hanya
	 * benar-benar membaca konfigurasi pada pemanggilan pertama (dijaga oleh {@link #hasInit});
	 * pemanggilan berikutnya langsung kembali tanpa efek apa pun.
	 */
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

	/** Cache/penyimpanan sementara kode otorisasi OAuth terkait alur Azure, dikunci bebas oleh pemanggil. */
	public static Map<String, String> codesAzure = new HashMap<String, String>();
	/** Cache/penyimpanan sementara kode otorisasi OAuth Google (Calendar/Drive/Classroom), dikunci bebas oleh pemanggil. */
	public static Map<String, String> codes = new HashMap<String, String>();
	/** Cache/penyimpanan sementara kode otorisasi OAuth terkait alur Dropbox, berupa peta bertingkat (kunci luar ke peta detail) yang dikunci bebas oleh pemanggil. */
	@SuppressWarnings("rawtypes")
	public static Map<String, Map> codesDropbox = new HashMap<String, Map>();

	/** Mengembalikan client id OAuth Google Calendar, memicu {@link #init()} bila belum pernah dijalankan. */
	public static String getGoogle_calendar_client_id() {
		init();
		return google_calendar_client_id;
	}

	/** Menimpa client id OAuth Google Calendar secara langsung (tanpa melalui konfigurasi database). */
	public static void setGoogle_calendar_client_id(String google_calendar_client_id) {
		GoogleCommon.google_calendar_client_id = google_calendar_client_id;
	}

	/** Mengembalikan JSON kredensial klien OAuth Google Calendar (termasuk {@code client_secret}), memicu {@link #init()} bila belum pernah dijalankan. */
	public static String getGoogle_calendar_key() {
		init();
		return google_calendar_key;
	}

	/** Menimpa JSON kredensial klien OAuth Google Calendar secara langsung. */
	public static void setGoogle_calendar_key(String google_calendar_key) {
		GoogleCommon.google_calendar_key = google_calendar_key;
	}

	/** Mengembalikan client id OAuth Google Drive, memicu {@link #init()} bila belum pernah dijalankan. */
	public static String getGoogle_drive_client_id() {
		init();
		return google_drive_client_id_https;
	}

	/** Menimpa client id OAuth Google Drive secara langsung. */
	public static void setGoogle_drive_client_id(String google_drive_client_id_https) {
		GoogleCommon.google_drive_client_id_https = google_drive_client_id_https;
	}

	/** Mengembalikan JSON kredensial klien OAuth Google Drive (termasuk {@code client_secret}), memicu {@link #init()} bila belum pernah dijalankan. */
	public static String getGoogle_drive_key() {
		init();
		return google_drive_key_https;
	}

	/** Menimpa JSON kredensial klien OAuth Google Drive secara langsung. */
	public static void setGoogle_drive_key(String google_drive_key_https) {
		GoogleCommon.google_drive_key_https = google_drive_key_https;
	}

	/** Mengembalikan URL redirect OAuth untuk Google Calendar, memicu {@link #init()} bila belum pernah dijalankan. */
	public static String getRedirect_url_calendar() {
		init();
		return redirect_url_calendar_https;
	}

	/** Menimpa URL redirect OAuth Google Calendar secara langsung. */
	public static void setRedirect_url_calendar(String redirect_url_calendar_https) {
		GoogleCommon.redirect_url_calendar_https = redirect_url_calendar_https;
	}

	/** Mengembalikan URL redirect OAuth untuk Google Drive, memicu {@link #init()} bila belum pernah dijalankan. */
	public static String getRedirect_url_drive() {
		init();
		return redirect_url_drive_https;
	}

	/** Menimpa URL redirect OAuth Google Drive secara langsung. */
	public static void setRedirect_url_drive(String redirect_url_drive_https) {
		GoogleCommon.redirect_url_drive_https = redirect_url_drive_https;
	}

	/** Mengembalikan client id OAuth Google Classroom, memicu {@link #init()} bila belum pernah dijalankan. */
	public static String getGoogle_classroom_client_id() {
		init();
		return google_classroom_client_id;
	}

	/** Menimpa client id OAuth Google Classroom secara langsung. */
	public static void setGoogle_classroom_client_id(String google_classroom_client_id) {
		GoogleCommon.google_classroom_client_id = google_classroom_client_id;
	}

	/** Mengembalikan JSON kredensial klien OAuth Google Classroom (termasuk {@code client_secret}), memicu {@link #init()} bila belum pernah dijalankan. */
	public static String getGoogle_classroom_key() {
		init();
		return google_classroom_key;
	}

	/** Menimpa JSON kredensial klien OAuth Google Classroom secara langsung. */
	public static void setGoogle_classroom_key(String google_classroom_key) {
		GoogleCommon.google_classroom_key = google_classroom_key;
	}

	/** Mengembalikan URL redirect OAuth untuk Google Classroom, memicu {@link #init()} bila belum pernah dijalankan. */
	public static String getRedirect_url_classroom() {
		init();
		return redirect_url_classroom;
	}

	/** Menimpa URL redirect OAuth Google Classroom secara langsung. */
	public static void setRedirect_url_classroom(String redirect_url_classroom) {
		GoogleCommon.redirect_url_classroom = redirect_url_classroom;
	}

}
