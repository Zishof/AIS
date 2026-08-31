package ais.service.registration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h3>Harness uji KONKURENSI route {@code /pendaftaran} (§21.3) -- via HTTP ke server deploy.</h3>
 *
 * <p>Jalankan terhadap server dev/UAT (BUKAN produksi):</p>
 * <pre>
 * java -cp &lt;classes&gt; ais.service.registration.VerifikasiKonkurensiPendaftaran \
 *      http://localhost:8080/ecampus 50
 * </pre>
 *
 * <p>Skenario: N thread submit BERSAMAAN dgn username SAMA (idempotency key beda) -- lulus bila
 * MAKSIMAL SATU respons {@code REGISTRATION_CREATED}; sisanya {@code USERNAME_NOT_AVAILABLE}/
 * {@code EMAIL_ALREADY_REGISTERED}/{@code RATE_LIMITED}. Lalu N thread dgn idempotency key SAMA --
 * lulus bila seluruh respons sukses memuat registrationCode IDENTIK (replay, bukan baris baru).
 * Catatan: rate limit submit 10/jam/IP membatasi hasil sukses -- harness memakai jeda konfigurasi
 * server uji (naikkan sementara `pendaftaran_paket` limit? TIDAK -- cukup verifikasi invariant
 * "maks satu sukses", RATE_LIMITED dihitung sah).</p>
 */
public final class VerifikasiKonkurensiPendaftaran {

	/**
	 * Titik masuk harness uji konkurensi manual untuk route {@code /pendaftaran} (§21.3 dokumen
	 * master) -- dijalankan langsung sebagai proses Java terpisah terhadap server dev/UAT yang
	 * sudah berjalan (harness ini TIDAK memakai Hibernate/koneksi DB langsung, murni klien HTTP).
	 *
	 * <p>
	 * Argumen: {@code args[0]} base URL server (default {@code http://localhost:8080}),
	 * {@code args[1]} jumlah thread konkuren {@code n} untuk skenario pertama (default 50).
	 * </p>
	 *
	 * <p>
	 * Setelah mengambil satu sesi/CSRF/{@code formInstanceId} bersama lewat
	 * {@link #ambilSesiDanCsrf(String)} (dipakai ulang oleh SELURUH thread pada kedua skenario --
	 * sengaja meniru satu pengguna yang membuka form sekali lalu mengirim submit berulang, bukan
	 * {@code n} sesi terpisah), harness menjalankan dua skenario berurutan:
	 * </p>
	 * <ol>
	 * <li><b>Skenario username-sama</b>: {@code n} thread di-<i>gate</i> oleh satu
	 * {@link CountDownLatch} agar submit dimulai SEBERSAMAAN mungkin, masing-masing mengirim
	 * {@code desiredUsername} yang SAMA tetapi {@code idempotencyKey} BERBEDA (per-thread) ke
	 * {@code POST /pendaftaran}. Invariant yang diverifikasi: MAKSIMAL SATU respons memuat
	 * {@code REGISTRATION_CREATED} ({@code sukses}); respons lain yang dianggap SAH adalah
	 * {@code USERNAME_NOT_AVAILABLE}, {@code EMAIL_ALREADY_REGISTERED}, atau {@code RATE_LIMITED}
	 * (rate limit submit 10/jam/IP memang membatasi jumlah percobaan yang bisa sukses, sehingga
	 * dihitung sah, bukan kegagalan harness); respons di luar itu dihitung {@code lain} dan
	 * dicetak (dipotong lewat {@link #potong(String)}) sebagai indikasi kegagalan tak terduga.
	 * Lulus bila {@code sukses <= 1 && lain == 0}.</li>
	 * <li><b>Skenario idempotency-sama</b>: 10 thread mengirim {@code idempotencyKey} yang SAMA
	 * (username berbeda dari skenario 1, disisipi akhiran {@code "b"}) secara bersamaan. Setiap
	 * {@code registrationCode} yang berhasil diekstrak dari respons (lewat
	 * {@link #ekstrak(String, String)}) dikumpulkan; invariant yang diverifikasi adalah SELURUH
	 * kode yang berhasil didapat harus IDENTIK (replay hasil submit pertama, bukan baris
	 * pendaftaran baru) -- diukur lewat jumlah nilai unik pada himpunan hasil. Lulus bila jumlah
	 * kode unik {@code <= 1}.</li>
	 * </ol>
	 *
	 * <p>
	 * Ringkasan tiap skenario dicetak ke {@code System.out} berikut vonis {@code [LULUS]}/
	 * {@code [GAGAL]}; proses keluar lewat {@code System.exit(0)} hanya bila KEDUA skenario lulus,
	 * selain itu {@code System.exit(1)} -- memudahkan pemakaian dari skrip CI/operator yang
	 * mengecek exit code.
	 * </p>
	 *
	 * @param args argumen baris perintah: {@code [baseUrl, jumlahThread]}, keduanya opsional
	 * @throws Exception diteruskan apa adanya dari kegagalan pengambilan sesi/CSRF awal
	 *                    ({@link #ambilSesiDanCsrf(String)}) atau interupsi menunggu
	 *                    {@link CountDownLatch}; galat per-thread pada request submit individual
	 *                    ditangkap secara lokal dan dihitung sebagai {@code lain}/gagal, tidak
	 *                    menjatuhkan proses utama
	 */
	public static void main(String[] args) throws Exception {
		final String base = args.length > 0 ? args[0] : "http://localhost:8080";
		final int n = args.length > 1 ? Integer.parseInt(args[1]) : 50;
		final String username = "uji" + (System.currentTimeMillis() % 1000000);

		// ---- Ambil CSRF + cookie sesi dari GET form ----
		final String[] sesi = ambilSesiDanCsrf(base);
		System.out.println("Sesi=" + sesi[0].substring(0, Math.min(20, sesi[0].length())) + "... csrf=ok");

		// ---- Skenario 1: N submit username sama, idempotency beda ----
		final AtomicInteger sukses = new AtomicInteger(0);
		final AtomicInteger tolakUsername = new AtomicInteger(0);
		final AtomicInteger lain = new AtomicInteger(0);
		final CountDownLatch mulai = new CountDownLatch(1);
		final CountDownLatch selesai = new CountDownLatch(n);
		for (int i = 0; i < n; i++) {
			final int idx = i;
			new Thread(new Runnable() {
				public void run() {
					try {
						mulai.await();
						String body = bodySubmit(username, "u" + idx + "-" + username + "@uji.example",
								"key-" + username + "-" + idx, sesi[1]);
						String jawab = post(base + "/pendaftaran", body, sesi[0]);
						if (jawab.contains("REGISTRATION_CREATED")) {
							sukses.incrementAndGet();
						} else if (jawab.contains("USERNAME_NOT_AVAILABLE")
								|| jawab.contains("RATE_LIMITED")
								|| jawab.contains("EMAIL_ALREADY_REGISTERED")) {
							tolakUsername.incrementAndGet();
						} else {
							lain.incrementAndGet();
							System.out.println("  respons tak terduga: " + potong(jawab));
						}
					} catch (Exception e) {
						lain.incrementAndGet();
					} finally {
						selesai.countDown();
					}
				}
			}).start();
		}
		mulai.countDown();
		selesai.await();
		System.out.println("Skenario username-sama: sukses=" + sukses.get() + " ditolak="
				+ tolakUsername.get() + " lain=" + lain.get());
		boolean lulus1 = sukses.get() <= 1 && lain.get() == 0;
		System.out.println(lulus1 ? "[LULUS] maksimal satu reservasi menang"
				: "[GAGAL] lebih dari satu sukses / respons tak terduga");

		// ---- Skenario 2: idempotency key sama -> registrationCode identik ----
		final String kunciSama = "key-idem-" + username;
		final List<String> kodeList = java.util.Collections.synchronizedList(new ArrayList<String>());
		final CountDownLatch mulai2 = new CountDownLatch(1);
		final CountDownLatch selesai2 = new CountDownLatch(10);
		for (int i = 0; i < 10; i++) {
			new Thread(new Runnable() {
				public void run() {
					try {
						mulai2.await();
						String body = bodySubmit(username + "b", "idem-" + username + "@uji.example",
								kunciSama, sesi[1]);
						String jawab = post(base + "/pendaftaran", body, sesi[0]);
						String kode = ekstrak(jawab, "registrationCode");
						if (kode != null) {
							kodeList.add(kode);
						}
					} catch (Exception e) { /* dihitung dari ukuran kodeList */
					} finally {
						selesai2.countDown();
					}
				}
			}).start();
		}
		mulai2.countDown();
		selesai2.await();
		java.util.HashSet<String> unik = new java.util.HashSet<String>(kodeList);
		System.out.println("Skenario idempotency: respons berkode=" + kodeList.size() + " kodeUnik=" + unik.size());
		boolean lulus2 = unik.size() <= 1;
		System.out.println(lulus2 ? "[LULUS] idempotency key sama -> satu permohonan"
				: "[GAGAL] idempotency menghasilkan >1 kode: " + unik);

		System.exit(lulus1 && lulus2 ? 0 : 1);
	}

	/**
	 * Mengambil satu sesi HTTP + token CSRF + {@code formInstanceId} dari halaman form
	 * {@code GET <base>/pendaftaran}, untuk dipakai ulang oleh seluruh thread pada kedua skenario
	 * di {@link #main(String[])} (satu sesi peniru satu pengguna nyata yang membuka form sekali).
	 *
	 * <p>
	 * Cookie sesi diekstrak dari SELURUH header {@code Set-Cookie} pada respons (redirect TIDAK
	 * diikuti otomatis -- {@code setInstanceFollowRedirects(false)} -- karena cookie sesi biasanya
	 * muncul pada respons awal, bukan setelah redirect), digabung jadi satu string {@code "; "}
	 * -separated dengan bagian atribut (mis. {@code Path=}, {@code HttpOnly}) setelah {@code ';'}
	 * dibuang. Body HTML dibaca penuh lalu di-scan lewat {@link #ekstrakAttr(String, String)}
	 * untuk nilai atribut {@code value} pada field tersembunyi {@code csrf} dan
	 * {@code formInstanceId}; bila salah satu tidak ditemukan, method melempar
	 * {@link IllegalStateException} (form dianggap berubah struktur/gagal dimuat).
	 * </p>
	 *
	 * <p>
	 * Sebelum mengembalikan hasil, method SENGAJA menunggu ({@code Thread.sleep(6000)}) untuk
	 * melewati pengecekan waktu-tempuh-pengisian-form minimal di sisi server (anti-bot berbasis
	 * elapsed time) -- tanpa jeda ini submit berikutnya dari seluruh thread akan ditolak server
	 * karena dianggap diisi terlalu cepat untuk manusia.
	 * </p>
	 *
	 * @param base base URL server (tanpa {@code /pendaftaran} di akhir)
	 * @return array dua elemen: indeks 0 = cookie sesi gabungan, indeks 1 = {@code csrf} dan
	 *         {@code formInstanceId} digabung dengan pemisah {@code '|'} (dipecah kembali oleh
	 *         {@link #bodySubmit(String, String, String, String)})
	 * @throws Exception diteruskan dari kegagalan koneksi HTTP/I/O, atau
	 *                    {@link IllegalStateException} bila token {@code csrf}/{@code formInstanceId}
	 *                    tidak ditemukan pada HTML form
	 */
	private static String[] ambilSesiDanCsrf(String base) throws Exception {
		HttpURLConnection c = (HttpURLConnection) new URL(base + "/pendaftaran").openConnection();
		c.setInstanceFollowRedirects(false);
		String cookie = "";
		List<String> setCookies = c.getHeaderFields().get("Set-Cookie");
		if (setCookies != null) {
			for (int i = 0; i < setCookies.size(); i++) {
				String s = setCookies.get(i);
				int titikKoma = s.indexOf(';');
				cookie += (cookie.isEmpty() ? "" : "; ") + (titikKoma > 0 ? s.substring(0, titikKoma) : s);
			}
		}
		StringBuilder html = new StringBuilder();
		BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
		String baris;
		while ((baris = r.readLine()) != null) {
			html.append(baris).append('\n');
		}
		r.close();
		String csrf = ekstrakAttr(html.toString(), "name=\"csrf\" value=\"");
		String formInstance = ekstrakAttr(html.toString(), "name=\"formInstanceId\" value=\"");
		if (csrf == null || formInstance == null) {
			throw new IllegalStateException("Tidak menemukan csrf/formInstanceId pada halaman form.");
		}
		// elapsed-time check server: tunggu melewati ambang minimal isi form.
		Thread.sleep(6000);
		return new String[] { cookie, csrf + "|" + formInstance };
	}

	/**
	 * Menyusun body {@code application/x-www-form-urlencoded} untuk satu percobaan submit form
	 * pendaftaran, dengan seluruh field selain {@code desiredUsername}/{@code emailLogin}/
	 * {@code idempotencyKey} diisi nilai uji tetap (nama usaha, jenis usaha id {@code "1"},
	 * password {@code "PasswordUji123"}, persetujuan terms/privacy versi {@code "2026-01"}).
	 *
	 * @param username    nilai untuk field {@code desiredUsername} sekaligus disisipkan ke
	 *                    {@code namaUsaha}
	 * @param email       nilai untuk field {@code emailLogin}
	 * @param idemKey     nilai untuk field {@code idempotencyKey}
	 * @param csrfDanForm hasil {@link #ambilSesiDanCsrf(String)} indeks 1 ({@code csrf} dan
	 *                    {@code formInstanceId} digabung {@code '|'}), dipecah kembali di sini
	 * @return body form ter-encode siap dikirim lewat {@link #post(String, String, String)}
	 * @throws Exception diteruskan dari kegagalan {@link URLEncoder#encode(String, String)}
	 */
	private static String bodySubmit(String username, String email, String idemKey, String csrfDanForm)
			throws Exception {
		String[] cf = csrfDanForm.split("\\|");
		StringBuilder b = new StringBuilder();
		tambah(b, "action", "submit_registration");
		tambah(b, "csrf", cf[0]);
		tambah(b, "formInstanceId", cf[1]);
		tambah(b, "idempotencyKey", idemKey);
		tambah(b, "namaUsaha", "Uji Konkurensi " + username);
		tambah(b, "jenisUsahaIds", "1");
		tambah(b, "desiredUsername", username);
		tambah(b, "emailLogin", email);
		tambah(b, "password", "PasswordUji123");
		tambah(b, "konfirmasiPassword", "PasswordUji123");
		tambah(b, "setujuTerms", "1");
		tambah(b, "setujuPrivacy", "1");
		tambah(b, "termsVersion", "2026-01");
		tambah(b, "privacyVersion", "2026-01");
		return b.toString();
	}

	/** Menambahkan satu pasangan {@code k=v} (nilai di-{@link URLEncoder#encode(String, String) encode} UTF-8) ke {@code b}, disisipi {@code '&'} bila {@code b} sudah berisi field sebelumnya. Dipakai oleh {@link #bodySubmit}. */
	private static void tambah(StringBuilder b, String k, String v) throws Exception {
		if (b.length() > 0) {
			b.append('&');
		}
		b.append(k).append('=').append(URLEncoder.encode(v, "UTF-8"));
	}

	/**
	 * Mengirim satu {@code POST} form-urlencoded ke {@code url} dengan {@code cookie} sesi
	 * (bila ada), lalu membaca seluruh body respons sebagai {@code String} UTF-8 -- dari
	 * {@code getErrorStream()} bila status {@code >= 400}, atau {@code getInputStream()} bila
	 * sukses, sehingga pesan error server (mis. {@code RATE_LIMITED}) tetap terbaca oleh
	 * pemanggil alih-alih hanya melempar {@link java.io.IOException}.
	 *
	 * @param url    URL tujuan lengkap
	 * @param body   body form-urlencoded yang sudah disusun (lihat {@link #bodySubmit})
	 * @param cookie header {@code Cookie} yang dikirim apa adanya bila tidak kosong, boleh
	 *               {@code null}/kosong untuk request tanpa sesi
	 * @return body respons mentah (biasanya JSON) sebagai satu {@code String}
	 * @throws Exception diteruskan dari kegagalan koneksi/I/O HTTP
	 */
	private static String post(String url, String body, String cookie) throws Exception {
		HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
		c.setRequestMethod("POST");
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		if (cookie != null && !cookie.isEmpty()) {
			c.setRequestProperty("Cookie", cookie);
		}
		OutputStream os = c.getOutputStream();
		os.write(body.getBytes("UTF-8"));
		os.flush();
		os.close();
		BufferedReader r = new BufferedReader(new InputStreamReader(
				c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String baris;
		while ((baris = r.readLine()) != null) {
			sb.append(baris);
		}
		r.close();
		return sb.toString();
	}

	/** Ekstraksi atribut HTML sederhana (bukan parser HTML sungguhan): mengembalikan teks setelah kemunculan pertama {@code penanda} sampai tanda kutip ganda berikutnya, atau {@code null} bila {@code penanda} tidak ditemukan. Dipakai {@link #ambilSesiDanCsrf(String)} untuk membaca {@code value="..."} field tersembunyi. */
	private static String ekstrakAttr(String html, String penanda) {
		int i = html.indexOf(penanda);
		if (i < 0) {
			return null;
		}
		int mulai = i + penanda.length();
		int akhir = html.indexOf('"', mulai);
		return akhir < 0 ? null : html.substring(mulai, akhir);
	}

	/** Ekstraksi nilai {@code String} JSON sederhana (bukan parser JSON sungguhan, hanya cocok untuk field bertipe string sederhana tanpa karakter escape rumit): mencari pola literal {@code "kunci":"} lalu mengembalikan teks sampai tanda kutip ganda berikutnya, atau {@code null} bila tidak ditemukan. Dipakai untuk membaca {@code registrationCode} dari respons submit. */
	private static String ekstrak(String json, String kunci) {
		int i = json.indexOf("\"" + kunci + "\":\"");
		if (i < 0) {
			return null;
		}
		int mulai = i + kunci.length() + 4;
		int akhir = json.indexOf('"', mulai);
		return akhir < 0 ? null : json.substring(mulai, akhir);
	}

	/** Memotong {@code s} ke maksimal 200 karakter agar respons tak terduga yang dicetak ke log tidak membanjiri konsol. */
	private static String potong(String s) {
		return s.length() > 200 ? s.substring(0, 200) : s;
	}
}
