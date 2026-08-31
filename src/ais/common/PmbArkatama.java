package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.httpclient.methods.GetMethod;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Agama;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Konfigurasi;
import ais.database.model.Kota;
import ais.database.model.PekerjaanOrangTua;
import ais.database.model.Propinsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.ui.util.MyMessageboxConfig;

/**
 * Klien integrasi AIS dengan sistem PMB (Penerimaan Mahasiswa Baru) pihak ketiga
 * <b>"Arkatama"</b> (host default {@code https://pmb.pusdiktan.id}, tampak sebagai platform PMB
 * milik/berkaitan dengan Pusdiktan). Kelas ini menjembatani dua arah komunikasi: (1) MENGIRIM
 * data pendaftar (calon mahasiswa) dari AIS ke sistem Arkatama begitu mereka mendaftar
 * ({@link #doPost}) dan begitu mereka dinyatakan lolos seleksi berkas ({@link #doPostLolos}); dan
 * (2) MENARIK data referensi (provinsi, kabupaten/kota, kecamatan, jalur masuk, agama, program
 * studi, jenis sekolah, jurusan sekolah, pekerjaan orang tua) dari sistem Arkatama ke tabel-tabel
 * referensi lokal AIS lewat serangkaian method {@code syn*} yang dipicu bersama oleh
 * {@link #synRef()}.
 *
 * <p>
 * <b>PERINGATAN KEAMANAN — kredensial tertanam (hardcoded) sebagai nilai default</b>: method
 * {@link #login()} mengambil kredensial akun Arkatama lewat
 * {@link Common#getKonfigurasi(String, String)} dengan nilai default tertanam langsung di kode
 * sumber bila konfigurasi database belum diisi: {@code pmb_arkatama_username} default
 * {@code "445002"} dan {@code pmb_arkatama_password} default {@code "12345"} — password berupa
 * angka sederhana yang mudah ditebak. Kredensial ini berpotensi terpakai diam-diam sebagai
 * fallback produksi bila baris konfigurasi terkait belum diisi eksplisit di database, dan
 * tersimpan sebagai plain text di riwayat kontrol versi. Nilai-nilai ini TIDAK diubah di sini —
 * lihat catatan keamanan pada laporan dokumentasi.
 * </p>
 *
 * <p>
 * <b>PERINGATAN KEAMANAN TAMBAHAN — token dan kredensial pada baris perintah proses</b>: seluruh
 * komunikasi HTTP di kelas ini dilakukan dengan MEMANGGIL BINER {@code curl} EKSTERNAL lewat
 * {@link ProcessBuilder} (bukan memakai pustaka HTTP client Java), dengan (a) flag {@code -k}
 * yang MENONAKTIFKAN verifikasi sertifikat TLS/SSL (rentan terhadap serangan
 * man-in-the-middle), dan (b) kredensial login ({@link #doLogin}) maupun token akses
 * (header {@code Authorization: <token>} pada {@link #doPost}, {@link #doPostLolos},
 * {@link #prosesPost}, {@link #prosesGet}) diteruskan sebagai ARGUMEN BARIS PERINTAH biasa
 * (bukan lewat mekanisme yang lebih aman seperti variabel lingkungan atau stdin sebagaimana
 * dilakukan {@link ais.common.gdrive.BackupUtil} untuk password basis data). Argumen baris
 * perintah suatu proses umumnya terlihat oleh pengguna lain pada mesin yang sama lewat perkakas
 * inspeksi proses OS (mis. {@code ps aux} di Unix), sehingga token/kredensial berpotensi
 * terekspos ke pihak lain yang memiliki akses ke server aplikasi selama proses {@code curl}
 * berjalan.
 * </p>
 *
 * <p>
 * <b>PERINGATAN DESAIN — state autentikasi bersifat statis-global, bukan per-sesi/per-pengguna</b>:
 * field {@link #token}, {@link #username}, dan {@link #password} adalah field statis PUBLIK pada
 * kelas ini — artinya HANYA ADA SATU token/kredensial aktif untuk SELURUH aplikasi (dibagikan
 * lintas seluruh thread/pengguna/sesi HTTP yang berjalan pada JVM yang sama), bukan disimpan per
 * request/per pengguna. Konsekuensinya: (a) dua proses sinkronisasi atau pengiriman data yang
 * berjalan bersamaan pada thread berbeda dapat saling menimpa {@link #token} satu sama lain
 * (race condition), berpotensi menyebabkan satu proses memakai token milik proses lain atau token
 * yang sudah tidak valid; (b) karena field ini {@code public}, kode lain di luar kelas ini secara
 * teknis dapat membaca ATAU MENGUBAH token/kredensial yang sedang aktif secara langsung tanpa
 * melalui {@link #login()}. Perilaku ini tidak diubah di sini karena instruksi dokumentasi hanya
 * mencakup penambahan Javadoc — lihat catatan pada laporan dokumentasi.
 * </p>
 *
 * <p>
 * <b>Pola sinkronisasi referensi ({@code syn*})</b> — seluruh method {@code synXxx(Label label)}
 * (kecuali {@link #synRef()} yang bertindak sebagai orkestrator) mengikuti struktur identik: (1)
 * memanggil endpoint referensi Arkatama yang bersangkutan lewat {@link #prosesGet(String)}; (2)
 * untuk setiap baris data yang dikembalikan, mencari entitas lokal AIS yang cocok dengan strategi
 * bertingkat — persis berdasarkan kode (exact match), lalu persis berdasarkan nama (exact match),
 * lalu nama yang BERAKHIR dengan teks pencarian ({@link MatchMode#END}) sebagai upaya toleransi
 * variasi penulisan — dan bila tidak ditemukan sama sekali, entitas baru dibuat; (3) entitas
 * ditandai berasal dari sinkronisasi ini lewat kolom {@code keterangan} yang diisi
 * {@code PmbArkatama.class.getSimpleName()} ({@code "PmbArkatama"}); (4) untuk beberapa entitas
 * ({@link JenisSeleksi}, {@link JenisSekolahMahasiswaBaru}, {@link JurusanSekolahMahasiswaBaru},
 * {@link PekerjaanOrangTua}), SETELAH seluruh baris dari Arkatama diproses, dijalankan SQL native
 * {@code UPDATE ... SET aktif=false WHERE keterangan != 'PmbArkatama'} — pola ini secara efektif
 * MENONAKTIFKAN seluruh baris entitas terkait yang BUKAN berasal dari sinkronisasi Arkatama
 * (termasuk baris yang dibuat manual oleh admin AIS), menjadikan sistem Arkatama sebagai
 * "sumber kebenaran" (source of truth) tunggal untuk entitas-entitas tersebut setiap kali
 * sinkronisasi dijalankan. Data wilayah administratif (provinsi/kabupaten-kota/kecamatan)
 * disinkronkan ganda: ke entitas domain AIS ({@link Propinsi}/{@link Kota}) DAN ke tabel generik
 * {@link Wilayah} berjenjang (level 1/2/3, dihubungkan lewat kolom {@code induk}/{@code feeder}),
 * yang tampaknya dipakai sistem lapor-diri eksternal lain (mis. PDDikti/Feeder Dikti) di luar
 * cakupan file ini.
 * </p>
 *
 * <p>
 * Method {@link #synRef()} adalah titik masuk orkestrasi: memeriksa gerbang konfigurasi
 * {@code integrasi_pmb_arkatama} (default {@link Konfigurasi#TIDAK_AKTIF}), lalu bila aktif,
 * menjalankan {@link #login()} diikuti seluruh method {@code syn*} secara berurutan pada sebuah
 * {@link Thread} terpisah agar antarmuka tidak terkunci, dengan progres dipantau lewat
 * {@link Timer} ZK yang membaca {@link Label} yang diperbarui thread pekerja.
 * </p>
 */
public class PmbArkatama {

	/**
	 * Token akses (bearer) hasil login terakhir ke API Arkatama. <b>PERINGATAN</b>: field statis
	 * PUBLIK yang dibagikan lintas seluruh aplikasi (bukan per-sesi/per-pengguna) — lihat
	 * peringatan desain pada Javadoc kelas.
	 */
	public static String token = "";
	/**
	 * Username Arkatama yang terakhir dipakai untuk login, disimpan sebagai state global.
	 * <b>PERINGATAN</b>: lihat peringatan desain pada Javadoc kelas.
	 */
	public static String username = "";
	/**
	 * Password Arkatama yang terakhir dipakai untuk login, disimpan sebagai state global dalam
	 * bentuk plain text di memori. <b>PERINGATAN</b>: lihat peringatan desain pada Javadoc kelas.
	 */
	public static String password = "";

	/**
	 * Melakukan autentikasi ke endpoint {@code /api/Auth} Arkatama lewat proses {@code curl}
	 * eksternal, mengirim {@code username}/{@code password} sebagai body JSON, memparse respons,
	 * dan menyimpan token hasil login ke field statis {@link #token}. Juga menyimpan
	 * {@code username}/{@code password} yang dipakai ke field statis {@link #username}/
	 * {@link #password} (lihat peringatan keamanan pada Javadoc kelas mengenai kredensial pada
	 * baris perintah proses dan state global).
	 *
	 * <p>
	 * Kegagalan (jaringan, parsing JSON, kredensial ditolak) ditangkap dan dicatat lewat
	 * {@link ais.common.ErrorAuditUtil#record(Throwable, String)}; pada kegagalan, {@link #token}
	 * TIDAK diperbarui (tetap bernilai sebelumnya), sehingga pemanggil perlu memeriksa
	 * {@link #token} setelah pemanggilan untuk mengetahui apakah login berhasil.
	 * </p>
	 *
	 * @param username username akun Arkatama
	 * @param password password akun Arkatama
	 * @param strURL   URL lengkap endpoint autentikasi Arkatama
	 */
	private static void doLogin(String username, String password, String strURL) {
		PmbArkatama.username = username;
		PmbArkatama.password = password;
		try {

			String hasil = "";
			try {

				JSONObject postData = new JSONObject();
				postData.put("username", username);
				postData.put("password", password);

				String[] command = { "curl", "-k", "-H", "Accept: application/json", "-X", "POST", strURL, "--data",
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
				hasil = builder.toString();

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:69");
			}

			System.out.println(hasil);

			JSONObject jSONObject = new JSONObject(hasil);
			// System.out.println("jSONObject = " + jSONObject);

			JSONObject data = jSONObject.getJSONObject("data");
			token = data.getString("token");
			System.out.println("token = " + token);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:81");
		}
	}

	/**
	 * Mengirim data pendaftaran satu calon mahasiswa ke Arkatama lewat endpoint
	 * {@code /api/Registrasi/register}, memetakan field {@link BiodataCalonMahasiswa} AIS ke
	 * format JSON yang diharapkan Arkatama (jalur masuk, nama, NIK, NISN, jenis kelamin, email,
	 * kode provinsi/kabupaten/kecamatan hasil turunan dari kecamatan calon, nomor pendaftaran,
	 * asal instansi, dan jabatan di instansi asal bila diisi).
	 *
	 * <p>
	 * Bila {@link #token} kosong, {@link #login()} dipanggil otomatis lebih dulu. Bila respons
	 * Arkatama berstatus {@code "200"}, id registrasi hasil Arkatama disimpan ke field
	 * {@code biodataCalonMahasiswa.setPinPassword(...)} (dipakai kembali sebagai referensi id
	 * eksternal pada {@link #doPostLolos}) dan pesan sukses ditambahkan ke {@code hasils}; selain
	 * itu (termasuk pengecualian apa pun), pesan kegagalan berisi detail error dari Arkatama
	 * ditambahkan ke {@code hasils}. Method ini tidak melempar pengecualian ke pemanggil — seluruh
	 * hasil (sukses maupun gagal) dilaporkan lewat penambahan baris ke {@code hasils}, sehingga
	 * cocok dipanggil berulang untuk memproses banyak calon mahasiswa sekaligus tanpa satu
	 * kegagalan menghentikan proses keseluruhan.
	 * </p>
	 *
	 * @param biodataCalonMahasiswa data calon mahasiswa yang hendak didaftarkan ke Arkatama; field
	 *                              {@code pinPassword}-nya akan diisi id registrasi Arkatama bila
	 *                              berhasil
	 * @param hasils                daftar (dimodifikasi di tempat) tempat menambahkan pesan hasil
	 *                              (sukses/gagal) untuk baris pendaftaran ini
	 */
	public static void doPost(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> hasils) {
		if (token == null || token.trim().isEmpty()) {
			login();
		}
		try {
			String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
					+ "/api/Registrasi/register");

			JSONObject postData = new JSONObject();
			postData.put("id_jalur_masuk", biodataCalonMahasiswa.getJenisSeleksi().getKode());
			postData.put("nama_lengkap", biodataCalonMahasiswa.getNama());
			postData.put("nik", biodataCalonMahasiswa.getNoIdentitas());
			postData.put("nisn", biodataCalonMahasiswa.getNisn());

			postData.put("jenis_kelamin", biodataCalonMahasiswa.getJenisKelamin());
			postData.put("email", biodataCalonMahasiswa.getEmail().split(",")[0]);

			postData.put("kode_provinsi", biodataCalonMahasiswa.getKecamatanCalon() == null
					|| biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk() == null
					|| biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getWilayahInduk() == null ? ""
							: biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getWilayahInduk().getKode());
			postData.put("kode_kabupaten",
					biodataCalonMahasiswa.getKecamatanCalon() == null
							|| biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk() == null ? ""
									: biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getKode());
			postData.put("kode_kecamatan", biodataCalonMahasiswa.getKecamatanCalon() == null ? ""
					: biodataCalonMahasiswa.getKecamatanCalon().getKode());
			postData.put("no_pendaftaran", biodataCalonMahasiswa.getNoRegistrasi());

			postData.put("asal_instansi", biodataCalonMahasiswa.getNamaSekolahAsal() == null ? ""
					: biodataCalonMahasiswa.getNamaSekolahAsal().getNama());

			if (!biodataCalonMahasiswa.getJabatanDiInstansiAsal().isEmpty()) {
				postData.put("jabatan", biodataCalonMahasiswa.getJabatanDiInstansiAsal());
			}

			String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H", "Authorization: " + token, "-X",
					"POST", strURL, "--data", postData.toString() };

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

			JSONObject jSONObject = new JSONObject(hasil);
			// System.out.println("jSONObject = " + jSONObject);

			if (!jSONObject.isNull("status") && jSONObject.get("status").toString().trim().equals("200")) {
				biodataCalonMahasiswa
						.setPinPassword(jSONObject.getJSONObject("data").getString("id_registrasi").trim());
				hasils.add("Sukses pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
						+ biodataCalonMahasiswa.getNama() + " " + biodataCalonMahasiswa.getPinPassword());
			} else {
				hasils.add("Error pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
						+ biodataCalonMahasiswa.getNama() + ", error : " + jSONObject.getString("error"));
			}
		} catch (Exception e) {
			hasils.add("Error pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
					+ biodataCalonMahasiswa.getNama());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:153");
		}
	}

	/**
	 * Mengirim data kelulusan seleksi berkas satu calon mahasiswa ke Arkatama lewat endpoint
	 * {@code /api/Registrasi/lolosBerkas}, memetakan field {@link BiodataCalonMahasiswa} yang jauh
	 * lebih lengkap dibanding {@link #doPost} — mencakup id registrasi Arkatama sebelumnya
	 * (diambil dari {@code getPinPassword()}, hasil {@link #doPost} sebelumnya), data pribadi
	 * lengkap (agama, status perkawinan, no HP), wilayah asal, URL foto profil (diambil lewat
	 * {@link CommonMedia#getUrlFotoPengguna}), pilihan program studi 1 dan 2, jenis dan jurusan
	 * sekolah asal, pekerjaan ayah, serta data instansi asal (khusus jalur tertentu, mis. pindahan
	 * dari instansi lain).
	 *
	 * <p>
	 * Bila {@link #token} kosong, {@link #login()} dipanggil otomatis lebih dulu. Bila respons
	 * Arkatama berstatus {@code "200"}, id registrasi kelulusan hasil Arkatama disimpan ke
	 * {@code biodataCalonMahasiswa.setProgramNIM(...)} dan pesan sukses ditambahkan ke
	 * {@code hasils}; selain itu, pesan berisi detail error ditambahkan ke {@code hasils}.
	 * Method ini tidak melempar pengecualian ke pemanggil, mengikuti pola pelaporan hasil yang
	 * sama dengan {@link #doPost}.
	 * </p>
	 *
	 * @param biodataCalonMahasiswa data calon mahasiswa yang sudah lolos seleksi berkas; harus
	 *                              sudah memiliki {@code pinPassword} terisi dari {@link #doPost}
	 *                              sebelumnya; field {@code programNIM}-nya akan diisi id
	 *                              registrasi kelulusan Arkatama bila berhasil
	 * @param hasils                daftar (dimodifikasi di tempat) tempat menambahkan pesan hasil
	 *                              (sukses/gagal) untuk baris ini
	 */
	public static void doPostLolos(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> hasils) {
		if (token == null || token.trim().isEmpty()) {
			login();
		}
		try {
			String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
					+ "/api/Registrasi/lolosBerkas");

			JSONObject postData = new JSONObject();
			postData.put("id_registrasi", biodataCalonMahasiswa.getPinPassword());
			postData.put("id_jalur_masuk", biodataCalonMahasiswa.getJenisSeleksi().getKode());
			postData.put("no_peserta_tes", biodataCalonMahasiswa.getNoRegistrasi());

			postData.put("nama_lengkap", biodataCalonMahasiswa.getNama());
			postData.put("nik", biodataCalonMahasiswa.getNoIdentitas());
			postData.put("nisn", biodataCalonMahasiswa.getNisn());

			postData.put("jenis_kelamin",
					biodataCalonMahasiswa.getJenisKelamin() == null ? "" : biodataCalonMahasiswa.getJenisKelamin());
			postData.put("no_hp", biodataCalonMahasiswa.getHp());

			postData.put("id_agama",
					biodataCalonMahasiswa.getAgama() == null ? "" : biodataCalonMahasiswa.getAgama().getKode());

			postData.put("status_perkawinan", biodataCalonMahasiswa.getStatusNikah().equals(0) ? "Belum Kawin"
					: biodataCalonMahasiswa.getStatusNikah().equals(1) ? "Kawin" : "Pernah Kawin");

			postData.put("email", biodataCalonMahasiswa.getEmail().split(",")[0]);

			postData.put("kode_provinsi",
					biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getWilayahInduk().getKode());
			postData.put("kode_kabupaten", biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getKode());
			postData.put("kode_kecamatan", biodataCalonMahasiswa.getKecamatanCalon().getKode());

			String url_photo;
			if (biodataCalonMahasiswa.getMahasiswa() != null) {
				url_photo = CommonMedia.getUrlFotoPengguna(new Tbmuser(biodataCalonMahasiswa.getMahasiswa()));
			} else {
				url_photo = CommonMedia.getUrlFotoPengguna(new Tbmuser(biodataCalonMahasiswa));
			}
			postData.put("url_photo", url_photo);
			postData.put("id_prodi_pilihan1",
					biodataCalonMahasiswa.getProdi1() == null ? "" : biodataCalonMahasiswa.getProdi1().getKode());
			postData.put("id_prodi_pilihan2",
					biodataCalonMahasiswa.getProdi2() == null ? "" : biodataCalonMahasiswa.getProdi2().getKode());
			postData.put("id_jenis_sekolah", biodataCalonMahasiswa.getJenisSekolah() == null ? ""
					: biodataCalonMahasiswa.getJenisSekolah().getKode());
			postData.put("asal_sekolah", biodataCalonMahasiswa.getNamaSekolahAsal() == null ? ""
					: biodataCalonMahasiswa.getNamaSekolahAsal().getNama());

			postData.put("id_jurusan", biodataCalonMahasiswa.getJurusanSekolah() == null ? ""
					: biodataCalonMahasiswa.getJurusanSekolah().getKode());

			postData.put("pekerjaan_orang_tua", biodataCalonMahasiswa.getPekerjaanAyah() == null ? ""
					: biodataCalonMahasiswa.getPekerjaanAyah().getKode());

			if (!biodataCalonMahasiswa.getInstansiAsal().isEmpty()) {
				postData.put("instansi_asal", biodataCalonMahasiswa.getInstansiAsal());
			}
			if (biodataCalonMahasiswa.getKotaInstansi() != null
					&& biodataCalonMahasiswa.getKotaInstansi().getWilayahInduk() != null) {
				postData.put("kode_provinsi_instansi",
						biodataCalonMahasiswa.getKotaInstansi().getWilayahInduk().getKode());
			}
			if (biodataCalonMahasiswa.getKotaInstansi() != null) {
				postData.put("kode_kabupaten_instansi", biodataCalonMahasiswa.getKotaInstansi().getKode());
			}
			if (!biodataCalonMahasiswa.getJabatanDiInstansiAsal().isEmpty()) {
				postData.put("jabatan", biodataCalonMahasiswa.getJabatanDiInstansiAsal());
			}

			System.out.println("postData = " + postData);

			String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H", "Authorization: " + token, "-X",
					"POST", strURL, "--data", postData.toString() };

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

			JSONObject jSONObject = new JSONObject(hasil);
			// System.out.println("jSONObject = " + jSONObject);

			if (jSONObject.getString("status").trim().equals("200")) {
				biodataCalonMahasiswa.setProgramNIM(jSONObject.getString("id_reg_lolos_berkas").trim());
				hasils.add("Sukses pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
						+ biodataCalonMahasiswa.getNama() + " " + biodataCalonMahasiswa.getPinPassword());
			} else {
				hasils.add("Hasil pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
						+ biodataCalonMahasiswa.getNama() + ", error : " + jSONObject.getString("error"));
			}
		} catch (Exception e) {
			hasils.add("Hasil pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
					+ biodataCalonMahasiswa.getNama());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:261");
		}
	}

	/**
	 * Method utilitas generik untuk mengirim permintaan HTTP POST ke Arkatama dengan body
	 * {@code data} dan header {@code Authorization: <token>}, memakai proses {@code curl}
	 * eksternal (bukan objek {@link GetMethod} yang dibuat pada baris-baris awal method ini, yang
	 * hanya dipakai untuk menyusun header namun TIDAK PERNAH benar-benar dieksekusi — permintaan
	 * sesungguhnya sepenuhnya dilakukan lewat {@code curl}), lalu memparse respons menjadi
	 * {@link JSONObject}.
	 *
	 * @param strURL URL lengkap endpoint yang dituju
	 * @param data   body permintaan (string JSON) yang dikirim lewat {@code --data} curl
	 * @return respons yang berhasil diparse sebagai {@link JSONObject}, atau {@code null} bila
	 *         permintaan/parsing gagal
	 */
	public static JSONObject prosesPost(String strURL, String data) {
		try {
			GetMethod post = new GetMethod(strURL);
			post.setRequestHeader("Authorization", token);
			post.setRequestHeader("Content-type", "application/json");

			String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H", "Authorization: " + token, "-X",
					"POST", strURL, "--data", data };

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

			JSONObject jSONObject = new JSONObject(hasil);

			return jSONObject;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:292");
		}
		return null;
	}

	/**
	 * Method utilitas generik untuk mengirim permintaan HTTP GET ke Arkatama dengan header
	 * {@code Authorization: <token>}, memakai proses {@code curl} eksternal (objek
	 * {@link GetMethod} yang dibuat di awal method hanya dipakai untuk menyusun header dan TIDAK
	 * PERNAH benar-benar dieksekusi, sama seperti pada {@link #prosesPost}), lalu memparse
	 * respons menjadi {@link JSONObject}. Dipakai oleh seluruh method {@code syn*} untuk menarik
	 * data referensi dari Arkatama.
	 *
	 * @param strURL URL lengkap endpoint referensi yang dituju
	 * @return respons yang berhasil diparse sebagai {@link JSONObject}, atau {@code null} bila
	 *         permintaan/parsing gagal
	 */
	private static JSONObject prosesGet(String strURL) {
		try {
			GetMethod post = new GetMethod(strURL);
			post.setRequestHeader("Authorization", token);
			post.setRequestHeader("Content-type", "application/json");

			String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H", "Authorization: " + token, "-X",
					"GET", strURL };

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

			JSONObject jSONObject = new JSONObject(hasil);

			return jSONObject;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:324");
		}
		return null;
	}

	/**
	 * Melakukan login ke Arkatama memakai kredensial dari konfigurasi
	 * {@code pmb_arkatama_username}/{@code pmb_arkatama_password} (lihat peringatan keamanan pada
	 * Javadoc kelas mengenai nilai default kredensial yang tertanam) dan host dari konfigurasi
	 * {@code pmb_arkatama_host_url} (default {@code https://pmb.pusdiktan.id}), lalu mendelegasikan
	 * ke {@link #doLogin(String, String, String)} yang mengisi {@link #token}.
	 */
	public static void login() {
		String username = Common.getKonfigurasi("pmb_arkatama_username", "445002").getNilai();
		String password = Common.getKonfigurasi("pmb_arkatama_password", "12345").getNilai();

		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Auth");

		doLogin(username, password, strURL);
	}

	/**
	 * Menyinkronkan data provinsi dari Arkatama ({@code /api/Ref/Provinsi}) ke entitas
	 * {@link Propinsi} AIS DAN ke {@link Wilayah} level 1 (wilayah induk teratas, dengan
	 * {@code induk} diset {@code "000000"}), mengikuti pola pencarian/pembuatan bertingkat dan
	 * penandaan {@code keterangan} yang dijelaskan pada Javadoc kelas. Berbeda dari method
	 * {@code syn*} lain, method ini TIDAK menonaktifkan baris provinsi yang bukan berasal dari
	 * sinkronisasi ini (tidak ada langkah {@code UPDATE ... aktif=false} di akhir).
	 *
	 * @param label komponen label ZK untuk menampilkan progres proses (kode+nama yang sedang
	 *              diproses) ke pengguna; nilainya dibaca oleh {@link Timer} pemantau di
	 *              {@link #synRef()}
	 */
	public static void synPropinsi(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/Provinsi");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("kode_provinsi").trim();
					String nama = data.getString("nama_provinsi").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);

					Propinsi propinsi = (Propinsi) session.createCriteria(Propinsi.class)
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (propinsi == null) {
						propinsi = (Propinsi) session.createCriteria(Propinsi.class)
								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (propinsi == null) {
						propinsi = (Propinsi) session.createCriteria(Propinsi.class)
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (propinsi == null) {
						propinsi = new Propinsi();
						propinsi.setNegara(ConstantValues.INDONESIA);
					}
					propinsi.setKode(kode);
					propinsi.setNama(nama);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, propinsi);
					session.getTransaction().commit();

					Wilayah wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "1"))
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (wilayah == null) {
						wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "1"))
								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (wilayah == null) {
						wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "1"))
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (wilayah == null) {
						wilayah = new Wilayah();
						wilayah.setInduk("000000");
						wilayah.setLevel("1");
						wilayah.setFeeder(kode);
					}
					wilayah.setKeterangan(PmbArkatama.class.getSimpleName());
					wilayah.setKode(kode);
					wilayah.setNama(nama);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, wilayah);
					session.getTransaction().commit();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:402");
			}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Menyinkronkan data kabupaten/kota dari Arkatama ({@code /api/Ref/KabupatenKota}) ke entitas
	 * {@link Kota} AIS (dikaitkan ke {@link Propinsi} induknya lewat kode provinsi) DAN ke
	 * {@link Wilayah} level 2 (dikaitkan ke {@link Wilayah} level 1 induknya). Baris kabupaten/kota
	 * yang provinsi induknya belum ditemukan di AIS (belum tersinkron lewat {@link #synPropinsi})
	 * dilewati tanpa dibuat. Mengikuti pola pencarian/pembuatan bertingkat yang sama, dengan
	 * strategi pencarian nama tambahan (penggantian kata "kabupaten" menjadi "kab." lalu dicocokkan
	 * sebagai akhiran nama) sebagai upaya terakhir sebelum membuat baris baru. Tidak menonaktifkan
	 * baris yang bukan dari sinkronisasi ini.
	 *
	 * @param label komponen label ZK untuk menampilkan progres proses
	 */
	public static void synKotakab(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/KabupatenKota");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("kode_kabupaten_kota").trim();
					String nama = data.getString("nama_kabupaten_kota").trim();
					String kode_provinsi = data.getString("kode_provinsi").trim();

					Propinsi propinsi = (Propinsi) session.createCriteria(Propinsi.class)
							.add(Restrictions.ilike("kode", kode_provinsi, MatchMode.EXACT)).setMaxResults(1)
							.uniqueResult();

					System.out.println("kode : " + kode + ", nama : " + nama + ", propinsi : " + propinsi);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama + ", propinsi : " + propinsi);

					if (propinsi != null) {

						Kota kota = (Kota) session.createCriteria(Kota.class).add(Restrictions.eq("propinsi", propinsi))
								.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
						if (kota == null) {
							kota = (Kota) session.createCriteria(Kota.class).add(Restrictions.eq("propinsi", propinsi))
									.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1)
									.uniqueResult();
						}

						if (kota == null) {
							kota = new Kota();
							kota.setPropinsi(propinsi);
						}
						kota.setKode(kode);
						kota.setNama(nama);
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, kota);
						session.getTransaction().commit();

						Wilayah wilayahInduk = (Wilayah) session.createCriteria(Wilayah.class)
								.add(Restrictions.eq("level", "1"))
								.add(Restrictions.ilike("kode", kode_provinsi, MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult();

						System.out.println("wilayahInduk : " + wilayahInduk);

						if (wilayahInduk != null) {
							Wilayah wilayah = (Wilayah) session.createCriteria(Wilayah.class)
									.add(Restrictions.eq("level", "2"))
									.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
									.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1)
									.uniqueResult();
							if (wilayah == null) {
								wilayah = (Wilayah) session.createCriteria(Wilayah.class)
										.add(Restrictions.eq("level", "2"))
										.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
										.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1)
										.uniqueResult();
							}
							if (wilayah == null) {
								wilayah = (Wilayah) session.createCriteria(Wilayah.class)
										.add(Restrictions.eq("level", "2"))
										.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
										.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1)
										.uniqueResult();
							}

							if (wilayah == null) {
								wilayah = (Wilayah) session.createCriteria(Wilayah.class)
										.add(Restrictions.eq("level", "2"))
										.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
										.add(Restrictions.ilike("nama",
												nama.toLowerCase().replaceAll("kabupaten", "kab."), MatchMode.END))
										.setMaxResults(1).uniqueResult();
							}

							if (wilayah == null) {
								wilayah = new Wilayah();
								wilayah.setInduk(wilayahInduk.getFeeder());
								wilayah.setFeeder(kode);
							}
							wilayah.setKeterangan(PmbArkatama.class.getSimpleName());
							wilayah.setLevel("2");
							wilayah.setWilayahInduk(wilayahInduk);
							wilayah.setKode(kode);
							wilayah.setNama(nama);
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, wilayah);
							session.getTransaction().commit();
						}
					}

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:505");
			}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Menyinkronkan data kecamatan dari Arkatama ({@code /api/Ref/Kecamatan}) ke {@link Wilayah}
	 * level 3, dikaitkan ke {@link Wilayah} level 2 (kabupaten/kota) induknya lewat kode
	 * kabupaten/kota. Berbeda dari {@link #synPropinsi}/{@link #synKotakab}, kecamatan HANYA
	 * disinkronkan ke tabel {@link Wilayah} generik — tidak ada entitas domain khusus AIS untuk
	 * kecamatan yang diperbarui di sini. Baris kecamatan yang kabupaten/kota induknya belum
	 * ditemukan dilewati. Tidak menonaktifkan baris yang bukan dari sinkronisasi ini.
	 *
	 * @param label komponen label ZK untuk menampilkan progres proses
	 */
	public static void synKecamatan(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/Kecamatan");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("kode_kecamatan").trim();
					String nama = data.getString("nama_kecamatan").trim();
					String kode_kebupaten_kota = data.getString("kode_kebupaten_kota").trim();

					Wilayah wilayahInduk = (Wilayah) session.createCriteria(Wilayah.class)
							.add(Restrictions.eq("level", "2"))
							.add(Restrictions.ilike("kode", kode_kebupaten_kota, MatchMode.EXACT)).setMaxResults(1)
							.uniqueResult();

					System.out.println("kode : " + kode + ", nama : " + nama + ", wilayahInduk : " + wilayahInduk);

					label.setValue(
							"Proses data kode : " + kode + ", nama : " + nama + ", wilayahInduk : " + wilayahInduk);

					if (wilayahInduk != null) {
						Wilayah wilayah = (Wilayah) session.createCriteria(Wilayah.class)
								.add(Restrictions.eq("level", "3"))
								.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
								.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
						if (wilayah == null) {
							wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "3"))
									.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
									.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1)
									.uniqueResult();
						}
						if (wilayah == null) {
							wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "3"))
									.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
									.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1)
									.uniqueResult();
						}

						if (wilayah == null) {
							wilayah = new Wilayah();
							wilayah.setInduk(wilayahInduk.getFeeder());
							wilayah.setFeeder(kode);
						}
						wilayah.setKeterangan(PmbArkatama.class.getSimpleName());
						wilayah.setLevel("3");
						wilayah.setWilayahInduk(wilayahInduk);
						wilayah.setKode(kode);
						wilayah.setNama(nama);
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, wilayah);
						session.getTransaction().commit();
					}

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:570");
			}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Menyinkronkan data jalur masuk (jenis seleksi) dari Arkatama ({@code /api/Ref/JalurMasuk})
	 * ke entitas {@link JenisSeleksi} AIS. Pencarian entitas yang sudah ada HANYA dilakukan
	 * berdasarkan kode (exact match) — strategi fallback pencarian berdasarkan nama yang dipakai
	 * method {@code syn*} lain tampak DIKOMENTARI (dinonaktifkan) pada implementasi ini, sehingga
	 * baris dengan kode yang berbeda namun nama yang mirip akan selalu dibuat sebagai baris baru,
	 * bukan dicocokkan ke baris yang sudah ada. Kode jalur tambahan dari Arkatama disimpan ke
	 * {@link JenisSeleksi#setKodeLain(String)}. Setelah seluruh baris diproses, jalankan SQL
	 * native yang MENONAKTIFKAN ({@code aktif=false}) seluruh baris {@code jenis_seleksi} yang
	 * BUKAN berasal dari sinkronisasi ini (lihat pola pada Javadoc kelas) — termasuk jenis seleksi
	 * yang dibuat manual oleh admin AIS untuk keperluan di luar jalur Arkatama.
	 *
	 * @param label komponen label ZK untuk menampilkan progres proses
	 */
	public static void synJalurMasuk(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/JalurMasuk");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_jalur").trim();
					String nama = data.getString("nama_jalur").trim();
					String kode_jalur = data.getString("kode_jalur").trim();

					System.out.println("kode : " + kode + ", nama : " + nama + " kode_jalur : " + kode_jalur);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					JenisSeleksi jenisSeleksi = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
//							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
//					if (jenisSeleksi == null) {
//						jenisSeleksi = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
//								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
//					}
//					if (jenisSeleksi == null) {
//						jenisSeleksi = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
//								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
//					}

					if (jenisSeleksi == null) {
						jenisSeleksi = new JenisSeleksi();
						jenisSeleksi.setDeskripsi(nama);
					}
					jenisSeleksi.setKodeLain(kode_jalur);
					jenisSeleksi.setKode(kode);
					jenisSeleksi.setNama(nama);
					jenisSeleksi.setKeterangan(PmbArkatama.class.getSimpleName());
					jenisSeleksi.setAktif(true);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, jenisSeleksi);
					session.getTransaction().commit();

				}

				session.createSQLQuery("update jenis_seleksi set aktif=false where keterangan !='"
						+ PmbArkatama.class.getSimpleName() + "';").executeUpdate();

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:626");
			}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Menyinkronkan data agama dari Arkatama ({@code /api/Ref/agama}) ke entitas {@link Agama}
	 * AIS, mengikuti pola pencarian/pembuatan bertingkat standar (kode exact, nama exact, nama
	 * berakhiran) dan penandaan {@code keterangan}. Tidak menonaktifkan baris yang bukan dari
	 * sinkronisasi ini (tidak ada langkah {@code UPDATE ... aktif=false}).
	 *
	 * @param label komponen label ZK untuk menampilkan progres proses
	 */
	public static void synAgama(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/agama");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_agama").trim();
					String nama = data.getString("nama_agama").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					Agama agama = (Agama) session.createCriteria(Agama.class)
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (agama == null) {
						agama = (Agama) session.createCriteria(Agama.class)

								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (agama == null) {
						agama = (Agama) session.createCriteria(Agama.class)
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (agama == null) {
						agama = new Agama();
					}
					agama.setKode(kode);
					agama.setNama(nama);
					agama.setKeterangan(PmbArkatama.class.getSimpleName());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, agama);
					session.getTransaction().commit();

				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:673");
			}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Menyinkronkan data program studi dari Arkatama ({@code /api/Ref/prodi}) ke entitas
	 * {@link Jurusan} AIS, mengikuti pola pencarian/pembuatan bertingkat standar. Kode program
	 * studi tambahan dari Arkatama disimpan ke {@link Jurusan#setKodeLain(String)}. Tidak
	 * menonaktifkan baris yang bukan dari sinkronisasi ini.
	 *
	 * @param label komponen label ZK untuk menampilkan progres proses
	 */
	public static void synProdi(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/prodi");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_prodi").trim();
					String nama = data.getString("nama_prodi").trim();
					String kode_prodi = data.getString("kode_prodi").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					Jurusan jurusan = (Jurusan) session.createCriteria(Jurusan.class)
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (jurusan == null) {
						jurusan = (Jurusan) session.createCriteria(Jurusan.class)

								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (jurusan == null) {
						jurusan = (Jurusan) session.createCriteria(Jurusan.class)
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (jurusan == null) {
						jurusan = new Jurusan();
					}
					jurusan.setKodeLain(kode_prodi);
					jurusan.setKode(kode);
					jurusan.setNama(nama);
					jurusan.setKeterangan(PmbArkatama.class.getSimpleName());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, jurusan);
					session.getTransaction().commit();

				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:722");
			}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Menyinkronkan data jenis sekolah asal dari Arkatama ({@code /api/Ref/jenisSekolah}) ke
	 * entitas {@link JenisSekolahMahasiswaBaru} AIS, mengikuti pola pencarian/pembuatan bertingkat
	 * standar (pencarian dibatasi pada baris yang sedang aktif atau belum berstatus). Setelah
	 * seluruh baris diproses, dijalankan SQL native yang menonaktifkan seluruh baris
	 * {@code jenis_sekolah_mahasiswa_baru} yang bukan berasal dari sinkronisasi ini (lihat pola
	 * pada Javadoc kelas).
	 *
	 * @param label komponen label ZK untuk menampilkan progres proses
	 */
	public static void synJenisSekolah(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/jenisSekolah");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_jenis_sekolah").trim();
					String nama = data.getString("nama_jenis_sekolah").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					JenisSekolahMahasiswaBaru jenisSekolahMahasiswaBaru = (JenisSekolahMahasiswaBaru) session
							.createCriteria(JenisSekolahMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (jenisSekolahMahasiswaBaru == null) {
						jenisSekolahMahasiswaBaru = (JenisSekolahMahasiswaBaru) session
								.createCriteria(JenisSekolahMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (jenisSekolahMahasiswaBaru == null) {
						jenisSekolahMahasiswaBaru = (JenisSekolahMahasiswaBaru) session
								.createCriteria(JenisSekolahMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (jenisSekolahMahasiswaBaru == null) {
						jenisSekolahMahasiswaBaru = new JenisSekolahMahasiswaBaru();
					}
					jenisSekolahMahasiswaBaru.setKode(kode);
					jenisSekolahMahasiswaBaru.setNama(nama);
					jenisSekolahMahasiswaBaru.setKeterangan(PmbArkatama.class.getSimpleName());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, jenisSekolahMahasiswaBaru);
					session.getTransaction().commit();

				}

				session.createSQLQuery("update jenis_sekolah_mahasiswa_baru set aktif=false where keterangan !='"
						+ PmbArkatama.class.getSimpleName() + "';").executeUpdate();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:774");
			}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Menyinkronkan data jurusan sekolah asal dari Arkatama ({@code /api/Ref/jurusanSekolah}) ke
	 * entitas {@link JurusanSekolahMahasiswaBaru} AIS. Berbeda dari method {@code syn*} lain,
	 * respons Arkatama untuk endpoint ini TIDAK menyertakan informasi jenis sekolah induknya
	 * secara eksplisit per baris — sehingga method ini mengambil SELURUH
	 * {@link JenisSekolahMahasiswaBaru} yang sedang aktif di AIS lebih dulu, lalu untuk SETIAP
	 * baris jurusan dari Arkatama, baris tersebut dicocokkan/dibuat SEKALI UNTUK MASING-MASING
	 * jenis sekolah yang ada (loop bersarang: setiap baris data Arkatama × setiap
	 * {@link JenisSekolahMahasiswaBaru} aktif) — dengan kata lain, satu nama/kode jurusan dari
	 * Arkatama akan menghasilkan satu baris {@link JurusanSekolahMahasiswaBaru} untuk TIAP jenis
	 * sekolah yang aktif di AIS, bukan hanya satu baris global. Setelah seluruh kombinasi
	 * diproses, dijalankan SQL native yang menonaktifkan seluruh baris
	 * {@code jurusan_sekolah_mahasiswa_baru} yang bukan berasal dari sinkronisasi ini.
	 *
	 * @param label komponen label ZK untuk menampilkan progres proses
	 */
	public static void synJurusanSekolah(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/jurusanSekolah");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				@SuppressWarnings("unchecked")
				List<JenisSekolahMahasiswaBaru> jenisSekolahMahasiswaBarus = session
						.createCriteria(JenisSekolahMahasiswaBaru.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).list();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_jurusan_sekolah").trim();
					String nama = data.getString("nama_jurusan_sekolah").trim();

					for (JenisSekolahMahasiswaBaru jenisSekolahMahasiswaBaru : jenisSekolahMahasiswaBarus) {

						System.out.println("kode : " + kode + ", nama : " + nama + ", jenisSekolahMahasiswaBaru "
								+ jenisSekolahMahasiswaBaru);

						label.setValue("Proses data kode : " + kode + ", nama : " + nama
								+ ", jenisSekolahMahasiswaBaru " + jenisSekolahMahasiswaBaru);

						JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) session
								.createCriteria(JurusanSekolahMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("jenisSekolahMahasiswaBaru", jenisSekolahMahasiswaBaru))
								.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
						if (jurusanSekolahMahasiswaBaru == null) {
							jurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) session
									.createCriteria(JurusanSekolahMahasiswaBaru.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("jenisSekolahMahasiswaBaru", jenisSekolahMahasiswaBaru))
									.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1)
									.uniqueResult();
						}
						if (jurusanSekolahMahasiswaBaru == null) {
							jurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) session
									.createCriteria(JurusanSekolahMahasiswaBaru.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("jenisSekolahMahasiswaBaru", jenisSekolahMahasiswaBaru))
									.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1)
									.uniqueResult();
						}

						if (jurusanSekolahMahasiswaBaru == null) {
							jurusanSekolahMahasiswaBaru = new JurusanSekolahMahasiswaBaru();
						}
						jurusanSekolahMahasiswaBaru.setJenisSekolahMahasiswaBaru(jenisSekolahMahasiswaBaru);
						jurusanSekolahMahasiswaBaru.setKode(kode);
						jurusanSekolahMahasiswaBaru.setNama(nama);
						jurusanSekolahMahasiswaBaru.setKeterangan(PmbArkatama.class.getSimpleName());
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, jurusanSekolahMahasiswaBaru);
						session.getTransaction().commit();
					}
				}

				session.createSQLQuery("update jurusan_sekolah_mahasiswa_baru set aktif=false where keterangan !='"
						+ PmbArkatama.class.getSimpleName() + "';").executeUpdate();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:842");
			}

			HibernateUtil.closeSession();
		}
	}

	/**
	 * Menyinkronkan data pekerjaan orang tua dari Arkatama ({@code /api/Ref/pekerjaan}) ke
	 * entitas {@link PekerjaanOrangTua} AIS, mengikuti pola pencarian/pembuatan bertingkat standar
	 * (pencarian dibatasi pada baris yang sedang aktif atau belum berstatus). Setelah seluruh
	 * baris diproses, dijalankan SQL native yang menonaktifkan seluruh baris
	 * {@code pekerjaan_orang_tua} yang bukan berasal dari sinkronisasi ini.
	 *
	 * @param label komponen label ZK untuk menampilkan progres proses
	 */
	public static void synPekerjaanOrangTua(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/pekerjaan");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {

				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_pekerjaan").trim();
					String nama = data.getString("nama_pekerjaan").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					PekerjaanOrangTua pekerjaanOrangTua = (PekerjaanOrangTua) session
							.createCriteria(PekerjaanOrangTua.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (pekerjaanOrangTua == null) {
						pekerjaanOrangTua = (PekerjaanOrangTua) session.createCriteria(PekerjaanOrangTua.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (pekerjaanOrangTua == null) {
						pekerjaanOrangTua = (PekerjaanOrangTua) session.createCriteria(PekerjaanOrangTua.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (pekerjaanOrangTua == null) {
						pekerjaanOrangTua = new PekerjaanOrangTua();
					}
					pekerjaanOrangTua.setKode(kode);
					pekerjaanOrangTua.setNama(nama);
					pekerjaanOrangTua.setKeterangan(PmbArkatama.class.getSimpleName());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, pekerjaanOrangTua);
					session.getTransaction().commit();

				}

				session.createSQLQuery("update pekerjaan_orang_tua set aktif=false where keterangan !='"
						+ PmbArkatama.class.getSimpleName() + "';").executeUpdate();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:896");
			}
			HibernateUtil.closeSession();
		}
	}

	public static void synRef() {

		if (!Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF)) {
			try {
				MyMessageboxConfig.show("Singkronisasi PMB Arkatama tidak diaktifkan", "Pemberitahuan",
						MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:910");
			}
			return;
		}

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi PMB Arkatama"));

		new Thread(new Runnable() {

			@Override
			public void run() {
				login();

				if (token != null && !token.trim().isEmpty()) {
					synAgama(label);
					synProdi(label);
					synJalurMasuk(label);
					synJenisSekolah(label);
					synJurusanSekolah(label);
					synPekerjaanOrangTua(label);
					synPropinsi(label);
					synKotakab(label);
					synKecamatan(label);
					label.setValue("");
				} else {
					label.setValue("Error");
				}

			}
		}).start();

		final Timer timer = new Timer(500);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					MyMessageboxConfig.show("Singkronisasi PMB Arkatama telah selesai", "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					timer.detach();
				} else if (label.getValue().equalsIgnoreCase("Error")) {
					Clients.clearBusy();
					PesanFormalHelper.tampilkanGagal("sinkronisasi data referensi PMB Arkatama",
							"Sistem gagal memperoleh token autentikasi (login) ke server PMB Arkatama, "
									+ "kemungkinan disebabkan kredensial/konfigurasi integrasi PMB Arkatama yang "
									+ "belum benar atau server PMB Arkatama sedang tidak dapat dihubungi.",
							new String[] {
									"Periksa kembali username/password/URL integrasi PMB Arkatama pada menu Konfigurasi.",
									"Pastikan server aplikasi memiliki akses jaringan ke server PMB Arkatama.",
									"Ulangi proses sinkronisasi ini beberapa saat lagi." });
					timer.detach();
				}

			}
		});
		timer.start();
	}

}
