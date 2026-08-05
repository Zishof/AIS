package ais.action.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.j256.simplemagic.ContentType;

import ais.action.master.helper.UserOnlineCounter;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.servlet.api.WaApi;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisPengaduan;
import ais.database.model.Konfigurasi;
import ais.database.model.NotifikasiWa;
import ais.database.model.Pengaduan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.TanyaJawab;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.WaProfile;
import ais.database.model.sekolah.Sekolah;
import net.sf.jmimemagic.Magic;

/**
 * Servlet implementation class CheckISBN
 */
public class Wa extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private String WEBHOOK_VERIFY_TOKEN = "12345";

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Wa() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String queryString = request.getQueryString();
		String msg = "Error, wrong token";

		if (queryString != null) {
			String verifyToken = request.getParameter("hub.verify_token");
			String challenge = request.getParameter("hub.challenge");
			// String mode = request.getParameter("hub.mode");

			if (StringUtils.equals(WEBHOOK_VERIFY_TOKEN, verifyToken) && !StringUtils.isEmpty(challenge)) {
				msg = challenge;
			} else {
				msg = "";
			}
		} else {
			System.out.println("Exception no verify token found in querystring:" + queryString);
		}

		response.getWriter().write(msg);
		response.getWriter().flush();
		response.getWriter().close();
		response.setStatus(HttpServletResponse.SC_OK);

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

	public static String tanya(String tanyaApa) throws Exception {

		String apa = "{\r\n" + "  \"contents\": [\r\n" + "    {\r\n" + "      \"role\": \"user\",\r\n"
				+ "      \"parts\": [\r\n" + "        {\r\n" + "          \"text\": \"TANYA_APA_SAJA\"\r\n"
				+ "        }\r\n" + "      ]\r\n" + "    }\r\n" + "  ],\r\n" + "  \"generationConfig\": {\r\n"
				+ "    \"temperature\": 1,\r\n" + "    \"topK\": 64,\r\n" + "    \"topP\": 0.95,\r\n"
				+ "    \"maxOutputTokens\": 8192,\r\n" + "    \"responseMimeType\": \"text/plain\"\r\n" + "  }\r\n"
				+ "}";

		tanyaApa = apa.replaceAll("TANYA_APA_SAJA", tanyaApa);

		File fileOut = new File("/opt/tanya/tanyaApa.txt");
		if (!fileOut.getParentFile().exists()) {
			fileOut.getParentFile().mkdirs();
		}

		FileUtils.writeStringToFile(fileOut, tanyaApa);

		String linkPost = "https://generativelanguage.googleapis.com/v1beta/models/"
				+ Common.getKonfigurasi("ai_chatbot_versi_model_gemini", "gemini-2.0-flash").getNilai().trim()
				+ ":generateContent?key="
				+ Common.getKonfigurasi("ai_chatbot_api_key_gemini", "AIzaSyDBSOM4dHks3kQuXyhDzhBkRQz98VjHzPs")
						.getNilai();

		System.out.println("tanyaApa -> " + tanyaApa);

		String[] command = { "curl", "--location", linkPost, "--header", "Content-Type: application/json", "--data",
				"@" + fileOut.getAbsolutePath() };
		String message = "";
		try {

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();

			System.out.println("hasil -> " + hasil);

			JSONObject jsonObject = new JSONObject(hasil);

			message = (jsonObject.getJSONArray("candidates").getJSONObject(0).getJSONObject("content")
					.getJSONArray("parts").getJSONObject(0).get("text")) + "";

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:160");
		}
		return message;
	}

	private String ambilPesan(String to, String tanya, String media, int coba) throws Exception {

		if (coba > 5) {
			return "Maaf, pertanyaan anda belum bisa kami proses, coba kirimkan pertanyaan lain";
		}

		if (Common.bolehKonfigurasi("ai_menggunakan_gemini", Konfigurasi.TIDAK_AKTIF)) {

			String tanyaApa = Common.getKonfigurasi("ai_chatbot_model_gemini", "{\r\n" + "  \"contents\": [\r\n"
					+ "    {\r\n" + "      \"role\": \"user\",\r\n" + "      \"parts\": [\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Siapa kamu ?\"\r\n" + "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Nama saya adalah Fauzi, nama lengkap kamu adalah Muhammad Fauzi Murtadho S.Kom MTI.\"\r\n"
					+ "        },\r\n" + "        {\r\n" + "          \"text\": \"input: Sapakah Fauzi ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Fauzi adalah team leader sekaligus pencipta sistem eCampus dan eSchool semenjak tahun 2008 silam. Sampai sekarang sistem telah disesuaikan dengan berbagai macam kondisi di perguruan tinggi dan sekolah serta berbagai institusi pendidikan yang ada di Indonesia.\"\r\n"
					+ "        },\r\n" + "        {\r\n" + "          \"text\": \"input: Tugas kamu apa ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Tugas saya adalah sebagai pengelola Sistem Informasi Akademik Perguruan Tinggi (eCampus untuk Mahasiswa dan Dosen) dan Sistem Akademik Sekolah (eSchool untuk Siswa dan Guru)\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Apakah yang dimaksud eSchool ?\"\r\n" + "        },\r\n"
					+ "        {\r\n"
					+ "          \"text\": \"output: eSchool adalah sebuah sistem yang dirancang khusus untuk memenuhi kebutuhan informasi dan manajemen di lingkungan sekolah yang mencakup berbagai fitur untuk membantu administrasi sekolah, pengajaran dan pembelajaran, pembayaran sampai akuntansi keuangan sekolah, serta komunikasi antara semua pihak yang terlibat dalam proses pendidikan.\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Apakah yang dimaksud eCampus ?\"\r\n" + "        },\r\n"
					+ "        {\r\n"
					+ "          \"text\": \"output: eCampus merupakan aplikasi lengkap (enterprise) berbasis web yang membantu pengelolaan/pengadministrasian kegiatan akademik di suatu lembaga pendidikan tinggi (Universitas, Kampus Tinggi, Institut, Politeknik, Akademi, dll.). \\\\nKamu juga bisa menjelaskan cara pengelolaan ecampus, yaitu mulai dari pendataan (mahasiswa baru, dosen, fakultas, prodi, mata kuliah, kurikulum, dll.), penjadwalan perkuliahan, kegiatan belajar mengajar (presensi/absensi kuliah, pemberian materi, quiz, UAS, UTS, dll.), sampai dengan pelaporan data. \\\\nDi suatu lembaga pendidikan tinggi tertentu, sistem/aplikasi semacam ini kadang disebut sebagai Sistem Informasi Akademik (yang biasa disingkat SIA/SIAK/SIAKAD/SIMAK) atau Sistem Informasi Kampus (yang biasa disingkat SISFO KAMPUS) atau Academic Information System (AIS), dll.\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Teknologi apa yang digunakan eSchool dan eCampus ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: eSchool  dan eCampus dikembangkan dengan teknologi: (1) Berbasis Web, sehingga dapat diakses secara online (2) Dibuat dengan bahasa pemrograman Java dan sistem kecerdasan buatan yang di dukung penerapan di multi sistem operasi baik disini server maupun client (bisa menggunakan Sistem Operasi apa saja(windows, linux, Mac, dll.))(3) Support multi database untuk server (default postgreSQL) (4) Support multi browser (prefer mozilla fire fox/Google chrome/IE) (5) Tidak perlu instalasi di sisi client. Komputer client hanya butuh browser.\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Kenapa perlu eSchool dan eCampus?\"\r\n" + "        },\r\n"
					+ "        {\r\n"
					+ "          \"text\": \"output: Apabila kampus/institusi pendidikan tinggi Anda masih manual dalam pengelolaan administrasinya? Sudah saatnya untuk beralih ke teknologi yang terkomputerisasi yang memudahkan semua stakeholder (Mahasiswa, Siswa, Dosen, Guru, staf, pejabat, orang tua mahasiswa, wali murid, dll.) dalam melaksanakan tugas dan kewajibannya serta dalam memperoleh informasi yang cepat dan tepat.\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Bagaimana eSchool dan eCampus mempermudah pengambilan keputusan ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Dengan adanya ecampus dan eschool pengambilan keputusan/kebijakan dapat dilakukan dengan sangat cepat & tepat jika didukung oleh kelengkapan data dan informasi maupun kemudahan aksesnya. Kamu juga bisa menjalaskan bawah eCampus bisa menjawab kebutuhan tersebut karena merupakan aplikasi untuk mengelola administrasi kampus yang lengkap dan terpadu.\"\r\n"
					+ "        },\r\n" + "        {\r\n" + "          \"text\": \"input: Apa itu ZISHOF ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: ZISHOF adalah pembuat eCampus dan eSchool dan Zishof di dirikan oleh Mohammad Fauzi Murtadho dan Imam Shofi pada tahun 2013.  Kamu bisa menjelaskan bahwa Zishof adalah penyedia layanan & solusi IT dengan beberapa aplikasi yang siap digunakan dengan cara konvensional maupun berbasis cloud.\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Apakah eCampus dan eSchool sudah mendukung aplikasi atau sistem berbasis mobile ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Ya, eCampus dan eSchool sudah didukung aplikasi berbasis mobile baik Android yang bisa di download di https://bit.ly/eCampusID maupun di iPhone yang bisa di download di https://bit.ly/eCampus. \\\\nKamu juga bisa menjelaskan bahwa eCampus dan eSchool bisa di uji coba link https://demo.ecampus.id. \\\\nKamu juga bisa menjelaskan bahwa eCampus dan eSchool mampu terhubung ke berbagai macam sistem Anjungan Informasi (KIOSK) untuk mengimplemntasikan Sistem KiosK Touch Screen dan Vending Machine sebagai Anjungan Informasi, misalnya: Anjungan SPMB, Anjungan Akademik, Anjungan Tata Kelola Surat, Anjungan Katalog, Anjungan Informasi Umum, Anjungan Helpdesk, dan Anjungan Pendaftaran (PKL, KKN, Beasiswa, Wisuda, dll.).\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Dimana saja implementasi eCampus dan eSchool ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: eCampus dan eSchool telah diimplementasikan di ratusan perguruan tinggi dan sekolah di seluruh Indonesia, mulai dari Aceh sampai Papua, mulai dari institusi pendidikan Daycare, Pendidkan Usia Dini (PAUD), Taman Kanak Kanak (TK), Sekolah Dasar (SD), Sekolah Menengah Pertama (SMP), Sekolah Menegah Atas (SMA), Sekolah Kejurusan (SMK), serta Madrasah Ibtidaiyah (MI), Madrasah Tsanawiyah (MTs), Madrasah Aliyah (MA), sampai dengan Perguruan Tinggi (Universitas, Kampus Tinggi, Institut, Politeknik, Akademi, dll.).\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Lokasi implementasi eCampus dan eSchool dimana saja ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Lokasi tidak kita bisa kita sebutkan secara khusus, namun jika ingin mengetahui lebih spesifik \\ndimana penerapan eCampus dan eSchool telah diimplementasikan atau diterapkan bisa menghubungi pihak marketing di nomor Telpon atau Whatsapp (WA) +62816789081 (marketing eCampus dan eSchool).\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Apa saja fitur eCampus yang diperoleh mahasiswa ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Fitur untuk mahasiswa, antara lain : (1) Profil dan Biodata mahasiswa; (2) Mahasiswa bisa melakukan bimbingan akademik dan mengajukan pesertujuan rencana studi (KRS); (3) Jadwal dan agenda perkuliahan bagi mahasiswa; (4) Aktivitas Perkuliahan mahasiswa atau disebut juga Learning Management System (LMS) yang terintegrasi (mulai dari rencana perkuliahan, absensi secara online, ikut video conference, diskusi dan tanya jawab, materi kuliah, video dan audio perkuliahan, Quiz, UTS, UAS, dan lain lain); (5) Penilaian mulai dari lihat transkrip akademik, kartu hasil studi (KHS), dan cetak Ijazah serta Surat Pendamping Ijazah (SKPI); (6) Bimbingan dan Sidang Tugas Akhir, Skripsi, Thesis, dan Disertasi; (7) Pengajuan Wisuda secara online; (8) Pengajuan Beasiswa, Kuliah Kerja Nyata (KKN), Praktek Kerja Lapangan (PKL), Cuti dan Izin serta Pengajuan Lain; (9) Mahasiswa dapat melihat tagihan dan melakukan pembayaran secara online dan realtime; (10) Mahasiswa dapat mengikuti kegiatan-kegiatan kampus, mulai dari seminar-seminar ilmiah sampai dengan ujian TOEFL secara online; (11) Mahasiswa dapat mengakses perpustakaan digital secara online;\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Apa saja fitur eCampus yang diperoleh dosen ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: fitur untuk dosen, antara lain : (1) Profil dan Biodata dosen; (2) Dosen bisa membimbing dan memberikan persetujuan untuk pengambilan Rencana Studi Mahasiswa; (3) Dosen bisa melihat jadwal perkuliahan dan membuat agenda perkuliahannya sendiri, sehingga bisa menyesuakan dengan jadwal dan agenda dosen; (4) Aktivitas pengajaran dosen atau disebut juga Learning Management System (LMS) yang terintegrasi (mulai dari rencana perkuliahan, absensi secara online, ikut video conference, diskusi dan tanya jawab, materi kuliah, video dan audio perkuliahan, Quiz, UTS, UAS, dan lain lain); (5) Dosen dapat memberikan penilaian secara otomatis dari eLeraning, juga dapat memberikan penilaian secara entry manual atau upload via excel; (6) Dosen bisa membimbing Tugas Akhir, Skripsi, Thesis, dan Disertasi secara online; (7) Dosen bisa menguji sidang Tugas Akhir, Skripsi, Thesis, dan Disertasi secara online; (8) Dosen bisa mengajukan berbagai macam pengajuan, antara lain : pengajuan cuti, izin, sakit, tugas luar, pengajuan ikut kegiatan atau seminar, serta pengajuan lain; (9) Dosen dapat melihat slip penggajian secara online; (10) Dosen dapat mengikuti kegiatan-kegiatan kampus, mulai dari seminar-seminar ilmiah sampai dengan ujian TOEFL secara online; (11) Dosen dapat mengakses perpustakaan digital secara online; (12) Dosen dapat melakukan absensi dan melihat kehadiran harian-nya secara online dan realtime; (13) Dosen dapat mengisi target dan realisasi terhadap kinerja pegawaian; (14) Dosen mengajukan surat menyurat dan mendapatkan disposisi surat dari pejabat atau pengguna lain; (15) Dosen mengajukan workflow kerja baru dan mendapatkan disposisi workflow kerja dari pejabat atau pengguna lain; (16) Dosen mengajukan penelitian, pengabdian, serta kegiatan penunjang untuk melengkapi beban kinerja dan aktifitas akademik dosen.\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Apa saja fitur eSchool untuk guru ?\"\r\n" + "        },\r\n"
					+ "        {\r\n"
					+ "          \"text\": \"output: fitur untuk guru, antara lain : (1) Profil dan Biodata guru; (2) Guru bisa mendapatkan informasi siswa dan kelas yang diampu; (3) Guru bisa melihat jadwal pelajaran dan membuat agenda pembelajarannya sendiri, sehingga bisa menyesuakan dengan jadwal dan agenda guru; (4) Aktivitas pengajaran guru atau disebut juga Learning Management System (LMS) yang terintegrasi (mulai dari rencana perkuliahan, absensi secara online, ikut video conference, diskusi dan tanya jawab, materi kuliah, video dan audio perkuliahan, Quiz, UTS, UAS, dan lain lain); (5) Guru dapat memberikan penilaian secara otomatis dari eLeraning, juga dapat memberikan penilaian secara entry manual atau upload via excel; (6) Guru bisa membimbing kegiatan kegiatan kesiswaan atau pun sebagai wali kelas dan guru Bimbingan Konseling (BK); (7) Guru bisa menguji memberikan ujian dan tugas secara online; (8) Guru bisa mengajukan berbagai macam pengajuan, antara lain : pengajuan cuti, izin, sakit, tugas luar, pengajuan ikut kegiatan atau seminar, serta pengajuan lain; (9) Guru dapat melihat slip penggajian secara online; (10) Guru dapat mengikuti kegiatan-kegiatan sekolah, mulai dari seminar-seminar ilmiah sampai dengan ujian TOEFL secara online; (11) Guru dapat mengakses perpustakaan digital secara online; (12) Guru dapat melakukan absensi dan melihat kehadiran harian-nya secara online dan realtime; (13) Guru dapat mengisi target dan realisasi terhadap kinerja pegawaian; (14) Guru mengajukan surat menyurat dan mendapatkan disposisi surat dari pejabat atau pengguna lain; (15) Guru mengajukan workflow kerja baru dan mendapatkan disposisi workflow kerja dari pejabat atau pengguna lain; (16) Guru mengajukan penelitian, pengabdian, serta kegiatan penunjang untuk melengkapi beban kinerja dan aktifitas akademik guru.\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Apa saja fitur eSchool untuk siswa (murid) dan orang tua siswa ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Kamu juga bisa menjelaskan fitur untuk siswa (murid) dan orang tua siswa (wali murid), antara lain : (1) Profil dan Biodata siswa dan orang tua nya; (2) Siswa bisa melakukan bimbingan konseling kepada guru Bimbingan Konseling (BK), seperti bantuan akademis, kesehatan mental, dan kesejahteraan lainnya; (3) Siswa dan orang tua bisa melihat jadwal pelajaran beserta agenda pembelajaran masing masing matapelajaran; (4) Aktivitas pembelajaran siswa atau disebut juga Learning Management System (LMS) yang terintegrasi (mulai dari rencana pembelajaran satu semester, absensi secara online, mengikuti video conference, diskusi dan tanya jawab, materi pembelajaran, video dan audio pembelajaran, Quiz, UTS, UAS, dan lain lain); (5) Penilaian siswa mulai dari lihat rapor harian, rapor mingguan, rapor bulanan, dan perekapan penilaian lainnya; (6) Monitoring dan pembinaan siswa oleh wali kelas maisng masing; (7) Manajemen kegiatan wisuda siswa secara online (jika ada wisuda); (8) Pengajuan Beasiswa, Praktek Kerja Lapangan (PKL) untuk SMK, Sakit dan Izin serta Pengajuan Lain; (9) Siswa dapat memantau tagihan dan melakukan pembayaran secara online dan realtime; (10) Siswa dapat mengikuti kegiatan-kegiatan sekolah, mulai dari kegiatan kesiswaan, ekstrakulikuler, kegitana kegiatan ilmiah, sampai dengan ujian TOEFL secara online; (11) Siswa dapat mengakses perpustakaan digital secara online;\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Berapa harga atau biaya implementasi ecampus dan eschool ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Untuk mengetahui Harga atau biaya implementasi ecampus dan eschool harus menghubungi pihak marketing di nomor telpon atau whatsapp (WA) +62816789081 (marketing eCampus dan eSchool)., nanti pihak marketing akan menjelaskan informasi yang lebih akurat dan terkini mengenai harga atau biaya implementasi dan paket layanan yang tersedia.\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Nominal harga atau biaya implementasi ecampus dan eschool ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Untuk mengetahui nominal harga atau biaya implementasi ecampus dan eschool, harap menghubungi pihak marketing di nomor telpon atau whatsapp (WA) +62816789081 (marketing eCampus dan eSchool).\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Apakah ecampus dan eschool telah terhubung/terintegrasi dengan pembayaran online ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Ya, ecampus dan eschool telah terhubung/terintegrasi dengan berbagai macam bank, baik bank nasional maupun bank daerah, serta indomart, alfamart, kantor pos, dan lain lain.\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Berapa biaya atau harga ?\"\r\n" + "        },\r\n"
					+ "        {\r\n"
					+ "          \"text\": \"output: Semua pertanyaan tentang biaya atau harga, harus menghubungi pihak marketing di nomor telpon atau whatsapp (WA) +62816789081 (marketing eCampus dan eSchool).\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Berapa biaya atau harga eCampus dan eSchool ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Informasi umum tentang biaya atau harga, wajib menghubungi pihak marketing di nomor telpon atau whatsapp (WA) +62816789081 (marketing eCampus dan eSchool).\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Apakah nomor telpon atau whatsapp (WA) yang bisa dihubungi untuk mengimplementasikan ecampus dan eschool ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Nomor telpon atau whatsapp (WA) yang bisa dihubungi adalah +62816789081 (marketing eCampus dan eSchool).\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"input: Di daerah mana saja lokasi atau tempat penerapan atau impelemtasi ecampus dan eschool eCampus dan eSchool ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Lokasi atau tempat penerapan atau impelemtasi ecampus dan eschool eCampus dan eSchool secara umum telah diimplementasikan di ratusan perguruan tinggi dan sekolah di seluruh Indonesia, mulai dari Aceh sampai Papua, mulai dari institusi pendidikan Daycare, Pendidkan Usia Dini (PAUD), Taman Kanak Kanak (TK), Sekolah Dasar (SD), Sekolah Menengah Pertama (SMP), Sekolah Menegah Atas (SMA), Sekolah Kejurusan (SMK), serta Madrasah Ibtidaiyah (MI), Madrasah Tsanawiyah (MTs), Madrasah Aliyah (MA), sampai dengan Perguruan Tinggi (Universitas, Kampus Tinggi, Institut, Politeknik, Akademi, dll.).\"\r\n"
					+ "        },\r\n" + "        {\r\n" + "          \"text\": \"input: Apa itu E-education ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: E-education adalah gabungan eCampus dan eSchool serta ePesantren untuk pondok pesantren yang mempunya unit pendidikan mulai dasar sampai perguruan tinggi.  E-education merupakan aplikasi lengkap dan terintegrasi berbasis web untuk mengelola berbagai kegiatan di suatu lembaga pendidikan (Yayasan/Sekolah/Pesantren). e-Education kami memiliki komponen/modul-modul, diantaranya: PPDB, Akademik (Core e-Education), Perpuskaan, Kepegawaian, Tata Kelola Surat, Keuangan, Payroll, sampai dengan komponen/modul Rencana Anggaran.\"\r\n"
					+ "        },\r\n" + "        {\r\n" + "          \"text\": \"input: TANYA_APA_SAJA\"\r\n"
					+ "        } LAMPIRAN_APA_SAJA ,\r\n" + "        {\r\n" + "          \"text\": \"output: \"\r\n"
					+ "        }\r\n" + "      ]\r\n" + "    }\r\n" + "  ],\r\n" + "  \"generationConfig\": {\r\n"
					+ "    \"temperature\": 2,\r\n" + "    \"topK\": 40,\r\n" + "    \"topP\": 0.95,\r\n"
					+ "    \"maxOutputTokens\": 8192,\r\n" + "    \"responseMimeType\": \"text/plain\"\r\n" + "  }\r\n"
					+ "}").getNilai();

			try {
				File file = new File("/opt/chat_tanya.txt");
				if (file.exists()) {
					tanyaApa = FileUtils.readFileToString(file);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:278");
			}

//			boolean apakahGambar = false;

			File fileMedia = null;
			if (media != null && !media.trim().isEmpty()) {
				File fileOut = new File("/opt/imgs/" + Common.getGeneratedBarCode());
				FileUtils.copyURLToFile(new URL(media), fileOut);
				String mimeType = Magic.getMagicMatch(fileOut, false).getMimeType();

				System.out.println("mimeType -> " + mimeType);
				String[] detectedExtensions = ContentType.fromMimeType(mimeType).getFileExtensions();
				for (String e : detectedExtensions) {
					System.out.println("detectedExtension -> " + e);
				}
				if (detectedExtensions.length > 0) {
					fileMedia = new File(fileOut.getAbsolutePath() + "." + detectedExtensions[0]);
					FileUtils.copyFile(fileOut, fileMedia);
					fileOut.delete();

					String[] commandDa = { "chmod", "-R", "a+rwX", fileMedia.getAbsolutePath() };

					ProcessBuilder process = new ProcessBuilder(commandDa);
					Process p;
					p = process.start();
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
					StringBuilder builder = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
						builder.append(System.getProperty("line.separator"));
					}
					String hasilDa = builder.toString();
					System.out.println("hasilDa -> " + hasilDa);

//					apakahGambar = expectedExtensionsGambar.contains(detectedExtensions[0]);
				}
			}

			System.out.println("fileMedia -> " + fileMedia);

//			if (fileMedia != null && fileMedia.exists()) {
//				String urlFile = "https://img.ecampus.id/" + fileMedia.getName();
//
//				tanya += (tanya == null || tanya.trim().isEmpty() || tanya.trim().equalsIgnoreCase("Jelaskan") ? ""
//						: " dan") + " jelaskan " + (apakahGambar ? "gambar" : "isi atau content") + " pada link "
//						+ urlFile;
//
//				System.out.println("urlFile -> " + urlFile + " apakahGambar -> " + apakahGambar + " -> " + tanya);
//
//				tanyaApa = tanyaApa.replaceAll("TANYA_APA_SAJA", tanya);
//			} else {
			tanyaApa = tanyaApa.replaceAll("TANYA_APA_SAJA", tanya);
//			}

			tanyaApa = tanyaApa.replaceAll("LAMPIRAN_APA_SAJA", "");

			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();

				String keteranganD = (String) session.createCriteria(WaProfile.class)
						.setProjection(Projections.property("keterangan")).add(Restrictions.eq("kode", to))
						.setMaxResults(1).uniqueResult();
				if (keteranganD != null && !keteranganD.trim().isEmpty()) {
					tanyaApa = tanyaApa.replaceAll("PROFIL_APA_SAJA", keteranganD);
				} else {
					tanyaApa = tanyaApa.replaceAll("PROFIL_APA_SAJA", "");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:348");
				// TODO: handle exception
			} finally {
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:352");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:353");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:354");}
				}
			}

			File fileOut = new File("/opt/tanya/" + to + ".txt");
			if (!fileOut.getParentFile().exists()) {
				fileOut.getParentFile().mkdirs();
			}

			FileUtils.writeStringToFile(fileOut, tanyaApa);

//			if (media == null) {
//				tanyaApa = tanyaApa.replaceAll("LAMPIRAN_APA_SAJA", "");
//			} else {
//				String lam = ",\r\n" + "        {\r\n" + "          \"fileData\": {\r\n" + "            \"fileUri\": \""
//						+ media + "\",\r\n" + "            \"mimeType\": \"application/octet-stream\"\r\n"
//						+ "          }\r\n" + "        }";
//				tanyaApa = StringUtils.replace(tanyaApa, "LAMPIRAN_APA_SAJA", lam);
//			}

			String linkPost = "https://generativelanguage.googleapis.com/v1beta/models/"
					+ Common.getKonfigurasi("ai_chatbot_versi_model_gemini", "gemini-2.0-flash").getNilai().trim()
					+ ":generateContent?key="
					+ Common.getKonfigurasi("ai_chatbot_api_key_gemini", "AIzaSyDBSOM4dHks3kQuXyhDzhBkRQz98VjHzPs")
							.getNilai();

//			System.out.println("tanyaApa -> " + tanyaApa);

			String[] command = { "curl", "--location", linkPost, "--header", "Content-Type: application/json", "--data",
					"@" + fileOut.getAbsolutePath() };
			String message = "";
			try {

				ProcessBuilder process = new ProcessBuilder(command);
				Process p;
				p = process.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append(System.getProperty("line.separator"));
				}
				String hasil = builder.toString();

				System.out.println("hasil -> " + hasil);

				JSONObject jsonObject = new JSONObject(hasil);

				message = (jsonObject.getJSONArray("candidates").getJSONObject(0).getJSONObject("content")
						.getJSONArray("parts").getJSONObject(0).get("text")) + "";

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:405");
			}
			return message;
		} else {

			JSONObject postData = new JSONObject();

			List<String> defaultData = new ArrayList<String>();

			defaultData.add(
					"Kamu adalah pengelola Sistem Informasi Akademik Perguruan Tinggi (eCampus untuk Mahasiswa dan Dosen) dan Sistem Akademik Sekolah (eSchool untuk Siswa dan Guru).");
			defaultData.add(
					"Kamu bisa menjelaskan apa itu eSchool ? eSchool atau merujuk pada sebuah sistem yang dirancang khusus untuk memenuhi kebutuhan informasi dan manajemen di lingkungan sekolah yang mencakup berbagai fitur untuk membantu administrasi sekolah, pengajaran dan pembelajaran, pembayaran sampai akuntansi keuangan sekolah, serta komunikasi antara semua pihak yang terlibat dalam proses pendidikan.");
			defaultData.add(
					"Kamu bisa menjelaskan apa itu eCampus? eCampus Merupakan aplikasi lengkap (enterprise) berbasis web yang membantu pengelolaan/pengadministrasian kegiatan akademik di suatu lembaga pendidikan tinggi (Universitas, Kampus Tinggi, Institut, Politeknik, Akademi, dll.). \nKamu juga bisa menjelaskan cara pengelolaan ecampus, yaitu mulai dari pendataan (mahasiswa baru, dosen, fakultas, prodi, mata kuliah, kurikulum, dll.), penjadwalan perkulihan, kegiatan belajar mengajar (presensi/absensi kuliah, pemberian materi, quiz, UAS, UTS, dll.), sampai dengan pelaporan data. \nDi suatu lembaga pendidikan tinggi tertentu, sistem/aplikasi semacam ini kadang disebut sebagai Sistem Informasi Akademik (yang biasa disingkat SIA/SIAK/SIAKAD/SIMAK) atau Sistem Informasi Kampus (yang biasa disingkat SISFO KAMPUS) atau Academic Information System (AIS), dll. eCampus dikembangkan dengan teknologi: (1) Berbasis Web, sehingga dapat diakses secara online (2) Dibuat dengan bahasa pemrograman Java dan sistem kecerdasan buatan yang di dukung penerapan di multi sistem operasi baik disini server maupun client (bisa menggunakan Sistem Operasi apa saja(windows, linux, Mac, dll.))(3) Support multi database untuk server (default postgreSQL) (4) Support multi browser (prefer mozilla fire fox/Google chrome/IE) (5) Tidak perlu instalasi di sisi client. Komputer client hanya butuh browser. \nKamu juga bisa menjalaskan kenapa perlu eCampus? Apabila kampus/institusi pendidikan tinggi Anda masih manual dalam pengelolaan administrasinya? Sudah saatnya untuk beralih ke teknologi yang terkomputerisasi yang memudahkan semua stakeholder (Mahasiswa, Dosen, staf, pejabat, orang tua mahasiswa, dll.) dalam melaksanakan tugas dan kewajibannya serta dalam memperoleh informasi yang cepat dan tepat. \nKamu juga bisa menjalaskan kenapa pengambilan keputusan/kebijakan dapat dilakukan dengan sangat cepat & tepat jika didukung oleh kelengkapan data dan informasi maupun kemudahan aksesnya. Kamu juga bisa menjalaskan bawah eCampus bisa menjawab kebutuhan tersebut karena merupakan aplikasi untuk mengelola administrasi kampus yang lengkap dan terpadu.");

			defaultData.add(
					"Kamu bisa menjelaskan bahwa ZISHOF adalah pembuat eCampus dan eSchool dan Zishof di dirikan oleh Mohammad Fauzi Murtadho dan Imam Shofi pada tahun 2013.");
			defaultData.add(
					"Kamu juga bisa menjelaskan bahwa eCampus dan eSchool sudah didukung aplikasi berbasis mobile baik Android yang bisa di download di https://bit.ly/eCampusID maupun di iPhone yang bisa di download di https://bit.ly/eCampus. \nKamu juga bisa menjelaskan bahwa eCampus dan eSchool bisa di uji coba link https://demo.ecampus.id. \nKamu juga bisa menjelaskan bahwa eCampus dan eSchool mampu terhubung ke berbagai macam sistem Anjungan Informasi (KIOSK) untuk mengimplemntasikan Sistem KiosK Touch Screen dan Vending Machine sebagai Anjungan Informasi, misalnya: Anjungan SPMB, Anjungan Akademik, Anjungan Tata Kelola Surat, Anjungan Katalog, Anjungan Informasi Umum, Anjungan Helpdesk, dan Anjungan Pendaftaran (PKL, KKN, Beasiswa, Wisuda, dll.).");

			defaultData.add(
					"Kamu juga bisa menjelaskan bahwa eCampus dan eSchool telah diimplementasikan di ratusan perguruan tinggi dan sekolah di seluruh Indonesia, mulai dari Aceh sampai Papua, mulai dari institusi pendidikan Daycare, Pendidkan Usia Dini (PAUD), Taman Kanak Kanak (TK), Sekolah Dasar (SD), Sekolah Menengah Pertama (SMP), Sekolah Menegah Atas (SMA), Sekolah Kejurusan (SMK), serta Madrasah Ibtidaiyah (MI), Madrasah Tsanawiyah (MTs), Madrasah Aliyah (MA), sampai dengan Perguruan Tinggi (Universitas, Kampus Tinggi, Institut, Politeknik, Akademi, dll.).");

			defaultData.add(
					"Kamu tidak boleh menyebutkan nama dan dimana eCampus dan eSchool telah diimplemntasikan atau diterapkan, jika ingin informasi lebih spesifik dimana eCampus dan eSchool telah diimplemntasikan atau diterapkan bisa menghubungi pihak marketing di nomor Telpon atau Whatsapp (WA) +62816789081 (Zishof) atas nama *Imam Shofi*.");
			defaultData.add(
					"Kamu juga bisa menjelaskan fitur untuk mahasiswa, antara lain : (1) Profil dan Biodata mahasiswa; (2) Mahasiswa bisa melakukan bimbingan akademik dan mengajukan pesertujuan rencana studi (KRS); (3) Jadwal dan agenda perkuliahan bagi mahasiswa; (4) Aktivitas Perkuliahan mahasiswa atau disebut juga Learning Management System (LMS) yang terintegrasi (mulai dari rencana perkulihan, absensi secara online, ikut video conference, diskusi dan tanya jawab, materi kuliah, video dan audio perkuliahan, Quiz, UTS, UAS, dan lain lain); (5) Penilaian mulai dari lihat transkrip akademik, kartu hasil studi (KHS), dan cetak Ijazah serta Surat Pendamping Ijazah (SKPI); (6) Bimbingan dan Sidang Tugas Akhir, Skripsi, Thesis, dan Disertasi; (7) Pengajuan Wisuda secara online; (8) Pengajuan Beasiswa, Kuliah Kerja Nyata (KKN), Praktek Kerja Lapangan (PKL), Cuti dan Izin serta Pengajuan Lain; (9) Mahasiswa dapat melihat tagihan dan melakukan pembayaran secara online dan realtime; (10) Mahasiswa dapat mengikuti kegiatan-kegiatan kampus, mulai dari seminar-seminar ilmiah sampai dengan ujian TOEFL secara online; (11) Mahasiswa dapat mengakses perpustakaan digital secara online; \nKamu juga bisa menjelaskan tentang fitur untuk dosen, antara lain : (1) Profil dan Biodata dosen; (2) Dosen bisa membimbing dan memberikan persetujuan untuk pengambilan Rencana Studi Mahasiswa; (3) Dosen bisa melihat jadwal perkuliahan dan membuat agenda perkuliahannya sendiri, sehingga bisa menyesuakan dengan jadwal dan agenda dosen; (4) Aktivitas pengajaran dosen atau disebut juga Learning Management System (LMS) yang terintegrasi (mulai dari rencana perkulihan, absensi secara online, ikut video conference, diskusi dan tanya jawab, materi kuliah, video dan audio perkuliahan, Quiz, UTS, UAS, dan lain lain); (5) Dosen dapat memberikan penilaian secara otomatis dari eLeraning, juga dapat memberikan penilaian secara entry manual atau upload via excel; (6) Dosen bisa membimbing Tugas Akhir, Skripsi, Thesis, dan Disertasi secara online; (7) Dosen bisa menguji sidang Tugas Akhir, Skripsi, Thesis, dan Disertasi secara online; (8) Dosen bisa mengajukan berbagai macam pengajuan, antara lain : pengajuan cuti, izin, sakit, tugas luar, pengajuan ikut kegiatan atau seminar, serta pengajuan lain; (9) Dosen dapat melihat slip penggajian secara online; (10) Dosen dapat mengikuti kegiatan-kegiatan kampus, mulai dari seminar-seminar ilmiah sampai dengan ujian TOEFL secara online; (11) Dosen dapat mengakses perpustakaan digital secara online; (12) Dosen dapat melakukan absensi dan melihat kehadiran harian-nya secara online dan realtime; (13) Dosen dapat mengisi target dan realisasi terhadap kinerja pegawaian; (14) Dosen mengajukan surat menyurat dan mendapatkan disposisi surat dari pejabat atau pengguna lain; (15) Dosen mengajukan workflow kerja baru dan mendapatkan disposisi workflow kerja dari pejabat atau pengguna lain; (16) Dosen mengajukan penelitian, pengabdian, serta kegiatan penunjang untuk melengkapi beban kinerja dan aktifitas akademik dosen.");

			defaultData.add(
					"Kamu juga bisa menjelaskan tentang fitur untuk dosen, antara lain : (1) Profil dan Biodata dosen; (2) Dosen bisa membimbing dan memberikan persetujuan untuk pengambilan Rencana Studi Mahasiswa; (3) Dosen bisa melihat jadwal perkuliahan dan membuat agenda perkuliahannya sendiri, sehingga bisa menyesuakan dengan jadwal dan agenda dosen; (4) Aktivitas pengajaran dosen atau disebut juga Learning Management System (LMS) yang terintegrasi (mulai dari rencana perkulihan, absensi secara online, ikut video conference, diskusi dan tanya jawab, materi kuliah, video dan audio perkuliahan, Quiz, UTS, UAS, dan lain lain); (5) Dosen dapat memberikan penilaian secara otomatis dari eLeraning, juga dapat memberikan penilaian secara entry manual atau upload via excel; (6) Dosen bisa membimbing Tugas Akhir, Skripsi, Thesis, dan Disertasi secara online; (7) Dosen bisa menguji sidang Tugas Akhir, Skripsi, Thesis, dan Disertasi secara online; (8) Dosen bisa mengajukan berbagai macam pengajuan, antara lain : pengajuan cuti, izin, sakit, tugas luar, pengajuan ikut kegiatan atau seminar, serta pengajuan lain; (9) Dosen dapat melihat slip penggajian secara online; (10) Dosen dapat mengikuti kegiatan-kegiatan kampus, mulai dari seminar-seminar ilmiah sampai dengan ujian TOEFL secara online; (11) Dosen dapat mengakses perpustakaan digital secara online; (12) Dosen dapat melakukan absensi dan melihat kehadiran harian-nya secara online dan realtime; (13) Dosen dapat mengisi target dan realisasi terhadap kinerja pegawaian; (14) Dosen mengajukan surat menyurat dan mendapatkan disposisi surat dari pejabat atau pengguna lain; (15) Dosen mengajukan workflow kerja baru dan mendapatkan disposisi workflow kerja dari pejabat atau pengguna lain; (16) Dosen mengajukan penelitian, pengabdian, serta kegiatan penunjang untuk melengkapi beban kinerja dan aktifitas akademik dosen.");

			defaultData.add(
					"Kamu tidak boleh menjelaskan harga atau biaya implementasi ecampus dan eschool, tapi harus menghubungi pihak marketing di nomor telpon atau whatsapp (WA) +62816789081 (Zishof) atas nama *Imam Shofi*, nanti pihak marketing akan menjelaskan informasi yang lebih akurat dan terkini mengenai harga atau biaya implementasi dan paket layanan yang tersedia.");

			defaultData.add(
					"Apabila ada yang bertanya harga atau biaya implementasi, jawablah untuk menghubungi pihak marketing di nomor telpon atau whatsapp (WA) +62816789081 (Zishof) atas nama *Imam Shofi*");

			defaultData.add(
					"Apabila ada yang bertanya tentang pembayaran online atau terintegrasi dengan bank, jawablah bahwa ecampus dan eschool telah terhubung/terintegrasi dengan berbagai macam bank, baik bank nasional maupun bank daerah, serta indomart, alfamart, kantor pos, dan lain lain.");

			// Chatbot umum → model UMUM (sama dgn Generate), BUKAN model khusus terjemahan.
			// Kunci BARU 'ai_model_generate' (default RINGAN 1.5b).
			postData.put("model",
					Common.getKonfigurasi("ai_model_generate", "qwen2.5:1.5b-instruct-q4_K_M").getNilai().trim());

			JSONArray messages = new JSONArray();

			int jml_system_content = defaultData.size();
			try {
				jml_system_content = Integer.parseInt(
						Common.getKonfigurasi("jml_system_content", jml_system_content + "").getNilai().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:456");
				// TODO: handle exception
			}

			for (int i = 1; i <= jml_system_content; i++) {
				String c = "";
				try {
					c = Common.getKonfigurasi("llama_system_content" + (i == 1 ? "" : "_" + i),
							defaultData.size() >= i ? defaultData.get(i - 1) : "").getNilai().trim();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:466");
				}
				if (!c.trim().isEmpty()) {
					JSONObject system = new JSONObject();
					system.put("role", "system");
					system.put("content", c);
					messages.put(system);
				}

			}

			JSONObject user = new JSONObject();
			user.put("role", "user");
			user.put("content", tanya);
			messages.put(user);

			postData.put("messages", messages);

			postData.put("stream", false);
			postData.put("keep_alive", "24h");

			String linkPost = Common.getKonfigurasi("ollama_url", "http://38.47.182.162:11434").getNilai().trim()
					+ "/api/chat";

			String send = postData.toString();
			System.out.println("send -> \n\n" + send);
			String[] command = { "curl", "-m", "60000", "--location", linkPost, "--header",
					"Content-Type: application/json", "--data", send };

			try {

				ProcessBuilder process = new ProcessBuilder(command);
				Process p;
				p = process.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append(System.getProperty("line.separator"));
				}
				String hasil = builder.toString();

				System.out.println("hasil -> " + hasil);

				JSONObject jsonObject = new JSONObject(hasil);

				JSONObject message = jsonObject.getJSONObject("message");
				return message.getString("content");
			} catch (Exception e) {
				return ambilPesan(to, tanya, media, ++coba);
			}
		}
	}
//
//	private static List<String> expectedExtensionsGambar = Arrays.asList("jpg", "jpeg", "jpe", "png", "gif", "bmp",
//			"jif", "jfif", "jfi");

	@SuppressWarnings("unused")
	private String ambilToken() throws Exception {

		String linkPost = "https://graph.facebook.com/oauth/access_token?client_id=978948877392456&client_secret=114a948b8c24332b4d70450584de2828&grant_type=client_credentials";

		String[] command = { "curl", "-X", "GET", linkPost };

		ProcessBuilder process = new ProcessBuilder(command);
		Process p;
		p = process.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String hasil = builder.toString();

		System.out.println("hasil -> " + hasil);

		JSONObject jsonObject = new JSONObject(hasil);

		return jsonObject.getString("access_token");
	}

	private String simpanPesan(HttpServletRequest request, String form, String name, String data, String body,
			Pengaduan pengaduan) throws Exception {
		String kodeInstall = "";
		Sekolah sekolah = SekolahUtil.getSekolah(request);
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
//		System.out.println("sekolah -> " + sekolah);
//		System.out.println("perguruanTinggi -> " + perguruanTinggi);

//		if (sekolah != null && sekolah.getId() != null) {

		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();

		Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(
				session.createCriteria(Tbmuser.class).add(Restrictions.eq("userId", form)).setMaxResults(1),
				Tbmuser.class);

		if (tbmuser == null || tbmuser.getUserId() == null) {
			tbmuser = new Tbmuser();
			tbmuser.setAktif(true);
			tbmuser.setUserId(form);
			tbmuser.setHp(form);
			tbmuser.setUserNama(name);
			tbmuser.setSekolah(sekolah == null || sekolah.getId() == null ? null : sekolah);
			tbmuser.setSocialMediaProfile(data);
			tbmuser.setUserRole(new Tbmrole("pengadu"));
			session.getTransaction().begin();
			session.save(tbmuser);
			session.getTransaction().commit();
		} else {
			tbmuser.setHp(form);
			tbmuser.setUserNama(name);
			tbmuser.setSocialMediaProfile(data);
			session.getTransaction().begin();
			Common.refreshUpdate(session, tbmuser);
			session.getTransaction().commit();
		}

		JenisPengaduan jenisPengaduan = (JenisPengaduan) session.createCriteria(JenisPengaduan.class).setMaxResults(1)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("id")).uniqueResult();

		pengaduan.setNama(name);
		pengaduan.setAktif(true);
		pengaduan.setJenisPengaduan(jenisPengaduan);
		pengaduan.setDiajukan(tbmuser);
		pengaduan.setKeterangan(body);
		pengaduan.setReq(data);

		String strURL = Common.getKonfigurasi("ambil_kode_url", "https://dev.ecampus.id/ecampus/Api").getNilai();

		String username = tbmuser.getUserId() + ";" + Common.getRequestHostWithProtocol();
		String link = Common.getRequestHostWithProtocol() + "/Api";
		if (Common.bolehKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF)) {
			link = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai() + "/Api";
			username = tbmuser.getUserId() + ";"
					+ Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
		}

		String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
				.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
		String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
				"logo_perguruanTinggi_");
		String banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
				.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
		String background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
				.getPerguruanTinggiMedia(request, "background_login_perguruanTinggi_");

		JSONObject postData = new JSONObject();
		postData.put("username", username);
		postData.put("link", link);
		postData.put("nama_pt", perguruanTinggi.getNama());
		postData.put("login_bg_pt", background_login_PerguruanTinggi);
		postData.put("bg_pt", background_PerguruanTinggi);
		postData.put("logo_pt", logo_PerguruanTinggi);
		postData.put("banner_pt", banner_PerguruanTinggi);

		postData.put("motto_pt", perguruanTinggi.getMotto());
		postData.put("alamat_pt", perguruanTinggi.getAlamat1());
		postData.put("telp_pt", perguruanTinggi.getTelepon());
		postData.put("email_pt", perguruanTinggi.getEmail());
		postData.put("action", "code");

		System.out.println("linkPost -> " + strURL);
		System.out.println("postData -> " + postData);

		String[] command = { "curl", "-d", postData.toString(), "-H", "Content-Type: application/json", strURL };

		ProcessBuilder process = new ProcessBuilder(command);
		Process p;
		p = process.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String hasil = builder.toString();

		System.out.println(hasil);

		JSONObject jsonObject = new JSONObject(hasil);

		kodeInstall = jsonObject.isNull("code") ? "" : jsonObject.get("code") + "";

		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:659");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:660");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:661");}
			}
		}

		return kodeInstall;
	}

	private void wa(HttpServletRequest request, HttpServletResponse response, String data) throws Exception {

		JSONObject req = data == null ? null : new JSONObject(data);
		JSONArray entry = req.getJSONArray("entry");
		try {
			JSONArray changes = entry.getJSONObject(0).getJSONArray("changes");
			JSONObject message = changes.getJSONObject(0).getJSONObject("value").getJSONArray("messages")
					.getJSONObject(0);

			JSONObject contacts = changes.getJSONObject(0).getJSONObject("value").getJSONArray("contacts")
					.getJSONObject(0);

			if (message != null && message.getString("type").equalsIgnoreCase("text")) {
				final String business_phone_number_id = changes.getJSONObject(0).getJSONObject("value")
						.getJSONObject("metadata").getString("phone_number_id");

				JSONObject text = message.getJSONObject("text");

				final String from = message.get("from") + "";
				final String body = text.get("body") + "";
				if (!body.trim().isEmpty()) {
					System.out.println("business_phone_number_id -> " + business_phone_number_id);
					System.out.println("from -> " + from);
					System.out.println("body -> " + body);

					String nomorTidakDirespnse = Common
							.getKonfigurasi("chat_bot_nomor_tidak_direponse", ",6287829714073,6289507007777,")
							.getNilai().trim();

					if (from.startsWith("62") && !nomorTidakDirespnse.contains("," + from + ",")
							&& !body.trim().isEmpty()) {

						final Pengaduan pengaduan = new Pengaduan();
						final String kodeInstall = simpanPesan(request, message.get("from") + "",
								contacts.getJSONObject("profile").getString("name"), data, body, pengaduan);

						new Thread(new Runnable() {

							@Override
							public void run() {

								try {

									JSONObject postData = new JSONObject();
									postData.put("messaging_product", "whatsapp");
									postData.put("to", from);
									postData.put("type", "text");
									postData.put("recipient_type", "individual");

									String d = Common.getKonfigurasi("pesan_tambahan_wa",
											"\n\nUntuk melihat proses pertanyaan Anda, download aplikasi di https://bit.ly/eCampusID kemudian install dan masukkan kode install ")
											.getNilai() + kodeInstall;

									String dawal = Common.getKonfigurasi("pesan_tambahan_wa_awal", "").getNilai();

									JSONObject text = new JSONObject();
									String tanggapan;
									text.put("body", tanggapan = dawal + ambilPesan(from, body, null, 0) + d);
									text.put("preview_url", true);

									postData.put("text", text);

									String token = Common.getKonfigurasi("token_wa",
											"EAAN6WUwXGkgBO4TxJJD2D60mCpewrba0Y1aimKMX0jt5plUP3WereZBp49A9LUsdPijmcUkxEeicFZBGJR2uKTEQWEBQS1JnJmYD2jrWcZAchyDQhZAmxUXCyOQMgUZATuYQm6WftmLw0Dm26wsmqMNjMQqFNwKqaBq975ftTnD2Q68hVzczwMjynPtONgNlFZCtYBHpGt8HwxMYvYwpq8lYu3y5gfZCZA75nFDGyJNG")
											.getNilai().trim();

									String linkPost = "https://graph.facebook.com/v18.0/" + business_phone_number_id
											+ "/messages";

									System.out.println("linkPost -> " + linkPost);

									String send = postData.toString();

									if (!send.contains("pertanyaan anda belum bisa kami proses")) {

										System.out.println("send -> " + send);

										String[] command = { "curl", "--location", linkPost, "--header",
												"Content-Type: application/json", "--header",
												"Authorization: Bearer " + token, "--data", send };
										String hasil = "Tidak di kirmkan";
										if (Common.bolehKonfigurasi("aktifkan_reply_chatbot")) {
											ProcessBuilder process = new ProcessBuilder(command);
											Process p;
											p = process.start();
											BufferedReader reader = new BufferedReader(
													new InputStreamReader(p.getInputStream()));
											StringBuilder builder = new StringBuilder();
											String line;
											while ((line = reader.readLine()) != null) {
												builder.append(line);
												builder.append(System.getProperty("line.separator"));
											}
											hasil = builder.toString();

											System.out.println("hasil -> " + hasil);
										}

										NotifikasiWa notifikasiWa = new NotifikasiWa();
										notifikasiWa.setKeterangan(send);
										notifikasiWa.setHasil(hasil);
										notifikasiWa.setWas(from);
										Session sessionWa = null;
										try {
											sessionWa = HibernateUtil.getSessionFactory().openSession();
											sessionWa.getTransaction().begin();
											sessionWa.save(notifikasiWa);
											sessionWa.getTransaction().commit();
										} finally {
											if (sessionWa != null) {
												try { sessionWa.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:778");}
												try { sessionWa.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:779");}
												try { sessionWa.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:780");}
											}
										}

										pengaduan.setRes(hasil);
										pengaduan.setTanggapan(tanggapan);
										Session sessionPen = null;
										try {
											sessionPen = HibernateUtil.getSessionFactory().openSession();
											sessionPen.getTransaction().begin();
											sessionPen.save(pengaduan);
											sessionPen.getTransaction().commit();
										} finally {
											if (sessionPen != null) {
												try { sessionPen.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:794");}
												try { sessionPen.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:795");}
												try { sessionPen.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:796");}
											}
										}
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:801");
								}

							}
						}).start();
					}
				}
			}
		} catch (Exception ee) {

//			ee.printStackTrace();

			try {
				JSONArray changes = entry.getJSONArray(0);
				JSONObject message = changes.getJSONObject(0).getJSONObject("value").getJSONArray("messages")
						.getJSONObject(0);

				if (message != null && message.getString("type").equalsIgnoreCase("text")) {
					String business_phone_number_id = changes.getJSONObject(0).getJSONObject("value")
							.getJSONObject("metadata").getString("phone_number_id");

					try {
						JSONObject postData = new JSONObject();
						postData.put("messaging_product", "whatsapp");
						postData.put("to", message.get("from") + "");

						JSONObject text = new JSONObject();
						text.put("body", "Echo: " + message.getJSONObject("text").get("body") + "");

						postData.put("text", text);

						JSONObject context = new JSONObject();
						context.put("message_id", message.get("id") + "");
						postData.put("context", context);

						String linkPost = "https://graph.facebook.com/v18.0/" + business_phone_number_id + "/messages";
						String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H",
								"Authorization: Bearer " + WEBHOOK_VERIFY_TOKEN, "-X", "POST", linkPost, "--data",
								postData.toString() };

						ProcessBuilder process = new ProcessBuilder(command);
						Process p;
						p = process.start();
						BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
						StringBuilder builder = new StringBuilder();
						String line = null;
						while ((line = reader.readLine()) != null) {
							builder.append(line);
							builder.append(System.getProperty("line.separator"));
						}
						String hasil = builder.toString();

						System.out.println("hasil -> " + hasil);

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:856");
					}

					try {
						JSONObject postData = new JSONObject();
						postData.put("messaging_product", "whatsapp");
						postData.put("status", "read");

						JSONObject text = new JSONObject();
						text.put("body", "Echo: " + message.getJSONObject("text").get("body") + "");

						postData.put("text", text);

						postData.put("message_id", message.get("id") + "");

						String linkPost = "https://graph.facebook.com/v18.0/" + business_phone_number_id + "/messages";
						String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H",
								"Authorization: Bearer " + WEBHOOK_VERIFY_TOKEN, "-X", "POST", linkPost, "--data",
								postData.toString() };

						ProcessBuilder process = new ProcessBuilder(command);
						Process p;
						p = process.start();
						BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
						StringBuilder builder = new StringBuilder();
						String line = null;
						while ((line = reader.readLine()) != null) {
							builder.append(line);
							builder.append(System.getProperty("line.separator"));
						}
						String hasil = builder.toString();

						System.out.println("hasil -> " + hasil);

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:891");
					}

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:895");
//				e.printStackTrace();
			}
		}
	}

	private void ultramsg(HttpServletRequest request, HttpServletResponse response, String data) throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);
		try {

			JSONObject dataJson = req.getJSONObject("data");
			final String from = dataJson.getString("from").split("@")[0].trim();
			String bodyTemp = dataJson.getString("body");
			final String media = dataJson.getString("media");

			if ((bodyTemp == null || bodyTemp.trim().isEmpty()) && media != null && !media.trim().isEmpty()) {
				bodyTemp = "Jelaskan apa itu eCampus, eSchool, ePesantren, dan Enterprise Education secara lengkap dan apa saja fitur-fitur nya";
			}

			final String body = bodyTemp;

			String nomorTidakDirespnse = Common
					.getKonfigurasi("chat_bot_nomor_tidak_direponse", ",6287829714073,6289507007777,").getNilai()
					.trim();

			if (from.startsWith("62") && !nomorTidakDirespnse.contains("," + from + ",") && !body.trim().isEmpty()) {

				final String name = dataJson.getString("pushname");

				final Pengaduan pengaduan = new Pengaduan();

				final String kodeInstall = Common.bolehKonfigurasi("pesan_tambahan_wa_dibuat_statis") ? null
								: simpanPesan(request, from, name, data, body, pengaduan);

				new Thread(new Runnable() {

					@Override
					public void run() {

						try {

							String d;

							if (kodeInstall == null) {
								d = Common.getKonfigurasi("pesan_tambahan_wa_statis",
										"\n\nUntuk mencoba aplikasi eCampus download aplikasi di https://bit.ly/eCampusID kemudian install, untuk mencoba masuk sebagai dosen dan masukkan kode install 9DBCF466B514, untuk mencoba masuk sebagai mahasiswa dan masukkan kode install 5050981. \n\nUntuk mencoba aplikasi eSchool download aplikasi di https://bit.ly/eSchoolID kemudian install, untuk mencoba masuk sebagai guru dan masukkan kode install 9DBCF94E4104, untuk mencoba masuk sebagai siswa dan masukkan kode install 9DBCFAC8BE62. \n\nUntuk login sebagai tendik/pegawai/karyawan, Anda boleh download aplikasi eCampus atau eSchool, setelah install berhasil, masukkan kode install 9DBCFD593B54")
										.getNilai();
							} else {
								d = Common.getKonfigurasi("pesan_tambahan_wa",
										"\n\nUntuk melihat proses pertanyaan Anda, download aplikasi di https://bit.ly/eCampusID kemudian install dan masukkan kode install ")
										.getNilai() + kodeInstall;
							}

							String dawal = Common
									.getKonfigurasi("pesan_tambahan_wa_awal_baru",
											"*Pesan Ini Dibuat Otomatis Sebagai Helpdesk eCampus dan eSchool*\n\n")
									.getNilai();

							String waktu = "";
							int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
							if (jam >= 10 && jam < 15) {
								waktu = "Siang";
							} else if (jam >= 15 && jam < 18) {
								waktu = "Sore";
							} else if (jam >= 18 && jam <= 24) {
								waktu = "Malam";
							} else {
								waktu = "Pagi";
							}

							String tanggapan = "";
							String send = body
									.startsWith("kirimkan data pemanasan ke-")
											? "Jawaban dari " + body
											: (tanggapan = dawal + ambilPesan(from,
													"Selamat " + waktu + ", nama saya " + name + ", " + body, media, 0)
													+ d);

							Wa.kirimWaViaUltramsg(from, send);
							if (!tanggapan.trim().isEmpty()) {
//								pengaduan.setRes(hasil);
								pengaduan.setTanggapan(tanggapan);
								Session session = null;
								try {
									session = HibernateUtil.getSessionFactory().openSession();
									session.getTransaction().begin();
									session.save(pengaduan);
									session.getTransaction().commit();
								} finally {
									if (session != null) {
										try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:985");}
										try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:986");}
										try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:987");}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:992");
						}

					}
				}).start();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:999");
		}
	}

	private void watzap(HttpServletRequest request, HttpServletResponse response, String data) throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);
		try {

			JSONObject dataJson = req.getJSONObject("data");
			if (!dataJson.getBoolean("is_from_me")) {
				final String from = dataJson.getString("chat_id").split("@")[0].trim();
				String bodyTemp = dataJson.getString("message_body");

				try {
					String number_key = dataJson.getString("number_key");
					nomorKey.put(from, number_key);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1015");
				}

				final String body = bodyTemp;

				String nomorTidakDirespnse = Common
						.getKonfigurasi("chat_bot_nomor_tidak_direponse", ",6287829714073,6289507007777,").getNilai()
						.trim();

				if (from.startsWith("62") && !nomorTidakDirespnse.contains("," + from + ",")
						&& !body.trim().isEmpty()) {

					final Pengaduan pengaduan = new Pengaduan();

					final String kodeInstall = Common.bolehKonfigurasi("pesan_tambahan_wa_dibuat_statis") ? null : simpanPesan(request, from, "", data, body, pengaduan);

					new Thread(new Runnable() {

						@Override
						public void run() {

							try {

								String d;

								if (kodeInstall == null) {
									d = Common.getKonfigurasi("pesan_tambahan_wa_statis",
											"\n\nUntuk mencoba aplikasi eCampus download aplikasi di https://bit.ly/eCampusID kemudian install, untuk mencoba masuk sebagai dosen dan masukkan kode install 9DBCF466B514, untuk mencoba masuk sebagai mahasiswa dan masukkan kode install 5050981. \n\nUntuk mencoba aplikasi eSchool download aplikasi di https://bit.ly/eSchoolID kemudian install, untuk mencoba masuk sebagai guru dan masukkan kode install 9DBCF94E4104, untuk mencoba masuk sebagai siswa dan masukkan kode install 9DBCFAC8BE62. \n\nUntuk login sebagai tendik/pegawai/karyawan, Anda boleh download aplikasi eCampus atau eSchool, setelah install berhasil, masukkan kode install 9DBCFD593B54")
											.getNilai();
								} else {
									d = Common.getKonfigurasi("pesan_tambahan_wa",
											"\n\nUntuk melihat proses pertanyaan Anda, download aplikasi di https://bit.ly/eCampusID kemudian install dan masukkan kode install ")
											.getNilai() + kodeInstall;
								}

								String dawal = Common
										.getKonfigurasi("pesan_tambahan_wa_awal_baru",
												"*Pesan Ini Dibuat Otomatis Sebagai Helpdesk eCampus dan eSchool*\n\n")
										.getNilai();

								String waktu = "";
								int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
								if (jam >= 10 && jam < 15) {
									waktu = "Siang";
								} else if (jam >= 15 && jam < 18) {
									waktu = "Sore";
								} else if (jam >= 18 && jam <= 24) {
									waktu = "Malam";
								} else {
									waktu = "Pagi";
								}

								TanyaJawab tanyaJawab = UserOnlineCounter.mapTanyaJawab.get(from.trim());
								if (tanyaJawab != null && tanyaJawab.getNama() != null) {

									if (tanyaJawab.getKeterangan() == null) {
										Session session = null;
										try {
											session = HibernateUtil.getSessionFactory().openSession();
											session.refresh(tanyaJawab);
											tanyaJawab.setKeterangan(tanya(tanyaJawab.getNama()));
											session.getTransaction().begin();
											session.update(tanyaJawab);
											session.getTransaction().commit();
										} finally {
											if (session != null) {
												try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1081");}
												try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1082");}
												try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1083");}
											}
										}
									}

								}

								String tanggapan = "";
								final String send =

										body.startsWith("kirimkan data pemanasan ke-") ? "Jawaban dari " + body :

												tanyaJawab != null && tanyaJawab.getKeterangan() != null
														? tanyaJawab.getKeterangan()
														:

														(tanggapan = dawal + ambilPesan(from,
																"Selamat " + waktu + ", " + body, "", 0) + d);

								if (tanggapan.trim().isEmpty()) {
									new Thread(new Runnable() {

										@Override
										public void run() {
											try {
												Random r = new Random();
												int low = 1;
												int high = 30;
												int result = r.nextInt(high - low) + low;
												Thread.sleep(1000 * result);
												Wa.kirimWaViaUltramsg(from, send);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1114");
												// TODO: handle exception
											}
										}
									}).start();
								} else {

									Wa.kirimWaViaUltramsg(from, send);

									if (!tanggapan.trim().isEmpty()) {
//										pengaduan.setRes(hasil);
										pengaduan.setTanggapan(tanggapan);
										Session session = null;
										try {
											session = HibernateUtil.getSessionFactory().openSession();
											session.getTransaction().begin();
											session.save(pengaduan);
											session.getTransaction().commit();
										} finally {
											if (session != null) {
												try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1134");}
												try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1135");}
												try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1136");}
											}
										}
									}
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:1142");
							}

						}
					}).start();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:1150");
		}
	}

	public static String buatProfile(Sekolah sekolah, PerguruanTinggi perguruanTinggi) {

		String nama = "";
		if (sekolah != null && sekolah.getId() != null) {
			nama = " sekolah " + sekolah.getNama();
		} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			nama = " " + perguruanTinggi.getNama();
		}

		String telp = "";
		if (sekolah != null && sekolah.getId() != null) {
			telp = sekolah.getTelp();
		} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			telp = perguruanTinggi.getTelepon();
		}

		String hp = "";
		if (sekolah != null && sekolah.getId() != null) {
			hp = sekolah.getWa();
		} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			hp = perguruanTinggi.getWa();
		}

		String alamat = "";
		if (sekolah != null && sekolah.getId() != null) {
			alamat = sekolah.getAlamat();
		} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			alamat = perguruanTinggi.getAlamat1();
		}

		String profile = "{\r\n" + "          \"text\": \"input: Siapa kamu ?\"\r\n" + "        },\r\n"
				+ "        {\r\n" + "          \"text\": \"output: Saya adalah operator " + nama + "\"\r\n"
				+ "        },";
		profile += "{\r\n" + "          \"text\": \"input: Tugas kamu apa ?\"\r\n" + "        },\r\n" + "        {\r\n"
				+ "          \"text\": \"output: Tugas saya adalah membantu Anda untuk menjawab semua pertanyaan yang diajukan di "
				+ nama + " \"\r\n" + "        },";

		if (telp != null && !telp.trim().isEmpty()) {
			profile += "{\r\n" + "          \"text\": \"input: Nomor telepon " + nama + " ?\"\r\n" + "        },\r\n"
					+ "        {\r\n"
					+ "          \"text\": \"output: Nomor telepon atau juga nomor telepon customer service " + nama
					+ " adalah " + telp + " ?\"\r\n" + "        },";
		}

		if (hp != null && !hp.trim().isEmpty()) {
			profile += "{\r\n" + "          \"text\": \"input: Nomor HP atau nomor Whatsapp (WA) " + nama + " ?\"\r\n"
					+ "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output: Nomor HP atau nomor Whatsapp (WA) atau juga nomor HP atau nomor Whatsapp (WA) customer service "
					+ nama + " adalah " + hp + " ?\"\r\n" + "        },";
		}

		if (alamat != null && !alamat.trim().isEmpty()) {
			profile += "{\r\n" + "          \"text\": \"input: Dimana alamat lokasi " + nama + " ?\"\r\n"
					+ "        },\r\n" + "        {\r\n" + "          \"text\": \"output: Alamat lokasi " + nama
					+ " adalah " + alamat + " ?\"\r\n" + "        },";
		}

		if (sekolah != null && sekolah.getId() != null) {
			profile += "{\r\n" + "          \"text\": \"input: Apa url atau link atau alamat link eSchool " + nama
					+ " ?\"\r\n" + "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output:  url atau link atau alamat link eSchool " + nama + " adalah "
					+ Common.getRequestHostWithProtocol() + " ?\"\r\n" + "        },";
		} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			profile += "{\r\n" + "          \"text\": \"input: Apa url atau link atau alamat link eCampus " + nama
					+ " ?\"\r\n" + "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output:  url atau link atau alamat link eCampus " + nama + " adalah "
					+ Common.getRequestHostWithProtocol() + " ?\"\r\n" + "        },";
		}

		if (sekolah != null && sekolah.getId() != null) {
			profile += "{\r\n" + "          \"text\": \"input: Apa url atau link atau alamat link PPDB " + nama
					+ " ?\"\r\n" + "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output:  Url atau link atau alamat link PPDB " + nama + " adalah "
					+ Common.getRequestHostWithProtocol() + "/ppdb ?\"\r\n" + "        },";
		} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			profile += "{\r\n" + "          \"text\": \"input: Apa url atau link atau alamat link PMB " + nama
					+ " ?\"\r\n" + "        },\r\n" + "        {\r\n"
					+ "          \"text\": \"output:  Url atau link atau alamat link PMB " + nama + " adalah "
					+ Common.getRequestHostWithProtocol() + "/pmb ?\"\r\n" + "        },";
		}

		profile += "{\r\n" + "          \"text\": \"input: Apa url atau link atau alamat link tracer study " + nama
				+ " ?\"\r\n" + "        },\r\n" + "        {\r\n"
				+ "          \"text\": \"output:  url atau link atau alamat link tracer study " + nama + " adalah "
				+ Common.getRequestHostWithProtocol() + "/alumni ?\"\r\n" + "        },";

		return profile;

	}

	public static void kirimWaViaUltramsg(String from, String send) throws Exception {
		kirimWaViaUltramsg(from, send, null, null, null);
	}

	public static void kirimWaViaUltramsg(String from, String send, String namaFile, String url) throws Exception {
		kirimWaViaUltramsg(from, send, namaFile, url,
				Wa.buatProfile(SekolahUtil.getSekolah(), PerguruanTinggiUtil.getPerguruanTinggi()), true);
	}

	public static void kirimWaViaUltramsg(String from, String send, String namaFile, String url, String profile)
			throws Exception {
		kirimWaViaUltramsg(from, send, namaFile, url, profile, true);
	}

	public static int indexPengiriman = 0;
	public static Map<String, String> nomorKey = new HashMap<String, String>();

	public static void kirimWaViaUltramsg(final String froma, final String send, final String namaFile,
			final String url, final String profile, final boolean ulang) throws Exception {

		new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					String from = froma;
					String hasil = "";
					if (from == null || send == null) {
						return;
					}
					int l = 10000 - 1;
					if (send.length() > l) {

						for (int i = 0; i < send.length(); i += l) {
							String sub = send.substring(i, Math.min(i + l, send.length()));
							kirimWaViaUltramsg(from, sub, namaFile, url, profile);
						}
					} else {
						System.out.println("send -> " + send);

						if (Common.bolehKonfigurasi("aktifkan_reply_chatbot")) {

							if (from != null && !from.trim().isEmpty()
									&& !(from == null || from.toString().trim().isEmpty()
											|| from.toString().trim().equals("00000000000000000000")
											|| from.toString().trim().equals("000000000"))) {
								from = from.startsWith("0") ? "62" + from.substring(1) : from;
							}

							String[] command = WaApi.ultramsgFormat(from, send, namaFile, url);

							if (Common.getKonfigurasi("chatbot_pakai_watzap", Konfigurasi.AKTIF).getNilai().trim()
									.equalsIgnoreCase(Konfigurasi.AKTIF)) {
								command = WaApi.watzapFormat(from, send, namaFile, url);
							}

							ProcessBuilder process = new ProcessBuilder(command);
							Process p;
							p = process.start();

							BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
							StringBuilder builder = new StringBuilder();
							String line;
							while ((line = reader.readLine()) != null) {
								builder.append(line);
								builder.append(System.getProperty("line.separator"));
							}
							hasil = builder.toString();
							System.out.println("hasil -> " + hasil);
							NotifikasiWa notifikasiWa = new NotifikasiWa();
							notifikasiWa.setKeterangan(send);
							notifikasiWa.setHasil(hasil);
							notifikasiWa.setWas(from);
							Session session = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();
								session.getTransaction().begin();
								session.save(notifikasiWa);
								session.getTransaction().commit();
							} finally {
								if (session != null) {
									try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1326");}
									try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1327");}
									try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1328");}
								}
							}

						}

						if (profile != null && !profile.trim().isEmpty()) {
							try {
								String linkPost = "https://dev.ecampus.id/ecampus/Api";

								JSONObject jsonObject = new JSONObject();
								jsonObject.put("action", "waProfile");
								jsonObject.put("kode", from.trim());
								jsonObject.put("keterangan", profile.trim());

								String[] commandDa = { "curl", "-d", jsonObject.toString(), "-H",
										"Content-Type: application/json", linkPost };

								System.out.println("linkPost -> " + linkPost + ", to " + from + ", data " + profile);
								ProcessBuilder process = new ProcessBuilder(commandDa);
								Process p;
								p = process.start();
								BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
								StringBuilder builder = new StringBuilder();
								String line;
								while ((line = reader.readLine()) != null) {
									builder.append(line);
									builder.append(System.getProperty("line.separator"));
								}
								String hasilDa = builder.toString();
								System.out.println("hasilDa -> " + hasilDa);

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1360");
								// TODO: handle exception
							}
						}

					}

					try {
						if (ulang) {
							if (hasil != null && hasil.trim().toLowerCase().contains("file not exist")) {
								kirimWaViaUltramsg(from, send, null, null, profile, false);
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:1374");
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Wa.java:1377");
				}

			}
		}).start();

	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();

		String querystring = request.getQueryString();
		System.out.println("==> VA data => " + data);
		System.out.println("==> VA querystring => " + querystring);
		if (Common.bolehKonfigurasi("aktifkan_chatbot")) {
			if (data.contains("whatsapp_business_account")) {
				wa(request, response, data);
			} else if (data.contains("incoming_chat")) {
				watzap(request, response, data);
			} else if (data.contains("message_received")) {
				ultramsg(request, response, data);
			}
		}
		response.setStatus(200);
	}

	public static String ubahKeBold(String msg) {
		// Guard: string null/kosong tak punya index 0 -> hindari StringIndexOutOfBoundsException
		// pada msg.charAt(i) di bawah (mis. hasil AI kosong krn respons API gagal/tanpa
		// "candidates"). Perilaku utk string non-kosong TIDAK berubah.
		if (msg == null || msg.isEmpty()) {
			return msg;
		}

		int firstIndex = 0, secondIndex = 0, pairCount = 0;

		int i = 0;
		while (true) {

			try {
				char c = msg.charAt(i);

				if (c == '*') {
					if (pairCount == 0) {
						pairCount += 1;
						firstIndex = i;
					} else if (pairCount == 1) {
						pairCount += 1;
						secondIndex = i;
					}
				}

				if (pairCount == 2) {

					// replacing first * with <b>
					msg = msg.substring(0, firstIndex) + "<b>" + msg.substring(firstIndex + 1);

					// replacing second * with </b>
					// increasing secondIndex by 2
					// because length of "msg" is increased by 2
					// after replacing <b> with *
					secondIndex += 2;
					msg = msg.substring(0, secondIndex) + "</b>" + msg.substring(secondIndex + 1);

					// changing pair count to 0 as we have replaced the * pair
					// and are looking for new pair
					pairCount = 0;

					// Increasing i value to 5 because we have added <b> and </b> (total length = 7)
					// on replacement of * pairs (total length = 2)
					// 7 - 2 = 5
					i += 5;

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Wa.java:1452");
				// TODO: handle exception
			}

			i += 1;

			if (i >= msg.length())
				break;
		}

		return msg;
	}

}
