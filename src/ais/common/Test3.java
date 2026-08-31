package ais.common;

/**
 * Kelas scratch/uji coba manual (bukan bagian dari alur bisnis AIS dan bukan unit test formal
 * — tidak memakai JUnit/assert apa pun) yang dipakai pengembang untuk mencoba secara cepat
 * pembentukan tanda tangan HMAC-SHA512 lewat {@link Common#buildHmacSignature(String, String)},
 * kemungkinan besar untuk keperluan integrasi dengan API pihak ketiga bergaya perbankan/payment
 * gateway (pola {@code payload;create_va:<nomor>} menyerupai skema pembuatan Virtual Account).
 *
 * <p>
 * Sebagian besar isi method {@code main} berupa baris kode yang dikomentari ({@code //}) —
 * sisa-sisa percobaan sebelumnya (mis. parsing header {@code Authorization: Bearer ...},
 * penghitungan HMAC manual memakai {@code javax.crypto.Mac}/{@code Hashing} Guava, hingga
 * percobaan mem-parse dokumen Google Docs Viewer lewat Jsoup) yang ditinggalkan sebagai
 * jejak eksplorasi dan referensi cepat, bukan kode yang dieksekusi.
 * </p>
 *
 * <p>
 * <b>PERINGATAN KEAMANAN:</b> variabel {@code signatureKey} pada method {@link #main(String[])}
 * berisi string yang menyerupai <i>signature key</i>/secret rahasia yang ditulis langsung
 * (hardcoded) di kode sumber. Bila nilai ini adalah kredensial aktif milik integrasi pihak
 * ketiga sungguhan (bukan sekadar nilai contoh/dummy dari dokumentasi API), maka ini adalah
 * kebocoran rahasia di dalam repositori kode dan sebaiknya dicabut/diputar (rotate) oleh pihak
 * yang berwenang serta dipindahkan ke konfigurasi/secret store di luar kode sumber. Javadoc ini
 * TIDAK mengubah nilai tersebut — lihat catatan pada {@link #main(String[])} dan ringkasan
 * laporan terkait untuk detail lokasi baris.
 * </p>
 */
public class Test3 {



	/**
	 * Titik masuk uji coba manual. Membentuk sebuah {@code payload} contoh (pola
	 * {@code "<random>;create_va:<nomor>"}) dan menandatanganinya dengan
	 * {@link Common#buildHmacSignature(String, String)} memakai {@code signatureKey} yang
	 * ditulis langsung di kode (lihat peringatan keamanan pada javadoc kelas), lalu mencetak
	 * hasil tanda tangan ke konsol untuk diperiksa manual oleh pengembang. Baris-baris lain
	 * yang dikomentari adalah percobaan alternatif yang tidak dieksekusi.
	 *
	 * @param args argumen baris perintah, tidak dipakai
	 * @throws Exception diteruskan apa adanya dari {@link Common#buildHmacSignature(String, String)}
	 *                    (mis. kegagalan algoritma HMAC tidak tersedia di JVM)
	 */
	public static void main(String[] args) throws Exception {

//		String Authorization = "Bearer 07765ab7-a18c-42e8-a4e8-b4484b3230c9";
//		String tokenReq = Authorization.split(" ")[1].trim();

//		System.out.println("==> tokenReq " + tokenReq);

		String signatureKey = "0CcfEADwiAssIGQ6AMiWbiP9VHI0zzrBu4WUKfY1bNEF9q3FZJ";

		String payload = "pKMUSJfL5G8wKHSbhoTU7PQ3TJdX0HlV;create_va:1234567885";

//		String signature = Hashing.hmacSha512(strClientScret_bca.getBytes()).newHasher()
//				.putString(StringToSign, StandardCharsets.UTF_8).hash().toString();

//		Mac HmacSHA512 = Mac.getInstance("HmacSHA512");
//		SecretKeySpec secret_key = new SecretKeySpec(strClientScret_bca.getBytes(),
//				"HmacSHA512");
//		HmacSHA512.init(secret_key);
//
//		String signature = org.apache.commons.codec.binary.Base64
//				.encodeBase64String(HmacSHA512.doFinal(payload.getBytes()));
//		

		String signature = Common.buildHmacSignature(payload, signatureKey);

//		Mac HmacSHA512 = Mac.getInstance("HmacSHA512");
//		SecretKeySpec secret_key = new SecretKeySpec(strClientScret_bca.getBytes(), "HmacSHA512");
//		HmacSHA512.init(secret_key);
//
//		String signature = Base64.encodeBase64String(HmacSHA512.doFinal(StringToSign.getBytes()));

		System.out.println("==> signature " + signature);
//		Document doc = Jsoup.parse(
//				new URL("https://docs.google.com/viewerng/viewer?url=http://123.231.135.102/SIM/Mg-3.ppt"), 5000);
//
//		String title = doc.select("title").text();
//		System.out.println(title);
	}

}
