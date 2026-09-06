package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.virtualaccount.BankaltimtaraResponseUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.WaktuUtil;

/**
 * Servlet endpoint <b>gateway Bank Kaltimtara (kode internal {@code "BMS"})</b>
 * untuk pembayaran Virtual Account dan QRIS.
 *
 * <h3>Dua arah komunikasi</h3>
 * Berbeda dengan {@link Mandiri} dan {@link Bniresponse} yang hanya menerima,
 * kelas ini bekerja dua arah:
 * <ul>
 * <li><b>Keluar (AIS &rarr; bank).</b> {@link #checkPakaiva} dan
 * {@link #checkPakaiqris} dipakai untuk rekonsiliasi manual "Cek Pembayaran":
 * AIS login ke gateway memakai username+password dari konfigurasi, menerima
 * bearer {@code token}, lalu menanyakan status sebuah tagihan. Perintah HTTP-nya
 * dijalankan lewat proses {@code curl} eksternal
 * ({@link #jalankanPerintahAmbilKeluaran}), bukan klien HTTP dalam JVM.</li>
 * <li><b>Masuk (bank &rarr; AIS).</b> {@link #process} menerima callback
 * pembayaran dan meneruskannya ke {@link #doProses} lalu {@link #doProcess},
 * yang benar-benar membukukan pelunasan.</li>
 * </ul>
 *
 * <h3>PENTING &mdash; letak pemeriksaan kredensial</h3>
 * Kredensial gateway <b>hanya dipakai pada arah keluar</b>, yaitu untuk
 * memperoleh token pada {@link #checkPakaiva}/{@link #checkPakaiqris}. Jalur
 * <b>masuk</b> &mdash; satu-satunya jalur yang membukukan uang &mdash;
 * <b>tidak memverifikasi tanda tangan, MAC, maupun token</b> atas payload
 * callback. Satu-satunya pengenalan pemanggil adalah pencocokan alamat IP ke
 * tabel {@link BankHost} lewat
 * {@link PembayaranUtil#getBankHost(String, String)} di {@link #process}, dan
 * hasilnya tidak dipakai sebagai gerbang. Pembaca kode perlu menyadari bahwa
 * perlindungan endpoint masuk bertumpu pada pembatasan jaringan di depan
 * aplikasi, bukan pada pemeriksaan di dalam kode ini.
 *
 * <h3>Bentuk callback masuk</h3>
 * {@link #doProses} mengenali dua bentuk payload:
 * <ul>
 * <li><b>VA</b> &mdash; memuat {@code number} (nomor VA), {@code amount}
 * (nominal), dan {@code trx_date};</li>
 * <li><b>QRIS</b> &mdash; memuat {@code kd_tagihan}; pada bentuk ini nominal
 * sengaja diisi sentinel negatif yang belakangan diganti nilai total tagihan itu
 * sendiri di {@link #doProcess}, karena kanal QRIS memang tidak mengirim
 * nominal terpisah.</li>
 * </ul>
 * URI yang berakhiran {@code BankaltimtaraReversal} menandai callback pembatalan
 * ({@code reversal}), yang membalik pembayaran alih-alih membukukannya.
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-06):</b> {@link #checkPakaiqris} dan
 * {@link #checkPakaiva} sebelumnya memakai kredensial gateway Bankaltimtara nyata sebagai
 * nilai default hardcoded untuk konfigurasi {@code bankaltimtara_qris_username} (default lama
 * {@code "qrisdev"}), {@code bankaltimtara_qris_password} (default lama
 * {@code "PB@|1Kp@paN19112021"}), {@code bankaltimtara_username} (default lama
 * {@code "ubtva1"}), dan {@code bankaltimtara_password} (default lama {@code "12345678"}).
 * Keempat default itu sudah diganti string kosong — kredensial kini WAJIB diisi lewat
 * konfigurasi database. Kedua method itu juga sebelumnya mencetak objek {@code login}
 * (username+password JSON) dan bearer {@code token} hasil autentikasi ke stdout/log server via
 * {@code System.out.println} — baris log tersebut sudah dihapus. Kredensial lama yang
 * sebelumnya tertanam sudah lama berada di riwayat SVN dan WAJIB dianggap bocor — perlu
 * dirotasi di sisi gateway Bankaltimtara bila masih dipakai produksi. Pola identik (kredensial
 * hardcode + log plaintext) juga diperbaiki di kelas
 * {@code ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBankaltimtara},
 * {@code DownloadNoUjianCalonMahasiswaBankBankaltimtara}, dan
 * {@code DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara}.
 * </p>
 */
public class Bankaltimtara extends HttpServlet {
	/** Versi serialisasi standar {@link HttpServlet}; tidak dipakai secara fungsional. */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton utilitas pembayaran; di kelas ini dipakai untuk memetakan alamat IP
	 * pemanggil callback ke baris {@link BankHost}.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan kontainer servlet untuk
	 * meng-instansiasi endpoint ini.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Bankaltimtara() {
		super();
	}

	/**
	 * Menangani request HTTP GET ke endpoint callback dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>
	 * Exception ditelan dan hanya ditampilkan bagi admin agar koneksi dari bank
	 * tidak putus tanpa jawaban.
	 *
	 * @param request  request masuk dari gateway Bank Kaltimtara
	 * @param response respons yang akan diisi badan JSON
	 * @throws ServletException bila kontainer servlet gagal memproses request
	 * @throws IOException      bila terjadi kegagalan I/O saat menulis respons
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
	 * Rekonsiliasi manual kanal <b>QRIS</b>: menanyakan langsung ke gateway Bank
	 * Kaltimtara apakah sebuah tagihan sudah dibayar, lalu menandainya lunas bila
	 * memang sudah.
	 *
	 * <h3>Dua panggilan berurutan</h3>
	 * <ol>
	 * <li><b>Autentikasi</b> ke {@code bankaltimtara_gateway_qris_url_autentication}
	 * dengan {@code bankaltimtara_qris_username} dan
	 * {@code bankaltimtara_qris_password} dari konfigurasi, menghasilkan bearer
	 * {@code token}.</li>
	 * <li><b>Cek status</b> ke {@code bankaltimtara_gateway_qris_url_check_status}
	 * dengan {@code kd_tagihan} (kode VA) dan {@code institusi} dari
	 * {@code bankaltimtara_inst_id_qris}, memakai token tersebut.</li>
	 * </ol>
	 * Keduanya dijalankan lewat proses {@code curl} eksternal
	 * ({@link #jalankanPerintahAmbilKeluaran}) dengan batas waktu koneksi 10 detik
	 * dan total 45 detik.
	 *
	 * <h3>Pencocokan nominal</h3>
	 * {@code bruto_amount} dari bank dibandingkan dengan
	 * {@link VirtualAccountBank#totalBiaya()}, yaitu total <i>ditambah</i> biaya
	 * administrasi &mdash; bukan {@code getTotal()} saja. Ini penting: VA/QRIS
	 * diterbitkan sebesar {@code totalBiaya()}, sehingga bila
	 * {@code bankaltimtara_biaya_administrasi} bukan nol, membandingkan ke
	 * {@code getTotal()} tidak akan pernah cocok dan tagihan yang sebenarnya sudah
	 * lunas di bank tidak pernah tertandai lunas di sistem. Hanya bila nominal cocok
	 * dan VA belum tertandai lunas, {@link VirtualAccountBank#bayarVa} dipanggil.
	 *
	 * <p>
	 * Kredensial yang dipakai di sini adalah kredensial <b>keluar</b>; lihat catatan
	 * pada javadoc kelas mengenai letak pemeriksaan kredensial dan riwayat perbaikan
	 * kredensial hardcoded.
	 *
	 * @param virtualAccountBankReadOnly VA yang hendak dicek; dipakai hanya untuk
	 *                                   dibaca kode dan nominalnya, penandaan lunas
	 *                                   dilakukan lewat session terpisah
	 * @return objek JSON hasil cek status dari bank, atau {@code null} bila respons
	 *         tidak dapat diurai
	 * @throws Exception bila autentikasi atau cek status gagal, termasuk ketika bank
	 *                   menjawab bahwa transaksi belum ada
	 */
	public static JSONObject checkPakaiqris(final VirtualAccountBank virtualAccountBankReadOnly) throws Exception {
		System.out.println("Request body: ");

		String strURL = (Common.getKonfigurasi("bankaltimtara_gateway_qris_url_autentication",
				"https://api-dev.bankaltimtara.co.id:8084/api/user/auth").getNilai());

		String user = Common.getKonfigurasi("bankaltimtara_qris_username", "").getNilai();
		String pwd = Common.getKonfigurasi("bankaltimtara_qris_password", "").getNilai();

		JSONObject login = new JSONObject();
		login.put("username", user);
		login.put("password", pwd);

		String[] command = { "curl", "--silent", "--show-error", "--connect-timeout", "10", "--max-time", "45",
				"--location", strURL, "--header",
				"Content-type: application/json", "--data-raw", login.toString() };

		String hasil = jalankanPerintahAmbilKeluaran(command, true);
		System.out.println(hasil);

		JSONObject jsonObject2 = null;
		try {
			JSONObject jSONObject = BankaltimtaraResponseUtil.parseJson(hasil, "autentikasi QRIS");
			String token = BankaltimtaraResponseUtil.ambilStringWajib(jSONObject, "token", "autentikasi QRIS");

			strURL = (Common.getKonfigurasi("bankaltimtara_gateway_qris_url_check_status",
					"https://api-dev.bankaltimtara.co.id:8084/api/qrismpm/transaction/status").getNilai());

			String inst_id = Common.getKonfigurasi("bankaltimtara_inst_id_qris", "211028001").getNilai();

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("kd_tagihan", virtualAccountBankReadOnly.getKode());
			jsonObject.put("institusi", inst_id);
			String postData = jsonObject.toString();

			System.out.println(postData + "");

			command = new String[] { "curl", "--silent", "--show-error", "--connect-timeout", "10",
					"--max-time", "45", "--location", strURL, "--header",
					"Content-type: application/json", "--header", "Authorization: Bearer " + token, "--data",
					postData };

			hasil = jalankanPerintahAmbilKeluaran(command, true);
			System.out.println(hasil);

			jsonObject2 = BankaltimtaraResponseUtil.parseJson(hasil, "cek status QRIS");
			BankaltimtaraResponseUtil.pastikanSukses(jsonObject2, "cek status QRIS");
		} catch (Exception e) {
			throw e;
		}

		System.out.println("hasil -> " + hasil);

		try {

			if (jsonObject2 != null) {
				Double paid = Double.parseDouble((jsonObject2.get("bruto_amount") + "").trim());
				// Bandingkan ke totalBiaya() (total+biaya admin), BUKAN getTotal() saja -- VA/QRIS
				// dibuat & ditagihkan sebesar totalBiaya() (lihat DownloadTagihanMahasiswaBankBankaltimtara
				// & pengecekan H2H masuk di file ini yg SUDAH benar pakai totalBiaya()). Bila
				// bankaltimtara_biaya_administrasi != 0, bandingkan ke getTotal() saja TAK PERNAH cocok
				// -> VA yg sudah lunas di bank tak pernah ditandai lunas di sini ("Cek Ulang" diam gagal).
				if (paid.intValue() == virtualAccountBankReadOnly.totalBiaya()) {
					if (VirtualAccountBank.isSudahTerbayar(virtualAccountBankReadOnly)) {
						return jsonObject2;
					}
					Session session = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						VirtualAccountBank.bayarVa(virtualAccountBankReadOnly, WaktuUtil.getDate(), hasil, session);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bankaltimtara.java:161");
					} finally {
						if (session != null) {
							try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:164");}
							try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:165");}
							try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:166");}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bankaltimtara.java:172");
		}
		return jsonObject2;
	}

	/**
	 * Cek status pembayaran Bankaltimtara dengan FALLBACK ke kanal lain: coba dulu kanal
	 * yang tercatat di VA ({@link VirtualAccountBank#getPakaiva()} â€” VA atau QRIS), lalu
	 * bila kanal itu gagal (exception apa pun dari bank, mis. "code: 04; message: not
	 * found"), coba kanal SATUNYA sebelum menyerah. Diminta karena "Cek Ulang" yang cuma
	 * mengecek satu kanal sering gagal walau bank sebenarnya sudah mencatat pembayaran di
	 * kanal lainnya (kd_tagihan/kode VA yang sama dipakai bank utk kedua kanal).
	 *
	 * @throws Exception bila KEDUA kanal gagal â€” pesan memuat rincian error dari keduanya.
	 */
	public static JSONObject checkPakaivaAtauQris(final VirtualAccountBank virtualAccountBankReadOnly)
			throws Exception {
		boolean vaDulu = virtualAccountBankReadOnly.getPakaiva();
		Exception errorPertama;
		try {
			return vaDulu ? checkPakaiva(virtualAccountBankReadOnly) : checkPakaiqris(virtualAccountBankReadOnly);
		} catch (Exception e) {
			errorPertama = e;
			/* Kegagalan kanal pertama karena bank menjawab "belum ada transaksi" (code 04 /
			 * "not found") BUKAN kerusakan sistem: itu jawaban wajar untuk tagihan yang memang
			 * belum dibayar. Dulu kondisi ini dicatat sebagai error lengkap dengan stack trace
			 * pada SETIAP klik "Periksa Ulang", sehingga log dipenuhi kejadian normal dan error
			 * yang sesungguhnya jadi sulit ditemukan. Kanal cadangan tetap dicoba seperti biasa;
			 * hanya cara pencatatannya yang disesuaikan. */
			if (merupakanTransaksiBelumAdaDiBank(e)) {
				System.out.println("Bankaltimtara: kanal " + (vaDulu ? "VA" : "QRIS")
						+ " menjawab transaksi belum ada (code 04) untuk VA id="
						+ virtualAccountBankReadOnly.getId() + ", mencoba kanal "
						+ (vaDulu ? "QRIS" : "VA") + ".");
			} else {
				ais.common.ErrorAuditUtil.record(e, "Bankaltimtara.checkPakaivaAtauQris: kanal utama ("
						+ (vaDulu ? "VA" : "QRIS") + ") gagal, mencoba kanal lain (" + (vaDulu ? "QRIS" : "VA") + ")");
			}
		}
		try {
			return vaDulu ? checkPakaiqris(virtualAccountBankReadOnly) : checkPakaiva(virtualAccountBankReadOnly);
		} catch (Exception eFallback) {
			/* Bila KEDUA kanal sama-sama menjawab "belum ada transaksi", penyebabnya adalah data
			 * pembayaran yang memang belum tercatat di bank -- bukan gangguan aplikasi. Berikan
			 * pesan yang dapat dimengerti operator, bukan tumpukan pesan teknis dua kanal. */
			if (merupakanTransaksiBelumAdaDiBank(errorPertama) && merupakanTransaksiBelumAdaDiBank(eFallback)) {
				throw new Exception("Bank Bankaltimtara belum mencatat transaksi apa pun untuk Virtual Account ini"
						+ " (VA id=" + virtualAccountBankReadOnly.getId() + "). Pembayaran kemungkinan memang belum"
						+ " dilakukan, atau kode billing-nya sudah kedaluwarsa dan perlu dibuat ulang.", errorPertama);
			}
			throw new Exception(
					"Pemeriksaan ulang gagal di kedua kanal Bankaltimtara. " + (vaDulu ? "VA" : "QRIS") + ": "
							+ errorPertama.getMessage() + " | " + (vaDulu ? "QRIS" : "VA") + ": "
							+ eFallback.getMessage(),
					errorPertama);
		}
	}

	/**
	 * Jalankan perintah eksternal (curl) lalu kembalikan SELURUH keluarannya sebagai String.
	 *
	 * <p><b>Alasan perbaikan.</b> Keempat pemanggilan {@code ProcessBuilder} di kelas ini DULU
	 * membaca stdout lewat {@code BufferedReader} yang <b>tidak pernah ditutup</b>, dan proses
	 * {@code curl}-nya tidak pernah di-{@code destroy()}. Setiap pemeriksaan status pembayaran
	 * karena itu meninggalkan file descriptor pipe dan objek {@code Process} menggantung sampai
	 * GC berjalan -- pada server yang ramai berujung pada "Too many open files" dan proses curl
	 * zombie. Semua pembersihan kini dilakukan di {@code finally}, sehingga tetap terjadi
	 * meskipun pembacaan gagal di tengah jalan.</p>
	 *
	 * <p><b>Perilaku dipertahankan.</b> Cara membaca datanya sama persis dengan kode lama
	 * (baris demi baris, dipisah {@code line.separator} platform). Parameter
	 * {@code gabungkanErrorStream} mengikuti setelan pemanggil. Seluruh pemeriksaan VA/QRIS
	 * memakai {@code --silent --show-error}, sehingga stderr aman digabungkan dan pesan kegagalan
	 * koneksi tetap dapat dibaca tanpa progress-meter curl merusak JSON.</p>
	 *
	 * <p>{@code waitFor()} hanya dipanggil bila stderr digabungkan ke stdout -- pada kasus itu
	 * seluruh keluaran sudah habis terbaca sehingga proses dijamin tidak menggantung. Bila
	 * stderr terpisah dan tidak dibaca, {@code waitFor()} berpotensi menggantung saat buffer
	 * pipe penuh, jadi prosesnya cukup ditutup lewat {@code destroy()}.</p>
	 */
	private static String jalankanPerintahAmbilKeluaran(String[] command, boolean gabungkanErrorStream)
			throws Exception {
		Process p = null;
		BufferedReader reader = null;
		try {
			ProcessBuilder process = new ProcessBuilder(command);
			if (gabungkanErrorStream) {
				process.redirectErrorStream(true);
			}
			p = process.start();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			return builder.toString();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception abaikan) {
				}
			}
			if (p != null) {
				try {
					p.getOutputStream().close();
				} catch (Exception abaikan) {
				}
				try {
					p.getErrorStream().close();
				} catch (Exception abaikan) {
				}
				if (gabungkanErrorStream) {
					try {
						p.waitFor();
					} catch (InterruptedException interupsi) {
						// Jangan telan status interrupt: kembalikan supaya lapisan di atas tahu.
						Thread.currentThread().interrupt();
					} catch (Exception abaikan) {
					}
				}
				try {
					p.destroy();
				} catch (Exception abaikan) {
				}
			}
		}
	}

	/**
	 * Apakah kegagalan berasal dari jawaban Bankaltimtara "transaksi tidak ditemukan"
	 * (<code>code: 04</code> / <code>not found</code>) -- yaitu keadaan bisnis yang WAJAR untuk
	 * tagihan yang belum dibayar, bukan kerusakan sistem yang perlu diaudit sebagai error.
	 */
	private static boolean merupakanTransaksiBelumAdaDiBank(Throwable e) {
		Throwable cur = e;
		int pengaman = 0;
		while (cur != null && pengaman < 12) {
			String pesan = cur.getMessage();
			if (pesan != null) {
				String kecil = pesan.toLowerCase();
				if (kecil.indexOf("not found") >= 0
						|| kecil.indexOf("\"code\":\"04\"") >= 0
						|| kecil.indexOf("code: 04") >= 0) {
					return true;
				}
			}
			if (cur.getCause() == cur) {
				break;
			}
			cur = cur.getCause();
			pengaman++;
		}
		return false;
	}

	/**
	 * Validasi minimal sebelum {@code new JSONObject(hasil)}: pastikan respons tidak
	 * null/kosong dan diawali karakter '{' (setelah di-trim). Endpoint bank kadang membalas
	 * string kosong atau halaman HTML error (mis. gateway timeout/404) alih-alih JSON, yang
	 * kalau langsung di-parse akan melempar {@link org.json.JSONException} mentah ("A
	 * JSONObject text must begin with '{'...") sampai ke dialog admin. Di sini exception
	 * diganti dengan pesan Indonesia yang jelas agar admin tahu akar masalahnya (respons bank
	 * tidak valid), bukan pesan teknis JSON.
	 *
	 * @param hasil   string respons mentah dari bank yang akan diparse sebagai JSON.
	 * @param konteks label singkat sumber respons (mis. "login"/"cek status VA") untuk pesan error.
	 * @throws Exception bila respons tidak valid sebagai JSON object.
	 */
	private static void validasiResponJsonBankaltimtara(String hasil, String konteks) throws Exception {
		String trimmed = hasil == null ? "" : hasil.trim();
		if (trimmed.isEmpty() || trimmed.charAt(0) != '{') {
			throw new Exception("Gagal memeriksa status VA Bankaltimtara (" + konteks
					+ "): respons dari bank tidak valid (kosong atau bukan format JSON yang dikenali). Respons: "
					+ BankaltimtaraResponseUtil.ringkas(trimmed) + ". "
					+ "Kemungkinan gateway bank sedang bermasalah/berubah. Silakan coba lagi beberapa saat, "
					+ "atau hubungi administrator bila terus berulang.");
		}
	}

	public static JSONObject checkPakaiva(final VirtualAccountBank virtualAccountBankReadOnly) throws Exception {
		String strURL = (Common.getKonfigurasi("bankaltimtara_gateway_url_autentication",
				"https://api-dev.bankaltimtara.co.id:8300/api/user/auth").getNilai());

		String linkPost = Common.getKonfigurasi("url_status_va_bankaltimtara",
				"https://api-dev.bankaltimtara.co.id:8081/api-service/api/va/paid/nova").getNilai().trim();

		String user = Common.getKonfigurasi("bankaltimtara_username", "").getNilai();
		String pwd = Common.getKonfigurasi("bankaltimtara_password", "").getNilai();

		JSONObject login = new JSONObject();
		login.put("username", user);
		login.put("password", pwd);

		String[] command = { "curl", "--silent", "--show-error", "--connect-timeout", "10", "--max-time", "45",
				"--location", strURL, "--header", "Content-type: application/json", "--data-raw",
				login.toString() };

		String hasil = jalankanPerintahAmbilKeluaran(command, true);
		System.out.println(hasil);

		validasiResponJsonBankaltimtara(hasil, "login/autentikasi");
		JSONObject jSONObject = new JSONObject(hasil);

		String token = jSONObject.getString("token");

		String post = linkPost + "/" + virtualAccountBankReadOnly.getKode();

		String[] commandPost = { "curl", "--silent", "--show-error", "--connect-timeout", "10", "--max-time",
				"45", "--location", post, "--header", "Authorization: Bearer " + token };

		System.out.println("linkPost -> " + post);

		JSONObject jsonObject2 = null;
		Exception errorLive = null;

		try {

			hasil = jalankanPerintahAmbilKeluaran(commandPost, true);

			System.out.println("hasil -> " + hasil);

			jsonObject2 = prosesHasilCekVaBankaltimtara(virtualAccountBankReadOnly, hasil, false);

		} catch (Exception e) {
			// Kode 04/not found adalah keadaan bisnis yang normal ketika VA memang belum
			// terbentuk di sisi bank. Jangan mencatatnya sebagai error aplikasi; pemanggil
			// tetap akan menjalankan fallback notifikasi tersimpan di bawah ini.
			if (!merupakanTransaksiBelumAdaDiBank(e)) {
				e.printStackTrace();
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit src/ais/action/servlet/Bankaltimtara.java:checkPakaiva-live");
			}
			errorLive = e;
		}

		if (errorLive != null) {
			// FALLBACK: pengecekan LIVE ke gateway Bankaltimtara gagal (mis. endpoint "nova" sedang
			// bermasalah/berubah dan membalas HTML 404 alih-alih JSON, sehingga new JSONObject(hasil)
			// gagal parse). Daripada berhenti dengan hasil kosong, olah ULANG notifikasi TERAKHIR yang
			// sudah tersimpan di kolom notif (hasil pengecekan/notifikasi bank sebelumnya yang valid),
			// SEOLAH-OLAH baru saja diterima â€” tapi diproses langsung secara lokal, tanpa panggilan
			// jaringan baru ke bank. Ini murni fallback pemulihan data yang SUDAH ADA; bila tidak ada
			// notifikasi tersimpan sama sekali, error LIVE asli tetap dilempar apa adanya agar admin
			// melihat akar masalah sebenarnya (mis. endpoint bank yang berubah/error).
			String notifTersimpan = virtualAccountBankReadOnly.getNotif();
			if (notifTersimpan != null && !notifTersimpan.trim().isEmpty()) {
				try {
					jsonObject2 = prosesHasilCekVaBankaltimtara(virtualAccountBankReadOnly, notifTersimpan, true);
				} catch (Exception eFallback) {
					ais.common.ErrorAuditUtil.record(eFallback,
							"auto-audit src/ais/action/servlet/Bankaltimtara.java:checkPakaiva fallback notif tersimpan gagal juga");
					throw errorLive;
				}
			} else {
				throw errorLive;
			}
		}

		return jsonObject2;
	}

	/**
	 * Olah satu string hasil pengecekan status VA Bankaltimtara (baik dari pengecekan LIVE ke
	 * gateway, maupun dari notifikasi yang sebelumnya tersimpan di kolom {@code notif}): cocokkan
	 * nominal, lalu tandai lunas via {@link VirtualAccountBank#bayarVa} bila belum.
	 *
	 * <p>Format LIVE membungkus nominal di {@code data.amount}; format notifikasi tersimpan (hasil
	 * webhook H2H sebelumnya, mis. {@code {"inst_id":...,"number":...,"amount":...,"reff":...,"date":...}})
	 * menaruh {@code amount} langsung di root â€” keduanya ditangani di sini.</p>
	 *
	 * @param dariNotifTersimpan true bila {@code hasil} berasal dari fallback notif tersimpan (bukan
	 *                           pengecekan live baru) â€” ditandai di JSON balikan agar admin tahu sumber datanya.
	 */
	private static JSONObject prosesHasilCekVaBankaltimtara(VirtualAccountBank virtualAccountBankReadOnly,
			String hasil, boolean dariNotifTersimpan) throws Exception {
		validasiResponJsonBankaltimtara(hasil, dariNotifTersimpan ? "notifikasi tersimpan" : "cek status VA");
		JSONObject jsonObject2 = new JSONObject(hasil);

		JSONObject object = ambilDataPembayaranBankaltimtara(jsonObject2);
		if (object == null) {
			throw new Exception("Respons cek status Bankaltimtara tidak memuat transaksi dengan field amount: "
					+ BankaltimtaraResponseUtil.ringkas(hasil) + ", VA id=" + virtualAccountBankReadOnly.getId());
		}

		Double paid = parseNominalBankaltimtara(object.get("amount"));
				// Sama seperti checkPakaiqris: bandingkan ke totalBiaya() (total+biaya admin), BUKAN
				// getTotal() saja -- konsisten dgn pengecekan H2H masuk di file ini (baris ~502) yg
				// SUDAH pakai totalBiaya(). Bug lama di sini: getTotal() saja membuat VA yg sudah lunas
				// (dgn biaya admin > 0) tak pernah cocok -> "Cek Ulang" diam-diam gagal update status.
		if (paid.intValue() == virtualAccountBankReadOnly.totalBiaya()) {
			if (VirtualAccountBank.isSudahTerbayar(virtualAccountBankReadOnly)) {
				return tandaiSumberData(jsonObject2, dariNotifTersimpan);
			}
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				VirtualAccountBank.bayarVa(virtualAccountBankReadOnly, WaktuUtil.getDate(), hasil, session);
			} finally {
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:257");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:258");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:259");}
				}
			}
		} else {
					// Nominal dari bank TIDAK cocok totalBiaya() -- sebelumnya diam saja (VA tetap
					// tampak belum lunas tanpa keterangan). Catat spy "sudah bayar tapi status tak
					// berubah" bisa ditelusuri: idnya VA, nominal versi bank vs versi sistem.
					ais.common.ErrorAuditUtil.record(
							new Exception("Nominal cek Bankaltimtara tidak cocok: dibayar=" + paid.intValue()
									+ ", totalBiaya() sistem=" + virtualAccountBankReadOnly.totalBiaya()
									+ ", VA id=" + virtualAccountBankReadOnly.getId()),
					"Bankaltimtara.prosesHasilCekVaBankaltimtara: nominal tidak cocok, status VA tidak diperbarui");
			throw new Exception("Nominal pembayaran Bankaltimtara " + paid.intValue()
					+ " tidak sama dengan tagihan sistem " + virtualAccountBankReadOnly.totalBiaya()
					+ " untuk VA " + virtualAccountBankReadOnly.getKode() + ".");
		}

		return tandaiSumberData(jsonObject2, dariNotifTersimpan);
	}

	/**
	 * Mengekstrak objek transaksi yang memuat field {@code amount} dari respons cek
	 * status Bank Kaltimtara, yang bentuknya tidak selalu sama.
	 *
	 * <h3>Bentuk yang ditangani</h3>
	 * <ul>
	 * <li>{@code data} berupa objek &mdash; dipakai langsung bila punya
	 * {@code amount};</li>
	 * <li>{@code data} berupa larik &mdash; ditelusuri <b>dari belakang</b> supaya
	 * yang terambil adalah entri terbaru, dan dipakai entri pertama yang punya
	 * {@code amount};</li>
	 * <li>tanpa pembungkus {@code data} &mdash; objek respons itu sendiri dipakai
	 * bila sudah memuat {@code amount}.</li>
	 * </ul>
	 *
	 * @param response objek JSON respons dari bank
	 * @return objek transaksi yang memuat {@code amount}, atau {@code null} bila
	 *         tidak ada satu pun bentuk di atas yang cocok
	 * @throws Exception bila pembacaan struktur JSON gagal
	 */
	private static JSONObject ambilDataPembayaranBankaltimtara(JSONObject response) throws Exception {
		if (!response.isNull("data")) {
			Object data = response.get("data");
			if (data instanceof JSONObject) {
				JSONObject object = (JSONObject) data;
				if (!object.isNull("amount")) {
					return object;
				}
			} else if (data instanceof JSONArray) {
				JSONArray daftar = (JSONArray) data;
				for (int i = daftar.length() - 1; i >= 0; i--) {
					Object item = daftar.get(i);
					if (item instanceof JSONObject && !((JSONObject) item).isNull("amount")) {
						return (JSONObject) item;
					}
				}
			}
		}
		return response.isNull("amount") ? null : response;
	}

	/**
	 * Mengubah nilai nominal dari respons bank menjadi {@link Double}, dengan
	 * toleransi terhadap format yang dikirim gateway.
	 *
	 * <p>
	 * Pemisah ribuan berupa koma dibuang lebih dulu dan spasi di tepi dipangkas,
	 * karena Bank Kaltimtara dapat mengirim nominal sebagai teks berformat
	 * ({@code "1,250,000"}) maupun sebagai angka. Nilai kosong maupun yang tidak
	 * dapat diurai sengaja dilempar sebagai exception ber-pesan jelas &mdash; bukan
	 * dianggap nol &mdash; supaya kegagalan pembacaan tidak pernah tersamar sebagai
	 * pembayaran bernilai nol.
	 *
	 * @param nominal nilai mentah dari JSON respons; boleh {@code null}
	 * @return nominal sebagai {@link Double}
	 * @throws Exception bila nominal kosong atau tidak dapat diurai sebagai angka
	 */
	private static Double parseNominalBankaltimtara(Object nominal) throws Exception {
		String nilai = nominal == null ? "" : nominal.toString().trim().replace(",", "");
		if (nilai.isEmpty()) {
			throw new Exception("Nominal pembayaran Bankaltimtara kosong.");
		}
		try {
			return Double.parseDouble(nilai);
		} catch (NumberFormatException e) {
			throw new Exception("Nominal pembayaran Bankaltimtara tidak dapat dibaca: " + nilai, e);
		}
	}

	/**
	 * Menyisipkan penanda asal-usul data ke objek JSON hasil cek pembayaran, supaya
	 * admin tidak salah menyimpulkan bahwa hasil yang dilihatnya berasal dari
	 * pengecekan langsung ke bank.
	 *
	 * <p>
	 * Bila hasil berasal dari cadangan notifikasi tersimpan, ditambahkan field
	 * {@code _sumberData} bernilai {@code "notifikasi_tersimpan_fallback"} beserta
	 * {@code _keterangan} yang menjelaskan bahwa pengecekan live ke gateway gagal
	 * dan angka yang ditampilkan diproses ulang dari notifikasi terakhir. Bila
	 * berasal dari pengecekan live, objek dikembalikan apa adanya tanpa penanda.
	 *
	 * <p>
	 * Kegagalan menyisipkan penanda sengaja tidak dilempar &mdash; hanya dicatat ke
	 * Error Log &mdash; karena penandaan bersifat informatif dan tidak boleh
	 * menggagalkan hasil cek pembayaran yang sudah diperoleh.
	 *
	 * @param jsonObject2        objek hasil cek yang akan ditandai
	 * @param dariNotifTersimpan {@code true} bila hasil berasal dari cadangan
	 *                           notifikasi tersimpan, bukan pengecekan live
	 * @return objek yang sama, sudah ditandai bila perlu
	 */
	private static JSONObject tandaiSumberData(JSONObject jsonObject2, boolean dariNotifTersimpan) {
		try {
			if (dariNotifTersimpan) {
				jsonObject2.put("_sumberData", "notifikasi_tersimpan_fallback");
				jsonObject2.put("_keterangan", "Pengecekan LIVE ke gateway Bankaltimtara gagal (lihat log admin "
						+ "untuk detail); hasil di atas diproses ULANG dari notifikasi TERAKHIR yang tersimpan, "
						+ "BUKAN hasil pengecekan live saat ini.");
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:tandaiSumberData");
		}
		return jsonObject2;
	}

	/**
	 * Menangani request HTTP POST ke endpoint callback &mdash; jalur normal
	 * notifikasi pembayaran VA/QRIS Bank Kaltimtara &mdash; dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request  request masuk dari gateway Bank Kaltimtara; badan request
	 *                 dibaca sebagai JSON mentah
	 * @param response respons yang akan diisi badan JSON
	 * @throws ServletException bila kontainer servlet gagal memproses request
	 * @throws IOException      bila terjadi kegagalan I/O saat menulis respons
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
	 * Menentukan apakah sebuah VA sudah melewati batas waktu bayar sehingga callback
	 * otomatis dari bank harus ditolak.
	 *
	 * <p>
	 * Dipakai <b>hanya</b> pada jalur callback publik. Rekonsiliasi manual
	 * ({@code chekLagi}/{@code chek}) dan {@code reversal} sengaja dikecualikan di
	 * {@link #doProcess}, karena admin memang perlu bisa merapikan tagihan yang
	 * sudah lewat tenggat.
	 *
	 * <p>
	 * Batas waktu diambil dari {@link VirtualAccountBank#getKadaluarsaWaktu()}; VA
	 * tanpa batas waktu dianggap belum kedaluwarsa. Perbandingan memakai
	 * {@code !batasPembayaran.after(sekarang)} sehingga tepat pada detik batas pun
	 * sudah dihitung kedaluwarsa.
	 *
	 * @param virtualAccountBank VA yang diperiksa; {@code null} dianggap belum
	 *                           kedaluwarsa
	 * @return {@code true} bila callback otomatis harus ditolak karena tenggat sudah
	 *         lewat
	 */
	private static boolean sudahKadaluarsaUntukCallbackOtomatis(VirtualAccountBank virtualAccountBank) {
		if (virtualAccountBank == null) {
			return false;
		}
		Date batasPembayaran = virtualAccountBank.getKadaluarsaWaktu();
		return batasPembayaran != null && !batasPembayaran.after(WaktuUtil.getDate());
	}

	/**
	 * Inti pemrosesan callback Bank Kaltimtara: mencari Virtual Account,
	 * memvalidasinya, lalu membukukan pelunasan &mdash; atau membalikkannya bila
	 * {@code reversal}.
	 *
	 * <h3>Urutan validasi</h3>
	 * <ol>
	 * <li><b>Nominal terbaca</b> &mdash; {@code Double.NaN} (sentinel dari
	 * {@link #doProses} untuk {@code amount} kosong/rusak) ditolak dengan
	 * "Format nominal transaksi tidak valid".</li>
	 * <li><b>Tanggal terbaca</b> &mdash; {@code trx_date} yang tidak dapat diurai
	 * ditolak dengan "Format tanggal transaksi tidak valid".</li>
	 * <li><b>VA ditemukan</b> lewat
	 * {@link VirtualAccountBank#ambilVa(String, double, BankHost)}; gagal dijawab
	 * "Nomor VA salah".</li>
	 * <li><b>Kedaluwarsa</b> &mdash; lewat
	 * {@link #sudahKadaluarsaUntukCallbackOtomatis}, hanya untuk callback publik
	 * ({@code chekLagi}, {@code reversal}, dan {@code chek} dikecualikan); tagihan
	 * lewat tenggat diarahkan ke rekonsiliasi manual.</li>
	 * <li><b>Sudah terbayar</b> &mdash; lewat
	 * {@link VirtualAccountBank#isSudahTerbayarUntukPayment}.</li>
	 * <li><b>Kecocokan nominal</b> &mdash; pada mode pembayaran, nominal dari bank
	 * harus sama dengan {@link VirtualAccountBank#totalBiaya()}.</li>
	 * </ol>
	 *
	 * <h3>Sentinel nominal negatif (kanal QRIS)</h3>
	 * Bila {@code nominalP} bernilai negatif &mdash; yang di {@link #doProses} hanya
	 * terjadi untuk payload ber-{@code kd_tagihan}, yakni kanal QRIS yang memang
	 * tidak mengirim nominal terpisah &mdash; nilainya lebih dulu diganti dengan
	 * {@code getTotal() + getBiayaAdmin()} milik VA yang bersangkutan. Akibatnya
	 * pemeriksaan kecocokan nominal pada langkah 6 terpenuhi dengan sendirinya untuk
	 * jalur ini; besaran yang dibukukan adalah total tagihan menurut sistem, bukan
	 * angka yang dikirim pemanggil.
	 *
	 * <h3>Jalur entitas dan token cicilan</h3>
	 * Seperti pada {@link Mandiri}, alur bercabang antara siswa/calon siswa dan
	 * mahasiswa/calon mahasiswa, dan kolom {@code cicilan} milik VA diurai menjadi
	 * token yang masing-masing menjadi {@link CicilanPembayaran} (format numerik,
	 * {@code Bulanan-}, {@code Item-}, dan {@code Keranjang-}). Pada mode
	 * {@code reversal} baris-baris pembayaran itu dihapus kembali dan VA
	 * dikembalikan ke keadaan belum lunas.
	 *
	 * <h3>Pencatatan</h3>
	 * Blok {@code finally} selalu memanggil
	 * {@code PembayaranGatewayHelper.catatLogHostToHost(...)} dengan payload mentah,
	 * sehingga setiap callback terekam terlepas dari berhasil atau tidaknya.
	 *
	 * @param nominalP nominal dari bank; {@code NaN} berarti tidak terbaca, nilai
	 *                 negatif berarti "pakai total tagihan sistem" (kanal QRIS)
	 * @param tanggalP waktu transaksi dari bank sebagai teks
	 * @param va       nomor VA atau kode tagihan yang ditagih
	 * @param bank     nama bank pembayar; disimpan sebagai validator
	 * @param bankHost baris {@link BankHost} hasil pencocokan IP; boleh {@code null}
	 *                 &mdash; alur tetap berjalan
	 * @param request  request HTTP asli; boleh {@code null} pada rekonsiliasi manual
	 * @param data     payload JSON mentah; disimpan apa adanya ke Log Host-to-Host
	 * @param chekLagi {@code true} melewati pemeriksaan kedaluwarsa
	 * @param inquery  {@code true} bila hanya menanyakan rincian tanpa membukukan
	 * @param reversal {@code true} membatalkan pembayaran yang sudah dibukukan
	 * @param chek     {@code true} bila dipicu admin dari menu rekonsiliasi
	 * @return objek JSON respons untuk bank, memuat {@code errorCode} dan
	 *         {@code statusDescription}
	 * @throws Exception bila terjadi kegagalan di luar yang sudah ditangani internal
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi, boolean inquery, boolean reversal, boolean chek)
			throws Exception {

		JSONObject response = new JSONObject(data);
		response.put("errorCode", "00");
		response.put("statusDescription", "Success");

		JSONArray billDetails = new JSONArray();

		if (inquery) {
			response.put("billDetails", billDetails);
		}

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			JSONArray rincian = new JSONArray();
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			VirtualAccountBank virtualAccountBankNtt = null;
			Session session = null;
			try {

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominalP, bankHost);

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getKadaluarsa() != null) {
					System.out.println(
							"Kadaluara " + Common.databaseDateFormat.get().format(virtualAccountBankNtt.getKadaluarsa()));
				}
				Date tanggal = ais.ui.util.WaktuUtil.getDate();
				// VALIDASI DEFENSIF: bank kadang mengirim trx_date kosong/tak lengkap
				// (callback duplikat/retry/health-check) - jangan langsung parse mentah.
				boolean tanggalTidakValid = false;
				if (tanggalP == null || tanggalP.trim().isEmpty()) {
					tanggalTidakValid = true;
					ais.common.ErrorAuditUtil.record(
							new Exception("trx_date kosong/tidak dikirim oleh bank pada callback Bankaltimtara"),
							"Bankaltimtara.doProcess: trx_date kosong, callback ditolak sebelum parse tanggal (baris ~321)");
				} else {
					try {
						tanggal = Common.databaseDateFormat1.get().parse(tanggalP);
					} catch (Exception e) {
						tanggalTidakValid = true;
						ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:322");
					}
				}
				// Callback otomatis wajib tunduk pada batas waktu VA berdasarkan waktu server.
				// Parameter chekLagi hanya untuk rekonsiliasi manual yang dipicu pengguna.
				boolean kadaluarsaCallbackOtomatis = !chekLagi && !reversal && !chek
						&& sudahKadaluarsaUntukCallbackOtomatis(virtualAccountBankNtt);
				if (Double.isNaN(nominalP)) {
					// VALIDASI DEFENSIF: amount kosong/tak valid dari bank (lihat sentinel di doProses)
					// - tolak callback dengan respons error yang jelas, jangan lempar exception mentah.
					response.put("errorCode", "07");
					response.put("statusDescription", "Format nominal transaksi tidak valid");
				} else if (tanggalTidakValid) {
					response.put("errorCode", "06");
					response.put("statusDescription", "Format tanggal transaksi tidak valid");
				} else if (kadaluarsaCallbackOtomatis) {
					response.put("errorCode", "03");
					response.put("statusDescription",
							"Tagihan kadaluarsa; gunakan tombol Cek Pembayaran untuk rekonsiliasi manual");
				} else if (virtualAccountBankNtt == null) {
					response.put("errorCode", "01");
					response.put("statusDescription", "Nomor VA salah");
				}
				
				

//				else if (!reversal && !chek && virtualAccountBankNtt != null
//						&& virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
//
//					response.put("errorCode", "02");
//					response.put("statusDescription", "Tagihan tidak tersedia");
//
//				} else {

				else if (reversal && virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
						&& virtualAccountBankNtt.getKegiatan() == null
						&& virtualAccountBankNtt.getPembayaran() == null) {
					response = new JSONObject();
					response.put("errorCode", "05");
					response.put("statusDescription", "Reversal gagal");
				} else

//				if (!reversal && !chek && virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
//						&& ((virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)
//								|| virtualAccountBankNtt.getPembayaran() != null)) {
//					response.put("errorCode", "03");
//					response.put("statusDescription", "Tagihan sudah terbayar");
//				}
//
//				else 
					
					
					
					

				if ("00".equals(response.optString("errorCode"))
						&& VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, reversal,
								chek)) {
					response.put("errorCode", "03");
					response.put("statusDescription", "Tagihan sudah terbayar");
				}

				else if ("00".equals(response.optString("errorCode")) && virtualAccountBankNtt != null
						&& virtualAccountBankNtt.getTotal() > 0.1) {

					if (nominalP < 0.0) {
						nominalP = virtualAccountBankNtt.getTotal() + virtualAccountBankNtt.getBiayaAdmin();
					}

					String TahunID = "";
					try {
						TahunID = virtualAccountBankNtt.getTahunAkademik().split("/")[0];
						response.put("TahunID", TahunID);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:382");
						// TODO: handle exception
					}

					session = HibernateUtil.getSessionFactory().openSession();
					{

						Double nominal = nominalP;

						if (!inquery && nominal.intValue() != virtualAccountBankNtt.totalBiaya()) {
							response.put("errorCode", "04");
							response.put("statusDescription", "Nominal Tagihan tidak sesuai");
						} else {

							if (inquery) {

								response.put("billAmount", virtualAccountBankNtt.getTotal().intValue()
										+ virtualAccountBankNtt.getBiayaAdmin().intValue());

								if (virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
									JSONObject bill = new JSONObject();
									bill.put("billID", "0010");
									bill.put("billName", "Biaya admin");
									bill.put("billNameID", "0");
									bill.put("billAmount", virtualAccountBankNtt.getBiayaAdmin().intValue());

									bill.put("billAmountBayar", 0);
									bill.put("billAmountPay", 0);
									bill.put("TahunID", TahunID);

									billDetails.put(bill);

								}
							} else {
								response.put("amount", virtualAccountBankNtt.getTotal().intValue()
										+ virtualAccountBankNtt.getBiayaAdmin().intValue());
							}

							

							if (virtualAccountBankNtt.getSiswa() != null
									|| virtualAccountBankNtt.getCalonSiswa() != null) {

								Sekolah sekolah = null;
								if (virtualAccountBankNtt.getSiswa() != null) {
									sekolah = virtualAccountBankNtt.getSiswa().getSekolah();
									nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getSiswa().getNama();

								} else if (virtualAccountBankNtt.getCalonSiswa() != null) {
									sekolah = virtualAccountBankNtt.getCalonSiswa().getSekolah();
									nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getCalonSiswa().getNama();

								}

								response.put("Nama", nama);
								response.put("FormID", nim);
								response.put("ProdiNama", sekolah == null ? "" : sekolah.getNama());

								Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(virtualAccountBankNtt,
										session, tanggal, bank, inquery, data, false);

								for (List<Tagihan> tagihans : map.values()) {

									for (Tagihan tagihan : tagihans) {

										if (inquery) {
											JSONObject bill = new JSONObject();

											bill.put("billID", tagihan.getItemBiayaSekolah().getKode());
											bill.put("billName", tagihan.getItemBiayaSekolah().getNama());
											bill.put("billNameID", tagihan.getItemBiayaSekolah().getId() + "");

											bill.put("billAmount", tagihan.getNominal().intValue());

											bill.put("billAmountBayar", 0);
											bill.put("billAmountPay", 0);
											bill.put("TahunID", TahunID);

											billDetails.put(bill);

											if (tagihan.getDenda() > 0.1) {
												bill = new JSONObject();
												bill.put("billID", "D" + tagihan.getItemBiayaSekolah().getKode());
												bill.put("billName",
														"Denda " + tagihan.getItemBiayaSekolah().getNama());
												bill.put("billNameID", "0" + tagihan.getItemBiayaSekolah().getId());
												bill.put("billAmount", tagihan.getDenda().intValue());

												bill.put("billAmountBayar", 0);
												bill.put("billAmountPay", 0);
												bill.put("TahunID", TahunID);

												billDetails.put(bill);
											}

										}
									}

								}

							} else {

								JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
								Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
								BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
										.getBiodataCalonMahasiswa();

								Integer semester = virtualAccountBankNtt.getSemester();

								nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi() : mahasiswa.getNim();
								nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();
								Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
								if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdiLulus() != null) {
									jurusan = biodataCalonMahasiswa.getProdiLulus();
								} else if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdi1() != null) {
									jurusan = biodataCalonMahasiswa.getProdi1();
								}

								response.put("Nama", nama);
								response.put("FormID", nim);
								response.put("Gelombang",
										biodataCalonMahasiswa == null
												|| biodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
														: biodataCalonMahasiswa.getGelombangPendaftaran().getNama());
								response.put("ProdiNama", jurusan == null ? "" : jurusan.getNama());
								response.put("FakultasNama", jurusan == null ? "" : jurusan.getFakultas().getNama());

								Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
										: session.createCriteria(Kegiatan.class)
												.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan()))
												.uniqueResult());

								if (kegiatan == null || kegiatan.getId() == null) {

									kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)

											.addOrder(Order.asc("id"))

											.add(biodataCalonMahasiswa != null
													? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
													: Restrictions.eq("mahasiswa", mahasiswa))
											.add(Restrictions.eq("jenisKegiatan",
													virtualAccountBankNtt.getJenisKegiatan()))
											.add(Restrictions.eq("semster", semester))

											.setMaxResults(1).uniqueResult();
								}

								if (kegiatan == null || kegiatan.getId() == null) {
									kegiatan = new Kegiatan();
								}

								if (reversal) {

									kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
									kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
									kegiatan.setNama(nama);
									kegiatan.setUploadVirtualAccount(null);
									kegiatan.setAmount(virtualAccountBankNtt.getTotal());
									kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
									kegiatan.setMahasiswa(mahasiswa);
									kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
									kegiatan.setSemster(semester);
									kegiatan.setJenisKegiatan(jenisKegiatan);
									kegiatan.setTanggal(tanggal);
									kegiatan.setValidated(1);
									kegiatan.setValidator(bank);

									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, kegiatan);
									session.getTransaction().commit();

									virtualAccountBankNtt.setKegiatan(null);

									List<Long> detailBiayasId = new ArrayList<Long>();
									for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
										try {
											detailBiayasId.add(Long.parseLong(id.trim()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:562");

										}
									}

									Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
											.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("id", detailBiayasId))
											.list();

									Double nilaiBiayaHarusDiBayars = 0.0;
									for (DetailBiaya detailBiaya : detailBiayas) {

										DetailKegiatan detailKegiatan = kegiatan.ambilSatuDetailKegiatan(detailBiaya,
												session);
										if (detailKegiatan == null) {
											detailKegiatan = new DetailKegiatan();
										}

										Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

										detailKegiatan.setBiaya(biaya);

										detailKegiatan.setDetailBiaya(detailBiaya);
										detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
										detailKegiatan.setKegiatan(kegiatan);

										nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);

									}

									session.getTransaction().begin();
									session.createSQLQuery("delete from cicilan_pembayaran where ref_va = "
											+ virtualAccountBankNtt.getId()).executeUpdate();
									session.getTransaction().commit();

									Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session,
											kegiatan);
									Double jumlah = d[0];
									Double denda = d[1];
									kegiatan.setDenda(denda.doubleValue());
									kegiatan.setAmountTerhutang(
											nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));

									kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
											: virtualAccountBankNtt.getTotal());
									kegiatan.setValidator(bank);

									session.getTransaction().begin();
									Common.refreshUpdate(session, kegiatan);
									session.getTransaction().commit();

									VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, null, data, bank);
								} else {

									kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
									kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
									kegiatan.setNama(nama);
									kegiatan.setUploadVirtualAccount(null);
									kegiatan.setAmount(virtualAccountBankNtt.getTotal());
									kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
									kegiatan.setMahasiswa(mahasiswa);
									kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
									kegiatan.setSemster(semester);
									kegiatan.setJenisKegiatan(jenisKegiatan);
									kegiatan.setTanggal(tanggal);
									kegiatan.setValidated(1);
									kegiatan.setValidator(bank);

									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, kegiatan);
									session.getTransaction().commit();

									List<Long> detailBiayasId = new ArrayList<Long>();
									for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
										try {
											detailBiayasId.add(Long.parseLong(id.trim()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:639");

										}
									}

									Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
											.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("id", detailBiayasId))
											.list();

									Double nilaiBiayaHarusDiBayars = 0.0;
									for (DetailBiaya detailBiaya : detailBiayas) {
										Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

										nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);

									}

									Double[] totalCicilan = kegiatan.hitungTotalDanDendaFromCicilan();
									Double total = totalCicilan[0];
									Double totalTagihan = kegiatan.getAmount() + kegiatan.getAmountTerhutang();
									System.out.println(
											"cicilanPembayaran total -> " + total + " totalTagihan " + totalTagihan);

									if (virtualAccountBankNtt.getCicilan() != null
											&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
										for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(),
												",")) {
											if (Common.isNumber(idPemBul)) {
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
														.createCriteria(PengaturanPembayaranBulanan.class)
														.add(Restrictions.idEq(Long.parseLong(idPemBul)))
														.uniqueResult();

												if (pengaturanPembayaranBulanan != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();

													ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
															.getItemBiaya();

													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[spl.length - 1]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:684");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();

														bill.put("billID", itemBiaya.getKode());
														bill.put("billName", itemBiaya.getNama());
														bill.put("billNameID", itemBiaya.getId() + "");

														bill.put("billAmount", subtotal.intValue());

														bill.put("billAmountBayar", 0);
														bill.put("billAmountPay", 0);
														bill.put("TahunID", TahunID);

														billDetails.put(bill);

													} else {

														CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																.createCriteria(CicilanPembayaran.class)
																.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																.uniqueResult();

														if (cicilanPembayaran == null) {
															cicilanPembayaran = new CicilanPembayaran(
																	pengaturanPembayaranBulanan.getDetailBiaya());
														}
														cicilanPembayaran.setRef(ref);
														cicilanPembayaran.setValidator(bank);
														cicilanPembayaran.setKegiatan(kegiatan);
														cicilanPembayaran.setItemBiaya(pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya());
														cicilanPembayaran.setPengaturanPembayaranBulanan(
																pengaturanPembayaranBulanan);
														cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
														cicilanPembayaran.setNilai(subtotal);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());

														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														session.getTransaction().commit();

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya().getNama());
														jsonObjectRinci.put("bulan",
																pengaturanPembayaranBulanan.getNamaBulan());
														jsonObjectRinci.put("nominal", pengaturanPembayaranBulanan
																.ambilNominalModifikasi(mahasiswa, semester));

														rincian.put(jsonObjectRinci);

													}
												}
											} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
														.createCriteria(PengaturanPembayaranBulanan.class)
														.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
														.uniqueResult();

												if (pengaturanPembayaranBulanan != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();

													ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
															.getItemBiaya();
													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[spl.length - 1]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:771");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();

														bill.put("billID", itemBiaya.getKode());
														bill.put("billName", itemBiaya.getNama());
														bill.put("billNameID", itemBiaya.getId() + "");

														bill.put("billAmount", subtotal.intValue());

														bill.put("billAmountBayar", 0);
														bill.put("billAmountPay", 0);
														bill.put("TahunID", TahunID);

														billDetails.put(bill);

													} else {
														CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																.createCriteria(CicilanPembayaran.class)
																.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																.uniqueResult();

														if (cicilanPembayaran == null) {
															cicilanPembayaran = new CicilanPembayaran(
																	pengaturanPembayaranBulanan.getDetailBiaya());
														}
														cicilanPembayaran.setRef(ref);
														cicilanPembayaran.setValidator(bank);
														cicilanPembayaran.setKegiatan(kegiatan);
														cicilanPembayaran.setItemBiaya(itemBiaya);
														cicilanPembayaran.setPengaturanPembayaranBulanan(
																pengaturanPembayaranBulanan);
														cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

														cicilanPembayaran.setNilai(subtotal);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														session.getTransaction().commit();

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya().getNama());
														jsonObjectRinci.put("bulan",
																pengaturanPembayaranBulanan.getNamaBulan());
														jsonObjectRinci.put("nominal", subtotal);

														rincian.put(jsonObjectRinci);
													}
												}

											} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
												ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
														.simpleObject(
																session.createCriteria(ItemBiaya.class)
																		.add(Restrictions.idEq(Long
																				.parseLong(idPemBul.split("-")[1]))),
																ItemBiaya.class);

												if (itemBiaya != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();
													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[2]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:854");
													}

													Long detailBiayaId = null;
													try {
														String[] spl = idPemBul.split("-");
														detailBiayaId = Long.parseLong(spl[4]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:861");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();

														bill.put("billID", itemBiaya.getKode());
														bill.put("billName", itemBiaya.getNama());
														bill.put("billNameID", itemBiaya.getId() + "");

														bill.put("billAmount", subtotal.intValue());

														bill.put("billAmountBayar", 0);
														bill.put("billAmountPay", 0);
														bill.put("TahunID", TahunID);

													} else {

														CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																.createCriteria(CicilanPembayaran.class)
																.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																.uniqueResult();

														if (cicilanPembayaran == null) {
															cicilanPembayaran = new CicilanPembayaran(
																	DetailBiaya.muatRefAman(session, detailBiayaId));
														}
														cicilanPembayaran.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
														cicilanPembayaran.setRef(ref);
														cicilanPembayaran.setValidator(bank);
														cicilanPembayaran.setKegiatan(kegiatan);
														cicilanPembayaran.setItemBiaya(itemBiaya);
														cicilanPembayaran.setPengaturanPembayaranBulanan(null);
														cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

														cicilanPembayaran.setNilai(subtotal);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														session.getTransaction().commit();

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", itemBiaya.getNama());
														jsonObjectRinci.put("bulan", "");
														jsonObjectRinci.put("nominal", subtotal);

														rincian.put(jsonObjectRinci);
													}

												}
											} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
												// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
												// menjadi Kegiatan+Cicilan nyata â€” pemroses terpusat yang sama dengan Esmartlink.
												ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
														virtualAccountBankNtt, inquery, bank, bankHost, tanggal, data, null);
											}
										}
									}

									response.put("numBill", billDetails.length());

									if (reversal) {
										response = new JSONObject();
										response.put("errorCode", "00");
										response.put("statusDescription", "Success");
									}

									else if (inquery) {

									} else {

										response.put("UserLogin", "");
										response.put("Password", "");

										Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session,
												kegiatan);
										Double jumlah = d[0];
										Double denda = d[1];
										kegiatan.setDenda(denda.doubleValue());
										kegiatan.setAmountTerhutang(
												nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));

										kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
												: virtualAccountBankNtt.getTotal());
										kegiatan.setValidator(bank);

										session.getTransaction().begin();
										Common.refreshUpdate(session, kegiatan);
										session.getTransaction().commit();

										VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
												bank);

									}

								}
							}

							response = new JSONObject();
							response.put("code", "00");
							response.put("message", "Success");
						}
					}

				}
//				}
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();

				if (reversal) {
					response = new JSONObject();
					response.put("errorCode", "05");
					response.put("statusDescription", "Reversal gagal");
				} else {

					response.put("errorCode", "91");
					response.put("statusDescription", "Link Down");
				}
				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:1000");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:1001");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:1002");}
				}
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						response.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		return response;
	}

	/**
	 * Mengurai payload callback Bank Kaltimtara menjadi parameter terpisah,
	 * memvalidasi kelengkapannya, lalu memanggil {@link #doProcess}.
	 *
	 * <h3>Dua bentuk payload</h3>
	 * <ul>
	 * <li><b>VA</b> &mdash; {@code number} sebagai nomor VA (cadangan: parameter
	 * query bernama sama) dan {@code amount} sebagai nominal.</li>
	 * <li><b>QRIS</b> &mdash; kehadiran {@code kd_tagihan} menimpa nomor VA dan
	 * menyetel nominal ke sentinel {@code -1.0}, yang di {@link #doProcess} berarti
	 * "pakai total tagihan menurut sistem" karena kanal ini tidak mengirim nominal
	 * terpisah.</li>
	 * </ul>
	 *
	 * <h3>Validasi defensif</h3>
	 * {@code amount} kosong atau tidak dapat diurai tidak lagi melempar
	 * {@code NumberFormatException} melainkan disetel ke sentinel
	 * {@code Double.NaN}. Sentinel itu sengaja dibedakan dari nilai negatif supaya
	 * "nominal rusak" tidak tertukar dengan "pakai total tagihan". Callback dengan
	 * {@code amount} rusak atau {@code trx_date} kosong ditolak lebih awal dengan
	 * {@link #errorDb} tanpa pernah menyentuh basis data.
	 *
	 * <p>
	 * Mode {@code reversal} dikenali dari URI request yang berakhiran
	 * {@code BankaltimtaraReversal}. Callback publik selalu dipanggil dengan
	 * {@code chekLagi = false} dan {@code inquery = false}, sehingga VA kedaluwarsa
	 * wajib ditolak di jalur ini.
	 *
	 * @param data     payload JSON mentah; boleh {@code null}
	 * @param request  request HTTP asli; boleh {@code null} pada rekonsiliasi manual
	 * @param bankHost baris {@link BankHost} hasil pencocokan IP; boleh {@code null}
	 * @param bank     nama bank pembayar, di endpoint ini {@code "BMS"}
	 * @param chek     {@code true} bila dipicu admin dari menu rekonsiliasi
	 * @return badan respons JSON sebagai {@link String}
	 * @throws Exception bila penguraian JSON tingkat akar gagal atau tidur simulasi
	 *                   timeout diinterupsi
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);

		String va = req == null || req.isNull("number") ? (request == null ? "" : request.getParameter("number"))
				: req.getString("number");

		double nominalP = 0.0;
		if (req != null && !req.isNull("amount")) {
			// VALIDASI DEFENSIF: bank kadang mengirim amount kosong ("")
			// (callback duplikat/retry/health-check) - jangan langsung parseDouble mentah.
			// Sentinel Double.NaN dipakai supaya tak bentrok dengan nominalP<0.0 (arti lain: pakai total tagihan).
			String amountStr = req.get("amount") + "";
			if (amountStr.trim().isEmpty()) {
				nominalP = Double.NaN;
				System.out.println("Callback Bankaltimtara ditolak: amount kosong.");
			} else {
				try {
					nominalP = Double.parseDouble(amountStr);
				} catch (Exception e) {
					nominalP = Double.NaN;
					System.out.println("Callback Bankaltimtara ditolak: amount tidak valid. " + e.getMessage());
				}
			}
		}

		if (req != null && !req.isNull("kd_tagihan")) {
			va = req.get("kd_tagihan") + "";
			nominalP = -1.0;
		}

		boolean reversal = false;
		if (request != null && request.getRequestURI().endsWith("BankaltimtaraReversal")) {
			reversal = true;
		}

		String tanggalP = null;
		if (req != null && !req.isNull("trx_date")) {
			tanggalP = req.get("trx_date") + "";
		}
		if (Double.isNaN(nominalP) || tanggalP == null || tanggalP.trim().length() == 0) {
			System.out.println("Callback Bankaltimtara ditolak: amount/trx_date wajib belum lengkap.");
			return errorDb(false, data);
		}

		String body;
		try {
			// Callback publik bukan rekonsiliasi manual: VA kedaluwarsa wajib ditolak.
			body = Bankaltimtara
					.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, false, false, reversal, chek)
					.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bankaltimtara.java:1047");
			body = errorDb(false, data);

		}

		if (Common.bolehKonfigurasi("bankaltimtara_va_sleep", Konfigurasi.TIDAK_AKTIF)) {
			Thread.sleep(3 * 1000);
			body = timeoutDb(req.isNull("amount"), data);
		}

		System.out.println("response->" + body);

		return body;
	}

	/**
	 * Titik masuk bersama {@link #doGet} dan {@link #doPost}: membaca badan request
	 * sebagai teks, mengenali pemanggil, memanggil {@link #doProses}, lalu menulis
	 * hasilnya sebagai {@code application/json}.
	 *
	 * <h3>Pengenalan pemanggil &mdash; catatan penting</h3>
	 * Pemanggil dikenali <b>hanya</b> lewat
	 * {@link PembayaranUtil#getBankHost(String, String)} dengan alamat
	 * {@link HttpServletRequest#getRemoteAddr()} dan nama bank {@code "BMS"}. Hal-hal
	 * yang perlu disadari pembaca kode:
	 * <ul>
	 * <li><b>Tidak ada verifikasi kriptografis pada jalur masuk.</b> Kredensial
	 * gateway yang dimiliki instalasi ini hanya dipakai ke arah keluar, yaitu untuk
	 * memperoleh bearer token pada {@link #checkPakaiva}/{@link #checkPakaiqris}.
	 * Payload callback yang membukukan uang tidak disertai maupun diperiksa tanda
	 * tangan, MAC, atau token apa pun. Bandingkan dengan {@link Bniresponse}, yang
	 * mendekode payload memakai kunci bersama sehingga pemeriksaannya benar-benar
	 * berlaku pada cabang transaksi.</li>
	 * <li><b>Hasil pencocokan IP tidak menjadi gerbang.</b> {@code bankHost} yang
	 * {@code null} tetap diteruskan dan pembukuan berjalan penuh.</li>
	 * <li>Varian {@code getBankHost} yang dipakai memakai alamat soket langsung,
	 * <b>bukan</b> header {@code X-Forwarded-For}/{@code CF-Connecting-IP}, sehingga
	 * IP tidak dapat dipalsukan lewat header oleh pemanggil.</li>
	 * <li>Di sisi {@link PembayaranUtil} masih berlaku dua pelonggaran umum:
	 * pembuatan otomatis baris {@link BankHost} bila konfigurasi
	 * {@code apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis} aktif, dan
	 * cadangan ke baris ber-IP {@code 0.0.0.0} yang cocok untuk alamat mana pun.</li>
	 * </ul>
	 * Dengan demikian perlindungan endpoint masuk ini bertumpu pada pembatasan
	 * jaringan di depan aplikasi, bukan pada pemeriksaan di dalam kode.
	 *
	 * @param request  request HTTP dari gateway bank
	 * @param response respons yang akan diisi badan JSON beserta header
	 *                 {@code length} dan {@code Content-Type}
	 * @throws Exception bila pembacaan badan request atau penulisan respons gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Read from request

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

		String bank = "BMS";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = doProses(data, request, bankHost, bank, false);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
		writer.write(body);
		writer.flush();

	}

	/**
	 * Menyusun respons baku bertanda <b>timeout</b> ({@code errorCode 68},
	 * {@code "Connection Timeout"}), dipakai hanya oleh sakelar uji
	 * {@code bankaltimtara_va_sleep} pada {@link #doProses}.
	 *
	 * <p>
	 * Berbeda dengan {@link Mandiri#timeoutDb}, respons di sini dibangun <b>di atas
	 * payload permintaan</b>: objek JSON asli dari bank disalin lalu ditimpa field
	 * status, mengikuti bentuk protokol Bank Kaltimtara yang mengharapkan seluruh
	 * field permintaan dikembalikan.
	 *
	 * @param inquery penanda mode inquiry; disediakan demi keseragaman tanda tangan
	 *                dan tidak memengaruhi hasil
	 * @param data    payload JSON mentah dari bank yang menjadi dasar respons
	 * @return badan JSON siap kirim
	 * @throws Exception bila {@code data} bukan JSON yang sah
	 */
	private static String timeoutDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "68");
		jsonObjectResponse.put("statusDescription", "Connection Timeout");
		return jsonObjectResponse.toString();
	}

	/**
	 * Menyusun respons baku bertanda <b>gagal</b> ({@code errorCode 91},
	 * {@code "Link Down"}), dipakai {@link #doProses} baik sebagai penolakan dini
	 * ketika {@code amount}/{@code trx_date} tidak lengkap, maupun sebagai jaring
	 * pengaman ketika {@link #doProcess} melempar exception.
	 *
	 * <p>
	 * Sama seperti {@link #timeoutDb}, respons dibangun di atas payload permintaan:
	 * objek JSON asli disalin lalu ditimpa field status.
	 *
	 * @param inquery penanda mode inquiry; disediakan demi keseragaman tanda tangan
	 *                dan tidak memengaruhi hasil
	 * @param data    payload JSON mentah dari bank yang menjadi dasar respons
	 * @return badan JSON siap kirim
	 * @throws Exception bila {@code data} bukan JSON yang sah
	 */
	private static String errorDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "91");
		jsonObjectResponse.put("statusDescription", "Link Down");
		return jsonObjectResponse.toString();
	}
}
