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

	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	/**
	 * Menangani GET dengan mendelegasikan langsung ke {@link #proses}; dipakai oleh aksi baca
	 * ({@code muat}, {@code terjemah}) yang tidak mengubah data.
	 *
	 * @param request  request HTTP masuk; parameter {@code aksi} menentukan operasi, lihat
	 *                 {@link #proses}
	 * @param response response HTTP keluar; selalu diisi JSON, lihat {@link #proses}
	 * @throws ServletException tidak pernah dilempar keluar dari sini karena {@link #proses}
	 *                          menangkap seluruh {@code Throwable} secara internal;
	 *                          dipertahankan karena tanda tangan {@link HttpServlet#doGet}
	 * @throws IOException      bila penulisan respons JSON gagal
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		proses(request, response);
	}

	/**
	 * Menangani POST dengan perilaku identik seperti {@link #doGet}: mendelegasikan langsung
	 * ke {@link #proses}. Aksi {@code simpan} (satu-satunya yang mengubah data) menuntut POST
	 * secara eksplisit di dalam {@link #simpan}, bukan lewat pembatasan pada method ini.
	 *
	 * @param request  request HTTP masuk; parameter sama seperti pada {@link #doGet}
	 * @param response response HTTP keluar; sama seperti pada {@link #doGet}
	 * @throws ServletException idem {@link #doGet}
	 * @throws IOException      idem {@link #doGet}
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		proses(request, response);
	}

	/**
	 * Titik masuk tunggal untuk seluruh aksi API ini: menetapkan header respons, menggerbang
	 * seluruh akses di belakang {@link Common#getApakahAdmin()}, lalu mendelegasikan ke handler
	 * aksi yang sesuai berdasarkan parameter {@code aksi}.
	 *
	 * <p>Alur: (1) set {@code Content-Type: application/json; charset=UTF-8} dan
	 * {@code Cache-Control: no-store} (isi kamus bergantung pengguna, tidak boleh ter-cache
	 * bersama); (2) evaluasi {@link Common#getApakahAdmin()} — kegagalan apa pun pada
	 * pengecekan ini dianggap BUKAN admin (fail-closed) lewat {@code catch (Throwable)}; (3)
	 * bila bukan admin, balas hanya {@code {"admin":false}} tanpa data kamus apa pun dan
	 * kembali; (4) bila admin, baca parameter {@code aksi} dan panggil {@link #muat},
	 * {@link #terjemah}, atau {@link #simpan} sesuai nilainya, atau isi pesan "Aksi tidak
	 * dikenal" bila tidak cocok salah satu; (5) {@code Throwable} apa pun dari langkah manapun
	 * ditangkap, dicatat ke {@link ErrorAuditUtil}, dan dibalas sebagai JSON generik gagal; (6)
	 * hasil akhir selalu ditulis lewat {@link #tulis}, kecuali {@link #simpan} sudah mengirim
	 * responsnya sendiri (mis. 403) dan mengembalikan {@code false}.</p>
	 *
	 * @param request  request HTTP masuk; parameter {@code aksi} ({@code muat}/{@code terjemah}/
	 *                 {@code simpan}) menentukan operasi, parameter lain tergantung aksi
	 * @param response response HTTP keluar; selalu diisi JSON, kecuali {@link #simpan} menolak
	 *                 lebih dulu lewat {@link #tolak}
	 * @throws IOException bila penulisan respons JSON gagal
	 */
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

	/**
	 * Handler aksi {@code muat}: mengembalikan kunci kamus beserta keempat terjemahan
	 * (Indonesia/English/Arab/Mandarin) untuk sebuah teks, plus token CSRF baru yang harus
	 * disertakan pemanggil bila nanti mengirim aksi {@code simpan}. Hanya dipanggil setelah
	 * {@link #proses} memastikan pemanggil admin.
	 *
	 * @param request request HTTP masuk; parameter {@code teks} adalah teks asli yang sedang
	 *                ditampilkan di halaman JSP, dipakai untuk menurunkan {@code kunci} kamus
	 * @param hasil   objek JSON respons yang diisi: {@code ok}, {@code kunci}, {@code indonesia}
	 *                (fallback ke {@code teks} bila kolom Indonesia kosong di kamus),
	 *                {@code english}, {@code arab}, {@code mandarin}, {@code csrfHeader}, dan
	 *                {@code csrfToken}
	 * @throws Exception bila {@link LabelBahasaHelper#kunci} atau
	 *                    {@link LabelBahasaHelper#ambilTerjemahan} melempar
	 */
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

	/**
	 * Handler aksi {@code terjemah}: menghasilkan terjemahan otomatis (English/Arab/Mandarin)
	 * dari sebuah teks sumber TANPA menyimpan apa pun ke kamus — murni pratinjau bagi admin
	 * sebelum memutuskan menekan tombol Simpan. Hanya dipanggil setelah {@link #proses}
	 * memastikan pemanggil admin.
	 *
	 * @param request request HTTP masuk; parameter {@code teks} adalah teks sumber (Indonesia)
	 *                yang akan diterjemahkan
	 * @param hasil   objek JSON respons yang diisi: {@code ok} (bernilai {@code false} bila
	 *                {@code teks} kosong), {@code english}, {@code arab}, {@code mandarin}
	 * @throws Exception bila {@link LabelBahasaHelper#terjemahOtomatis} melempar (mis. layanan
	 *                    terjemahan otomatis/AI sedang tidak tersedia)
	 */
	private void terjemah(HttpServletRequest request, JSONObject hasil) throws Exception {
		String sumber = teks(request.getParameter("teks"));
		hasil.put("ok", sumber.length() > 0);
		hasil.put("english", LabelBahasaHelper.terjemahOtomatis(sumber, "english"));
		hasil.put("arab", LabelBahasaHelper.terjemahOtomatis(sumber, "arab"));
		hasil.put("mandarin", LabelBahasaHelper.terjemahOtomatis(sumber, "mandarin"));
	}

	/**
	 * Handler aksi {@code simpan}: menyimpan keempat terjemahan sebuah kunci kamus ke
	 * penyimpanan permanen lewat {@link LabelBahasaHelper#simpan}. Satu-satunya aksi pada API
	 * ini yang mengubah data, sehingga digerbang dua lapis tambahan di luar pengecekan admin
	 * pada {@link #proses}: metode HTTP harus POST, dan token CSRF harus valid.
	 *
	 * <p>Alur: (1) tolak (403, lewat {@link #tolak}) bila metode request bukan POST; (2) tolak
	 * (403) bila {@link NewUiCsrfUtil#isValid(HttpServletRequest)} mengembalikan {@code false};
	 * (3) ambil {@code kunci}, atau turunkan dari {@code teks} bila {@code kunci} tidak dikirim
	 * pemanggil; (4) panggil {@link LabelBahasaHelper#simpan} dengan keempat nilai bahasa; (5)
	 * isi {@code hasil} dengan status keberhasilan, kunci yang dipakai, dan pesan untuk
	 * ditampilkan ke admin.</p>
	 *
	 * @param request  request HTTP masuk; parameter {@code kunci} (opsional), {@code teks}
	 *                 (sumber kunci bila {@code kunci} tidak dikirim), {@code indonesia},
	 *                 {@code english}, {@code arab}, {@code mandarin} berisi nilai yang disimpan
	 * @param response response HTTP keluar; diisi 403 lewat {@link #tolak} bila salah satu
	 *                 gerbang (metode/CSRF) gagal, atau dibiarkan untuk diisi {@link #proses}
	 *                 lewat {@link #tulis} bila lolos
	 * @param hasil    objek JSON respons yang diisi {@code ok}, {@code kunci}, dan {@code pesan}
	 *                 bila kedua gerbang lolos
	 * @return {@code false} bila respons sudah dikirim sendiri oleh {@link #tolak} (mis. 403),
	 *         menandakan {@link #proses} tidak perlu menulis respons lagi; {@code true} bila
	 *         {@code hasil} sudah diisi dan siap ditulis oleh pemanggil
	 * @throws Exception bila {@link LabelBahasaHelper#kunci} atau
	 *                    {@link LabelBahasaHelper#simpan} melempar
	 */
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

	/**
	 * Menulis respons JSON penolakan dengan status HTTP 403 (Forbidden) — dipakai oleh
	 * {@link #simpan} ketika gerbang metode HTTP atau token CSRF gagal.
	 *
	 * @param response response HTTP keluar; diisi status {@code 403} dan badan JSON
	 * @param pesan    pesan penolakan yang ditampilkan ke admin (mis. "Token CSRF tidak valid.")
	 * @throws IOException bila penulisan respons gagal
	 */
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

	/**
	 * Menulis representasi teks {@code hasil} apa adanya ke badan respons — titik keluar
	 * tunggal yang dipakai seluruh handler aksi (langsung maupun lewat {@link #tolak}) agar
	 * format penulisan JSON konsisten.
	 *
	 * @param response response HTTP keluar; badannya diisi {@code hasil.toString()}
	 * @param hasil    objek JSON yang sudah diisi oleh pemanggil
	 * @throws IOException bila penulisan ke writer respons gagal
	 */
	private void tulis(HttpServletResponse response, JSONObject hasil) throws IOException {
		response.getWriter().write(hasil.toString());
	}

	/**
	 * Menormalkan nilai parameter request menjadi string yang tidak pernah {@code null}:
	 * mengembalikan string kosong bila {@code nilai} adalah {@code null}, atau hasil
	 * {@link String#trim()} bila tidak, agar seluruh pemanggil di kelas ini bebas melakukan
	 * pengecekan panjang/isi tanpa perlu menjaga null-check berulang.
	 *
	 * @param nilai nilai mentah dari {@link HttpServletRequest#getParameter}, boleh {@code null}
	 * @return {@code nilai} yang sudah di-{@code trim}, atau string kosong bila {@code nilai}
	 *         adalah {@code null}
	 */
	private static String teks(String nilai) {
		return nilai == null ? "" : nilai.trim();
	}
}
