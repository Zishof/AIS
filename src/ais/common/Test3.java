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
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-01):</b> variabel {@code signatureKey} pada method
 * {@link #main(String[])} sebelumnya berisi string yang menyerupai <i>signature key</i>/secret
 * rahasia yang ditulis langsung (hardcoded) di kode sumber. Karena kelas ini terverifikasi tidak
 * dipanggil oleh kode aplikasi lain (hanya dapat dijalankan manual lewat {@code main}), nilai itu
 * kini diambil dari system property ({@code -Dtest3.signaturekey=...}) alih-alih tertanam di kode,
 * dengan {@link #main} langsung berhenti dan menampilkan petunjuk pemakaian bila belum diisi.
 * Baris {@code System.out.println} yang sebelumnya mencetak hasil signature juga sudah dihapus.
 * Nilai lama yang sebelumnya tertanam ({@code "0CcfEADwiAssIGQ6AMiWbiP9VHI0zzrBu4WUKfY1bNEF9q3FZJ"})
 * sudah lama berada di riwayat SVN dan WAJIB dianggap bocor bila merupakan kredensial aktif milik
 * integrasi pihak ketiga sungguhan — perlu dirotasi oleh pihak yang berwenang.
 * </p>
 */
public class Test3 {



	/**
	 * Titik masuk uji coba manual. Membaca {@code signatureKey} dari system property (berhenti
	 * dengan pesan bila belum diisi), membentuk sebuah {@code payload} contoh (pola
	 * {@code "<random>;create_va:<nomor>"}) dan menandatanganinya dengan
	 * {@link Common#buildHmacSignature(String, String)}, lalu mencetak hasil tanda tangan ke
	 * konsol untuk diperiksa manual oleh pengembang. Baris-baris lain yang dikomentari adalah
	 * percobaan alternatif yang tidak dieksekusi.
	 *
	 * @param args argumen baris perintah, tidak dipakai
	 * @throws Exception diteruskan apa adanya dari {@link Common#buildHmacSignature(String, String)}
	 *                    (mis. kegagalan algoritma HMAC tidak tersedia di JVM)
	 */
	public static void main(String[] args) throws Exception {

//		String Authorization = "Bearer 07765ab7-a18c-42e8-a4e8-b4484b3230c9";
//		String tokenReq = Authorization.split(" ")[1].trim();

//		System.out.println("==> tokenReq " + tokenReq);

		String signatureKey = System.getProperty("test3.signaturekey", "");
		if (signatureKey.trim().isEmpty()) {
			System.out.println("Jalankan dengan -Dtest3.signaturekey=...");
			return;
		}

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

		System.out.println("==> signature dihitung, panjang " + signature.length() + " karakter");
//		Document doc = Jsoup.parse(
//				new URL("https://docs.google.com/viewerng/viewer?url=http://123.231.135.102/SIM/Mg-3.ppt"), 5000);
//
//		String title = doc.select("title").text();
//		System.out.println(title);
	}

}
