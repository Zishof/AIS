package ais.ui.util;

import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my tabbox. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Tabbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code init()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Tabbox
 */
public class MyTabbox extends Tabbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9054954341532370611L;

	public MyTabbox() {
		super();
		init();
	}

	private void init() {

		if (Common.isMobile()) {

			Common.createDefaultTimerNoBusy(new EventListener() {

				@SuppressWarnings({ "unchecked" })
				@Override
				public void onEvent(Event arg0) throws Exception {

					List<Tab> tabs = getTabs().getChildren();
					Vbox vbox = new Vbox();
					Hbox hbox;
					for (int i = 0; i < tabs.size(); i++) {
						if (i == 0 || i % 3 == 0) {
							hbox = new Hbox();
							vbox.appendChild(hbox);
						}
						
					}

				}
			});

		}

	}
}
