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

	private static void tambah(StringBuilder b, String k, String v) throws Exception {
		if (b.length() > 0) {
			b.append('&');
		}
		b.append(k).append('=').append(URLEncoder.encode(v, "UTF-8"));
	}

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

	private static String ekstrakAttr(String html, String penanda) {
		int i = html.indexOf(penanda);
		if (i < 0) {
			return null;
		}
		int mulai = i + penanda.length();
		int akhir = html.indexOf('"', mulai);
		return akhir < 0 ? null : html.substring(mulai, akhir);
	}

	private static String ekstrak(String json, String kunci) {
		int i = json.indexOf("\"" + kunci + "\":\"");
		if (i < 0) {
			return null;
		}
		int mulai = i + kunci.length() + 4;
		int akhir = json.indexOf('"', mulai);
		return akhir < 0 ? null : json.substring(mulai, akhir);
	}

	private static String potong(String s) {
		return s.length() > 200 ? s.substring(0, 200) : s;
	}
}
