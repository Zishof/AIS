package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Textbox;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my textbox angka. Tipe ini membakukan default dan
 * perilaku tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang
 * sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Textbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code EventListener eventListener};
 * pembacaan/pencarian ({@code getValue()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Textbox
 */
public class MyTextboxAngka extends Textbox {

	private EventListener eventListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Common.createDefaultTimerNoBusy(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setValue(MyTextboxAngka.this.getValue());
				}
			}, "", false, 500);
		}
	};

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyTextboxAngka() {
		super();
		setWidth("90%");
		addEventListener("onChange", eventListener);
	}

	public MyTextboxAngka(String value) throws WrongValueException {
		super(value);
		setWidth("90%");
		addEventListener("onChange", eventListener);
	}

	public String getValue() {
		return super.getValue().replaceAll("\\D+", "");
	}

}
