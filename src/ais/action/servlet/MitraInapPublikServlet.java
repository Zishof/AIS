package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ais.common.security.PublicRegistrationRateLimiter;

/**
 * <h3>Endpoint publik MitraInap ({@code /mitrainap-publik}) -- LANGKAH 6.</h3>
 *
 * <p>ANONIM: tanpa token staf (pola {@code PendaftaranTenantServlet}, BUKAN bearer PosApi) --
 * resolusi jalur publik sesuai handover §2.7. Seluruh logika di
 * {@link ais.action.servlet.api.MitraInapPublikHelper}; servlet ini hanya: parse request,
 * rate limit per IP ({@link PublicRegistrationRateLimiter}), honeypot utk POST booking,
 * dan amplop JSON. Error internal TIDAK pernah membocorkan stack trace ke publik.</p>
 *
 * <p>GET  {@code ?mode=katalog[&properti_id=..]} -- katalog properti+tipe (60/jam/IP)<br>
 * GET  {@code ?mode=ketersediaan&tipe_kamar_id=..&checkin=..&checkout=..} (120/jam/IP)<br>
 * GET  {@code ?mode=status&kode_booking=..&telp=..} (60/jam/IP)<br>
 * POST body JSON {@code {mode:"booking", nama, telp, email?, properti_id, tipe_kamar_id,
 * checkin, checkout, jumlah_tamu?, idempotency_key, website:""}} (5/jam/IP; field
 * {@code website} = honeypot, WAJIB kosong -- bot yang mengisinya ditolak diam-diam).</p>
 */
public class MitraInapPublikServlet extends HttpServlet {

	/** Versi serialisasi tetap 1L; servlet tidak pernah benar-benar diserialisasi ke stream. */
	private static final long serialVersionUID = 1L;

	/**
	 * Menentukan alamat IP klien untuk keperluan pembatasan laju (rate limit): mengutamakan
	 * segmen pertama header {@code X-Forwarded-For} (jika ada di belakang proxy/load balancer),
	 * atau {@code request.getRemoteAddr()} sebagai fallback.
	 *
	 * @param request permintaan HTTP masuk
	 * @return alamat IP klien yang dipakai sebagai kunci rate limit
	 */
	private static String clientIp(HttpServletRequest request) {
		String xf = request.getHeader("X-Forwarded-For");
		if (xf != null && xf.trim().length() > 0) {
			int koma = xf.indexOf(',');
			return (koma > 0 ? xf.substring(0, koma) : xf).trim();
		}
		return request.getRemoteAddr();
	}

	/**
	 * Menulis hasil JSON ke respons dengan header {@code Cache-Control: no-store} dan CORS
	 * terbuka ({@code Access-Control-Allow-Origin: *}) -- aman karena endpoint anonim ini tidak
	 * membaca cookie sesi apa pun (tidak ada permukaan CSRF: perubahan state hanya lewat booking
	 * ber-idempotency-key dan rate limit per IP).
	 *
	 * @param response respons HTTP keluar
	 * @param hasil objek JSON yang akan ditulis sebagai body respons
	 * @throws IOException jika terjadi galat I/O saat menulis respons
	 */
	private static void tulis(HttpServletResponse response, JSONObject hasil) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setHeader("Cache-Control", "no-store");
		// Situs publik boleh dari origin mana pun -- endpoint ini memang anonim dan
		// tidak membaca cookie sesi (tidak ada CSRF surface: state diubah hanya lewat
		// booking ber-idempotency-key + rate limit).
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.getWriter().write(hasil.toString());
	}

	/**
	 * Menangani galat internal tak terduga: mencatatnya lewat audit ({@link ais.common.ErrorAuditUtil})
	 * dan membalas JSON status generik ({@code status=91}) TANPA membocorkan stack trace atau
	 * detail teknis ke klien publik.
	 *
	 * @param response respons HTTP keluar
	 * @param e galat yang terjadi
	 * @param request permintaan HTTP terkait, disertakan ke audit untuk konteks
	 * @throws IOException jika terjadi galat I/O saat menulis respons
	 */
	private static void galatInternal(HttpServletResponse response, Exception e,
			HttpServletRequest request) throws IOException {
		ais.common.ErrorAuditUtil.record(e, "auto-audit MitraInapPublikServlet", request);
		JSONObject hasil = new JSONObject();
		try {
			hasil.put("status", "91");
			hasil.put("description", "Layanan sedang tidak dapat memproses permintaan. Coba lagi.");
		} catch (Exception abaikan) {
			ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) MitraInapPublikServlet.galat");
		}
		tulis(response, hasil);
	}

	/**
	 * Menangani permintaan baca publik: katalog properti/tipe kamar, cek ketersediaan, atau
	 * status booking, masing-masing dengan rate limit per IP tersendiri. Anonim -- tanpa gerbang
	 * login apa pun (lihat javadoc kelas). Parameter mentah disalin ke objek query sebelum
	 * diteruskan ke {@link ais.action.servlet.api.MitraInapPublikHelper}.
	 *
	 * @param request permintaan HTTP masuk; parameter {@code mode} dan field katalog/ketersediaan/status dibaca di sini
	 * @param response respons HTTP keluar; selalu JSON
	 * @throws ServletException tidak pernah dilempar, hanya dideklarasikan oleh kontrak servlet
	 * @throws IOException jika terjadi galat I/O saat menulis respons
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject hasil = new JSONObject();
		try {
			String mode = request.getParameter("mode") == null ? "" : request.getParameter("mode").trim();
			String ip = clientIp(request);
			JSONObject q = new JSONObject();
			String[] fields = { "properti_id", "tipe_kamar_id", "checkin", "checkout", "kode_booking", "telp" };
			for (int i = 0; i < fields.length; i++) {
				String v = request.getParameter(fields[i]);
				if (v != null) q.put(fields[i], v.trim());
			}
			if ("katalog".equals(mode)) {
				if (!PublicRegistrationRateLimiter.izinkan("mitrainap-katalog|" + ip, 60, 3600000L)) {
					hasil.put("status", "91");
					hasil.put("description", "Terlalu banyak permintaan. Coba beberapa saat lagi.");
				} else {
					ais.action.servlet.api.MitraInapPublikHelper.katalog(q, hasil);
				}
			} else if ("ketersediaan".equals(mode)) {
				if (!PublicRegistrationRateLimiter.izinkan("mitrainap-cek|" + ip, 120, 3600000L)) {
					hasil.put("status", "91");
					hasil.put("description", "Terlalu banyak permintaan. Coba beberapa saat lagi.");
				} else {
					ais.action.servlet.api.MitraInapPublikHelper.ketersediaan(q, hasil);
				}
			} else if ("status".equals(mode)) {
				if (!PublicRegistrationRateLimiter.izinkan("mitrainap-status|" + ip, 60, 3600000L)) {
					hasil.put("status", "91");
					hasil.put("description", "Terlalu banyak permintaan. Coba beberapa saat lagi.");
				} else {
					ais.action.servlet.api.MitraInapPublikHelper.status(q, hasil);
				}
			} else {
				hasil.put("status", "91");
				hasil.put("description", "mode tidak dikenal (katalog|ketersediaan|status).");
			}
			tulis(response, hasil);
		} catch (Exception e) {
			galatInternal(response, e, request);
		}
	}

	/**
	 * Menangani permintaan booking publik (mode {@code booking} saja). Membaca body JSON mentah
	 * dibatasi 20000 karakter, memeriksa honeypot ({@code website} wajib kosong -- jika terisi,
	 * dibalas sukses PALSU tanpa menyimpan apa pun agar bot tidak mendapat sinyal deteksi),
	 * lalu menerapkan rate limit booking per IP sebelum mendelegasikan ke
	 * {@link ais.action.servlet.api.MitraInapPublikHelper#booking}. Anonim -- tanpa gerbang
	 * login apa pun (lihat javadoc kelas).
	 *
	 * @param request permintaan HTTP masuk; body JSON berisi data booking dibaca di sini
	 * @param response respons HTTP keluar; selalu JSON
	 * @throws ServletException tidak pernah dilempar, hanya dideklarasikan oleh kontrak servlet
	 * @throws IOException jika terjadi galat I/O saat membaca body atau menulis respons
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject hasil = new JSONObject();
		try {
			StringBuilder body = new StringBuilder();
			BufferedReader reader = request.getReader();
			String baris;
			while ((baris = reader.readLine()) != null) {
				body.append(baris);
				if (body.length() > 20000) break; // payload publik tidak pernah sebesar ini
			}
			JSONObject q = body.length() == 0 ? new JSONObject() : new JSONObject(body.toString());
			String mode = q.optString("mode", "").trim();
			String ip = clientIp(request);
			if (!"booking".equals(mode)) {
				hasil.put("status", "91");
				hasil.put("description", "mode tidak dikenal (booking).");
			} else if (q.optString("website", "").trim().length() > 0) {
				// Honeypot terisi = bot. Balas sukses PALSU tanpa menyimpan apa pun
				// (pola PendaftaranTenantServlet: jangan beri sinyal ke bot).
				hasil.put("status", "00");
				hasil.put("kode_booking", "BOOK-TERIMA");
				hasil.put("description", "Booking diterima.");
			} else if (!PublicRegistrationRateLimiter.izinkan("mitrainap-booking|" + ip, 5, 3600000L)) {
				hasil.put("status", "91");
				hasil.put("description", "Terlalu banyak percobaan booking dari alamat ini. Coba lagi nanti.");
			} else {
				ais.action.servlet.api.MitraInapPublikHelper.booking(q, hasil);
			}
			tulis(response, hasil);
		} catch (Exception e) {
			galatInternal(response, e, request);
		}
	}
}
