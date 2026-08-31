package ais.ui.util;

import org.zkoss.zk.ui.event.Event;

import ais.database.model.GeneralValueObject;

/**
 * Kontrak gabungan untuk layar/aksi ZK yang perlu melakukan dua hal saat pertama kali dibuka
 * (atau saat menambah data baru): menyiapkan objek data baru lewat {@link
 * #init(GeneralValueObject)} dan menjalankan pencarian dengan kriteria bawaan lewat {@link
 * #onSearchDefault(Event)} — lihat juga {@link DataSearchDefault} yang hanya mencakup bagian
 * pencarian bawaannya saja.
 */
public interface DataInitDefault {

	/**
	 * Menginisialisasi objek data (mengisi nilai default) sebelum ditampilkan di form entri.
	 *
	 * @param obj objek entitas ({@link GeneralValueObject}) yang akan diinisialisasi
	 * @throws Exception diteruskan bila terjadi kegagalan saat inisialisasi
	 */
	public void init(GeneralValueObject obj) throws Exception;

	/**
	 * Menjalankan pencarian dengan kriteria bawaan/awal.
	 *
	 * @param event event ZK pemicu
	 */
	public void onSearchDefault(Event event);
}
