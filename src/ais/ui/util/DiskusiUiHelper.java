package ais.ui.util;

/**
 * <h1>DiskusiUiHelper — Pembangun tampilan "gelembung komentar" diskusi bergaya modern</h1>
 *
 * <p>
 * Kelas utilitas ini menjadi <b>satu sumber terpusat</b> (single source of truth) untuk membentuk
 * potongan HTML kecil yang dipakai pada papan diskusi e-Learning/perkuliahan (lihat
 * {@code PertemuanPunyaDiskusiHelper}). Tujuannya membuat tampilan diskusi <b>menyerupai kolom
 * komentar pada media sosial populer</b> (mis. Facebook): setiap komentar tampil sebagai
 * "gelembung" (bubble) berlatar lembut dengan sudut membulat, nama penulis dicetak tebal, waktu
 * dicetak halus/abu-abu, dan isi komentar di bawahnya. Seluruh <i>gaya visual</i> (warna, jarak,
 * sudut, bayangan, perilaku responsif) <b>tidak</b> ditanam sebagai <i>inline style</i> di Java,
 * melainkan diletakkan terpusat di berkas CSS aplikasi ({@code /css/css_utama.css}, blok
 * {@code ais-diskusi-*}). Java hanya menempelkan <i>class</i> CSS yang sudah disepakati. Dengan
 * begitu, perubahan tampilan cukup dilakukan di satu tempat (CSS) dan otomatis konsisten di mana
 * pun komentar dirender.
 * </p>
 *
 * <h2>Mengapa dipusatkan di satu kelas?</h2>
 * <p>
 * Sebelum standardisasi ini, setiap titik render komentar menyusun sendiri rangkaian HTML
 * ber-<i>inline style</i> (mis. {@code background-color: rgba(169,169,169,0.4); border-radius:20px})
 * yang berbeda-beda dan sulit dirawat: untuk mengganti warna atau jarak, pengembang harus menyisir
 * banyak baris kode Java yang tersebar. Dengan {@code DiskusiUiHelper}, pembuatan markup gelembung
 * komentar dilakukan lewat metode tunggal {@link #bubbleKomentarHtml(String, String, String)}.
 * Pendekatan ini sejalan dengan prinsip <b>DRY</b> (<i>Don't Repeat Yourself</i>) dan praktik
 * komponen yang dapat dipakai-ulang (<i>reusable component</i>) sehingga memudahkan pemeliharaan di
 * kemudian hari: cukup ubah metode ini atau CSS terkait, seluruh papan diskusi langsung mengikuti.
 * </p>
 *
 * <h2>Anatomi gelembung komentar</h2>
 * <p>
 * Markup yang dihasilkan {@link #bubbleKomentarHtml(String, String, String)} berbentuk:
 * </p>
 * <pre>{@code
 * <div class="ais-diskusi-bubble">
 *   <div class="ais-diskusi-meta">
 *     <span class="ais-diskusi-nama">Nama Penulis (Peran)</span>
 *     <span class="ais-diskusi-waktu">2 jam yang lalu</span>
 *   </div>
 *   <div class="ais-diskusi-isi">Isi komentar ...</div>
 * </div>
 * }</pre>
 * <ul>
 *   <li>{@code ais-diskusi-bubble} — wadah gelembung (latar lembut, sudut membulat, bayangan tipis).</li>
 *   <li>{@code ais-diskusi-meta} — baris identitas: nama + waktu.</li>
 *   <li>{@code ais-diskusi-nama} — nama penulis, dicetak tebal dan gelap agar mudah dibaca.</li>
 *   <li>{@code ais-diskusi-waktu} — keterangan waktu, dicetak halus/abu-abu agar tidak dominan.</li>
 *   <li>{@code ais-diskusi-isi} — isi komentar; otomatis membungkus kata panjang/URL agar tidak meluber.</li>
 * </ul>
 *
 * <h2>Responsif (mobile &amp; desktop)</h2>
 * <p>
 * Karena seluruh gaya berada di CSS, gelembung otomatis menyesuaikan diri: pada layar lebar
 * (desktop) gelembung tampil rapat di samping avatar, sedangkan pada layar sempit (ponsel) jarak
 * dan sudut dikecilkan lewat <i>media query</i> ({@code @media (max-width:600px)}) sehingga tetap
 * nyaman dibaca dan disentuh. Pengembang yang memakai kelas ini tidak perlu memikirkan ukuran
 * layar; tata letak adaptif sudah ditangani oleh kontrak <i>class</i> ini.
 * </p>
 *
 * <h2>Keamanan (mencegah HTML/skrip yang tidak diinginkan)</h2>
 * <p>
 * Nama penulis dan isi komentar berasal dari masukan pengguna. Untuk mencegah markup atau skrip
 * yang tidak diinginkan ikut ter-render (mis. tag {@code <script>} atau karakter {@code <}, {@code &}
 * yang merusak struktur), nilai-nilai tersebut di-<i>escape</i> terlebih dahulu melalui
 * {@link #escapeHtml(String)} sebelum disisipkan. Pemanggil yang <i>sudah</i> memiliki HTML aman
 * (mis. isi yang sudah dibersihkan dan diberi tautan) dapat memakai
 * {@link #bubbleKomentarHtmlIsiAman(String, String, String)} agar isi tidak di-escape ulang.
 * Pemisahan dua metode ini membuat keputusan keamanan menjadi eksplisit di sisi pemanggil.
 * </p>
 *
 * <h2>Cara pakai</h2>
 * <pre>{@code
 * String html = DiskusiUiHelper.bubbleKomentarHtml(
 *         "Adelia Amanda (Mahasiswa)", "16 jam yang lalu, Kamis, 25-06-2026", isiTeks);
 * new ais.ui.util.MyHtml(html).setParent(vbox);
 * // Beri sclass pada baris & area aksi agar CSS terpusat berlaku:
 * hboxBarisKomentar.setSclass(DiskusiUiHelper.SCLASS_ROW);
 * hboxAksi.setSclass(DiskusiUiHelper.SCLASS_ACTIONS);
 * }</pre>
 *
 * <h2>Catatan pemeliharaan</h2>
 * <p>
 * Jangan menambahkan <i>inline style</i> baru pada pemanggil; cukup andalkan <i>class</i> CSS di
 * sini agar seluruh tampilan diskusi tetap seragam dan satu-titik-ubah terjaga. Bila membutuhkan
 * varian baru (mis. komentar pengajar disorot warna berbeda), tambahkan <i>class</i> modifikasi di
 * CSS dan metode pembungkus baru di kelas ini — bukan menyalin-tempel HTML di banyak tempat.
 * Konstanta nama <i>class</i> sengaja diekspos sebagai {@code public static final String} agar
 * penamaan konsisten dan mudah dilacak (tidak ada "magic string" yang tersebar).
 * </p>
 *
 * @author e-Campus UI Team
 */
public final class DiskusiUiHelper {

	/** Sclass baris komentar (avatar + isi sejajar) — dipasang pada Hbox baris di sisi ZK. */
	public static final String SCLASS_ROW = "ais-diskusi-row";
	/** Sclass kolom isi komentar (di kanan avatar). */
	public static final String SCLASS_BODY = "ais-diskusi-body";
	/** Sclass baris aksi (Balas / Hapus) agar tombol tampil sebagai tautan teks halus. */
	public static final String SCLASS_ACTIONS = "ais-diskusi-actions";

	private DiskusiUiHelper() {
	}

	/**
	 * Membentuk HTML gelembung komentar bergaya modern dengan isi yang <b>di-escape</b> (aman untuk
	 * teks biasa hasil masukan pengguna). Gunakan varian ini bila {@code isi} berupa teks polos.
	 *
	 * @param oleh  nama penulis beserta peran, mis. {@code "Adelia (Mahasiswa)"} (boleh null).
	 * @param waktu keterangan waktu yang sudah diformat (boleh null/kosong).
	 * @param isi   isi komentar berupa teks polos; akan di-escape agar aman.
	 * @return potongan HTML gelembung komentar siap dipasang ke {@code MyHtml}.
	 */
	public static String bubbleKomentarHtml(String oleh, String waktu, String isi) {
		return rakitBubble(escapeHtml(oleh), escapeHtml(waktu), escapeHtml(isi));
	}

	/**
	 * Sama seperti {@link #bubbleKomentarHtml(String, String, String)} namun isi <b>tidak</b>
	 * di-escape — dipakai bila pemanggil sudah menyiapkan {@code isiAman} sebagai HTML yang
	 * dipercaya (mis. teks bersih yang sebagian sudah diberi tautan {@code <a>}). Nama dan waktu
	 * tetap di-escape.
	 *
	 * @param oleh    nama penulis beserta peran (boleh null).
	 * @param waktu   keterangan waktu (boleh null/kosong).
	 * @param isiAman isi komentar dalam bentuk HTML yang sudah dipastikan aman oleh pemanggil.
	 * @return potongan HTML gelembung komentar siap dipasang ke {@code MyHtml}.
	 */
	public static String bubbleKomentarHtmlIsiAman(String oleh, String waktu, String isiAman) {
		return rakitBubble(escapeHtml(oleh), escapeHtml(waktu), isiAman == null ? "" : isiAman);
	}

	/** Rakit markup final dari bagian-bagian yang sudah disiapkan/di-escape pemanggil. */
	private static String rakitBubble(String namaAman, String waktuAman, String isiAman) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("<div class=\"ais-diskusi-bubble\"><div class=\"ais-diskusi-meta\">");
		sb.append("<span class=\"ais-diskusi-nama\">").append(namaAman == null ? "" : namaAman).append("</span>");
		if (waktuAman != null && waktuAman.trim().length() > 0) {
			sb.append("<span class=\"ais-diskusi-waktu\">").append(waktuAman).append("</span>");
		}
		sb.append("</div><div class=\"ais-diskusi-isi\">").append(isiAman == null ? "" : isiAman).append("</div></div>");
		return sb.toString();
	}

	/**
	 * Meng-<i>escape</i> karakter HTML penting ({@code & < > " '}) agar teks masukan pengguna tampil
	 * apa adanya dan tidak ditafsirkan sebagai markup/skrip. Null diperlakukan sebagai string kosong.
	 *
	 * @param s teks mentah (boleh null).
	 * @return teks aman untuk disisipkan ke dalam HTML.
	 */
	public static String escapeHtml(String s) {
		if (s == null) {
			return "";
		}
		StringBuilder out = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '&':
				out.append("&amp;");
				break;
			case '<':
				out.append("&lt;");
				break;
			case '>':
				out.append("&gt;");
				break;
			case '"':
				out.append("&quot;");
				break;
			case '\'':
				out.append("&#39;");
				break;
			default:
				out.append(c);
			}
		}
		return out.toString();
	}
}
