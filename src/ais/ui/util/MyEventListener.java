package ais.ui.util;

import org.zkoss.zk.ui.event.EventListener;

/**
 * Kelas dasar abstrak untuk {@link EventListener} kustom di AIS yang perlu membawa satu nilai
 * konteks tambahan ({@link #value}) selain event ZK itu sendiri. Pola ini dipakai ketika
 * listener yang sama didaftarkan berulang untuk beberapa komponen (mis. dalam perulangan
 * membangun baris grid) dan masing-masing instance perlu "mengingat" data terkait baris/objek
 * tertentu tanpa bergantung pada atribut komponen ZK. Subclass mengimplementasikan
 * {@code onEvent(Event)} dari {@link EventListener} sesuai kebutuhan masing-masing, memanfaatkan
 * {@link #value} untuk mengakses konteks yang sudah diset sebelumnya (biasanya lewat
 * konstruktor turunan).
 */
public abstract class MyEventListener implements EventListener {

	/** Nilai konteks bebas yang dilekatkan pada instance listener ini oleh kode pembuatnya. */
	public Object value;

}
