package ais.ui.util;

import java.util.Date;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Timebox;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my timebox. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Timebox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code init()}); mutasi data ({@code
 * setWidth()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Timebox
 */
public class MyTimebox extends Timebox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;

	public MyTimebox() {
		super();
		init();
//		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
//		calendar.set(Calendar.MINUTE, 0);
//		calendar.set(Calendar.SECOND, 0);
//		setValue(calendar.getTime());
	}

	public MyTimebox(Date date) throws WrongValueException {
		super(date);
		init();
//		if (date == null) {
//			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
//			calendar.set(Calendar.MINUTE, 0);
//			calendar.set(Calendar.SECOND, 0);
//			setValue(calendar.getTime());
//		}
	}

	private void init() {
		super.setFormat(Common.timeFormat.get().toPattern());
	}

	@Override
	public void setWidth(String width) {
		// // TODO Auto-generated method stub
		// super.setWidth(width);
	}

	
}
