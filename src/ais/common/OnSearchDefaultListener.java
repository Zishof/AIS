package ais.common;

import org.zkoss.zk.ui.event.Event;

/**
 * Antarmuka callback (listener) sederhana untuk menangani aksi "pencarian default" pada
 * komponen antarmuka ZK di aplikasi AIS. Interface ini mengikuti pola standar
 * {@code EventListener} di ekosistem ZK — alih-alih mengimplementasikan
 * {@code org.zkoss.zk.ui.event.EventListener<Event>} secara langsung, sejumlah komponen/halaman
 * di AIS mendefinisikan kontrak khusus seperti ini agar niat pemanggilan lebih jelas dibaca
 * (yaitu: "lakukan pencarian dengan kriteria bawaan/default", bukan sembarang event umum).
 *
 * <p>
 * Pemakaian tipikal: sebuah komponen (mis. textbox pencarian atau tombol "cari") didaftarkan
 * dengan instance implementasi interface ini sebagai penanganan event, sehingga saat pengguna
 * memicu aksi pencarian tanpa mengubah filter/kriteria tambahan, method {@link
 * #onSearchDefault(Event)} yang dipanggil. Implementasi konkretnya biasanya berada pada
 * kelas controller/composer ZK di modul terkait (mis. daftar data dengan kotak pencarian di
 * bagian atas), yang mengeksekusi query pencarian standar begitu event ini diterima.
 * </p>
 *
 * <p>
 * Karena hanya berisi satu method abstrak, interface ini juga kompatibel dipakai sebagai
 * functional interface (lambda) pada Java 8 ke atas, meskipun basis kode AIS pada umumnya masih
 * memakai gaya anonymous inner class mengikuti kebiasaan penulisan listener ZK yang lama.
 * </p>
 */
public interface OnSearchDefaultListener {

	/**
	 * Dipanggil ketika aksi pencarian dengan kriteria default dipicu oleh pengguna pada
	 * komponen ZK terkait.
	 *
	 * @param event event ZK asli yang memicu pemanggilan ini (mis. event klik tombol cari atau
	 *              event "onOK" pada textbox pencarian)
	 */
	public void onSearchDefault(Event event);
}
