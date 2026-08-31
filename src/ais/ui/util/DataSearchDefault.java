package ais.ui.util;

import org.zkoss.zk.ui.event.Event;

/**
 * Kontrak untuk layar/aksi ZK yang menyediakan pencarian bawaan (default) yang dijalankan
 * otomatis, misalnya saat layar pertama kali dibuka atau saat tombol "reset pencarian"
 * ditekan. Implementasi mengisi ulang kriteria pencarian ke kondisi awal lalu memuat data
 * lewat {@link #onSearchDefault(Event)}.
 */
public interface DataSearchDefault {

	/**
	 * Menjalankan pencarian dengan kriteria bawaan/awal (tanpa filter tambahan dari pengguna).
	 *
	 * @param event event ZK pemicu (mis. klik tombol atau event komposisi layar)
	 */
	public void onSearchDefault(Event event);

}
