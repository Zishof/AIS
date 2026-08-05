package ais.ui.util;

import org.zkoss.zul.Tabpanel;

/**
 * {@code MyTabpanel} adalah pembungkus (subclass) terpusat dari {@link org.zkoss.zul.Tabpanel}
 * milik ZK yang menetapkan satu perilaku default: <b>gaya (style) awal {@code min-height: 2000px}</b>.
 *
 * <h2>Latar belakang masalah</h2>
 * <p>
 * Pada ZK 5.5 yang dipakai aplikasi ini, sebuah {@code Tabpanel} yang berada di dalam rantai tinggi
 * otomatis ({@code height:auto}) — misalnya ketika halaman dimuat sebagai satu-scroll, di-include ke
 * tab lain, atau ketika komponen anak di-mount secara lazy setelah tab dipilih — kerap "kolaps"
 * menjadi tinggi 0 piksel. Akibatnya isi tab yang sebenarnya sudah ter-mount tampak <i>kosong</i>
 * karena area panelnya menciut sampai tidak terlihat, atau konten yang lebih tinggi dari 0px justru
 * terpotong. Gejala ini sering muncul berpasangan dengan masalah lazy-load {@code onClick} tab (di
 * ZK 5.5 klik tab hanya memicu {@code onSelect} pada {@code Tabbox}) sehingga sulit dibedakan antara
 * "konten belum di-mount" dengan "konten sudah di-mount tetapi panelnya 0px".
 * </p>
 *
 * <h2>Solusi yang diberikan kelas ini</h2>
 * <p>
 * Dengan memberi {@code min-height: 2000px} sebagai gaya bawaan, panel dijamin memiliki tinggi
 * minimal yang cukup besar sehingga tidak pernah kolaps ke 0px. Nilai 2000px sengaja dipilih besar
 * agar cukup untuk mayoritas halaman konten panjang (mis. daftar pertemuan e-Learning, rekap nilai,
 * laporan) tanpa perlu mengatur tinggi per-halaman satu per satu. Untuk halaman yang isinya lebih
 * tinggi dari 2000px, panel tetap tumbuh mengikuti konten (karena yang dipasang adalah
 * <i>min</i>-height, bukan height tetap); untuk halaman yang isinya lebih pendek, tersisa ruang
 * kosong di bawah — konsekuensi yang diterima demi menjamin konten selalu tampil.
 * </p>
 *
 * <h2>Cara pemakaian</h2>
 * <ol>
 *   <li><b>Di berkas ZUL:</b> gunakan atribut {@code use}, contoh:
 *       {@code <tabpanel use="ais.ui.util.MyTabpanel"/>} atau
 *       {@code <tabpanel use="ais.ui.util.MyTabpanel" style="padding:0"/>}.</li>
 *   <li><b>Di kode Java:</b> ganti pemanggilan Tabpanel lama menjadi {@code new MyTabpanel()}
 *       (atau bentuk FQN {@code new ais.ui.util.MyTabpanel()} bila tidak ingin menambah import).</li>
 * </ol>
 *
 * <h2>Kompatibilitas &amp; catatan penting</h2>
 * <p>
 * Kelas ini adalah subclass murni dari {@code Tabpanel} tanpa mengubah API apa pun; seluruh method
 * bawaan ({@code setHeight}, {@code setStyle}, {@code setSclass}, {@code appendChild}, dst.) tetap
 * tersedia dan berperilaku sama. Setiap kode yang melakukan {@code instanceof Tabpanel} atau melakukan
 * <i>cast</i> ke {@code Tabpanel} tetap berjalan karena {@code MyTabpanel} <b>adalah</b> sebuah
 * {@code Tabpanel}. Oleh sebab itu penggantian bersifat <i>drop-in</i> dan aman secara tipe.
 * </p>
 * <p>
 * Gaya {@code min-height: 2000px} dipasang di konstruktor sebagai <b>default</b>. Bila pemanggil
 * (baik lewat atribut {@code style} pada ZUL maupun panggilan {@code setStyle(...)} di Java) menetapkan
 * gaya sendiri, maka gaya default tersebut akan <b>ditimpa</b> — ini disengaja agar halaman yang sudah
 * punya kebutuhan ukuran spesifik tetap dihormati. Sebaliknya, pemakaian {@code setHeight(...)} tidak
 * menghapus properti {@code min-height} pada atribut {@code style} sehingga keduanya bisa berdampingan.
 * </p>
 * <p>
 * Karena nilai {@code min-height} berlaku pada SEMUA tabpanel yang memakai kelas ini, perlu diperhatikan
 * bahwa dialog/tab kecil pun akan memiliki tinggi minimal 2000px. Jika di kemudian hari nilai 2000px
 * dirasa terlalu besar untuk sebagian halaman, cukup ubah satu tempat: konstruktor kelas ini (atau
 * pindahkan ke aturan CSS terpusat) sehingga perubahannya berlaku global tanpa menyentuh ratusan
 * berkas pemanggil. Inilah alasan utama sentralisasi ke dalam satu kelas.
 * </p>
 *
 * @author AIS
 * @see org.zkoss.zul.Tabpanel
 */
public class MyTabpanel extends Tabpanel {

	private static final long serialVersionUID = 1L;

	
	/**
	 * Membuat {@code MyTabpanel} dengan gaya bawaan {@code min-height: 2000px} (anti-kolaps ke 0px)
	 * dan {@code overflow-y: auto}. Ditambah penyesuaian otomatis di sisi klien: bila parent-nya
	 * berukuran terbatas (&lt; 2000px), tabpanel akan menyusut setinggi parent dan memunculkan SCROLL
	 * (lihat {@link #SKRIP_AUTO_SCROLL}). Gaya dasar tetap dapat ditimpa pemanggil via {@code style}
	 * (ZUL) atau {@code setStyle(...)} (Java).
	 */
	public MyTabpanel() {
		super();
		// min-height:2000px mencegah panel kolaps ke 0px saat parent container belum punya tinggi definitif
		// (kasus umum: tabbox di dalam form row / popup window dengan height:100% yang belum terselesaikan).
		// Ini SEKALIGUS menjadi kondisi awal sebelum SKRIP_AUTO_SCROLL berjalan (skrip me-reset ke 2000px
		// lalu mengukur), dan menjadi perilaku fallback saat auto-tinggi cerdas dimatikan.
		//setStyle("min-height:2000px;overflow-y:auto;overflow-x:hidden;");
		// Auto-tinggi cerdas (default ON, dapat dimatikan via konfigurasi tabpanel_auto_tinggi_cerdas):
		// ukur wadah pengklip terdekat -> pas-kan tinggi panel + overflow-y:auto (scroll), sehingga konten
		// tidak terpotong / tidak menyisakan ruang 2000px. Bila tak ada wadah terbatas, skrip membiarkan
		// min-height:2000px (anti-kolaps). Dipasang di onBind agar mengukur setelah DOM ter-render.
		
		
		/** TIDAK NGEFEK **/
		//if (AUTO_TINGGI_CERDAS) {
		//	setWidgetListener("onBind", SKRIP_AUTO_SCROLL);
		//}
	}
}
