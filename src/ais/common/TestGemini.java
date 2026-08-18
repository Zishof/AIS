package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestGemini {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String tanya = "curl \\\r\n"
				+ "  -X POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=API_KEY \\\r\n"
				+ "  -H 'Content-Type: application/json' \\\r\n" + "  -d @<(echo '{\r\n" + "  \"contents\": [\r\n"
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
				+ "          \"text\": \"input: Dimana saja implementasi eCampus dan eSchool ?\"\r\n" + "        },\r\n"
				+ "        {\r\n"
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
				+ "          \"text\": \"input: Apa saja fitur eCampus yang diperoleh dosen ?\"\r\n" + "        },\r\n"
				+ "        {\r\n"
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
				+ "        },\r\n" + "        {\r\n" + "          \"text\": \"input: Berapa biaya atau harga ?\"\r\n"
				+ "        },\r\n" + "        {\r\n"
				+ "          \"text\": \"output: Semua pertanyaan tentang biaya atau harga, harus menghubungi pihak marketing di nomor telpon atau whatsapp (WA) +62816789081 (marketing eCampus dan eSchool).\"\r\n"
				+ "        },\r\n" + "        {\r\n"
				+ "          \"text\": \"input: Berapa biaya atau harga eCampus dan eSchool ?\"\r\n" + "        },\r\n"
				+ "        {\r\n"
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
				+ "        },\r\n" + "        {\r\n" + "          \"text\": \"input: TANYA_APA_SAJA\"\r\n" + "        },\r\n"
				+ "        {\r\n" + "          \"text\": \"output: \"\r\n" + "        }\r\n" + "      ]\r\n"
				+ "    }\r\n" + "  ],\r\n" + "  \"generationConfig\": {\r\n" + "    \"temperature\": 2,\r\n"
				+ "    \"topK\": 40,\r\n" + "    \"topP\": 0.95,\r\n" + "    \"maxOutputTokens\": 8192,\r\n"
				+ "    \"responseMimeType\": \"text/plain\"\r\n" + "  }\r\n" + "}')";

		tanya = tanya.replaceAll("API_KEY", "AIzaSyDBSOM4dHks3kQuXyhDzhBkRQz98VjHzPs");
		tanya = tanya.replaceAll("TANYA_APA_SAJA",
				"apa itu ecampus dan eschool dan apa saja fitur fitur nya dan bagaimana cara implementasinya dan berapa harganya ?");
		
		
		System.out.println("tanya -> "+tanya);
		
		String hasil = "";
		try {
			Process process = Runtime.getRuntime().exec(tanya);

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			hasil = builder.toString();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/TestGemini.java:129");
			// TODO: handle exception
		}

		System.out.println("hasil -> " + hasil);
	}

}
