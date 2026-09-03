package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ais.common.Common;
import ais.common.ErrorAuditUtil;
import ais.common.LabelBahasaHelper;
import ais.common.newui.NewUiCsrfUtil;

/**
 * {@code LabelBahasaApi} — endpoint JSON untuk menyunting teks/label multi-bahasa dari
 * halaman JSP, padanan {@code EditorLabelBahasa} di sisi ZKoss.
 *
 * <h3>Untuk apa</h3>
 * <p>Alert pada halaman JSP kini memakai dialog aplikasi ({@code pesan-formal.js}).
 * Endpoint ini melengkapinya dengan tombol <b>Ubah Teks</b> khusus administrator:
 * kalimat yang janggal atau salah terjemah dapat diperbaiki tepat di tempat kalimat itu
 * muncul, tanpa masuk menu Konfigurasi terpisah dan tanpa redeploy.</p>
 *
 * <h3>Aksi</h3>
 * <ul>
 *   <li>{@code aksi=muat&teks=...} — status admin, kunci kamus, dan keempat terjemahan.
 *       Dipakai halaman untuk memutuskan apakah tombol Ubah Teks perlu ditampilkan.</li>
 *   <li>{@code aksi=terjemah&teks=...} — hasil terjemahan otomatis untuk English,
 *       Arabic, dan Mandarin. TIDAK menyimpan apa pun.</li>
 *   <li>{@code aksi=simpan} — menyimpan keempat bahasa (wajib POST + token CSRF).</li>
 * </ul>
 *
 * <h3>Keamanan</h3>
 * <p>SELURUH aksi menuntut {@link Common#getApakahAdmin()}; yang bukan administrator
 * memperoleh {@code {"admin":false}} tanpa data dan tanpa efek apa pun — termasuk pada
 * aksi baca, karena isi kamus tidak perlu dibuka ke pengguna biasa. Aksi yang MENGUBAH
 * data ({@code simpan}) menuntut metode POST dan token CSRF yang sah lewat
 * {@link NewUiCsrfUtil}; tanpa itu permintaan ditolak 403. Pemeriksaan hak akses juga
 * diulang di dalam {@code LabelBahasaHelper}, sehingga endpoint ini bukan satu-satunya
 * lapis pertahanan.</p>
 *
 * <p>Kompatibilitas: Java 1.6 (tanpa lambda, diamond, try-with-resources, atau Stream).</p>
 */
public class LabelBahasaApi extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		proses(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		proses(request, response);
	}

	private void proses(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// charset ditetapkan lewat setContentType di bawah. Tidak memakai
		// setCharacterEncoding(String) terpisah karena method itu baru ada di Servlet 2.4,
		// sedangkan servlet_.jar yang dipaketkan aplikasi ini masih 2.3.
		response.setContentType("application/json; charset=UTF-8");
		// Isi kamus bergantung pada pengguna yang sedang masuk; jangan sampai tersimpan
		// di cache bersama.
		response.setHeader("Cache-Control", "no-store");

		JSONObject hasil = new JSONObject();
		try {
			boolean admin = false;
			try {
				admin = Common.getApakahAdmin();
			} catch (Throwable t) {
				admin = false;
			}
			hasil.put("admin", admin);
			if (!admin) {
				// Sengaja TIDAK menjelaskan lebih jauh: pengguna biasa cukup tahu tombolnya
				// tidak tersedia untuknya.
				tulis(response, hasil);
				return;
			}

			String aksi = teks(request.getParameter("aksi"));
			if ("muat".equals(aksi)) {
				muat(request, hasil);
			} else if ("terjemah".equals(aksi)) {
				terjemah(request, hasil);
			} else if ("simpan".equals(aksi)) {
				if (!simpan(request, response, hasil)) {
					return;
				}
			} else {
				hasil.put("ok", false);
				hasil.put("pesan", "Aksi tidak dikenal.");
			}
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "LabelBahasaApi.proses aksi=" + request.getParameter("aksi"));
			try {
				hasil.put("ok", false);
				hasil.put("pesan", "Permintaan tidak dapat diproses.");
			} catch (Throwable abaikan) {
				// respons tetap dikirim apa adanya di bawah
			}
		}
		tulis(response, hasil);
	}

	private void muat(HttpServletRequest request, JSONObject hasil) throws Exception {
		String teksAsli = teks(request.getParameter("teks"));
		String kunci = LabelBahasaHelper.kunci(teksAsli);
		String[] nilai = LabelBahasaHelper.ambilTerjemahan(kunci);
		hasil.put("ok", true);
		hasil.put("kunci", kunci);
		// Bila kolom Indonesia masih kosong, pakai teks apa adanya dari halaman supaya
		// penerjemahan otomatis punya sumber dan admin tidak perlu mengetik ulang.
		hasil.put("indonesia", nilai[LabelBahasaHelper.INDONESIA].length() > 0
				? nilai[LabelBahasaHelper.INDONESIA] : teksAsli);
		hasil.put("english", nilai[LabelBahasaHelper.ENGLISH]);
		hasil.put("arab", nilai[LabelBahasaHelper.ARAB]);
		hasil.put("mandarin", nilai[LabelBahasaHelper.MANDARIN]);
		hasil.put("csrfHeader", NewUiCsrfUtil.HEADER);
		hasil.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
	}

	private void terjemah(HttpServletRequest request, JSONObject hasil) throws Exception {
		String sumber = teks(request.getParameter("teks"));
		hasil.put("ok", sumber.length() > 0);
		hasil.put("english", LabelBahasaHelper.terjemahOtomatis(sumber, "english"));
		hasil.put("arab", LabelBahasaHelper.terjemahOtomatis(sumber, "arab"));
		hasil.put("mandarin", LabelBahasaHelper.terjemahOtomatis(sumber, "mandarin"));
	}

	/** @return {@code false} bila respons sudah dikirim sendiri (mis. 403) */
	private boolean simpan(HttpServletRequest request, HttpServletResponse response, JSONObject hasil)
			throws Exception {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			tolak(response, "Penyimpanan harus memakai metode POST.");
			return false;
		}
		if (!NewUiCsrfUtil.isValid(request)) {
			tolak(response, "Token CSRF tidak valid.");
			return false;
		}
		String kunci = teks(request.getParameter("kunci"));
		if (kunci.length() == 0) {
			// Kunci boleh diturunkan dari teksnya bila pemanggil tidak mengirimkannya.
			kunci = LabelBahasaHelper.kunci(teks(request.getParameter("teks")));
		}
		boolean ok = LabelBahasaHelper.simpan(kunci,
				teks(request.getParameter("indonesia")), teks(request.getParameter("english")),
				teks(request.getParameter("arab")), teks(request.getParameter("mandarin")));
		hasil.put("ok", ok);
		hasil.put("kunci", kunci);
		hasil.put("pesan", ok ? "Teks berhasil diperbarui."
				: "Teks gagal disimpan. Rinciannya tercatat pada audit galat.");
		return true;
	}

	private void tolak(HttpServletResponse response, String pesan) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		JSONObject j = new JSONObject();
		try {
			j.put("ok", false);
			j.put("admin", true);
			j.put("pesan", pesan);
		} catch (Exception abaikan) {
			// respons tetap dikirim apa adanya
		}
		tulis(response, j);
	}

	private void tulis(HttpServletResponse response, JSONObject hasil) throws IOException {
		response.getWriter().write(hasil.toString());
	}

	private static String teks(String nilai) {
		return nilai == null ? "" : nilai.trim();
	}
}
