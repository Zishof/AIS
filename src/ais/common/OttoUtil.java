package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.json.JSONObject;

import com.google.common.hash.Hashing;

import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

//import org.json.JSONObject;

/**
 * Klien integrasi payment gateway OTTO (OttoPay) untuk membuat token transaksi virtual account
 * (VA) online bagi empat kategori pembayar yang dikenal AIS: mahasiswa aktif ({@link Mahasiswa}),
 * calon mahasiswa/pendaftar ({@link BiodataCalonMahasiswa}), siswa sekolah aktif ({@link Siswa}),
 * dan calon siswa/pendaftar sekolah ({@link CalonSiswa}). Kelas ini menyediakan empat overload
 * publik {@link #post} — satu per kategori pembayar — yang seluruhnya menyusun payload JSON
 * permintaan token dengan struktur identik ({@code customerDetails} + {@code transactionDetails})
 * namun mengambil data identitas (email, nama, nomor identitas sebagai {@code lastName}, nomor HP)
 * dari entitas yang berbeda, lalu bermuara pada satu implementasi privat kanonik {@link
 * #doPost(String, VirtualAccountBank)} yang benar-benar menandatangani dan mengirim permintaan ke
 * OttoPay.
 *
 * <h2>Alur permintaan token (di {@link #doPost})</h2>
 * <ol>
 * <li>Body JSON mentah yang sudah disusun pemanggil disimpan apa adanya ke
 * {@link VirtualAccountBank#setRequest(String)} untuk keperluan audit/rekonsiliasi.</li>
 * <li>Body tersebut "dibersihkan" ({@code b}) dengan membuang seluruh karakter selain huruf,
 * angka, {@code { } : . ,} — bentuk ringkas inilah yang dipakai sebagai bahan tanda tangan, BUKAN
 * body asli yang dikirim ke API (body asli tetap dikirim utuh via {@code --data-raw}).</li>
 * <li>Kredensial ({@code otto_api_key}/{@code otto_mid}) dan URL endpoint token
 * ({@code otto_token_url}) dibaca dari konfigurasi terpusat AIS lewat
 * {@code Common.getKonfigurasi(String, String)}, dengan nilai default yang tertanam di kode sumber
 * bila konfigurasi belum diisi — lihat peringatan keamanan di bawah.</li>
 * <li>Timestamp Unix (epoch milliseconds) diambil lalu tiga digit terakhirnya dipotong (efektif
 * epoch second dengan presisi turun), dipakai sebagai bagian dari tanda tangan dan header
 * {@code Timestamp}.</li>
 * <li>Tanda tangan (signature) dihitung sebagai HMAC-SHA512 atas string
 * {@code lower(bodyBersih) + "&" + timestamp + "&" + apiKey}, dengan {@code apiKey} itu sendiri
 * dipakai sebagai kunci HMAC (lewat Guava {@link Hashing#hmacSha512(byte[])}).</li>
 * <li>MID (merchant id) di-encode Base64 dan dikirim sebagai header {@code Authorization: Basic
 * <MID base64>} — perhatikan ini BUKAN skema HTTP Basic Auth standar (yang seharusnya
 * {@code base64(user:password)}), melainkan MID polos yang di-Base64-kan sendiri, sekadar
 * memanfaatkan nama header {@code Authorization: Basic} sebagai konvensi API OttoPay.</li>
 * <li>Permintaan HTTP POST dijalankan lewat proses eksternal {@code curl} (bukan pustaka HTTP
 * Java), dengan seluruh header (Signature/Timestamp/Authorization/Content-Type) dan body
 * ({@code --data-raw}) sebagai argumen baris perintah — isi permintaan (termasuk signature dan
 * MID ter-encode) TIDAK LAGI dicetak ke {@code System.out} (lihat riwayat keamanan di bawah), namun
 * tetap berpotensi terlihat di daftar proses OS selama {@code curl} berjalan (keterbatasan
 * arsitektur yang belum diperbaiki pada commit ini).</li>
 * <li>Output {@code curl} (stdout gabungan) dibaca penuh, disimpan ke
 * {@link VirtualAccountBank#setResponse(String)} untuk audit, lalu diparsing sebagai
 * {@link JSONObject} dan dikembalikan ke pemanggil.</li>
 * </ol>
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-02)</b> — pada baris pembacaan konfigurasi di
 * {@link #doPost}, nilai default yang dipakai bila kunci {@code otto_api_key} atau
 * {@code otto_mid} belum diisi di konfigurasi SEBELUMNYA ditulis langsung sebagai literal string
 * RAHASIA di kode sumber kelas ini ({@code apiKey} default {@code "KP33PP0EE0AAP1EE1009010PP01I91OA"},
 * {@code MID} default {@code "OP1E00030999"}) — nilai IDENTIK dengan yang sebelumnya tertanam di
 * {@link OttoTest} (skrip uji coba terpisah, sudah diperbaiki lebih dulu). Default itu sudah
 * dihapus (kini string kosong); {@link #doPost} kini melempar {@link IllegalStateException} bila
 * {@code otto_api_key}/{@code otto_mid} belum diisi di konfigurasi, alih-alih diam-diam memakai
 * kredensial tertanam sebagai fallback produksi. Baris {@code System.out.println} yang sebelumnya
 * mencetak seluruh argumen command-line {@code curl} (berisi signature dan MID ter-encode Base64)
 * serta body respons OttoPay juga sudah dihapus, karena berpotensi membocorkan bahan autentikasi
 * transaksi lewat log server.
 * </p>
 *
 * <p>
 * <b>TINDAK LANJUT DI LUAR PERUBAHAN KODE INI</b>: {@code apiKey}/{@code MID} yang sebelumnya
 * tertanam sudah lama berada di riwayat SVN dan WAJIB dianggap bocor — perlu dirotasi di sisi
 * OttoPay bila masih aktif di produksi, terlepas dari default URL endpoint yang mengarah ke
 * subdomain {@code dev-secure.ottopay.id} (tampak sebagai lingkungan sandbox/uji coba).
 * </p>
 */
public class OttoUtil {

	/**
	 * Membuat token transaksi VA online OttoPay untuk pembayaran oleh mahasiswa aktif. Menyusun
	 * payload dari data {@code mahasiswa} (email, nama, NIM sebagai {@code lastName}, nomor HP) dan
	 * nama perguruan tinggi (diturunkan dari rantai relasi jurusan-fakultas-perguruan tinggi milik
	 * mahasiswa) sebagai {@code merchantName}, dengan {@code orderId} dibangkitkan dari
	 * {@link Common#getGeneratedBarCode()}. Delegasi akhir ke {@link #doPost(String,
	 * VirtualAccountBank)} yang menandatangani dan mengirim permintaan sesungguhnya.
	 *
	 * @param mahasiswa                 mahasiswa yang melakukan pembayaran, sumber data identitas
	 * @param nominal                   nominal transaksi dalam Rupiah (IDR)
	 * @param virtualAccountBankOnline  entitas VA yang akan diisi request/response mentah untuk audit
	 * @return respons token dari OttoPay dalam bentuk {@link JSONObject}
	 * @throws Exception diteruskan dari kegagalan proses {@code curl} eksternal atau parsing JSON
	 *                    respons di {@link #doPost}
	 */
	public static JSONObject post(Mahasiswa mahasiswa, int nominal, VirtualAccountBank virtualAccountBankOnline)
			throws Exception {

		String bodyS = "{\"customerDetails\":{\"email\":\"" + mahasiswa.getEmail() + "\"," + "\"firstName\":\""
				+ mahasiswa.getNama() + "\",\"lastName\":\"" + mahasiswa.getNim() + "\",\"phone\":\""
				+ mahasiswa.ambilHp() + "\"}," + "\"transactionDetails\":{\"amount\":" + nominal
				+ ",\"currency\":\"IDR\"," + "\"merchantName\":\""
				+ mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama() + "\",\"orderId\":\""
				+ Common.getGeneratedBarCode() + "\",\"vaOrderId\":\"\"}}";
		return doPost(bodyS, virtualAccountBankOnline);
	}

	/**
	 * Seperti {@link #post(Mahasiswa, int, VirtualAccountBank)}, untuk pembayaran oleh calon
	 * mahasiswa/pendaftar. Nomor registrasi pendaftaran dipakai sebagai {@code lastName}, dan nama
	 * perguruan tinggi diambil dari gelombang pendaftaran milik {@code biodataCalonMahasiswa}
	 * sebagai {@code merchantName}.
	 *
	 * @param biodataCalonMahasiswa    calon mahasiswa yang melakukan pembayaran
	 * @param nominal                  nominal transaksi dalam Rupiah (IDR)
	 * @param virtualAccountBankOnline entitas VA yang akan diisi request/response mentah untuk audit
	 * @return respons token dari OttoPay dalam bentuk {@link JSONObject}
	 * @throws Exception diteruskan dari {@link #doPost(String, VirtualAccountBank)}
	 */
	public static JSONObject post(BiodataCalonMahasiswa biodataCalonMahasiswa, int nominal,
			VirtualAccountBank virtualAccountBankOnline) throws Exception {

		String bodyS = "{\"customerDetails\":{\"email\":\"" + biodataCalonMahasiswa.getEmail() + "\","
				+ "\"firstName\":\"" + biodataCalonMahasiswa.getNama() + "\",\"lastName\":\""
				+ biodataCalonMahasiswa.getNoRegistrasi() + "\",\"phone\":\"" + biodataCalonMahasiswa.ambilHp() + "\"},"
				+ "\"transactionDetails\":{\"amount\":" + nominal + ",\"currency\":\"IDR\"," + "\"merchantName\":\""
				+ biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi().getNama() + "\",\"orderId\":\""
				+ Common.getGeneratedBarCode() + "\",\"vaOrderId\":\"\"}}";
		return doPost(bodyS, virtualAccountBankOnline);
	}

	/**
	 * Seperti {@link #post(Mahasiswa, int, VirtualAccountBank)}, untuk pembayaran oleh siswa sekolah
	 * aktif. Nomor induk siswa nasional dipakai sebagai {@code lastName}, dan nama sekolah (dari
	 * relasi {@code siswa.getSekolah()}) sebagai {@code merchantName}.
	 *
	 * @param siswa                    siswa yang melakukan pembayaran
	 * @param nominal                  nominal transaksi dalam Rupiah (IDR)
	 * @param virtualAccountBankOnline entitas VA yang akan diisi request/response mentah untuk audit
	 * @return respons token dari OttoPay dalam bentuk {@link JSONObject}
	 * @throws Exception diteruskan dari {@link #doPost(String, VirtualAccountBank)}
	 */
	public static JSONObject post(Siswa siswa, int nominal, VirtualAccountBank virtualAccountBankOnline)
			throws Exception {

		String bodyS = "{\"customerDetails\":{\"email\":\"" + siswa.getAlamatEmail() + "\"," + "\"firstName\":\""
				+ siswa.getNama() + "\",\"lastName\":\"" + siswa.getNomorIndukNasional() + "\",\"phone\":\""
				+ siswa.ambilHp() + "\"}," + "\"transactionDetails\":{\"amount\":" + nominal + ",\"currency\":\"IDR\","
				+ "\"merchantName\":\"" + siswa.getSekolah().getNama() + "\",\"orderId\":\""
				+ Common.getGeneratedBarCode() + "\",\"vaOrderId\":\"\"}}";
		return doPost(bodyS, virtualAccountBankOnline);
	}

	/**
	 * Seperti {@link #post(Mahasiswa, int, VirtualAccountBank)}, untuk pembayaran oleh calon
	 * siswa/pendaftar sekolah. Nomor registrasi pendaftaran dipakai sebagai {@code lastName}, dan
	 * nama sekolah sebagai {@code merchantName}.
	 *
	 * @param calonSiswa               calon siswa yang melakukan pembayaran
	 * @param nominal                  nominal transaksi dalam Rupiah (IDR)
	 * @param virtualAccountBankOnline entitas VA yang akan diisi request/response mentah untuk audit
	 * @return respons token dari OttoPay dalam bentuk {@link JSONObject}
	 * @throws Exception diteruskan dari {@link #doPost(String, VirtualAccountBank)}
	 */
	public static JSONObject post(CalonSiswa calonSiswa, int nominal, VirtualAccountBank virtualAccountBankOnline)
			throws Exception {

		String bodyS = "{\"customerDetails\":{\"email\":\"" + calonSiswa.getAlamatEmail() + "\"," + "\"firstName\":\""
				+ calonSiswa.getNama() + "\",\"lastName\":\"" + calonSiswa.getNoRegistrasi() + "\",\"phone\":\""
				+ calonSiswa.ambilHp() + "\"}," + "\"transactionDetails\":{\"amount\":" + nominal
				+ ",\"currency\":\"IDR\"," + "\"merchantName\":\"" + calonSiswa.getSekolah().getNama()
				+ "\",\"orderId\":\"" + Common.getGeneratedBarCode() + "\",\"vaOrderId\":\"\"}}";
		return doPost(bodyS, virtualAccountBankOnline);
	}

	/**
	 * Implementasi kanonik seluruh keluarga {@code post}: menandatangani payload JSON yang sudah
	 * disusun pemanggil (HMAC-SHA512), menyiapkan header otentikasi OttoPay, mengirim permintaan
	 * lewat proses eksternal {@code curl}, dan mengembalikan respons sebagai JSON. Lihat javadoc
	 * kelas untuk uraian langkah demi langkah proses penandatanganan dan peringatan keamanan terkait
	 * kredensial default tertanam.
	 *
	 * @param bodyS                    body JSON permintaan token, sudah disusun lengkap oleh salah
	 *                                 satu overload {@code post}
	 * @param virtualAccountBankOnline entitas VA yang diisi request mentah (sebelum kirim) dan
	 *                                 response mentah (setelah kirim) untuk keperluan audit
	 * @return respons {@code curl} yang diparsing sebagai {@link JSONObject}
	 * @throws Exception bila proses {@code curl} gagal dijalankan/dibaca, atau bila output yang
	 *                    diterima bukan JSON valid
	 */
	private static JSONObject doPost(String bodyS, VirtualAccountBank virtualAccountBankOnline) throws Exception {

		virtualAccountBankOnline.setRequest(bodyS);

		String b = bodyS.replaceAll("[^a-zA-Z0-9{}:.,]", "");

		String apiKey = Common.getKonfigurasi("otto_api_key", "").getNilai();
		String MID = Common.getKonfigurasi("otto_mid", "").getNilai();
		if (apiKey == null || apiKey.trim().isEmpty() || MID == null || MID.trim().isEmpty()) {
			throw new IllegalStateException(
					"Kredensial OttoPay belum dikonfigurasi. Isi konfigurasi otto_api_key dan otto_mid.");
		}
		String currentTimestamp = Instant.now().toEpochMilli() + "";
		currentTimestamp = currentTimestamp.substring(0, currentTimestamp.length() - 3);
		String valueToDigest = b.trim().toLowerCase() + "&" + currentTimestamp + "&" + apiKey;

		String encodedMID = Base64.getEncoder().encodeToString(MID.getBytes());
		String signature = Hashing.hmacSha512(apiKey.getBytes()).newHasher()
				.putString(valueToDigest, StandardCharsets.UTF_8).hash().toString();
		String url = Common
				.getKonfigurasi("otto_token_url", "https://dev-secure.ottopay.id/payment-services/v2.1.0/api/token")
				.getNilai();

		String[] command = { "curl", "--location", "--request", "POST", url, "--header", "Signature: " + signature,
				"--header", "Timestamp: " + currentTimestamp, "--header", "Authorization: Basic " + encodedMID,
				"--header", "Content-Type: application/json", "--data-raw", bodyS };

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
		virtualAccountBankOnline.setResponse(hasil);
		return new JSONObject(hasil);
	}

}
