package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet kiosk "Welsis" (<i>Welcome Siswa</i> — anjungan absensi kedatangan siswa mandiri:
 * scan kartu/input kode), dipetakan ke URL <code>/welsis</code> (lihat {@code web.xml}).
 *
 * <p><b>Peran.</b> Sama seperti servlet kiosk sejenis {@link Welpus} (perpustakaan): kelas ini
 * murni <i>router tampilan</i>, TIDAK menyentuh basis data dan TIDAK memiliki aksi
 * (<code>?action=...</code>) miliknya sendiri. Satu-satunya keputusan yang diambilnya adalah
 * memilih salah satu dari dua implementasi layar kiosk:</p>
 * <ul>
 *   <li><b>Layar baru (default)</b> — meneruskan ke {@code /WEB-INF/baru/welsis.jsp}, yang pada
 *       gilirannya menyertakan {@code /WEB-INF/baru/modul/welsis/welsis.jsp} (layar scanner) dan
 *       memanggil layanan JSON-nya lewat {@code /welsis?hanya_tampil_jsp=true&p=welsis&s=_welsis_service}
 *       ({@code _welsis_service.jsp}).</li>
 *   <li><b>Layar lama</b> — bila parameter <code>versilama</code> bernilai <code>true</code>
 *       (huruf besar/kecil diabaikan), meneruskan ke ZUL {@code /WEB-INF/z/x/y/welsis.zul} yang
 *       di-<i>apply</i> composer ZK {@code ais.action.master.sekolah.KunjunganSiswaAction}.</li>
 * </ul>
 *
 * <h3>STATUS KEAMANAN (diperiksa dan ditambal 2026-09-03)</h3>
 * <p>Sebelum tambalan ini, <code>_welsis_service.jsp</code> pra-otentikasi PENUH (tidak ada
 * gerbang Spring Security untuk <code>/welsis</code> — jatuh ke katalog <code>/**</code> ber-akses
 * {@code IS_AUTHENTICATED_ANONYMOUSLY} — dan servlet ini sendiri tidak memeriksa sesi apa pun):
 * <code>action=list</code> membocorkan nama/NIS/kelas/jam seluruh siswa LINTAS SEMUA SEKOLAH DAN
 * YAYASAN dalam satu instalasi tanpa filter kepemilikan apa pun, dan <code>action=scan</code>
 * mencari {@code Siswa} secara global (lintas sekolah) lalu memalsukan baris kehadiran untuk kode
 * apa pun yang cocok — sekaligus berfungsi sebagai <i>oracle</i> identitas NIS/NISN karena nama
 * asli siswa yang cocok selalu dikembalikan.</p>
 *
 * <p><b>Perbaikan (layar baru / jalur default):</b> {@code _welsis_service.jsp} kini:</p>
 * <ol>
 *   <li>meresolusi sekolah pemilik kiosk di SERVER lewat {@code SekolahUtil.getSekolah(request)}
 *       (hak akses staf yang login, lalu domain/subdomain permintaan, lalu fallback satu-satunya
 *       sekolah pada instalasi bersekolah tunggal) — TIDAK PERNAH dari parameter klien; gagal-tutup
 *       (menolak seluruh aksi) bila sekolah tidak dapat ditentukan;</li>
 *   <li>membatasi <code>action=scan</code> (pencarian &amp; pencatatan {@code Siswa}/
 *       {@code KunjunganSiswa}) dan <code>action=list</code> ke sekolah kiosk itu saja, dan
 *       <code>action=list</code> ke tanggal hari ini saja (sesuai label UI "Log Absensi Hari Ini");</li>
 *   <li>mewajibkan <code>action=scan</code> memakai POST + token CSRF sesi ({@code NewUiCsrfUtil}),
 *       meniru pola <code>action=scan</code>/<code>action=guest</code> pada anjungan kunjungan
 *       perpustakaan ({@link ais.action.master.library.modern.LibraryVisitKioskApi});</li>
 *   <li>menyamarkan nama/NIS pada <code>action=list</code> untuk pemanggil non-staf (inisial +
 *       tiga karakter terakhir), meniru penyamaran pada anjungan kunjungan perpustakaan.</li>
 * </ol>
 *
 * <p><b>BELUM ditambal — layar lama (<code>?versilama=true</code>):</b> sama seperti kondisi
 * {@link Welpus} sebelum ditambal, {@code KunjunganSiswaAction.initCriteria()} jatuh ke
 * {@code Restrictions.sqlRestriction("true")} selama kombobox sekolah belum dipilih, sehingga
 * grid ZK tetap dapat memuat SELURUH baris {@code KunjunganSiswa} lintas sekolah/yayasan untuk
 * pengunjung anonim; {@code onKodeSiswa()} pun menolak memindai tanpa sekolah dipilih tetapi TIDAK
 * membatasi sekolah yang boleh dipilih ke sekolah tertentu. Jalur ini TIDAK tersentuh oleh
 * perbaikan pada javadoc ini dan perlu ditangani terpisah bila layar lama masih dipakai di
 * lapangan.</p>
 *
 * <p><b>Tidak ditemukan pembatasan laju (<i>rate limiting</i>)</b> pada <code>action=scan</code>;
 * pembatasan sekolah di atas mempersempit ruang enumerasi NIS/NISN dari seluruh instalasi menjadi
 * satu sekolah, tetapi tidak menghilangkan sepenuhnya sifat oracle identitasnya.</p>
 *
 * @see ais.action.master.sekolah.KunjunganSiswaAction
 * @see ais.database.model.sekolah.KunjunganSiswa
 * @see Welpus
 */
public class Welsis extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Welsis() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		boolean isRequestVersiLamaNull = request.getParameter("versilama") == null;

		if (!isRequestVersiLamaNull && request.getParameter("versilama").equalsIgnoreCase("true")) {
			request.getRequestDispatcher("/WEB-INF/z/x/y/welsis.zul").forward(request, response);
			return;
		}

		String dispatcherPath = "/WEB-INF/baru/welsis.jsp";
		request.getRequestDispatcher(dispatcherPath).forward(request, response);

	}

}
