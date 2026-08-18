package ais.ui.util;

import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;

/**
 * <h2>Pabrik &amp; penjaga konsistensi kolom grid daftar CRUD (dipakai-ulang seluruh aplikasi)</h2>
 *
 * <p>
 * Kelas utilitas ini menjadi <b>satu tempat terpusat</b> (single source of truth) untuk membangun
 * kolom-kolom grid data pada halaman CRUD e-Campus yang grid-nya dibentuk dari sisi <i>Java</i>
 * (mis. {@code AmbilData*}, jendela tambah/ubah, atau renderer yang menyusun {@link Columns} secara
 * programatik). Tujuannya melengkapi perbaikan CSS terpusat di {@code /css/css_utama.css}: bila CSS
 * memastikan tabel grid <b>mengisi penuh 100%</b> lebar kontainer (tidak ada ruang kosong di sisi
 * kanan) dan responsif di perangkat sempit, maka kelas ini memastikan <b>definisi kolomnya rapi
 * sejak awal</b> — jumlah persentase tepat 100%, kolom aksi memiliki lebar baku yang cukup untuk
 * tombol Ubah/Copy/Hapus yang kini lebih besar, serta kolom tersembunyi dan kolom "+" (detail)
 * ditangani dengan konvensi yang seragam. Dengan begitu, pengembang berikutnya tidak perlu menebak
 * angka lebar, tidak perlu menyalin pola dari halaman lain, dan tidak berisiko membuat grid yang
 * lebar kolomnya menyimpang (kurang/lebih dari 100%) sehingga tampak gepeng atau menyisakan ruang
 * kosong.
 * </p>
 *
 * <h3>Mengapa dipusatkan?</h3>
 * <p>
 * Sebelumnya, ratusan halaman mendeklarasikan lebar kolom satu per satu dengan angka yang sering
 * tidak dijumlahkan sehingga totalnya bisa 60%, 121%, bahkan 206%. Akibatnya tampilan tidak
 * konsisten: ada yang menyisakan ruang kosong, ada yang berdesakan. Memusatkan pembuatan kolom di
 * {@code GridKolomHelper} membuat perawatan jauh lebih mudah — cukup mengubah konstanta atau logika
 * di sini (mis. mengubah lebar baku kolom aksi dari {@link #LEBAR_KOLOM_AKSI}) untuk berdampak ke
 * seluruh grid yang memakainya. Pendekatan ini sejalan dengan praktik terbaik (DRY — <i>Don't
 * Repeat Yourself</i>) dan memudahkan audit konsistensi UI.
 * </p>
 *
 * <h3>Konvensi kolom</h3>
 * <ul>
 *   <li><b>Kolom data</b> — {@link #kolomData(String, int)} / {@link #kolomData(String, int, String)}:
 *       kolom berisi nilai, lebarnya dinyatakan dalam persen. Jumlah seluruh kolom data + kolom aksi
 *       sebaiknya = 100. Bila perlu pengurutan klik-header, sertakan {@code sortField}.</li>
 *   <li><b>Kolom aksi</b> — {@link #kolomAksi()}: kolom terakhir tempat tombol Ubah/Copy/Hapus
 *       (dari {@code CommonUiFactoryHelper.copyEditDeleteButtons}) diletakkan. Lebar baku
 *       {@link #LEBAR_KOLOM_AKSI} sengaja cukup lega agar tiga tombol berikon besar tampil rapi dan
 *       di tengah.</li>
 *   <li><b>Kolom detail</b> — {@link #kolomDetail()}: kolom sempit selebar {@link #LEBAR_KOLOM_DETAIL}
 *       untuk tombol "+" pembuka baris detail. Lebar piksel tetap (bukan persen) karena ikonnya
 *       berukuran tetap; sisa lebar dibagi ke kolom persen.</li>
 *   <li><b>Kolom tersembunyi</b> — {@link #kolomTersembunyi(String)}: kolom 0% yang sengaja
 *       disembunyikan namun tetap menjaga keselarasan jumlah sel header dan baris.</li>
 * </ul>
 *
 * <h3>Penjaga total 100%</h3>
 * <p>
 * {@link #totalPersen(MyColumnConfig...)} menghitung total persentase kolom, sedangkan
 * {@link #pasang(Grid, MyColumnConfig...)} memasang seluruh kolom ke grid sekaligus. Saat mode
 * <i>debug</i> aktif, {@code pasang} mencetak peringatan bila total menyimpang jauh dari 100 — ini
 * membantu menangkap definisi yang keliru sedini mungkin tanpa mengganggu produksi (tidak melempar
 * exception agar halaman tetap tampil; CSS {@code width:100%} tetap menjadi jaring pengaman visual).
 * </p>
 *
 * <h3>Hubungan dengan CSS &amp; responsivitas</h3>
 * <p>
 * Grid yang dibangun lewat helper ini diharapkan diberi {@code sclass} {@link #SCLASS_GRID_DATA}
 * ({@code "dgrid"}) agar terhubung ke aturan CSS terpusat: pada desktop tabel mengisi 100% (kolom
 * persen membagi ruang secara proporsional), sedangkan pada layar &le; 820px tabel diberi lebar
 * minimum sehingga dapat digeser horizontal tanpa kolom menjadi gepeng. Dengan kombinasi
 * "definisi kolom rapi (Java) + tata letak adaptif (CSS)", grid tampil enak dipandang baik di
 * desktop maupun perangkat mobile.
 * </p>
 *
 * <h3>Contoh pemakaian</h3>
 * <pre>{@code
 * MyGrid grid = new MyGrid();
 * grid.setSclass(GridKolomHelper.SCLASS_GRID_DATA);
 * GridKolomHelper.pasang(grid,
 *         GridKolomHelper.kolomDetail(),
 *         GridKolomHelper.kolomData("Kode", 12, "kode"),
 *         GridKolomHelper.kolomData("Nama", 38, "nama"),
 *         GridKolomHelper.kolomData("Prodi", 25),
 *         GridKolomHelper.kolomData("SKS", 15, "sks"),
 *         GridKolomHelper.kolomAksi());   // 12+38+25+15+10 = 100
 * }</pre>
 *
 * @author e-Campus UI Team
 */
public final class GridKolomHelper {

	/** Sclass standar grid data CRUD; menghubungkan grid ke aturan tata letak terpusat di css_utama.css. */
	public static final String SCLASS_GRID_DATA = "dgrid";

	/** Lebar baku kolom aksi (Ubah/Copy/Hapus) — cukup lega untuk tiga tombol berikon besar. */
	public static final String LEBAR_KOLOM_AKSI = "10%";

	/** Lebar baku kolom "+" pembuka detail baris (piksel tetap karena ikon berukuran tetap). */
	public static final String LEBAR_KOLOM_DETAIL = "50px";

	private GridKolomHelper() {
		// utilitas statis — tidak untuk diinstansiasi
	}

	/**
	 * Membuat kolom data berlabel dengan lebar persen.
	 *
	 * @param label  judul kolom (otomatis diterjemahkan via {@link MyColumnConfig})
	 * @param persen lebar kolom dalam persen (0–100)
	 * @return kolom siap dipasang
	 */
	public static MyColumnConfig kolomData(String label, int persen) {
		MyColumnConfig kolom = new MyColumnConfig(label);
		kolom.setWidth(persen + "%");
		return kolom;
	}

	/**
	 * Membuat kolom data berlabel, berlebar persen, sekaligus dapat diurutkan dengan klik header.
	 *
	 * @param label     judul kolom
	 * @param persen    lebar kolom dalam persen
	 * @param sortField nama properti untuk pengurutan ({@code sort="auto(<field>)"})
	 * @return kolom siap dipasang
	 */
	public static MyColumnConfig kolomData(String label, int persen, String sortField) {
		MyColumnConfig kolom = kolomData(label, persen);
		if (sortField != null && !sortField.trim().isEmpty()) {
			try {
				kolom.setSort("auto(" + sortField.trim() + ")");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/GridKolomHelper.java:127");
				// pengurutan opsional — abaikan bila tak dapat dipasang
			}
		}
		return kolom;
	}

	/**
	 * Membuat kolom aksi (kolom terakhir) tempat tombol Ubah/Copy/Hapus diletakkan.
	 *
	 * @return kolom aksi berlebar {@link #LEBAR_KOLOM_AKSI}
	 */
	public static MyColumnConfig kolomAksi() {
		MyColumnConfig kolom = new MyColumnConfig("Aksi");
		kolom.setWidth(LEBAR_KOLOM_AKSI);
		kolom.setAlign("center");
		return kolom;
	}

	/**
	 * Membuat kolom "+" pembuka baris detail (lebar piksel tetap).
	 *
	 * @return kolom detail berlebar {@link #LEBAR_KOLOM_DETAIL}
	 */
	public static MyColumnConfig kolomDetail() {
		MyColumnConfig kolom = new MyColumnConfig("");
		kolom.setWidth(LEBAR_KOLOM_DETAIL);
		return kolom;
	}

	/**
	 * Membuat kolom tersembunyi (0%) yang tetap menjaga jumlah sel tetap selaras.
	 *
	 * @param label judul kolom (untuk dokumentasi/aksesibilitas walau tak tampak)
	 * @return kolom berlebar 0%
	 */
	public static MyColumnConfig kolomTersembunyi(String label) {
		MyColumnConfig kolom = new MyColumnConfig(label);
		kolom.setWidth("0%");
		return kolom;
	}

	/**
	 * Menjumlahkan lebar persen dari kolom-kolom yang diberikan (kolom non-persen dilewati).
	 *
	 * @param kolom daftar kolom
	 * @return total persen (idealnya 100)
	 */
	public static int totalPersen(MyColumnConfig... kolom) {
		int total = 0;
		if (kolom != null) {
			for (MyColumnConfig k : kolom) {
				if (k == null) {
					continue;
				}
				String w = k.getWidth();
				if (w != null && w.trim().endsWith("%")) {
					try {
						total += (int) Math.round(Double.parseDouble(w.trim().replace("%", "")));
					} catch (NumberFormatException ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/ui/util/GridKolomHelper.java:186");
						// abaikan lebar yang tak dapat diurai
					}
				}
			}
		}
		return total;
	}

	/**
	 * Memasang seluruh kolom ke {@code grid} dalam satu langkah, sekaligus memeriksa total persen.
	 * Bila total menyimpang jauh dari 100 (dan mode debug aktif), peringatan dicetak ke konsol —
	 * tidak melempar exception agar halaman tetap tampil; CSS {@code width:100%} tetap menjadi
	 * jaring pengaman visual.
	 *
	 * @param grid  grid tujuan (sebaiknya ber-{@code sclass} {@link #SCLASS_GRID_DATA})
	 * @param kolom kolom-kolom yang akan dipasang berurutan
	 * @return objek {@link Columns} yang sudah terisi
	 */
	public static Columns pasang(Grid grid, MyColumnConfig... kolom) {
		Columns columns = new Columns();
		columns.setParent(grid);
		if (kolom != null) {
			for (MyColumnConfig k : kolom) {
				if (k != null) {
					k.setParent(columns);
				}
			}
		}
		int total = totalPersen(kolom);
		if (total != 0 && Math.abs(total - 100) > 3) {
			System.out.println("[GridKolomHelper] PERINGATAN: total lebar kolom persen = " + total
					+ "% (idealnya 100%). Periksa definisi kolom grid.");
		}
		return columns;
	}

	/** @return {@link Column} terakhir (kolom aksi) dari sebuah {@link Columns}, atau {@code null}. */
	public static Column kolomTerakhir(Columns columns) {
		if (columns == null || columns.getChildren().isEmpty()) {
			return null;
		}
		Object last = columns.getChildren().get(columns.getChildren().size() - 1);
		return last instanceof Column ? (Column) last : null;
	}
}
