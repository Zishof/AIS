package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.report.Report;
import ais.common.Common;

/**
 * Servlet publik yang mengalirkan berkas laporan kepegawaian ({@code namaLaporan}, bawaan
 * {@code "daftar_pegawai"}) sebagai PDF/gambar, dibangun dari templat JasperReports pada
 * direktori {@code employ/}.
 *
 * <h4>Keamanan &mdash; temuan (diverifikasi dari kode dan templat laporan, 2026-09-07)</h4>
 * <p>Endpoint ini <b>sepenuhnya anonim</b>: {@link #doGet}/{@link #doPost}/{@link #process}
 * tidak memiliki pemeriksaan sesi/login apa pun (tidak ada {@code HttpSession}, tidak ada
 * {@code Common.getCurrentUser()}), dan tidak ada {@code intercept-url} khusus untuk
 * {@code /AmbilLaporanDaftarPegawai} pada {@code applicationContext-security.xml} (berbeda
 * dengan {@code AmbilLaporanMahasiswa} pada paket yang sama, yang sudah digerbangi).</p>
 * <p>Lebih jauh, {@link #process} meneruskan parameter {@code namaLaporan} MENTAH langsung
 * sebagai nama berkas templat ({@code "employ/" + namaLaporan}) tanpa daftar putih maupun
 * validasi apa pun, dan {@code type} MENTAH langsung sebagai {@code formatLaporan} sekaligus
 * bagian header {@code Content-Type}. Direktori laporan {@code employ/} yang sama memuat,
 * di antaranya, {@code daftar_pegawai.jrxml} (daftar pegawai aktif institusi: NIP, nama,
 * pangkat/golongan, jabatan, masa kerja, perkiraan masa pensiun, status pegawai, pendidikan
 * terakhir) MAUPUN templat terkait gaji seperti {@code Draft_Gaji_Pokok.jrxml} dan
 * {@code Rekap_Tunjangan_Pekerja_Report.jrxml} &mdash; siapa pun yang mengetahui/menebak nama
 * berkas dapat memintanya lewat parameter {@code namaLaporan} tanpa login.</p>
 * <p>Pemeriksaan query {@code daftar_pegawai.jrxml} mengonfirmasi TIDAK ADA klausa cakupan
 * satker/institusi sama sekali (hanya {@code where (a.aktif=true or a.aktif is null)} ditambah
 * beberapa filter opsional yang semuanya bernilai "tampilkan semua" ketika parameternya kosong)
 * &mdash; dan servlet ini TIDAK mengirim parameter filter apa pun ke laporan (parameter yang
 * diteruskan hanya string acak dari {@code HashMapGenerator.getRandStringSerializable()}).
 * Konsekuensinya, permintaan anonim ke {@code /AmbilLaporanDaftarPegawai} menghasilkan PDF
 * berisi SELURUH daftar pegawai aktif institusi, bukan satu baris seperti pola id-tunggal pada
 * {@code Struk}.</p>
 * <p>Ini adalah temuan yang BERBEDA dari klaster {@code task_493423ef} (yang mencakup id
 * sekuensial per-baris); temuan ini sudah dilaporkan terpisah untuk ditambal (lihat catatan
 * commit/task terkait), bukan diperbaiki di sesi dokumentasi ini.</p>
 *
 * @see HttpServlet
 */
public class AmbilLaporanDaftarPegawai extends HttpServlet {
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
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}. Tidak ada
	 * gerbang otentikasi/otorisasi di sini maupun di {@link #process} &mdash; lihat bagian
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
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}, termasuk tidak
	 * adanya gerbang otentikasi/otorisasi.
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
	 * Inti servlet: menerjemahkan parameter {@code type} dan {@code namaLaporan} menjadi
	 * berkas laporan di bawah direktori {@code employ/}, lalu menyalinnya ke {@code response}
	 * sebagai unduhan.
	 *
	 * <h4>Urutan kerja</h4>
	 * <ol>
	 *   <li>seluruh nama dan nilai parameter dicetak ke {@code System.out};</li>
	 *   <li>{@code type} dibaca (bawaan {@link Report#HTML} bila tidak ada) dan
	 *       {@code namaLaporan} dibaca (bawaan {@code "daftar_pegawai"} bila tidak ada) &mdash;
	 *       keduanya dipakai APA ADANYA tanpa validasi (lihat bagian Keamanan pada
	 *       dokumentasi kelas);</li>
	 *   <li>parameter laporan dibangun lewat {@code HashMapGenerator.getRandStringSerializable()}
	 *       (hanya berisi nilai acak internal, TANPA filter cakupan satker/institusi apa pun);</li>
	 *   <li>bila {@code type} adalah {@code "image"} (tanpa peka besar/kecil huruf), berkas
	 *       gambar dibangun lewat {@link Report#generateFileImageReport} dan {@code Content-Type}
	 *       disetel {@code "image/jpeg"}; selain itu, berkas dibangun lewat
	 *       {@link Report#generateFileReport} dan {@code Content-Type} disetel
	 *       {@code "application/" + type} MENTAH dari parameter;</li>
	 *   <li>berkas hasil disalin ke {@code response.getOutputStream()} dengan penyangga
	 *       1&nbsp;KiB.</li>
	 * </ol>
	 * <p>Tidak ada gerbang otentikasi/otorisasi pada method ini &mdash; lihat bagian Keamanan
	 * pada dokumentasi kelas untuk rincian temuan dan status penanganannya.</p>
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

		String type = request.getParameter("type") == null ? Report.HTML : request.getParameter("type");
		String namaLaporan = request.getParameter("namaLaporan") == null ? "daftar_pegawai"
				: request.getParameter("namaLaporan");

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
