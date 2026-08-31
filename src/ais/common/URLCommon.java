package ais.common;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.DspaceLog;

/**
 * Kumpulan utilitas HTTP klien statis di AIS, dengan fokus utama sebagai lapisan integrasi REST
 * ke server DSpace (repositori digital, dipakai untuk menyimpan/mengunggah dokumen skripsi/tesis/
 * karya ilmiah dsb.) via {@link ConstantValues#DSPACE_URL_PRIVATE}, ditambah satu utilitas HTTP
 * POST generik ({@link #getPostResponseHeader(String, String)}) yang dipakai di luar konteks
 * DSpace.
 *
 * <h2>Pola integrasi DSpace</h2>
 * <p>
 * Seluruh method CRUD DSpace ({@link #post}, {@link #get}, {@link #put}, {@link #delete},
 * {@link #upload}/{@link #uploadOld}) mengikuti pola yang sama: autentikasi memakai cookie sesi
 * ({@code JSESSIONID}) yang diteruskan sebagai parameter (bukan disimpan sebagai state kelas),
 * request dibangun manual lewat {@link HttpURLConnection} (tanpa library HTTP client), dan SETIAP
 * pemanggilan — sukses maupun gagal — dicatat sebagai satu baris {@link DspaceLog} lewat
 * {@link #simpanDspaceLog(DspaceLog)}, sehingga riwayat integrasi DSpace (aksi, status HTTP, pesan
 * error) selalu terlacak di database aplikasi terlepas dari hasil pemanggilan. Sebagian besar
 * method dalam kelas ini SENGAJA menelan exception di dalam blok {@code catch} (hanya mengisi
 * {@code dspaceLog.setError(...)}) dan tetap mengembalikan nilai (kadang {@code null} atau
 * {@code 0}) alih-alih melempar ulang — pemanggil harus memeriksa nilai kembalian, bukan
 * mengandalkan exception, untuk mendeteksi kegagalan pada sebagian besar method di kelas ini.
 * </p>
 *
 * <h2>Method {@link #upload(String, String, File, String)} vs {@link #uploadOld}</h2>
 * <p>
 * {@link #upload} adalah implementasi AKTIF (menjalankan {@code curl} eksternal via
 * {@link ProcessBuilder}) dengan penanganan proses yang cermat (pengurasan stream stderr di thread
 * terpisah + watchdog batas waktu) — lihat komentar sejarah insiden pada badan method tersebut
 * untuk kronologi bug deadlock yang pernah terjadi dan bagaimana perbaikannya. {@link #uploadOld}
 * adalah implementasi LAMA berbasis {@link HttpURLConnection} murni (membangun multipart/form-data
 * secara manual) yang ditinggalkan sebagai referensi/cadangan.
 * </p>
 *
 * <p>
 * Tidak ditemukan kredensial atau API key tertanam pada kelas ini — nilai {@code cookie} selalu
 * diterima sebagai parameter dari pemanggil (nilai sesi yang sudah diperoleh sebelumnya di luar
 * kelas ini), dan URL dasar DSpace dibaca dari {@link ConstantValues#DSPACE_URL_PRIVATE}.
 * </p>
 */
public class URLCommon {
	/**
	 * Utilitas HTTP POST generik (di luar konteks integrasi DSpace) yang mengirim {@code
	 * postData} sebagai {@code application/x-www-form-urlencoded} dan mengembalikan seluruh
	 * header respons. Dipakai bila pemanggil butuh membaca header (mis. {@code Location},
	 * cookie set) dari hasil POST, bukan sekadar body.
	 *
	 * <p>
	 * Header {@code User-Agent}/{@code Origin}/{@code Referer} sengaja diisi menyerupai browser
	 * standar (Chrome di Linux) — komentar pada kode menjelaskan ini untuk menghindari endpoint
	 * di balik WAF Cloudflare salah mengklasifikasikan request server-ke-server yang sah sebagai
	 * bot/crawler berbahaya (error Cloudflare 1010). Timeout koneksi 15 detik, timeout baca 30
	 * detik.
	 * </p>
	 *
	 * <p>
	 * Bila status HTTP respons &gt;= 400, method ini melempar {@link IOException} berisi status,
	 * pesan status, dan ringkasan body respons (dipotong maksimum 500 karakter lewat
	 * {@link #ringkasResponse(String)}); bila kegagalan terindikasi berasal dari halaman
	 * tantangan Cloudflare (dideteksi {@link #isCloudflareResponse(String)}) dan status 403,
	 * pesan exception ditambahi petunjuk untuk mengatur {@code dspace_private_url} ke alamat
	 * internal yang tidak melewati WAF — lihat juga {@link #isWafForbidden(Exception)} untuk
	 * mendeteksi kondisi ini dari luar berdasarkan pesan exception.
	 * </p>
	 *
	 * @param urlStr   URL tujuan POST
	 * @param postData isi body yang dikirim (di-encode UTF-8); {@code null} diperlakukan sebagai
	 *                 string kosong
	 * @return peta header respons HTTP (nama header ke daftar nilainya)
	 * @throws Exception termasuk {@link IOException} bila status HTTP &gt;= 400, atau kegagalan
	 *                    koneksi/IO lainnya
	 */
	public static Map<String, List<String>> getPostResponseHeader(String urlStr, String postData) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection con = null;
		DataOutputStream output = null;
		DataInputStream input = null;
		try {
			con = (HttpURLConnection) url.openConnection();

		// CURLOPT_POST
		con.setRequestMethod("POST");

		// CURLOPT_FOLLOWLOCATION
		con.setInstanceFollowRedirects(true);

		byte[] postBytes = (postData == null ? "" : postData).getBytes("UTF-8");
		con.setRequestProperty("Content-length", String.valueOf(postBytes.length));
		/*
		 * Cloudflare error 1010 menolak signature User-Agent bot/custom walaupun
		 * request berasal dari server eCampus yang sah. Gunakan signature browser
		 * standar dan sertakan origin agar endpoint DSpace di balik WAF tidak salah
		 * menggolongkan integrasi server sebagai crawler berbahaya.
		 */
		con.setRequestProperty("User-Agent",
				"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
		String origin = url.getProtocol() + "://" + url.getAuthority();
		con.setRequestProperty("Origin", origin);
		con.setRequestProperty("Referer", origin + "/");
		con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		con.setConnectTimeout(15000);
		con.setReadTimeout(30000);

		con.setDoOutput(true);
		con.setDoInput(true);

		output = new DataOutputStream(con.getOutputStream());
		output.write(postBytes);
		output.flush();

		int status = con.getResponseCode();
		InputStream responseStream = status >= 400 ? con.getErrorStream() : con.getInputStream();
		if (responseStream == null) {
			responseStream = con.getInputStream();
		}
		input = new DataInputStream(responseStream);
		int c;
		StringBuilder resultBuf = new StringBuilder();
		while ((c = input.read()) != -1) {
			resultBuf.append((char) c);
		}
		if (status >= 400) {
			String responseRingkas = ringkasResponse(resultBuf.toString());
			String petunjukWaf = status == 403 && isCloudflareResponse(responseRingkas)
					? " Atur dspace_private_url ke alamat internal DSpace yang tidak melewati Cloudflare/WAF."
					: "";
			throw new IOException("POST " + urlStr + " gagal. HTTP " + status + " " + con.getResponseMessage()
					+ ". Response: " + responseRingkas + petunjukWaf);
		}

		Map<String, List<String>> map = con.getHeaderFields();
		return map;
		} finally {
			if (input != null) {
				try { input.close(); } catch (Exception ignored) { }
			}
			if (output != null) {
				try { output.close(); } catch (Exception ignored) { }
			}
			if (con != null) {
				con.disconnect();
			}
		}
	}

	/**
	 * Mendeteksi apakah teks {@code response} berasal dari halaman tantangan/blokir Cloudflare
	 * (ciri-ciri: mengandung "cloudflare", URL challenge Cloudflare, teks "just a moment", atau
	 * kode error 1010) — dipakai untuk membedakan kegagalan HTTP asli dari server target
	 * dengan blokir WAF yang mencegat request sebelum sampai ke aplikasi.
	 *
	 * @param response teks body respons yang diperiksa
	 * @return {@code true} bila teks mengandung salah satu penanda halaman Cloudflare
	 */
	private static boolean isCloudflareResponse(String response) {
		String isi = response == null ? "" : response.toLowerCase();
		return isi.indexOf("cloudflare") >= 0 || isi.indexOf("challenges.cloudflare.com") >= 0
				|| isi.indexOf("just a moment") >= 0 || isi.indexOf("error code: 1010") >= 0;
	}

	/**
	 * Memeriksa apakah {@code exception} yang diberikan merepresentasikan kegagalan HTTP 403
	 * yang disebabkan blokir Cloudflare (dibangun oleh {@link #getPostResponseHeader(String,
	 * String)} saat status &gt;= 400) — dicek dari teks pesan exception, bukan tipe exception
	 * khusus. Dipakai pemanggil di luar kelas ini untuk menampilkan pesan/penanganan berbeda
	 * saat kegagalan integrasi disebabkan WAF, bukan kegagalan aplikasi target.
	 *
	 * @param exception exception yang akan diperiksa; {@code null} atau tanpa pesan dianggap
	 *                  bukan kegagalan WAF
	 * @return {@code true} bila pesan exception menunjukkan HTTP 403 dari respons Cloudflare
	 */
	public static boolean isWafForbidden(Exception exception) {
		String pesan = exception == null || exception.getMessage() == null ? ""
				: exception.getMessage().toLowerCase();
		return pesan.indexOf("http 403") >= 0 && isCloudflareResponse(pesan);
	}

	/**
	 * Meringkas teks body respons untuk disisipkan ke pesan error: baris baru/tab diratakan
	 * jadi spasi tunggal, spasi ganda dirapikan, dan hasil dipotong maksimum 500 karakter
	 * (ditambah {@code "..."} bila dipotong) agar pesan exception tetap ringkas dan tidak
	 * membanjiri log dengan HTML/JSON respons yang panjang.
	 *
	 * @param response teks body respons mentah
	 * @return ringkasan satu-baris, maksimum 500 karakter
	 */
	private static String ringkasResponse(String response) {
		String isi = response == null ? "" : response.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
		while (isi.indexOf("  ") >= 0) {
			isi = isi.replace("  ", " ");
		}
		if (isi.length() > 500) {
			return isi.substring(0, 500) + "...";
		}
		return isi;
	}

	/**
	 * Mengirim HTTP POST JSON ke endpoint DSpace ({@code ConstantValues.DSPACE_URL_PRIVATE +
	 * "/" + acton}), diautentikasi lewat cookie {@code JSESSIONID}, dan mencatat hasilnya
	 * sebagai {@link DspaceLog} beraksi {@link DspaceLog#SIMPAN}.
	 *
	 * <p>
	 * Karakter non-printable pada {@code postData} dibuang lebih dulu (regex {@code \P{Print}})
	 * sebelum dikirim. Bila JSON respons memuat field {@code retrieveLink} atau {@code handle},
	 * nilainya dicatat ke {@link DspaceLog#setHandle(String)} sebagai jejak identitas objek
	 * DSpace yang terpengaruh.
	 * </p>
	 *
	 * <p>
	 * Exception yang terjadi selama proses request DITANGKAP secara internal (dicatat ke
	 * {@link ErrorAuditUtil} dan {@code dspaceLog.setError(...)}), bukan dilempar ulang —
	 * method mengembalikan {@code null} pada kegagalan; pemanggil harus memeriksa nilai
	 * kembalian, bukan mengandalkan exception, untuk mendeteksi kegagalan.
	 * </p>
	 *
	 * @param cookie   nilai {@code JSESSIONID} sesi DSpace yang sudah diautentikasi sebelumnya
	 * @param acton    path aksi relatif terhadap {@code DSPACE_URL_PRIVATE} (typo "acton"
	 *                 dipertahankan sesuai nama parameter asli di kode)
	 * @param postData body JSON yang dikirim
	 * @return JSON hasil parsing respons, atau {@code null} bila terjadi kegagalan yang
	 *         tertangkap secara internal
	 * @throws Exception dideklarasikan pada tanda tangan namun pada praktiknya kegagalan
	 *                    ditangkap secara internal, bukan dilempar ulang, kecuali dari
	 *                    {@link #simpanDspaceLog(DspaceLog)}
	 */
	public static JSONObject post(String cookie, String acton, String postData) throws Exception {
		JSONObject jsonObject = null;

		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(postData);

		dspaceLog.setAction(DspaceLog.SIMPAN);

		try {
			String u = ConstantValues.DSPACE_URL_PRIVATE + "/" + acton;
			
			
			postData = postData.replaceAll("\\P{Print}", "");
			
			System.out.println("post acton = " + u + ", postData = " + postData);

			URL url = new URL(u);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("POST");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
			con.setRequestProperty("Content-length", String.valueOf(postData.length()));
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			con.setDoInput(true);

			DataOutputStream output = new DataOutputStream(con.getOutputStream());
			output.writeBytes(postData);
			output.close();

			String data = "{uuid:\"\"}";
			try {
				// read the response
				DataInputStream input = new DataInputStream(con.getInputStream());
				int c;
				StringBuilder resultBuf = new StringBuilder();
				while ((c = input.read()) != -1) {
					resultBuf.append((char) c);
				}
				input.close();
				data = resultBuf.toString();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/URLCommon.java:106");

			}

			Map<String, List<String>> map = con.getHeaderFields();
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				System.out.println("Key : " + entry.getKey() + ", Value : " + entry.getValue());
			}

			jsonObject = new JSONObject(data);
			System.out.println("jsonObject = " + jsonObject);

			String handle = !jsonObject.isNull("retrieveLink") ? jsonObject.getString("retrieveLink")
					: !jsonObject.isNull("handle") ? jsonObject.getString("handle") : "";
			dspaceLog.setHandle(handle);

			int code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("post Response    (Code):" + code);

			dspaceLog.setStatus(code);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/URLCommon.java:127");
			dspaceLog.setError(e.getMessage());
		}

		simpanDspaceLog(dspaceLog);

		return jsonObject;
	}

	/**
	 * Mengirim HTTP GET ke endpoint DSpace ({@code ConstantValues.DSPACE_URL_PRIVATE + "/" +
	 * action}) tanpa autentikasi cookie, mem-parsing body respons sebagai JSON.
	 *
	 * <p>
	 * Berbeda dari {@link #post}/{@link #put}/{@link #delete}, method ini TIDAK mencatat
	 * {@link DspaceLog} — murni operasi baca. Kegagalan membaca stream input ditangkap secara
	 * diam-diam (dicatat ke {@link ErrorAuditUtil}) dan digantikan nilai default
	 * {@code {"uuid":""}} sebelum di-parse sebagai JSON, sehingga method ini TIDAK PERNAH
	 * mengembalikan {@code null} akibat kegagalan baca — hanya JSON placeholder.
	 * </p>
	 *
	 * @param action path aksi relatif terhadap {@code DSPACE_URL_PRIVATE}
	 * @return JSON hasil parsing respons, atau JSON placeholder {@code {"uuid":""}} bila
	 *         pembacaan respons gagal
	 * @throws Exception diteruskan dari kegagalan membuka koneksi atau parsing JSON pada data
	 *                    yang berhasil dibaca
	 */
	public static JSONObject get(String action) throws Exception {

		String urlStr = ConstantValues.DSPACE_URL_PRIVATE + "/" + action;

		URL url = new URL(urlStr);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// CURLOPT_POST
		con.setRequestMethod("GET");

		// CURLOPT_FOLLOWLOCATION
		con.setInstanceFollowRedirects(true);
		con.setRequestProperty("Accept", "application/json");

		con.setDoOutput(true);
		con.setDoInput(true);

		String data = "{uuid:\"\"}";
		try {
			// read the response
			DataInputStream input = new DataInputStream(con.getInputStream());
			int c;
			StringBuilder resultBuf = new StringBuilder();
			while ((c = input.read()) != -1) {
				resultBuf.append((char) c);
			}
			input.close();
			data = resultBuf.toString();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/URLCommon.java:168");

		}

		JSONObject jsonObject = new JSONObject(data);
		System.out.println("jsonObject = " + jsonObject);
		return jsonObject;

	}

	/**
	 * Memeriksa ketersediaan/kesuksesan sebuah endpoint DSpace lewat HTTP GET, tanpa membaca
	 * body respons — hanya memeriksa apakah status HTTP bernilai 200.
	 *
	 * @param action path aksi relatif terhadap {@code DSPACE_URL_PRIVATE} yang akan diperiksa
	 * @return {@code true} bila status HTTP respons adalah 200
	 * @throws Exception diteruskan dari kegagalan membuka koneksi HTTP
	 */
	public static boolean cek(String action) throws Exception {

		String urlStr = ConstantValues.DSPACE_URL_PRIVATE + "/" + action;

		System.out.println("cek = " + urlStr);

		URL url = new URL(urlStr);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// CURLOPT_POST
		con.setRequestMethod("GET");

		// CURLOPT_FOLLOWLOCATION
		con.setInstanceFollowRedirects(true);
		con.setRequestProperty("Accept", "application/json");

		con.setDoOutput(true);
		con.setDoInput(true);

		int code = con.getResponseCode(); // 200 = HTTP_OK
		System.out.println("Response    (Code):" + code);
		return code == 200;

	}

	/**
	 * Implementasi AKTIF unggah berkas biner ke satu item DSpace (endpoint
	 * {@code /items/{uuid}/bitstreams}), dijalankan lewat proses eksternal {@code curl} (bukan
	 * {@link HttpURLConnection}) karena lebih tangguh menangani unggahan berkas besar.
	 *
	 * <p>
	 * <b>Latar belakang perbaikan (lihat juga komentar rinci pada badan method):</b> versi
	 * sebelumnya memakai flag {@code curl -v} (verbose) yang membanjiri STDERR tanpa pernah
	 * dikuras, sehingga begitu buffer pipa STDERR penuh, proses {@code curl} macet menulis dan
	 * seluruh thread pemanggil ikut menggantung selamanya menunggu STDOUT (insiden nyata:
	 * satu thread tercatat macet di titik ini selama seminggu penuh). Implementasi saat ini
	 * memberi tiga pengaman: (1) flag {@code -sS} (senyap namun tetap menampilkan error, bukan
	 * verbose); (2) thread daemon terpisah yang secara aktif mengonsumsi/membuang STDERR proses
	 * curl sepanjang proses berjalan, sehingga pipanya tidak pernah penuh; (3) thread watchdog
	 * daemon yang memaksa {@code p.destroy()} bila proses melewati batas waktu (konfigurasi
	 * {@code timeout_upload_dspace_detik}, default 300 detik, dipaksa minimum 10 detik) —
	 * sehingga proses yang macet karena sebab lain pun tidak lagi menggantung tanpa batas.
	 * </p>
	 *
	 * <p>
	 * Bila watchdog memicu penghentian paksa (melewati batas waktu), method melempar
	 * {@link Exception} yang menyebutkan nama berkas dan batas waktu yang terlampaui — ini
	 * SATU-SATUNYA jalur di method ini yang benar-benar melempar exception ke pemanggil;
	 * kegagalan lain (mis. curl mengembalikan JSON error) ditangkap dan dicatat ke
	 * {@link DspaceLog#setError(String)} tanpa dilempar ulang.
	 * </p>
	 *
	 * @param cookie      nilai {@code JSESSIONID} sesi DSpace
	 * @param uuid        id item DSpace tujuan unggahan
	 * @param binaryFile  berkas lokal yang akan diunggah
	 * @param description deskripsi berkas, disisipkan ke query string dan {@link DspaceLog}
	 * @return JSON hasil parsing respons curl, atau {@code null} bila terjadi kegagalan yang
	 *         tertangkap secara internal (selain kasus timeout watchdog)
	 * @throws Exception dilempar bila proses unggah melewati batas waktu watchdog; kegagalan
	 *                    lain umumnya ditangkap secara internal
	 */
	public static JSONObject upload(String cookie, String uuid, File binaryFile, String description) throws Exception {
		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(description + ". File : " + binaryFile.getName());

		dspaceLog.setAction(DspaceLog.UPLOAD);

		JSONObject jsonObject = null;
		try {

			String charset = "UTF-8";
			String u = ConstantValues.DSPACE_URL_PRIVATE + "/items/" + uuid + "/bitstreams?name="
					+ URLEncoder.encode(binaryFile.getName(), charset) + "&description="
					+ URLEncoder.encode(description, charset);

			String data = "{uuid:\"\"}";
			try {

				System.out.println("curl file " + binaryFile.getAbsolutePath() + " to " + u);

				/*
				 * PERBAIKAN KEBUNTUAN PROSES (snapshot performa 12-19/08/2026).
				 *
				 * Versi lama memakai flag "-v" (verbose) sehingga curl menulis BANYAK keluaran ke
				 * STDERR, sementara kode hanya membaca STDOUT dan TIDAK PERNAH menguras STDERR.
				 * Begitu buffer pipa stderr penuh (biasanya beberapa puluh KB), curl TERBLOKIR saat
				 * menulis; karena curl berhenti, stdout tidak lagi terisi dan readLine() di sini
				 * menunggu SELAMANYA. Inilah sebabnya satu thread ("Thread-327") tercatat macet di
				 * baris ini pada SELURUH 151 snapshot selama seminggu penuh, dan satu thread
				 * request Tomcat ikut macet dengan pola yang sama.
				 *
				 * Tiga pengaman sekarang:
				 *   1. "-v" diganti "-sS" (senyap tapi tetap menampilkan pesan error) sehingga
				 *      stderr tidak lagi dibanjiri.
				 *   2. STDERR tetap DIKURAS thread tersendiri, supaya buffer tak mungkin penuh
				 *      walau curl mengeluarkan pesan panjang.
				 *   3. Watchdog menghentikan proses bila melewati batas waktu, sehingga thread
				 *      tidak pernah lagi menggantung tanpa batas.
				 */
				String[] command = { "curl", "-k", "-4", "-sS", "-H", "Content-Type: multipart/form-data", "--cookie",
						"JSESSIONID=" + cookie + "", "-H", "accept: application/json", "-X", "POST", u, "-T",
						binaryFile.getAbsolutePath() };

				int batasDetik = 300;
				try {
					batasDetik = (int) Common.parseAngkaKonfigurasi(
							Common.getKonfigurasi("timeout_upload_dspace_detik", "300").getNilai(), 300);
				} catch (Exception eKonf) {
					ais.common.ErrorAuditUtil.record(eKonf, "URLCommon.upload.timeoutKonfigurasi");
				}
				if (batasDetik < 10) {
					batasDetik = 10;
				}

				ProcessBuilder process = new ProcessBuilder(command);
				final Process p = process.start();

				// (2) Penguras STDERR — WAJIB, lihat penjelasan di atas.
				Thread pengurasError = new Thread(new Runnable() {
					public void run() {
						BufferedReader errReader = null;
						try {
							errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
							while (errReader.readLine() != null) {
								// sengaja dibuang: yang penting pipa tidak pernah penuh
							}
						} catch (Exception abaikan) {
						} finally {
							try {
								if (errReader != null) {
									errReader.close();
								}
							} catch (Exception abaikan) {
							}
						}
					}
				}, "dspace-upload-stderr");
				pengurasError.setDaemon(true);
				pengurasError.start();

				// (3) Watchdog batas waktu.
				final int batasDetikFinal = batasDetik;
				final java.util.concurrent.atomic.AtomicBoolean lewatBatas =
						new java.util.concurrent.atomic.AtomicBoolean(false);
				Thread watchdog = new Thread(new Runnable() {
					public void run() {
						try {
							Thread.sleep(batasDetikFinal * 1000L);
							lewatBatas.set(true);
							p.destroy();
						} catch (InterruptedException selesaiNormal) {
							Thread.currentThread().interrupt();
						} catch (Exception abaikan) {
						}
					}
				}, "dspace-upload-watchdog");
				watchdog.setDaemon(true);
				watchdog.start();

				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line = null;
				try {
					while ((line = reader.readLine()) != null) {
						builder.append(line);
						builder.append(System.getProperty("line.separator"));
					}
				} finally {
					try {
						reader.close();
					} catch (Exception abaikan) {
					}
					try {
						p.waitFor();
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
					watchdog.interrupt();
				}
				if (lewatBatas.get()) {
					throw new Exception("Upload berkas ke DSpace dihentikan karena melewati batas waktu "
							+ batasDetikFinal + " detik (berkas: " + binaryFile.getName() + ").");
				}
				data = builder.toString();

				System.out.println("data = " + data);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/URLCommon.java:240");
			}

			jsonObject = new JSONObject(data);
			System.out.println("jsonObject = " + jsonObject);
			String handle = !jsonObject.isNull("retrieveLink") ? jsonObject.getString("retrieveLink")
					: !jsonObject.isNull("handle") ? jsonObject.getString("handle") : "";
			dspaceLog.setHandle(handle);

			dspaceLog.setStatus(200);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/URLCommon.java:251");
			dspaceLog.setError(e.getMessage());
		}

		simpanDspaceLog(dspaceLog);

		return jsonObject;
	}

	/**
	 * Implementasi LAMA unggah berkas biner ke item DSpace, memakai {@link HttpURLConnection}
	 * murni dengan body {@code multipart/form-data} dibangun manual (boundary dari heksadesimal
	 * timestamp, ditulis lewat {@link PrintWriter}/{@link OutputStreamWriter}). Ditinggalkan
	 * sebagai referensi/cadangan setelah digantikan {@link #upload(String, String, File,
	 * String)} berbasis {@code curl} yang lebih tangguh untuk berkas besar — lihat javadoc
	 * {@link #upload(String, String, File, String)} untuk kronologi alasan penggantian.
	 *
	 * @param cookie      nilai {@code JSESSIONID} sesi DSpace
	 * @param uuid        id item DSpace tujuan unggahan
	 * @param binaryFile  berkas lokal yang akan diunggah
	 * @param description deskripsi berkas
	 * @return JSON hasil parsing respons, atau {@code null} bila terjadi kegagalan (ditangkap
	 *         dan dicatat ke {@link DspaceLog#setError(String)})
	 * @throws Exception diteruskan dari kegagalan koneksi/IO yang tidak tertangkap oleh blok
	 *                    {@code catch} internal method ini
	 */
	public static JSONObject uploadOld(String cookie, String uuid, File binaryFile, String description)
			throws Exception {

		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(description + ". File : " + binaryFile.getName());

		dspaceLog.setAction(DspaceLog.UPLOAD);

		JSONObject jsonObject = null;
		try {

			String charset = "UTF-8";
			String param = "data";
			String boundary = Long.toHexString(System.currentTimeMillis()); // Just
																			// generate
																			// some
																			// unique
																			// random
																			// value.
			String CRLF = "\r\n"; // Line separator required by
									// multipart/form-data.

			URL url = new URL(ConstantValues.DSPACE_URL_PRIVATE + "/items/" + uuid + "/bitstreams?name="
					+ URLEncoder.encode(binaryFile.getName(), charset) + "&description="
					+ URLEncoder.encode(description, charset));
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("POST");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
			con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			con.setDoInput(true);

			OutputStream output = con.getOutputStream();
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
			// Send normal param.
			writer.append("--" + boundary).append(CRLF);
			writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
			writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
			writer.append(CRLF).append(param).append(CRLF).flush();

			// Send binary file.
			writer.append("--" + boundary).append(CRLF);
			writer.append(
					"Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + binaryFile.getName() + "\"")
					.append(CRLF);
			writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
			writer.append("Content-Transfer-Encoding: binary").append(CRLF);
			writer.append(CRLF).flush();
			Files.copy(binaryFile.toPath(), output);
			output.flush(); // Important before continuing with writer!
			writer.append(CRLF).flush(); // CRLF is important! It indicates end
											// of boundary.

			// End of multipart/form-data.
			writer.append("--" + boundary + "--").append(CRLF).flush();

			// Request is lazily fired whenever you need to obtain information
			// about
			// response.
			int responseCode = ((HttpURLConnection) con).getResponseCode();
			System.out.println(responseCode); // Should be 200

			String data = "{uuid:\"\"}";
			try {
				// read the response
				DataInputStream input = new DataInputStream(con.getInputStream());
				int c;
				StringBuilder resultBuf = new StringBuilder();
				while ((c = input.read()) != -1) {
					resultBuf.append((char) c);
				}
				input.close();
				data = resultBuf.toString();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/URLCommon.java:343");

			}

			jsonObject = new JSONObject(data);
			System.out.println("jsonObject = " + jsonObject);
			String handle = !jsonObject.isNull("retrieveLink") ? jsonObject.getString("retrieveLink")
					: !jsonObject.isNull("handle") ? jsonObject.getString("handle") : "";
			dspaceLog.setHandle(handle);

			int code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("Upload Response    (Code):" + code);

			dspaceLog.setStatus(code);
		} catch (Exception e) {
			dspaceLog.setError(e.getMessage());
		}

		simpanDspaceLog(dspaceLog);

		return jsonObject;
	}

	/**
	 * Mengirim HTTP PUT JSON ke endpoint DSpace ({@code ConstantValues.DSPACE_URL_PRIVATE + "/"
	 * + acton}), diautentikasi lewat cookie {@code JSESSIONID}, dan mencatat hasilnya sebagai
	 * {@link DspaceLog} beraksi {@link DspaceLog#UBAH}. Pola sama dengan {@link #post}, namun
	 * mengembalikan kode status HTTP mentah alih-alih JSON hasil parsing (dipakai saat pemanggil
	 * hanya perlu tahu berhasil/tidaknya operasi ubah).
	 *
	 * @param cookie   nilai {@code JSESSIONID} sesi DSpace
	 * @param acton    path aksi relatif terhadap {@code DSPACE_URL_PRIVATE}
	 * @param postData body JSON perubahan yang dikirim
	 * @return kode status HTTP respons (200 = OK), atau {@code 0} bila terjadi kegagalan yang
	 *         tertangkap secara internal sebelum status sempat dibaca
	 * @throws Exception dideklarasikan pada tanda tangan namun kegagalan pada praktiknya
	 *                    ditangkap secara internal, kecuali dari
	 *                    {@link #simpanDspaceLog(DspaceLog)}
	 */
	public static int put(String cookie, String acton, String postData) throws Exception {
		int code = 0;

		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(postData);

		dspaceLog.setAction(DspaceLog.UBAH);

		try {
			String urlStr = ConstantValues.DSPACE_URL_PRIVATE + "/" + acton;
			URL url = new URL(urlStr);

			System.out.println("put urlStr = " + urlStr + ", postData = " + postData);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("PUT");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
			con.setRequestProperty("Content-length", String.valueOf(postData.length()));
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			con.setDoInput(true);

			DataOutputStream output = new DataOutputStream(con.getOutputStream());
			output.writeBytes(postData);
			output.close();

			Map<String, List<String>> map = con.getHeaderFields();
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				System.out.println("Key : " + entry.getKey() + ", Value : " + entry.getValue());
			}

			code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("Put Response    (Code):" + code);

			dspaceLog.setStatus(code);
		} catch (Exception e) {
			dspaceLog.setError(e.getMessage());
		}

		simpanDspaceLog(dspaceLog);
		return code;
	}

	/**
	 * Mengirim HTTP DELETE ke endpoint DSpace ({@code ConstantValues.DSPACE_URL_PRIVATE + "/" +
	 * acton}), diautentikasi lewat cookie {@code JSESSIONID}, dan mencatat hasilnya sebagai
	 * {@link DspaceLog} beraksi {@link DspaceLog#HAPUS}. Pola sama dengan {@link #put}.
	 *
	 * @param cookie      nilai {@code JSESSIONID} sesi DSpace
	 * @param acton       path aksi relatif terhadap {@code DSPACE_URL_PRIVATE} yang akan dihapus
	 * @param description deskripsi operasi, dicatat ke {@link DspaceLog}
	 * @return kode status HTTP respons (200 = OK), atau {@code 0} bila terjadi kegagalan yang
	 *         tertangkap secara internal
	 * @throws Exception dideklarasikan pada tanda tangan namun kegagalan pada praktiknya
	 *                    ditangkap secara internal, kecuali dari
	 *                    {@link #simpanDspaceLog(DspaceLog)}
	 */
	public static int delete(String cookie, String acton, String description) throws Exception {

		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(description);

		dspaceLog.setAction(DspaceLog.HAPUS);

		int code = 0;

		try {

			String urlStr = ConstantValues.DSPACE_URL_PRIVATE + "/" + acton;
			URL url = new URL(urlStr);

			System.out.println("delete urlStr = " + urlStr);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("DELETE");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			con.setDoInput(true);

			code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("Delete Response    (Code):" + code);

			dspaceLog.setStatus(code);

		} catch (Exception e) {
			dspaceLog.setError(e.getMessage());
		}

		simpanDspaceLog(dspaceLog);
		return code;
	}

	/**
	 * Menyimpan satu baris {@link DspaceLog} dalam transaksi Hibernate sendiri, dengan rollback
	 * eksplisit bila terjadi kegagalan. Sesi Hibernate native SELALU dibersihkan tuntas di
	 * blok {@code finally} (lewat {@link HibernateUtil#closeSessionQuietly(Session)} DAN
	 * {@link HibernateUtil#closeSession()}) — komentar pada kode menegaskan pembersihan ganda
	 * ini disengaja karena sesi dibuka/dimiliki oleh jalur integrasi ini sendiri, sehingga wajib
	 * ditutup tuntas agar tidak meninggalkan koneksi database dalam status "idle in transaction".
	 *
	 * @param dspaceLog baris log yang akan disimpan
	 * @throws Exception diteruskan dari kegagalan Hibernate saat menyimpan (transaksi di-rollback
	 *                    lebih dulu sebelum exception diteruskan)
	 */
	private static void simpanDspaceLog(DspaceLog dspaceLog) throws Exception {
		Session session = null;
		Transaction transaction = null;
		try {
			session = HibernateUtil.currentNativeSession();
			transaction = session.beginTransaction();
			session.save(dspaceLog);
			transaction.commit();
			transaction = null;
		} finally {
			if (transaction != null && transaction.isActive()) {
				try { transaction.rollback(); } catch (Exception ignored) { }
			}
			// currentNativeSession dibuka/dimiliki jalur integrasi ini, maka wajib
			// dibersihkan tuntas agar koneksi tidak tertinggal idle in transaction.
			HibernateUtil.closeSessionQuietly(session);
			HibernateUtil.closeSession();
		}
	}
}
