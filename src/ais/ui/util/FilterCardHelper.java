package ais.ui.util;

import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

/**
 * <h2>Pembangun "Kartu Filter" pencarian standar CRUD (dipakai-ulang seluruh aplikasi)</h2>
 *
 * <p>
 * Kelas utilitas ini menjadi <b>satu sumber terpusat</b> (single source of truth) untuk membentuk
 * blok filter/pencarian baku e-Campus dari sisi <i>Java</i> — yaitu struktur HTML/ZK yang dikenal
 * lewat rangkaian <i>sclass</i> {@code ais-crud-filter-card} &rarr; {@code ais-crud-filter-head}
 * (judul + tombol aksi) &rarr; {@code ais-crud-filter-body} (grid berisi field filter). Struktur
 * inilah yang sudah dipakai konsisten di ratusan halaman {@code .zul} (mis. {@code agama.zul} untuk
 * filter ringkas dan {@code mahasiswa.zul} untuk filter banyak field), serta menjadi sasaran
 * standardisasi halaman-halaman lama yang sebelumnya memakai pola "peluncur pencarian + popup"
 * ({@code ecampus-zul-filter-launcher} + {@code addWindowPencarian}) yang tampak sempit dan kurang
 * rapi. Dengan memusatkan pembuatannya di sini, halaman yang membangun UI secara programatik tidak
 * perlu lagi menyalin-tempel rangkaian {@code Div} ber-<i>sclass</i> yang sama; cukup memanggil
 * {@link #buatKartuFilter(String)} lalu mengisi area aksi dan area body.
 * </p>
 *
 * <h3>Mengapa dipusatkan?</h3>
 * <p>
 * Sebelum standardisasi, setiap halaman menata sendiri bilah filternya sehingga tampilannya
 * berbeda-beda: ada yang memakai {@code borderlayout} dengan {@code north} tipis berisi tombol
 * "Pencarian Lebih Lanjut" yang membuka popup, ada yang inline namun dengan gaya tak seragam.
 * Ketidakseragaman ini menyulitkan pemeliharaan dan membingungkan pengguna. Dengan
 * {@code FilterCardHelper}, perubahan tampilan maupun perilaku filter (mis. menambah ikon,
 * mengubah jarak, atau menyesuaikan perilaku responsif) <b>cukup dilakukan di CSS terpusat</b>
 * ({@code /css/css_utama.css}, blok {@code ais-crud-filter-*}) dan/atau di kelas ini, lalu otomatis
 * berlaku konsisten di mana pun. Pendekatan ini sejalan dengan prinsip DRY (<i>Don't Repeat
 * Yourself</i>) dan praktik komponen yang dapat dipakai-ulang.
 * </p>
 *
 * <h3>Anatomi kartu</h3>
 * <p>
 * {@link #buatKartuFilter(String)} mengembalikan sebuah {@link Div} ber-<i>sclass</i>
 * {@code ais-crud-filter-card} yang sudah berisi dua bagian:
 * </p>
 * <ul>
 *   <li><b>Kepala</b> ({@code ais-crud-filter-head}) — memuat judul ({@code ais-crud-filter-title},
 *       mis. "Pencarian &amp; Filter") di kiri dan wadah tombol aksi ({@code ais-crud-filter-actions})
 *       di kanan. Tombol seperti Tambah/Cari ditempatkan di sini melalui {@link #areaAksi(Div)}.</li>
 *   <li><b>Badan</b> ({@code ais-crud-filter-body}) — wadah tempat grid field filter dipasang
 *       melalui {@link #areaBody(Div)}. Grid sebaiknya memakai {@code sclass="ais-crud-filter-grid"}
 *       agar mengikuti tata letak responsif terpusat (kolom membungkus rapi di layar sempit).</li>
 * </ul>
 *
 * <h3>Responsivitas</h3>
 * <p>
 * Karena seluruh gaya berada di CSS (bukan <i>inline</i>), kartu filter otomatis menyesuaikan diri:
 * pada desktop field tersusun rapi dalam beberapa kolom, sedangkan pada perangkat sempit kolom
 * membungkus ke bawah sehingga tetap terbaca dan nyaman disentuh. Pengembang cukup fokus pada
 * <i>field</i> yang relevan; tata letak adaptif ditangani oleh kontrak <i>sclass</i> ini.
 * </p>
 *
 * <h3>Contoh pemakaian</h3>
 * <pre>{@code
 * Div kartu = FilterCardHelper.buatKartuFilter("Pencarian & Filter");
 * // tombol aksi di kepala:
 * FilterCardHelper.areaAksi(kartu).appendChild(tombolTambah);
 * FilterCardHelper.areaAksi(kartu).appendChild(tombolCari);
 * // grid field di badan:
 * MyGrid filter = new MyGrid();
 * filter.setSclass("ais-crud-filter-grid");
 * // ... susun kolom + baris field ...
 * FilterCardHelper.areaBody(kartu).appendChild(filter);
 * // pasang ke North borderlayout (dengan padding) atau langsung ke portal:
 * FilterCardHelper.bungkusNorth(kartu).setParent(north);
 * }</pre>
 *
 * <p>
 * <b>Catatan pemeliharaan.</b> Jangan menata ulang gaya kartu lewat <i>inline style</i> di
 * pemanggil; cukup andalkan <i>sclass</i> agar seluruh halaman tetap seragam dan satu-titik-ubah
 * terjaga. Untuk membangun kolom grid data (bukan filter), gunakan {@link GridKolomHelper}; untuk
 * tombol aksi baris (Ubah/Copy/Hapus), gunakan {@code CommonUiFactoryHelper.copyEditDeleteButtons}.
 * Ketiganya membentuk perangkat reuse UI CRUD yang saling melengkapi.
 * </p>
 *
 * @author e-Campus UI Team
 */
public final class FilterCardHelper {

	/** Sclass kartu filter terluar — terhubung ke gaya terpusat di css_utama.css. */
	public static final String SCLASS_CARD = "ais-crud-filter-card";
	/** Sclass bagian kepala (judul + aksi). */
	public static final String SCLASS_HEAD = "ais-crud-filter-head";
	/** Sclass judul kartu filter. */
	public static final String SCLASS_TITLE = "ais-crud-filter-title";
	/** Sclass wadah tombol aksi (kanan kepala). */
	public static final String SCLASS_ACTIONS = "ais-crud-filter-actions";
	/** Sclass bagian badan (tempat grid field filter). */
	public static final String SCLASS_BODY = "ais-crud-filter-body";
	/** Sclass grid field filter (tata letak responsif). */
	public static final String SCLASS_FILTER_GRID = "ais-crud-filter-grid";

	private FilterCardHelper() {
		// utilitas statis — tidak untuk diinstansiasi
	}

	/**
	 * Membangun kerangka kartu filter standar berisi kepala (judul + wadah aksi) dan badan kosong.
	 *
	 * @param judul teks judul (mis. "Pencarian &amp; Filter"); bila {@code null} dipakai default
	 * @return {@link Div} kartu filter siap diisi via {@link #areaAksi(Div)} dan {@link #areaBody(Div)}
	 */
	public static Div buatKartuFilter(String judul) {
		Div kartu = new Div();
		kartu.setSclass(SCLASS_CARD);

		Div head = new Div();
		head.setSclass(SCLASS_HEAD);
		head.setParent(kartu);

		Label title = new Label(judul == null || judul.trim().isEmpty() ? "Pencarian & Filter" : judul);
		title.setSclass(SCLASS_TITLE);
		title.setParent(head);

		Div actions = new Div();
		actions.setSclass(SCLASS_ACTIONS);
		actions.setParent(head);

		Div body = new Div();
		body.setSclass(SCLASS_BODY);
		body.setParent(kartu);

		return kartu;
	}

	/**
	 * Mengambil wadah tombol aksi (kanan kepala) dari kartu filter.
	 *
	 * @param kartuFilter kartu hasil {@link #buatKartuFilter(String)}
	 * @return {@link Div} {@code ais-crud-filter-actions}, atau {@code null} bila struktur tak dikenal
	 */
	public static Div areaAksi(Div kartuFilter) {
		return cariAnakBerSclass(cariAnakBerSclass(kartuFilter, SCLASS_HEAD), SCLASS_ACTIONS);
	}

	/**
	 * Mengambil wadah badan (tempat grid field) dari kartu filter.
	 *
	 * @param kartuFilter kartu hasil {@link #buatKartuFilter(String)}
	 * @return {@link Div} {@code ais-crud-filter-body}, atau {@code null} bila struktur tak dikenal
	 */
	public static Div areaBody(Div kartuFilter) {
		return cariAnakBerSclass(kartuFilter, SCLASS_BODY);
	}

	/**
	 * Membungkus kartu filter dengan padding agar pas dipasang di {@code north} borderlayout.
	 *
	 * @param kartuFilter kartu yang akan dibungkus
	 * @return {@link Div} pembungkus berisi {@code kartuFilter}
	 */
	public static Div bungkusNorth(Div kartuFilter) {
		Div wrap = new Div();
		wrap.setStyle("padding:12px;box-sizing:border-box;");
		if (kartuFilter != null) {
			kartuFilter.setParent(wrap);
		}
		return wrap;
	}

	/** Mencari anak langsung ber-{@code sclass} tertentu. */
	private static Div cariAnakBerSclass(Div induk, String sclass) {
		if (induk == null) {
			return null;
		}
		for (Object anak : induk.getChildren()) {
			if (anak instanceof Div && sclass.equals(((Div) anak).getSclass())) {
				return (Div) anak;
			}
		}
		return null;
	}
}
