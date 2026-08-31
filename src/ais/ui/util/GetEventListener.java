package ais.ui.util;

import org.zkoss.zk.ui.event.EventListener;

/**
 * Kontrak sederhana untuk komponen/kelas kustom di AIS yang menyimpan referensi ke satu
 * {@link EventListener} sebagai properti yang dapat dibaca dan ditulis ulang dari luar. Dipakai
 * ketika listener suatu komponen perlu diganti atau diakses secara dinamis setelah komponen
 * dibuat (mis. mengganti perilaku klik tombol tergantung state layar), alih-alih listener
 * ditetapkan sekali secara permanen saat komponen dibangun.
 */
public interface GetEventListener {

	/**
	 * Mengambil listener yang sedang terpasang.
	 *
	 * @return listener aktif saat ini, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener();

	/**
	 * Mengganti/menetapkan listener yang dipakai.
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener);

}
