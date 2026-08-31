package ais.common.sinta;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.PerguruanTinggi;
import ais.database.model.SintaArticle;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.penelitiandanpengabdian.TahapanPenyusunanArtikel;

/**
 * Web crawler (bukan klien API resmi — mengambil dan mem-parsing halaman HTML publik secara
 * langsung dengan Jsoup) untuk situs SINTA (Science and Technology Index) Kementerian Pendidikan,
 * yaitu basis data sitasi/publikasi ilmiah nasional Indonesia yang dikelola Kemdikbud. Kelas ini
 * dipakai untuk menyinkronkan dua jenis data dari SINTA ke database AIS berdasarkan kode SINTA satu
 * perguruan tinggi ({@link PerguruanTinggi#getKodeSinta()}):
 * <ol>
 * <li><b>Daftar dosen ber-SINTA</b> — dikumpulkan lewat {@link #populateKodeSintaDosen(JSONArray,
 * String, int, Label)} dari halaman profil afiliasi SINTA, dicocokkan ke entitas {@link Dosen} lokal
 * berdasarkan NIDN, lalu kolom {@code kodeSinta} pada dosen yang cocok diperbarui.</li>
 * <li><b>Artikel ilmiah tiap dosen</b> — dikumpulkan lewat {@link #singkronkanArtikel(Dosen, Label,
 * Session, TahapanPenyusunanArtikel)} (yang mendelegasikan pengambilan data mentah ke
 * {@link SintaCrawler#populateData}), disimpan sebagai {@link SintaArticle}, dan dari situ dibuatkan
 * (atau diperbarui) catatan {@link Artikel} pada modul penelitian-dan-pengabdian AIS, termasuk
 * membuat entitas {@link JurnalPenelitian} baru secara otomatis bila nama jurnal pada data SINTA
 * belum dikenal.</li>
 * </ol>
 * <p>
 * Method utama, {@link #singkronkan(Label, PerguruanTinggi)}, dipanggil dari layar ZKoss (parameter
 * {@link Label} dipakai sebagai indikator progres tekstual yang diperbarui langsung selama proses
 * berjalan — pola "polling label" sederhana untuk memberi umpan balik ke pengguna tanpa mekanisme
 * push/WebSocket). Kegagalan pada satu dosen (mis. halaman SINTA-nya berformat tak terduga) SENGAJA
 * ditangkap dan dicatat ke audit tanpa menghentikan sinkronisasi dosen lain dalam batch yang sama —
 * lihat komentar inline pada loop {@code singkronkanArtikel} di {@link #singkronkan}.
 * </p>
 *
 * <h2>Kerapuhan terhadap perubahan struktur halaman SINTA</h2>
 * <p>
 * Karena data diambil dengan mem-parsing HTML berdasarkan selector CSS tetap (mis.
 * {@code dl[class=uk-description-list-line]}, {@code a[class=text-blue]}) dan pola teks tertentu
 * (mis. mencari elemen yang teksnya mengandung substring {@code "nidn"} lalu memecahnya dengan
 * separator {@code ":"}), crawler ini SANGAT rentan berhenti bekerja bila SINTA mengubah struktur/
 * kelas CSS halamannya — tidak ada API resmi berversi yang dipakai di sini. Setiap kegagalan parsing
 * per field (link, author, vol, issue, tahun, jurnal, judul, page) ditangkap individual dan dicatat
 * ke audit tanpa menggagalkan penyimpanan artikel secara keseluruhan, sehingga artikel tetap
 * tersimpan dengan field yang berhasil diambil meski sebagian field lain gagal di-parse.
 * </p>
 *
 * <p>
 * Metode {@link #main(String[])} adalah skrip uji coba manual berdiri sendiri yang menjalankan
 * ulang logika pengambilan satu halaman profil afiliasi SINTA dengan kode perguruan tinggi
 * ({@code id=626}) dan kode dosen ({@code id=8443}) yang di-hardcode, mencetak hasilnya ke konsol —
 * tidak dipanggil dari alur aplikasi.
 * </p>
 */
public class SintaPtCrawler {

	/**
	 * Titik masuk sinkronisasi utama, dipanggil dari layar ZKoss untuk memicu proses penuh: (1)
	 * mengambil daftar dosen ber-SINTA milik {@code perguruanTinggi} lewat {@link
	 * #populateKodeSintaDosen(JSONArray, String, int, Label)}; (2) mencocokkan setiap entri hasil
	 * crawl (berdasarkan NIDN) dengan entitas {@link Dosen} lokal yang masih aktif, memperbarui kolom
	 * {@code kodeSinta} pada dosen yang cocok, dan mengumpulkan dosen yang berhasil dicocokkan ke
	 * dalam {@code dosenSinta}; (3) untuk setiap dosen dalam {@code dosenSinta}, memicu sinkronisasi
	 * artikel lewat {@link #singkronkanArtikel(Dosen, Label, Session, TahapanPenyusunanArtikel)},
	 * dengan kegagalan pada satu dosen ditangkap dan dicatat ke audit tanpa menghentikan sisa batch.
	 * Sepanjang proses, {@code label} diperbarui berulang kali untuk menampilkan progres ke pengguna,
	 * dan dikosongkan kembali di akhir (baik sukses maupun bila tidak ada data ditemukan).
	 *
	 * @param label           komponen ZKoss yang nilainya diperbarui langsung sebagai indikator
	 *                        progres tekstual selama proses berjalan
	 * @param perguruanTinggi perguruan tinggi target sinkronisasi; method langsung kembali tanpa
	 *                        melakukan apa pun bila {@code null} atau kode SINTA-nya kosong
	 */
	@SuppressWarnings("unchecked")
	public static void singkronkan(final Label label, PerguruanTinggi perguruanTinggi) {
		if (perguruanTinggi == null || perguruanTinggi.getKodeSinta().isEmpty()) {
			return;
		}

		JSONArray data = new JSONArray();
		try {
			populateKodeSintaDosen(data, perguruanTinggi.getKodeSinta(), 1, label);
		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:40");
		}

		System.out.println("data -> " + data);

		if (data.length() == 0) {
			label.setValue("");
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		List<Dosen> dosenSinta = new ArrayList<Dosen>();
		for (int i = 0; i < data.length(); i++) {
			try {
				JSONObject jsonObject = data.getJSONObject(i);
				if (!jsonObject.isNull("nidn")) {
					List<Dosen> dosens = ConstantValues.simpleList(session.createCriteria(Dosen.class)
							.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
							.add(Restrictions.eq("nidn", jsonObject.getString("nidn"))), Dosen.class);
					for (Dosen dosen : dosens) {
						String id = jsonObject.getString("id");
						label.setValue("update data dosen -> " + dosen.getNama() + " dengan id SINTA " + id);
						dosen.setKodeSinta(id);
						session.getTransaction().begin();
						Common.refreshUpdate(session, dosen);
						session.getTransaction().commit();
						dosenSinta.add(dosen);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:70");
			}

		}

		TahapanPenyusunanArtikel tahapanPenyusunanArtikel = (TahapanPenyusunanArtikel) session
				.createCriteria(TahapanPenyusunanArtikel.class).add(Restrictions.eq("nama", "Dicetak (terbit)"))
				.setMaxResults(1).uniqueResult();

		for (Dosen dosen : dosenSinta) {
			try {
				singkronkanArtikel(dosen, label, session, tahapanPenyusunanArtikel);
			} catch (Exception eArtikel) {
				// Jangan biarkan 1 dosen gagal (mis. artikel SINTA-nya format tak terduga)
				// menghentikan sinkronisasi artikel utk SISA dosen dalam batch ini.
				ais.common.ErrorAuditUtil.record(eArtikel,
						"auto-audit src/ais/common/sinta/SintaPtCrawler.java:singkronkanArtikelBaris dosen="
								+ (dosen == null ? "-" : dosen.getNama()));
			}
		}
		HibernateUtil.closeSession();

		label.setValue("");
	}

	/**
	 * Mengambil dan menyinkronkan seluruh artikel ilmiah milik satu {@code dosen} dari SINTA (lewat
	 * {@link SintaCrawler#populateData}) ke database lokal. Untuk setiap artikel hasil crawl: mencari
	 * {@link SintaArticle} yang sudah ada berdasarkan kombinasi dosen+link+judul (case-insensitive
	 * lewat {@code ilike}), membuat baru bila belum ada, lalu mengisi field-field artikel (link,
	 * author, volume, issue, tahun, jurnal, judul, halaman) satu per satu — SETIAP field dibungkus
	 * {@code try/catch} terpisah sehingga kegagalan parsing satu field (mis. field tidak ada pada
	 * data mentah SINTA) tidak menggagalkan pengisian field lain maupun penyimpanan artikel itu
	 * sendiri.
	 *
	 * <p>
	 * Setelah {@link SintaArticle} tersimpan, bila dosen memiliki akun {@link Tbmuser} aktif, method
	 * ini juga membuat/memperbarui catatan {@link Artikel} pada modul penelitian-dan-pengabdian:
	 * mencari (atau membuat baru bila belum ada) {@link JurnalPenelitian} berdasarkan {@code path}
	 * yang diturunkan dari nama jurnal (huruf kecil, spasi diganti underscore; nama jurnal kosong
	 * memakai fallback {@code "Jurnal Default"}), lalu menghubungkan {@link Artikel} ke
	 * {@link SintaArticle}, {@link Tbmuser}, {@code tahapanPenyusunanArtikel} yang diberikan, dan
	 * {@link JurnalPenelitian} tersebut.
	 * </p>
	 *
	 * @param dosen                     dosen pemilik artikel yang disinkronkan; harus sudah memiliki
	 *                                  {@code kodeSinta} terisi
	 * @param label                     komponen ZKoss untuk indikator progres, diteruskan ke
	 *                                  {@link SintaCrawler#populateData}
	 * @param session                   sesi Hibernate aktif yang dipakai untuk seluruh query/simpan
	 *                                  dalam method ini (dikelola oleh pemanggil, tidak dibuka/
	 *                                  ditutup di sini)
	 * @param tahapanPenyusunanArtikel  tahap default yang diisikan pada {@link Artikel} baru (mis.
	 *                                  "Dicetak (terbit)")
	 */
	public static void singkronkanArtikel(Dosen dosen, Label label, Session session,
			TahapanPenyusunanArtikel tahapanPenyusunanArtikel) {

		JSONArray dataArtikel = new JSONArray();
		try {
			SintaCrawler.populateData(dataArtikel, dosen.getKodeSinta(), 1, label, dosen);
			System.out.println(dataArtikel);
			for (int i = 0; i < dataArtikel.length(); i++) {
				try {
					JSONObject jsonObject = dataArtikel.getJSONObject(i);

					SintaArticle sintaArticle = (SintaArticle) session.createCriteria(SintaArticle.class)
							.add(Restrictions.eq("dosen", dosen))
							.add(Restrictions.ilike("link", jsonObject.getString("link")))
							.add(Restrictions.ilike("nama", jsonObject.getString("judul"))).uniqueResult();
					if (sintaArticle == null) {
						sintaArticle = new SintaArticle();
					}
					sintaArticle.setKeterangan(jsonObject.toString());
					sintaArticle.setDosen(dosen);
					try {
						sintaArticle.setLink(jsonObject.getString("link"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:109");
						// TODO: handle exception
					}
					try {
						sintaArticle.setAuthor(jsonObject.getString("author"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:114");
						// TODO: handle exception
					}
					try {
						sintaArticle.setVol(jsonObject.getString("vol"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:119");
						// TODO: handle exception
					}
					try {
						sintaArticle.setIssue(jsonObject.getString("issue"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:124");
						// TODO: handle exception
					}
					try {
						sintaArticle.setTahun(Integer.parseInt(jsonObject.getString("tahun")));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:129");
						// TODO: handle exception
					}

					try {
						sintaArticle.setJurnal(jsonObject.getString("jurnal"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:135");
						// TODO: handle exception
					}

					try {
						sintaArticle.setNama(jsonObject.getString("judul"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:141");
						// TODO: handle exception
					}

					try {
						sintaArticle.setPage(jsonObject.getString("page"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:147");
						// TODO: handle exception
					}

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, sintaArticle);
					session.getTransaction().commit();

					Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(
							session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("dosen", dosen)).setMaxResults(1),
							Tbmuser.class);
					if (tbmuser != null) {
						String namaJurnal = sintaArticle.getJurnal().isEmpty() ? "Jurnal Default"
								: sintaArticle.getJurnal();

						String path = namaJurnal.toLowerCase().trim().replaceAll(" ", "_");

						JurnalPenelitian jurnalPenelitian = (JurnalPenelitian) ConstantValues
								.simpleObject(
										session.createCriteria(JurnalPenelitian.class)
												.add(Restrictions.eq("path", path)).setMaxResults(1),
										JurnalPenelitian.class);
						if (jurnalPenelitian == null) {
							jurnalPenelitian = new JurnalPenelitian();
							jurnalPenelitian.setJudul(namaJurnal);
							jurnalPenelitian.setPath(path);
							session.getTransaction().begin();
							session.save(jurnalPenelitian);
							session.getTransaction().commit();
						}

						Artikel artikel = (Artikel) session.createCriteria(Artikel.class)
								.add(Restrictions.eq("sintaArticle", sintaArticle)).uniqueResult();
						if (artikel == null) {
							artikel = new Artikel();
						}
						artikel.setTbmuser(tbmuser);
						artikel.setSintaArticle(sintaArticle);
						artikel.setTahapanPenyusunanArtikel(tahapanPenyusunanArtikel);
						artikel.setJurnalPenelitian(jurnalPenelitian);
						session.getTransaction().begin();
						session.saveOrUpdate(artikel);
						session.getTransaction().commit();

					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:193");
				}

			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:198");
		}
	}

	/**
	 * Mengambil satu halaman daftar dosen dari halaman profil afiliasi SINTA
	 * ({@code https://sinta.kemdikbud.go.id/affiliations/profile}) untuk kode perguruan tinggi
	 * {@code kode}, mem-parsing setiap entri dosen (nama, link profil, id SINTA yang diekstrak dari
	 * parameter query pada link, dan NIDN yang dicari dari elemen mana pun yang teksnya mengandung
	 * substring {@code "nidn"}) menjadi satu {@link JSONObject} per dosen, menambahkannya ke
	 * {@code data}, lalu <b>memanggil dirinya sendiri secara rekursif untuk halaman berikutnya</b>
	 * ({@code page + 1}) sampai suatu halaman tidak lagi mengandung elemen artikel/dosen apa pun
	 * (selector {@code dl[class=uk-description-list-line]} kosong) — pada titik itu rekursi berhenti.
	 * Karena rekursi berbasis paginasi tanpa batas atas eksplisit, jumlah pemanggilan bergantung
	 * sepenuhnya pada jumlah halaman yang tersedia di SINTA untuk kode perguruan tinggi tersebut.
	 *
	 * @param data  akumulator hasil; diisi di tempat (bukan dikembalikan) dengan satu
	 *              {@link JSONObject} per dosen yang ditemukan pada seluruh halaman
	 * @param kode  kode SINTA perguruan tinggi yang di-crawl
	 * @param page  nomor halaman yang diambil pada pemanggilan ini (dimulai dari 1 oleh pemanggil
	 *              awal di {@link #singkronkan})
	 * @param label komponen ZKoss yang diperbarui dengan ringkasan data tiap dosen begitu ditemukan,
	 *              sebagai indikator progres
	 * @throws Exception diteruskan dari kegagalan koneksi HTTP Jsoup ({@code timeout} 3 detik) ke
	 *                    situs SINTA
	 */
	public static void populateKodeSintaDosen(JSONArray data, String kode, int page, Label label) throws Exception {
		Document doc = Jsoup.connect("https://sinta.kemdikbud.go.id/affiliations/profile")
				.data("page", page + "", "view", "authors", "id", kode, "sort", "year2").userAgent("Mozilla")
				.timeout(3000).get();
		Elements articles = doc.select("dl[class=uk-description-list-line]");
		if (articles.isEmpty()) {
			return;
		}

		for (Element article : articles) {
			JSONObject jsonObject = new JSONObject();
			String text = article.select("a[class=text-blue]").text();
			jsonObject.put("nama", text);

			String articleLink = article.select("a[class=text-blue]").select("a[href]").attr("href");
			jsonObject.put("link", articleLink);
			String id = null;
			try {
				String[] pairs = articleLink.split("&");

				for (String pair : pairs) {
					try {
						int idx = pair.indexOf("=");
						String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
						String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
						// System.out.println("key => "+key);
						if (key.endsWith("id"))
							id = value.trim();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:230");
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:234");
			}
			jsonObject.put("id", id);

			// System.out.println("---------------------------------------");
			for (Element all : article.getAllElements()) {
				try {
					String nidn = all.text();
					if (nidn.toLowerCase().trim().contains("nidn")) {
						String[] a = StringUtils.split(nidn, ":");
						nidn = a[a.length - 1].trim();
						jsonObject.put("nidn", nidn);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:247");
					// TODO: handle exception
				}

			}
			label.setValue("Ambil data dari SINTA -> " + jsonObject.toString());
			data.put(jsonObject);
		}
		populateKodeSintaDosen(data, kode, ++page, label);
	}

	/**
	 * Skrip uji coba manual berdiri sendiri: mengulang logika inti {@link
	 * #populateKodeSintaDosen(JSONArray, String, int, Label)} (tanpa rekursi paginasi dan tanpa
	 * parameter {@link Label}) terhadap SATU halaman profil afiliasi SINTA dengan kode perguruan
	 * tinggi ({@code 626}) dan kode dosen ({@code 8443}) yang ditulis langsung sebagai literal,
	 * mencetak hasil parsing sebagai JSON ke konsol. Tidak dipanggil dari alur aplikasi AIS — murni
	 * untuk verifikasi manual perilaku parsing HTML saat pengembangan.
	 *
	 * @param argv argumen baris perintah; tidak dipakai
	 * @throws Exception diteruskan dari kegagalan koneksi HTTP Jsoup ke situs SINTA
	 */
	public static void main(String[] argv) throws Exception {

		Document doc = Jsoup.connect("https://sinta.kemdikbud.go.id/affiliations/profile/626")
				.data("page", "1", "view", "authors", "id", "8443", "sort", "year2").userAgent("Mozilla").timeout(3000)
				.get();

		// Document doc =
		// Jsoup.connect("https://sinta.kemdikbud.go.id/affiliations/profile?page=1&view=authors&id=8443&sort=year2")
		// .timeout(3000)
		// .get();

		// System.out.println(doc.html());

		Elements articles = doc.select("dl[class=uk-description-list-line]");
		// System.out.println(articles.html());

		JSONArray jsonArray = new JSONArray();
		for (Element article : articles) {
			JSONObject jsonObject = new JSONObject();
			String text = article.select("a[class=text-blue]").text();
			jsonObject.put("nama", text);

			String articleLink = article.select("a[class=text-blue]").select("a[href]").attr("href");
			jsonObject.put("link", articleLink);
			String id = null;
			try {
				String[] pairs = articleLink.split("&");

				for (String pair : pairs) {
					try {
						int idx = pair.indexOf("=");
						String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
						String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
						// System.out.println("key => "+key);
						if (key.endsWith("id"))
							id = value.trim();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:294");
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/sinta/SintaPtCrawler.java:298");
			}
			jsonObject.put("id", id);

			// System.out.println("---------------------------------------");
			for (Element all : article.getAllElements()) {
				try {
					String nidn = all.text();
					if (nidn.toLowerCase().trim().contains("nidn")) {
						String[] a = StringUtils.split(nidn, ":");
						nidn = a[a.length - 1].trim();
						jsonObject.put("nidn", nidn);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/sinta/SintaPtCrawler.java:311");
					// TODO: handle exception
				}

			}

			jsonArray.put(jsonObject);
		}

		System.out.println(jsonArray.toString());
	}

}
