package ais.common.sinta;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.zkoss.zul.Label;

import ais.database.model.Dosen;

/**
 * Web crawler/scraper yang mengambil daftar publikasi (artikel/dokumen sitasi) milik seorang
 * dosen dari situs <b>SINTA</b> (Science and Technology Index, layanan indeksasi publikasi ilmiah
 * milik Kemdikbudristek — {@code http://sinta.ristekbrin.go.id}), menggunakan pustaka Jsoup untuk
 * mengunduh dan mem-parsing halaman HTML publik situs tersebut (bukan lewat API resmi).
 *
 * <p>
 * Alur kerja utama ada di {@link #populateData(JSONArray, String, int, Label, Dosen)}: method ini
 * mengunduh halaman detail penulis SINTA untuk satu {@code kode} (id penulis SINTA milik dosen)
 * pada satu nomor halaman (paging), mem-parsing setiap blok deskripsi artikel
 * ({@code dl.uk-description-list-line}) menjadi objek {@link JSONObject} berisi judul, tautan,
 * penulis, nama jurnal, halaman, volume, issue, dan tahun terbit, lalu <b>memanggil dirinya
 * sendiri secara rekursif</b> untuk halaman berikutnya ({@code page + 1}) sampai suatu halaman
 * tidak lagi mengandung blok artikel (daftar {@code articles} kosong), yang menjadi kondisi henti
 * rekursi. Progres pengambilan ditampilkan ke pengguna secara langsung lewat komponen ZK
 * {@link Label} yang diberikan pemanggil.
 * </p>
 *
 * <h2>Kerapuhan terhadap perubahan situs eksternal</h2>
 * <p>
 * Karena seluruh ekstraksi data bergantung pada struktur HTML statis situs SINTA (nama kelas CSS
 * seperti {@code uk-description-list-line}, {@code uk-text-primary}, {@code indexed-by}, serta
 * asumsi format teks {@code indexedby} yang dipisah karakter {@code "|"}, {@code ","}, dan
 * {@code ":"} pada posisi indeks tetap), crawler ini SANGAT RENTAN terhadap perubahan tata letak
 * atau markup situs SINTA di luar kendali AIS. Perubahan sekecil apa pun pada struktur halaman
 * (mis. penggantian nama kelas CSS, urutan kolom {@code indexedby}, atau elemen yang hilang) dapat
 * membuat parsing gagal senyap (banyak blok {@code try/catch} kosong yang hanya mencatat galat ke
 * {@link ais.common.ErrorAuditUtil} tanpa menghentikan proses) atau menghasilkan data yang salah
 * tanpa terdeteksi. Tidak ada mekanisme validasi skema/versi terhadap halaman yang diunduh.
 * </p>
 *
 * <p>
 * <b>Catatan tambahan:</b> method {@link #populateData(JSONArray, String, int, Label, Dosen)}
 * tidak memiliki batas kedalaman rekursi eksplisit — pemanggilan berulang akan terus berjalan
 * selama situs SINTA masih mengembalikan halaman dengan artikel, sehingga secara teoritis dapat
 * menghasilkan rekursi yang sangat dalam untuk penulis dengan jumlah publikasi yang sangat banyak.
 * Kode yang dikomentari di awal kelas ({@code data(JSONObject)}) adalah sisa eksperimen sebelumnya
 * untuk mengikuti tautan artikel individual dan tidak lagi dipanggil dari mana pun.
 * </p>
 */
public class SintaCrawler {

	// public static void data(JSONObject jsonObject) throws Exception {
	// if (!jsonObject.isNull("link")) {
	// String link = jsonObject.getString("link");
	// try {
	// Document doc =
	// Jsoup.connect(link).userAgent("Mozilla").timeout(3000).get();
	// // System.out.println("-------------------\n" + link +
	// // "\n-------------------------------\n" + doc.html());
	// String articleLink =
	// doc.select("meta[http-equiv]").attr("content").trim().split(";", 2)[1]
	// .replaceAll("url=", "");
	// System.out.println("-------------------\n" + articleLink +
	// "\n-------------------------------\n");
	// jsonObject.put("articleLink", articleLink);
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:30");
	// e.printStackTrace();
	// }
	// }
	// }

	/**
	 * Mengambil dan mem-parsing satu halaman daftar dokumen sitasi milik penulis SINTA
	 * {@code kode}, menambahkan setiap artikel yang ditemukan ke {@code data}, lalu memanggil
	 * dirinya sendiri secara rekursif untuk halaman berikutnya sampai suatu halaman tidak lagi
	 * mengandung blok artikel. Lihat Javadoc kelas untuk penjelasan lengkap format data yang
	 * diekstrak dan risiko kerapuhan terhadap perubahan struktur situs SINTA.
	 *
	 * @param data  akumulator hasil (dimodifikasi di tempat/mutable): setiap artikel yang
	 *              ditemukan ditambahkan sebagai {@link JSONObject} ke larik ini
	 * @param kode  id penulis pada situs SINTA (parameter {@code id} pada URL detail penulis)
	 * @param page  nomor halaman yang akan diambil pada pemanggilan ini (dimulai dari 1 oleh
	 *              pemanggil awal; bertambah satu pada setiap rekursi)
	 * @param label komponen ZK tempat pesan progres ("Ambil artikel milik ... -> judul")
	 *              ditampilkan ke pengguna selama proses berjalan
	 * @param dosen entitas {@link Dosen} pemilik publikasi, dipakai untuk menampilkan namanya pada
	 *              pesan progres
	 * @throws Exception diteruskan dari kegagalan koneksi/parsing Jsoup yang tidak tertangkap oleh
	 *                    blok {@code try/catch} internal (mis. kegagalan {@code Jsoup.connect(...).get()})
	 */
	public static void populateData(JSONArray data, String kode, int page, Label label, Dosen dosen) throws Exception {
		Document doc = Jsoup.connect("http://sinta.ristekbrin.go.id/authors/detail")
				.data("page", page + "", "view", "documentsgs", "id", kode).userAgent("Mozilla").timeout(3000).get();

		Elements articles = doc.select("dl[class=uk-description-list-line]");
		if (articles.isEmpty()) {
			return;
		}
		for (Element article : articles) {

			// System.out.println("--------------------------------------------------\n"
			// + article.html());

			JSONObject jsonObject = new JSONObject();
			String judul = article.select("dt[class=uk-text-primary]").text();
			jsonObject.put("judul", judul);
			String articleLink = article.select("dt[class=uk-text-primary]").select("a[href]").attr("href");
			jsonObject.put("link", articleLink);

			try {
				String author = article.select("dd").html().split("\n")[0];
				jsonObject.put("author", author);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:58");
			}

			String indexedby = article.select("dd[class=indexed-by]").text();
			// jsonObject.put("indexedby", indexedby);
			String[] s = StringUtils.split(indexedby, "|");
			try {
				String jurnal = s[0].split(",", 2)[0].trim();
				jsonObject.put("jurnal", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:67");
			}
			try {
				String jurnal = s[0].split(",", 2)[1].trim();
				jsonObject.put("page", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:72");
			}

			try {
				String jurnal = s[1].split(":", 2)[1].trim();
				jsonObject.put("vol", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:78");
			}
			try {
				String jurnal = s[2].split(":", 2)[1].trim();
				jsonObject.put("issue", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:83");
			}

			try {
				String jurnal = s[3].trim();
				jsonObject.put("tahun", jurnal);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaCrawler.java:89");
			}

			// data(jsonObject);

			label.setValue("Ambil artikel milik \"" + dosen.getNama() + "\" -> " + judul);

			data.put(jsonObject);
		}

		populateData(data, kode, ++page, label, dosen);
	}

	/**
	 * Titik masuk demo/uji coba manual untuk menjalankan {@link #populateData} dengan id penulis
	 * SINTA contoh ({@code "5983166"}) dan mencetak hasilnya ke konsol.
	 *
	 * @param argv tidak dipakai
	 * @throws Exception diteruskan apa adanya dari {@link #populateData}
	 */
	public static void main(String[] argv) throws Exception {

		JSONArray data = new JSONArray();
		populateData(data, "5983166", 1, new Label(), new Dosen());
		System.out.println(data);
	}

}
