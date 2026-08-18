package ais.common;

import java.util.ArrayList;
import java.util.List;

import ais.database.model.Konfigurasi;

/**
 * Pusat <b>formalisasi</b> seluruh pesan yang dikirimkan kepada pengguna — baik
 * melalui <b>email</b>, <b>notifikasi aplikasi</b> (lonceng ZKoss / pusat
 * notifikasi JSP), maupun <b>WhatsApp</b>. Tujuan kelas ini adalah memastikan
 * setiap pesan keluar memiliki <b>bahasa yang sangat resmi/formal</b> dan
 * <b>panjang minimal yang memadai</b> (secara baku 1.000 kata), sembari tetap
 * mempertahankan isi inti pesan apa adanya.
 *
 * <h3>Mengapa kelas ini ada</h3>
 * Sebelumnya, kalimat-kalimat baku tersebar (hardcoded) di {@link CommonNotifikasi},
 * {@link CommonEmail}, dan {@code MailSender}. Akibatnya gaya bahasa tidak seragam
 * dan tidak dapat diubah administrator tanpa menyentuh kode. Kelas ini memusatkan
 * semua kalimat baku tersebut sehingga: (1) seragam di semua kanal, (2) dapat
 * <b>dikustomisasi</b> lewat menu Konfigurasi pada grup <b>"Grup Notifikasi"</b>
 * (lihat {@code KonfigurasiNewAction#initTabGrupNotifikasi}), dan (3) menjamin
 * panjang minimal yang dapat diatur.
 *
 * <h3>Idempoten (aman dipanggil berkali-kali)</h3>
 * Setiap pesan yang sudah diformalkan diberi <b>penanda tersembunyi</b>
 * ({@link #PENANDA_HTML} untuk HTML, {@link #PENANDA_WA} untuk teks WA). Bila pesan
 * yang sama melewati lebih dari satu titik pemformatan (mis. notifikasi yang juga
 * dikirim sebagai email), pemanggilan kedua langsung mengembalikan pesan apa adanya
 * sehingga tidak terjadi pemformatan ganda (salam/tanda tangan dobel).
 *
 * <h3>Kompatibilitas</h3>
 * Ditulis untuk Java 1.6/1.7 (tanpa lambda/stream) dan defensif terhadap null,
 * agar tidak pernah menggagalkan jalur pengiriman pesan; bila terjadi galat saat
 * memformat, pesan asli dikembalikan apa adanya.
 */
public class FormalisasiPesanUtil {

	private FormalisasiPesanUtil() {
	}

	/** Penanda tersembunyi (komentar HTML) bahwa body HTML sudah diformalkan. */
	public static final String PENANDA_HTML = "<!-- AIS_PESAN_FORMAL_V1 -->";
	/** Penanda tersembunyi (zero-width) bahwa teks WA sudah diformalkan. */
	public static final String PENANDA_WA = "​​";

	// ==========================================================================
	// KUNCI KONFIGURASI (semua tampil di tab "Grup Notifikasi")
	// ==========================================================================
	public static final String K_AKTIF = "notif_formal_aktif";
	public static final String K_TERAPKAN_EMAIL = "notif_formal_terapkan_email";
	public static final String K_TERAPKAN_NOTIF = "notif_formal_terapkan_notifikasi";
	public static final String K_TERAPKAN_WA = "notif_formal_terapkan_wa";
	public static final String K_MINIMAL_KATA = "notif_formal_minimal_kata";
	public static final String K_NAMA_INSTITUSI = "notif_formal_nama_institusi";
	public static final String K_SALAM = "notif_formal_salam";
	public static final String K_PEMBUKA = "notif_formal_pembuka";
	public static final String K_PENTING_JUDUL = "notif_formal_klausul_penting_judul";
	public static final String K_PENTING = "notif_formal_klausul_penting";
	public static final String K_LANGKAH_JUDUL = "notif_formal_klausul_langkah_judul";
	public static final String K_LANGKAH = "notif_formal_klausul_langkah";
	public static final String K_INTEGRITAS_JUDUL = "notif_formal_klausul_integritas_judul";
	public static final String K_INTEGRITAS = "notif_formal_klausul_integritas";
	public static final String K_BANTUAN_JUDUL = "notif_formal_klausul_bantuan_judul";
	public static final String K_BANTUAN = "notif_formal_klausul_bantuan";
	public static final String K_PENUTUP = "notif_formal_penutup";
	public static final String K_TANDA_TANGAN = "notif_formal_tanda_tangan";
	public static final String K_DISCLAIMER = "notif_formal_disclaimer";
	public static final String K_TAMBAHAN_1 = "notif_formal_klausul_tambahan_1";
	public static final String K_TAMBAHAN_2 = "notif_formal_klausul_tambahan_2";
	public static final String K_TAMBAHAN_3 = "notif_formal_klausul_tambahan_3";

	// ==========================================================================
	// NILAI BAKU (default) — sengaja panjang & sangat resmi agar 1 lewatan saja
	// umumnya sudah melampaui 1.000 kata.
	// ==========================================================================
	public static final String D_NAMA_INSTITUSI = "Sistem Informasi Akademik";
	public static final String D_SALAM = "Yth. Bapak/Ibu/Saudara/Saudari Sivitas Akademika yang Kami Hormati,";
	public static final String D_PEMBUKA = "Dengan memanjatkan puji dan syukur ke hadirat Tuhan Yang Maha Esa, serta seraya "
			+ "mengharapkan agar Bapak/Ibu/Saudara/Saudari senantiasa berada dalam keadaan sehat wal'afiat dan "
			+ "sukses dalam menjalankan segala aktivitas, perkenankanlah kami melalui pemberitahuan resmi ini "
			+ "menyampaikan sebuah informasi yang memiliki keterkaitan langsung dengan aktivitas, hak, serta "
			+ "kewajiban akademik maupun administratif Bapak/Ibu/Saudara/Saudari. Kami sampaikan pemberitahuan ini "
			+ "dengan penuh hormat dan kesungguhan, dengan harapan agar dapat diterima, dicermati, dan "
			+ "ditindaklanjuti sebagaimana mestinya.";

	public static final String D_PENTING_JUDUL = "Mengapa pemberitahuan ini penting untuk Anda perhatikan?";
	public static final String D_PENTING = "Setiap pemberitahuan yang kami terbitkan melalui kanal resmi ini bertujuan untuk "
			+ "menjamin bahwa tidak ada satu pun agenda, kewajiban, maupun hak akademik dan administratif Anda yang "
			+ "terlewatkan. Kami menyadari sepenuhnya bahwa kelancaran proses pendidikan sangat ditentukan oleh "
			+ "ketepatan, ketelitian, dan kedisiplinan seluruh pihak dalam menyikapi setiap informasi yang "
			+ "disampaikan. Keterlambatan dalam merespons informasi semacam ini berpotensi menimbulkan konsekuensi "
			+ "administratif maupun akademik yang dapat memengaruhi kelancaran studi, rekam jejak penilaian, status "
			+ "keuangan, ataupun keikutsertaan Anda pada berbagai kegiatan yang menyertainya. Oleh karena itu, "
			+ "dengan segala kerendahan hati, kami sangat menganjurkan agar Anda berkenan membaca keseluruhan pesan "
			+ "ini hingga tuntas, memahami setiap butir yang tercantum di dalamnya, serta menindaklanjutinya "
			+ "sesegera mungkin sesuai dengan ketentuan yang berlaku.";

	public static final String D_LANGKAH_JUDUL = "Langkah-langkah yang perlu Anda lakukan.";
	public static final String D_LANGKAH = "Mohon dengan hormat agar Anda berkenan segera meninjau informasi ini melalui portal "
			+ "resmi institusi pada kesempatan pertama. Apabila Anda membuka pemberitahuan ini secara langsung dari "
			+ "dalam aplikasi, baik melalui lonceng pemberitahuan pada antarmuka ZKoss maupun pusat notifikasi pada "
			+ "antarmuka JSP, Anda cukup menekan (klik) pemberitahuan ini dan sistem akan secara otomatis membuka "
			+ "halaman yang dituju dalam jendela tampilan tersendiri. Kami mengimbau agar Anda mencatat setiap "
			+ "tanggal, batas waktu, serta ketentuan yang tercantum, dan melakukan langkah tindak lanjut secara "
			+ "berurutan dan cermat agar tidak terjadi kekeliruan maupun kelalaian yang tidak diinginkan.";

	public static final String D_INTEGRITAS_JUDUL = "Komitmen terhadap integritas dan ketepatan waktu.";
	public static final String D_INTEGRITAS = "Perlu kami sampaikan bahwa seluruh proses akademik dan administratif di lingkungan "
			+ "institusi ini dijalankan dengan menjunjung tinggi nilai-nilai kejujuran, kedisiplinan, keadilan, dan "
			+ "tanggung jawab. Kami mengimbau Bapak/Ibu/Saudara/Saudari untuk senantiasa mematuhi setiap batas waktu "
			+ "(tenggat) yang telah ditetapkan, menjaga keaslian serta keorisinalan setiap pekerjaan dan dokumen "
			+ "yang Anda sampaikan, dan memastikan bahwa seluruh data yang Anda berikan adalah benar, sahih, serta "
			+ "dapat dipertanggungjawabkan. Sikap proaktif, jujur, dan bertanggung jawab dalam menindaklanjuti "
			+ "pemberitahuan ini merupakan bagian yang tidak terpisahkan dari budaya akademik yang sehat, "
			+ "profesional, dan bermartabat, yang senantiasa kita junjung tinggi bersama.";

	public static final String D_BANTUAN_JUDUL = "Bantuan dan informasi lebih lanjut.";
	public static final String D_BANTUAN = "Apabila di kemudian hari terdapat hal-hal yang dirasa kurang jelas, Anda mengalami "
			+ "kendala teknis, ataupun membutuhkan penjelasan tambahan berkenaan dengan isi pemberitahuan ini, kami "
			+ "dengan senang hati membuka diri untuk membantu. Jangan ragu untuk menghubungi unit pelayanan "
			+ "akademik, dosen atau guru pengampu, bagian administrasi/tata usaha, ataupun administrator sistem pada "
			+ "jam kerja yang telah ditentukan. Kami berkomitmen untuk memberikan pelayanan yang sebaik-baiknya dan "
			+ "memastikan setiap kebutuhan serta pertanyaan Anda dapat tertangani dengan cepat, tepat, dan "
			+ "memuaskan.";

	public static final String D_PENUTUP = "Demikian pemberitahuan resmi ini kami sampaikan dengan sebenar-benarnya untuk dapat "
			+ "menjadi perhatian sekaligus untuk ditindaklanjuti sebagaimana mestinya. Besar harapan kami agar "
			+ "informasi ini dapat memberikan manfaat serta kemudahan bagi Bapak/Ibu/Saudara/Saudari. Atas segala "
			+ "perhatian, kerja sama, pengertian, dan tanggung jawab yang telah Anda berikan, kami menyampaikan "
			+ "penghargaan yang setinggi-tingginya serta ucapan terima kasih yang setulus-tulusnya.";

	public static final String D_TANDA_TANGAN = "Hormat kami,";
	public static final String D_DISCLAIMER = "Pesan ini dibuat dan dikirimkan secara otomatis oleh sistem. Mohon untuk tidak "
			+ "membalas pesan ini secara langsung. Apabila Anda memerlukan tindak lanjut, silakan menggunakan kanal "
			+ "resmi yang telah disediakan oleh institusi.";

	public static final String D_TAMBAHAN_1 = "Sebagai bagian dari upaya berkelanjutan untuk meningkatkan mutu pelayanan dan "
			+ "keterbukaan informasi, institusi senantiasa berusaha menyampaikan setiap pemberitahuan secara jelas, "
			+ "lengkap, dan tepat waktu. Kami memandang bahwa komunikasi yang baik antara institusi dengan seluruh "
			+ "sivitas akademika merupakan fondasi penting bagi terciptanya lingkungan pendidikan yang harmonis, "
			+ "tertib, dan produktif. Oleh sebab itu, kami sangat menghargai kesediaan Bapak/Ibu/Saudara/Saudari "
			+ "untuk membaca, memahami, dan memberikan perhatian yang semestinya terhadap setiap informasi yang "
			+ "kami sampaikan, sekecil apa pun informasi tersebut tampak pada awalnya.";
	public static final String D_TAMBAHAN_2 = "Kami juga ingin menegaskan bahwa kerahasiaan serta keamanan data pribadi Anda "
			+ "merupakan prioritas yang senantiasa kami jaga. Seluruh informasi yang tercantum dalam pemberitahuan "
			+ "ini ditujukan secara khusus kepada penerima yang berhak. Apabila Anda merasa bukan merupakan pihak "
			+ "yang dituju, atau menerima pesan ini karena suatu kekeliruan, kami mohon dengan hormat agar Anda "
			+ "berkenan mengabaikan dan tidak menyebarluaskan isi pemberitahuan ini kepada pihak mana pun, serta "
			+ "apabila berkenan menyampaikan informasi tersebut kepada kami agar dapat segera kami perbaiki demi "
			+ "menjaga ketertiban dan keamanan bersama.";
	public static final String D_TAMBAHAN_3 = "Akhir kata, perkenankanlah kami sekali lagi menyampaikan rasa hormat dan apresiasi "
			+ "yang sebesar-besarnya atas waktu, perhatian, serta dedikasi yang telah Anda curahkan. Kami berharap "
			+ "agar hubungan baik dan kerja sama yang telah terjalin selama ini dapat terus terpelihara dan semakin "
			+ "erat di masa-masa mendatang. Semoga segala ikhtiar yang kita lakukan bersama senantiasa memperoleh "
			+ "kemudahan, kelancaran, serta keberkahan. Sekali lagi, kami ucapkan terima kasih yang tiada terhingga "
			+ "atas perhatian dan kerja samanya.";

	// ==========================================================================
	// PEMBACAAN KONFIGURASI
	// ==========================================================================

	private static String cfg(String key, String def) {
		try {
			Konfigurasi k = Common.getKonfigurasi(key, def);
			if (k != null && k.getNilai() != null && !k.getNilai().trim().isEmpty()) {
				return k.getNilai();
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/FormalisasiPesanUtil.java:169");
			// abaikan — kembalikan default.
		}
		return def;
	}

	private static boolean aktifKey(String key) {
		return aktifKeyDef(key, true);
	}

	/**
	 * Seperti {@link #aktifKey(String)} namun dengan default eksplisit {@code def}
	 * (dipakai bila konfigurasi belum diisi). Berguna untuk kanal yang default-nya
	 * bukan AKTIF, mis. formalisasi notifikasi in-app (lihat {@link #terapkanNotifikasi()}).
	 */
	private static boolean aktifKeyDef(String key, boolean def) {
		try {
			String d = def ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF;
			return Common.getKonfigurasi(key, d).getNilai().trim().equalsIgnoreCase(Konfigurasi.AKTIF);
		} catch (Throwable t) {
			return def;
		}
	}

	/** Master switch formalisasi pesan. */
	public static boolean aktif() {
		return aktifKey(K_AKTIF);
	}

	public static boolean terapkanEmail() {
		return aktifKey(K_TERAPKAN_EMAIL);
	}

	/**
	 * Formalisasi untuk NOTIFIKASI IN-APP (lonceng/pusat notifikasi). Default
	 * <b>TIDAK AKTIF</b>: notifikasi dalam aplikasi — terutama di mobile — sebaiknya
	 * RINGKAS supaya isi inti (mis. info kehadiran &amp; kepulangan) langsung terbaca dan
	 * tidak tertimbun preamble panjang. Formalisasi tetap dipakai untuk EMAIL
	 * (lihat {@link #terapkanEmail()}, default AKTIF). Admin dapat menyalakan kembali
	 * lewat konfigurasi {@code notif_formal_terapkan_notifikasi} = AKTIF.
	 */
	public static boolean terapkanNotifikasi() {
		return aktifKeyDef(K_TERAPKAN_NOTIF, false);
	}

	public static boolean terapkanWa() {
		return aktifKey(K_TERAPKAN_WA);
	}

	/** Panjang minimal kata (baku 1.000), dibatasi pada rentang wajar 0..6000. */
	public static int minimalKata() {
		int min = 1000;
		try {
			min = Integer.parseInt(cfg(K_MINIMAL_KATA, "1000").trim());
		} catch (Throwable t) {
			min = 1000;
		}
		if (min < 0) {
			min = 0;
		}
		if (min > 6000) {
			min = 6000;
		}
		return min;
	}

	public static String namaInstitusi() {
		return cfg(K_NAMA_INSTITUSI, D_NAMA_INSTITUSI);
	}

	public static String salam() {
		return cfg(K_SALAM, D_SALAM);
	}

	public static String pembuka() {
		return cfg(K_PEMBUKA, D_PEMBUKA);
	}

	public static String pentingJudul() {
		return cfg(K_PENTING_JUDUL, D_PENTING_JUDUL);
	}

	public static String pentingIsi() {
		return cfg(K_PENTING, D_PENTING);
	}

	public static String langkahJudul() {
		return cfg(K_LANGKAH_JUDUL, D_LANGKAH_JUDUL);
	}

	public static String langkahIsi() {
		return cfg(K_LANGKAH, D_LANGKAH);
	}

	public static String integritasJudul() {
		return cfg(K_INTEGRITAS_JUDUL, D_INTEGRITAS_JUDUL);
	}

	public static String integritasIsi() {
		return cfg(K_INTEGRITAS, D_INTEGRITAS);
	}

	public static String bantuanJudul() {
		return cfg(K_BANTUAN_JUDUL, D_BANTUAN_JUDUL);
	}

	public static String bantuanIsi() {
		return cfg(K_BANTUAN, D_BANTUAN);
	}

	public static String penutup() {
		return cfg(K_PENUTUP, D_PENUTUP);
	}

	public static String tandaTangan() {
		return cfg(K_TANDA_TANGAN, D_TANDA_TANGAN);
	}

	public static String disclaimer() {
		return cfg(K_DISCLAIMER, D_DISCLAIMER);
	}

	/** Kumpulan paragraf baku (untuk dirangkai & sebagai bahan pemenuhan panjang minimal). */
	private static List<String> poolKlausul() {
		List<String> l = new ArrayList<String>();
		l.add("<b>" + pentingJudul() + "</b><br>" + pentingIsi());
		l.add("<b>" + integritasJudul() + "</b><br>" + integritasIsi());
		l.add("<b>" + bantuanJudul() + "</b><br>" + bantuanIsi());
		l.add(cfg(K_TAMBAHAN_1, D_TAMBAHAN_1));
		l.add(cfg(K_TAMBAHAN_2, D_TAMBAHAN_2));
		l.add(cfg(K_TAMBAHAN_3, D_TAMBAHAN_3));
		return l;
	}

	// ==========================================================================
	// UTILITAS TEKS
	// ==========================================================================

	/** Menghapus tag & komentar HTML menjadi teks polos (untuk hitung kata / WA). */
	public static String stripHtml(String html) {
		if (html == null) {
			return "";
		}
		String s = html;
		try {
			s = s.replaceAll("(?is)<!--.*?-->", " ");
			s = s.replaceAll("(?i)<\\s*br\\s*/?>", "\n");
			s = s.replaceAll("(?i)</\\s*(p|li|div|tr|h[1-6])\\s*>", "\n");
			s = s.replaceAll("(?i)<\\s*li[^>]*>", "\n- ");
			s = s.replaceAll("<[^>]*>", " ");
			s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
					.replace("&quot;", "\"").replace("&#39;", "'");
			s = s.replace(PENANDA_WA, "");
			// rapikan spasi tetapi pertahankan baris baru.
			s = s.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
			s = s.replaceAll("\\n{3,}", "\n\n");
			s = s.trim();
		} catch (Throwable t) {
			return html;
		}
		return s;
	}

	/** Menghitung jumlah kata pada konten (HTML akan dibersihkan terlebih dahulu). */
	public static int hitungKata(String konten) {
		String teks = stripHtml(konten);
		if (teks.trim().isEmpty()) {
			return 0;
		}
		String[] kata = teks.trim().split("\\s+");
		return kata.length;
	}

	public static boolean sudahFormalHtml(String html) {
		return html != null && html.contains(PENANDA_HTML);
	}

	public static boolean sudahFormalWa(String teks) {
		return teks != null && teks.startsWith(PENANDA_WA);
	}

	// ==========================================================================
	// API UTAMA — HTML (email & notifikasi)
	// ==========================================================================

	/**
	 * Membungkus body HTML menjadi pesan yang sangat formal dan memenuhi panjang
	 * minimal. Idempoten: bila {@code body} sudah pernah diformalkan
	 * ({@link #PENANDA_HTML}) atau master switch nonaktif, body dikembalikan apa
	 * adanya. Cocok untuk pesan yang BELUM memiliki struktur formal sendiri (mis.
	 * email dari {@link CommonEmail} atau body bebas dari modul lain).
	 *
	 * @param subject judul pesan (boleh null) — disertakan pada paragraf rujukan
	 * @param body    isi inti pesan dalam HTML (boleh null)
	 * @return body HTML yang sudah diformalkan, atau body asli bila tidak diproses
	 */
	public static String bungkusFormalHtml(String subject, String body) {
		if (body == null) {
			body = "";
		}
		if (!aktif() || sudahFormalHtml(body)) {
			return body;
		}
		try {
			StringBuilder b = new StringBuilder();
			b.append(PENANDA_HTML);
			b.append(salam()).append("<br><br>");
			b.append(pembuka());
			if (subject != null && !subject.trim().isEmpty()) {
				b.append(" Adapun pemberitahuan ini berkenaan dengan: <b>").append(subject.trim()).append("</b>.");
			}
			b.append("<br><br>");

			// Isi inti pesan dipertahankan apa adanya.
			b.append("<b>Isi Pemberitahuan.</b><br>");
			b.append(body);
			b.append("<br><br>");

			// Langkah tindak lanjut.
			b.append("<b>").append(langkahJudul()).append("</b><br>").append(langkahIsi()).append("<br><br>");

			// Klausul baku + pemenuhan panjang minimal.
			b.append(rangkaiHinggaMinimal(b.toString(), poolKlausul()));

			// Penutup + tanda tangan + disclaimer.
			b.append("<br>").append(penutup()).append("<br><br>");
			b.append(tandaTangan()).append("<br>");
			b.append("<b>").append(namaInstitusi()).append("</b><br>");
			b.append("<i>").append(disclaimer()).append("</i>");

			return b.toString();
		} catch (Throwable t) {
			return body;
		}
	}

	/**
	 * Memastikan body HTML yang SUDAH memiliki struktur formal (mis. hasil
	 * {@code CommonNotifikasi.formatBaku}) mencapai panjang minimal kata, lalu
	 * memberi penanda formal. TIDAK menambahkan salam/tanda tangan baru (agar tidak
	 * dobel). Idempoten terhadap penanda.
	 *
	 * @param body    body HTML yang sudah terstruktur
	 * @param subject judul (tidak dipakai untuk menambah salam, hanya kelengkapan)
	 * @return body yang dijamin memenuhi panjang minimal & bertanda formal
	 */
	public static String pastikanMinimalKataHtml(String body, String subject) {
		if (body == null) {
			body = "";
		}
		if (!aktif() || sudahFormalHtml(body)) {
			return body;
		}
		try {
			StringBuilder b = new StringBuilder();
			b.append(PENANDA_HTML);
			b.append(body);
			b.append(rangkaiHinggaMinimal(b.toString(), poolKlausul()));
			return b.toString();
		} catch (Throwable t) {
			return body;
		}
	}

	/**
	 * Merangkai paragraf-paragraf dari {@code pool} (dalam balutan HTML) sampai total
	 * kata pada {@code sudahAda} + hasil rangkaian mencapai {@link #minimalKata()}.
	 * Bila pool habis namun belum cukup, pool diulang (dengan batas iterasi) sebagai
	 * jaring pengaman agar panjang minimal selalu tercapai.
	 *
	 * @param sudahAda konten yang sudah terkumpul (untuk dihitung kata awalnya)
	 * @param pool     kumpulan paragraf baku
	 * @return potongan HTML tambahan (mungkin kosong bila sudah cukup)
	 */
	private static String rangkaiHinggaMinimal(String sudahAda, List<String> pool) {
		int min = minimalKata();
		StringBuilder tambahan = new StringBuilder();
		int total = hitungKata(sudahAda);
		if (pool == null || pool.isEmpty()) {
			return "";
		}
		int guard = 0;
		int maxIterasi = 400;
		while (total < min && guard < maxIterasi) {
			String p = pool.get(guard % pool.size());
			if (p != null && !p.trim().isEmpty()) {
				tambahan.append("<br><br>").append(p);
				total += hitungKata(p);
			}
			guard++;
		}
		return tambahan.toString();
	}

	// ==========================================================================
	// API UTAMA — TEKS WhatsApp (plain text)
	// ==========================================================================

	/**
	 * Menyusun teks WhatsApp yang resmi dari body (boleh HTML), diawali prefiks
	 * {@code awal} dan judul tebal. Tag HTML dibersihkan, lalu panjang minimal kata
	 * dipenuhi memakai paragraf baku. Idempoten via {@link #PENANDA_WA}.
	 *
	 * @param judul judul pesan (boleh null)
	 * @param body  isi pesan (boleh HTML/teks)
	 * @param awal  prefiks (mis. catatan otomatis), boleh null
	 * @return teks WA yang resmi & memenuhi panjang minimal
	 */
	public static String teksWa(String judul, String body, String awal) {
		String htmlAsli = body == null ? "" : body;
		String isi = stripHtml(htmlAsli);
		// Bila body SUDAH formal (mengandung penanda HTML dari hulu, mis. hasil
		// formatBaku/bungkusFormalHtml) atau WA sudah pernah diformalkan, JANGAN
		// menambah salam/penutup baru — cukup rangkai prefiks + judul + isi resmi.
		boolean sudahFormal = sudahFormalHtml(htmlAsli) || sudahFormalWa(isi);
		if (!aktif() || !terapkanWa()) {
			// kembalikan rangkaian apa adanya (perilaku lama tetap aman).
			StringBuilder s = new StringBuilder();
			if (awal != null) {
				s.append(awal);
			}
			if (judul != null && !judul.trim().isEmpty()) {
				s.append("*").append(judul.trim()).append("*\n\n");
			}
			s.append(isi);
			return s.toString();
		}
		try {
			StringBuilder s = new StringBuilder();
			s.append(PENANDA_WA);
			if (awal != null && !awal.trim().isEmpty()) {
				s.append(awal);
				if (!awal.endsWith("\n")) {
					s.append("\n\n");
				}
			}
			if (!sudahFormal) {
				// body mentah: tambahkan salam pembuka resmi.
				s.append(stripHtml(salam())).append("\n\n");
			}
			if (judul != null && !judul.trim().isEmpty()) {
				s.append("*").append(judul.trim()).append("*\n\n");
			}
			s.append(isi);

			// pemenuhan panjang minimal memakai paragraf baku (teks polos).
			int min = minimalKata();
			List<String> pool = poolKlausul();
			int total = hitungKata(s.toString());
			int guard = 0;
			while (total < min && guard < 400 && !pool.isEmpty()) {
				String p = stripHtml(pool.get(guard % pool.size()));
				if (p != null && !p.trim().isEmpty()) {
					s.append("\n\n").append(p);
					total += hitungKata(p);
				}
				guard++;
			}

			if (!sudahFormal) {
				// body mentah: lengkapi penutup, tanda tangan, dan disclaimer.
				s.append("\n\n").append(stripHtml(penutup()));
				s.append("\n\n").append(stripHtml(tandaTangan())).append("\n").append(namaInstitusi());
				s.append("\n\n").append(stripHtml(disclaimer()));
			}
			return s.toString();
		} catch (Throwable t) {
			StringBuilder s = new StringBuilder();
			if (awal != null) {
				s.append(awal);
			}
			if (judul != null && !judul.trim().isEmpty()) {
				s.append("*").append(judul.trim()).append("*\n\n");
			}
			s.append(isi);
			return s.toString();
		}
	}
}
