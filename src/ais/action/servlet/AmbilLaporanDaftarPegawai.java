package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ais.action.report.Report;
import ais.common.Common;
import ais.database.model.Tbmuser;

/**
 * Servlet yang mengalirkan laporan daftar pegawai institusi ({@code namaLaporan}, bawaan
 * {@code "daftar_pegawai"}) sebagai PDF/gambar, dibangun dari templat JasperReports pada
 * direktori {@code employ/}.
 *
 * <h4>Keamanan &mdash; gerbang (ditambal 2026-09-07, temuan semula diverifikasi tanggal sama)</h4>
 * <p>Sebelum tambalan ini, endpoint ini <b>sepenuhnya anonim</b>: tidak ada pemeriksaan
 * sesi/login apa pun, dan {@code namaLaporan}/{@code type} diteruskan MENTAH tanpa daftar
 * putih ke {@code Report.generateFileReport}. Direktori {@code employ/} yang sama memuat,
 * berdampingan dengan {@code daftar_pegawai.jrxml} (query TANPA klausa cakupan
 * satker/institusi &mdash; menampilkan SELURUH pegawai aktif), templat terkait gaji seperti
 * {@code Draft_Gaji_Pokok.jrxml} dan {@code Rekap_Tunjangan_Pekerja_Report.jrxml}. Ini adalah
 * temuan yang BERBEDA dari klaster {@code task_493423ef} (yang mencakup id sekuensial
 * per-baris) &mdash; di sini dampaknya dump massal PII seluruh pegawai sekaligus.</p>
 * <p>{@link #process} kini menggerbangi permintaan secara <i>fail-closed</i> sebelum laporan
 * apa pun dibangun, memakai pola yang sama dengan {@code AmbilLaporanMahasiswa} pada paket
 * yang sama:</p>
 * <ul>
 *   <li>Pengguna wajib login &mdash; dibaca langsung dari atribut {@code "mytbmuser"} pada
 *       {@link HttpSession} permintaan, <b>bukan</b> {@code Common.getCurrentUser()}/
 *       {@code getCurrentUser(request)} (keduanya, bila sesi kosong, jatuh ke parameter
 *       permintaan {@code user} dan mempercayainya tanpa verifikasi sesi &mdash; celah
 *       terpisah yang masih terbuka di banyak berkas lain pada basis kode ini). Sesi kosong
 *       &rarr; 401.</li>
 *   <li>Pengguna yang login harus memegang
 *       {@link ais.database.model.Tbmrole#getMelihatDataPegawaiLain()} bernilai {@code true}
 *       &mdash; getter ini secara khusus menjawab "boleh melihat data pegawai LAIN", persis
 *       kebutuhan laporan yang menampilkan seluruh pegawai institusi (bukan
 *       {@code getMelihatDataSatkerLain()}, yang menjawab pertanyaan lintas-satker, bukan
 *       lintas-pegawai). Gagal &rarr; 403.</li>
 *   <li>Parameter {@code namaLaporan} digerbangi daftar putih {@link #NAMA_LAPORAN_DIIZINKAN}
 *       (hanya {@code "daftar_pegawai"}, satu-satunya nilai yang pernah dipakai &mdash; tidak
 *       ditemukan pemanggil JSP/ZK yang mengirim nilai lain) sehingga templat gaji pada
 *       direktori {@code employ/} yang sama tidak dapat diakses lewat endpoint ini; nilai di
 *       luar daftar putih &rarr; 400. Parameter {@code type} digerbangi daftar putih
 *       {@link #TYPE_DIIZINKAN} ({@code Report.HTML}, {@code Report.PDF}, {@code "image"})
 *       sehingga tidak diteruskan mentah ke header {@code Content-Type}.</li>
 * </ul>
 * <p><b>Belum ditambal:</b> {@code applicationContext-security.xml} belum diberi
 * {@code intercept-url} eksplisit untuk {@code /AmbilLaporanDaftarPegawai} sebagai pertahanan
 * berlapis (gerbang utama tetap di kode servlet ini). Laporan juga belum di-scope ke satker
 * pengguna login &mdash; siapa pun yang lolos gerbang {@code getMelihatDataPegawaiLain()}
 * tetap melihat seluruh pegawai aktif institusi, bukan hanya satkernya sendiri; ini konsisten
 * dengan makna getter tersebut ("lintas pegawai", bukan "lintas satker"), tetapi perlu
 * ditinjau ulang bila institusi bersifat multi-satker/multi-tenant.</p>
 *
 * @see HttpServlet
 * @see ais.database.model.Tbmrole#getMelihatDataPegawaiLain()
 */
public class AmbilLaporanDaftarPegawai extends HttpServlet {

	/**
	 * Daftar putih nilai parameter {@code namaLaporan} yang boleh dilayani {@link #process}.
	 * Direktori laporan {@code employ/} juga memuat templat gaji yang tidak boleh terekspos
	 * lewat endpoint kepegawaian ini; hanya nilai bawaan yang pernah dipakai yang diizinkan.
	 */
	private static final Set<String> NAMA_LAPORAN_DIIZINKAN = new HashSet<String>(Arrays.asList("daftar_pegawai"));

	/**
	 * Daftar putih nilai parameter {@code type} yang boleh dilayani {@link #process}, agar
	 * nilainya tidak diteruskan mentah sebagai bagian header {@code Content-Type}.
	 */
	private static final Set<String> TYPE_DIIZINKAN = new HashSet<String>(
			Arrays.asList(Report.HTML, Report.PDF, "image"));
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet. Tidak melakukan
	 * inisialisasi apa pun; seluruh state diambil per-permintaan di {@link #process}.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLaporanDaftarPegawai() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}. Gerbang
	 * otentikasi/otorisasi dijalankan di {@link #process}, bukan di sini &mdash; lihat bagian
	 * Keamanan pada dokumentasi kelas.
	 *
	 * @param request  permintaan masuk berisi parameter {@code type} dan {@code namaLaporan}
	 * @param response balasan yang akan diisi bita berkas laporan
	 * @throws ServletException tidak pernah dilempar keluar method ini; kegagalan
	 *                          {@link #process} ditelan {@link Common#tampilErrorJikaAdmin(Exception)}
	 * @throws IOException      tidak pernah dilempar keluar method ini, dengan alasan yang sama
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
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}, termasuk gerbang
	 * otentikasi/otorisasi yang dijalankan {@link #process}.
	 *
	 * @param request  permintaan masuk berisi parameter {@code type} dan {@code namaLaporan}
	 * @param response balasan yang akan diisi bita berkas laporan
	 * @throws ServletException tidak pernah dilempar keluar method ini
	 * @throws IOException      tidak pernah dilempar keluar method ini
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

	/**
	 * Inti servlet: menggerbangi permintaan, lalu menerjemahkan parameter {@code type} dan
	 * {@code namaLaporan} menjadi berkas laporan di bawah direktori {@code employ/}, dan
	 * menyalinnya ke {@code response} sebagai unduhan.
	 *
	 * <h4>Urutan kerja</h4>
	 * <ol>
	 *   <li>seluruh nama dan nilai parameter dicetak ke {@code System.out};</li>
	 *   <li>gerbang login (fail-closed): baca LANGSUNG dari {@link HttpSession} (atribut
	 *       {@code "mytbmuser"}); kosong &rarr; balasan 401, proses dihentikan &mdash; lihat
	 *       peringatan Keamanan pada dokumentasi kelas untuk alasan tidak memakai
	 *       {@code Common.getCurrentUser()};</li>
	 *   <li>gerbang otorisasi: pengguna login harus memegang
	 *       {@link ais.database.model.Tbmrole#getMelihatDataPegawaiLain()} bernilai
	 *       {@code true}; gagal &rarr; balasan 403;</li>
	 *   <li>{@code type} dibaca (bawaan {@link Report#HTML} bila tidak ada) dan
	 *       {@code namaLaporan} dibaca (bawaan {@code "daftar_pegawai"} bila tidak ada), lalu
	 *       keduanya digerbangi daftar putih {@link #TYPE_DIIZINKAN}/
	 *       {@link #NAMA_LAPORAN_DIIZINKAN}; nilai di luar daftar putih &rarr; balasan 400,
	 *       proses dihentikan (lihat bagian Keamanan pada dokumentasi kelas);</li>
	 *   <li>parameter laporan dibangun lewat {@code HashMapGenerator.getRandStringSerializable()}
	 *       (hanya berisi nilai acak internal, TANPA filter cakupan satker/institusi apa pun
	 *       &mdash; lihat catatan "Belum ditambal" pada dokumentasi kelas);</li>
	 *   <li>bila {@code type} adalah {@code "image"} (tanpa peka besar/kecil huruf), berkas
	 *       gambar dibangun lewat {@link Report#generateFileImageReport} dan {@code Content-Type}
	 *       disetel {@code "image/jpeg"}; selain itu, berkas dibangun lewat
	 *       {@link Report#generateFileReport} dan {@code Content-Type} disetel
	 *       {@code "application/" + type} (sudah divalidasi daftar putih di atas);</li>
	 *   <li>berkas hasil disalin ke {@code response.getOutputStream()} dengan penyangga
	 *       1&nbsp;KiB.</li>
	 * </ol>
	 *
	 * @param request  permintaan masuk berisi parameter {@code type} dan {@code namaLaporan}
	 * @param response balasan yang akan diisi bita berkas laporan
	 * @throws Exception bila pembangunan laporan atau penulisan balasan gagal
	 */
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Enumeration<String> enumeration = request.getParameterNames();
		while (enumeration.hasMoreElements()) {
			String param = enumeration.nextElement();
			System.out.print("  " + param + " = " + request.getParameter(param));
		}

		// Gerbang login (fail-closed): baca LANGSUNG dari HttpSession, bukan lewat
		// Common.getCurrentUser()/getCurrentUser(request) -- keduanya, bila sesi kosong, jatuh
		// ke parameter permintaan "user" dan mempercayainya tanpa verifikasi sesi. Lihat
		// Javadoc kelas untuk rincian.
		HttpSession httpSession = request.getSession(false);
		Tbmuser tbmuserLogin = httpSession == null ? null : (Tbmuser) httpSession.getAttribute("mytbmuser");
		if (tbmuserLogin == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Silakan login terlebih dahulu.");
			return;
		}

		// Gerbang otorisasi: laporan ini menampilkan SELURUH pegawai aktif institusi, jadi
		// pengguna login wajib memegang hak melihat data pegawai lain (bukan hanya data
		// dirinya sendiri).
		boolean bolehLihatDataPegawaiLain = tbmuserLogin.hakAkses() != null
				&& Boolean.TRUE.equals(tbmuserLogin.hakAkses().getMelihatDataPegawaiLain());
		if (!bolehLihatDataPegawaiLain) {
			ServletContext sc = getServletContext();
			sc.log("Akses laporan daftar pegawai ditolak: pengguna " + tbmuserLogin.getUserId()
					+ " tidak memegang hak melihat data pegawai lain.");
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Anda tidak berhak mengakses laporan ini.");
			return;
		}

		String type = request.getParameter("type") == null ? Report.HTML : request.getParameter("type");
		String namaLaporan = request.getParameter("namaLaporan") == null ? "daftar_pegawai"
				: request.getParameter("namaLaporan");

		if (!TYPE_DIIZINKAN.contains(type) || !NAMA_LAPORAN_DIIZINKAN.contains(namaLaporan)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter laporan tidak dikenali.");
			return;
		}

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		File file = null;

		if (!type.equalsIgnoreCase("image")) {
			file = Report.generateFileReport(type, parameters, "employ/" + namaLaporan, ais.ui.util.WaktuUtil.getDate(),
					Common.locale);
			response.setContentType("application/" + type);
		} else {
			file = Report.generateFileImageReport(type, parameters, "employ/" + namaLaporan,
					ais.ui.util.WaktuUtil.getDate(), Common.locale);
			response.setContentType("image/jpeg");
		}

		// System.out.println("file = " + file);

		ServletOutputStream out = response.getOutputStream();
		FileInputStream in = new FileInputStream(file);
		int length = (int) file.length();

		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		while ((length = in.read(buffer)) != -1) {
			out.write(buffer, 0, length);
		}

		in.close();
		out.flush();

	}

}
