package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ais.common.Common;

/**
 * Servlet cermin HTML halaman <code>/main</code> tanpa "watermark" evaluasi ZK &mdash; dipetakan
 * ke <code>/n</code> (servlet-name <code>n</code> pada {@code web.xml}, kelas {@code M}; perhatikan
 * bahwa nama pemetaan URL, nama <i>servlet-name</i>, dan nama kelas ketiganya berbeda).
 *
 * <p><b>Tujuan.</b> Mengambil ulang halaman <code>/main</code> dari server yang sama lewat
 * permintaan HTTP <i>sisi-server</i> (memakai pustaka Jsoup, bukan {@code RequestDispatcher}),
 * membuang komentar HTML watermark evaluasi lisensi ZK
 * (<code>&lt;!-- ZK 5.0.13 EE 2013100810 Evaluation Only --&gt;</code>), lalu menuliskan HTML hasil
 * saringan itu langsung sebagai respons. Efeknya: pengunjung <code>/n</code> melihat salinan
 * tampilan awal <code>/main</code> tanpa watermark, tanpa perlu ZK memuat ulang seluruh siklus
 * hidup <i>desktop</i>-nya.</p>
 *
 * <p><b>BUKAN {@link MServet} &mdash; dua servlet berbeda dengan nama kelas mirip.</b> Berkas ini
 * ({@code ais.action.servlet.M}, dipetakan ke <code>/n</code>) sudah diverifikasi langsung terhadap
 * {@code web.xml} dan kode {@link MServet} (dipetakan ke <code>/m</code>, servlet-name
 * <code>m</code>): keduanya <b>tidak berbagi kode maupun pemetaan URL</b>. {@link MServet} adalah
 * penerima "magic link" login otomatis yang menjadi subjek temuan <code>task_5a059324</code>
 * (token DES deterministik dengan passphrase global tertanam, memungkinkan pengambilalihan akun).
 * Kelas {@code M} di berkas ini <b>tidak mendekripsi token apa pun, tidak memanggil
 * {@code SecurityFilter.doAutoLogin}, dan tidak membaca parameter permintaan sama sekali</b> &mdash;
 * satu-satunya kemiripan adalah nama kelas satu huruf yang membingungkan. <b>Temuan
 * {@code task_5a059324} tidak berlaku untuk berkas ini.</b></p>
 *
 * <p><b>TEMUAN BARU &mdash; SSRF/pemalsuan isi lewat header <code>Host</code> yang tidak
 * disaring.</b> Diverifikasi langsung dari kode {@link #process}: URL yang diambil dibangun dari
 * {@code Common.getRequestHostWithProtocol(request)}, yang meneruskan ke
 * {@code CommonCurrentSessionHelper.getRequestHostWithProtocol(HttpServletRequest)}. Method itu
 * menyusun host langsung dari <code>request.getServerName()</code> (yakni header {@code Host} pada
 * permintaan HTTP) <b>tanpa melalui gerbang {@code Common.sanitizedRequestHostForCurrentUrl(...)}
 * / {@code isHostAllowedForCurrentUrl(...)}</b> yang dipakai di tempat lain (lihat
 * {@link Dashboard#process} dan berkas sejenis) untuk menolak nilai {@code Host} yang tidak
 * dikenal. Karena hasilnya lalu diserahkan ke {@code Jsoup.connect(url).get()} &mdash; permintaan
 * HTTP keluar sungguhan dari server &mdash; pengiriman header <code>Host</code> yang dipalsukan
 * (mis. <code>Host: layanan-internal.contoh</code>) membuat server AIS sendiri yang mengambil
 * <code>http(s)://&lt;host palsu&gt;/main</code> dan <b>menuliskan hasilnya kembali ke pemanggil</b>
 * lewat {@code out.print(h)}. Ini adalah pola <i>Server-Side Request Forgery</i> (SSRF) klasik:
 * server dapat dipaksa memanggil host mana pun yang dapat dijangkaunya (termasuk layanan internal
 * yang tidak diekspos ke publik) dan hasilnya dibocorkan ke penyerang lewat respons ini, dengan
 * satu-satunya syarat header {@code Host} pada permintaan awal dapat dipalsukan (lazim bila server
 * tidak divalidasi ketat oleh <i>reverse proxy</i> di depannya). Kerentanan ini belum ditambal;
 * dokumentasi ini tidak mengubah perilaku kode.</p>
 *
 * <p><b>Penanganan galat longgar.</b> Blok {@code catch} terluar pada {@link #process} hanya
 * memanggil {@code System.out.println(e)} tanpa mencatat lewat {@code ErrorAuditUtil} dan tanpa
 * mengirim status HTTP galat ke klien; bila pengambilan atau penulisan gagal, klien menerima
 * respons kosong berstatus 200.</p>
 *
 * <p><b>Nama kelas dan komentar menyesatkan.</b> Komentar bawaan generator Eclipse semula berbunyi
 * "Servlet implementation class CheckISBN" &mdash; sisa salin-tempel dari servlet perpustakaan,
 * tidak ada hubungannya dengan fungsi kelas ini.</p>
 *
 * @see MServet
 * @see ais.common.Common#getRequestHostWithProtocol(HttpServletRequest)
 * @see ais.common.Common#sanitizedRequestHostForCurrentUrl(HttpServletRequest)
 */
public class M extends HttpServlet {

	/**
	 * Nomor versi serialisasi bawaan {@link HttpServlet}.
	 *
	 * <p>Dibiarkan pada nilai {@code 1L} hasil wizard servlet Eclipse. Servlet ini tidak menyimpan
	 * state instance apa pun, sehingga serialisasi/deserialisasi kontainer tidak membawa data yang
	 * perlu dijaga kompatibilitasnya.</p>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diperlukan kontainer servlet.
	 *
	 * <p>Hanya memanggil konstruktor {@link HttpServlet}; tidak ada inisialisasi tambahan. Seluruh
	 * pekerjaan dilakukan per-request di {@link #process(HttpServletRequest, HttpServletResponse)},
	 * sehingga instance servlet tetap tanpa state dan aman dipakai bersama oleh banyak thread.</p>
	 */
	public M() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET &mdash; bentuk pemanggilan lazim untuk mengambil salinan HTML
	 * <code>/main</code> tanpa watermark.
	 *
	 * <p><b>Cara kerja.</b> Melimpahkan seluruh pekerjaan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}. Karena {@code process} sudah
	 * membungkus badannya sendiri dalam {@code try/catch} yang menelan semua galat (lihat Javadoc
	 * {@link #process}), blok {@code catch} di sini praktis tidak pernah tersentuh.</p>
	 *
	 * @param request permintaan HTTP; header {@code Host} dibaca secara tidak langsung lewat
	 *        {@code Common.getRequestHostWithProtocol(request)} di dalam {@code process} (lihat
	 *        catatan SSRF pada Javadoc kelas)
	 * @param response tanggapan HTTP; diisi salinan HTML halaman <code>/main</code>
	 * @throws ServletException bila kontainer melaporkan kegagalan servlet
	 * @throws IOException bila terjadi kegagalan I/O pada response
	 * @see #process(HttpServletRequest, HttpServletResponse)
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
	 * Menangani permintaan HTTP POST &mdash; identik dengan
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p><b>Cara kerja.</b> Sama persis dengan {@code doGet}: memanggil
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.</p>
	 *
	 * @param request permintaan HTTP; lihat catatan pada {@link #doGet}
	 * @param response tanggapan HTTP; diisi salinan HTML halaman <code>/main</code>
	 * @throws ServletException bila kontainer melaporkan kegagalan servlet
	 * @throws IOException bila terjadi kegagalan I/O pada response
	 * @see #process(HttpServletRequest, HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Inti servlet: mengambil ulang HTML <code>/main</code> lewat permintaan HTTP sisi-server,
	 * membuang watermark evaluasi ZK, lalu menuliskannya sebagai respons.
	 *
	 * <h3>Alur lengkap</h3>
	 * <p><b>Langkah 1 &mdash; penyusunan URL sumber.</b>
	 * <code>Common.getRequestHostWithProtocol(request) + "/main"</code> membangun URL dari skema
	 * ({@code http}/{@code https} sesuai {@code Common.isSecure(request)}),
	 * {@code request.getServerName()} (header {@code Host} permintaan masuk, TIDAK disaring lewat
	 * {@code sanitizedRequestHostForCurrentUrl}), dan {@code request.getContextPath()}. Lihat
	 * catatan SSRF pada Javadoc kelas: nilai ini dapat dipalsukan lewat header {@code Host}.</p>
	 *
	 * <p><b>Langkah 2 &mdash; pengambilan halaman.</b> {@code Jsoup.connect(url).get()} melakukan
	 * permintaan HTTP GET sungguhan ke URL tersebut dan mem-parsing hasilnya menjadi
	 * {@link Document}. Ini adalah panggilan jaringan sisi-server yang blocking, tanpa batas waktu
	 * eksplisit yang diset di sini (memakai nilai baku Jsoup).</p>
	 *
	 * <p><b>Langkah 3 &mdash; penyaringan watermark.</b> Seluruh HTML (<code>document.html()</code>)
	 * diproses {@code org.apache.commons.lang3.StringUtils.replace(...)} untuk menghapus literal
	 * komentar <code>&lt;!-- ZK 5.0.13 EE 2013100810 Evaluation Only --&gt;</code>. Ini pencarian
	 * literal (bukan regex), jadi hanya menghapus kemunculan persis string tersebut.</p>
	 *
	 * <p><b>Langkah 4 &mdash; penulisan respons.</b> {@code response.setContentType("text/html")}
	 * lalu HTML hasil saringan dituliskan langsung lewat {@link PrintWriter} dan ditutup. Tidak ada
	 * pengaturan <i>charset</i> eksplisit maupun header cache.</p>
	 *
	 * <p><b>Langkah 5 &mdash; penanganan galat.</b> Seluruh Langkah 1&ndash;4 dibungkus satu
	 * {@code try/catch} yang, bila gagal, hanya memanggil {@code System.out.println(e)} &mdash;
	 * tidak ada pencatatan {@code ErrorAuditUtil}, tidak ada perubahan status HTTP. Klien yang
	 * memicu kegagalan menerima respons HTTP 200 kosong.</p>
	 *
	 * @param request permintaan HTTP; header {@code Host} dipakai (lewat
	 *        {@code Common.getRequestHostWithProtocol}) untuk menyusun URL yang diambil sisi-server
	 * @param response tanggapan HTTP; diisi HTML halaman <code>/main</code> yang sudah disaring dari
	 *        watermark evaluasi ZK
	 * @throws Exception dideklarasikan pada signature, tetapi badan method menangkap seluruh
	 *         {@code Exception} secara internal sehingga praktis tidak pernah melempar ke pemanggil
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {

			String url = Common.getRequestHostWithProtocol(request) + "/main";
			Document document = Jsoup.connect(url).get(); 

			String h = document.html();
			h = org.apache.commons.lang3.StringUtils.replace(h, "<!-- ZK 5.0.13 EE 2013100810 Evaluation Only -->", "");

			response.setContentType("text/html");
			PrintWriter out = response.getWriter();

			out.print(h);

			out.close();

		} catch (Exception e) {
			System.out.println(e);
		}

	}

}
