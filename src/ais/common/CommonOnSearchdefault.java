package ais.common;

import org.zkoss.zk.ui.event.Event;

/**
 * Kontrak (marker interface fungsional) untuk composer/controller ZK yang memiliki perilaku
 * "pencarian default" — yaitu aksi pencarian yang dijalankan secara otomatis begitu suatu
 * halaman/daftar (grid, listbox, atau komponen pencarian lain) selesai dimuat, tanpa menunggu
 * pengguna menekan tombol cari secara eksplisit. Pola ini umum dipakai pada layar-layar AIS yang
 * menampilkan daftar data (mis. daftar mahasiswa, daftar transaksi, daftar nilai) di mana
 * pengguna mengharapkan grid sudah terisi data begitu halaman terbuka, alih-alih tampil kosong
 * sampai tombol "Cari" ditekan.
 *
 * <p>
 * Implementasi khas dari interface ini adalah composer ZK (kelas yang meng-extend
 * {@code org.zkoss.zk.ui.util.GenericForwardComposer} atau sejenisnya) yang mendaftarkan method
 * {@link #onSearchDefault(Event)} sebagai event listener untuk event kustom (mis.
 * {@code onSearchDefault} yang di-post lewat {@code Events.postEvent} atau dipicu dari method
 * {@code doAfterCompose}) yang dijadwalkan berjalan setelah komponen ZK selesai dirender/di-bind.
 * Dengan menandatangani kontrak ini, kelas composer memberi sinyal eksplisit bahwa ia memiliki
 * logika pencarian awal yang terpisah dari logika pencarian manual (mis. method
 * {@code onSearch} atau {@code doSearch} yang dipicu tombol), sehingga kedua alur — otomatis dan
 * manual — dapat berbagi query/filter yang sama namun dipicu dari titik yang berbeda dalam
 * siklus hidup halaman.
 * </p>
 *
 * <p>
 * Interface ini murni sebagai penanda kontrak (tidak memiliki implementasi default maupun
 * konstanta), sehingga setiap kelas yang mengimplementasikannya WAJIB menyediakan implementasi
 * konkret {@link #onSearchDefault(Event)} sendiri — biasanya berisi pembentukan kriteria/HQL
 * pencarian awal (misalnya berdasarkan tahun akademik aktif, semester berjalan, atau filter
 * default lain) lalu memuat hasilnya ke komponen tampilan (grid/listbox) yang relevan.
 * </p>
 */
public interface CommonOnSearchdefault {

	/**
	 * Dipanggil oleh kerangka event ZK untuk menjalankan pencarian default/awal suatu halaman.
	 * Implementasi method ini bertanggung jawab membangun kriteria pencarian bawaan (tanpa
	 * input eksplisit dari pengguna) dan mengisi komponen tampilan terkait dengan hasilnya,
	 * sehingga halaman tidak tampil kosong saat pertama kali dibuka.
	 *
	 * @param event objek {@link Event} ZK yang memicu pemanggilan ini, biasanya event kustom
	 *              {@code onSearchDefault} yang di-post/dijadwalkan setelah komponen selesai
	 *              disusun (doAfterCompose)
	 */
	public void onSearchDefault(Event event);

}
