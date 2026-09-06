package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;

/**
 * Servlet pencetak <b>kartu/slip Virtual Account</b> berformat PDF, memakai template
 * {@code report/va.jrxml}.
 *
 * <h4>Catatan arsitektur &mdash; bukan pembaca data, melainkan pencetak ulang parameter</h4>
 * <p>Berbeda dari kebanyakan {@code AmbilLaporan*} lain yang mengambil data dari basis data
 * berdasarkan id pada parameter, servlet ini <b>tidak melakukan satu pun kueri data transaksi</b>.
 * {@code report/va.jrxml} tidak memiliki {@code queryString} sama sekali; seluruh medan yang tampil
 * di PDF ({@code nama}, {@code va}, {@code nominal}, {@code biayaAdministrasi}, {@code kadalurasa},
 * {@code biayaTotal}, {@code terbilang}, {@code qr}, {@code keteranganNominal1}) diambil langsung dari
 * {@code REPORT_PARAMETERS_MAP}, yaitu peta yang dibentuk {@link #laporan} dari <b>seluruh parameter
 * permintaan HTTP apa adanya</b> (lihat {@link #laporan}). Satu-satunya data yang benar-benar diambil
 * dari basis data adalah gambar kop surat ({@code kop_surat}/{@code kop_bawah_surat}), dipetakan
 * lewat {@link PerguruanTinggiUtil#getPerguruanTinggi(HttpServletRequest)}/
 * {@link SekolahUtil#getSekolah(HttpServletRequest)} (resolusi tenant dari konteks permintaan, bukan
 * dari parameter yang dikendalikan pemanggil).</p>
 *
 * <h4>PERINGATAN KEAMANAN &mdash; tidak ada gerbang login, berbeda dari saudara sekelasnya</h4>
 * <p>Endpoint ini <b>tidak muncul di {@code applicationContext-security.xml}</b> dan jatuh ke aturan
 * penampung {@code /**} bernilai {@code IS_AUTHENTICATED_ANONYMOUSLY} &mdash; berbeda dari
 * {@code AmbilLaporanMahasiswa} dan {@code AmbilLaporanDaftarPegawai} yang masing-masing sudah
 * ditambahi {@code intercept-url} eksplisit setelah audit broken-access-control sebelumnya. Karena
 * servlet ini tidak mengambil data pribadi dari basis data berdasarkan id (lihat paragraf di atas),
 * konsekuensi utamanya <b>bukan</b> kebocoran data mahasiswa/pegawai, melainkan <b>pemalsuan
 * dokumen</b>: siapa pun tanpa login dapat memanggil {@code /AmbilLaporanVa} dengan
 * {@code nama}/{@code va}/{@code nominal}/{@code biayaTotal}/{@code terbilang} rekaan bebas, dan
 * menerima PDF berkop surat resmi institusi (diresolusi otomatis dari tenant permintaan) yang terlihat
 * seperti slip pembayaran VA asli. Ini pola yang sama arahnya dengan temuan broken-access-control pada
 * servlet {@code AmbilLaporan*} lain, namun dampaknya berbeda (forjer dokumen, bukan kebocoran PII)
 * karena tidak adanya kueri data pada kelas ini.</p>
 *
 * @see PerguruanTinggiUtil
 * @see SekolahUtil
 * @see ais.action.report.Report
 */
public class AmbilLaporanVa extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena instance
	 * servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLaporanVa() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}.
	 *
	 * <p>Kegagalan ditelan {@link Common#tampilErrorJikaAdmin(Exception)} sehingga pemanggil tidak
	 * menerima jejak galat mentah; lihat juga penanganan kegagalan internal pada {@link #process}
	 * yang membalas {@code SC_INTERNAL_SERVER_ERROR} bila berkas laporan gagal dibentuk.</p>
	 *
	 * @param request  permintaan berisi parameter yang akan dicetak apa adanya ke PDF
	 * @param response balasan berupa berkas PDF ({@code application/pdf})
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
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
	 * Menangani permintaan HTTP POST dengan mendelegasikannya ke {@link #process}.
	 *
	 * <p>Perilaku sama persis dengan {@link #doGet}.</p>
	 *
	 * @param request  permintaan berisi parameter yang akan dicetak apa adanya ke PDF
	 * @param response balasan berupa berkas PDF ({@code application/pdf})
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
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
	 * Mencetak seluruh nama parameter permintaan ke {@code System.out} (tanpa nilai lengkap yang
	 * berguna &mdash; lihat kode), membentuk berkas PDF lewat {@link #laporan}, lalu menuliskannya
	 * ke respons.
	 *
	 * <p>Bila {@link #laporan} mengembalikan {@code null} (kegagalan pembentukan laporan), respons
	 * dibalas {@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR} dan pesan dicatat lewat
	 * {@link ServletContext#log(String)}. Sebaliknya, berkas PDF dibaca dan disalin ke
	 * {@link ServletOutputStream} memakai penyangga 1024 byte, dengan header
	 * {@code Content-Type: application/pdf}.</p>
	 *
	 * @param request permintaan HTTP; seluruh parameternya diteruskan apa adanya ke {@link #laporan}
	 * @param resp    respons yang akan diisi berkas PDF, atau status 500 bila laporan gagal dibentuk
	 * @throws Exception bila pembacaan/penulisan berkas atau pembentukan laporan gagal
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		Enumeration<String> enumeration = request.getParameterNames();
		while (enumeration.hasMoreElements()) {
			String param = enumeration.nextElement();
			System.out.print("  " + param + " = " + request.getParameter(param));
		}

		File file = laporan(request);

		if (file == null) {
			ServletContext sc = getServletContext();
			sc.log("Laporan Mahasiswa tidak ditemukan");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		resp.setContentType("application/pdf");

		ServletOutputStream out = resp.getOutputStream();
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

	/**
	 * Membentuk berkas PDF kartu Virtual Account dari template {@code report/va.jrxml}, memakai
	 * seluruh parameter permintaan sebagai medan yang dicetak apa adanya, ditambah gambar kop surat
	 * yang diresolusi dari tenant permintaan.
	 *
	 * <p>Peta parameter awal dibentuk {@code HashMapGenerator.getRandStringSerializable()} lalu
	 * ditimpa oleh <b>seluruh</b> parameter {@code request} (nama-nilai apa adanya, tanpa validasi
	 * atau sanitasi apa pun) &mdash; termasuk medan yang dirender langsung ke PDF seperti
	 * {@code nama}, {@code va}, {@code nominal}, {@code biayaTotal}, dan {@code terbilang} (lihat
	 * peringatan pada Javadoc kelas). Setelahnya, kop surat ditentukan:</p>
	 * <ul>
	 *   <li>bila {@link SekolahUtil#getSekolah(HttpServletRequest)} mengembalikan {@link Sekolah}
	 *       bervalid id, kop atas diambil dari {@link LampiranLain#KOP_SEKOLAH} milik sekolah itu,
	 *       jatuh ke {@link LampiranLain#KOP_PT} milik {@link PerguruanTinggi} bila sekolah tidak
	 *       punya kop sendiri; kop bawah diambil dari {@link LampiranLain#KOP_BAWAH_SEKOLAH} (jatuh
	 *       ke {@link LampiranLain#KOP_BAWAH_PT} bila perguruan tinggi tersedia);</li>
	 *   <li>bila tidak ada {@link Sekolah}, kop atas/bawah diambil langsung dari
	 *       {@link LampiranLain#KOP_PT}/{@link LampiranLain#KOP_BAWAH_PT} milik
	 *       {@link PerguruanTinggi} hasil {@link PerguruanTinggiUtil#getPerguruanTinggi(HttpServletRequest)};</li>
	 *   <li>bila lampiran kop tidak ditemukan pada salah satu cabang, parameter kop diisi string
	 *       kosong (bukan dihilangkan dari peta).</li>
	 * </ul>
	 *
	 * <p>Berkas akhirnya dibentuk lewat {@link ais.action.report.Report#generateFileReport(String,
	 * java.util.Map, String, java.util.Date, java.util.Locale)} dengan kode laporan {@code "va"}.</p>
	 *
	 * @param request permintaan HTTP; seluruh parameternya masuk apa adanya ke peta parameter laporan
	 * @return berkas PDF hasil generate, atau {@code null}/melempar bila pembentukan laporan gagal
	 * @throws Exception bila pembentukan laporan oleh {@link ais.action.report.Report} gagal
	 */
	private File laporan(HttpServletRequest request) throws Exception {

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		Enumeration<String> ennu = request.getParameterNames();
		while (ennu.hasMoreElements()) {
			String name = ennu.nextElement();
			parameters.put(name, request.getParameter(name));
		}

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
		Sekolah sekolah = SekolahUtil.getSekolah(request);

		if (sekolah != null && sekolah.getId() != null) {

			LampiranLain lampiranLain = LampiranLain.ambil(sekolah.getId(), LampiranLain.KOP_SEKOLAH);
			if (lampiranLain != null) {
				parameters.put("kop_surat", lampiranLain.ambilFile().getAbsolutePath());
				parameters.put("kop_surat_local", lampiranLain.ambilFile().getAbsolutePath());
			} else {
				if (perguruanTinggi != null) {
					lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
					if (lampiranLain != null) {
						parameters.put("kop_surat", lampiranLain.ambilFile().getAbsolutePath());
						parameters.put("kop_surat_local", lampiranLain.ambilFile().getAbsolutePath());
					} else {
						parameters.put("kop_surat", "");
						parameters.put("kop_surat_local", "");
					}

					lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
					if (lampiranLain != null) {
						parameters.put("kop_bawah_surat", lampiranLain.ambilFile().getAbsolutePath());
						parameters.put("kop_bawah_surat_local", lampiranLain.ambilFile().getAbsolutePath());
					} else {
						parameters.put("kop_bawah_surat", "");
						parameters.put("kop_bawah_surat_local", "");
					}
				}
			}

			lampiranLain = LampiranLain.ambil(sekolah.getId(), LampiranLain.KOP_BAWAH_SEKOLAH);
			if (lampiranLain != null) {
				parameters.put("kop_bawah_surat", lampiranLain.ambilFile().getAbsolutePath());
				parameters.put("kop_bawah_surat_local", lampiranLain.ambilFile().getAbsolutePath());
			}

		} else {
			if (perguruanTinggi != null) {
				LampiranLain lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
				if (lampiranLain != null) {
					parameters.put("kop_surat", lampiranLain.ambilFile().getAbsolutePath());
					parameters.put("kop_surat_local", lampiranLain.ambilFile().getAbsolutePath());
				} else {
					parameters.put("kop_surat", "");
					parameters.put("kop_surat_local", "");
				}

				lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
				if (lampiranLain != null) {
					parameters.put("kop_bawah_surat", lampiranLain.ambilFile().getAbsolutePath());
					parameters.put("kop_bawah_surat_local", lampiranLain.ambilFile().getAbsolutePath());
				} else {
					parameters.put("kop_bawah_surat", "");
					parameters.put("kop_bawah_surat_local", "");
				}
			}

		}

		System.out.println("parameters => " + parameters);
		File file = Report.generateFileReport(Report.PDF, parameters, "va", ais.ui.util.WaktuUtil.getDate(),
				Common.locale);
		return file;
	}

}
